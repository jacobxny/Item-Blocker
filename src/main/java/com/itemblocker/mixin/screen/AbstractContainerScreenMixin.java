package com.itemblocker.mixin.screen;


import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyHandler;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.keymapping.KeyOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {


    @Shadow
    public Slot hoveredSlot;
    @Unique
    private static ItemStack stack;

    @Unique
    private static Slot itemSlot;

    @Unique
    private static boolean isCreativeInvScreen;
    @Unique
    private static boolean isContainerScreen;
    @Unique
    private static boolean isCraftingScreen;
    @Unique
    private static boolean isAnvilScreen;
    @Unique
    private static boolean isHopperScreen;
    @Unique
    private static boolean isHopperInventory;
    @Unique
    private static boolean isAnvilInventory;
    @Unique
    private static boolean isCreativeHotbar;
    @Unique
    private static boolean isContainerInventory;
    @Unique
    private static boolean isDefaultInventory;
    @Unique
    private static boolean isCraftingInventory;
    @Unique
    private static boolean isDefaultInventoryScreen;
    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();


    @Inject(method = "extractSlot", at = @At("HEAD"))
    public void getItem(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        stack = slot.getItem();
        itemSlot = slot;
        setVariables();
    }

    @ModifyArgs(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void setItemSlot(Args args) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (BlockedItemsConfig.isBlocked(stack)) {
            if (isCreativeInvScreen && DroppingUtils.creativeTab.getType() == CreativeModeTab.Type.INVENTORY) {
                if (isDefaultInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }
            else if (isCreativeInvScreen) {
                if ((DroppingUtils.creativeTab.getType() != CreativeModeTab.Type.INVENTORY) && isCreativeHotbar) {
                    args.set(0, ItemStack.EMPTY);
                }
            }

            if (isHopperScreen) {
                if (isHopperInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }

            if (isCraftingScreen) {
                if (isCraftingInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }

            if (isDefaultInventoryScreen) {
                if (isDefaultInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }

            if (isAnvilScreen) {
                if (isAnvilInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }


            if (isContainerScreen) {
                if (isContainerInventory) {
                    args.set(0, ItemStack.EMPTY);
                }
            }
        }
    }

    @Inject(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"), cancellable = true)
    public void cancelItemText(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (BlockedItemsConfig.isBlocked(stack)) {
            if (isCreativeInvScreen && DroppingUtils.creativeTab.getType() == CreativeModeTab.Type.INVENTORY) {
                if (isDefaultInventory) {
                    ci.cancel();
                }
            }
            else if (isCreativeInvScreen) {
                if ((DroppingUtils.creativeTab.getType() != CreativeModeTab.Type.INVENTORY) && isCreativeHotbar) {
                    if (BlockedItemsConfig.isBlocked(stack)) {
                        ci.cancel();
                    }
                }
            }
            if (isDefaultInventoryScreen) {
                if (isDefaultInventory) {
                    ci.cancel();
                }
            }

            if (isCraftingScreen) {
                if (isCraftingInventory) {
                    ci.cancel();
                }
            }


            if (isHopperScreen) {
                if (isHopperInventory) {
                    ci.cancel();
                }
            }

            if (isAnvilScreen) {
                if (isAnvilInventory) {
                    ci.cancel();
                }
            }

            if (isContainerScreen) {
                if (isContainerInventory) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "extractTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER), cancellable = true)
    public void cancelTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!options.isToggled()) {
            return;
        }
        ItemStack item = hoveredSlot.getItem();
        if (BlockedItemsConfig.isBlocked(item)) {
            ci.cancel();
        }
    }

    @Unique
    private static void setVariables() {
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        isCreativeInvScreen = currentScreen instanceof CreativeModeInventoryScreen;
        isContainerScreen = currentScreen instanceof ContainerScreen;
        isCraftingScreen = currentScreen instanceof CraftingScreen;
        isAnvilScreen = currentScreen instanceof AnvilScreen;
        isHopperScreen = currentScreen instanceof HopperScreen;
        isDefaultInventoryScreen = currentScreen instanceof InventoryScreen;
        isHopperInventory = itemSlot.index >= 5 && itemSlot.index <= 40;
        isAnvilInventory = itemSlot.index >= 3 && itemSlot.index <= 38;
        isCreativeHotbar = itemSlot.index >= 45 && itemSlot.index <= 53;
        isContainerInventory = itemSlot.index >= 27 && itemSlot.index <= 62;
        isDefaultInventory = itemSlot.index >= 9 && itemSlot.index <= 44;
        isCraftingInventory = itemSlot.index >= 10 && itemSlot.index <= 45;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void setPos(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemUtil.setItemXPos(player.getX());
        ItemUtil.setItemYPos(player.getY());
        ItemUtil.setItemZPos(player.getZ());
    }

}
