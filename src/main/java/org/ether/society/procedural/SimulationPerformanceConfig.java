/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 2.9.5
 */
public class SimulationPerformanceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Enforce strict bit-identical determinism (disables all approximations) */
    private boolean strictDeterminism = true;

    /** Enable sparse cell skipping (skip updates for empty deep ocean/desert cells without events) */
    private boolean enableSparseCellSkipping = false;

    /** Enable multi-frequency climate ticks (e.g. run climate diffusion every N ticks instead of every tick) */
    private boolean enableMultiFreqClimateTicks = false;
    private int climateTickFrequency = 5;

    /** Enable parallel stream execution (may introduce non-deterministic floating-point summation order) */
    private boolean enableParallelExecution = false;

    /** Enable downwind spatial range truncation in dust storm & pollution dispersion */
    private boolean enableSpatialRangeTruncation = false;

    public SimulationPerformanceConfig() {}

    public SimulationPerformanceConfig(boolean strictDeterminism) {
        setStrictDeterminism(strictDeterminism);
    }

    public boolean isStrictDeterminism() {
        return strictDeterminism;
    }

    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.enableSparseCellSkipping = false;
            this.enableMultiFreqClimateTicks = false;
            this.enableParallelExecution = false;
            this.enableSpatialRangeTruncation = false;
        }
    }

    public boolean isEnableSparseCellSkipping() {
        return enableSparseCellSkipping;
    }

    public void setEnableSparseCellSkipping(boolean enableSparseCellSkipping) {
        this.enableSparseCellSkipping = enableSparseCellSkipping;
        if (enableSparseCellSkipping) {
            this.strictDeterminism = false;
        }
    }

    public boolean isEnableMultiFreqClimateTicks() {
        return enableMultiFreqClimateTicks;
    }

    public void setEnableMultiFreqClimateTicks(boolean enableMultiFreqClimateTicks) {
        this.enableMultiFreqClimateTicks = enableMultiFreqClimateTicks;
        if (enableMultiFreqClimateTicks) {
            this.strictDeterminism = false;
        }
    }

    public int getClimateTickFrequency() {
        return climateTickFrequency;
    }

    public void setClimateTickFrequency(int climateTickFrequency) {
        this.climateTickFrequency = Math.max(1, climateTickFrequency);
    }

    public boolean isEnableParallelExecution() {
        return enableParallelExecution;
    }

    public void setEnableParallelExecution(boolean enableParallelExecution) {
        this.enableParallelExecution = enableParallelExecution;
        if (enableParallelExecution) {
            this.strictDeterminism = false;
        }
    }

    public boolean isEnableSpatialRangeTruncation() {
        return enableSpatialRangeTruncation;
    }

    public void setEnableSpatialRangeTruncation(boolean enableSpatialRangeTruncation) {
        this.enableSpatialRangeTruncation = enableSpatialRangeTruncation;
        if (enableSpatialRangeTruncation) {
            this.strictDeterminism = false;
        }
    }
}
