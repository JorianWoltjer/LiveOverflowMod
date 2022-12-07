package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.client.Keybinds.packetQueue;
import static com.jorianwoltjer.liveoverflowmod.client.Keybinds.worldGuardBypassEnabled;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // Delay other packets while any packets in packetQueue
    @Inject(method = "send*", at = @At("HEAD"), cancellable = true)
    private void normalSend(Packet<?> packet, CallbackInfo ci) {
        if (worldGuardBypassEnabled) {  // Cancel other packets from getting in the way
            packetQueue.add(packet);  // Do send them later
            ci.cancel();
        }
    }

}
