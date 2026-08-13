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
 * Dynamic Albedo, Orbital Precession (Green Sahara) & Climate Disaster Engine.
 * Modifies local surface albedo, orbital precession cycles, and climate catastrophe events:
 * <ul>
 *   <li><b>Périodes Humides / Sahara Vert (Orbital Precession)</b>: Simulates Milankovitch precession cycles
 *       (21,000-year cycle). Peak orbital insolation intensifies sub-tropical monsoons, transforming continental
 *       deserts (12°N to 30°N) into fertile grassland/savanna and filling mega-lakes & aquifers.</li>
 *   <li><b>Catastrophes Climatiques (Climate Disasters)</b>:
 *       <ul>
 *         <li><b>Hiver Volcanique (Volcanic Winter)</b>: Stratospheric SO₂ aerosol cooling lowering global temperatures.</li>
 *         <li><b>Inondations & Mégatempêtes (Floods & Megastorms)</b>: Coastal surges damaging infrastructure and triggering water-borne diseases.</li>
 *       </ul>
 *   </li>
 *   <li><b>Rétroaction Albédo-Température</b>: Vegetation expansion lowers albedo, amplifying local warming/humidity; ice cover increases albedo, cooling.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.6.0
 */
public class AlbedoClimateEngine {
    private static final Logger logger = LoggerFactory.getLogger(AlbedoClimateEngine.class);

    /**
     * Legacy convenience method for updating albedo feedback.
     */
    public static void processAlbedoFeedback(List<H3Cell> cells) {
        processAlbedoAndClimateEvents(cells, 0L, 0.0);
    }

    /**
     * Updates dynamic albedo, orbital precession (Green Sahara), and climate disaster events.
     *
     * @param cells                 List of H3 simulation cells
     * @param simulationYear        Current simulation year (for Milankovitch orbital cycle calculation)
     * @param volcanicCoolingOffset Active volcanic aerosol cooling in °C (0.0 if normal, -2.0 to -6.0 during volcanic winters)
     */
    public static void processAlbedoAndClimateEvents(List<H3Cell> cells, long simulationYear, double volcanicCoolingOffset) {
        if (cells == null || cells.isEmpty()) return;

        // 1. Calculate Orbital Precession Phase (21,000-year Milankovitch cycle)
        double precessionPhase = Math.sin((simulationYear % 21000L) / 21000.0 * 2.0 * Math.PI);

        int greenSaharaCells = 0;
        int floodDisasterCells = 0;

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            if (elev <= 0 || cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                cell.setDynamicAlbedo(0.06);
                continue;
            }

            // ── 1. Orbital Precession / Continuous Subtropical Monsoon Forcing ─
            // Continuous physical insolation curve across subtropical latitudes (5°N to 35°N)
            double latitudinalMonsoonWeight = (lat >= 5.0 && lat <= 35.0) 
                    ? Math.sin(Math.toRadians((lat - 5.0) / 30.0 * 180.0)) 
                    : 0.0;
            double continuousMonsoonForcing = latitudinalMonsoonWeight * Math.max(0.0, precessionPhase);

            if (continuousMonsoonForcing > 0.05 && cell.getBiome() == Biome.DESERT) {
                greenSaharaCells++;
                // Continuous monsoon penetration: increase rainfall, aquifers, and soil carbon proportional to solar insolation
                cell.setRainfall(Math.min(1200.0, cell.getRainfall() + 500.0 * continuousMonsoonForcing));
                cell.setAccessibleAquifer(Math.min(15000.0, cell.getAccessibleAquifer() + 2000.0 * continuousMonsoonForcing));
                cell.setWaterResource(Math.min(1000.0, cell.getWaterResource() + 400.0 * continuousMonsoonForcing));

                // Gradual ecological biome transition based on cumulative forcing
                if (continuousMonsoonForcing > 0.3) {
                    cell.setBiome(Biome.PLAINS);
                }
                cell.setSoilOrganicCarbon(Math.min(60.0, cell.getSoilOrganicCarbon() + 15.0 * continuousMonsoonForcing));
                cell.setBiomassNatural(Math.min(800.0, cell.getBiomassNatural() + 300.0 * continuousMonsoonForcing));
            }

            // ── 2. Albedo Radiative Feedback Loop ─────────────────────────────
            double albedo = cell.calculateDynamicAlbedo();
            double currentTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            // Radiative forcing feedback: high albedo reflects heat, low albedo absorbs heat
            double albedoDelta = (0.30 - albedo) * 2.5;

            // ── 3. Apply Volcanic Aerosol Cooling (Catastrophe Climatique) ────
            double updatedTemp = currentTemp + (albedoDelta * 0.08) + volcanicCoolingOffset;
            cell.setTemperature(Math.max(-250.0, Math.min(600.0, updatedTemp)));

            // ── 4. Megastorm / Flood Disaster Impact ──────────────────────────
            if (cell.getRainfall() > 900.0 && elev < 30.0 && cell.getBiome() == Biome.BEACH) {
                floodDisasterCells++;
                // Destroy infrastructure & capital
                cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * 0.70));
                // Contaminate water temporarily
                cell.setWaterResource(Math.max(50.0, cell.getWaterResource() * 0.50));
            }
        }

        if (greenSaharaCells > 0 || floodDisasterCells > 0 || volcanicCoolingOffset < -0.5) {
            logger.info("Climate Engine: Green Sahara active across {} cells, {} flood disaster cells, Volcanic cooling offset: {}°C",
                    greenSaharaCells, floodDisasterCells, String.format("%.2f", volcanicCoolingOffset));
        }
    }
}
