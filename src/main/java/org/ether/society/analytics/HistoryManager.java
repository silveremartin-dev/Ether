package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;


import java.util.ArrayList;
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

    private int maxSnapshots = 2000;

    public int getMaxSnapshots() {
        return maxSnapshots;
    }

    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = Math.max(10, maxSnapshots);
    }

    /**
     * Capture a full world state snapshot for replay (high-capacity buffer with smart decimation).
     */
    public synchronized void captureWorldSnapshot(H3SimulationEngine engine) {
        if (engine.getCells() == null || engine.getCells().isEmpty()) return;
        long tickIndex = engine.getTimeManager().getTotalTicks();
        
        List<H3Cell> snapshot = engine.getCells().parallelStream()
            .map(H3Cell::snapshot)
            .collect(Collectors.toList());
            
        // If exceeding max snapshots, thin out older snapshots by removing alternate entries in the first half
        if (worldSnapshots.size() >= maxSnapshots) {
            List<Long> keys = new ArrayList<>(worldSnapshots.keySet());
            int half = keys.size() / 2;
            for (int i = 1; i < half; i += 2) {
                worldSnapshots.remove(keys.get(i));
            }
            if (worldSnapshots.size() >= maxSnapshots) {
                worldSnapshots.pollFirstEntry();
            }
        }
        worldSnapshots.put(tickIndex, snapshot);
    }

    public synchronized List<H3Cell> getWorldSnapshot(long tickIndex) {
        return worldSnapshots.get(tickIndex);
    }
    
    public synchronized NavigableMap<Long, List<H3Cell>> getWorldSnapshots() {
        return worldSnapshots;
    }

    public synchronized int getSnapshotCount() {
        return worldSnapshots.size();
    }

    public synchronized Long getMinTick() {
        return worldSnapshots.isEmpty() ? null : worldSnapshots.firstKey();
    }

    public synchronized Long getMaxTick() {
        return worldSnapshots.isEmpty() ? null : worldSnapshots.lastKey();
    }

    public synchronized Long getTickByIndex(int index) {
        if (worldSnapshots.isEmpty() || index < 0 || index >= worldSnapshots.size()) return null;
        return new ArrayList<>(worldSnapshots.keySet()).get(index);
    }

    public synchronized List<H3Cell> getSnapshotByIndex(int index) {
        Long tick = getTickByIndex(index);
        return tick != null ? worldSnapshots.get(tick) : null;
    }

    public synchronized List<H3Cell> getNearestSnapshot(long targetTick) {
        if (worldSnapshots.isEmpty()) return null;
        Long floor = worldSnapshots.floorKey(targetTick);
        Long ceiling = worldSnapshots.ceilingKey(targetTick);
        if (floor == null) return worldSnapshots.get(ceiling);
        if (ceiling == null) return worldSnapshots.get(floor);
        return (targetTick - floor <= ceiling - targetTick) ? worldSnapshots.get(floor) : worldSnapshots.get(ceiling);
    }

    public synchronized void truncateAfter(int year, int month, long tick) {
        history.truncateAfter(year, month);
        worldSnapshots.tailMap(tick, false).clear();
    }

    public synchronized void reset() {
        history.clear();
        worldSnapshots.clear();
    }
}
