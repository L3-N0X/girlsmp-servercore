package de.lenox.servercore.mixin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** There is no public constructor taking entries, so filtered copies are built empty and filled through this. */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface ClientboundPlayerInfoUpdatePacketAccessor {
	@Mutable
	@Accessor("entries")
	void servercore$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
