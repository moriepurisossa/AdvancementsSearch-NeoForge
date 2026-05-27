package com.ayane.advancementssearch.injection.mixin;

import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ParentElementMixin {

    @Inject(
            method = "mouseReleased",
            at = @At(value = "HEAD")
    )
    private void onMouseReleasedInAdvancementsScreen(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (this instanceof AdvancementsScreenExtension ext) {
            ext.advancementssearch$onMouseReleased(mouseX, mouseY, button);
        } else if (this instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$onMouseReleased(mouseX, mouseY, button);
        }
    }

    @Inject(
            method = "charTyped",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onCharTypedInAdvancementsScreen(
            char codePoint,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (this instanceof AdvancementsScreenExtension ext
                && ext.advancementssearch$charTyped(codePoint, modifiers)
        ) {
            cir.setReturnValue(true);
            return;
        }
        if (this instanceof BetterAdvancementsScreenExtension baExt
                && baExt.advancementssearch$ba$charTyped(codePoint, modifiers)
        ) {
            cir.setReturnValue(true);
        }
    }
}
