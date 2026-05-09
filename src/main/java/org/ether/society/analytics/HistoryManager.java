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
        org.ether.society.core.dod.WorldBuffer world = engine.getWorldBuffer();

        if (world == null) return;

        long totalPop = 0;
        double totalFood = 0;
        double totalCapital = 0;
        double sumLifespan = 0;
        double sumTech = 0;
        long populatedCount = 0;

        float[] pop = world.getBiomassHuman();
        float[] food = world.getFoodResource();
        float[] capital = world.getResourceCapital();
        float[] lifespan = world.getLifespan();
        float[] tech = world.getTechnologyLevel();

        for (int i = 0; i < world.getCapacity(); i++) {
            totalPop += (long)pop[i];
            totalFood += food[i];
            totalCapital += capital[i];

            if (pop[i] > 0.1f) {
                sumLifespan += lifespan[i];
                sumTech += tech[i];
                populatedCount++;
            }
        }

        double avgLifespan = (populatedCount > 0) ? sumLifespan / populatedCount : 0;
        double avgTech = (populatedCount > 0) ? sumTech / populatedCount : 0;
        double dummyGini = 0.0;

        HistorySnapshot snapshot = new HistorySnapshot(
                year, month, totalPop, totalFood, totalCapital, avgLifespan, dummyGini, avgTech);

        history.addSnapshot(snapshot);
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
