package com.itemblocker.client.screen;

import com.itemblocker.api.enums.AlignmentMode;
import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.enums.PickupMode;
import com.itemblocker.api.screen.GlobalScreen;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
/* import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.gui.ModsScreen;

 */
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class SettingsScreen extends GlobalScreen {

    int pageNumber = 1;
    int maxPages = 2;

    Button backButton;
    Button forwardButton;
    Screen parent;
    CycleButton<ItemIDMode> setItemIdsButton;
    CycleButton<AlignmentMode> alignmentButton;
    CycleButton<PickupMode> pickupModeButton;
    public SettingsScreen(Screen parent) {
        this.parent = parent;
        super(Component.translatable("itemblocker.text.options_title"), null);
    }



    @Override
    protected void init() {
        // Pickup mode toggle
        pickupModeButton = CycleButton.builder(PickupMode::getDisplayText, BlockedItemsConfig.getPickupMode())
                .withValues(PickupMode.values())
                .create(
                        this.width / 2 - 150,
                        60,
                        300,
                        20,
                        Component.translatable("itemblocker.text.pickup_mode"),
                        (button, value) -> BlockedItemsConfig.setPickupMode(value)
                );

        alignmentButton = CycleButton.builder(AlignmentMode::getDisplayText, BlockedItemsConfig.getAlignmentMode())
                .withValues(AlignmentMode.values())
                .create(
                        this.width / 2 - 150,
                        60,
                        300,
                        20,
                        Component.literal("Alignment Mode"),
                        (button, value) ->  {
                            BlockedItemsConfig.setAlignmentMode(value);

                        }
                );

        setItemIdsButton = CycleButton.builder(ItemIDMode::getDisplayText, BlockedItemsConfig.getItemIdMode())
                .withValues(ItemIDMode.values())
                .create(
                        this.width / 2 - 150,
                        170,
                        300,
                        20,
                        Component.translatable("itemblocker.text.item_id_mode"),
                        (button, value) -> BlockedItemsConfig.setUseItemIds(value)
                );


        // Back button
        Button doneButton = Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(this.width / 2 - 75, this.height - 30, 150, 20)
                .build();

        this.addRenderableWidget(doneButton);

        // Back button
        forwardButton = Button.builder(Component.literal(">"), button ->  {
                pageNumber += 1;
                })
                .bounds(this.width / 2 + 80, this.height - 30, 30, 20)
                .build();

        backButton = Button.builder(Component.literal("<"), button ->  {
                    pageNumber -= 1;
                })
                .bounds(this.width / 2 - 110, this.height - 30, 30, 20)
                .build();

    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int y = 90;
        int xpos = this.width / 2 - 150;
        int ypos = 200;
        if (pageNumber != 1) {
            backButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.addWidget(backButton);
        }
        if (pageNumber == 1) {
            this.removeWidget(backButton);
        }

        if (pageNumber != maxPages) {
            forwardButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.addWidget(forwardButton);
        }
        if (pageNumber == maxPages) {
            this.removeWidget(forwardButton);
        }

        if (pageNumber == 1) {
            setItemIdsButton.extractRenderState(graphics, mouseX, mouseY, delta);
            pickupModeButton.extractRenderState(graphics, mouseX, mouseY, delta);
            pickupModeButton.active = true;
            setItemIdsButton.active = true;
            this.removeWidget(alignmentButton);
            alignmentButton.active = false;
            this.addWidget(pickupModeButton);
            this.addWidget(setItemIdsButton);
            if (ItemBlockerClient.isUsingClientMode()) {
                graphics.text(this.font, Component.translatable("itemblocker.text.client_only"), this.width / 2 - 150, y, 0xFF55FF55, false);
                y += 15;
                graphics.text(this.font, Component.translatable("itemblocker.text.works_on_server"), this.width / 2 - 150, y, Colors.WHITE, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.automatically"), this.width / 2 - 150, y, Colors.WHITE, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.briefly_show"), this.width / 2 - 150, y, Colors.WHITE, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.no_server_installation"), this.width / 2 - 150, y, 0xFF55FF55, false);
            } else {
                graphics.text(this.font, Component.translatable("itemblocker.text.server_only"), this.width / 2 - 150, y, 0xFFFFAA00, false);
                y += 15;
                graphics.text(this.font, Component.translatable("itemblocker.text.no_pickup"), this.width / 2 - 150, y, Colors.WHITE, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.cleaner_experience"), this.width / 2 - 150, y, Colors.WHITE, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.server_installation"), this.width / 2 - 150, y, 0xFFFF5555, false);
                y += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.wont_work"), this.width / 2 - 150, y, 0xFFFF5555, false);
            }

            y += 20;
            graphics.fill(this.width / 2 - 150, y, this.width / 2 + 150, y + 1, 0x80FFFFFF);
            


            if (ItemBlockerClient.isUsingIDMode()) {
                graphics.text(this.font, Component.translatable("itemblocker.text.use_item_id"), xpos, ypos, 0xFFFFAA00, false);
                ypos += 15;
                graphics.text(this.font, Component.translatable("itemblocker.text.item_show_ids"), xpos, ypos, Colors.WHITE, false);
                ypos += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.looks_like_id"), xpos, ypos, Colors.WHITE, false);
            } else {
                graphics.text(this.font, Component.translatable("itemblocker.text.use_item_names"), xpos, ypos, 0xFF55FF55, false);
                ypos += 15;
                graphics.text(this.font, Component.translatable("itemblocker.text.item_show_names"), xpos, ypos, Colors.WHITE, false);
                ypos += 12;
                graphics.text(this.font, Component.translatable("itemblocker.text.looks_like_name"), xpos, ypos, Colors.WHITE, false);
            }
            ypos += 20;

            graphics.fill(this.width / 2 - 150, ypos, this.width / 2 + 150, ypos + 1, 0x80FFFFFF);
            ypos += 16;
            graphics.text(this.font, Component.translatable("itemblocker.text.restart_note"), this.width / 2 - 150, ypos, 0xFFFFFF55, false);
        }
        
        if (pageNumber == 2) {
            this.removeWidget(pickupModeButton);
            alignmentButton.active = true;
            pickupModeButton.active = false;
            setItemIdsButton.active = false;
            this.removeWidget(setItemIdsButton);
            this.addWidget(alignmentButton);
            alignmentButton.extractRenderState(graphics, mouseX, mouseY, delta);
            int y2 = 90;
            int yPos2 = 200;
            if (ItemBlockerClient.isCenterAligned()) {
                graphics.text(this.font, Component.literal("Mode: Center Aligned"), this.width / 2 - 150, y2, 0xFF55FF55, false);
                y2 += 15;
                graphics.text(this.font, Component.literal("- Item widgets are aligned to the center of the screen"), this.width / 2 - 150, y2, Colors.WHITE, false);
                y2 += 12;
                graphics.text(this.font, Component.literal("- Default and cleanest option"), this.width / 2 - 150, y2, Colors.WHITE, false);
            } else {
                graphics.text(this.font, Component.literal("Mode: Left Aligned"), this.width / 2 - 150, y2, 0xFFFFAA00, false);
                y2 += 15;
                graphics.text(this.font, Component.literal("- Item widgets are aligned to the left of the screen"), this.width / 2 - 150, y2, Colors.WHITE, false);
                y2 += 12;
                graphics.text(this.font, Component.literal("- Might be slightly buggy and might not work as intended"), this.width / 2 - 150, y2, Colors.WHITE, false);
            }

            yPos2 -= 25;

            graphics.fill(this.width / 2 - 150, yPos2, this.width / 2 + 150, yPos2 + 1, 0x80FFFFFF);

            yPos2 -= 18;
            graphics.text(this.font, Component.translatable("itemblocker.text.restart_note"), this.width / 2 - 150, yPos2, 0xFFFFFF55, false);
        }
    }

    @Override
    public void onClose() {
        if (BlockedItemsConfig.getAlignmentMode() == AlignmentMode.LEFT_ALIGNED) {
            parent = new LeftAlignedScreen(null/*new ModsScreen(null)*/);
        }
        else {
            parent = new BlockedItemsScreen(null /*new ModsScreen(null)*/);
        }
        this.minecraft.gui.setScreen(parent);
    }

}