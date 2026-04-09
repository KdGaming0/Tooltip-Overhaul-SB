package com.kd_gaming1.tooltipoverhaulsb.render;

import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;

import java.util.EnumMap;
import java.util.Map;

/**
 * Centralised rarity → colour lookup.
 *
 * <p>Every colour is stored in ARGB format (0xAARRGGBB).
 * Three categories are provided:
 * <ul>
 *   <li><strong>Text</strong> — item name, footer, stars</li>
 *   <li><strong>Badge background</strong> — darker tones for the pill badges</li>
 *   <li><strong>Border</strong> — semi-transparent overlay for the tooltip frame</li>
 * </ul>
 */
public final class RarityColors {

    private RarityColors() {}

    private static final Map<Rarity, Integer> TEXT   = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, Integer> BADGE  = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, Integer> BORDER = new EnumMap<>(Rarity.class);

    static {
        TEXT.put(Rarity.COMMON,       0xFFFFFFFF);
        TEXT.put(Rarity.UNCOMMON,     0xFF55FF55);
        TEXT.put(Rarity.RARE,         0xFF5555FF);
        TEXT.put(Rarity.EPIC,         0xFFAA00AA);
        TEXT.put(Rarity.LEGENDARY,    0xFFFFAA00);
        TEXT.put(Rarity.MYTHIC,       0xFFFF55FF);
        TEXT.put(Rarity.DIVINE,       0xFF55FFFF);
        TEXT.put(Rarity.SPECIAL,      0xFFFF5555);
        TEXT.put(Rarity.VERY_SPECIAL, 0xFFFF5555);

        BADGE.put(Rarity.COMMON,       0xFF333333);
        BADGE.put(Rarity.UNCOMMON,     0xFF204020);
        BADGE.put(Rarity.RARE,         0xFF202060);
        BADGE.put(Rarity.EPIC,         0xFF3D003D);
        BADGE.put(Rarity.LEGENDARY,    0xFF3D2D00);
        BADGE.put(Rarity.MYTHIC,       0xFF3D003D);
        BADGE.put(Rarity.DIVINE,       0xFF003D3D);
        BADGE.put(Rarity.SPECIAL,      0xFF3D0000);
        BADGE.put(Rarity.VERY_SPECIAL, 0xFF3D0000);

        BORDER.put(Rarity.COMMON,       0x55FFFFFF);
        BORDER.put(Rarity.UNCOMMON,     0x5555FF55);
        BORDER.put(Rarity.RARE,         0x555555FF);
        BORDER.put(Rarity.EPIC,         0x55AA00AA);
        BORDER.put(Rarity.LEGENDARY,    0x55FFAA00);
        BORDER.put(Rarity.MYTHIC,       0x55FF55FF);
        BORDER.put(Rarity.DIVINE,       0x5555FFFF);
        BORDER.put(Rarity.SPECIAL,      0x55FF5555);
        BORDER.put(Rarity.VERY_SPECIAL, 0x55FF5555);
    }

    /** Rarity colour for item name, footer text, and stars. */
    public static int textColor(Rarity r) {
        return TEXT.getOrDefault(r, 0xFFFFFFFF);
    }

    /** Dark background for badge pills. */
    public static int badgeBg(Rarity r) {
        return BADGE.getOrDefault(r, 0xFF333333);
    }

    /** Semi-transparent border overlay colour. */
    public static int borderColor(Rarity r) {
        return BORDER.getOrDefault(r, 0x55FFFFFF);
    }

    /**
     * Brightens an ARGB colour by adding a fixed offset to each RGB channel.
     * Used for gem slot borders and highlight effects.
     */
    public static int brighten(int argb, int amount) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >>  8) & 0xFF) + amount);
        int b = Math.min(255, ( argb        & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}