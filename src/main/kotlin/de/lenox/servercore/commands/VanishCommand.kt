package de.lenox.servercore.commands

import com.mojang.brigadier.CommandDispatcher
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import de.lenox.servercore.core.vanish.VanishModule
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
import net.minecraft.commands.Commands.hasPermission
import net.minecraft.commands.Commands.literal

/**
 * - `/vanish`: become invisible to everyone else, or visible again (operators only). Stays on across rejoins and
 *   restarts, see [VanishModule].
 */
object VanishCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			literal("vanish")
				.requires(hasPermission(LEVEL_GAMEMASTERS))
				.executes { ctx ->
					val player = ctx.source.playerOrException
					val vanish = !VanishModule.isVanished(player)
					VanishModule.setVanished(player, vanish)
					val prefix = Cmp("» ", Theme.SUBTEXT_3)
					ctx.source.sendSuccess(
						if (vanish) {
							Cmp(prefix, Cmp("You are vanished. ", Theme.LIGHT_PURPLE), Cmp("Others saw you leave the game.", Theme.SUBTEXT_1))
						} else {
							Cmp(prefix, Cmp("You are visible again. ", Theme.LIGHT_GREEN), Cmp("Others saw you join the game.", Theme.SUBTEXT_1))
						},
						false,
					)
					1
				},
		)
	}
}
