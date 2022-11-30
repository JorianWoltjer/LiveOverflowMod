package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // Log outgoing packets (for debugging)
    @SuppressWarnings({"EmptyMethod", "CommentedOutCode"})
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    void onSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
//        LOGGER.info("---> " + packet.getClass().getSimpleName());
//
//        if (packet instanceof PlayerMoveC2SPacket _packet) {
//            LOGGER.info(String.format("PlayerMoveC2SPacket(%.3f, %.3f, %.3f)",
//                    _packet.getX(0),
//                    _packet.getY(0),
//                    _packet.getZ(0)
//            ));
//        }
    }

    // Log incoming packets (for debugging)
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
//        LOGGER.info("<--- " + packet.getClass().getSimpleName());

        // Save statistics response
        if (packet instanceof StatisticsS2CPacket statsPacket) {
            // Get mined stat for FastBreak
            Integer statsValue = statsPacket.getStatMap().get(Stats.MINED.getOrCreateStat(Block.getBlockFromItem(fastBreakHack.itemToPlace)));
            fastBreakHack.onStatResponse(statsValue);
        }
    }

    // Delay other packets while any packets in packetQueue
    @Inject(method = "send*", at = @At("HEAD"), cancellable = true)
    private void normalSend(Packet<?> packet, CallbackInfo ci) {
        if (packetQueue.size() > 0) {
            packetQueue.add(packet);  // Do send them later
            ci.cancel();
        }
    }

    // Clear packet queue
    @Inject(method = "disconnect", at = @At("HEAD"))
    void onDisconnect(Text reason, CallbackInfo ci) {
        packetQueue.clear();
    }
}
