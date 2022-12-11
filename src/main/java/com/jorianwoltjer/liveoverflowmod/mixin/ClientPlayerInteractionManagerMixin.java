package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static com.jorianwoltjer.liveoverflowmod.helper.Utils.insertToCenter;

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

        if (reachHack.enabled) {
            if (packetQueue.size() > 0) {
                return;  // Already running
            }
            String targetName;
            if (target.getType().equals(EntityType.PLAYER)) {
                targetName = target.getName().getString();
            } else {
                targetName = target.getType().getName().getString();
            }
            reachHack.message(String.format("Hit §a%s §r(%.1fm)", targetName, target.distanceTo(client.player)));

            Vec3d virtualPosition = client.player.getPos();
            Vec3d targetPos = target.getPos().subtract(  // Subtract a bit from the end
                    target.getPos().subtract(virtualPosition).normalize().multiply(2)
            );
            // If player is too far away, move closer
            while (target.squaredDistanceTo(virtualPosition.add(0, client.player.getStandingEyeHeight(), 0)) >= MathHelper.square(6.0)) {
                Vec3d movement = targetPos.subtract(virtualPosition);

                boolean lastPacket = false;
                if (movement.lengthSquared() >= 100) {  // Length squared is max 100 (otherwise "moved too quickly")
                    // Normalize to length 10
                    movement = movement.normalize().multiply(9.9);
                } else {  // If short enough, this is last packet
                    lastPacket = true;
                }
                virtualPosition = virtualPosition.add(movement);
                // Add forward and backwards packets
                insertToCenter(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(virtualPosition.x, virtualPosition.y, virtualPosition.z, true));
                if (!lastPacket) {  // If not the last packet, add a backwards packet (only need one at the sheep)
                    insertToCenter(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(virtualPosition.x, virtualPosition.y, virtualPosition.z, true));
                }
            }
            // Add hit packet in the middle and original position at the end
            insertToCenter(packetQueue, PlayerInteractEntityC2SPacket.attack(target, client.player.isSneaking()));
            packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(), client.player.getY(), client.player.getZ(), true));
            packetQueue.add(new HandSwingC2SPacket(client.player.getActiveHand()));  // Serverside animation
            client.player.resetLastAttackedTicks();  // Reset attack cooldown

            ci.cancel();
        }
    }

}
