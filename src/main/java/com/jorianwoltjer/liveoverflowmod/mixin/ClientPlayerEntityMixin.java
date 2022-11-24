package com.jorianwoltjer.liveoverflowmod.mixin;

import com.jorianwoltjer.liveoverflowmod.client.Keybinds;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    // If this is false, no movement packets will be sent
    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isCamera()Z"))
    private boolean isCamera(ClientPlayerEntity instance) {
        return ((ClientPlayerEntityAccessor)instance)._client().getCameraEntity() == instance &&
                Keybinds.packetQueue.size() == 0;
    }

    // No boat movement packets
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z"))
    private boolean hasVehicle(ClientPlayerEntity instance) {
        return instance.getVehicle() != null &&
                Keybinds.packetQueue.size() == 0;
    }

    // No hand swing before attack (will reset the attack cooldown otherwise)
    @Redirect(method = "swingHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void swingHand(ClientPlayNetworkHandler instance, Packet<?> packet) {
        if (Keybinds.packetQueue.size() == 0) {
            instance.getConnection().send(packet);
        }
    }

}
