package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // Log packets
    @SuppressWarnings({"EmptyMethod", "CommentedOutCode"})
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    void onSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
//        LOGGER.info("---> " + packet.getClass().getSimpleName());

//        if (packet instanceof PlayerMoveC2SPacket _packet) {
//            LOGGER.info(String.format("PlayerMoveC2SPacket(%.3f, %.3f, %.3f)",
//                    _packet.getX(0),
//                    _packet.getY(0),
//                    _packet.getZ(0)
//            ));
//        }
    }

    @SuppressWarnings({"EmptyMethod"})
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
//        LOGGER.info("<--- " + packet.getClass().getSimpleName());
    }
}
