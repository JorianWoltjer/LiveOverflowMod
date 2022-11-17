package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void _syncSelectedSlot();

    @Accessor("networkHandler")
    ClientPlayNetworkHandler _networkHandler();

    @Accessor("gameMode")
    GameMode _gameMode();

}
