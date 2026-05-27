package com.ayane.advancementssearch.injection.mixin.compat.betteradvancements;

import java.util.List;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor interface for BA's BetterAdvancementWidget private fields.
 * See {@link BetterAdvancementTabAccessor} for the rationale.
 */
@Mixin(BetterAdvancementWidget.class)
public interface BetterAdvancementWidgetAccessor {

    @Accessor("parent")
    void advancementssearch$ba$setParent(BetterAdvancementWidget parent);

    @Accessor("children")
    List<BetterAdvancementWidget> advancementssearch$ba$getChildren();

    /** Owning tab — note BA's field is {@code betterAdvancementTabGui}, not {@code tab}. */
    @Accessor("betterAdvancementTabGui")
    BetterAdvancementTab advancementssearch$ba$getTab();
}
