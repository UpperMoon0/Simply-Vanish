package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {
    @Shadow @Final private Entity entity;
    @Shadow public abstract void removePlayer(ServerPlayer player);
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void vanishguard$denyTracking(ServerPlayer viewer, CallbackInfo ci) {
        if (VanishGuard.hidden(entity)) { removePlayer(viewer); ci.cancel(); }
    }
}
