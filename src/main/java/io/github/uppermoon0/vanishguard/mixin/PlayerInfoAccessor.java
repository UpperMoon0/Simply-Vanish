package io.github.uppermoon0.vanishguard.mixin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoAccessor {
    @Accessor("entries") @Mutable void vanishguard$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
