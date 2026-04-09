package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pet XP progress bar — shown near the top of a pet tooltip, before stats.
 *
 * <pre>
 *   NEXT LEVEL      (or MAX LEVEL)
 *   [████████████░░░░░░░░░░]
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

    private static final int LINE_H     = TooltipLayoutBuilder.LINE_H;
    private static final int BAR_HEIGHT = 4;

    private static final int BG_COLOR   = 0xFF333333;
    private static final int FILL_COLOR = 0xFF55FF55;
    private static final int MAX_COLOR  = 0xFF55FF55;
    private static final int LABEL_CLR  = 0xFFAAAAAA;
    private static final int XP_COLOR   = 0xFF888888;

    /** Header text + bar + gap + XP count text. */
    @Override
    public int height() {
        return LINE_H + BAR_HEIGHT + 2 + LINE_H + 2;
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

        // Header: "NEXT LEVEL" centred
        String header = "NEXT LEVEL";
        g.drawString(f, header, x + (totalWidth - f.width(header)) / 2, y, LABEL_CLR, false);

        // Progress bar
        int barY    = y + LINE_H + 1;
        int filledW = (int) Math.round(totalWidth * Math.min(Math.max(progress, 0.0), 1.0));

        g.fill(x, barY, x + totalWidth, barY + BAR_HEIGHT, BG_COLOR);
        if (filledW > 0) {
            g.fill(x, barY, x + filledW, barY + BAR_HEIGHT, FILL_COLOR);
        }

        // XP count: "current / next XP"
        String xpText;
        if (expForNext > 0) {
            xpText = TooltipLayoutBuilder.formatExp(expInLevel) + " / "
                    + TooltipLayoutBuilder.formatExp(expForNext) + " XP";
        } else {
            xpText = TooltipLayoutBuilder.formatExp(totalExp) + " XP";
        }
        int countY = barY + BAR_HEIGHT + 2;
        g.drawString(f, xpText, x + (totalWidth - f.width(xpText)) / 2, countY, XP_COLOR, false);
    }

    private void drawMaxLevel(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        String maxLabel = "MAX LEVEL";
        g.drawString(f, maxLabel, x + (totalWidth - f.width(maxLabel)) / 2, y, MAX_COLOR, false);

        String xpStr = TooltipLayoutBuilder.formatExp(totalExp) + " XP total";
        g.drawString(f, xpStr, x + (totalWidth - f.width(xpStr)) / 2, y + LINE_H + 2, XP_COLOR, false);
    }
}