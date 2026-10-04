package de.lenox.servercore.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.lenox.servercore.core.vanish.VanishModule;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanish: hides the entity from other players, hides death messages and keeps chat from vanished players working. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	@Shadow
	public ServerGamePacketListenerImpl connection;

	@Shadow
	private boolean acceptsChatMessages() {
		throw new AssertionError();
	}

	/** Decides whether [viewer] tracks (sees) this player's entity, in every game mode. */
	@Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
	private void servercore$hideVanished(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
		if (VanishModule.isHiddenFrom(self().getUUID(), viewer)) cir.setReturnValue(false);
	}

	// The vanished player still sees their own death message on the death screen.
	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private boolean servercore$hideDeathMessage(PlayerList list, Component message, boolean overlay) {
		return !VanishModule.isVanished(self().getUUID());
	}

	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
	private boolean servercore$hideTeamDeathMessage(PlayerList list, Player player, Component message) {
		return !VanishModule.isVanished(self().getUUID());
	}

	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
	private boolean servercore$hideOtherTeamsDeathMessage(PlayerList list, Player player, Component message) {
		return !VanishModule.isVanished(self().getUUID());
	}

	/**
	 * Clients disconnect when they get signed chat from a player that isn't in their tab list, so chat (and /msg,
	 * /me, /say, ...) of a vanished player is sent as unsigned "disguised" chat, which looks the same.
	 */
	@Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
	private void servercore$disguiseVanishedChat(OutgoingChatMessage message, boolean filtered, ChatType.Bound chatType, CallbackInfo ci) {
		if (!(message instanceof OutgoingChatMessage.Player(PlayerChatMessage chat))) return;
		if (!VanishModule.isHiddenFrom(chat.sender(), self())) return;
		ci.cancel();
		if (acceptsChatMessages() && !chat.filter(filtered).isFullyFiltered()) {
			connection.sendDisguisedChatMessage(chat.decoratedContent(), chatType);
		}
	}

	private ServerPlayer self() {
		return (ServerPlayer) (Object) this;
	}
}
