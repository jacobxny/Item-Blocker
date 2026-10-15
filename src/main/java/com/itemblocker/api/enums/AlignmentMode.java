package com.itemblocker.api.enums;

import net.minecraft.network.chat.Component;

public enum AlignmentMode {
    CENTER_ALIGNED(Component.literal("Center aligned")),
    LEFT_ALIGNED(Component.literal("Left aligned"));

    private final Component displayName;

    AlignmentMode(Component displayName) {
        this.displayName = displayName;
    }

    public Component getDisplayText() {
        return displayName;
    }
}
