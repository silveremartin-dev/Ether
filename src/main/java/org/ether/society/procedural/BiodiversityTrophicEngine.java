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
 * Biodiversity Index & Trophic Cascade Simulation Engine.
 * Models:
 * 1. <b>Species Richness & Ecosystem Health (H')</b>: Evaluated from natural biomass, rainfall, and absence of toxic pollution.
 * 2. <b>Trophic Cascades</b>: Extinction of apex predators / pollinators from pollution or deforestation leads to herbivore overpopulation,
 *    causing topsoil loss, overgrazing, and accelerated desertification.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.9.0
 */
public class BiodiversityTrophicEngine {
    private static final Logger logger = LoggerFactory.getLogger(BiodiversityTrophicEngine.class);

    /**
     * Executes one biodiversity and trophic cascade tick across cells.
     */
    public static void processBiodiversityTrophicCascades(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int collapseCells = 0;

        for (H3Cell cell : cells) {
            if (cell.getElevation() <= 0 || cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                continue;
            }

            double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;

            // 1. Biodiversity Index H' calculation (0.0 degraded to 1.0 pristine)
            double biodiversityIndex = Math.min(1.0, (naturalBiomass / 1000.0) * Math.min(1.5, rain / 500.0) * Math.max(0.1, 1.0 - pollution / 500.0));

            // 2. Trophic Cascade Collapse Trigger (Biodiversity H' < 0.15)
            if (biodiversityIndex < 0.15 && cell.getBiome() != Biome.DESERT) {
                collapseCells++;
                // Ecosystem collapse degrades soil organic carbon and natural recovery speed
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.5));
                cell.setBiomassNatural(Math.max(0.0, naturalBiomass * 0.90));
            }
        }

        if (collapseCells > 0) {
            logger.info("Biodiversity Engine: Trophic cascade collapse affecting {} ecologically degraded cells.", collapseCells);
        }
    }
}
