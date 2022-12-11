package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.ClientPlayerInteractionManagerMixin;
import com.jorianwoltjer.liveoverflowmod.mixin.MinecraftClientMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Reach extends ToggledHack {
    /**
     * Hit enitities from far away
     * @see MinecraftClientMixin
     * @see ClientPlayerInteractionManagerMixin
     */
    public Reach() {
        super("Reach", GLFW.GLFW_KEY_BACKSLASH);
    }
}
