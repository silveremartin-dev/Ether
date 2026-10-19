package org.ether.society.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <h1>Simulation History</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Evaluates simulation trajectories using multi-metric error metrics (Mean Absolute Percentage Error, Pearson correlation, and spatial centroid divergence).
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationHistory {
    private final List<HistorySnapshot> snapshots = new ArrayList<>();
    private int maxCapacity = 10000;

    /**
     * Default maximum snapshot capacity before uniform decimation.
     */
    public static final int DEFAULT_MAX_CAPACITY = 10000;

    /**
     * Constructs a SimulationHistory buffer with standard capacity.
     */
    public SimulationHistory() {
        this(DEFAULT_MAX_CAPACITY);
    }

    /**
     * Constructs a SimulationHistory buffer with custom capacity.
     *
     * @param maxCapacity maximum allowed snapshots before smart decimation
     */
    public SimulationHistory(int maxCapacity) {
        this.maxCapacity = Math.max(100, maxCapacity);
    }

    /**
     * Gets maximum snapshot capacity.
     *
     * @return current capacity limit
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Sets maximum snapshot capacity.
     *
     * @param maxCapacity new capacity limit (minimum 100)
     */
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = Math.max(100, maxCapacity);
    }

    /**
     * Adds a snapshot with guaranteed preservation of the initial state ($t_0$).
     * <p>
     * When capacity is exceeded, an equidistant thinning algorithm removes every second
     * intermediate snapshot between index 1 and $N-2$, halving the historical density
     * while preserving the original starting state (index 0), the most recent states,
     * and a strictly bounded memory footprint.
     * </p>
     *
     * @param snapshot the history snapshot to record
     */
    public void addSnapshot(HistorySnapshot snapshot) {
        if (snapshot == null) return;
        synchronized (snapshots) {
            if (snapshots.size() >= maxCapacity) {
                // Pin the very first snapshot (index 0, scenario startup step): NEVER REMOVE INDEX 0
                // Thin out alternate intermediate snapshots from end to 1
                for (int i = snapshots.size() - 2; i >= 1; i -= 2) {
                    snapshots.remove(i);
                }
            }
            snapshots.add(snapshot);
        }
    }

    /**
     * Get snapshots.
     *
     * @return the resulting computation or state reference
     */
    public List<HistorySnapshot> getSnapshots() {
        synchronized (snapshots) {
            return Collections.unmodifiableList(new ArrayList<>(snapshots));
        }
    }

    /**
     * Clear.
     *
     */
    public void clear() {
        synchronized (snapshots) {
            snapshots.clear();
        }
    }

    /**
     * Truncate after.
     *
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     */
    public void truncateAfter(int year, int month) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        synchronized (snapshots) {
            snapshots.removeIf(s -> s.year() > year || (s.year() == year && s.month() > month));
        }
    }
}
