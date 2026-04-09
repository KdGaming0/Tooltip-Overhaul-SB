package com.kd_gaming1.tooltipoverhaulsb.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kd_gaming1.tooltipoverhaulsb.data.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates all sub-parsers and produces a single {@link SkyBlockItemData}.
 *
 * <p><b>Call once per tooltip event</b>. Cache the result by item stack identity
 * to avoid re-parsing every frame (see Step 8).
 *
 * <p><b>NBT structure (Hypixel 1.21.x):</b>
 * SkyBlock item data lives directly at the root of {@code minecraft:custom_data}.
 * There is no {@code ExtraAttributes} subtag in this version.
 * <pre>{@code
 * custom_data: { id:"JUJU_SHORTBOW", enchantments:{...}, modifier:"hasty", ... }
 * }</pre>
 *
 * <p><b>Conflict resolution priority:</b>
 * <ol>
 *   <li>NBT ({@code minecraft:custom_data}) — absolute authority</li>
 *   <li>Hardcoded reference data — calculation authority (pet levels, gem stats)</li>
 *   <li>Tooltip lines — display/fallback only</li>
 *   <li>Unknown lines — preserved verbatim, never interpreted</li>
 * </ol>
 */
public final class SkyBlockItemParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkyBlockItemParser.class);

    private final TooltipLineClassifier classifier  = new TooltipLineClassifier();
    private final PetLevelCalculator    petCalc     = new PetLevelCalculator();
    private final GemSlotParser         gemParser   = new GemSlotParser();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Parses an {@link ItemStack} and its pre-computed tooltip lines into
     * a fully structured {@link SkyBlockItemData}.
     *
     * @param stack        item being hovered
     * @param tooltipLines plain-text tooltip lines with formatting codes stripped;
     *                     index 0 is the item name line
     * @return parsed data record; never {@code null}
     */
    public SkyBlockItemData parse(ItemStack stack, List<String> tooltipLines) {
        // 1. NBT custom_data (authoritative — SkyBlock data is at root level)
        CompoundTag customData = extractCustomData(stack);

        // 2. Read scalar NBT fields
        String  hypixelId      = customData.getString("id").orElse(null);
        String  reforgeId      = customData.getString("modifier").orElse(null);
        int     hotPotatoCount = customData.getIntOr("hot_potato_count", 0);
        int     upgradeLevel   = customData.getIntOr("upgrade_level", 0);
        boolean isDungeonItem  = customData.getBooleanOr("dungeon_item", false);
        boolean hasDrillFuel   = customData.contains("drill_fuel");
        int     drillFuelRaw   = customData.getIntOr("drill_fuel", 0);

        CompoundTag enchNbt = customData.getCompoundOrEmpty("enchantments");
        CompoundTag gemsNbt = customData.getCompoundOrEmpty("gems");

        // 3. Display name from ItemStack (not tooltip[0] — avoids formatting artefacts)
        String displayName = stack.getHoverName().getString();

        // 4. Classify tooltip lines into typed sections
        TooltipLineClassifier.ParsedSections sections = classifier.classify(tooltipLines);

        // 5. Rarity (tooltip last line — no NBT equivalent)
        Rarity rarity = sections.rarity != null ? sections.rarity : Rarity.COMMON;

        // 6. Item type (NBT id primary, tooltip type tag fallback)
        String  lastLine = tooltipLines.isEmpty() ? "" : tooltipLines.get(tooltipLines.size() - 1);
        ItemType itemType = ItemType.infer(hypixelId, lastLine);

        // 7. Enchantments from NBT (authoritative — tooltip enchant lines are display only)
        List<EnchantEntry> enchantments = parseEnchantments(enchNbt);

        // 8. Gem slots (NBT gems key + reference data)
        List<GemSlot> gemSlots = gemsNbt.isEmpty()
                ? Collections.emptyList()
                : gemParser.parse(gemsNbt, rarity);

        // 9. Pet data (NBT petInfo is the sole authority)
        PetData petData = "PET".equals(hypixelId) ? parsePet(customData) : null;

        // 10. Drill-specific fields
        Integer drillFuel    = hasDrillFuel ? drillFuelRaw : null;
        Integer drillMaxFuel = hasDrillFuel ? parseDrillMaxFuel(sections) : null;

        return new SkyBlockItemData(
                displayName,
                hypixelId,
                itemType,
                rarity,
                Collections.unmodifiableList(sections.stats),
                enchantments,
                Collections.unmodifiableList(sections.abilities),
                gemSlots,
                petData,
                sections.gearScore,
                sections.scaledGearScore,
                reforgeId,
                sections.reforgeBonusText,
                hotPotatoCount > 0 ? hotPotatoCount : null,
                upgradeLevel   > 0 ? upgradeLevel   : null,
                isDungeonItem,
                sections.isSoulbound,
                sections.isCoopSoulbound,
                drillFuel,
                drillMaxFuel,
                Collections.unmodifiableList(sections.loreLines),
                Collections.unmodifiableList(sections.unknownLines)
        );
    }

    // =========================================================================
    // Sub-parsers
    // =========================================================================

    private List<EnchantEntry> parseEnchantments(CompoundTag enchNbt) {
        if (enchNbt.isEmpty()) return Collections.emptyList();

        List<EnchantEntry> result = new ArrayList<>();
        for (String key : enchNbt.keySet()) {
            int level = enchNbt.getIntOr(key, 0);
            if (level > 0) result.add(new EnchantEntry(key, level));
        }
        result.sort(Comparator.comparing(EnchantEntry::id));
        return Collections.unmodifiableList(result);
    }

    /**
     * Parses the {@code petInfo} JSON string from NBT into a {@link PetData}.
     *
     * <p>Fallback: if {@code petInfo.exp} is missing or the JSON is malformed,
     * returns {@code null} and the caller preserves raw tooltip lines.
     */
    @Nullable
    private PetData parsePet(CompoundTag customData) {
        String petInfoRaw = customData.getString("petInfo").orElse(null);
        if (petInfoRaw == null || petInfoRaw.isBlank()) {
            LOGGER.warn("[SkyBlockItemParser] PET item has no petInfo tag — skipping level calculation");
            return null;
        }

        try {
            JsonObject petInfo = JsonParser.parseString(petInfoRaw).getAsJsonObject();

            if (!petInfo.has("exp")) {
                LOGGER.warn("[SkyBlockItemParser] petInfo.exp missing — skipping level calculation");
                return null;
            }

            String petType  = petInfo.get("type").getAsString();
            String tier     = petInfo.get("tier").getAsString();
            double rawExp   = petInfo.get("exp").getAsDouble();

            String heldItem = (petInfo.has("heldItem") && !petInfo.get("heldItem").isJsonNull())
                    ? petInfo.get("heldItem").getAsString()
                    : null;

            int candyUsed = petInfo.has("candyUsed")
                    ? petInfo.get("candyUsed").getAsInt()
                    : 0;

            return petCalc.calculate(petType, tier, rawExp, heldItem, candyUsed);

        } catch (Exception e) {
            LOGGER.warn("[SkyBlockItemParser] Failed to parse petInfo — {}", e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to extract max fuel from known tooltip line patterns.
     *
     * <p>Max fuel is part of the drill fuel tank description line
     * (e.g. {@code "100,000 Max Fuel Capacity"}) — it has no dedicated NBT key.
     * Returns {@code null} if the line cannot be found.
     */
    @Nullable
    private Integer parseDrillMaxFuel(TooltipLineClassifier.ParsedSections sections) {
        for (String line : sections.unknownLines) {
            if (line.contains("Max Fuel Capacity")) {
                String digits = line.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try { return Integer.parseInt(digits); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /**
     * Extracts the {@code minecraft:custom_data} component as a CompoundTag.
     *
     * <p>On Hypixel SkyBlock 1.21.x, item data (id, enchantments, modifier, etc.)
     * lives directly at the root of custom_data — there is no ExtraAttributes subtag.
     *
     * @return the custom_data tag, or an empty tag if absent
     */
    private CompoundTag extractCustomData(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return (cd != null) ? cd.copyTag() : new CompoundTag();
    }
}