package com.orbitron.companion;

import com.orbitron.companion.chat.ChatListener;
import com.orbitron.companion.registry.ModEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrbitronCompanionMod implements ModInitializer {
    public static final String MOD_ID = "orbitron-companion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Orbitron Companion mod initialised");
        ModEntities.register();

        ChatListener chatListener = new ChatListener();
        chatListener.register();

        PlayerJoinHandler.register();
    }
}
