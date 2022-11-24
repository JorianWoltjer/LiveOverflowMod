package com.jorianwoltjer.liveoverflowmod.client;

import com.jorianwoltjer.liveoverflowmod.command.*;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.command.CommandRegistryAccess;

public class ClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keybinds.registerKeybinds();  // Register keybinds
        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::checkKeybinds);  // Register a callback to be called every tick

        ClientCommandRegistrationCallback.EVENT.register(ClientEntrypoint::registerCommands);
    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        ClipCommand.register(dispatcher);
    }
}
