package de.lenox.servercore.core.utils.components

import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.kyori.adventure.text.Component
import net.minecraft.server.MinecraftServer

/**
 * Converts to a vanilla component, for the few places that only take vanilla components (packets,
 * vanilla APIs). Messages don't need this: players and command sources take Adventure components directly
 * (`player.sendMessage(...)`, `source.sendSuccess(component, false)`).
 *
 * The result is a plain vanilla component (no Adventure wrapper), so it works in every packet and protocol
 * phase. Translatable parts are still translated by each client.
 */
fun Component.toNative(server: MinecraftServer): net.minecraft.network.chat.Component =
	MinecraftServerAudiences.of(server).nonWrappingSerializer().serialize(this)
