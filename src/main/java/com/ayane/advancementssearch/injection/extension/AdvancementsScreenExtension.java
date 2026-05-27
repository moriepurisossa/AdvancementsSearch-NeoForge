package com.ayane.advancementssearch.injection.extension;

import com.ayane.advancementssearch.HighlightType;
import com.ayane.advancementssearch.SearchByType;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.ResourceLocation;

public interface AdvancementsScreenExtension {

    void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget);

    boolean advancementssearch$isSearchActive();

    int advancementssearch$getTreeWidth();

    int advancementssearch$getTreeHeight();

    ResourceLocation advancementssearch$getHighlightedAdvancementId();

    HighlightType advancementssearch$getHighlightType();

    boolean advancementssearch$isHighlightAtInvisibleState();

    void advancementssearch$stopHighlight();

    void advancementssearch$search(
            String query,
            SearchByType searchByType,
            boolean autoHighlightSingle,
            HighlightType highlightType
    );

    void advancementssearch$highlightAdvancement(ResourceLocation advancementId, HighlightType highlightType);

    void advancementssearch$tick();

    boolean advancementssearch$charTyped(char chr, int modifiers);

    void advancementssearch$resize(int width, int height);

    void advancementssearch$onMouseReleased(double mouseX, double mouseY, int button);
}
