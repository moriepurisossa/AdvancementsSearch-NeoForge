package com.ayane.advancementssearch.injection.mixin.compat.betteradvancements;

import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Parallel of {@link com.ayane.advancementssearch.injection.mixin.AdvancementTabMixin}
 * targeting Better Advancements' tab class.
 *
 * <p>4-I-2b: just the background-cancel — the two saveFocused/resetFocused hooks
 * are deferred to 4-I-2c when click-to-jump behaviour is implemented.
 */
@Mixin(BetterAdvancementTab.class)
public abstract class BetterAdvancementTabMixin {

    @Shadow
    @Final
    private BetterAdvancementsScreen screen;

    /**
     * Cancel the tiled background blit in {@code drawContents} when the owning screen
     * is in search mode. BA's drawContents reads {@code display.getBackground().orElse(
     * INTENTIONAL_MISSING_TEXTURE)}, and our synthetic searchRoot deliberately passes
     * {@code Optional.empty()} — without this wrap, the missing-texture pink/black
     * checker tiles the entire content area while results are displayed.
     *
     * <p>Two blit calls exist inside drawContents (full row and partial bottom row);
     * @WrapOperation catches both since they share the same descriptor.
     */
    @WrapOperation(
            method = "drawContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
            )
    )
    private void advancementssearch$ba$cancelBackgroundRenderInSearch(
            GuiGraphics guiGraphics,
            ResourceLocation sprite,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            Operation<Void> original
    ) {
        if (screen instanceof BetterAdvancementsScreenExtension baExt
                && baExt.advancementssearch$ba$isSearchActive()) {
            return;
        }
        original.call(guiGraphics, sprite, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    /**
     * Capture the widget currently being hovered as the screen's focused widget.
     * Mirror of vanilla {@code AdvancementTabMixin.saveFocusedAdvancementWidget}, but
     * targeting BA's {@code drawToolTips} (capital T, 7 args) which calls
     * {@code BetterAdvancementWidget.drawHover(GuiGraphics, II, F, II)} inside its
     * hover loop. {@code @Local(ordinal=0)} captures the loop's widget variable.
     */
    @Inject(
            method = "drawToolTips",
            at = @At(
                    value = "INVOKE",
                    target = "Lbetteradvancements/common/gui/BetterAdvancementWidget;drawHover(Lnet/minecraft/client/gui/GuiGraphics;IIFII)V",
                    shift = At.Shift.AFTER
            )
    )
    public void advancementssearch$ba$saveFocusedAdvancementWidget(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int width,
            int height,
            CallbackInfo ci,
            @Local(ordinal = 0) BetterAdvancementWidget widget
    ) {
        if (screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$setFocusedAdvancementWidget(widget);
        }
    }

    /**
     * Clear the focused widget at the start of every drawToolTips frame so stale
     * "last hovered" state doesn't leak across frames when the cursor leaves all widgets.
     * If a widget is hovered this frame, the saveFocusedAdvancementWidget @Inject above
     * overwrites it later in the same drawToolTips call.
     */
    @Inject(
            method = "drawToolTips",
            at = @At(value = "HEAD")
    )
    public void advancementssearch$ba$resetFocusedAdvancementWidget(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int width,
            int height,
            CallbackInfo ci
    ) {
        if (screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$setFocusedAdvancementWidget(null);
        }
    }
}
