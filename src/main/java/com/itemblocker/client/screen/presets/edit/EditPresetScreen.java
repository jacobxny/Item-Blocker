package com.itemblocker.client.screen.presets.edit;

import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.screen.misc.Fade;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.screen.AbstractItemWidget;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.config.PresetsConfig;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.records.ItemEntry;
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

public class EditPresetScreen extends AbstractItemWidget {
    private final String presetName;
    private EditBox itemInputField;
    private List<ItemEntry> itemsList;
    private long invalidIDStartTime = -1;
    private long successTime = -1;
    private long alreadyTime = -1;
    private String itemId;

    public EditPresetScreen(Screen parent, String presetName) {
        super(Component.translatable("itemblocker.text.edit_preset", presetName), parent);
        this.presetName = presetName;
    }

    @Override
    public String getHeaderText() {
        return "itemblocker.text.items_in_preset";
    }

    @Override
    public int getListY() {
        return 95;
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
    protected EditBox getEditBox() {
        return itemInputField;
    }

    @Override
    public int getListHeight() {
        return height - 135;
    }


    @Override
    public List<ItemEntry> getItemList() {
        return itemsList;
    }

    @Override
    public String getEmptyListText() {
        return "itemblocker.text.no_preset_items";
    }

    @Override
    protected boolean isItemSelected(String itemId) {
        return PresetsConfig.getPreset(presetName).contains(itemId);
    }

    @Override
    protected void init() {
        super.init();
        this.itemInputField = new EditBox(
                this.font,
                this.width / 2 - 100,
                40,
                200,
                20,
                Component.translatable("itemblocker.text.item_id")
        );
        this.itemInputField.setMaxLength(256);
        this.itemInputField.setResponder(text -> {
            editBoxQuery = text;
            this.refreshResults();
        });
        this.addRenderableWidget(this.itemInputField);
        this.setInitialFocus(this.itemInputField);


        Button editButton = Button.builder(Component.translatable("Edit Name"), button -> {
                    minecraft.gui.setScreen(new EditPresetNameScreen(this, presetName));
                })
                .bounds(this.width / 2 + 110, this.height - 30, 95, 20)
                .build();
        this.addRenderableWidget(editButton);


        this.refreshList();
    }


    private void refreshList() {
        this.itemsList = new ArrayList<>();
        for (String itemId : PresetsConfig.getPreset(presetName)) {
            this.itemsList.add(new ItemEntry(itemId));
        }
        this.sortList(itemsList);
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
            graphics.text(font, toRender, (this.width / 2) - font.width(toRender) / 2, 27, color, false);
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
            graphics.text(font, toRender, (this.width / 2) - font.width(toRender) / 2, 27, color, false);
        }
    }


    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        int listY = this.getListY();
        int yOffset = listY + 5;
        int scrollOffset = this.getScrollOffset();

        List<ItemEntry> list = this.getActiveList();
        for (int i = scrollOffset; i < list.size() && i < scrollOffset + itemsPerPage; i++) {
            ItemEntry entry = list.get(i);

            int removeX = this.width / 2 + 70;
            int removeY = yOffset;


            if (click.x() >= removeX && click.x() <= removeX + 20 &&
                    click.y()  >= removeY && click.y() <= removeY + 16) {

                if (searching) {
                    if (PresetsConfig.getPreset(presetName).contains(entry.itemId())) {
                        PresetsConfig.removeItemFromPreset(presetName,entry.itemId());
                    }
                    else {
                        PresetsConfig.addItemToPreset(presetName,entry.itemId());
                    }
                }
                else {
                    PresetsConfig.removeItemFromPreset(presetName, entry.itemId());
                    if (entry == list.getLast()) {
                        if (itemsPerPage < list.size()) {
                            this.decrementScrollOffset();
                        }
                    }
                }
                this.playClickSound();
                this.refreshList();
            }

            yOffset += 22;
        }

        return super.mouseClicked(click, doubleClick);
    }


    @Override
    protected void checkItemId() {
        itemId = this.itemInputField.getValue().trim();
        if (!itemId.isEmpty()) {
            itemId = itemId.toLowerCase().replace(" ", "_");
            String normalItemName = itemId;

            if (!itemId.contains(":")) {
                itemId = "minecraft:" + itemId;
            }

            Identifier id = Identifier.tryParse(itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                if (!itemId.equals("minecraft:air")) {
                    if (PresetsConfig.getPreset(presetName).contains(itemId)) {
                        alreadyTime = -1;
                        alreadyTime = Util.getMillis();
                    }
                    else {
                        alreadyTime = -1;
                        successTime = Util.getMillis();
                    }
                    PresetsConfig.addItemToPreset(presetName, itemId);
                    invalidIDStartTime = -1;
                    this.itemInputField.setValue("");
                    this.refreshList();
                    Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                }
            } else {
                itemId = itemId.replace("minecraft:", "");
                for (String modId : ItemBlockerClient.currentModList) {
                    itemId = modId + ":" + itemId;
                    id = Identifier.tryParse(itemId);
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                        if (PresetsConfig.getPreset(presetName).contains(itemId)) {
                            successTime = -1;
                            alreadyTime = Util.getMillis();
                        }
                        else {
                            alreadyTime = -1;
                            successTime = Util.getMillis();
                        }
                        PresetsConfig.addItemToPreset(presetName, itemId);
                        invalidIDStartTime = -1;
                        this.itemInputField.setValue("");
                        this.refreshList();
                        Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                        break;
                    }
                    else {
                        successTime = -1;
                        alreadyTime = -1;
                        invalidIDStartTime = Util.getMillis();
                    }
                    itemId = normalItemName;
                }
            }
        }
    }
}