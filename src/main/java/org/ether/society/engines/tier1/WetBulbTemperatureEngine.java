/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wet-Bulb Temperature (T_wb) & Human Hyperthermia Limit Engine.
 * Models:
 * 1. <b>Stull Wet-Bulb Temperature Formula (T_wb in °C)</b>: Psychrometric thermodynamic function of dry-bulb temperature (T) and relative humidity (RH).
 * 2. <b>Continuous Hyperthermia Lethality Hazard</b>: Sherwood & Huber (2010), mortality grows exponentially beyond physiological skin cooling limit (T_wb >= 35.0°C).
 * 3. <b>Labor Productivity Attenuation</b>: Dunne et al. (2013), physical work capacity drops progressively between 25°C and 35°C wet-bulb.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WetBulbTemperatureEngine {
    private static final Logger logger = LoggerFactory.getLogger(WetBulbTemperatureEngine.class);

    /**
     * Calculates Stull Wet-Bulb Temperature (T_wb in °C) from Dry-Bulb Temperature T (°C) and Relative Humidity RH (%).
     *
     * @param tempC Dry-bulb temperature in °C
     * @param relativeHumidityPercent Relative humidity (0 to 100)
     * @return Wet-bulb temperature in °C
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

    /**
     * Executes one wet-bulb temperature hyperthermia check across cells (monthly default).
     */
    public static void processWetBulbHyperthermia(List<H3Cell> cells) {
        processWetBulbHyperthermia(cells, 1.0 / 12.0);
    }

    /**
     * Executes wet-bulb hyperthermia and labor attenuation across cells with explicit time integration.
     *
     * @param cells spatial cells
     * @param deltaYears time step in years
     */
    public static void processWetBulbHyperthermia(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        int hyperthermiaCasualties = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double tempC = cell.getTemperatureOr(20.0);
            double rain = cell.getRainfallOr(500.0);
            double rh = Math.min(100.0, Math.max(20.0, (rain / 1200.0) * 100.0));

            double wetBulbTempC = calculateWetBulbTemperatureStull(tempC, rh);

            // Labor productivity attenuation (Dunne et al. 2013): progressive loss of physical work capacity above 25°C
            if (wetBulbTempC > 25.0) {
                double work = cell.getResourceWorkOr(0.0);
                if (work > 0.0) {
                    double workLossFactor = 0.50 / (1.0 + Math.exp(-(wetBulbTempC - 31.0) / 1.5));
                    cell.setResourceWork(Math.max(0.0, work * (1.0 - Math.clamp(workLossFactor * deltaYears * 12.0, 0.0, 0.90))));
                }
            }

            // Continuous hyperthermia lethality limit (Sherwood & Huber 2010): T_wb >= 35.0°C
            if (wetBulbTempC >= 35.0 && cell.getPopulation() != null && cell.getPopulation() > 0) {
                hyperthermiaCasualties++;
                int pop = cell.getPopulation();
                // Exponential mortality hazard beyond 35°C, scaled by heatwave exposure duration (deltaYears * 12 months)
                double mortalityHazard = 1.0 - Math.exp(-0.25 * (wetBulbTempC - 35.0));
                double exposureScale = Math.clamp(deltaYears * 12.0, 0.05, 1.0);
                int deaths = (int) Math.round(pop * mortalityHazard * exposureScale);
                cell.setPopulation(Math.max(0, pop - deaths));
            }
        }

        if (hyperthermiaCasualties > 0) {
            logger.warn("Wet-Bulb Engine: Lethal hyperthermia event (T_wb >= 35°C) affecting {} populated cells.", hyperthermiaCasualties);
        }
    }
}
