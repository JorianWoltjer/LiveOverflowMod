package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.ClientPlayerInteractionManagerMixin;
import com.jorianwoltjer.liveoverflowmod.mixin.MinecraftClientMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.helper.Utils.insertToCenter;

public class Reach extends ToggledHack {
    /**
     * Hit enitities from far away
     * @see MinecraftClientMixin
     * @see ClientPlayerInteractionManagerMixin
     */
    public Reach() {
        super("Reach", GLFW.GLFW_KEY_RIGHT_BRACKET);
    }

    @Override
    public void onEnable() {
        clipReachHack.enabled = false;  // Disable clip reach
    }

    public void hitEntity(Entity target) {
        if (client.player == null) return;

        if (packetQueue.size() > 0) {
            return;  // Already running (may take multiple ticks)
        }

        Vec3d pos = client.player.getPos();
        Vec3d targetPos = target.getPos().subtract(  // Subtract a bit from the end
                target.getPos().subtract(pos).normalize().multiply(2)
        );
        // If player is still too far away, move closer
        while (target.squaredDistanceTo(pos.add(0, client.player.getStandingEyeHeight(), 0)) >= MathHelper.square(6.0)) {
            Vec3d movement = targetPos.subtract(pos);

            boolean lastPacket = false;
            if (movement.lengthSquared() >= 100) {  // Length squared is max 100 (otherwise "moved too quickly")
                // Normalize to length 10
                movement = movement.normalize().multiply(9.9);
            } else {  // If short enough, this is last packet
                lastPacket = true;
            }
            pos = pos.add(movement);

            // Add forward and backwards packets
            insertToCenter(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
            if (!lastPacket) {  // If not the last packet, add a backwards packet (only need one at the sheep)
                insertToCenter(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
            }
        }
        // Add hit packet in the middle and original position at the end
        insertToCenter(packetQueue, PlayerInteractEntityC2SPacket.attack(target, client.player.isSneaking()));
        packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(), client.player.getY(), client.player.getZ(), true));
        packetQueue.add(new HandSwingC2SPacket(client.player.getActiveHand()));  // Serverside animation
        client.player.resetLastAttackedTicks();  // Reset attack cooldown
    }
}
