package de.lenox.servercore.mixin;

import de.lenox.servercore.core.vanish.VanishModule;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Last line of defense for vanish: every packet to a player passes here, so vanished players are removed from all
 * tab list updates (join, game mode, latency, ...) no matter where vanilla sends them from.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerCommonPacketListenerImplMixin {
	@Shadow
	public abstract void send(Packet<?> packet, @Nullable ChannelFutureListener listener);

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
	private void servercore$hideVanished(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
		if (!((Object) this instanceof ServerGamePacketListenerImpl game)) return;
		ServerPlayer viewer = game.player;
		if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
			List<ClientboundPlayerInfoUpdatePacket.Entry> visible = info.entries().stream()
				.filter(entry -> !VanishModule.isHiddenFrom(entry.profileId(), viewer))
				.toList();
			if (visible.size() == info.entries().size()) return;
			ci.cancel();
			if (visible.isEmpty()) return;
			ClientboundPlayerInfoUpdatePacket filtered = new ClientboundPlayerInfoUpdatePacket(info.actions(), List.of());
			((ClientboundPlayerInfoUpdatePacketAccessor) filtered).servercore$setEntries(visible);
			send(filtered, listener);
		} else if (packet instanceof ClientboundTakeItemEntityPacket take) {
			// Clients animate pickups by unknown entities as if they picked the item up themselves.
			if (viewer.level().getEntity(take.getPlayerId()) instanceof ServerPlayer collector
				&& VanishModule.isHiddenFrom(collector.getUUID(), viewer)) {
				ci.cancel();
			}
		}
	}
}
