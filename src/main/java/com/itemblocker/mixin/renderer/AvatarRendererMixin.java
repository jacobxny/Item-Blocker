package com.itemblocker.mixin.renderer;


import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.keymapping.KeyOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Unique
    private static ItemStack stack;

    @Unique
    private static Avatar playerAvatar;

    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();

    @ModifyArgs(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;"))
    private static void setItem(Args args) {
        if (!options.isToggled()) {
            return;
        }
        if (options.isTemporaryAllowPressed()) {
            return;
        }
        if (Minecraft.getInstance().player != null) {
            if (playerAvatar.getId() == Minecraft.getInstance().player.getId()) {
                if (BlockedItemsConfig.isBlocked(stack)) {
                    args.set(1, ItemStack.EMPTY);
                }
            }
        }
    }

    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", at = @At("HEAD"))
    private static void getItem(Avatar avatar, HumanoidArm arm, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        playerAvatar = avatar;
        stack = avatar.getItemInHand(InteractionHand.MAIN_HAND);
    }

}
