package de.lenox.servercore.core.vanish

import net.minecraft.world.entity.Entity

/** Implemented on `ChunkMap` by a mixin: cast `level.chunkSource.chunkMap` to this. */
interface EntityTracking {
	/** Re-checks for every player in the level whether they should see [entity], e.g. after it (un)vanished. */
	@Suppress("FunctionName")
	fun servercore_refreshTracking(entity: Entity)
}
