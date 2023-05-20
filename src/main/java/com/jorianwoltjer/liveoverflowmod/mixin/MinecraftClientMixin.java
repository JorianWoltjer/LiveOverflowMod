package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.clipReachHack;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.reachHack;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private void updateCrossairTarget() {
        if (reachHack.enabled || clipReachHack.enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            Optional<Entity> entity = DebugRenderer.getTargetedEntity(client.player, 100);
            entity.ifPresent(e -> client.crosshairTarget = new EntityHitResult(e));
        }
    }

    // Extend reach for attack
    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        updateCrossairTarget();
    }

    // Extend reach for interact
    @Inject(method = "doItemUse", at = @At(value = "HEAD"))
    private void doItemUse(CallbackInfo ci) {
        updateCrossairTarget();
    }

}
