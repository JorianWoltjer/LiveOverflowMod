package com.jorianwoltjer.liveoverflowmod.client;

import com.jorianwoltjer.liveoverflowmod.mixin.ClientConnectionInvoker;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedList;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;


public class Keybinds {
    public static final String LIVEOVERFLOW_CATEGORY = "category.liveoverflowmod";
    public static final double MAX_DELTA = 0.05;
    public static final int OFFHAND = 45;

    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static ClientPlayNetworkHandler networkHandler;

    private static final KeyBinding worldGuardBypassToggle = new KeyBinding("key.liveoverflowmod.worldguardbypass_toggle",
            GLFW.GLFW_KEY_SEMICOLON, LIVEOVERFLOW_CATEGORY);  // Bypass WorldGuard region protection
    private static final KeyBinding reachKeybind = new KeyBinding("key.liveoverflowmod.reach",
            GLFW.GLFW_KEY_BACKSLASH, LIVEOVERFLOW_CATEGORY);  // Extend reach to infinity
    private static final KeyBinding panicKeybind = new KeyBinding("key.liveoverflowmod.panic",
            GLFW.GLFW_KEY_COMMA, LIVEOVERFLOW_CATEGORY);  // Fly up as fast as possible
    private static final KeyBinding modToggle = new KeyBinding("key.liveoverflowmod.passive_toggle",
            GLFW.GLFW_KEY_MINUS, LIVEOVERFLOW_CATEGORY);  // Toggle passive mods on/off
    private static final KeyBinding placeKeybind = new KeyBinding("key.liveoverflowmod.place",
            GLFW.GLFW_KEY_RIGHT_BRACKET, LIVEOVERFLOW_CATEGORY);  // Place blocks around you

    // packetQueue: [1, 2, 3, 4, 5, A, 4, 3, 2, 1, 0]
    public static LinkedList<Packet<?>> packetQueue = new LinkedList<>();
    public static boolean worldGuardBypassEnabled = false;
    public static boolean reachEnabled = false;
    public static boolean placeEnabled = false;
    public static boolean passiveModsEnabled = true;
    public static int flyingTimer = 0;
    public static int panicTimer = 0;

    public static void registerKeybinds() {
        KeyBindingHelper.registerKeyBinding(worldGuardBypassToggle);
        KeyBindingHelper.registerKeyBinding(reachKeybind);
        KeyBindingHelper.registerKeyBinding(panicKeybind);
        KeyBindingHelper.registerKeyBinding(modToggle);
        KeyBindingHelper.registerKeyBinding(placeKeybind);
    }

    public static boolean inSameBlock(Vec3d vector, Vec3d other) {
        return other.x >= Math.floor(vector.x) && other.x <= Math.ceil(vector.x) &&
                other.y >= Math.floor(vector.y) && other.y <= Math.ceil(vector.y) &&
                other.z >= Math.floor(vector.z) && other.z <= Math.ceil(vector.z);
    }

    public static void checkKeybinds(MinecraftClient client) {
        networkHandler = client.getNetworkHandler();
        if (client.player != null && client.world != null && networkHandler != null && client.interactionManager != null) {
            while (modToggle.wasPressed()) {  // Toggle whole mod
                passiveModsEnabled = !passiveModsEnabled;
                if (passiveModsEnabled) {
                    client.player.sendMessage(Text.of(PREFIX + "Passive Mods: §aEnabled"), true);
                } else {
                    client.player.sendMessage(Text.of(PREFIX + "Passive Mods: §cDisabled"), true);
                }
                // Reload chunks
                client.worldRenderer.reload();
            }

            // Toggle WorldGuard Bypass
            while (worldGuardBypassToggle.wasPressed()) {
                flyingTimer = 0;
                if (worldGuardBypassEnabled) {
                    worldGuardBypassEnabled = false;
                    client.player.sendMessage(Text.of(PREFIX + "WorldGuard Bypass: §cDisabled"), true);
                } else {
                    worldGuardBypassEnabled = true;
                    client.player.sendMessage(Text.of(PREFIX + "WorldGuard Bypass: §aEnabled"), true);
                }
            }

            // WorldGuard bypass
            if (worldGuardBypassEnabled) {
                if (++flyingTimer > 20) {  // Max 80, to bypass "Flying is not enabled"
                    ((ClientConnectionInvoker) networkHandler.getConnection())._sendImmediately(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),
                            client.player.getY() - 0.04, client.player.getZ(), client.player.isOnGround()), null);
                    flyingTimer = 0;  // Reset
                } else {
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
            }

            // Reach
            while (reachKeybind.wasPressed()) {
                reachEnabled = !reachEnabled;
                if (reachEnabled) {
                    client.player.sendMessage(Text.of(PREFIX + "Reach: §aEnabled"), true);
                } else {
                    client.player.sendMessage(Text.of(PREFIX + "Reach: §cDisabled"), true);
                }
            }

            // Send packets from reach queue (max 5)
            int movementPacketsLeft = 5;
            int blockBreakPacketsLeft = 1;
            while (packetQueue.size() > 0 && movementPacketsLeft > 0 && blockBreakPacketsLeft > 0) {
                Packet<?> packet = packetQueue.remove(0);
                if (packet instanceof PlayerMoveC2SPacket || packet instanceof VehicleMoveC2SPacket) {
                    movementPacketsLeft--;
                }
                if (packet instanceof PlayerActionC2SPacket breakPacket && breakPacket.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                    blockBreakPacketsLeft--;
                }
                ((ClientConnectionInvoker) networkHandler.getConnection())._sendImmediately(packet, null);
            }

            // Panic
            if (panicKeybind.wasPressed()) {
                client.player.sendMessage(Text.of(PREFIX + "Panic: §aFlying up 1000 blocks"), true);
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
                    client.player.sendMessage(Text.of(PREFIX + "Panic: §cFinished"), true);
                }
            }

            while (placeKeybind.wasPressed()) {
                placeEnabled = !placeEnabled;
                if (placeEnabled) {
                    client.player.sendMessage(Text.of(PREFIX + "Place: §aEnabled"), true);
                } else {
                    client.player.sendMessage(Text.of(PREFIX + "Place: §cDisabled"), true);
                }
            }
            if (placeEnabled) {
                BlockPos[] positions = {
                        client.player.getBlockPos().add(0, 2, 0),
                        client.player.getBlockPos().add(1, 1, 0),
                        client.player.getBlockPos().add(0, 1, 1),
                        client.player.getBlockPos().add(-1, 1, 0),
                        client.player.getBlockPos().add(0, 1, -1),
                        client.player.getBlockPos().add(1, 0, 0),
                        client.player.getBlockPos().add(0, 0, 1),
                        client.player.getBlockPos().add(-1, 0, 0),
                        client.player.getBlockPos().add(0, 0, -1),
                };
                // Check if enough blocks in offhand
                if (client.player.getOffHandStack().getItem() == Items.COBBLESTONE && client.player.getOffHandStack().getCount() > positions.length) {
                    for (BlockPos pos : positions) {
                        if (client.world.getBlockState(pos).isAir()) {  // Only place if empty
                            placeAt(pos);
                        }
                    }
                }
            }
        }
    }

    public static void placeAt(BlockPos pos) {
        networkHandler.sendPacket(
                new PlayerInteractBlockC2SPacket(
                        Hand.OFF_HAND,
                        new BlockHitResult(
                                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                                Direction.DOWN,
                                pos,
                                false
                        ),
                        0
                )
        );
    }
}
