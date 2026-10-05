package de.lenox.servercore.commands

import com.mojang.brigadier.CommandDispatcher
import de.lenox.servercore.core.invsee.InvseeMenu
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.toNative
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.hasPermission
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.SimpleMenuProvider

/**
 * - `/invsee <player>`: opens the full inventory of an online player (hotbar, main inventory, armor, offhand, ...) as
 *   a chest GUI you can edit (operators only), see [InvseeMenu].
 */
object InvseeCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			literal("invsee")
				.requires(hasPermission(LEVEL_GAMEMASTERS))
				.then(
					argument("player", EntityArgument.player()).executes { ctx ->
						val viewer = ctx.source.playerOrException
						val target = EntityArgument.getPlayer(ctx, "player")
						val title = Cmp("${target.plainTextName}'s inventory").toNative(ctx.source.server)
						viewer.openMenu(SimpleMenuProvider({ id, inventory, _ -> InvseeMenu(id, inventory, target) }, title))
						1
					},
				),
		)
	}
}
