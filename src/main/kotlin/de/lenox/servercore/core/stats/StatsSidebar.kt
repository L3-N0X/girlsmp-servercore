package de.lenox.servercore.core.stats

import de.lenox.servercore.core.resources.Font
import de.lenox.servercore.core.scoreboard.ScoreboardProvider
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.server.level.ServerPlayer

/** Default sidebar: the player's own playtime and joins plus the online count. */
object StatsSidebar : ScoreboardProvider {
	private val title = Cmp("GirlSMP", Theme.LIGHT_PINK, Theme.LIGHT_PURPLE, Font.CAPS).decorate(TextDecoration.BOLD)
	private val lines = listOf(
		Component.empty(),
		label("Playtime"),
		label("Joins"),
		label("Online"),
		Component.empty(),
	)

	override fun getTitle(player: ServerPlayer) = title

	override fun getLines(player: ServerPlayer) = lines

	override fun getScores(player: ServerPlayer): List<Component?> {
		val stats = PlayerStatsModule.online(player.uuid)
		return listOf(
			null,
			value(stats?.let { formatDuration(it.playtimeSeconds, withSeconds = false) }, Theme.LIGHT_AQUA),
			value(stats?.joins?.toString(), Theme.LIGHT_MINT),
			value(player.level().server.playerList.playerCount.toString(), Theme.LIGHT_GOLD),
			null,
		)
	}

	/** Small-caps label. */
	private fun label(text: String) = Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp(text.lowercase(), Theme.SUBTEXT_1, Font.CAPS))

	/** Monospaced value; `...` while the player's stats are still loading. */
	private fun value(text: String?, color: TextColor) = if (text == null) Cmp("...", Theme.SUBTEXT_3, Font.MONO) else Cmp(text, color, Font.MONO)
}
