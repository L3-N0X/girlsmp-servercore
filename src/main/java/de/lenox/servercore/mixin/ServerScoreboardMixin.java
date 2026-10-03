package de.lenox.servercore.mixin;

import de.lenox.servercore.core.scoreboard.ScoreboardModule;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps our packet sidebar visible when vanilla puts an objective into the sidebar slot. */
@Mixin(ServerScoreboard.class)
abstract class ServerScoreboardMixin {
	@Inject(method = "setDisplayObjective", at = @At("TAIL"))
	private void servercore$keepCustomSidebar(DisplaySlot slot, Objective objective, CallbackInfo ci) {
		if (slot == DisplaySlot.SIDEBAR) ScoreboardModule.onVanillaSidebarChanged();
	}
}
