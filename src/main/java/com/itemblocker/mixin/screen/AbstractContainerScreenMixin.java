package com.itemblocker.mixin.screen;

import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.keymapping.KeyOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {


    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();

    @Shadow
    public Slot hoveredSlot;

    @Unique
    private static boolean shouldHide(Slot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!options.isToggled() || options.isTemporaryAllowPressed()) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || slot.container != player.getInventory()) {
            return false;
        }
        if (slot.getContainerSlot() >= DroppingUtils.PLAYER_INVENTORY_SLOTS) {
            return false;
        }

        return BlockedItemsConfig.isBlocked(stack);
    }

    @WrapOperation(
            method = "extractSlot",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack hideItem(Slot slot, Operation<ItemStack> original) {
        ItemStack stack = original.call(slot);
        return shouldHide(slot, stack) ? ItemStack.EMPTY : stack;
    }

    @Inject(
            method = "extractTooltip",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER),
            cancellable = true
    )
    private void hideTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (hoveredSlot != null && shouldHide(hoveredSlot, hoveredSlot.getItem())) {
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void setPos(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemUtil.setItemXPos(player.getX());
        ItemUtil.setItemYPos(player.getY());
        ItemUtil.setItemZPos(player.getZ());
    }

}