package com.itemblocker.api.screen.misc;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;



@SuppressWarnings("unused")
public abstract class AbstractFadeManager {


    public abstract @NotNull Fade getDefaultFade();

    public int fadeOut(@Nullable Fade fade, long startTime) {
        if (fade == null) {
            fade = this.getDefaultFade();
            Objects.requireNonNull(fade, "Cannot have a null default fade!");
        }

        return this.fadeOut(fade.getTotalTime(), fade.getStartTime(), fade.getColor(), startTime);
    }

    public int fadeIn(@Nullable Fade fade, long startTime) {
        if (fade == null) {
            fade = this.getDefaultFade();
            Objects.requireNonNull(fade, "Cannot have a null default fade!");
        }

        return this.fadeIn(fade.getTotalTime(), fade.getStartTime(), fade.getColor(), startTime);
    }


    public int fadeOut(long duration, float fadeStart, int color, long startTime) {
        long elapsed = Util.getMillis() - startTime;

        if (elapsed < duration) {
            int alpha;
            if (elapsed < fadeStart) {
                alpha = 255;
            } else {
                float fadeProgress = (elapsed - fadeStart) / (duration - fadeStart);
                alpha = (int) (255 * (1f - fadeProgress));
                alpha = Mth.clamp(alpha, 0, 255);
            }
            return (alpha << 24) | color;
        }
        return color;
    }

    public int fadeIn(long duration, float fadeStart, int color, long startTime) {
        long elapsed = Util.getMillis() - startTime;
        int targetAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        if (elapsed < duration) {
            int alpha;
            if (elapsed < fadeStart) {
                alpha = 0;
            } else {
                float fadeProgress = (elapsed - fadeStart) / (duration - fadeStart);
                alpha = (int) (targetAlpha * fadeProgress);
                alpha = Mth.clamp(alpha, 0, targetAlpha);
            }
            return (alpha << 24) | rgb;
        }
        return color;
    }

    public int fadeInOut(Fade fade, long startTime) {
        long duration = fade.getTotalTime();
        float fadeInDuration = fade.getFadeInDuration();
        float fadeOutDuration = fade.getFadeOutDuration();
        int color = fade.getColor();

        return fadeInOut(duration, fadeInDuration, fadeOutDuration, color, startTime);
    }

    public int fadeInOut(
            long duration,
            float fadeInDuration,
            float fadeOutDuration,
            int color,
            long startTime
    ) {
        long elapsed = Util.getMillis() - startTime;

        int targetAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int alpha;

        if (elapsed >= duration) {
            alpha = 0;
        } else if (elapsed < fadeInDuration) {
            alpha = (int) (targetAlpha * (elapsed / fadeInDuration));
        } else if (elapsed > duration - fadeOutDuration) {
            alpha = (int) (targetAlpha * ((duration - elapsed) / fadeOutDuration));
        } else {
            alpha = targetAlpha;
        }

        alpha = Mth.clamp(alpha, 0, targetAlpha);
        return (alpha << 24) | rgb;
    }

}