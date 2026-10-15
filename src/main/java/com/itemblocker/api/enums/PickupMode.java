package com.itemblocker.api.enums;

import net.minecraft.network.chat.Component;

public enum PickupMode {
    CLIENT_ONLY(Component.translatable("itemblocker.text.client")),
    SERVER_SIDE(Component.translatable("itemblocker.text.server"));

    private final Component displayName;

    PickupMode(Component displayName) {
        this.displayName = displayName;
    }

    public Component getDisplayText() {
        return this.displayName;
    }
}
