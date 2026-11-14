package com.itemblocker.client.screen;

import com.itemblocker.api.screen.AbstractItemWidget;
import com.itemblocker.api.screen.util.Fade;
import com.itemblocker.api.RegistryUtils;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.screen.util.Rect;
import com.itemblocker.api.screen.util.TutorialStep;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.client.screen.presets.PresetsScreen;
import com.itemblocker.api.records.ItemEntry;
import com.itemblocker.api.logging.Loggers;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;


public class BlockedItemsScreen extends AbstractItemWidget {
    private long invalidIDStartTime = -1;
    private Button tutorialButton;
    private boolean inTutorial = false;
    private long successTime = -1;
    private long alreadyTime = -1;
    private String itemId;
    private int clearX;
    private int optionsX;
    private int presetsX;
    private final int rowY = 90;

    private static final float BUTTON_HOVER_ANIM_SPEED = 14f;
    private static final float FADE_IN = 0.25f;
    private static final float FADE_OUT = 0.25f;

    private float clearHoverProgress = 0f;
    private float optionsHoverProgress = 0f;
    private float presetsHoverProgress = 0f;
    private long lastButtonFrameTimeNanos = -1;


    private static final int TUTORIAL_BOX_PADDING = 3;
    private static final int TUTORIAL_SCREEN_MARGIN = 4;

    private List<TutorialStep> tutorialSteps = List.of();
    private float[] tutorialStepStarts = new float[0];  
    private float tutorialTotalSeconds = 0f;
    private long tutorialStartTime = -1;


    public BlockedItemsScreen(Screen parent) {
        super(Component.translatable("itemblocker.text.config"), parent);
    }


    @Override
    public int getListY() {
        return 120;
    }

    @Override
    protected int getListX() {
        return width / 2 - 105;
    }

    @Override
    protected int getListWidth() {
        return 210;
    }

    @Override
    public int getListHeight() {
        return height - 160;
    }

    @Override
    public String getEmptyListText() {
        return "itemblocker.text.no_items";
    }

    @Override
    public boolean isItemSelected(String itemId) {
        return BlockedItemsConfig.isBlocked(itemId);
    }

    @Override
    protected void init() {
        super.init();

        clearX = this.width / 2 - 30;
        optionsX = this.width / 2 - 10;
        presetsX = this.width / 2 + 10;

        if (!RegistryUtils.isRegistryLoaded) {
            RegistryUtils.loadRegistry();
        }

        BlockedItemsConfig.load();


        this.tutorialButton = Button.builder(Component.literal("?"), button -> {
            this.startTutorial();
        }).bounds(this.doneButton.getX() - 50, this.height - 30, 30, 20).build();
        this.addRenderableWidget(tutorialButton);

        this.buildTutorial();

        this.refreshList();
        if (!BlockedItemsConfig.hasDoneTutorial()) {
            this.startTutorial();
        }

    }

    private void refreshList() {
        this.storedItems = new ArrayList<>();
        for (String itemId : BlockedItemsConfig.getBlockedItems()) {
            this.storedItems.add(new ItemEntry(itemId));
        }
        sortList(storedItems);
        Loggers.CLIENT_LOGGER.info("Refreshed list, {} blocked items", this.storedItems.size());
    }


    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (invalidIDStartTime >= 0) {
            int color = fadeManager.fadeOut(Fade.LONG_RED_FADE, invalidIDStartTime);
            graphics.text(this.font, Component.translatable("itemblocker.text.invald_id"), this.width / 2 - 103, 27, color, false);
        }
        else if (successTime >= 0) {
            int color = fadeManager.fadeOut(Fade.GREEN_FADE, successTime);
            String text;
            if (ItemBlockerClient.isUsingIDMode()) {
                text = itemId;
            }
            else {
                text = ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            Component component = Component.translatable("itemblocker.text.success", text);

            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }
        else if (alreadyTime >= 0) {
            int color = fadeManager.fadeOut(Fade.YELLOW_FADE, alreadyTime);
            String text;
            if (ItemBlockerClient.isUsingIDMode()) {
                text = itemId;
            }
            else {
                text = ItemUtil.getItemNameFromID(itemId, true, RegistryUtils.isRegistryLoaded);
            }
            Component component = Component.translatable("itemblocker.text.already_blocked", text);
            FormattedCharSequence toRender = component.getVisualOrderText();
            graphics.text(this.font, toRender, (this.width / 2) - this.font.width(toRender) / 2, 27, color, false);
        }

        this.renderButtons(graphics, mouseX, mouseY);
        this.renderTutorial(graphics, mouseX, mouseY, delta);
        if (inTutorial && (Util.getMillis() - tutorialStartTime) / 1000f >= tutorialTotalSeconds) {
            this.finishTutorial();
        }
    }

    private void renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float deltaSeconds = lastButtonFrameTimeNanos < 0 ? 0f : (now - lastButtonFrameTimeNanos) / 1_000_000_000f;
        lastButtonFrameTimeNanos = now;

        Identifier clearButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/bin.png");
        Identifier settingsButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/settings.png");
        Identifier presetsButton = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/presets.png");

        boolean clearHovered = mouseX >= clearX && mouseX <= clearX + 16 && mouseY >= rowY && mouseY <= rowY + 16;
        boolean optionsHovered = mouseX >= optionsX && mouseX <= optionsX + 16 && mouseY >= rowY && mouseY <= rowY + 16;
        boolean presetsHovered = mouseX >= presetsX && mouseX <= presetsX + 16 && mouseY >= rowY && mouseY <= rowY + 16;

        if (clearHovered || optionsHovered || presetsHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        clearHoverProgress = this.approach(clearHoverProgress, clearHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);
        optionsHoverProgress = this.approach(optionsHoverProgress, optionsHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);
        presetsHoverProgress = this.approach(presetsHoverProgress, presetsHovered ? 1f : 0f, BUTTON_HOVER_ANIM_SPEED, deltaSeconds);

        widgetGraphics.animatedButton(graphics, clearButton, clearX, rowY, clearHoverProgress, clearHovered, false, Component.translatable("itemblocker.text.clear_all"));
        widgetGraphics.animatedButton(graphics, settingsButton, optionsX, rowY, optionsHoverProgress, optionsHovered, false, Component.translatable("itemblocker.text.options"));
        widgetGraphics.animatedButton(graphics, presetsButton, presetsX, rowY, presetsHoverProgress, presetsHovered, false, Component.translatable("itemblocker.text.presets"));
    }


    /**
     * Used to create all the steps used in the player tutorial when first entering the screen or pressing the tutorial button.
     * <br>
     * To add a step:
     * <br></br>
     * 1. First, add a {@link TutorialStep.Builder} to the list using {@link TutorialStep#step(String)}
     * <br></br>
     * 2. To add a box and arrow to the step, you can use {@link TutorialStep.Builder#box(IntSupplier, IntSupplier, int)} and
     * {@link TutorialStep.Builder#pointAt(IntSupplier, IntSupplier)} to point the arrow at a specific point. A box is required to create a tutorial step!
     * <br></br>
     * 3. To make it highlight a screen element, you can use {@link TutorialStep.Builder#reveal(TutorialStep.Reveal)} and include the element rendering method,
     * most likely {@link AbstractWidget#extractRenderState(GuiGraphicsExtractor, int, int, float)} )} in a lambda.
     * <br></br>
     * 4. You can also add an optional outline around a specific widget by either using {@link Rect#of(AbstractWidget)}
     * or by providing specific coordinates by using {@code new Rect(int, int, int, int)}
     * <br></br>
     * 5. Finally to build the tutorial step, you can use {@link TutorialStep.Builder#build()} to return a new {@link TutorialStep} instance,
     * with the parameters mentioned above.
     */

    private void buildTutorial() {
        tutorialSteps = List.of(
                TutorialStep.step("itemblocker.tutorial.item_ids")
                        .box(() -> width / 2 - 300, () -> height / 2 - 20, 180)
                        .pointAt(() -> inputField.getX(), () -> inputField.getY() + 20)
                        .reveal((g, mx, my, d) -> inputField.extractRenderState(g, mx, my, d))
                        .build(),

                TutorialStep.step("itemblocker.tutorial.submit_item")
                        .box(() -> width / 2 + 100, () -> height / 2 - 10, 180)
                        .pointAt(() -> addButton.getX() + addButton.getWidth(), () -> addButton.getY() + addButton.getHeight())
                        .outline(() -> Rect.of(addButton))
                        .reveal((g, mx, my, d) -> addButton.extractRenderState(g, mx, my, d))
                        .build(),

                TutorialStep.step("itemblocker.tutorial.clear_all")
                        .box(() -> width / 2 - 180, () -> height / 2 + 40, 180)
                        .pointAt(() -> clearX + 9, () -> rowY + 17)
                        .reveal(animatedButtonReveal("bin.png", () -> clearX, 0.5f))
                        .build(),

                TutorialStep.step("itemblocker.tutorial.options")
                        .box(() -> width / 2 - 50, () -> height / 2 + 40, 180)
                        .pointAt(() -> optionsX + 9, () -> rowY + 17)
                        .reveal(animatedButtonReveal("settings.png", () -> optionsX, 0.5f))
                        .build(),

                TutorialStep.step("itemblocker.tutorial.presets")
                        .box(() -> width / 2 + 30, () -> height / 2 + 20, 200)
                        .pointAt(() -> presetsX + 9, () -> rowY + 17)
                        .reveal(animatedButtonReveal("presets.png", () -> presetsX, 0.3f))
                        .seconds(6f)
                        .build(),

                TutorialStep.step("itemblocker.tutorial.blocked_items")
                        .box(() -> width / 2 - 310, () -> getListY() + getListHeight() / 2 - 30, 176)
                        .pointAt(this::getListX, () -> getListY() + getListHeight() / 2)
                        .outline(() -> new Rect(getListX(), getListY(), getListWidth(), getListHeight()))
                        .reveal((g, mx, my, d) -> renderWidget(g, mx, my))
                        .seconds(7.5f)
                        .build(),

                TutorialStep.step("itemblocker.tutorial.tutorial")
                        .box(() -> width / 2 - 90, () -> height / 2 - 35, 150)
                        .pointAt(() -> tutorialButton.getX() + tutorialButton.getWidth(), () -> tutorialButton.getY())
                        .outline(() -> Rect.of(tutorialButton))
                        .build()
        );

        tutorialStepStarts = new float[tutorialSteps.size()];
        float t = 0f;
        for (int i = 0; i < tutorialSteps.size(); i++) {
            tutorialStepStarts[i] = t;
            t += tutorialSteps.get(i).seconds();
        }
        tutorialTotalSeconds = t;
    }


    private TutorialStep.Reveal animatedButtonReveal(String texture, IntSupplier x, float hoverProgress) {
        Identifier id = Identifier.fromNamespaceAndPath(ItemBlockerClient.MOD_ID, "textures/gui/" + texture);
        return (g, _, _, _) -> widgetGraphics.animatedButton(g, id, x.getAsInt(), rowY, hoverProgress, false, true, Component.empty());
    }

    private void renderTutorial(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!inTutorial || tutorialSteps.isEmpty()) {
            return;
        }

        float elapsed = (Util.getMillis() - tutorialStartTime) / 1000f;
        
        int bgColor = fadeManager.fadeInOut(new Fade(0x88000000, tutorialTotalSeconds, FADE_IN, FADE_OUT), tutorialStartTime);
        int skipColor = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, tutorialTotalSeconds, FADE_IN, FADE_OUT), tutorialStartTime);
        graphics.fill(0, 0, width, height, bgColor);
        graphics.text(this.font, Component.translatable("itemblocker.tutorial.skip"), 4, 8, skipColor);
        
        int current = -1;
        for (int i = 0; i < tutorialSteps.size(); i++) {
            if (elapsed >= tutorialStepStarts[i]) {
                current = i;
            }
        }
        if (current < 0 || elapsed >= tutorialTotalSeconds) {
            return;
        }

        TutorialStep step = tutorialSteps.get(current);
        long stepStart = tutorialStartTime + (long) (tutorialStepStarts[current] * 1000f);
        int white = fadeManager.fadeInOut(new Fade(0xFFFFFFFF, step.seconds(), FADE_IN, FADE_OUT), stepStart);
        int black = fadeManager.fadeInOut(new Fade(0xFF000000, step.seconds(), FADE_IN, FADE_OUT), stepStart);

        if (step.reveal() != null) {
            step.reveal().draw(graphics, mouseX, mouseY, delta);
        }

        if (step.outline() != null) {
            Rect r = step.outline().get();
            graphics.outline(r.x(), r.y(), r.w(), r.h(), white);
        }
        
        Component text = Component.translatable(step.hint());
        int boxW = step.boxW();
        int wrapW = boxW - TUTORIAL_BOX_PADDING * 2;
        int boxH = this.font.wordWrapHeight(text, wrapW) + TUTORIAL_BOX_PADDING * 2 + 4;
        int boxX = Math.clamp(step.boxX().getAsInt(), TUTORIAL_SCREEN_MARGIN, width - boxW - TUTORIAL_SCREEN_MARGIN);
        int boxY = Math.clamp(step.boxY().getAsInt(), TUTORIAL_SCREEN_MARGIN, height - boxH - TUTORIAL_SCREEN_MARGIN);
        
        if (step.targetX() != null && step.targetY() != null) {
            int tx = step.targetX().getAsInt();
            int ty = step.targetY().getAsInt();
            int ax = Math.clamp(tx, boxX, boxX + boxW);
            int ay = Math.clamp(ty, boxY, boxY + boxH);
            if (ax != tx || ay != ty) {
                widgetGraphics.arrow(graphics, ax, ay, tx, ty, 1, 5, white);
            }
        }

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, black);
        graphics.outline(boxX, boxY, boxW, boxH, white);
        graphics.textWithWordWrap(this.font, text, boxX + TUTORIAL_BOX_PADDING, boxY + TUTORIAL_BOX_PADDING, wrapW, white);
    }

    private void startTutorial() {
        inTutorial = true;
        this.tutorialButton.active = false;
        tutorialStartTime = Util.getMillis();
    }

    private void finishTutorial() {
        inTutorial = false;
        tutorialStartTime = -1;
        this.tutorialButton.active = true;
        this.tutorialButton.setFocused(false);
        BlockedItemsConfig.finishTutorial();
    }


    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
        if (inTutorial) {
            if (click.x() >= this.tutorialButton.getX() && click.x() <= this.tutorialButton.getX() + 30 &&
                    click.y() >= this.tutorialButton.getY() && click.y() <= this.tutorialButton.getY() + 20){
                return super.mouseClicked(click, doubleClick);
            }
            else {
                this.finishTutorial();
            }
        }
        int listY = this.getListY();
        int yOffset = listY + 5;
        int scrollOffset = this.getScrollOffset();

        if (click.x() >= clearX && click.x() <= clearX + 16 &&
                click.y()  >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            for (String itemId : BlockedItemsConfig.getBlockedItems()) {
                BlockedItemsConfig.removeBlockedItem(itemId);
            }
            this.refreshList();
            this.resetScrollOffset();
            Loggers.CLIENT_LOGGER.info("Cleared all blocked items");
        }

        if (click.x() >= optionsX && click.x() <= optionsX + 16 &&
                click.y() >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            minecraft.gui.setScreen(new SettingsScreen(this));
        }

        if (click.x() >= presetsX && click.x() <= presetsX + 16 &&
                click.y() >= rowY && click.y() <= rowY + 16) {
            this.playClickSound();
            minecraft.gui.setScreen(new PresetsScreen(this));
        }

        List<ItemEntry> list = this.getActiveList();
        for (int i = scrollOffset; i < list.size() && i < scrollOffset + itemsPerPage; i++) {
            ItemEntry entry = list.get(i);

            int removeX = this.width / 2 + 70;
            int removeY = yOffset;


            if (click.x() >= removeX && click.x() <= removeX + 20 &&
                    click.y()  >= removeY && click.y() <= removeY + 16) {

                if (searching) {
                    if (BlockedItemsConfig.isBlocked(entry.itemId())) {
                        BlockedItemsConfig.removeBlockedItem(entry.itemId());
                    }
                    else {
                        BlockedItemsConfig.addBlockedItem(entry.itemId());
                    }
                }
                else {
                    BlockedItemsConfig.removeBlockedItem(entry.itemId());
                    if (entry == list.getLast()) {
                        if (itemsPerPage < list.size()) {
                            this.decrementScrollOffset();
                        }
                    }
                }
                this.playClickSound();
                this.refreshList();
                Loggers.CLIENT_LOGGER.info("Removed blocked item: {}", entry.itemId());
            }
            yOffset += 22;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (inTutorial) {
            this.finishTutorial();
        }
        return super.keyPressed(event);
    }


    @Override
    protected void checkItemId() {
        itemId = this.inputField.getValue().trim();
        boolean containsMinecraftNamespace = false;
        if (!itemId.isEmpty()) {
            itemId = itemId.toLowerCase().replace(" ", "_");
            String originalItemID = itemId;


            if (!itemId.contains(":")) {
                containsMinecraftNamespace = true;
                itemId = "minecraft:" + itemId;
            }


            Identifier id = Identifier.tryParse(itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                if (!itemId.equals("minecraft:air")) {
                    if (BlockedItemsConfig.getBlockedItems().contains(itemId)) {
                        successTime = -1;
                        alreadyTime = Util.getMillis();
                    }
                    else {
                        alreadyTime = -1;
                        successTime = Util.getMillis();
                    }
                    BlockedItemsConfig.addBlockedItem(itemId);
                    invalidIDStartTime = -1;
                    this.inputField.setValue("");
                    this.refreshList();
                    Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                }
            }
            else {
                if (containsMinecraftNamespace) {
                    itemId = itemId.replace("minecraft:", "");
                }
                for (String modId : ItemBlockerClient.currentModList) {
                    itemId = modId + ":" + itemId;
                    id = Identifier.tryParse(itemId);
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                        if (BlockedItemsConfig.getBlockedItems().contains(itemId)) {
                            successTime = -1;
                            alreadyTime = Util.getMillis();
                        }
                        else {
                            alreadyTime = -1;
                            successTime = Util.getMillis();
                        }
                        BlockedItemsConfig.addBlockedItem(itemId);
                        invalidIDStartTime = -1;
                        this.inputField.setValue("");
                        this.refreshList();
                        Loggers.CLIENT_LOGGER.info("Added blocked item: {}", itemId);
                        break;
                    }
                    else {
                        invalidIDStartTime = Util.getMillis();
                        alreadyTime = -1;
                        successTime = -1;
                    }
                    itemId = originalItemID;
                }
            }
        }
    }
}