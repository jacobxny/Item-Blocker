package com.itemblocker.api.item;

import com.google.common.annotations.Beta;
import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.records.ItemEntry;
import io.netty.util.SuppressForbidden;
import jdk.jfr.Experimental;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Coerce;

public class ItemUtil {

    private static ItemStack itemStack;

    public static boolean isItemFromGround = false;

    private static double itemXPos;

    private static double itemYPos;

    private static double itemZPos;

    private static float bobOffset;

    private static int age;

    private static float visualRotation;

    private static float yRot;

    private static int health;


    public static String getItemNameFromID(String itemId, boolean shouldCapitalise, boolean registryLoaded) {
        if (registryLoaded) {
            return getItemNameFromID(itemId);
        }
        else {
            int colonIndex = itemId.indexOf(":");
            String namespace = itemId.substring(0, colonIndex + 1);
            String visualId = itemId.replace(namespace, "");
            visualId = visualId.replace("_", " ");
            if (shouldCapitalise) {
                visualId = WordUtils.capitalizeFully(visualId);
            }
            return visualId;
        }
    }

    private static String getItemNameFromID(String itemId) {
        ItemEntry entry = new ItemEntry(itemId);
        return entry.getItemName();
    }

    public static boolean isMusicDisc(ItemStack stack) {
        return stack.getComponents().has(DataComponents.JUKEBOX_PLAYABLE);
    }

    public static String getMusicDiscName(ItemStack stack) {
        JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (playable == null) {
            return "";
        }
        return playable.song().value().description().getString();
    }


    private static String getItemName(@NotNull ItemStack itemStack) {
        return itemStack.getItemName().getString();
    }

    private static String getItemId(ItemStack itemStack) {
        Item item = itemStack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.toString();
    }


    public static String getItemDetails(ItemStack stack, ItemIDMode mode) {
        if (mode == ItemIDMode.USE_ITEM_ID) {
            return getItemId(stack);
        }
        else {
            return getItemName(stack);
        }
    }


    public static ItemStack getHeldItem(Minecraft minecraft) {
        return minecraft.player.getInventory().getSelectedItem();
    }


    public static void setItemXPos(double xPos) {
        itemXPos = xPos;
    }

    public static void setItemYPos(double yPos) {
        itemYPos = yPos;
    }

    public static void setItemZPos(double zPos) {
        itemZPos = zPos;
    }

    public static void setBobOffset(float offset) {
        bobOffset = offset;
    }

    public static void setAge(int itemAge) {
        age = itemAge;
    }

    public static void setVisualRotation(float rotation) {
        visualRotation = rotation;
    }

    public static void setYRot(float yRotation) {
        yRot = yRotation;
    }

    public static void setHealth(int itemHealth) {
        health = itemHealth;
    }

    public static double itemXPos() {
        return itemXPos;
    }

    public static double itemYPos() {
        return itemYPos;
    }

    public static double itemZPos() {
        return itemZPos;
    }

    public static float bobOffset() {
        return bobOffset;
    }

    public static int age() {
        return age;
    }

    public static float visualRotation() {
        return visualRotation;
    }

    public static float yRot() {
        return yRot;
    }

    public static int health() {
        return health;
    }

    public static ItemStack getCurrentItemInMemory() {
        return itemStack;
    }

    public static void setCurrentItemInMemory(ItemStack stack) {
        itemStack = stack;
    }

    public static void setPos(Vec3 pos) {
        ItemUtil.setItemXPos(pos.x);
        ItemUtil.setItemYPos(pos.y);
        ItemUtil.setItemZPos(pos.z);
    }

}
