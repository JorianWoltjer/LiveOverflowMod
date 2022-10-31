package com.jorianwoltjer.liveoverflowmod.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedList;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;


public class Keybinds {
    public static final String LIVEOVERFLOW_CATEGORY = "category.liveoverflowmod";
    public static final double MAX_BREAK_SQUARED_DISTANCE = MathHelper.square(6.0);
    public static double MAX_DELTA = 0.05;

    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static ClientPlayNetworkHandler networkHandler;

    private static final KeyBinding worldGuardBypassToggle = new KeyBinding("key.liveoverflowmod.worldguardbypass_toggle",
                GLFW.GLFW_KEY_SEMICOLON, LIVEOVERFLOW_CATEGORY);  // Bypass WorldGuard region protection
    private static final KeyBinding reachKeybind = new KeyBinding("key.liveoverflowmod.reach",
            GLFW.GLFW_KEY_BACKSLASH, LIVEOVERFLOW_CATEGORY);  // Hit the nearest player from far away
    private static final KeyBinding panicKeybind = new KeyBinding("key.liveoverflowmod.panic",
            GLFW.GLFW_KEY_COMMA, LIVEOVERFLOW_CATEGORY);  // Fly up as fast as possible

    public static LinkedList<Packet<?>> packetQueue = new LinkedList<>();
    public static boolean worldGuardBypassEnabled = false;
    public static boolean needsHitPacket = false;
    public static int panicTimer = 0;
    public static PlayerEntity targetPlayer;
    public static Vec3d virtualPosition;

    public static void registerKeybinds() {
        KeyBindingHelper.registerKeyBinding(worldGuardBypassToggle);
        KeyBindingHelper.registerKeyBinding(reachKeybind);
        KeyBindingHelper.registerKeyBinding(panicKeybind);
    }

    public static PlayerEntity getClosestPlayer() {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        PlayerEntity closestPlayer = null;
        double closestPlayerDistance = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {  // Skip self
                continue;
            }
            double distance = mc.player.squaredDistanceTo(player);
            if (distance < closestPlayerDistance) {  // Found a closer player
                closestPlayer = player;
                closestPlayerDistance = distance;
            }
        }
        return closestPlayer;
    }

    public static boolean inSameBlock(Vec3d vector, Vec3d other) {
        return other.x >= Math.floor(vector.x) && other.x <= Math.ceil(vector.x) &&
                other.y >= Math.floor(vector.y) && other.y <= Math.ceil(vector.y) &&
                other.z >= Math.floor(vector.z) && other.z <= Math.ceil(vector.z);
    }

    public static <T> void addToMiddle(LinkedList<T> list, T object) {
        int middle = list.size() / 2;
        list.add(middle, object);
    }

    public static void checkKeybinds(MinecraftClient client) {
        networkHandler = client.getNetworkHandler();
        if (client.player != null && client.world != null && networkHandler != null) {
            // Toggle WorldGuard Bypass
            while (worldGuardBypassToggle.wasPressed()) {
                if (worldGuardBypassEnabled) {
                    worldGuardBypassEnabled = false;
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rWorldGuard Bypass: §cDisabled"), false);
                } else {
                    worldGuardBypassEnabled = true;
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rWorldGuard Bypass: §aEnabled"), false);
                }
            }

            // WorldGuard bypass
            if (worldGuardBypassEnabled) {
                client.player.setVelocity(0, 0, 0);

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

                if (vec.length() > 0) {
                    vec = vec.normalize();  // Normalize to length 1

                    if (!(vec.x == 0 && vec.z == 0)) {  // Rotate by looking yaw (won't change length)
                        double moveAngle = Math.atan2(vec.x, vec.z) + Math.toRadians(client.player.getYaw() + 90);
                        double x = Math.cos(moveAngle);
                        double z = Math.sin(moveAngle);
                        vec = new Vec3d(x, vec.y, z);
                    }

                    vec = vec.multiply(MAX_DELTA);  // Scale to maxDelta

                    Vec3d newPos = new Vec3d(client.player.getX() + vec.x, client.player.getY() + vec.y, client.player.getZ() + vec.z);
                    // If able to add more without going over a block boundary, add more
                    boolean extra = false;
                    if (client.options.sprintKey.isPressed()) {  // Trigger by sprinting
                        while (inSameBlock(newPos.add(vec.multiply(1.5)), new Vec3d(client.player.prevX, client.player.prevY, client.player.prevZ))) {
                            newPos = newPos.add(vec);
                            extra = true;
                        }
                    }

                    client.player.setPosition(newPos);

                    // Send tiny movement so delta is small enough
                    PlayerMoveC2SPacket.Full smallMovePacket = new PlayerMoveC2SPacket.Full(client.player.getX(), client.player.getY(),
                            client.player.getZ(), client.player.getYaw(), client.player.getPitch(), client.player.isOnGround());
                    networkHandler.getConnection().send(smallMovePacket);

                    // Send far away packet for "moving too quickly!" to reset position
                    if (!extra) {
                        PlayerMoveC2SPacket.Full farPacket = new PlayerMoveC2SPacket.Full(client.player.getX() + 1337.0, client.player.getY() + 1337.0,
                                client.player.getZ() + 1337.0, client.player.getYaw(), client.player.getPitch(), client.player.isOnGround());
                        networkHandler.getConnection().send(farPacket);
                    }
                }
            }

            // Reach
            while (reachKeybind.wasPressed()) {
                if (packetQueue.size() > 0) {
                    break;  // Already running
                }
                targetPlayer = getClosestPlayer();
                if (targetPlayer != null) {
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rReach: §a" + targetPlayer.getEntityName()), false);
                    needsHitPacket = true;
                    virtualPosition = client.player.getPos();
                    // Move close enough to player
                    for (int i = 0; i < 5; i++) {  // Max 5 packets per tick
                        // If player is too far away, move closer
                        if (targetPlayer.squaredDistanceTo(virtualPosition.add(0, client.player.getStandingEyeHeight(), 0)) >= MAX_BREAK_SQUARED_DISTANCE) {
                            Vec3d movementNeeded = targetPlayer.getPos().subtract(virtualPosition);
                            double length = movementNeeded.lengthSquared();

                            LOGGER.info(String.format("Movement needed: %s (%f)", movementNeeded, length));

                            // Squared length is max 100
                            if (length > 100) {
                                // Normalize
                                movementNeeded = movementNeeded.multiply(9.9 / Math.sqrt(length));  // Almost 10 to give some leeway
                            }
                            LOGGER.info(String.format("Movement packet: %s (%f)", movementNeeded, movementNeeded.lengthSquared()));

                            Vec3d pos = virtualPosition.add(movementNeeded);

                            virtualPosition = pos;
                            // Add forward and backwards packets
                            addToMiddle(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                            addToMiddle(packetQueue, new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                        }
                    }
                    // Add hit packet and back to original position
                    addToMiddle(packetQueue, PlayerInteractEntityC2SPacket.attack(targetPlayer, client.player.isSneaking()));
                    packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(), client.player.getY(), client.player.getZ(), true));
                } else {
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rReach: §cNo players found"), false);
                }
            }

            // Send packets from reach queue (max 5)
            int movementPacketsLeft = 5;
            while (packetQueue.size() != 0 && movementPacketsLeft != 0) {
                Packet<?> packet = packetQueue.remove(0);
                if (packet instanceof PlayerMoveC2SPacket) {
                    movementPacketsLeft--;
                }
                networkHandler.getConnection().send(packet);
            }

            // Panic
            if (panicKeybind.wasPressed()) {
                client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rPanic: §aFlying up 1000 blocks"), false);
                panicTimer = 20;
            }
            if (panicTimer > 0) {
                panicTimer--;
                for (int i = 0; i < 5; i++) {  // Max 5 packets per tick
                    Vec3d pos = client.player.getPos().add(0, 10, 0);  // Max 10 blocks per packet
                    client.player.setPosition(pos);
                    client.player.setVelocity(0, 0, 0);
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                }
                if (panicTimer == 0) {  // If end
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §rPanic: §cDisabled"), false);
                }
            }
        }
    }
}

