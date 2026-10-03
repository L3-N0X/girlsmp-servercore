package de.lenox.servercore.commands

import com.mojang.brigadier.CommandDispatcher
import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.onServerThread
import de.lenox.servercore.core.resources.ResourcePackModule
import de.lenox.servercore.core.storage.Storage
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
import net.minecraft.commands.Commands.hasPermission
import net.minecraft.commands.Commands.literal

/**
 * - `/resourcepack`: which pack version players get
 * - `/resourcepack reload`: re-read the config, look up the newest release and send it to everyone online if it changed
 */
object ResourcePackCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			literal("resourcepack")
				.requires(hasPermission(LEVEL_GAMEMASTERS))
				.executes { ctx ->
					ctx.source.sendSuccess(status(ResourcePackModule.current), false)
					1
				}
				.then(literal("reload").executes { ctx -> reload(ctx.source) }),
		)
	}

	private fun reload(source: CommandSourceStack): Int {
		val server = source.server
		Storage.scope.launch {
			try {
				val pack = ResourcePackModule.reload(server, pushToOnline = true)
				server.onServerThread { source.sendSuccess(status(pack), true) }
			} catch (e: Exception) {
				ServerCore.logger.error("Resource pack reload failed", e)
				server.execute { source.sendFailure(Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Reload failed, see the server log.", Theme.LIGHT_RED))) }
			}
		}
		return 1
	}

	private fun status(pack: ResourcePackModule.Pack?): Component {
		val prefix = Cmp("» ", Theme.SUBTEXT_3)
		if (pack == null) return Cmp(prefix, Cmp("No resource pack is sent (disabled or no release found, see the log).", Theme.LIGHT_RED))
		return Cmp(
			prefix,
			Cmp("Resource pack ", Theme.SUBTEXT_2),
			Cmp("v${pack.version}", Theme.LIGHT_PINK, Theme.LIGHT_PURPLE),
			Cmp(" · ", Theme.SUBTEXT_3),
			Cmp("download", Theme.LIGHT_AZURE)
				.hoverEvent(HoverEvent.showText(Cmp(pack.url, Theme.SUBTEXT_1)))
				.clickEvent(ClickEvent.openUrl(pack.url)),
		)
	}
}
