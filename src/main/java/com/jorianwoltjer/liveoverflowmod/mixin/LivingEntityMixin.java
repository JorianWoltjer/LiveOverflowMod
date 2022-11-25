package com.jorianwoltjer.liveoverflowmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.worldGuardBypassHack;

@Mixin(net.minecraft.entity.LivingEntity.class)
public class LivingEntityMixin {
    // Disable regular movement when WorldGuard Bypass is enabled
    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isImmobile()Z"))
    private boolean isImmobile(net.minecraft.entity.LivingEntity livingEntity) {
        return worldGuardBypassHack.enabled;
    }
}
