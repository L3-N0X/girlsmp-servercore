package de.lenox.servercore.mixin;

import de.lenox.servercore.core.vanish.EntityTracking;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Lets the vanish module re-check who sees a player right away instead of when they next move. */
@Mixin(ChunkMap.class)
abstract class ChunkMapMixin implements EntityTracking {
	// Values are the package-private ChunkMap.TrackedEntity, reached through TrackedEntityAccessor.
	@Shadow @Final private Int2ObjectMap<?> entityMap;
	@Shadow @Final private ServerLevel level;

	@Override
	public void servercore_refreshTracking(Entity entity) {
		Object tracked = entityMap.get(entity.getId());
		if (tracked != null) ((TrackedEntityAccessor) tracked).servercore$updatePlayers(level.players());
	}
}
