/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import java.util.List;

/**
 * Planetary Boundaries Assessment Engine.
 * Evaluates the 9 Planetary Boundaries status (0.0 Safe -> 1.0 High Risk Boundary Exceeded).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class PlanetaryBoundariesEngine {

    public record BoundaryStatus(
        double climateChangeRisk,
        double biosphereIntegrityRisk,
        double freshwaterDepletionRisk,
        double biogeochemicalNPKRisk,
        double landSystemChangeRisk,
        double oceanAcidificationRisk,
        double stratosphericOzoneRisk,
        double atmosphericAerosolRisk,
        double chemicalPollutionRisk
    ) {}

    public static BoundaryStatus assessBoundaries(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            return new BoundaryStatus(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        }

        double totalPop = 0;
        double totalAquiferDeficit = 0;
        double totalSocDeficit = 0;
        double totalPollution = 0;
        double totalTemp = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            totalPop += cell.getPopulation() != null ? cell.getPopulation() : 0;
            totalAquiferDeficit += Math.max(0.0, 500.0 - (cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 500.0));
            totalSocDeficit += Math.max(0.0, 100.0 - (cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 100.0));
            totalPollution += cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            totalTemp += cell.getTemperature() != null ? cell.getTemperature() : 15.0;
        }

        double avgTemp = totalTemp / cells.size();
        double climateRisk = Math.min(1.0, Math.max(0.0, (avgTemp - 14.0) / 6.0));
        double freshwaterRisk = Math.min(1.0, (totalAquiferDeficit / cells.size()) / 400.0);
        double npkRisk = Math.min(1.0, (totalSocDeficit / cells.size()) / 80.0);
        double pollutionRisk = Math.min(1.0, (totalPollution / cells.size()) / 500.0);
        double biosphereRisk = Math.min(1.0, 1.0 - TrophicEcosystemEngine.getGlobalBiodiversityIndex());
        double aerosolRisk = Math.min(1.0, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() / 2.0);

        return new BoundaryStatus(
            climateRisk, biosphereRisk, freshwaterRisk, npkRisk,
            0.2 + 0.5 * climateRisk, 0.1 + 0.4 * climateRisk,
            0.1, aerosolRisk, pollutionRisk
        );
    }
}
