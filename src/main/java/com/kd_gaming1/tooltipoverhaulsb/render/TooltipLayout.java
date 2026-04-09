package com.kd_gaming1.tooltipoverhaulsb.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * An immutable, pre-measured tooltip layout ready for rendering.
 *
 * <p>Produced by {@link TooltipLayoutBuilder} and consumed by
 * {@link SkyBlockTooltipRenderer}. Separating measurement from drawing
 * allows caching: the same layout can be drawn across multiple frames
 * without re-parsing or re-measuring.
 *
 * <p>Thread-safety: instances are immutable and safe to share, but
 * {@link DrawEntry#draw} must only be called on the render thread.
 */
public final class TooltipLayout {

    private final List<DrawEntry> entries;
    private final int contentWidth;
    private final int contentHeight;

    TooltipLayout(List<DrawEntry> entries, int contentWidth, int contentHeight) {
        this.entries       = List.copyOf(entries);
        this.contentWidth  = contentWidth;
        this.contentHeight = contentHeight;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    /** Ordered list of renderable rows. */
    public List<DrawEntry> entries() {
        return entries;
    }

    /**
     * Total pixel width of the tooltip content area (including inner padding).
     * This is the width passed to {@code TooltipRenderUtil.renderTooltipBackground}.
     */
    public int contentWidth() {
        return contentWidth;
    }

    /** Total pixel height of the tooltip content area. */
    public int contentHeight() {
        return contentHeight;
    }

    /** True if this layout contains no renderable entries. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // =========================================================================
    // DrawEntry contract
    // =========================================================================

    /**
     * A single renderable row in the tooltip layout.
     *
     * <p>Each entry reports its height and natural width so the layout engine
     * can size the tooltip box before anything is drawn. Implementations must
     * be lightweight — they are created once and may be cached across frames.
     */
    public interface DrawEntry {

        /** Pixel height this entry occupies. */
        int height();

        /**
         * Minimum pixel width this entry needs.
         * Used to compute the overall tooltip width before rendering.
         */
        int naturalWidth(Font f);

        /**
         * Draw this entry into the tooltip at the given position.
         *
         * @param g          current graphics context
         * @param f          font to use for text
         * @param x          left edge of the content area (inside padding)
         * @param y          top edge for this entry
         * @param totalWidth usable width (content width minus padding)
         */
        void draw(GuiGraphics g, Font f, int x, int y, int totalWidth);
    }
}