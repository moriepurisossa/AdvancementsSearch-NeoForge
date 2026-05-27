package com.ayane.advancementssearch.injection.mixin;

import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private AdvancementsScreen screen;

    @Inject(
            method = "drawTooltips",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawHover(Lnet/minecraft/client/gui/GuiGraphics;IIFII)V",
                    shift = At.Shift.AFTER
            )
    )
    public void saveFocusedAdvancementWidget(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int width,
            int height,
            CallbackInfo ci,
            @Local(ordinal = 0) AdvancementWidget advancementWidget
    ) {
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancementssearch$setFocusedAdvancementWidget(advancementWidget);
        }
    }

    @Inject(
            method = "drawTooltips",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;clamp(FFF)F",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            )
    )
    public void resetFocusedAdvancementWidget(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int width,
            int height,
            CallbackInfo ci
    ) {
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancementssearch$setFocusedAdvancementWidget(null);
        }
    }

    @WrapOperation(
            method = "drawContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
            )
    )
    private void cancelBackgroundRenderInSearch(
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
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension &&
                !advancementsScreenExtension.advancementssearch$isSearchActive()
        ) {
            original.call(guiGraphics, sprite, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }
}
