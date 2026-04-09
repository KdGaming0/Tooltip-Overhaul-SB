package com.kd_gaming1.tooltipoverhaulsb.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code GuiGraphics.deferredTooltip} so the intercept mixin can
 * replace the deferred render with a custom one without invoking any of the
 * overloaded {@code setTooltipForNextFrame} methods (which would re-trigger
 * tooltip logic or require access to internal argument types).
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {

    /**
     * Writes directly to the private {@code deferredTooltip} field.
     * Pass {@code null} to clear a previously queued tooltip.
     */
    @Accessor("deferredTooltip")
    void tosb_setDeferredTooltip(@Nullable Runnable tooltip);
}