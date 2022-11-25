package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.*;
import net.minecraft.entity.player.PlayerEntity;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.globalTimer;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS;

public class PassiveMods extends ToggledHack {
    /**
     * Passive mods that should always be on.
     * - Randomize Texture Rotations
     * - Disable Weird Packets: World Border, Creative Mode, Demo Mode, End Credits
     * - Insta-Mine
     * - Anti-Human Bypass (round coordinates)
     * @see AbstractBlockMixin
     * @see ClientPlayNetworkHandlerMixin
     * @see ClientPlayerInteractionManagerMixin
     * @see PlayerPositionFullPacketMixin
     * @see PlayerPositionPacketMixin
     * @see VehicleMovePacketMixin
     */
    public PassiveMods() {
        super("Passive Mods", GLFW_KEY_MINUS);
        this.enabled = true;  // Default on
    }

    @Override
    public void tickEnabled() {
        if (client.world == null) return;

        // Find Herobrine
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getName().getString().equals("Herobrine")) {
                message(String.format(
                        "%sFound %s §r(%.1f, %.1f, %.1f)",
                        globalTimer % 8 > 4 ? "" : "§a",  // Blinking
                        player.getName().getString(),
                        player.getX(), player.getY(), player.getZ()
                ));
                break;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        client.worldRenderer.reload();  // Reload chunks (for Texture Rotations)
    }

    @Override
    public void onDisable() {
        super.onDisable();

        client.worldRenderer.reload();  // Reload chunks (for Texture Rotations)
    }
}
