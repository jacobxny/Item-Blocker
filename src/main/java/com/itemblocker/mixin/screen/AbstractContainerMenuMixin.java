package com.itemblocker.mixin.screen;

import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow
    public abstract ItemStack getCarried();


    @Inject(method = "doClick", at = @At("HEAD"))
    public void getIndex(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        Loggers.CLIENT_LOGGER.info("input = {}", containerInput);
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (BlockedItemsConfig.isBlocked(this.getCarried())) {
            if (!ItemUtil.isItemFromGround) {
                if (localPlayer != null) {
                    ItemUtil.setPos(localPlayer.position());
                }
            }
        }
        Loggers.CLIENT_LOGGER.info("Index of slot is: {}", slotIndex);
    }

}
