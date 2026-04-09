package com.kd_gaming1.tooltipoverhaulsb.data;

/**
 * A single enchantment sourced from the NBT {@code enchantments} compound.
 * The id and level are canonical; the display name is resolved later by the renderer.
 */
public record EnchantEntry(
        /** Hypixel enchant ID in snake_case, e.g. "ultimate_flowstate". */
        String id,
        int level
) {}