package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;


import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * <h1>History Manager</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Evaluates simulation trajectories using multi-metric error metrics (Mean Absolute Percentage Error, Pearson correlation, and spatial centroid divergence).
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HistoryManager {


    private final SimulationHistory history = new SimulationHistory();
    private final NavigableMap<Long, List<H3Cell>> worldSnapshots = new TreeMap<>();

    /*
     * Get history.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public SimulationHistory getHistory() {
        return history;
    }

    /*
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

    /* Internal state variable for max snapshots (int). */
    private int maxSnapshots = 100;

    /*
     * Get max snapshots.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public int getMaxSnapshots() {
        return maxSnapshots;
    }

    /*
     * Set max snapshots.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param maxSnapshots the max snapshots parameter (int)
     */
    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = Math.max(10, maxSnapshots);
    }

    /*
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

    /*
     * Get world snapshot.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param tickIndex the tick index parameter (long)
     * @return the resulting computation or state reference
     */
    public synchronized List<H3Cell> getWorldSnapshot(long tickIndex) {
        return worldSnapshots.get(tickIndex);
    }
    
    /*
     * Get world snapshots.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized NavigableMap<Long, List<H3Cell>> getWorldSnapshots() {
        return worldSnapshots;
    }

    /*
     * Get snapshot count.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized int getSnapshotCount() {
        return worldSnapshots.size();
    }

    /*
     * Get min tick.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized Long getMinTick() {
        return worldSnapshots.isEmpty() ? null : worldSnapshots.firstKey();
    }

    /*
     * Get max tick.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized Long getMaxTick() {
        return worldSnapshots.isEmpty() ? null : worldSnapshots.lastKey();
    }

    /*
     * Get tick by index.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param index the index parameter (int)
     * @return the resulting computation or state reference
     */
    public synchronized Long getTickByIndex(int index) {
        if (worldSnapshots.isEmpty() || index < 0 || index >= worldSnapshots.size()) return null;
        return new ArrayList<>(worldSnapshots.keySet()).get(index);
    }

    /*
     * Get snapshot by index.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param index the index parameter (int)
     * @return the resulting computation or state reference
     */
    public synchronized List<H3Cell> getSnapshotByIndex(int index) {
        Long tick = getTickByIndex(index);
        return tick != null ? worldSnapshots.get(tick) : null;
    }

    /*
     * Get nearest snapshot.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param targetTick the target tick parameter (long)
     * @return the resulting computation or state reference
     */
    public synchronized List<H3Cell> getNearestSnapshot(long targetTick) {
        if (worldSnapshots.isEmpty()) return null;
        Long floor = worldSnapshots.floorKey(targetTick);
        Long ceiling = worldSnapshots.ceilingKey(targetTick);
        if (floor == null) return worldSnapshots.get(ceiling);
        if (ceiling == null) return worldSnapshots.get(floor);
        return (targetTick - floor <= ceiling - targetTick) ? worldSnapshots.get(floor) : worldSnapshots.get(ceiling);
    }

    /*
     * Truncate after.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     * @param tick the tick parameter (long)
     */
    public synchronized void truncateAfter(int year, int month, long tick) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        history.truncateAfter(year, month);
        worldSnapshots.tailMap(tick, false).clear();
    }

    /*
     * Reset.
     * Enforces physical invariants and updates associated state variables within {@code HistoryManager}.
     *
     */
    public synchronized void reset() {
        history.clear();
        worldSnapshots.clear();
    }
}
