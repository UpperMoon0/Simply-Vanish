package io.github.uppermoon0.simplyvanish;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

@Mod(SimplyVanish.ID)
public final class SimplyVanish {
    public static final String ID = "simplyvanish";
    private static volatile VanishConfig config;
    public SimplyVanish() { config = VanishConfig.load(FMLPaths.CONFIGDIR.get().resolve("simply-vanish.json")); }
    public static boolean observer() { return config == null || config.observerMode(); }
    public static boolean hidden(UUID id) { return config != null && config.hiddenUuids().contains(id); }
    public static boolean hidden(net.minecraft.world.entity.Entity entity) { return entity instanceof ServerPlayer && hidden(entity.getUUID()); }
    public static List<ServerPlayer> visible(Collection<ServerPlayer> players) { return players.stream().filter(p -> !hidden(p)).toList(); }
}
