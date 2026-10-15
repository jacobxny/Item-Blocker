package com.itemblocker.api.logging;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Loggers {
    private static final String MOD_ID = "itemblocker";
    private static final String PRESETS_ID = MOD_ID + " (Presets)";
    /**
     * Should only be used when logging server only things. <p></p>
     * Client side logging can be done through the {@code CLIENT_LOGGER}.
     */
    public static final Logger SERVER_LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Should only be used when logging anything to do with presets. <p></p>
     * Like saving or generating an import code.
     */
    public static final Logger PRESETS_LOGGER = LoggerFactory.getLogger(PRESETS_ID);

    /**
     * Should only be used when logging client only things. <p></p>
     * Server logging can be handled through the {@code SERVER_LOOGER}.
     */
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger(MOD_ID);

}
