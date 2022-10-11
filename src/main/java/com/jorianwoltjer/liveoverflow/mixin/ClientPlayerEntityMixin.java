package com.jorianwoltjer.liveoverflow.mixin;

import com.jorianwoltjer.liveoverflow.client.Keybinds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.network.ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "sendMovementPackets", at = @At("TAIL"), cancellable = true)
    private void sendMovementPackets(CallbackInfo ci) {
        if (Keybinds.packetQueue.size() > 0) {
            ci.cancel();  // Cancel movement while Reach hack is sending packets
        }
    }
}
