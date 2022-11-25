package com.jorianwoltjer.liveoverflowmod.hacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;

public abstract class ToggledHack {
    public static final String LIVEOVERFLOW_CATEGORY = "category.liveoverflowmod";

    public final KeyBinding keybind;
    public final String name;
    public boolean enabled = false;

    /**
     * A hack that can be toggled on/off
     * @param name Display name of the hack
     * @param key The GLFW key code of the keybind
     */
    ToggledHack(String name, int key) {
        keybind = new KeyBinding("key.liveoverflowmod." + this.getClass().getSimpleName().toLowerCase() + "_toggle",
                key, LIVEOVERFLOW_CATEGORY);
        this.name = name;
    }

    /**
     * Called every tick
     */
    public void tick(MinecraftClient client) {  // Called every tick
        if (keybind.wasPressed()) {
            enabled = !enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
        if (enabled) {
            tickEnabled();
        }
    }

    /**
     * Called every tick, but only when the hack is enabled
     */
    public void tickEnabled() {}

    /**
     * Called when the hack is enabled
     */
    void onEnable() {
        message("§aEnabled");
    }

    /**
     * Called when the hack is disabled
     */
    void onDisable() {
        message("§cDisabled");
    }

    /**
     * Send a message via the action bar with the prefix
     * @param message The message to send
     */
    public void message(String message) {
        if (client.player == null) return;

        client.player.sendMessage(Text.of(PREFIX + name + ": " + message), true);
    }
}
