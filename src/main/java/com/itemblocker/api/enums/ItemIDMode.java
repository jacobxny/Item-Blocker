package com.itemblocker.api.enums;

import net.minecraft.network.chat.Component;

public enum ItemIDMode {
    USE_ITEM_ID(Component.translatable("itemblocker.text.item_ids")),
    USE_ITEM_NAMES(Component.translatable("itemblocker.text.item_names"));

    private final Component displayName;

    ItemIDMode(Component displayName) {
        this.displayName = displayName;
    }

    public Component getDisplayText()  {
        return this.displayName;
    }
}
