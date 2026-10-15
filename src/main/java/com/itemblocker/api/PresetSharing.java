package com.itemblocker.api;


import com.itemblocker.api.logging.InvalidPresetCodeException;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.itemblocker.client.config.PresetsConfig;
import org.apache.commons.codec.binary.Base64;
import java.util.List;

public abstract class PresetSharing {

    private static final String SEPARATOR = "|";
    private static final String ITEM_SEPARATOR = ",";
    private static final String PREFIX = "IB:";
    private static final Base64 base64 = Base64.builder().get();

    /**
     * Encodes a preset into a shareable string.
     */
    public static ExportedPreset exportPreset(String name) {
        List<String> items = PresetsConfig.getPreset(name);
        String raw = PREFIX + name + SEPARATOR + String.join(ITEM_SEPARATOR, items);
        String encodedString = base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        if (ItemBlockerClient.isDev) {
            Loggers.PRESETS_LOGGER.info("Encoded Base 64 preset code is {}", encodedString);
        }
        return new ExportedPreset(encodedString, raw);
    }

    /**
     * Decodes a shareable string back into a preset.
     * Returns null if the string is invalid.
     */
    public static ImportedPreset importPreset(String code) {
        try {
            byte[] decoded;
            try {
                decoded = base64.decode(code.trim());
            } catch (Exception e) {
                throw new InvalidPresetCodeException("Failed to import preset code, invalid Base64 string: " + code);
            }
            String raw = new String(decoded, StandardCharsets.UTF_8);

            if (ItemBlockerClient.isDev) {
                Loggers.PRESETS_LOGGER.info("Raw decoded preset code is {}", raw);
            }

            if (!raw.startsWith(PREFIX)) {
                return null;
            }

            raw = raw.substring(PREFIX.length());
            int separatorIndex = raw.indexOf(SEPARATOR);
            if (separatorIndex == -1) {
                return null;
            }

            String name = raw.substring(0, separatorIndex);
            String itemsPart = raw.substring(separatorIndex + 1);

            List<String> items = new ArrayList<>();
            if (!itemsPart.isEmpty()) {
                for (String item : itemsPart.split(ITEM_SEPARATOR)) {
                    if (!item.isBlank()) {
                        items.add(item.trim());
                    }
                }
            }

            if (name.isBlank()) {
                return null;
            }

            return new ImportedPreset(name, items);
        } catch (InvalidPresetCodeException e){
            Loggers.PRESETS_LOGGER.error(e.getMessage());
            return null;
        }
    }

    public record ImportedPreset(String name, List<String> items) {
    }

    /**
     *
     * @param exportCode A base64 String which contains the preset and item names.
     * @param decodedString An exported preset also contains it's decoded code for debugging purposes
     */
    public record ExportedPreset(String exportCode, String decodedString) {
    }
}