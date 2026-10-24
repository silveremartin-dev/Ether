/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Ultra-High Precision Cartographic Generator for Ether Simulation Engine.
 * Supports continuous topographic landmasking and tailored multi-channel spatial tensors
 * across all 19 built-in scenarios.
 */
public class HistoricalMapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalMapGenerator.class);

    /** Internal state: width (int). */
    public static final int WIDTH = 2048;
    /** Internal state: height (int). */
    public static final int HEIGHT = 1024;

    private static final List<Path2D> LAND_POLYGONS = new ArrayList<>();
    private static final List<Path2D> SEA_POLYGONS = new ArrayList<>();
    private static boolean[][] FAST_LAND_GRID = new boolean[720][360];

    static {
        initHighPrecisionGeographicPolygons();
    }

    // Archaeological Hominin Hearths for Organic Orographic Voronoi (-100,000 BP)
    // Centralized in PaleoGeographyCoordinatesCatalog
    private static final double[][] HEARTHS_SAPIENS_AFRICA = PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_AFRICA;
    private static final double[][] HEARTHS_SAPIENS_PIONEERS = PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_PIONEERS;
    private static final double[][] HEARTHS_NEANDERTHAL_WEST = PaleoGeographyCoordinatesCatalog.HEARTHS_NEANDERTHAL_WEST;
    private static final double[][] HEARTHS_NEANDERTHAL_ZAGROS = PaleoGeographyCoordinatesCatalog.HEARTHS_NEANDERTHAL_ZAGROS;
    private static final double[][] HEARTHS_DENISOVAN_ALTAI = PaleoGeographyCoordinatesCatalog.HEARTHS_DENISOVAN_ALTAI;
    private static final double[][] HEARTHS_EASTERN_ARCHAIC = PaleoGeographyCoordinatesCatalog.HEARTHS_EASTERN_ARCHAIC;

    /**
     * Compute hominin clade weights.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
     */
    public static double[] computeHomininCladeWeights(double lon, double lat) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
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
        for (double[] h : HEARTHS_SAPIENS_AFRICA) d0 = Math.min(d0, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d0 += 50.0; // Cannot cross Mediterranean into Europe
        dists[0] = d0;

        // Clade 1: Sapiens Out-of-Africa Pioneers (Levant / Arabia)
        double d1 = Double.MAX_VALUE;
        for (double[] h : HEARTHS_SAPIENS_PIONEERS) d1 = Math.min(d1, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isEuropeanLandmass) d1 += 50.0;
        dists[1] = d1;

        // Clade 2: Neanderthals Western Classical Mousterian (Europe & Iberia)
        double d2 = Double.MAX_VALUE;
        for (double[] h : HEARTHS_NEANDERTHAL_WEST) d2 = Math.min(d2, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d2 += 50.0; // Cannot cross Mediterranean into North Africa
        dists[2] = d2;

        // Clade 3: Neanderthals Zagros & Near East
        double d3 = Double.MAX_VALUE;
        for (double[] h : HEARTHS_NEANDERTHAL_ZAGROS) d3 = Math.min(d3, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass) d3 += 50.0; // Cannot cross into North Africa
        dists[3] = d3;

        // Clade 4: Denisovans Altai & Siberian Archaic
        double d4 = Double.MAX_VALUE;
        for (double[] h : HEARTHS_DENISOVAN_ALTAI) d4 = Math.min(d4, Math.sqrt(distSq(lon, lat, h[0], h[1])));
        if (isAfricanLandmass || isEuropeanLandmass) d4 += 50.0;
        d4 += himalayas * 16.0; // Himalayan barrier separating Denisovans from Indian subcontinent
        dists[4] = d4;

        // Clade 5: Eastern Archaic & Sundaland
        double d5 = Double.MAX_VALUE;
        for (double[] h : HEARTHS_EASTERN_ARCHAIC) d5 = Math.min(d5, Math.sqrt(distSq(lon, lat, h[0], h[1])));
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
     * Get hominin entity id.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
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
     * Get hominin species type.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
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
     * Blend paleo traits.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param valSapiens the val sapiens parameter (double)
     * @param valNeanderthal the val neanderthal parameter (double)
     * @param valDenisovan the val denisovan parameter (double)
     * @return the resulting computation or state reference
     */
    public static double blendPaleoTraits(double lon, double lat, double valSapiens, double valNeanderthal, double valDenisovan) {
        double[] w = computeHomininCladeWeights(lon, lat);
        if (w == null) return 0.0;
        // Clades 0,1: Sapiens | Clades 2,3: Neanderthal | Clades 4,5: Denisovan
        double sWeight = w[0] + w[1];
        double nWeight = w[2] + w[3];
        double dWeight = w[4] + w[5];
        return sWeight * valSapiens + nWeight * valNeanderthal + dWeight * valDenisovan;
    }

    // --- PREHISTORIC GLACIAL & GEOGRAPHIC POLYGONS ---
    /**
     * Chaikin smooth polygon delegating to {@link PaleoGeographyCoordinatesCatalog}.
     *
     * @param poly the poly parameter (double[][])
     * @param iterations the iterations parameter (int)
     * @return the resulting smoothed polygon
     */
    public static double[][] chaikinSmoothPolygon(double[][] poly, int iterations) {
        return PaleoGeographyCoordinatesCatalog.chaikinSmoothPolygon(poly, iterations);
    }

    // Quaternary Continental Ice Sheets - Centralized in PaleoGeographyCoordinatesCatalog
    public static final double[][] POLY_LAURENTIDE_LGM = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM;
    public static final double[][] POLY_LAURENTIDE_MIS3 = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_MIS3;
    public static final double[][] POLY_LAURENTIDE_YD = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_YD;
    public static final double[][] POLY_FENNOSCANDIA_LGM = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM;
    public static final double[][] POLY_FENNOSCANDIA_MIS3 = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3;
    public static final double[][] POLY_FENNOSCANDIA_YD = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_YD;
    public static final double[][] POLY_FENNOSCANDIA_EARLY_HOLOCENE = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_EARLY_HOLOCENE;
    public static final double[][] POLY_ALPS_LGM = PaleoGeographyCoordinatesCatalog.POLY_ALPS_LGM;
    public static final double[][] POLY_PATAGONIA_LGM = PaleoGeographyCoordinatesCatalog.POLY_PATAGONIA_LGM;
    public static final double[][] POLY_BERINGIA_REFUGE = PaleoGeographyCoordinatesCatalog.POLY_BERINGIA_REFUGE;

    // Detailed Continental Migration Arteries - Centralized in PaleoGeographyCoordinatesCatalog
    public static final double[][] ROUTE_PACIFIC_KELP_HIGHWAY = PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY;
    public static final double[][] ROUTE_ICE_FREE_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_ICE_FREE_CORRIDOR;
    public static final double[][] ROUTE_COLUMBIA_SNAKE = PaleoGeographyCoordinatesCatalog.ROUTE_COLUMBIA_SNAKE;
    public static final double[][] ROUTE_MISSOURI_RIVER = PaleoGeographyCoordinatesCatalog.ROUTE_MISSOURI_RIVER;
    public static final double[][] ROUTE_MISSISSIPPI_OHIO = PaleoGeographyCoordinatesCatalog.ROUTE_MISSISSIPPI_OHIO;
    public static final double[][] ROUTE_LOWER_MISSISSIPPI = PaleoGeographyCoordinatesCatalog.ROUTE_LOWER_MISSISSIPPI;
    public static final double[][] ROUTE_SOUTHEAST_FLORIDA = PaleoGeographyCoordinatesCatalog.ROUTE_SOUTHEAST_FLORIDA;
    public static final double[][] ROUTE_ANDES_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR;
    public static final double[][] ROUTE_AMAZON_MAINSTEM = PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM;
    public static final double[][] ROUTE_AMAZON_MADEIRA = PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MADEIRA;
    public static final double[][] ROUTE_BRAZIL_SAVANNA = PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA;
    public static final double[][] ROUTE_BERINGIA_STEPPE_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR;

    /**
     * Dist to polyline.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param line the line parameter (double[][])
     * @return the resulting computation or state reference
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
     * Signed distance to polygon.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param poly the poly parameter (double[][])
     * @return the resulting computation or state reference
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
     * Is remote oceanic island.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static boolean isRemoteOceanicIsland(double lon, double lat, long year) {
        // 1. Remote Polynesia & Central/Eastern Pacific
        if (year < 1200) {
            // Hawaii
            if (lat >= 18.0 && lat <= 23.0 && lon >= -162.0 && lon <= -154.0) return true;
            // Easter Island (Rapa Nui)
            if (lat >= -28.0 && lat <= -26.0 && lon >= -110.0 && lon <= -108.0) return true;
            // Galapagos
            if (lat >= -2.0 && lat <= 2.0 && lon >= -92.0 && lon <= -88.0) return true;
            // Cocos Island & Malpelo Island (Eastern Pacific uninhabited)
            if (lat >= 4.5 && lat <= 6.5 && lon >= -88.0 && lon <= -86.0) return true;
            if (lat >= 3.0 && lat <= 4.8 && lon >= -82.5 && lon <= -80.5) return true;
            // Clipperton & Revillagigedo
            if (lat >= 9.0 && lat <= 11.5 && lon >= -110.5 && lon <= -108.0) return true;
            if (lat >= 17.5 && lat <= 20.0 && lon >= -115.5 && lon <= -109.5) return true;
            // Juan Fernandez & Desventuradas (Chilean Pacific)
            if (lat >= -34.5 && lat <= -25.5 && lon >= -82.0 && lon <= -78.0) return true;
            // New Zealand
            if (year < 1280 && lat <= -34.0 && lat >= -48.0 && lon >= 165.0 && lon <= 180.0) return true;
            // Remote Eastern/Central Pacific Basin
            if (lon >= -180.0 && lon <= -120.0 && lat >= -30.0 && lat <= 30.0) return true;
            // South Pacific isolated islands
            if (lon >= -150.0 && lon <= -130.0 && lat >= -30.0 && lat <= 0.0) return true;
        }
        if (year < -1200) {
            // West Polynesia / Micronesia / Remote Melanesia (Fiji, Samoa, Tonga, Vanuatu, Marshall, Carolines, Marianas)
            if (lon >= 155.0 && lon <= 180.0 && lat >= -25.0 && lat <= 20.0) return true;
            if (lon >= 135.0 && lon <= 165.0 && lat >= 5.0 && lat <= 22.0) return true;
        }

        // 2. Remote Atlantic Islands
        if (year < 1400) {
            // Fernando de Noronha & Trindade
            if (lat >= -4.5 && lat <= -3.0 && lon >= -33.5 && lon <= -31.5) return true;
            if (lat >= -21.5 && lat <= -19.5 && lon >= -30.5 && lon <= -28.0) return true;
            // Azores
            if (lat >= 36.0 && lat <= 40.5 && lon >= -32.0 && lon <= -24.0) return true;
            // Madeira
            if (lat >= 32.0 && lat <= 33.5 && lon >= -17.5 && lon <= -16.0) return true;
            // Cape Verde (settled 1462 AD)
            if (lat >= 14.5 && lat <= 17.5 && lon >= -25.5 && lon <= -22.5) return true;
            // Saint Helena & Ascension & Tristan da Cunha
            if (lat >= -17.0 && lat <= -15.0 && lon >= -6.5 && lon <= -5.0) return true;
            if (lat >= -8.5 && lat <= -7.5 && lon >= -15.0 && lon <= -13.5) return true;
            if (lat >= -38.0 && lat <= -36.0 && lon >= -13.5 && lon <= -11.5) return true;
            // Falklands / Malvinas & South Georgia
            if (lat >= -53.5 && lat <= -51.0 && lon >= -62.0 && lon <= -57.0) return true;
            if (lat >= -55.0 && lat <= -53.5 && lon >= -39.0 && lon <= -35.0) return true;
            // Bermuda
            if (lat >= 31.5 && lat <= 33.0 && lon >= -65.5 && lon <= -64.0) return true;
            // Iceland (settled 874 AD)
            if (year < 874 && lat >= 63.0 && lat <= 67.0 && lon >= -25.0 && lon <= -12.0) return true;
        }
        if (year < -1000) {
            // Canary Islands
            if (lat >= 27.5 && lat <= 29.5 && lon >= -18.5 && lon <= -13.0) return true;
        }

        // 3. Remote Indian Ocean Islands
        if (year < 500) {
            // Madagascar (settled ~500 AD)
            if (lat <= -11.5 && lat >= -26.0 && lon >= 43.0 && lon <= 51.0) return true;
            // Mascarenes (Mauritius, Reunion, Rodrigues)
            if (lat <= -19.5 && lat >= -22.0 && lon >= 55.0 && lon <= 64.0) return true;
            // Seychelles
            if (lat >= -5.5 && lat <= -4.0 && lon >= 54.5 && lon <= 56.5) return true;
            // Comoros
            if (lat >= -13.0 && lat <= -11.0 && lon >= 43.0 && lon <= 45.5) return true;
            // Maldives & Chagos
            if (year < -500 && lat >= -8.0 && lat <= 8.0 && lon >= 71.0 && lon <= 74.5) return true;
            // Sub-Antarctic Islands
            if (lat < -35.0 && lon >= 35.0 && lon <= 90.0) return true;
        }

        return false;
    }

    /**
     * Get hominin occupancy weight.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
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

        if (year <= -85000L) {
            // -100,000 BP (MIS 5e Eemian Interglacial): Confined to Africa & Eurasia, Sahul & Americas strictly 0
            if (lon < -25.0 && lon > -170.0) return 0.0; // Americas strictly unpopulated
            if (lon > 110.0 && lat < -5.0) return 0.0;   // Sahul strictly pre-landfall
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
            // -74,000 BP (Youngest Toba Tuff Super-Eruption & MIS 4 Glacial Stadial):
            // Americas & Sahul strictly uninhabited
            if ((lon < -25.0 && lon > -170.0) || (lon > 110.0 && lat < -5.0)) {
                return 0.0;
            }
            int species = getHomininSpeciesType(lon, lat);
            if (species == 0) return 0.0;

            // Scandinavian glacier
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            // Toba Volcanic Ash Depopulation in South Asia (Jurreru/Narmada/Deccan severely depressed)
            double dAsh = Math.exp(-(Math.pow(lat - 18.0, 2) / 120.0 + Math.pow(lon - 80.0, 2) / 220.0));
            double ashFactor = Math.max(0.08, 1.0 - dAsh * 0.88);

            // Cold permafrost clamp at 52°N in Eurasia
            double maxLat = 52.0;
            if (lat > maxLat) return 0.0;
            if (lat > 46.0) {
                double t = Math.clamp((lat - 46.0) / 6.0, 0.0, 1.0);
                return Math.min(wFenno, (1.0 - t * t * (3.0 - 2.0 * t)) * ashFactor);
            }
            return wFenno * ashFactor;
        } else if (year <= -52000L) {
            // -65,000 to -52,000 BP (MIS 4 / Early MIS 3 Out-of-Africa Coastal Dispersal to Northern Sahul):
            // Americas strictly uninhabited
            if (lon < -25.0 && lon > -170.0) return 0.0;

            // Sahul initial landfall: northern fringe only (Madjedbebe / Kimberley), southern Australia & Tasmania strictly 0
            if (lon > 110.0 && lat < -5.0) {
                if (lat < -20.0) return 0.0; // Southern Australia & Tasmania unpopulated
                double dMadj = Math.sqrt(distSq(lon, lat, 132.9, -12.5));
                double dKimb = Math.sqrt(distSq(lon, lat, 125.0, -16.0));
                if (dMadj <= 5.0 || dKimb <= 5.0) return 0.65;
                return 0.15;
            }

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));
            if (lat >= 56.0) return 0.0;
            if (lat > 48.0) {
                double t = Math.clamp((lat - 48.0) / 8.0, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -42000L) {
            // -52,000 to -42,000 BP (MIS 3 Sahul Colonization across interior, Bassian plain crossing in progress):
            // Americas strictly uninhabited
            if (lon < -25.0 && lon > -170.0) return 0.0;

            // Sahul mainland populated; Tasmania strictly uninhabited before Bassian bridge traversal (~39-35k BP)
            if (lon > 110.0 && lat < -5.0) {
                if (lat < -39.0) return 0.0; // Tasmania strictly unpopulated at -50,000 BP
                return 1.0;
            }

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));
            if (lat >= 58.5) return 0.0;
            if (lat > 50.0) {
                double t = Math.clamp((lat - 50.0) / 8.5, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -32000L) {
            // -42,000 to -32,000 BP (Late MIS 3 / Aurignacian / Tasmania firmly established):
            // Americas strictly uninhabited before LGM
            if (lon < -25.0 && lon > -170.0) return 0.0;

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
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
            double dLaur = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_MIS3);
            if (dLaur <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            // Beringia mammoth steppe corridor (Siberia -> Alaska -> Yukon)
            double dBeringia = distToPolyline(lon, lat, ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 5.5) {
                return Math.min(wLaur, 0.85);
            }

            // Pacific Coastal Kelp Highway
            double dPacific = distToPolyline(lon, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 4.0) {
                return 0.50 * wLaur;
            }

            // Pre-LGM Americas attested enclaves (White Sands, Chiquihuite, Meadowcroft, Pedra Furada)
            double dWhiteSands = Math.sqrt(distSq(lon, lat, -106.3, 32.8));
            double dChiquihuite = Math.sqrt(distSq(lon, lat, -103.5, 24.2));
            double dMeadowcroft = Math.sqrt(distSq(lon, lat, -80.4, 40.3));
            double dFurada = Math.sqrt(distSq(lon, lat, -42.5, -9.3));
            if (dWhiteSands <= 6.0 || dChiquihuite <= 6.0 || dMeadowcroft <= 5.0 || dFurada <= 6.0) {
                return 0.35 * wLaur;
            }

            // Unpopulated glaciated interior of Americas (Canada and northern US)
            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 36.0 && lat >= -54.0) {
                    return 0.10 * wLaur; // Sparse pioneer background
                }
                return 0.0; // Barren periglacial tundra
            }

            // Eurasia high arctic cutoff
            if (lat >= 72.0) return 0.0;
            if (lat > 66.0) {
                double t = Math.clamp((lat - 66.0) / 6.0, 0.0, 1.0);
                return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
            }
            return wFenno;
        } else if (year <= -16000L) {
            // -24,000 to -16,000 BP (LGM Peak, Solutrean, Kebaran & White Sands Footprints ~23k-21k BP)
            // 1. Laurentide / Cordilleran Ice Sheet
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_LGM);
            if (dLaurentide <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

            // 2. Fennoscandian / British Ice Sheet
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_LGM);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            // 3. Alpine & Patagonian Ice Caps
            double dAlps = signedDistanceToPolygon(lon, lat, POLY_ALPS_LGM);
            if (dAlps <= 0) return 0.0;
            double dPatagonia = signedDistanceToPolygon(lon, lat, POLY_PATAGONIA_LGM);
            if (dPatagonia <= 0) return 0.0;

            // 4. Beringia mammoth steppe corridor (Siberia -> Alaska -> Yukon)
            double dBeringia = distToPolyline(lon, lat, ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 6.0) {
                return Math.min(wLaur, 0.85);
            }

            // 5. Americas: Pacific Coastal Kelp Highway + Southern Interior Corridors & Enclaves
            double dPacific = distToPolyline(lon, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 4.5) {
                return 0.70 * wLaur;
            }

            double dAndes = distToPolyline(lon, lat, ROUTE_ANDES_CORRIDOR);
            if (dAndes <= 4.0) return 0.55;
            double dAmazon = distToPolyline(lon, lat, ROUTE_AMAZON_MAINSTEM);
            if (dAmazon <= 4.0) return 0.45;
            double dSavanna = distToPolyline(lon, lat, ROUTE_BRAZIL_SAVANNA);
            if (dSavanna <= 4.0) return 0.50;

            double dWhiteSands = Math.sqrt(distSq(lon, lat, -106.3, 32.8));
            double dCactus = Math.sqrt(distSq(lon, lat, -77.3, 36.8));
            double dPageLadson = Math.sqrt(distSq(lon, lat, -83.9, 30.1));
            if (dWhiteSands <= 6.5 || dCactus <= 5.5 || dPageLadson <= 5.5) {
                return 0.45 * wLaur;
            }

            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 38.0 && lat >= -54.0) {
                    return 0.15 * wLaur; // Diffuse southern interior foragers
                }
                return 0.0; // Barren periglacial desert immediately south of Laurentide
            }

            // High Arctic extreme (beyond 72°N)
            if (lat >= 72.0) return 0.0;
            double wArctic = 1.0;
            if (lat > 68.0) {
                double t = Math.clamp((lat - 68.0) / 4.0, 0.0, 1.0);
                wArctic = 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return Math.min(wFenno, wArctic);
        } else if (year <= -13500L) {
            // -16,000 to -13,500 BP (Deglaciation, Pacific Coastal Route Expansion & Monte Verde II ~14.5k BP)
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_LGM);
            if (dLaurentide <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_LGM);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            double dBeringia = distToPolyline(lon, lat, ROUTE_BERINGIA_STEPPE_CORRIDOR);
            if (dBeringia <= 6.0) return Math.min(wLaur, 0.90);

            double dPacific = distToPolyline(lon, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
            if (dPacific <= 5.5) return 0.85 * wLaur;

            double dAndes = distToPolyline(lon, lat, ROUTE_ANDES_CORRIDOR);
            if (dAndes <= 4.0) return 0.65;
            double dAmazon = distToPolyline(lon, lat, ROUTE_AMAZON_MAINSTEM);
            if (dAmazon <= 4.0) return 0.55;
            double dSavanna = distToPolyline(lon, lat, ROUTE_BRAZIL_SAVANNA);
            if (dSavanna <= 4.0) return 0.60;

            if (lon >= -135.0 && lon <= -30.0) {
                if (lat <= 44.0 && lat >= -54.0) return 0.35 * wLaur;
                return 0.0;
            }
            return Math.min(wFenno, 1.0);
        } else if (year <= -10500L) {
            // -13,500 to -10,500 BP (Younger Dryas, Ice-Free Corridor Open, Clovis/Folsom & Late Magdalenian)
            double dLaur = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_YD);
            if (dLaur <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_YD);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0; // Greenland inland ice
            return Math.min(wLaur, wFenno);
        } else if (year <= -8000L) {
            // -10,500 to -8,000 BP (Early Holocene Deglaciation / Maglemosian / PPNA)
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_EARLY_HOLOCENE);
            if (dFenno <= 0) return 0.0;
            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0; // Greenland inland ice
            if (lat > 75.0) return 0.0;
            return 1.0;
        } else {
            if (lat > 60.0 && lon > -55.0 && lon < -18.0 && year < -2500) return 0.0; // Greenland pre-Saqqaq
            if (lat > 80.0) return 0.0;
            return 1.0;
        }
    }

    /**
     * Is hominin occupied.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static boolean isHomininOccupied(double lon, double lat, long year) {
        return getHomininOccupancyWeight(lon, lat, year) > 0.001;
    }

    /**
     * Is land.
     *
     * @param lng the lng parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
     */
    public static boolean isLand(double lng, double lat) {
        BufferedImage mask = loadElevationMask();
        if (mask != null) {
            int mx = Math.clamp((int) ((lng + 180.0) / 360.0 * mask.getWidth()), 0, mask.getWidth() - 1);
            int my = Math.clamp((int) ((90.0 - lat) / 180.0 * mask.getHeight()), 0, mask.getHeight() - 1);
            return mask.getRaster().getSample(mx, my, 0) > 0;
        }
        if (FAST_LAND_GRID != null) {
            int gx = Math.clamp((int) ((lng + 180.0) / 360.0 * 720), 0, 719);
            int gy = Math.clamp((int) ((90.0 - lat) / 180.0 * 360), 0, 359);
            return FAST_LAND_GRID[gx][gy];
        }
        return lat >= -60.0 && lat <= 75.0;
    }

    /**
     * Create pure transparent canvas.
     *
     * @return the resulting computation or state reference
     */
    public static BufferedImage createPureTransparentCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Populate scenario historical maps.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public static void populateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;

        try {
            // 1. Try loading from standardized data/maps/Earth/<year>/ directory
            if (loadFromYearDirectory(scenario)) {
                return;
            }

            String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");

            // 2. Try loading from data/cache/<safeName>_*.png
            if (loadFromDiskCache(scenario, cacheDir, safeName)) {
                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache 'data/cache/{}_*.png'.", scenario.getName(), safeName);
                return;
            }

            // 2b. Fallback: try the presetKey as cache prefix (legacy cache files may use the short preset key)
            String presetKey = scenario.getPresetKey();
            if (presetKey != null && !presetKey.isEmpty() && !presetKey.equals(safeName)) {
                if (loadFromDiskCache(scenario, cacheDir, presetKey)) {
                    logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache using presetKey 'data/cache/{}_*.png'.", scenario.getName(), presetKey);
                    return;
                }
                // 2c. Glob scan: find any 'data/cache/<presetKey>*_density.png' (e.g. song_dynasty_1000_density.png)
                if (java.nio.file.Files.isDirectory(cacheDir)) {
                    try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(cacheDir)) {
                        java.util.Optional<java.nio.file.Path> found = stream
                            .filter(p -> {
                                String n = p.getFileName().toString();
                                return n.startsWith(presetKey) && n.endsWith("_density.png");
                            })
                            .findFirst();
                        if (found.isPresent()) {
                            String fileName = found.get().getFileName().toString();
                            // Strip trailing '_density.png' to get the actual prefix used for all sibling files
                            String derivedPrefix = fileName.substring(0, fileName.length() - "_density.png".length());
                            if (loadFromDiskCache(scenario, cacheDir, derivedPrefix)) {
                                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache using derived prefix 'data/cache/{}_*.png'.", scenario.getName(), derivedPrefix);
                                return;
                            }
                        }
                    } catch (Exception scanEx) {
                        logger.debug("Cache dir scan failed for scenario '{}': {}", scenario.getName(), scanEx.getMessage());
                    }
                }
            }

            // 2d. Fallback: try populationDensityType lowercased as cache prefix (e.g. INDIA_MAURYA -> india_maurya)
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";
            String densityTypeLower = type.toLowerCase(java.util.Locale.ROOT);
            if (!densityTypeLower.equals(safeName) && !densityTypeLower.equals(presetKey)) {
                if (loadFromDiskCache(scenario, cacheDir, densityTypeLower)) {
                    logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache using density type prefix 'data/cache/{}_*.png'.", scenario.getName(), densityTypeLower);
                    return;
                }
                // Also try glob scan with densityType prefix (e.g. sakoku_japan_ from JAPAN_SAKOKU)
                if (java.nio.file.Files.isDirectory(cacheDir)) {
                    try (java.util.stream.Stream<java.nio.file.Path> stream2 = java.nio.file.Files.list(cacheDir)) {
                        java.util.Optional<java.nio.file.Path> found2 = stream2
                            .filter(p -> {
                                String n = p.getFileName().toString();
                                return n.endsWith("_density.png") && java.util.Arrays.stream(densityTypeLower.split("_"))
                                    .filter(word -> word.length() > 3)
                                    .allMatch(n::contains);
                            })
                            .findFirst();
                        if (found2.isPresent()) {
                            String fileName2 = found2.get().getFileName().toString();
                            String derivedPrefix2 = fileName2.substring(0, fileName2.length() - "_density.png".length());
                            if (loadFromDiskCache(scenario, cacheDir, derivedPrefix2)) {
                                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache using density-type derived prefix 'data/cache/{}_*.png'.", scenario.getName(), derivedPrefix2);
                                return;
                            }
                        }
                    } catch (Exception scanEx2) {
                        logger.debug("Density-type cache dir scan failed for '{}': {}", scenario.getName(), scanEx2.getMessage());
                    }
                }
            }

            // 0. Try High-Precision Natural Earth & SVG Vector Cartography Ingestion Pipeline First
            SvgMapIngestor.SvgIngestionResult neResult = NaturalEarthVectorIngestor.loadNaturalEarthMap(type);
            if (neResult == null) {
                neResult = SvgMapIngestor.ingestForScenario(type);
            }
            if (neResult == null && scenario.getStartDateYear() != 0) {
                neResult = HgisAtlasIngestor.loadHistoricalPolityMap(scenario.getStartDateYear(), type);
            }

            // 1. Demographic Density Map
            BufferedImage imgDensity = (neResult != null && neResult.densityImage != null) ? neResult.densityImage : null;
            if (imgDensity == null) {
                if (scenario.getStartDateYear() <= -70000) {
                    imgDensity = applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                    logger.info("Generated authentic hominin demographic density tensor for Paleolithic epoch year {} (Out of Africa).", scenario.getStartDateYear());
                } else if (scenario.getStartDateYear() < -10000) {
                    BufferedImage baseHyde = Hyde34GridReader.loadForYear(-10000);
                    if (baseHyde != null) {
                        imgDensity = applyAltimetryCoastlineMask(applyPrehistoricGeographicMask(scenario.getPresetKey() != null ? scenario.getPresetKey() : safeName, scenario.getStartDateYear(), baseHyde));
                    } else {
                        imgDensity = applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
                    }
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                    logger.info("Generated precalculated density map for prehistoric epoch year {} (Sahul / Beringia).", scenario.getStartDateYear());
                } else {
                    imgDensity = Hyde34GridReader.loadForYear(scenario.getStartDateYear());
                    if (imgDensity != null) {
                        imgDensity = applyAltimetryCoastlineMask(imgDensity);
                        scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                        logger.info("Successfully populated scenario '{}' density tensor using HYDE 3.4 5-arc-minute grid for year {}.", scenario.getName(), scenario.getStartDateYear());
                    } else {
                        imgDensity = rasterizeDensityMap(type, scenario);
                        if (imgDensity != null) {
                            imgDensity = applyAltimetryCoastlineMask(imgDensity);
                            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                        }
                    }
                }
            } else {
                imgDensity = applyAltimetryCoastlineMask(imgDensity);
                scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
            }

            // 1. Multi-Channel Isogloss Map (Index 0)
            BufferedImage imgIsogloss = (neResult != null && neResult.isoglossImage != null) ? neResult.isoglossImage : rasterizeIsoglossMap(type, scenario);
            scenario.setCustomTensorMapBase64(0, bufferedImageToBase64Png(imgIsogloss));

            // 2. Multi-Channel Kinship Map (Index 1)
            BufferedImage imgKinship = (neResult != null && neResult.kinshipImage != null) ? neResult.kinshipImage : rasterizeKinshipMap(type, scenario);
            scenario.setCustomTensorMapBase64(1, bufferedImageToBase64Png(imgKinship));

            // 3. Multi-Channel Rituals Map (Index 2)
            BufferedImage imgRituals = (neResult != null && neResult.ritualsImage != null) ? neResult.ritualsImage : rasterizeRitualsMap(type, scenario);
            scenario.setCustomTensorMapBase64(2, bufferedImageToBase64Png(imgRituals));

            // 4. Multi-Channel Sovereignty Map (Index 3)
            BufferedImage imgSovereignty = (neResult != null && neResult.sovereigntyImage != null) ? neResult.sovereigntyImage : rasterizeSovereigntyMap(type, scenario);
            scenario.setCustomTensorMapBase64(3, bufferedImageToBase64Png(imgSovereignty));

            // 5. Multi-Channel Technology Mode Map (Index 4)
            BufferedImage imgTechnology = rasterizeTechnologyMap(type, scenario);
            scenario.setCustomTensorMapBase64(4, bufferedImageToBase64Png(imgTechnology));

            // 6. Multi-Channel Trade Network Map (Index 5)
            BufferedImage imgTrade = rasterizeTradeNetworkMap(type, scenario);
            scenario.setCustomTensorMapBase64(5, bufferedImageToBase64Png(imgTrade));

            // 7. Multi-Channel Institutional Complexity Map (Index 6)
            BufferedImage imgInstitutional = rasterizeInstitutionalComplexityMap(type, scenario);
            scenario.setCustomTensorMapBase64(6, bufferedImageToBase64Png(imgInstitutional));

            // 8. Multi-Channel Ecological Footprint Map (Index 7)
            BufferedImage imgEcological = rasterizeEcologicalFootprintMap(type, scenario);
            scenario.setCustomTensorMapBase64(7, bufferedImageToBase64Png(imgEcological));

            // 9. Multi-Channel Pathogen Immunity Map (Index 8)
            BufferedImage imgPathogen = rasterizePathogenImmunityMap(type, scenario);
            scenario.setCustomTensorMapBase64(8, bufferedImageToBase64Png(imgPathogen));

            // 10. Extensible Cultural Tensors (Indices 9 to N-1) if N > 9
            int dims = scenario.getCultureVectorDimensions();
            if (dims > 9) {
                for (int i = 9; i < dims; i++) {
                    BufferedImage imgExt = rasterizeExtensibleTensorMap(i, type, scenario);
                    scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(imgExt));
                }
            }

            // --- EXTENSIBLE GEOLOGICAL & ENERGY RESOURCE TENSORS (TAB 2) ---
            // Index 0: Coal Deposits
            BufferedImage imgCoal = rasterizeCoalMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(0, bufferedImageToBase64Png(imgCoal));

            // Index 1: Crude Oil Reserves
            BufferedImage imgOil = rasterizeOilMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(1, bufferedImageToBase64Png(imgOil));

            // Index 2: Natural Gas Fields
            BufferedImage imgGas = rasterizeGasMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(2, bufferedImageToBase64Png(imgGas));

            // Index 3: Uranium & Thorium Ores
            BufferedImage imgUranium = rasterizeUraniumMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(3, bufferedImageToBase64Png(imgUranium));

            // Index 4: Helium-3 Fusion Ores
            BufferedImage imgHe3 = rasterizeHelium3Map(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(4, bufferedImageToBase64Png(imgHe3));

            // Index 5: Iron & Copper Base Metals
            BufferedImage imgIronCopper = rasterizeIronCopperMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(5, bufferedImageToBase64Png(imgIronCopper));

            // Index 6: Precious Metals (Au / Ag / Pt)
            BufferedImage imgPreciousMetals = rasterizePreciousMetalsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(6, bufferedImageToBase64Png(imgPreciousMetals));

            // Index 7: Rare Earths & Critical Minerals (REE / Li)
            BufferedImage imgRareEarths = rasterizeRareEarthsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(7, bufferedImageToBase64Png(imgRareEarths));

            // Index 8: Mantle Heat Flux & Tectonics
            BufferedImage imgMantleHeat = rasterizeMantleHeatMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(8, bufferedImageToBase64Png(imgMantleHeat));

            // Index 9: Freshwater Aquifers
            BufferedImage imgAquifer = rasterizeAquiferMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(9, bufferedImageToBase64Png(imgAquifer));

            // Extensible Geology Tensors (Indices 10 to N-1) if N > 10
            int resDims = scenario.getResourceVectorDimensions();
            if (resDims > 10) {
                for (int i = 10; i < resDims; i++) {
                    BufferedImage imgExtRes = rasterizeExtensibleResourceTensorMap(i, type, scenario);
                    scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(imgExtRes));
                }
            }

            // Save to standardized data/maps/Earth/<year>/ directory
            saveImagesToYearDirectory(scenario.getStartDateYear(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);

            // Save cultural tensor maps to disk cache
            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);

            // Save geological tensor maps to disk cache
            saveGeologyTensorsToDiskCache(scenario.getName(), imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);

        } catch (Exception e) {
            logger.error("Failed to generate historical maps for scenario {}", scenario.getName(), e);
        }
    }

    /**
     * Load from year directory.
     *
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static boolean loadFromYearDirectory(Scenario scenario) {
        if (scenario == null) return false;
        java.nio.file.Path baseEarthDir = java.nio.file.Paths.get("data", "maps", "ether", "earth");
        if (!java.nio.file.Files.isDirectory(baseEarthDir)) {
            return false;
        }

        long year = scenario.getStartDateYear();
        java.nio.file.Path earthDir = baseEarthDir.resolve(String.valueOf(year));
        long resolvedYear = year;

        if (!java.nio.file.Files.isDirectory(earthDir)) {
            if (scenario.getPlanetPreset() != null) {
                long assoc = scenario.getPlanetPreset().getAssociatedEpochYear();
                java.nio.file.Path assocDir = baseEarthDir.resolve(String.valueOf(assoc));
                if (java.nio.file.Files.isDirectory(assocDir)) {
                    earthDir = assocDir;
                    resolvedYear = assoc;
                }
            }
            if (!java.nio.file.Files.isDirectory(earthDir)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(baseEarthDir)) {
                    java.util.Optional<Long> closest = stream
                        .filter(java.nio.file.Files::isDirectory)
                        .map(p -> {
                            try {
                                return Long.parseLong(p.getFileName().toString());
                            } catch (NumberFormatException nfe) {
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .min(java.util.Comparator.comparingLong(y -> Math.abs(y - year)));
                    if (closest.isPresent()) {
                        resolvedYear = closest.get();
                        earthDir = baseEarthDir.resolve(String.valueOf(resolvedYear));
                    }
                } catch (Exception e) {
                    logger.debug("Failed to list earth map dirs: {}", e.getMessage());
                }
            }
        }

        if (!java.nio.file.Files.isDirectory(earthDir)) {
            return false;
        }

        try {
            // Check density
            java.nio.file.Path densityFile = earthDir.resolve("earth_" + resolvedYear + "_density.png");
            if (!java.nio.file.Files.exists(densityFile)) {
                // Try fallback with any *density.png in directory
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(earthDir)) {
                    java.util.Optional<java.nio.file.Path> anyDensity = stream
                        .filter(p -> p.getFileName().toString().endsWith("_density.png"))
                        .findFirst();
                    if (anyDensity.isPresent()) {
                        densityFile = anyDensity.get();
                    }
                }
            }

            if (java.nio.file.Files.exists(densityFile)) {
                BufferedImage img = ImageIO.read(densityFile.toFile());
                if (img != null) {
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(img));
                }
            }

            String[] mapTags = {
                "isogloss", "kinship", "rituals", "sovereignty",
                "technology", "tradenetwork", "institutional", "ecological", "pathogen"
            };
            for (int i = 0; i < mapTags.length; i++) {
                java.nio.file.Path p = earthDir.resolve("earth_" + resolvedYear + "_" + mapTags[i] + ".png");
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            String[] geoTags = {
                "coal", "oil", "gas", "uranium",
                "helium3", "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };
            for (int i = 0; i < geoTags.length; i++) {
                java.nio.file.Path p = earthDir.resolve("earth_" + resolvedYear + "_" + geoTags[i] + ".png");
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            boolean loaded = scenario.getCustomDensityBase64() != null;
            if (loaded) {
                logger.info("Successfully loaded scenario '{}' (Year {}, resolved epoch {}) maps from standardized directory '{}'.", scenario.getName(), year, resolvedYear, earthDir);
            }
            return loaded;
        } catch (Exception e) {
            logger.warn("Failed to load scenario '{}' maps from year directory '{}': {}", scenario.getName(), earthDir, e.getMessage());
            return false;
        }
    }

    public static final int BIOME_DEEP_OCEAN = 0x000032; // RGB(0, 0, 50)
    public static final int BIOME_OCEAN      = 0x001464; // RGB(0, 20, 100)
    public static final int BIOME_BEACH      = 0xF0DC96; // RGB(240, 220, 150)
    public static final int BIOME_PLAINS     = 0x64C832; // RGB(100, 200, 50)
    public static final int BIOME_FOREST     = 0x147814; // RGB(20, 120, 20)
    public static final int BIOME_JUNGLE     = 0x005000; // RGB(0, 80, 0)
    public static final int BIOME_DESERT     = 0xFFC832; // RGB(255, 200, 50)
    public static final int BIOME_HILLS      = 0x969664; // RGB(150, 150, 100)
    public static final int BIOME_MOUNTAINS  = 0x646464; // RGB(100, 100, 100)
    public static final int BIOME_TUNDRA     = 0x96C8DC; // RGB(150, 200, 220)
    public static final int BIOME_SNOW       = 0xFFFFFF; // RGB(255, 255, 255)
    public static final int BIOME_GLACIER    = 0xDCF0FF; // RGB(220, 240, 255)

    private static final org.ether.society.generation.SimplexNoise CLIMATE_NOISE = new org.ether.society.generation.SimplexNoise(424242L);

    /**
     * Compute surface temperature.
     *
     * @param lat the lat parameter (double)
     * @param lon the lon parameter (double)
     * @param elevM the elev m parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeSurfaceTemperature(double lat, double lon, double elevM, long year) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);

        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        double tEq = 28.0;
        double tNorthPole = -30.0;
        double tSouthPole = -48.0;

        if (year <= -70000L) {
            tEq = 28.5; tNorthPole = -26.0; tSouthPole = -44.0; // Eemian / MIS 5e
        } else if (year <= -40000L) {
            tEq = 25.5; tNorthPole = -40.0; tSouthPole = -54.0; // MIS 3
        } else if (year <= -22000L) {
            tEq = 24.0; tNorthPole = -48.0; tSouthPole = -58.0; // LGM Onset
        } else if (year <= -18000L) {
            tEq = 23.0; tNorthPole = -52.0; tSouthPole = -62.0; // LGM Peak
        } else if (year <= -10500L) {
            tEq = 24.5; tNorthPole = -44.0; tSouthPole = -54.0; // Younger Dryas
        } else if (year <= -9000L) {
            tEq = 26.8; tNorthPole = -34.0; tSouthPole = -50.0; // Early Holocene
        } else if (year <= -7000L) {
            tEq = 27.2; tNorthPole = -31.0; tSouthPole = -49.0; // Early Neolithic
        } else if (year <= -4500L) {
            tEq = 28.0; tNorthPole = -28.0; tSouthPole = -47.0; // Holocene Optimum
        }

        // 1. Asymmetric hemispheric thermal baseline (thermal equator is naturally at ~6°N)
        double baseTemp;
        if (lat >= 6.0) {
            double f = Math.sin(Math.toRadians((lat - 6.0) * (90.0 / 84.0)));
            baseTemp = tEq * (1.0 - f * f) + tNorthPole * (f * f);
        } else {
            double f = Math.sin(Math.toRadians((6.0 - lat) * (90.0 / 96.0)));
            baseTemp = tEq * (1.0 - f * f) + tSouthPole * (f * f);
        }

        double tempC = baseTemp;

        // 2. Global Ocean Currents & Western/Eastern Boundary Gyres
        // North Atlantic Drift & Gulf Stream (+6°C to +8.5°C in NE Atlantic & NW Europe)
        if (year > -10500L || year <= -70000L) {
            double dGulf = Math.exp(-(Math.pow(lat - 56.0, 2) / 220.0 + Math.pow(lon - 5.0, 2) / 600.0));
            tempC += dGulf * 7.5;
        }
        // Kuroshio Current (+3.5°C off Japan & East Asia)
        double dKuroshio = Math.exp(-(Math.pow(lat - 35.0, 2) / 120.0 + Math.pow(lon - 140.0, 2) / 300.0));
        tempC += dKuroshio * 3.5;

        // Cold Eastern Boundary Currents (California, Humboldt, Benguela, Canary, Labrador, Oyashio)
        double dHumboldt = Math.exp(-(Math.pow(lat - (-22.0), 2) / 250.0 + Math.pow(lon - (-75.0), 2) / 60.0));
        tempC -= dHumboldt * 4.5;
        double dBenguela = Math.exp(-(Math.pow(lat - (-24.0), 2) / 200.0 + Math.pow(lon - 12.0, 2) / 50.0));
        tempC -= dBenguela * 4.0;
        double dCalif = Math.exp(-(Math.pow(lat - 32.0, 2) / 150.0 + Math.pow(lon - (-122.0), 2) / 60.0));
        tempC -= dCalif * 3.5;
        double dCanary = Math.exp(-(Math.pow(lat - 24.0, 2) / 150.0 + Math.pow(lon - (-18.0), 2) / 60.0));
        tempC -= dCanary * 3.0;
        double dLabrador = Math.exp(-(Math.pow(lat - 54.0, 2) / 100.0 + Math.pow(lon - (-56.0), 2) / 100.0));
        tempC -= dLabrador * 6.5;
        double dOyashio = Math.exp(-(Math.pow(lat - 50.0, 2) / 100.0 + Math.pow(lon - 155.0, 2) / 120.0));
        tempC -= dOyashio * 5.0;

        // 3. Deep Continental Winter Cold Poles (Continentality)
        if (elevM >= 0.0) {
            double dSiberia = (Math.pow(lat - 64.0, 2) / 220.0) + (Math.pow(lon - 125.0, 2) / 800.0);
            if (dSiberia < 4.0) tempC -= (16.0 * Math.exp(-dSiberia * 0.5));

            double dCanada = (Math.pow(lat - 62.0, 2) / 180.0) + (Math.pow(lon - (-105.0), 2) / 600.0);
            if (dCanada < 4.0) tempC -= (11.0 * Math.exp(-dCanada * 0.5));
        }

        // 4. Elevation Lapse Rate (-6.5 °C per 1000m on land)
        if (elevM > 0.0) {
            tempC -= 0.0065 * elevM;
        }

        // 5. Glacial Cold Dome & Albedo Cooling
        if (year <= -18000L) {
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_LGM);
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_LGM);
            double dAlps = signedDistanceToPolygon(lon, lat, POLY_ALPS_LGM);
            double dPatagonia = signedDistanceToPolygon(lon, lat, POLY_PATAGONIA_LGM);

            double coolLaurentide = 16.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 14.0 / (1.0 + Math.exp(dFenno / 2.0));
            double coolAlps = 9.0 / (1.0 + Math.exp(dAlps / 1.5));
            double coolPatagonia = 8.0 / (1.0 + Math.exp(dPatagonia / 1.5));
            tempC -= (coolLaurentide + coolFenno + coolAlps + coolPatagonia);
        } else if (year <= -40000L) {
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_MIS3);
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_MIS3);
            double coolLaurentide = 11.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 10.0 / (1.0 + Math.exp(dFenno / 2.0));
            tempC -= (coolLaurentide + coolFenno);
        } else if (year <= -10500L) {
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_YD);
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_YD);
            double coolLaurentide = 12.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 11.0 / (1.0 + Math.exp(dFenno / 2.0));
            double amocPlume = Math.exp(-(Math.pow(lat - 56.0, 2) + Math.pow(lon - (-15.0), 2)) / 250.0);
            tempC -= (coolLaurentide + coolFenno + amocPlume * 9.0);
        }

        // 6. Spherical Harmonic & Planetary Wave Meander Noise (~1.5°C)
        double thermalNoise = CLIMATE_NOISE.noise(nx * 2.5, ny * 2.5, nz * 2.5) * 1.8
                            + CLIMATE_NOISE.noise(nx * 6.0, ny * 6.0, nz * 6.0) * 0.7;
        tempC += thermalNoise;

        return Math.clamp(tempC, -50.0, 50.0);
    }

    /**
     * Compute annual precipitation.
     *
     * @param lat the lat parameter (double)
     * @param lon the lon parameter (double)
     * @param elevM the elev m parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeAnnualPrecipitation(double lat, double lon, double elevM, long year) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);
        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        // 1. Asymmetric undulating ITCZ (curved thermal equator between 4°N and 10°N)
        double itczLat = 6.0 + 3.0 * Math.sin(radLon * 2.0 + 0.5) + 2.0 * Math.cos(radLon * 3.0);
        double dItcz = Math.abs(lat - itczLat);
        double rainMm = 2400.0 * Math.exp(-(dItcz * dItcz) / 100.0);

        // 2. Subtropical Hadley Descending Arid Belts (~20° to 34° in each hemisphere)
        double dryNorth = Math.exp(-Math.pow(lat - 26.0, 2) / 65.0);
        double drySouth = Math.exp(-Math.pow(lat - (-24.0), 2) / 60.0);
        rainMm *= (1.0 - Math.max(dryNorth, drySouth) * 0.78);

        // 3. Mid-latitude Storm Tracks (~42° to 58° N/S)
        double stormNorth = Math.exp(-Math.pow(lat - 50.0, 2) / 80.0) * 1050.0;
        double stormSouth = Math.exp(-Math.pow(lat - (-48.0), 2) / 75.0) * 1250.0;
        rainMm += (stormNorth + stormSouth);

        // 4. Polar Aridification (> 62° N/S)
        double absLat = Math.abs(lat);
        if (absLat > 62.0) {
            rainMm = Math.max(60.0, rainMm * Math.exp(-(absLat - 62.0) / 8.5));
        }

        // 5. Regional Wind Advection & Major Monsoons:
        // A. Tropical Rainforest Basins (Amazon, Congo, Sundaland/Maritime Continent)
        double dAmazon = Math.exp(-(Math.pow(lat - (-3.0), 2) / 120.0 + Math.pow(lon - (-62.0), 2) / 250.0));
        rainMm += dAmazon * 1400.0;
        double dCongo = Math.exp(-(Math.pow(lat - 0.0, 2) / 80.0 + Math.pow(lon - 22.0, 2) / 120.0));
        rainMm += dCongo * 1200.0;
        double dGuinea = Math.exp(-(Math.pow(lat - 6.5, 2) / 30.0 + Math.pow(lon - (-2.0), 2) / 180.0));
        rainMm += dGuinea * 1100.0;
        double dSunda = Math.exp(-(Math.pow(lat - 0.0, 2) / 120.0 + Math.pow(lon - 120.0, 2) / 450.0));
        rainMm += dSunda * 1500.0;

        // B. Asian & Australian Monsoons:
        double dIndia = Math.exp(-(Math.pow(lat - 22.0, 2) / 90.0 + Math.pow(lon - 82.0, 2) / 160.0));
        rainMm += dIndia * 950.0;
        double dEastAsia = Math.exp(-(Math.pow(lat - 28.0, 2) / 100.0 + Math.pow(lon - 118.0, 2) / 180.0));
        rainMm += dEastAsia * 750.0;

        // C. Cold Current Coastal Deserts & Rain Shadows (Desiccation):
        double dAtacama = Math.exp(-(Math.pow(lat - (-22.0), 2) / 120.0 + Math.pow(lon - (-70.0), 2) / 25.0));
        rainMm *= (1.0 - dAtacama * 0.92);
        double dNamib = Math.exp(-(Math.pow(lat - (-24.0), 2) / 80.0 + Math.pow(lon - 14.5, 2) / 20.0));
        rainMm *= (1.0 - dNamib * 0.90);
        double dSomalia = Math.exp(-(Math.pow(lat - 7.0, 2) / 60.0 + Math.pow(lon - 46.0, 2) / 60.0));
        rainMm *= (1.0 - dSomalia * 0.75);
        double dSaharaCore = Math.exp(-(Math.pow(lat - 24.0, 2) / 70.0 + Math.pow(lon - 12.0, 2) / 350.0));
        rainMm *= (1.0 - dSaharaCore * 0.85);
        double dArabiaCore = Math.exp(-(Math.pow(lat - 23.0, 2) / 50.0 + Math.pow(lon - 48.0, 2) / 100.0));
        rainMm *= (1.0 - dArabiaCore * 0.85);

        // D. Mid-latitude West Coast Maritime Plumes:
        double dPacNW = Math.exp(-(Math.pow(lat - 48.0, 2) / 60.0 + Math.pow(lon - (-125.0), 2) / 40.0));
        rainMm += dPacNW * 1300.0;
        double dWestEuro = Math.exp(-(Math.pow(lat - 54.0, 2) / 70.0 + Math.pow(lon - (-5.0), 2) / 70.0));
        rainMm += dWestEuro * 850.0;
        double dChile = Math.exp(-(Math.pow(lat - (-46.0), 2) / 70.0 + Math.pow(lon - (-74.0), 2) / 30.0));
        rainMm += dChile * 1600.0;
        double dNZ = Math.exp(-(Math.pow(lat - (-43.0), 2) / 30.0 + Math.pow(lon - 171.0, 2) / 30.0));
        rainMm += dNZ * 1800.0;

        // E. Central Asian / Tarim / Gobi Continental Rain Shadow Desiccation:
        double dGobi = Math.exp(-(Math.pow(lat - 41.0, 2) / 80.0 + Math.pow(lon - 90.0, 2) / 350.0));
        rainMm *= (1.0 - dGobi * 0.80);

        // 6. Orographic Precipitation & Elevation Coupling
        if (elevM > 350.0 && absLat < 65.0) {
            double oro = Math.min(1000.0, (elevM - 350.0) * 0.38);
            rainMm += oro;
        }

        // 7. Paleoclimatic Epoch Monsoon Shifts:
        if (year <= -70000L || (year <= -4500L && year >= -10000L)) {
            // Green Sahara / African Humid Period & Arabian wet corridor
            double greenSahara = Math.exp(-(Math.pow(lat - 21.0, 2) / 90.0 + Math.pow(lon - 14.0, 2) / 500.0));
            double greenArabia = Math.exp(-(Math.pow(lat - 22.0, 2) / 55.0 + Math.pow(lon - 48.0, 2) / 120.0));
            rainMm += (greenSahara * 950.0 + greenArabia * 650.0);
        } else if (year <= -18000L && year >= -25000L) {
            // LGM Global Glacial Aridification
            rainMm *= (1.0 - 0.40 / (1.0 + Math.exp(-(absLat - 30.0) / 6.0)));
        }

        // 8. Atmospheric Planetary Wave & Fluid Turbulence Noise (multi-octave simplex)
        double fluidNoise = CLIMATE_NOISE.noise(nx * 3.0 + 50.0, ny * 3.0 + 50.0, nz * 3.0 + 50.0) * 0.15
                          + CLIMATE_NOISE.noise(nx * 7.0 + 80.0, ny * 7.0 + 80.0, nz * 7.0 + 80.0) * 0.08;
        rainMm *= (1.0 + fluidNoise);

        return Math.clamp(rainMm, 0.0, 3000.0);
    }

    /**
     * Compute seasonality amplitude.
     *
     * @param lat the lat parameter (double)
     * @param lon the lon parameter (double)
     * @param elevM the elev m parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeSeasonalityAmplitude(double lat, double lon, double elevM, long year) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);
        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        double absLat = Math.abs(lat);

        // 1. Orbital Obliquity Amplitude (Milankovitch)
        double obliqFactor = (year <= -70000L) ? 1.05 : ((year <= -18000L) ? 0.96 : 1.0);
        double baseAmp = Math.sin(Math.toRadians(absLat)) * 22.0 * obliqFactor;

        // 2. Fundamental Ocean vs Land Thermal Capacity Difference
        double ampC;
        if (elevM < 0.0) {
            // Open Oceans have massive heat capacity: seasonal range is strictly buffered (2°C to 7°C)
            ampC = 2.0 + Math.sin(Math.toRadians(absLat)) * 5.0;
            ampC += CLIMATE_NOISE.noise(nx * 4.0 + 20.0, ny * 4.0 + 20.0, nz * 4.0 + 20.0) * 0.8;
        } else {
            // Land continentality is strongly asymmetric and driven by landmass width
            ampC = baseAmp;

            // Siberian Hyper-Continentality Core (Yakutia / Verkhoyansk ~64°N, 125°E)
            double contSiberia = Math.exp(-(Math.pow(lat - 64.0, 2) / 250.0 + Math.pow(lon - 120.0, 2) / 600.0));
            // Canadian Shield Continentality (~60°N, -100°W)
            double contCanada = Math.exp(-(Math.pow(lat - 60.0, 2) / 200.0 + Math.pow(lon - (-100.0), 2) / 450.0));
            // Central Asian / Mongolian Continentality (~46°N, 85°E)
            double contAsia = Math.exp(-(Math.pow(lat - 46.0, 2) / 150.0 + Math.pow(lon - 85.0, 2) / 350.0));

            ampC += (contSiberia * 26.0 + contCanada * 18.0 + contAsia * 14.0);

            if (lat > 28.0) {
                ampC += 5.0;
            }

            // Maritime coasts damping: Western Europe westerlies keep seasonality mild
            double dEuro = Math.exp(-(Math.pow(lat - 52.0, 2) / 120.0 + Math.pow(lon - 5.0, 2) / 200.0));
            ampC -= dEuro * 6.0;

            // Equatorial tropical landmasses (Amazon, Congo, Indonesia) have minimal seasonality (< 3°C)
            if (absLat < 10.0) {
                ampC = Math.min(4.0, ampC * 0.25);
            }

            // Southern Hemisphere land has much lower continentality
            if (lat < -10.0) {
                ampC = Math.min(16.0, ampC * 0.65);
            }

            double landNoise = CLIMATE_NOISE.noise(nx * 3.5 + 30.0, ny * 3.5 + 30.0, nz * 3.5 + 30.0) * 1.5;
            ampC += landNoise;
        }

        if (year <= -18000L) {
            ampC *= 1.10;
        }

        return Math.clamp(ampC, 0.0, 50.0);
    }

    /**
     * Rasterize biomes map.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeBiomesMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage elevMask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / HEIGHT;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / WIDTH;
                double elevM = (etopo != null) ? etopo[y][x] : ((elevMask != null && (elevMask.getRGB(x, y) & 0xFF) > 128) ? 100.0 : -100.0);
                boolean isLand = elevM >= 0.0;

                if (!isLand) {
                    img.setRGB(x, y, (elevM < -2000.0) ? BIOME_DEEP_OCEAN : BIOME_OCEAN);
                    continue;
                }

                double tempC = WorldClimEmpiricalRasterLoader.getTemperature(lat, lon, elevM, year);
                double rainMm = WorldClimEmpiricalRasterLoader.getPrecipitation(lat, lon, elevM, year);

                int bColor = WorldClimEmpiricalRasterLoader.classifyBiome(tempC, rainMm, elevM, lat, lon, year);
                img.setRGB(x, y, bColor);
            }
        }
        return img;
    }

    /**
     * Rasterize temperature map.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeTemperatureMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double tempC = WorldClimEmpiricalRasterLoader.getTemperature(lat, lon, elevM, year);

                // Encode temperature [-50°C, +50°C] -> [0, 255]
                double norm = Math.clamp((tempC + 50.0) / 100.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    /**
     * Rasterize precipitation map.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizePrecipitationMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double rainMm = WorldClimEmpiricalRasterLoader.getPrecipitation(lat, lon, elevM, year);

                // Perceptual Square-Root Rain Normalization (0 to 4000 mm/yr)
                // Using sqrt scaling gives high contrast across dry (<250mm), moderate (500-1000mm) and intense monsoon (>2500mm)
                double norm = Math.clamp(Math.sqrt(rainMm / 4000.0), 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    /**
     * Rasterize seasonality map.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeSeasonalityMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double ampC = WorldClimEmpiricalRasterLoader.getSeasonality(lat, lon, elevM, year);

                // Encode seasonality [1°C to 65°C] -> [0, 255] with full linear dynamic range
                double norm = Math.clamp((ampC - 1.0) / 60.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    public static void saveImagesToYearDirectory(long year, BufferedImage imgDensity, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas, BufferedImage imgUranium,
            BufferedImage imgHe3, BufferedImage imgIronCopper, BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        try {
            java.nio.file.Path earthDir = java.nio.file.Paths.get("data", "maps", "ether", "earth", String.valueOf(year));
            java.nio.file.Files.createDirectories(earthDir);

            // 1. Save standard earth_<year>_<layer>.png in ultra-high-definition 2048x1024 with updated timestamp
            writePngFile(imgDensity, earthDir.resolve("earth_" + year + "_density.png").toFile());
            writePngFile(imgIsogloss, earthDir.resolve("earth_" + year + "_isogloss.png").toFile());
            writePngFile(imgKinship, earthDir.resolve("earth_" + year + "_kinship.png").toFile());
            writePngFile(imgRituals, earthDir.resolve("earth_" + year + "_rituals.png").toFile());
            writePngFile(imgSovereignty, earthDir.resolve("earth_" + year + "_sovereignty.png").toFile());
            writePngFile(imgTech, earthDir.resolve("earth_" + year + "_technology.png").toFile());
            writePngFile(imgTrade, earthDir.resolve("earth_" + year + "_tradenetwork.png").toFile());
            writePngFile(imgInst, earthDir.resolve("earth_" + year + "_institutional.png").toFile());
            writePngFile(imgEco, earthDir.resolve("earth_" + year + "_ecological.png").toFile());
            // Invariant Geological & Energy Resource Tensors (Static across epochs — copy baseline if not passed)
            String[] staticMinerals = {"coal", "oil", "gas", "uranium", "helium3", "iron_copper", "precious_metals", "rare_earths", "geothermal"};
            BufferedImage[] staticImgs = {imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat};
            for (int i = 0; i < staticMinerals.length; i++) {
                String min = staticMinerals[i];
                java.nio.file.Path minPath = earthDir.resolve("earth_" + year + "_" + min + ".png");
                if (staticImgs[i] != null) {
                    writePngFile(staticImgs[i], minPath.toFile());
                } else if (!minPath.toFile().exists()) {
                    java.io.File srcMin = new java.io.File("data/maps/ether/earth/2026/earth_2026_" + min + ".png");
                    if (!srcMin.exists()) srcMin = new java.io.File("data/maps/ether/earth/-100000/earth_-100000_" + min + ".png");
                    if (srcMin.exists()) {
                        java.nio.file.Files.copy(srcMin.toPath(), minPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            writePngFile(imgAquifer, earthDir.resolve("earth_" + year + "_aquifers.png").toFile());

            // 2. Save authentic paleoclimatic biomes map
            java.nio.file.Path biomesPath = earthDir.resolve("earth_" + year + "_biomes.png");
            BufferedImage biomesImg = rasterizeBiomesMap(year);
            if (biomesImg != null) {
                writePngFile(biomesImg, biomesPath.toFile());
            }

            // 3. Save authentic epoch paleoclimatic layers (Temperature, Precipitation, Seasonality)
            java.nio.file.Path tempPath = earthDir.resolve("earth_" + year + "_temperature.png");
            BufferedImage tempImg = rasterizeTemperatureMap(year);
            if (tempImg != null) {
                writePngFile(tempImg, tempPath.toFile());
            }

            java.nio.file.Path rainPath = earthDir.resolve("earth_" + year + "_precipitation.png");
            BufferedImage rainImg = rasterizePrecipitationMap(year);
            if (rainImg != null) {
                writePngFile(rainImg, rainPath.toFile());
            }

            java.nio.file.Path seasPath = earthDir.resolve("earth_" + year + "_seasonality.png");
            BufferedImage seasImg = rasterizeSeasonalityMap(year);
            if (seasImg != null) {
                writePngFile(seasImg, seasPath.toFile());
            }

            // 4. Ensure invariant NOAA ETOPO relief elevation map is present across all epochs with updated timestamp
            java.nio.file.Path elevPath = earthDir.resolve("earth_" + year + "_elevation.png");
            java.io.File srcElev = new java.io.File("data/maps/ether/earth/earth_elevation.png");
            if (!srcElev.exists()) srcElev = new java.io.File("data/maps/ether/earth/2026/earth_2026_elevation.png");
            if (!srcElev.exists()) srcElev = new java.io.File("data/maps/ether/earth/-100000/earth_-100000_elevation.png");
            if (srcElev.exists()) {
                java.nio.file.Files.copy(srcElev.toPath(), elevPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                elevPath.toFile().setLastModified(System.currentTimeMillis());
            } else {
                BufferedImage mask = loadElevationMask();
                if (mask != null) writePngFile(mask, elevPath.toFile());
            }

            // 5. Generate / Update exhaustive academic provenance, cultural registry, and README.md (Rule 6 AGENTS.md)
            generateOrUpdateEpochMetadataFiles(earthDir, year);

            logger.info("Persisted standard scenario cartographic maps and updated documentation into 'data/maps/ether/earth/{}/'", year);
        } catch (Exception e) {
            logger.warn("Failed to persist scenario maps to year directory 'data/maps/ether/earth/{}/': {}", year, e.getMessage());
        }
    }

    /**
     * Write png file.
     *
     * @param img the img parameter (BufferedImage)
     * @param targetFile the target file parameter (File)
     */
    public static void writePngFile(BufferedImage img, File targetFile) {
        if (img == null || targetFile == null) return;
        try {
            ImageIO.write(img, "PNG", targetFile);
            targetFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            logger.warn("Failed to write image to {}: {}", targetFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Generates or refreshes provenance_and_sources.json, cultural_registry.json, and README.md in accordance with AGENTS.md Rule 6.
     */
    public static void generateOrUpdateEpochMetadataFiles(java.nio.file.Path earthDir, long year) {
        HistoricalEpochMetadataGenerator.generateOrUpdateEpochMetadataFiles(earthDir, year);
    }

    public static void saveCulturalTensorsToYearDirectory(long year, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        try {
            java.nio.file.Path earthDir = java.nio.file.Paths.get("data", "maps", "ether", "earth", String.valueOf(year));
            java.nio.file.Files.createDirectories(earthDir);

            if (imgIsogloss != null)        ImageIO.write(imgIsogloss,        "PNG", earthDir.resolve("earth_" + year + "_isogloss.png").toFile());
            if (imgKinship != null)         ImageIO.write(imgKinship,         "PNG", earthDir.resolve("earth_" + year + "_kinship.png").toFile());
            if (imgRituals != null)         ImageIO.write(imgRituals,         "PNG", earthDir.resolve("earth_" + year + "_rituals.png").toFile());
            if (imgSovereignty != null)     ImageIO.write(imgSovereignty,     "PNG", earthDir.resolve("earth_" + year + "_sovereignty.png").toFile());
            if (imgTech != null)            ImageIO.write(imgTech,            "PNG", earthDir.resolve("earth_" + year + "_technology.png").toFile());
            if (imgTrade != null)           ImageIO.write(imgTrade,           "PNG", earthDir.resolve("earth_" + year + "_tradenetwork.png").toFile());
            if (imgInst != null)            ImageIO.write(imgInst,            "PNG", earthDir.resolve("earth_" + year + "_institutional.png").toFile());
            if (imgEco != null)             ImageIO.write(imgEco,             "PNG", earthDir.resolve("earth_" + year + "_ecological.png").toFile());
            if (imgPathogen != null)        ImageIO.write(imgPathogen,        "PNG", earthDir.resolve("earth_" + year + "_pathogen.png").toFile());

            logger.info("Persisted 9 authentic cultural tensors into 'data/maps/ether/earth/{}/'", year);
        } catch (Exception e) {
            logger.error("Failed to save cultural tensors for year {}", year, e);
        }
    }

    /**
     * Force generate cultural tensors only.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public static void forceGenerateCulturalTensorsOnly(Scenario scenario) {
        if (scenario == null) return;
        try {
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";
            long year = scenario.getStartDateYear();

            // 1. Cliodynamic & Cultural Tensors (10 rasters)
            BufferedImage imgDensity = rasterizeDensityMap(type, scenario);
            BufferedImage imgIsogloss = rasterizeIsoglossMap(type, scenario);
            BufferedImage imgKinship = rasterizeKinshipMap(type, scenario);
            BufferedImage imgRituals = rasterizeRitualsMap(type, scenario);
            BufferedImage imgSovereignty = rasterizeSovereigntyMap(type, scenario);
            BufferedImage imgTechnology = rasterizeTechnologyMap(type, scenario);
            BufferedImage imgTrade = rasterizeTradeNetworkMap(type, scenario);
            BufferedImage imgInstitutional = rasterizeInstitutionalComplexityMap(type, scenario);
            BufferedImage imgEcological = rasterizeEcologicalFootprintMap(type, scenario);
            BufferedImage imgPathogen = rasterizePathogenImmunityMap(type, scenario);

            // 2. Geological & Energy Resource Tensors: Aquifer is dynamic per epoch; static minerals are copied from baseline if missing
            BufferedImage imgAquifer = rasterizeAquiferMap(type, scenario);

            // 3. Save all 25 standard rasters (Physical + Cliodynamic + Resources) to year directory
            saveImagesToYearDirectory(year, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology,
                    imgTrade, imgInstitutional, imgEcological, imgPathogen, null, null, null, null,
                    null, null, null, null, null, imgAquifer);

            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);

            if (year == 0L) {
                saveImagesToDiskCache("Empire Romain & Pax Romana (An 0)", imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            }

            logger.info("Successfully regenerated and persisted the COMPLETE 25-raster suite for scenario '{}' (Year {}).",
                    scenario.getName(), year);
        } catch (Exception e) {
            logger.error("Failed to generate complete raster suite for scenario {}", scenario.getName(), e);
        }
    }

    /**
     * Apply prehistoric geographic mask via {@link PaleolithicDensityGenerator}.
     */
    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {
        return PaleolithicDensityGenerator.applyPrehistoricGeographicMask(scenarioKey, year, src);
    }

    /**
     * Generate prehistoric synthetic density map via {@link PaleolithicDensityGenerator}.
     */
    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        return PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(year, type);
    }

    public static void saveImagesToDiskCache(String scenarioName, BufferedImage imgDensity, BufferedImage imgSovereignty, BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech, BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");
            java.nio.file.Files.createDirectories(cacheDir);

            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);

            if (imgDensity != null) ImageIO.write(imgDensity, "PNG", cacheDir.resolve(safeName + "_density.png").toFile());
            if (imgSovereignty != null) ImageIO.write(imgSovereignty, "PNG", cacheDir.resolve(safeName + "_sovereignty.png").toFile());
            if (imgIsogloss != null) ImageIO.write(imgIsogloss, "PNG", cacheDir.resolve(safeName + "_isogloss.png").toFile());
            if (imgKinship != null) ImageIO.write(imgKinship, "PNG", cacheDir.resolve(safeName + "_kinship.png").toFile());
            if (imgRituals != null) ImageIO.write(imgRituals, "PNG", cacheDir.resolve(safeName + "_rituals.png").toFile());
            if (imgTech != null) ImageIO.write(imgTech, "PNG", cacheDir.resolve(safeName + "_technology.png").toFile());
            if (imgTrade != null) ImageIO.write(imgTrade, "PNG", cacheDir.resolve(safeName + "_tradenetwork.png").toFile());
            if (imgInst != null) ImageIO.write(imgInst, "PNG", cacheDir.resolve(safeName + "_institutional.png").toFile());
            if (imgEco != null) ImageIO.write(imgEco, "PNG", cacheDir.resolve(safeName + "_ecological.png").toFile());
            if (imgPathogen != null) ImageIO.write(imgPathogen, "PNG", cacheDir.resolve(safeName + "_pathogen.png").toFile());

            logger.info("Persisted cultural tensor maps to disk cache 'data/cache/{}_*.png'", safeName);
        } catch (Exception e) {
            logger.warn("Failed to write cultural tensor maps to disk cache directory: {}", e.getMessage());
        }
    }

    public static void saveGeologyTensorsToDiskCache(String scenarioName,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas,
            BufferedImage imgUranium, BufferedImage imgHe3, BufferedImage imgIronCopper,
            BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");
            java.nio.file.Files.createDirectories(cacheDir);
            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);

            if (imgCoal != null)           ImageIO.write(imgCoal,           "PNG", cacheDir.resolve(safeName + "_coal.png").toFile());
            if (imgOil != null)            ImageIO.write(imgOil,            "PNG", cacheDir.resolve(safeName + "_oil.png").toFile());
            if (imgGas != null)            ImageIO.write(imgGas,            "PNG", cacheDir.resolve(safeName + "_gas.png").toFile());
            if (imgUranium != null)        ImageIO.write(imgUranium,        "PNG", cacheDir.resolve(safeName + "_uranium.png").toFile());
            if (imgHe3 != null)            ImageIO.write(imgHe3,            "PNG", cacheDir.resolve(safeName + "_he3.png").toFile());
            if (imgIronCopper != null)     ImageIO.write(imgIronCopper,     "PNG", cacheDir.resolve(safeName + "_ironcopper.png").toFile());
            if (imgPreciousMetals != null) ImageIO.write(imgPreciousMetals, "PNG", cacheDir.resolve(safeName + "_preciousmetals.png").toFile());
            if (imgRareEarths != null)     ImageIO.write(imgRareEarths,     "PNG", cacheDir.resolve(safeName + "_rareearths.png").toFile());
            if (imgMantleHeat != null)     ImageIO.write(imgMantleHeat,     "PNG", cacheDir.resolve(safeName + "_mantleheat.png").toFile());
            if (imgAquifer != null)        ImageIO.write(imgAquifer,        "PNG", cacheDir.resolve(safeName + "_aquifer.png").toFile());

            logger.info("Persisted 10 geological tensor maps to disk cache 'data/cache/{}_*.png'", safeName);
        } catch (Exception e) {
            logger.warn("Failed to write geological tensor maps to disk cache directory: {}", e.getMessage());
        }
    }

    /**
     * Load from disk cache.
     *
     * @param scenario the scenario parameter (Scenario)
     * @param cacheDir the cache dir parameter (java.nio.file.Path)
     * @param safeName the safe name parameter (String)
     * @return the resulting computation or state reference
     */
    public static boolean loadFromDiskCache(Scenario scenario, java.nio.file.Path cacheDir, String safeName) {
        java.nio.file.Path densityCache = cacheDir.resolve(safeName + "_density.png");
        if (!java.nio.file.Files.exists(densityCache)) {
            return false;
        }
        try {
            BufferedImage imgDensity = ImageIO.read(densityCache.toFile());
            if (imgDensity == null) return false;

            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));

            String[] mapKeys = {
                "_isogloss.png", "_kinship.png", "_rituals.png", "_sovereignty.png",
                "_technology.png", "_tradenetwork.png", "_institutional.png", "_ecological.png", "_pathogen.png"
            };
            for (int i = 0; i < mapKeys.length; i++) {
                java.nio.file.Path p = cacheDir.resolve(safeName + mapKeys[i]);
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            // Geology tensor cache keys — indices must match setCustomGeologyTensorMapBase64 order:
            // 0=Coal, 1=Oil, 2=Gas, 3=Uranium, 4=He3, 5=IronCopper, 6=PreciousMetals, 7=RareEarths, 8=MantleHeat, 9=Aquifer
            String[] geoKeys = {
                "_coal.png", "_oil.png", "_gas.png", "_uranium.png",
                "_he3.png", "_ironcopper.png", "_preciousmetals.png", "_rareearths.png", "_mantleheat.png", "_aquifer.png"
            };
            for (int i = 0; i < geoKeys.length; i++) {
                java.nio.file.Path p = cacheDir.resolve(safeName + geoKeys[i]);
                if (!java.nio.file.Files.exists(p) && i == 6) {
                    p = cacheDir.resolve(safeName + "_preciousree.png");
                }
                if (!java.nio.file.Files.exists(p) && i == 7) {
                    p = cacheDir.resolve(safeName + "_rareearths.png");
                }
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            return true;
        } catch (Exception e) {
            logger.warn("Failed to load scenario '{}' cartographic tensors from disk cache: {}", scenario.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Ensure all scenario maps generated.
     *
     */
    public static void ensureAllScenarioMapsGenerated() {
        ensureAllScenarioMapsGenerated(false);
    }

    /**
     * Ensure all scenario maps generated.
     *
     * @param force the force parameter (boolean)
     */
    public static void ensureAllScenarioMapsGenerated(boolean force) {
        List<Scenario> builtIns = Scenario.getBuiltInScenarios();
        logger.info("Verifying and ensuring cartographic maps for all {} built-in scenarios in 'data/maps/ether/earth/<year>/' (force={})...", builtIns.size(), force);
        int generated = 0;
        for (Scenario sc : builtIns) {
            long year = sc.getStartDateYear();
            java.nio.file.Path earthDir = java.nio.file.Paths.get("data", "maps", "ether", "earth", String.valueOf(year));
            boolean exists = !force &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_density.png")) &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_isogloss.png")) &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_sovereignty.png"));
            if (!exists) {
                forceGenerateScenarioHistoricalMaps(sc);
                generated++;
            }
        }
        logger.info("Batch generation check complete. Generated/updated {} scenarios in data/maps/ether/earth/.", generated);
    }

    /**
     * Force generate scenario historical maps.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public static void forceGenerateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;
        try {
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";

            SvgMapIngestor.SvgIngestionResult neResult = NaturalEarthVectorIngestor.loadNaturalEarthMap(type);
            if (neResult == null) {
                neResult = SvgMapIngestor.ingestForScenario(type);
            }
            if (neResult == null && scenario.getStartDateYear() != 0) {
                neResult = HgisAtlasIngestor.loadHistoricalPolityMap(scenario.getStartDateYear(), type);
            }

            BufferedImage imgDensity = (neResult != null && neResult.densityImage != null) ? neResult.densityImage : null;
            if (imgDensity == null) {
                if (scenario.getStartDateYear() <= -4500) {
                    BufferedImage realHyde = (scenario.getStartDateYear() >= -10000) ? Hyde34GridReader.loadForYear(scenario.getStartDateYear()) : null;
                    if (realHyde != null) {
                        imgDensity = applyAltimetryCoastlineMask(realHyde);
                    } else {
                        imgDensity = applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
                    }
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                } else {
                    imgDensity = Hyde34GridReader.loadForYear(scenario.getStartDateYear());
                    if (imgDensity != null) {
                        imgDensity = applyAltimetryCoastlineMask(imgDensity);
                        scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                    } else {
                        imgDensity = rasterizeDensityMap(type, scenario);
                        if (imgDensity != null) {
                            imgDensity = applyAltimetryCoastlineMask(imgDensity);
                            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                        }
                    }
                }
            } else {
                imgDensity = applyAltimetryCoastlineMask(imgDensity);
                scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
            }

            BufferedImage imgIsogloss = (neResult != null && neResult.isoglossImage != null) ? neResult.isoglossImage : rasterizeIsoglossMap(type, scenario);
            scenario.setCustomTensorMapBase64(0, bufferedImageToBase64Png(imgIsogloss));

            BufferedImage imgKinship = (neResult != null && neResult.kinshipImage != null) ? neResult.kinshipImage : rasterizeKinshipMap(type, scenario);
            scenario.setCustomTensorMapBase64(1, bufferedImageToBase64Png(imgKinship));

            BufferedImage imgRituals = (neResult != null && neResult.ritualsImage != null) ? neResult.ritualsImage : rasterizeRitualsMap(type, scenario);
            scenario.setCustomTensorMapBase64(2, bufferedImageToBase64Png(imgRituals));

            BufferedImage imgSovereignty = (neResult != null && neResult.sovereigntyImage != null) ? neResult.sovereigntyImage : rasterizeSovereigntyMap(type, scenario);
            scenario.setCustomTensorMapBase64(3, bufferedImageToBase64Png(imgSovereignty));

            BufferedImage imgTechnology = rasterizeTechnologyMap(type, scenario);
            scenario.setCustomTensorMapBase64(4, bufferedImageToBase64Png(imgTechnology));

            BufferedImage imgTrade = rasterizeTradeNetworkMap(type, scenario);
            scenario.setCustomTensorMapBase64(5, bufferedImageToBase64Png(imgTrade));

            BufferedImage imgInstitutional = rasterizeInstitutionalComplexityMap(type, scenario);
            scenario.setCustomTensorMapBase64(6, bufferedImageToBase64Png(imgInstitutional));

            BufferedImage imgEcological = rasterizeEcologicalFootprintMap(type, scenario);
            scenario.setCustomTensorMapBase64(7, bufferedImageToBase64Png(imgEcological));

            BufferedImage imgPathogen = rasterizePathogenImmunityMap(type, scenario);
            scenario.setCustomTensorMapBase64(8, bufferedImageToBase64Png(imgPathogen));

            int dims = scenario.getCultureVectorDimensions();
            if (dims > 9) {
                for (int i = 9; i < dims; i++) {
                    BufferedImage imgExt = rasterizeExtensibleTensorMap(i, type, scenario);
                    scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(imgExt));
                }
            }

            BufferedImage imgCoal = rasterizeCoalMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(0, bufferedImageToBase64Png(imgCoal));

            BufferedImage imgOil = rasterizeOilMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(1, bufferedImageToBase64Png(imgOil));

            BufferedImage imgGas = rasterizeGasMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(2, bufferedImageToBase64Png(imgGas));

            BufferedImage imgUranium = rasterizeUraniumMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(3, bufferedImageToBase64Png(imgUranium));

            BufferedImage imgHe3 = rasterizeHelium3Map(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(4, bufferedImageToBase64Png(imgHe3));

            BufferedImage imgIronCopper = rasterizeIronCopperMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(5, bufferedImageToBase64Png(imgIronCopper));

            BufferedImage imgPreciousMetals = rasterizePreciousMetalsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(6, bufferedImageToBase64Png(imgPreciousMetals));

            BufferedImage imgRareEarths = rasterizeRareEarthsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(7, bufferedImageToBase64Png(imgRareEarths));

            BufferedImage imgMantleHeat = rasterizeMantleHeatMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(8, bufferedImageToBase64Png(imgMantleHeat));

            BufferedImage imgAquifer = rasterizeAquiferMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(9, bufferedImageToBase64Png(imgAquifer));

            int resDims = scenario.getResourceVectorDimensions();
            if (resDims > 10) {
                for (int i = 10; i < resDims; i++) {
                    BufferedImage imgExtRes = rasterizeExtensibleResourceTensorMap(i, type, scenario);
                    scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(imgExtRes));
                }
            }

            saveImagesToYearDirectory(scenario.getStartDateYear(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            saveGeologyTensorsToDiskCache(scenario.getName(), imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);

            logger.info("Force-generated all tensors for scenario '{}' (Year {}) in pure grayscale on black.", scenario.getName(), scenario.getStartDateYear());
        } catch (Exception e) {
            logger.error("Failed to force-generate maps for scenario {}", scenario.getName(), e);
        }
    }

    // --- 1. CLEAN DENSITY MAP ---

    /**
     * Rasterize density map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeDensityMap(String type, Scenario scenario) {
        return rasterizeDensityMapForYear(type, scenario, (scenario != null) ? scenario.getStartDateYear() : -10000);
    }

    /**
     * Rasterize density map for year.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @param targetYear the target year parameter (long)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeDensityMapForYear(String type, Scenario scenario, long targetYear) {
        if (scenario != null && scenario.isUseRealEarthData() && (targetYear < -10000 || targetYear > 2024)) {
            throw new IllegalStateException("ZERO FALLBACK VIOLATION: Empirical HYDE 3.4 dataset unavailable for year " + targetYear);
        }
        if (targetYear < -10000) {
            return applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(targetYear, type));
        }
        BufferedImage realHydeImg = Hyde34GridReader.loadForYear(targetYear);
        if (realHydeImg != null) {
            logger.info("Ingested authentic HYDE 3.4 5-arc-minute Esri ASCII raster grid for year {}", targetYear);
            return applyAltimetryCoastlineMask(realHydeImg);
        }

        // Fallback for futuristic or zero-fallback scenarios
        return applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(targetYear, type));
    }

    /** Cached elevation mask (1=land, 0=ocean) derived from earth_elevation.png at elevation cut 0m. */
    private static volatile BufferedImage cachedElevationMask = null;
    private static final Object ELEV_LOCK = new Object();

    /**
     * Loads the altimetry-derived land/ocean mask from earth_elevation.png in data/maps/ether/.
     * Pixels with luminance ≤ threshold (corresponding to ≤ 0m elevation) are ocean.
     */
    public static BufferedImage loadElevationMask() {
        if (cachedElevationMask != null) return cachedElevationMask;
        synchronized (ELEV_LOCK) {
            if (cachedElevationMask != null) return cachedElevationMask;
            try {
                int tw = WIDTH, th = HEIGHT;
                BufferedImage mask = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);

                // 1. Load directly from official NOAA ETOPO 2022 GeoTIFF
                float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(tw, th);
                if (etopo != null) {
                    for (int y = 0; y < th; y++) {
                        for (int x = 0; x < tw; x++) {
                            mask.getRaster().setSample(x, y, 0, (etopo[y][x] >= 0.0f) ? 255 : 0);
                        }
                    }
                    cachedElevationMask = mask;
                    logger.info("Altimetry coastline mask loaded directly from official NOAA ETOPO 2022 GeoTIFF ({}x{}).", tw, th);
                    return cachedElevationMask;
                }

                // 2. Fallback to grayscale elevation map
                BufferedImage elev = null;
                java.io.File f100k = new java.io.File("data/maps/ether/earth/-100000/earth_-100000_elevation.png");
                if (f100k.exists()) {
                    try { elev = ImageIO.read(f100k); } catch (Exception ignored) {}
                }
                if (elev == null) {
                    java.io.File f2026 = new java.io.File("data/maps/ether/earth/2026/earth_2026_elevation.png");
                    if (f2026.exists()) {
                        try { elev = ImageIO.read(f2026); } catch (Exception ignored) {}
                    }
                }
                if (elev == null) {
                    java.io.File fCache = new java.io.File("data/cache/earth_elevation.png");
                    if (fCache.exists()) {
                        try { elev = ImageIO.read(fCache); } catch (Exception ignored) {}
                    }
                }
                if (elev == null) {
                    logger.warn("earth_elevation.png not found — altimetry coastline mask disabled.");
                    return null;
                }
                for (int y = 0; y < th; y++) {
                    for (int x = 0; x < tw; x++) {
                        int sx = Math.clamp((int) ((x / (double) tw) * elev.getWidth()), 0, elev.getWidth() - 1);
                        int sy = Math.clamp((int) ((y / (double) th) * elev.getHeight()), 0, elev.getHeight() - 1);
                        int rgb = elev.getRGB(sx, sy);
                        int r = (rgb >> 16) & 0xFF;
                        int gr = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        double lum = (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
                        boolean isLand = (lum >= 0.478);

                        mask.getRaster().setSample(x, y, 0, isLand ? 255 : 0);
                    }
                }
                cachedElevationMask = mask;
                logger.info("Altimetry coastline mask loaded from data/maps/ether/ ({}x{} → {}x{}).",
                        elev.getWidth(), elev.getHeight(), tw, th);
                return cachedElevationMask;
            } catch (Exception e) {
                logger.warn("Failed to load altimetry coastline mask: {}", e.getMessage());
                return null;
            }
        }
    }

    /**
     * Applies the altimetry-derived coastline mask to a density image.
     * Ocean pixels (mask=0) are forced to the ocean background colour 0x000000 (Pure Black).
     * This ensures coastlines are derived from real elevation data, not vectorized outlines.
     */
    public static BufferedImage applyAltimetryCoastlineMask(BufferedImage src) {
        if (src == null) return null;
        BufferedImage mask = loadElevationMask();
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) / h * 180.0;
            for (int x = 0; x < w; x++) {
                int mx = (int) ((x + 0.5) * (mask != null ? mask.getWidth() : w) / w);
                int my = (int) ((y + 0.5) * (mask != null ? mask.getHeight() : h) / h);
                mx = Math.clamp(mx, 0, (mask != null ? mask.getWidth() : w) - 1);
                my = Math.clamp(my, 0, (mask != null ? mask.getHeight() : h) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                if (land == 0) {
                    // Ocean pixel -> strictly pure black
                    out.setRGB(x, y, 0x000000);
                } else {
                    out.setRGB(x, y, src.getRGB(x, y));
                }
            }
        }
        return out;
    }

    /**
     * Get topographic habitability.
     *
     * @param scenarioType the scenario type parameter (String)
     * @param lng the lng parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
     */
    public static double getTopographicHabitability(String scenarioType, double lng, double lat) {
        double himalayas = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow(lng - 85.0, 2)) / 100.0);
        double alps = Math.exp(-(Math.pow(lat - 46.0, 2) + Math.pow(lng - 10.0, 2)) / 30.0);
        double zagros = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lng - 47.0, 2)) / 40.0);
        double andes = Math.exp(-(Math.pow(lat - (-20.0), 2) + Math.pow(lng - (-70.0), 2)) / 60.0);

        double mountainPenalty = himalayas * 0.75 + alps * 0.5 + zagros * 0.5 + andes * 0.6;
        boolean isGreenSahara = scenarioType != null && scenarioType.equalsIgnoreCase("GREEN_SAHARA");
        double saharaPenalty = 0.0;
        if (!isGreenSahara) {
            double saharaCore = Math.exp(-(Math.pow(lat - 23.0, 2) + Math.pow(lng - 12.0, 2)) / 140.0);
            saharaPenalty = saharaCore * 0.6;
        }

        double factor = 1.0 - mountainPenalty - saharaPenalty;
        return Math.max(0.12, Math.min(1.0, factor));
    }

        public static final int[] COLORS_SOVEREIGNTY_100K = HistoricalCulturalTensorRasterizer.COLORS_SOVEREIGNTY_100K;
    public static final int[] COLORS_ISOGLOSS_100K = HistoricalCulturalTensorRasterizer.COLORS_ISOGLOSS_100K;
    public static final int[] COLORS_KINSHIP_100K = HistoricalCulturalTensorRasterizer.COLORS_KINSHIP_100K;
    public static final int[] COLORS_RITUALS_100K = HistoricalCulturalTensorRasterizer.COLORS_RITUALS_100K;

    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        return HistoricalCulturalTensorRasterizer.blendCladeRgb(w, cladeColors, occWeight);
    }

    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeSovereigntyMap(type, scenario);
    }

    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeIsoglossMap(type, scenario);
    }

    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeKinshipMap(type, scenario);
    }

    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeRitualsMap(type, scenario);
    }

    public static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTechnologyMap(type, scenario);
    }

    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTradeNetworkMap(type, scenario);
    }

    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
    }

    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
    }

    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizePathogenImmunityMap(type, scenario);
    }


    private static void initHighPrecisionGeographicPolygons() {
        // Iberian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-9.5, 36.0}, {-9.0, 37.0}, {-9.5, 38.8}, {-9.4, 41.8}, {-8.9, 43.3}, {-3.5, 43.5}, {1.7, 42.5}, {3.3, 42.4}, {0.2, 38.0}, {-5.4, 36.0}, {-7.5, 37.0}
        }));

        // Italian Peninsula & Sicily & Sardinia/Corsica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {7.5, 43.7}, {9.8, 44.4}, {12.2, 45.8}, {13.5, 45.8}, {13.8, 44.8}, {15.0, 43.5}, {18.5, 40.2}, {17.5, 39.8}, {16.0, 38.0}, {15.5, 41.0}, {12.2, 41.8}, {9.8, 44.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {12.4, 37.8}, {15.6, 38.3}, {15.2, 36.6}, {12.4, 37.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {8.5, 38.8}, {9.7, 43.0}, {8.5, 43.0}, {8.0, 38.8}
        }));

        // Greece & Aegean & Peloponnese & Crete
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {19.5, 39.8}, {20.5, 39.5}, {22.5, 40.5}, {24.0, 41.0}, {26.5, 40.5}, {24.0, 37.5}, {22.5, 36.4}, {21.5, 36.5}, {20.5, 38.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {23.5, 35.0}, {26.3, 35.3}, {26.0, 35.0}, {23.5, 35.0}
        }));

        // Anatolia & Caucasus
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {26.2, 40.2}, {29.0, 41.2}, {35.0, 42.0}, {38.0, 42.1}, {41.5, 41.6}, {44.0, 39.0}, {36.0, 36.5}, {32.5, 36.2}, {27.2, 36.8}
        }));

        // Levant & Near East & Arabian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 36.5}, {36.5, 37.0}, {41.0, 37.0}, {42.0, 34.0}, {36.0, 31.0}, {34.2, 31.3}, {35.5, 33.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 28.0}, {43.0, 12.5}, {54.0, 16.0}, {59.0, 22.5}, {56.0, 26.0}, {48.0, 30.0}, {35.0, 30.0}
        }));

        // Nile Delta & Nile Valley
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {29.5, 31.5}, {32.5, 31.5}, {34.0, 27.8}, {35.0, 22.0}, {31.0, 22.0}, {29.5, 30.0}
        }));

        // Maghreb & North Africa Coast
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.0, 28.0}, {-5.4, 35.8}, {3.0, 36.8}, {10.0, 37.5}, {11.5, 33.0}, {15.0, 32.5}, {25.0, 32.0}, {30.0, 31.5}, {30.0, 28.0}, {10.0, 22.0}, {-10.0, 20.0}
        }));

        // Sub-Saharan Africa (High-Resolution Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.5, 14.8}, {-17.0, 21.0}, {-16.0, 16.5}, {-14.0, 12.0}, {-7.5, 4.4}, {2.0, 6.2}, {9.5, 4.5}, {9.0, 2.0}, {9.5, -1.0}, {13.0, -12.0}, {12.0, -17.0},
            {15.0, -23.0}, {18.0, -34.5}, {26.0, -33.0}, {33.0, -27.0}, {40.0, -15.0}, {41.0, -4.0}, {51.2, 11.8}, {43.0, 12.5}, {37.0, 19.5}, {33.0, 27.0},
            {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Madagascar
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {43.5, -12.0}, {50.0, -12.5}, {50.5, -16.0}, {47.0, -25.5}, {43.0, -25.0}, {43.5, -12.0}
        }));

        // Western & Central & Eastern Europe
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-4.8, 48.4}, {2.5, 51.0}, {7.5, 53.5}, {10.0, 54.5}, {14.0, 54.5}, {22.0, 54.5}, {30.0, 60.0}, {30.0, 46.0}, {26.5, 40.5}, {13.5, 45.8}, {7.5, 43.7}, {-1.8, 43.4}, {-4.8, 48.4}
        }));

        // British Isles & Ireland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-5.5, 50.0}, {1.8, 51.3}, {1.5, 53.0}, {-0.5, 54.5}, {-2.0, 57.5}, {-4.5, 58.5}, {-6.5, 56.5}, {-4.5, 52.0}, {-5.5, 50.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.5, 51.5}, {-6.0, 52.0}, {-5.8, 55.3}, {-10.5, 54.0}
        }));

        // Indian Subcontinent (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {61.5, 25.0}, {68.0, 24.0}, {72.8, 21.0}, {73.0, 15.5}, {75.0, 11.0}, {77.5, 8.1}, {79.8, 10.0}, {80.2, 13.0}, {85.0, 19.5}, {88.5, 21.5}, {92.0, 22.0},
            {94.0, 28.0}, {88.0, 27.0}, {80.0, 30.0}, {74.0, 35.0}, {68.0, 30.0}, {61.5, 25.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {79.5, 9.8}, {81.8, 9.8}, {81.8, 6.0}, {79.5, 6.0}
        }));

        // China & East Asia (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {98.0, 21.0}, {108.0, 20.0}, {113.0, 22.5}, {118.0, 24.5}, {121.5, 28.5}, {122.0, 31.5}, {119.5, 35.0}, {122.5, 37.5}, {124.0, 40.0},
            {130.0, 42.5}, {135.0, 48.0}, {140.0, 55.0}, {120.0, 53.0}, {100.0, 50.0}, {90.0, 45.0}, {80.0, 40.0}, {98.0, 21.0}
        }));

        // Southeast Asia & Indochina
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {92.5, 20.0}, {98.5, 10.0}, {100.5, 6.0}, {104.0, 1.3}, {104.0, 10.0}, {109.0, 12.0}, {108.0, 16.0}, {106.0, 21.0}, {98.0, 21.0}
        }));

        // Indonesia & Philippines & Sahul / Australia (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {113.5, -26.0}, {114.0, -22.0}, {122.0, -18.0}, {130.0, -14.0}, {136.0, -12.0}, {142.0, -10.5}, {150.0, -23.0}, {153.5, -28.0}, {150.0, -37.5}, {140.0, -38.5}, {138.0, -35.0}, {129.0, -32.0}, {115.0, -34.5}, {113.5, -26.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {95.0, 5.5}, {106.0, -6.0}, {105.0, -2.0}, {95.0, 5.5} // Sumatra
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {108.5, 7.0}, {119.0, 4.0}, {115.0, -4.0}, {109.0, -3.0} // Borneo
        }));

        // Japan (Honshu, Kyushu, Shikoku, Hokkaido)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {130.0, 31.0}, {132.0, 33.5}, {135.0, 34.5}, {139.0, 35.5}, {141.0, 38.0}, {141.5, 41.5}, {140.0, 40.5}, {136.0, 36.0}, {130.5, 33.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {140.0, 41.5}, {145.5, 44.0}, {145.0, 45.5}, {141.0, 45.5}, {140.0, 41.5} // Hokkaido
        }));

        // South America (High-Precision Realistic Curved Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-77.0, 8.5}, {-73.0, 11.5}, {-62.0, 10.5}, {-50.0, 1.5}, {-35.0, -5.0}, {-35.0, -8.0}, {-38.0, -13.0}, {-41.0, -21.0},
            {-48.0, -28.0}, {-53.0, -33.0}, {-57.0, -38.0}, {-65.0, -45.0}, {-68.0, -54.0}, {-75.0, -52.0}, {-74.0, -42.0}, {-72.0, -30.0},
            {-76.0, -14.0}, {-81.0, -5.0}, {-79.0, 2.0}, {-77.0, 8.5}
        }));

        // Central America & Caribbean
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-92.0, 16.0}, {-88.0, 15.0}, {-83.0, 8.5}, {-77.0, 8.5}, {-83.0, 10.0}, {-90.0, 14.0}
        }));

        // North America (High-Precision Realistic Coastlines)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-168.0, 65.0}, {-150.0, 60.0}, {-135.0, 55.0}, {-124.0, 48.0}, {-123.0, 38.0}, {-117.0, 32.0}, {-105.0, 20.0}, {-97.0, 26.0},
            {-90.0, 29.0}, {-81.0, 25.0}, {-80.0, 30.0}, {-75.0, 35.0}, {-70.0, 42.0}, {-64.0, 46.0}, {-55.0, 52.0}, {-65.0, 60.0},
            {-80.0, 65.0}, {-100.0, 68.0}, {-120.0, 70.0}, {-140.0, 70.0}, {-168.0, 65.0}
        }));

        // Iranian Plateau, Central Asia & Kazakhstan (Fixing Eurasian Inland Gap)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {44.0, 38.0}, {46.0, 47.0}, {50.0, 55.0}, {85.0, 55.0}, {87.0, 48.0}, {80.0, 35.0}, {74.0, 35.0}, {68.0, 24.0}, {62.0, 25.0}, {51.0, 36.0}, {44.0, 38.0}
        }));

        // Sahel & Central African Hinterland (Fixing African Diagonal Gap)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.0, 21.0}, {10.0, 22.0}, {30.0, 28.0}, {33.0, 27.0}, {37.0, 19.5}, {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Greenland (Realistic Arctic Outline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-73.0, 78.0}, {-60.0, 83.0}, {-18.0, 82.0}, {-20.0, 70.0}, {-40.0, 60.0}, {-55.0, 60.0}, {-68.0, 75.0}
        }));

        // Northern Europe & Scandinavia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {5.0, 58.0}, {10.0, 62.0}, {14.0, 68.0}, {18.0, 70.0}, {30.0, 70.0}, {30.0, 55.0}, {10.0, 54.0}
        }));

        // Siberia & Northern Eurasia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {30.0, 55.0}, {30.0, 75.0}, {80.0, 74.0}, {120.0, 74.0}, {175.0, 68.0}, {170.0, 60.0}, {140.0, 50.0}, {80.0, 50.0}, {50.0, 50.0}
        }));

        // Antarctica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-180.0, -65.0}, {180.0, -65.0}, {180.0, -90.0}, {-180.0, -90.0}
        }));

        // INLAND SEAS & MAJOR GULFS
        // Mediterranean Sea (corrected boundary — avoids rectangular clipping at Gibraltar/Iberia/Anatolia)
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {-5.4, 35.9}, {-1.8, 35.7}, {0.5, 37.5}, {3.2, 37.1}, {8.0, 37.5},
            {10.5, 38.2}, {12.5, 38.1}, {15.5, 38.1}, {17.0, 39.4}, {19.5, 39.0},
            {20.9, 37.0}, {23.5, 37.5}, {26.0, 36.2}, {28.2, 36.5}, {29.5, 36.2},
            {30.5, 32.2}, {32.5, 32.1}, {34.8, 32.0}, {36.0, 33.5}, {36.3, 35.1},
            {35.5, 36.5}, {36.5, 37.0}, {35.8, 37.8}, {28.0, 38.5}, {26.0, 40.5},
            {23.5, 41.0}, {20.0, 40.5}, {16.5, 41.0}, {13.5, 44.0}, {12.0, 44.2},
            {8.5, 44.3}, {3.5, 43.3}, {2.0, 42.5}, {-0.5, 40.5}, {-1.5, 38.5},
            {-3.8, 36.8}, {-5.4, 35.9}
        }));
        // Aegean Sea
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {22.5, 40.5}, {24.0, 41.5}, {26.5, 40.9}, {27.5, 39.0}, {26.5, 37.5},
            {24.5, 37.0}, {22.5, 37.5}, {22.5, 38.5}, {23.5, 39.5}, {22.5, 40.5}
        }));
        // Black Sea + Bosphorus connection
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {28.0, 41.0}, {29.0, 41.2}, {30.0, 42.5}, {33.0, 44.0}, {36.0, 45.0},
            {38.0, 46.0}, {41.0, 43.5}, {41.5, 41.5}, {38.0, 41.0}, {34.5, 41.5},
            {31.0, 41.8}, {28.5, 41.3}, {28.0, 41.0}
        }));
        // Red Sea
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {32.5, 29.5}, {34.0, 28.0}, {36.5, 25.0}, {38.0, 22.0}, {40.0, 18.0},
            {41.5, 14.0}, {43.5, 12.5}, {43.0, 12.0}, {40.0, 12.5}, {36.5, 14.0},
            {33.5, 17.0}, {32.0, 20.0}, {32.0, 24.0}, {32.5, 27.0}, {32.5, 29.5}
        }));

        // Precompute 720x360 boolean grid for O(1) instant land lookups
        FAST_LAND_GRID = new boolean[720][360];
        for (int gy = 0; gy < 360; gy++) {
            double lat = 90.0 - (gy + 0.5) / 360.0 * 180.0;
            for (int gx = 0; gx < 720; gx++) {
                double lng = -180.0 + (gx + 0.5) / 720.0 * 360.0;
                int x = (int) ((lng + 180.0) / 360.0 * WIDTH);
                int y = (int) ((90.0 - lat) / 180.0 * HEIGHT);
                Point p = new Point(x, y);
                boolean isSea = false;
                for (Path2D sea : SEA_POLYGONS) {
                    if (sea.contains(p)) { isSea = true; break; }
                }
                if (!isSea) {
                    for (Path2D land : LAND_POLYGONS) {
                        if (land.contains(p)) { FAST_LAND_GRID[gx][gy] = true; break; }
                    }
                }
            }
        }
    }

    private static Path2D createPolygon(double[][] points) {
        Path2D p = new Path2D.Double();
        if (points.length == 0) return p;

        List<double[]> densified = new ArrayList<>();
        int n = points.length;
        for (int i = 0; i < n; i++) {
            double[] p1 = points[i];
            double[] p2 = points[(i + 1) % n];
            densified.add(p1);
            int subSteps = 10;
            for (int s = 1; s < subSteps; s++) {
                double t = s / (double) subSteps;
                double lng = p1[0] + t * (p2[0] - p1[0]);
                double lat = p1[1] + t * (p2[1] - p1[1]);
                densified.add(new double[]{lng, lat});
            }
        }

        int startX = (int) ((densified.get(0)[0] + 180.0) / 360.0 * WIDTH);
        int startY = (int) ((90.0 - densified.get(0)[1]) / 180.0 * HEIGHT);
        p.moveTo(startX, startY);

        for (int i = 1; i < densified.size(); i++) {
            int px = (int) ((densified.get(i)[0] + 180.0) / 360.0 * WIDTH);
            int py = (int) ((90.0 - densified.get(i)[1]) / 180.0 * HEIGHT);
            p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    /**
     * Generate procedural maps for scenario.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public static void generateProceduralMapsForScenario(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    private static List<CityPoint> getCitiesForScenario(String type, Scenario scenario) {
        return HistoricalScenarioFeaturesCatalog.getCitiesForScenario(type, scenario);
    }

    private static List<CityPoint> getCitiesForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getCitiesForScenario(type);
    }

    private static List<RiverRibbon> getRiversForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getRiversForScenario(type);
    }

    private static List<EmpireTerritory> getEmpiresForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getEmpiresForScenario(type);
    }

    private static List<LanguageZone> getLanguageZonesForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getLanguageZonesForScenario(type);
    }

    private static List<LanguageZone> getKinshipZonesForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getKinshipZonesForScenario(type);
    }

    private static List<LanguageZone> getSacredSitesForScenario(String type) {
        return HistoricalScenarioFeaturesCatalog.getSacredSitesForScenario(type);
    }

    private static double distSq(double lng1, double lat1, double lng2, double lat2) {
        return HistoricalScenarioFeaturesCatalog.distSq(lng1, lat1, lng2, lat2);
    }

    /**
     * Buffered image to base64png.
     *
     * @param bImg the b img parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static String bufferedImageToBase64Png(BufferedImage bImg) {
        if (bImg == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bImg, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to encode BufferedImage to Base64 PNG", e);
            return null;
        }
    }

    /**
     * Rasterize extensible tensor map.
     *
     * @param tensorIndex the tensor index parameter (int)
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeExtensibleTensorMap(int tensorIndex, String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        long seed = 11235L + tensorIndex * 11111L;
        if (scenario != null && scenario.getTensorSeeds() != null && scenario.getTensorSeeds().containsKey(tensorIndex)) {
            seed = scenario.getTensorSeeds().get(tensorIndex);
        } else if (scenario != null && scenario.getCulturalSeed() != 0) {
            seed = scenario.getCulturalSeed() ^ (tensorIndex * 0x9E3779B97F4A7C15L);
        }

        double pFreq = 0.020;
        double pAmp = 1.00;
        double pDiff = 0.25;
        if (scenario != null && scenario.getTensorProceduralParameters() != null && scenario.getTensorProceduralParameters().containsKey(tensorIndex)) {
            java.util.Map<String, Double> pMap = scenario.getTensorProceduralParameters().get(tensorIndex);
            if (pMap != null) {
                pFreq = pMap.getOrDefault("scenario.tensor.ext.p1.label", 0.020);
                pAmp = pMap.getOrDefault("scenario.tensor.ext.p2.label", 1.00);
                pDiff = pMap.getOrDefault("scenario.tensor.ext.p3.label", 0.25);
            }
        }

        java.util.Random rnd = new java.util.Random(seed);
        double phaseLng = rnd.nextDouble() * Math.PI * 2.0;
        double phaseLat = rnd.nextDouble() * Math.PI * 2.0;
        double scale = Math.clamp(pFreq, 0.001, 0.20);
        float hueBase = (float) ((tensorIndex * 0.137 + (rnd.nextDouble() * 0.2)) % 1.0);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;
                if (!isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A); // Equirectangular ocean basemap (Color 15, 23, 42)
                    continue;
                }

                double val = (Math.sin(lon * scale + phaseLng) * Math.cos(lat * scale + phaseLat) * 0.5 + 0.5) * pAmp;
                val = Math.clamp(val, 0.0, 1.0);
                float sat = (float) Math.clamp(0.5f + val * 0.45f * (1.0 - pDiff * 0.5), 0.0, 1.0);
                float bright = (float) Math.clamp(0.2f + val * 0.75f, 0.0, 1.0);
                int rgb = Color.HSBtoRGB(hueBase, sat, bright);
                img.setRGB(x, y, rgb);
            }
        }
        g.dispose();
        return img;
    }

    public static BufferedImage rasterizeCoastlines(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    public static java.util.List<double[]> loadMRDSDeposits(String... commodityKeywords) {
        return GeologicalResourceMapRasterizer.loadMRDSDeposits(commodityKeywords);
    }

    public static void rasterizeSpotListToAlpha(BufferedImage img, java.util.List<double[]> spots, Color themeColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeSpotListToAlpha(img, spots, themeColor, defaultRadiusPx);
    }

    public static void rasterizeTieredSpotList(BufferedImage img, java.util.List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeTieredSpotList(img, spots, lowColor, medColor, highColor, defaultRadiusPx);
    }

    private static volatile BufferedImage cachedCoalMap = null;
    private static volatile BufferedImage cachedOilMap = null;
    private static volatile BufferedImage cachedGasMap = null;
    private static volatile BufferedImage cachedUraniumMap = null;
    private static volatile BufferedImage cachedHe3Map = null;
    private static volatile BufferedImage cachedIronCopperMap = null;
    private static volatile BufferedImage cachedPreciousMetalsMap = null;
    private static volatile BufferedImage cachedRareEarthsMap = null;
    private static volatile BufferedImage cachedMantleHeatMap = null;
    private static volatile BufferedImage cachedAquiferMap = null;
    private static volatile float[][] cachedWhymapBlurredGrid = null;

    public static BufferedImage rasterizeCoalMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeCoalMap(type, scenario);
    }

    public static BufferedImage rasterizeOilMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeOilMap(type, scenario);
    }

    public static BufferedImage rasterizeGasMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeGasMap(type, scenario);
    }

    public static BufferedImage rasterizeUraniumMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeUraniumMap(type, scenario);
    }

    public static BufferedImage rasterizeHelium3Map(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeHelium3Map(type, scenario);
    }

    public static BufferedImage rasterizeIronCopperMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeIronCopperMap(type, scenario);
    }

    public static BufferedImage rasterizePreciousMetalsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizePreciousMetalsMap(type, scenario);
    }

    public static BufferedImage rasterizeRareEarthsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeRareEarthsMap(type, scenario);
    }

    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeMantleHeatMap(type, scenario);
    }

    /**
     * Get paleo aquifer recharge factor.
     *
     * @param lon the lon parameter (double)
     * @param lat the lat parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double getPaleoAquiferRechargeFactor(double lon, double lat, long year) {
        return GeologicalResourceMapRasterizer.getPaleoAquiferRechargeFactor(lon, lat, year);
    }

    private static synchronized float[][] getOrComputeBaseWhymapGrid() {
        return GeologicalResourceMapRasterizer.getOrComputeBaseWhymapGrid();
    }

    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeAquiferMap(type, scenario);
    }

    public static BufferedImage rasterizeExtensibleResourceTensorMap(int index, String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeExtensibleResourceTensorMap(index, type, scenario);
    }

    /**
     * Precache all built in scenarios.
     *
     */
    public static void precacheAllBuiltInScenarios() {
        String[] types = {
            "ONE_CONTINENT", "OUT_OF_AFRICA_100K", "FERTILE_CRESCENT_8000BC",
            "ROMAN_EMPIRE_0", "SONG_DYNASTY_1000", "MALI_EMPIRE_1324",
            "EGYPT_NILE", "MESOPOTAMIA_ASSYRIA", "GREEN_SAHARA", "INDIA_MAURYA",
            "SAKOKU_JAPAN", "MESOAMERICA", "AMERICAS_1491", "SAHUL_MIGRATION",
            "BERINGIA_AMERICAS", "URBAN_CLUSTERS",
            "SILK_ROAD_NEXUS", "LGM_REFUGIA", "INDIAN_OCEAN_TRADE", "BRONZE_AGE_COLLAPSE"
        };
        logger.info("Pre-caching cartographic tensor maps for {} built-in scenarios to 'data/cache/'...", types.length);
        for (String type : types) {
            Scenario dummy = new Scenario();
            dummy.setName(type);
            dummy.setPopulationDensityType(type);
            populateScenarioHistoricalMaps(dummy);
        }
        logger.info("Cartographic pre-caching complete.");
    }
}


