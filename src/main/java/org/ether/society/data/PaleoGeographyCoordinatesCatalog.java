/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * High-Precision Paleogeographical Coordinates & Geometry Catalog for Ether Simulation Engine.
 * 
 * <p>Centralizes empirical archaeological hearths, Quaternary continental ice sheet boundary polygons,
 * and paleolithic migration corridor polylines (Kelp Highway, Ice-Free Corridor, Beringia Steppe).
 * Extracted from {@link HistoricalMapGenerator} to ensure modular cartographic architecture,
 * Separation of Concerns (SoC), and high maintainability.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PaleoGeographyCoordinatesCatalog {

    private PaleoGeographyCoordinatesCatalog() {
        // Static utility catalog
    }

    // =========================================================================
    // ARCHAEOLOGICAL HOMININ HEARTHS (-100,000 BP)
    // =========================================================================

    /** Archaeological Middle Stone Age (MSA) Sapiens hearths across Africa. */
    public static final double[][] HEARTHS_SAPIENS_AFRICA = {
        {36.0, 4.5},    // Omo Kibish / Rift Valley Core
        {40.5, 8.9},    // Herto / Middle Awash
        {32.7, 26.1},   // Taramsa Hill / Upper Nile
        {31.2, 30.0},   // Nile Delta / Lower Egypt
        {-8.8, 31.6},   // Jebel Irhoud (Morocco / Western Maghreb)
        {-2.4, 34.8},   // Taforalt & Rhafas (Eastern Morocco / Western Algeria)
        {1.3, 35.4},    // Columnata / Tiaret (Central Algeria)
        {5.4, 36.3},    // Aïn Boucherit / Aïn Hanech (Setif / Eastern Algeria)
        {22.0, 32.0},   // Haua Fteah (Cyrenaica / Libya)
        {5.1, 7.4},     // Iho Eleru (West Africa)
        {-17.0, 14.7},  // Bargny / Dakar Peninsula (Senegal MSA / Western Tip)
        {-12.0, 12.0},  // Falémé / Ounjougou (West African Savanna MSA)
        {-14.0, 24.0},  // Bir Gandus / Western Sahara MSA
        {20.0, 0.0},    // Congo Basin
        {21.2, -34.4},  // Blombos / Klasies River (South Africa)
        {31.9, -27.0}   // Border Cave (KwaZulu-Natal)
    };

    /** Early Sapiens pioneer settlements in the Levantine and Arabian corridors. */
    public static final double[][] HEARTHS_SAPIENS_PIONEERS = {
        {35.3, 32.7},   // Skhul & Qafzeh (Mount Carmel / Southern Levant)
        {34.0, 29.5},   // Sinai Corridor
        {41.0, 28.0},   // Al-Wusta (Nefud Desert / Central Arabia)
        {50.0, 26.0},   // Eastern Arabian Coastal Oasis
        {45.5, 18.5},   // Mundafan (Rub' al Khali / Southern Arabia)
        {44.0, 13.0},   // Bab-el-Mandeb crossing (Yemen)
        {54.0, 17.0},   // Dhofar coastal oasis (Oman)
        {55.9, 25.1}    // Jebel Faya (UAE / Gulf of Oman)
    };

    /** Western and Southern European Neanderthal hearth sites. */
    public static final double[][] HEARTHS_NEANDERTHAL_WEST = {
        {-5.3, 36.1},   // Gorham's Cave (Gibraltar / Southern Iberia)
        {-3.5, 42.3},   // Atapuerca (Northern Iberia)
        {-9.0, 39.0},   // Gruta da Oliveira (Portugal / Atlantic Iberia)
        {1.0, 45.0},    // La Ferrassie / Dordogne (France)
        {8.5, 50.0},    // Neander Valley / Swabian Jura (Germany)
        {-1.5, 53.2},   // Creswell Crags & Pontnewydd (Britain / Wales)
        {13.0, 41.5},   // Monte Circeo / Grotta Guattari (Italy)
        {22.4, 36.6},   // Kalamakia Cave (Mani / Southern Greece)
        {21.7, 39.7},   // Theopetra (Thessaly)
        {16.0, 45.0},   // Krapina / Vindija (Balkans)
        {34.0, 45.0}    // Kiik-Koba (Crimea)
    };

    /** Near Eastern and Zagros Mousterian Neanderthal sites. */
    public static final double[][] HEARTHS_NEANDERTHAL_ZAGROS = {
        {30.6, 37.0},   // Karain Cave (Anatolia)
        {40.0, 44.0},   // Mezmaiskaya (Caucasus)
        {44.2, 36.8},   // Shanidar Cave (Zagros / Northern Iraq)
        {47.4, 34.4},   // Bisitun & Wezmeh (Western Iran)
        {51.5, 32.4},   // Qaleh Bozi (Central Iran / Isfahan)
        {53.3, 29.8},   // Arsanjan / Barm-e Shur (Southern Iran / Fars)
        {52.0, 36.0}    // Hotu & Kamarband (Alborz / Caspian)
    };

    /** Altai, Siberian, and Tibetan Denisovan / Archaic hearths. */
    public static final double[][] HEARTHS_DENISOVAN_ALTAI = {
        {84.7, 51.4},   // Denisova Cave (Altai Mountains)
        {67.0, 38.0},   // Teshik-Tash / Obi-Rakhmat (Uzbekistan / Central Asia)
        {95.0, 55.0},   // Ust'-Ishim / Krasnoyarsk (Siberia)
        {102.8, 35.2},  // Baishiya Karst Cave (Tibetan Plateau / Xiahe)
        {115.0, 40.0}   // Nihewan / Zhoukoudian (Northern China)
    };

    /** South and Southeast Asian archaic hominin refugia. */
    public static final double[][] HEARTHS_EASTERN_ARCHAIC = {
        {77.0, 22.0},   // Narmada Valley (Central India)
        {79.0, 13.0},   // Attirampakkam (Southern India)
        {103.0, 20.0},  // Tam Pa Ling (Indochina)
        {111.5, 25.5},  // Daoxian / Fuyan Cave (Southern China)
        {102.0, 2.0},   // Malayan Peninsula
        {111.0, -7.4}   // Solo River / Ngandong (Java / Sundaland)
    };

    // =========================================================================
    // CHAIKIN SMOOTHING ALGORITHM
    // =========================================================================

    /**
     * Applies Chaikin's corner cutting algorithm to generate organic polygon contours.
     *
     * @param poly input coordinate polygon vertices [N][2] {lon, lat}
     * @param iterations subdivision iteration count
     * @return smoothed coordinate polygon vertices
     */
    public static double[][] chaikinSmoothPolygon(double[][] poly, int iterations) {
        if (poly == null || poly.length < 3 || iterations <= 0) return poly;
        double[][] current = poly;
        for (int iter = 0; iter < iterations; iter++) {
            int n = current.length;
            double[][] next = new double[2 * n][2];
            for (int i = 0; i < n; i++) {
                double[] p0 = current[i];
                double[] p1 = current[(i + 1) % n];
                next[2 * i][0] = 0.75 * p0[0] + 0.25 * p1[0];
                next[2 * i][1] = 0.75 * p0[1] + 0.25 * p1[1];
                next[2 * i + 1][0] = 0.25 * p0[0] + 0.75 * p1[0];
                next[2 * i + 1][1] = 0.25 * p0[1] + 0.75 * p1[1];
            }
            current = next;
        }
        return current;
    }

    // =========================================================================
    // QUATERNARY CONTINENTAL ICE SHEET POLYGONS
    // =========================================================================

    /** Laurentide ice sheet polygon at Last Glacial Maximum (LGM, ~20,000 BP). */
    public static final double[][] POLY_LAURENTIDE_LGM = chaikinSmoothPolygon(new double[][]{
        {-142.0, 60.0}, {-136.0, 56.0}, {-126.0, 51.0}, {-120.0, 48.0},
        {-114.0, 47.5}, {-104.0, 46.5}, {-96.0, 42.5}, {-90.0, 39.5},
        {-83.0, 39.0}, {-77.0, 40.5}, {-73.0, 41.0}, {-68.0, 42.5},
        {-63.0, 45.0}, {-52.0, 47.0}, {-53.0, 54.0}, {-58.0, 61.0},
        {-65.0, 70.0}, {-85.0, 75.0}, {-115.0, 75.0}, {-135.0, 70.0}
    }, 3);

    /** Laurentide ice sheet polygon during Marine Isotope Stage 3 (MIS3, ~50,000 BP). */
    public static final double[][] POLY_LAURENTIDE_MIS3 = chaikinSmoothPolygon(new double[][]{
        {-98.0, 65.0}, {-95.0, 55.0}, {-85.0, 52.0}, {-75.0, 52.0}, {-65.0, 56.0},
        {-60.0, 62.0}, {-70.0, 68.0}, {-85.0, 70.0}
    }, 2);

    /** Laurentide ice sheet polygon during Younger Dryas (YD, ~10,900 BP). */
    public static final double[][] POLY_LAURENTIDE_YD = chaikinSmoothPolygon(new double[][]{
        {-122.0, 69.0}, {-114.0, 63.0}, {-106.0, 57.0}, {-94.0, 50.5}, {-82.0, 48.0},
        {-70.0, 48.5}, {-62.0, 50.0}, {-55.0, 54.0}, {-58.0, 62.0}, {-64.0, 72.0},
        {-85.0, 76.0}, {-112.0, 75.0}
    }, 2);

    /** Fennoscandian ice sheet polygon at LGM (~20,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_LGM = chaikinSmoothPolygon(new double[][]{
        {-11.0, 54.0}, {-10.0, 51.5}, {-6.0, 52.0}, {-1.0, 53.0},
        {4.0, 52.5}, {10.0, 52.0}, {18.0, 52.5}, {26.0, 54.0},
        {34.0, 57.5}, {42.0, 62.0}, {52.0, 67.0}, {58.0, 71.0},
        {50.0, 75.0}, {35.0, 76.0}, {15.0, 74.0}, {5.0, 69.0},
        {-2.0, 64.0}, {-8.0, 58.5}
    }, 3);

    /** Fennoscandian ice sheet polygon during MIS3 (~50,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_MIS3 = chaikinSmoothPolygon(new double[][]{
        {6.0, 60.0}, {8.0, 62.0}, {14.0, 68.0}, {20.0, 70.0},
        {25.0, 68.0}, {22.0, 64.0}, {16.0, 62.0}, {10.0, 59.0}
    }, 2);

    /** Fennoscandian ice sheet polygon during Younger Dryas (~10,900 BP). */
    public static final double[][] POLY_FENNOSCANDIA_YD = chaikinSmoothPolygon(new double[][]{
        {4.0, 61.0}, {7.0, 63.5}, {14.0, 68.0}, {22.0, 70.0}, {30.0, 68.0},
        {32.0, 64.0}, {28.0, 60.5}, {22.0, 59.5}, {13.0, 58.5}, {7.0, 59.0}
    }, 2);

    /** Fennoscandian ice sheet polygon in the Early Holocene (~8,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_EARLY_HOLOCENE = chaikinSmoothPolygon(new double[][]{
        {10.0, 62.0}, {14.0, 65.5}, {18.0, 67.5}, {22.0, 67.0}, {18.0, 64.0}, {13.0, 62.5}
    }, 2);

    /** Alpine glacial ice sheet polygon at LGM. */
    public static final double[][] POLY_ALPS_LGM = chaikinSmoothPolygon(new double[][]{
        {5.5, 45.0}, {6.8, 46.2}, {9.5, 47.2}, {13.5, 47.0},
        {14.5, 46.0}, {11.5, 45.4}, {7.8, 45.0}
    }, 2);

    /** Patagonian ice sheet polygon at LGM. */
    public static final double[][] POLY_PATAGONIA_LGM = chaikinSmoothPolygon(new double[][]{
        {-74.0, -39.0}, {-71.2, -40.5}, {-70.8, -46.0}, {-72.0, -51.5},
        {-70.0, -55.0}, {-74.5, -54.2}, {-75.5, -46.5}, {-74.5, -41.0}
    }, 2);

    /** Beringia unglaciated steppe refuge polygon during LGM. */
    public static final double[][] POLY_BERINGIA_REFUGE = chaikinSmoothPolygon(new double[][]{
        {130.0, 71.0}, {145.0, 72.0}, {165.0, 70.0}, {-170.0, 68.0},
        {-160.0, 64.0}, {-145.0, 63.0}, {-135.0, 66.0}, {-138.0, 69.0},
        {-150.0, 71.5}, {-175.0, 72.0}, {160.0, 66.0}, {140.0, 67.0}
    }, 2);

    // =========================================================================
    // CONTINENTAL MIGRATION ARTERIES
    // =========================================================================

    /** Pacific Coastal Kelp Highway route across the rim of the Americas. */
    public static final double[][] ROUTE_PACIFIC_KELP_HIGHWAY = {
        {-165.0, 64.0}, {-158.0, 58.5}, {-152.0, 58.0}, {-145.0, 60.0},
        {-138.0, 59.0}, {-135.0, 57.0}, {-131.5, 53.5}, {-127.5, 50.5},
        {-124.5, 47.5}, {-124.0, 43.5}, {-122.5, 37.8}, {-119.8, 34.2},
        {-116.5, 31.5}, {-111.0, 25.0}, {-105.5, 21.5}, {-101.5, 17.5},
        {-96.5, 15.8}, {-92.5, 14.5}, {-87.5, 13.0}, {-85.5, 10.5},
        {-83.5, 8.5}, {-81.0, 7.5}, {-79.0, 8.0}, {-77.5, 6.0},
        {-78.5, 1.5}, {-80.5, -2.5}, {-80.0, -5.5}, {-79.0, -8.0},
        {-76.5, -14.0}, {-72.0, -16.5}, {-70.3, -20.0}, {-70.4, -27.0},
        {-71.5, -33.0}, {-73.2, -40.0}, {-73.8, -41.8}, {-74.5, -45.5},
        {-72.5, -51.5}, {-70.0, -53.5}
    };

    /** Continental interior ice-free corridor (opening ~13.8k to 12.5k BP). */
    public static final double[][] ROUTE_ICE_FREE_CORRIDOR = {
        {-140.0, 67.0}, {-135.0, 64.0}, {-128.0, 60.0}, {-122.0, 56.0},
        {-116.0, 52.0}, {-111.0, 48.0}, {-105.0, 43.0}, {-100.0, 38.0}
    };

    /** Columbia-Snake River migration corridor. */
    public static final double[][] ROUTE_COLUMBIA_SNAKE = {
        {-124.0, 46.2}, {-120.5, 45.7}, {-118.0, 46.2}, {-116.0, 43.5}, {-112.0, 42.5}
    };

    /** Missouri River migration corridor. */
    public static final double[][] ROUTE_MISSOURI_RIVER = {
        {-111.0, 46.5}, {-104.0, 47.5}, {-98.0, 43.0}, {-94.5, 39.5}, {-90.5, 38.8}
    };

    /** Mississippi-Ohio confluence corridor. */
    public static final double[][] ROUTE_MISSISSIPPI_OHIO = {
        {-94.5, 45.0}, {-91.0, 41.5}, {-89.0, 37.0}, {-84.5, 39.0}, {-80.0, 40.5}
    };

    /** Lower Mississippi River artery. */
    public static final double[][] ROUTE_LOWER_MISSISSIPPI = {
        {-89.0, 37.0}, {-91.0, 32.0}, {-89.5, 29.5}
    };

    /** Southeastern coastal migration corridor. */
    public static final double[][] ROUTE_SOUTHEAST_FLORIDA = {
        {-77.0, 37.5}, {-80.0, 34.0}, {-82.0, 31.0}, {-84.0, 30.0}, {-81.0, 26.5}
    };

    /** Andean spine migration corridor. */
    public static final double[][] ROUTE_ANDES_CORRIDOR = {
        {-78.5, 0.0}, {-77.5, -5.5}, {-76.8, -9.5}, {-74.5, -13.5},
        {-69.0, -18.0}, {-66.0, -24.0}, {-68.0, -32.5}
    };

    /** Amazon mainstem artery. */
    public static final double[][] ROUTE_AMAZON_MAINSTEM = {
        {-77.0, -4.0}, {-73.5, -3.8}, {-69.5, -3.5}, {-64.5, -3.2},
        {-60.0, -3.1}, {-55.0, -2.2}, {-51.0, -1.5}, {-48.5, -0.5}
    };

    /** Amazon-Madeira tributary artery. */
    public static final double[][] ROUTE_AMAZON_MADEIRA = {
        {-60.0, -3.1}, {-62.0, -8.0}, {-64.0, -12.0}, {-58.0, -15.0}
    };

    /** Brazilian savanna corridor. */
    public static final double[][] ROUTE_BRAZIL_SAVANNA = {
        {-42.5, -9.0}, {-43.5, -12.5}, {-44.5, -16.0}, {-44.0, -19.5}, {-48.0, -25.0}
    };

    /** Beringia mammoth steppe corridor across Siberia, Chukotka and Alaska. */
    public static final double[][] ROUTE_BERINGIA_STEPPE_CORRIDOR = {
        {130.0, 71.0}, {140.0, 70.5}, {150.0, 69.5}, {160.0, 68.0}, {170.0, 66.5},
        {179.0, 66.0}, {-175.0, 65.5}, {-168.0, 65.0}, {-160.0, 64.5}, {-150.0, 64.5},
        {-142.0, 64.0}, {-138.0, 64.0}
    };

    // =========================================================================
    // GEOMETRIC UTILITIES
    // =========================================================================

    /**
     * Calculates the minimum spherical arc distance between a coordinate and a polyline.
     *
     * @param lon longitude in degrees
     * @param lat latitude in degrees
     * @param line polyline coordinates array [M][2]
     * @return minimum Euclidean distance in degrees on the equirectangular projection
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

            double ex = lon - px;
            if (ex > 180.0) ex -= 360.0;
            else if (ex < -180.0) ex += 360.0;
            double ey = lat - py;
            double d2 = ex * ex + ey * ey;
            if (d2 < minD2) minD2 = d2;
        }
        return Math.sqrt(minD2);
    }
}
