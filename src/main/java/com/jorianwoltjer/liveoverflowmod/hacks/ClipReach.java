package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.ClientPlayerInteractionManagerMixin;
import com.jorianwoltjer.liveoverflowmod.mixin.MinecraftClientMixin;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.command.ClipCommand.moveTo;

public class ClipReach extends ToggledHack {
    /**
     * Hit enitities from far away *through blocks* by vclipping.
     * Not compatible with Meteor Client's:
     * - Anti-Hunger: will set onGound to false making you take fall damage
     * - Criticals: will send an extra move packet going over the limit
     *
     * @see MinecraftClientMixin
     * @see ClientPlayerInteractionManagerMixin
     */
    public ClipReach() {
        super("Clip Reach", GLFW.GLFW_KEY_LEFT_BRACKET);
    }

    @Override
    public void onEnable() {
        reachHack.enabled = false;  // Disable normal reach
    }

    public void hitEntity(Entity target) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();
        Vec3d targetPos = target.getPos();

        double maxDistance = 99.0D;
        Vec3d diff = pos.subtract(targetPos);
        double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
        double targetUp = flatUp + diff.y;

        for (int i = 0; i < 9; i++) {  // Build up TP range
            moveTo(pos);
        }
        moveTo(pos.add(0, maxDistance, 0));  // V-Clip up
        moveTo(targetPos.add(0, targetUp, 0));  // Can now move freely
        moveTo(targetPos);  // V-Clip down to target

        networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, client.player.isSneaking()));  // Hit packet
        networkHandler.sendPacket(new HandSwingC2SPacket(client.player.getActiveHand()));  // Serverside animation

        moveTo(targetPos.add(0, targetUp + 0.01, 0));  // V-Clip up
        moveTo(pos.add(0, maxDistance + 0.01, 0));  // Can now move freely
        moveTo(pos);  // V-Clip down to original position
        client.player.setPosition(pos);  // Set position on client-side
    }

    public void interactAtEntity(Entity target) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();
        Vec3d targetPos = target.getPos();

        double maxDistance = 99.0D;
        Vec3d diff = pos.subtract(targetPos);
        double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
        double targetUp = flatUp + diff.y;

        for (int i = 0; i < 9; i++) {  // Build up TP range
            moveTo(pos);
        }
        moveTo(pos.add(0, maxDistance, 0));  // V-Clip up
        moveTo(targetPos.add(0, targetUp, 0));  // Can now move freely
        moveTo(targetPos, client.player.getYaw(), 90);  // V-Clip down to target (looking down)

        networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));  // Interact item packet
        networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(target, client.player.isSneaking(), Hand.MAIN_HAND));  // Interact entity packet
        networkHandler.sendPacket(new HandSwingC2SPacket(client.player.getActiveHand()));  // Serverside animation

        moveTo(targetPos.add(0, targetUp + 0.01, 0), client.player.getYaw(), client.player.getPitch());  // V-Clip up (looking normal)
        moveTo(pos.add(0, maxDistance + 0.01, 0));  // Can now move freely
        moveTo(pos);  // V-Clip down to original position
        client.player.setPosition(pos);  // Set position on client-side
    }
}
