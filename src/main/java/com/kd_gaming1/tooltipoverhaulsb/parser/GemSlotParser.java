package com.kd_gaming1.tooltipoverhaulsb.parser;

import com.kd_gaming1.tooltipoverhaulsb.data.GemSlot;
import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;
import com.kd_gaming1.tooltipoverhaulsb.resource.SkyBlockDataResources;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses the {@code gems} compound tag from an item's custom data into a list of
 * {@link GemSlot} records.
 *
 * <p><b>NBT format:</b> {@code gems:{AMBER_0:"FINE", JADE_1:"FLAWLESS"}}
 * <ul>
 *   <li>Key format: {@code GEMTYPE_INDEX} (e.g. {@code AMBER_0})</li>
 *   <li>Value: tier string in UPPER_CASE (e.g. {@code "FINE"})</li>
 * </ul>
 *
 * <p>Invalid or unrecognised entries are retained as empty slots to preserve
 * all raw data per the unknown-line policy.
 *
 * <p>Stat bonuses use {@code double} precision to correctly represent fractional
 * values (e.g. Topaz Pristine: 0.4, 1.6, 2.2).
 */
public final class GemSlotParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GemSlotParser.class);

    GemSlotParser() {}

    // =========================================================================
    // Public entry point
    // =========================================================================

    /**
     * Parses all gem entries from a {@code gems} compound tag.
     *
     * @param gemsTag the {@code gems} subtag; may be {@code null} or empty
     * @param rarity  item rarity, required for stat bonus calculation
     * @return ordered list of resolved gem slots; never {@code null}; may be empty
     */
    public List<GemSlot> parse(@Nullable CompoundTag gemsTag, Rarity rarity) {
        List<GemSlot> result = new ArrayList<>();

        if (gemsTag == null || gemsTag.isEmpty()) {
            return result;
        }

        for (String nbtKey : gemsTag.keySet()) {
            String tierRaw = gemsTag.getStringOr(nbtKey, "");
            GemSlot slot   = parseSlot(nbtKey, tierRaw, rarity);
            if (slot != null) {
                result.add(slot);
            }
        }

        // Deterministic ordering: AMBER_0 before AMBER_1, etc.
        result.sort(Comparator.comparing(GemSlot::nbtKey));
        return result;
    }

    // =========================================================================
    // Slot parsing
    // =========================================================================

    /**
     * Parses a single NBT key/value pair into a {@link GemSlot}.
     *
     * <p>On parse failure the slot is still emitted (with null gem/tier) so that
     * the raw NBT key is not silently discarded.
     */
    @Nullable
    private GemSlot parseSlot(String nbtKey, String tierRaw, Rarity rarity) {
        // Keys like "AMBER_0" → gemType = "Amber", index ignored
        int lastUnderscore = nbtKey.lastIndexOf('_');
        if (lastUnderscore < 1) {
            LOGGER.debug("[GemSlotParser] Unrecognised gem key format: '{}' — emitting empty slot", nbtKey);
            return new GemSlot(nbtKey, null, null, "?", "Unknown", 0.0);
        }

        String gemTypeRaw        = nbtKey.substring(0, lastUnderscore);
        String gemTypeNormalised = normaliseGemType(gemTypeRaw); // "AMBER" → "Amber"

        String tier = normaliseTier(tierRaw);
        if (tier == null) {
            LOGGER.debug("[GemSlotParser] Unknown tier '{}' for key '{}' — emitting empty slot",
                    tierRaw, nbtKey);
            return new GemSlot(
                    nbtKey,
                    null,
                    null,
                    SkyBlockDataResources.gemSlotSymbol(gemTypeNormalised),
                    SkyBlockDataResources.gemStatName(gemTypeNormalised),
                    0.0
            );
        }

        double bonus = SkyBlockDataResources.gemStatBonus(gemTypeNormalised, tier, rarity);

        return new GemSlot(
                nbtKey,
                gemTypeNormalised,
                tier,
                SkyBlockDataResources.gemSlotSymbol(gemTypeNormalised),
                SkyBlockDataResources.gemStatName(gemTypeNormalised),
                bonus
        );
    }

    // =========================================================================
    // Normalisation
    // =========================================================================

    /** Converts {@code "AMBER"} → {@code "Amber"} for gemstone data lookup. */
    private String normaliseGemType(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0))
                + raw.substring(1).toLowerCase();
    }

    /**
     * Converts a tier string from ALL_CAPS NBT form to the Title Case used
     * in gemstone data.
     *
     * @return canonical tier string, or {@code null} for unrecognised values
     */
    @Nullable
    private String normaliseTier(String raw) {
        return switch (raw.toUpperCase()) {
            case "ROUGH"    -> "Rough";
            case "FLAWED"   -> "Flawed";
            case "FINE"     -> "Fine";
            case "FLAWLESS" -> "Flawless";
            case "PERFECT"  -> "Perfect";
            default         -> null;
        };
    }
}