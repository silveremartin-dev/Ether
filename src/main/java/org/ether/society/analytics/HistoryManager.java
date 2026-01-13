package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;


import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * Manages the collection of historical data from the simulation.
 */
public class HistoryManager {


    private final SimulationHistory history = new SimulationHistory();
    private final NavigableMap<Integer, List<H3Cell>> worldSnapshots = new TreeMap<>();

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
        // logger.debug("Captured history snapshot: Year {}", year);
    }

    /**
     * Capture a full world state snapshot for replay (expensive, call less frequently).
     */
    public void captureWorldSnapshot(H3SimulationEngine engine) {
        int year = engine.getTimeManager().getCurrentYear();
        
        List<H3Cell> snapshot = engine.getCells().parallelStream()
            .map(H3Cell::snapshot)
            .collect(Collectors.toList());
            
        worldSnapshots.put(year, snapshot);
    }

    public List<H3Cell> getWorldSnapshot(int year) {
        return worldSnapshots.get(year);
    }
    
    public NavigableMap<Integer, List<H3Cell>> getWorldSnapshots() {
        return worldSnapshots;
    }

    public void reset() {
        history.clear();
        worldSnapshots.clear();
    }
}
