package com.itemblocker.client.screen.presets;

import com.itemblocker.api.PresetSharing;
import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.screen.misc.Fade;
import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.client.screen.BlockedItemsScreen;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.screen.LeftAlignedScreen;
import com.itemblocker.client.screen.presets.edit.EditPresetScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

public class ImportPresetScreen extends GlobalScreen {
    private EditBox codeField;
    private Button importButton;
    private Button cancelButton;
    private long successTime = -1;
    private long emptyTime = -1;
    private long invalidTime = -1;

    public ImportPresetScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.import_preset"), parent);
    }

    @Override
    protected void init() {
        this.codeField = new EditBox(
                this.font,
                this.width / 2 - 150,
                70,
                300,
                20,
                Component.translatable("itemblocker.text.preset_code")
        );
        this.codeField.setMaxLength(Integer.MAX_VALUE);
        this.codeField.setHint(Component.translatable("itemblocker.text.paste_code"));
        this.addRenderableWidget(this.codeField);
        this.setInitialFocus(this.codeField);

        this.importButton = Button.builder(Component.translatable("itemblocker.text.import"), button -> {
                    this.tryImport() ;
                })
                .bounds(this.width / 2 - 100, 100, 95, 20)
                .build();
        this.addRenderableWidget(this.importButton);

        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
                    this.onClose();
                })
                .bounds(this.width / 2 + 5, 100, 95, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
    }


    private void tryImport() {
        String code = this.codeField.getValue().trim();
        if (code.isEmpty()) {
            successTime = -1;
            invalidTime = -1;
            emptyTime = Util.getMillis();
            return;
        }

        PresetSharing.ImportedPreset imported = PresetSharing.importPreset(code);
        if (imported == null) {
            Loggers.PRESETS_LOGGER.info("import test");
            successTime = -1;
            emptyTime = -1;
            invalidTime = Util.getMillis();
            return;
        }

        // Handle duplicate names
        String finalName = imported.name();
        if (PresetsConfig.getPresetNames().contains(finalName)) {
            finalName = finalName + "_imported";
            int count = 1;
            while (PresetsConfig.getPresetNames().contains(finalName)) {
                finalName = imported.name() + "_imported_" + count++;
            }
        }

        PresetsConfig.createPreset(finalName);
        for (String item : imported.items()) {
            PresetsConfig.addItemToPreset(finalName, item);
        }

        emptyTime = -1;
        invalidTime = -1;
        successTime = Util.getMillis();

        this.codeField.setValue("");
        Loggers.PRESETS_LOGGER.info("Imported preset: {} with {} items", finalName, imported.items().size());
        if (BlockedItemsConfig.getAlignmentMode() == AlignmentMode.CENTER_ALIGNED) {
            this.minecraft.gui.setScreen(new EditPresetScreen(new PresetsScreen(new BlockedItemsScreen(null)), imported.name()));
        }
        else {
            this.minecraft.gui.setScreen(new EditPresetScreen(new PresetsScreen(new LeftAlignedScreen(null)), imported.name()));
        }
    }


    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);


        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.paste_code_shared"),
                this.width / 2, 45, 0xFFAAAAAA);

        graphics.centeredText(this.font,
                Component.translatable("itemblocker.text.preset_contains"),
                this.width / 2, 57, 0xFFAAAAAA);


        if (successTime >= 0) {
            int color = fadeManager.fadeOut(Fade.GREEN_FADE, successTime);
            Component message = Component.translatable("itemblocker.text.succesful");
            graphics.centeredText(this.font, message, this.width / 2, 130, color);
        }

        else if (emptyTime >= 0) {
            int color = fadeManager.fadeOut(Fade.RED_FADE, emptyTime);
            Component message = Component.translatable("itemblocker.text.empty");
            graphics.centeredText(this.font, message, this.width / 2, 130, color);
        }

        else if (invalidTime >= 0) {
            int color = fadeManager.fadeOut(Fade.RED_FADE, invalidTime);
            Component message = Component.literal("Invalid preset code! Make sure you copied it correctly.");
            graphics.centeredText(this.font, message, this.width / 2, 130, color);
        }

    }


    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.isConfirmation() && this.codeField.isFocused()) {
            tryImport();
        }
        return super.keyPressed(keyInput);
    }

}