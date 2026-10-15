
package com.itemblocker.mixin.compat.pickupnotifier;


import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import fuzs.pickupnotifier.common.client.handler.AddEntriesHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
@Mixin(AddEntriesHandler.class)

public class AddEntriesHandlerMixin {
    @Unique
    private final static KeyOptions options = ItemBlockerClient.getOptions();

    @Inject(method = "addItemEntry(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private static void cancelNotification(ItemStack itemStack, int amount, CallbackInfo ci) {
        if (!options.isToggled()) {
            return;
        }

        if (BlockedItemsConfig.isBlocked(itemStack)) {
            ci.cancel();
        }
    }
}