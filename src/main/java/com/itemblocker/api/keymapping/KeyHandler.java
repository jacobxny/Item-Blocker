package com.itemblocker.api.keymapping;

import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.raycasting.RaycastUtils;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.client.screen.BlockedItemsScreen;
import com.itemblocker.client.screen.LeftAlignedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.logging.Logger;


@SuppressWarnings("unused")
public class KeyHandler {

    private final Minecraft minecraft;

    private final KeyOptions options;

    public KeyHandler(Minecraft minecraft, KeyOptions options) {
        this.minecraft = minecraft;
        this.options = options;
    }

    public KeyHandler(KeyOptions options) {
        this(Minecraft.getInstance(), options);
    }

    public KeyOptions getOptions() {
        return this.options;
    }

    public void handleKeyMappings() {

        if (options.openConfigKey.consumeClick()) {
            if (minecraft.gui.screen() == null) {
                if (BlockedItemsConfig.getAlignmentMode() == AlignmentMode.CENTER_ALIGNED) {
                    minecraft.gui.setScreen(new BlockedItemsScreen(null));
                }
                else {
                    minecraft.gui.setScreen(new LeftAlignedScreen(null));
                }
            }
        }

        if (options.addHeldItemKey.consumeClick()) {
            ItemStack heldItem = ItemUtil.getHeldItem(minecraft);
            RaycastUtils.sendOverlay(heldItem, BlockedItemsConfig.isBlocked(heldItem));
        }

        if (BlockedItemsConfig.devFlagsEnabled()) {
            if (options.loadConfigKey.consumeClick()) {
                BlockedItemsConfig.load();
                PresetsConfig.load();
                Loggers.CLIENT_LOGGER.info("Loaded configs!");
            }
        }


        if (options.toggleFilterKey.consumeClick()) {
            if (options.isToggled()) {
                minecraft.player.sendOverlayMessage(Component.literal("Filter is now:§c Disabled "));
                options.setToggled(false);
            } else {
                minecraft.player.sendOverlayMessage(Component.literal("Filter is now:§a Enabled "));
                options.setToggled(true);
            }
        }

        if (options.addBlockFromViewKey.consumeClick()) {
            RaycastUtils.raycast();
        }


        options.setIsTemporaryAllowPressed(options.temporaryBypassKey.isDown());

        if (options.isTemporaryAllowPressed() && minecraft.player != null) {
            Component key = options.temporaryBypassKey.getTranslatedKeyMessage();
            String stringKey = key.getString();
            minecraft.player.sendOverlayMessage(Component.literal("Filter is temporarily disabled while§e " + stringKey + "§r is held"));
        }


        if (!options.isTemporaryAllowPressed()) {
            DroppingUtils.iterateInventory();
        }
    }

}
