package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Decorative separator: a thin horizontal line with diamond (◇) accents at each end.
 * Used to bracket the gem slot row.
 */
public record DecorativeSeparator(int color) implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;
    private static final String DIAMOND = "◇";

    @Override public int height() { return 7; } // 3px gap + 1px line + 3px gap
    @Override public int naturalWidth(Font f) { return 40; } // always fills available width

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        int midY = y + 3;

        // Horizontal line spanning full width
        g.fill(x, midY, x + totalWidth, midY + 1, color);

        // Diamond symbols at each end (erase line behind them first)
        int dw = f.width(DIAMOND);
        int textY = midY - LINE_H / 2;

        // Clear line behind left diamond
        g.fill(x, midY, x + dw + 1, midY + 1, 0x00000000);
        g.drawString(f, DIAMOND, x, textY, color, false);

        // Clear line behind right diamond
        g.fill(x + totalWidth - dw - 1, midY, x + totalWidth, midY + 1, 0x00000000);
        g.drawString(f, DIAMOND, x + totalWidth - dw, textY, color, false);
    }
}