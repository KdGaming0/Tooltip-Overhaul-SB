package com.kd_gaming1.tooltipoverhaulsb.debug;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
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
        // TODO: Move to register
        if (!TooltipOverhaulConfig.enableDebugExtractor) return;

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
        CompoundTag result = new CompoundTag();
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                LOGGER.debug("  [extractCustomData] Found via CUSTOM_DATA component");
                return customData.copyTag();
            }

            LOGGER.debug("  [extractCustomData] No CUSTOM_DATA, trying PROFILE...");

            // 1. Try normal custom_data first (works for most items)
            if (customData != null) {
                return customData.copyTag(); // <-- player heads often land here, not in PROFILE
            }

            // 2. Player heads that genuinely have no custom_data
            var profile = stack.get(DataComponents.PROFILE);
            if (profile != null) {
                result.putString("PROFILE", profile.toString());
            }

            // 3. Component dump fallback
            var components = stack.getComponents();
            for (var type : components.keySet()) {
                Object value = components.get(type);
                if (value != null) {
                    result.putString(type.toString(), value.toString());
                }
            }

        } catch (Exception e) {
            LOGGER.warn("[TooltipDataExtractor] Failed to extract custom data: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Extracts the Hypixel-specific ExtraAttributes subtag.
     * Returns an empty tag if absent.
     */
    private static CompoundTag extractExtraAttributes(CompoundTag data) {
        // 1. Normal case
        if (data.contains("ExtraAttributes")) {
            return data.getCompound("ExtraAttributes").orElse(new CompoundTag());
        }

        // 2. Try parsing stringified NBT
        if (data.contains("minecraft:custom_data")) {
            String raw = data.getString("minecraft:custom_data").orElse("");

            if (!raw.isEmpty() && raw.contains("ExtraAttributes")) {
                try {
                    CompoundTag parsed = TagParser.parseCompoundFully(raw);
                    return parsed.getCompound("ExtraAttributes").orElse(new CompoundTag());
                } catch (Exception ignored) {}
            }
        }

        return new CompoundTag();
    }

    /**
     * Extracts the Hypixel item ID string (e.g. "ASPECT_OF_THE_END").
     * Returns an empty string if not present.
     */
    private static String extractHypixelItemId(CompoundTag extraAttributes) {
        // 1. Normal items (most SkyBlock items)
        String id = extraAttributes.getStringOr("id", "");
        if (!id.isEmpty()) return id;

        // 2. Pets (stored as JSON in "petInfo")
        String petInfo = extraAttributes.getStringOr("petInfo", "");
        if (!petInfo.isEmpty()) {
            try {
                com.google.gson.JsonObject json =
                        com.google.gson.JsonParser.parseString(petInfo).getAsJsonObject();

                String type = json.has("type") ? json.get("type").getAsString() : "UNKNOWN";
                String tier = json.has("tier") ? json.get("tier").getAsString() : "UNKNOWN";

                // Example result: PET_ENDERMAN_LEGENDARY
                return "PET_" + type + "_" + tier;

            } catch (Exception e) {
                // Fallback if JSON parsing fails
                return "PET_UNKNOWN";
            }
        }

        // 3. Fallback (no identifiable Hypixel data)
        return "";
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
            if (!hypixelItemId().isEmpty()) return hypixelItemId();

            // For vanilla items (player heads, etc.), append the first tooltip line
            // so each unique pet/head gets its own cache entry
            String displayName = tooltipLines().length > 0 ? tooltipLines()[0] : "";
            return registryName() + "|" + displayName;
        }
    }
}