package com.kd_gaming1.tooltipoverhaulsb.data;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A parsed ability block from the tooltip.
 *
 * <p>Abilities appear as:
 * <pre>
 * Ability: Name RIGHT CLICK
 * Description line 1.
 * Description line 2.
 * Mana Cost: 50, Cooldown: 5s
 * </pre>
 */
public record AbilityEntry(
        String name,
        @Nullable String trigger,        // "RIGHT CLICK", "LEFT CLICK", "SNEAK", etc.
        List<String> descriptionLines,
        @Nullable String manaCost,
        @Nullable String cooldown
) {}