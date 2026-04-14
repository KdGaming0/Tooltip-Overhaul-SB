package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Two-column stat row: {@code "label ·····  value"} with middle-dot fill.
 *
 * <p>Stat values are color-coded: green for positive, red for negative.
 * The dot leader uses middle dots (·) for a lighter, cleaner look.
 */
public record StatLine(String label, String value, int labelColor, int valueColor)
        implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;
    private static final int DOT_COLOR = 0xFF444444;
    private static final char DOT_CHAR = '\u00B7'; // middle dot ·

    private static final int POSITIVE_COLOR = 0xFF55FF55; // green
    private static final int NEGATIVE_COLOR = 0xFFFF5555; // red

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

        // Color-code the value based on sign
        int actualValueColor = resolveValueColor(value, valueColor);
        g.drawString(f, value, x + totalWidth - valueW, y, actualValueColor, false);
    }

    /**
     * Determines value color based on the leading sign character.
     * Positive values (+) render green, negative values (-) render red.
     * Falls back to the default valueColor for neutral/unsigned values.
     */
    private static int resolveValueColor(String value, int defaultColor) {
        String trimmed = value.trim();
        if (trimmed.startsWith("+")) return POSITIVE_COLOR;
        if (trimmed.startsWith("-")) return NEGATIVE_COLOR;
        return defaultColor;
    }

    /**
     * Builds a middle-dot fill string using arithmetic instead of measuring
     * an accumulating string. O(n) instead of O(n²).
     */
    private static String buildDots(Font f, int targetPixels) {
        int dotW = f.width(String.valueOf(DOT_CHAR));
        if (dotW <= 0) return " ";
        int count = targetPixels / dotW;
        return String.valueOf(DOT_CHAR).repeat(Math.max(0, count));
    }
}