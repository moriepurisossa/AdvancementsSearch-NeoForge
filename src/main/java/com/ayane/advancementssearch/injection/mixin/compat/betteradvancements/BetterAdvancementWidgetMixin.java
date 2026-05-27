package com.ayane.advancementssearch.injection.mixin.compat.betteradvancements;

import com.ayane.advancementssearch.AdvancementsSearchMod;
import com.ayane.advancementssearch.HighlightType;
import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Parallel of {@link com.ayane.advancementssearch.injection.mixin.AdvancementWidgetMixin}
 * targeting Better Advancements' widget class.
 *
 * <p>4-I-2d: blink animation + connection-line hide + tooltip suppression.
 * BA's {@code draw} method uses the SAME vanilla APIs
 * ({@code GuiGraphics.blitSprite} + {@code AdvancementWidgetType.frameSprite}),
 * so the vanilla mixin's blink injections port directly with identical descriptors.
 */
@Mixin(BetterAdvancementWidget.class)
public abstract class BetterAdvancementWidgetMixin {

    // BA's field is named "betterAdvancementTabGui", not "tab" like vanilla.
    @Shadow
    @Final
    private BetterAdvancementTab betterAdvancementTabGui;

    @Shadow
    @Final
    private AdvancementNode advancementNode;

    /**
     * In search mode, suppress connection lines between widgets (the searchTab is a flat
     * grid, no parent-child links). Same pattern as vanilla AdvancementWidgetMixin.
     */
    @Inject(
            method = "drawConnectivity",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void advancementssearch$ba$cancelLinesRenderInSearch(
            GuiGraphics guiGraphics,
            int x,
            int y,
            boolean dropShadow,
            CallbackInfo ci
    ) {
        if (AdvancementsSearchMod.isSearch(betterAdvancementTabGui.getRootNode())) {
            ci.cancel();
        }
    }

    /**
     * Once the highlighted widget is hovered and its tooltip is being drawn, stop the
     * highlight blink (user has clearly found it). Skipped while in search mode.
     */
    @Inject(
            method = "drawHover",
            at = @At(value = "HEAD")
    )
    public void advancementssearch$ba$checkHighlight(
            GuiGraphics guiGraphics,
            int x,
            int y,
            float fade,
            int width,
            int height,
            CallbackInfo ci
    ) {
        if (betterAdvancementTabGui.getScreen() instanceof BetterAdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$ba$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(betterAdvancementTabGui.getRootNode())
                    && advancementId != null
                    && advancementId.equals(advancementNode.holder().id())
            ) {
                ext.advancementssearch$ba$stopHighlight();
            }
        }
    }

    /**
     * During highlight, suppress tooltips on widgets OTHER than the highlighted one so
     * the user's attention isn't split. Allow tooltip on the highlighted widget itself.
     */
    @ModifyReturnValue(
            method = "isMouseOver",
            at = @At(value = "TAIL")
    )
    public boolean advancementssearch$ba$cancelTooltipRender(boolean original) {
        if (original && betterAdvancementTabGui.getScreen() instanceof BetterAdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$ba$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(betterAdvancementTabGui.getRootNode())
                    && advancementId != null
            ) {
                return advancementId.equals(advancementNode.holder().id());
            }
        }
        return original;
    }

    /**
     * Polish: Blink animation by skipping the frame render of the highlighted widget
     * during "invisible" phases. (HighlightType.WIDGET mode)
     *
     * <p>BA's {@code draw} calls {@code GuiGraphics.blitSprite(RL, IIII)V} for the
     * frame — same descriptor as vanilla, so this @WrapOperation is a direct port.
     */
    @WrapOperation(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void advancementssearch$ba$highlightWidget(
            GuiGraphics guiGraphics,
            ResourceLocation sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (betterAdvancementTabGui.getScreen() instanceof BetterAdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$ba$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(betterAdvancementTabGui.getRootNode())
                    && advancementId != null
                    && advancementId.equals(advancementNode.holder().id())
                    && ext.advancementssearch$ba$getHighlightType() == HighlightType.WIDGET
                    && ext.advancementssearch$ba$isHighlightAtInvisibleState()
            ) {
                return; // skip frame draw to create blink effect
            }
        }
        original.call(guiGraphics, sprite, x, y, width, height);
    }

    /**
     * Polish: Invert OBTAINED <-> UNOBTAINED status of the highlighted widget
     * during "invisible" phases. (HighlightType.OBTAINED_STATUS mode)
     *
     * <p>BA's {@code draw} calls {@code AdvancementWidgetType.frameSprite(AdvancementType)}
     * — same descriptor as vanilla, so this @Redirect is a direct port.
     */
    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidgetType;frameSprite(Lnet/minecraft/advancements/AdvancementType;)Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private ResourceLocation advancementssearch$ba$highlightObtainedStatus(
            AdvancementWidgetType type,
            AdvancementType advancementType
    ) {
        if (betterAdvancementTabGui.getScreen() instanceof BetterAdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$ba$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(betterAdvancementTabGui.getRootNode())
                    && advancementId != null
                    && advancementId.equals(advancementNode.holder().id())
                    && ext.advancementssearch$ba$getHighlightType() == HighlightType.OBTAINED_STATUS
                    && ext.advancementssearch$ba$isHighlightAtInvisibleState()
            ) {
                type = type == AdvancementWidgetType.OBTAINED
                        ? AdvancementWidgetType.UNOBTAINED
                        : AdvancementWidgetType.OBTAINED;
            }
        }
        return type.frameSprite(advancementType);
    }
}
