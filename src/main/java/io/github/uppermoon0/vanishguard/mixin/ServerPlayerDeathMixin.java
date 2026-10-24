package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filter the three vanilla public death-message paths, not death mechanics. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
    @Redirect(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
            require = 1, expect = 1, allow = 1)
    private void vanishguard$privateDeathAnnouncement(PlayerList list, Component message, boolean overlay) {
        list.broadcastSystemMessage(message,
                viewer -> VanishGuard.hidden((ServerPlayer)(Object)this) ? null : message, overlay);
    }

    @Redirect(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"),
            require = 1, expect = 1, allow = 1)
    private void vanishguard$privateTeamDeathAnnouncement(PlayerList list, Player player, Component message) {
        if (VanishGuard.hidden(player)) {
            list.getServer().sendSystemMessage(message);
        } else {
            list.broadcastSystemToTeam(player, message);
        }
    }

    @Redirect(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"),
            require = 1, expect = 1, allow = 1)
    private void vanishguard$privateOtherTeamDeathAnnouncement(PlayerList list, Player player, Component message) {
        if (VanishGuard.hidden(player)) {
            list.getServer().sendSystemMessage(message);
        } else {
            list.broadcastSystemToAllExceptTeam(player, message);
        }
    }
}
