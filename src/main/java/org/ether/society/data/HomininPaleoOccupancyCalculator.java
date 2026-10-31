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
 */
public final class HomininPaleoOccupancyCalculator {

    private HomininPaleoOccupancyCalculator() {
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
        
        // Madagascar & Mascarene Islands (Africa mainland coast is lon <= 41.0°E, Madagascar is >= 43.0°E)
        if (lat <= -10.0 && lat >= -30.0 && lon >= 43.0 && lon <= 65.0) return null;
        if (lat > -10.0 && lat <= -4.0 && lon >= 53.0 && lon <= 57.0) return null; // Seychelles

        // Isolated Oceanic Islands in the Atlantic:
        if (lon <= -21.5 && lat >= 14.0 && lat <= 18.0) return null; // Cape Verde archipelago (off Senegal)
        if (lon <= -13.3 && lat >= 27.5 && lat <= 29.5) return null; // Canary Islands (off Morocco/Sahara)
        if (lon <= -15.5 && lat >= 32.0 && lat <= 33.5) return null; // Madeira
        if (lon <= -24.0 && lat >= 36.0 && lat <= 40.0) return null; // Azores
        if (lon >= -6.5 && lon <= -5.0 && lat >= -16.5 && lat <= -15.5) return null; // Saint Helena
        if (lon >= -15.0 && lon <= -13.5 && lat >= -8.5 && lat <= -7.5) return null; // Ascension

        if (lat >= -2.0 && lat <= 2.0 && lon >= 6.0 && lon <= 9.0) return null; // São Tomé & Príncipe
        if (lat >= 12.0 && lat <= 13.0 && lon >= 53.0 && lon <= 55.0) return null; // Socotra Island
        if (lat <= -8.0 && lon >= 110.0) return null; // Australia / Sahul / Tasmania
        if (lat > -8.0 && lat <= 0.0 && lon >= 128.0) return null; // New Guinea
        if (lat > -8.0 && lat < 12.0 && lon >= 118.0 && lon <= 130.0) return null; // Wallacea
        if (lat >= 0.0 && lat < 20.0 && lon >= 120.0 && lon <= 128.0) return null; // Philippines
        if (lat >= 28.0 && lon >= 128.0) return null; // Japanese Archipelago
        if (lat > 65.0) return null; // Arctic uninhabited

        // --- 2. SMOOTH PHYSICAL BARRIERS & MARINE STRAITS ---
        // Himalayan / Tibetan mountain ridge barrier centered along (32°N, 85°E)
        double himalayas = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow((lon - 85.0) * 0.55, 2)) / 70.0);

        // Mediterranean Marine Strait Barrier (Strict separation: North Africa vs Southern Europe)
        boolean isAfricanLandmass = (lat <= 37.2 && lon >= -18.5 && lon <= 32.5);
        boolean isEuropeanLandmass = (lat >= 35.8 && lon >= -10.0 && lon <= 36.0);

        // --- 3. COMPUTE MINIMUM EFFECTIVE DISTANCE TO EACH CLADE ---
        double[] dists = new double[6];

        // Clade 0: Sapiens African Core
        double d0 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_AFRICA) d0 = Math.min(d0, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d0 += 50.0; // Cannot cross Mediterranean into Europe
        dists[0] = d0;

        // Clade 1: Sapiens Out-of-Africa Pioneers (Levant / Arabia)
        double d1 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_PIONEERS) d1 = Math.min(d1, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d1 += 50.0;
        dists[1] = d1;

        // Clade 2: Neanderthals Western Classical Mousterian (Europe & Iberia)
        double d2 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_NEANDERTHAL_WEST) d2 = Math.min(d2, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d2 += 50.0; // Cannot cross Mediterranean into North Africa
        dists[2] = d2;

        // Clade 3: Neanderthals Zagros & Near East
        double d3 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_NEANDERTHAL_ZAGROS) d3 = Math.min(d3, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d3 += 50.0; // Cannot cross into North Africa
        dists[3] = d3;

        // Clade 4: Denisovans Altai & Siberian Archaic
        double d4 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_DENISOVAN_ALTAI) d4 = Math.min(d4, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass || isEuropeanLandmass) d4 += 50.0;
        d4 += himalayas * 16.0; // Himalayan barrier separating Denisovans from Indian subcontinent
        dists[4] = d4;

        // Clade 5: Eastern Archaic & Sundaland
        double d5 = Double.MAX_VALUE;
        for (double[] h : PaleoGeographyCoordinatesCatalog.HEARTHS_EASTERN_ARCHAIC) d5 = Math.min(d5, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass || isEuropeanLandmass) d5 += 50.0;
        d5 += himalayas * 16.0; // Himalayan barrier separating Sunda/Indian hominins from Tibetan plateau
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
        // 1. Remote Polynesia & Central/Eastern Pacific
        if (year < 1200) {
            if (lat >= 18.0 && lat <= 23.0 && lon >= -162.0 && lon <= -154.0) return true; // Hawaii
            if (lat >= -28.0 && lat <= -26.0 && lon >= -110.0 && lon <= -108.0) return true; // Easter Island
            if (lat >= -2.0 && lat <= 2.0 && lon >= -92.0 && lon <= -88.0) return true; // Galapagos
            if (lat >= 4.5 && lat <= 6.5 && lon >= -88.0 && lon <= -86.0) return true; // Cocos
            if (lat >= 3.0 && lat <= 4.8 && lon >= -82.5 && lon <= -80.5) return true; // Malpelo
            if (lat >= 9.0 && lat <= 11.5 && lon >= -110.5 && lon <= -108.0) return true; // Clipperton
            if (lat >= 17.5 && lat <= 20.0 && lon >= -115.5 && lon <= -109.5) return true; // Revillagigedo
            if (lat >= -34.5 && lat <= -25.5 && lon >= -82.0 && lon <= -78.0) return true; // Juan Fernandez
            if (year < 1280 && lat <= -34.0 && lat >= -48.0 && lon >= 165.0 && lon <= 180.0) return true; // New Zealand
            if (lon >= -180.0 && lon <= -120.0 && lat >= -30.0 && lat <= 30.0) return true;
            if (lon >= -150.0 && lon <= -130.0 && lat >= -30.0 && lat <= 0.0) return true;
        }
        if (year < -1200) {
            if (lon >= 155.0 && lon <= 180.0 && lat >= -25.0 && lat <= 20.0) return true;
            if (lon >= 135.0 && lon <= 165.0 && lat >= 5.0 && lat <= 22.0) return true;
        }

        // 2. Remote Atlantic Islands
        if (year < 1400) {
            if (lat >= -4.5 && lat <= -3.0 && lon >= -33.5 && lon <= -31.5) return true;
            if (lat >= -21.5 && lat <= -19.5 && lon >= -30.5 && lon <= -28.0) return true;
            if (lat >= 36.0 && lat <= 40.5 && lon >= -32.0 && lon <= -24.0) return true; // Azores
            if (lat >= 32.0 && lat <= 33.5 && lon >= -17.5 && lon <= -16.0) return true; // Madeira
            if (lat >= 14.5 && lat <= 17.5 && lon >= -25.5 && lon <= -22.5) return true; // Cape Verde
            if (lat >= -17.0 && lat <= -15.0 && lon >= -6.5 && lon <= -5.0) return true; // Saint Helena
            if (lat >= -8.5 && lat <= -7.5 && lon >= -15.0 && lon <= -13.5) return true; // Ascension
            if (lat >= -38.0 && lat <= -36.0 && lon >= -13.5 && lon <= -11.5) return true; // Tristan da Cunha
            if (lat >= -53.5 && lat <= -51.0 && lon >= -62.0 && lon <= -57.0) return true; // Falklands
            if (lat >= -55.0 && lat <= -53.5 && lon >= -39.0 && lon <= -35.0) return true; // South Georgia
            if (lat >= 31.5 && lat <= 33.0 && lon >= -65.5 && lon <= -64.0) return true; // Bermuda
            if (year < 874 && lat >= 63.0 && lat <= 67.0 && lon >= -25.0 && lon <= -12.0) return true; // Iceland
        }
        if (year < -1000) {
            if (lat >= 27.5 && lat <= 29.5 && lon >= -18.5 && lon <= -13.0) return true; // Canary Islands
        }

        // 3. Remote Indian Ocean Islands
        if (year < 500) {
            if (lat <= -11.5 && lat >= -26.0 && lon >= 43.0 && lon <= 51.0) return true; // Madagascar
            if (lat <= -19.5 && lat >= -22.0 && lon >= 55.0 && lon <= 64.0) return true; // Mascarenes
            if (lat >= -5.5 && lat <= -4.0 && lon >= 54.5 && lon <= 56.5) return true; // Seychelles
            if (lat >= -13.0 && lat <= -11.0 && lon >= 43.0 && lon <= 45.5) return true; // Comoros
            if (year < -500 && lat >= -8.0 && lat <= 8.0 && lon >= 71.0 && lon <= 74.5) return true; // Maldives & Chagos
            if (lat < -35.0 && lon >= 35.0 && lon <= 90.0) return true; // Sub-Antarctic
        }

        return false;
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
        if (lat < -60.0) return 0.0; // Antarctica strictly uninhabited

        // Greenland strictly glaciated and uninhabited before Saqqaq colonization (~2500 BC / 4500 BP)
        if (year < -2500 && lat >= 58.0 && lon >= -75.0 && lon <= -10.0) {
            return 0.0;
        }

        // Remote oceanic islands filter
        if (isRemoteOceanicIsland(lon, lat, year)) {
            return 0.0;
        }

        if (year <= -2500000L) {
            // -3,000,000 BP (Late Pliocene / Australopithecus): Strictly confined to Sub-Saharan African cradle
            if (lon < 10.0 || lon > 45.0 || lat > 18.0 || lat < -35.0) return 0.0;
            // Great Rift Valley (Afar, Turkana, Olduvai) & South African Karst (Cradle of Humankind)
            boolean inRift = (lat >= -5.0 && lat <= 15.0 && lon >= 32.0 && lon <= 44.0);
            boolean inSouthAfrica = (lat >= -32.0 && lat <= -23.0 && lon >= 24.0 && lon <= 33.0);
            boolean inChad = (lat >= 12.0 && lat <= 18.0 && lon >= 15.0 && lon <= 21.0);
            if (inRift || inSouthAfrica || inChad) return 1.0;
            return 0.0;
        } else if (year <= -1500000L) {
            // -2,000,000 BP (Early Pleistocene / Homo erectus Out of Africa I)
            // Americas, Australia/Sahul, and northern latitudes strictly 0.0
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 45.0) return 0.0;
            // 1. African heartland
            if (lat >= -35.0 && lat <= 30.0 && lon >= -18.0 && lon <= 45.0) return 1.0;
            // 2. Levantine Corridor (Sinai, Jordan Rift, Levant towards Dmanisi)
            if (lat >= 27.0 && lat <= 42.5 && lon >= 33.0 && lon <= 48.0) return 1.0;
            // 3. Indian Ocean coastal & savanna highway (Arabia, Makran, Indus, India to Sundaland)
            if (lat >= -9.0 && lat <= 32.0 && lon >= 48.0 && lon <= 112.0) return 0.9;
            // 4. East Asian river basins (Nihewan, Lantian, Gongwangling in China)
            if (lat >= 22.0 && lat <= 41.0 && lon >= 100.0 && lon <= 122.0) return 0.8;
            return 0.0;
        } else if (year <= -500000L) {
            // -1,000,000 BP (Early/Mid-Pleistocene Acheulean): Africa, Southern/Western Europe, Southern & Eastern Asia
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 52.0) return 0.0;
            // Europe up to 52°N (Atapuerca, Orce, Mauer, Ceprano)
            if (lat >= 35.0 && lat <= 52.0 && lon >= -10.0 && lon <= 30.0) return 0.85;
            // Africa & Asia
            if (lat <= 45.0 && lon >= -18.0 && lon <= 125.0) return 1.0;
            return 0.0;
        } else if (year <= -200000L) {
            // -300,000 BP (Jebel Irhoud, Archaic Sapiens, Pre-Neanderthals, Denisovans)
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 55.0) return 0.0;
            return 1.0;
        } else if (year <= -85000L) {
            // -100,000 BP (MIS 5e Eemian Interglacial): Confined to Africa & Eurasia, Sahul & Americas strictly 0
            if (lon < -25.0 && lon > -170.0) return 0.0;
            if (lon > 110.0 && lat < -5.0) return 0.0;
            int species = getHomininSpeciesType(lon, lat);
            if (species == 0) return 0.0;
            double maxLat = 56.0 + 3.0 * Math.sin((lon - 10.0) * Math.PI / 90.0) - (lon > 60.0 ? (lon - 60.0) * 0.05 : 0.0);
            maxLat = Math.clamp(maxLat, 48.0, 58.5);
            double fadeStart = maxLat - 5.0;
            if (lat > maxLat) return 0.0;
            if (lat > fadeStart) {
                double t = Math.clamp((lat - fadeStart) / (maxLat - fadeStart), 0.0, 1.0);
                return 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return 1.0;
        } else if (year <= -65000L) {
            // -74,000 BP (Youngest Toba Tuff Super-Eruption & MIS 4 Glacial Stadial)
            if ((lon < -25.0 && lon > -170.0) || (lon > 110.0 && lat < -5.0)) {
                return 0.0;
            }
            int species = getHomininSpeciesType(lon, lat);
            if (species == 0) return 0.0;

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            // Toba Volcanic Ash Depopulation in South Asia
            double dAsh = Math.exp(-(Math.pow(lat - 18.0, 2) / 120.0 + Math.pow(lon - 80.0, 2) / 220.0));
            double ashFactor = Math.max(0.08, 1.0 - dAsh * 0.88);

            double maxLat = 52.0;
            if (lat > maxLat) return 0.0;
            if (lat > 46.0) {
                double t = Math.clamp((lat - 46.0) / 6.0, 0.0, 1.0);
                return Math.min(wFenno, (1.0 - t * t * (3.0 - 2.0 * t)) * ashFactor);
            }
            return wFenno * ashFactor;
        } else if (year <= -52000L) {
            // -65,000 to -52,000 BP (MIS 4 / Early MIS 3 Out-of-Africa Coastal Dispersal to Northern Sahul)
            if (lon < -25.0 && lon > -170.0) return 0.0;

            if (lon > 110.0 && lat < -5.0) {
                if (lat < -20.0) return 0.0;
                double dMadj = Math.sqrt(distSq(lon, lat, 132.9, -12.5));
                double dKimb = Math.sqrt(distSq(lon, lat, 125.0, -16.0));
                if (dMadj <= 5.0 || dKimb <= 5.0) return 0.65;
                return 0.15;
            }

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));
            if (lat >= 56.0) return 0.0;
            if (lat > 48.0) {
                double t = Math.clamp((lat - 48.0) / 8.0, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -42000L) {
            // -52,000 to -42,000 BP (MIS 3 Sahul Colonization across interior)
            if (lon < -25.0 && lon > -170.0) return 0.0;
            if (lon > 110.0 && lat < -5.0) {
                if (lat < -39.0) return 0.0;
                return 1.0;
            }

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));
            if (lat >= 58.5) return 0.0;
            if (lat > 50.0) {
                double t = Math.clamp((lat - 50.0) / 8.5, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -32000L) {
            // -42,000 to -32,000 BP (Late MIS 3 / Aurignacian / Tasmania firmly established)
            if (lon < -25.0 && lon > -170.0) return 0.0;

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            if (lat >= 58.5) return 0.0;
            if (lat > 50.0) {
                double t = Math.clamp((lat - 50.0) / 8.5, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -24000L) {
            // -32,000 to -24,000 BP (Pre-LGM Beringian Standstill & Earliest American Pioneers)
            double dLaur = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_MIS3);
            if (dLaur <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            double dBeringia = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 5.5) {
                return Math.min(wLaur, 0.85);
            }

            double dPacific = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 4.0) {
                return 0.50 * wLaur;
            }

            double dWhiteSands = Math.sqrt(distSq(lon, lat, -106.3, 32.8));
            double dChiquihuite = Math.sqrt(distSq(lon, lat, -103.5, 24.2));
            double dMeadowcroft = Math.sqrt(distSq(lon, lat, -80.4, 40.3));
            double dFurada = Math.sqrt(distSq(lon, lat, -42.5, -9.3));
            if (dWhiteSands <= 6.0 || dChiquihuite <= 6.0 || dMeadowcroft <= 5.0 || dFurada <= 6.0) {
                return 0.35 * wLaur;
            }

            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 36.0 && lat >= -54.0) {
                    return 0.10 * wLaur;
                }
                return 0.0;
            }

            if (lat >= 72.0) return 0.0;
            if (lat > 66.0) {
                double t = Math.clamp((lat - 66.0) / 6.0, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -16000L) {
            // -24,000 to -16,000 BP (LGM Peak, Solutrean, Kebaran & White Sands Footprints)
            double dLaurentide = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM);
            if (dLaurentide <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            double dAlps = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_ALPS_LGM);
            if (dAlps <= 0) return 0.0;
            double dPatagonia = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_PATAGONIA_LGM);
            if (dPatagonia <= 0) return 0.0;

            double dBeringia = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 6.0) {
                return Math.min(wLaur, 0.85);
            }

            double dPacific = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 4.5) {
                return 0.70 * wLaur;
            }

            double dAndes = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR);
            if (dAndes <= 4.0) return 0.55;
            double dAmazon = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM);
            if (dAmazon <= 4.0) return 0.45;
            double dSavanna = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA);
            if (dSavanna <= 4.0) return 0.50;

            double dWhiteSands = Math.sqrt(distSq(lon, lat, -106.3, 32.8));
            double dCactus = Math.sqrt(distSq(lon, lat, -77.3, 36.8));
            double dPageLadson = Math.sqrt(distSq(lon, lat, -83.9, 30.1));
            if (dWhiteSands <= 6.5 || dCactus <= 5.5 || dPageLadson <= 5.5) {
                return 0.45 * wLaur;
            }

            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 38.0 && lat >= -54.0) {
                    return 0.15 * wLaur;
                }
                return 0.0;
            }

            if (lat >= 72.0) return 0.0;
            double wArctic = 1.0;
            if (lat > 68.0) {
                double t = Math.clamp((lat - 68.0) / 4.0, 0.0, 1.0);
                wArctic = 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return Math.min(wFenno, wArctic);
        } else if (year <= -13500L) {
            // -16,000 to -13,500 BP (Deglaciation, Pacific Coastal Route Expansion & Monte Verde II)
            double dLaurentide = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM);
            if (dLaurentide <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            double dBeringia = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 6.0) return Math.min(wLaur, 0.90);

            double dPacific = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 5.5) return 0.85 * wLaur;

            double dAndes = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR);
            if (dAndes <= 4.0) return 0.65;
            double dAmazon = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM);
            if (dAmazon <= 4.0) return 0.55;
            double dSavanna = distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA);
            if (dSavanna <= 4.0) return 0.60;

            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 44.0 && lat >= -54.0) return 0.35 * wLaur;
                return 0.0;
            }
            return Math.min(wFenno, 1.0);
        } else if (year <= -10500L) {
            // -13,500 to -10,500 BP (Younger Dryas, Ice-Free Corridor Open, Clovis/Folsom)
            double dLaur = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_YD);
            if (dLaur <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_YD);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0; // Greenland inland ice
            return Math.min(wLaur, wFenno);
        } else if (year <= -8000L) {
            // -10,500 to -8,000 BP (Early Holocene Deglaciation / Maglemosian / PPNA)
            double dFenno = signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_EARLY_HOLOCENE);
            if (dFenno <= 0) return 0.0;
            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0;
            if (lat > 75.0) return 0.0;
            return 1.0;
        } else {
            if (lat > 60.0 && lon > -55.0 && lon < -18.0 && year < -2500) return 0.0; // Greenland pre-Saqqaq
            if (lat > 80.0) return 0.0;
            return 1.0;
        }
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
        return HistoricalScenarioFeaturesCatalog.distSq(lng1, lat1, lng2, lat2);
    }
}
