package com.itemblocker.api.screen;

import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.enums.MouseClick;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class LeftAlignedItemWidget extends GlobalScreen {


    protected LeftAlignedItemWidget(Component title, Screen parent) {
        super(title, parent);
    }



    private static final Identifier MISSING_TEXTURE = Identifier.withDefaultNamespace("textures/item/barrier.png");

    protected int scrollOffset;
    public static boolean isDragging = false;
    private boolean invalidItemEntry = false;
    public int itemsPerPage;
    public int maxScroll;

    private boolean isDraggingScrollbar = false;
    private int scrollbarDragGrabOffset = 0;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 12;
    private static final int SCROLLBAR_ARROW_HEIGHT = 7;


    private static final Map<String, Float> shakeTimers = new HashMap<>();
    private static String draggedItemId = null;
    private int dragOriginX, dragOriginY;
    private int dragGrabOffsetX, dragGrabOffsetY;
    private float dragCurrentX, dragCurrentY;
    protected static boolean springingBack = false;

    private float dragVelX = 0f, dragVelY = 0f;
    private static final float SPRING_STIFFNESS = 220f;
    private static final float SPRING_DAMPING = 18f;
    private static final float SPRING_SNAP_THRESHOLD = 0.35f;
    private static final float SPRING_VEL_THRESHOLD = 4f;

    private float dragTiltAngle = 0f;
    private float lastDragMouseX = 0f;
    private long lastDragNanoTime = 0L;
    private static final float TILT_MAX_DEGREES = 18f;
    private static final float TILT_VELOCITY_SCALE = 0.45f;
    private static final float TILT_RESPONSIVENESS = 22f;

    private long lastRenderNanoTime = 0L;

    private boolean canScrollUp = false;
    private boolean canScrollDown = false;

    protected abstract String getHeaderText();

    protected abstract int getListY();

    protected abstract int getListHeight();

    protected abstract int getListX();

    protected abstract int getListWidth();

    protected abstract EditBox getEditBox();


    protected abstract List<ItemEntry> getItemList();

    protected abstract String getEmptyListText();

    @Override
    protected void init() {

        Button doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 310, this.height - 22, 200, 20)
                .build();

        this.addRenderableWidget(doneButton);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.itemsPerPage = (this.getListHeight() - 10) / 22;
        this.maxScroll = Math.max(0, this.getItemList().size() - itemsPerPage);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);
        this.renderWidget(graphics, mouseX, mouseY, delta);
    }

    private void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int listY = this.getListY();
        int listHeight = this.getListHeight();
        List<ItemEntry> itemEntries = this.getItemList();


        if (itemEntries == null) {
            itemEntries = new ArrayList<>();
            for (String itemID : BlockedItemsConfig.getBlockedItems()) {
                itemEntries.add(new ItemEntry(itemID));
            }
        }
        if (itemsPerPage < itemEntries.size()) {
            this.renderScrollBar(graphics, mouseX, mouseY);
        }
        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanoTime == 0L ? 0f : (now - lastRenderNanoTime) / 1000000000f;
        deltaSeconds = Math.min(deltaSeconds, 0.05f);
        lastRenderNanoTime = now;




        String headerText = this.getHeaderText();
        if (headerText == null) {
            headerText = "";
        }


        graphics.text(this.font, Component.literal("Enter an Item ID:"), this.getListX() + 8, this.getListY() - 32, Colors.WHITE);

        String noItemText = this.getEmptyListText();
        if (noItemText == null) {
            noItemText = "";
        }

        else if (itemEntries.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable(noItemText),
                    width / 2,
                    listY + listHeight / 2,
                    0xFF888888
            );

        } else {
            int yOffset = listY + 5;
            int itemsPerPage = (listHeight - 10) / 22;

            ItemEntry draggedEntry = null;

            for (int i = scrollOffset; i < itemEntries.size() && i < scrollOffset + itemsPerPage; i++) {
                ItemEntry entry = itemEntries.get(i);

                int itemX = this.getListX() + 10;
                int itemY = yOffset;

                boolean isBeingDragged = entry.itemId().equals(draggedItemId);

                if (isBeingDragged) {
                    draggedEntry = entry;
                    yOffset += 22;
                    continue;
                }

                if (mouseX >= itemX && mouseX <= itemX + 14 &&
                        mouseY >= itemY && mouseY <= itemY + 14) {
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }


                this.renderItemIcon(graphics, entry, itemX, itemY, 0f);
                this.renderItemRow(graphics, entry, yOffset, mouseX, mouseY);

                yOffset += 22;
            }

            if (draggedEntry != null) {
                if (draggedItemId != null && !springingBack) {
                    dragCurrentX = mouseX - dragGrabOffsetX;
                    dragCurrentY = mouseY - dragGrabOffsetY;
                    this.updateDragTilt(mouseX, now, deltaSeconds);
                } else if (springingBack) {
                    this.stepSpring(deltaSeconds);
                    dragTiltAngle = this.approach(dragTiltAngle, 0f, TILT_RESPONSIVENESS * 1.5f, deltaSeconds);
                }

                this.renderItemIcon(graphics, draggedEntry, dragCurrentX, dragCurrentY, dragTiltAngle);
                int normalRowY = listY + 5 + (itemEntries.indexOf(draggedEntry) - scrollOffset) * 22;
                this.renderItemRow(graphics, draggedEntry, normalRowY, mouseX, mouseY);
            }
        }

    }


    protected void renderScrollBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int trackX0 = this.scrollbarX0();
        int trackX1 = this.scrollbarX1();
        int fullY0 = this.getListY();
        int fullY1 = fullY0 + this.getListHeight();

        graphics.fill(trackX0, fullY0, trackX1, fullY1, 0xFF0C0C0C);


        canScrollUp = scrollOffset > 0;
        canScrollDown = scrollOffset < maxScroll;
        boolean upHovered = canScrollUp && mouseX >= trackX0 && mouseX <= trackX1
                && mouseY >= fullY0 && mouseY <= fullY0 + SCROLLBAR_ARROW_HEIGHT;
        boolean downHovered = canScrollDown && mouseX >= trackX0 && mouseX <= trackX1
                && mouseY >= fullY1 - SCROLLBAR_ARROW_HEIGHT && mouseY <= fullY1;

        this.renderScrollArrow(graphics, trackX0, fullY0, true, canScrollUp, upHovered);
        this.renderScrollArrow(graphics, trackX0, fullY1 - SCROLLBAR_ARROW_HEIGHT, false, canScrollDown, downHovered);

        if (upHovered || downHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        ScrollThumbGeometry thumb = this.computeScrollThumb();
        if (thumb == null) {
            return;
        }

        int thumbY0 = thumb.y();
        int thumbY1 = thumbY0 + thumb.height();

        boolean thumbHovered = mouseX >= trackX0 && mouseX <= trackX1 && mouseY >= thumbY0 && mouseY <= thumbY1 && !isDragging && !springingBack;

        int base = isDraggingScrollbar ? 0xFFA6A6A5 : (thumbHovered ? 0xFF8A8A89 : 0xFF696868);
        int highlight = 0xFF9C9C9B;
        int shadow = 0xFF3E3E3D;

        graphics.fill(trackX0 + 1, thumbY0, trackX1 - 1, thumbY1, base);
        graphics.fill(trackX0 + 1, thumbY0, trackX1 - 1, thumbY0 + 1, highlight);
        graphics.fill(trackX0 + 1, thumbY1 - 1, trackX1 - 1, thumbY1, shadow);

        if (thumbHovered || isDraggingScrollbar) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    /**
     * Draws one arrow button (up or down) at the given top-left corner. Dimmed when
     * there's nothing left to scroll that direction, brightened on hover.
     */
    private void renderScrollArrow(GuiGraphicsExtractor graphics, int x0, int y0, boolean pointingUp, boolean enabled, boolean hovered) {
        int x1 = x0 + SCROLLBAR_WIDTH;

        int y1 = y0 + SCROLLBAR_ARROW_HEIGHT;


        int arrowColor = enabled ? (hovered ? 0xFFEDEDEC : 0xFFB8B8B7) : 0xFF4F4F4E;
        int cx = x0 + SCROLLBAR_WIDTH / 2;
        int arrowTop = y0 + (SCROLLBAR_ARROW_HEIGHT - 3) / 2;

        for (int row = 0; row < 3; row++) {
            int halfWidth = pointingUp ? row : (2 - row);
            int y = arrowTop + row;
            graphics.fill(cx - halfWidth, y, cx + halfWidth + 1, y + 1, arrowColor);
        }
    }

    private int scrollbarX0() {
        return this.getListX() + this.getListWidth() + 7;
    }

    private int scrollbarX1() {
        return this.scrollbarX0() + SCROLLBAR_WIDTH;
    }

    private int scrollbarTrackY0() {
        return this.getListY() + SCROLLBAR_ARROW_HEIGHT;
    }

    private int scrollbarTrackHeight() {
        return Math.max(0, this.getListHeight() - (2 * SCROLLBAR_ARROW_HEIGHT));
    }

    /**
     * Computes where the scroll thumb sits and how tall it is, purely from current
     * scroll state (scrollOffset / maxScroll / itemsPerPage). Both rendering and
     * mouse handling call this so they can never disagree with each other. The thumb
     * travels between the two arrow buttons, not across the full list height.
     */
    private ScrollThumbGeometry computeScrollThumb() {
        List<ItemEntry> itemEntries = this.getItemList();
        int totalItems = itemEntries == null ? 0 : itemEntries.size();
        if (itemsPerPage <= 0 || totalItems <= itemsPerPage) {
            return null;
        }

        int trackHeight = this.scrollbarTrackHeight();
        float visibleFraction = (float) itemsPerPage / totalItems;

        int minThumbHeight = Math.min(SCROLLBAR_MIN_THUMB_HEIGHT, trackHeight);
        int thumbHeight = Mth.clamp(Math.round(trackHeight * visibleFraction), minThumbHeight, trackHeight);

        int maxThumbTravel = trackHeight - thumbHeight;
        float scrollProgress = maxScroll <= 0 ? 0f : (float) scrollOffset / maxScroll;
        int thumbY = this.scrollbarTrackY0() + Math.round(maxThumbTravel * scrollProgress);

        return new ScrollThumbGeometry(thumbY, thumbHeight);
    }

    /**
     * Handles a left-click on the scrollbar track/thumb. Clicking the thumb grabs it
     * at the point clicked; clicking elsewhere on the track jumps the thumb there and
     * starts dragging immediately. Returns true if the click was consumed.
     */
    private boolean handleScrollbarClick(MouseButtonEvent event) {
        if (event.input() != MouseClick.LEFT_CLICK.value() || isDragging || springingBack) {
            return false;
        }

        List<ItemEntry> itemEntries = this.getItemList();
        if (itemEntries == null || itemsPerPage <= 0 || itemEntries.size() <= itemsPerPage) {
            return false;
        }

        double mx = event.x();
        double my = event.y();
        int trackX0 = this.scrollbarX0();
        int trackX1 = this.scrollbarX1();
        int trackY0 = this.getListY();
        int trackY1 = trackY0 + this.getListHeight();

        if (mx < trackX0 || mx > trackX1 || my < trackY0 || my > trackY1) {
            return false;
        }

        if (my <= trackY0 + SCROLLBAR_ARROW_HEIGHT) {
            if (canScrollUp) {
                this.playClickSound();
            }
            scrollOffset = Mth.clamp(scrollOffset - 1, 0, maxScroll);
            return true;
        }
        if (my >= trackY1 - SCROLLBAR_ARROW_HEIGHT) {
            if (canScrollDown) {
                this.playClickSound();
            }
            scrollOffset = Mth.clamp(scrollOffset + 1, 0, maxScroll);
            return true;
        }

        ScrollThumbGeometry thumb = this.computeScrollThumb();
        if (thumb == null) {
            return false;
        }

        if (my >= thumb.y() && my <= thumb.y() + thumb.height()) {
            this.scrollbarDragGrabOffset = (int) my - thumb.y();
        } else {
            this.scrollbarDragGrabOffset = thumb.height() / 2;
            this.updateScrollFromThumbY((int) my - this.scrollbarDragGrabOffset);
        }

        this.isDraggingScrollbar = true;
        return true;
    }

    /**
     * Given the desired top-Y for the thumb (in screen space), works out how far
     * through the list that corresponds to and updates scrollOffset accordingly.
     */
    private void updateScrollFromThumbY(int desiredThumbY) {
        if (maxScroll <= 0) {
            return;
        }

        ScrollThumbGeometry thumb = this.computeScrollThumb();
        if (thumb == null) {
            return;
        }

        int maxThumbTravel = this.scrollbarTrackHeight() - thumb.height();
        if (maxThumbTravel <= 0) {
            scrollOffset = 0;
            return;
        }

        int relativeY = Mth.clamp(desiredThumbY - this.scrollbarTrackY0(), 0, maxThumbTravel);
        float progress = (float) relativeY / maxThumbTravel;
        scrollOffset = Mth.clamp(Math.round(progress * maxScroll), 0, maxScroll);
    }



    private void updateDragTilt(int mouseX, long now, float deltaSeconds) {
        if (lastDragNanoTime == 0L) {
            lastDragMouseX = mouseX;
            lastDragNanoTime = now;
            return;
        }

        float dt = (now - lastDragNanoTime) / 1000000000f;
        lastDragNanoTime = now;

        if (dt > 0f) {
            float velX = (mouseX - lastDragMouseX) / dt;
            float targetTilt = Mth.clamp(-velX * TILT_VELOCITY_SCALE, -TILT_MAX_DEGREES, TILT_MAX_DEGREES);
            dragTiltAngle = this.approach(dragTiltAngle, targetTilt, TILT_RESPONSIVENESS, deltaSeconds);
        }

        lastDragMouseX = mouseX;
    }

    private float approach(float current, float target, float speed, float deltaSeconds) {
        if (deltaSeconds <= 0f) return current;
        float t = 1f - (float) Math.exp(-speed * deltaSeconds);
        return current + (target - current) * t;
    }


    private void stepSpring(float deltaSeconds) {
        if (deltaSeconds <= 0f) {
            return;
        }

        float dx = dragOriginX - dragCurrentX;
        float dy = dragOriginY - dragCurrentY;

        float accelX = dx * SPRING_STIFFNESS - dragVelX * SPRING_DAMPING;
        float accelY = dy * SPRING_STIFFNESS - dragVelY * SPRING_DAMPING;

        dragVelX += accelX * deltaSeconds;
        dragVelY += accelY * deltaSeconds;

        dragCurrentX += dragVelX * deltaSeconds;
        dragCurrentY += dragVelY * deltaSeconds;

        boolean closeEnough = Mth.abs(dx) < SPRING_SNAP_THRESHOLD && Math.abs(dy) < SPRING_SNAP_THRESHOLD;
        boolean slowEnough = Mth.abs(dragVelX) < SPRING_VEL_THRESHOLD && Math.abs(dragVelY) < SPRING_VEL_THRESHOLD;

        if (closeEnough && slowEnough) {
            dragCurrentX = dragOriginX;
            dragCurrentY = dragOriginY;
            dragVelX = 0f;
            dragVelY = 0f;
            draggedItemId = null;
            springingBack = false;
            dragTiltAngle = 0f;
        }
    }

    private void renderItemIcon(GuiGraphicsExtractor graphics, @NotNull ItemEntry entry, float itemX, float itemY, float rotation) {
        ItemStack stack = entry.getItemStack();
        float jiggleTime = shakeTimers.getOrDefault(entry.itemId(), 0f);

        if (jiggleTime > 0f) {
            float progress = jiggleTime / 0.4f;
            float wave = (float) Math.sin(jiggleTime * 40f);
            rotation += wave * progress * 15f;
        }

        float itemCenterX = itemX + 8f;
        float itemCenterY = itemY + 8f;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(itemCenterX, itemCenterY);
        pose.rotate((float) Math.toRadians(rotation));
        pose.translate(-8f, -8f);

        if (stack != null && !stack.isEmpty()) {
            graphics.item(stack, 0, 0);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISSING_TEXTURE, 0, 0, 0, 0, 16, 16, 16, 16);
        }
        pose.popMatrix();
    }

    private void renderItemRow(GuiGraphicsExtractor graphics, @NotNull ItemEntry entry, int yOffset, int mouseX, int mouseY) {
        ItemStack stack = entry.getItemStack();
        String displayText;

        if (stack != null && !stack.isEmpty()) {
            displayText = ItemUtil.getItemDetails(entry.getItemStack(), BlockedItemsConfig.getItemIdMode());
        } else {
            if (ItemBlockerClient.isUsingIDMode()) {
                displayText = entry.itemId();
            } else {
                displayText = ItemUtil.getItemNameFromID(entry.itemId(), true, RegistryUtils.isRegistryLoaded);
            }
        }

        int length = displayText.length();
        if (length > 25) {
            displayText = displayText.substring(0, 25) + "...";
        }
        graphics.text(this.font, displayText, this.getListX() + 30, yOffset + 4, Colors.WHITE, false);

        int removeX = this.getListX() + 175;
        boolean hovered = mouseX >= removeX && mouseX <= removeX + 20 &&
                mouseY >= yOffset && mouseY <= yOffset + 16;

        graphics.text(this.font, "X", removeX, yOffset + 4, hovered ? 0xFFFF5555 : 0xFFAAAAAA, false);
    }


    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        boolean grabbedScrollbar = this.handleScrollbarClick(event);
        if (grabbedScrollbar) {
            super.mouseClicked(event, doubleClick);
            return true;
        }

        int yOffset = this.getListY() + 5;
        List<ItemEntry> itemEntries = this.getItemList();
        if (itemEntries != null) {
            for (int i = scrollOffset; i < itemEntries.size() && i < scrollOffset + itemsPerPage; i++) {
                ItemEntry entry = itemEntries.get(i);

                int itemX = this.getListX() + 10;
                int itemY = yOffset;

                if (event.x() >= itemX && event.x() <= itemX + 14 &&
                        event.y() >= itemY && event.y() <= itemY + 14) {
                    if (event.input() == MouseClick.LEFT_CLICK.value()) {
                        this.startShake(entry.itemId(), 0.25f);
                    }
                    else {
                        this.startShake(entry.itemId(), 0.4f);
                    }
                    if ((event.input() ==  MouseClick.MIDDLE_CLICK.value()) || (event.input() == MouseClick.RIGHT_CLICK.value())) {
                        return false;
                    }
                    draggedItemId = entry.itemId();
                    dragOriginX = itemX;
                    dragOriginY = itemY;
                    dragGrabOffsetX = (int) (event.x() - itemX);
                    dragGrabOffsetY = (int) (event.y() - itemY);
                    dragCurrentX = itemX;
                    dragCurrentY = itemY;
                    springingBack = false;

                    dragVelX = 0f;
                    dragVelY = 0f;
                    dragTiltAngle = 0f;
                    lastDragMouseX = (float) event.x();
                    lastDragNanoTime = 0L;
                }

                yOffset += 22;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isDragging || springingBack) {
            return false;
        }
        int listY = this.getListY();
        int listHeight = this.getListHeight();

        if (mouseY >= listY && mouseY <= listY + listHeight) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)  verticalAmount));
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (isDragging || springingBack) {
            return false;
        }
        EditBox itemInputField = this.getEditBox();
        if (input.isConfirmation()) {
            this.checkItemId();
            itemInputField.setValue("");
        }
        if (getItemList().size() <= itemsPerPage) {
            return super.keyPressed(input);
        }
        if (!getItemList().isEmpty()) {
            if (input.isDown() && !input.hasShiftDown()) {
                if (scrollOffset == maxScroll) {
                    return false;
                }
                scrollOffset++;
                return false;
            }
            if (input.isUp() && !input.hasShiftDown()) {
                if (scrollOffset == 0) {
                    return false;
                }
                scrollOffset--;
                return false;
            }
        }
        return super.keyPressed(input);
    }

    protected native void checkItemId();

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (isDraggingScrollbar) {
            this.updateScrollFromThumbY((int) event.y() - scrollbarDragGrabOffset);
            return true;
        }
        if ((event.input() ==  MouseClick.MIDDLE_CLICK.value()) || (event.input() == MouseClick.RIGHT_CLICK.value())) {
            return false;
        }
        if (draggedItemId != null && !springingBack) {
            isDragging = true;
            dragCurrentX = (float) (mouseX - dragGrabOffsetX);
            dragCurrentY = (float) (mouseY - dragGrabOffsetY);
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        isDragging = false;
        if (draggedItemId != null) {
            springingBack = true;
            lastDragNanoTime = 0L;
        }
        return super.mouseReleased(event);
    }

    /**
     * Used to make an item shake with a certain intensity
     * @param itemId String value: An item's ID used to determine what item to shake.
     * @param intensity Float value: The amount the item should shake between 0 - 1. Intensity is also based on tick speed, higher tick speed = higher intensity.
     */
    public void startShake(String itemId, float intensity) {
        intensity = Mth.clamp(intensity, 0, 1);
        shakeTimers.put(itemId, intensity);
    }

    /**
     * Used to make all items shake.
     * @param intensity Float value: The amount the item should shake between 0 - 1. Intensity is also based on tick speed, higher tick speed = higher intensity.
     */

    private void shakeAllItems(float intensity) {
        intensity = Mth.clamp(intensity, 0, 1);
        for (ItemEntry entry : this.getItemList()) {
            String itemId = entry.itemId();
            shakeTimers.put(itemId, intensity);
        }
    }

    /**
     * Clears all screen effects like dragged or shaking items from the screen.
     */

    public static void clearScreenEffects() {
        draggedItemId = null;
        springingBack = false;
        isDragging = false;
        shakeTimers.clear();
    }

    protected int getScrollOffset() {
        return scrollOffset;
    }

    protected void resetScrollOffset() {
        this.scrollOffset = 0;
    }

    protected void incrementScrollOffset() {
        ++this.scrollOffset;
    }

    protected void decrementScrollOffset() {
        --this.scrollOffset;
    }

    protected void decreaseScrollOffset(int amount) {
        this.scrollOffset -= amount;
    }

    protected void increaseScrollOffset(int amount) {
        this.scrollOffset += amount;
    }


    @Override
    public void tick() {
        super.tick();
        shakeTimers.replaceAll((id, time) -> Math.max(0f, time - 0.05f));
        shakeTimers.values().removeIf(t -> t <= 0f);
    }

    private record ScrollThumbGeometry(int y, int height) {}
}