package com.itemblocker.mixin.server;

import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {



    @Unique
    public ItemStack item = null;
    @Unique
    public final KeyOptions options = ItemBlockerClient.getOptions();

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "HEAD"))
    public void cancelStat(ItemStack itemStack, boolean thrownFromHand, Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
        item = itemStack;
    }

    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/stats/Stat;I)V"))
    public void cancelStat(ServerPlayer instance, Stat<?> stat, int count, Operation<Void> original) {
        if (BlockedItemsConfig.isBlocked(item) && !options.isTemporaryAllowPressed() && options.isToggled()) {
            original.call(instance, stat, 0);
        }
        else {
            original.call(instance, stat, count);
        }
    }

    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/Identifier;)V"))
    public void cancelStat(ServerPlayer instance, Identifier identifier, Operation<Void> original) {
        if (BlockedItemsConfig.isBlocked(item) && !options.isTemporaryAllowPressed() && options.isToggled()) {
            return;
        }
        else {
            original.call(instance, identifier);
        }
    }
}
