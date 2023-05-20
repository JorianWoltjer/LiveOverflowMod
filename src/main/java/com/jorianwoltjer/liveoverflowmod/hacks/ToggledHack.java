package com.jorianwoltjer.liveoverflowmod.hacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.networkHandler;

public abstract class ToggledHack {
    public static final String LIVEOVERFLOW_CATEGORY = "category.liveoverflowmod";

    public final KeyBinding keybind;
    public final String name;
    public boolean defaultEnabled = false;
    public boolean enabled = defaultEnabled;

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
            toggle();
        }
        if (enabled && networkHandler != null) {  // networkHandler = Safety check
            tickEnabled();
        }
    }

    /**
     * Toggle the hack on/off
     */
    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    /**
     * Called every tick, but only when the hack is enabled
     */
    void tickEnabled() {}

    /**
     * Called when the hack is enabled
     */
    public void enable() {
        enabled = true;
        message("§aON");
        LOGGER.info("Enabled " + name);
        onEnable();
    }
    /**
     * Overridable method to add extra functionality when the hack is enabled
     */
    void onEnable() {}

    /**
     * Called when the hack is disabled
     */
    public void disable() {
        enabled = false;
        message("§cOFF");
        LOGGER.info("Disabled " + name);
        onDisable();
    }
    /**
     * Overridable method to add extra functionality when the hack is disabled
     */
    void onDisable() {}

    /**
     * Send a message via the action bar with the prefix
     * @param message The message to send
     */
    public void message(String message) {
        if (client.player == null) return;

        message = "§7" + message.replace("§r", "§7");  // Make white text gray

        client.player.sendMessage(Text.of(PREFIX + name + ": " + message), true);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
