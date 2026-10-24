package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GamePacketListenerMixin {
    @Shadow public ServerPlayer player;
    @Shadow public abstract void send(Packet<?> packet, PacketSendListener listener);
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void simplyvanish$filterInfo(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        Packet<?> filtered = io.github.uppermoon0.simplyvanish.PacketVisibility.filter(packet, player);
        if (filtered != packet) {
            ci.cancel();
            if (filtered != null) send(filtered, listener);
            else if (listener != null) listener.onSuccess();
        }
    }
    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void simplyvanish$leave(PlayerList list, Component message, boolean overlay) {
        list.broadcastSystemMessage(message, viewer -> SimplyVanish.hidden(player) ? null : message, overlay);
    }
}
