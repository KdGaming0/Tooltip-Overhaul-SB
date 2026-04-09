package com.kd_gaming1.tooltipoverhaulsb.parser;

import com.kd_gaming1.tooltipoverhaulsb.data.PetData;
import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;
import com.kd_gaming1.tooltipoverhaulsb.resource.SkyBlockDataResources;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Derives a {@link PetData} from raw {@code petInfo} values and the hardcoded
 * level curve.
 *
 * <p><b>Conflict resolution:</b>
 * <ol>
 *   <li>{@code petInfo.exp} from NBT — absolute EXP authority</li>
 *   <li>Hardcoded curve in {@link SkyBlockDataResources} — level authority</li>
 *   <li>Tooltip-displayed level — ignored entirely</li>
 * </ol>
 *
 * <p>This class is stateless and thread-safe.
 */
public final class PetLevelCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PetLevelCalculator.class);

    private static final int DEFAULT_MAX_LEVEL = 100;

    /** Do not instantiate — all methods are package-private via the parser. */
    PetLevelCalculator() {}

    // =========================================================================
    // Public entry point
    // =========================================================================

    /**
     * Calculates complete pet level data from raw NBT values.
     *
     * @param petType    internal type key, e.g. {@code "BAT"}
     * @param tierString rarity string from petInfo, e.g. {@code "MYTHIC"}
     * @param rawExp     petInfo.exp value
     * @param heldItem   petInfo.heldItem, may be {@code null}
     * @param candyUsed  petInfo.candyUsed
     * @return fully resolved {@link PetData}, or {@code null} if the tier cannot
     *         be parsed (error is logged)
     */
    @Nullable
    public PetData calculate(
            String petType,
            String tierString,
            double rawExp,
            @Nullable String heldItem,
            int candyUsed
    ) {
        Rarity tier = parseRarity(tierString);
        if (tier == null) {
            LOGGER.warn("[PetLevelCalculator] Unknown tier '{}' for pet '{}'", tierString, petType);
            return null;
        }

        String category  = SkyBlockDataResources.petCategory(petType);
        int    maxLevel  = resolveMaxLevel(petType);
        int[]  curve     = resolveLevelCurve(petType, tier);

        LevelResult result = computeLevel(rawExp, curve, maxLevel);

        return new PetData(
                petType,
                category,
                tier,
                rawExp,
                result.level,
                maxLevel,
                result.progress,
                result.expInLevel,
                result.expForNext,
                heldItem,
                candyUsed
        );
    }

    // =========================================================================
    // Core algorithm
    // =========================================================================

    /**
     * Walks the XP curve, consuming EXP threshold by threshold.
     *
     * <ul>
     *   <li>If remaining EXP is less than the next threshold, the pet is at
     *       the current level with partial progress.</li>
     *   <li>If the entire curve is consumed, the pet is at max level.</li>
     * </ul>
     *
     * @param rawExp   total accumulated EXP (authoritative from NBT)
     * @param curve    per-level EXP thresholds; index 0 = cost of level 1 → 2
     * @param maxLevel hard level cap
     * @return {@link LevelResult} with all derived values
     */
    private LevelResult computeLevel(double rawExp, int[] curve, int maxLevel) {
        double remaining = rawExp;
        int    level     = 1;

        for (int threshold : curve) {
            if (level >= maxLevel) {
                // Reached cap — report surplus EXP in expInLevel, progress = 0
                return new LevelResult(maxLevel, 0.0, remaining, 0.0);
            }

            if (remaining < threshold) {
                // Partial level — calculate exact fractional progress
                double progress = (threshold > 0)
                        ? Math.min(remaining / threshold, 1.0)
                        : 0.0;
                return new LevelResult(level, progress, remaining, threshold);
            }

            remaining -= threshold;
            level++;
        }

        // EXP exceeds entire curve — pet is at max level with surplus
        return new LevelResult(maxLevel, 0.0, remaining, 0.0);
    }

    // =========================================================================
    // Curve resolution
    // =========================================================================

    /**
     * Selects the correct XP curve for a pet.
     *
     * <p>Custom pets (dragon types) use their own hardcoded curve.
     * All others take a slice of the global curve, starting at the rarity offset.
     */
    private int[] resolveLevelCurve(String petType, Rarity tier) {
        int[] custom = SkyBlockDataResources.customPetLevelCurve(petType);
        if (custom != null) return custom;

        int offset      = resolveRarityOffset(petType, tier);
        int[] global    = SkyBlockDataResources.globalPetLevels();
        int   maxLevel  = resolveMaxLevel(petType);

        // We need (maxLevel - 1) threshold entries starting at the offset.
        // Each threshold covers one level transition (level N → N+1).
        int neededEntries = maxLevel - 1;
        int available     = global.length - offset;

        if (available <= 0) {
            LOGGER.warn("[PetLevelCalculator] Rarity offset {} exceeds curve length for pet '{}'",
                    offset, petType);
            return global;
        }

        int sliceLen = Math.min(neededEntries, available);
        int[] slice  = new int[sliceLen];
        System.arraycopy(global, offset, slice, 0, sliceLen);
        return slice;
    }

    private int resolveRarityOffset(String petType, Rarity tier) {
        Map<Rarity, Integer> custom = SkyBlockDataResources.customRarityOffsets(petType);
        if (custom != null) return custom.getOrDefault(tier, 0);
        return SkyBlockDataResources.globalRarityOffset(tier);
    }

    private int resolveMaxLevel(String petType) {
        Integer custom = SkyBlockDataResources.customPetMaxLevel(petType);
        return custom != null ? custom : DEFAULT_MAX_LEVEL;
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    @Nullable
    private Rarity parseRarity(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Rarity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // =========================================================================
    // Result value object
    // =========================================================================

    /** Intermediate result of a single level computation pass. */
    private record LevelResult(
            int    level,
            /** Fractional progress toward next level [0.0, 1.0]. 0.0 at max level. */
            double progress,
            /** Raw EXP accumulated within the current level. */
            double expInLevel,
            /** EXP required to reach next level. 0.0 at max level (no next level). */
            double expForNext
    ) {}
}