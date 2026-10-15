package com.itemblocker.mixin.screen;


import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.logging.Loggers;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {



    @Inject(method = "selectTab", at = @At("HEAD"))
    public void getTab(CreativeModeTab tab, CallbackInfo ci) {
        DroppingUtils.creativeTab = tab;
        Loggers.CLIENT_LOGGER.info("Creative tab is: {}", DroppingUtils.creativeTab.getType());
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void setTab(CallbackInfo ci) {
        DroppingUtils.creativeTab = CreativeModeTabs.getDefaultTab();
    }
}
