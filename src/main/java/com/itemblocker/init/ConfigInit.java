package com.itemblocker.init;

import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.config.PresetsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ConfigInit implements ClientModInitializer {

    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("itemblocker");


    @Override
    public void onInitializeClient() {
        BlockedItemsConfig.load();
        BlockedItemsConfig.save();
        PresetsConfig.load();
        PresetsConfig.save();
    }
}
