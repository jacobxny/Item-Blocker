package com.itemblocker.api.records;

import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemEntry(String itemId) {

    private static boolean exceptionThrown = false;

    public ItemStack getItemStack() {
        try {
            Identifier id = Identifier.tryParse(itemId);

            if (id != null) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                return new ItemStack(item);
            }
        } catch (Exception e) {
            if (!exceptionThrown) {
                Loggers.CLIENT_LOGGER.error("Registry not loaded yet!", e);
            }
            exceptionThrown = true;
        }
        return ItemStack.EMPTY;
    }

    public String getItemName() {
        ItemStack stack = this.getItemStack();
        return ItemUtil.getItemDetails(stack, ItemIDMode.USE_ITEM_NAMES);
    }
}

