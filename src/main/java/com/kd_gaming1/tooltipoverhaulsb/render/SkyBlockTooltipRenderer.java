package com.kd_gaming1.tooltipoverhaulsb.render;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import com.kd_gaming1.tooltipoverhaulsb.data.SkyBlockItemData;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
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
 *   <li>Draw the rarity-colored border using a tinted overlay of the frame sprite</li>
 *   <li>Render the item icon in the top-left</li>
 *   <li>Iterate entries and draw each one</li>
 * </ol>
 *
 * <p>This class owns <em>no</em> layout or content decisions. Those live in
 * {@link TooltipLayoutBuilder} and the {@link DrawEntry} implementations.
 *
 * <p>Stateless except for the layout cache.
 */
public final class SkyBlockTooltipRenderer {

    private SkyBlockTooltipRenderer() {}

    /** Shared cache — only one tooltip is ever visible at a time. */
    private static final TooltipLayoutCache CACHE = new TooltipLayoutCache();

    /**
     * Vanilla tooltip background uses 9px margin + 3px padding on each side.
     */
    private static final int BG_MARGIN  = 9;
    private static final int BG_PADDING = 3;

    // Sprite identifiers for background and frame
    private static final Identifier BACKGROUND_SPRITE =
            Identifier.withDefaultNamespace("tooltip/background");
    private static final Identifier FRAME_SPRITE =
            Identifier.withDefaultNamespace("tooltip/frame");

    /** Size of the item icon rendered in the top-left corner. */
    private static final int ICON_SIZE = 16;
    private static final int ICON_OFFSET_X = -2;
    private static final int ICON_OFFSET_Y = -2;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Renders the complete SkyBlock tooltip for the given item data.
     * Called as a deferred runnable by the mixin interceptor.
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

        // ── render ───────────────────────────────────────────────────────
        graphics.pose().pushMatrix();

        // ── background (vanilla sprite → resource-pack compatible) ───────
        TooltipRenderUtil.renderTooltipBackground(
                graphics, drawX, drawY, contentWidth, contentHeight, null);

        // ── rarity border: tint the frame sprite with rarity color ────────
        if (TooltipOverhaulConfig.useRarityColors) {
            drawTintedFrame(graphics, data, drawX, drawY, contentWidth, contentHeight);
        }

        // ── item icon ───────────────────────────────────────────────
        renderItemIcon(graphics, stack, drawX, drawY);

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
    // Border rendering — tinted frame sprite overlay
    // =========================================================================

    /**
     * Draws the vanilla frame sprite tinted with the rarity color.
     *
     * <p>Instead of drawing a separate 1px border outside the tooltip, we
     * re-render the frame sprite (which is the 9-sliced border texture) with
     * the rarity color applied as a tint. This produces a properly themed
     * border that respects resource packs and looks integrated.
     */
    private static void drawTintedFrame(
            GuiGraphics g, SkyBlockItemData data,
            int x, int y, int w, int h) {

        int color = RarityColors.borderColor(data.rarity());

        int x0 = x - BG_PADDING - BG_MARGIN;
        int y0 = y - BG_PADDING - BG_MARGIN;
        int paddedWidth  = w + BG_PADDING * 2 + BG_MARGIN * 2;
        int paddedHeight = h + BG_PADDING * 2 + BG_MARGIN * 2;

        // Render the frame sprite with the rarity color as a tint
        g.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME_SPRITE,
                x0, y0, paddedWidth, paddedHeight, color);
    }

    // =========================================================================
    // Item icon rendering
    // =========================================================================

    /**
     * Renders the item's icon in the top-left corner of the tooltip.
     * Positioned just inside the background padding area.
     */
    private static void renderItemIcon(
            GuiGraphics g, ItemStack stack,
            int tooltipX, int tooltipY) {

        if (stack.isEmpty()) return;

        int iconX = tooltipX + ICON_OFFSET_X;
        int iconY = tooltipY + ICON_OFFSET_Y;

        g.renderItem(stack, iconX, iconY);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static long getCurrentTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getGameTime();
        }
        return System.currentTimeMillis() / 50;
    }
}