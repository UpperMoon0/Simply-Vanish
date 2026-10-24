package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.PublicPlayerSnapshot;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QueryThreadGs4.class)
public abstract class QueryThreadGs4Mixin {
    @Unique
    private String[] vanishguard$ruleResponseNames = new String[0];

    @Redirect(
            method = "processPacket(Ljava/net/DatagramPacket;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerCount()I"),
            require = 1, expect = 1, allow = 1)
    private int vanishguard$publicBasicQueryCount(ServerInterface server) {
        return ((PublicPlayerSnapshot) server).vanishguard$getPublicPlayerCount();
    }

    @Redirect(
            method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerCount()I"),
            require = 1, expect = 1, allow = 1)
    private int vanishguard$publicFullQueryCount(ServerInterface server) {
        // Capture once so the response's count and names describe the same view.
        this.vanishguard$ruleResponseNames = ((PublicPlayerSnapshot) server).vanishguard$getPublicPlayerNames();
        return this.vanishguard$ruleResponseNames.length;
    }

    @Redirect(
            method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerNames()[Ljava/lang/String;"),
            require = 1, expect = 1, allow = 1)
    private String[] vanishguard$publicFullQueryNames(ServerInterface server) {
        return this.vanishguard$ruleResponseNames;
    }
}
