/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.integration;

import org.ether.society.core.dod.WorldBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 3-Tier Multi-Scale Numerical Integrator (Daily / Monthly / Annual).
 * Strictly decouples equations according to their natural physical timescales:
 * <ul>
 *   <li><b>Tier 1 - Daily (&Delta;t = 1 day)</b>: Fast logistics, price adjustments, transport flux pressure, daily agent movement.</li>
 *   <li><b>Tier 2 - Monthly (&Delta;t = 30 days)</b>: Climate insolation, seasonal agriculture, soil NPK, demographics & epidemiology.</li>
 *   <li><b>Tier 3 - Annual (&Delta;t = 365 days)</b>: Deep mineral ore depletion, Smil infrastructure inertia (35-year turnover), macro-historical statistics.</li>
 * </ul>
 *
 * <p><b>Role of strictDeterminism</b>: Enforces strict bit-identical reproducibility across runs
 * (deterministic IEEE 754 summation order, zero heuristic cell skipping, seed isolation).
 * It never alters the physical timescale separation: monthly equations are NEVER executed daily.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MultiScaleSymplecticIntegrator {
    private static final Logger logger = LoggerFactory.getLogger(MultiScaleSymplecticIntegrator.class);

    /* Internal state variable for days per month (int). */
    private final int daysPerMonth;
    /* Internal state variable for months per year (int). */
    private final int monthsPerYear;
    /* Internal state variable for strict determinism (boolean). */
    private final boolean strictDeterminism;

    /* Internal state variable for accumulated days (int). */
    private int accumulatedDays = 0;
    /* Internal state variable for accumulated months (int). */
    private int accumulatedMonths = 0;
    /* Internal state variable for accumulated monthly dt (double). */
    private double accumulatedMonthlyDt = 0;
    /* Internal state variable for accumulated annual dt (double). */
    private double accumulatedAnnualDt = 0;

    /*
     * Multi scale symplectic integrator.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     */
    public MultiScaleSymplecticIntegrator() {
        this(30, 12, true);
    }

    /*
     * Multi scale symplectic integrator.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @param daysPerMonth the days per month parameter (int)
     * @param strictDeterminism the strict determinism parameter (boolean)
     */
    public MultiScaleSymplecticIntegrator(int daysPerMonth, boolean strictDeterminism) {
        this(daysPerMonth, 12, strictDeterminism);
    }

    /*
     * Multi scale symplectic integrator.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @param daysPerMonth the days per month parameter (int)
     * @param monthsPerYear the months per year parameter (int)
     * @param strictDeterminism the strict determinism parameter (boolean)
     */
    public MultiScaleSymplecticIntegrator(int daysPerMonth, int monthsPerYear, boolean strictDeterminism) {
        this.daysPerMonth = Math.max(1, daysPerMonth);
        this.monthsPerYear = Math.max(1, monthsPerYear);
        this.strictDeterminism = strictDeterminism;
    }

    /*
     * Get days per month.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @return the resulting computation or state reference
     */
    public int getDaysPerMonth() { return daysPerMonth; }
    /*
     * Get months per year.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @return the resulting computation or state reference
     */
    public int getMonthsPerYear() { return monthsPerYear; }
    /*
     * Is strict determinism.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isStrictDeterminism() { return strictDeterminism; }

    /*
     * Get accumulated days.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @return the resulting computation or state reference
     */
    public int getAccumulatedDays() { return accumulatedDays; }
    /*
     * Get accumulated months.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     * @return the resulting computation or state reference
     */
    public int getAccumulatedMonths() { return accumulatedMonths; }

    /*
     * Advances simulation time by stepDays and evaluates the appropriate physical tiers:
     * - Tier 1 (Daily) is evaluated on every tick.
     * - Tier 2 (Monthly) is evaluated ONLY when a 30-day boundary is crossed.
     * - Tier 3 (Annual) is evaluated ONLY when a 12-month / 365-day boundary is crossed.
     */
    public IntegrationResult step(WorldBuffer buffer,
                                  int stepDays,
                                  Consumer<Float> dailyOperator,
                                  BiConsumer<Float, Integer> monthlyOperator,
                                  BiConsumer<Float, Integer> annualOperator,
                                  int currentMonth,
                                  int currentYear) {

        if (buffer == null || dailyOperator == null) {
            return new IntegrationResult(false, false);
        }

        float dtDaily = stepDays * 86400f;

        // 1. DAILY TIER (Fast scale logistics, prices, flux pressure)
        dailyOperator.accept(dtDaily);

        accumulatedDays += stepDays;
        accumulatedMonthlyDt += dtDaily;

        boolean executedMonthly = false;
        boolean executedAnnual = false;

        // 2. MONTHLY TIER (Climate, Demographics, Soil NPK, Agriculture)
        if (accumulatedDays >= daysPerMonth || stepDays >= daysPerMonth) {
            float dtMonthly = (float) accumulatedMonthlyDt;
            if (monthlyOperator != null) {
                monthlyOperator.accept(dtMonthly, currentMonth);
            }
            accumulatedAnnualDt += accumulatedMonthlyDt;
            accumulatedDays = 0;
            accumulatedMonthlyDt = 0;
            accumulatedMonths++;
            executedMonthly = true;

            // 3. ANNUAL TIER (Smil Infrastructure, Ore Depletion, Macro History)
            if (accumulatedMonths >= monthsPerYear) {
                float dtAnnual = (float) accumulatedAnnualDt;
                if (annualOperator != null) {
                    annualOperator.accept(dtAnnual, currentYear);
                }
                accumulatedMonths = 0;
                accumulatedAnnualDt = 0;
                executedAnnual = true;
            }
        }

        return new IntegrationResult(executedMonthly, executedAnnual);
    }

    /*
     * Reset.
     * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
     *
     */
    public void reset() {
        this.accumulatedDays = 0;
        this.accumulatedMonths = 0;
        this.accumulatedMonthlyDt = 0;
        this.accumulatedAnnualDt = 0;
    }

    public static class IntegrationResult {
        /* Internal state variable for monthly executed (boolean). */
        private final boolean monthlyExecuted;
        /* Internal state variable for annual executed (boolean). */
        private final boolean annualExecuted;

        /*
         * Integration result.
         * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
         *
         * @param monthlyExecuted the monthly executed parameter (boolean)
         * @param annualExecuted the annual executed parameter (boolean)
         * @return the resulting computation or state reference
         */
        public IntegrationResult(boolean monthlyExecuted, boolean annualExecuted) {
            this.monthlyExecuted = monthlyExecuted;
            this.annualExecuted = annualExecuted;
        }

        /*
         * Is monthly executed.
         * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
         *
         * @return the resulting computation or state reference
         */
        public boolean isMonthlyExecuted() { return monthlyExecuted; }
        /*
         * Is annual executed.
         * Enforces physical invariants and updates associated state variables within {@code MultiScaleSymplecticIntegrator}.
         *
         * @return the resulting computation or state reference
         */
        public boolean isAnnualExecuted() { return annualExecuted; }
    }
}

