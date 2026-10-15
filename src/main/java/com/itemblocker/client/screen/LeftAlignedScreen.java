package com.itemblocker.client.screen;

import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.screen.LeftAlignedItemWidget;
import com.itemblocker.api.screen.misc.Fade;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.screen.presets.PresetsScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class LeftAlignedScreen extends LeftAlignedItemWidget {

    private EditBox itemInputField;
    private List<ItemEntry> blockedItemsList;
    private long invalidIDStartTime = -1;
    private long successTime = -1;
    private long alreadyTime = -1;
    private String itemId;


    public LeftAlignedScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.config"), parent);
    }


    @Override
    public String getHeaderText() {
        return "itemblocker.text.blocked_items";
    }

    @Override
    public int getListY() {
        return 40;
    }

    @Override
    protected int getListX() {
        return width / 2 - 320;
    }

    @Override
    protected int getListWidth() {
        return 210;
    }

    @Override
    public int getListHeight() {
        return height - 65;
    }

    @Override
    public EditBox getEditBox() {
        return itemInputField;
    }


    @Override
    public List<ItemEntry> getItemList() {
        return blockedItemsList;
    }

    @Override
    public String getEmptyListText() {
        return "itemblocker.text.no_items";
    }

    @Override
    protected void init() {
        super.init();

        if (!RegistryUtils.isRegistryLoaded) {
            RegistryUtils.loadRegistry();
        }

        BlockedItemsConfig.load();

        this.itemInputField = new EditBox(
                this.font,
                this.getListX() + 8,
                this.getListY() - 17,
                175,
                20,
                Component.translatable("itemblocker.text.item_id")
        );
        this.itemInputField.setMaxLength(256);
        this.itemInputField.setHint(Component.literal("minecraft:rotten_flesh"));

        this.addRenderableWidget(this.itemInputField);
        this.setInitialFocus(this.itemInputField);

        Button optionsButton = Button.builder(Component.literal("⚙"), button -> this.minecraft.gui.setScreen(new SettingsScreen(this)))
                .bounds(this.width / 2 + 3, this.height - 22, 30, 20)
                .build();

        this.addRenderableWidget(optionsButton);

        Button presetsButton = Button.builder(Component.translatable("itemblocker.text.presets"), button -> this.minecraft.gui.setScreen(new PresetsScreen(this)))
                .bounds(this.width / 2 - 100, this.height - 22, 95, 20)
                .build();


        this.addRenderableWidget(presetsButton);

        this.refreshList();
    }

    private void refreshList() {
        this.blockedItemsList = new ArrayList<>();
        for (String itemId : BlockedItemsConfig.getBlockedItems()) {
            this.blockedItemsList.add(new ItemEntry(itemId));
        }
        Loggers.CLIENT_LOGGER.info("Refreshed list, {} blocked items", this.blockedItemsList.size());
    }


    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (invalidIDStartTime >= 0) {
            int color = fadeManager.fadeOut(Fade.LONG_RED_FADE, invalidIDStartTime);
            graphics.text(this.font, Component.literal("Invalid Item ID, did you type it correctly?"), this.width / 2 - 103, 27, color, false);
        }
        else if (successTime >= 0) {
            int color = fadeManager.fadeOut(Fade.GREEN_FADE, successTime);
            String text = "Successfully added: ";

            if (ItemBlockerClient.isUsingIDMode()) {
                text += itemId;
            }
            else {
                text += ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            Component component = Component.literal(text);
            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }
        else if (alreadyTime >= 0) {
            int color = fadeManager.fadeOut(Fade.YELLOW_FADE, alreadyTime);
            String text = "";
            if (ItemBlockerClient.isUsingIDMode()) {
                text += itemId;
            }

            else {
                text += ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            text += " is already blocked!";
            Component component = Component.literal(text);
            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        int listY = this.getListY();
        int yOffset = listY + 5;
        int scrollOffset = this.getScrollOffset();

        for (int i = scrollOffset; i < blockedItemsList.size() && i < scrollOffset + itemsPerPage; i++) {
            ItemEntry entry = blockedItemsList.get(i);


            int removeX = this.getListX() + 175;
            int removeY = yOffset;


            if (click.x() >= removeX && click.x() <= removeX + 20 &&
                    click.y()  >= removeY && click.y() <= removeY + 16) {
                BlockedItemsConfig.removeBlockedItem(entry.itemId());
                if (entry == blockedItemsList.getLast()) {
                    if (itemsPerPage < blockedItemsList.size()) {
                        this.decrementScrollOffset();
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
    protected void checkItemId() {
        itemId = this.itemInputField.getValue().trim();
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
                    this.itemInputField.setValue("");
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
                        this.itemInputField.setValue("");
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
}
