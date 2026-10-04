/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Boserupian Agricultural Intensification & Demographic Pressure Engine (Ester Boserup, 1965).
 *
 * <p>Models endogenous technological shifts driven by demographic carrying capacity pressure:</p>
 * <pre>
 *   Stage: Foraging â†’ Forest-Fallow â†’ Bush-Fallow â†’ Short-Fallow â†’ Multi-Cropping Irrigation
 *   Labor_req âˆ Yield^1.40
 *   Trigger: Density = Population / Habitable_Area â‰¥ Threshold_k
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class BoserupAgriculturalIntensificationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(BoserupAgriculturalIntensificationEngine.class);

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code BoserupAgriculturalIntensificationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Boserup Agricultural Intensification";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code BoserupAgriculturalIntensificationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models population pressure forcing technological transitions to higher-yield, higher-labor intensive agricultural systems.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code BoserupAgriculturalIntensificationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Boserup Agricultural Intensification Model (1965)]
               â€¢ Density Trigger:     D_i = N_i / Area_km2
               â€¢ Labor Demand:        L_req = L_0 Â· (Yield_target / Yield_0)^1.40
               â€¢ Technology Stages:   1: Foraging (D < 2), 2: Long Fallow (D < 10), 3: Annual Crop (D < 50), 4: Multi-Crop Irrigation (D >= 50)
               â€¢ Boserup Axiom:       Population pressure is the mother of agricultural innovation (anti-Malthusian).
               Units: D [hab/kmÂ²], L_req [heures-homme/hectare], Yield [kg/ha]
               Ref: E. Boserup (1965) "The Conditions of Agricultural Growth"
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code BoserupAgriculturalIntensificationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Agrarian Cliodynamics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code BoserupAgriculturalIntensificationEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            // Approximate cell area ~1000 kmÂ²
            double density = pop / 1000.0;
            double agriBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 50.0;

            if (density > 50.0) {
                // High density forces multi-cropping / terrace irrigation (Stage 4)
                cell.setBiomassAgriculture(agriBiomass + (agriBiomass * 0.04 * deltaYears));
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
                cell.setTechnologyLevel(Math.min(10.0, tech + 0.005 * deltaYears));
                // High labor demand absorbs work capacity
                double work = cell.getResourceWork() != null ? cell.getResourceWork() : 50.0;
                cell.setResourceWork(Math.max(10.0, work * 0.98));
            } else if (density > 10.0) {
                // Moderate density: Annual cropping / draft animals (Stage 3)
                cell.setBiomassAgriculture(agriBiomass + (agriBiomass * 0.02 * deltaYears));
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
                cell.setTechnologyLevel(Math.min(10.0, tech + 0.002 * deltaYears));
            }
        }
    }
}



