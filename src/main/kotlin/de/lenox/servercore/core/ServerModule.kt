package de.lenox.servercore.core

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource

/**
 * A self-contained piece of server functionality (tab list, bossbar, ...).
 * All hooks are called on the server thread by [ModuleManager].
 */
interface ServerModule {
	val id: String

	fun onServerStarted(server: MinecraftServer) {}

	fun onServerStopping(server: MinecraftServer) {}

	fun onTick(server: MinecraftServer) {}

	fun onPlayerJoin(player: ServerPlayer) {}

	fun onPlayerLeave(player: ServerPlayer) {}

	/** After a player died (not when a totem saved them). */
	fun onPlayerDeath(player: ServerPlayer, source: DamageSource) {}
}
