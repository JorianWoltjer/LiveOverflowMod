package com.jorianwoltjer.liveoverflowmod.client;

import com.jorianwoltjer.liveoverflowmod.hacks.ToggledHack;
import net.minecraft.client.gui.DrawContext;


import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.toggledHacks;

public class Gui {


    public static void render(DrawContext context, float tickDelta) {
        // Show all hacks in the bottom right corner
        for (int i = 0; i < toggledHacks.length; i++) {
            ToggledHack hack = toggledHacks[toggledHacks.length - i - 1];
            String text = String.format("§8[§7%s§8] §r%s§7:§r %s",
                    hack.keybind.getBoundKeyLocalizedText().getString(),
                    hack.name,
                    hack.isEnabled() ? "§aON" : "§cOFF"
            );
            renderTextShadow(context, text, i);
        }
        // Show title
        renderTextShadow(context, "§8[§7LiveOverflowMod §c⬤§8]", toggledHacks.length);
    }

    private static void renderTextShadow(DrawContext context, String text, float index) {
        int x = client.getWindow().getScaledWidth() - client.textRenderer.getWidth(text) - 2;
        int y = client.getWindow().getScaledHeight() - 12 - (int)(index * 11);

        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
    }
}
