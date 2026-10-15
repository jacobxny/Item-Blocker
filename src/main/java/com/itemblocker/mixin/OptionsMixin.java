package com.itemblocker.mixin;

import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.client.ItemBlockerClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin {

    @Final
    @Mutable
    @Shadow
    public KeyMapping[] keyMappings;

    @Unique
    public KeyOptions options = ItemBlockerClient.getOptions();

    @Inject(method = "load()V", at = @At("HEAD"))
    public void registerKeyMappings(CallbackInfo ci) {
        keyMappings = options.registerKeyMappings(keyMappings);
    }
}