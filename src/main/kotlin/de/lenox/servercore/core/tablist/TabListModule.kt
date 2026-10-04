package de.lenox.servercore.core.tablist

import de.lenox.servercore.core.ServerModule
import de.lenox.servercore.core.utils.components.toNative
import de.lenox.servercore.core.vanish.VanishModule
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * Shows the content of [provider] in every player's tab list, re-rendered every tick:
 *
 * - **Header and footer** are sent with [ClientboundTabListPacket] only when they changed. Vanilla never sends
 *   this packet itself, so nothing overwrites it.
 * - **Scores** (the text right of each name) are a packet-only list objective per player, see [TabListScores].
 *
 * Set [provider] to null to clear everything for everyone.
 *
 * TODO: custom player names with ClientboundPlayerInfoUpdatePacket.
 */
object TabListModule : ServerModule {
	override val id = "tablist"

	private data class Sent(val header: Component, val footer: Component)

	private val sent = HashMap<UUID, Sent>()
	private val scores = HashMap<UUID, TabListScores>()

	var provider: TabListProvider? = null

	override fun onTick(server: MinecraftServer) {
		updateHeaderAndFooter(server)
		updateScores(server)
	}

	override fun onPlayerLeave(player: ServerPlayer) {
		sent.remove(player.uuid)
		scores.remove(player.uuid)
	}

	override fun onServerStopping(server: MinecraftServer) {
		sent.clear()
		scores.clear()
	}

	/**
	 * Called (by a mixin) whenever vanilla changes the objective in the list slot, e.g. through
	 * `/scoreboard objectives setdisplay list`, which would otherwise replace our scores on every client.
	 */
	@JvmStatic
	fun onVanillaListChanged() {
		scores.values.forEach(TabListScores::resendDisplay)
	}

	private fun updateHeaderAndFooter(server: MinecraftServer) {
		val provider = provider
		server.playerList.players.forEach { player ->
			val previous = sent[player.uuid]
			// Nothing to clear for players that never got anything.
			if (provider == null && previous == null) return@forEach
			val next = if (provider == null) EMPTY else Sent(provider.getHeader(player), provider.getFooter(player))
			if (next == previous) return@forEach
			player.connection.send(ClientboundTabListPacket(next.header.toNative(server), next.footer.toNative(server)))
			if (provider == null) sent.remove(player.uuid) else sent[player.uuid] = next
		}
	}

	private fun updateScores(server: MinecraftServer) {
		val players = server.playerList.players
		// Rendered once per player and shared by all viewers.
		val next = provider?.let { provider -> players.associate { it.scoreboardName to provider.getScore(it) } }
		if (next == null || next.values.all { it == null }) {
			// Leaves the list slot to vanilla while there is nothing to show.
			scores.values.forEach(TabListScores::delete)
			scores.clear()
			return
		}
		val vanished = players.filter(VanishModule::isVanished)
		players.forEach { viewer ->
			val visible = if (vanished.isEmpty()) next else next - vanished.filter { it !== viewer }.map { it.scoreboardName }.toSet()
			scores.getOrPut(viewer.uuid) { TabListScores(viewer) }.update(visible)
		}
	}

	private val EMPTY = Sent(Component.empty(), Component.empty())
}
