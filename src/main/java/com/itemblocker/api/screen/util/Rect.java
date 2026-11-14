package com.itemblocker.api.screen.util;

import net.minecraft.client.gui.components.AbstractWidget;


public record Rect(int x, int y, int w, int h) {
    public static Rect of(AbstractWidget widget) {
        return new Rect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }

}
