package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Title row: item name on the left (offset for the item icon), star symbols on the right.
 * Stars are shown when {@code stars > 0} (dungeon upgrade level).
 *
 * <p>The title is offset rightward by {@link #ICON_SPACE} pixels to leave
 * room for the item icon rendered by the tooltip renderer.
 */
public record TitleLine(String name, int nameColor, int stars, int starColor)
        implements DrawEntry {

    private static final int LINE_H = TooltipLayoutBuilder.LINE_H;
    private static final char STAR_CHAR = TooltipLayoutBuilder.STAR_CHAR;
    private static final int MAX_STARS = 10;

    /** Horizontal space reserved for the item icon (16px icon + 2px gap). */
    private static final int ICON_SPACE = 18;

    @Override public int height() { return Math.max(LINE_H, 16); }

    @Override
    public int naturalWidth(Font f) {
        int w = ICON_SPACE + f.width(name);
        if (stars > 0) w += 3 + f.width(buildStars());
        return w;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        // Offset text to the right of the item icon space
        int textX = x + ICON_SPACE;
        int textY = y + (16 - LINE_H) / 2; // Vertically centre with 16px icon

        g.drawString(f, name, textX, textY, nameColor, false);

        if (stars > 0) {
            String starStr = buildStars();
            int starX = x + totalWidth - f.width(starStr);
            g.drawString(f, starStr, starX, textY, starColor, false);
        }
    }

    private String buildStars() {
        return String.valueOf(STAR_CHAR).repeat(Math.max(0, Math.min(stars, MAX_STARS)));
    }
}