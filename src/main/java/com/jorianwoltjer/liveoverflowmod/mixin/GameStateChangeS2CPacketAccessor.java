package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameStateChangeS2CPacket.class)
public interface GameStateChangeS2CPacketAccessor {
    @Accessor("reason")
    GameStateChangeS2CPacket.Reason _reason();

}
