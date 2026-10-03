package de.lenox.servercore.core

import kotlinx.coroutines.future.await
import net.minecraft.server.MinecraftServer
import java.util.concurrent.CompletableFuture

/**
 * Runs [block] on the server thread and suspends until it's done. Use this to get back to the
 * server thread after async work before touching players, the world or the scoreboard.
 */
suspend fun <T> MinecraftServer.onServerThread(block: () -> T): T =
	if (isSameThread) block() else CompletableFuture.supplyAsync(block, this).await()
