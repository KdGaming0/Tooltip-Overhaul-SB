package com.kd_gaming1.tooltipoverhaulsb;

import eu.midnightdust.lib.config.MidnightConfig;

/**
 * Runtime configuration for Tooltip Overhaul SB.
 *
 * <p>Managed by MidnightLib. All fields are {@code public static} as required.
 * Initialize once at mod startup with:
 * <pre>{@code
 * MidnightConfig.init("tooltip-overhaul-sb", TooltipOverhaulConfig.class);
 * }</pre>
 *
 * <p>Settings are exposed in the in-game options via ModMenu integration.
 */
public class TooltipOverhaulConfig extends MidnightConfig {

    // -------------------------------------------------------------------------
    // Category keys
    // -------------------------------------------------------------------------

    public static final String GENERAL  = "general";
    public static final String DISPLAY  = "display";
    public static final String ADVANCED = "advanced";

    // -------------------------------------------------------------------------
    // General
    // -------------------------------------------------------------------------

    /**
     * Master toggle. When {@code false}, the mod renders nothing and all other
     * settings are ignored.
     */
    @Entry(category = GENERAL)
    public static boolean enabled = true;

    // -------------------------------------------------------------------------
    // Display sections
    // -------------------------------------------------------------------------

    /** Show the gem slot row in the tooltip. */
    @Entry(category = DISPLAY)
    public static boolean showGemSlots = true;

    /** Show the enchantment section. */
    @Entry(category = DISPLAY)
    public static boolean showEnchantments = true;

    /** Show the pet XP progress bar and level information. */
    @Entry(category = DISPLAY)
    public static boolean showPetXpBar = true;

    /** Show ability blocks (RIGHT CLICK / LEFT CLICK / SNEAK). */
    @Entry(category = DISPLAY)
    public static boolean showAbilities = true;

    /** Show the reforge bonus block. */
    @Entry(category = DISPLAY)
    public static boolean showReforge = true;

    /** Colour the tooltip background and border using the item's rarity colour. */
    @Entry(category = DISPLAY)
    public static boolean useRarityColors = true;

    /** Show dungeon gear score and scaled gear score. */
    @Entry(category = DISPLAY)
    public static boolean showGearScore = true;

    // -------------------------------------------------------------------------
    // Advanced / compatibility
    // -------------------------------------------------------------------------

    /**
     * Preserve and display tooltip lines that the parser did not recognise.
     * Disable only if another mod produces unwanted extra lines.
     */
    @Entry(category = ADVANCED)
    public static boolean showUnknownLines = true;

    /**
     * Only apply the custom tooltip to items that have a valid Hypixel item ID.
     * When {@code false}, the parser also runs on plain vanilla items.
     */
    @Entry(category = ADVANCED)
    public static boolean requireHypixelId = true;

    /**
     * Enable the debug tooltip data extractor. Logs raw tooltip lines and
     * NBT data to the console when hovering over items. Useful for development
     * and troubleshooting. Disable in normal gameplay to reduce log noise.
     */
    @Entry(category = ADVANCED)
    public static boolean enableDebugExtractor = false;
}