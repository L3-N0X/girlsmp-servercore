package de.lenox.servercore.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.onServerThread
import de.lenox.servercore.core.resources.Font
import de.lenox.servercore.core.scoreboard.ScoreboardModule
import de.lenox.servercore.core.stats.PlayerStats
import de.lenox.servercore.core.stats.PlayerStatsModule
import de.lenox.servercore.core.stats.formatDuration
import de.lenox.servercore.core.storage.Storage
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import de.lenox.servercore.core.vanish.VanishModule
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.hasPermission
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.SharedSuggestionProvider
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * - `/stats [player]`: playtime, joins, deaths, totem pops, first join and last seen of yourself or any player
 *   (also offline)
 * - `/stats top <playtime|joins|deaths|totems>`: top 10 players
 * - `/stats toggle`: hide or show your sidebar
 * - `/stats reload`: re-read the JSON files after editing them by hand (gamemasters only)
 */
object StatsCommand {
	private const val TOP_SIZE = 10
	private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

	/** Gold, silver and bronze for the first three places, like the Eventrox leaderboards. */
	private val podiumColors = listOf(Theme.GOLD, Theme.SILVER, Theme.BRONZE)

	/** Widest rank (`#10`), so the names after the monospaced ranks line up. */
	private val rankWidth = "#$TOP_SIZE".length

	private enum class Category(
		val label: String,
		val title: String,
		val color: TextColor,
		val value: (PlayerStats) -> Long,
		val format: (Long) -> String,
	) {
		PLAYTIME("playtime", "Playtime", Theme.LIGHT_AQUA, PlayerStats::playtimeSeconds, { formatDuration(it, withSeconds = false) }),
		JOINS("joins", "Joins", Theme.LIGHT_MINT, { it.joins.toLong() }, Long::toString),
		DEATHS("deaths", "Deaths", Theme.LIGHT_RED, { it.deaths.toLong() }, Long::toString),
		TOTEMS("totems", "Totem Pops", Theme.LIGHT_YELLOW, { it.totemPops.toLong() }, Long::toString),
	}

	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			literal("stats")
				.executes { ctx ->
					val player = ctx.source.playerOrException
					val stats = PlayerStatsModule.online(player.uuid)
					if (stats == null) {
						ctx.source.sendFailure(error("Your stats are still loading, try again in a moment."))
						return@executes 0
					}
					ctx.source.sendSuccess(statsMessage(stats, online = true), false)
					1
				}
				.then(
					literal("top").apply {
						Category.entries.forEach { category ->
							then(literal(category.label).executes { ctx -> top(ctx.source, category) })
						}
					},
				)
				.then(
					literal("toggle").executes { ctx ->
						val player = ctx.source.playerOrException
						val hide = !ScoreboardModule.isHidden(player)
						ScoreboardModule.setHidden(player, hide)
						val again = Cmp("/stats toggle", Theme.LIGHT_PURPLE)
							.hoverEvent(HoverEvent.showText(Cmp("Click to run", Theme.SUBTEXT_2)))
							.clickEvent(ClickEvent.runCommand("/stats toggle"))
						ctx.source.sendSuccess(
							if (hide) {
								Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Sidebar hidden. ", Theme.SUBTEXT_1), again, Cmp(" shows it again.", Theme.SUBTEXT_2))
							} else {
								Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Sidebar shown.", Theme.SUBTEXT_1))
							},
							false,
						)
						1
					},
				)
				.then(
					literal("reload")
						.requires(hasPermission(LEVEL_GAMEMASTERS))
						.executes { ctx -> reload(ctx.source) },
				)
				.then(
					argument("player", StringArgumentType.word())
						.suggests { _, builder -> SharedSuggestionProvider.suggest(PlayerStatsModule.knownNames, builder) }
						.executes { ctx -> show(ctx.source, StringArgumentType.getString(ctx, "player")) },
				),
		)
	}

	private fun show(source: CommandSourceStack, name: String): Int {
		val server = source.server
		async(source) {
			val found = PlayerStatsModule.find(server, name)
			server.onServerThread {
				if (found == null) {
					source.sendFailure(error(Cmp("No stats found for ", Theme.LIGHT_RED), Cmp(name, Theme.WHITE), Cmp(".", Theme.LIGHT_RED)))
				} else {
					val (uuid, stats) = found
					source.sendSuccess(statsMessage(stats, online = isOnline(source, uuid)), false)
				}
			}
		}
		return 1
	}

	private fun top(source: CommandSourceStack, category: Category): Int {
		val server = source.server
		async(source) {
			val ranking = PlayerStatsModule.all(server).values
				.filter { it.name.isNotEmpty() }
				.sortedByDescending(category.value)
				.take(TOP_SIZE)
			server.onServerThread {
				val lines = ranking.mapIndexed { index, stats ->
					val first = index == 0
					Cmp(
						Cmp("\n "),
						Cmp("#${index + 1}".padEnd(rankWidth), podiumColors.getOrElse(index) { Theme.SUBTEXT_3 }, Font.MONO),
						Cmp(" "),
						playerName(stats.name, if (first) Cmp(stats.name, Theme.GOLD, Theme.LIGHT_GOLD) else Cmp(stats.name, Theme.SUBTEXT_1)),
						Cmp(" » ", Theme.SUBTEXT_3),
						Cmp(category.format(category.value(stats)), if (first) Theme.LIGHT_GOLD else category.color),
					)
				}.ifEmpty { listOf(Cmp("\n No data yet.", Theme.SUBTEXT_2)) }
				source.sendSuccess(Cmp(header("Top $TOP_SIZE", category.title), Cmp(lines)), false)
			}
		}
		return 1
	}

	private fun reload(source: CommandSourceStack): Int {
		val server = source.server
		async(source) {
			PlayerStatsModule.reload(server)
			server.onServerThread {
				source.sendSuccess(Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Reloaded player stats from disk.", Theme.LIGHT_GREEN)), true)
			}
		}
		return 1
	}

	private fun statsMessage(stats: PlayerStats, online: Boolean): Component =
		Cmp(
			header("Stats", stats.name),
			line("Playtime", Cmp(formatDuration(stats.playtimeSeconds), Theme.LIGHT_AQUA)),
			line("Joins", Cmp(stats.joins.toString(), Theme.LIGHT_MINT)),
			line("Deaths", Cmp(stats.deaths.toString(), Theme.LIGHT_RED)),
			line("Totem pops", Cmp(stats.totemPops.toString(), Theme.LIGHT_YELLOW)),
			line("First join", date(stats.firstJoin)),
			line("Last seen", if (online) Cmp("● online now", Theme.BRIGHT_GREEN) else date(stats.lastSeen)),
		)

	/** `Stats · Notch` with a gradient pixel-caps title. */
	private fun header(title: String, subject: String): Component =
		Cmp(Cmp(title, Theme.LIGHT_PURPLE, Theme.LIGHT_BLUE, Font.CAPS), Cmp(" · ", Theme.SUBTEXT_3), Cmp(subject, Theme.WHITE))

	/** A small-caps label followed by its value. */
	private fun line(label: String, value: Component): Component =
		Cmp(Cmp("\n » ", Theme.SUBTEXT_3), Cmp("${label.lowercase()} ", Theme.SUBTEXT_2, Font.CAPS), value)

	/** A date that shows how long ago it was on hover. */
	private fun date(instant: Instant?): Component {
		if (instant == null) return Cmp("unknown", Theme.SUBTEXT_3)
		val ago = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
		return Cmp(dateFormat.format(instant), Theme.LIGHT_PURPLE)
			.hoverEvent(HoverEvent.showText(Cmp("${formatDuration(ago, withSeconds = false)} ago", Theme.SUBTEXT_1)))
	}

	/** A player name that opens their stats when clicked. */
	private fun playerName(name: String, text: Component = Cmp(name, Theme.SUBTEXT_1)): Component =
		text
			.hoverEvent(HoverEvent.showText(Cmp(Cmp("Show stats of ", Theme.SUBTEXT_2), Cmp(name, Theme.WHITE))))
			.clickEvent(ClickEvent.runCommand("/stats $name"))

	private fun error(vararg message: Component): Component = Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp(*message))

	private fun error(message: String): Component = error(Cmp(message, Theme.LIGHT_RED))

	/** Vanished players count as offline for everyone who can't see them. */
	private fun isOnline(source: CommandSourceStack, uuid: UUID) =
		source.server.playerList.getPlayer(uuid)?.let { VanishModule.canSee(source, it) } ?: false

	/** Runs [block] off the server thread and reports failures to the command source. */
	private fun async(source: CommandSourceStack, block: suspend () -> Unit) {
		Storage.scope.launch {
			try {
				block()
			} catch (e: Exception) {
				ServerCore.logger.error("Stats command failed", e)
				source.server.execute { source.sendFailure(error("Something went wrong, see the server log.")) }
			}
		}
	}
}
