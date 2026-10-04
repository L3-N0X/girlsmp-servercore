package de.lenox.servercore.mixin;

import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanish: mobs ignore vanished players, so nobody sees mobs chase something invisible. */
@Mixin(Player.class)
abstract class PlayerMixin {
	@Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
	private void servercore$ignoreVanished(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayer player && VanishModule.isVanished(player.getUUID())) cir.setReturnValue(false);
	}
}
