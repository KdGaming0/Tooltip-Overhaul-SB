package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.data.GemSlot;
import com.kd_gaming1.tooltipoverhaulsb.render.RarityColors;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Gem slots row: renders each slot as a small coloured square with the gem symbol.
 * Empty slots show just the bracket symbol in gray.
 */
public final class GemSlotsRow implements DrawEntry {

    private static final int SLOT_SIZE = TooltipLayoutBuilder.GEM_SLOT_SIZE;
    private static final int SLOT_GAP  = TooltipLayoutBuilder.GEM_SLOT_GAP;
    private static final int LINE_H    = TooltipLayoutBuilder.LINE_H;

    private static final int EMPTY_BG     = 0xFF2A2A2A;
    private static final int EMPTY_BORDER = 0xFF555555;
    private static final int GEM_SYM_COLOR   = 0xFFFFFFFF;
    private static final int EMPTY_SYM_COLOR = 0xFF888888;

    private final List<GemSlot> slots;

    public GemSlotsRow(List<GemSlot> slots) {
        this.slots = List.copyOf(slots);
    }

    @Override
    public int height() {
        return SLOT_SIZE + 4;
    }

    @Override
    public int naturalWidth(Font f) {
        return slots.size() * SLOT_SIZE + Math.max(0, slots.size() - 1) * SLOT_GAP;
    }

    @Override
    public void draw(GuiGraphics g, Font f, int x, int y, int totalWidth) {
        if (slots.isEmpty()) return;

        // Centre the slots within the available width
        int rowWidth = naturalWidth(f);
        int startX   = x + (totalWidth - rowWidth) / 2;
        int slotY    = y + 2;

        for (int i = 0; i < slots.size(); i++) {
            GemSlot slot = slots.get(i);
            int sx = startX + i * (SLOT_SIZE + SLOT_GAP);

            int bgColor     = slot.hasGem() ? gemColor(slot.gemType()) : EMPTY_BG;
            int borderColor = slot.hasGem() ? RarityColors.brighten(bgColor, 80) : EMPTY_BORDER;

            // Background fill
            g.fill(sx, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

            // 1px border outline
            g.fill(sx,                  slotY,                 sx + SLOT_SIZE, slotY + 1,          borderColor);
            g.fill(sx,                  slotY + SLOT_SIZE - 1, sx + SLOT_SIZE, slotY + SLOT_SIZE,  borderColor);
            g.fill(sx,                  slotY,                 sx + 1,          slotY + SLOT_SIZE,  borderColor);
            g.fill(sx + SLOT_SIZE - 1,  slotY,                 sx + SLOT_SIZE, slotY + SLOT_SIZE,  borderColor);

            // Symbol centred in the slot
            String sym  = slot.slotSymbol();
            int symW    = f.width(sym);
            int symX    = sx + (SLOT_SIZE - symW) / 2;
            int symY    = slotY + (SLOT_SIZE - LINE_H) / 2 + 1;
            int symClr  = slot.hasGem() ? GEM_SYM_COLOR : EMPTY_SYM_COLOR;
            g.drawString(f, sym, symX, symY, symClr, false);
        }
    }

    /** Returns a display colour for the given gem type name. */
    private static int gemColor(String gemType) {
        if (gemType == null) return EMPTY_BG;
        return switch (gemType) {
            case "Ruby"     -> 0xFF8B1A1A;
            case "Amethyst" -> 0xFF6A0077;
            case "Jade"     -> 0xFF1A6A1A;
            case "Sapphire" -> 0xFF1A3A8B;
            case "Amber"    -> 0xFF8B5A00;
            case "Topaz"    -> 0xFF8B8B00;
            case "Jasper"   -> 0xFF8B1A8B;
            case "Opal"     -> 0xFF5A5A5A;
            default         -> 0xFF333333;
        };
    }
}