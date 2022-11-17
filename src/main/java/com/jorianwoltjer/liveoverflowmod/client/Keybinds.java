package com.jorianwoltjer.liveoverflowmod.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;



public class Keybinds {
    public static final String LIVEOVERFLOW_CATEGORY = "category.liveoverflowmod";
    public static double MAX_DELTA = 0.05;

    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static ClientPlayNetworkHandler networkHandler;

    private static final KeyBinding worldGuardBypassToggle = new KeyBinding("key.liveoverflowmod.worldguardbypass_toggle",
            GLFW.GLFW_KEY_SEMICOLON, LIVEOVERFLOW_CATEGORY);  // Bypass WorldGuard region protection
    private static final KeyBinding modToggle = new KeyBinding("key.liveoverflowmod.passive_toggle",
            GLFW.GLFW_KEY_MINUS, LIVEOVERFLOW_CATEGORY);  // Toggle passive mods on/off

    public static boolean worldGuardBypassEnabled = false;
    public static boolean passiveModsEnabled = true;
    public static int flyingTimer = 0;

    public static void registerKeybinds() {
        KeyBindingHelper.registerKeyBinding(worldGuardBypassToggle);
        KeyBindingHelper.registerKeyBinding(modToggle);
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
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §aEnabled"), false);
                } else {
                    client.player.sendMessage(Text.of("§7[LiveOverflowMod] §cDisabled"), false);
                }
            }

            // Toggle WorldGuard Bypass
            while (worldGuardBypassToggle.wasPressed()) {
                flyingTimer = 0;
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
                if (++flyingTimer > 30) {  // Max 80, to bypass "Flying is not enabled"
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(client.player.getX(),
                            client.player.getY() - 0.04, client.player.getZ(), client.player.isOnGround()));
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
        }
    }
}
