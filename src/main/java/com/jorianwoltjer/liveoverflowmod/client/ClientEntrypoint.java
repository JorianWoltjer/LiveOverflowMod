package com.jorianwoltjer.liveoverflowmod.client;

import com.jorianwoltjer.liveoverflowmod.command.*;
import com.jorianwoltjer.liveoverflowmod.hacks.*;
import com.jorianwoltjer.liveoverflowmod.mixin.ClientConnectionInvoker;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.command.ServerCommandSource;

import java.util.LinkedList;

public class ClientEntrypoint implements ClientModInitializer {
    public static final PassiveMods passiveMods = new PassiveMods();
    public static final WorldGuardBypass worldGuardBypassHack = new WorldGuardBypass();
    public static final Reach reachHack = new Reach();
    public static final ClipReach clipReachHack = new ClipReach();
    public static final PanicMode panicModeHack = new PanicMode();
    public static final FastMiner fastMinerHack = new FastMiner();
    public static final ToggledHack[] toggledHacks = new ToggledHack[] {
            passiveMods,
            worldGuardBypassHack,
            reachHack,
            clipReachHack,
            panicModeHack,
            fastMinerHack
    };
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static ClientPlayNetworkHandler networkHandler;
    public static int globalTimer = 0;
    public static final LinkedList<Packet<?>> packetQueue = new LinkedList<>();  // Max 5 per tick

    @Override
    public void onInitializeClient() {
        // Register functions for hacks
        for (ToggledHack hack : toggledHacks) {
            KeyBindingHelper.registerKeyBinding(hack.keybind);  // Keybinds
            ClientTickEvents.END_CLIENT_TICK.register(hack::tick);  // Every tick
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientEntrypoint::tickEnd);  // End of every tick
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerClientCommands(dispatcher));  // Client Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerServerCommands(dispatcher));  // Server Commands

        HudRenderCallback.EVENT.register(Gui::render);  // Render GUI
    }

    public static void tickEnd(MinecraftClient client) {
        // Update variables
        networkHandler = client.getNetworkHandler();
        globalTimer++;

        // Send packets from queue (max 5)
        int movementPacketsLeft = 5;
        while (packetQueue.size() > 0 && movementPacketsLeft > 0) {
            Packet<?> packet = packetQueue.remove(0);
            if (packet instanceof PlayerMoveC2SPacket || packet instanceof VehicleMoveC2SPacket) {
                movementPacketsLeft--;
            }
            ((ClientConnectionInvoker) networkHandler.getConnection())._sendImmediately(packet, null);
        }
    }

    public static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        ClipCommand.register(dispatcher);
    }

    public static void registerServerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        GetCodeCommand.register(dispatcher);
    }
}
