package com.itemblocker.api.screen.misc;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;


/**
 * Probably going to be used for a popup overhaul in the next update.
 * I figured this would be the best way to dynamically add text without abstraction.
 * Not sure, how to make abstract popups yet though.
 * <br>
 * <br>
 * TODO: Implement abstract popups.
 */
public class TextSupplier {


    private final ArrayList<TextCoordinate> textList = new ArrayList<>();

    public void addText(String text, int x, int y) {
        this.addText(text, x, y, 0xFFFFFFFF);
    }

    public void addText(Component text, int x, int y) {
        this.addText(text, x, y, 0xFFFFFFFF);
    }

    public void addText(String text, int x, int y, int color) {
        this.addTextToList(new TextCoordinate(text, x, y, color));
    }

    public void addText(Component text, int x, int y, int color) {
        this.addText(text.getString(), x, y, color);
    }


    public void removeText(TextCoordinate coordinate) {
        this.textList.remove(coordinate);
    }

    public void removeText(Component text, int x, int y, int color) {
        this.removeText(text.getString(), x, y, color);
    }

    public void removeText(String text, int x, int y, int color) {
        this.removeText(new TextCoordinate(text, x, y, color));
    }

    public void removeText(Component text, int x, int y) {
        this.removeText(text, x, y, 0xFFFFFFFF);
    }

    public void removeText(String text, int x, int y) {
        this.removeText(text, x, y, 0xFFFFFFFF);
    }


    private void addTextToList(TextCoordinate coordinate) {
        this.textList.add(coordinate);
    }

    public ArrayList<TextCoordinate> textList() {
        return textList;
    }

    public static TextSupplier getInstance() {
        return new TextSupplier();
    };
}
