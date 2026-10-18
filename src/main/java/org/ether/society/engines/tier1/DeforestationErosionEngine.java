/*
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
 * Deforestation Soil Erosion & Root Retention Loss Engine.
 * Models:
 * 1. <b>Root Retention Loss</b>: Clear-cutting timber biomass removes root soil binding.
 * 2. <b>Runoff Soil Carbon Washout</b>: Rainfall on deforested slopes washes away soil organic carbon (SOC).
 * 3. <b>Arid Biome Degradation</b>: Severe SOC loss converts fertile forest/hills into barren desert terrain.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class DeforestationErosionEngine {
    private static final Logger logger = LoggerFactory.getLogger(DeforestationErosionEngine.class);

    public static void processDeforestationErosion(List<H3Cell> cells) {
        processDeforestationErosion(cells, 1.0 / 12.0);
    }

    /*
     * Executes one deforestation soil erosion tick across cells.
     */
    public static void processDeforestationErosion(List<H3Cell> cells, double dtYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        int size = cells.size();
        H3Cell sample = cells.get(0);
        int h3Res = (sample != null && sample.getH3Index() != null && sample.getH3Index() != 0L)
                ? (int) ((sample.getH3Index() >>> 52) & 0xFL)
                : 3;
        double planarCellAreaKm2 = H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3);
        double areaScale = Math.max(0.05, planarCellAreaKm2 / 1770.49);
        double canopyThreshold = 100.0;
        double treeLossThreshold = 50.0;

        java.util.concurrent.atomic.AtomicInteger erodedCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.function.Consumer<H3Cell> cellProcessor = cell -> {
            if (cell == null) return;
            double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;
            double woodBiomass = cell.getWoodResource() != null ? (cell.getWoodResource() / Math.max(0.01, areaScale)) : naturalBiomass;
            double effectiveCanopy = Math.max(naturalBiomass, woodBiomass);
            double soc = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 20.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Severe deforestation on sloped terrain with high rainfall causes erosion
            if (effectiveCanopy < canopyThreshold && elev > 200.0 && rain > 400.0) {
                erodedCounter.incrementAndGet();
                double socLoss = (rain / 1000.0) * 0.5 * dtMonthlyRatio;
                double newSoc = Math.max(2.0, soc - socLoss);
                cell.setSoilOrganicCarbon(newSoc);

                // Soil degradation converts deforested terrain to arid desert if SOC < 5.0
                if (newSoc < 5.0) {
                    Biome currentBiome = cell.getBiome();
                    if (currentBiome == Biome.HILLS || currentBiome == Biome.FOREST || currentBiome == Biome.JUNGLE
                            || currentBiome == Biome.PLAINS || currentBiome == Biome.SAVANNAH) {
                        cell.setBiome(Biome.DESERT);
                    }
                } else if (effectiveCanopy < treeLossThreshold) {
                    // Tree cover lost but soil intact: converts dense forest/jungle to open grassland/hills
                    if (cell.getBiome() == Biome.FOREST || cell.getBiome() == Biome.JUNGLE) {
                        cell.setBiome(elev > 500.0 ? Biome.HILLS : Biome.PLAINS);
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

        int erodedCells = erodedCounter.get();
        if (erodedCells > 0) {
            logger.debug("Deforestation Erosion Engine: Soil carbon runoff active across {} deforested sloped cells.", erodedCells);
        }
    }
}


