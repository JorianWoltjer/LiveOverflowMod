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
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.networkHandler;

public class PanicMode extends ToggledHack {
    int panicFlyingTimer = 0;

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
        if (client.player == null || client.world == null) return;

        if (panicFlyingTimer > 0) {  // TODO: make this code simpler
            panicFlyingTimer--;
            for (int i = 0; i < 5; i++) {  // Max 5 packets per tick
                Vec3d pos = client.player.getPos().add(0, 10, 0);  // Max 10 blocks per packet
                client.player.setVelocity(0, 0, 0);
                if (client.player.getVehicle() != null) {
                    ClipCommand.moveVehicleTo(client.player.getVehicle(), pos);
                } else {
                    client.player.setPosition(pos);
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                }
            }
            if (panicFlyingTimer == 0) {  // If end, disconnect
                client.world.disconnect();
                client.disconnect(new DisconnectedScreen(null, Text.of(PREFIX), Text.of("Panic Mode: §cTriggered")));
            }
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
        this.enabled = false;
        panicFlyingTimer = 20;  // 20 ticks = 1 second
        message("§cTriggered§r, flying up...");
    }
}
