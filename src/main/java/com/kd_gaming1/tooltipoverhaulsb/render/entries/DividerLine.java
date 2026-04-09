package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Thin 1px horizontal rule with 2px padding above and below. */
public record DividerLine(int color) implements DrawEntry {
    @Override public int height() { return 5; }
    @Override public int naturalWidth(Font f) { return 0; }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        g.fill(x, y + 2, x + totalWidth, y + 3, color);
    }
}