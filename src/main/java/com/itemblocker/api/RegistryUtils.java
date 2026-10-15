package com.itemblocker.api;

import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.DataPackReloadCookie;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContextMapper;
import net.minecraft.commands.Commands;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class RegistryUtils {

    private static final Minecraft minecraft = Minecraft.getInstance();
    public static boolean isRegistryLoaded = false;

    private static final Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings = (context) -> new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(context.datapackWorldRegistries()));
    private static final WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration());


    private static WorldLoader.InitConfig createDefaultLoadConfig(final PackRepository packRepository, final WorldDataConfiguration config) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, config, false, true);
        return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
    }

    public static void loadRegistry() {
        long start = Util.getMillis();
        PackRepository vanillaOnlyPackRepository = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
        WorldDataConfiguration dataConfig = SharedConstants.IS_RUNNING_IN_IDE ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "tests"), List.of()), FeatureFlags.DEFAULT_FLAGS) : WorldDataConfiguration.DEFAULT;
        WorldLoader.InitConfig loadConfig = createDefaultLoadConfig(vanillaOnlyPackRepository, dataConfig);
        CompletableFuture<WorldCreationContext> loadResult = WorldLoader.load(loadConfig, (context) -> new WorldLoader.DataLoadOutput(new DataPackReloadCookie((WorldGenSettings)worldGenSettings.apply(context), context.dataConfiguration()), context.datapackDimensions()), (resources, managers, registries, cookie) -> {
            resources.close();
            return worldCreationContext.apply(managers, registries, (DataPackReloadCookie) cookie);
        }, Util.backgroundExecutor(), minecraft);
        Objects.requireNonNull(loadResult);
        minecraft.managedBlock(loadResult::isDone);
        long end = Util.getMillis();
        long total = end - start;

        if (ItemBlockerClient.isDev) {
            Loggers.CLIENT_LOGGER.info("Data config: {}", dataConfig);
            Loggers.CLIENT_LOGGER.info("Load config: {}", loadConfig);
            Loggers.CLIENT_LOGGER.info("Load result: {}", loadResult);
        }
        isRegistryLoaded = true;

        Loggers.CLIENT_LOGGER.info("Took {}ms to load registries.", total);
    }
}
