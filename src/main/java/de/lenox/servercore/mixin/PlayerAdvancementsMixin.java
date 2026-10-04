package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Consumer;

/** Vanish: no advancement announcements. The advancement itself (toast, rewards) is still granted. */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	// `advancement.display().ifPresent(display -> announce in chat)` is the only thing this call does.
	@WrapWithCondition(method = "award", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
	private boolean servercore$hideAnnouncement(Optional<?> display, Consumer<?> announce) {
		return !VanishModule.isVanished(player.getUUID());
	}
}
