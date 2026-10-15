package com.itemblocker.api.enums;

public enum MouseClick {

    LEFT_CLICK(1),
    MIDDLE_CLICK(2),
    RIGHT_CLICK(3);

    private final int value;

    MouseClick(final int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

}
