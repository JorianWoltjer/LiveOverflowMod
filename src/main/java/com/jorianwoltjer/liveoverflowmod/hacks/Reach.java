package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.MinecraftClientMixin;
import org.lwjgl.glfw.GLFW;

public class Reach extends ToggledHack {
    /**
     * Hit enitities from far away
     * @see MinecraftClientMixin
     */
    public Reach() {
        super("Reach", GLFW.GLFW_KEY_BACKSLASH);
    }
}
