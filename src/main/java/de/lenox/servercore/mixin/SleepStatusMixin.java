package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vanish: vanished players are skipped like spectators, so "1/2 players sleeping" doesn't count them. */
@Mixin(SleepStatus.class)
abstract class SleepStatusMixin {
	@WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
	private boolean servercore$skipVanished(ServerPlayer player, Operation<Boolean> original) {
		return original.call(player) || VanishModule.isVanished(player.getUUID());
	}
}
