package com.ayane.advancementssearch.injection.mixin;

import com.ayane.advancementssearch.AdvancementsSearchMod;
import com.ayane.advancementssearch.HighlightType;
import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow
    @Final
    public AdvancementTab tab;

    @Shadow
    @Final
    public AdvancementNode advancementNode;

    @Inject(
            method = "drawConnectivity",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void cancelLinesRenderInSearch(GuiGraphics guiGraphics, int x, int y, boolean dropShadow, CallbackInfo ci) {
        if (AdvancementsSearchMod.isSearch(tab.getRootNode())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "drawHover",
            at = @At(value = "HEAD")
    )
    public void checkHighlight(
            GuiGraphics guiGraphics,
            int x,
            int y,
            float fade,
            int width,
            int height,
            CallbackInfo ci
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            ResourceLocation advancementId = advancementsScreenExtension.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                    advancementId != null &&
                    advancementId.equals(advancementNode.holder().id())
            ) {
                advancementsScreenExtension.advancementssearch$stopHighlight();
            }
        }
    }

    @ModifyReturnValue(
            method = "isMouseOver",
            at = @At(value = "TAIL")
    )
    public boolean cancelTooltipRender(boolean original) {
        if (original && tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            ResourceLocation advancementId = advancementsScreenExtension.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) && advancementId != null) {
                return advancementId.equals(advancementNode.holder().id());
            }
        }
        return original;
    }

    /**
     * Polish: Blink animation by skipping the frame render of the highlighted widget
     * during "invisible" phases. (HighlightType.WIDGET mode)
     */
    @WrapOperation(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void highlightWidget(
            GuiGraphics guiGraphics,
            ResourceLocation sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                    advancementId != null &&
                    advancementId.equals(advancementNode.holder().id()) &&
                    ext.advancementssearch$getHighlightType() == HighlightType.WIDGET &&
                    ext.advancementssearch$isHighlightAtInvisibleState()
            ) {
                return; // skip frame draw to create blink effect
            }
        }
        original.call(guiGraphics, sprite, x, y, width, height);
    }

    /**
     * Polish: Invert OBTAINED <-> UNOBTAINED status of the highlighted widget
     * during "invisible" phases. (HighlightType.OBTAINED_STATUS mode)
     */
    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidgetType;frameSprite(Lnet/minecraft/advancements/AdvancementType;)Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private ResourceLocation highlightObtainedStatus(AdvancementWidgetType type, AdvancementType advancementType) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension ext) {
            ResourceLocation advancementId = ext.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                    advancementId != null &&
                    advancementId.equals(advancementNode.holder().id()) &&
                    ext.advancementssearch$getHighlightType() == HighlightType.OBTAINED_STATUS &&
                    ext.advancementssearch$isHighlightAtInvisibleState()
            ) {
                type = type == AdvancementWidgetType.OBTAINED
                        ? AdvancementWidgetType.UNOBTAINED
                        : AdvancementWidgetType.OBTAINED;
            }
        }
        return type.frameSprite(advancementType);
    }
}
