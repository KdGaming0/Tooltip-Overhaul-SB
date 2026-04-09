package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;

/**
 * A single gem slot resolved from NBT {@code gems} and the gemstone reference data.
 *
 * <p>Stat bonus is a {@code double} to support fractional values (e.g. Topaz Pristine
 * stats such as 0.4, 1.6, 2.2). Empty slots report a bonus of 0.0.
 *
 * <p>Empty slots have {@code gemType == null} and {@code tier == null}.
 */
public record GemSlot(
        /** Raw NBT key, e.g. {@code "AMBER_0"}. Used for ordering and debugging. */
        String nbtKey,

        /** Normalised gem type, e.g. {@code "Amber"}. {@code null} if slot is empty. */
        @Nullable String gemType,

        /** Tier string, e.g. {@code "Fine"}. {@code null} if slot is empty. */
        @Nullable String tier,

        /** Symbol for this slot's type, e.g. {@code "⸕"} for Amber. */
        String slotSymbol,

        /** Human-readable stat name, e.g. {@code "Mining Speed"}. */
        String statName,

        /**
         * Computed stat bonus for the item's rarity, as a {@code double}.
         * {@code 0.0} if the slot is empty or gem data is unavailable.
         * Fractional values are expected for Topaz (Pristine stat).
         */
        double statBonus
) {
    /** Returns {@code true} if this slot has a gem inserted. */
    public boolean hasGem() {
        return gemType != null && tier != null;
    }
}