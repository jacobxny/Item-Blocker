package com.itemblocker.api.keymapping;


import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import com.google.common.collect.Lists;
import org.lwjgl.sdl.SDL_KeyboardEvent;

import java.util.List;


public class KeyOptions {

    private static boolean isTemporaryAllowPressed = false;

    private static boolean isEnabled = true;

    public KeyMapping openConfigKey;
    public KeyMapping temporaryBypassKey;
    public KeyMapping addBlockFromViewKey;
    public KeyMapping toggleFilterKey;
    public KeyMapping addHeldItemKey;
    public KeyMapping loadConfigKey;


    private final List<KeyMapping> keyMappings = new ReferenceArrayList<>();

    private KeyMapping[] process(KeyMapping[] keysAll) {
        List<KeyMapping> newKeysAll = Lists.newArrayList(keysAll);
        newKeysAll.removeAll(keyMappings);
        newKeysAll.addAll(keyMappings);
        return newKeysAll.toArray(new KeyMapping[0]);
    }

    public KeyMapping[] registerKeyMappings(KeyMapping[] keys) {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("itemblocker", "category"));

        openConfigKey = new KeyMapping(
                "key.itemblocker.openconfig",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                category
        );
        keyMappings.add(openConfigKey);


        temporaryBypassKey = new KeyMapping(
                "key.itemblocker.temporarybypass",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LSHIFT,
                category
        );

        keyMappings.add(temporaryBypassKey);


        toggleFilterKey = new KeyMapping(
                "key.itemblocker.togglefilter",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_M,
                category
        );

        keyMappings.add(toggleFilterKey);

        addBlockFromViewKey = new KeyMapping(
                "key.itemblocker.addblockfromview",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                category
        );
        keyMappings.add(addBlockFromViewKey);


        addHeldItemKey = new KeyMapping(
                "key.itemblocker.addhelditem",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_SEMICOLON,
                category
        );
        keyMappings.add(addHeldItemKey);

        if (BlockedItemsConfig.devFlagsEnabled()) {
            loadConfigKey = new KeyMapping(
                    "Load Configs",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_F7,
                    category
            );
            keyMappings.add(loadConfigKey);
        }

        return this.process(keys);
    }

    public boolean isTemporaryAllowPressed() {
        return isTemporaryAllowPressed;
    }

    public boolean isToggled() {
        return isEnabled;
    }

    public void setToggled(boolean value) {
        isEnabled = value;
    }

    public void setIsTemporaryAllowPressed(boolean value) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (value) {
            if (player != null) {
                ItemUtil.setItemXPos(player.getX());
                ItemUtil.setItemYPos(player.getY());
                ItemUtil.setItemZPos(player.getZ());
            }
        }
        isTemporaryAllowPressed = value;
    }
}
