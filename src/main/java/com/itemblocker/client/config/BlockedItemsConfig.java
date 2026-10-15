package com.itemblocker.client.config;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.enums.PickupMode;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.init.ConfigInit;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public abstract class BlockedItemsConfig {

    private static final Jankson JANKSON = Jankson.builder().build();

    private static final Gson GSON = new Gson();


    private static final File LEGACY_CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "itemblocker.json"
    );

    private static final Path CONFIG_DIR = ConfigInit.CONFIG_DIR;


    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("itemblocker.json5");

    private static Set<String> blockedItems = new HashSet<>();
    private static PickupMode pickupMode = PickupMode.CLIENT_ONLY;
    private static AlignmentMode alignmentMode = AlignmentMode.CENTER_ALIGNED;
    private static boolean useItemIds = false;

    private static boolean loadRegistryOnLaunch = true;
    private static boolean enableDevFlags = FabricLoader.getInstance().isDevelopmentEnvironment();
    private static boolean hasDoneTutorial = false;
    private static boolean listItemsAlphabetically = true;



    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            loadJson5();
            return;
        }

        if (migrateFromJson()) {
            save();
            return;
        }

        save();
    }

    private static void loadJson5() {
        try {
            JsonObject json = JANKSON.load(CONFIG_PATH.toFile());
            ConfigData data = JANKSON.fromJson(json, ConfigData.class);
            if (data != null) {
                if (data.blockedItems != null) {
                    blockedItems = new HashSet<>(data.blockedItems);
                    blockedItems = blockedItems.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
                }
                if (data.pickupMode != null) {
                    try {
                        pickupMode = PickupMode.valueOf(data.pickupMode);
                    } catch (IllegalArgumentException e) {
                        Loggers.CLIENT_LOGGER.warn("Invalid pickup mode in config, using default");
                    }
                }
                if (data.alignmentMode != null) {
                    try {
                        alignmentMode = AlignmentMode.valueOf(data.alignmentMode);
                    } catch (IllegalArgumentException e) {
                        Loggers.CLIENT_LOGGER.warn("Invalid alignment mode in config, using default");
                    }
                }
                useItemIds = data.useItemIds;
                loadRegistryOnLaunch = data.loadRegistryOnLaunch;
                enableDevFlags = data.enableDevFlags;
                hasDoneTutorial = data.hasDoneTutorial;
                listItemsAlphabetically = data.listItemsAlphabetically;
            }
        } catch (Exception e) {
            Loggers.CLIENT_LOGGER.error("Failed to load config", e);
        }
    }

    private static boolean migrateFromJson() {
        Path newLocationJson = CONFIG_DIR.resolve("itemblocker.json");
        Path legacyJson = LEGACY_CONFIG_FILE.toPath();

        Path jsonPath;
        if (Files.exists(newLocationJson)) {
            jsonPath = newLocationJson;
        } else if (Files.exists(legacyJson)) {
            jsonPath = legacyJson;
        } else {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            LegacyConfigData data = GSON.fromJson(reader, LegacyConfigData.class);
            if (data == null) {
                return false;
            }

            if (data.blockedItems != null) {
                blockedItems = new HashSet<>(data.blockedItems);
            }
            if (data.pickupMode != null) {
                try {
                    pickupMode = PickupMode.valueOf(data.pickupMode);
                } catch (IllegalArgumentException e) {
                    Loggers.CLIENT_LOGGER.warn("Invalid pickup mode in legacy config, using default");
                }
            }
            if (data.alignmentMode != null) {
                try {
                    alignmentMode = AlignmentMode.valueOf(data.alignmentMode);
                } catch (IllegalArgumentException e) {
                    Loggers.CLIENT_LOGGER.warn("Invalid alignment mode in legacy config, using default");
                }
            }
            useItemIds = data.useItemIds;
            loadRegistryOnLaunch = data.loadRegistryOnLaunch;

            Loggers.CLIENT_LOGGER.info("Migrated itemblocker config from JSON to JSON5");
        } catch (Exception e) {
            Loggers.CLIENT_LOGGER.error("Failed to migrate legacy JSON config", e);
            return false;
        }

        try {
            Files.deleteIfExists(newLocationJson);
            Files.deleteIfExists(legacyJson);
        } catch (Exception e) {
            Loggers.CLIENT_LOGGER.warn("Failed to delete legacy JSON config after migration", e);
        }
        return true;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);

            ConfigData data = new ConfigData();
            data.blockedItems = new ArrayList<>(blockedItems);
            Collections.sort(data.blockedItems);
            data.pickupMode = pickupMode.name();
            data.alignmentMode = alignmentMode.name();
            data.useItemIds = useItemIds;
            data.loadRegistryOnLaunch = loadRegistryOnLaunch;
            data.enableDevFlags = enableDevFlags;
            data.hasDoneTutorial = hasDoneTutorial;
            data.listItemsAlphabetically = listItemsAlphabetically;

            String json5 = JANKSON.toJson(data).toJson(true, true);
            Files.writeString(CONFIG_PATH, json5);

        } catch (Exception e) {
            Loggers.CLIENT_LOGGER.error("Failed to save config", e);
        }
    }




    public static boolean isBlocked(@NotNull ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = id.toString();
        return isBlocked(itemId);
    }

    public static boolean isBlocked(@NotNull String itemId) {
        return blockedItems.contains(itemId);
    }

    public static boolean shouldLoadRegistryOnLaunch() {
        return loadRegistryOnLaunch;
    }

    public static boolean devFlagsEnabled() {
        return enableDevFlags;
    }


    public static AlignmentMode getAlignmentMode() {
        return alignmentMode;
    }

    public static void setAlignmentMode(AlignmentMode mode) {
        alignmentMode = mode;
        save();
    }

    public static ItemIDMode getItemIdMode() {
        return useItemIds ? ItemIDMode.USE_ITEM_ID : ItemIDMode.USE_ITEM_NAMES;
    }

    public static void setUseItemIds(ItemIDMode mode) {
        useItemIds = (mode == ItemIDMode.USE_ITEM_ID);
        save();
    }

    public static boolean hasDoneTutorial() {
        return hasDoneTutorial;
    }

    public static void finishTutorial() {
        hasDoneTutorial = true;
        save();
    }


    public static void addBlockedItem(ItemStack itemStack) {
        String itemId = ItemUtil.getItemDetails(itemStack, ItemIDMode.USE_ITEM_ID);
        if (Minecraft.getInstance().player != null) {
            ItemUtil.setItemXPos(Minecraft.getInstance().player.getX());
            ItemUtil.setItemYPos(Minecraft.getInstance().player.getY());
            ItemUtil.setItemZPos(Minecraft.getInstance().player.getZ());
        }
        addBlockedItem(itemId);
    }

    public static void addBlockedItem(String itemId) {
        blockedItems.add(itemId);
        blockedItems = sortItems((HashSet<String>) blockedItems);
        save();
    }

    public static void removeBlockedItem(String itemId) {
        blockedItems.remove(itemId);
        blockedItems = sortItems((HashSet<String>) blockedItems);
        save();
    }

    public static @NonNull HashSet<String> getBlockedItems() {
        HashSet<String> blockedItemsSet = new HashSet<>(blockedItems);
        blockedItemsSet = sortItems(blockedItemsSet);
        return blockedItemsSet;
    }

    public static PickupMode getPickupMode() {
        return pickupMode;
    }

    public static void setPickupMode(PickupMode mode) {
        pickupMode = mode;
        save();
    }

    public static boolean listItemsAlphabetically() {
        return listItemsAlphabetically;
    }

    private static class ConfigData {
        @Comment("Item IDs (e.g. \"minecraft:dirt\") that are currently blocked.")
        public List<String> blockedItems = new ArrayList<>();

        @Comment("How blocked items are handled when picked up.\nOptions: CLIENT_ONLY, SERVER_SIDE")
        public String pickupMode = PickupMode.CLIENT_ONLY.name();

        @Comment("Alignment used when rendering the main blocked items screen.\nOptions: CENTER_ALIGNED, LEFT_ALIGNED")
        public String alignmentMode = AlignmentMode.CENTER_ALIGNED.name();

        @Comment("If true, items are referred to by their ID instead of display name.")
        public boolean useItemIds = false;

        @Comment("Preload the item registry on launch.\nDefaults to true — disabling this causes the main config screen to stutter/freeze on first open.")
        public boolean loadRegistryOnLaunch = true;

        @Comment("Enables development features used in testing to test experimental features. \nMay have excessive logging if enabled. \nRequires a restart to take effect.")
        public boolean enableDevFlags = false;

        public boolean hasDoneTutorial = false;

        public boolean listItemsAlphabetically = true;
    }


    private static class LegacyConfigData {
        List<String> blockedItems = new ArrayList<>();
        String pickupMode = PickupMode.CLIENT_ONLY.name();
        String alignmentMode = AlignmentMode.CENTER_ALIGNED.name();
        boolean useItemIds = false;
        boolean loadRegistryOnLaunch = true;
    }

    public static HashSet<String> sortItems(HashSet<String> set) {
        if (listItemsAlphabetically) {
            return set.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        }
        else {
            return set;
        }
    }

}