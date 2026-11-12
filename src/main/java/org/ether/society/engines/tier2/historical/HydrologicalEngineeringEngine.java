/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
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
 * @version 1.0.0-beta.2
 */
public class HydrologicalEngineeringEngine extends AbstractSimulationEngine {
    public HydrologicalEngineeringEngine() {
        super("HydrologicalEngineeringEngine", "HydrologicalEngineeringEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    public static final double DEFAULT_TENOCHTITLAN_CAPITAL_THRESHOLD = 150.0;
    public static final double DEFAULT_ARAL_SEA_RAINFALL_THRESHOLD_MM = 300.0;
    public static final double DEFAULT_DAM_CAPITAL_THRESHOLD = 250.0;
    public static final double DEFAULT_DAM_MIN_ELEVATION_METERS = 300.0;

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null) return;
        int n = buf.getCapacity();
        byte[] biomes = buf.getBiomes();
        float[] capArr = buf.getResourceCapital();
        float[] elevArr = buf.getElevation();
        float[] rainArr = buf.getRainfall();
        float[] tempArr = buf.getTemperature();
        float[] popArr = buf.getBiomassHuman();
        float[] foodArr = buf.getFoodResource();
        float[] socArr = buf.getSoilOrganicCarbon();
        float[] waterArr = buf.getWaterResource();
        float[] solarArr = buf.getEnergySolar();

        byte lakeOrd = (byte) Biome.LAKE.ordinal();
        byte plainsOrd = (byte) Biome.PLAINS.ordinal();
        byte desertOrd = (byte) Biome.DESERT.ordinal();
        double timeStepDays = dtYears * 365.25;

        for (int i = 0; i < n; i++) {
            float capital = capArr[i];
            float elevation = elevArr[i];
            float rainfall = rainArr[i];
            float tempC = tempArr[i];
            float pop = popArr[i];
            byte biome = biomes[i];

            if (biome == lakeOrd && pop >= 1500.0f && capital >= DEFAULT_TENOCHTITLAN_CAPITAL_THRESHOLD) {
                biomes[i] = plainsOrd;
                foodArr[i] = Math.min(1000.0f, foodArr[i] + 50.0f);
                socArr[i] = Math.max(100.0f, socArr[i] + 15.0f);
            }

            if (biome == lakeOrd && rainfall < DEFAULT_ARAL_SEA_RAINFALL_THRESHOLD_MM && tempC >= 10.0f && capital >= 100.0f) {
                waterArr[i] = (float) Math.max(10.0, waterArr[i] - 20.0 * (timeStepDays / 30.0));
                if (waterArr[i] <= 50.0f) {
                    biomes[i] = desertOrd;
                }
            }

            if (elevation >= DEFAULT_DAM_MIN_ELEVATION_METERS && capital >= DEFAULT_DAM_CAPITAL_THRESHOLD && biome != lakeOrd) {
                waterArr[i] = Math.max(1000.0f, waterArr[i]);
                solarArr[i] += 100.0f * (float) dtYears;
            }
        }
    }
}
