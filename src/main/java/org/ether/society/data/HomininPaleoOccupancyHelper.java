/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * Analytical Hominin Paleo-Occupancy and Migration Dispersal Calculator.
 *
 * <p>Computes geographic occupancy weights, hominin clade affinities (Homo Sapiens,
 * Neanderthals, Denisovans), migration corridors (Kelp Highway, Ice-Free Corridor,
 * Beringia Steppe), and glaciated geographic boundaries across the Late Pleistocene
 * and Holocene (-100,000 BP to present).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HomininEpochDispersalBrackets
 * @see PaleoGeographyCoordinatesRegistry
 */
public final class HomininPaleoOccupancyHelper {

    private HomininPaleoOccupancyHelper() {
        // Static utility class
    }

    /**
     * Compute hominin clade weights via orographic Voronoi gaussian blend.
     *
     * @param lon longitude coordinate in degrees [-180, 180]
     * @param lat latitude coordinate in degrees [-90, 90]
     * @return 6-element normalized probability distribution across hominin clades, or {@code null} if uninhabited
     */
    public static double[] computeHomininCladeWeights(double lon, double lat) {
        // --- 1. STRICT UNINHABITED GEOGRAPHIC EXCLUSIONS (-100,000 BP) ---
        if (lat < -60.0 || lon < -26.0) return null; // Americas & Antarctica uninhabited
        if (lat < -35.2) return null; // All sub-Antarctic & South Indian islands (Marion, Crozet, Kerguelen, Bouvet)

        // Madagascar & Mascarene Islands (Africa mainland coast is lon <= 41.0A°E, Madagascar is >= 43.0A°E)
        if (lat <= -10.0 && lat >= -30.0 && lon >= 43.0 && lon <= 65.0) return null;
        if (lat > -10.0 && lat <= -4.0 && lon >= 53.0 && lon <= 57.0) return null; // Seychelles

        // Isolated Oceanic Islands in the Atlantic:
        if (lon <= -21.5 && lat >= 14.0 && lat <= 18.0) return null; // Cape Verde archipelago (off Senegal)
        if (lon <= -13.3 && lat >= 27.5 && lat <= 29.5) return null; // Canary Islands (off Morocco/Sahara)
        if (lon <= -15.5 && lat >= 32.0 && lat <= 33.5) return null; // Madeira
        if (lon <= -24.0 && lat >= 36.0 && lat <= 40.0) return null; // Azores
        if (lon >= -6.5 && lon <= -5.0 && lat >= -16.5 && lat <= -15.5) return null; // Saint Helena
        if (lon >= -15.0 && lon <= -13.5 && lat >= -8.5 && lat <= -7.5) return null; // Ascension

        if (lat >= -2.0 && lat <= 2.0 && lon >= 6.0 && lon <= 9.0) return null; // SÃ£o TomÃ© & PrÃ­ncipe
        if (lat >= 12.0 && lat <= 13.0 && lon >= 53.0 && lon <= 55.0) return null; // Socotra Island
        if (lat <= -8.0 && lon >= 110.0) return null; // Australia / Sahul / Tasmania
        if (lat > -8.0 && lat <= 0.0 && lon >= 128.0) return null; // New Guinea
        if (lat > -8.0 && lat < 12.0 && lon >= 118.0 && lon <= 130.0) return null; // Wallacea
        if (lat >= 0.0 && lat < 20.0 && lon >= 120.0 && lon <= 128.0) return null; // Philippines
        if (lat >= 28.0 && lon >= 128.0) return null; // Japanese Archipelago
        if (lat > 65.0) return null; // Arctic uninhabited

        // --- 2. SMOOTH PHYSICAL BARRIERS & MARINE STRAITS ---
        double himalayas = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow((lon - 85.0) * 0.55, 2)) / 70.0);

        boolean isAfricanLandmass = (lat <= 37.2 && lon >= -18.5 && lon <= 32.5);
        boolean isEuropeanLandmass = (lat >= 35.8 && lon >= -10.0 && lon <= 36.0);

        // --- 3. COMPUTE MINIMUM EFFECTIVE DISTANCE TO EACH CLADE ---
        double[] dists = new double[6];

        // Clade 0: Sapiens African Core
        double d0 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_SAPIENS_AFRICA) d0 = Math.min(d0, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d0 += 50.0;
        dists[0] = d0;

        // Clade 1: Sapiens Out-of-Africa Pioneers (Levant / Arabia)
        double d1 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_SAPIENS_PIONEERS) d1 = Math.min(d1, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d1 += 50.0;
        dists[1] = d1;

        // Clade 2: Neanderthals Western Classical Mousterian (Europe & Iberia)
        double d2 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_NEANDERTHAL_WEST) d2 = Math.min(d2, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d2 += 50.0;
        dists[2] = d2;

        // Clade 3: Neanderthals Zagros & Near East
        double d3 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_NEANDERTHAL_ZAGROS) d3 = Math.min(d3, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d3 += 50.0;
        dists[3] = d3;

        // Clade 4: Denisovans Altai & Siberian Archaic
        double d4 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_DENISOVAN_ALTAI) d4 = Math.min(d4, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass || isEuropeanLandmass) d4 += 50.0;
        d4 += himalayas * 16.0;
        dists[4] = d4;

        // Clade 5: Eastern Archaic & Sundaland
        double d5 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesRegistry.HEARTHS_EASTERN_ARCHAIC) d5 = Math.min(d5, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass || isEuropeanLandmass) d5 += 50.0;
        d5 += himalayas * 16.0;
        dists[5] = d5;

        // --- 4. SOFT-VORONOI GAUSSIAN BLEND ---
        double minDist = Double.MAX_VALUE;
        for (double d : dists) minDist = Math.min(minDist, d);

        double sigma = 3.5; // Smooth transition scale in degrees (~380 km soft gradient)
        double[] weights = new double[6];
        double sum = 0.0;
        for (int i = 0; i < 6; i++) {
            weights[i] = Math.exp(-(dists[i] - minDist) / sigma);
            sum += weights[i];
        }
        for (int i = 0; i < 6; i++) {
            weights[i] /= sum;
        }
        return weights;
    }

    /**
     * Get hominin entity identifier (1-indexed).
     *
     * @param lon longitude coordinate in degrees
     * @param lat latitude coordinate in degrees
     * @return entity ID (1 to 6), or 0 if uninhabited
     */
    public static int getHomininEntityId(double lon, double lat) {
        double[] weights = computeHomininCladeWeights(lon, lat);
        if (weights == null) return 0;
        int bestIdx = 0;
        double maxW = 0.0;
        for (int i = 0; i < 6; i++) {
            if (weights[i] > maxW) {
                maxW = weights[i];
                bestIdx = i;
            }
        }
        return bestIdx + 1;
    }

    /**
     * Get hominin species classification type.
     *
     * @param lon longitude coordinate in degrees
     * @param lat latitude coordinate in degrees
     * @return species type index (1=Sapiens, 2=Neanderthal, 3=Denisovan, 0=Uninhabited)
     */
    public static int getHomininSpeciesType(double lon, double lat) {
        int entity = getHomininEntityId(lon, lat);
        return switch (entity) {
            case 1, 2 -> 1; // Homo Sapiens
            case 3, 4 -> 2; // Neanderthals
            case 5, 6 -> 3; // Denisovans / Archaic Asian Hominins
            default -> 0;   // Uninhabited
        };
    }

    /**
     * Blends paleolithic cultural and sociological traits across hominin clades.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param valSapiens value for Homo Sapiens
     * @param valNeanderthal value for Neanderthals
     * @param valDenisovan value for Denisovans
     * @return weighted trait value
     */
    public static double blendPaleoTraits(double lon, double lat, double valSapiens, double valNeanderthal, double valDenisovan) {
        double[] w = computeHomininCladeWeights(lon, lat);
        if (w == null) return 0.0;
        double sWeight = w[0] + w[1];
        double nWeight = w[2] + w[3];
        double dWeight = w[4] + w[5];
        return sWeight * valSapiens + nWeight * valNeanderthal + dWeight * valDenisovan;
    }

    /**
     * Calculates the minimum spherical arc distance from a point to a polyline.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param line polyline coordinates array [N][2]
     * @return minimum distance in degrees
     */
    public static double distToPolyline(double lon, double lat, double[][] line) {
        if (line == null || line.length < 2) return Double.MAX_VALUE;
        double minD2 = Double.MAX_VALUE;
        for (int i = 0; i < line.length - 1; i++) {
            double x1 = line[i][0], y1 = line[i][1];
            double x2 = line[i + 1][0], y2 = line[i + 1][1];
            double dx = x2 - x1;
            if (dx > 180.0) dx -= 360.0;
            else if (dx < -180.0) dx += 360.0;
            double dy = y2 - y1;

            double plon = lon - x1;
            if (plon > 180.0) plon -= 360.0;
            else if (plon < -180.0) plon += 360.0;

            double len2 = dx * dx + dy * dy;
            double t = (len2 > 0) ? Math.clamp((plon * dx + (lat - y1) * dy) / len2, 0.0, 1.0) : 0.0;
            double px = x1 + t * dx;
            if (px > 180.0) px -= 360.0;
            else if (px < -180.0) px += 360.0;
            double py = y1 + t * dy;
            double d2 = distSq(lon, lat, px, py);
            if (d2 < minD2) minD2 = d2;
        }
        return Math.sqrt(minD2);
    }

    /**
     * Calculates the signed shortest distance from a coordinate to a closed polygon.
     * Negative values indicate the point is located inside the polygon.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param poly polygon vertices [N][2]
     * @return signed distance in degrees (negative = inside)
     */
    public static double signedDistanceToPolygon(double lon, double lat, double[][] poly) {
        boolean inside = false;
        double minD2 = Double.MAX_VALUE;
        int n = poly.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly[i][0], yi = poly[i][1];
            double xj = poly[j][0], yj = poly[j][1];
            if (((yi > lat) != (yj > lat)) && (lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-12) + xi)) {
                inside = !inside;
            }
            double dx = xi - xj;
            double dy = yi - yj;
            double len2 = dx * dx + dy * dy;
            double t = (len2 > 0) ? Math.clamp(((lon - xj) * dx + (lat - yj) * dy) / len2, 0.0, 1.0) : 0.0;
            double px = xj + t * dx;
            double py = yj + t * dy;
            double d2 = distSq(lon, lat, px, py);
            if (d2 < minD2) {
                minD2 = d2;
            }
        }
        double dist = Math.sqrt(minD2);
        return inside ? -dist : dist;
    }

    /**
     * Determines whether a geographic point corresponds to an uninhabited isolated oceanic island at a given historical year.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param year historical epoch year
     * @return {@code true} if the point is an uninhabited oceanic island
     */
    public static boolean isRemoteOceanicIsland(double lon, double lat, long year) {
        return HomininEpochDispersalBrackets.isRemoteOceanicIsland(lon, lat, year);
    }

    /**
     * Calculates the hominin occupancy coefficient in range \([0.0, 1.0]\).
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param year historical epoch year
     * @return occupancy weight \([0.0, 1.0]\)
     */
    public static double getHomininOccupancyWeight(double lon, double lat, long year) {
        return HomininEpochDispersalBrackets.getHomininOccupancyWeight(lon, lat, year);
    }

    /**
     * Determines whether a coordinate is occupied by hominins at a given historical year.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @param year historical epoch year
     * @return {@code true} if hominin occupancy weight > 0.001
     */
    public static boolean isHomininOccupied(double lon, double lat, long year) {
        return getHomininOccupancyWeight(lon, lat, year) > 0.001;
    }

    private static double distSq(double lng1, double lat1, double lng2, double lat2) {
        return HistoricalScenarioFeaturesRegistry.distSq(lng1, lat1, lng2, lat2);
    }
}
