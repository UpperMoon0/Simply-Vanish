package io.github.uppermoon0.vanishguard.fixture;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.protocol.game.*;
import java.util.List;

/** Test-only mod, excluded from the production artifact. No commands or permissions. */
@Mod("vanishguardfixture")
public final class RefreshFixture {
    private int ticks;
    private boolean sent;
    public RefreshFixture() { MinecraftForge.EVENT_BUS.addListener(this::tick); }
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || sent) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        var hidden = server.getPlayerList().getPlayerByName("VanishFixture");
        var visible = server.getPlayerList().getPlayerByName("VisibleFixtureB");
        if (hidden == null || visible == null || ++ticks < 100) return;
        sent = true;
        for (var viewer : server.getPlayerList().getPlayers()) {
            // Exact packet pattern used by delayed skin refresh, plus a mixed bundle.
            viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(hidden.getUUID())));
            viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(hidden)));
            viewer.connection.send(new ClientboundBundlePacket(List.of(
                new ClientboundPlayerInfoRemovePacket(List.of(hidden.getUUID(), visible.getUUID())),
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(hidden, visible)))));
        }
        System.out.println("VANISHGUARD_DELAYED_REFRESH_FIXTURE_SENT");
    }
}
