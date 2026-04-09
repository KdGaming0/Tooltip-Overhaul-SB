package com.kd_gaming1.tooltipoverhaulsb.resource;

import com.kd_gaming1.tooltipoverhaulsb.data.Rarity;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * All static reference data required by the parsers — hardcoded for performance
 * and stability. No file I/O at runtime.
 *
 * <p>All collections returned by accessors are unmodifiable.
 * This class is a pure utility and cannot be instantiated.
 */
public final class SkyBlockDataResources {

    private SkyBlockDataResources() {}

    // =========================================================================
    // Pet level curve
    // =========================================================================

    /**
     * Global XP thresholds for each level transition.
     * Index 0 = XP required to advance from level 1 → 2.
     * 119 entries total. Each rarity applies an offset into this array.
     */
    private static final int[] GLOBAL_PET_LEVELS = {
            100, 110, 120, 130, 145, 160, 175, 190, 210, 230,         //   0 – 9
            250, 275, 300, 330, 360, 400, 440, 490, 540, 600,         //  10 – 19
            660, 730, 800, 880, 960, 1050, 1150, 1260, 1380, 1510,    //  20 – 29
            1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920, 3160, 3420,//  30 – 39
            3700, 4000, 4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700,//  40 – 49
            9700, 10800, 12000, 13300, 14700, 16200, 17800, 19500, 21300, 23200, //50–59
            25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800, 48400, 52200,//60–69
            56200, 60400, 64800, 69400, 74200, 79200, 84700, 90700, 97200, 104200,//70–79
            111700, 119700, 128200, 137200, 146700, 156700, 167700, 179700, 192700, 206700,//80–89
            221700, 237700, 254700, 272700, 291700, 311700, 333700, 357700, 383700, 411700,//90–99
            441700, 476700, 516700, 561700, 611700,                    // 100–104
            666700, 726700, 791700, 861700, 936700,                    // 105–109
            1016700, 1101700, 1191700, 1286700, 1386700,               // 110–114
            1496700, 1616700, 1746700, 1886700                         // 115–118
    };

    /**
     * Rarity offset applied to the global curve.
     * LEGENDARY and MYTHIC share offset 20.
     */
    private static final Map<Rarity, Integer> RARITY_OFFSETS;

    static {
        Map<Rarity, Integer> m = new EnumMap<>(Rarity.class);
        m.put(Rarity.COMMON,    0);
        m.put(Rarity.UNCOMMON,  6);
        m.put(Rarity.RARE,     11);
        m.put(Rarity.EPIC,     16);
        m.put(Rarity.LEGENDARY, 20);
        m.put(Rarity.MYTHIC,    20);
        RARITY_OFFSETS = Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Custom pet level overrides
    // =========================================================================

    /** Golden Dragon, Jade Dragon, Rose Dragon: 200-level cap, unique XP curve. */
    private static final int[] DRAGON_PET_LEVELS = buildDragonCurve();

    private static int[] buildDragonCurve() {
        // Level 1→2 requires 0 XP, level 2→3 requires 5555, all others 1,886,700.
        // Array length = maxLevel - 1 = 199.
        int[] curve = new int[199];
        curve[0] = 0;
        curve[1] = 5555;
        Arrays.fill(curve, 2, 199, 1_886_700);
        return curve;
    }

    /** Pet types with a custom max level. */
    private static final Map<String, Integer> CUSTOM_MAX_LEVELS;

    static {
        Map<String, Integer> m = new HashMap<>();
        m.put("GOLDEN_DRAGON", 200);
        m.put("JADE_DRAGON",   200);
        m.put("ROSE_DRAGON",   200);
        CUSTOM_MAX_LEVELS = Collections.unmodifiableMap(m);
    }

    /** Pet types with a custom XP curve (non-null → use instead of global curve). */
    private static final Map<String, int[]> CUSTOM_CURVES;

    static {
        Map<String, int[]> m = new HashMap<>();
        m.put("GOLDEN_DRAGON", DRAGON_PET_LEVELS);
        m.put("JADE_DRAGON",   DRAGON_PET_LEVELS);
        m.put("ROSE_DRAGON",   DRAGON_PET_LEVELS);
        CUSTOM_CURVES = Collections.unmodifiableMap(m);
    }

    /**
     * Per-pet rarity offset overrides.
     * BINGO uses offset 0 regardless of rarity.
     */
    private static final Map<String, Map<Rarity, Integer>> CUSTOM_RARITY_OFFSETS;

    static {
        Map<Rarity, Integer> bingoOffsets = new EnumMap<>(Rarity.class);
        for (Rarity r : Rarity.values()) bingoOffsets.put(r, 0);

        Map<String, Map<Rarity, Integer>> m = new HashMap<>();
        m.put("BINGO", Collections.unmodifiableMap(bingoOffsets));
        CUSTOM_RARITY_OFFSETS = Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Pet type → skill category
    // =========================================================================

    private static final Map<String, String> PET_CATEGORIES;

    static {
        Map<String, String> m = new HashMap<>();
        // Mining
        for (String s : new String[]{"ROCK","BAT","MITHRIL_GOLEM","WITHER_SKELETON",
                "SILVERFISH","ENDERMITE","ARMADILLO","BAL","SCATHA","SNAIL","GLACITE_GOLEM"})
            m.put(s, "MINING");
        // Farming
        for (String s : new String[]{"BEE","CHICKEN","PIG","RABBIT","ELEPHANT",
                "MOOSHROOM_COW","SLUG","MOSQUITO","ROSE_DRAGON"})
            m.put(s, "FARMING");
        // Fishing
        for (String s : new String[]{"BLUE_WHALE","DOLPHIN","FLYING_FISH","BABY_YETI",
                "MEGALODON","SQUID","AMMONITE","REINDEER","HERMIT_CRAB","SEAL"})
            m.put(s, "FISHING");
        // Combat
        for (String s : new String[]{"BLACK_CAT","BLAZE","ENDER_DRAGON","ENDERMAN","GHOUL",
                "GOLEM","GRIFFIN","HORSE","HOUND","JERRY","MAGMA_CUBE","PHOENIX","PIGMAN",
                "SKELETON","SKELETON_HORSE","SNOWMAN","SPIDER","SPIRIT","TARANTULA","TURTLE",
                "TIGER","ZOMBIE","WOLF","GRANDMA_WOLF","GOLDEN_DRAGON","RAT","KUUDRA",
                "EERIE","PRECURSOR_DRONE","CROW"})
            m.put(s, "COMBAT");
        // Alchemy
        for (String s : new String[]{"JELLYFISH","SHEEP","PARROT","WITCH"})
            m.put(s, "ALCHEMY");
        // Foraging
        for (String s : new String[]{"MONKEY","GIRAFFE","LION","OCELOT","FROG","JADE_DRAGON"})
            m.put(s, "FORAGING");
        // Other
        m.put("GUARDIAN",  "ENCHANTING");
        m.put("OWL",       "TAMING");
        m.put("BINGO",     "ALL");
        PET_CATEGORIES = Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Gemstone data (tier → rarity → bonus value as double)
    // =========================================================================

    /**
     * Tier names in ascending quality order, matching the JSON keys.
     */
    public static final String[] GEM_TIERS = {"Rough", "Flawed", "Fine", "Flawless", "Perfect"};

    /**
     * Rarity names used as keys in the stats maps.
     */
    public static final String[] GEM_RARITIES = {
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE"
    };

    /**
     * Holds all data for one gemstone type.
     *
     * @param stat          stat name this gem contributes, e.g. "Health"
     * @param symbol        display symbol, e.g. "❤"
     * @param tierStats     tier → (rarity → bonus). Missing rarity → 0.0.
     */
    public record GemstoneData(
            String stat,
            String symbol,
            Map<String, Map<String, Double>> tierStats
    ) {}

    /** gemType (e.g. "Amber") → GemstoneData */
    private static final Map<String, GemstoneData> GEMSTONE_DATA;

    static {
        Map<String, GemstoneData> m = new LinkedHashMap<>();

        // Ruby — Health ❤
        m.put("Ruby", new GemstoneData("Health", "❤", buildTierMap(
                new double[]{1,2,3,4,5,7,7},      // Rough
                new double[]{3,4,5,6,8,10,10},     // Flawed
                new double[]{4,5,6,8,10,14,14},    // Fine
                new double[]{5,7,10,14,18,22,22},  // Flawless
                new double[]{6,9,13,18,24,30,30}   // Perfect
        )));

        // Amethyst — Defense ❈
        m.put("Amethyst", new GemstoneData("Defense", "❈", buildTierMap(
                new double[]{1,2,3,4,5,7,7},
                new double[]{3,4,5,6,8,10,10},
                new double[]{4,5,6,8,10,14,14},
                new double[]{5,7,10,14,18,22,22},
                new double[]{6,9,13,18,24,30,30}
        )));

        // Jade — Mining Fortune ☘
        m.put("Jade", new GemstoneData("Mining Fortune", "☘", buildTierMap(
                new double[]{2,4,6,8,10,12,14},
                new double[]{3,5,7,10,14,18,22},
                new double[]{5,7,10,15,20,25,30},
                new double[]{7,10,15,20,27,35,44},
                new double[]{10,14,20,30,40,50,60}
        )));

        // Sapphire — Intelligence ✎
        m.put("Sapphire", new GemstoneData("Intelligence", "✎", buildTierMap(
                new double[]{2,3,4,5,6,7,7},
                new double[]{5,6,7,8,10,10,10},
                new double[]{7,8,9,10,11,12,12},
                new double[]{10,11,12,14,17,20,20},
                new double[]{12,14,17,20,24,30,30}
        )));

        // Amber — Mining Speed ⸕
        m.put("Amber", new GemstoneData("Mining Speed", "⸕", buildTierMap(
                new double[]{4,8,12,16,20,24,28},
                new double[]{6,10,14,18,24,30,36},
                new double[]{10,14,20,28,36,45,54},
                new double[]{14,20,30,44,58,75,92},
                new double[]{20,28,40,60,80,100,120}
        )));

        // Topaz — Pristine ✧ (only LEGENDARY / MYTHIC, float values)
        m.put("Topaz", new GemstoneData("Pristine", "✧", buildTopazTierMap()));

        // Jasper — Strength ❁ (no DIVINE for some tiers)
        m.put("Jasper", new GemstoneData("Strength", "❁", buildJasperTierMap()));

        // Opal — True Defense ❂ (limited rarity support)
        m.put("Opal", new GemstoneData("True Defense", "❂", buildOpalTierMap()));

        GEMSTONE_DATA = Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Gemstone builder helpers
    // =========================================================================

    /**
     * Builds a tier → rarity → bonus map from parallel double arrays.
     * Row order: Rough, Flawed, Fine, Flawless, Perfect.
     * Column order: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, DIVINE.
     */
    private static Map<String, Map<String, Double>> buildTierMap(
            double[] rough, double[] flawed, double[] fine,
            double[] flawless, double[] perfect) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        double[][] rows = {rough, flawed, fine, flawless, perfect};
        for (int t = 0; t < GEM_TIERS.length; t++) {
            Map<String, Double> rarityMap = new LinkedHashMap<>();
            for (int r = 0; r < GEM_RARITIES.length && r < rows[t].length; r++) {
                rarityMap.put(GEM_RARITIES[r], rows[t][r]);
            }
            result.put(GEM_TIERS[t], Collections.unmodifiableMap(rarityMap));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Topaz only applies to LEGENDARY and MYTHIC. */
    private static Map<String, Map<String, Double>> buildTopazTierMap() {
        // tier → {LEGENDARY, MYTHIC}
        double[][] data = {
                {0.4, 0.5},   // Rough
                {0.8, 0.9},   // Flawed
                {1.2, 1.3},   // Fine
                {1.6, 1.8},   // Flawless
                {2.0, 2.2}    // Perfect
        };
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (int t = 0; t < GEM_TIERS.length; t++) {
            Map<String, Double> rm = new LinkedHashMap<>();
            rm.put("LEGENDARY", data[t][0]);
            rm.put("MYTHIC",    data[t][1]);
            result.put(GEM_TIERS[t], Collections.unmodifiableMap(rm));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Jasper: DIVINE not present on all tiers, MYTHIC absent on some. */
    private static Map<String, Map<String, Double>> buildJasperTierMap() {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        result.put("Rough",    sparseRarityMap(
                "COMMON",1, "UNCOMMON",2, "RARE",3, "EPIC",4, "LEGENDARY",4));
        result.put("Flawed",   sparseRarityMap(
                "COMMON",2, "UNCOMMON",3, "RARE",4, "EPIC",5, "LEGENDARY",5));
        result.put("Fine",     sparseRarityMap(
                "COMMON",3, "UNCOMMON",4, "RARE",5, "EPIC",6, "LEGENDARY",7, "MYTHIC",7));
        result.put("Flawless", sparseRarityMap(
                "COMMON",5, "UNCOMMON",6, "RARE",7, "EPIC",8, "LEGENDARY",10, "MYTHIC",12, "DIVINE",12));
        result.put("Perfect",  sparseRarityMap(
                "COMMON",6, "UNCOMMON",7, "RARE",9, "EPIC",11, "LEGENDARY",13, "MYTHIC",16, "DIVINE",16));

        return Collections.unmodifiableMap(result);
    }

    /** Opal: only RARE+ on lower tiers. */
    private static Map<String, Map<String, Double>> buildOpalTierMap() {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        result.put("Rough",    sparseRarityMap("RARE",1, "EPIC",2, "LEGENDARY",3));
        result.put("Flawed",   sparseRarityMap("RARE",2, "EPIC",3, "LEGENDARY",4));
        result.put("Fine",     sparseRarityMap("RARE",3, "EPIC",4, "LEGENDARY",5));
        result.put("Flawless", sparseRarityMap("RARE",4, "EPIC",5, "LEGENDARY",9));
        result.put("Perfect",  sparseRarityMap(
                "COMMON",5, "UNCOMMON",6, "RARE",7, "EPIC",9, "LEGENDARY",11, "MYTHIC",13));

        return Collections.unmodifiableMap(result);
    }

    /**
     * Builds a rarity → bonus map from alternating key/value pairs.
     * Usage: sparseRarityMap("EPIC", 4, "LEGENDARY", 5)
     */
    private static Map<String, Double> sparseRarityMap(Object... pairs) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            m.put((String) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Public accessors
    // =========================================================================

    /** Returns a defensive copy of the global pet level curve. */
    public static int[] globalPetLevels() {
        return Arrays.copyOf(GLOBAL_PET_LEVELS, GLOBAL_PET_LEVELS.length);
    }

    /**
     * Returns the starting index into {@link #globalPetLevels()} for a given rarity.
     * Returns 0 if the rarity is not mapped.
     */
    public static int globalRarityOffset(Rarity rarity) {
        return RARITY_OFFSETS.getOrDefault(rarity, 0);
    }

    /**
     * Returns the custom level curve for a specific pet type, or null if the
     * pet uses the global curve.
     */
    @Nullable
    public static int[] customPetLevelCurve(String petType) {
        return CUSTOM_CURVES.get(petType);
    }

    /**
     * Returns per-pet rarity offset overrides, or null if the pet uses global offsets.
     */
    @Nullable
    public static Map<Rarity, Integer> customRarityOffsets(String petType) {
        return CUSTOM_RARITY_OFFSETS.get(petType);
    }

    /** Returns the custom max level for a pet type, or null for the default (100). */
    @Nullable
    public static Integer customPetMaxLevel(String petType) {
        return CUSTOM_MAX_LEVELS.get(petType);
    }

    /** Returns the skill category for a pet type (e.g. "MINING"), or "UNKNOWN". */
    public static String petCategory(String petType) {
        return PET_CATEGORIES.getOrDefault(petType, "UNKNOWN");
    }

    /** Returns the full gemstone data map (gemType → GemstoneData). */
    public static Map<String, GemstoneData> gemstoneData() {
        return GEMSTONE_DATA;
    }

    /** Returns the slot symbol for a gem type, or "?" if unknown. */
    public static String gemSlotSymbol(String gemType) {
        GemstoneData d = GEMSTONE_DATA.get(gemType);
        return d != null ? d.symbol() : "?";
    }

    /** Returns the stat name for a gem type, or "Unknown" if not found. */
    public static String gemStatName(String gemType) {
        GemstoneData d = GEMSTONE_DATA.get(gemType);
        return d != null ? d.stat() : "Unknown";
    }

    /**
     * Looks up the stat bonus for a gem, given gem type, tier, and item rarity.
     *
     * @return the bonus value as a double; 0.0 if any lookup fails.
     */
    public static double gemStatBonus(String gemType, String tier, Rarity rarity) {
        GemstoneData gem = GEMSTONE_DATA.get(gemType);
        if (gem == null) return 0.0;

        Map<String, Double> rarityMap = gem.tierStats().get(tier);
        if (rarityMap == null) return 0.0;

        Double bonus = rarityMap.get(rarity.name());
        return bonus != null ? bonus : 0.0;
    }
}