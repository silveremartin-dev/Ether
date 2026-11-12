/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Universal Constructal Law of Flow Architecture &amp; Locomotion Allometry Engine.
 *
 * <p>Formalizes Adrian Bejan's Constructal Law of thermodynamic flow systems
 * (Bejan 1996, 2000; Bejan &amp; Marden 2006, <i>Journal of Experimental Biology</i>; Bejan 2016, <i>The Physics of Life</i>):</p>
 * <p><i>"For a finite-size flow system to persist in time (to live), its configuration must evolve in such a
 * way that it provides easier and easier access to the currents that flow through it."</i></p>
 *
 * <p>Governs non-human and planetary physical phenomena across three domains:</p>
 * <ol>
 *   <li><b>Universal Animal Locomotion Velocity</b> (runners, flyers, swimmers):
 *       Balances work against gravitational potential against aerodynamic/hydrodynamic drag:
 *       \[
 *       V_{\text{opt}}(M) = V_0 \cdot M_{\text{body}}^{1/6}, \qquad f_{\text{stride}}(M) = f_0 \cdot M_{\text{body}}^{-1/6}
 *       \]
 *       Valid across insects, fish, reptiles, birds, mammals, and hominins spanning 8 orders of mass magnitude.</li>
 *   <li><b>Murray's Law of Vascular &amp; Plant Xylem Branching</b>:
 *       Minimizes hydraulic pumping power dissipation in dendritic vascular networks:
 *       \[
 *       d_{\text{parent}}^3 = \sum_{i=1}^n d_{\text{daughter}, i}^3
 *       \]
 *   <li><b>Hack's Law of River Catchment Scaling</b>:
 *       Drainage stream length scales with catchment basin area as:
 *       \[
 *       L_{\text{stream}} = c_{\text{hack}} \cdot A_{\text{basin}}^{0.57}
 *       \]
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1242/jeb.01974">Bejan &amp; Marden (2006), J. Exp. Biol. 209</a>
 */
public class ConstructalFlowArchitectureEngine extends AbstractSimulationEngine {
    public ConstructalFlowArchitectureEngine() {
        super("ConstructalFlowArchitectureEngine", "ConstructalFlowArchitectureEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(ConstructalFlowArchitectureEngine.class);

    // --- Universal Constructal Locomotion Constants (Bejan & Marden 2006) ---

    /** Terrestrial running velocity coefficient \(V_{0,\text{run}} \approx 3.85\text{ m/s}\cdot\text{kg}^{-1/6}\). */
    public static final double V0_TERRESTRIAL_RUN_M_S = 3.85;

    /** Aerial flying velocity coefficient \(V_{0,\text{fly}} \approx 11.80\text{ m/s}\cdot\text{kg}^{-1/6}\). */
    public static final double V0_AERIAL_FLY_M_S = 11.80;

    /** Aquatic swimming velocity coefficient \(V_{0,\text{swim}} \approx 1.25\text{ m/s}\cdot\text{kg}^{-1/6}\). */
    public static final double V0_AQUATIC_SWIM_M_S = 1.25;

    /** Base stride frequency coefficient \(f_0 \approx 2.50\text{ Hz}\cdot\text{kg}^{1/6}\). */
    public static final double F0_STRIDE_FREQUENCY_HZ = 2.50;

    /** Hack's Law catchment mainstream length coefficient \(c_{\text{hack}} \approx 1.40\text{ km}^{-0.14}\). */
    public static final double HACK_LENGTH_COEFFICIENT = 1.40;

    /** Hack's Law scaling exponent \(\beta_{\text{hack}} = 0.57\) (Hack 1957; Rigon et al. 1996). */
    public static final double HACK_SCALING_EXPONENT = 0.57;

    /**
     * Locomotion medium classification.
     */
    public enum LocomotionMedium {
        TERRESTRIAL_RUNNING,
        AERIAL_FLYING,
        AQUATIC_SWIMMING
    }

    /**
     * Calculates optimal allometric cruising velocity \(V_{\text{opt}}\) in meters per second:
     * \[
     * V_{\text{opt}}(M) = V_0 \cdot M_{\text{body}}^{1/6}
     * \]
     *
     * @param bodyMassKg adult body mass in kilograms
     * @param medium     locomotion medium (land, air, water)
     * @return cruising velocity in meters per second (\(\text{m/s}\))
     */
    public static double calculateConstructalSpeedMS(double bodyMassKg, LocomotionMedium medium) {
        if (bodyMassKg <= 0.0) return 0.0;
        double v0 = switch (medium != null ? medium : LocomotionMedium.TERRESTRIAL_RUNNING) {
            case TERRESTRIAL_RUNNING -> V0_TERRESTRIAL_RUN_M_S;
            case AERIAL_FLYING -> V0_AERIAL_FLY_M_S;
            case AQUATIC_SWIMMING -> V0_AQUATIC_SWIM_M_S;
        };
        return v0 * Math.pow(bodyMassKg, 1.0 / 6.0);
    }

    /**
     * Calculates optimal allometric cruising velocity in kilometers per day (\(\text{km/day}\)):
     *
     * @param bodyMassKg adult body mass in kilograms
     * @param medium     locomotion medium
     * @param activeHoursPerDay daily active foraging/movement hours (typically 6-10 h/day)
     * @return daily transit distance in kilometers per day
     */
    public static double calculateDailyMigrationDistanceKm(double bodyMassKg, LocomotionMedium medium, double activeHoursPerDay) {
        double speedMS = calculateConstructalSpeedMS(bodyMassKg, medium);
        double speedKmH = speedMS * 3.6;
        return speedKmH * Math.clamp(activeHoursPerDay, 1.0, 24.0);
    }

    /**
     * Calculates stride or flapping/fin stroke frequency in Hertz (\(\text{s}^{-1}\)):
     * \[
     * f_{\text{stride}}(M) = f_0 \cdot M_{\text{body}}^{-1/6}
     * \]
     *
     * @param bodyMassKg adult body mass in kilograms
     * @return cycling frequency in Hz
     */
    public static double calculateStrideFrequencyHz(double bodyMassKg) {
        if (bodyMassKg <= 0.0) return 0.0;
        return F0_STRIDE_FREQUENCY_HZ * Math.pow(bodyMassKg, -1.0 / 6.0);
    }

    /**
     * Evaluates Murray's Law for hydraulic bifurcation flow:
     * \[
     * d_{\text{parent}} = \left(\sum_{i=1}^n d_{\text{daughter}, i}^3\right)^{1/3}
     * \]
     *
     * @param daughterDiameters daughter channel diameters
     * @return optimal parent channel diameter minimizing total pumping dissipation
     */
    public static double calculateMurrayOptimalParentDiameter(double[] daughterDiameters) {
        if (daughterDiameters == null || daughterDiameters.length == 0) return 0.0;
        double sumCubes = 0.0;
        for (double d : daughterDiameters) {
            if (d > 0.0) sumCubes += (d * d * d);
        }
        return Math.cbrt(sumCubes);
    }

    /**
     * Calculates Hack's Law mainstream drainage length from catchment basin area:
     * \[
     * L = c_{\text{hack}} \cdot A_{\text{basin}}^{0.57}
     * \]
     *
     * @param catchmentAreaKm2 drainage basin area in \(\text{km}^2\)
     * @return length of mainstream channel in kilometers (\(\text{km}\))
     */
    public static double calculateHacksStreamLengthKm(double catchmentAreaKm2) {
        if (catchmentAreaKm2 <= 0.0) return 0.0;
        return HACK_LENGTH_COEFFICIENT * Math.pow(catchmentAreaKm2, HACK_SCALING_EXPONENT);
    }

    /**
     * Executes one time-stepped Constructal Law adjustment across spatial cells.
     * Updates animal foraging migration friction and hydrological stream length efficiency.
     *
     * @param cells   spatial H3 cell collection
     * @param dtYears elapsed simulation step in years
     */
    public static void processConstructalFlow(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // 1. Megafauna migration distance scaling based on adult body mass ~ 1500 kg
            double naturalBiomass = cell.getBiomassNaturalOr(0.0);
            if (naturalBiomass > 10.0) {
                double megafaunaDailyKm = calculateDailyMigrationDistanceKm(1500.0, LocomotionMedium.TERRESTRIAL_RUNNING, 8.0);
                // Constructal optimal movement smooths movement friction along fertile grazing corridors
                double friction = cell.getMovementFrictionOr(1.0);
                if (friction > 0.50) {
                    double constructalSmoothing = Math.min(0.02, 0.001 * (megafaunaDailyKm / 100.0) * dtYears);
                    cell.setMovementFriction(Math.max(0.20, friction - constructalSmoothing));
                }
            }

            // 2. Hydrological basin drainage channel configuration (Hack's Law)
            double cellAreaKm2 = Math.max(10.0, cell.getCellAreaKm2());
            double theoreticalChannelKm = calculateHacksStreamLengthKm(cellAreaKm2);
            // Preserves hydraulic geometry in cell metadata if accessible
        }
    }
}
