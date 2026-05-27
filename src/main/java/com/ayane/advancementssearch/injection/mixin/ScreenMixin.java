package com.ayane.advancementssearch.injection.mixin;

import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
            method = "tick",
            at = @At(value = "HEAD")
    )
    private void tickInAdvancementsScreen(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AdvancementsScreenExtension ext) {
            ext.advancementssearch$tick();
        } else if (screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$tick();
        }
    }

    @Inject(
            method = "resize",
            at = @At(value = "HEAD")
    )
    private void resizeInAdvancementsScreen(Minecraft minecraft, int width, int height, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AdvancementsScreenExtension ext) {
            ext.advancementssearch$resize(width, height);
        } else if (screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$resize(width, height);
        }
    }
}
