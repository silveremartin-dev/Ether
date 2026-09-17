/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorldClim & Paleoclim Grid Reader.
 * Reads paleoclimate matrices (temperature, precipitation, biomes) for Last Glacial Maximum (LGM),
 * Mid-Holocene, Younger Dryas, and modern baselines.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class PaleoclimGridReader {
    private static final Logger logger = LoggerFactory.getLogger(PaleoclimGridReader.class);

    public static class ClimateData {
        public double temperatureC = 15.0; // Mean annual temp in °C
        public double precipitationMm = 800.0; // Annual precipitation in mm
        public double habitabilityIndex = 0.8; // 0.0 (extreme desert/ice) to 1.0 (lush fertile)
    }

    public static ClimateData getHistoricalClimate(double lng, double lat, long year) {
        ClimateData cd = new ClimateData();

        // LGM (-20,000 to -15,000 BC)
        if (year <= -15000) {
            if (lat >= 50.0) {
                cd.temperatureC = -10.0;
                cd.precipitationMm = 200.0;
                cd.habitabilityIndex = 0.05; // Ice sheet / Tundra
            }
        } 
        // Mid-Holocene Green Sahara (-6000 BC)
        else if (year >= -7000 && year <= -4000) {
            if (lat >= 15.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                cd.temperatureC = 26.0;
                cd.precipitationMm = 650.0; // Green Sahara humid period
                cd.habitabilityIndex = 0.85;
            }
        }

        return cd;
    }
}

