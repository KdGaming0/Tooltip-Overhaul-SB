package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Title row: item name on the left, star symbols on the right.
 * Stars are shown when {@code stars > 0} (dungeon upgrade level).
 */
public record TitleLine(String name, int nameColor, int stars, int starColor)
        implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;
    private static final char STAR_CHAR = TooltipLayoutBuilder.STAR_CHAR;
    private static final int MAX_STARS = 10;

    @Override public int height() { return LINE_H; }

    @Override
    public int naturalWidth(Font f) {
        int w = f.width(name);
        if (stars > 0) w += 3 + f.width(buildStars());
        return w;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        g.drawString(f, name, x, y, nameColor, false);
        if (stars > 0) {
            String starStr = buildStars();
            int starX = x + totalWidth - f.width(starStr);
            g.drawString(f, starStr, starX, y, starColor, false);
        }
    }

    private String buildStars() {
        return String.valueOf(STAR_CHAR).repeat(Math.max(0, Math.min(stars, MAX_STARS)));
    }
}