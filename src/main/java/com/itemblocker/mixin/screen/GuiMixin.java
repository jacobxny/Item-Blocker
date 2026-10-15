package com.itemblocker.mixin.screen;


import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.screen.AbstractItemWidget;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {


    @Inject(method = "setScreen", at = @At("HEAD"))
    public void getScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof AbstractItemWidget)) {
            AbstractItemWidget.clearScreenEffects();
        }
        if (ItemBlockerClient.isDev) {
            Loggers.CLIENT_LOGGER.info("Screen set to: {}", screen);
        }
    }
}
