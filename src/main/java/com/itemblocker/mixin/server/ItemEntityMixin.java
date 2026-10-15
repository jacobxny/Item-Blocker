package com.itemblocker.mixin.server;

import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Debug(export = true)
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public int age;

    @Shadow
    public float bobOffs;

    @Shadow
    public int health;

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public abstract int getAge();

    @Unique
    private final static KeyOptions options = ItemBlockerClient.getOptions();

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    public void setValues(EntityType<? extends ItemEntity> type, Level level, CallbackInfo ci) {
        if (ItemUtil.getCurrentItemInMemory() != null) {
            if (ItemUtil.isItemFromGround && BlockedItemsConfig.isBlocked(ItemUtil.getCurrentItemInMemory())) {
                this.age = ItemUtil.age();
                this.bobOffs = ItemUtil.bobOffset();
                this.health = ItemUtil.health();
            }
        }
    }

    @ModifyArg(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setYRot(F)V"))
    public float setYRot(float value) {
        if (ItemUtil.getCurrentItemInMemory() != null ) {
            if (ItemUtil.isItemFromGround && BlockedItemsConfig.isBlocked(ItemUtil.getCurrentItemInMemory())) {
                return ItemUtil.yRot();
            }
        }
        return value;
    }

    @ModifyArg(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"), index = 1)
    private int setCount (int count) {
        ItemStack item = this.getItem();
        if (BlockedItemsConfig.isBlocked(item)) {
            return 0;
        }
        return count;
    }

    @ModifyReturnValue(method = "getVisualRotationYInDegrees", at = @At("RETURN"))
    public float setVisualRotation(float original) {
        if (BlockedItemsConfig.isBlocked(this.getItem())) {
            return ItemUtil.visualRotation();
        }
        return original;
    }


    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onCollideWithEntity(Player player, CallbackInfo ci) {
        Entity item = (ItemEntity) (Object) this;
        ItemStack stack = this.getItem();
        if (BlockedItemsConfig.isBlocked(stack)) {
            ItemUtil.setPos(item.position());
            ItemUtil.setBobOffset(this.bobOffs);
            ItemUtil.setHealth(this.health);
            ItemUtil.setYRot(item.getYRot());
            ItemUtil.setAge(this.getAge());
            ItemUtil.setVisualRotation(item.getVisualRotationYInDegrees());
            if (ItemBlockerClient.isDev) {
                Loggers.CLIENT_LOGGER.info("Item X = {}", ItemUtil.itemXPos());
                Loggers.CLIENT_LOGGER.info("Item Y = {}", ItemUtil.itemYPos());
                Loggers.CLIENT_LOGGER.info("Item Z = {}", ItemUtil.itemZPos());
            }

            if (!ItemBlockerClient.isUsingClientMode() && !options.isTemporaryAllowPressed() && options.isToggled()) {
                Loggers.SERVER_LOGGER.info("Blocking pickup of: {} (server-side mode)", stack.getItem());
                ci.cancel();
            }
        }
    }

    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"))
    public void cancelStatAward(Player instance, Stat<?> stat, int count, Operation<Void> original) {
        if (BlockedItemsConfig.isBlocked(this.getItem()) && !options.isTemporaryAllowPressed() && options.isToggled()) {
            original.call(instance, stat, 0);
        }
        else {
            original.call(instance, stat, count);
        }
    }
}