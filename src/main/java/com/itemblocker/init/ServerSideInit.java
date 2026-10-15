package com.itemblocker.init;

import com.itemblocker.api.logging.Loggers;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
* Nothing actually is in this class, just the {@code onInitialize} and {@code onInitializeServer} methods.
* <p>
* Just registers the mod mixins on the server side.
*/

public class ServerSideInit implements DedicatedServerModInitializer, ModInitializer{

    @Override
    public void onInitializeServer() {
    }

    @Override
    public void onInitialize() {
    }
}
