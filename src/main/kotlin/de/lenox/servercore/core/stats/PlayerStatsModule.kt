package de.lenox.servercore.core.stats

import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.ServerModule
import de.lenox.servercore.core.onServerThread
import de.lenox.servercore.core.storage.JsonFileStore
import de.lenox.servercore.core.storage.Storage
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.time.Instant
import java.util.UUID

/**
 * Tracks playtime and joins per player.
 *
 * Only online players are kept in memory; their stats are saved on leave, every [AUTOSAVE_TICKS] and on
 * shutdown. Offline players are always read from disk, so their files can be edited by hand at any
 * time. Hand edits to an online player's file are overwritten unless `/stats reload` is run first.
 *
 * Everything here runs on the server thread; disk access is done by [store] in the background.
 */
object PlayerStatsModule : ServerModule {
	override val id = "playerstats"

	/** Files are named by UUID: `servercore/playerstats/<uuid>.json`. */
	val store = JsonFileStore("playerstats", PlayerStats.serializer())

	private const val ACCOUNT_TICKS = 20
	private const val AUTOSAVE_TICKS = 20 * 60 * 5
	private const val NANOS_PER_SECOND = 1_000_000_000L

	private class Session(var stats: PlayerStats) {
		/** Wall clock time up to which playtime has been added to [stats]. */
		var accountedUntil = System.nanoTime()
		var dirty = false
	}

	private val sessions = HashMap<UUID, Session>()

	private data class KnownPlayer(val name: String, val uuid: UUID)

	/** Lowercase name -> player, for everyone with a stats file. Used for command lookups and suggestions. */
	private val knownPlayers = HashMap<String, KnownPlayer>()

	private var ticks = 0L

	/** Names of all players with stats, for command suggestions. Server thread only. */
	val knownNames: List<String>
		get() = knownPlayers.values.map { it.name }

	override fun onServerStarted(server: MinecraftServer) {
		Storage.scope.launch { refreshOfflinePlayers(server) }
	}

	override fun onServerStopping(server: MinecraftServer) {
		sessions.forEach { (uuid, session) -> save(uuid, session) }
	}

	override fun onTick(server: MinecraftServer) {
		ticks++
		if (ticks % ACCOUNT_TICKS == 0L) {
			sessions.values.forEach { session ->
				val minutesBefore = session.stats.playtimeSeconds / 60
				account(session)
				if (session.stats.playtimeSeconds / 60 != minutesBefore) StatsScoreboard.update(server, session.stats)
			}
		}
		if (ticks % AUTOSAVE_TICKS == 0L) {
			sessions.forEach { (uuid, session) -> if (session.dirty) save(uuid, session) }
		}
	}

	override fun onPlayerJoin(player: ServerPlayer) {
		val server = player.level().server
		val uuid = player.uuid
		val name = player.plainTextName
		Storage.scope.launch {
			val now = Instant.now()
			// Counted directly on disk, so the join is persisted even if the player leaves right away.
			var previousName = ""
			val stats = store.update(uuid.toString()) { stored ->
				val old = stored ?: PlayerStats()
				previousName = old.name
				old.copy(name = name, joins = old.joins + 1, firstJoin = old.firstJoin ?: now, lastSeen = now)
			}
			server.onServerThread {
				if (previousName.isNotEmpty() && previousName != name) {
					StatsScoreboard.remove(server, previousName)
					// Another player may have taken over the old name in the meantime.
					if (knownPlayers[previousName.lowercase()]?.uuid == uuid) knownPlayers.remove(previousName.lowercase())
				}
				knownPlayers[name.lowercase()] = KnownPlayer(name, uuid)
				// The player may have left (or left and rejoined) while the file was loading.
				if (server.playerList.getPlayer(uuid) === player) sessions[uuid] = Session(stats)
				StatsScoreboard.update(server, stats)
			}
		}
	}

	override fun onPlayerLeave(player: ServerPlayer) {
		val session = sessions.remove(player.uuid) ?: return
		account(session)
		session.stats = session.stats.copy(lastSeen = Instant.now())
		save(player.uuid, session)
	}

	/** Current stats of an online player, including the time played up to now. Server thread only. */
	fun online(uuid: UUID): PlayerStats? = sessions[uuid]?.also(::account)?.stats

	/** Looks up a player by name (case-insensitive): online players from memory, everyone else from disk. */
	suspend fun find(server: MinecraftServer, name: String): Pair<UUID, PlayerStats>? {
		val uuid = server.onServerThread {
			server.playerList.getPlayerByName(name)?.uuid ?: knownPlayers[name.lowercase()]?.uuid
		} ?: return null
		return server.onServerThread { online(uuid) }?.let { uuid to it }
			?: store.load(uuid.toString())?.let { uuid to it }
	}

	/** Stats of every player that ever joined, with live values for online players. */
	suspend fun all(server: MinecraftServer): Map<UUID, PlayerStats> {
		val stored = store.loadAll().mapNotNull { (key, stats) -> key.toUuidOrNull()?.let { it to stats } }.toMap()
		return server.onServerThread { stored + sessions.mapValues { (_, session) -> account(session).stats } }
	}

	/**
	 * Re-reads all files from disk: online players' in-memory stats are replaced (dropping progress since
	 * the last save), the name index and the scoreboard are rebuilt. Use after editing files by hand.
	 */
	suspend fun reload(server: MinecraftServer) {
		val stored = refreshOfflinePlayers(server)
		server.onServerThread {
			sessions.forEach { (uuid, session) ->
				val fromDisk = stored[uuid] ?: return@forEach
				session.stats = fromDisk
				session.accountedUntil = System.nanoTime()
				session.dirty = false
				StatsScoreboard.update(server, fromDisk)
			}
		}
	}

	/** Loads all files, rebuilds [knownPlayers] and writes offline players' stats to the scoreboard. */
	private suspend fun refreshOfflinePlayers(server: MinecraftServer): Map<UUID, PlayerStats> {
		val stored = store.loadAll().mapNotNull { (key, stats) ->
			val uuid = key.toUuidOrNull()
			if (uuid == null) ServerCore.logger.warn("Ignoring {}/{}.json: file name is not a UUID", store.directory, key)
			uuid?.let { it to stats }
		}.toMap()
		server.onServerThread {
			knownPlayers.clear()
			stored.forEach { (uuid, stats) ->
				if (stats.name.isNotEmpty()) knownPlayers[stats.name.lowercase()] = KnownPlayer(stats.name, uuid)
				// Online players are updated by their session instead.
				if (uuid !in sessions) StatsScoreboard.update(server, stats)
			}
			ServerCore.logger.info("Loaded stats of {} players", stored.size)
		}
		return stored
	}

	/** Adds the whole seconds played since the last call to the session's playtime. */
	private fun account(session: Session): Session {
		val seconds = (System.nanoTime() - session.accountedUntil) / NANOS_PER_SECOND
		if (seconds > 0) {
			session.accountedUntil += seconds * NANOS_PER_SECOND
			session.stats = session.stats.copy(playtimeSeconds = session.stats.playtimeSeconds + seconds)
			session.dirty = true
		}
		return session
	}

	private fun save(uuid: UUID, session: Session) {
		store.saveAsync(uuid.toString(), session.stats)
		session.dirty = false
	}

	private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
}
