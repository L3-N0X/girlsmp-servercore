package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Function;

/** Vanish: `/list` leaves out vanished players for players who can't see them (count and names). */
@Mixin(ListPlayersCommand.class)
abstract class ListPlayersCommandMixin {
	@WrapOperation(method = "format", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
	private static List<ServerPlayer> servercore$hideVanished(
		PlayerList list,
		Operation<List<ServerPlayer>> original,
		CommandSourceStack source,
		Function<ServerPlayer, Component> formatter
	) {
		return original.call(list).stream().filter(player -> VanishModule.canSee(source, player)).toList();
	}
}
