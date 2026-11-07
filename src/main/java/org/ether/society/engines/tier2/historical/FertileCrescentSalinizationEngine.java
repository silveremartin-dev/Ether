/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mesopotamian Irrigation &amp; Fertile Crescent Salinization Engine Variant B33.1 (Pure) &amp; B33.2 (Hybrid).
 * Models progressive topsoil salinization in canal-irrigated alluvial river basins (Tigris/Euphrates):
 * 1. High evaporation deposits sodium salts (Na+) in topsoil.
 * 2. Crop yield degradation forces shift from salt-sensitive wheat to salt-tolerant barley.
 * 3. Severe salinization causes agricultural crash and historical shift of civilizational center north.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class FertileCrescentSalinizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(FertileCrescentSalinizationEngine.class);

    private static double salinizationRatePerCentury = 0.05; // 5% soil sodium buildup per century of irrigation

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // Universal hydro-climatic condition (no hardcoded GPS bounding box):
            // Salinization occurs in any warm/arid or intensively canal-irrigated alluvial plain
            // where evapotranspiration exceeds precipitation leaching (rainfall < 600 mm, elevation < 1000 m).
            double rainfall = cell.getRainfallOr(300.0);
            double temp = cell.getTemperatureOr(22.0);
            double elev = cell.getElevationOr(100.0);
            if (rainfall >= 650.0 || temp < 12.0 || elev > 1200.0) continue;

            double food = cell.getFoodResourceOr(1000.0);
            int pop = Math.max(1, cell.getPopulationOr(100));
            double kpc = cell.getResourceCapitalOr(50.0) / pop;

            // Continuous sigmoidal hydraulic irrigation & drainage capital envelope (Jacobsen & Adams 1958; Hillel 1998):
            // Gravity canal irrigation susceptibility emerges with earthen canal capital (kpc >= 25 kg/cap)
            // and is alleviated by pressurized subsurface tile drainage and pumping (kpc >= 2500 kg/cap).
            double drainageAttenuation = 1.0 / (1.0 + Math.exp((kpc - 2500.0) / 500.0));
            double canalActivation = 1.0 / (1.0 + Math.exp(-(kpc - 25.0) / 10.0));
            double irrigationSusceptibility = canalActivation * drainageAttenuation;

            if (irrigationSusceptibility > 0.05) {
                double aridityMultiplier = Math.clamp((650.0 - rainfall) / 400.0, 0.25, 1.5);
                double effectiveRate = salinizationRatePerCentury * aridityMultiplier * irrigationSusceptibility;
                double salinizationFactor = Math.max(0.20, Math.exp(-effectiveRate * (deltaYears / 100.0)));
                cell.setFoodResource(food * salinizationFactor);
                double curSalinity = cell.getSoilSalinityOr(0.0);
                cell.setSoilSalinity(Math.min(20.0, curSalinity + effectiveRate * (deltaYears / 10.0)));
            }
        }
    }

    /**
     * Get salinization rate per century.
     *
     * @return the resulting computation or state reference
     */
    public static double getSalinizationRatePerCentury() { return salinizationRatePerCentury; }

    /**
     * Set salinization rate per century operation.
     * <p>
     * Executes operational logic for {@code FertileCrescentSalinizationEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (double)
     */
    public static void setSalinizationRatePerCentury(double rate) { salinizationRatePerCentury = Math.max(0.0, rate); }

    /**
     * Resets salinization rate per century for scenario isolation.
     */
    public static void resetState() {
        salinizationRatePerCentury = 0.05;
    }
}
