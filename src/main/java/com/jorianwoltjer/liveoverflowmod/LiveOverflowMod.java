package com.jorianwoltjer.liveoverflowmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveOverflowMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("liveoverflowmod");
    public static final String PREFIX = "§8[§c⬤§7 §7LiveOverflowMod§8] §r";

    @Override
    public void onInitialize() {
        LOGGER.info("Successfully loaded LiveOverflowMod");
    }
}
