package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;

/**
 * SkyBlock rarity tiers in ascending order.
 * Parsing always reads the last tooltip line, which may include a type suffix
 * (e.g. "LEGENDARY DUNGEON BOW") or decorators (e.g. "a MYTHIC SWORD a").
 * Use {@link #fromTooltipLine} to handle all known formats.
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
     *
     * <p>Handles multiple formats:
     * <ul>
     *   <li>Plain: {@code "LEGENDARY HELMET"}</li>
     *   <li>Dungeon: {@code "DUNGEON RARE WEAPON"}</li>
     *   <li>Decorated: {@code "a MYTHIC SWORD a"} (Fabled/rarity-upgraded items)</li>
     *   <li>Decorated with symbols: {@code "✦ MYTHIC BOOTS ✦"} or {@code "+ MYTHIC SWORD ≈"}</li>
     * </ul>
     *
     * @return matched rarity, or null if no known rarity token is found
     */
    @Nullable
    public static Rarity fromTooltipLine(String line) {
        if (line == null || line.isBlank()) return null;

        // Strip common decorator characters and trim
        String cleaned = line.trim();

        // Remove known prefix/suffix decorators: "a", "*", "✦", "+", "≈", etc.
        // These appear on rarity-upgraded items (Fabled, Withered, etc.)
        cleaned = stripDecorators(cleaned);

        String upper = cleaned.toUpperCase();

        // Skip "DUNGEON" prefix if present
        if (upper.startsWith("DUNGEON ")) {
            upper = upper.substring("DUNGEON ".length()).trim();
        }

        // Check longer tokens first so "VERY SPECIAL" matches before "SPECIAL"
        if (upper.startsWith("VERY SPECIAL")) return VERY_SPECIAL;
        for (Rarity r : values()) {
            if (upper.startsWith(r.name().replace('_', ' '))) return r;
        }
        return null;
    }

    /**
     * Strips known decorator characters from the start and end of a rarity line.
     * SkyBlock uses various symbols around rarity-upgraded item footers.
     */
    private static String stripDecorators(String s) {
        // Remove single-char decorators from both ends iteratively
        // Known decorators: a, *, ✦, +, ≈, ✪, §, ☆, ◆
        int start = 0;
        int end = s.length();

        while (start < end) {
            char c = s.charAt(start);
            if (isDecorator(c) || c == ' ') {
                start++;
            } else {
                break;
            }
        }

        while (end > start) {
            char c = s.charAt(end - 1);
            if (isDecorator(c) || c == ' ') {
                end--;
            } else {
                break;
            }
        }

        return (start < end) ? s.substring(start, end).trim() : s.trim();
    }

    private static boolean isDecorator(char c) {
        return c == 'a' || c == '*' || c == '✦' || c == '+' || c == '≈'
                || c == '✪' || c == '☆' || c == '◆' || c == '§'
                || c == '♻' || c == '⚚';
    }

    /** Returns the display string matching the in-game representation. */
    public String displayName() {
        return name().replace('_', ' ');
    }
}