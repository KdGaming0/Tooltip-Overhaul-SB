package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pet XP progress bar — shown near the top of a pet tooltip, before stats.
 *
 * <p>Features:
 * <ul>
 *   <li>8px tall bar with 1px border frame</li>
 *   <li>Gradient fill (bright green top → darker green bottom)</li>
 *   <li>Centered "NEXT LEVEL" or "MAX LEVEL" header</li>
 *   <li>XP count text below the bar</li>
 * </ul>
 *
 * <pre>
 *   NEXT LEVEL
 *   ┌──────────────────────────────┐
 *   │████████████░░░░░░░░░░░░░░░░░│
 *   └──────────────────────────────┘
 *   123,456 / 456,789 XP
 * </pre>
 */
public record PetXpBar(
        int     nextLevel,
        double  progress,
        double  expInLevel,
        double  expForNext,
        boolean isMaxLevel,
        double  totalExp
) implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;

    // Bar dimensions
    private static final int BAR_HEIGHT    = 8;
    private static final int BORDER_WIDTH  = 1;

    // Colors
    private static final int BORDER_COLOR     = 0xFF444444;
    private static final int BG_COLOR         = 0xFF1A1A1A;
    private static final int FILL_TOP_COLOR   = 0xFF55FF55;  // bright green
    private static final int FILL_BOT_COLOR   = 0xFF22AA22;  // darker green
    private static final int MAX_TOP_COLOR    = 0xFF55FF55;
    private static final int MAX_BOT_COLOR    = 0xFF22AA22;
    private static final int LABEL_COLOR      = 0xFFAAAAAA;
    private static final int MAX_LABEL_COLOR  = 0xFF55FF55;
    private static final int XP_COLOR         = 0xFF888888;

    /** Header text + gap + border + bar + border + gap + XP count text. */
    @Override
    public int height() {
        return LINE_H                          // header
                + 2                            // gap
                + BORDER_WIDTH                 // top border
                + BAR_HEIGHT                   // fill area
                + BORDER_WIDTH                 // bottom border
                + 2                            // gap
                + LINE_H                       // XP text
                + 2;                           // bottom margin
    }

    @Override
    public int naturalWidth(Font f) {
        return 140;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        if (isMaxLevel) {
            drawMaxLevel(g, f, x, y, totalWidth);
            return;
        }

        int curY = y;

        // Header: "NEXT LEVEL" centred
        String header = "NEXT LEVEL";
        g.drawString(f, header, x + (totalWidth - f.width(header)) / 2, curY, LABEL_COLOR, false);
        curY += LINE_H + 2;

        // Bar frame
        int barLeft   = x;
        int barRight  = x + totalWidth;
        int barTop    = curY;
        int barBottom = barTop + BORDER_WIDTH * 2 + BAR_HEIGHT;

        // Border (1px rectangle)
        drawBorder(g, barLeft, barTop, barRight, barBottom, BORDER_COLOR);

        // Background fill inside border
        int innerLeft   = barLeft + BORDER_WIDTH;
        int innerRight  = barRight - BORDER_WIDTH;
        int innerTop    = barTop + BORDER_WIDTH;
        int innerBottom = barBottom - BORDER_WIDTH;
        g.fill(innerLeft, innerTop, innerRight, innerBottom, BG_COLOR);

        // Gradient progress fill
        double clampedProgress = Math.min(Math.max(progress, 0.0), 1.0);
        int fillWidth = (int) Math.round((innerRight - innerLeft) * clampedProgress);
        if (fillWidth > 0) {
            g.fillGradient(innerLeft, innerTop,
                    innerLeft + fillWidth, innerBottom,
                    FILL_TOP_COLOR, FILL_BOT_COLOR);
        }

        curY = barBottom + 2;

        // XP count: "current / next XP"
        String xpText;
        if (expForNext > 0) {
            xpText = TooltipLayoutBuilder.formatExp(expInLevel) + " / "
                    + TooltipLayoutBuilder.formatExp(expForNext) + " XP";
        } else {
            xpText = TooltipLayoutBuilder.formatExp(totalExp) + " XP";
        }
        g.drawString(f, xpText, x + (totalWidth - f.width(xpText)) / 2, curY, XP_COLOR, false);
    }

    private void drawMaxLevel(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        int curY = y;

        // "MAX LEVEL" in green
        String maxLabel = "MAX LEVEL";
        g.drawString(f, maxLabel, x + (totalWidth - f.width(maxLabel)) / 2, curY, MAX_LABEL_COLOR, false);
        curY += LINE_H + 2;

        // Full gradient bar
        int barLeft   = x;
        int barRight  = x + totalWidth;
        int barTop    = curY;
        int barBottom = barTop + BORDER_WIDTH * 2 + BAR_HEIGHT;

        drawBorder(g, barLeft, barTop, barRight, barBottom, BORDER_COLOR);

        int innerLeft   = barLeft + BORDER_WIDTH;
        int innerRight  = barRight - BORDER_WIDTH;
        int innerTop    = barTop + BORDER_WIDTH;
        int innerBottom = barBottom - BORDER_WIDTH;

        g.fillGradient(innerLeft, innerTop, innerRight, innerBottom,
                MAX_TOP_COLOR, MAX_BOT_COLOR);

        curY = barBottom + 2;

        // Total XP
        String xpStr = TooltipLayoutBuilder.formatExp(totalExp) + " XP total";
        g.drawString(f, xpStr, x + (totalWidth - f.width(xpStr)) / 2, curY, XP_COLOR, false);
    }

    /**
     * Draws a 1px border rectangle (outline only, no fill).
     */
    private static void drawBorder(GuiGraphics g, int left, int top, int right, int bottom, int color) {
        g.fill(left,      top,        right,     top + 1,       color); // top
        g.fill(left,      bottom - 1, right,     bottom,        color); // bottom
        g.fill(left,      top,        left + 1,  bottom,        color); // left
        g.fill(right - 1, top,        right,     bottom,        color); // right
    }
}