package io.github.uppermoon0.vanishguard.mixin;

import io.github.uppermoon0.vanishguard.VanishGuard;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ListPlayersCommand.class)
public abstract class ListPlayersCommandMixin {
    // Covers both /list and /list uuids, including their returned command count.
    @Redirect(
            method = "format(Lnet/minecraft/commands/CommandSourceStack;Ljava/util/function/Function;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"),
            require = 1, expect = 1, allow = 1)
    private static List<ServerPlayer> vanishguard$playerFacingList(
            PlayerList playerList, CommandSourceStack source,
            Function<ServerPlayer, Component> formatter) {
        List<ServerPlayer> players = playerList.getPlayers();
        return source.getEntity() instanceof ServerPlayer ? VanishGuard.visible(players) : players;
    }
}
