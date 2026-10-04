package de.lenox.servercore.mixin;

import de.lenox.servercore.core.stats.PlayerStatsModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts totem pops for the player stats; Fabric API has no event for them. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
	private void servercore$countTotemPop(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof ServerPlayer player) PlayerStatsModule.onTotemPop(player);
	}
}
