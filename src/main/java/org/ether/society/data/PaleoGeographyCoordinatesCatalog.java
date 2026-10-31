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
 * Coordinates are dynamically loaded from external JSON datasets via {@link PaleoGeometryCatalog}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PaleoGeographyCoordinatesCatalog {

    private PaleoGeographyCoordinatesCatalog() {
        // Static utility catalog
    }

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
    // ARCHAEOLOGICAL HOMININ HEARTHS (-100,000 BP)
    // =========================================================================

    /** Archaeological Middle Stone Age (MSA) Sapiens hearths across Africa. */
    public static final double[][] HEARTHS_SAPIENS_AFRICA = PaleoGeometryCatalog.loadHearth("HEARTHS_SAPIENS_AFRICA");

    /** Early Sapiens pioneer settlements in the Levantine and Arabian corridors. */
    public static final double[][] HEARTHS_SAPIENS_PIONEERS = PaleoGeometryCatalog.loadHearth("HEARTHS_SAPIENS_PIONEERS");

    /** Western and Southern European Neanderthal hearth sites. */
    public static final double[][] HEARTHS_NEANDERTHAL_WEST = PaleoGeometryCatalog.loadHearth("HEARTHS_NEANDERTHAL_WEST");

    /** Near Eastern and Zagros Mousterian Neanderthal sites. */
    public static final double[][] HEARTHS_NEANDERTHAL_ZAGROS = PaleoGeometryCatalog.loadHearth("HEARTHS_NEANDERTHAL_ZAGROS");

    /** Altai, Siberian, and Tibetan Denisovan / Archaic hearths. */
    public static final double[][] HEARTHS_DENISOVAN_ALTAI = PaleoGeometryCatalog.loadHearth("HEARTHS_DENISOVAN_ALTAI");

    /** South and Southeast Asian archaic hominin refugia. */
    public static final double[][] HEARTHS_EASTERN_ARCHAIC = PaleoGeometryCatalog.loadHearth("HEARTHS_EASTERN_ARCHAIC");

    // =========================================================================
    // QUATERNARY CONTINENTAL ICE SHEET POLYGONS
    // =========================================================================

    /** Laurentide ice sheet polygon at Last Glacial Maximum (LGM, ~20,000 BP). */
    public static final double[][] POLY_LAURENTIDE_LGM = PaleoGeometryCatalog.loadIceSheet("POLY_LAURENTIDE_LGM");

    /** Laurentide ice sheet polygon during Marine Isotope Stage 3 (MIS3, ~50,000 BP). */
    public static final double[][] POLY_LAURENTIDE_MIS3 = PaleoGeometryCatalog.loadIceSheet("POLY_LAURENTIDE_MIS3");

    /** Laurentide ice sheet polygon during Younger Dryas (YD, ~10,900 BP). */
    public static final double[][] POLY_LAURENTIDE_YD = PaleoGeometryCatalog.loadIceSheet("POLY_LAURENTIDE_YD");

    /** Fennoscandian ice sheet polygon at LGM (~20,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_LGM = PaleoGeometryCatalog.loadIceSheet("POLY_FENNOSCANDIA_LGM");

    /** Fennoscandian ice sheet polygon during MIS3 (~50,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_MIS3 = PaleoGeometryCatalog.loadIceSheet("POLY_FENNOSCANDIA_MIS3");

    /** Fennoscandian ice sheet polygon during Younger Dryas (~10,900 BP). */
    public static final double[][] POLY_FENNOSCANDIA_YD = PaleoGeometryCatalog.loadIceSheet("POLY_FENNOSCANDIA_YD");

    /** Fennoscandian ice sheet polygon in the Early Holocene (~8,000 BP). */
    public static final double[][] POLY_FENNOSCANDIA_EARLY_HOLOCENE = PaleoGeometryCatalog.loadIceSheet("POLY_FENNOSCANDIA_EARLY_HOLOCENE");

    /** Alpine glacial ice sheet polygon at LGM. */
    public static final double[][] POLY_ALPS_LGM = PaleoGeometryCatalog.loadIceSheet("POLY_ALPS_LGM");

    /** Patagonian ice sheet polygon at LGM. */
    public static final double[][] POLY_PATAGONIA_LGM = PaleoGeometryCatalog.loadIceSheet("POLY_PATAGONIA_LGM");

    /** Beringia unglaciated steppe refuge polygon during LGM. */
    public static final double[][] POLY_BERINGIA_REFUGE = PaleoGeometryCatalog.loadIceSheet("POLY_BERINGIA_REFUGE");

    // =========================================================================
    // CONTINENTAL MIGRATION ARTERIES
    // =========================================================================

    /** Pacific Coastal Kelp Highway route across the rim of the Americas. */
    public static final double[][] ROUTE_PACIFIC_KELP_HIGHWAY = PaleoGeometryCatalog.loadRoute("ROUTE_PACIFIC_KELP_HIGHWAY");

    /** Continental interior ice-free corridor (opening ~13.8k to 12.5k BP). */
    public static final double[][] ROUTE_ICE_FREE_CORRIDOR = PaleoGeometryCatalog.loadRoute("ROUTE_ICE_FREE_CORRIDOR");

    /** Columbia-Snake River migration corridor. */
    public static final double[][] ROUTE_COLUMBIA_SNAKE = PaleoGeometryCatalog.loadRoute("ROUTE_COLUMBIA_SNAKE");

    /** Missouri River migration corridor. */
    public static final double[][] ROUTE_MISSOURI_RIVER = PaleoGeometryCatalog.loadRoute("ROUTE_MISSOURI_RIVER");

    /** Mississippi-Ohio confluence corridor. */
    public static final double[][] ROUTE_MISSISSIPPI_OHIO = PaleoGeometryCatalog.loadRoute("ROUTE_MISSISSIPPI_OHIO");

    /** Lower Mississippi River artery. */
    public static final double[][] ROUTE_LOWER_MISSISSIPPI = PaleoGeometryCatalog.loadRoute("ROUTE_LOWER_MISSISSIPPI");

    /** Southeastern coastal migration corridor. */
    public static final double[][] ROUTE_SOUTHEAST_FLORIDA = PaleoGeometryCatalog.loadRoute("ROUTE_SOUTHEAST_FLORIDA");

    /** Andean spine migration corridor. */
    public static final double[][] ROUTE_ANDES_CORRIDOR = PaleoGeometryCatalog.loadRoute("ROUTE_ANDES_CORRIDOR");

    /** Amazon mainstem artery. */
    public static final double[][] ROUTE_AMAZON_MAINSTEM = PaleoGeometryCatalog.loadRoute("ROUTE_AMAZON_MAINSTEM");

    /** Amazon-Madeira tributary artery. */
    public static final double[][] ROUTE_AMAZON_MADEIRA = PaleoGeometryCatalog.loadRoute("ROUTE_AMAZON_MADEIRA");

    /** Brazilian savanna corridor. */
    public static final double[][] ROUTE_BRAZIL_SAVANNA = PaleoGeometryCatalog.loadRoute("ROUTE_BRAZIL_SAVANNA");

    /** Beringia mammoth steppe corridor across Siberia, Chukotka and Alaska. */
    public static final double[][] ROUTE_BERINGIA_STEPPE_CORRIDOR = PaleoGeometryCatalog.loadRoute("ROUTE_BERINGIA_STEPPE_CORRIDOR");

    /** Southern coastal dispersal route out of Africa across the Indian Ocean rim to Sahul (~65,000–50,000 BP). */
    public static final double[][] ROUTE_OUT_OF_AFRICA_SOUTHERN_DISPERSAL = PaleoGeometryCatalog.loadRoute("ROUTE_OUT_OF_AFRICA_SOUTHERN_DISPERSAL");

    /** Levantine & Danubian expansion corridor into Europe (~45,000 BP Aurignacian). */
    public static final double[][] ROUTE_OUT_OF_AFRICA_NORTHERN_LEVANTINE = PaleoGeometryCatalog.loadRoute("ROUTE_OUT_OF_AFRICA_NORTHERN_LEVANTINE");

    /** Eurasian mammoth steppe highway across Central Asia to Lake Baikal and Yana (~45,000–25,000 BP). */
    public static final double[][] ROUTE_EURASIAN_STEPPE_MAMMOTH_HIGHWAY = PaleoGeometryCatalog.loadRoute("ROUTE_EURASIAN_STEPPE_MAMMOTH_HIGHWAY");

    /** Mediterranean coastal expansion artery (~45,000–40,000 BP). */
    public static final double[][] ROUTE_MEDITERRANEAN_COASTAL_EXPANSION = PaleoGeometryCatalog.loadRoute("ROUTE_MEDITERRANEAN_COASTAL_EXPANSION");

    /** East Asian coastal paleolithic expansion artery into Korea and Japan (~38,000 BP). */
    public static final double[][] ROUTE_EAST_ASIAN_COASTAL_EXPANSION = PaleoGeometryCatalog.loadRoute("ROUTE_EAST_ASIAN_COASTAL_EXPANSION");

    /** Trans-Saharan green paleocorridor across the Lake Mega-Chad and Ahaggar during humid phases. */
    public static final double[][] ROUTE_SAHARA_GREEN_PALEOCORRIDOR = PaleoGeometryCatalog.loadRoute("ROUTE_SAHARA_GREEN_PALEOCORRIDOR");

    /** Austronesian maritime voyaging expansion across Wallacea, Melanesia, Polynesia and Madagascar. */
    public static final double[][] ROUTE_AUSTRONESIAN_MARITIME_EXPANSION = PaleoGeometryCatalog.loadRoute("ROUTE_AUSTRONESIAN_MARITIME_EXPANSION");

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
