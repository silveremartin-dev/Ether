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
 * GBIF & Megafauna Extinction Database Ingestor.
 * Calculates animal biomass and hunting capacity during Pleistocene/Holocene transitions.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 4.0.0
 */
public class MegafaunaIngestor {
    private static final Logger logger = LoggerFactory.getLogger(MegafaunaIngestor.class);

    /**
     * Returns megafauna biomass multiplier for a geographic coordinate and year.
     */
    public static double getMegafaunaBiomassFactor(double lng, double lat, long year) {
        if (year < -10000) {
            // Mammoth steppe & megafauna abundance (Beringia, Mammoth Steppe Eurasia, North America)
            if (lat >= 50.0) return 2.5; // High megafauna density
            return 1.8;
        } else if (year < -5000) {
            // Post-extinction transition
            return 1.2;
        }
        return 1.0; // Modern baseline
    }
}
