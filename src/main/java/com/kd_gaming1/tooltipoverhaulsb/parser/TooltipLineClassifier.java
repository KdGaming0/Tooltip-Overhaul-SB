package com.kd_gaming1.tooltipoverhaulsb.parser;

import com.kd_gaming1.tooltipoverhaulsb.data.AbilityEntry;
import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;
import com.kd_gaming1.tooltipoverhaulsb.data.StatEntry;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * State-machine parser that classifies raw tooltip lines into typed sections.
 *
 * <p>Input: ordered list of plain-text tooltip lines (formatting codes stripped).
 * Output: populated {@link ParsedSections} aggregate.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Unknown lines are never discarded — they flow into {@code unknownLines}.</li>
 *   <li>No item-specific hardcoding. All detection is pattern-based.</li>
 *   <li>Stat values are read from tooltip lines; enchant IDs/levels come from NBT.</li>
 *   <li>Vanilla enchant description blocks are consumed so they don't duplicate
 *       the compact NBT-sourced enchant section.</li>
 * </ul>
 */
public final class TooltipLineClassifier {

    // --- Stat line: "Damage: +580" or "Mining Speed: +2,115 (+240)"
    private static final Pattern STAT_PATTERN =
            Pattern.compile("^(.+?):\\s*([+\\-][\\d,\\.]+%?)(.*)$");

    // --- Bonus values in parentheses: "(+240)" or "(-3%)"
    private static final Pattern BONUS_PATTERN =
            Pattern.compile("\\(([+\\-][\\d,\\.]+%?)\\)");

    // --- Gear Score: "Gear Score: 734 (1759)"
    private static final Pattern GEAR_SCORE_PATTERN =
            Pattern.compile("^Gear Score:\\s*([\\d,]+)(?:\\s*\\(([\\d,]+)\\))?$");

    // --- Ability header: "Ability: Throwing Axe RIGHT CLICK"
    private static final Pattern ABILITY_PATTERN =
            Pattern.compile("^Ability:\\s*(.+?)(?:\\s+(RIGHT CLICK|LEFT CLICK|SNEAK|HOLD RIGHT CLICK|SNEAK LEFT CLICK|SNEAK RIGHT CLICK))?$");

    // --- Mana / cooldown
    private static final Pattern MANA_PATTERN     = Pattern.compile("Mana Cost:\\s*([\\d,]+)");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("Cooldown:\\s*([\\d.]+s)");

    // --- Reforge bonus header (ends with "Bonus")
    private static final Pattern REFORGE_BONUS_PATTERN =
            Pattern.compile("^\\S+\\s+Bonus$");

    // --- Gem slot indicator line: "[⚔] [⚔] [☂]"
    private static final Pattern GEM_SLOT_LINE_PATTERN =
            Pattern.compile("^(\\[.+?\\]\\s*)+$");

    // --- Progress line: "Progress to Level 90: 32.5%"
    private static final Pattern PROGRESS_PATTERN =
            Pattern.compile("^Progress to Level (\\d+):\\s*([\\d.]+)%$");

    // --- Soulbound markers
    private static final String SOULBOUND_TEXT       = "Soulbound";
    private static final String COOP_SOULBOUND_TEXT  = "Co-op Soulbound";

    // --- Enchant description line pattern: "EnchantName Level" or
    //     "EnchantName Level Number" (e.g. "Compact VII 146,962")
    //     Also matches multi-word enchants like "Depth Strider III"
    private static final Pattern ENCHANT_LINE_PATTERN =
            Pattern.compile("^[A-Z][a-z]+(?: [A-Z][a-z]+)*\\s+(?:I{1,3}V?|VI{0,3}|IX|X|[1-9]\\d*)(?:\\s+[\\d,]+)?$");

    // --- Enchant description continuation: "Increases damage dealt..."
    //     Lines that start lowercase or describe what an enchant does
    private static final Pattern ENCHANT_DESC_CONTINUATION =
            Pattern.compile("^(?:[a-z]|Increases|Reduces|Grants|Gain|Heals|Adds).*");

    // --- XP progress bar line (whitespace-heavy, e.g. "                          279,974.3/861.7k")
    private static final Pattern XP_BAR_LINE_PATTERN =
            Pattern.compile("^\\s+[\\d,.]+/[\\d,.]+[kKmM]?$");

    // --- "MAX LEVEL" or "▸ 34,068,339 XP" lines
    private static final Pattern MAX_LEVEL_PATTERN =
            Pattern.compile("^(?:MAX LEVEL|▸\\s*[\\d,]+\\s*XP)$");

    // --- Pet interaction lines
    private static final Pattern PET_ACTION_PATTERN =
            Pattern.compile("^(?:Right-click to add|Left-click to summon|Shift Left-click|Right-click to convert|Can be upgraded at).*");

    /**
     * Classifies all tooltip lines into typed sections.
     *
     * @param lines         raw tooltip lines (index 0 is the item name, already in displayName)
     * @param nbtEnchantIds set of enchant IDs from NBT (lowercase, underscored).
     *                      Used to identify and consume vanilla enchant description blocks.
     * @return populated sections object
     */
    public ParsedSections classify(List<String> lines, Set<String> nbtEnchantIds) {
        ParsedSections out = new ParsedSections();

        ParseState state = ParseState.STATS;
        AbilityBuilder currentAbility = null;
        StringBuilder reforgeBonusBuffer = null;
        boolean insideEnchantDescBlock = false;

        // Skip line 0 (display name — already parsed from ItemStack.getHoverName)
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            // --- Empty line: reset transient states ---
            if (trimmed.isEmpty()) {
                if (currentAbility != null) {
                    out.abilities.add(currentAbility.build());
                    currentAbility = null;
                }
                if (reforgeBonusBuffer != null) {
                    out.reforgeBonusText = reforgeBonusBuffer.toString().strip();
                    reforgeBonusBuffer = null;
                    state = ParseState.LORE;
                }
                insideEnchantDescBlock = false;
                continue;
            }

            // --- Soulbound (with * decorators stripped) ---
            String stripped = trimmed.replace("*", "").trim();
            if (stripped.equals(COOP_SOULBOUND_TEXT)) {
                out.isCoopSoulbound = true;
                continue;
            }
            if (stripped.equals(SOULBOUND_TEXT)) {
                out.isSoulbound = true;
                continue;
            }

            // --- Last line: rarity (handles "a MYTHIC SWORD a" format) ---
            if (i == lines.size() - 1) {
                Rarity r = Rarity.fromTooltipLine(trimmed);
                if (r != null) {
                    out.rarity = r;
                    continue;
                }
            }

            // --- Gear Score ---
            Matcher gsm = GEAR_SCORE_PATTERN.matcher(trimmed);
            if (gsm.matches()) {
                out.gearScore = parseIntClean(gsm.group(1));
                if (gsm.group(2) != null) out.scaledGearScore = parseIntClean(gsm.group(2));
                continue;
            }

            // --- Gem slot indicator line ---
            if (GEM_SLOT_LINE_PATTERN.matcher(trimmed).matches()
                    && trimmed.contains("[") && trimmed.length() < 60) {
                out.gemSlotLine = trimmed;
                continue;
            }

            // --- Ability header ---
            Matcher am = ABILITY_PATTERN.matcher(trimmed);
            if (am.matches()) {
                if (currentAbility != null) out.abilities.add(currentAbility.build());
                currentAbility = new AbilityBuilder(am.group(1), am.group(2));
                state = ParseState.ABILITY;
                insideEnchantDescBlock = false;
                continue;
            }

            // --- Inside an active ability block ---
            if (state == ParseState.ABILITY && currentAbility != null) {
                Matcher mm = MANA_PATTERN.matcher(trimmed);
                Matcher cm = COOLDOWN_PATTERN.matcher(trimmed);
                boolean hasMana     = mm.find();
                boolean hasCooldown = cm.find();

                if (hasMana) currentAbility.manaCost = mm.group(1);
                if (hasCooldown) currentAbility.cooldown = cm.group(1);

                if (hasMana || hasCooldown) {
                    // This is a meta line — consume it, don't add as desc
                    // Only close the ability if this is a cooldown-only or final meta line
                    // Don't close yet — wait for empty line to close naturally
                } else {
                    currentAbility.descLines.add(trimmed);
                }
                continue;
            }

            // --- Reforge bonus header ---
            if (REFORGE_BONUS_PATTERN.matcher(trimmed).matches()) {
                reforgeBonusBuffer = new StringBuilder(trimmed).append("\n");
                state = ParseState.REFORGE_BONUS;
                insideEnchantDescBlock = false;
                continue;
            }

            // --- Inside reforge bonus block ---
            if (state == ParseState.REFORGE_BONUS && reforgeBonusBuffer != null) {
                reforgeBonusBuffer.append(trimmed).append("\n");
                continue;
            }

            // --- Progress line (pets) ---
            if (PROGRESS_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            // --- XP bar numeric line (pets) ---
            if (XP_BAR_LINE_PATTERN.matcher(line).matches()) {
                continue;
            }

            // --- MAX LEVEL / XP total lines (pets) ---
            if (MAX_LEVEL_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            // --- Pet action lines ---
            if (PET_ACTION_PATTERN.matcher(trimmed).matches()) {
                out.loreLines.add(trimmed);
                continue;
            }

            if (!nbtEnchantIds.isEmpty() && isEnchantHeader(trimmed, nbtEnchantIds)) {
                insideEnchantDescBlock = true;
                // Consume: don't add to unknownLines
                continue;
            }
            if (insideEnchantDescBlock) {
                // Consume continuation lines of enchant description
                if (ENCHANT_DESC_CONTINUATION.matcher(trimmed).matches()
                        || trimmed.endsWith(".")
                        || trimmed.endsWith("%")
                        || trimmed.endsWith("mobs by 30%")
                        || trimmed.matches(".*\\d+%?\\.?$")) {
                    continue;
                }
                // Not a continuation — fall through
                insideEnchantDescBlock = false;
            }

            // --- Stat line ---
            Matcher sm = STAT_PATTERN.matcher(trimmed);
            if (sm.matches() && state == ParseState.STATS) {
                StatEntry entry = buildStatEntry(sm, trimmed);
                if (entry != null) {
                    out.stats.add(entry);
                    continue;
                }
            }

            // --- Lore / description (italic lines, flavour text) ---
            if (state == ParseState.LORE || isLoreLine(trimmed)) {
                out.loreLines.add(trimmed);
                state = ParseState.LORE;
                continue;
            }

            // --- Unknown: preserve verbatim ---
            out.unknownLines.add(trimmed);
        }

        // Flush any open ability block at end of lines
        if (currentAbility != null) {
            out.abilities.add(currentAbility.build());
        }
        if (reforgeBonusBuffer != null) {
            out.reforgeBonusText = reforgeBonusBuffer.toString().strip();
        }

        return out;
    }

    /**
     * Overload for backward compatibility — calls with empty enchant set.
     */
    public ParsedSections classify(List<String> lines) {
        return classify(lines, Set.of());
    }

    // --- Private helpers ---

    /**
     * Checks if a line is an enchant header by matching against known NBT enchant IDs.
     * E.g., for NBT key "growth", matches "Growth V" or "Growth V".
     * Also matches "Compact VII 146,962" (with counter).
     */
    private boolean isEnchantHeader(String trimmed, Set<String> nbtEnchantIds) {
        // Quick structural check: must match "Word(s) RomanOrNumber [OptionalNumber]"
        if (!ENCHANT_LINE_PATTERN.matcher(trimmed).matches()) return false;

        // Extract the enchant name (everything before the Roman numeral / level)
        String normalized = trimmed.replaceAll("\\s+\\d[\\d,]*$", "")  // strip trailing counter
                .replaceAll("\\s+(?:I{1,3}V?|VI{0,3}|IX|X|\\d+)$", "") // strip level
                .trim()
                .toLowerCase()
                .replace(' ', '_');

        // Also try "ultimate_" prefix
        return nbtEnchantIds.contains(normalized)
                || nbtEnchantIds.contains("ultimate_" + normalized);
    }

    @Nullable
    private StatEntry buildStatEntry(Matcher sm, String rawLine) {
        String name = sm.group(1).trim();
        String baseRaw = sm.group(2).replace(",", "").replace("%", "");
        double base;
        try {
            base = Double.parseDouble(baseRaw);
        } catch (NumberFormatException e) {
            return null;
        }

        List<Double> bonuses = new ArrayList<>();
        String remainder = sm.group(3);
        if (remainder != null) {
            Matcher bm = BONUS_PATTERN.matcher(remainder);
            while (bm.find()) {
                String bonusRaw = bm.group(1).replace(",", "").replace("%", "");
                try {
                    bonuses.add(Double.parseDouble(bonusRaw));
                } catch (NumberFormatException ignored) {}
            }
        }

        return new StatEntry(name, base, Collections.unmodifiableList(bonuses), rawLine);
    }

    /** Lines that are clearly lore: italic, start with "That thing", etc. */
    private boolean isLoreLine(String line) {
        return false;
    }

    private int parseIntClean(String s) {
        return Integer.parseInt(s.replace(",", ""));
    }

    // --- State machine ---

    private enum ParseState {
        STATS, ABILITY, REFORGE_BONUS, LORE
    }

    // --- Builder for ability blocks ---

    private static final class AbilityBuilder {
        final String name;
        @Nullable final String trigger;
        final List<String> descLines = new ArrayList<>();
        @Nullable String manaCost;
        @Nullable String cooldown;

        AbilityBuilder(String name, @Nullable String trigger) {
            this.name = name;
            this.trigger = trigger;
        }

        AbilityEntry build() {
            return new AbilityEntry(name, trigger,
                    List.copyOf(descLines),
                    manaCost, cooldown);
        }
    }

    /** Mutable aggregate populated by the classifier, consumed by the parser. */
    public static final class ParsedSections {
        public @Nullable Rarity rarity;
        public @Nullable Integer gearScore;
        public @Nullable Integer scaledGearScore;
        public @Nullable String gemSlotLine;
        public @Nullable String reforgeBonusText;
        public boolean isSoulbound;
        public boolean isCoopSoulbound;
        public final List<StatEntry> stats = new ArrayList<>();
        public final List<AbilityEntry> abilities = new ArrayList<>();
        public final List<String> loreLines = new ArrayList<>();
        public final List<String> unknownLines = new ArrayList<>();
    }
}