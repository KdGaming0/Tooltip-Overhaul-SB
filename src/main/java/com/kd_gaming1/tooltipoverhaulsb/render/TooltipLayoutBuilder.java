package com.kd_gaming1.tooltipoverhaulsb.render;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import com.kd_gaming1.tooltipoverhaulsb.data.*;
import com.kd_gaming1.tooltipoverhaulsb.render.TooltipLayout.DrawEntry;
import com.kd_gaming1.tooltipoverhaulsb.render.entries.*;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a {@link SkyBlockItemData} into a measured {@link TooltipLayout}.
 *
 * <p>This class owns all section-ordering and content-formatting decisions.
 * The renderer ({@link SkyBlockTooltipRenderer}) only draws; it never decides
 * <em>what</em> to draw.
 *
 * <p>Stateless — all methods are static and side-effect-free.
 */
public final class TooltipLayoutBuilder {

    private TooltipLayoutBuilder() {}

    // =========================================================================
    // Layout constants
    // =========================================================================

    public static final int LINE_H   = 9;   // Minecraft font line height in pixels
    public static final int PAD_X    = 5;   // left/right inner padding
    public static final int MIN_WIDTH = 120;
    public static final int MAX_WIDTH = 320; // hard cap to prevent off-screen overflow

    // Badge styling
    public static final int BADGE_PAD_X  = 4;
    public static final int BADGE_PAD_Y  = 1;
    public static final int BADGE_HEIGHT = LINE_H + BADGE_PAD_Y * 2;
    public static final int BADGE_GAP    = 3;

    // Gem slot styling
    public static final int GEM_SLOT_SIZE = 12;
    public static final int GEM_SLOT_GAP  = 4;

    public static final char STAR_CHAR = '\u272A'; // ✪

    // =========================================================================
    // Unknown-line patterns (compiled once)
    // =========================================================================

    private static final Pattern KILLS_PATTERN =
            Pattern.compile("^Kills?:\\s*([\\d,]+)$");
    private static final Pattern BREAKING_POWER_PATTERN =
            Pattern.compile("^Breaking Power\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUEL_LINE_PATTERN =
            Pattern.compile("^Fuel:\\s*[\\d,]+/", Pattern.CASE_INSENSITIVE);

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Builds a fully measured tooltip layout from parsed item data.
     *
     * @param font the font used to measure text widths
     * @param data parsed item data (never null)
     * @return a measured, immutable layout ready for rendering
     */
    public static TooltipLayout build(Font font, SkyBlockItemData data) {
        List<DrawEntry> entries = buildEntries(font, data);

        // ── measure ──────────────────────────────────────────────────────
        int contentWidth = entries.stream()
                .mapToInt(e -> e.naturalWidth(font))
                .max()
                .orElse(MIN_WIDTH);
        contentWidth = Math.max(contentWidth + PAD_X * 2, MIN_WIDTH);
        contentWidth = Math.min(contentWidth, MAX_WIDTH);

        int contentHeight = entries.stream().mapToInt(DrawEntry::height).sum();

        return new TooltipLayout(entries, contentWidth, contentHeight);
    }

    // =========================================================================
    // Entry list builder (section ordering)
    // =========================================================================

    /**
     * Produces the ordered list of {@link DrawEntry} objects for an item.
     *
     * <h3>Section order (top → bottom)</h3>
     * <ol>
     *   <li>Title + stars</li>
     *   <li>Badge row (rarity, type, HP count, BP)</li>
     *   <li>Gem slots (with decorative separators)</li>
     *   <li>Pet XP bar (pets only, before stats)</li>
     *   <li>Gear score (dungeon items only)</li>
     *   <li>Stats</li>
     *   <li>Enchantments</li>
     *   <li>Special lines (drill parts, fuel, rune)</li>
     *   <li>Abilities (regular or pet-style)</li>
     *   <li>Lore</li>
     *   <li>Kills</li>
     *   <li>Reforge bonus</li>
     *   <li>Soulbound</li>
     *   <li>Unknown lines (if enabled)</li>
     *   <li>Rarity footer</li>
     * </ol>
     */
    private static List<DrawEntry> buildEntries(Font font, SkyBlockItemData data) {
        List<DrawEntry> out = new ArrayList<>();
        boolean isPet = data.petData() != null;

        addTitle(out, data);
        addBadgeRow(out, data);
        addGemSlots(out, data);

        if (isPet && TooltipOverhaulConfig.showPetXpBar) {
            addPetXpBar(out, data.petData());
        }

        addGearScore(out, data);
        addStats(out, data);
        addEnchants(out, font, data);
        addSpecialLines(out, data);

        if (isPet) {
            addPetAbilities(out, data);
        } else {
            addAbilities(out, data);
        }

        addLore(out, data);
        addKills(out, data);
        addReforge(out, data);
        addSoulbound(out, data);

        if (TooltipOverhaulConfig.showUnknownLines) {
            addUnknownLines(out, data);
        }

        addRarityFooter(out, data);
        return out;
    }

    // =========================================================================
    // Section builders
    // =========================================================================

    // --- 1. Title + stars ----------------------------------------------------

    private static void addTitle(List<DrawEntry> out, SkyBlockItemData data) {
        int color = RarityColors.textColor(data.rarity());
        int stars = data.stars() != null ? data.stars() : 0;
        out.add(new TitleLine(data.displayName(), color, stars, color));
    }

    // --- 2. Badge row --------------------------------------------------------

    private static void addBadgeRow(List<DrawEntry> out, SkyBlockItemData data) {
        List<BadgeRow.Badge> badges = new ArrayList<>();

        badges.add(new BadgeRow.Badge(
                data.rarity().displayName(),
                RarityColors.badgeBg(data.rarity()),
                RarityColors.textColor(data.rarity())));

        if (data.itemType() != ItemType.MISC) {
            badges.add(new BadgeRow.Badge(
                    data.itemType().name(), 0xFF2A2A2A, 0xFFAAAAAA));
        }

        if (data.hotPotatoCount() != null && data.hotPotatoCount() > 0) {
            badges.add(new BadgeRow.Badge(
                    "HP " + data.hotPotatoCount(), 0xFF5A1A00, 0xFFFF8844));
        }

        Integer bp = extractBreakingPower(data);
        if (bp != null) {
            badges.add(new BadgeRow.Badge(
                    "BP " + bp, 0xFF1A3A1A, 0xFF55FF55));
        }

        if (!badges.isEmpty()) {
            out.add(new GapLine(2));
            out.add(new BadgeRow(badges));
        }
    }

    // --- 3. Gem slots --------------------------------------------------------

    private static void addGemSlots(List<DrawEntry> out, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showGemSlots || data.gemSlots().isEmpty()) return;

        int sepColor = RarityColors.borderColor(data.rarity());
        out.add(new DecorativeSeparator(sepColor));
        out.add(new GemSlotsRow(data.gemSlots()));
        out.add(new DecorativeSeparator(sepColor));
    }

    // --- 4. Pet XP bar -------------------------------------------------------

    private static void addPetXpBar(List<DrawEntry> out, PetData pet) {
        out.add(new GapLine(3));
        out.add(new PetXpBar(
                pet.calculatedLevel() + 1,
                pet.progressToNextLevel(),
                pet.expInCurrentLevel(),
                pet.expForNextLevel(),
                pet.isMaxLevel(),
                pet.rawExp()));
        out.add(new GapLine(2));
    }

    // --- 5. Gear score -------------------------------------------------------

    private static void addGearScore(List<DrawEntry> out, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showGearScore || data.gearScore() == null) return;

        out.add(new GapLine(3));
        String gs = "Gear Score: " + data.gearScore()
                + (data.scaledGearScore() != null ? " (" + data.scaledGearScore() + ")" : "");
        out.add(new TextLine(gs, 0xFFAAAAAA));
    }

    // --- 6. Stats ------------------------------------------------------------

    private static void addStats(List<DrawEntry> out, SkyBlockItemData data) {
        if (data.stats().isEmpty()) return;

        out.add(new GapLine(3));
        for (StatEntry stat : data.stats()) {
            String valuePart = extractValuePart(stat);
            out.add(new StatLine(stat.name(), valuePart, 0xFFAAAAAA, 0xFFFFFFFF));
        }
    }

    /**
     * Extracts the value portion from a stat's raw line.
     * Falls back to formatting the base value if the raw line is malformed.
     */
    private static String extractValuePart(StatEntry stat) {
        int colonIdx = stat.rawLine().indexOf(':');
        if (colonIdx >= 0 && colonIdx < stat.rawLine().length() - 1) {
            return stat.rawLine().substring(colonIdx + 1).trim();
        }
        // Fallback: format the numeric value
        return "+" + formatStatValue(stat.baseValue());
    }

    // --- 7. Enchantments -----------------------------------------------------

    private static void addEnchants(List<DrawEntry> out, Font font, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showEnchantments || data.enchantments().isEmpty()) return;

        out.add(new DividerLine(0x55888888));

        List<EnchantEntry> ultimates = new ArrayList<>();
        List<EnchantEntry> regular   = new ArrayList<>();
        for (EnchantEntry e : data.enchantments()) {
            if (e.id().startsWith("ultimate_")) ultimates.add(e);
            else regular.add(e);
        }

        // Ultimates: one per line, pink
        for (EnchantEntry e : ultimates) {
            out.add(new TextLine(formatEnchant(e), 0xFFFF55FF));
        }

        // Regular: comma-separated, wrapped at available width
        int wrapWidth = MAX_WIDTH - PAD_X * 2;
        StringBuilder line = new StringBuilder();
        for (EnchantEntry e : regular) {
            String formatted = formatEnchant(e);
            String sep = line.isEmpty() ? "" : ", ";
            int projectedWidth = font.width(line.toString() + sep + formatted);
            if (!line.isEmpty() && projectedWidth > wrapWidth) {
                out.add(new TextLine(line.toString(), 0xFFFFFF55));
                line.setLength(0);
                line.append(formatted);
            } else {
                line.append(sep).append(formatted);
            }
        }
        if (!line.isEmpty()) {
            out.add(new TextLine(line.toString(), 0xFFFFFF55));
        }
    }

    // --- 8. Special lines (drill parts, fuel, rune) --------------------------

    private static void addSpecialLines(List<DrawEntry> out, SkyBlockItemData data) {
        boolean addedGap = false;
        for (String line : data.unknownLines()) {
            if (isKillsLine(line) || isBreakingPowerLine(line) || isFuelLine(line)) continue;
            if (!addedGap) { out.add(new GapLine(2)); addedGap = true; }
            out.add(new TextLine(line, 0xFF888888));
        }

        // Fuel line rendered separately with spacing
        for (String line : data.unknownLines()) {
            if (isFuelLine(line)) {
                out.add(new GapLine(2));
                out.add(new TextLine(line, 0xFFAAAAAA));
                break;
            }
        }
    }

    // --- 9a. Regular item abilities ------------------------------------------

    private static void addAbilities(List<DrawEntry> out, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showAbilities || data.abilities().isEmpty()) return;

        for (AbilityEntry ability : data.abilities()) {
            out.add(new GapLine(3));
            String trigger = ability.trigger() != null ? "  " + ability.trigger() : "";
            out.add(new TextLine("Ability: " + ability.name() + trigger, 0xFF55FFFF));

            for (String desc : ability.descriptionLines()) {
                out.add(new TextLine(desc, 0xFFAAAAAA));
            }

            if (ability.manaCost() != null || ability.cooldown() != null) {
                StringBuilder meta = new StringBuilder();
                if (ability.manaCost() != null) meta.append("Mana Cost: ").append(ability.manaCost());
                if (ability.cooldown() != null) {
                    if (!meta.isEmpty()) meta.append(", ");
                    meta.append("Cooldown: ").append(ability.cooldown());
                }
                out.add(new TextLine(meta.toString(), 0xFF888888));
            }
        }
    }

    // --- 9b. Pet abilities (centered decorative headers) ---------------------

    private static void addPetAbilities(List<DrawEntry> out, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showAbilities || data.abilities().isEmpty()) return;

        int headerColor = RarityColors.textColor(data.rarity());
        for (AbilityEntry ability : data.abilities()) {
            out.add(new DividerLine(0x33888888));
            out.add(new PetAbilityEntry(ability, headerColor));
        }
    }

    // --- 10. Lore ------------------------------------------------------------

    private static void addLore(List<DrawEntry> out, SkyBlockItemData data) {
        if (data.loreLines().isEmpty()) return;
        out.add(new GapLine(3));
        for (String lore : data.loreLines()) {
            out.add(new TextLine(lore, 0xFF888888));
        }
    }

    // --- 11. Kills -----------------------------------------------------------

    private static void addKills(List<DrawEntry> out, SkyBlockItemData data) {
        for (String line : data.unknownLines()) {
            Matcher m = KILLS_PATTERN.matcher(line.trim());
            if (m.matches()) {
                out.add(new GapLine(3));
                out.add(new TextLine("Kills: " + m.group(1), 0xFFFFFF55));
                break;
            }
        }
    }

    // --- 12. Reforge bonus ---------------------------------------------------

    private static void addReforge(List<DrawEntry> out, SkyBlockItemData data) {
        if (!TooltipOverhaulConfig.showReforge || data.reforgeBonusText() == null) return;

        out.add(new DividerLine(0x55888888));
        for (String line : data.reforgeBonusText().split("\n")) {
            if (line.isBlank()) continue;
            boolean isHeader = line.trim().endsWith("Bonus");
            out.add(new TextLine(line.trim(), isHeader ? 0xFFFF5555 : 0xFFAAAAAA));
        }
    }

    // --- 13. Soulbound -------------------------------------------------------

    private static void addSoulbound(List<DrawEntry> out, SkyBlockItemData data) {
        if (data.isCoopSoulbound()) {
            out.add(new GapLine(3));
            out.add(new TextLine("Co-op Soulbound", 0xFF55FFFF));
        } else if (data.isSoulbound()) {
            out.add(new GapLine(3));
            out.add(new TextLine("Soulbound", 0xFF55FFFF));
        }
    }

    // --- 14. Unknown lines ---------------------------------------------------

    private static void addUnknownLines(List<DrawEntry> out, SkyBlockItemData data) {
        // Only add lines not already handled by addSpecialLines or addKills
        for (String line : data.unknownLines()) {
            if (isKillsLine(line) || isBreakingPowerLine(line) || isFuelLine(line)) continue;
            // These are already emitted in addSpecialLines — skip to avoid duplication
        }
        // Intentionally empty: all unknown lines are already rendered in addSpecialLines.
        // This method exists as a hook for future unknown-line categories (e.g. drill parts
        // parsed separately). Keeping it avoids forgetting the slot in the layout order.
    }

    // --- 15. Rarity footer ---------------------------------------------------

    private static void addRarityFooter(List<DrawEntry> out, SkyBlockItemData data) {
        out.add(new DividerLine(RarityColors.borderColor(data.rarity())));

        String footer = data.rarity().displayName();
        if (data.itemType() != ItemType.MISC) {
            footer += " " + data.itemType().name();
        }
        if (data.isDungeonItem()) {
            footer = "DUNGEON " + footer;
        }
        out.add(new TextLine(footer, RarityColors.textColor(data.rarity())));
    }

    // =========================================================================
    // Formatting helpers
    // =========================================================================

    private static String formatEnchant(EnchantEntry e) {
        String name = e.id().replace('_', ' ');
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return name + " " + toRoman(e.level());
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";   case 2 -> "II";  case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V";   case 6 -> "VI";  case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX";  case 10 -> "X";  default -> String.valueOf(n);
        };
    }

    public static String formatStatValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    public static String formatExp(double xp) {
        if (xp >= 1_000_000) return String.format("%.2fM", xp / 1_000_000.0);
        if (xp >= 1_000)     return String.format("%,.0f", xp);
        return String.format("%.0f", xp);
    }

    // =========================================================================
    // Unknown-line classification
    // =========================================================================

    private static boolean isKillsLine(String line) {
        return KILLS_PATTERN.matcher(line.trim()).matches();
    }

    private static boolean isBreakingPowerLine(String line) {
        return BREAKING_POWER_PATTERN.matcher(line.trim()).matches();
    }

    private static boolean isFuelLine(String line) {
        return FUEL_LINE_PATTERN.matcher(line.trim()).find();
    }

    private static Integer extractBreakingPower(SkyBlockItemData data) {
        for (String line : data.unknownLines()) {
            Matcher m = BREAKING_POWER_PATTERN.matcher(line.trim());
            if (m.matches()) {
                try { return Integer.parseInt(m.group(1)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}