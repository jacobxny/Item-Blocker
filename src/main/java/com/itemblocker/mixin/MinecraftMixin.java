package com.itemblocker.mixin;


import com.itemblocker.api.RegistryUtils;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V", shift = At.Shift.AFTER))
    public void loadRegistry(GameConfig gameConfig, CallbackInfo ci) {
        if (BlockedItemsConfig.shouldLoadRegistryOnLaunch()) {
            RegistryUtils.loadRegistry();
        }
        ItemBlockerClient.isDev = BlockedItemsConfig.devFlagsEnabled();
        if (ItemBlockerClient.isModMenuInstalled) {
            for (Mod mod : ModMenu.MODS.values()) {
                if (this.isRemoveMod(mod)) {
                    ItemBlockerClient.currentModList.remove(mod.getId());
                }
            }
        }
        Loggers.CLIENT_LOGGER.info("Mod list: {}", ItemBlockerClient.currentModList);
    }

    @Unique
    private boolean isRemoveMod(Mod mod) {
        return mod.getBadges().contains(Mod.Badge.LIBRARY);
    }


    @Inject(method = "tick", at = @At("HEAD"))
    public void runTick(CallbackInfo ci) {
        ItemBlockerClient.tick((Minecraft) (Object) this);
    }
}
