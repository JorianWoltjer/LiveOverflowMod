package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Accessor("overlayMessage")
    Text _overlayMessage();

    @Accessor("overlayRemaining")
    int _overlayRemaining();
}
