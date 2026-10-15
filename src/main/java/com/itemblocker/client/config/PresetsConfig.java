package com.itemblocker.client.config;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.init.ConfigInit;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class PresetsConfig {

    private static final Jankson JANKSON = Jankson.builder().build();

    private static final Gson GSON = new Gson();


    private static final File LEGACY_PRESETS_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "itemblocker_presets.json"
    );

    private static final Path CONFIG_PATH = ConfigInit.CONFIG_DIR.resolve("itemblocker_presets.json5");

    private static Map<String, List<String>> presets = new HashMap<>();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            loadJson5();
            return;
        }

        if (migrateFromJson()) {
            save();
        }
    }

    private static void loadJson5() {
        try {
            JsonObject json = JANKSON.load(CONFIG_PATH.toFile());
            PresetsData data = JANKSON.fromJson(json, PresetsData.class);
            if (data != null && data.presets != null) {
                presets = new HashMap<>();

                for (Map.Entry<String, List<String>> entry : data.presets.entrySet()) {
                    List<String> cleanList = new ArrayList<>();
                    if (entry.getValue() != null) {
                        for (Object rawItem : entry.getValue()) {
                            if (rawItem != null) {
                                cleanList.add(String.valueOf(rawItem).replace("\"", ""));
                            }
                        }
                    }
                    presets.put(entry.getKey(), cleanList);
                }
            }
        } catch (Exception e) {
            Loggers.PRESETS_LOGGER.error("Failed to load presets", e);
        }
    }

    private static boolean migrateFromJson() {
        Path newLocationJson = ConfigInit.CONFIG_DIR.resolve("itemblocker_presets.json");
        Path legacyJson = LEGACY_PRESETS_FILE.toPath();

        Path jsonPath;
        if (Files.exists(newLocationJson)) {
            jsonPath = newLocationJson;
        } else if (Files.exists(legacyJson)) {
            jsonPath = legacyJson;
        } else {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            LegacyPresetsData data = GSON.fromJson(reader, LegacyPresetsData.class);
            if (data == null || data.presets == null) {
                return false;
            }
            presets = data.presets;
            Loggers.PRESETS_LOGGER.info("Migrated presets from JSON to JSON5");
        } catch (Exception e) {
            Loggers.PRESETS_LOGGER.error("Failed to migrate legacy JSON presets", e);
            return false;
        }

        try {
            Files.deleteIfExists(newLocationJson);
            Files.deleteIfExists(legacyJson);
        } catch (Exception e) {
            Loggers.PRESETS_LOGGER.warn("Failed to delete legacy JSON presets after migration", e);
        }
        return true;
    }

    public static void save() {
        try {
            Files.createDirectories(ConfigInit.CONFIG_DIR);

            PresetsData data = new PresetsData();
            data.presets = new HashMap<>(presets);

            String json5 = JANKSON.toJson(data).toJson(true, true);
            Files.writeString(CONFIG_PATH, json5);

        } catch (Exception e) {
            Loggers.PRESETS_LOGGER.error("Failed to save presets", e);
        }
    }


    public static void createPreset(String name) {
        if (!presets.containsKey(name)) {
            presets.put(name, new ArrayList<>());
            save();
        }
    }

    public static void deletePreset(String name) {
        presets.remove(name);
        save();
    }

    public static void renamePreset(String oldName, String newName) {
        if (!presets.containsKey(oldName) || newName == null || newName.isBlank()) return;
        if (presets.containsKey(newName)) return;

        List<String> items = presets.get(oldName);
        presets.put(newName, items);
        presets.remove(oldName);
        save();
    }

    public static void addItemToPreset(String presetName, String itemId) {
        List<String> items = presets.get(presetName);
        if (items != null && !items.contains(itemId)) {
            items.add(itemId);
            Collections.sort(items);
            save();
        }
    }

    public static void removeItemFromPreset(String presetName, String itemId) {
        List<String> items = presets.get(presetName);
        if (items != null) {
            items.remove(itemId);
            Collections.sort(items);
            save();
        }
    }

    public static ArrayList<String> getPreset(String name) {
        List<String> rawItems = presets.getOrDefault(name, new ArrayList<>());
        ArrayList<String> cleanList = new ArrayList<>();
        for (Object item : rawItems) {
            cleanList.add(String.valueOf(item).replace("\"", ""));
        }
        return cleanList;
    }

    public static HashSet<String> getPresetNames() {
        return new HashSet<>(presets.keySet());
    }

    public static void applyPreset(String name, boolean clearList) {
        List<String> items = presets.get(name);
        if (items != null) {
            if (clearList) {
                for (String itemId : new HashSet<>(BlockedItemsConfig.getBlockedItems())) {
                    BlockedItemsConfig.removeBlockedItem(itemId);
                }
            }
            for (String itemId : items) {
                BlockedItemsConfig.addBlockedItem(itemId);
            }
        }
    }



    private static class PresetsData {
        @Comment("Saved presets. Each key is a preset name mapped to its list of item IDs.")
        public Map<String, List<String>> presets = new HashMap<>();
    }

    private static class LegacyPresetsData {
        Map<String, List<String>> presets = new HashMap<>();
    }

}