package com.itemblocker.client;

import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.enums.PickupMode;
import com.itemblocker.api.keymapping.KeyHandler;
import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.screen.AbstractItemWidget;
import com.itemblocker.api.screen.misc.TextSupplier;
import com.itemblocker.client.config.BlockedItemsConfig;
/* import fuzs.puzzleslib.common.api.client.event.v1.ClientLifecycleEvents;
import fuzs.puzzleslib.common.api.client.event.v1.ClientTickEvents;
import fuzs.puzzleslib.common.api.client.event.v1.entity.ClientEntityEvents;

 */
import com.sun.jna.Native;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.*;


public class ItemBlockerClient implements ClientModInitializer {

    private static final FabricLoader instance = FabricLoader.getInstance();
    private final boolean isPickupNotifInstalled = instance.isModLoaded("pickupnotifications");
    private final boolean isPickupNotifierInstalled = instance.isModLoaded("pickupnotifier");
    public static final boolean isModMenuInstalled = instance.isModLoaded("modmenu");
    public static boolean isDev = false;
    private static final KeyOptions options = new KeyOptions();
    public static final ArrayList<String> currentModList = new ArrayList<>();
    public static final String MOD_ID = "itemblocker";
    public static final ModContainer itemBlockerInstance = instance.getModContainer(MOD_ID).get();
    public static final String MOD_VERSION = "v" + itemBlockerInstance.getMetadata().getVersion().getFriendlyString();
    public static KeyHandler handler;

    @Override
    public void onInitializeClient() {
        Loggers.CLIENT_LOGGER.info("Item Blocker initializing...");
        isDev = BlockedItemsConfig.devFlagsEnabled();
        long start = Util.getMillis();
        handler = new KeyHandler(Minecraft.getInstance(), options);

        if (isPickupNotifInstalled) {
            Loggers.CLIENT_LOGGER.info("-----------------------------------------------------------------------------------------------------");
            Loggers.CLIENT_LOGGER.info("Pickup Notifications is installed, using ItemPickupMixin to cancel notifications.");
            Loggers.CLIENT_LOGGER.info("If an injection error occurs about it, the method may have changed or the mod isn't loaded properly.");
            Loggers.CLIENT_LOGGER.info("-----------------------------------------------------------------------------------------------------");
        }

        if (isPickupNotifierInstalled) {
            Loggers.CLIENT_LOGGER.info("-----------------------------------------------------------------------------------------------------");
            Loggers.CLIENT_LOGGER.info("Pickup Notifier is installed, using AddEntriesHandlerMixin to cancel notifications.");
            Loggers.CLIENT_LOGGER.info("If an injection error occurs about it, the method may have changed or the mod isn't loaded properly.");
            Loggers.CLIENT_LOGGER.info("-----------------------------------------------------------------------------------------------------");
        }



        Collection<ModContainer> modList = FabricLoader.getInstance().getAllMods();
        for (ModContainer mod : modList) {
            boolean addMod = this.isAddMod(mod);
            if (addMod) {
                currentModList.add(mod.getMetadata().getId());
            }
        }

        long end = Util.getMillis();
        long total = end - start;


        Loggers.CLIENT_LOGGER.info("Client initialized in {}ms", total);
    }

    public static void tick(Minecraft minecraft) {
        handler.handleKeyMappings();
    }

    public static boolean isUsingClientMode() {
        return BlockedItemsConfig.getPickupMode() == PickupMode.CLIENT_ONLY;
    }

    public static KeyOptions getOptions() {
        return options;
    }

    public static boolean isUsingIDMode() {
        return BlockedItemsConfig.getItemIdMode() == ItemIDMode.USE_ITEM_ID;
    }

    public static boolean isCenterAligned() {
        return BlockedItemsConfig.getAlignmentMode() == AlignmentMode.CENTER_ALIGNED;
    }


    private boolean isAddMod(ModContainer mod) {
        boolean addMod = true;
        if (mod.getMetadata().getId().contains("lib")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().contains("fabric-")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().equals("fabricloader")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().contains("mixin")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().equals("java")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().equals("minecraft")) {
            addMod = false;
        }
        else if (mod.getMetadata().getId().equals("modmenu")) {
            addMod = false;
        }
        else if (mod.getMetadata().getName().contains("lib")) {
            addMod = false;
        }
        else if (mod.getMetadata().getName().contains("api")) {
            addMod = false;
        }
        return addMod;
    }

}
