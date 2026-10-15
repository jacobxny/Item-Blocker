package com.itemblocker.client.screen.presets;

import com.itemblocker.api.PresetSharing;
import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.api.enums.MouseClick;
import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.screen.presets.edit.EditPresetScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PresetsScreen extends GlobalScreen {
    private List<PresetEntry> presetsList;
    private int scrollOffset = 0;
    private boolean isShiftDown = false;

    private boolean showingExportPopup = false;
    private String exportCode = "";
    private String exportPresetName = "";

    private boolean showingDeletePopup = false;
    private String deletePresetName = "";
    private String fullPresetName = "";

    public PresetsScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.manage_presets"), parent);
    }

    @Override
    protected void init() {

        PresetsConfig.load();

        Button createButton = Button.builder(Component.translatable("itemblocker.text.create_preset"), button -> {
                    this.minecraft.gui.setScreen(new CreatePresetScreen(this));
                })
                .createNarration(narration -> Component.literal("Create New Preset"))
                .bounds(this.width / 2 - 105, 30, 105, 20)
                .build();
        this.addRenderableWidget(createButton);


        Button importButton = Button.builder(Component.translatable("itemblocker.text.import_preset"), button -> {
                    this.minecraft.gui.setScreen(new ImportPresetScreen(this));
                })
                .createNarration(narration -> Component.literal("Import Presett"))
                .bounds(this.width / 2 + 5, 30, 100, 20)
                .build();
        this.addRenderableWidget(importButton);

        Button backButton = Button.builder(Component.translatable("gui.back"), button -> {
                    this.onClose();
                })
                .createNarration(narration -> Component.literal("Back"))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
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

        int listY = 60;
        int listHeight = this.height - 100;

        graphics.text(this.font, Component.translatable("itemblocker.text.your_presets"), this.width / 2 - 150, listY - 9, Colors.WHITE, false);


        graphics.fill(this.width / 2 - 155, listY, this.width / 2 + 155, listY + listHeight, 0x80000000);

        if (presetsList.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("itemblocker.text.no_presets"),
                    this.width / 2,
                    listY + listHeight / 2,
                    0xFF888888
            );
        } else {
            this.renderButtons(graphics, listY, listHeight, mouseX, mouseY);
        }


        if (showingExportPopup) {
            this.renderExportPopup(graphics, mouseX, mouseY);
        }

        if (showingDeletePopup) {
            this.renderDeletePopup(graphics, mouseX, mouseY);
        }
    }

    private void renderButtons(GuiGraphicsExtractor graphics, int listY, int listHeight, int mouseX, int mouseY) {
        int yOffset = listY + 5;
        int itemsPerPage = (listHeight - 10) / 30;

        for (int i = scrollOffset; i < presetsList.size() && i < scrollOffset + itemsPerPage; i++) {
            PresetEntry entry = presetsList.get(i);

            String presetName = entry.name;
            int length = presetName.length();
            if (length > 23) {
                presetName = presetName.substring(0, 23);
                presetName += "...";
            }
            graphics.text(this.font, presetName, this.width / 2 - 145, yOffset + 2, Colors.WHITE, false);


            int itemCount = PresetsConfig.getPreset(entry.name).size();
            int textX = this.width / 2 - 145;
            int textY = yOffset + 14;


            graphics.text(this.font,
                    Component.translatable("itemblocker.text.item_count", itemCount),
                    textX, textY,
                    0xFFAAAAAA,
                    false);

            int applyX = this.width / 2 + 5;
            boolean applyHovered = mouseX >= applyX && mouseX <= applyX + 38 &&
                    mouseY >= yOffset && mouseY <= yOffset + 20;
            if (showingDeletePopup || showingExportPopup) {
                applyHovered = false;
            }
            graphics.fill(applyX, yOffset, applyX + 38, yOffset + 20, applyHovered ? Colors.GREEN.color(false) : Colors.GREEN.color(true));
            graphics.centeredText(this.font, Component.translatable("itemblocker.text.apply"), applyX + 19, yOffset + 6, Colors.WHITE);


            int exportX = this.width / 2 + 48;
            boolean exportHovered = mouseX >= exportX && mouseX <= exportX + 38 &&
                    mouseY >= yOffset && mouseY <= yOffset + 20;
            if (showingDeletePopup || showingExportPopup) {
                exportHovered = false;
            }
            graphics.fill(exportX, yOffset, exportX + 38, yOffset + 20, exportHovered ? Colors.ORANGE.color(false) : Colors.ORANGE.color(true));
            graphics.centeredText(this.font, Component.translatable("itemblocker.text.export"), exportX + 19, yOffset + 6, Colors.WHITE);

            int editX = this.width / 2 + 91;
            boolean editHovered = mouseX >= editX && mouseX <= editX + 30 &&
                    mouseY >= yOffset && mouseY <= yOffset + 20;

            if (showingDeletePopup || showingExportPopup) {
                editHovered = false;
            }
            graphics.fill(editX, yOffset, editX + 30, yOffset + 20, editHovered ? Colors.BLUE.color(false) : Colors.BLUE.color(true));
            graphics.centeredText(this.font, Component.translatable("itemblocker.text.edit"), editX + 15, yOffset + 6, Colors.WHITE);


            int deleteX = this.width / 2 + 126;
            boolean deleteHovered = mouseX >= deleteX && mouseX <= deleteX + 28 &&
                    mouseY >= yOffset && mouseY <= yOffset + 20;
            if (showingDeletePopup || showingExportPopup) {
                deleteHovered = false;
            }
            graphics.fill(deleteX, yOffset, deleteX + 28, yOffset + 20, deleteHovered ? Colors.RED.color(false) : Colors.RED.color(true));
            graphics.centeredText(this.font, "🗑", deleteX + 14, yOffset + 6, Colors.WHITE);

            yOffset += 30;
        }
    }

    private void renderExportPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int popupW = 320;
        int popupH = 120;
        int popupX = this.width / 2 - popupW / 2;
        int popupY = this.height / 2 - popupH / 2;

        graphics.fill(0, 0, this.width, this.height, 0x88000000);


        graphics.fill(popupX, popupY, popupX + popupW, popupY + popupH, 0xFF222222);

        graphics.outline(popupX, popupY, popupW, popupH, 0xFFFFFFFF);


        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.export_preset", exportPresetName), this.width / 2, popupY + 8, 0xFFFFAA00);


        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.press_copy_key"), this.width / 2, popupY + 22, 0xFFAAAAAA);


        graphics.fill(popupX + 8, popupY + 33, popupX + popupW - 8, popupY + 55, 0xFF111111);


        String displayCode = exportCode;
        int maxCodeWidth = popupW - 24;
        if (this.font.width(displayCode) > maxCodeWidth) {
            while (this.font.width(displayCode + "...") > maxCodeWidth && !displayCode.isEmpty()) {
                displayCode = displayCode.substring(0, displayCode.length() - 1);
            }
            displayCode += "...";
        }
        graphics.text(this.font, displayCode, popupX + 12, popupY + 40, 0xFF55FF55, false);


        int copyX = this.width / 2 - 80;
        int copyY = popupY + 62;
        boolean copyHovered = mouseX >= copyX && mouseX <= copyX + 70 &&
                mouseY >= copyY && mouseY <= copyY + 20;
        graphics.fill(copyX, copyY, copyX + 70, copyY + 20, copyHovered ? Colors.ORANGE.color(false) : Colors.ORANGE.color(true));
        graphics.centeredText(this.font, Component.translatable("itemblocker.text.copy"), copyX + 35, copyY + 6, Colors.WHITE);

        int closeX = this.width / 2 + 10;
        int closeY = popupY + 62;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 70 &&
                mouseY >= closeY && mouseY <= closeY + 20;
        graphics.fill(closeX, closeY, closeX + 70, closeY + 20, closeHovered ? Colors.RED.color(false) : Colors.RED.color(true));
        graphics.centeredText(this.font, Component.translatable("itemblocker.text.close"), closeX + 35, closeY + 6, Colors.WHITE);

        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.share_code"), this.width / 2, popupY + 90, 0xFF888888);
        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.other_player_info"), this.width / 2, popupY + 102, 0xFF888888);
    }


    private void renderDeletePopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int popupW = 320;
        int popupH = 118;
        int popupX = this.width / 2 - popupW / 2;
        int popupY = this.height / 2 - popupH / 2;

        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        graphics.fill(popupX, popupY, popupX + popupW, (popupY + popupH) - 20, 0xFF222222);
        graphics.outline(popupX, popupY, popupW, popupH - 20, 0xFFFFFFFF);


        int length = deletePresetName.length();

        if (length > 22) {
            deletePresetName = deletePresetName.substring(0, 22);
            deletePresetName += "...";
        }
        graphics.centeredText(this.font,
                Component.literal("Are you sure you want to delete: " + deletePresetName + "?"), this.width / 2, popupY + 8, 0xFFFFAA00);


        graphics.centeredText(this.font,
                Component.literal("This will permanently remove the preset and its contents!"), this.width / 2, popupY + 22, 0xFFAAAAAA);


        graphics.text(this.font, "You can hold Shift + Click to skip this popup.", popupX + 46, popupY + 39,  0xFF888888);

        int confirmX = this.width / 2 - 80;
        int confirmY = popupY + 60;
        boolean confirmHovered = mouseX >= confirmX && mouseX <= confirmX + 70 &&
                mouseY >= confirmY && mouseY <= confirmY + 20;
        graphics.fill(confirmX, confirmY,  confirmX + 70, confirmY + 20, confirmHovered ? Colors.GREEN.color(false) : Colors.GREEN.color(true));
        graphics.centeredText(this.font, Component.literal("Confirm"), confirmX + 35,  confirmY+ 6, Colors.WHITE);

        int closeX = this.width / 2 + 10;
        int closeY = popupY + 60;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 70 &&
                mouseY >= closeY && mouseY <= closeY + 20;
        graphics.fill(closeX, closeY, closeX + 70, closeY + 20, closeHovered ?  Colors.RED.color(false) : Colors.RED.color(true) );
        graphics.centeredText(this.font, Component.translatable("itemblocker.text.close"), closeX + 35, closeY + 6, Colors.WHITE);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        if (showingExportPopup) {
            if (click.input() == MouseClick.MIDDLE_CLICK.value() || click.input() == MouseClick.RIGHT_CLICK.value()) {
                return false;
            }
            int popupW = 320;
            int popupH = 120;
            int popupX = this.width / 2 - popupW / 2;
            int popupY = this.height / 2 - popupH / 2;


            int copyX = this.width / 2 - 80;
            int copyY = popupY + 62;
            if (click.x() >= copyX && click.x() <= copyX + 70 &&
                    click.y() >= copyY && click.y() <= copyY + 20) {
                this.playClickSound();
                SystemToast.add(minecraft.gui.toastManager(), new SystemToast.SystemToastId(4000), Component.literal("Copied to clipboard!"), Component.literal("Copied export code to clipboard!"));
                showingExportPopup = false;
                this.minecraft.keyboardHandler.setClipboard(exportCode);
                Loggers.PRESETS_LOGGER.info("Copied preset code to clipboard");
            }

            int closeX = this.width / 2 + 10;
            int closeY = popupY + 62;
            if (click.x() >= closeX && click.x() <= closeX + 70 &&
                    click.y() >= closeY && click.y() <= closeY + 20) {
                this.playClickSound();
                showingExportPopup = false;
            }

            if (click.x() < popupX || click.x() > popupX + popupW || click.y() < popupY || click.y() > popupY + popupH) {
                showingExportPopup = false;
            }
        }

        else if (showingDeletePopup) {
            if (click.input() == MouseClick.MIDDLE_CLICK.value() || click.input() == MouseClick.RIGHT_CLICK.value()) {
                return false;
            }
            int popupW = 320;
            int popupH = 120;
            int popupX = this.width / 2 - popupW / 2;
            int popupY = this.height / 2 - popupH / 2;


            int confirmX = this.width / 2 - 80;
            int confirmY = popupY + 62;
            if (click.x() >= confirmX && click.x() <= confirmX + 70 &&
                    click.y() >= confirmY && click.y() <= confirmY + 20) {
                this.playClickSound();
                PresetsConfig.deletePreset(fullPresetName);
                Loggers.PRESETS_LOGGER.info("Deleted preset: {}" ,fullPresetName);
                showingDeletePopup = false;
                this.refreshList();
                Loggers.PRESETS_LOGGER.info("Copied preset code to clipboard");
            }

            int closeX = this.width / 2 + 10;
            int closeY = popupY + 62;
            if (click.x() >= closeX && click.x() <= closeX + 70 &&
                    click.y() >= closeY && click.y() <= closeY + 20) {
                this.playClickSound();
                showingDeletePopup = false;
            }

            if (click.x() < popupX || click.x() > popupX + popupW || click.y() < popupY || click.y() > popupY + popupH) {
                showingDeletePopup = false;
            }
        }

        else {
            int listY = 60;
            int listHeight = this.height - 100;
            int yOffset = listY + 5;
            int itemsPerPage = (listHeight - 10) / 30;

            for (int i = scrollOffset; i < presetsList.size() && i < scrollOffset + itemsPerPage; i++) {
                PresetEntry entry = presetsList.get(i);


                int applyX = this.width / 2 + 5;
                if (click.x() >= applyX && click.x() <= applyX + 38 &&
                        click.y() >= yOffset && click.y() <= yOffset + 20) {
                    if (isShiftDown) {
                        PresetsConfig.applyPreset(entry.name, false);
                    }
                    else {
                        PresetsConfig.applyPreset(entry.name, true);
                    }
                    this.playClickSound();
                    Loggers.PRESETS_LOGGER.info("Applied preset: {}", entry.name);
                }

                int exportX = this.width / 2 + 48;
                if (click.x() >= exportX && click.x() <= exportX + 38 &&
                        click.y() >= yOffset && click.y() <= yOffset + 20) {
                    exportPresetName = entry.name;
                    exportCode = PresetSharing.exportPreset(entry.name).exportCode();
                    showingExportPopup = true;
                    this.playClickSound();
                    Loggers.PRESETS_LOGGER.info("Exporting preset: {}", entry.name);
                }

                int editX = this.width / 2 + 91;
                if (click.x() >= editX && click.x() <= editX + 30 &&
                        click.y() >= yOffset && click.y() <= yOffset + 20) {
                    this.playClickSound();
                    this.minecraft.gui.setScreen(new EditPresetScreen(this, entry.name));
                }

                int deleteX = this.width / 2 + 126;
                if (click.x() >= deleteX && click.x() <= deleteX + 28 &&
                        click.y() >= yOffset && click.y() <= yOffset + 20) {

                    if (isShiftDown) {
                        Loggers.PRESETS_LOGGER.info("Deleted preset: {}", entry.name);
                        PresetsConfig.deletePreset(entry.name);
                    }
                    if (!isShiftDown) {
                        showingDeletePopup = true;
                        deletePresetName = entry.name;
                        fullPresetName = entry.name;
                    }
                    this.refreshList();
                    this.playClickSound();
                }

                yOffset += 30;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listY = 60;
        int listHeight = this.height - 100;

        if (!showingExportPopup && !showingDeletePopup && mouseY >= listY && mouseY <= listY + listHeight) {
            int itemsPerPage = (listHeight - 10) / 30;
            int maxScroll = Math.max(0, presetsList.size() - itemsPerPage);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    public boolean keyPressed(@NonNull KeyEvent keyInput) {
        String displayCode = exportCode;

        if (showingExportPopup && keyInput.isEscape()) {
            showingExportPopup = false;
        }

        if (showingDeletePopup && keyInput.isConfirmation()) {
            PresetsConfig.deletePreset(deletePresetName);
            showingDeletePopup = false;
        }
        if (showingExportPopup && keyInput.isCopy()) {
            SystemToast.add(minecraft.gui.toastManager(), new SystemToast.SystemToastId(4000), Component.literal("Copied to clipboard!"), Component.literal("Copied export code to clipboard!"));
            minecraft.keyboardHandler.setClipboard(displayCode);
        }
        return super.keyPressed(keyInput);
    }
    


    private record PresetEntry(String name) {
    }
}