package com.itemblocker.mixin.renderer;


import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.logging.Loggers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmedEntityRenderState.class)
public abstract class ArmedEntityRenderStateMixin {


    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();

    @Unique
    private static LivingEntity livingEntity;



    @Inject(method = "extractArmedEntityRenderState", at = @At(value = "TAIL"))
    private static void setStateItem(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver itemModelResolver, float partialTicks, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (!options.isToggled()) {
                return;
            }
            if (options.isTemporaryAllowPressed()) {
                return;
            }
            if (entity.getId() == player.getId()) {
                ItemStack rightArmStack = entity.getItemHeldByArm(HumanoidArm.RIGHT);
                ItemStack leftArmStack = entity.getItemHeldByArm(HumanoidArm.LEFT);

                if (BlockedItemsConfig.isBlocked(rightArmStack)) {
                    Loggers.CLIENT_LOGGER.info("isRightBlocked = {}", BlockedItemsConfig.isBlocked(rightArmStack));
                    state.rightHandItemStack = ItemStack.EMPTY;
                }
                if (BlockedItemsConfig.isBlocked(leftArmStack)) {
                    Loggers.CLIENT_LOGGER.info("isLeftBlocked = {}", BlockedItemsConfig.isBlocked(leftArmStack));
                    state.leftHandItemStack = ItemStack.EMPTY;
                }
            }
        }
    }

    @ModifyArg(method = "extractArmedEntityRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/LivingEntity;)V", ordinal = 0), index = 1)
    private static ItemStack setRightItem(ItemStack item) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (options.isTemporaryAllowPressed()) {
            return item;
        }
        if (!options.isToggled()) {
            return item;
        }
        if (player != null) {
            if (livingEntity.getId() == player.getId()) {
                if (BlockedItemsConfig.isBlocked(item)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return item;
    }

    @ModifyArg(method = "extractArmedEntityRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/LivingEntity;)V", ordinal = 1), index = 1)
    private static ItemStack setLeftItem(ItemStack item) {
        if (options.isTemporaryAllowPressed()) {
            return item;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (!options.isToggled()) {
            return item;
        }
        if (player != null) {
            if (livingEntity.getId() == player.getId()) {
                if (BlockedItemsConfig.isBlocked(item)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return item;
    }

    @Inject(method = "extractArmedEntityRenderState", at = @At("HEAD"))
    private static void getEntity(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver itemModelResolver, float partialTicks, CallbackInfo ci) {
        livingEntity = entity;
    }
}
