package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;

/**
 * Parsed pet data derived exclusively from NBT {@code petInfo} JSON
 * and the {@code pets.json} level curve.
 *
 * <p>All level and progress values are recalculated — the tooltip's displayed
 * level is never trusted.
 */
public record PetData(
        /** Internal type key, e.g. "BAT". */
        String petType,
        /** Skill category, e.g. "MINING". */
        String category,
        Rarity tier,
        double rawExp,
        /** Level derived from exp curve. Always in [1, maxLevel]. */
        int calculatedLevel,
        int maxLevel,
        /** Progress to next level as fraction [0.0, 1.0]. 0.0 if at max level. */
        double progressToNextLevel,
        /** EXP accumulated within the current level. */
        double expInCurrentLevel,
        /** EXP required to reach the next level. 0 if at max level. */
        double expForNextLevel,
        /** Held item ID from petInfo, e.g. "PET_ITEM_MINING_SKILL_BOOST_RARE". */
        @Nullable String heldItemId,
        int candyUsed
) {
    /** True if this pet is at its maximum level. */
    public boolean isMaxLevel() {
        return calculatedLevel >= maxLevel;
    }
}