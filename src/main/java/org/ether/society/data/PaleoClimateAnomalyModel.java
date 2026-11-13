/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * <h1>PaleoClimateAnomalyModel</h1>
 * <p>
 * Physical, ice-core calibrated, and continuous paleoclimate anomaly generator across deep-time timescales
 * (-100,000 BP to 2025 CE).
 * </p>
 * <p>
 * Models coupled polar amplification, volcanic aerosol radiative forcing (e.g. Toba ~ -74,000 BP),
 * Laurentide/Fennoscandian ice sheet albedo & topographic lapse cooling, AMOC shutdown plumes (Younger Dryas),
 * and astronomical orbital precession (Green Sahara / African Humid Period).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PaleoClimateAnomalyModel {

    private PaleoClimateAnomalyModel() {
        // Utility class
    }

    /**
     * Computes the continuous temperature anomaly delta relative to the modern 1950 baseline.
     *
     * @param lat   latitude in degrees
     * @param lon   longitude in degrees
     * @param elevM elevation in meters
     * @param year  astronomical year (negative for BCE/BP)
     * @return temperature anomaly delta in degrees Celsius
     */
    public static double computePaleoTemperatureDelta(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 0.0; // Modern baseline

        double radLat = Math.toRadians(lat);
        double sinLat = Math.sin(radLat);

        // Continuous Global Mean Temperature Anomaly (EPICA / NGRIP delta18O / TraCE-21ka)
        double[][] climateAnchors = {
            {-100000.0,  1.2}, // MIS 5e Eemian Warm Optimum (+1.2A°C)
            { -85000.0, -1.0}, // MIS 5a/5b stadial cooling
            { -74000.0, -4.8}, // -74,000 BP: Toba Super-Eruption & MIS 4 Stadial
            { -60000.0, -3.2}, // Early MIS 3
            { -50000.0, -3.5}, // MIS 3 Interstadial
            { -35000.0, -4.2}, // Late MIS 3
            { -25000.0, -5.6}, // Early LGM
            { -20000.0, -6.2}, // LGM Peak Glaciation
            { -16000.0, -4.5}, // Heinrich Stadial 1
            { -14500.0, -2.0}, // B-lling-Aller-d Interstadial
            { -12000.0, -3.5}, // Younger Dryas Cold Stadial
            { -10000.0, -0.5}, // Early Holocene Preboreal
            {  -7000.0,  0.8}, // Holocene Climate Optimum (+0.8A°C)
            {  -3000.0,  0.2}, // Neoglacial Transition
            {      0.0,  0.0}, // Common Era baseline
            {   1000.0,  0.3}, // Medieval Climate Optimum
            {   1650.0, -0.5}, // Little Ice Age
            {   1950.0,  0.0}  // 1950 baseline
        };

        double globalAnom = 0.0;
        if (year >= 1 && year <= 2000) {
            // High-fidelity empirical PAGES 2k Consortium (2019) multi-proxy Common Era anomaly
            globalAnom = Pages2kClimateProvider.getTemperatureAnomaly((int) year);
        } else if (year <= climateAnchors[0][0]) {
            globalAnom = climateAnchors[0][1];
        } else if (year >= climateAnchors[climateAnchors.length - 1][0]) {
            globalAnom = climateAnchors[climateAnchors.length - 1][1];
        } else {
            for (int i = 0; i < climateAnchors.length - 1; i++) {
                double y0 = climateAnchors[i][0];
                double y1 = climateAnchors[i + 1][0];
                if (year >= y0 && year <= y1) {
                    double t = (year - y0) / (y1 - y0);
                    // Smooth Hermite cubic interpolation
                    double smoothT = t * t * (3.0 - 2.0 * t);
                    globalAnom = climateAnchors[i][1] * (1.0 - smoothT) + climateAnchors[i + 1][1] * smoothT;
                    break;
                }
            }
        }

        // 2D Smooth Polar & Continental Amplification (Smooth continuous spherical function):
        double polarFactor = 0.65 + 1.35 * (sinLat * sinLat);
        double landAmplification = (elevM >= 0.0) ? 1.30 : 0.85;
        double deltaT = globalAnom * polarFactor * landAmplification;

        // Toba Volcanic Aerosol Shielding (Smooth Gaussian plume concentrated in South/SE Asia ~ -74,000 BP)
        if (year <= -65000L && year > -85000L) {
            double timeWeight = Math.exp(-Math.pow((year - (-74000.0)) / 4500.0, 2));
            double tobaPlume = Math.exp(-(Math.pow(lat - 15.0, 2) / 350.0 + Math.pow(lon - 85.0, 2) / 1000.0));
            deltaT -= (tobaPlume * 4.5 * timeWeight);
        }

        // Ice sheet albedo & topographic lapse rate cooling over ice domes
        if (year <= -35000L && year > -70000L) {
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_MIS3);
            double dLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_MIS3);
            if (dFenno <= 4.0) deltaT -= 8.0 * Math.exp(-Math.max(0, dFenno) / 2.0);
            if (dLaur <= 4.0) deltaT -= 9.0 * Math.exp(-Math.max(0, dLaur) / 2.0);
        } else if (year <= -16000L) {
            double dLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_LGM);
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_LGM);
            double dAlps = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_ALPS_LGM);
            double dPat = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_PATAGONIA_LGM);

            if (dLaur <= 5.0) deltaT -= 14.0 * Math.exp(-Math.max(0, dLaur) / 2.5);
            if (dFenno <= 5.0) deltaT -= 12.0 * Math.exp(-Math.max(0, dFenno) / 2.5);
            if (dAlps <= 3.0) deltaT -= 7.0 * Math.exp(-Math.max(0, dAlps) / 1.5);
            if (dPat <= 3.0) deltaT -= 6.0 * Math.exp(-Math.max(0, dPat) / 1.5);
        } else if (year <= -10500L) {
            double amocPlume = Math.exp(-(Math.pow(lat - 56.0, 2) / 180.0 + Math.pow(lon - (-15.0), 2) / 350.0));
            double ydLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_YD);
            double ydFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_YD);

            if (ydLaur <= 4.0) deltaT -= 10.0 * Math.exp(-Math.max(0, ydLaur) / 2.0);
            if (ydFenno <= 4.0) deltaT -= 9.0 * Math.exp(-Math.max(0, ydFenno) / 2.0);
            deltaT -= (amocPlume * 7.5);
        }

        return deltaT;
    }

    /**
     * Computes the dimensionless precipitation scaling factor for a given coordinate and epoch.
     *
     * @param lat   latitude in degrees
     * @param lon   longitude in degrees
     * @param elevM elevation in meters
     * @param year  astronomical year
     * @return multiplicative precipitation factor
     */
    public static double computePaleoPrecipitationFactor(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 1.0;

        double absLat = Math.abs(lat);

        if (year <= -65000L && year > -85000L) {
            // Toba Volcanic Drought in Monsoon Tropics (Smooth continuous 2D Gaussian plume)
            double timeWeight = Math.exp(-Math.pow((year - (-74000.0)) / 5000.0, 2));
            double dMonsoon = Math.exp(-(Math.pow(lat - 15.0, 2) / 400.0 + Math.pow(lon - 85.0, 2) / 1200.0));
            return 0.85 - (dMonsoon * 0.30 * timeWeight);
        } else if (year <= -18000L && year >= -25000L) {
            return 1.0 - 0.38 * Math.sin(Math.toRadians(absLat));
        } else if (year <= -10500L && year > -12800L) {
            // Asian monsoon drought in Younger Dryas (Smooth 2D Gaussian plume)
            double dYdMonsoon = Math.exp(-(Math.pow(lat - 20.0, 2) / 350.0 + Math.pow(lon - 90.0, 2) / 1000.0));
            return 0.90 - (dYdMonsoon * 0.15);
        }
        return 1.0;
    }

    /**
     * Computes absolute precipitation delta anomaly in mm/year.
     *
     * @param lat   latitude in degrees
     * @param lon   longitude in degrees
     * @param elevM elevation in meters
     * @param year  astronomical year
     * @return precipitation delta in mm/year
     */
    public static double computePaleoPrecipitationDelta(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 0.0;

        // African Humid Period / Green Sahara & Arabian wet corridor (-100,000 BP & -10,000 to -5,000 BP)
        if (year <= -85000L || (year <= -4500L && year >= -10500L)) {
            double greenSahara = Math.exp(-(Math.pow(lat - 21.0, 2) / 80.0 + Math.pow(lon - 15.0, 2) / 450.0));
            double greenArabia = Math.exp(-(Math.pow(lat - 22.0, 2) / 50.0 + Math.pow(lon - 48.0, 2) / 120.0));
            return (greenSahara * 950.0) + (greenArabia * 650.0);
        }

        return 0.0;
    }

    /**
     * Computes dimensionless seasonality multiplier factor for the paleoclimate epoch.
     *
     * @param year astronomical year
     * @return seasonality factor
     */
    public static double computePaleoSeasonalityFactor(long year) {
        if (year >= 1950) return 1.0;

        if (year <= -85000L || (year <= -6000L && year >= -11000L)) {
            return 1.15; // High obliquity / enhanced seasonality
        } else if (year <= -65000L && year > -85000L) {
            return 1.20; // Post-Toba severe winter extremes
        } else if (year <= -18000L && year >= -25000L) {
            return 0.92;
        }
        return 1.0;
    }
}
