package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.command.ClipCommand;
import com.jorianwoltjer.liveoverflowmod.mixin.ClientPlayNetworkHandlerMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

public class PanicMode extends ToggledHack {
    final double MAX_DELTA = 10;
    boolean panicActive = false;

    /**
     * Fly up and disconnect if the player is in a dangerous situation.
     * - If another player is in render distance
     * - If health goes down
     * @see ClientPlayNetworkHandlerMixin
     */
    public PanicMode() {
        super("Panic", GLFW.GLFW_KEY_M);
    }

    @Override
    public void tick(MinecraftClient client) {
        super.tick(client);
        if (client.world == null) return;

        if (panicActive && packetQueue.size() == 0) {  // If ended, disconnect
            panicActive = false;  // Disable for next join
            client.world.disconnect();
            client.disconnect(new DisconnectedScreen(null, Text.of(PREFIX), Text.of("Panic Mode: §cTriggered")));
        }
    }

    @Override
    public void tickEnabled() {
        if (client.world == null) return;

        if (client.world.getPlayers().size() > 1) {
            triggerPanic();
        }
    }

    public void triggerPanic() {
        if (client.player == null) return;

        this.enabled = false;  // Don't trigger repeatedly
        panicActive = true;
        message("§cTriggered§r, flying up...");

        // Fly up 1000 blocks
        for (int i = 0; i < 1000/MAX_DELTA; i++) {
            Vec3d pos = client.player.getPos().add(0, MAX_DELTA, 0);  // Max 10 blocks per packet

            if (client.player.getVehicle() != null) {  // If in boat
                ClipCommand.moveVehicleTo(client.player.getVehicle(), pos);
            } else {
                client.player.setPosition(pos);
                packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
            }
        }

    }
}
