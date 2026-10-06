/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.config;

import java.io.Serializable;

/**
 * Simulation Performance & Approximation Configuration Mode.
 * Controls performance shortcuts and heuristic approximations that impact simulation determinism.
 * <p>
 * When <b>strictDeterminism</b> is true, all performance shortcuts (sparse cell skipping,
 * multi-frequency climate ticks, floating-point reordering, downwind range truncation) are disabled,
 * guaranteeing bit-identical simulation evolution trajectories for identical initial conditions.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationPerformanceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /* Enforce strict bit-identical determinism (disables all approximations) */
    private boolean strictDeterminism = true;

    /* Enable sparse cell skipping (skip updates for empty deep ocean/desert cells without events) */
    private boolean enableSparseCellSkipping = false;

    /* Enable ocean macro aggregation (group deep abyssal cells z < -200m) */
    private boolean enableOceanMacroAggregation = false;

    /* Enable exclusive coastal navigation (pathfinding focused on coasts & straits) */
    private boolean enableCoastalNavigationOnly = false;

    /* Enable multi-frequency climate ticks (e.g. run climate diffusion every N ticks instead of every tick) */
    private boolean enableMultiFreqClimateTicks = false;
    /* Internal state variable for climate tick frequency (int). */
    private int climateTickFrequency = 5;

    /* Enable parallel stream execution (may introduce non-deterministic floating-point summation order) */
    private boolean enableParallelExecution = false;

    /* Enable downwind spatial range truncation in dust storm & pollution dispersion */
    private boolean enableSpatialRangeTruncation = false;

    /*
     * Simulation performance config.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     */
    public SimulationPerformanceConfig() {}

    /*
     * Simulation performance config.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param strictDeterminism the strict determinism parameter (boolean)
     */
    public SimulationPerformanceConfig(boolean strictDeterminism) {
        setStrictDeterminism(strictDeterminism);
    }

    /*
     * Is strict determinism.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isStrictDeterminism() {
        return strictDeterminism;
    }

    /*
     * Set strict determinism.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param strictDeterminism the strict determinism parameter (boolean)
     */
    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.enableSparseCellSkipping = false;
            this.enableOceanMacroAggregation = false;
            this.enableCoastalNavigationOnly = false;
            this.enableMultiFreqClimateTicks = false;
            this.enableParallelExecution = false;
            this.enableSpatialRangeTruncation = false;
        }
    }

    /*
     * Is enable sparse cell skipping.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableSparseCellSkipping() {
        return enableSparseCellSkipping;
    }

    /*
     * Is sparse cell skipping.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isSparseCellSkipping() {
        return enableSparseCellSkipping;
    }

    /*
     * Set enable sparse cell skipping.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableSparseCellSkipping the enable sparse cell skipping parameter (boolean)
     */
    public void setEnableSparseCellSkipping(boolean enableSparseCellSkipping) {
        this.enableSparseCellSkipping = enableSparseCellSkipping;
        if (enableSparseCellSkipping) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is enable ocean macro aggregation.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableOceanMacroAggregation() {
        return enableOceanMacroAggregation;
    }

    /*
     * Set enable ocean macro aggregation.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableOceanMacroAggregation the enable ocean macro aggregation parameter (boolean)
     */
    public void setEnableOceanMacroAggregation(boolean enableOceanMacroAggregation) {
        this.enableOceanMacroAggregation = enableOceanMacroAggregation;
        if (enableOceanMacroAggregation) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is enable coastal navigation only.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableCoastalNavigationOnly() {
        return enableCoastalNavigationOnly;
    }

    /*
     * Set enable coastal navigation only.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableCoastalNavigationOnly the enable coastal navigation only parameter (boolean)
     */
    public void setEnableCoastalNavigationOnly(boolean enableCoastalNavigationOnly) {
        this.enableCoastalNavigationOnly = enableCoastalNavigationOnly;
        if (enableCoastalNavigationOnly) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is enable multi freq climate ticks.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableMultiFreqClimateTicks() {
        return enableMultiFreqClimateTicks;
    }

    /*
     * Set enable multi freq climate ticks.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableMultiFreqClimateTicks the enable multi freq climate ticks parameter (boolean)
     */
    public void setEnableMultiFreqClimateTicks(boolean enableMultiFreqClimateTicks) {
        this.enableMultiFreqClimateTicks = enableMultiFreqClimateTicks;
        if (enableMultiFreqClimateTicks) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Get climate tick frequency.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public int getClimateTickFrequency() {
        return climateTickFrequency;
    }

    /*
     * Set climate tick frequency.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param climateTickFrequency the climate tick frequency parameter (int)
     */
    public void setClimateTickFrequency(int climateTickFrequency) {
        this.climateTickFrequency = Math.max(1, climateTickFrequency);
    }

    /*
     * Is enable parallel execution.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableParallelExecution() {
        return enableParallelExecution;
    }

    /*
     * Set enable parallel execution.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableParallelExecution the enable parallel execution parameter (boolean)
     */
    public void setEnableParallelExecution(boolean enableParallelExecution) {
        this.enableParallelExecution = enableParallelExecution;
        if (enableParallelExecution) {
            this.strictDeterminism = false;
        }
    }

    /* Number of parallel execution threads (0 = auto-detect all available CPU cores, 1 = single-threaded deterministic) */
    private int parallelThreadCount = 0;

    /*
     * Is enable spatial range truncation.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEnableSpatialRangeTruncation() {
        return enableSpatialRangeTruncation;
    }

    /*
     * Set enable spatial range truncation.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param enableSpatialRangeTruncation the enable spatial range truncation parameter (boolean)
     */
    public void setEnableSpatialRangeTruncation(boolean enableSpatialRangeTruncation) {
        this.enableSpatialRangeTruncation = enableSpatialRangeTruncation;
        if (enableSpatialRangeTruncation) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Get parallel thread count.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @return the resulting computation or state reference
     */
    public int getParallelThreadCount() {
        return parallelThreadCount;
    }

    /*
     * Set parallel thread count.
     * Enforces physical invariants and updates associated state variables within {@code SimulationPerformanceConfig}.
     *
     * @param parallelThreadCount the parallel thread count parameter (int)
     */
    public void setParallelThreadCount(int parallelThreadCount) {
        this.parallelThreadCount = Math.max(0, parallelThreadCount);
    }
}


