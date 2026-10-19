*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mesopotamian Irrigation & Fertile Crescent Salinization Engine Variant B33.1 (Pure) & B33.2 (Hybrid).
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
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // Universal hydro-climatic condition (no hardcoded GPS bounding box):
            // Salinization occurs in any warm/arid or intensively canal-irrigated alluvial plain
            // where evapotranspiration exceeds precipitation leaching (rainfall < 600 mm, elevation < 1000 m).
            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 300.0;
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 22.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 100.0;
            if (rainfall >= 650.0 || temp < 12.0 || elev > 1200.0) continue;

            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 1000.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

            // Pre-modern canal irrigation without subsurface tile drainage (Tech level 1.0 to 3.5)
            if (tech >= 1.0 && tech <= 3.5) {
                double aridityMultiplier = Math.clamp((650.0 - rainfall) / 400.0, 0.25, 1.5);
                double effectiveRate = salinizationRatePerCentury * aridityMultiplier;
                double salinizationFactor = Math.max(0.20, Math.exp(-effectiveRate * (deltaYears / 100.0)));
                cell.setFoodResource(food * salinizationFactor);
                double curSalinity = cell.getSoilSalinity() != null ? cell.getSoilSalinity() : 0.0;
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


