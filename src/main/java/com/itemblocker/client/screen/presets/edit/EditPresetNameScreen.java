package com.itemblocker.client.screen.presets.edit;

import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.screen.misc.Fade;
import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.client.screen.BlockedItemsScreen;
import com.itemblocker.client.screen.LeftAlignedScreen;
import com.itemblocker.client.screen.presets.PresetsScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

public class EditPresetNameScreen extends GlobalScreen {
    private final String presetName;
    private EditBox nameField;
    private Button saveButton;
    private Button cancelButton;
    private long existingMessageStartTime = -1;
    private long sameNameMessageTime = -1;
    public EditPresetNameScreen(Screen parent, String presetName) {
        super(Component.literal("Edit Preset Name"), parent);
        this.presetName = presetName;
    }


    @Override
    protected void init() {
        this.nameField = new EditBox(
                this.font,
                this.width / 2 - 100,
                60,
                200,
                20,
                Component.translatable("itemblocker.text.preset_name")
        );
        this.nameField.setMaxLength(32);
        this.nameField.setHint(Component.translatable("itemblocker.text.preset_placeholder"));
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        this.saveButton = Button.builder(Component.literal("Save"), button -> {
                    this.rename();
                })
                .bounds(this.width / 2 - 100, 90, 95, 20)
                .build();
        this.addRenderableWidget(this.saveButton);
        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
                    this.onClose();
                })
                .bounds(this.width / 2 + 5, 90, 95, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);

    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(this.font, Component.literal("Current name: §e" + presetName), this.width / 2, 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.literal("Enter a new name for your preset"), this.width / 2 - 100, 45, 0xFFAAAAAA, false);
        if (existingMessageStartTime >= 0) {
            int color = fadeManager.fadeOut(Fade.RED_FADE, existingMessageStartTime);
            graphics.text(this.font, Component.literal("A preset with that name already exists"), this.width / 2 - 100, 130, color, false);
        }

        if (sameNameMessageTime >= 0) {
            int color = fadeManager.fadeOut(Fade.RED_FADE, sameNameMessageTime);
            graphics.text(this.font, Component.literal("Cannot rename the preset to it's existing name"), this.width / 2 - 120, 130, color, false);
        }

    }


    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.isConfirmation() && this.nameField.isFocused()) {
            this.rename();
        }

        return super.keyPressed(keyInput);
    }


    public void rename() {
        String newName = this.nameField.getValue().trim();
        if (!newName.isEmpty()) {
            if (newName.equals(presetName)) {
                existingMessageStartTime = -1;
                sameNameMessageTime = Util.getMillis();
            }
            else if (PresetsConfig.getPresetNames().contains(newName)) {
                sameNameMessageTime = -1;
                existingMessageStartTime = Util.getMillis();
            }
            else {
                PresetsConfig.renamePreset(presetName, newName);
                if (BlockedItemsConfig.getAlignmentMode() == AlignmentMode.CENTER_ALIGNED) {
                    this.minecraft.gui.setScreen(new EditPresetScreen(new PresetsScreen(new BlockedItemsScreen(null)), newName));
                }
                else {
                    this.minecraft.gui.setScreen(new EditPresetScreen(new PresetsScreen(new LeftAlignedScreen(null)), newName));
                }
            }
        }
    }
}