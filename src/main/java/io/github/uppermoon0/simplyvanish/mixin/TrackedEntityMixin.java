package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
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
    private void simplyvanish$denyTracking(ServerPlayer viewer, CallbackInfo ci) {
        if (SimplyVanish.hidden(entity)) { removePlayer(viewer); ci.cancel(); }
    }
}
