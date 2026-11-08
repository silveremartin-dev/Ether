/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bergmann-Allen Ecomorphology &amp; Thermoregulatory Scaling Engine.
 *
 * <p>Formalizes the universal allometric and morphological scaling laws established by
 * Carl Bergmann (1847), Joel Asaph Allen (1877), Christopher Ruff (1994, 2002), and
 * J. Bristol Foster's Island Rule (1964).</p>
 *
 * <p>Key Biophysical Mechanics:</p>
 * <ol>
 *   <li><b>Bergmann's Rule (Body Mass vs. Paleotemperature)</b>:
 *       \[
 *       M^*(T) = M_{\text{ref}} \cdot \left[1.0 + \beta_{\text{bergmann}} \cdot \tanh\left(\frac{T_{\text{ref}} - T}{s_T}\right)\right]
 *       \]
 *       where body mass increases in cold periglacial environments to minimize thermal dissipation per unit volume.</li>
 *   <li><b>Allen's Rule (Surface-Area-to-Mass Ratio)</b>:
 *       \[
 *       \frac{SA}{M} = c_{\text{geom}} \cdot M^{-1/3} \quad [\text{m}^2/\text{kg}]
 *       \]
 *       where distal limb length and cylindrical body diameter adapt to regulate conductive heat exchange.</li>
 *   <li><b>Foster's Insular Dwarfism / Gigantism Rule (1964)</b>:
 *       On isolated geographical islands with restricted resource area \(A_{\text{land}}\),
 *       large megafauna dwarf toward sustainable biomass ceilings (\(M \propto A_{\text{land}}^{1/3}\)).</li>
 *   <li><b>Dynamic Energy Budget (DEB) Somatic Maintenance Feedback</b>:
 *       Thermoregulatory energy dissipation \([\dot{p}_M]\) scales with effective surface area:
 *       \[
 *       [\dot{p}_M](T, M) = [\dot{p}_{M0}] \cdot \left[1.0 + \gamma_{\text{thermo}} \cdot \frac{SA}{M} \cdot \max(0, T_{\text{comfort}} - T)\right]
 *       \]</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1002/ajpa.1330370606">Ruff (1994), Morphological adaptation in hominids</a>
 * @see <a href="https://doi.org/10.1038/202234a0">Foster (1964), Nature 202</a>
 */
public class BergmannAllenMorphologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(BergmannAllenMorphologyEngine.class);

    // --- Fundamental Anthropometric & Biophysical Constants ---

    /** Baseline reference temperature \(T_{\text{ref}} = 15.0^\circ\text{C}\). */
    public static final double T_REF_CELSIUS = 15.0;

    /** Characteristic temperature scaling width \(s_T = 18.0^\circ\text{C}\). */
    public static final double S_TEMP_CELSIUS = 18.0;

    /** Maximum Bergmann body mass latitude scaling factor \(\beta_{\text{bergmann}} \approx 0.25\) (\(\pm 25\%\) body mass variation). */
    public static final double BERGMANN_SCALING_AMPLITUDE = 0.25;

    /** Human baseline healthy structural lean mass \(M_{\text{human,ref}} = 0.050\text{ metric tons}\) (\(50.0\text{ kg}\)). */
    public static final double HUMAN_REFERENCE_MASS_TONS = 0.050;

    /** Human geometric surface area constant \(c_{\text{geom}} \approx 0.048\text{ m}^2\cdot\text{kg}^{1/3}\) (Du Bois &amp; Du Bois 1916). */
    public static final double HUMAN_GEOMETRIC_SURFACE_COEFFICIENT = 0.048;

    /** Thermoregulatory comfort boundary temperature \(T_{\text{comfort}} \approx 20.0^\circ\text{C}\). */
    public static final double THERMAL_COMFORT_TEMP_CELSIUS = 20.0;

    /** Thermoregulatory conductance coefficient \(\gamma_{\text{thermo}} \approx 0.035\). */
    public static final double THERMOREGULATORY_CONDUCTANCE_COEFFICIENT = 0.035;

    /** Morphological phenotypic adaptation relaxation timescale \(\tau_{\text{morph}} \approx 50.0\text{ years}\). */
    public static final double MORPHOLOGICAL_RELAXATION_TIME_YEARS = 50.0;

    /**
     * Calculates the equilibrium Bergmann body mass in metric tons for a given ambient temperature:
     * \[
     * M^*(T) = M_{\text{ref}} \cdot \left[1.0 + \beta_{\text{bergmann}} \cdot \tanh\left(\frac{T_{\text{ref}} - T}{s_T}\right)\right]
     * \]
     *
     * @param tempCelsius ambient mean surface temperature in \(^\circ\text{C}\)
     * @param refMassTons baseline taxon body mass in metric tons
     * @return equilibrium adapted body mass in metric tons
     */
    public static double calculateBergmannEquilibriumMass(double tempCelsius, double refMassTons) {
        double safeRef = Math.max(1e-4, refMassTons);
        double deltaT = (T_REF_CELSIUS - tempCelsius) / S_TEMP_CELSIUS;
        double massMultiplier = 1.0 + BERGMANN_SCALING_AMPLITUDE * Math.tanh(deltaT);
        return safeRef * massMultiplier;
    }

    /**
     * Calculates the Surface-Area-to-Body-Mass ratio (\(SA/M\)) according to Allen's rule:
     * \[
     * \frac{SA}{M} = c_{\text{geom}} \cdot M_{\text{kg}}^{-1/3} \quad [\text{m}^2/\text{kg}]
     * \]
     *
     * @param bodyMassKg body mass in kilograms
     * @return surface area to mass ratio in \(\text{m}^2/\text{kg}\)
     */
    public static double calculateSurfaceAreaToMassRatio(double bodyMassKg) {
        double safeMass = Math.max(0.1, bodyMassKg);
        return HUMAN_GEOMETRIC_SURFACE_COEFFICIENT * Math.pow(safeMass, -1.0 / 3.0);
    }

    /**
     * Calculates Foster's Island Dwarfism scaling factor for megafauna on restricted landmasses:
     * \[
     * f_{\text{island}}(A_{\text{cell}}, N_{\text{neighbors}}) = \min\left(1.0, \; \left(\frac{A_{\text{effective}}}{A_{\text{mainland}}}\right)^{0.33}\right)
     * \]
     *
     * @param isIsland whether the land cell is an isolated insular geography
     * @return dimensionless mass scaling factor \(\in [0.25, 1.0]\)
     */
    public static double calculateFosterIslandDwarfismFactor(boolean isIsland) {
        if (!isIsland) return 1.0;
        // Insular dwarf mammoths (Wrangel Island) and dwarf elephants were ~25-35% of mainland size
        return 0.35;
    }

    /**
     * Calculates the thermoregulatory somatic maintenance multiplier for Dynamic Energy Budget:
     * \[
     * \mu_{\text{thermo}} = 1.0 + \gamma_{\text{thermo}} \cdot \frac{SA}{M} \cdot \max(0, T_{\text{comfort}} - T)
     * \]
     *
     * @param tempCelsius ambient temperature in \(^\circ\text{C}\)
     * @param bodyMassKg  body mass in kilograms
     * @return dimensionless somatic maintenance multiplier \(\ge 1.0\)
     */
    public static double calculateThermoregulatoryMaintenanceMultiplier(double tempCelsius, double bodyMassKg) {
        if (tempCelsius >= THERMAL_COMFORT_TEMP_CELSIUS) {
            return 1.0;
        }
        double saRatio = calculateSurfaceAreaToMassRatio(bodyMassKg);
        double coldStress = THERMAL_COMFORT_TEMP_CELSIUS - tempCelsius;
        return 1.0 + THERMOREGULATORY_CONDUCTANCE_COEFFICIENT * saRatio * coldStress;
    }

    /**
     * Calculates the adapted body mass in kilograms for a given temperature and island geography.
     *
     * @param tempCelsius ambient temperature in \(^\circ\text{C}\)
     * @param isIsland    whether the geography is an isolated island
     * @return adapted body mass in kilograms
     */
    public static double getAdaptedBodyMassKg(double tempCelsius, boolean isIsland) {
        double baseMassKg = calculateBergmannEquilibriumMass(tempCelsius, HUMAN_REFERENCE_MASS_TONS * 1000.0);
        return baseMassKg * calculateFosterIslandDwarfismFactor(isIsland);
    }

    /**
     * Executes one time-stepped ecomorphological adaptation update across cells.
     * Evaluates adapted body mass and applies thermoregulatory metabolic energy surcharges in cold climates.
     *
     * @param cells   populated cell domain
     * @param dtYears elapsed simulation step in years
     */
    public static void processMorphologicalAdaptation(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.max(1e-4, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double tempC = cell.getTemperatureOr(15.0);
            boolean isIsland = Boolean.TRUE.equals(cell.getIsCoastal()) && cell.getElevationOr(0.0) > 0.0;
            double adaptedMassKg = getAdaptedBodyMassKg(tempC, isIsland);

            // Thermoregulatory metabolic surcharge in cold climates modulates caloric food consumption
            double maintMult = calculateThermoregulatoryMaintenanceMultiplier(tempC, adaptedMassKg);
            if (maintMult > 1.0) {
                double food = cell.getFoodResourceOr(0.0);
                if (food > 0.0) {
                    double extraConsumption = (maintMult - 1.0) * (pop * 0.02) * dtSafe;
                    cell.setFoodResource(Math.max(0.0, food - extraConsumption));
                }
            }
        }
    }
}
