package de.lenox.servercore.core.stats

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.FixedFormat
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.criteria.ObjectiveCriteria

/**
 * Mirrors [PlayerStats] into vanilla scoreboard objectives, so they can be shown with
 * `/scoreboard objectives setdisplay sidebar playtime` or used in command blocks / datapacks.
 *
 * - `playtime`: minutes played (rendered as e.g. `12h 5m`)
 * - `joins`: number of joins
 * - `deaths`: number of deaths
 * - `totem_pops`: totems of undying used up
 *
 * The JSON files are the source of truth; the scoreboard is overwritten from them.
 */
object StatsScoreboard {
	const val PLAYTIME = "playtime"
	const val JOINS = "joins"
	const val DEATHS = "deaths"
	const val TOTEM_POPS = "totem_pops"

	fun update(server: MinecraftServer, stats: PlayerStats) {
		if (stats.name.isEmpty()) return
		val holder = ScoreHolder.forNameOnly(stats.name)
		val scoreboard = server.scoreboard

		scoreboard.getOrCreatePlayerScore(holder, objective(server, PLAYTIME, "Playtime")).apply {
			val minutes = (stats.playtimeSeconds / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
			set(minutes)
			numberFormatOverride(FixedFormat(Component.literal(formatDuration(minutes * 60L, withSeconds = false))))
		}
		scoreboard.getOrCreatePlayerScore(holder, objective(server, JOINS, "Joins")).set(stats.joins)
		scoreboard.getOrCreatePlayerScore(holder, objective(server, DEATHS, "Deaths")).set(stats.deaths)
		scoreboard.getOrCreatePlayerScore(holder, objective(server, TOTEM_POPS, "Totem Pops")).set(stats.totemPops)
	}

	/** Removes the scores of a name, e.g. after the player renamed themselves. */
	fun remove(server: MinecraftServer, name: String) {
		val holder = ScoreHolder.forNameOnly(name)
		val scoreboard = server.scoreboard
		listOf(PLAYTIME, JOINS, DEATHS, TOTEM_POPS).forEach { id ->
			scoreboard.getObjective(id)?.let { scoreboard.resetSinglePlayerScore(holder, it) }
		}
	}

	/** Looked up by name every time, so an objective removed via `/scoreboard` is simply recreated. */
	private fun objective(server: MinecraftServer, name: String, displayName: String): Objective =
		server.scoreboard.getObjective(name) ?: server.scoreboard.addObjective(
			name,
			ObjectiveCriteria.DUMMY,
			Component.literal(displayName),
			ObjectiveCriteria.RenderType.INTEGER,
			false,
			null,
		)
}

/** Formats seconds as `1d 3h 12m 5s`, leaving out leading zero units. */
fun formatDuration(totalSeconds: Long, withSeconds: Boolean = true): String {
	val days = totalSeconds / 86_400
	val hours = totalSeconds % 86_400 / 3_600
	val minutes = totalSeconds % 3_600 / 60
	val seconds = totalSeconds % 60
	val parts = buildList {
		if (days > 0) add("${days}d")
		if (days > 0 || hours > 0) add("${hours}h")
		add("${minutes}m")
		if (withSeconds) add("${seconds}s")
	}
	return parts.joinToString(" ")
}
