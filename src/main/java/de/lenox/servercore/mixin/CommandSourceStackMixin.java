package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

/** Vanish: vanished players aren't suggested as command arguments to players who can't see them. */
@Mixin(CommandSourceStack.class)
abstract class CommandSourceStackMixin {
	@ModifyReturnValue(method = "getOnlinePlayerNames", at = @At("RETURN"))
	private Collection<String> servercore$hideVanished(Collection<String> names) {
		CommandSourceStack source = (CommandSourceStack) (Object) this;
		List<String> hidden = source.getServer().getPlayerList().getPlayers().stream()
			.filter(player -> !VanishModule.canSee(source, player))
			.map(player -> player.getGameProfile().name())
			.toList();
		if (hidden.isEmpty()) return names;
		return names.stream().filter(name -> !hidden.contains(name)).toList();
	}
}
