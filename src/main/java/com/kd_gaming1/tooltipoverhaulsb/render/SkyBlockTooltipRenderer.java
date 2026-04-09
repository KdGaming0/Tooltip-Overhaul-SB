package com.kd_gaming1.tooltipoverhaulsb.render;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import com.kd_gaming1.tooltipoverhaulsb.data.SkyBlockItemData;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2ic;

/**
 * Renders a fully custom tooltip for SkyBlock items.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Obtain a measured {@link TooltipLayout} (from cache or fresh build)</li>
 *   <li>Position the tooltip on screen via {@link DefaultTooltipPositioner}</li>
 *   <li>Draw the background (vanilla sprite, resource-pack compatible)</li>
 *   <li>Draw the rarity border overlay</li>
 *   <li>Iterate entries and draw each one</li>
 * </ol>
 *
 * <p>This class owns <em>no</em> layout or content decisions. Those live in
 * {@link TooltipLayoutBuilder} and the {@link DrawEntry} implementations.
 *
 * <h3>Z-ordering</h3>
 * The tooltip is rendered via the deferred tooltip system which calls
 * {@code graphics.nextStratum()} before executing. This places the tooltip
 * above inventory slot items automatically — no manual Z-translate is needed.
 *
 * <p>Stateless except for the layout cache.
 */
public final class SkyBlockTooltipRenderer {

    private SkyBlockTooltipRenderer() {}

    /** Shared cache — only one tooltip is ever visible at a time. */
    private static final TooltipLayoutCache CACHE = new TooltipLayoutCache();

    /**
     * Vanilla tooltip background uses 9px margin + 3px padding on each side.
     * Content coordinates passed to TooltipRenderUtil are the inner content origin;
     * the visual background extends further out.
     */
    private static final int BG_MARGIN  = 9;
    private static final int BG_PADDING = 3;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Renders the complete SkyBlock tooltip for the given item data.
     * Called as a deferred runnable by the mixin interceptor.
     *
     * @param graphics current graphics context
     * @param font     font to use for rendering
     * @param data     parsed item data (never null, never empty)
     * @param stack    the hovered ItemStack
     * @param mouseX   cursor X position
     * @param mouseY   cursor Y position
     */
    public static void render(
            GuiGraphics graphics, Font font,
            SkyBlockItemData data, ItemStack stack,
            int mouseX, int mouseY) {

        // ── layout (cached) ──────────────────────────────────────────────
        long gameTick = getCurrentTick();
        TooltipLayout layout = CACHE.getOrBuild(font, data, stack, gameTick);
        if (layout.isEmpty()) return;

        int contentWidth  = layout.contentWidth();
        int contentHeight = layout.contentHeight();
        int padX          = TooltipLayoutBuilder.PAD_X;

        // ── position ─────────────────────────────────────────────────────
        Vector2ic pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                graphics.guiWidth(), graphics.guiHeight(),
                mouseX, mouseY, contentWidth, contentHeight);
        int drawX = pos.x();
        int drawY = pos.y();

        // ── render (Z already elevated by nextStratum in renderDeferredElements) ──
        graphics.pose().pushMatrix();

        // ── background (vanilla sprite → resource-pack compatible) ───────
        TooltipRenderUtil.renderTooltipBackground(
                graphics, drawX, drawY, contentWidth, contentHeight, null);

        // ── rarity border overlay ────────────────────────────────────────
        if (TooltipOverhaulConfig.useRarityColors) {
            drawRarityBorder(graphics, data, drawX, drawY, contentWidth, contentHeight);
        }

        // ── draw all entries ─────────────────────────────────────────────
        int innerWidth = contentWidth - padX * 2;
        int curY = drawY;
        for (DrawEntry entry : layout.entries()) {
            entry.draw(graphics, font, drawX + padX, curY, innerWidth);
            curY += entry.height();
        }

        graphics.pose().popMatrix();
    }

    /**
     * Clears the layout cache. Call when config changes or the player
     * disconnects from a server.
     */
    public static void invalidateCache() {
        CACHE.invalidate();
    }

    // =========================================================================
    // Border rendering
    // =========================================================================

    /**
     * Draws a 1px rarity-coloured border around the tooltip background sprite.
     *
     * <p>The vanilla background sprite extends from
     * {@code (x - MARGIN - PADDING)} to {@code (x + w + MARGIN + PADDING)},
     * so the border is drawn just outside that region.
     */
    private static void drawRarityBorder(
            GuiGraphics g, SkyBlockItemData data,
            int x, int y, int w, int h) {

        int bc = RarityColors.borderColor(data.rarity());

        // The visual background extends BG_MARGIN + BG_PADDING beyond the
        // content origin on each side. Border sits 1px outside that.
        int left   = x - BG_MARGIN - BG_PADDING - 1;
        int top    = y - BG_MARGIN - BG_PADDING - 1;
        int right  = x + w + BG_MARGIN + BG_PADDING + 1;
        int bottom = y + h + BG_MARGIN + BG_PADDING + 1;

        // Top edge
        g.fill(left, top,      right,     top + 1,    bc);
        // Bottom edge
        g.fill(left, bottom,   right,     bottom + 1, bc);
        // Left edge
        g.fill(left, top,      left + 1,  bottom + 1, bc);
        // Right edge
        g.fill(right - 1, top, right,     bottom + 1, bc);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns the current game tick for cache staleness checks.
     * Falls back to system time if no level is loaded (e.g. main menu).
     */
    private static long getCurrentTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getGameTime();
        }
        // Fallback: approximate tick from wall clock (20 ticks/sec)
        return System.currentTimeMillis() / 50;
    }
}