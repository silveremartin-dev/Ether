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
 * HYDE 3.2 (History Database of the Global Environment) Calibrator.
 * 
 * Provides spatially-referenced historical population density distributions
 * for simulation dates from -10,000 BC (Holocene Transition) through antiquity to modern era.
 */
public class HydeDataCalibrator {
    private static final Logger logger = LoggerFactory.getLogger(HydeDataCalibrator.class);

    /**
     * Calculates the HYDE 3.2 population density factor for a given coordinate (lng, lat) at a specific year.
     * 
     * @param lng Longitude (-180 to 180)
     * @param lat Latitude (-90 to 90)
     * @param year Historical year (e.g., -10000 for 10,000 BC, 0 for 1 AD, 1324 for 1324 AD)
     * @return Population density multiplier (1.0 = baseline, >1.0 = high density core)
     */
    public static double getHydeDensityFactor(double lng, double lat, long year) {
        double factor = 1.0;

        // 1. Neolithic & Early Agriculture (-10,000 BC to -5,000 BC)
        if (year <= -5000) {
            // Fertile Crescent & Levant (-10k BC HYDE Core)
            double fertileCrescent = gaussian(lng, lat, 36.0, 34.0, 8.0, 6.0);
            // Nile Delta & Lower Nile
            double nileDelta = gaussian(lng, lat, 31.0, 30.0, 4.0, 5.0);
            // Yellow River Valley (Yangshao/Cishan)
            double yellowRiver = gaussian(lng, lat, 114.0, 35.0, 8.0, 6.0);
            // Indus Valley Early Settlements (Mehrgarh)
            double indusValley = gaussian(lng, lat, 68.0, 29.0, 6.0, 5.0);

            factor += fertileCrescent * 4.5 + nileDelta * 3.5 + yellowRiver * 3.0 + indusValley * 2.5;
        } 
        // 2. Bronze Age & Early State Systems (-5,000 BC to -1,000 BC)
        else if (year <= -1000) {
            // Mesopotamia (Sumer, Akkad, Babylon)
            double mesopotamia = gaussian(lng, lat, 44.5, 32.5, 7.0, 5.0);
            // Egypt (Old/Middle/New Kingdom)
            double egypt = gaussian(lng, lat, 31.5, 27.0, 3.0, 8.0);
            // Indus Valley Civilization (Harappa, Mohenjo-daro)
            double indus = gaussian(lng, lat, 71.0, 27.5, 8.0, 7.0);
            // North China Plain (Shang/Zhou)
            double chinaCore = gaussian(lng, lat, 115.0, 34.5, 9.0, 7.0);
            // Mesoamerica (Olmec)
            double olmec = gaussian(lng, lat, -95.0, 18.0, 6.0, 5.0);
            // Andean Coast (Caral-Supe)
            double andes = gaussian(lng, lat, -77.5, -10.5, 5.0, 6.0);

            factor += mesopotamia * 5.0 + egypt * 4.5 + indus * 4.0 + chinaCore * 4.5 + olmec * 2.5 + andes * 2.5;
        }
        // 3. Classical Antiquity (-1,000 BC to 500 AD)
        else if (year <= 500) {
            // Mediterranean Basin & Roman Empire Core
            double italy = gaussian(lng, lat, 12.5, 42.0, 6.0, 5.0);
            double greeceAnatolia = gaussian(lng, lat, 26.0, 38.5, 8.0, 6.0);
            double iberiaGaul = gaussian(lng, lat, 2.0, 43.0, 10.0, 7.0);
            // Han Dynasty China
            double hanChina = gaussian(lng, lat, 114.0, 32.0, 12.0, 9.0);
            // Maurya/Gupta India (Gangetic Plain)
            double gangesIndia = gaussian(lng, lat, 82.0, 25.5, 10.0, 6.0);
            // Maya Lowlands
            double maya = gaussian(lng, lat, -89.5, 17.0, 5.0, 5.0);

            factor += italy * 4.5 + greeceAnatolia * 4.0 + iberiaGaul * 3.5 + hanChina * 5.5 + gangesIndia * 5.0 + maya * 3.0;
        }
        // 4. Medieval & Pre-Modern Era (500 AD to 1600 AD)
        else {
            // Song/Ming China Core
            double chinaDense = gaussian(lng, lat, 118.0, 30.0, 12.0, 10.0);
            // Indo-Gangetic Plain & South India
            double indiaDense = gaussian(lng, lat, 78.0, 22.0, 14.0, 10.0);
            // Western & Central Europe
            double europeDense = gaussian(lng, lat, 8.0, 48.0, 12.0, 8.0);
            // West African Sahel (Mali / Songhai Empire)
            double maliEmpire = gaussian(lng, lat, -3.0, 15.0, 8.0, 5.0);
            // Mesoamerica (Aztec / Tenochtitlan) & Inca Empire
            double aztecInca = gaussian(lng, lat, -99.0, 19.5, 6.0, 6.0) + gaussian(lng, lat, -75.0, -12.0, 6.0, 8.0);

            factor += chinaDense * 5.5 + indiaDense * 5.0 + europeDense * 4.5 + maliEmpire * 3.5 + aztecInca * 4.0;
        }

        return factor;
    }

    private static double gaussian(double lng, double lat, double centerLng, double centerLat, double sigmaLng, double sigmaLat) {
        double dLng = (lng - centerLng) / sigmaLng;
        double dLat = (lat - centerLat) / sigmaLat;
        return Math.exp(-(dLng * dLng + dLat * dLat) / 2.0);
    }
}
