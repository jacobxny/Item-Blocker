package com.itemblocker.api.screen;

import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.enums.ItemIDMode;
import com.itemblocker.api.enums.MouseClick;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Predicate;

public abstract class AbstractItemWidget extends GlobalScreen {

    private static final Identifier MISSING_TEXTURE = Identifier.withDefaultNamespace("textures/item/barrier.png");
    private final Identifier SEARCH_TEXTURE = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/search.png");
    private final Identifier TICK_TEXTURE = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/tick.png");
    private final Identifier GREEN_TICK_TEXTURE = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/green_tick.png");
    private static final float BUTTON_HOVER_SCALE = 1.18f;
    private static final float BUTTON_HOVER_ANIM_SPEED = 14f;

    protected int scrollOffset;
    public static boolean isDragging = false;
    public final WidgetGraphics widgetGraphics;
    public Button addButton;
    public Button doneButton;
    public int itemsPerPage;
    public int maxScroll;

    private boolean isDraggingScrollbar = false;
    public boolean searching = false;
    private int scrollbarDragGrabOffset = 0;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 12;
    private static final int SCROLLBAR_ARROW_HEIGHT = 7;


    private static final Map<String, Float> shakeTimers = new HashMap<>();
    public static final ArrayList<ItemEntry> allItems = new ArrayList<>();
    private final List<ItemEntry> filteredItems = new ArrayList<>();
    public static  String editBoxQuery = "";
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
    private long lastButtonFrameTimeNanos = -1;
    private float searchHoverProgress = 0f;
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

    protected abstract boolean isItemSelected(String itemId);

    protected AbstractItemWidget(Component title, Screen parent) {
        super(title, parent);
        this.widgetGraphics = new WidgetGraphics(this.font);
    }

    @Override
    protected void init() {
        addButton = Button.builder(Component.translatable("itemblocker.text.add_item"), button -> checkItemId())
                .bounds(this.width / 2 - 100, 65, 200, 20)
                .build();

        this.addRenderableWidget(addButton);

        doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build();

        this.addRenderableWidget(doneButton);


        Set<Identifier> registryList = BuiltInRegistries.ITEM.keySet();
        allItems.clear();
        for (final Identifier id : registryList) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            String itemId = ItemUtil.getItemDetails(item.getDefaultInstance(), ItemIDMode.USE_ITEM_ID);
            allItems.add(new ItemEntry(itemId));
        }
        if (BlockedItemsConfig.listItemsAlphabetically()) {
            this.sortList(allItems);
        }
        this.refreshResults();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.itemsPerPage = (this.getListHeight() - 10) / 22;
        this.maxScroll = Math.max(0, this.getActiveList().size() - itemsPerPage);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);
        this.renderSearchButton(graphics, mouseX, mouseY);
        this.renderWidget(graphics, mouseX, mouseY);
    }


    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int listY = this.getListY();
        int listHeight = this.getListHeight();
        List<ItemEntry> itemEntries = this.getActiveList();

        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanoTime == 0L ? 0f : (now - lastRenderNanoTime) / 1_000_000_000f;
        deltaSeconds = Math.min(deltaSeconds, 0.05f);
        lastRenderNanoTime = now;

        int x0 = this.getListX();
        int x1 = this.getListWidth();

        Component header = searching
                ? Component.literal("Items: " + itemEntries.size())
                : Component.translatable(this.getHeaderText(), itemEntries.size());
        graphics.text(this.font, header, x0 + 10, listY - 8, Colors.WHITE, false);

        graphics.fill(x0, listY, x0 + x1, listY + listHeight, 0x80000000);

        if (itemEntries.isEmpty()) {
            Component empty = searching
                    ? Component.literal("No items match your search")
                    : Component.translatable(this.getEmptyListText());
            graphics.centeredText(this.font, empty, width / 2, listY + listHeight / 2, 0xFF888888);
            return;
        }

        int yOffset = listY + 5;
        ItemEntry draggedEntry = null;

        for (int i = scrollOffset; i < itemEntries.size() && i < scrollOffset + itemsPerPage; i++) {
            ItemEntry entry = itemEntries.get(i);
            int itemX = x0 + 10;

            if (entry.itemId().equals(draggedItemId)) {
                draggedEntry = entry;
                yOffset += 22;
                continue;
            }

            if (mouseX >= itemX && mouseX <= itemX + 14 && mouseY >= yOffset && mouseY <= yOffset + 14) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            this.renderItemIcon(graphics, entry, itemX, yOffset, 0f);
            this.renderItemRow(graphics, entry, yOffset, mouseX, mouseY, searching);
            yOffset += 22;
        }

        if (draggedEntry != null) {
            if (!springingBack) {
                dragCurrentX = mouseX - dragGrabOffsetX;
                dragCurrentY = mouseY - dragGrabOffsetY;
                this.updateDragTilt(mouseX, now, deltaSeconds);
                graphics.requestCursor(DRAG_CURSOR);
            } else {
                this.stepSpring(deltaSeconds);
                dragTiltAngle = this.approach(dragTiltAngle, 0f, TILT_RESPONSIVENESS * 1.5f, deltaSeconds);
            }
            this.renderItemIcon(graphics, draggedEntry, dragCurrentX, dragCurrentY, dragTiltAngle);
            int normalRowY = listY + 5 + (itemEntries.indexOf(draggedEntry) - scrollOffset) * 22;
            this.renderItemRow(graphics, draggedEntry, normalRowY, mouseX, mouseY, searching);
        }

        if (itemsPerPage < itemEntries.size()) {
            this.renderScrollBar(graphics, mouseX, mouseY);
        }
    }

    public void renderSearchButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float deltaSeconds = lastButtonFrameTimeNanos < 0 ? 0f : (now - lastButtonFrameTimeNanos) / 1_000_000_000f;
        int searchX = this.getEditBox().getRight() + 7;
        int searchY = this.getEditBox().getY() + 2;
        lastButtonFrameTimeNanos = now;


        boolean searchHovered = mouseX >= searchX && mouseX <= searchX + 16 && mouseY >= searchY && mouseY <= searchY + 16;

        if (searchHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        searchHoverProgress = this.approach(searchHoverProgress, searchHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);

        widgetGraphics.animatedButton(graphics, SEARCH_TEXTURE, searchX, searchY, searchHoverProgress, searchHovered, false, "Search");
    }


    protected void renderScrollBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int trackX0 = this.scrollbarX0();
        int trackX1 = this.scrollbarX1() + 1;
        int fullY0 = this.getListY();
        int fullY1 = fullY0 + this.getListHeight();

        graphics.fill(trackX0, fullY0, trackX1, fullY1, 0xFF0C0C0C);


        canScrollUp = scrollOffset > 0;
        canScrollDown = scrollOffset < maxScroll;
        boolean upHovered = mouseX >= trackX0 && mouseX <= trackX1
                && mouseY >= fullY0 && mouseY <= fullY0 + SCROLLBAR_ARROW_HEIGHT;
        boolean downHovered = mouseX >= trackX0 && mouseX <= trackX1
                && mouseY >= fullY1 - SCROLLBAR_ARROW_HEIGHT && mouseY <= fullY1;

        this.renderScrollArrow(graphics, trackX0, fullY0, true, canScrollUp, upHovered);
        this.renderScrollArrow(graphics, trackX0, fullY1 - SCROLLBAR_ARROW_HEIGHT, false, canScrollDown, downHovered);

        if (upHovered) {
            if (!canScrollUp) {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
            else {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
        if (downHovered) {
            if (!canScrollDown) {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
            else {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        ScrollThumbGeometry thumb = this.computeScrollThumb();
        if (thumb == null) {
            return;
        }


        int thumbY0 = thumb.y();
        int thumbY1 = thumbY0 + thumb.height();
        int thumbX0 = this.scrollbarX0() + 1;
        int thumbX1 = this.scrollbarX1();

        boolean thumbHovered = mouseX >= thumbX0 && mouseX <= thumbX1 && mouseY >= thumbY0 && mouseY <= thumbY1 && !isDragging && !springingBack;

        int base = isDraggingScrollbar ? 0xFFA6A6A5 : (thumbHovered ? 0xFF8A8A89 : 0xFF696868);
        int highlight = 0xA9C9C9B;
        int shadow = 0xAA3E3E3D;

        graphics.fill(thumbX0 + 1, thumbY0, thumbX1 - 1, thumbY1, base);
        graphics.fill(thumbX0 + 1, thumbY0, thumbX1 - 1, thumbY0 + 1, highlight);
        graphics.fill(thumbX0 + 1, thumbY1 - 1, thumbX1 - 1, thumbY1, shadow);

        if (thumbHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        if (isDraggingScrollbar) {
            graphics.requestCursor(DRAG_CURSOR);
        }
        if (thumbHovered && (isDragging || springingBack)) {
            graphics.requestCursor(CursorTypes.NOT_ALLOWED);
        }
    }

    /**
     * Draws one arrow button (up or down) at the given top-left corner. Dimmed when
     * there's nothing left to scroll that direction, brightened on hover.
     */
    private void renderScrollArrow(GuiGraphicsExtractor graphics,  int x0, int y0, boolean pointingUp, boolean enabled, boolean hovered) {


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


    private ScrollThumbGeometry computeScrollThumb() {
        List<ItemEntry> itemEntries = this.getActiveList();
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


    private boolean handleScrollbarClick(MouseButtonEvent event) {
        if (event.input() != MouseClick.LEFT_CLICK.value() || isDragging || springingBack) {
            return false;
        }


        List<ItemEntry> itemEntries = this.getActiveList();
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

    private void renderItemRow(GuiGraphicsExtractor graphics, @NotNull ItemEntry entry, int yOffset, int mouseX, int mouseY, boolean searching) {
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



        if (ItemUtil.isMusicDisc(stack)) {
            displayText = ItemUtil.getMusicDiscName(stack);
        }


        int length = displayText.length();
        if (length > 25) {
            displayText = displayText.substring(0, 25) + "...";
        }
        graphics.text(this.font, displayText, this.getListX() + 30, yOffset + 4, Colors.WHITE, false);

        int removeX = this.getListX() + 175;
        boolean hovered = mouseX >= removeX && mouseX <= removeX + 20 &&
                mouseY >= yOffset && mouseY <= yOffset + 16;

        if (searching) {
            if (this.isItemSelected(entry.itemId())) {
                graphics.text(this.font, "X", removeX, yOffset + 4, hovered ? 0xFFFF5555 : 0xFFAAAAAA, false);
            }
            else if (hovered) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, GREEN_TICK_TEXTURE, removeX - 6, yOffset, 0, 0, 16, 16, 16, 16);
            }
            else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TICK_TEXTURE, removeX - 6, yOffset, 0, 0, 16, 16, 16, 16);
            }
        }
        else {
            graphics.text(this.font, "X", removeX, yOffset + 4, hovered ? 0xFFFF5555 : 0xFFAAAAAA, false);
        }
    }


    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        boolean grabbedScrollbar = this.handleScrollbarClick(event);
        int searchX = this.getEditBox().getRight() + 7;
        int searchY = this.getEditBox().getY() + 2;
        if (grabbedScrollbar) {
            return super.mouseClicked(event, doubleClick);
        }

        if (!isDragging) {
            if (event.x() >= searchX && event.x() <= searchX + 16 &&
                    event.y() >= searchY && event.y() <= searchY + 16) {
                searching = !searching;
                this.refreshResults();
                this.playClickSound();
                if (ItemBlockerClient.isDev) {
                    Loggers.CLIENT_LOGGER.info("Searching = {}", searching);
                }
            }
        }

        int yOffset = this.getListY() + 5;
        List<ItemEntry> itemEntries = this.getActiveList();

        if (itemEntries != null) {
            for (int i = scrollOffset; i < itemEntries.size() && i < scrollOffset + itemsPerPage; i++) {
                ItemEntry entry = itemEntries.get(i);

                int itemX = this.getListX() + 10;
                int itemY = yOffset;

                if (event.x() >= itemX && event.x() <= itemX + 14 &&
                        event.y() >= itemY && event.y() <= itemY + 14) {
                    if (event.input() == MouseClick.LEFT_CLICK.value()) {
                        this.startShake(entry, 0.25f);
                    }
                    else {
                        this.startShake(entry, 0.4f);
                    }
                    if (ItemBlockerClient.isDev) {
                        Loggers.CLIENT_LOGGER.info("input = {}", event.input());
                    }
                    if (event.input() == MouseClick.LEFT_CLICK.value()) {
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
                }

                yOffset += 22;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    protected List<ItemEntry> getActiveList() {
        if (searching) {
            return filteredItems;
        }
        List<ItemEntry> list = this.getItemList();
        return list == null ? List.of() : list;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isDragging || springingBack) {
            return false;
        }
        int listY = this.getListY();
        int listHeight = this.getListHeight();

        if (mouseY >= listY && mouseY <= listY + listHeight) {
            scrollOffset = Math.clamp(scrollOffset - (int) verticalAmount, 0, maxScroll);
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
        List<ItemEntry> active = this.getActiveList();
        if (active.size() <= itemsPerPage) {
            return super.keyPressed(input);
        }
        if (!active.isEmpty()) {
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
    public void onClose() {
        allItems.clear();
        filteredItems.clear();
        editBoxQuery = "";
        searching = false;
        clearScreenEffects();
        super.onClose();
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (isDraggingScrollbar) {
            this.updateScrollFromThumbY((int) event.y() - scrollbarDragGrabOffset);
            return true;
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
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
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
     * @param itemEntry An item entry record: Contains the item's id.
     * @param intensity Float value: The amount the item should shake between 0 - 1. Intensity is also based on tick speed, higher tick speed = higher intensity.
     */
    public void startShake(ItemEntry itemEntry, float intensity) {
        String itemId = itemEntry.itemId();
        intensity = Mth.clamp(intensity, 0, 1);
        shakeTimers.put(itemId, intensity);
    }

    /**
     * Used to make all items shake.
     * @param intensity Float value: The amount the item should shake between 0 - 1. Intensity is also based on tick speed, higher tick speed = higher intensity.
     */

    private void shakeAllItems(float intensity) {
        intensity = Mth.clamp(intensity, 0, 1);
        for (ItemEntry entry : this.getActiveList()) {
            this.startShake(entry, intensity);
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

    public void sortList(List<ItemEntry> list) {
        list.sort(Comparator.comparing(
                entry -> {
                    ItemStack stack = entry.getItemStack();
                    if (ItemUtil.isMusicDisc(stack)) {
                        return ItemUtil.getMusicDiscName(stack);
                    }
                    if (stack != null && !stack.isEmpty()) {
                        return stack.getHoverName().getString();
                    }
                    return entry.itemId();
                },
                String.CASE_INSENSITIVE_ORDER
        ));
    }

    public void refreshResults() {
        if (!searching) {
            return;
        }
        filteredItems.clear();

        String query = editBoxQuery.trim().toLowerCase(Locale.ROOT);
        if (query.startsWith("#")) {
            Set<String> tagged = this.collectItemsInMatchingTags(query.substring(1).trim().replace(' ', '_'));
            for (ItemEntry entry : allItems) {
                if (tagged.contains(entry.itemId())) {
                    filteredItems.add(entry);
                }
            }
        }
        else if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String idQuery = query.replace(' ', '_');
            for (ItemEntry entry : allItems) {
                if (matchesQuery(entry, query, idQuery)) {
                    filteredItems.add(entry);
                }
            }
        }
        this.scrollOffset = 0;
    }

    private Set<String> collectItemsInMatchingTags(String searchTerm) {
        int colonIndex = searchTerm.indexOf(':');
        Predicate<Identifier> matcher;
        if (colonIndex == -1) {
            matcher = id -> id.getPath().contains(searchTerm);
        } else {
            String nsMatcher = searchTerm.substring(0, colonIndex).trim();
            String pathMatcher = searchTerm.substring(colonIndex + 1).trim();
            matcher = id -> id.getNamespace().contains(nsMatcher) && id.getPath().contains(pathMatcher);
        }

        Set<String> result = new HashSet<>();
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> matcher.test(named.key().location()))
                .forEach(named -> named.forEach(holder ->
                        result.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString())));
        return result;
    }

    private boolean matchesQuery(ItemEntry entry, String query, String idQuery) {
        if (entry.itemId().toLowerCase(Locale.ROOT).contains(idQuery)) {
            return true;
        }
        ItemStack stack = entry.getItemStack();
        if (ItemUtil.isMusicDisc(stack)) {
            return stack != null && !stack.isEmpty()
                    &&  ItemUtil.getMusicDiscName(stack).toLowerCase(Locale.ROOT).contains(query);
        }
        return stack != null && !stack.isEmpty()
                && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private record ScrollThumbGeometry(int y, int height) {

    }

    public static class WidgetGraphics {

        private final Font font;
        public WidgetGraphics(Font font) {
            this.font = font;
        }

        public void line(
                GuiGraphicsExtractor graphics,
                 float x1,
                 float y1,
                 float x2,
                 float y2,
                 float thickness,
                 int color
        ) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float angle = (float) Math.atan2(dy, dx);

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(x1, y1);
            pose.rotate(angle);
            pose.translate(0, -thickness / 2f);

            graphics.fill(0, 0, Math.round(length), Math.max(1, Math.round(thickness)), color);

            pose.popMatrix();
        }

        public void arrow(
                GuiGraphicsExtractor graphics,
                int x1,
                int y1,
                int x2,
                int y2,
                float thickness,
                float headLength,
                int color
        ) {
            line(graphics, x1, y1, x2, y2, thickness, color);

            float angle = (float) Math.atan2(y2 - y1, x2 - x1);
            float spread = (float) Math.toRadians(25);

            float leftX = x2 - headLength * (float) Math.cos(angle - spread);
            float leftY = y2 - headLength * (float) Math.sin(angle - spread);
            float rightX = x2 - headLength * (float) Math.cos(angle + spread);
            float rightY = y2 - headLength * (float) Math.sin(angle + spread);

            line(graphics, x2, y2, leftX, leftY, thickness, color);
            line(graphics, x2, y2, rightX, rightY, thickness, color);
        }

        public void animatedButton(
                GuiGraphicsExtractor graphics,
                Identifier texture,
                int x,
                int y,
                float hoverProgress,
                boolean hovered,
                boolean renderBackground,
                String tooltipText
        ) {
            if (renderBackground) {
                if (hoverProgress > 0.01f) {
                    int alpha = Math.round(0x66 * hoverProgress) << 24 | 0xFFFFFF;
                    graphics.fill(x, y, x + 16, y + 16, alpha);
                }
            }

            if (hovered) {
                List<ClientTooltipComponent> component = new ArrayList<>();
                Component text = Component.literal(tooltipText).withStyle(ChatFormatting.WHITE);
                component.add(ClientTooltipComponent.create(FormattedCharSequence.forward(text.getString(), text.getStyle())));
                graphics.tooltip(this.font, component, x, y, DefaultTooltipPositioner.INSTANCE, ItemStack.EMPTY.getComponents().get(DataComponents.TOOLTIP_STYLE), false);
            }

            float scale = 1f + (BUTTON_HOVER_SCALE - 1f) * hoverProgress;
            float centerX = x + 8f;
            float centerY = y + 8f;

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(centerX, centerY);
            pose.scale(scale, scale);
            pose.translate(-centerX, -centerY);

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16);

            pose.popMatrix();
        }
    }

}