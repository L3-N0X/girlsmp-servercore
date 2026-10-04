package de.lenox.servercore.core.tablist

import de.lenox.servercore.core.utils.components.toNative
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.chat.numbers.FixedFormat
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import java.util.Optional

/**
 * The text right of the names in one player's tab list, sent purely with packets like
 * [de.lenox.servercore.core.scoreboard.Sidebar]: a client-only objective in the [DisplaySlot.LIST] slot whose
 * scores have a [FixedFormat] each. Players without a score show nothing ([BlankFormat]). Only changed scores
 * are re-sent.
 *
 * Normally managed by [TabListModule].
 */
class TabListScores(player: ServerPlayer) {
	// The connection stays the same across respawns, the ServerPlayer object doesn't.
	private val connection = player.connection
	private val server = player.level().server

	/** Score holder (player name) -> text the client currently shows. */
	private val sent = HashMap<String, Component>()
	var deleted = false
		private set

	init {
		send(ClientboundSetObjectivePacket(OBJECTIVE, ClientboundSetObjectivePacket.METHOD_ADD))
		resendDisplay()
	}

	/** Sets the text per player name; names that are missing or null show nothing. */
	fun update(scores: Map<String, Component?>) {
		if (deleted) return
		scores.forEach { (name, score) ->
			if (score == null || sent[name] == score) return@forEach
			send(
				ClientboundSetScorePacket(
					name,
					OBJECTIVE_NAME,
					0,
					Optional.empty(),
					Optional.of(FixedFormat(score.toNative(server))),
				),
			)
			sent[name] = score
		}
		sent.keys.filter { scores[it] == null }.forEach { name ->
			send(ClientboundResetScorePacket(name, OBJECTIVE_NAME))
			sent.remove(name)
		}
	}

	/** Removes the scores from the client and shows the server's own list objective again, if there is one. */
	fun delete() {
		if (deleted) return
		deleted = true
		send(ClientboundSetObjectivePacket(OBJECTIVE, ClientboundSetObjectivePacket.METHOD_REMOVE))
		// The client still knows the vanilla objective (the server sends every displayed one), only the slot is empty.
		server.scoreboard.getDisplayObjective(DisplaySlot.LIST)?.let {
			send(ClientboundSetDisplayObjectivePacket(DisplaySlot.LIST, it))
		}
	}

	/** Puts the scores back into the list slot, e.g. after vanilla displayed another objective there. */
	fun resendDisplay() {
		if (!deleted) send(ClientboundSetDisplayObjectivePacket(DisplaySlot.LIST, OBJECTIVE))
	}

	private fun send(packet: Packet<*>) = connection.send(packet)

	private companion object {
		/** Client-only objective; the name only has to not clash with real objectives. */
		const val OBJECTIVE_NAME = "servercore.tablist"

		/** A throwaway objective: the packets only read its name, title and format from it. */
		val OBJECTIVE = Objective(
			Scoreboard(),
			OBJECTIVE_NAME,
			ObjectiveCriteria.DUMMY,
			net.minecraft.network.chat.Component.empty(),
			ObjectiveCriteria.RenderType.INTEGER,
			false,
			BlankFormat.INSTANCE,
		)
	}
}
