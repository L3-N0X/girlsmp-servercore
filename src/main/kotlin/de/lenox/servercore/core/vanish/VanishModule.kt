package de.lenox.servercore.core.vanish

import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.ServerModule
import de.lenox.servercore.core.storage.JsonFileStore
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets operators vanish (`/vanish`): a vanished player is invisible to every other player, in every game mode.
 *
 * - **Hidden:** no entity (and no item pickup animation), no tab list entry, not counted in the server list,
 *   `/list`, the sidebar's online count or for sleeping, not found by name in commands of non-operators and
 *   ignored by mobs.
 * - **Silent:** no join, leave, death or advancement messages. Toggling while online fakes a leave/join message,
 *   so others see the usual "left the game" / "joined the game".
 * - **Chat still works:** messages of a vanished player reach everyone as unsigned ("disguised") chat, since
 *   clients disconnect on signed chat from a player that isn't in their tab list.
 *
 * Who is vanished is saved in `servercore/vanish/vanished.json` and survives restarts. The vanilla hooks are
 * mixins that call [isHiddenFrom] and friends.
 */
object VanishModule : ServerModule {
	override val id = "vanish"

	@Serializable
	private data class Settings(
		/** UUIDs of vanished players. */
		val vanished: Set<String> = emptySet(),
	)

	private val store = JsonFileStore("vanish", Settings.serializer())
	private const val SETTINGS_KEY = "vanished"

	/** Concurrent: packet filtering can happen off the server thread. */
	private val vanished: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

	override fun onServerStarted(server: MinecraftServer) {
		// Loaded blocking on purpose: it has to be known before the first player is placed in the world (which only
		// happens once the server ticks), or a vanished player would join visibly. It's one tiny file.
		val settings = runBlocking { store.load(SETTINGS_KEY) } ?: Settings()
		vanished.clear()
		settings.vanished.mapNotNullTo(vanished) { runCatching { UUID.fromString(it) }.getOrNull() }
		ServerCore.logger.info("Loaded {} vanished players", vanished.size)
	}

	override fun onPlayerJoin(player: ServerPlayer) {
		if (!isVanished(player)) return
		ServerCore.logger.info("{} joined vanished", player.plainTextName)
		player.sendMessage(message(Cmp("You are vanished. ", Theme.LIGHT_PURPLE), Cmp("Nobody saw you join.", Theme.SUBTEXT_1)))
	}

	fun isVanished(player: ServerPlayer) = player.uuid in vanished

	@JvmStatic
	fun isVanished(uuid: UUID) = uuid in vanished

	/** Whether [viewer] must not see the player with [target]'s UUID. Everyone but the vanished player themselves. */
	@JvmStatic
	fun isHiddenFrom(target: UUID, viewer: ServerPlayer) = target in vanished && target != viewer.uuid

	@JvmStatic
	fun canSee(viewer: ServerPlayer, target: ServerPlayer) = !isHiddenFrom(target.uuid, viewer)

	/**
	 * Whether commands run by [source] may find [target] by name and list them. Operators (and the console,
	 * command blocks and functions) still can, so they can manage vanished players.
	 */
	@JvmStatic
	fun canSee(source: CommandSourceStack, target: ServerPlayer) =
		target.uuid !in vanished ||
			source.entity?.uuid == target.uuid ||
			source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)

	/** Online players [viewer] can see, including themselves. */
	fun visiblePlayers(viewer: ServerPlayer): List<ServerPlayer> =
		viewer.level().server.playerList.players.filter { canSee(viewer, it) }

	/** Vanishes or reveals an online player; the choice is saved. */
	fun setVanished(player: ServerPlayer, vanish: Boolean) {
		if (vanish == isVanished(player)) return
		val server = player.level().server
		val others = server.playerList.players.filter { it !== player }
		if (vanish) {
			vanished += player.uuid
			// Entity first, then the tab list entry, like vanilla does when a player leaves.
			refreshTracking(player)
			val remove = ClientboundPlayerInfoRemovePacket(listOf(player.uuid))
			others.forEach { it.connection.send(remove) }
		} else {
			vanished -= player.uuid
			// Tab list entry first: clients only spawn player entities they have a tab list entry for.
			val add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(player))
			others.forEach { it.connection.send(add) }
			refreshTracking(player)
		}
		val key = if (vanish) "multiplayer.player.left" else "multiplayer.player.joined"
		val fake = Component.translatable(key, player.displayName).withStyle(ChatFormatting.YELLOW)
		server.sendSystemMessage(fake)
		others.forEach { it.sendSystemMessage(fake) }
		player.level().updateSleepingPlayerList()
		server.invalidateStatus()
		store.saveAsync(SETTINGS_KEY, Settings(vanished.mapTo(sortedSetOf()) { it.toString() }))
	}

	/** Makes everyone near [player] start or stop seeing their entity, depending on [isHiddenFrom]. */
	private fun refreshTracking(player: ServerPlayer) {
		(player.level().chunkSource.chunkMap as EntityTracking).servercore_refreshTracking(player)
	}

	private fun message(vararg parts: net.kyori.adventure.text.Component) = Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp(*parts))
}
