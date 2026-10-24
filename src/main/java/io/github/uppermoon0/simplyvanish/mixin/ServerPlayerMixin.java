package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique private static final String SAVED = "SimplyVanishOriginalMode";
    @Unique private static final String PREVIOUS = "SimplyVanishPreviousMode";
    @Inject(method = "loadGameTypes", at = @At("TAIL"))
    private void simplyvanish$loadMode(CompoundTag data, CallbackInfo ci) { simplyvanish$observerMode(true); }
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void simplyvanish$restoreMode(ServerPlayer old, boolean alive, CallbackInfo ci) { simplyvanish$observerMode(false); }
    @Unique private void simplyvanish$observerMode(boolean login) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        CompoundTag data = player.getPersistentData();
        CompoundTag persistent = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (SimplyVanish.hidden(player) && SimplyVanish.observer()) {
            if (login || !persistent.contains(SAVED, 3)) {
                persistent.putInt(SAVED, player.gameMode.getGameModeForPlayer().getId());
                GameType previous = player.gameMode.getPreviousGameModeForPlayer();
                persistent.putInt(PREVIOUS, previous == null ? -1 : previous.getId());
            }
            data.put(Player.PERSISTED_NBT_TAG, persistent);
            ((GameModeAccessor)player.gameMode).simplyvanish$setMode(GameType.SPECTATOR, player.gameMode.getPreviousGameModeForPlayer());
        } else if (persistent.contains(SAVED, 3)) {
            // Saved vanilla fields already contain the original mode. On login honor
            // vanilla force-gamemode and changes made while this mod was uninstalled.
            if (!login) {
                GameType original = GameType.byId(persistent.getInt(SAVED));
                ((GameModeAccessor)player.gameMode).simplyvanish$setMode(original, persistent.getInt(PREVIOUS) < 0 ? null : GameType.byId(persistent.getInt(PREVIOUS)));
            }
            persistent.remove(SAVED);
            persistent.remove(PREVIOUS);
            data.put(Player.PERSISTED_NBT_TAG, persistent);
        }
    }
    // Persist the real mode, not the temporary observer state. Removing the mod remains safe.
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplyvanish$saveOriginalMode(CompoundTag output, CallbackInfo ci) {
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
    private void simplyvanish$keepObserver(GameType mode, CallbackInfoReturnable<Boolean> cir) {
        if (SimplyVanish.hidden((ServerPlayer)(Object)this) && SimplyVanish.observer() && mode != GameType.SPECTATOR) cir.setReturnValue(false);
    }
}
