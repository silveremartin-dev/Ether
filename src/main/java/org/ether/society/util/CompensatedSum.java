/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

/**
 * High-Precision Kahan-Neumaier Compensated Summation Accumulator.
 * Prevents floating-point precision loss and catastrophic cancellation
 * when aggregating massive state variables (Biomass, Population, Carbon, Energy)
 * over hundreds of thousands of simulation years.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public final class CompensatedSum {

    private double sum = 0.0;
    private double compensation = 0.0;

    public CompensatedSum() {}

    public CompensatedSum(double initialValue) {
        this.sum = initialValue;
    }

    /**
     * Adds a value using Neumaier's algorithm (handles both small and large additions).
     */
    public void add(double value) {
        double t = sum + value;
        if (Math.abs(sum) >= Math.abs(value)) {
            compensation += (sum - t) + value;
        } else {
            compensation += (value - t) + sum;
        }
        sum = t;
    }

    /**
     * Returns the exact compensated sum.
     */
    public double get() {
        return sum + compensation;
    }

    /**
     * Resets the accumulator.
     */
    public void reset() {
        this.sum = 0.0;
        this.compensation = 0.0;
    }

    /**
     * Static utility to sum an array of doubles with Kahan-Neumaier compensation.
     */
    public static double sum(double[] values) {
        if (values == null || values.length == 0) return 0.0;
        double s = 0.0;
        double c = 0.0;
        for (double v : values) {
            double t = s + v;
            if (Math.abs(s) >= Math.abs(v)) {
                c += (s - t) + v;
            } else {
                c += (v - t) + s;
            }
            s = t;
        }
        return s + c;
    }
}
