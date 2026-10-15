package com.itemblocker.mixin;

import com.itemblocker.api.item.ItemUtil;
import net.minecraft.SuppressForbidden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "handlePickItemFromBlock", at = @At("TAIL"))
    private static void pickBlockCheck(BlockPos pos, boolean includeData, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemUtil.setPos(player.position());
        ItemUtil.isItemFromGround = false;
    }

    @Inject(method = "handlePickItemFromEntity", at = @At("TAIL"))
    private static void pickEntityCheck(Entity entity, boolean includeData, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemUtil.setPos(player.position());
        ItemUtil.isItemFromGround = false;
    }

}
