package de.lenox.servercore.core.scoreboard

import de.lenox.servercore.core.ServerModule
import de.lenox.servercore.core.onServerThread
import de.lenox.servercore.core.storage.JsonFileStore
import de.lenox.servercore.core.storage.Storage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * Shows a packet-only [Sidebar] to every player with the content of [provider], re-rendered every tick
 * (only changed lines are sent). Set [provider] to null to hide the sidebar for everyone; players can hide
 * it for themselves with [setHidden] (`/stats toggle`), which is saved in `servercore/scoreboard/settings.json`.
 */
object ScoreboardModule : ServerModule {
	override val id = "scoreboard"

	@Serializable
	private data class Settings(
		/** UUIDs of players who turned the sidebar off. */
		val hiddenFor: Set<String> = emptySet(),
	)

	private val store = JsonFileStore("scoreboard", Settings.serializer())
	private const val SETTINGS_KEY = "settings"

	private val boards = HashMap<UUID, Sidebar>()
	private val hiddenFor = HashSet<UUID>()

	var provider: ScoreboardProvider? = null

	override fun onServerStarted(server: MinecraftServer) {
		Storage.scope.launch {
			val settings = store.load(SETTINGS_KEY) ?: Settings()
			server.onServerThread {
				hiddenFor.clear()
				settings.hiddenFor.mapNotNullTo(hiddenFor) { runCatching { UUID.fromString(it) }.getOrNull() }
			}
		}
	}

	override fun onTick(server: MinecraftServer) {
		val provider = provider
		if (provider == null) {
			clearAllBoards()
			return
		}
		server.playerList.players.forEach { player ->
			if (player.uuid in hiddenFor) return@forEach
			val board = boards.getOrPut(player.uuid) { Sidebar(player) }
			board.updateTitle(provider.getTitle(player))
			board.updateLines(provider.getLines(player), provider.getScores(player))
		}
	}

	override fun onPlayerLeave(player: ServerPlayer) {
		boards.remove(player.uuid)
	}

	override fun onServerStopping(server: MinecraftServer) {
		boards.clear()
	}

	fun isHidden(player: ServerPlayer) = player.uuid in hiddenFor

	/** Turns the sidebar off or on for one player; the choice is saved. */
	fun setHidden(player: ServerPlayer, hidden: Boolean) {
		if (hidden) {
			hiddenFor += player.uuid
			boards.remove(player.uuid)?.delete()
		} else {
			hiddenFor -= player.uuid // shown again on the next tick
		}
		store.saveAsync(SETTINGS_KEY, Settings(hiddenFor.mapTo(sortedSetOf()) { it.toString() }))
	}

	fun clearAllBoards() {
		boards.values.forEach(Sidebar::delete)
		boards.clear()
	}

	/**
	 * Called (by a mixin) whenever vanilla changes the objective in the sidebar slot, e.g. through
	 * `/scoreboard objectives setdisplay sidebar`, which would otherwise replace our sidebar on every client.
	 */
	@JvmStatic
	fun onVanillaSidebarChanged() {
		boards.values.forEach(Sidebar::resendDisplay)
	}
}
