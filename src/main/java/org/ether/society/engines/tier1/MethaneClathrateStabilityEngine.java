/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marine Methane Clathrate Thermodynamic Stability &amp; Dissociation Engine.
 *
 * <p>Formalizes the thermodynamic phase equilibrium of subsea methane hydrate reservoirs and
 * benthic thermal outgassing established by Dickens et al. (1995), Buffett &amp; Archer (2004),
 * Sloan (1998), and Archer et al. (2009, <i>PNAS</i>).</p>
 *
 * <p>Key Geophysical &amp; Thermodynamic Mechanics:</p>
 * <ol>
 *   <li><b>Phase Boundary Equilibrium Curve (Sloan 1998; Buffett &amp; Archer 2004)</b>:
 *       \[
 *       T_{\text{eq}}(P) = T_{\text{base}} + \alpha_{\text{hydrate}} \cdot \ln\left(\frac{P}{P_0}\right) - \Delta T_{\text{salinity}} \quad [^\circ\text{C}]
 *       \]
 *       where hydrostatic pressure at water depth \(z\) is \(P(z) = \rho_{\text{sw}} \cdot g \cdot z\) (in \(\text{MPa}\)),
 *       with reference pressure \(P_0 = 2.5\text{ MPa}\) (depth \(\approx 250\text{ m}\)), slope \(\alpha_{\text{hydrate}} \approx 9.5^\circ\text{C}\),
 *       and seawater salinity depression \(\Delta T_{\text{salinity}} \approx 1.2^\circ\text{C}\).</li>
 *   <li><b>Gas Hydrate Stability Zone (GHSZ) Thickness</b>:
 *       With sediment geothermal gradient \(G_T \approx 0.035^\circ\text{C/m}\) (\(35^\circ\text{C/km}\)),
 *       the thickness of the sub-seafloor sediment stability layer is:
 *       \[
 *       Z_{\text{GHSZ}}(z) = \max\left(0, \; \frac{T_{\text{eq}}(z) - T_{\text{bottom}}}{G_T}\right) \quad [\text{m}]
 *       \]</li>
 *   <li><b>Hyperthermal Clathrate Dissociation &amp; Methane Outgassing</b>:
 *       When deep ocean bottom water temperature \(T_{\text{bottom}}\) exceeds pre-industrial / glacial
 *       steady-state baselines (\(\Delta T_{\text{bottom}} > 0\)), thermal diffusion into benthic sediments
 *       causes thinning of the GHSZ (\(\Delta Z < 0\)) and irreversible dissociation of frozen solid hydrates
 *       into gaseous methane (\(\text{CH}_4\)):
 *       \[
 *       \Phi_{\text{CH}_4} = \kappa_{\text{dissoc}} \cdot A_{\text{benthic}} \cdot \max(0, T_{\text{bottom}} - T_{\text{bottom,baseline}}) \cdot \Delta t \quad [\text{Gt C / yr}]
 *       \]</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see <a href="https://doi.org/10.1029/95PA02087">Dickens et al. (1995), Paleocene-Eocene Thermal Maximum Hydrate Dissociation</a>
 * @see <a href="https://doi.org/10.1029/2003GB002131">Buffett &amp; Archer (2004), Global inventory of methane clathrate</a>
 * @see <a href="https://doi.org/10.1073/pnas.0800885105">Archer et al. (2009), Ocean methane hydrates as a slow tipping point</a>
 */
public class MethaneClathrateStabilityEngine extends AbstractSimulationEngine {
    public MethaneClathrateStabilityEngine() {
        super("MethaneClathrateStabilityEngine", "MethaneClathrateStabilityEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(MethaneClathrateStabilityEngine.class);

    // --- Fundamental Thermodynamic & Oceanographic Constants ---

    /** Seawater density \(\rho_{\text{sw}} = 1025.0\text{ kg/m}^3\). */
    public static final double SEAWATER_DENSITY_KG_M3 = 1025.0;

    /** Standard gravitational acceleration \(g = 9.80665\text{ m/s}^2\). */
    public static final double GRAVITY_ACCELERATION_M_S2 = 9.80665;

    /** Reference pressure for clathrate stability onset \(P_0 = 2.50\text{ MPa}\) (\(\approx 248.5\text{ m}\) water depth). */
    public static final double P_ZERO_CLATHRATE_MPA = 2.50;

    /** Minimum ocean water depth for methane hydrate stability in meters (\(\approx 250\text{ m}\)). */
    public static final double MIN_HYDRATE_DEPTH_METERS = 250.0;

    /** Base equilibrium phase temperature at reference pressure \(T_{\text{base}} = -1.50^\circ\text{C}\). */
    public static final double T_BASE_PHASE_CELSIUS = -1.50;

    /** Clathrate logarithmic phase slope parameter \(\alpha_{\text{hydrate}} = 9.50^\circ\text{C}\). */
    public static final double ALPHA_HYDRATE_SLOPE = 9.50;

    /** Seawater 35 PSU salinity freezing point / hydrate depression \(\Delta T_{\text{salinity}} = 1.20^\circ\text{C}\). */
    public static final double SALINITY_DEPRESSION_CELSIUS = 1.20;

    /** Continental slope sub-seafloor sediment geothermal gradient \(G_T = 0.035^\circ\text{C/m}\) (\(35.0^\circ\text{C/km}\)). */
    public static final double SEDIMENT_GEOTHERMAL_GRADIENT_C_PER_M = 0.035;

    /** Baseline pre-industrial deep ocean bottom water temperature \(T_{\text{bottom,baseline}} = 2.0^\circ\text{C}\). */
    public static final double BASELINE_BOTTOM_WATER_TEMP_CELSIUS = 2.0;

    /** Specific methane dissociation outgassing rate per degree benthic warming (\(\kappa \approx 0.008\text{ ppm CH}_4 / (^\circ\text{C} \cdot \text{cell} \cdot \text{yr})\)). */
    public static final double SPECIFIC_METHANE_DISSOCIATION_RATE = 0.008;

    /**
     * Calculates hydrostatic pressure in \(\text{MPa}\) at a given ocean depth in meters:
     * \[
     * P(z) = \frac{\rho_{\text{sw}} \cdot g \cdot z}{10^6} \quad [\text{MPa}]
     * \]
     *
     * @param depthMeters water column depth in meters (\(\ge 0\))
     * @return hydrostatic pressure in \(\text{MPa}\)
     */
    public static double calculateHydrostaticPressureMPa(double depthMeters) {
        if (depthMeters <= 0.0) return 0.101325; // Surface atmospheric pressure ~0.101 MPa
        return (SEAWATER_DENSITY_KG_M3 * GRAVITY_ACCELERATION_M_S2 * depthMeters) * 1e-6;
    }

    /**
     * Calculates the thermodynamic phase equilibrium temperature \(T_{\text{eq}}(P)\) in \(^\circ\text{C}\)
     * above which methane hydrate dissociates into gas:
     * \[
     * T_{\text{eq}}(P) = T_{\text{base}} + \alpha_{\text{hydrate}} \cdot \ln\left(\frac{P}{P_0}\right) - \Delta T_{\text{salinity}}
     * \]
     *
     * @param pressureMPa hydrostatic pore pressure in \(\text{MPa}\)
     * @return phase stability boundary temperature in \(^\circ\text{C}\)
     */
    public static double calculateEquilibriumPhaseTemperatureCelsius(double pressureMPa) {
        if (pressureMPa < P_ZERO_CLATHRATE_MPA) {
            // Below minimum pressure (shallow waters < 250m), hydrates are unstable at any oceanic temperature > -10A°C
            return -50.0;
        }
        double logRatio = Math.log(pressureMPa / P_ZERO_CLATHRATE_MPA);
        return T_BASE_PHASE_CELSIUS + ALPHA_HYDRATE_SLOPE * logRatio - SALINITY_DEPRESSION_CELSIUS;
    }

    /**
     * Calculates the thickness of the Gas Hydrate Stability Zone (GHSZ) in sub-seafloor sediment in meters:
     * \[
     * Z_{\text{GHSZ}} = \max\left(0, \; \frac{T_{\text{eq}}(P(z)) - T_{\text{bottom}}}{G_T}\right)
     * \]
     *
     * @param depthMeters ocean bathymetry depth in meters
     * @param bottomWaterTempC benthic ocean water temperature in \(^\circ\text{C}\)
     * @return sediment thickness of hydrate stability zone in meters (\(\ge 0.0\))
     */
    public static double calculateGHSZThicknessMeters(double depthMeters, double bottomWaterTempC) {
        if (depthMeters < MIN_HYDRATE_DEPTH_METERS) {
            return 0.0;
        }
        double pMPa = calculateHydrostaticPressureMPa(depthMeters);
        double tEq = calculateEquilibriumPhaseTemperatureCelsius(pMPa);
        double deltaT = tEq - bottomWaterTempC;
        if (deltaT <= 0.0) {
            return 0.0;
        }
        return deltaT / SEDIMENT_GEOTHERMAL_GRADIENT_C_PER_M;
    }

    /**
     * Estimates benthic ocean bottom water temperature from surface temperature and polar AMOC coupling.
     * In deep oceans (\(z > 1000\text{ m}\)), water is buffered by polar sinking water (\(\approx 0.5 - 3.0^\circ\text{C}\)),
     * warming when global ocean heat content increases.
     *
     * @param surfaceTempC surface temperature of cell in \(^\circ\text{C}\)
     * @param depthMeters  water depth in meters
     * @param globalOceanWarmingAnomalyC deep ocean benthic warming anomaly from baseline
     * @return estimated bottom water temperature in \(^\circ\text{C}\)
     */
    public static double estimateBottomWaterTemperature(double surfaceTempC, double depthMeters, double globalOceanWarmingAnomalyC) {
        if (depthMeters < 100.0) {
            return surfaceTempC; // Shallow shelves mixed to bottom
        }
        // Deep water thermocline decay toward benthic polar baseline
        double thermoclineFactor = Math.exp(-depthMeters / 600.0);
        double deepBase = BASELINE_BOTTOM_WATER_TEMP_CELSIUS + Math.max(0.0, globalOceanWarmingAnomalyC);
        return deepBase + (surfaceTempC - deepBase) * thermoclineFactor;
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processClathrateDissociation(buf, dtYears, 0.0);
    }

    /**
     * Executes one time-stepped marine clathrate stability evaluation across ocean cells.
     *
     * @param buffer WorldBuffer DoD state
     * @param dtYears elapsed simulation step in years
     * @param globalBenthicWarmingAnomalyC global bottom water temperature anomaly in \(^\circ\text{C}\)
     * @return total methane released into ocean-atmosphere in ppm equivalent
     */
    public static double processClathrateDissociation(WorldBuffer buffer, double dtYears, double globalBenthicWarmingAnomalyC) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0 || globalBenthicWarmingAnomalyC <= 0.0) return 0.0;

        double dtSafe = Math.max(1e-4, dtYears);
        double totalMethanePpmOutgassed = 0.0;
        int activeDissociationCells = 0;

        float[] elevArr = buffer.getElevation();
        byte[] biomes = buffer.getBiomes();
        float[] tempArr = buffer.getTemperature();
        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double elevation = elevArr[i];
            byte biomeOrd = biomes[i];

            // Marine benthic criteria: submerged cells (elevation < 0) or marine biomes
            boolean isMarine = elevation < 0.0 || biomeOrd == oceanOrd || biomeOrd == deepOceanOrd;
            if (!isMarine) continue;

            double depthMeters = Math.max(0.0, -elevation);
            if (depthMeters < MIN_HYDRATE_DEPTH_METERS) continue;

            double surfTemp = tempArr[i];
            double bottomWaterTemp = estimateBottomWaterTemperature(surfTemp, depthMeters, globalBenthicWarmingAnomalyC);

            double ghszThickness = calculateGHSZThicknessMeters(depthMeters, bottomWaterTemp);

            // Dissociation triggered when benthic bottom water warms above pre-industrial baseline
            double tempExcess = Math.max(0.0, globalBenthicWarmingAnomalyC);
            if (tempExcess > 0.0 && ghszThickness > 0.0) {
                activeDissociationCells++;
                double cellRelease = tempExcess * SPECIFIC_METHANE_DISSOCIATION_RATE * dtSafe;
                totalMethanePpmOutgassed += cellRelease;
            }
        }

        if (activeDissociationCells > 0 && totalMethanePpmOutgassed > 0.01) {
            logger.info("Methane Clathrate Engine: Dissociation across {} deep marine cells released {} ppm CH4 equivalent (Anomaly: +{} C).",
                    activeDissociationCells,
                    String.format(java.util.Locale.ROOT, "%.3f", totalMethanePpmOutgassed),
                    String.format(java.util.Locale.ROOT, "%.2f", globalBenthicWarmingAnomalyC));
        }

        return totalMethanePpmOutgassed;
    }
}
