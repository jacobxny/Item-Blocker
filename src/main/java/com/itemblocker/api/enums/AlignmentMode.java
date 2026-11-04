package com.itemblocker.api.enums;

import net.minecraft.network.chat.Component;

public enum AlignmentMode {
    CENTER_ALIGNED(Component.translatable("itemblocker.text.center_aligned")),
    LEFT_ALIGNED(Component.translatable("itemblocker.text.left_aligned"));

    private final Component displayName;

    AlignmentMode(Component displayName) {
        this.displayName = displayName;
    }

    public Component getDisplayText() {
        return displayName;
    }
}
