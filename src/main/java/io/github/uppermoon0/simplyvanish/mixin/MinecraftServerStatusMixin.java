package io.github.uppermoon0.simplyvanish.mixin;

import io.github.uppermoon0.simplyvanish.SimplyVanish;
import io.github.uppermoon0.simplyvanish.PublicPlayerSnapshot;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerStatusMixin implements PublicPlayerSnapshot {
    @Unique
    private volatile String[] simplyvanish$publicPlayerNames = new String[0];

    /**
     * Filter only the status builder, not PlayerList.getPlayers or global slot
     * counts. The vanilla builder retains its random sample, privacy settings,
     * and Forge's status JSON/cache and forgeData handling.
     */
    @Redirect(
            method = "buildPlayerStatus()Lnet/minecraft/network/protocol/status/ServerStatus$Players;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"),
            require = 1, expect = 1, allow = 1)
    private List<ServerPlayer> simplyvanish$publicStatusPlayers(PlayerList playerList) {
        List<ServerPlayer> visible = SimplyVanish.visible(playerList.getPlayers());
        this.simplyvanish$publicPlayerNames = visible.stream()
                .map(player -> player.getGameProfile().getName())
                .toArray(String[]::new);
        return visible;
    }

    @Override
    @Unique
    public int simplyvanish$getPublicPlayerCount() {
        return this.simplyvanish$publicPlayerNames.length;
    }

    @Override
    @Unique
    public String[] simplyvanish$getPublicPlayerNames() {
        return this.simplyvanish$publicPlayerNames.clone();
    }
}
