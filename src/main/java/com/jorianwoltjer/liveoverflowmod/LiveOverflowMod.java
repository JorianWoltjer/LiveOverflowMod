package com.jorianwoltjer.liveoverflowmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveOverflowMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("liveoverflowmod");

    @Override
    public void onInitialize() {
        LOGGER.info("Successfully loaded LiveOverflowMod");
    }

}
