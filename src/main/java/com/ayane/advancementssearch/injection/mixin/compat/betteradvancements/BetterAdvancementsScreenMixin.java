package com.ayane.advancementssearch.injection.mixin.compat.betteradvancements;

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
import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementWidget;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
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

/**
 * Mirror of {@link com.ayane.advancementssearch.injection.mixin.AdvancementsScreenMixin}
 * targeting Better Advancements' replacement screen.
 *
 * <p>Phase 4-I-1: skeleton only — fields, helpers, and extension interface implementations.
 * Actual @Inject / @Redirect / @WrapOperation entries are added in Phase 4-I-2.
 */
@Mixin(BetterAdvancementsScreen.class)
public abstract class BetterAdvancementsScreenMixin extends Screen implements BetterAdvancementsScreenExtension {

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
    private static final int SEARCH_FIELD_WIDTH = 90;

    @Unique
    private static final int SEARCH_FIELD_HEIGHT = 12;

    @Unique
    private static final int WIDGET_HIGHLIGHT_COUNT = 5;

    @Unique
    private static final int WIDGET_HIGHLIGHT_TICKS = 3;

    @Unique
    private static final int SEARCH_FIELD_TEXT_LEFT_OFFSET = 2;

    @Unique
    private static final int WIDGET_SIZE = 26;

    @Unique
    private static final int TREE_X_OFFSET = 3;

    /**
     * BA's inner content panel right edge sits at {@code xRight - BA_INNER_PADDING}
     * (the panel border thickness). Aligning the search bar's right edge here makes
     * the bar line up flush with the advancement-display area's right edge.
     */
    @Unique
    private static final int BA_INNER_PADDING = 9;

    // === State Fields ===
    @Unique
    private EditBox advancementssearch$ba$searchField;

    @Unique
    private AdvancementNode advancementssearch$ba$searchRootAdvancement;

    @Unique
    private BetterAdvancementTab advancementssearch$ba$searchTab;

    @Unique
    private final ArrayList<AdvancementNode> advancementssearch$ba$searchResults = new ArrayList<>();

    @Unique
    private boolean advancementssearch$ba$isSearchActive;

    @Unique
    private int advancementssearch$ba$searchResultsColumnsCount;

    @Unique
    private int advancementssearch$ba$searchResultsOriginX;

    @Unique
    private BetterAdvancementWidget advancementssearch$ba$focusedAdvancementWidget;

    @Unique
    private int advancementssearch$ba$windowX;

    @Unique
    private int advancementssearch$ba$windowY;

    @Unique
    private int advancementssearch$ba$treeWidth;

    @Unique
    private int advancementssearch$ba$treeHeight;

    @Unique
    private AdvancementNode advancementssearch$ba$highlightedAdvancement;

    @Unique
    private ResourceLocation advancementssearch$ba$highlightedAdvancementId;

    @Unique
    private HighlightType advancementssearch$ba$highlightType;

    @Unique
    private int advancementssearch$ba$widgetHighlightCounter;

    @Unique
    private boolean advancementssearch$ba$isFocusedAdvancementClicked;

    @Unique
    private String advancementssearch$ba$pendingSearchText;

    /**
     * Snapshot of {@code searchField.getValue()} from the previous tick. Used by
     * {@link #advancementssearch$ba$tick()} as a fallback to detect text changes
     * when another mod intercepts {@code ContainerEventHandler.charTyped} before
     * our ParentElementMixin handler can route it through
     * {@link #advancementssearch$ba$charTyped(char, int)}. Polled at 20 Hz, so worst
     * case the search refresh lags one tick (~50ms) — barely perceptible to a typist.
     */
    @Unique
    private String advancementssearch$ba$lastSeenSearchText;

    // === Shadow fields (mapped via AT) ===
    @Shadow
    @Final
    private ClientAdvancements clientAdvancements;

    @Shadow
    @Nullable
    private BetterAdvancementTab selectedTab;

    @Shadow
    @Final
    private Map<AdvancementHolder, BetterAdvancementTab> tabs;

    @Shadow
    protected int internalWidth;

    @Shadow
    protected int internalHeight;

    // Dummy constructor required for extending Screen (Mixin pattern, never actually called)
    protected BetterAdvancementsScreenMixin() {
        super(null);
    }

    // === Extension Interface Implementations ===

    @Override
    public void advancementssearch$ba$setFocusedAdvancementWidget(Object focusedAdvancementWidget) {
        this.advancementssearch$ba$focusedAdvancementWidget = (BetterAdvancementWidget) focusedAdvancementWidget;
    }

    @Override
    public boolean advancementssearch$ba$isSearchActive() {
        return advancementssearch$ba$isSearchActive;
    }

    @Override
    public int advancementssearch$ba$getTreeWidth() {
        return advancementssearch$ba$treeWidth;
    }

    @Override
    public int advancementssearch$ba$getTreeHeight() {
        return advancementssearch$ba$treeHeight;
    }

    @Override
    public ResourceLocation advancementssearch$ba$getHighlightedAdvancementId() {
        return advancementssearch$ba$highlightedAdvancementId;
    }

    @Override
    public HighlightType advancementssearch$ba$getHighlightType() {
        return advancementssearch$ba$highlightType;
    }

    @Override
    public boolean advancementssearch$ba$isHighlightAtInvisibleState() {
        return advancementssearch$ba$widgetHighlightCounter != 0
                && (advancementssearch$ba$widgetHighlightCounter / WIDGET_HIGHLIGHT_TICKS) % 2 == 0;
    }

    @Override
    public void advancementssearch$ba$stopHighlight() {
        advancementssearch$ba$highlightedAdvancementId = null;
        advancementssearch$ba$highlightType = null;
        advancementssearch$ba$widgetHighlightCounter = 0;
    }

    @Override
    public void advancementssearch$ba$search(
            String query,
            SearchByType searchByType,
            boolean autoHighlightSingle,
            HighlightType highlightType
    ) {
        advancementssearch$ba$searchInternal(query, searchByType);
        if (autoHighlightSingle && advancementssearch$ba$searchResults.size() == 1) {
            advancementssearch$ba$highlight(advancementssearch$ba$searchResults.getFirst(), highlightType);
            advancementssearch$ba$searchResults.clear();
            return;
        }
        query = SearchByType.addMaskToQuery(query, searchByType);
        if (advancementssearch$ba$searchField != null) {
            advancementssearch$ba$searchField.setValue(query);
        }
        advancementssearch$ba$isSearchActive = !query.isEmpty();
        advancementssearch$ba$showSearchResults();
    }

    @Override
    public void advancementssearch$ba$highlightAdvancement(ResourceLocation advancementId, HighlightType highlightType) {
        for (AdvancementNode advancement : advancementssearch$ba$getAdvancements(false)) {
            if (advancementId.equals(advancement.holder().id())) {
                advancementssearch$ba$highlight(advancement, highlightType);
                break;
            }
        }
    }

    @Override
    public void advancementssearch$ba$tick() {
        if (advancementssearch$ba$widgetHighlightCounter > 0) {
            advancementssearch$ba$widgetHighlightCounter--;
            if (advancementssearch$ba$widgetHighlightCounter == 0) {
                advancementssearch$ba$stopHighlight();
            }
        }
        // Fallback for environments where other mods intercept ContainerEventHandler.charTyped
        // before our ParentElementMixin handler — poll for text changes here so search still
        // refreshes even if charTyped never reaches us. Verified-needed in ATM10 modpack;
        // harmless overhead in vanilla / clean BA envs (no-op when text unchanged).
        if (advancementssearch$ba$searchField != null) {
            String currentText = advancementssearch$ba$searchField.getValue();
            if (advancementssearch$ba$lastSeenSearchText == null) {
                advancementssearch$ba$lastSeenSearchText = currentText;
            } else if (!advancementssearch$ba$lastSeenSearchText.equals(currentText)) {
                advancementssearch$ba$lastSeenSearchText = currentText;
                advancementssearch$ba$searchByUser();
            }
        }
    }

    @Override
    public boolean advancementssearch$ba$charTyped(char chr, int modifiers) {
        if (advancementssearch$ba$searchField != null) {
            String oldText = advancementssearch$ba$searchField.getValue();
            if (advancementssearch$ba$searchField.charTyped(chr, modifiers)) {
                if (!oldText.equals(advancementssearch$ba$searchField.getValue())) {
                    advancementssearch$ba$searchByUser();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void advancementssearch$ba$resize(int width, int height) {
        if (advancementssearch$ba$searchField != null) {
            advancementssearch$ba$pendingSearchText = advancementssearch$ba$searchField.getValue();
        }
    }

    @Override
    public void advancementssearch$ba$onMouseReleased(double mouseX, double mouseY, int button) {
        // GLFW_MOUSE_BUTTON_LEFT == 0
        // TODO Phase 4-I-2: hook focus-click-release behavior once searchTab semantics are decided
        //   (BA does not have a hideable hidden-tab concept the same way vanilla does).
        if (advancementssearch$ba$isFocusedAdvancementClicked
                && advancementssearch$ba$focusedAdvancementWidget != null
                && button == 0
        ) {
            ResourceLocation focusedAdvancementId =
                    advancementssearch$ba$focusedAdvancementWidget.getAdvancement().holder().id();
            for (AdvancementNode advancement : advancementssearch$ba$getAdvancements(true)) {
                if (advancement.holder().id().equals(focusedAdvancementId)) {
                    advancementssearch$ba$highlight(advancement, HighlightType.WIDGET);
                    break;
                }
            }
        }
    }

    // === Helper methods (Unique) ===

    @Unique
    private @NotNull ArrayList<AdvancementNode> advancementssearch$ba$getAdvancements(boolean shouldExcludeRoots) {
        ArrayList<AdvancementNode> result = new ArrayList<>();
        Map<AdvancementHolder, AdvancementProgress> progresses = this.clientAdvancements.progress;
        for (AdvancementHolder advancementHolder : new ArrayList<>(progresses.keySet())) {
            if (advancementHolder == null) {
                continue;
            }
            Advancement advancement = advancementHolder.value();
            AdvancementNode node = this.clientAdvancements.getTree().get(advancementHolder);
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
    private void advancementssearch$ba$searchByUser() {
        if (advancementssearch$ba$searchField == null) {
            return;
        }
        String query = advancementssearch$ba$searchField.getValue();
        // Sync the tick-poll cache so the fallback in tick() doesn't re-trigger.
        advancementssearch$ba$lastSeenSearchText = query;
        advancementssearch$ba$isSearchActive = !query.isEmpty();
        advancementssearch$ba$searchInternal(SearchByType.getQueryWithoutMask(query), SearchByType.findByMask(query));
        advancementssearch$ba$showSearchResults();
    }

    /**
     * Build synthetic AdvancementNodes for search results and feed them into the
     * pre-built {@link #advancementssearch$ba$searchTab}. Mirror of vanilla's
     * showSearchResults but adapted for BA's tab API (addAdvancement / getWidget).
     */
    @Unique
    private void advancementssearch$ba$showSearchResults() {
        if (advancementssearch$ba$searchTab == null) {
            return;
        }
        advancementssearch$ba$resetSearchTab();
        if (advancementssearch$ba$searchResults.isEmpty()) {
            return;
        }
        BetterAdvancementTabAccessor tabAcc = (BetterAdvancementTabAccessor) advancementssearch$ba$searchTab;
        tabAcc.advancementssearch$ba$addWidget(
                tabAcc.advancementssearch$ba$getRoot(),
                advancementssearch$ba$searchRootAdvancement.holder()
        );

        int rowIndex = 0;
        int columnIndex = 0;
        Map<AdvancementHolder, AdvancementProgress> progresses = this.clientAdvancements.progress;
        AdvancementNode rootAdvancement =
                new AdvancementNode(advancementssearch$ba$searchRootAdvancement.holder(), null);
        AdvancementNode parentNode = rootAdvancement;

        for (AdvancementNode searchResult : advancementssearch$ba$searchResults) {
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

            advancementssearch$ba$searchTab.addAdvancement(newNode);
            // BA's BetterAdvancementWidget.getAdvancementProgress is a SETTER despite
            // the `get` prefix (return type is void). Don't mistake it for a getter.
            BetterAdvancementWidget widget = advancementssearch$ba$searchTab.getWidget(newHolder);
            if (widget != null) {
                widget.getAdvancementProgress(progresses.get(newHolder));
            }
            if (columnIndex == advancementssearch$ba$searchResultsColumnsCount - 1) {
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
    private void advancementssearch$ba$resetSearchTab() {
        if (advancementssearch$ba$searchTab == null) {
            return;
        }
        BetterAdvancementTabAccessor tabAcc = (BetterAdvancementTabAccessor) advancementssearch$ba$searchTab;
        tabAcc.advancementssearch$ba$setMinX(Integer.MAX_VALUE);
        tabAcc.advancementssearch$ba$setMinY(Integer.MAX_VALUE);
        tabAcc.advancementssearch$ba$setMaxX(Integer.MIN_VALUE);
        tabAcc.advancementssearch$ba$setMaxY(Integer.MIN_VALUE);
        tabAcc.advancementssearch$ba$setScrollX(advancementssearch$ba$searchResultsOriginX);
        tabAcc.advancementssearch$ba$setScrollY(0);
        tabAcc.advancementssearch$ba$setCentered(true);
        // BA's BetterAdvancementWidget uses `private BetterAdvancementWidget parent`
        // (writable) and `private final List<BetterAdvancementWidget> children` (cleared
        // via .clear()). Accessed via BetterAdvancementWidgetAccessor.
        Map<AdvancementHolder, BetterAdvancementWidget> widgets = tabAcc.advancementssearch$ba$getWidgets();
        for (BetterAdvancementWidget widget : widgets.values()) {
            BetterAdvancementWidgetAccessor wAcc = (BetterAdvancementWidgetAccessor) widget;
            wAcc.advancementssearch$ba$setParent(null);
            wAcc.advancementssearch$ba$getChildren().clear();
        }
        widgets.clear();
    }

    @Unique
    private void advancementssearch$ba$searchInternal(String query, SearchByType searchByType) {
        query = query.toLowerCase(Locale.ROOT);
        advancementssearch$ba$searchResults.clear();
        if (query.trim().isEmpty()) {
            return;
        }
        boolean checkEverywhere = searchByType == SearchByType.EVERYWHERE;
        for (AdvancementNode node : advancementssearch$ba$getAdvancements(true)) {
            DisplayInfo display = node.advancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            String iconName = display.getIcon().getHoverName().getString().toLowerCase(Locale.ROOT);

            if ((checkEverywhere || searchByType == SearchByType.TITLE) && title.contains(query)
                    || (checkEverywhere || searchByType == SearchByType.DESCRIPTION) && description.contains(query)
                    || (checkEverywhere || searchByType == SearchByType.ICON) && iconName.contains(query)
            ) {
                advancementssearch$ba$searchResults.add(node);
            }
        }
        advancementssearch$ba$searchResults.sort(Comparator.comparing(n -> n.holder().id()));

        List<AdvancementType> frameOrder = Arrays.asList(
                AdvancementType.TASK,
                AdvancementType.GOAL,
                AdvancementType.CHALLENGE
        );
        advancementssearch$ba$searchResults.sort((a, b) -> {
            DisplayInfo da = a.advancement().display().orElse(null);
            DisplayInfo db = b.advancement().display().orElse(null);
            if (da == null || db == null) {
                return 0;
            }
            return Integer.compare(frameOrder.indexOf(da.getType()), frameOrder.indexOf(db.getType()));
        });
    }

    @Unique
    private void advancementssearch$ba$highlight(@NotNull AdvancementNode advancement, HighlightType type) {
        if (advancementssearch$ba$highlightedAdvancement != null) {
            return;
        }
        advancementssearch$ba$isSearchActive = false;
        advancementssearch$ba$highlightedAdvancement = advancement;
        advancementssearch$ba$highlightType = type;
        // BA does not expose ClientAdvancements directly the same way — we still call the
        // vanilla API which BA listens to via its onSelectedTabChanged listener hook.
        this.clientAdvancements.setSelectedTab(advancement.root().holder(), true);
    }

    // ============================================================
    // === Phase 4-I-2a injections — search bar UI only ===========
    // ============================================================

    /**
     * Create the search EditBox and the virtual searchTab at the end of BA's init.
     * The searchTab is a real {@link BetterAdvancementTab} that we keep OUT of the
     * {@code tabs} map, so BA never draws it as a side-tab. We swap it in via
     * @Redirect when search mode is active.
     */
    @Inject(
            method = "init",
            at = @At(value = "TAIL")
    )
    public void advancementssearch$ba$initInject(CallbackInfo ci) {
        advancementssearch$ba$searchField = new EditBox(
                this.font,
                0,
                0,
                SEARCH_FIELD_WIDTH - SEARCH_FIELD_TEXT_LEFT_OFFSET - 8,
                this.font.lineHeight,
                CommonComponents.EMPTY
        );
        advancementssearch$ba$searchField.setBordered(false);
        advancementssearch$ba$searchField.setTextColor(0xFFFFFF);
        advancementssearch$ba$searchField.setCanLoseFocus(false);
        addWidget(advancementssearch$ba$searchField);
        setInitialFocus(advancementssearch$ba$searchField);

        if (advancementssearch$ba$searchTab == null) {
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
            advancementssearch$ba$searchRootAdvancement = new AdvancementNode(rootHolder, null);

            BetterAdvancementsScreen self = (BetterAdvancementsScreen) (Object) this;
            if (this.minecraft != null) {
                // BetterAdvancementTabType cannot be null (constructor likely dereferences it),
                // so we pass ABOVE — it's only used by drawTab() which we never call on searchTab
                // (searchTab is kept out of the `tabs` map, so BA's renderWindow never iterates it).
                advancementssearch$ba$searchTab = new BetterAdvancementTab(
                        this.minecraft,
                        self,
                        BetterAdvancementTabType.ABOVE,
                        0,
                        advancementssearch$ba$searchRootAdvancement,
                        searchRootDisplay
                );
            }
        }

        // Restore search text across resize (set by advancementssearch$ba$resize)
        if (advancementssearch$ba$pendingSearchText != null) {
            advancementssearch$ba$searchField.setValue(advancementssearch$ba$pendingSearchText);
            advancementssearch$ba$pendingSearchText = null;
            if (!advancementssearch$ba$searchField.getValue().isEmpty()) {
                advancementssearch$ba$isSearchActive = true;
                advancementssearch$ba$searchByUser();
            }
        }
    }

    /**
     * Draw the search bar background + EditBox after BA finishes rendering its window
     * chrome. Position derived from BA's known layout formula:
     *   xLeft  = 30 + (width - internalWidth) / 2
     *   yTop   = 40 + (height - internalHeight) / 2
     *   xRight = (internalWidth - 30) + (width - internalWidth) / 2
     *
     * <p>Inner content lives between (xLeft, yTop) and (xRight, yBottom); we anchor
     * the search bar to (xRight - SEARCH_FIELD_WIDTH, yTop + 4) so it sits at the
     * top-right corner of the inner panel.
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;renderWindow(Lnet/minecraft/client/gui/GuiGraphics;IIIIII)V",
                    shift = At.Shift.AFTER
            )
    )
    public void advancementssearch$ba$renderInject(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (advancementssearch$ba$searchField == null) {
            return;
        }

        // Recompute search-result grid layout based on captured tree dimensions.
        // If columns/origin changed, repopulate the searchTab so the grid wraps correctly.
        int frameOffset = 1;
        int frameContainerWidth = frameOffset + WIDGET_SIZE + frameOffset;
        int columnsCount = advancementssearch$ba$treeWidth > 0
                ? advancementssearch$ba$treeWidth / frameContainerWidth : 0;
        int rowWidth = frameContainerWidth * columnsCount;
        int horizontalOffset = advancementssearch$ba$treeWidth - rowWidth - TREE_X_OFFSET;
        int originX = horizontalOffset / 2;
        if (advancementssearch$ba$searchResultsColumnsCount != columnsCount
                || advancementssearch$ba$searchResultsOriginX != originX) {
            advancementssearch$ba$searchResultsColumnsCount = columnsCount;
            advancementssearch$ba$searchResultsOriginX = originX;
            if (advancementssearch$ba$isSearchActive) {
                advancementssearch$ba$showSearchResults();
            }
        }

        int xMarginH = (this.width - this.internalWidth) / 2;
        int yMarginV = (this.height - this.internalHeight) / 2;
        int xRight = (this.internalWidth - 30) + xMarginH;
        int yTop = 40 + yMarginV;

        int symmetryFixX = 1;
        // Align the bar's right edge with the inner-content panel's right edge so
        // the bar lines up flush with where advancements are rendered.
        int fieldX = xRight - BA_INNER_PADDING - SEARCH_FIELD_WIDTH + symmetryFixX;
        int fieldY = yTop + 4;

        guiGraphics.blit(
                CREATIVE_INVENTORY_TEXTURE,
                fieldX,
                fieldY,
                80,
                4,
                SEARCH_FIELD_WIDTH,
                SEARCH_FIELD_HEIGHT,
                256,
                256
        );

        advancementssearch$ba$searchField.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        advancementssearch$ba$searchField.setY(fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        advancementssearch$ba$searchField.render(guiGraphics, mouseX, mouseY, partialTick);

        // Magnifying glass icon INSIDE the bar at the right edge (8x8, vertically centered).
        int iconX = fieldX + SEARCH_FIELD_WIDTH - SEARCH_ICON_SIZE;
        int iconY = fieldY + (SEARCH_FIELD_HEIGHT - SEARCH_ICON_SIZE) / 2;
        guiGraphics.blit(SEARCH_ICON_TEXTURE, iconX, iconY, 0, 0, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
    }

    /**
     * Forward keyPressed to the search field. Mirror of vanilla's keyPressedInject.
     * Capture all keys except ESC so that BA's keybind shortcuts (zoom, pan, etc.)
     * don't fire while the user is typing.
     */
    @Inject(
            method = "keyPressed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void advancementssearch$ba$keyPressedInject(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (advancementssearch$ba$searchField == null) {
            return;
        }
        String oldText = advancementssearch$ba$searchField.getValue();
        if (advancementssearch$ba$searchField.keyPressed(keyCode, scanCode, modifiers)) {
            if (!oldText.equals(advancementssearch$ba$searchField.getValue())) {
                advancementssearch$ba$searchByUser();
            }
            cir.setReturnValue(true);
            return;
        }
        // GLFW_KEY_ESCAPE = 256 — let BA close the screen
        if (keyCode != 256) {
            cir.setReturnValue(true);
        }
    }

    // ============================================================
    // === Phase 4-I-2b injections — search results & tab swap ===
    // ============================================================

    /**
     * Capture window position + tree dimensions by wrapping BA's renderInside call
     * from render(). BA already computes these values (left/top/right/bottom/width/height)
     * before calling renderInside, so we just snapshot them and call through.
     *
     * <p>BA's renderInside signature: {@code (GuiGraphics, mouseX, mouseY, left, top,
     * right, bottom, width, height)} — 1 GuiGraphics + 8 ints.
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;renderInside(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIII)V"
            )
    )
    public void advancementssearch$ba$getWindowSizes(
            BetterAdvancementsScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom,
            int width,
            int height,
            Operation<Void> original
    ) {
        advancementssearch$ba$windowX = left;
        advancementssearch$ba$windowY = top;
        advancementssearch$ba$treeWidth = width;
        advancementssearch$ba$treeHeight = height;
        original.call(screen, guiGraphics, mouseX, mouseY, left, top, right, bottom, width, height);
    }

    /**
     * Critical: when in search mode, redirect renderInside's selectedTab access
     * to our searchTab so the search results render in place of the active tab's tree.
     * Returns null when searchTab has only the root widget (no results) to show empty.
     */
    @Redirect(
            method = "renderInside",
            at = @At(
                    value = "FIELD",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;selectedTab:Lbetteradvancements/common/gui/BetterAdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable BetterAdvancementTab advancementssearch$ba$renderInsideRedirect(BetterAdvancementsScreen self) {
        if (!advancementssearch$ba$isSearchActive) {
            return selectedTab;
        }
        return advancementssearch$ba$searchTab != null
                && ((BetterAdvancementTabAccessor) advancementssearch$ba$searchTab)
                        .advancementssearch$ba$getWidgets().size() > 1
                ? advancementssearch$ba$searchTab : null;
    }

    /**
     * In search mode, redirect renderToolTips' selectedTab access to searchTab.
     */
    @Redirect(
            method = "renderToolTips",
            at = @At(
                    value = "FIELD",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;selectedTab:Lbetteradvancements/common/gui/BetterAdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable BetterAdvancementTab advancementssearch$ba$renderToolTipsRedirect(BetterAdvancementsScreen self) {
        if (!advancementssearch$ba$isSearchActive) {
            return selectedTab;
        }
        return advancementssearch$ba$searchTab != null
                && ((BetterAdvancementTabAccessor) advancementssearch$ba$searchTab)
                        .advancementssearch$ba$getWidgets().size() > 1
                ? advancementssearch$ba$searchTab : null;
    }

    /**
     * In search mode, redirect scrolling to searchTab so the user can scroll search results.
     */
    @Redirect(
            method = "mouseScrolled",
            at = @At(
                    value = "FIELD",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;selectedTab:Lbetteradvancements/common/gui/BetterAdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable BetterAdvancementTab advancementssearch$ba$mouseScrolledRedirect(BetterAdvancementsScreen self) {
        return advancementssearch$ba$isSearchActive ? advancementssearch$ba$searchTab : selectedTab;
    }

    /**
     * In search mode, redirect dragging to searchTab so the user can pan search results.
     */
    @Redirect(
            method = "mouseDragged",
            at = @At(
                    value = "FIELD",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementsScreen;selectedTab:Lbetteradvancements/common/gui/BetterAdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable BetterAdvancementTab advancementssearch$ba$mouseDraggedRedirect(BetterAdvancementsScreen self) {
        return advancementssearch$ba$isSearchActive ? advancementssearch$ba$searchTab : selectedTab;
    }

    // ============================================================
    // === Phase 4-I-2c injections — click-to-jump & UX polish ===
    // ============================================================

    @Unique
    private static final Component SEARCH_TITLE = Component.translatable("gui.recipebook.search_hint");

    /**
     * Forward mouseClicked to searchField; track which advancement was clicked for
     * highlight-on-release. Mirror of vanilla mouseClickedInject, adapted for BA's
     * private widget field {@code betterAdvancementTabGui} (accessed via accessor).
     */
    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void advancementssearch$ba$mouseClickedInject(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (advancementssearch$ba$searchField != null
                && advancementssearch$ba$searchField.mouseClicked(mouseX, mouseY, button)) {
            advancementssearch$ba$isSearchActive =
                    !advancementssearch$ba$searchField.getValue().isEmpty();
            cir.setReturnValue(true);
            return;
        }
        // GLFW_MOUSE_BUTTON_LEFT == 0; check that the focused widget belongs to OUR searchTab.
        advancementssearch$ba$isFocusedAdvancementClicked =
                advancementssearch$ba$focusedAdvancementWidget != null
                && ((BetterAdvancementWidgetAccessor) advancementssearch$ba$focusedAdvancementWidget)
                        .advancementssearch$ba$getTab() == advancementssearch$ba$searchTab
                && button == 0;
    }

    /** Reset the focused-clicked flag on scroll. */
    @Inject(
            method = "mouseScrolled",
            at = @At(value = "HEAD")
    )
    private void advancementssearch$ba$resetFocusedAdvancementOnScroll(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        advancementssearch$ba$isFocusedAdvancementClicked = false;
    }

    /** Reset the focused-clicked flag on drag. */
    @Inject(
            method = "mouseDragged",
            at = @At(value = "HEAD")
    )
    private void advancementssearch$ba$resetFocusedAdvancementOnDrag(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        advancementssearch$ba$isFocusedAdvancementClicked = false;
    }

    /**
     * Clicking a tab while in search mode exits search and clears any pending highlight.
     * BA's mouseClicked calls {@code ClientAdvancements.setSelectedTab} on tab click,
     * same as vanilla — redirect that call to also tear down our search state first.
     */
    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
            )
    )
    private void advancementssearch$ba$mouseClickedTabRedirect(
            ClientAdvancements advancements,
            AdvancementHolder tab,
            boolean tellServer
    ) {
        advancementssearch$ba$isSearchActive = false;
        advancementssearch$ba$stopHighlight();
        advancements.setSelectedTab(tab, true);
    }

    /**
     * In search mode, force every tab to render as unselected. BA's {@code drawTab}
     * takes 6 args: {@code (GuiGraphics, int, int, int, int, boolean isSelected)} —
     * isSelected sits at index 5. The drawTab call site is inside renderWindow's
     * tab-iteration loop (NOT render — verified via bytecode disassembly).
     */
    @ModifyArgs(
            method = "renderWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementTab;drawTab(Lnet/minecraft/client/gui/GuiGraphics;IIIIZ)V"
            )
    )
    private void advancementssearch$ba$renderModifyTabSelected(Args args) {
        if (advancementssearch$ba$isSearchActive) {
            args.set(5, false);
        }
    }

    /**
     * In search mode, replace the window title with the search-hint label.
     *
     * <p>BA's title is drawn in <b>renderWindow</b> using a 6-arg shadow variant on a
     * pre-composited {@link FormattedCharSequence}:
     * {@code drawString(Font, FormattedCharSequence, IIIZ)I}. The composite holds
     * "{@code TITLE - <selectedTab.title>}", which we replace wholesale with the
     * search-hint sequence while search is active.
     *
     * <p>(Re-verified via javap: offset 425 getstatic TITLE, offset 502 drawString
     * with shadow variant — all in renderWindow, NOT render. The 5-arg
     * {@code (Font, FormattedCharSequence, III)I} call at render offset 201 is the
     * tab-count display, not the title.)
     */
    @Redirect(
            method = "renderWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
            )
    )
    private int advancementssearch$ba$modifyWindowTitleRender(
            GuiGraphics guiGraphics,
            Font font,
            FormattedCharSequence text,
            int x,
            int y,
            int color,
            boolean shadow
    ) {
        if (advancementssearch$ba$isSearchActive) {
            text = SEARCH_TITLE.getVisualOrderText();
        }
        // Right edge of the title area: leave room for the search bar
        int rightEdgeX = x + advancementssearch$ba$treeWidth - SEARCH_FIELD_WIDTH - BA_INNER_PADDING - 3;
        int textWidth = font.width(text);
        int availableWidth = rightEdgeX - x;

        if (textWidth > availableWidth && availableWidth > 0) {
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

    // ============================================================
    // === Phase 4-I-2d injection — highlight scroll-to-center ===
    // ============================================================

    /**
     * After the tree is rendered, if an advancement is queued for highlight, position
     * the tab's scroll so the target widget is centered AND start the blink counter.
     *
     * <p>IMPORTANT: BA's {@code scroll(dx, dy, viewW, viewH)} method silently no-ops
     * when {@code maxX - minX <= viewW} (tree fits in viewport) — its clamp logic only
     * runs the assignment branch when the tree overflows. For programmatic centering
     * (highlight), we want to ALWAYS position the widget at viewport center, even on
     * small trees. So we bypass scroll() and set scrollX/scrollY directly via the
     * accessor.
     *
     * <p>BA's renderInside actual arg order (verified via javap on render() call site):
     * {@code (GuiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip)}.
     * The last two ints are tab pagination params, NOT viewport dimensions — we compute
     * the real viewport size as {@code right - left} and {@code bottom - top}.
     *
     * <p>Comparison uses advancement ID (not reference identity) for robustness across
     * any tree-reload edge cases.
     */
    @Inject(
            method = "renderInside",
            at = @At(value = "TAIL")
    )
    private void advancementssearch$ba$startHighlight(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom,
            int maxTabs,
            int skip,
            CallbackInfo ci
    ) {
        if (advancementssearch$ba$highlightedAdvancement == null || selectedTab == null) {
            return;
        }
        int viewW = right - left;
        int viewH = bottom - top;
        ResourceLocation targetId = advancementssearch$ba$highlightedAdvancement.holder().id();
        BetterAdvancementTabAccessor tabAcc = (BetterAdvancementTabAccessor) selectedTab;
        for (BetterAdvancementWidget widget : tabAcc.advancementssearch$ba$getWidgets().values()) {
            if (widget != null
                    && widget.getAdvancement() != null
                    && targetId.equals(widget.getAdvancement().holder().id())
            ) {
                int targetScrollX = (viewW - WIDGET_SIZE) / 2 - widget.getX() - TREE_X_OFFSET;
                int targetScrollY = (viewH - WIDGET_SIZE) / 2 - widget.getY();
                tabAcc.advancementssearch$ba$setScrollX(targetScrollX);
                tabAcc.advancementssearch$ba$setScrollY(targetScrollY);
                tabAcc.advancementssearch$ba$setCentered(true);

                advancementssearch$ba$highlightedAdvancement = null;
                advancementssearch$ba$highlightedAdvancementId = targetId;
                advancementssearch$ba$widgetHighlightCounter =
                        WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                return;
            }
        }
    }
}
