package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Vanish: commands of players who can't see vanished players (non-operators) don't find them, e.g. `/msg`. */
@Mixin(EntitySelector.class)
abstract class EntitySelectorMixin {
	@ModifyReturnValue(method = "findEntities", at = @At("RETURN"))
	private List<? extends Entity> servercore$hideVanishedEntities(List<? extends Entity> entities, CommandSourceStack source) {
		return servercore$visible(entities, source);
	}

	@ModifyReturnValue(method = "findPlayers", at = @At("RETURN"))
	private List<ServerPlayer> servercore$hideVanishedPlayers(List<ServerPlayer> players, CommandSourceStack source) {
		return servercore$visible(players, source);
	}

	private static <T extends Entity> List<T> servercore$visible(List<T> entities, CommandSourceStack source) {
		if (entities.stream().allMatch(entity -> servercore$canSee(source, entity))) return entities;
		return entities.stream().filter(entity -> servercore$canSee(source, entity)).toList();
	}

	private static boolean servercore$canSee(CommandSourceStack source, Entity entity) {
		return !(entity instanceof ServerPlayer player) || VanishModule.canSee(source, player);
	}
}
