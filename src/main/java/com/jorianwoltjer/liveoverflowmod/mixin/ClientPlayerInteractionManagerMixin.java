package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // Insta-Mine
    @Inject(method = "attackBlock", at = @At(value = "HEAD"), cancellable = true)
    private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (client.world == null || client.player == null) return;

        if (passiveMods.enabled) {
            BlockState blockState = client.world.getBlockState(pos);
            double speed = blockState.calcBlockBreakingDelta(client.player, client.world, pos);
            if (!blockState.isAir() && speed > 0.5F) {  // If you can break the block fast enough, break it instantly
                client.world.breakBlock(pos, true, client.player);
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                cir.setReturnValue(true);  // Return true to break the block on the client-side
            }
        }
    }

    // On attack, use Reach
    @Inject(method = "attackEntity", at = @At(value = "HEAD"), cancellable = true)
    private void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (client.player == null) return;

        if (reachHack.enabled || clipReachHack.enabled) {
            Vec3d pos = client.player.getPos();

            // If player is too far away, needs reach
            if (target.squaredDistanceTo(pos.add(0, client.player.getStandingEyeHeight(), 0)) >= MathHelper.square(6.0)) {
                String targetName;
                if (target.getType().equals(EntityType.PLAYER)) {
                    targetName = target.getName().getString();
                } else {
                    targetName = target.getType().getName().getString();
                }

                if (reachHack.enabled) {
                    reachHack.message(String.format("Hit §a%s §r(§b%.0fm§r)", targetName, target.distanceTo(client.player)));
                    reachHack.hitEntity(target);
                } else if (clipReachHack.enabled) {
                    clipReachHack.message(String.format("Hit §a%s §r(§b%.0fm§r)", targetName, target.distanceTo(client.player)));
                    clipReachHack.hitEntity(target);
                }

                ci.cancel();
            }
        }
    }

    // On interact, use Reach
    @Inject(method = "interactEntityAtLocation", at = @At(value = "HEAD"), cancellable = true)
    private void interactEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (client.player == null) return;

        if (clipReachHack.enabled && hand.equals(Hand.MAIN_HAND)) {
            Vec3d pos = client.player.getPos();

            // If player is too far away, needs reach
            if (entity.squaredDistanceTo(pos.add(0, client.player.getStandingEyeHeight(), 0)) >= MathHelper.square(6.0)) {
                String targetName;
                if (entity.getType().equals(EntityType.PLAYER)) {
                    targetName = entity.getName().getString();
                } else {
                    targetName = entity.getType().getName().getString();
                }

                clipReachHack.message(String.format("Interacted with §a%s §r(§b%.0fm§r)", targetName, entity.distanceTo(client.player)));
                clipReachHack.interactAtEntity(entity);

                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

}
