package org.ether.society.analytics;

/**
 * A snapshot of global simulation statistics at a specific point in time.
 */
public record HistorySnapshot(
        int year,
        int month,
        long totalPopulation,
        double totalFood,
        double totalWealth, // e.g. from resources or capital
        double avgLifespan,
        double globalGini,
        double avgTechnology) {
}
