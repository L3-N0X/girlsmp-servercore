package de.lenox.servercore.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** The class is package-private, so its public methods can't be called directly. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityAccessor {
	@Invoker("updatePlayers")
	void servercore$updatePlayers(List<ServerPlayer> players);
}
