package org.grill.servlinker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class Servlinker implements ModInitializer {

    public static final String MOD_ID = "ServLinker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	
    @Override
    public void onInitialize() {
        
        LOGGER.info("Hello, World!");
    }
}
