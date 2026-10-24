package io.github.uppermoon0.vanishguard;

import io.github.uppermoon0.vanishguard.mixin.PlayerInfoAccessor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Copies only changed packets. Never edits shared broadcast packet state. */
public final class PacketVisibility {
    private PacketVisibility() {}
    public static Packet<?> filter(Packet<?> packet, ServerPlayer viewer) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
            var entries = info.entries().stream().filter(e -> visible(e.profileId(), viewer)).toList();
            if (entries.size() == info.entries().size()) return packet;
            if (entries.isEmpty()) return null;
            var copy = new ClientboundPlayerInfoUpdatePacket(info.actions(), List.<ServerPlayer>of());
            ((PlayerInfoAccessor)copy).vanishguard$setEntries(entries);
            return copy;
        }
        if (packet instanceof ClientboundPlayerInfoRemovePacket removal) {
            var ids = removal.profileIds().stream().filter(id -> visible(id, viewer)).toList();
            if (ids.size() == removal.profileIds().size()) return packet;
            return ids.isEmpty() ? null : new ClientboundPlayerInfoRemovePacket(ids);
        }
        if (packet instanceof ClientboundSetPassengersPacket passengers) {
            if (!visibleEntity(passengers.getVehicle(), viewer)) return null;
            int[] ids = Arrays.stream(passengers.getPassengers()).filter(id -> visibleEntity(id, viewer)).toArray();
            if (ids.length == passengers.getPassengers().length) return packet;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeVarInt(passengers.getVehicle());
                buffer.writeVarIntArray(ids);
                return new ClientboundSetPassengersPacket(buffer);
            } finally { buffer.release(); }
        }
        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<ClientGamePacketListener>> kept = new ArrayList<>();
            boolean changed = false;
            for (Packet<ClientGamePacketListener> nested : bundle.subPackets()) {
                Packet<?> filtered = filter(nested, viewer);
                changed |= filtered != nested;
                if (filtered != null) kept.add(clientPacket(filtered));
            }
            if (!changed) return packet;
            return kept.isEmpty() ? null : new ClientboundBundlePacket(kept);
        }
        return packet;
    }
    @SuppressWarnings("unchecked")
    private static Packet<ClientGamePacketListener> clientPacket(Packet<?> packet) {
        return (Packet<ClientGamePacketListener>)packet;
    }
    private static boolean visible(UUID uuid, ServerPlayer viewer) {
        return uuid.equals(viewer.getUUID()) || !VanishGuard.hidden(uuid);
    }
    private static boolean visibleEntity(int id, ServerPlayer viewer) {
        for (ServerPlayer player : viewer.server.getPlayerList().getPlayers()) {
            if (player.getId() == id) return visible(player.getUUID(), viewer);
        }
        return true;
    }
}
