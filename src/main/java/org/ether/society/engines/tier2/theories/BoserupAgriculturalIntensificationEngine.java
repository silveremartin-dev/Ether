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
 *   Stage: Foraging → Forest-Fallow → Bush-Fallow → Short-Fallow → Multi-Cropping Irrigation
 *   Labor_req âˆ Yield^1.40
 *   Trigger: Density = Population / Habitable_Area ≥ Threshold_k
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BoserupAgriculturalIntensificationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(BoserupAgriculturalIntensificationEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Boserup Agricultural Intensification";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models population pressure forcing technological transitions to higher-yield, higher-labor intensive agricultural systems.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Boserup Agricultural Intensification Model (1965)]
               • Density Trigger:     D_i = N_i / Area_km2
               • Labor Demand:        L_req = L_0 · (Yield_target / Yield_0)^1.40
               • Technology Stages:   1: Foraging (D < 2), 2: Long Fallow (D < 10), 3: Annual Crop (D < 50), 4: Multi-Crop Irrigation (D >= 50)
               • Boserup Axiom:       Population pressure is the mother of agricultural innovation (anti-Malthusian).
               Units: D [hab/km²], L_req [heures-homme/hectare], Yield [kg/ha]
               Ref: E. Boserup (1965) "The Conditions of Agricultural Growth"
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Agrarian Cliodynamics";
    }

    /**
     * Process.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            // Calculate genuine geographic density in hab/km² based on planar H3 resolution area
            int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                    ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double cellArea = Math.max(10.0, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3));
            double areaScale = Math.max(0.05, cellArea / 1770.49);
            double density = pop / cellArea;
            double tech = cell.getTechnologyLevelOr(1.0);
            if (tech < 1.5 && density < 2.0) {
                // Stage 1 Foraging (D < 2 hab/km², tech < 1.5): no crop cultivation
                continue;
            }
            org.ether.society.model.Biome biome = cell.getBiome() != null ? cell.getBiome() : org.ether.society.model.Biome.PLAINS;
            double arableBiomeFactor = switch (biome) {
                case MOUNTAINS -> 0.35;
                case HILLS, BEACH -> 0.55;
                case TUNDRA, DESERT, SNOW -> 0.15;
                default -> 1.0;
            };
            double agriBiomass = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                    ? cell.getBiomassAgriculture()
                    : Math.min(2000.0 * areaScale * arableBiomeFactor, Math.max(50.0 * areaScale * arableBiomeFactor, pop * 2.5));

            double techLandCeiling = Math.max(pop * (400.0 + 50.0 * Math.min(tech, 10.0)) * 1.5, (360000.0 + 550000.0 * Math.pow(Math.max(0.0, tech - 1.4), 1.5)) * areaScale * arableBiomeFactor);

            if (density > 50.0) {
                // High density forces multi-cropping / terrace irrigation (Stage 4)
                cell.setBiomassAgriculture(Math.min(techLandCeiling * 1.35, agriBiomass + (agriBiomass * 0.04 * deltaYears)));
                cell.setTechnologyLevel(Math.min(10.0, tech + 0.005 * deltaYears));
                // High labor demand absorbs work capacity
                double work = cell.getResourceWorkOr(50.0);
                cell.setResourceWork(Math.max(10.0, work * 0.98));
            } else if (density > 10.0) {
                // Moderate density: Annual cropping / draft animals (Stage 3)
                cell.setBiomassAgriculture(Math.min(techLandCeiling * 1.20, agriBiomass + (agriBiomass * 0.025 * deltaYears)));
                cell.setTechnologyLevel(Math.min(10.0, tech + 0.002 * deltaYears));
            } else if (density > 0.20 && tech >= 1.5) {
                // Early agrarian expansion: Forest-Fallow to Bush-Fallow & hydraulic paddy intensification (Stage 2)
                cell.setBiomassAgriculture(Math.min(techLandCeiling, agriBiomass + (agriBiomass * 0.018 * deltaYears)));
                cell.setTechnologyLevel(Math.min(3.5, tech + 0.0010 * deltaYears));
            } else if (tech >= 1.5 && (cell.getBiomassAgriculture() == null || cell.getBiomassAgriculture() <= 0.0)) {
                cell.setBiomassAgriculture(agriBiomass);
            }
        }
    }
}



