package de.lenox.servercore.core.scoreboard

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer

/** Content of the sidebar, rendered per player every tick by [ScoreboardModule]. */
interface ScoreboardProvider {
	fun getTitle(player: ServerPlayer): Component

	/** Lines from top to bottom, at most [Sidebar.MAX_LINES]. */
	fun getLines(player: ServerPlayer): List<Component>

	/** Right-aligned text per line (same order as [getLines]); null or missing entries show nothing. */
	fun getScores(player: ServerPlayer): List<Component?> = emptyList()
}
