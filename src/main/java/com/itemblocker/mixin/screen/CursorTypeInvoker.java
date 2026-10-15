package com.itemblocker.mixin.screen;

import com.itemblocker.api.screen.GlobalScreen;
import com.mojang.blaze3d.platform.cursor.CursorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CursorType.class)
public interface CursorTypeInvoker {

    @Invoker("<init>")
    static CursorType create(String name, long handle) {
        throw new AssertionError();
    }
}
