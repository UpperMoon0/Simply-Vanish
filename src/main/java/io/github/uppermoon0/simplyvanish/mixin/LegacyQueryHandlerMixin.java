package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.PublicPlayerSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.LegacyQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LegacyQueryHandler.class)
public abstract class LegacyQueryHandlerMixin {
    // All three pre-1.7 ping formats report a count independently of ServerStatus.
    @Redirect(
            method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
            remap = false, // Netty's override is not obfuscated; its Minecraft call is.
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getPlayerCount()I", remap = true),
            require = 3, expect = 3, allow = 3)
    private int simplyvanish$publicLegacyCount(MinecraftServer server) {
        return ((PublicPlayerSnapshot) server).simplyvanish$getPublicPlayerCount();
    }
}
