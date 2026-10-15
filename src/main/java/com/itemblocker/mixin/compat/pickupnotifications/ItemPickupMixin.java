package com.itemblocker.mixin.compat.pickupnotifications;

import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.keymapping.KeyOptions;
import me.roundaround.pickupnotifications.event.ItemPickup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ItemPickup.class)
public interface ItemPickupMixin {


    @Inject(method = "emit", at = @At("HEAD"), cancellable = true)
    private static void cancelEmit(ItemStack stack, CallbackInfo ci) {
        KeyOptions options = ItemBlockerClient.getOptions();
        if (!options.isToggled()) {
            return;
        }

        if (BlockedItemsConfig.isBlocked(stack)) {
            ci.cancel();
        }
    }

}
