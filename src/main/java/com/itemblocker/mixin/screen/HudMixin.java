package com.itemblocker.mixin.screen;


import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.keymapping.KeyOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();


    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V"))
    public void cancelItem(GuiGraphicsExtractor instance, LivingEntity owner, ItemStack itemStack, int x, int y, int seed, Operation<Void> original) {
        if (options.isTemporaryAllowPressed()) {
            original.call(instance, owner, itemStack, x, y, seed);
        }
        else if (!options.isToggled()) {
            original.call(instance, owner, itemStack, x, y, seed);
        }
        else if (BlockedItemsConfig.isBlocked(itemStack)) {
            original.call(instance, owner, ItemStack.EMPTY, x, y, seed);
        }

        else {
            original.call(instance, owner, itemStack, x, y, seed);
        }
    }


    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    public void cancelItemText(GuiGraphicsExtractor instance, Font font, ItemStack itemStack, int x, int y, Operation<Void> original) {
        if (!options.isToggled()) {
            original.call(instance, font, itemStack, x, y);
            return;
        }
        else if (options.isTemporaryAllowPressed()) {
            original.call(instance, font, itemStack, x, y);
            return;
        }
        else if (BlockedItemsConfig.isBlocked(itemStack)) {
            original.call(instance, font, ItemStack.EMPTY, x, y);
        }
        original.call(instance, font, itemStack, x, y);
    }

}
