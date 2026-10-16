package com.itemblocker.api.screen;


import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.screen.misc.*;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.mixin.screen.CursorTypeInvoker;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class GlobalScreen extends Screen {

    private final Screen parent;

    private final SoundManager soundManager = this.minecraft.getSoundManager();

    protected final AbstractFadeManager fadeManager = this.getFadeManager();

    public static CursorType DRAG_CURSOR;



    protected GlobalScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
        this.registerCustomCursors();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, Colors.WHITE);
        int versionWidth = this.font.width(ItemBlockerClient.MOD_VERSION);
        int versionX = this.width - versionWidth - 2;
        graphics.text(this.font, ItemBlockerClient.MOD_VERSION, versionX, this.height - 10, Colors.WHITE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }


    protected @NotNull AbstractFadeManager getFadeManager() {
        return new AbstractFadeManager() {
            @Override
            public @NonNull Fade getDefaultFade() {
                return Fade.RED_FADE;
            }
        };
    }


    public void playClickSound() {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1));
    }

    public void registerCustomCursors() {
        try {
            DRAG_CURSOR = this.createCursor("drag.png", 13, 13);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register drag cursor", e);
        }
    }

    private CursorType createCursor(String fileName, int hotX, int hotY) {
        String resourcePath = "/assets/itemblocker/textures/gui/cursor/" + fileName;
        ByteBuffer pixels;
        int w, h;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1), pc = stack.mallocInt(1);
            ByteBuffer png = readResourceToByteBuffer(resourcePath);
            pixels = STBImage.stbi_load_from_memory(png, pw, ph, pc, 4);
            MemoryUtil.memFree(png);
            w = pw.get(0);
            h = ph.get(0);
        } catch (IOException e) {
            Loggers.CLIENT_LOGGER.info("Failed to read cursor image, defaulting to default cursor type");
            return CursorTypes.POINTING_HAND;
        }
        if (pixels == null) {
            Loggers.CLIENT_LOGGER.info("Failed to read pixels in STBImage, defaulting to default cursor type");
            return CursorTypes.POINTING_HAND;
        }

        @Nullable SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(w, h, SDLPixels.SDL_PIXELFORMAT_RGBA32, pixels, w * 4);
        long handle = SDLMouse.SDL_CreateColorCursor(surface, hotX, hotY);
        SDLSurface.SDL_DestroySurface(surface);
        STBImage.stbi_image_free(pixels);
        if (handle == 0L) {
            return CursorType.DEFAULT;
        }

        return CursorTypeInvoker.create("drag_cursor", handle);
    }

    private static ByteBuffer readResourceToByteBuffer(String path) throws IOException {
        try (InputStream is = GlobalScreen.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Missing resource: " + path);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        }
    }
}
