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

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.packetQueue;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.reachHack;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @SuppressWarnings("SameParameterValue")
    private static <T> void insertToCenter(LinkedList<T> list, T object) {
        int middle = (list.size() + 1) / 2;  // Rounded up
        list.add(middle, object);
    }

    // Extend reach
    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (reachHack.enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            Optional<Entity> entity = DebugRenderer.getTargetedEntity(client.player, 100);
            entity.ifPresent(e -> client.crosshairTarget = new EntityHitResult(e));
        }
    }

    // On attack
    @Redirect(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
    private void attackEntity(ClientPlayerInteractionManager instance, PlayerEntity player, Entity target) {
        // packetQueue: [1, 2, 3, 4, 5, A, 4, 3, 2, 1, 0]
        // A = attack packet
        // 0 = starting position
        // 1-5 = path
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

        } else {  // Orignal `attackEntity()` code if not enabled
            ClientPlayerInteractionManagerAccessor _instance = (ClientPlayerInteractionManagerAccessor) instance;
            _instance._syncSelectedSlot();
            _instance._networkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
            if (_instance._gameMode() != GameMode.SPECTATOR) {
                player.attack(target);
                player.resetLastAttackedTicks();
            }
        }
    }
}
