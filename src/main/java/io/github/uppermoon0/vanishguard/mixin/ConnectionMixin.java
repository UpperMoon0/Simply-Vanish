package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.PacketVisibility;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The outbound transport boundary also covers mods using Connection directly. */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow public abstract PacketListener getPacketListener();
    @Shadow public abstract void send(Packet<?> packet, PacketSendListener listener);
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void vanishguard$filterPlayPacket(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        // Login/status, client-side and authentication traffic are not modified.
        if (!(getPacketListener() instanceof ServerGamePacketListenerImpl play)) return;
        Packet<?> filtered = PacketVisibility.filter(packet, play.player);
        if (filtered != packet) {
            ci.cancel();
            if (filtered != null) send(filtered, listener);
            else if (listener != null) listener.onSuccess();
        }
    }
}
