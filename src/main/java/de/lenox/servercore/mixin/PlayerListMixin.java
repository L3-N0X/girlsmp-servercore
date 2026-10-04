package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vanish: no join message and no tab list removal for players nobody could see. */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {
	@WrapWithCondition(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private boolean servercore$hideJoinMessage(PlayerList list, Component message, boolean overlay, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
		return !VanishModule.isVanished(player.getUUID());
	}

	@WrapWithCondition(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	private boolean servercore$hideTabListRemoval(PlayerList list, Packet<?> packet, ServerPlayer player) {
		return !VanishModule.isVanished(player.getUUID());
	}
}
