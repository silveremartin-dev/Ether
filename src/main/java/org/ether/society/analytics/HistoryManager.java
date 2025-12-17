package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages the collection of historical data from the simulation.
 */
public class HistoryManager {
    private static final Logger logger = LoggerFactory.getLogger(HistoryManager.class);

    private final SimulationHistory history = new SimulationHistory();

    // Config: How often to capture? Every month might be too much for long runs.
    // Let's capture every tick for now, or maybe filtering in UI.
    // Ideally capture every month or year.

    public SimulationHistory getHistory() {
        return history;
    }

    /**
     * Capture a snapshot of the current engine state.
     */
    public void captureSnapshot(H3SimulationEngine engine) {
        int year = engine.getTimeManager().getCurrentYear();
        int month = engine.getTimeManager().getCurrentMonth();
        List<H3Cell> cells = engine.getCells();

        long totalPop = 0;
        double totalFood = 0;
        double totalCapital = 0;
        double sumLifespan = 0;
        double sumTech = 0;

        // Naive Gini Calc requires list of wealths, simplified global accumulation for
        // now
        // For MVP, just tracking basic aggregates.

        long populatedCount = 0;

        for (H3Cell c : cells) {
            totalPop += c.getPopulation();
            totalFood += c.getFoodResource();
            totalCapital += c.getResourceCapital();

            if (c.getPopulation() > 0) {
                sumLifespan += c.getLifespan();
                sumTech += c.getTechnologyLevel();
                populatedCount++;
            }
        }

        double avgLifespan = (populatedCount > 0) ? sumLifespan / populatedCount : 0;
        double avgTech = (populatedCount > 0) ? sumTech / populatedCount : 0;

        // Placeholder Gini (actual calculation is expensive O(N log N) or needs
        // histogram)
        // We'll skip Gini for this MVP snapshot to keep tick fast.
        double dummyGini = 0.0;

        HistorySnapshot snapshot = new HistorySnapshot(
                year, month, totalPop, totalFood, totalCapital, avgLifespan, dummyGini, avgTech);

        history.addSnapshot(snapshot);
        // logger.debug("Captured history snapshot: Year {}", year);
    }

    public void reset() {
        history.clear();
    }
}
