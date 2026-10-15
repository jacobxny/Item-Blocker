package com.itemblocker.mixin.server;


import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.item.ItemUtil;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.logging.Logger;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Unique
    private static ItemStack stack;

    @Unique
    private static ItemStack item;

    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();


    @ModifyArgs(method = "createItemStackToDrop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V", ordinal = 0))
    public void cancelVelocity(Args args) {
        if (!options.isToggled()) {
            return;
        }

        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (stack != null) {
            if (BlockedItemsConfig.isBlocked(stack)) {
                args.set(0, 0.0);
                args.set(1, 0.0);
                args.set(2, 0.0);
            }
        }
    }

    @ModifyArgs(method = "createItemStackToDrop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V", ordinal = 1))
    public void cancelVelocityTwo(Args args) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (stack != null) {
            if (BlockedItemsConfig.isBlocked(stack)) {
                args.set(0, 0.0);
                args.set(1, 0.0);
                args.set(2, 0.0);
            }
        }
    }

    @ModifyArgs(method = "createItemStackToDrop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    public void setItemPos(Args args) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (BlockedItemsConfig.isBlocked(stack)) {
            args.set(1, ItemUtil.itemXPos());
            args.set(2, ItemUtil.itemYPos());
            args.set(3, ItemUtil.itemZPos());
        }
    }




    @Inject(method = "createItemStackToDrop", at = @At("HEAD"))
    public void setStack(ItemStack itemStack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        stack = itemStack;
    }


    @Inject(method = "drop", at = @At("HEAD"))
    public void getItem(ItemStack itemStack, boolean thrownFromHand, Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
        item = itemStack;
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    public void cancelSwing(InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (item != null) {
            if (BlockedItemsConfig.isBlocked(item)) {
                Loggers.CLIENT_LOGGER.info("should cancel!");
                cir.cancel();
            }
        }
    }
}
