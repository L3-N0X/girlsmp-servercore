package de.lenox.servercore.mixin;

import de.lenox.servercore.core.scoreboard.ScoreboardModule;
import de.lenox.servercore.core.tablist.TabListModule;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps our packet sidebar and tab list scores visible when vanilla puts an objective into their slot. */
@Mixin(ServerScoreboard.class)
abstract class ServerScoreboardMixin {
	@Inject(method = "setDisplayObjective", at = @At("TAIL"))
	private void servercore$keepCustomDisplays(DisplaySlot slot, Objective objective, CallbackInfo ci) {
		if (slot == DisplaySlot.SIDEBAR) ScoreboardModule.onVanillaSidebarChanged();
		if (slot == DisplaySlot.LIST) TabListModule.onVanillaListChanged();
	}
}
