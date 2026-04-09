package com.kd_gaming1.tooltipoverhaulsb.mixin;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import com.kd_gaming1.tooltipoverhaulsb.data.SkyBlockItemData;
import com.kd_gaming1.tooltipoverhaulsb.hook.TooltipInterceptHook;
import com.kd_gaming1.tooltipoverhaulsb.render.SkyBlockTooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Intercepts tooltip rendering using a two-phase approach.
 */
@Mixin(GuiGraphics.class)
public abstract class TooltipInterceptMixin {

    @Unique
    private static final Logger TOSB_LOGGER = LoggerFactory.getLogger("TooltipOverhaulSB/Mixin");

    @Unique
    private static boolean TOSB_FIRST_CALL_LOGGED = false;

    @Inject(
            method = "setTooltipForNextFrameInternal",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tosb_interceptTooltipInternal(
            Font font, List<ClientTooltipComponent> lines,
            int xo, int yo, ClientTooltipPositioner positioner,
            @Nullable Identifier style, boolean replaceExisting,
            CallbackInfo ci) {

        if (!TOSB_FIRST_CALL_LOGGED) {
            TOSB_LOGGER.info("[TOSB] Mixin active — setTooltipForNextFrameInternal reached.");
            TOSB_FIRST_CALL_LOGGED = true;
        }

        if (!TooltipOverhaulConfig.enabled) return;

        TooltipInterceptHook.CapturedTooltip captured = TooltipInterceptHook.consumeCaptured();
        if (captured == null) return;

        SkyBlockItemData data  = captured.data();
        ItemStack        stack = captured.stack();

        GuiGraphics guiGraphics = (GuiGraphics) (Object) this;

        ((GuiGraphicsAccessor) guiGraphics).tosb_setDeferredTooltip(
                () -> SkyBlockTooltipRenderer.render(
                        guiGraphics, font, data, stack, xo, yo)
        );

        ci.cancel();
    }
}