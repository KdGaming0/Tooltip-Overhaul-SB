package com.kd_gaming1.tooltipoverhaulsb.parser;

import com.kd_gaming1.tooltipoverhaulsb.data.AbilityEntry;
import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;
import com.kd_gaming1.tooltipoverhaulsb.data.StatEntry;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * Classifies all tooltip lines into typed sections.
     *
     * @param lines raw tooltip lines (index 0 is the item name, already in displayName)
     * @return populated sections object
     */
    public ParsedSections classify(List<String> lines) {
        ParsedSections out = new ParsedSections();

        ParseState state = ParseState.STATS;
        AbilityBuilder currentAbility = null;
        StringBuilder reforgeBonusBuffer = null;

        // Skip line 0 (display name — already parsed from ItemStack.getHoverName)
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            // --- Empty line: reset transient ability/reforge states ---
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
                continue;
            }

            // --- Soulbound ---
            if (trimmed.equals(COOP_SOULBOUND_TEXT)) {
                out.isCoopSoulbound = true;
                continue;
            }
            if (trimmed.equals(SOULBOUND_TEXT)) {
                out.isSoulbound = true;
                continue;
            }

            // --- Last line: rarity ---
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
                continue;
            }

            // --- Inside an active ability block ---
            if (state == ParseState.ABILITY && currentAbility != null) {
                Matcher mm = MANA_PATTERN.matcher(trimmed);
                Matcher cm = COOLDOWN_PATTERN.matcher(trimmed);
                if (mm.find()) currentAbility.manaCost = mm.group(1);
                if (cm.find()) currentAbility.cooldown = cm.group(1);
                if (mm.find() || cm.find()) {
                    out.abilities.add(currentAbility.build());
                    currentAbility = null;
                    state = ParseState.LORE;
                } else {
                    currentAbility.descLines.add(trimmed);
                }
                continue;
            }

            // --- Reforge bonus header ---
            if (REFORGE_BONUS_PATTERN.matcher(trimmed).matches()) {
                reforgeBonusBuffer = new StringBuilder(trimmed).append("\n");
                state = ParseState.REFORGE_BONUS;
                continue;
            }

            // --- Inside reforge bonus block ---
            if (state == ParseState.REFORGE_BONUS && reforgeBonusBuffer != null) {
                reforgeBonusBuffer.append(trimmed).append("\n");
                continue;
            }

            // --- Progress line (pets) ---
            Matcher pm = PROGRESS_PATTERN.matcher(trimmed);
            if (pm.matches()) {
                // Parsed by PetLevelCalculator — skip to avoid double-processing
                continue;
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
            // We detect lore heuristically: once stats/abilities are done,
            // remaining non-matching lines are lore.
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

    // --- Private helpers ---

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
        // Lore lines are typically short flavour text or descriptions
        // starting with a non-stat character — no perfect heuristic exists,
        // so we fall back to the state machine above.
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