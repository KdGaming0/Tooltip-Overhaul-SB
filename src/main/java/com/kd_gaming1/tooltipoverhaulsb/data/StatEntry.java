package com.kd_gaming1.tooltipoverhaulsb.data;

import java.util.List;

/**
 * A single stat line parsed from the tooltip.
 *
 * <p>Example tooltip line: {@code "Mining Speed: +2,115 (+240) (+75)"}
 * <ul>
 *   <li>{@code name} = "Mining Speed"</li>
 *   <li>{@code baseValue} = 2115.0</li>
 *   <li>{@code bonusValues} = [240.0, 75.0]</li>
 *   <li>{@code rawLine} = original line, preserved for rendering</li>
 * </ul>
 */
public record StatEntry(
        String name,
        double baseValue,
        List<Double> bonusValues,
        String rawLine
) {
    /** Computed total: base + sum of bonuses. */
    public double totalValue() {
        return baseValue + bonusValues.stream().mapToDouble(Double::doubleValue).sum();
    }
}