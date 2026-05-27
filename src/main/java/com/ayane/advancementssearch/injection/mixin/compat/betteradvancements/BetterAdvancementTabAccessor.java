package com.ayane.advancementssearch.injection.mixin.compat.betteradvancements;

import java.util.Map;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin accessor interface exposing BA's private/protected tab internals.
 *
 * <p>NeoForge AccessTransformers only process Minecraft classes — external mod
 * jars (provided via {@code compileOnly}) stay un-modified at compile time.
 * The Mixin transformer DOES patch BA at load time, so @Accessor / @Invoker
 * here generate the needed bridge methods AND make the cast type-safe at compile.
 *
 * <p>Usage: {@code ((BetterAdvancementTabAccessor) tab).advancementssearch$ba$setScrollX(0);}
 */
@Mixin(BetterAdvancementTab.class)
public interface BetterAdvancementTabAccessor {

    @Accessor("scrollX")
    int advancementssearch$ba$getScrollX();

    @Accessor("scrollX")
    void advancementssearch$ba$setScrollX(int value);

    @Accessor("scrollY")
    int advancementssearch$ba$getScrollY();

    @Accessor("scrollY")
    void advancementssearch$ba$setScrollY(int value);

    @Accessor("minX")
    void advancementssearch$ba$setMinX(int value);

    @Accessor("minY")
    void advancementssearch$ba$setMinY(int value);

    @Accessor("maxX")
    void advancementssearch$ba$setMaxX(int value);

    @Accessor("maxY")
    void advancementssearch$ba$setMaxY(int value);

    @Accessor("centered")
    void advancementssearch$ba$setCentered(boolean value);

    @Accessor("widgets")
    Map<AdvancementHolder, BetterAdvancementWidget> advancementssearch$ba$getWidgets();

    @Accessor("root")
    BetterAdvancementWidget advancementssearch$ba$getRoot();

    /** Invoker for BA's private {@code addWidget(widget, holder)} method. */
    @Invoker("addWidget")
    void advancementssearch$ba$addWidget(BetterAdvancementWidget widget, AdvancementHolder holder);
}
