/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Urban Allometry & Metabolic Scaling Engine (West, Bettencourt et al., 2007).
 *
 * <p>Models non-linear fractal power laws governing human urban agglomerations:</p>
 * <ul>
 *   <li><b>Super-linear scaling (β ≈ 1.15)</b>: Socio-economic output (GDP, patents, innovation, crime)
 *       scales super-linearly with urban population: $Y \propto N^{1.15}$.</li>
 *   <li><b>Sub-linear scaling (γ ≈ 0.85)</b>: Material infrastructure networks (road surface, power grid length, pipes)
 *       scale sub-linearly with urban population: $I \propto N^{0.85}$ (geometry of fractal supply networks).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.3.0
 */
public class WestBettencourtAllometryEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(WestBettencourtAllometryEngine.class);

    public static final double SUPERLINEAR_EXPONENT = 1.15; // Socioeconomic returns
    public static final double SUBLINEAR_EXPONENT = 0.85;   // Infrastructure efficiency

    @Override
    public String getName() {
        return "West-Bettencourt Urban Allometry";
    }

    @Override
    public String getDescription() {
        return "Models urban scaling laws: super-linear innovation/output (N^1.15) and sub-linear infrastructure network economy (N^0.85).";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [West-Bettencourt Urban Scaling Laws (PNAS 2007)]
               • Super-Linear Output:  Y_i = Y_0 · (N_i / N_ref)^1.15  (GDP, Patents, Wages)
               • Sub-Linear Network:  I_i = I_0 · (N_i / N_ref)^0.85  (Roads, Cables, Energy Grid)
               • Metabolic Pace:      v_pace ∝ N_i^0.15 (Pace of urban life & interactions)
               Units: N [hab], Y [$/an, brevets], I [km réseau, J/hab]
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Cliodynamics & Urban Scaling";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double refPop = 1000.0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 100) continue;

            double normalizedPop = Math.max(1.0, (double) pop / refPop);

            // Super-linear capital & innovation output
            double superlinearFactor = Math.pow(normalizedPop, SUPERLINEAR_EXPONENT - 1.0); // N^0.15 boost per capita
            double currentCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;
            cell.setResourceCapital(currentCapital + (currentCapital * 0.02 * superlinearFactor * deltaYears));

            // Sub-linear infrastructure maintenance cost efficiency
            double sublinearFactor = Math.pow(normalizedPop, SUBLINEAR_EXPONENT - 1.0); // N^-0.15 savings per capita
            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            // High density reduces relative movement friction due to denser transport networks
            cell.setMovementFriction(Math.max(0.5, friction * (1.0 - 0.01 * sublinearFactor * deltaYears)));
        }
    }
}
