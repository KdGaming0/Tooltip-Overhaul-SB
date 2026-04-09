package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Empty vertical gap (spacer). */
public record GapLine(int pixels) implements DrawEntry {
    @Override public int height() { return pixels; }
    @Override public int naturalWidth(Font f) { return 0; }
    @Override public void draw(GuiGraphics g, Font f, int x, int y, int w) {}
}