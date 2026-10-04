/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Analytical $R_0$ Epidemiological & Pathogen Stress Generator.
 * Computes continuous biophysical transmission potential $R_0(x, y, t)$ combining:
 * 1. Smooth thermal and tropical vector performance (Mordecai et al. Malaria, Dengue, Yellow Fever).
 * 2. Major hyper-endemic river basins (Amazon, Congo, Ganges, Niger, Mekong, Sundaland).
 * 3. Old World zoonotic crowd disease reservoirs in high-density agrarian hearths.
 * 4. Continuous orographic suppression (smooth high-altitude attenuation, no rectangular boxes).
 * 5. Pre-1492 immunological isolation of Americas and Oceania with post-1492 Columbian epidemic shock.
 * 6. Historical epidemic pandemic pulses (Justinian 536 AD, Black Death 1347 AD, Columbian 1492, Spanish Flu 1918).
 * 7. Modern epidemiological transition & public health sanitation suppression post-1900.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 2.0.0
 */
public class AnalyticalEpidemiologyModel {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticalEpidemiologyModel.class);

    /*
     * Generates an authentic continuous $R_0$ pathogen stress raster for the specified epoch.
     *
     * @param year Historical epoch year
     * @param width Image width
     * @param height Image height
     * @param elevationMask Land/ocean mask
     * @return Grayscale BufferedImage representing pathogen intensity (0-255)
     */
    public static BufferedImage generatePathogenMap(long year, int width, int height, BufferedImage elevationMask) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Active historical pandemic outbreak pulses
        double[][] activeEpidemics = getEpidemicOutbreaksForYear(year);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            double absLat = Math.abs(lat);

            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (elevationMask != null ? elevationMask.getWidth() : width) / width), 0, (elevationMask != null ? elevationMask.getWidth() : width) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (elevationMask != null ? elevationMask.getHeight() : height) / height), 0, (elevationMask != null ? elevationMask.getHeight() : height) - 1);
                int landSample = (elevationMask != null) ? elevationMask.getRaster().getSample(mx, my, 0) : 255;

                if (landSample == 0 || lat < -60.0 || !HistoricalMapGenerator.isLand(lon, lat)) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                // --- 1. Continuous Tropical Vector Burden (Malaria, Arboviruses, Yellow Fever) ---
                // Smooth Gaussian thermal profile peaked at equator with monsoonal shift
                double tropicalCore = Math.exp(-(lat * lat) / (2.0 * 22.0 * 22.0));
                double tropicalIntensity = tropicalCore * 150.0;

                // Smooth river basin endemicity enhancements
                double amazon = Math.exp(-(Math.pow(lat - (-3.0), 2) + Math.pow(lon - (-60.0), 2)) / 160.0);
                double congo = Math.exp(-(Math.pow(lat - 0.0, 2) + Math.pow(lon - 22.0, 2)) / 140.0);
                double ganges = Math.exp(-(Math.pow(lat - 24.0, 2) + Math.pow(lon - 85.0, 2)) / 80.0);
                double niger = Math.exp(-(Math.pow(lat - 10.0, 2) + Math.pow(lon - 5.0, 2)) / 90.0);
                double mekong = Math.exp(-(Math.pow(lat - 14.0, 2) + Math.pow(lon - 105.0, 2)) / 80.0);
                double sundaland = Math.exp(-(Math.pow(lat - (-1.0), 2) + Math.pow(lon - 114.0, 2)) / 120.0);

                tropicalIntensity += (amazon * 55.0 + congo * 65.0 + ganges * 60.0 + niger * 60.0 + mekong * 50.0 + sundaland * 45.0);

                // --- 2. Continuous Mountain / High-Altitude Vector Barrier ---
                double altitudeSuppression = getSmoothAltitudeSuppression(lon, lat);
                tropicalIntensity *= altitudeSuppression;

                // --- 3. Old World Zoonotic Crowd Disease Reservoir (Smallpox, Measles, Typhus, Cholera) ---
                boolean isOldWorld = (lon >= -20.0 && lon <= 150.0 && lat >= -35.0 && lat <= 68.0);
                double crowdIntensity = 0.0;
                if (isOldWorld && year >= -4000L) {
                    double medit = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 15.0, 2)) / 160.0);
                    double china = Math.exp(-(Math.pow(lat - 34.0, 2) + Math.pow(lon - 114.0, 2)) / 140.0);
                    double india = Math.exp(-(Math.pow(lat - 22.0, 2) + Math.pow(lon - 78.0, 2)) / 120.0);
                    double mideast = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow(lon - 44.0, 2)) / 100.0);
                    double europe = Math.exp(-(Math.pow(lat - 48.0, 2) + Math.pow(lon - 10.0, 2)) / 120.0);
                    crowdIntensity = (medit * 75.0 + china * 90.0 + india * 85.0 + mideast * 80.0 + europe * 75.0);
                }

                // --- 4. Historical Isolation of Americas & Oceania (Pre-1492 vs Columbian Shock) ---
                boolean isAmericas = (lon <= -30.0 && lon >= -170.0);
                boolean isOceania = (lat <= -10.0 && lon >= 110.0 && lon <= 180.0);
                double baseStress;

                if (isAmericas || isOceania) {
                    if (year < 1492L) {
                        // Pre-Columbian epidemiological isolation: Pristine low baseline, absent of Old World crowd zoonoses (smallpox, measles, plague) and African falciparum/yellow fever
                        baseStress = 12.0 + tropicalCore * 8.0;
                    } else {
                        // Virgin soil epidemic shock following 1492 Columbian exchange
                        double timeSinceContact = Math.min(120.0, (double) (year - 1492L));
                        double shockFactor = Math.min(1.0, timeSinceContact / 35.0);
                        baseStress = 25.0 + shockFactor * 195.0 + (isAmericas ? amazon * 35.0 : 0.0);
                    }
                } else {
                    // Old World continuous baseline
                    baseStress = 22.0 + tropicalIntensity * 0.60 + crowdIntensity * 0.65;
                }

                // --- 5. Historical Epidemic Pandemic Outbreak Pulses ---
                double epidemicBoost = 0.0;
                for (double[] ep : activeEpidemics) {
                    double dLat = lat - ep[1];
                    double dLon = (lon - ep[0]) * Math.cos(Math.toRadians((lat + ep[1]) * 0.5));
                    double d2 = dLat * dLat + dLon * dLon;
                    double radiusSq = ep[2] * ep[2];
                    if (d2 <= radiusSq * 4.0) {
                        double falloff = Math.exp(-d2 / (2.0 * radiusSq));
                        epidemicBoost = Math.max(epidemicBoost, falloff * ep[3] * 120.0);
                    }
                }

                double totalStress = baseStress + epidemicBoost;

                // --- 6. Modern Public Health, Sanitation & Antibiotics Transition (post-1900) ---
                if (year >= 1900L) {
                    double sanitaryFactor = computeContinuousSanitarySuppression(lon, lat, year);
                    totalStress *= sanitaryFactor;
                }

                // Paleolithic low-density baseline scaling
                if (year <= -70000L) {
                    double speciesBaseline = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 38.0, 22.0, 26.0);
                    totalStress = speciesBaseline + tropicalCore * 90.0;
                }

                int grayVal = (int) Math.clamp(totalStress * occWeight, 0, 255);
                img.setRGB(x, y, (grayVal << 16) | (grayVal << 8) | grayVal);
            }
        }

        return img;
    }

    /*
     * Continuous, smooth elevation barrier (Himalayas/Tibet, Andes, Alps, Ethiopian Highlands).
     * Smooth Gaussian falloff with distance to mountain crests — no sharp rectangular box discontinuities.
     */
    private static double getSmoothAltitudeSuppression(double lon, double lat) {
        // Tibetan Plateau / Himalayas core (approx 32°N, 88°E)
        double tibet = Math.exp(-(Math.pow(lat - 32.5, 2) / 45.0 + Math.pow(lon - 88.0, 2) / 120.0));
        // High Andes Cordillera (narrow north-south ridge)
        double andes = Math.exp(-(Math.pow(lat - (-18.0), 2) / 400.0 + Math.pow(lon - (-72.0), 2) / 25.0));
        // Ethiopian Highlands (approx 10°N, 39°E)
        double ethiopia = Math.exp(-(Math.pow(lat - 10.0, 2) / 18.0 + Math.pow(lon - 39.0, 2) / 18.0));
        // European Alps
        double alps = Math.exp(-(Math.pow(lat - 46.5, 2) / 10.0 + Math.pow(lon - 10.0, 2) / 25.0));

        double maxMountain = Math.max(Math.max(tibet, andes), Math.max(ethiopia, alps));
        // Smoothly attenuates vector load down to 20% in high mountains
        return 1.0 - 0.80 * maxMountain;
    }

    private static double[][] getEpidemicOutbreaksForYear(long year) {
        if (year == 536L || (year >= 530L && year <= 560L)) {
            // Justinianic Plague (Pelusium, Constantinople, Rome)
            return new double[][]{
                {32.3, 31.2, 20.0, 0.90}, // Pelusium / Levant
                {28.9, 41.0, 18.0, 0.85}, // Constantinople / Balkans
                {12.5, 41.9, 16.0, 0.80}  // Rome / Central Mediterranean
            };
        } else if (year == 1347L || (year >= 1345L && year <= 1355L)) {
            // Black Death Pandemic (Caffa, Genoa, Paris, Silk Road)
            return new double[][]{
                {35.3, 45.0, 22.0, 0.95}, // Caffa / Black Sea
                {12.5, 41.9, 20.0, 0.90}, // Italy / Genoa / Venice
                {2.3, 48.8, 20.0, 0.85},  // France / Northern Europe
                {80.0, 42.0, 25.0, 0.75}  // Central Asia
            };
        } else if (year == 1492L || (year >= 1492L && year <= 1550L)) {
            // Columbian Exchange Early Epidemic Epicenters
            return new double[][]{
                {-70.0, 19.0, 15.0, 0.90}, // Hispaniola / Caribbean
                {-99.1, 19.4, 18.0, 0.85}  // Tenochtitlan / Mesoamerica
            };
        } else if (year == 1914L || (year >= 1914L && year <= 1920L)) {
            // Spanish Flu / Wartime epidemic conditions
            return new double[][]{
                {3.0, 50.0, 15.0, 0.75},  // Western Europe
                {-95.0, 39.0, 18.0, 0.65} // North America
            };
        }
        return new double[0][];
    }

    private static double computeContinuousSanitarySuppression(double lon, double lat, long year) {
        // High-income industrialized zones with piped clean water and antibiotics
        double na = Math.exp(-(Math.pow(lat - 40.0, 2) / 120.0 + Math.pow(lon - (-95.0), 2) / 350.0));
        double eu = Math.exp(-(Math.pow(lat - 50.0, 2) / 100.0 + Math.pow(lon - 12.0, 2) / 200.0));
        double ea = Math.exp(-(Math.pow(lat - 35.0, 2) / 80.0 + Math.pow(lon - 130.0, 2) / 150.0));

        double devWeight = Math.max(Math.max(na, eu), ea);

        if (year >= 2000L) {
            return 1.0 - 0.75 * devWeight - 0.40 * (1.0 - devWeight);
        } else if (year >= 1950L) {
            return 1.0 - 0.55 * devWeight - 0.25 * (1.0 - devWeight);
        } else if (year >= 1914L) {
            return 1.0 - 0.35 * devWeight - 0.15 * (1.0 - devWeight);
        } else {
            // 1900
            return 1.0 - 0.20 * devWeight;
        }
    }
}
