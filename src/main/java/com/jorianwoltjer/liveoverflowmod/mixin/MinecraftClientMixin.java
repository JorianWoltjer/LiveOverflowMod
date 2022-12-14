package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.Optional;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    // Extend reach
    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (reachHack.enabled || clipReachHack.enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            Optional<Entity> entity = DebugRenderer.getTargetedEntity(client.player, 100);
            entity.ifPresent(e -> client.crosshairTarget = new EntityHitResult(e));
        }
    }

}
