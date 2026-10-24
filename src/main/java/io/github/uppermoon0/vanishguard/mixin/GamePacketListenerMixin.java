package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
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
    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void vanishguard$leave(PlayerList list, Component message, boolean overlay) {
        list.broadcastSystemMessage(message, viewer -> VanishGuard.hidden(player) ? null : message, overlay);
    }
}
