package com.itemblocker.mixin.server;


import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Shadow
    public abstract ItemStack getPickupItem();

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    public void setPositions(Player player, CallbackInfo ci) {
        KeyOptions options = ItemBlockerClient.getOptions();
        if (BlockedItemsConfig.isBlocked(this.getPickupItem())) {
            if (!ItemBlockerClient.isUsingClientMode() && !options.isTemporaryAllowPressed() && options.isToggled()) {
                ci.cancel();
            }
            ItemUtil.setPos(player.position());
        }
    }
}
