package com.itemblocker.client.screen;

import com.itemblocker.api.screen.AbstractItemWidget;
import com.itemblocker.api.screen.misc.Fade;
import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.screen.presets.PresetsScreen;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.logging.Loggers;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;


public class BlockedItemsScreen extends AbstractItemWidget {
    private int firstTicks = 0;
    private long invalidIDStartTime = -1;
    private Button tutorialButton;
    private boolean inTutorial = false;
    private long successTime = -1;
    private long alreadyTime = -1;
    private long testTime = -1;
    private long test1Time = -1;
    private long test2Time = -1;
    private long test3Time = -1;
    private long test4Time = -1;
    private long test5Time = -1;
    private long test6Time = -1;
    private String itemId;
    private int clearX;
    private int optionsX;
    private int presetsX;
    private final int rowY = 90;

    private static final float BUTTON_HOVER_ANIM_SPEED = 14f;
    private static final float PROMPT_DURATION = 5f;
    private static final float FADE_IN = 0.25f;
    private static final float FADE_OUT = 0.25f;

    private float clearHoverProgress = 0f;
    private float optionsHoverProgress = 0f;
    private float presetsHoverProgress = 0f;
    private long lastButtonFrameTimeNanos = -1;


    public BlockedItemsScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.config"), parent);
    }


    @Override
    public int getListY() {
        return 120;
    }

    @Override
    protected int getListX() {
        return width / 2 - 105;
    }

    @Override
    protected int getListWidth() {
        return 210;
    }

    @Override
    public int getListHeight() {
        return height - 160;
    }

    @Override
    public String getEmptyListText() {
        return "itemblocker.text.no_items";
    }

    @Override
    public boolean isItemSelected(String itemId) {
        return BlockedItemsConfig.isBlocked(itemId);
    }

    @Override
    protected void init() {
        super.init();

        clearX = this.width / 2 - 30;
        optionsX = this.width / 2 - 10;
        presetsX = this.width / 2 + 10;

        if (!RegistryUtils.isRegistryLoaded) {
            RegistryUtils.loadRegistry();
        }

        BlockedItemsConfig.load();


        this.tutorialButton = Button.builder(Component.literal("?"), button -> {
            this.startTutorial();
        }).bounds(this.doneButton.getX() - 50, this.height - 30, 30, 20).build();
        this.addRenderableWidget(tutorialButton);



        this.refreshList();
        if (!BlockedItemsConfig.hasDoneTutorial()) {
            this.startTutorial();
        }

    }

    private void refreshList() {
        this.storedItems = new ArrayList<>();
        for (String itemId : BlockedItemsConfig.getBlockedItems()) {
            this.storedItems.add(new ItemEntry(itemId));
        }
        sortList(storedItems);
        Loggers.CLIENT_LOGGER.info("Refreshed list, {} blocked items", this.storedItems.size());
    }


    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (invalidIDStartTime >= 0) {
            int color = fadeManager.fadeOut(Fade.LONG_RED_FADE, invalidIDStartTime);
            graphics.text(this.font, Component.translatable("itemblocker.text.invald_id"), this.width / 2 - 103, 27, color, false);
        }
        else if (successTime >= 0) {
            int color = fadeManager.fadeOut(Fade.GREEN_FADE, successTime);
            String text;
            if (ItemBlockerClient.isUsingIDMode()) {
                text = itemId;
            }
            else {
                text = ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            Component component = Component.translatable("itemblocker.text.success", text);

            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }
        else if (alreadyTime >= 0) {
            int color = fadeManager.fadeOut(Fade.YELLOW_FADE, alreadyTime);
            String text;
            if (ItemBlockerClient.isUsingIDMode()) {
                text = itemId;
            }

            else {
                text = ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            Component component = Component.translatable("itemblocker.text.already_blocked", text);
            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }

        this.renderButtons(graphics, mouseX, mouseY);
        this.renderTutorial(graphics, mouseX, mouseY, delta);
    }

    private void renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float deltaSeconds = lastButtonFrameTimeNanos < 0 ? 0f : (now - lastButtonFrameTimeNanos) / 1_000_000_000f;
        lastButtonFrameTimeNanos = now;

        Identifier clearButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/bin.png");
        Identifier settingsButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/settings.png");
        Identifier presetsButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/presets.png");

        boolean clearHovered = mouseX >= clearX && mouseX <= clearX + 16 && mouseY >= rowY && mouseY <= rowY + 16;
        boolean optionsHovered = mouseX >= optionsX && mouseX <= optionsX + 16 && mouseY >= rowY && mouseY <= rowY + 16;
        boolean presetsHovered = mouseX >= presetsX && mouseX <= presetsX + 16 && mouseY >= rowY && mouseY <= rowY + 16;

        if (clearHovered || optionsHovered || presetsHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        clearHoverProgress = this.approach(clearHoverProgress, clearHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);
        optionsHoverProgress = this.approach(optionsHoverProgress, optionsHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);
        presetsHoverProgress = this.approach(presetsHoverProgress, presetsHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);

        widgetGraphics.animatedButton(graphics, clearButton, clearX, rowY, clearHoverProgress, clearHovered, false, Component.translatable("itemblocker.text.clear_all"));
        widgetGraphics.animatedButton(graphics, settingsButton, optionsX, rowY, optionsHoverProgress, optionsHovered, false, Component.translatable("itemblocker.text.options"));
        widgetGraphics.animatedButton(graphics, presetsButton, presetsX, rowY, presetsHoverProgress, presetsHovered, false, Component.translatable("itemblocker.text.presets"));
    }


    private void renderTutorial(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (testTime >= 0) {
            int col = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, PROMPT_DURATION, FADE_IN, FADE_OUT), testTime);
            int col1 = fadeManager.fadeInOut(new Fade(0xFF000000, PROMPT_DURATION, FADE_IN, FADE_OUT), testTime);
            int bgColor = fadeManager.fadeInOut(new Fade(0x88000000, 39f, FADE_IN, FADE_OUT), testTime);
            int textColor = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, 39f, FADE_IN, FADE_OUT), testTime);

            graphics.fill(0, 0, width, height, bgColor);
            widgetGraphics.arrow(graphics, width / 2 - 200, height / 2 - 20, inputField.getX(), inputField.getY() + 20, 1, 5, col);
            graphics.fill(width / 2 - 300, height / 2 - 20, width / 2 - 120, height / 2 + 30, col1);
            graphics.outline(width / 2 - 300, height / 2 - 20, 180, 50, col);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.item_ids"), width / 2 - 297, height / 2 - 17, 177, col);
            graphics.text(this.font, Component.translatable("itemblocker.tutorial.skip"), 4, 8, textColor);
            if (test1Time < 0) {
                inputField.extractRenderState(graphics, mouseX, mouseY, delta);
            }

        }
        if (test1Time >= 0) {
            int col2 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, PROMPT_DURATION, FADE_IN, FADE_OUT), test1Time);
            int col3 = fadeManager.fadeInOut(new Fade(0xFF000000, PROMPT_DURATION, FADE_IN, FADE_OUT), test1Time);
            widgetGraphics.arrow(graphics, width / 2 + 200, height / 2 - 10, this.addButton.getX() + this.addButton.getWidth(), this.addButton.getY() + this.addButton.getHeight(), 1, 5, col2);
            if (test2Time < 0) {
                addButton.extractRenderState(graphics, mouseX, mouseY, delta);
            }
            graphics.outline(addButton.getX(), addButton.getY(), addButton.getWidth(), addButton.getHeight(), col2);
            graphics.fill(width / 2 + 100, height / 2 - 10, width / 2 + 280, height / 2 + 40, col3);
            graphics.outline(width / 2 + 100, height / 2 - 10, 180, 50, col2);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.submit_item"), width / 2 + 103, height / 2 - 7, 179, col2);
        }
        if (test2Time >= 0) {
            int col4 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, PROMPT_DURATION, FADE_IN, FADE_OUT), test2Time);
            int col5 = fadeManager.fadeInOut(new Fade(0xFF000000, PROMPT_DURATION, FADE_IN, FADE_OUT), test2Time);
            widgetGraphics.arrow(graphics, width / 2 - 120, height / 2 + 40, clearX + 9, rowY + 17, 1, 5, col4);
            graphics.fill(width / 2 - 180, height / 2 + 40, width / 2, height / 2 + 80, col5);
            graphics.outline(width / 2 - 180, height / 2 + 40, 180, 40, col4);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.clear_all"), width / 2 - 177, height / 2 + 43, 179, col4);
            if (test3Time < 0) {
                widgetGraphics.animatedButton(graphics, Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/bin.png"), clearX, rowY, 0.5f, false, true, Component.empty());
            }
        }
        if (test3Time >= 0) {
            int col6 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, PROMPT_DURATION, FADE_IN, FADE_OUT), test3Time);
            int col7 = fadeManager.fadeInOut(new Fade(0xFF000000, PROMPT_DURATION, FADE_IN, FADE_OUT), test3Time);
            widgetGraphics.arrow(graphics, width / 2, height / 2 + 40, optionsX + 9, rowY + 17, 1, 5, col6);
            graphics.fill(width / 2 - 50, height / 2 + 40, width / 2 + 130, height / 2 + 80, col7);
            if (test4Time < 0) {
                widgetGraphics.animatedButton(graphics, Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/settings.png"), optionsX, rowY, 0.5f, false, true, Component.empty());
            }
            graphics.outline(width / 2 - 50, height / 2 + 40, 180, 40, col6);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.options"), width / 2 - 47, height / 2 + 43, 180, col6);
        }
        if (test4Time >= 0) {
            int col6 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, 6f, FADE_IN, FADE_OUT), test4Time);
            int col7 = fadeManager.fadeInOut(new Fade(0xFF000000, 6f, FADE_IN, FADE_OUT), test4Time);
            widgetGraphics.arrow(graphics, width / 2 + 80, height / 2 + 20, presetsX + 9, rowY + 17, 1, 5, col6);
            graphics.fill(width / 2 + 30, height / 2 + 20, width / 2 + 230, height / 2 + 85, col7);
            if (test5Time < 0) {
                widgetGraphics.animatedButton(graphics, Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/presets.png"), presetsX, rowY, 0.3f, false, true, Component.empty());
            }
            graphics.outline(width / 2 + 30, height / 2 + 20, 200, 65, col6);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.presets"), width / 2 + 33, height / 2 + 23, 190, col6);
        }
        if (test5Time >= 0) {
            int col6 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, 7.5f, FADE_IN, FADE_OUT), test5Time);
            int col7 = fadeManager.fadeInOut(new Fade(0xFF000000, 7.5f, FADE_IN, FADE_OUT), test5Time);
            widgetGraphics.arrow(graphics, width / 2 - 134, this.getListY() + (this.getListHeight() / 2), this.getListX(), this.getListY() + (this.getListHeight() / 2), 1, 5, col6);
            graphics.fill(width / 2 - 310, this.getListY() + (this.getListHeight() / 2) - 30, width / 2 - 134, this.getListY() + (this.getListHeight() / 2) + 30, col7);
            graphics.outline(width / 2 - 310, this.getListY() + (this.getListHeight() / 2) - 30, 176, 60, col6);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.blocked_items"), width / 2 - 307, this.getListY() + (this.getListHeight() / 2) - 27, 170, col6);
            if (test6Time < 0) {
                this.renderWidget(graphics, mouseX, mouseY);
            }
            graphics.outline(this.getListX(), this.getListY(), this.getListWidth(), this.getListHeight(), col6);
        }
        if (test6Time >= 0) {
            int col6 = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, 5f, FADE_IN, FADE_OUT), test6Time);
            int col7 = fadeManager.fadeInOut(new Fade(0xFF000000, 5f, FADE_IN, FADE_OUT), test6Time);
            widgetGraphics.arrow(graphics, width / 2 - 30, height / 2 + 5, this.tutorialButton.getX() + this.tutorialButton.getWidth(), this.tutorialButton.getY(), 1, 5, col6);
            graphics.fill(width / 2 - 90, height / 2 + 5, width / 2 + 60, height / 2 - 35, col7);
            graphics.outline(width / 2 - 90, height / 2 - 35, 150, 40, col6);
            graphics.textWithWordWrap(this.font, Component.translatable("itemblocker.tutorial.tutorial"), width / 2 - 87, height / 2 - 32, 150, col6);
            graphics.outline(this.tutorialButton.getX(), this.tutorialButton.getY(), this.tutorialButton.getWidth(), this.tutorialButton.getHeight(), col6);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        if (inTutorial) {
            if (click.x() >= this.tutorialButton.getX() && click.x() <= this.tutorialButton.getX() + 30 &&
                    click.y() >= this.tutorialButton.getY() && click.y() <= this.tutorialButton.getY() + 30){
                return super.mouseClicked(click, doubleClick);
            }
            else {
                this.finishTutorial();
            }
        }
        int listY = this.getListY();
        int yOffset = listY + 5;
        int scrollOffset = this.getScrollOffset();

        if (click.x() >= clearX && click.x() <= clearX + 16 &&
                click.y()  >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            for (String itemId : BlockedItemsConfig.getBlockedItems()) {
                BlockedItemsConfig.removeBlockedItem(itemId);
            }
            this.refreshList();
            this.resetScrollOffset();
            Loggers.CLIENT_LOGGER.info("Cleared all blocked items");
        }

        if (click.x() >= optionsX && click.x() <= optionsX + 16 &&
                click.y() >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            minecraft.gui.setScreen(new SettingsScreen(this));
        }

        if (click.x() >= presetsX && click.x() <= presetsX + 16 &&
                click.y() >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            minecraft.gui.setScreen(new PresetsScreen(this));
        }

        List<ItemEntry> list = this.getActiveList();
        for (int i = scrollOffset; i < list.size() && i < scrollOffset + itemsPerPage; i++) {
            ItemEntry entry = list.get(i);

            int removeX = this.width / 2 + 70;
            int removeY = yOffset;


            if (click.x() >= removeX && click.x() <= removeX + 20 &&
                    click.y()  >= removeY && click.y() <= removeY + 16) {

                if (searching) {
                    if (BlockedItemsConfig.isBlocked(entry.itemId())) {
                        BlockedItemsConfig.removeBlockedItem(entry.itemId());
                    }
                    else {
                        BlockedItemsConfig.addBlockedItem(entry.itemId());
                    }
                }
                else {
                    BlockedItemsConfig.removeBlockedItem(entry.itemId());
                    if (entry == list.getLast()) {
                        if (itemsPerPage < list.size()) {
                            this.decrementScrollOffset();
                        }
                    }
                }
                this.playClickSound();
                this.refreshList();
                Loggers.CLIENT_LOGGER.info("Removed blocked item: {}", entry.itemId());
            }
            yOffset += 22;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (inTutorial) {
            this.finishTutorial();
        }
        return super.keyPressed(event);
    }


    @Override
    protected void checkItemId() {
        itemId = this.inputField.getValue().trim();
        boolean containsMinecraftNamespace = false;
        if (!itemId.isEmpty()) {
            itemId = itemId.toLowerCase().replace(" ", "_");
            String originalItemID = itemId;


            if (!itemId.contains(":")) {
                containsMinecraftNamespace = true;
                itemId = "minecraft:" + itemId;
            }


            Identifier id = Identifier.tryParse(itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                if (!itemId.equals("minecraft:air")) {
                    if (BlockedItemsConfig.getBlockedItems().contains(itemId)) {
                        successTime = -1;
                        alreadyTime = Util.getMillis();
                    }
                    else {
                        alreadyTime = -1;
                        successTime = Util.getMillis();
                    }
                    BlockedItemsConfig.addBlockedItem(itemId);
                    invalidIDStartTime = -1;
                    this.inputField.setValue("");
                    this.refreshList();
                    Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                }
            }
            else {
                if (containsMinecraftNamespace) {
                    itemId = itemId.replace("minecraft:", "");
                }
                for (String modId : ItemBlockerClient.currentModList) {
                    itemId = modId + ":" + itemId;
                    id = Identifier.tryParse(itemId);
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                        if (BlockedItemsConfig.getBlockedItems().contains(itemId)) {
                            successTime = -1;
                            alreadyTime = Util.getMillis();
                        }
                        else {
                            alreadyTime = -1;
                            successTime = Util.getMillis();
                        }
                        BlockedItemsConfig.addBlockedItem(itemId);
                        invalidIDStartTime = -1;
                        this.inputField.setValue("");
                        this.refreshList();
                        Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                        break;
                    }
                    else {
                        invalidIDStartTime = Util.getMillis();
                        alreadyTime = -1;
                        successTime = -1;
                    }
                    itemId = originalItemID;
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (testTime >= 0) {
            ++firstTicks;
            if (firstTicks == 108) {
                test1Time = Util.getMillis();
            }
            if (firstTicks == 208) {
                test2Time = Util.getMillis();
            }
            if (firstTicks == 308) {
                test3Time = Util.getMillis();
            }
            if (firstTicks == 408) {
                test4Time = Util.getMillis();
            }
            if (firstTicks == 528) {
                test5Time = Util.getMillis();
            }
            if (firstTicks == 677) {
                test6Time = Util.getMillis();
            }
            if (firstTicks == 777) {
                this.finishTutorial();
            }
        }
    }

    private void startTutorial() {
        inTutorial = true;
        this.tutorialButton.active = false;
        testTime = Util.getMillis();
    }

    private void finishTutorial() {
        inTutorial = false;
        testTime = -1;
        test1Time = -1;
        test2Time = -1;
        test3Time = -1;
        test4Time = -1;
        test5Time = -1;
        test6Time = -1;
        firstTicks = 0;
        this.tutorialButton.active = true;
        BlockedItemsConfig.finishTutorial();
    }
}