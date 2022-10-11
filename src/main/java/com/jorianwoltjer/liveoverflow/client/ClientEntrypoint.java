package com.jorianwoltjer.liveoverflow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keybinds.registerKeybinds();  // Register keybinds
        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::checkKeybinds);  // Register a callback to be called every tick
    }
}
