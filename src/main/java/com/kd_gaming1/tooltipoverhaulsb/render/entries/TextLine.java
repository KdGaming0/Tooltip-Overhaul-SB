package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Plain left-aligned text line. */
public record TextLine(String text, int color) implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;

    @Override public int height() { return LINE_H; }
    @Override public int naturalWidth(Font f) { return f.width(text); }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        g.drawString(f, text, x, y, color, false);
    }
}