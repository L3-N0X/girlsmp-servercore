package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Vanish: vanished players are neither counted nor listed in the multiplayer server list. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@WrapOperation(method = "buildPlayerStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
	private List<ServerPlayer> servercore$hideVanished(PlayerList list, Operation<List<ServerPlayer>> original) {
		return original.call(list).stream().filter(player -> !VanishModule.isVanished(player.getUUID())).toList();
	}
}
