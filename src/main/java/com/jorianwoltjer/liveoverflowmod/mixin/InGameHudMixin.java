package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Don't overwrite the overlay message if it contains "LiveOverflow"
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        Text currentMessage = ((InGameHudAccessor) client.inGameHud)._overlayMessage();
        int remaining = ((InGameHudAccessor) client.inGameHud)._overlayRemaining();
        if (currentMessage == null || message == null) return;

        if (currentMessage.getString().contains("LiveOverflow") && !message.getString().contains("LiveOverflow") && remaining > 20) {
            ci.cancel();
        }
    }

}
