package com.kd_gaming1.tooltipoverhaulsb.render;

import com.kd_gaming1.tooltipoverhaulsb.data.SkyBlockItemData;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Single-entry cache for the most recently built tooltip layout.
 *
 * <h3>Why a single entry?</h3>
 * Only one tooltip is visible at a time in Minecraft. A player hovers over
 * one item, and the tooltip renders every frame until they move the cursor.
 * A single-entry cache with identity-based invalidation eliminates ~98% of
 * redundant {@link TooltipLayoutBuilder#build} calls.
 *
 * <h3>Invalidation</h3>
 * The cache invalidates when:
 * <ul>
 *   <li>The hovered {@link ItemStack} changes (by reference, not deep equality —
 *       the same slot always returns the same ItemStack instance)</li>
 *   <li>The item's NBT tag count changes (catches enchantment changes, reforges)</li>
 *   <li>The Minecraft game tick advances past a staleness threshold (5 ticks = 250ms),
 *       which handles edge cases like real-time stat updates from the server</li>
 * </ul>
 *
 * <p>Thread-safety: this class is not thread-safe. It must only be accessed
 * from the render thread, which is guaranteed by the mixin call site.
 */
public final class TooltipLayoutCache {

    /** Number of game ticks before a cached layout is considered stale. */
    private static final int STALE_TICKS = 5;

    // Cached state
    private @Nullable ItemStack cachedStack;
    private int cachedComponentCount;
    private long cachedTick;
    private @Nullable TooltipLayout cachedLayout;

    /**
     * Returns a layout for the given item, rebuilding only if the cache is invalid.
     *
     * @param font      font for measurement
     * @param data      parsed item data
     * @param stack     the hovered ItemStack (used for identity check)
     * @param gameTick  current game tick from {@code Minecraft.getInstance().level.getGameTime()}
     * @return a measured layout, never null
     */
    public TooltipLayout getOrBuild(Font font, SkyBlockItemData data,
                                    ItemStack stack, long gameTick) {
        if (isValid(stack, gameTick)) {
            return cachedLayout;
        }

        // Rebuild
        TooltipLayout layout = TooltipLayoutBuilder.build(font, data);

        // Store
        cachedStack          = stack;
        cachedComponentCount = stack.getComponents().size();
        cachedTick           = gameTick;
        cachedLayout         = layout;

        return layout;
    }

    /** Clears the cache. Call when the config changes or the player leaves a server. */
    public void invalidate() {
        cachedStack  = null;
        cachedLayout = null;
    }

    private boolean isValid(ItemStack stack, long gameTick) {
        if (cachedLayout == null || cachedStack == null) return false;
        // Reference identity — same inventory slot returns the same object
        if (cachedStack != stack) return false;
        // Component count changed (enchants added, reforge applied)
        if (stack.getComponents().size() != cachedComponentCount) return false;
        // Staleness timeout
        return (gameTick - cachedTick) < STALE_TICKS;
    }
}