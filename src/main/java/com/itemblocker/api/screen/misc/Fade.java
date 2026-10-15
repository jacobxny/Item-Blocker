package com.itemblocker.api.screen.misc;


public class Fade {

    private final int color;
    private final long totalMs;
    private final float fadeInDuration;
    private final float fadeOutDuration;

    public static final Fade RED_FADE = new Fade(0x00FF5555, 3.5f, 1f);
    public static final Fade GREEN_FADE = new Fade(0x0055FF55,3.5f, 1f);
    public static final Fade YELLOW_FADE = new Fade(0x00FFFF55, 3.5f, 1f);
    public static final Fade LONG_RED_FADE = new Fade(0x00FF5555, 4f, 1f);


    public Fade(int color, float duration, float fadeTime) {
        this.color = color;
        this.totalMs = (long) (duration * 1000L);
        this.fadeOutDuration = fadeTime * 1000;
        this.fadeInDuration = 0;
    }

    public Fade(int color, float duration, float fadeInTime, float fadeOutTime) {
        this.color = color;
        this.totalMs = (long) (duration * 1000L);
        this.fadeInDuration = fadeInTime * 1000;
        this.fadeOutDuration = fadeOutTime * 1000;
    }

    public int getColor() {
        return this.color;
    }

    public float getStartTime() {
        return this.totalMs - fadeOutDuration;
    }

    public float getFadeInDuration() { return fadeInDuration; }
    public float getFadeOutDuration() { return fadeOutDuration; }

    public long getTotalTime() {
        return this.totalMs;
    }
}