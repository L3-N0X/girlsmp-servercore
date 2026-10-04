package de.lenox.servercore.core

import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.storage.Storage
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/**
 * Wires Fabric API events once and fans them out to all registered [ServerModule]s.
 */
object ModuleManager {
	private val modules = mutableListOf<ServerModule>()

	/** The running server, or null before SERVER_STARTED / after SERVER_STOPPED. */
	var server: MinecraftServer? = null
		private set

	fun register(vararg module: ServerModule) {
		modules += module
	}

	fun init() {
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			this.server = server
			modules.forEach { it.onServerStarted(server) }
			ServerCore.logger.info("Enabled modules: {}", modules.joinToString { it.id })
		}
		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			modules.forEach { it.onServerStopping(server) }
		}
		ServerLifecycleEvents.SERVER_STOPPED.register {
			// After all players were disconnected (and their data queued for saving), before the JVM exits.
			Storage.awaitPendingWrites()
			this.server = null
		}
		ServerTickEvents.END_SERVER_TICK.register { server ->
			modules.forEach { it.onTick(server) }
		}
		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			modules.forEach { it.onPlayerJoin(handler.player) }
		}
		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
			modules.forEach { it.onPlayerLeave(handler.player) }
		}
		ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
			if (entity is ServerPlayer) modules.forEach { it.onPlayerDeath(entity, source) }
		}
	}
}
