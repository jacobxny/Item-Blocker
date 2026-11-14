package com.itemblocker.api.screen.util;

import com.itemblocker.api.logging.IllegalTutorialStepException;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class TutorialStep {

    private final String hint;
    private final IntSupplier boxX;
    private final IntSupplier boxY;
    private final int boxW;
    private final IntSupplier targetX;
    private final IntSupplier targetY;
    private final Supplier<Rect> outline;
    private final Reveal reveal;
    private final float seconds;

    private static final float DEFAULT_STEP_SECONDS = 5f;

    private TutorialStep(
            String hint,
            IntSupplier boxX,
            IntSupplier boxY,
            int boxW,
            IntSupplier targetX,
            IntSupplier targetY,
            Supplier<Rect> outline,
            Reveal reveal,
            float seconds
    ) {
        this.hint = hint;
        this.boxX = boxX;
        this.boxY = boxY;
        this.boxW = boxW;
        this.targetX = targetX;
        this.targetY = targetY;
        this.outline = outline;
        this.reveal = reveal;
        this.seconds = seconds;
    }

    public static TutorialStep.Builder step(String hint) {
        return new TutorialStep.Builder(hint);
    }

    public String hint() {
        return this.hint;
    }

    public IntSupplier boxX() {
        return this.boxX;
    }

    public IntSupplier boxY() {
        return this.boxY;
    }

    public int boxW() {
        return this.boxW;
    }

    public IntSupplier targetX() {
        return this.targetX;
    }

    public IntSupplier targetY() {
        return this.targetY;
    }

    public Supplier<Rect> outline() {
        return this.outline;
    }

    public Reveal reveal() {
        return this.reveal;
    }

    public float seconds() {
        return this.seconds;
    }


    public static final class Builder {
        private final String hint;
        private IntSupplier boxX = null;
        private IntSupplier boxY = null;
        private Integer boxW = null;
        private IntSupplier targetX;
        private IntSupplier targetY;
        private Supplier<Rect> outline;
        private Reveal reveal;
        private float seconds = DEFAULT_STEP_SECONDS;

        private Builder(String hint) {
            this.hint = hint;
        }

        public TutorialStep.Builder box(IntSupplier x, IntSupplier y, int width) {
            this.boxX = x;
            this.boxY = y;
            this.boxW = width;
            return this;
        }

        public TutorialStep.Builder pointAt(IntSupplier x, IntSupplier y) {
            this.targetX = x;
            this.targetY = y;
            return this;
        }

        public TutorialStep.Builder outline(Supplier<Rect> outline) {
            this.outline = outline;
            return this;
        }

        public TutorialStep.Builder reveal(Reveal reveal) {
            this.reveal = reveal;
            return this;
        }

        public TutorialStep.Builder seconds(float seconds) {
            this.seconds = seconds;
            return this;
        }

        public TutorialStep build() {
            this.checkForRequirements();
            return new TutorialStep(hint, boxX, boxY, boxW, targetX, targetY, outline, reveal, seconds);
        }

        private void checkForRequirements() {
            if (boxX == null || boxY == null || boxW == null) {
                Throwable reason = new NullPointerException("boxX, boxY or boxW were null when creating tutorial step");
                throw new IllegalTutorialStepException("Must provide a box when creating a step!", reason);
            }
        }
    }

    @FunctionalInterface
    public interface Reveal {
        void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);
    }

}
