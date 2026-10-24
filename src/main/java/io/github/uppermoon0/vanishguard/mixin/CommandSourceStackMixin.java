package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin {
    @Shadow
    public abstract Entity getEntity();

    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(
            method = "getOnlinePlayerNames()Ljava/util/Collection;",
            at = @At("HEAD"), cancellable = true,
            require = 1, expect = 1, allow = 1)
    private void vanishguard$playerFacingSuggestions(CallbackInfoReturnable<Collection<String>> callback) {
        if (this.getEntity() instanceof ServerPlayer) {
            callback.setReturnValue(VanishGuard.visible(this.getServer().getPlayerList().getPlayers())
                    .stream().map(player -> player.getGameProfile().getName()).toList());
        }
    }
}
