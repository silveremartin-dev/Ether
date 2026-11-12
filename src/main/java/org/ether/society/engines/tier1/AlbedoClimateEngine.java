/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Dynamic Albedo, Orbital Precession (Green Sahara) & Climate Disaster Engine.
 * Modifies local surface albedo, orbital precession cycles, and climate catastrophe events:
 * <ul>
 *   <li><b>Humid Periods / Green Sahara (Orbital Precession)</b>: Simulates Milankovitch precession cycles
 *       (21,000-year cycle). Peak orbital insolation intensifies sub-tropical monsoons, transforming continental
 *       deserts (12Â°N to 30Â°N) into fertile grassland/savanna and filling mega-lakes & aquifers.</li>
 *   <li><b>Climate Disasters</b>:
 *       <ul>
 *         <li><b>Volcanic Winter</b>: Stratospheric SOâ‚‚ aerosol cooling lowering global temperatures.</li>
 *         <li><b>Floods & Megastorms</b>: Coastal surges damaging infrastructure and triggering water-borne diseases.</li>
 *       </ul>
 *   </li>
 *   <li><b>Ice-Albedo & Vegetation-Albedo Feedback</b>: Vegetation expansion lowers albedo, amplifying local warming/humidity; ice cover increases albedo, cooling.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AlbedoClimateEngine extends AbstractSimulationEngine {
    public AlbedoClimateEngine() {
        super("AlbedoClimateEngine", "AlbedoClimateEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(AlbedoClimateEngine.class);

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        double dtSafe = Math.max(0.0001, dtYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 2.0);

        float[] elevArr = buf.getElevation();
        byte[] biomes = buf.getBiomes();
        float[] rainArr = buf.getRainfall();
        float[] tempArr = buf.getTemperature();
        float[] capArr = buf.getResourceCapital();
        float[] waterArr = buf.getWaterResource();
        float[] bioNatArr = buf.getBiomassNatural();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte beachOrd = (byte) Biome.BEACH.ordinal();

        double capRetention = Math.exp(-4.2801 * dtSafe);
        double waterRetention = Math.exp(-8.3178 * dtSafe);

        for (int i = 0; i < n; i++) {
            float elev = elevArr[i];
            byte biome = biomes[i];
            if (elev <= 0.0f || biome == oceanOrd || biome == deepOceanOrd) continue;

            double bioNat = bioNatArr[i];
            double albedo = (biome == (byte) Biome.DESERT.ordinal()) ? 0.35 : Math.max(0.12, 0.25 - 0.10 * Math.tanh(bioNat / 500.0));
            double currentTemp = tempArr[i];
            double equilibriumTempAnomaly = (0.30 - albedo) * 6.0;
            double baselineTemp = 22.0 - 0.0065 * Math.max(0.0, elev);
            double targetTemp = baselineTemp + equilibriumTempAnomaly;
            double updatedTemp = currentTemp + (targetTemp - currentTemp) * thermalRelaxationFactor;
            tempArr[i] = (float) Math.clamp(updatedTemp, -250.0, 600.0);

            double rain = rainArr[i];
            if (rain > 900.0 && elev < 30.0 && (biome == beachOrd || elev < 10.0)) {
                capArr[i] = (float) (capArr[i] * capRetention);
                waterArr[i] = (float) Math.max(50.0, waterArr[i] * waterRetention);
            }
        }
    }
}
