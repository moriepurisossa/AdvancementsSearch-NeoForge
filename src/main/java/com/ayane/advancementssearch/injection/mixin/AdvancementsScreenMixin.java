package com.ayane.advancementssearch.injection.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.ayane.advancementssearch.AdvancementsSearchMod;
import com.ayane.advancementssearch.HighlightType;
import com.ayane.advancementssearch.SearchByType;
import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements AdvancementsScreenExtension {

    // === Constants ===
    @Unique
    private static final ResourceLocation CREATIVE_INVENTORY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_item_search.png");

    @Unique
    private static final ResourceLocation SEARCH_ICON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancementsSearchMod.MODID, "textures/gui/search_icon.png");

    @Unique
    private static final int SEARCH_ICON_SIZE = 8;

    @Unique
    private static final Component SEARCH_TITLE = Component.translatable("gui.recipebook.search_hint");

    @Unique
    private static final int SEARCH_FIELD_UV_X = 80;

    @Unique
    private static final int SEARCH_FIELD_UV_Y = 4;

    @Unique
    private static final int SEARCH_FIELD_WIDTH = 90;

    @Unique
    private static final int SEARCH_FIELD_HEIGHT = 12;

    @Unique
    private static final int WINDOW_BORDER_SIZE = 9;

    @Unique
    private static final int WINDOW_HEADER_HEIGHT = 18;

    @Unique
    private static final int WIDGET_SIZE = 26;

    @Unique
    private static final int TREE_X_OFFSET = 3;

    @Unique
    private static final int WIDGET_HIGHLIGHT_COUNT = 5;

    @Unique
    private static final int WIDGET_HIGHLIGHT_TICKS = 3;

    @Unique
    private static final int SEARCH_FIELD_TEXT_LEFT_OFFSET = 2;

    // === State Fields ===
    @Unique
    private EditBox searchField;

    @Unique
    private AdvancementNode searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private final ArrayList<AdvancementNode> searchResults = new ArrayList<>();

    @Unique
    private boolean isSearchActive;

    @Unique
    private int searchResultsColumnsCount;

    @Unique
    private int searchResultsOriginX;

    @Unique
    private AdvancementWidget focusedAdvancementWidget;

    @Unique
    private int windowX;

    @Unique
    private int windowY;

    @Unique
    private int treeWidth;

    @Unique
    private int treeHeight;

    @Unique
    private AdvancementNode highlightedAdvancement;

    @Unique
    private ResourceLocation highlightedAdvancementId;

    @Unique
    private HighlightType highlightType;

    @Unique
    private int widgetHighlightCounter;

    @Unique
    private boolean isFocusedAdvancementClicked;

    @Unique
    private String pendingSearchText;

    /**
     * Snapshot of {@code searchField.getValue()} from the previous tick. Used by
     * {@link #advancementssearch$tick()} as a fallback to detect text changes when
     * another mod intercepts {@code ContainerEventHandler.charTyped} before our
     * ParentElementMixin handler can route it through
     * {@link #advancementssearch$charTyped(char, int)}. Polled at 20 Hz, so worst
     * case the search refresh lags one tick (~50ms) — barely perceptible to a typist.
     */
    @Unique
    private String lastSeenSearchText;

    // === Shadow fields ===
    @Shadow
    @Final
    private ClientAdvancements advancements;

    @Shadow
    @Nullable
    private AdvancementTab selectedTab;

    // Dummy constructor required for extending Screen (Mixin pattern, never actually called)
    protected AdvancementsScreenMixin() {
        super(null);
    }

    // === Extension Interface Implementations ===

    @Override
    public void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget) {
        this.focusedAdvancementWidget = focusedAdvancementWidget;
    }

    @Override
    public boolean advancementssearch$isSearchActive() {
        return isSearchActive;
    }

    @Override
    public int advancementssearch$getTreeWidth() {
        return treeWidth;
    }

    @Override
    public int advancementssearch$getTreeHeight() {
        return treeHeight;
    }

    @Override
    public ResourceLocation advancementssearch$getHighlightedAdvancementId() {
        return highlightedAdvancementId;
    }

    @Override
    public HighlightType advancementssearch$getHighlightType() {
        return highlightType;
    }

    @Override
    public boolean advancementssearch$isHighlightAtInvisibleState() {
        return widgetHighlightCounter != 0 && (widgetHighlightCounter / WIDGET_HIGHLIGHT_TICKS) % 2 == 0;
    }

    @Override
    public void advancementssearch$stopHighlight() {
        highlightedAdvancementId = null;
        highlightType = null;
        widgetHighlightCounter = 0;
    }

    @Override
    public void advancementssearch$search(
            String query,
            SearchByType searchByType,
            boolean autoHighlightSingle,
            HighlightType highlightType
    ) {
        searchInternal(query, searchByType);
        if (autoHighlightSingle && searchResults.size() == 1) {
            highlight(searchResults.getFirst(), highlightType);
            searchResults.clear();
            return;
        }
        query = SearchByType.addMaskToQuery(query, searchByType);
        if (searchField != null) {
            searchField.setValue(query);
        }
        isSearchActive = !query.isEmpty();
        showSearchResults();
    }

    @Override
    public void advancementssearch$highlightAdvancement(ResourceLocation advancementId, HighlightType highlightType) {
        for (AdvancementNode advancement : getAdvancements(false)) {
            if (advancementId.equals(advancement.holder().id())) {
                highlight(advancement, highlightType);
                break;
            }
        }
    }

    @Override
    public void advancementssearch$tick() {
        if (widgetHighlightCounter > 0) {
            widgetHighlightCounter--;
            if (widgetHighlightCounter == 0) {
                advancementssearch$stopHighlight();
            }
        }
        // Fallback for environments where other mods intercept ContainerEventHandler.charTyped
        // before our ParentElementMixin handler — poll for text changes here so search still
        // refreshes even if charTyped never reaches us.
        if (searchField != null) {
            String currentText = searchField.getValue();
            if (lastSeenSearchText == null) {
                lastSeenSearchText = currentText;
            } else if (!lastSeenSearchText.equals(currentText)) {
                lastSeenSearchText = currentText;
                searchByUser();
            }
        }
    }

    @Override
    public boolean advancementssearch$charTyped(char chr, int modifiers) {
        if (searchField != null) {
            String oldText = searchField.getValue();
            if (searchField.charTyped(chr, modifiers)) {
                if (!oldText.equals(searchField.getValue())) {
                    searchByUser();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void advancementssearch$resize(int width, int height) {
        // Save current search text into a holder; initInject will restore it after the
        // native resize -> clearWidgets -> init -> initInject sequence recreates searchField.
        if (searchField != null) {
            pendingSearchText = searchField.getValue();
        }
    }

    @Override
    public void advancementssearch$onMouseReleased(double mouseX, double mouseY, int button) {
        // GLFW_MOUSE_BUTTON_LEFT == 0
        if (isFocusedAdvancementClicked &&
                focusedAdvancementWidget != null &&
                focusedAdvancementWidget.tab == searchTab &&
                button == 0
        ) {
            ResourceLocation focusedAdvancementId = focusedAdvancementWidget.advancementNode.holder().id();
            for (AdvancementNode advancement : getAdvancements(true)) {
                if (advancement.holder().id().equals(focusedAdvancementId)) {
                    highlight(advancement, HighlightType.WIDGET);
                    break;
                }
            }
        }
    }

    // === Helper methods (Unique) ===

    @Unique
    private @NotNull ArrayList<AdvancementNode> getAdvancements(boolean shouldExcludeRoots) {
        ArrayList<AdvancementNode> result = new ArrayList<>();
        Map<AdvancementHolder, AdvancementProgress> progresses = this.advancements.progress;
        for (AdvancementHolder advancementHolder : new ArrayList<>(progresses.keySet())) {
            if (advancementHolder == null) {
                continue;
            }
            Advancement advancement = advancementHolder.value();
            // In Mojang, root status is determined by parent being null in the AdvancementNode tree.
            // We check from the tree side instead.
            AdvancementNode node = this.advancements.getTree().get(advancementHolder);
            if (node == null) {
                continue;
            }
            if (shouldExcludeRoots && node.parent() == null) {
                continue;
            }
            DisplayInfo display = advancement.display().orElse(null);
            if (display == null) {
                continue;
            }
            if (display.isHidden()) {
                AdvancementProgress progress = progresses.get(advancementHolder);
                if (progress == null || !progress.isDone()) {
                    continue;
                }
            }
            AdvancementNode rootNode = node.root();
            if (rootNode == null) {
                continue;
            }
            result.add(node);
        }
        return result;
    }

    @Unique
    private void searchByUser() {
        if (searchField == null) {
            return;
        }
        String query = searchField.getValue();
        // Sync the tick-poll cache so the fallback in tick() doesn't re-trigger.
        lastSeenSearchText = query;
        isSearchActive = !query.isEmpty();
        searchInternal(SearchByType.getQueryWithoutMask(query), SearchByType.findByMask(query));
        showSearchResults();
    }

    @Unique
    private void searchInternal(String query, SearchByType searchByType) {
        query = query.toLowerCase(Locale.ROOT);
        searchResults.clear();
        if (query.trim().isEmpty()) {
            return;
        }
        boolean checkEverywhere = searchByType == SearchByType.EVERYWHERE;
        for (AdvancementNode node : getAdvancements(true)) {
            DisplayInfo display = node.advancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            String iconName = display.getIcon().getHoverName().getString().toLowerCase(Locale.ROOT);

            if ((checkEverywhere || searchByType == SearchByType.TITLE) && title.contains(query) ||
                    (checkEverywhere || searchByType == SearchByType.DESCRIPTION) && description.contains(query) ||
                    (checkEverywhere || searchByType == SearchByType.ICON) && iconName.contains(query)
            ) {
                searchResults.add(node);
            }
        }
        searchResults.sort(Comparator.comparing(n -> n.holder().id()));

        List<AdvancementType> frameOrder = Arrays.asList(
                AdvancementType.TASK,
                AdvancementType.GOAL,
                AdvancementType.CHALLENGE
        );
        searchResults.sort((a, b) -> {
            DisplayInfo da = a.advancement().display().orElse(null);
            DisplayInfo db = b.advancement().display().orElse(null);
            if (da == null || db == null) {
                return 0;
            }
            return Integer.compare(frameOrder.indexOf(da.getType()), frameOrder.indexOf(db.getType()));
        });
    }

    @Unique
    private void showSearchResults() {
        if (searchTab == null) {
            return;
        }
        resetSearchTab();
        if (searchResults.isEmpty()) {
            return;
        }
        searchTab.addWidget(searchTab.root, searchRootAdvancement.holder());

        int rowIndex = 0;
        int columnIndex = 0;
        Map<AdvancementHolder, AdvancementProgress> progresses = this.advancements.progress;
        AdvancementNode rootAdvancement = new AdvancementNode(searchRootAdvancement.holder(), null);
        AdvancementNode parentNode = rootAdvancement;
        for (AdvancementNode searchResult : searchResults) {
            DisplayInfo srcDisplay = searchResult.advancement().display().orElse(null);
            if (srcDisplay == null) {
                continue;
            }
            DisplayInfo newDisplay = new DisplayInfo(
                    srcDisplay.getIcon(),
                    srcDisplay.getTitle(),
                    srcDisplay.getDescription(),
                    srcDisplay.getBackground(),
                    srcDisplay.getType(),
                    srcDisplay.shouldShowToast(),
                    srcDisplay.shouldAnnounceChat(),
                    srcDisplay.isHidden()
            );
            newDisplay.setLocation(columnIndex, rowIndex);

            Advancement.Builder builder = Advancement.Builder.advancement()
                    .parent(parentNode.holder())
                    .display(newDisplay)
                    .rewards(searchResult.advancement().rewards())
                    .requirements(searchResult.advancement().requirements());
            searchResult.advancement().criteria().forEach(builder::addCriterion);
            if (searchResult.advancement().sendsTelemetryEvent()) {
                builder = builder.sendsTelemetryEvent();
            }
            AdvancementHolder newHolder = builder.build(searchResult.holder().id());
            AdvancementNode newNode = new AdvancementNode(newHolder, parentNode);

            searchTab.addAdvancement(newNode);
            searchTab.widgets.get(newHolder).setProgress(progresses.get(newHolder));
            if (columnIndex == searchResultsColumnsCount - 1) {
                parentNode = rootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentNode = new AdvancementNode(newHolder, newNode);
                columnIndex++;
            }
        }
    }

    @Unique
    private void resetSearchTab() {
        if (searchTab == null) {
            return;
        }
        searchTab.minX = Integer.MAX_VALUE;
        searchTab.minY = Integer.MAX_VALUE;
        searchTab.maxX = Integer.MIN_VALUE;
        searchTab.maxY = Integer.MIN_VALUE;
        searchTab.scrollX = searchResultsOriginX;
        searchTab.scrollY = 0;
        searchTab.centered = true;
        for (AdvancementWidget widget : searchTab.widgets.values()) {
            widget.parent = null;
            widget.children.clear();
        }
        searchTab.widgets.clear();
    }

    @Unique
    private void highlight(@NotNull AdvancementNode advancement, HighlightType type) {
        if (highlightedAdvancement != null) {
            return;
        }
        isSearchActive = false;
        highlightedAdvancement = advancement;
        highlightType = type;
        this.advancements.setSelectedTab(advancement.root().holder(), true);
    }

    // === Injections ===

    /**
     * Creates the search EditBox and the virtual searchTab when the screen is initialized.
     */
    @Inject(
            method = "init",
            at = @At(value = "TAIL")
    )
    public void initInject(CallbackInfo ci) {
        searchField = new EditBox(
                this.font,
                0,
                0,
                SEARCH_FIELD_WIDTH - SEARCH_FIELD_TEXT_LEFT_OFFSET - 8,
                this.font.lineHeight,
                CommonComponents.EMPTY
        );
        searchField.setBordered(false);
        searchField.setTextColor(0xFFFFFF);
        searchField.setCanLoseFocus(false);
        addWidget(searchField);
        setInitialFocus(searchField);

        if (searchTab == null) {
            DisplayInfo searchRootDisplay = new DisplayInfo(
                    ItemStack.EMPTY,
                    Component.empty(),
                    Component.empty(),
                    Optional.empty(),
                    AdvancementType.TASK,
                    false,
                    false,
                    true
            );
            AdvancementHolder rootHolder = Advancement.Builder
                    .recipeAdvancement()
                    .display(searchRootDisplay)
                    .build(AdvancementsSearchMod.ADVANCEMENTS_SEARCH_ID);
            searchRootAdvancement = new AdvancementNode(rootHolder, null);

            AdvancementsScreen self = (AdvancementsScreen) (Object) this;
            if (this.minecraft != null) {
                searchTab = new AdvancementTab(
                        this.minecraft,
                        self,
                        null,
                        0,
                        searchRootAdvancement,
                        searchRootDisplay
                );
            }
        }

        // Restore search text across resize (set by advancementssearch$resize)
        if (pendingSearchText != null) {
            searchField.setValue(pendingSearchText);
            pendingSearchText = null;
            if (!searchField.getValue().isEmpty()) {
                isSearchActive = true;
                searchByUser();
            }
        }
    }

    /**
     * Captures the window position and computes treeWidth/treeHeight by wrapping the renderInside call.
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;renderInside(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"
            )
    )
    public void getWindowSizes(
            AdvancementsScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            Operation<Void> original
    ) {
        windowX = x;
        windowY = y;
        treeWidth = Math.abs(windowX * 2 - this.width) - WINDOW_BORDER_SIZE - WINDOW_BORDER_SIZE;
        treeHeight = Math.abs(windowY * 2 - this.height) - WINDOW_HEADER_HEIGHT - WINDOW_BORDER_SIZE;
        original.call(screen, guiGraphics, mouseX, mouseY, x, y);
    }

    /**
     * Renders the search bar background and EditBox after renderWindow is called.
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;renderWindow(Lnet/minecraft/client/gui/GuiGraphics;II)V",
                    shift = At.Shift.AFTER
            )
    )
    public void renderInject(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (searchField == null) {
            return;
        }

        int frameOffset = 1;
        int frameContainerWidth = frameOffset + WIDGET_SIZE + frameOffset;
        int columnsCount = treeWidth / frameContainerWidth;
        int rowWidth = frameContainerWidth * columnsCount;
        int horizontalOffset = treeWidth - rowWidth - TREE_X_OFFSET;
        int originX = horizontalOffset / 2;
        if (searchResultsColumnsCount != columnsCount || searchResultsOriginX != originX) {
            searchResultsColumnsCount = columnsCount;
            searchResultsOriginX = originX;
            if (isSearchActive) {
                showSearchResults();
            }
        }

        int symmetryFixX = 1;
        int fieldX = windowX + treeWidth + WINDOW_BORDER_SIZE - SEARCH_FIELD_WIDTH + symmetryFixX;
        int fieldY = windowY + 4;

        guiGraphics.blit(
                CREATIVE_INVENTORY_TEXTURE,
                fieldX,
                fieldY,
                SEARCH_FIELD_UV_X,
                SEARCH_FIELD_UV_Y,
                SEARCH_FIELD_WIDTH,
                SEARCH_FIELD_HEIGHT,
                256,
                256
        );

        searchField.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchField.setY(fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchField.render(guiGraphics, mouseX, mouseY, partialTick);

        // Magnifying glass icon OUTSIDE the bar (to its right, in the title bar area),
        // 8x8 with a small gap, vertically centered with the bar.
        int iconX = fieldX + SEARCH_FIELD_WIDTH + 2;
        int iconY = fieldY + (SEARCH_FIELD_HEIGHT - SEARCH_ICON_SIZE) / 2;
        guiGraphics.blit(SEARCH_ICON_TEXTURE, iconX, iconY, 0, 0, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
    }

    /**
     * Forward keyPressed to searchField. Capture all keys except ESC so that the screen
     * doesn't react to letters that would otherwise trigger other shortcuts.
     */
    @Inject(
            method = "keyPressed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void keyPressedInject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (searchField == null) {
            return;
        }
        String oldText = searchField.getValue();
        if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
            if (!oldText.equals(searchField.getValue())) {
                searchByUser();
            }
            cir.setReturnValue(true);
            return;
        }
        // GLFW_KEY_ESCAPE = 256
        if (keyCode != 256) {
            cir.setReturnValue(true);
        }
    }

    // Note: charTyped is intentionally NOT injected here.
    // AdvancementsScreen doesn't override charTyped (inherits from Screen),
    // so it's not a valid @Inject target. ParentElementMixin already routes
    // charTyped through advancementssearch$charTyped on this extension.

    /**
     * Forward mouseClicked to searchField; track which advancement was clicked for highlight-on-release.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void mouseClickedInject(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
            isSearchActive = !searchField.getValue().isEmpty();
            cir.setReturnValue(true);
            return;
        }
        isFocusedAdvancementClicked = focusedAdvancementWidget != null &&
                focusedAdvancementWidget.tab == searchTab &&
                button == 0; // GLFW_MOUSE_BUTTON_LEFT
    }

    /**
     * Reset the focused-clicked flag when the user scrolls.
     */
    @Inject(
            method = "mouseScrolled",
            at = @At(value = "HEAD")
    )
    private void resetFocusedAdvancementOnScroll(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        isFocusedAdvancementClicked = false;
    }

    /**
     * Reset the focused-clicked flag when the user drags.
     */
    @Inject(
            method = "mouseDragged",
            at = @At(value = "HEAD")
    )
    private void resetFocusedAdvancementOnDrag(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        isFocusedAdvancementClicked = false;
    }

    // === Tab swap redirects (4-G-5) ===

    /**
     * Critical: When in search mode, redirect rendering to the searchTab.
     * Returns null when searchTab has only the root widget (no results) to show empty state.
     */
    @Redirect(
            method = "renderInside",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable AdvancementTab renderInsideRedirect(AdvancementsScreen self) {
        if (!isSearchActive) {
            return selectedTab;
        }
        return searchTab != null && searchTab.widgets.size() > 1 ? searchTab : null;
    }

    /**
     * In search mode, make all tabs render as "unselected" (drawTab's isSelected = false).
     */
    @ModifyArgs(
            method = "renderWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;drawTab(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"
            )
    )
    private void renderWindowModifyTabSelected(Args args) {
        if (isSearchActive) {
            args.set(3, false);
        }
    }

    /**
     * In search mode, replace the window title with the search-hint label.
     * If the title is too long to fit before the search bar, scroll it back and forth.
     * GuiGraphics.drawString returns int (rendered width), so this redirect must mirror that.
     */
    @Redirect(
            method = "renderWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"
            )
    )
    private int modifyWindowTitleRender(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int x,
            int y,
            int color,
            boolean shadow
    ) {
        if (isSearchActive) {
            text = SEARCH_TITLE;
        }
        int rightEdgeX = x + treeWidth - SEARCH_FIELD_WIDTH - 3;
        int textWidth = font.width(text);
        int availableWidth = rightEdgeX - x;

        if (textWidth > availableWidth) {
            int bottomY = y + font.lineHeight;
            int excessWidth = textWidth - availableWidth;
            double timeInSeconds = Util.getMillis() / 1000.0;
            double adjustmentFactor = Math.max((double) excessWidth * 0.5, 3);
            double oscillation =
                    Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * timeInSeconds / adjustmentFactor)) / 2 + 0.5;
            double offset = Mth.lerp(oscillation, 0, excessWidth);

            guiGraphics.enableScissor(x, y, rightEdgeX, bottomY);
            int result = guiGraphics.drawString(font, text, x - (int) offset, y, color, shadow);
            guiGraphics.disableScissor();
            return result;
        }
        return guiGraphics.drawString(font, text, x, y, color, shadow);
    }

    /**
     * In search mode, redirect tooltip rendering to searchTab.
     */
    @Redirect(
            method = "renderTooltips",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;drawTooltips(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"
            )
    )
    private void renderTooltipsRedirectTab(
            AdvancementTab selectedTab,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int x,
            int y
    ) {
        if (isSearchActive && searchTab != null) {
            selectedTab = searchTab;
        }
        selectedTab.drawTooltips(guiGraphics, mouseX, mouseY, x, y);
    }

    /**
     * In search mode, redirect scrolling to searchTab.
     */
    @Redirect(
            method = "mouseScrolled",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private AdvancementTab mouseScrolledRedirect(AdvancementsScreen self) {
        return isSearchActive ? searchTab : selectedTab;
    }

    /**
     * In search mode, redirect dragging to searchTab.
     */
    @Redirect(
            method = "mouseDragged",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private AdvancementTab mouseDraggedRedirect(AdvancementsScreen self) {
        return isSearchActive ? searchTab : selectedTab;
    }

    /**
     * Clicking a tab while in search mode exits search and clears highlight.
     */
    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
            )
    )
    private void mouseClickedRedirect(
            ClientAdvancements advancements,
            AdvancementHolder tab,
            boolean tellServer
    ) {
        isSearchActive = false;
        advancementssearch$stopHighlight();
        advancements.setSelectedTab(tab, true);
    }

    /**
     * After the tree is rendered, if an advancement is queued for highlight,
     * scroll its tab to center it and start the blink counter.
     */
    @Inject(
            method = "renderInside",
            at = @At(value = "TAIL")
    )
    private void startHighlight(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int offsetX,
            int offsetY,
            CallbackInfo ci
    ) {
        if (highlightedAdvancement == null || selectedTab == null) {
            return;
        }
        for (AdvancementWidget widget : selectedTab.widgets.values()) {
            if (widget != null && widget.advancementNode == highlightedAdvancement) {
                int centerX = (WIDGET_SIZE - treeWidth) / 2;
                int centerY = (WIDGET_SIZE - treeHeight) / 2;
                selectedTab.scroll(
                        -(selectedTab.scrollX + widget.getX() + TREE_X_OFFSET + centerX),
                        -(selectedTab.scrollY + widget.getY() + centerY)
                );
                highlightedAdvancement = null;
                highlightedAdvancementId = widget.advancementNode.holder().id();
                widgetHighlightCounter = WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                break;
            }
        }
    }
}
