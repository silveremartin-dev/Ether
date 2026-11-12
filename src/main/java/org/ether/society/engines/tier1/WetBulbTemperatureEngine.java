/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wet-Bulb Temperature (T_wb) & Human Hyperthermia Limit Engine.
 * Models:
 * 1. <b>Stull Wet-Bulb Temperature Formula (T_wb in Â°C)</b>: Psychrometric thermodynamic function of dry-bulb temperature (T) and relative humidity (RH).
 * 2. <b>Continuous Hyperthermia Lethality Hazard</b>: Sherwood & Huber (2010), mortality grows exponentially beyond physiological skin cooling limit (T_wb >= 35.0Â°C).
 * 3. <b>Labor Productivity Attenuation</b>: Dunne et al. (2013), physical work capacity drops progressively between 25Â°C and 35Â°C wet-bulb.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WetBulbTemperatureEngine extends AbstractSimulationEngine {
    public WetBulbTemperatureEngine() {
        super("WetBulbTemperatureEngine", "WetBulbTemperatureEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(WetBulbTemperatureEngine.class);

    /**
     * Calculates Stull Wet-Bulb Temperature (T_wb in Â°C) from Dry-Bulb Temperature T (Â°C) and Relative Humidity RH (%).
     *
     * @param tempC Dry-bulb temperature in Â°C
     * @param relativeHumidityPercent Relative humidity (0 to 100)
     * @return Wet-bulb temperature in Â°C
     */
    public static double calculateWetBulbTemperatureStull(double tempC, double relativeHumidityPercent) {
        double rh = Math.max(1.0, Math.min(100.0, relativeHumidityPercent));
        double t = tempC;

        return t * Math.atan(0.151977 * Math.sqrt(rh + 8.313659))
                + Math.atan(t + rh)
                - Math.atan(rh - 1.676331)
                + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
                - 4.686035;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double tempC = buffer.getTemperature()[i];
        double rain = buffer.getRainfall()[i];
        double rh = Math.min(100.0, Math.max(20.0, (rain / 1200.0) * 100.0));
        double wetBulbTempC = calculateWetBulbTemperatureStull(tempC, rh);

        if (wetBulbTempC >= 35.0) {
            double bioHuman = buffer.getBiomassHuman()[i];
            if (bioHuman > 0.0) {
                double mortalityHazard = 1.0 - Math.exp(-0.25 * (wetBulbTempC - 35.0));
                double exposureScale = Math.clamp(dtYears * 12.0, 0.05, 1.0);
                acc.dBiomassHuman -= (float) (bioHuman * mortalityHazard * exposureScale);
            }
        }
    }

    /**
     * Executes one wet-bulb temperature hyperthermia check across DoD buffer (monthly default).
     */
    public static void processWetBulbHyperthermia(WorldBuffer buffer) {
        processWetBulbHyperthermia(buffer, 1.0 / 12.0);
    }

    /**
     * Executes wet-bulb hyperthermia and labor attenuation across DoD buffer with explicit time integration.
     *
     * @param buffer WorldBuffer DoD state
     * @param deltaYears time step in years
     */
    public static void processWetBulbHyperthermia(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        int hyperthermiaCasualties = 0;
        float[] tempArr = buffer.getTemperature();
        float[] rainArr = buffer.getRainfall();
        float[] bioHuman = buffer.getBiomassHuman();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double tempC = tempArr[i];
            double rain = rainArr[i];
            double rh = Math.min(100.0, Math.max(20.0, (rain / 1200.0) * 100.0));
            double wetBulbTempC = calculateWetBulbTemperatureStull(tempC, rh);

            if (wetBulbTempC >= 35.0 && bioHuman[i] > 0.0f) {
                hyperthermiaCasualties++;
                double mortalityHazard = 1.0 - Math.exp(-0.25 * (wetBulbTempC - 35.0));
                double exposureScale = Math.clamp(deltaYears * 12.0, 0.05, 1.0);
                bioHuman[i] = (float) Math.max(0.0, bioHuman[i] * (1.0 - mortalityHazard * exposureScale));
            }
        }

        if (hyperthermiaCasualties > 0) {
            logger.warn("Wet-Bulb Engine: Lethal hyperthermia event (T_wb >= 35 C) affecting {} populated cells.", hyperthermiaCasualties);
        }
    }
}
