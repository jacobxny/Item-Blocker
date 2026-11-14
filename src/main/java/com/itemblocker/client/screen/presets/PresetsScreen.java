package com.itemblocker.client.screen.presets;

import com.itemblocker.api.PresetSharing;
import com.itemblocker.api.screen.util.Colors;
import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.screen.presets.edit.EditPresetScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PresetsScreen extends GlobalScreen {

    private static final int SCREEN_MARGIN = 5;

    private static final int TOP_BTN_PAD = 16;
    private static final int TOP_BTN_GAP = 5;

    private static final int LIST_TOP = 60;
    private static final int ROW_H = 30;
    private static final int BTN_H = 20;
    private static final int LIST_MIN_W = 320;
    private static final int LIST_NAME_PAD_LEFT = 10;
    private static final int LIST_PAD_RIGHT = 11;
    private static final int LIST_MIN_NAME_AREA = 150;
    private static final int ROW_BTN_GAP = 5;
    private static final int ROW_BTN_PAD = 4;

    private static final int POPUP_MIN_W = 320;
    private static final int POPUP_PADDING = 16;
    private static final int POPUP_BTN_PAD = 14;
    private static final int POPUP_MIN_BTN_W = 70;
    private static final int POPUP_BTN_GAP = 20;
    private static final int NAME_CAP_W = 150;
    private static final int EXPORT_POPUP_H = 120;
    private static final int DELETE_POPUP_H = 98;

    private List<PresetEntry> presetsList;
    private int scrollOffset = 0;
    private boolean isShiftDown = false;

    private boolean showingExportPopup = false;
    private String exportCode = "";
    private String exportPresetName = "";

    private boolean showingDeletePopup = false;
    private String fullPresetName = "";

    public PresetsScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.manage_presets"), parent);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = this.font.width(ellipsis);
        if (maxWidth <= ellipsisW) {
            return "";
        }
        String result = text;
        while (!result.isEmpty() && this.font.width(result) + ellipsisW > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.isEmpty() && Character.isHighSurrogate(result.charAt(result.length() - 1))) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    private String fitText(Component text, int maxWidth) {
        return fitText(text.getString(), maxWidth);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, Component label, int fillColor) {
        graphics.fill(x, y, x + w, y + BTN_H, fillColor);
        graphics.centeredText(this.font, label, x + w / 2, y + 6, Colors.WHITE);
    }

    private record ButtonPair(int firstX, int firstW, int secondX, int secondW, int totalW) {}

    private ButtonPair layoutButtonPair(Component first, Component second) {
        int w1 = Math.max(POPUP_MIN_BTN_W, this.font.width(first) + POPUP_BTN_PAD);
        int w2 = Math.max(POPUP_MIN_BTN_W, this.font.width(second) + POPUP_BTN_PAD);
        int total = w1 + POPUP_BTN_GAP + w2;
        int startX = this.width / 2 - total / 2;
        return new ButtonPair(startX, w1, startX + w1 + POPUP_BTN_GAP, w2, total);
    }


    private record ListLayout(int left, int width,
                              int nameX, int nameMaxW,
                              int applyX, int applyW, Component applyText,
                              int exportX, int exportW, Component exportText,
                              int editX, int editW, Component editText,
                              int deleteX, int deleteW, Component deleteText) {}

    private ListLayout computeListLayout() {
        Component applyText = Component.translatable("itemblocker.text.apply");
        Component exportText = Component.translatable("itemblocker.text.export");
        Component editText = Component.translatable("itemblocker.text.edit");
        Component deleteText = Component.literal("🗑");

        int applyW = Math.max(38, this.font.width(applyText) + ROW_BTN_PAD);
        int exportW = Math.max(38, this.font.width(exportText) + ROW_BTN_PAD);
        int editW = Math.max(30, this.font.width(editText) + ROW_BTN_PAD);
        int deleteW = Math.max(28, this.font.width(deleteText) + ROW_BTN_PAD);
        int buttonsW = applyW + exportW + editW + deleteW + ROW_BTN_GAP * 3;

        int listW = Math.max(LIST_MIN_W, LIST_NAME_PAD_LEFT + LIST_MIN_NAME_AREA + ROW_BTN_GAP + buttonsW - ROW_BTN_GAP + LIST_PAD_RIGHT);
        listW = Math.min(listW, this.width - SCREEN_MARGIN * 2);

        int left = this.width / 2 - listW / 2 + 5;
        left = Math.clamp(left, SCREEN_MARGIN, this.width - listW - SCREEN_MARGIN);
        int right = left + listW;

        int deleteX = right - LIST_PAD_RIGHT - deleteW;
        int editX = deleteX - ROW_BTN_GAP - editW;
        int exportX = editX - ROW_BTN_GAP - exportW;
        int applyX = exportX - ROW_BTN_GAP - applyW;

        int nameX = left + LIST_NAME_PAD_LEFT;
        int nameMaxW = Math.max(0, applyX - ROW_BTN_GAP - nameX);

        return new ListLayout(left, listW, nameX, nameMaxW,
                applyX, applyW, applyText,
                exportX, exportW, exportText,
                editX, editW, editText,
                deleteX, deleteW, deleteText);
    }


    private record ExportPopupLayout(int x, int y, int w, int h,
                                     ButtonPair buttons, int buttonY,
                                     String title, String copyKey, String share, String info,
                                     Component copyText, Component closeText) {}

    private ExportPopupLayout computeExportPopupLayout() {
        int maxContentW = Math.max(0, this.width - SCREEN_MARGIN * 2 - POPUP_PADDING * 2);

        String shownName = fitText(exportPresetName, NAME_CAP_W);
        String title = fitText(Component.translatable("itemblocker.text.export_preset", shownName), maxContentW);
        String copyKey = fitText(Component.translatable("itemblocker.text.press_copy_key"), maxContentW);
        String share = fitText(Component.translatable("itemblocker.text.share_code"), maxContentW);
        String info = fitText(Component.translatable("itemblocker.text.other_player_info"), maxContentW);
        Component copyText = Component.translatable("itemblocker.text.copy");
        Component closeText = Component.translatable("itemblocker.text.close");

        ButtonPair buttons = layoutButtonPair(copyText, closeText);

        int contentW = Math.max(
                Math.max(this.font.width(title), this.font.width(copyKey)),
                Math.max(this.font.width(share), this.font.width(info)));
        contentW = Math.max(contentW, buttons.totalW());

        int w = Math.clamp(contentW + POPUP_PADDING * 2, POPUP_MIN_W, this.width - SCREEN_MARGIN * 2);
        int h = EXPORT_POPUP_H;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - h / 2;

        return new ExportPopupLayout(x, y, w, h, buttons, y + 62, title, copyKey, share, info, copyText, closeText);
    }


    private record DeletePopupLayout(int x, int y, int w, int h,
                                     ButtonPair buttons, int buttonY,
                                     String title, String warning, String hint,
                                     Component confirmText, Component closeText) {}

    private DeletePopupLayout computeDeletePopupLayout() {
        int maxContentW = Math.max(0, this.width - SCREEN_MARGIN * 2 - POPUP_PADDING * 2);

        String title = fitText(Component.translatable("itemblocker.text.confirm_delete", fullPresetName), maxContentW);
        String warning = fitText(Component.translatable("itemblocker.text.permanently_remove"), maxContentW);
        String hint = fitText(Component.translatable("itemblocker.text.shift_click"), maxContentW);
        Component confirmText = Component.translatable("itemblocker.text.confirm");
        Component closeText = Component.translatable("itemblocker.text.close");

        ButtonPair buttons = layoutButtonPair(confirmText, closeText);

        int contentW = Math.max(
                Math.max(this.font.width(title), this.font.width(warning)),
                Math.max(this.font.width(hint), buttons.totalW()));

        int w = Math.clamp(contentW + POPUP_PADDING * 2, POPUP_MIN_W, this.width - SCREEN_MARGIN * 2);
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - (DELETE_POPUP_H + 20) / 2;

        return new DeletePopupLayout(x, y, w, DELETE_POPUP_H, buttons, y + 60, title, warning, hint, confirmText, closeText);
    }


    @Override
    protected void init() {

        PresetsConfig.load();

        Component createText = Component.translatable("itemblocker.text.create_preset");
        Component importText = Component.translatable("itemblocker.text.import_preset");
        Component backText = Component.translatable("gui.back");

        int createW = Math.max(105, this.font.width(createText) + TOP_BTN_PAD);
        int importW = Math.max(100, this.font.width(importText) + TOP_BTN_PAD);
        int topStartX = this.width / 2 - (createW + TOP_BTN_GAP + importW) / 2;

        Button createButton = Button.builder(createText, button -> {
                    this.minecraft.gui.setScreen(new CreatePresetScreen(this));
                })
                .createNarration(narration -> Component.translatable("itemblocker.text.create_preset"))
                .bounds(topStartX, 30, createW, 20)
                .build();
        this.addRenderableWidget(createButton);

        Button importButton = Button.builder(importText, button -> {
                    this.minecraft.gui.setScreen(new ImportPresetScreen(this));
                })
                .createNarration(narration -> Component.translatable("itemblocker.text.import_preset"))
                .bounds(topStartX + createW + TOP_BTN_GAP, 30, importW, 20)
                .build();
        this.addRenderableWidget(importButton);

        int backW = Math.max(200, this.font.width(backText) + TOP_BTN_PAD);
        Button backButton = Button.builder(backText, button -> {
                    this.onClose();
                })
                .createNarration(narration -> Component.translatable("gui.back"))
                .bounds(this.width / 2 - backW / 2, this.height - 30, backW, 20)
                .build();
        this.addRenderableWidget(backButton);

        this.refreshList();
    }

    private void refreshList() {
        this.presetsList = new ArrayList<>();
        for (String presetName : PresetsConfig.getPresetNames()) {
            this.presetsList.add(new PresetEntry(presetName));
        }
    }


    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        isShiftDown = minecraft.hasShiftDown();

        int listY = LIST_TOP;
        int listHeight = this.height - 100;
        ListLayout l = computeListLayout();

        graphics.text(this.font, Component.translatable("itemblocker.text.your_presets"), l.left() + 5, listY - 9, Colors.WHITE, false);

        graphics.fill(l.left(), listY, l.left() + l.width(), listY + listHeight, 0x80000000);

        if (presetsList.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("itemblocker.text.no_presets"),
                    this.width / 2,
                    listY + listHeight / 2,
                    0xFF888888
            );
        } else {
            this.renderButtons(graphics, l, listY, listHeight, mouseX, mouseY);
        }

        if (showingExportPopup) {
            this.renderExportPopup(graphics, mouseX, mouseY);
        }

        if (showingDeletePopup) {
            this.renderDeletePopup(graphics, mouseX, mouseY);
        }
    }

    private void renderButtons(GuiGraphicsExtractor graphics, ListLayout l, int listY, int listHeight, int mouseX, int mouseY) {
        int yOffset = listY + 5;
        int itemsPerPage = (listHeight - 10) / ROW_H;
        boolean blocked = showingDeletePopup || showingExportPopup;

        for (int i = scrollOffset; i < presetsList.size() && i < scrollOffset + itemsPerPage; i++) {
            PresetEntry entry = presetsList.get(i);

            graphics.text(this.font, fitText(entry.name, l.nameMaxW()), l.nameX(), yOffset + 2, Colors.WHITE, false);

            int itemCount = PresetsConfig.getPreset(entry.name).size();
            String countText = fitText(Component.translatable("itemblocker.text.item_count", itemCount), l.nameMaxW());
            graphics.text(this.font, countText, l.nameX(), yOffset + 14, 0xFFAAAAAA, false);

            boolean applyHovered = !blocked && inside(mouseX, mouseY, l.applyX(), yOffset, l.applyW(), BTN_H);
            drawButton(graphics, l.applyX(), yOffset, l.applyW(), l.applyText(),
                    applyHovered ? Colors.GREEN.color(false) : Colors.GREEN.color(true));

            boolean exportHovered = !blocked && inside(mouseX, mouseY, l.exportX(), yOffset, l.exportW(), BTN_H);
            drawButton(graphics, l.exportX(), yOffset, l.exportW(), l.exportText(),
                    exportHovered ? Colors.ORANGE.color(false) : Colors.ORANGE.color(true));

            boolean editHovered = !blocked && inside(mouseX, mouseY, l.editX(), yOffset, l.editW(), BTN_H);
            drawButton(graphics, l.editX(), yOffset, l.editW(), l.editText(),
                    editHovered ? Colors.BLUE.color(false) : Colors.BLUE.color(true));

            boolean deleteHovered = !blocked && inside(mouseX, mouseY, l.deleteX(), yOffset, l.deleteW(), BTN_H);
            drawButton(graphics, l.deleteX(), yOffset, l.deleteW(), l.deleteText(),
                    deleteHovered ? Colors.RED.color(false) : Colors.RED.color(true));

            yOffset += ROW_H;
            if (applyHovered || exportHovered || editHovered || deleteHovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    private void renderExportPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ExportPopupLayout l = computeExportPopupLayout();
        ButtonPair b = l.buttons();

        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        graphics.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), 0xFF222222);
        graphics.outline(l.x(), l.y(), l.w(), l.h(), 0xFFFFFFFF);

        graphics.centeredText(this.font, l.title(), this.width / 2, l.y() + 8, 0xFFFFAA00);
        graphics.centeredText(this.font, l.copyKey(), this.width / 2, l.y() + 22, 0xFFAAAAAA);

        graphics.fill(l.x() + 8, l.y() + 33, l.x() + l.w() - 8, l.y() + 55, 0xFF111111);

        String displayCode = fitText(exportCode, l.w() - 24);
        graphics.text(this.font, displayCode, l.x() + 12, l.y() + 40, 0xFF55FF55, false);

        boolean copyHovered = inside(mouseX, mouseY, b.firstX(), l.buttonY(), b.firstW(), BTN_H);
        drawButton(graphics, b.firstX(), l.buttonY(), b.firstW(), l.copyText(),
                copyHovered ? Colors.ORANGE.color(false) : Colors.ORANGE.color(true));

        boolean closeHovered = inside(mouseX, mouseY, b.secondX(), l.buttonY(), b.secondW(), BTN_H);
        drawButton(graphics, b.secondX(), l.buttonY(), b.secondW(), l.closeText(),
                closeHovered ? Colors.RED.color(false) : Colors.RED.color(true));

        graphics.centeredText(this.font, l.share(), this.width / 2, l.y() + 90, 0xFF888888);
        graphics.centeredText(this.font, l.info(), this.width / 2, l.y() + 102, 0xFF888888);

        if (closeHovered || copyHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void renderDeletePopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DeletePopupLayout l = computeDeletePopupLayout();
        ButtonPair b = l.buttons();

        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        graphics.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), 0xFF222222);
        graphics.outline(l.x(), l.y(), l.w(), l.h(), 0xFFFFFFFF);

        graphics.centeredText(this.font, l.title(), this.width / 2, l.y() + 8, 0xFFFFAA00);
        graphics.centeredText(this.font, l.warning(), this.width / 2, l.y() + 22, 0xFFAAAAAA);
        graphics.centeredText(this.font, l.hint(), this.width / 2, l.y() + 39, 0xFF888888);

        boolean confirmHovered = inside(mouseX, mouseY, b.firstX(), l.buttonY(), b.firstW(), BTN_H);
        drawButton(graphics, b.firstX(), l.buttonY(), b.firstW(), l.confirmText(),
                confirmHovered ? Colors.GREEN.color(false) : Colors.GREEN.color(true));

        boolean closeHovered = inside(mouseX, mouseY, b.secondX(), l.buttonY(), b.secondW(), BTN_H);
        drawButton(graphics, b.secondX(), l.buttonY(), b.secondW(), l.closeText(),
                closeHovered ? Colors.RED.color(false) : Colors.RED.color(true));

        if (closeHovered || confirmHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        double mx = click.x();
        double my = click.y();

        if (showingExportPopup) {
            if (click.input() != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            ExportPopupLayout l = computeExportPopupLayout();
            ButtonPair b = l.buttons();

            if (inside(mx, my, b.firstX(), l.buttonY(), b.firstW(), BTN_H)) {
                this.playClickSound();
                this.addToast(Component.translatable("itemblocker.text.copied"), Component.translatable("itemblocker.text.copied_code"));
                showingExportPopup = false;
                this.minecraft.keyboardHandler.setClipboard(exportCode);
                Loggers.PRESETS_LOGGER.info("Copied preset code to clipboard");
            }

            if (inside(mx, my, b.secondX(), l.buttonY(), b.secondW(), BTN_H)) {
                this.playClickSound();
                showingExportPopup = false;
            }

            if (!inside(mx, my, l.x(), l.y(), l.w(), l.h())) {
                showingExportPopup = false;
            }
        }

        else if (showingDeletePopup) {
            if (click.input() != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            DeletePopupLayout l = computeDeletePopupLayout();
            ButtonPair b = l.buttons();

            if (inside(mx, my, b.firstX(), l.buttonY(), b.firstW(), BTN_H)) {
                this.playClickSound();
                PresetsConfig.deletePreset(fullPresetName);
                Loggers.PRESETS_LOGGER.info("Deleted preset: {}", fullPresetName);
                showingDeletePopup = false;
                this.refreshList();
            }

            if (inside(mx, my, b.secondX(), l.buttonY(), b.secondW(), BTN_H)) {
                this.playClickSound();
                showingDeletePopup = false;
            }

            if (!inside(mx, my, l.x(), l.y(), l.w(), l.h())) {
                showingDeletePopup = false;
            }
        }

        else {
            int listHeight = this.height - 100;
            int yOffset = LIST_TOP + 5;
            int itemsPerPage = (listHeight - 10) / ROW_H;
            ListLayout l = computeListLayout();

            for (int i = scrollOffset; i < presetsList.size() && i < scrollOffset + itemsPerPage; i++) {
                PresetEntry entry = presetsList.get(i);

                if (inside(mx, my, l.applyX(), yOffset, l.applyW(), BTN_H)) {
                    String toastTest;
                    if (isShiftDown) {
                        PresetsConfig.applyPreset(entry.name, false);
                        toastTest = "itemblocker.text.apply_no_clear";
                    }
                    else {
                        PresetsConfig.applyPreset(entry.name, true);
                        toastTest = "itemblocker.text.apply_clear";
                    }
                    this.addToast(Component.translatable(toastTest), Component.translatable("itemblocker.text.applied", entry.name));
                    this.playClickSound();
                    Loggers.PRESETS_LOGGER.info("Applied preset: {}", entry.name);
                }

                if (inside(mx, my, l.exportX(), yOffset, l.exportW(), BTN_H)) {
                    exportPresetName = entry.name;
                    exportCode = PresetSharing.exportPreset(entry.name).exportCode();
                    showingExportPopup = true;
                    this.playClickSound();
                    Loggers.PRESETS_LOGGER.info("Exporting preset: {}", entry.name);
                }

                if (inside(mx, my, l.editX(), yOffset, l.editW(), BTN_H)) {
                    this.playClickSound();
                    this.minecraft.gui.setScreen(new EditPresetScreen(this, entry.name));
                }

                if (inside(mx, my, l.deleteX(), yOffset, l.deleteW(), BTN_H)) {

                    if (isShiftDown) {
                        Loggers.PRESETS_LOGGER.info("Deleted preset: {}", entry.name);
                        PresetsConfig.deletePreset(entry.name);
                    }
                    if (!isShiftDown) {
                        showingDeletePopup = true;
                        fullPresetName = entry.name;
                    }
                    this.refreshList();
                    this.playClickSound();
                }

                yOffset += ROW_H;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = this.height - 100;

        if (!showingExportPopup && !showingDeletePopup && mouseY >= LIST_TOP && mouseY <= LIST_TOP + listHeight) {
            int itemsPerPage = (listHeight - 10) / ROW_H;
            int maxScroll = Math.max(0, presetsList.size() - itemsPerPage);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyInput) {
        if (showingExportPopup && keyInput.isEscape()) {
            showingExportPopup = false;
        }

        if (showingDeletePopup && keyInput.isConfirmation()) {
            PresetsConfig.deletePreset(fullPresetName);
            showingDeletePopup = false;
            this.refreshList();
        }
        if (showingExportPopup && keyInput.isCopy()) {
            this.addToast(Component.translatable("itemblocker.text.copied"), Component.translatable("itemblocker.text.copied_code"));
            minecraft.keyboardHandler.setClipboard(exportCode);
        }
        return super.keyPressed(keyInput);
    }

    private record PresetEntry(String name) {
    }
}