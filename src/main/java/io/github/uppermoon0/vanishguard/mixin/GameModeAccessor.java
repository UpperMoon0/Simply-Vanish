package io.github.uppermoon0.vanishguard.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ServerPlayerGameMode.class)
public interface GameModeAccessor {
    @Invoker("setGameModeForPlayer") void vanishguard$setMode(GameType mode, GameType previous);
}
