package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void simplyvanish$join(PlayerList list, Component message, boolean overlay, Connection connection, ServerPlayer player) {
        // The three-argument overload retains vanilla console logging.
        list.broadcastSystemMessage(message, viewer -> SimplyVanish.hidden(player) ? null : message, overlay);
    }
}
