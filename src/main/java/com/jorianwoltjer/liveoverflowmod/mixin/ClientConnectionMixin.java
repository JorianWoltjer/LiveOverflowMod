package com.jorianwoltjer.liveoverflowmod.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // Log packets
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    void onSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
//        if (packet instanceof PlayerInteractBlockC2SPacket interactPacket) {
//            BlockHitResult blockHitResult = interactPacket.getBlockHitResult();
//            LOGGER.info("PlayerInteractBlockC2SPacket: " + blockHitResult.getBlockPos() + " " + blockHitResult.getSide()
//                    + " " + blockHitResult.getPos() + " " + blockHitResult.isInsideBlock());
//
//        }

//        LOGGER.info("Sending packet: " + packet.getClass().getSimpleName());
//        if (packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
////            LOGGER.info("PlayerInteractEntityC2SPacket: " + interactPacket.getT + " " + interactPacket.());
//        }
    }
}
