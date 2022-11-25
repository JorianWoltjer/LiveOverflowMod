package com.jorianwoltjer.liveoverflowmod.mixin;

import com.jorianwoltjer.liveoverflowmod.helper.Utils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.passiveMods;

@Mixin(PlayerMoveC2SPacket.PositionAndOnGround.class)
public class PlayerPositionPacketMixin {
    // Anti-human bypass
    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;<init>(DDDFFZZZ)V"))
    private static void init(Args args) {
        if (passiveMods.enabled) {
            Utils.onPositionPacket(args);
        }
    }

}
