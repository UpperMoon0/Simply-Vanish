package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique private static final String SAVED = "VanishGuardOriginalMode";
    @Unique private static final String PREVIOUS = "VanishGuardPreviousMode";
    @Inject(method = "loadGameTypes", at = @At("TAIL"))
    private void vanishguard$loadMode(CompoundTag data, CallbackInfo ci) { vanishguard$observerMode(true); }
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void vanishguard$restoreMode(ServerPlayer old, boolean alive, CallbackInfo ci) { vanishguard$observerMode(false); }
    @Unique private void vanishguard$observerMode(boolean login) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        CompoundTag data = player.getPersistentData();
        CompoundTag persistent = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (VanishGuard.hidden(player) && VanishGuard.observer()) {
            if (login || !persistent.contains(SAVED, 3)) {
                persistent.putInt(SAVED, player.gameMode.getGameModeForPlayer().getId());
                GameType previous = player.gameMode.getPreviousGameModeForPlayer();
                persistent.putInt(PREVIOUS, previous == null ? -1 : previous.getId());
            }
            data.put(Player.PERSISTED_NBT_TAG, persistent);
            ((GameModeAccessor)player.gameMode).vanishguard$setMode(GameType.SPECTATOR, player.gameMode.getPreviousGameModeForPlayer());
        } else if (persistent.contains(SAVED, 3)) {
            // Saved vanilla fields already contain the original mode. On login honor
            // vanilla force-gamemode and changes made while this mod was uninstalled.
            if (!login) {
                GameType original = GameType.byId(persistent.getInt(SAVED));
                ((GameModeAccessor)player.gameMode).vanishguard$setMode(original, persistent.getInt(PREVIOUS) < 0 ? null : GameType.byId(persistent.getInt(PREVIOUS)));
            }
            persistent.remove(SAVED);
            persistent.remove(PREVIOUS);
            data.put(Player.PERSISTED_NBT_TAG, persistent);
        }
    }
    // Persist the real mode, not the temporary observer state. Removing the mod remains safe.
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vanishguard$saveOriginalMode(CompoundTag output, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persistent.contains(SAVED, 3)) {
            output.putInt("playerGameType", persistent.getInt(SAVED));
            int previous = persistent.getInt(PREVIOUS);
            if (previous < 0) output.remove("previousPlayerGameType");
            else output.putInt("previousPlayerGameType", previous);
        }
    }
    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void vanishguard$keepObserver(GameType mode, CallbackInfoReturnable<Boolean> cir) {
        if (VanishGuard.hidden((ServerPlayer)(Object)this) && VanishGuard.observer() && mode != GameType.SPECTATOR) cir.setReturnValue(false);
    }
}
