package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * Fully parsed, immutable representation of a SkyBlock item tooltip.
 *
 * <p>This is the single output of the parsing layer and the single input
 * to the rendering layer. All fields that may be absent are annotated nullable.
 */
public record SkyBlockItemData(

        // --- Identity ---
        String displayName,
        /** Hypixel item ID from custom_data. Null for plain vanilla items. */
        @Nullable String hypixelId,
        ItemType itemType,
        Rarity rarity,

        // --- Stats (from tooltip lines — no NBT equivalent) ---
        List<StatEntry> stats,

        // --- Enchantments (from NBT, authoritative) ---
        List<EnchantEntry> enchantments,

        // --- Abilities (from tooltip lines) ---
        List<AbilityEntry> abilities,

        // --- Gem slots (dual-source: layout from tooltip, gems from NBT) ---
        List<GemSlot> gemSlots,

        // --- Pet-specific (null for non-pets) ---
        @Nullable PetData petData,

        // --- Dungeon metadata ---
        @Nullable Integer gearScore,
        @Nullable Integer scaledGearScore,

        // --- Reforge ---
        @Nullable String reforgeId,
        /** Parsed reforge bonus description block (multi-line). */
        @Nullable String reforgeBonusText,

        // --- Item metadata ---
        @Nullable Integer hotPotatoCount,
        @Nullable Integer stars,
        boolean isDungeonItem,
        boolean isSoulbound,
        boolean isCoopSoulbound,

        // --- Drill-specific ---
        @Nullable Integer drillFuel,
        @Nullable Integer drillMaxFuel,

        // --- Lore (flavour/description lines, italic in-game) ---
        List<String> loreLines,

        // --- Fallback: all lines that did not match any known pattern ---
        List<String> unknownLines

) {}