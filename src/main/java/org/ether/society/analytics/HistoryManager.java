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
    private final NavigableMap<Long, List<H3Cell>> worldSnapshots = new TreeMap<>();

    public SimulationHistory getHistory() {
        return history;
    }

    /**
     * Capture a snapshot of the current engine state.
     */
     public void captureSnapshot(H3SimulationEngine engine) {
         if (engine == null) return;
         int year = engine.getTimeManager().getCurrentYear();
         int month = engine.getTimeManager().getCurrentMonth();

         long totalPop = engine.getTotalPopulation();
         double totalFood = engine.getTotalFood();
         double totalWealth = engine.getBuiltCapitalTotal();
         double avgLifespan = engine.getCurrentLifeExpectancy();
         double globalGini = engine.getCurrentGini();
         double avgTech = engine.getAverageTechnology();

         double energyCaptured = engine.getEnergyCaptured();
         double kardashevScale = engine.getKardashevScale();
         double happinessIndex = engine.getHappinessIndex();
         double conflictLevel = engine.getConflictLevel();
         double gdpTotal = engine.getCurrentGDP();
         double divisionLabor = engine.getDivisionOfLaborIndex();
         double systemComplexity = engine.getSystemComplexityIndex();
         double collectiveMemory = engine.getCollectiveMemoryStock();
         double carbonFootprint = engine.getCarbonFootprint();
         double collapseVulnerability = engine.getCollapseVulnerability();
         double tps = engine.getCurrentTPS();
         double resourceDepletion = engine.getResourceDepletionRate();
         double naturalBiomass = engine.getTotalBiomassNatural();
         double potableWater = engine.getPotableWaterTotal();
         double fertilityRate = engine.getCurrentFertility();
         int cityStates = engine.getCityStatesCount();
         double eliteOverproduction = engine.getEliteOverproductionIndex();

         HistorySnapshot snapshot = new HistorySnapshot(
                 year, month, totalPop, totalFood, totalWealth, avgLifespan, globalGini, avgTech,
                 energyCaptured, kardashevScale, happinessIndex, conflictLevel, gdpTotal, divisionLabor,
                 systemComplexity, collectiveMemory, carbonFootprint, collapseVulnerability, tps,
                 resourceDepletion, naturalBiomass, potableWater, fertilityRate, cityStates, eliteOverproduction
         );

         history.addSnapshot(snapshot);
     }

    /**
     * Capture a full world state snapshot for replay (bounded ring buffer).
     */
    public void captureWorldSnapshot(H3SimulationEngine engine) {
        if (engine.getCells() == null || engine.getCells().isEmpty()) return;
        long tickIndex = engine.getTimeManager().getTotalTicks();
        
        List<H3Cell> snapshot = engine.getCells().parallelStream()
            .map(H3Cell::snapshot)
            .collect(Collectors.toList());
            
        // Ring buffer: keep max 24 snapshots in memory to prevent excessive RAM pressure
        if (worldSnapshots.size() >= 24) {
            worldSnapshots.pollFirstEntry();
        }
        worldSnapshots.put(tickIndex, snapshot);
    }

    public List<H3Cell> getWorldSnapshot(long tickIndex) {
        return worldSnapshots.get(tickIndex);
    }
    
    public NavigableMap<Long, List<H3Cell>> getWorldSnapshots() {
        return worldSnapshots;
    }

    public void truncateAfter(int year, int month, long tick) {
        history.truncateAfter(year, month);
        worldSnapshots.tailMap(tick, false).clear();
    }

    public void reset() {
        history.clear();
        worldSnapshots.clear();
    }
}
