package com.ayane.advancementssearch.injection.extension;

import com.ayane.advancementssearch.HighlightType;
import com.ayane.advancementssearch.SearchByType;

import net.minecraft.resources.ResourceLocation;

/**
 * Parallel of {@link AdvancementsScreenExtension} for the Better Advancements
 * replacement screen. Kept separate so each Mixin can hold its own
 * `focusedAdvancementWidget` field with the correct concrete type
 * (vanilla AdvancementWidget vs. BetterAdvancementWidget).
 */
public interface BetterAdvancementsScreenExtension {

    void advancementssearch$ba$setFocusedAdvancementWidget(Object focusedAdvancementWidget);

    boolean advancementssearch$ba$isSearchActive();

    int advancementssearch$ba$getTreeWidth();

    int advancementssearch$ba$getTreeHeight();

    ResourceLocation advancementssearch$ba$getHighlightedAdvancementId();

    HighlightType advancementssearch$ba$getHighlightType();

    boolean advancementssearch$ba$isHighlightAtInvisibleState();

    void advancementssearch$ba$stopHighlight();

    void advancementssearch$ba$search(
            String query,
            SearchByType searchByType,
            boolean autoHighlightSingle,
            HighlightType highlightType
    );

    void advancementssearch$ba$highlightAdvancement(ResourceLocation advancementId, HighlightType highlightType);

    void advancementssearch$ba$tick();

    boolean advancementssearch$ba$charTyped(char chr, int modifiers);

    void advancementssearch$ba$resize(int width, int height);

    void advancementssearch$ba$onMouseReleased(double mouseX, double mouseY, int button);
}
