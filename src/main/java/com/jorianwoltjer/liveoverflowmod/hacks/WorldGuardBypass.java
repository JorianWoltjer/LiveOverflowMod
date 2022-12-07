package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.LivingEntityMixin;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.networkHandler;

public class WorldGuardBypass extends ToggledHack {
    final double MAX_DELTA = 0.05001;  // Absolute maximum is sqrt(1/256) = 0.0625
    int flyingTimer = 0;

    /**
     * Walk through a WorldGuard protected region (with entry: deny) by avoiding `PlayerMoveEvent`s
     * @see LivingEntityMixin
     */
    public WorldGuardBypass() {
        super("WorldGuard Bypass", GLFW.GLFW_KEY_SEMICOLON);
    }

    @Override
    public void tickEnabled() {
        if (client.player == null || client.world == null) return;

        client.player.setVelocity(0, 0, 0);

        if (++flyingTimer > 20) {  // Max 80, to bypass "Flying is not enabled"
            Vec3d pos = client.player.getPos();
            pos = pos.add(0, -MAX_DELTA, 0);  // Small down position

            client.player.setPosition(pos.x, pos.y, pos.z);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(pos.x + 1337.0, pos.y + 1337.0,  // Far packet again to keep bypassing
                    pos.z + 1337.0, client.player.getYaw(), client.player.getPitch(), true));

            flyingTimer = 0;  // Reset
        } else {
            Vec3d vec = getMovementVec();
            assert vec != null;

            if (vec.length() > 0) {
                if (!(vec.x == 0 && vec.z == 0)) {  // Rotate by looking yaw (won't change length)
                    double moveAngle = Math.atan2(vec.x, vec.z) + Math.toRadians(client.player.getYaw() + 90);
                    double x = Math.cos(moveAngle);
                    double z = Math.sin(moveAngle);
                    vec = new Vec3d(x, vec.y, z);
                }

                vec = vec.multiply(MAX_DELTA);  // Scale to maxDelta

                Vec3d newPos = new Vec3d(client.player.getX() + vec.x, client.player.getY() + vec.y, client.player.getZ() + vec.z);
                if (collides(newPos)) return;  // Don't move if it would collide

                // If able to add more without going over a block boundary, add more
                boolean extra = false;
                if (client.options.sprintKey.isPressed()) {  // Trigger by sprinting
                    // If doesn't cross block boundary, and doesn't collide with anything, add more
                    while (inSameBlock(newPos.add(vec.multiply(1.5)), new Vec3d(client.player.prevX, client.player.prevY, client.player.prevZ)) &&
                            !collides(newPos.add(vec.multiply(1.5)))) {
                        newPos = newPos.add(vec);
                        extra = true;
                    }
                }

                client.player.setPosition(newPos);

                // Send tiny movement so delta is small enough
                PlayerMoveC2SPacket.Full smallMovePacket = new PlayerMoveC2SPacket.Full(client.player.getX(), client.player.getY(),
                        client.player.getZ(), client.player.getYaw(), client.player.getPitch(), true);
                networkHandler.sendPacket(smallMovePacket);

                // Send far away packet for "moving too quickly!" to reset position
                if (!extra) {
                    PlayerMoveC2SPacket.Full farPacket = new PlayerMoveC2SPacket.Full(client.player.getX() + 1337.0, client.player.getY() + 1337.0,
                            client.player.getZ() + 1337.0, client.player.getYaw(), client.player.getPitch(), true);
                    networkHandler.sendPacket(farPacket);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        flyingTimer = 0;
    }


    public static boolean inSameBlock(Vec3d vector, Vec3d other) {
        return other.x >= Math.floor(vector.x) && other.x <= Math.ceil(vector.x) &&
                other.y >= Math.floor(vector.y) && other.y <= Math.ceil(vector.y) &&
                other.z >= Math.floor(vector.z) && other.z <= Math.ceil(vector.z);
    }

    public static boolean collides(Vec3d pos) {
        if (client.player == null || client.world == null) return false;

        return !client.world.isSpaceEmpty(client.player, client.player.getBoundingBox().offset(pos.subtract(client.player.getPos())));
    }

    public static Vec3d getMovementVec() {
        if (client.player == null) return null;

        Vec3d vec = new Vec3d(0, 0, 0);

        // Key presses changing position
        if (client.player.input.jumping) {  // Move up
            vec = vec.add(new Vec3d(0, 1, 0));
        } else if (client.player.input.sneaking) {  // Move down
            vec = vec.add(new Vec3d(0, -1, 0));
        } else {
            // Horizontal movement (not at the same time as vertical)
            if (client.player.input.pressingForward) {
                vec = vec.add(new Vec3d(0, 0, 1));
            }
            if (client.player.input.pressingRight) {
                vec = vec.add(new Vec3d(1, 0, 0));
            }
            if (client.player.input.pressingBack) {
                vec = vec.add(new Vec3d(0, 0, -1));
            }
            if (client.player.input.pressingLeft) {
                vec = vec.add(new Vec3d(-1, 0, 0));
            }
        }

        return vec.normalize();
    }

}
