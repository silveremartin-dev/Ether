/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wet-Bulb Temperature (T_wb) & Human Hyperthermia Limit Engine.
 * Models:
 * 1. <b>Magnus-Tetens Dew Point (T_d in °C)</b>: Dew point calculation from dry-bulb temperature (T) and relative humidity (RH).
 * 2. <b>Stull Wet-Bulb Temperature Formula (T_wb in °C)</b>.
 * 3. <b>Thermodynamic Hyperthermia Lethality</b>: T_wb ≥ 35.0°C causes 100% human mortality due to physical impossibility of evaporative cooling.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.2.0
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
     * Executes one wet-bulb temperature hyperthermia check across cells.
     */
    public static void processWetBulbHyperthermia(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int hyperthermiaCasualties = 0;

        for (H3Cell cell : cells) {
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 20.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double rh = Math.min(100.0, Math.max(20.0, (rain / 1200.0) * 100.0));

            double wetBulbTempC = calculateWetBulbTemperatureStull(tempC, rh);

            // Hyperthermia lethality limit (T_wb >= 35.0°C)
            if (wetBulbTempC >= 35.0 && cell.getPopulation() != null && cell.getPopulation() > 0) {
                hyperthermiaCasualties++;
                int pop = cell.getPopulation();
                int deaths = (int) (pop * 0.15); // 15% mortality rate per heatwave tick
                cell.setPopulation(Math.max(0, pop - deaths));
            }
        }

        if (hyperthermiaCasualties > 0) {
            logger.warn("Wet-Bulb Engine: Lethal hyperthermia event (T_wb >= 35°C) affecting {} populated cells.", hyperthermiaCasualties);
        }
    }
}
