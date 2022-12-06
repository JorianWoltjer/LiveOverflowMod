package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.command.ClipCommand;
import com.jorianwoltjer.liveoverflowmod.mixin.ClientPlayNetworkHandlerMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

public class PanicMode extends ToggledHack {
    final double MAX_DELTA = 10;
    boolean panicActive = false;
    PanicReason reason;

    /**
     * Fly up and disconnect if the player is in a dangerous situation.
     * - If another player is in render distance
     * - If health goes down
     * @see ClientPlayNetworkHandlerMixin
     */
    public PanicMode() {
        super("Panic", GLFW.GLFW_KEY_COMMA);
    }

    @Override
    public void tick(MinecraftClient client) {
        super.tick(client);
        if (client.world == null) return;

        if (panicActive && packetQueue.size() == 0) {  // If ended, disconnect
            panicActive = false;  // Disable for next join

            StringBuilder message = new StringBuilder("Panic Mode: §cTriggered§r\n");
            switch (reason.reason) {
                case PLAYER_NEARBY -> message.append(String.format("§7Player §a%s §7was nearby", reason.player.getName().getString()));
                case DAMAGE -> message.append("§7You took damage");
            }

            client.world.disconnect();
            client.disconnect(new DisconnectedScreen(null, Text.of(PREFIX), Text.of(message.toString())));
        }
    }

    @Override
    public void tickEnabled() {
        if (client.world == null) return;

        List<AbstractClientPlayerEntity> players = client.world.getPlayers();
        if (players.size() > 1) {
            AbstractClientPlayerEntity nearby = players.get(1);
            triggerPanic(new PanicReason(PanicReason.Reason.PLAYER_NEARBY, nearby, (double) nearby.distanceTo(client.player)));
        }
    }

    public void triggerPanic(PanicReason reason) {
        if (client.player == null) return;

        this.reason = reason;
        this.enabled = false;  // Don't trigger repeatedly
        panicActive = true;
        message("§cTriggered§r, flying up...");

        // Fly up 1000 blocks
        for (int i = 0; i < 1000/MAX_DELTA; i++) {
            Vec3d pos = client.player.getPos().add(0, MAX_DELTA, 0);  // Max 10 blocks per packet

            if (client.player.getVehicle() != null) {  // If in boat
                moveVehicleTo(client.player.getVehicle(), pos);
            } else {
                client.player.setPosition(pos);
                packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
            }
        }

    }

    public static class PanicReason {
        public Reason reason;
        public Double distance;
        public PlayerEntity player;

        public PanicReason(Reason reason, PlayerEntity nearbyPlayer, Double distance) {
            this.reason = reason;
            this.distance = distance;
            this.player = nearbyPlayer;
        }

        public enum Reason {
            PLAYER_NEARBY,
            DAMAGE
        }
    }

    public static void moveVehicleTo(Entity vehicle, Vec3d pos) {
        vehicle.updatePosition(pos.x, pos.y, pos.z);
        packetQueue.add(new VehicleMoveC2SPacket(vehicle));
    }
}
