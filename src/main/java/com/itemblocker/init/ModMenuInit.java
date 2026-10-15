package com.itemblocker.init;

import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.screen.BlockedItemsScreen;
import com.itemblocker.client.screen.LeftAlignedScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuInit implements ModMenuApi  {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        BlockedItemsConfig.load();
        if (BlockedItemsConfig.getAlignmentMode() == AlignmentMode.CENTER_ALIGNED) {
            return BlockedItemsScreen::new;
        }
        else {
            return LeftAlignedScreen::new;
        }
    }

}
