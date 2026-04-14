package com.kd_gaming1.tooltipoverhaulsb.render.entries;

import com.kd_gaming1.tooltipoverhaulsb.data.GemSlot;
import com.kd_gaming1.tooltipoverhaulsb.render.RarityColors;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayoutBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Gem slots row: renders each slot as a styled square with the gem symbol.
 * Empty slots show just the bracket symbol in gray.
 *
 * <p>Designed for easy migration to texture-based rendering. When a sprite
 * {@link Identifier} is set via {@link #setSlotSprite}, the slot background
 * will use the sprite instead of solid color fills. Until then, falls back
 * to the programmatic square rendering.
 */
public final class GemSlotsRow implements DrawEntry {

    private static final int SLOT_SIZE = TooltipLayoutBuilder.GEM_SLOT_SIZE;
    private static final int SLOT_GAP  = TooltipLayoutBuilder.GEM_SLOT_GAP;
    private static final int LINE_H    = TooltipLayoutBuilder.LINE_H;

    private static final int EMPTY_BG       = 0xFF2A2A2A;
    private static final int EMPTY_BORDER   = 0xFF555555;
    private static final int GEM_SYM_COLOR  = 0xFFFFFFFF;
    private static final int EMPTY_SYM_COLOR = 0xFF888888;

    // Inner highlight for filled gem slots (subtle top-edge shine)
    private static final int HIGHLIGHT_COLOR = 0x33FFFFFF;

    private final List<GemSlot> slots;

    /**
     * Optional sprite for slot backgrounds. When set, each slot renders
     * this sprite tinted by gem color instead of using g.fill().
     * Set this once globally when your texture atlas is ready.
     */
    private static @Nullable Identifier slotSprite = null;
    private static @Nullable Identifier emptySlotSprite = null;

    public GemSlotsRow(List<GemSlot> slots) {
        this.slots = List.copyOf(slots);
    }

    /**
     * Sets the sprite identifiers used for gem slot rendering.
     * Call once during mod initialization when textures are available.
     *
     * @param filled sprite for slots containing a gem (tinted by gem color)
     * @param empty  sprite for empty slots
     */
    public static void setSlotSprite(@Nullable Identifier filled, @Nullable Identifier empty) {
        slotSprite = filled;
        emptySlotSprite = empty;
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

            if (slot.hasGem()) {
                drawFilledSlot(g, f, slot, sx, slotY);
            } else {
                drawEmptySlot(g, f, slot, sx, slotY);
            }
        }
    }

    private void drawFilledSlot(GuiGraphics g, Font f, GemSlot slot, int sx, int slotY) {
        int bgColor     = gemColor(slot.gemType());
        int borderColor = RarityColors.brighten(bgColor, 80);

        // Use sprite if available, otherwise fall back to programmatic rendering
        if (slotSprite != null) {
            drawSpriteSlot(g, slotSprite, sx, slotY, bgColor);
        } else {
            drawProgrammaticSlot(g, sx, slotY, bgColor, borderColor);
        }

        // Symbol centred in the slot
        drawSymbol(g, f, slot.slotSymbol(), sx, slotY, GEM_SYM_COLOR);
    }

    private void drawEmptySlot(GuiGraphics g, Font f, GemSlot slot, int sx, int slotY) {
        if (emptySlotSprite != null) {
            drawSpriteSlot(g, emptySlotSprite, sx, slotY, EMPTY_BG);
        } else {
            drawProgrammaticSlot(g, sx, slotY, EMPTY_BG, EMPTY_BORDER);
        }

        drawSymbol(g, f, slot.slotSymbol(), sx, slotY, EMPTY_SYM_COLOR);
    }

    /**
     * Renders a slot using a tinted sprite texture.
     * The sprite is rendered at SLOT_SIZE × SLOT_SIZE with the given color tint.
     */
    private void drawSpriteSlot(GuiGraphics g, Identifier sprite, int sx, int slotY, int tintColor) {
        g.blitSprite(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                sprite, sx, slotY, SLOT_SIZE, SLOT_SIZE, tintColor);
    }

    /**
     * Renders a slot using programmatic fills (no texture needed).
     * Includes a 1px border, background fill, and subtle top highlight.
     */
    private void drawProgrammaticSlot(GuiGraphics g, int sx, int slotY,
                                      int bgColor, int borderColor) {
        // Background fill
        g.fill(sx, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

        // 1px border outline
        g.fill(sx,                  slotY,                 sx + SLOT_SIZE, slotY + 1,          borderColor);
        g.fill(sx,                  slotY + SLOT_SIZE - 1, sx + SLOT_SIZE, slotY + SLOT_SIZE,  borderColor);
        g.fill(sx,                  slotY,                 sx + 1,          slotY + SLOT_SIZE,  borderColor);
        g.fill(sx + SLOT_SIZE - 1,  slotY,                 sx + SLOT_SIZE, slotY + SLOT_SIZE,  borderColor);

        // Subtle top-edge highlight (1px, translucent white)
        g.fill(sx + 1, slotY + 1, sx + SLOT_SIZE - 1, slotY + 2, HIGHLIGHT_COLOR);
    }

    /**
     * Draws the gem symbol centred within a slot.
     */
    private void drawSymbol(GuiGraphics g, Font f, String sym, int sx, int slotY, int color) {
        int symW = f.width(sym);
        int symX = sx + (SLOT_SIZE - symW) / 2;
        int symY = slotY + (SLOT_SIZE - LINE_H) / 2 + 1;
        g.drawString(f, sym, symX, symY, color, false);
    }

    /**
     * Maps gem type names to representative ARGB colors.
     */
    private static int gemColor(String gemType) {
        if (gemType == null) return EMPTY_BG;
        return switch (gemType) {
            case "Ruby"     -> 0xFF991111;
            case "Amethyst" -> 0xFF771199;
            case "Jade"     -> 0xFF119933;
            case "Sapphire" -> 0xFF113399;
            case "Amber"    -> 0xFFAA6600;
            case "Topaz"    -> 0xFFAAAA00;
            case "Jasper"   -> 0xFFCC3366;
            case "Opal"     -> 0xFF6688AA;
            default         -> 0xFF555555;
        };
    }
}