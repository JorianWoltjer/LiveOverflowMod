package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Normalize WorldBorder
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setSize(D)V"))
    private void setSize(Args args) {  // Set radius to default 30 million
        if (passiveMods.enabled) {
            args.set(0, 30000000.0D);  // radius
        }
    }
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setCenter(DD)V"))
    private void setCenter(Args args) {  // Set center to 0 0
        if (passiveMods.enabled) {
            args.set(0, 0.0D);  // x
            args.set(1, 0.0D);  // z
        }
    }

    // Disable demo popup, end credits and creative mode
    @Redirect(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason getReason(GameStateChangeS2CPacket instance) {
        GameStateChangeS2CPacket.Reason reason = ((GameStateChangeS2CPacketAccessor) instance)._reason();
        if (passiveMods.enabled) {
            if (reason.equals(GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN) ||  // Demo popup
                reason.equals(GameStateChangeS2CPacket.GAME_MODE_CHANGED)) {  // Creative mode
                // Completely ignore packets
                return null;
            } else if (reason.equals(GameStateChangeS2CPacket.GAME_WON)) {  // End credits (still send respawn packet)
                networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
                return null;
            }
        }
        return reason;
    }

    // Panic if hit
    @Inject(method = "onHealthUpdate", at = @At("HEAD"))
    void onHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (panicModeHack.enabled && packet.getHealth() < 20.0F) {
            panicModeHack.triggerPanic();
        }
    }
}
