package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.data.AbilityEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pet ability entry: renders a centred decorative header
 * {@code "─── AbilityName ───"} followed by description lines
 * and optional mana/cooldown metadata.
 */
public final class PetAbilityEntry implements DrawEntry {

    private static final int LINE_H     = TooltipLayoutBuilder.LINE_H;
    private static final int DESC_COLOR = 0xFFAAAAAA;
    private static final int META_COLOR = 0xFF888888;
    private static final int TOP_GAP    = 3;

    private final AbilityEntry ability;
    private final int headerColor;

    public PetAbilityEntry(AbilityEntry ability, int headerColor) {
        this.ability     = ability;
        this.headerColor = headerColor;
    }

    @Override
    public int height() {
        // top gap + header line + description lines + optional meta line
        return TOP_GAP + LINE_H
                + ability.descriptionLines().size() * LINE_H
                + (hasMeta() ? LINE_H : 0);
    }

    @Override
    public int naturalWidth(Font f) {
        int w = f.width("─── " + ability.name() + " ───");
        for (String d : ability.descriptionLines()) {
            w = Math.max(w, f.width(d));
        }
        return w;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        int curY = y + TOP_GAP;

        // Centred "─── Name ───" header
        String header = "─── " + ability.name() + " ───";
        int headerW   = f.width(header);
        int centredX  = x + (totalWidth - headerW) / 2;
        g.drawString(f, header, centredX, curY, headerColor, false);
        curY += LINE_H;

        // Description lines
        for (String desc : ability.descriptionLines()) {
            g.drawString(f, desc, x, curY, DESC_COLOR, false);
            curY += LINE_H;
        }

        // Meta (mana cost, cooldown)
        if (hasMeta()) {
            StringBuilder meta = new StringBuilder();
            if (ability.manaCost() != null) {
                meta.append("(Mana Cost: ").append(ability.manaCost()).append(")");
            }
            if (ability.cooldown() != null) {
                if (!meta.isEmpty()) meta.append(", ");
                meta.append("(").append(ability.cooldown()).append(" cooldown)");
            }
            g.drawString(f, meta.toString(), x, curY, META_COLOR, false);
        }
    }

    private boolean hasMeta() {
        return ability.manaCost() != null || ability.cooldown() != null;
    }
}