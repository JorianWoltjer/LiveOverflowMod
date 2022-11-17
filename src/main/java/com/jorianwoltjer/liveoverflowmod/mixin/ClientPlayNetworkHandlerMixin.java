package com.jorianwoltjer.liveoverflowmod.mixin;

import com.jorianwoltjer.liveoverflowmod.client.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Normalize WorldBorder
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setSize(D)V"))
    private void setSize(Args args) {  // Set radius to default 30 million
        if (Keybinds.passiveModsEnabled) {
            args.set(0, 30000000.0D);  // radius
        }
    }
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setCenter(DD)V"))
    private void setCenter(Args args) {  // Set center to 0 0
        if (Keybinds.passiveModsEnabled) {
            args.set(0, 0.0D);  // x
            args.set(1, 0.0D);  // z
        }
    }

    // Disable demo popup, end credits and creative mode
    @Redirect(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason getReason(GameStateChangeS2CPacket instance) {
        GameStateChangeS2CPacket.Reason reason = ((GameStateChangeS2CPacketAccessor) instance)._reason();
        if (Keybinds.passiveModsEnabled) {
            if (reason.equals(GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN) ||  // Demo popup
                reason.equals(GameStateChangeS2CPacket.GAME_MODE_CHANGED)) {  // Creative mode
                // Completely ignore packets
                return null;
            } else if (reason.equals(GameStateChangeS2CPacket.GAME_WON)) {  // End credits (still send respawn packet)
                MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
                return null;
            }
        }
        return reason;
    }
}
