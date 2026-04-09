package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;

/**
 * SkyBlock rarity tiers in ascending order.
 * Parsing always reads the last tooltip line, which may include a type suffix
 * (e.g. "LEGENDARY DUNGEON BOW") — use {@link #fromTooltipLine} to handle that.
 */
public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC,
    DIVINE,
    SPECIAL,
    VERY_SPECIAL;

    /**
     * Parses rarity from the final tooltip line.
     * The line may contain extra tokens after the rarity word (e.g. "LEGENDARY HELMET").
     *
     * @return matched rarity, or null if no known rarity token is found
     */
    @Nullable
    public static Rarity fromTooltipLine(String line) {
        if (line == null || line.isBlank()) return null;
        String upper = line.trim().toUpperCase();
        // Check longer tokens first so "VERY SPECIAL" matches before "SPECIAL"
        if (upper.startsWith("VERY SPECIAL")) return VERY_SPECIAL;
        for (Rarity r : values()) {
            if (upper.startsWith(r.name().replace('_', ' '))) return r;
        }
        return null;
    }

    /** Returns the display string matching the in-game representation. */
    public String displayName() {
        return name().replace('_', ' ');
    }
}