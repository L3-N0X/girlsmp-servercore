package de.lenox.servercore.core.scoreboard

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
 * The sidebar of one player, sent purely with packets (like FastBoard on Paper). It never touches the
 * server's scoreboard, so vanilla scoreboard rules (objective/score limits, teams, `/scoreboard`) don't apply
 * and every player can see something different.
 *
 * Uses the per-score display names and number formats that vanilla has since 1.20.3: each line is a
 * score whose display name is the line text, the red numbers are hidden ([BlankFormat]) and the optional
 * right-hand text is a [FixedFormat]. Only lines that changed are re-sent.
 *
 * Normally managed by [ScoreboardModule]; use this directly only for sidebars outside of a provider.
 */
class Sidebar(player: ServerPlayer) {
	// The connection stays the same across respawns, the ServerPlayer object doesn't.
	private val connection = player.connection
	private val server = player.level().server

	var title: Component = Component.empty()
		private set
	private var lines: List<Component> = emptyList()
	private var scores: List<Component?> = emptyList()
	var deleted = false
		private set

	init {
		send(ClientboundSetObjectivePacket(objective(), ClientboundSetObjectivePacket.METHOD_ADD))
		resendDisplay()
	}

	fun updateTitle(title: Component) {
		if (deleted || title == this.title) return
		this.title = title
		send(ClientboundSetObjectivePacket(objective(), ClientboundSetObjectivePacket.METHOD_CHANGE))
	}

	/**
	 * Sets the lines from top to bottom (the client shows at most [MAX_LINES]). [scores] is the optional
	 * right-aligned text per line; a missing or null entry shows nothing.
	 */
	fun updateLines(lines: List<Component>, scores: List<Component?> = emptyList()) {
		if (deleted) return
		val newLines = lines.take(MAX_LINES)
		val newScores = newLines.indices.map { scores.getOrNull(it) }
		newLines.indices.forEach { i ->
			if (newLines[i] != this.lines.getOrNull(i) || newScores[i] != this.scores.getOrNull(i)) {
				send(
					ClientboundSetScorePacket(
						owner(i),
						OBJECTIVE_NAME,
						// The client sorts by score descending: 0, -1, -2, ... keeps the lines in order.
						-i,
						Optional.of(newLines[i].toNative(server)),
						Optional.ofNullable(newScores[i]?.let { FixedFormat(it.toNative(server)) }),
					),
				)
			}
		}
		(newLines.size until this.lines.size).forEach { i -> send(ClientboundResetScorePacket(owner(i), OBJECTIVE_NAME)) }
		this.lines = newLines
		this.scores = newScores
	}

	/** Removes the sidebar from the client. */
	fun delete() {
		if (deleted) return
		deleted = true
		send(ClientboundSetObjectivePacket(objective(), ClientboundSetObjectivePacket.METHOD_REMOVE))
	}

	/** Puts the sidebar back into the sidebar slot, e.g. after vanilla displayed another objective there. */
	fun resendDisplay() {
		if (!deleted) send(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective()))
	}

	/** A throwaway objective: the packets only read its name, title and format from it. */
	private fun objective() = Objective(
		PACKET_SCOREBOARD,
		OBJECTIVE_NAME,
		ObjectiveCriteria.DUMMY,
		title.toNative(server),
		ObjectiveCriteria.RenderType.INTEGER,
		false,
		BlankFormat.INSTANCE,
	)

	private fun send(packet: Packet<*>) = connection.send(packet)

	companion object {
		const val MAX_LINES = 15

		/** Client-only objective; the name only has to not clash with real objectives. */
		private const val OBJECTIVE_NAME = "servercore.sidebar"

		/** Never registered anywhere, only needed to construct [Objective]s. */
		private val PACKET_SCOREBOARD = Scoreboard()

		/** Score holder names are never shown (every line has a display name), they only have to be unique. */
		private fun owner(line: Int) = "servercore.line.$line"
	}
}
