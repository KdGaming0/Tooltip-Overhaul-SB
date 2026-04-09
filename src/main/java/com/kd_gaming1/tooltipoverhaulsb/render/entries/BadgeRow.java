package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Renders one or more coloured pill badges side by side.
 * Used for the rarity badge, type badge, and extra attribute badges (HP count, BP, etc.).
 */
public final class BadgeRow implements DrawEntry {

    private static final int PAD_X  = TooltipLayoutBuilder.BADGE_PAD_X;
    private static final int PAD_Y  = TooltipLayoutBuilder.BADGE_PAD_Y;
    private static final int HEIGHT = TooltipLayoutBuilder.BADGE_HEIGHT;
    private static final int GAP    = TooltipLayoutBuilder.BADGE_GAP;
    private static final int OUTLINE_COLOR = 0x55FFFFFF;

    /** A single badge pill with text and colours. */
    public record Badge(String label, int bgColor, int textColor) {}

    private final List<Badge> badges;

    public BadgeRow(List<Badge> badges) {
        this.badges = List.copyOf(badges);
    }

    @Override
    public int height() {
        return HEIGHT + 2; // +2 bottom gap
    }

    @Override
    public int naturalWidth(Font f) {
        int w = 0;
        for (int i = 0; i < badges.size(); i++) {
            if (i > 0) w += GAP;
            w += f.width(badges.get(i).label()) + PAD_X * 2;
        }
        return w;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        int cx = x;
        for (Badge badge : badges) {
            int labelW = f.width(badge.label());
            int bw = labelW + PAD_X * 2;

            // Background fill
            g.fill(cx, y, cx + bw, y + HEIGHT, badge.bgColor());

            // 1px outline (top, bottom, left, right)
            g.fill(cx,        y,            cx + bw, y + 1,          OUTLINE_COLOR);
            g.fill(cx,        y + HEIGHT - 1, cx + bw, y + HEIGHT,  OUTLINE_COLOR);
            g.fill(cx,        y,            cx + 1,    y + HEIGHT,   OUTLINE_COLOR);
            g.fill(cx + bw - 1, y,          cx + bw,   y + HEIGHT,  OUTLINE_COLOR);

            // Label text
            g.drawString(f, badge.label(), cx + PAD_X, y + PAD_Y, badge.textColor(), false);

            cx += bw + GAP;
        }
    }
}