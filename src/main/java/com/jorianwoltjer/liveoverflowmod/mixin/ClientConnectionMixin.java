package com.jorianwoltjer.liveoverflowmod.mixin;

import com.jorianwoltjer.liveoverflowmod.hacks.ToggledHack;
import net.minecraft.block.Block;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // Log outgoing packets (for debugging)
    @SuppressWarnings({"EmptyMethod", "CommentedOutCode"})
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    void onSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
//        LOGGER.info("---> " + packet.getClass().getSimpleName());
//
//        if (packet instanceof PlayerInteractBlockC2SPacket _packet) {
//            LOGGER.info(String.format("PlayerInteractBlockC2SPacket(%s, %s, %s)", _packet.getHand(), _packet.getBlockHitResult().getBlockPos(), _packet.getBlockHitResult().getPos()));
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
        for (ToggledHack hack : toggledHacks) {
            if (hack.enabled != hack.defaultEnabled) {
                hack.toggle();
            }
        }
    }
}
