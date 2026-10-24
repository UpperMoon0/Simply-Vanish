package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep rewards, progress, Forge events, self toasts, and console auditing intact. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Redirect(
            method = "award(Lnet/minecraft/advancements/Advancement;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
            require = 1, expect = 1, allow = 1)
    private void simplyvanish$privateAdvancementAnnouncement(PlayerList list, Component message, boolean overlay) {
        list.broadcastSystemMessage(message,
                viewer -> SimplyVanish.hidden(this.player) ? null : message, overlay);
    }
}
