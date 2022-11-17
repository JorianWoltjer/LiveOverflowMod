package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
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

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.client.Keybinds.packetQueue;
import static com.jorianwoltjer.liveoverflowmod.client.Keybinds.reachEnabled;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private static <T> void addToMiddle(LinkedList<T> list, T object) {
        int middle = (list.size() + 1) / 2;  // Rounded up
        list.add(middle, object);
    }

    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (reachEnabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            Optional<Entity> entity = DebugRenderer.getTargetedEntity(client.player, 100);
            entity.ifPresent(e -> {
                client.crosshairTarget = new EntityHitResult(e);
            });
        }
    }

    @Redirect(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
    private void attackEntity(ClientPlayerInteractionManager instance, PlayerEntity player, Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (reachEnabled) {
            if (packetQueue.size() > 0) {
                return;  // Already running
            }
            String targetName;
            if (target.getType().equals(EntityType.PLAYER)) {
                targetName = target.getName().getString();
            } else {
                targetName = target.getType().getName().getString();
            }
            client.player.sendMessage(Text.of(String.format(PREFIX + "Reach: Hit §a%s §r(%.1f blocks)",
                    targetName, client.player.distanceTo(target))), true);

            Vec3d virtualPosition = client.player.getPos();
            // Move close enough to target
            while (true) {
                // If player is too far away, move closer
                if (target.squaredDistanceTo(virtualPosition.add(0, client.player.getStandingEyeHeight(), 0)) >= MathHelper.square(6.0)) {
                    Vec3d movement = target.getPos().subtract(virtualPosition);
                    double length = movement.lengthSquared();

                    boolean lastPacket = false;
                    if (length >= 100) {  // Length squared is max 100
                        // Normalize to length 10
                        movement = movement.multiply(9.9 / Math.sqrt(length));
                    } else {  // If short enough, this is last packet
                        movement = movement.multiply(Math.max(0, Math.sqrt(length) - 2) / 10);  // Reduce length by 2 blocks
                        lastPacket = true;
                    }
                    virtualPosition = virtualPosition.add(movement);
                    // Add forward and backwards packets
                    addToMiddle(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(virtualPosition.x, virtualPosition.y, virtualPosition.z, true));
                    if (!lastPacket) {  // If not the last packet, add a backwards packet (only need one at the sheep)
                        addToMiddle(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(virtualPosition.x, virtualPosition.y, virtualPosition.z, true));
                    }
                } else {
                    break;
                }
            }
            // Add hit packet in the middle and original position at the end
            addToMiddle(packetQueue, PlayerInteractEntityC2SPacket.attack(target, client.player.isSneaking()));
            packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(), client.player.getY(), client.player.getZ(), true));

        } else {  // Orignal code if not enabled
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
