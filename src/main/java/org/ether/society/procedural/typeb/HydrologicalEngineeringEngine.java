/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Anthropogenic Hydrological Landscape Engineering Engine.
 * 
 * Simulates major human-driven and climate-driven water engineering and landscape transformations:
 * 1. Tenochtitlan / Lake Texcoco Urban Reclamation: Draining and infilling shallow inland lakes
 *    to construct urban metropolises and agricultural chinampas (Tech >= 3.5, Capital >= 150.0).
 * 2. Aral Sea Endorheic Desiccation: Inland lake shrinkage resulting from intensive upstream agricultural
 *    water diversion and high net evaporation rates (Tech >= 3.0, low rainfall).
 * 3. Mountain Water Reservoirs & Hydroelectric Dams: Construction of high-elevation water retention dams
 *    and reservoirs in rugged terrain (Elevation >= 300m, Tech >= 4.5, Capital >= 250.0), stabilizing
 *    regional water security and generating hydroelectric energy.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class HydrologicalEngineeringEngine {
    private static final Logger logger = LoggerFactory.getLogger(HydrologicalEngineeringEngine.class);

    public static final double DEFAULT_TENOCHTITLAN_TECH_THRESHOLD = 3.5;
    public static final double DEFAULT_TENOCHTITLAN_CAPITAL_THRESHOLD = 150.0;

    public static final double DEFAULT_ARAL_SEA_RAINFALL_THRESHOLD_MM = 300.0;

    public static final double DEFAULT_DAM_TECH_THRESHOLD = 4.5;
    public static final double DEFAULT_DAM_CAPITAL_THRESHOLD = 250.0;
    public static final double DEFAULT_DAM_MIN_ELEVATION_METERS = 300.0;

    /**
     * Executes hydrological engineering transformations using default parameters.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        processHybrid(cells, timeStepDays,
                DEFAULT_TENOCHTITLAN_TECH_THRESHOLD, DEFAULT_TENOCHTITLAN_CAPITAL_THRESHOLD,
                DEFAULT_ARAL_SEA_RAINFALL_THRESHOLD_MM,
                DEFAULT_DAM_TECH_THRESHOLD, DEFAULT_DAM_CAPITAL_THRESHOLD, DEFAULT_DAM_MIN_ELEVATION_METERS);
    }

    /**
     * Executes hydrological engineering transformations with parameterizable physical thresholds.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays,
                                    double tenochtitlanTech, double tenochtitlanCapital,
                                    double aralRainfallThreshold,
                                    double damTech, double damCapital, double damMinElevation) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            int pop = cell.getPopulation();
            Biome biome = cell.getBiome();

            // 1. Tenochtitlan / Lake Texcoco Urban Reclamation & Wetland Drainage
            if (biome == Biome.LAKE && pop >= 1500) {
                if (tech >= tenochtitlanTech && capital >= tenochtitlanCapital) {
                    cell.setBiome(Biome.PLAINS);
                    cell.setIsPolder(true);
                    cell.setFoodResource(Math.min(1000.0, cell.getFoodResource() + 50.0 * (timeStepDays / 30.0)));
                    cell.setSoilOrganicCarbon(Math.max(100.0, cell.getSoilOrganicCarbon() + 15.0 * (timeStepDays / 30.0)));
                    logger.info("🏞️ Hydrological Reclamation (Tenochtitlan Model): Cell {} drained Lake Texcoco for urban expansion.", cell.getH3Index());
                }
            }

            // 2. Aral Sea Endorheic Desiccation & Water Diversion
            if (biome == Biome.LAKE && rainfall < aralRainfallThreshold && tempC >= 10.0) {
                // Intensive irrigation water extraction by surrounding agricultural population
                if (tech >= 3.0 && capital >= 100.0) {
                    cell.setWaterResource(Math.max(10.0, cell.getWaterResource() - 20.0 * (timeStepDays / 30.0)));
                    if (cell.getWaterResource() <= 50.0) {
                        cell.setBiome(Biome.DESERT);
                        cell.setMovementFriction(Math.min(10.0, cell.getMovementFriction() * 1.35)); // Salt flat / dust storm friction
                        logger.info("🏜️ Endorheic Desiccation (Aral Sea Model): Cell {} lake basin desiccated into salt desert.", cell.getH3Index());
                    }
                }
            }

            // 3. Mountain Water Reservoirs & Hydroelectric Dam Construction
            if (elevation >= damMinElevation && (cell.getMovementFriction() >= 1.3 || elevation >= 500.0)) {
                if (tech >= damTech && capital >= damCapital && !cell.getIsPolder()) {
                    // Dam reservoir created: stabilizes water security and generates hydroelectric power
                    cell.setWaterResource(1000.0);
                    cell.setEnergySolar(cell.getEnergySolar() + 100.0 * (timeStepDays / 30.0)); // Hydroelectric power boost
                    cell.setResourceWork(cell.getResourceWork() + 30.0 * (timeStepDays / 30.0));
                    logger.info("🏗️ Mountain Reservoir Dam Construction: Cell {} built hydroelectric water retention dam.", cell.getH3Index());
                }
            }
        }
    }
}

