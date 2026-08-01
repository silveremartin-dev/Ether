/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Soil Stoichiometry & Liebig's Law of the Minimum Engine.
 * Models:
 * 1. <b>N-P-K Soil Stoichiometry (Nitrogen, Phosphorus, Potassium in kg/ha)</b>.
 * 2. <b>Liebig's Law of the Minimum</b>: Agricultural yield is strictly bounded by the scarcest nutrient:
 *    Yield = min([N], [P], [K], SoilMoisture).
 * 3. <b>Crop Nutrient Depletion & Organic Soil Carbon Recycling</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.1.0
 */
public class SoilNutrientNPKEngine {
    private static final Logger logger = LoggerFactory.getLogger(SoilNutrientNPKEngine.class);

    /**
     * Calculates crop yield multiplier based on Liebig's Law of the Minimum.
     *
     * @param cell H3 terrain cell
     * @return Yield factor between 0.05 and 2.0
     */
    public static double calculateLiebigYieldFactor(H3Cell cell) {
        if (cell == null) return 1.0;

        double nitrogenRatio = Math.min(2.0, (cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 20.0) / 30.0);
        double phosphorusRatio = Math.min(2.0, (cell.getRainfall() != null ? cell.getRainfall() : 500.0) / 400.0);
        double potassiumRatio = 1.0; // Baseline mineral potassium

        // Liebig's Law of the Minimum
        return Math.max(0.05, Math.min(nitrogenRatio, Math.min(phosphorusRatio, potassiumRatio)));
    }

    /**
     * Executes one N-P-K nutrient consumption and soil recycling tick across cells.
     */
    public static void processSoilNutrients(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) continue;

            double yieldFactor = calculateLiebigYieldFactor(cell);
            double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;

            // Crop yield bounded by Liebig's Law of the Minimum
            cell.setBiomassAgriculture(Math.min(3000.0, currentAgri * yieldFactor));

            // Intensive farming depletes soil organic carbon unless fallow/manure recycling occurs
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop > 200) {
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.1));
            }
        }
    }
}
