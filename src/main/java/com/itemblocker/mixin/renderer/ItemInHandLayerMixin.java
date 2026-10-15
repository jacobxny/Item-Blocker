package com.itemblocker.mixin.renderer;

import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.keymapping.KeyOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", ordinal = 0))
    public void cancelRender(ItemInHandLayer instance, ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Operation<Void> original) {
        if (!options.isToggled()) {
            original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
            return;
        }

        if (options.isTemporaryAllowPressed()) {
            original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
            return;
        }
        if (!itemStack.isEmpty()) {
            if (BlockedItemsConfig.isBlocked(itemStack)) {
                original.call(instance, state, item, ItemStack.EMPTY, arm, poseStack, submitNodeCollector, lightCoords);
                return;
            }
        }

        original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);

    }

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", ordinal = 1))
    public void cancelRender2(ItemInHandLayer instance, ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Operation<Void> original) {
        if (!options.isToggled()) {
            original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
            return;
        }

        if (options.isTemporaryAllowPressed()) {
            original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
            return;
        }
        if (!itemStack.isEmpty()) {
            if (BlockedItemsConfig.isBlocked(itemStack)) {
                original.call(instance, state, item, ItemStack.EMPTY, arm, poseStack, submitNodeCollector, lightCoords);
                return;
            }
        }
        original.call(instance, state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, name = "itemStack")
    public ItemStack renderEmpty(ItemStack itemStack) {
        if (options.isTemporaryAllowPressed()) {
            return itemStack;
        }
        if (!options.isToggled()) {
            return itemStack;
        }
        if (BlockedItemsConfig.isBlocked(itemStack)) {
            return ItemStack.EMPTY;
        }

        return itemStack;
    }
}
