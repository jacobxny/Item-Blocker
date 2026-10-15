package com.itemblocker.api.screen.misc;


import org.jetbrains.annotations.Contract;

@SuppressWarnings("unused")
public class Colors {

    private final int color;
    private final int hoveredColor;

    public static final Colors RED = new Colors(0xFFFF5555, 0xFFAA3333);
    public static final Colors BLUE = new Colors(0xFF5555FF, 0xFF3333AA);
    public static final Colors ORANGE = new Colors(0xFFFFAA00, 0xFFAA7700);
    public static final Colors GREEN = new Colors(0xFF55FF55, 0xFF33AA33);
    public static final int BLACK = new Colors(0xFF000000).color(false);
    public static final int WHITE = new Colors(0xFFFFFFFF).color(false);


    public Colors(int color, int hoveredColor) {
        this.color = color;
        this.hoveredColor = hoveredColor;
    }


    public Colors(int color) {
        this(color, color);
    }

    public int color(boolean hovered) {
        if (hovered) {
            return hoveredColor;
        }
        return color;
    }

}
