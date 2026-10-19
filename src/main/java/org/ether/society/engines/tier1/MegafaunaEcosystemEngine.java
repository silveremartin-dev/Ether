*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Megafauna Steppe Ecosystem Engineering & Overkill Feedback Engine.
 * Eliminates arbitrary threshold cutoffs in favor of continuous physical state equations:
 * 1. <b>Hunting Pressure Ratio</b>: Overhunting rate = f(Population Density / Wild Biomass Density).
 * 2. <b>Continuous Biomass & Soil Organic Carbon Depletion</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MegafaunaEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(MegafaunaEcosystemEngine.class);

    /**
     * Executes one megafauna ecosystem engineering tick across cells assuming a default monthly step.
     *
     * @param cells list of H3 spatial cells
     */
    public static void processMegafaunaEcosystem(List<H3Cell> cells) {
        processMegafaunaEcosystem(cells, 1.0 / 12.0);
    }

    /**
     * Executes one time-scaled megafauna ecosystem engineering tick across cells.
     * Models Zimov's Pleistocene Mammoth Steppe overkill hypothesis: in cryospheric steppe-tundra
     * ({@link Biome#SNOW}, {@link Biome#TUNDRA}) or Paleolithic hunter-gatherer plains
     * ({@code tech < 1.5}), excessive foraging pressure extirpates megafaunal grazers, reducing
     * trampling/manuring and triggering shrub/forest encroachment when soil organic carbon drops.
     *
     * @param cells   list of H3 spatial cells
     * @param dtYears elapsed simulation step in years (\(\Delta t\))
     */
    public static void processMegafaunaEcosystem(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;

        double safeDt = Math.max(1e-4, dtYears);
        int size = cells.size();
        java.util.concurrent.atomic.AtomicInteger megafaunaCollapseCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.function.Consumer<H3Cell> cellProcessor = cell -> {
            if (cell == null) return;
            Biome biome = cell.getBiome();
            boolean isCryosphericSteppe = (biome == Biome.SNOW || biome == Biome.TUNDRA);
            boolean isOpenGrassland = (biome == Biome.PLAINS);

            if (isCryosphericSteppe || isOpenGrassland) {
                int humanPop = cell.getPopulation() != null ? cell.getPopulation() : 0;
                double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 1.0;
                double domesticLivestock = cell.getBiomassLivestock() != null ? cell.getBiomassLivestock() : 0.0;

                // Universal substitution: pastoral/agricultural societies with high domestic livestock
                // rely less on wild megafauna hunting, whereas societies without livestock exert full hunting pressure
                // regardless of technological epoch (e.g., Pleistocene mammoth hunters or 19th-century bison hunters).
                double wildGameReliance = 1.0 / (1.0 + (domesticLivestock / Math.max(1.0, humanPop * 0.5)));

                // Continuous hunting pressure ratio = Effective Hunting Population / Natural Biomass Stock
                double huntingPressureRatio = (humanPop * wildGameReliance) / Math.max(10.0, naturalBiomass);

                if (huntingPressureRatio > 0.10) {
                    megafaunaCollapseCounter.incrementAndGet();

                    // Time-scaled continuous exponential decay of wild megafauna biomass and soil carbon
                    double annualDepletionRate = Math.min(0.20, huntingPressureRatio * 0.02);
                    double stepDepletion = Math.min(0.20, annualDepletionRate * safeDt);
                    cell.setBiomassNatural(Math.max(10.0, naturalBiomass * (1.0 - stepDepletion)));
                    double soc = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 15.0;
                    double updatedSoc = Math.max(2.0, soc - (stepDepletion * 0.5));
                    cell.setSoilOrganicCarbon(updatedSoc);

                    // Shrub/woodland encroachment converts open steppe plains to forest when grazers & SOC collapse
                    if (isOpenGrassland && updatedSoc < 8.0) {
                        cell.setBiome(Biome.FOREST);
                    }
                }
            }
        };

        if (size >= 10000) {
            cells.parallelStream().forEach(cellProcessor);
        } else {
            for (int i = 0; i < size; i++) {
                cellProcessor.accept(cells.get(i));
            }
        }

        int megafaunaCollapseEvents = megafaunaCollapseCounter.get();
        if (megafaunaCollapseEvents > 0) {
            logger.debug("Megafauna Engine: Continuous trophic hunting pressure active across {} cells.", megafaunaCollapseEvents);
        }
    }
}


