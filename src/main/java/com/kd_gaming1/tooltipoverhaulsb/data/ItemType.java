package com.kd_gaming1.tooltipoverhaulsb.data;

/**
 * Broad structural category of a SkyBlock item.
 * Determines which tooltip sections are expected during parsing.
 */
public enum ItemType {
    WEAPON,
    TOOL,
    ARMOR,
    PET,
    ACCESSORY,
    CONSUMABLE,
    MISC;

    /**
     * Infers the item type from the last tooltip line (rarity + optional type tag)
     * and the Hypixel item ID. The item ID is the primary signal; the tooltip type
     * tag is a fallback.
     *
     * @param hypixelId    value of the "id" key in custom_data, may be null for vanilla
     * @param lastLine     last tooltip line, e.g. "LEGENDARY DUNGEON BOW"
     * @return best-effort item type, never null
     */
    public static ItemType infer(String hypixelId, String lastLine) {
        if ("PET".equals(hypixelId)) return PET;

        String upper = (lastLine == null ? "" : lastLine.toUpperCase());
        if (upper.contains("SWORD") || upper.contains("BOW")
                || upper.contains("FISHING ROD") || upper.contains("WAND")
                || upper.contains("AXE") && upper.contains("DUNGEON")) {
            return WEAPON;
        }
        if (upper.contains("HELMET") || upper.contains("CHESTPLATE")
                || upper.contains("LEGGINGS") || upper.contains("BOOTS")) {
            return ARMOR;
        }
        if (upper.contains("PICKAXE") || upper.contains("DRILL")
                || upper.contains("HOE") || upper.contains("AXE")
                || upper.contains("SHOVEL") || upper.contains("FISHING ROD")) {
            return TOOL;
        }
        if (upper.contains("ACCESSORY") || upper.contains("TALISMAN")) {
            return ACCESSORY;
        }
        if (upper.contains("CONSUMABLE") || upper.contains("POTION")) {
            return CONSUMABLE;
        }
        return MISC;
    }
}