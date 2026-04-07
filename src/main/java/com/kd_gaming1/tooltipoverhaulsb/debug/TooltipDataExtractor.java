package com.kd_gaming1.tooltipoverhaulsb.debug;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Development utility: intercepts item tooltips and logs raw tooltip lines
 * alongside NBT/component data for analysis.
 *
 * IMPORTANT: This class is for data collection only.
 * It does not modify any tooltip content.
 * Disable or remove before release.
 */
public class TooltipDataExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger("TooltipOverhaulSB/Extractor");

    // Throttle: only log each unique item ID once per session to avoid log spam.
    // Key: Hypixel item ID string (e.g. "ASPECT_OF_THE_END"), or item registry name as fallback.
    private static final java.util.Set<String> LOGGED_ITEM_IDS = new java.util.HashSet<>();

    /**
     * Registers the tooltip data extraction callback.
     * Call this from your client entrypoint.
     */
    public static void register() {
        ItemTooltipCallback.EVENT.register(TooltipDataExtractor::onTooltip);
        LOGGER.info("[TooltipDataExtractor] Registered. Will log new item tooltips to console.");
    }

    /**
     * Clears the cache of already-logged items.
     * Useful when you want to re-capture data during a session.
     */
    public static void clearCache() {
        LOGGED_ITEM_IDS.clear();
        LOGGER.info("[TooltipDataExtractor] Cache cleared. All items will be re-logged.");
    }

    // -------------------------------------------------------------------------
    // Core callback
    // -------------------------------------------------------------------------

    private static void onTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipFlag type,
            List<Component> lines
    ) {
        if (stack.isEmpty()) return;

        RawItemData data = extractData(stack, lines);

        // Only log items we haven't seen yet this session
        if (LOGGED_ITEM_IDS.contains(data.itemKey())) return;
        LOGGED_ITEM_IDS.add(data.itemKey());

        logData(data);
    }

    // -------------------------------------------------------------------------
    // Extraction
    // -------------------------------------------------------------------------

    /**
     * Builds a RawItemData record from the stack and its current tooltip lines.
     */
    private static RawItemData extractData(ItemStack stack, List<Component> lines) {
        String registryName = extractRegistryName(stack);
        String[] tooltipLines = extractTooltipLines(lines);
        CompoundTag customData = extractCustomData(stack);
        CompoundTag extraAttributes = extractExtraAttributes(customData);
        String hypixelItemId = extractHypixelItemId(extraAttributes);

        return new RawItemData(
                registryName,
                hypixelItemId,
                tooltipLines,
                customData,
                extraAttributes
        );
    }

    /** Returns the registry path of the item (e.g. "minecraft:diamond_sword"). */
    private static String extractRegistryName(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Converts tooltip Component list to plain strings for logging. */
    private static String[] extractTooltipLines(List<Component> lines) {
        return lines.stream()
                .map(Component::getString)
                .toArray(String[]::new);
    }

    /**
     * Extracts the minecraft:custom_data component as a CompoundTag.
     * Returns an empty tag if absent.
     */
    private static CompoundTag extractCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return new CompoundTag();
        return customData.copyTag();
    }

    /**
     * Extracts the Hypixel-specific ExtraAttributes subtag.
     * Returns an empty tag if absent.
     */
    private static CompoundTag extractExtraAttributes(CompoundTag customData) {
        return customData.getCompoundOrEmpty("ExtraAttributes");
    }

    /**
     * Extracts the Hypixel item ID string (e.g. "ASPECT_OF_THE_END").
     * Returns an empty string if not present.
     */
    private static String extractHypixelItemId(CompoundTag extraAttributes) {
        return extraAttributes.getStringOr("id", "");
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    private static void logData(RawItemData data) {
        LOGGER.info("=== TOOLTIP CAPTURE ===");
        LOGGER.info("  Registry Name  : {}", data.registryName());
        LOGGER.info("  Hypixel Item ID: {}", data.hypixelItemId().isEmpty() ? "(none)" : data.hypixelItemId());
        LOGGER.info("  Tooltip Lines  ({}):", data.tooltipLines().length);
        for (int i = 0; i < data.tooltipLines().length; i++) {
            LOGGER.info("    [{}] {}", i, data.tooltipLines()[i]);
        }
        LOGGER.info("  Custom Data (raw NBT):");
        LOGGER.info("    {}", data.customData());
        LOGGER.info("  ExtraAttributes:");
        LOGGER.info("    {}", data.extraAttributes());
        LOGGER.info("=======================");
    }

    // -------------------------------------------------------------------------
    // Data container
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of an item's tooltip and component data.
     *
     * @param registryName    Minecraft registry name (e.g. "minecraft:skull")
     * @param hypixelItemId   Hypixel item ID from ExtraAttributes, or empty string
     * @param tooltipLines    Plain-text tooltip lines in order
     * @param customData      Full minecraft:custom_data tag (may be empty)
     * @param extraAttributes ExtraAttributes subtag (may be empty)
     */
    public record RawItemData(
            String registryName,
            String hypixelItemId,
            String[] tooltipLines,
            CompoundTag customData,
            CompoundTag extraAttributes
    ) {
        /**
         * Returns a stable deduplication key:
         * Prefers the Hypixel item ID if present, falls back to registry name.
         */
        public String itemKey() {
            return hypixelItemId().isEmpty() ? registryName() : hypixelItemId();
        }
    }
}