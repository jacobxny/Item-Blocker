package com.itemblocker.client.screen.presets;

import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.screen.presets.edit.EditPresetScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class CreatePresetScreen extends GlobalScreen {
    private final Screen parent;
    private EditBox nameField;

    public CreatePresetScreen(Screen parent) {
        this.parent = parent;
        super(Component.translatable("itemblocker.text.create_preset"), parent);
    }


    @Override
    protected void init() {
        // Preset name field
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

        // Save button
        Button saveButton = Button.builder(Component.translatable("itemblocker.text.create"), button -> {
                    String name = this.nameField.getValue().trim();
                    if (!name.isEmpty()) {
                        PresetsConfig.createPreset(name);
                        Loggers.PRESETS_LOGGER.info("Created preset: {}", name);
                        if (this.minecraft.gui != null) {
                            this.minecraft.gui.setScreen(new EditPresetScreen(this.parent, name));
                        }
                    }
                })
                .bounds(this.width / 2 - 100, 90, 95, 20)
                .build();

        this.addRenderableWidget(saveButton);

        // Cancel button
        Button cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
                    this.onClose();
                })
                .bounds(this.width / 2 + 5, 90, 95, 20)
                .build();
        this.addRenderableWidget(cancelButton);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Instructions
        graphics.text(this.font, Component.translatable("itemblocker.text.enter_name"), this.width / 2 - 100, 45, 0xFFAAAAAA, false);

        // Info text
        int y = 120;
        graphics.text(this.font, Component.translatable("itemblocker.text.after_creation"), this.width / 2 - 120, y, 0xFFFFFF55, false);
    }


    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.isConfirmation() && this.nameField.isFocused()) {
            String name = this.nameField.getValue().trim();
            if (!name.isEmpty()) {
                PresetsConfig.createPreset(name);
                Loggers.PRESETS_LOGGER.info("Created preset: {}", name);
                if (this.minecraft.gui != null) {
                    this.minecraft.gui.setScreen(new EditPresetScreen(this.parent, name));
                }
            }
        }

        return super.keyPressed(keyInput);
    }


}