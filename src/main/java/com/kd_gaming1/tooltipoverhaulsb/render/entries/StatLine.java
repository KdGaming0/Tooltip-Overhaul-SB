package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Two-column stat row: {@code "label ·····  value"} with dot fill.
 *
 * <p>The dot string is pre-built using arithmetic on the single-dot width
 * rather than measuring an accumulating StringBuilder each iteration.
 */
public record StatLine(String label, String value, int labelColor, int valueColor)
        implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;
    private static final int DOT_COLOR = 0xFF555555;

    @Override public int height() { return LINE_H; }

    @Override
    public int naturalWidth(Font f) {
        // Minimum: label + small dot gap + value
        return f.width(label) + 8 + f.width(value);
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        int labelW = f.width(label);
        int valueW = f.width(value);
        int dotSpace = totalWidth - labelW - valueW;

        g.drawString(f, label, x, y, labelColor, false);

        if (dotSpace > 0) {
            String dots = buildDots(f, dotSpace);
            g.drawString(f, dots, x + labelW, y, DOT_COLOR, false);
        }

        g.drawString(f, value, x + totalWidth - valueW, y, valueColor, false);
    }

    /**
     * Builds a dot-fill string using arithmetic instead of measuring
     * an accumulating string. O(n) instead of O(n²).
     */
    private static String buildDots(Font f, int targetPixels) {
        int dotW = f.width(".");
        if (dotW <= 0) return " ";
        int count = targetPixels / dotW;
        return ".".repeat(Math.max(0, count));
    }
}