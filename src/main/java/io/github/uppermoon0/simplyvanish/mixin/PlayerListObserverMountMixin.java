package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListObserverMountMixin {
    /**
     * Vanilla restores RootVehicle after selecting the login game mode. Let it
     * restore the vehicle, then dismount the observer without discarding the
     * vehicle or deleting saved NBT. Packet filtering also removes transient
     * hidden passenger references emitted before this final login hook.
     */
    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("TAIL"), require = 1, expect = 1, allow = 1)
    private void simplyvanish$dismountRestoredObserver(Connection connection, ServerPlayer player, CallbackInfo callback) {
        if (SimplyVanish.hidden(player) && SimplyVanish.observer()) {
            player.stopRiding();
        }
    }
}
