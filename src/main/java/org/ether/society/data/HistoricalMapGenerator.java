/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    public static final int WIDTH = 2048;
    public static final int HEIGHT = 1024;

    private static final List<Path2D> LAND_POLYGONS = new ArrayList<>();
    private static final List<Path2D> SEA_POLYGONS = new ArrayList<>();
    private static boolean[][] FAST_LAND_GRID = new boolean[720][360];

    static {
        initHighPrecisionGeographicPolygons();
    }

    // Archaeological Hominin Hearths for Organic Orographic Voronoi (-100,000 BP)
    private static final double[][] HEARTHS_SAPIENS_AFRICA = {
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

    private static final double[][] HEARTHS_SAPIENS_PIONEERS = {
        {35.3, 32.7},   // Skhul & Qafzeh (Mount Carmel / Southern Levant)
        {34.0, 29.5},   // Sinai Corridor
        {41.0, 28.0},   // Al-Wusta (Nefud Desert / Central Arabia)
        {50.0, 26.0},   // Eastern Arabian Coastal Oasis
        {45.5, 18.5},   // Mundafan (Rub' al Khali / Southern Arabia)
        {44.0, 13.0},   // Bab-el-Mandeb crossing (Yemen)
        {54.0, 17.0},   // Dhofar coastal oasis (Oman)
        {55.9, 25.1}    // Jebel Faya (UAE / Gulf of Oman)
    };

    private static final double[][] HEARTHS_NEANDERTHAL_WEST = {
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

    private static final double[][] HEARTHS_NEANDERTHAL_ZAGROS = {
        {30.6, 37.0},   // Karain Cave (Anatolia)
        {40.0, 44.0},   // Mezmaiskaya (Caucasus)
        {44.2, 36.8},   // Shanidar Cave (Zagros / Northern Iraq)
        {47.4, 34.4},   // Bisitun & Wezmeh (Western Iran)
        {51.5, 32.4},   // Qaleh Bozi (Central Iran / Isfahan)
        {53.3, 29.8},   // Arsanjan / Barm-e Shur (Southern Iran / Fars)
        {52.0, 36.0}    // Hotu & Kamarband (Alborz / Caspian)
    };

    private static final double[][] HEARTHS_DENISOVAN_ALTAI = {
        {84.7, 51.4},   // Denisova Cave (Altai Mountains)
        {67.0, 38.0},   // Teshik-Tash / Obi-Rakhmat (Uzbekistan / Central Asia)
        {95.0, 55.0},   // Ust'-Ishim / Krasnoyarsk (Siberia)
        {102.8, 35.2},  // Baishiya Karst Cave (Tibetan Plateau / Xiahe)
        {115.0, 40.0}   // Nihewan / Zhoukoudian (Northern China)
    };

    private static final double[][] HEARTHS_EASTERN_ARCHAIC = {
        {77.0, 22.0},   // Narmada Valley (Central India)
        {79.0, 13.0},   // Attirampakkam (Southern India)
        {103.0, 20.0},  // Tam Pa Ling (Indochina)
        {111.5, 25.5},  // Daoxian / Fuyan Cave (Southern China)
        {102.0, 2.0},   // Malayan Peninsula
        {111.0, -7.4}   // Solo River / Ngandong (Java / Sundaland)
    };

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

    public static int getHomininSpeciesType(double lon, double lat) {
        int entity = getHomininEntityId(lon, lat);
        return switch (entity) {
            case 1, 2 -> 1; // Homo Sapiens
            case 3, 4 -> 2; // Neanderthals
            case 5, 6 -> 3; // Denisovans / Archaic Asian Hominins
            default -> 0;   // Uninhabited
        };
    }

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
    private static final double[][] POLY_LAURENTIDE_LGM = {
        {-142.0, 60.0}, {-136.0, 58.0}, {-126.0, 53.0}, {-122.0, 49.0},
        {-114.0, 48.0}, {-104.0, 47.0}, {-96.0, 43.0}, {-90.0, 40.0},
        {-84.0, 39.5}, {-78.0, 41.0}, {-73.5, 41.0}, {-70.0, 42.0},
        {-64.0, 45.0}, {-52.0, 47.0}, {-53.0, 52.0}, {-58.0, 60.0},
        {-65.0, 70.0}, {-85.0, 75.0}, {-115.0, 75.0}, {-135.0, 70.0}
    };

    private static final double[][] POLY_FENNOSCANDIA_LGM = {
        {-11.0, 54.0}, {-8.0, 51.5}, {-2.0, 52.0}, {4.0, 52.5},
        {12.0, 52.5}, {18.0, 52.0}, {26.0, 54.0}, {34.0, 57.0},
        {42.0, 64.0}, {55.0, 70.0}, {45.0, 75.0}, {25.0, 77.0},
        {10.0, 75.0}, {0.0, 68.0}, {-6.0, 60.0}
    };

    private static final double[][] POLY_ALPS_LGM = {
        {5.5, 45.0}, {7.0, 46.0}, {10.0, 47.5}, {14.0, 47.0},
        {14.5, 46.0}, {11.0, 45.5}, {7.5, 45.0}
    };

    private static final double[][] POLY_BERINGIA_REFUGE = {
        {130.0, 71.0}, {145.0, 72.0}, {165.0, 70.0}, {-170.0, 68.0},
        {-160.0, 64.0}, {-145.0, 63.0}, {-135.0, 66.0}, {-138.0, 69.0},
        {-150.0, 71.5}, {-175.0, 72.0}, {160.0, 66.0}, {140.0, 67.0}
    };

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

    public static double getHomininOccupancyWeight(double lon, double lat, long year) {
        if (lat < -60.0) return 0.0; // Antarctica strictly uninhabited

        // High Arctic extreme (> 75°N)
        if (lat > 75.0) {
            double t = Math.clamp((lat - 75.0) / 5.0, 0.0, 1.0);
            return 1.0 - t * t * (3.0 - 2.0 * t);
        }

        // Remote oceanic islands (Madagascar before 500 AD, Iceland before 874 AD, NZ before 1280 AD)
        if (year < 500) {
            if (lat <= -10.0 && lat >= -28.0 && lon >= 42.0 && lon <= 52.0) return 0.0; // Madagascar
            if (lat <= -18.0 && lat >= -22.0 && lon >= 56.0 && lon <= 65.0) return 0.0; // Mascarenes
        }
        if (year < 874) {
            if (lat >= 63.0 && lat <= 67.0 && lon >= -25.0 && lon <= -12.0) return 0.0; // Iceland
        }
        if (year < 1280) {
            if (lat <= -34.0 && lat >= -48.0 && lon >= 165.0 && lon <= 180.0) return 0.0; // New Zealand
        }

        if (year <= -70000L) {
            // -100,000 BP (MIS 5c/5d)
            int species = getHomininSpeciesType(lon, lat);
            if (species == 0) return 0.0;
            double maxLat = (lon <= 40.0) ? 58.0 : (58.0 - Math.min(6.0, (lon - 40.0) / 15.0));
            double fadeStart = maxLat - 4.0;
            if (lat > fadeStart) {
                double t = Math.clamp((lat - fadeStart) / (maxLat - fadeStart), 0.0, 1.0);
                return 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return 1.0;
        } else if (year <= -40000L) {
            // -50,000 BP (MIS 3 / Sahul Colonization & IUP)
            // Americas strictly uninhabited: smooth Atlantic & Pacific barrier
            if (lon < -25.0 && lon > -170.0) {
                return 0.0;
            }
            // High latitude boreal fade
            if (lat > 52.0) {
                double t = Math.clamp((lat - 52.0) / 12.0, 0.0, 1.0);
                return 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return 1.0;
        } else if (year <= -18000L) {
            // -25,000 & -20,000 BP (Gravettian / LGM Peak / Beringian Standstill)
            // 1. Laurentide / Cordilleran Ice Sheet
            double dLaurentide = signedDistanceToPolygon(lon, lat, POLY_LAURENTIDE_LGM);
            double wLaurentide = 1.0 / (1.0 + Math.exp(-dLaurentide / 2.2));

            // 2. Fennoscandian / British Ice Sheet
            double dFenno = signedDistanceToPolygon(lon, lat, POLY_FENNOSCANDIA_LGM);
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 2.0));

            // 3. Alpine Ice Cap
            double dAlps = signedDistanceToPolygon(lon, lat, POLY_ALPS_LGM);
            double wAlps = 1.0 / (1.0 + Math.exp(-dAlps / 1.2));

            // 4. Americas south of Laurentide ice (strictly uninhabited before ~16k BP)
            boolean isAmericasSouth = (lon >= -130.0 && lon <= -30.0 && lat < 55.0);
            if (isAmericasSouth) {
                return 0.0;
            }

            double occ = Math.min(wLaurentide, Math.min(wFenno, wAlps));
            return Math.clamp(occ, 0.0, 1.0);
        } else if (year <= -4500L) {
            // -10,900 to -6,000 BP (Younger Dryas & Early/Middle Holocene)
            // Greenland inland ice
            if (lat > 68.0 && lon > -55.0 && lon < -18.0) return 0.0;
            return 1.0;
        }
        return 1.0;
    }

    public static boolean isHomininOccupied(double lon, double lat, long year) {
        return getHomininOccupancyWeight(lon, lat, year) > 0.001;
    }

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

    public static BufferedImage createPureTransparentCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

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

    public static boolean loadFromYearDirectory(Scenario scenario) {
        if (scenario == null) return false;
        long year = scenario.getStartDateYear();
        java.nio.file.Path earthDir = java.nio.file.Paths.get("data", "maps", "ether", "earth", String.valueOf(year));
        if (!java.nio.file.Files.isDirectory(earthDir)) {
            return false;
        }

        try {
            // Check density
            java.nio.file.Path densityFile = earthDir.resolve("earth_" + year + "_density.png");
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
                java.nio.file.Path p = earthDir.resolve("earth_" + year + "_" + mapTags[i] + ".png");
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
                java.nio.file.Path p = earthDir.resolve("earth_" + year + "_" + geoTags[i] + ".png");
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            boolean loaded = scenario.getCustomDensityBase64() != null;
            if (loaded) {
                logger.info("Successfully loaded scenario '{}' (Year {}) maps from standardized directory '{}'.", scenario.getName(), year, earthDir);
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

    public static BufferedImage rasterizeBiomesMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage refBiome = null;
        try {
            java.io.File refFile = new java.io.File("data/maps/reference_earth_biomes.png");
            if (!refFile.exists()) refFile = new java.io.File("data/maps/ether/earth/2026/earth_2026_biomes.png");
            if (!refFile.exists()) refFile = new java.io.File("data/maps/scratch_db000ab_earth_biomes.png");
            if (refFile.exists()) {
                refBiome = ImageIO.read(refFile);
            }
        } catch (Exception e) {
            logger.warn("Could not load reference biome map: {}", e.getMessage());
        }

        BufferedImage elevMask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / HEIGHT;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / WIDTH;
                boolean isLand = (elevMask != null) ? ((elevMask.getRGB(x, y) & 0xFF) > 128) : (lat > -60.0);

                if (!isLand) {
                    img.setRGB(x, y, BIOME_OCEAN);
                    continue;
                }

                int baseColor = BIOME_PLAINS;
                if (refBiome != null) {
                    int rx = Math.clamp((int) (((lon + 180.0) / 360.0) * (refBiome.getWidth() - 1)), 0, refBiome.getWidth() - 1);
                    int ry = Math.clamp((int) (((90.0 - lat) / 180.0) * (refBiome.getHeight() - 1)), 0, refBiome.getHeight() - 1);
                    int c = refBiome.getRGB(rx, ry) & 0xFFFFFF;
                    if (c != BIOME_OCEAN && c != BIOME_DEEP_OCEAN && c != 0x000000) {
                        baseColor = c;
                    }
                }

                // Apply epoch-specific paleoclimatic adjustments
                int bColor = baseColor;
                if (lat < -60.0) {
                    bColor = BIOME_GLACIER; // Antarctica ice sheet
                } else if (lat > 75.0 && lon > -70.0 && lon < -15.0) {
                    bColor = BIOME_GLACIER; // Greenland ice sheet
                } else if (year <= -70000L) {
                    // -100,000 BP (Eemian MIS 5d/5e): Green Sahara, Arabian wet corridors, mammoth steppe
                    if (lat >= 14.0 && lat <= 26.0 && lon >= -15.0 && lon <= 55.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Savanna
                    } else if (lat >= 48.0 && lat <= 62.0 && lon >= -10.0 && lon <= 120.0 && baseColor == BIOME_TUNDRA) {
                        bColor = BIOME_PLAINS; // Steppe
                    } else if (lat > 66.0 && (lon > 10.0 && lon < 40.0)) {
                        bColor = BIOME_GLACIER; // Nascent Scandinavian ice
                    }
                } else if (year <= -40000L) {
                    // -50,000 BP (MIS 3): Moderate glaciation, Sahul savanna, Eurasian mammoth steppe
                    if (lat >= 62.0 && lon >= 10.0 && lon <= 35.0) {
                        bColor = BIOME_GLACIER; // Fennoscandian ice
                    } else if (lat >= 55.0 && lon >= -110.0 && lon <= -65.0) {
                        bColor = BIOME_GLACIER; // Laurentide core
                    } else if (lat >= 45.0 && lon >= -10.0 && lon <= 180.0) {
                        bColor = (baseColor == BIOME_FOREST || baseColor == BIOME_JUNGLE) ? BIOME_PLAINS : baseColor; // Mammoth steppe / steppe-tundra
                    } else if (lat <= -10.0 && lat >= -42.0 && lon >= 112.0 && lon <= 155.0) {
                        if (baseColor == BIOME_DESERT) bColor = BIOME_PLAINS; // Sahul savanna & dry sclerophyll woodland
                    }
                } else if (year <= -18000L) {
                    // -25,000 BP & -20,000 BP (LGM Peak): Laurentide, Fennoscandia, Alps, Patagonia, Mammoth Steppe
                    if (lat >= 40.0 && lon >= -135.0 && lon <= -55.0) {
                        bColor = BIOME_GLACIER; // Laurentide & Cordilleran ice sheet
                    } else if (lat >= 50.0 && lat <= 72.0 && lon >= -12.0 && lon <= 48.0) {
                        bColor = BIOME_GLACIER; // Fennoscandian & British-Irish ice sheet
                    } else if (lat >= 44.5 && lat <= 47.8 && lon >= 5.5 && lon <= 15.0) {
                        bColor = BIOME_GLACIER; // Alpine ice cap
                    } else if (lat <= -40.0 && lon >= -75.0 && lon <= -68.0) {
                        bColor = BIOME_GLACIER; // Patagonian ice sheet
                    } else if (lat >= 38.0 && lon >= -10.0 && lon <= 180.0) {
                        bColor = BIOME_TUNDRA; // Continuous unglaciated Mammoth Steppe across all Siberia & Eurasia (no taiga artifacts)
                    } else if (lat >= 38.0 && lon < -135.0) {
                        bColor = BIOME_TUNDRA; // Beringian Mammoth Steppe
                    }
                } else if (year <= -10500L) {
                    // -10,900 BP (Younger Dryas): Abrupt cold reversal, AMOC shutdown
                    if (lat >= 48.0 && lat <= 68.0 && lon >= -105.0 && lon <= -60.0) {
                        bColor = BIOME_GLACIER; // Laurentide residual sheet
                    } else if (lat >= 60.0 && lat <= 70.0 && lon >= 8.0 && lon <= 30.0) {
                        bColor = BIOME_GLACIER; // Fennoscandian glacier
                    } else if (lat >= 50.0 && lat <= 60.0 && lon >= -10.0 && lon <= 45.0) {
                        bColor = BIOME_TUNDRA; // European periglacial tundra / cold steppe
                    } else if (lat >= 48.0 && lon >= 45.0 && lon <= 180.0 && (baseColor == BIOME_FOREST || baseColor == BIOME_JUNGLE)) {
                        bColor = BIOME_PLAINS; // Siberian mammoth steppe
                    }
                } else if (year <= -9000L) {
                    // -10,000 BP (Early Holocene / Preboreal): Glacial retreat, early African Humid Period
                    if (lat >= 55.0 && lat <= 68.0 && lon >= -95.0 && lon <= -65.0) {
                        bColor = BIOME_GLACIER; // Shrinking Laurentide ice
                    } else if (lat >= 64.0 && lat <= 69.0 && lon >= 12.0 && lon <= 22.0) {
                        bColor = BIOME_GLACIER; // Scandinavian mountain ice remnant
                    } else if (lat >= 14.0 && lat <= 26.0 && lon >= -15.0 && lon <= 40.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Early Green Sahara savanna
                    } else if (lat >= 15.0 && lat <= 26.0 && lon >= 40.0 && lon <= 58.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Early Green Arabia
                    }
                } else if (year <= -7000L) {
                    // -8,000 BP (8.2 ka Event / Early Neolithic): African Humid Period & European primary forest
                    if (lat >= 58.0 && lat <= 66.0 && lon >= -80.0 && lon <= -65.0) {
                        bColor = BIOME_GLACIER; // Ungava Laurentide ice remnant
                    } else if (lat >= 13.0 && lat <= 30.0 && lon >= -16.0 && lon <= 40.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Green Sahara
                    } else if (lat >= 14.0 && lat <= 28.0 && lon >= 40.0 && lon <= 60.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Green Arabia
                    } else if (lat >= 42.0 && lat <= 56.0 && lon >= -10.0 && lon <= 28.0 && baseColor == BIOME_PLAINS) {
                        bColor = BIOME_FOREST; // Climax mixed oak-elm forest in temperate Europe
                    }
                } else if (year <= -4500L) {
                    // -6,000 BP (Holocene Climatic Optimum / Middle Neolithic): Maximum Green Sahara & Mega-Chad
                    if (lat >= 12.0 && lat <= 30.0 && lon >= -17.0 && lon <= 40.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Lush Green Sahara & Mega-Chad Savanna
                    } else if (lat >= 13.0 && lat <= 28.0 && lon >= 40.0 && lon <= 60.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                        bColor = BIOME_PLAINS; // Green Arabia
                    } else if (lat >= 40.0 && lat <= 58.0 && lon >= -10.0 && lon <= 30.0 && baseColor == BIOME_PLAINS) {
                        bColor = BIOME_FOREST; // Dense European broadleaf forest
                    }
                }
                img.setRGB(x, y, bColor);
            }
        }
        return img;
    }

    public static BufferedImage rasterizeTemperatureMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        // Baseline global thermal parameters (°C)
        double tEq = 27.0;
        double tPole = -35.0;

        if (year <= -70000L) {
            // Eemian (-100k BP): Warmer interglacial
            tEq = 28.0;
            tPole = -28.0;
        } else if (year <= -40000L) {
            // MIS 3 (-50k BP): Moderate interstadial
            tEq = 25.0;
            tPole = -42.0;
        } else if (year <= -22000L) {
            // LGM Onset (-25k BP)
            tEq = 24.0;
            tPole = -48.0;
        } else if (year <= -18000L) {
            // LGM Peak (-20k BP): Maximum glacial cooling
            tEq = 23.0;
            tPole = -55.0;
        } else if (year <= -10500L) {
            // Younger Dryas (-10,900 BP): AMOC cold reversal
            tEq = 24.5;
            tPole = -46.0;
        } else if (year <= -9000L) {
            // Early Holocene (-10,000 BP): Rapid post-glacial warming
            tEq = 26.5;
            tPole = -36.0;
        } else if (year <= -7000L) {
            // Early Neolithic (-8,000 BP): Boreal thermal climb
            tEq = 26.8;
            tPole = -33.0;
        } else if (year <= -4500L) {
            // Holocene Optimum (-6,000 BP): Hypsithermal thermal peak (+1 to +2°C global)
            tEq = 27.5;
            tPole = -30.0;
        } else if (year <= -1000L) {
            tEq = 26.0;
            tPole = -38.0;
        }

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            double cosLat = Math.cos(Math.toRadians(lat));
            double sinLat = Math.abs(Math.sin(Math.toRadians(lat)));

            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                boolean isLand = (mask != null) && (mask.getRaster().getSample(mx, my, 0) > 0);

                // Thermal latitudinal profile
                double tempC = tEq * cosLat * cosLat + tPole * sinLat * sinLat;

                // Continental thermal enhancement / ocean moderation
                if (isLand) {
                    double distFromCoast = Math.min(1.0, Math.abs(lon) / 100.0);
                    if (lat > 35.0) {
                        tempC -= (lat - 35.0) * 0.15; // Higher continental winter chill
                    }
                }

                // Glacial ice sheet albedo feedback cooling
                if (year <= -18000L) {
                    if (lat >= 40.0 && lon >= -135.0 && lon <= -55.0) tempC -= 14.0; // Laurentide
                    else if (lat >= 50.0 && lat <= 72.0 && lon >= -12.0 && lon <= 48.0) tempC -= 12.0; // Fennoscandia
                } else if (year <= -10500L) {
                    // North Atlantic AMOC shutdown cold plume
                    if (lat >= 45.0 && lat <= 68.0 && lon >= -40.0 && lon <= 25.0) tempC -= 7.0;
                }

                // Encode temperature [-50°C, +50°C] -> [0, 255]
                double norm = Math.clamp((tempC + 50.0) / 100.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    public static BufferedImage rasterizePrecipitationMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            double absLat = Math.abs(lat);

            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                // 1. ITCZ Tropical precipitation belt
                double rainMm = 2200.0 * Math.exp(-(absLat * absLat) / 120.0);

                // 2. Subtropical dry descent (Hadley cell descending branch ~20° to 32°)
                double dryBelt = Math.exp(-Math.pow(absLat - 25.0, 2) / 60.0);
                rainMm *= (1.0 - dryBelt * 0.85);

                // 3. Mid-latitude storm tracks (~40° to 55°)
                double stormTrack = Math.exp(-Math.pow(absLat - 48.0, 2) / 80.0);
                rainMm += stormTrack * 950.0;

                // 4. Polar arid zone
                if (absLat > 65.0) {
                    rainMm = Math.max(80.0, rainMm * Math.exp(-(absLat - 65.0) / 10.0));
                }

                // Paleoclimatic monsoon & aridity shifts
                if (year <= -70000L || (year <= -4500L && year >= -10000L)) {
                    // African Humid Period / Green Sahara & Arabian wet phase
                    if (lat >= 12.0 && lat <= 28.0 && lon >= -16.0 && lon <= 55.0) {
                        rainMm += 950.0;
                    }
                    // Asian summer monsoon enhancement
                    if (lat >= 20.0 && lat <= 38.0 && lon >= 70.0 && lon <= 120.0) {
                        rainMm += 400.0;
                    }
                } else if (year <= -18000L && year >= -25000L) {
                    // LGM Global Aridification (40-50% reduction in mid/high latitudes)
                    if (absLat >= 30.0) rainMm *= 0.55;
                } else if (year <= -40000L) {
                    // MIS 3 Sahul monsoon
                    if (lat <= -10.0 && lat >= -30.0 && lon >= 115.0 && lon <= 150.0) {
                        rainMm += 450.0;
                    }
                }

                // Encode rainfall [0, 3000 mm/yr] -> [0, 255]
                double norm = Math.clamp(rainMm / 3000.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    public static BufferedImage rasterizeSeasonalityMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            double absLat = Math.abs(lat);

            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                boolean isLand = (mask != null) && (mask.getRaster().getSample(mx, my, 0) > 0);

                // Base orbital obliquity seasonality amplitude (0° at equator, ~25°C at poles)
                double ampC = Math.sin(Math.toRadians(absLat)) * 26.0;

                // Continentality boost: deep Eurasian / North American continental interior
                if (isLand) {
                    double contSiberia = Math.exp(-(Math.pow(lat - 62.0, 2) + Math.pow(lon - 105.0, 2)) / 500.0);
                    double contCanada = Math.exp(-(Math.pow(lat - 55.0, 2) + Math.pow(lon - (-95.0), 2)) / 400.0);
                    ampC += (contSiberia * 22.0 + contCanada * 18.0);
                    if (absLat > 30.0) ampC += 6.0;
                }

                if (year <= -18000L) {
                    // Glacial high continentality
                    ampC *= 1.15;
                }

                // Encode seasonality [0, 50°C] -> [0, 255]
                double norm = Math.clamp(ampC / 50.0, 0.0, 1.0);
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

            // 1. Save standard earth_<year>_<layer>.png in ultra-high-definition 2048x1024
            if (imgDensity != null)         ImageIO.write(imgDensity,         "PNG", earthDir.resolve("earth_" + year + "_density.png").toFile());
            if (imgIsogloss != null)        ImageIO.write(imgIsogloss,        "PNG", earthDir.resolve("earth_" + year + "_isogloss.png").toFile());
            if (imgKinship != null)         ImageIO.write(imgKinship,         "PNG", earthDir.resolve("earth_" + year + "_kinship.png").toFile());
            if (imgRituals != null)         ImageIO.write(imgRituals,         "PNG", earthDir.resolve("earth_" + year + "_rituals.png").toFile());
            if (imgSovereignty != null)     ImageIO.write(imgSovereignty,     "PNG", earthDir.resolve("earth_" + year + "_sovereignty.png").toFile());
            if (imgTech != null)            ImageIO.write(imgTech,            "PNG", earthDir.resolve("earth_" + year + "_technology.png").toFile());
            if (imgTrade != null)           ImageIO.write(imgTrade,           "PNG", earthDir.resolve("earth_" + year + "_tradenetwork.png").toFile());
            if (imgInst != null)            ImageIO.write(imgInst,            "PNG", earthDir.resolve("earth_" + year + "_institutional.png").toFile());
            if (imgEco != null)             ImageIO.write(imgEco,             "PNG", earthDir.resolve("earth_" + year + "_ecological.png").toFile());
            if (imgPathogen != null)        ImageIO.write(imgPathogen,        "PNG", earthDir.resolve("earth_" + year + "_pathogen.png").toFile());

            if (imgCoal != null)            ImageIO.write(imgCoal,            "PNG", earthDir.resolve("earth_" + year + "_coal.png").toFile());
            if (imgOil != null)             ImageIO.write(imgOil,             "PNG", earthDir.resolve("earth_" + year + "_oil.png").toFile());
            if (imgGas != null)             ImageIO.write(imgGas,             "PNG", earthDir.resolve("earth_" + year + "_gas.png").toFile());
            if (imgUranium != null)         ImageIO.write(imgUranium,         "PNG", earthDir.resolve("earth_" + year + "_uranium.png").toFile());
            if (imgHe3 != null)             ImageIO.write(imgHe3,             "PNG", earthDir.resolve("earth_" + year + "_helium3.png").toFile());
            if (imgIronCopper != null)      ImageIO.write(imgIronCopper,      "PNG", earthDir.resolve("earth_" + year + "_iron_copper.png").toFile());
            if (imgPreciousMetals != null)  ImageIO.write(imgPreciousMetals,  "PNG", earthDir.resolve("earth_" + year + "_precious_metals.png").toFile());
            if (imgRareEarths != null)      ImageIO.write(imgRareEarths,      "PNG", earthDir.resolve("earth_" + year + "_rare_earths.png").toFile());
            if (imgMantleHeat != null)      ImageIO.write(imgMantleHeat,      "PNG", earthDir.resolve("earth_" + year + "_geothermal.png").toFile());
            if (imgAquifer != null)         ImageIO.write(imgAquifer,         "PNG", earthDir.resolve("earth_" + year + "_aquifers.png").toFile());

            // 2. Save authentic paleoclimatic biomes map (if missing)
            java.nio.file.Path biomesPath = earthDir.resolve("earth_" + year + "_biomes.png");
            if (!java.nio.file.Files.exists(biomesPath)) {
                BufferedImage biomesImg = rasterizeBiomesMap(year);
                if (biomesImg != null) {
                    ImageIO.write(biomesImg, "PNG", biomesPath.toFile());
                }
            }

            // 3. Save authentic epoch paleoclimatic layers (Temperature, Precipitation, Seasonality) if missing
            java.nio.file.Path tempPath = earthDir.resolve("earth_" + year + "_temperature.png");
            if (!java.nio.file.Files.exists(tempPath)) {
                BufferedImage tempImg = rasterizeTemperatureMap(year);
                if (tempImg != null) {
                    ImageIO.write(tempImg, "PNG", tempPath.toFile());
                }
            }
            java.nio.file.Path rainPath = earthDir.resolve("earth_" + year + "_precipitation.png");
            if (!java.nio.file.Files.exists(rainPath)) {
                BufferedImage rainImg = rasterizePrecipitationMap(year);
                if (rainImg != null) {
                    ImageIO.write(rainImg, "PNG", rainPath.toFile());
                }
            }
            java.nio.file.Path seasPath = earthDir.resolve("earth_" + year + "_seasonality.png");
            if (!java.nio.file.Files.exists(seasPath)) {
                BufferedImage seasImg = rasterizeSeasonalityMap(year);
                if (seasImg != null) {
                    ImageIO.write(seasImg, "PNG", seasPath.toFile());
                }
            }

            // 4. Ensure invariant NOAA ETOPO relief elevation map is present across all epochs
            java.nio.file.Path elevPath = earthDir.resolve("earth_" + year + "_elevation.png");
            if (!java.nio.file.Files.exists(elevPath)) {
                java.io.File srcElev = new java.io.File("data/maps/reference_earth_elevation.png");
                if (!srcElev.exists()) srcElev = new java.io.File("data/maps/ether/earth/-100000/earth_-100000_elevation.png");
                if (!srcElev.exists()) srcElev = new java.io.File("data/maps/ether/earth/2026/earth_2026_elevation.png");
                if (srcElev.exists()) {
                    java.nio.file.Files.copy(srcElev.toPath(), elevPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            logger.info("Persisted standard scenario cartographic maps into 'data/maps/ether/earth/{}/'", year);
        } catch (Exception e) {
            logger.warn("Failed to persist scenario maps to year directory 'data/maps/ether/earth/{}/': {}", year, e.getMessage());
        }
    }

    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {
        if (src == null) return null;
        if (year >= -10000) return src;

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / h;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / w;
                int rgb = src.getRGB(x, y);

                double weight = getHomininOccupancyWeight(lon, lat, year);
                if (weight > 0.001 && isLand(lon, lat)) {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int lum = (int) ((0.299 * r + 0.587 * g + 0.114 * b) * weight);
                    lum = Math.clamp(lum, 0, 255);
                    masked.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
                } else {
                    masked.setRGB(x, y, 0x000000); // Pure black for uninhabited regions & oceans
                }
            }
        }
        return masked;
    }

    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;
                boolean isLand = isLand(lng, lat);
                double weight = getHomininOccupancyWeight(lng, lat, year);
                if (!isLand || weight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                double dens = 0.0;
                if (year <= -70000) {
                    // -100,000 BP: Sapiens in Africa & Levant, Mousterian Neanderthals, Denisovans
                    double densSapiens = 1.8 +
                        5.5 * Math.exp(-distSq(lng, lat, 36.0, 0.0) / 450.0) +      // East African Rift
                        4.0 * Math.exp(-distSq(lng, lat, 22.0, -34.0) / 300.0) +    // South African Coast
                        3.5 * Math.exp(-distSq(lng, lat, 32.0, 26.0) / 300.0) +     // Nile Corridor
                        2.5 * Math.exp(-distSq(lng, lat, 10.0, 14.0) / 400.0) +     // Sahel / West Africa
                        3.0 * Math.exp(-distSq(lng, lat, 35.0, 31.5) / 250.0) +     // Levant
                        2.2 * Math.exp(-distSq(lng, lat, 50.0, 16.0) / 300.0);      // Southern Arabia

                    double densNeanderthal = 0.35 +
                        0.85 * Math.exp(-distSq(lng, lat, 2.0, 44.0) / 350.0) +     // France / Cantabria
                        0.75 * Math.exp(-distSq(lng, lat, -4.0, 40.0) / 300.0) +    // Iberia
                        0.70 * Math.exp(-distSq(lng, lat, 16.0, 47.0) / 350.0) +    // Central Europe / Balkans
                        0.65 * Math.exp(-distSq(lng, lat, 40.0, 44.0) / 300.0) +    // Caucasus / Crimea
                        0.60 * Math.exp(-distSq(lng, lat, 44.0, 36.0) / 300.0) +    // Zagros
                        0.45 * Math.exp(-distSq(lng, lat, 85.0, 51.0) / 250.0);     // Altai

                    double densDenisovan = 0.40 +
                        1.10 * Math.exp(-distSq(lng, lat, 112.0, 34.0) / 400.0) +   // Yellow River / North China
                        0.95 * Math.exp(-distSq(lng, lat, 110.0, 26.0) / 400.0) +   // South China
                        0.90 * Math.exp(-distSq(lng, lat, 105.0, -2.0) / 500.0) +   // Sundaland
                        0.80 * Math.exp(-distSq(lng, lat, 102.0, 16.0) / 350.0) +   // Indochina
                        0.75 * Math.exp(-distSq(lng, lat, 78.0, 20.0) / 400.0) +    // Indian Subcontinent
                        0.40 * Math.exp(-distSq(lng, lat, 102.0, 35.0) / 250.0);    // Tibetan Plateau

                    dens = blendPaleoTraits(lng, lat, densSapiens, densNeanderthal, densDenisovan);
                } else if (year <= -40000) {
                    // -50,000 BP (MIS 3): Sapiens expansion, initial IUP in Eurasia, maritime colonization of SAHUL
                    double densAfrica = 1.6 +
                        4.5 * Math.exp(-distSq(lng, lat, 36.0, 0.5) / 400.0) +      // East Africa
                        3.5 * Math.exp(-distSq(lng, lat, 21.5, -34.0) / 300.0) +    // South Africa (Blombos/Klasies)
                        3.0 * Math.exp(-distSq(lng, lat, 32.5, 26.0) / 280.0) +     // Nile
                        2.2 * Math.exp(-distSq(lng, lat, 8.0, 12.0) / 350.0) +      // West Africa
                        2.5 * Math.exp(-distSq(lng, lat, -7.0, 32.0) / 300.0);      // Maghreb (Taforalt)

                    double densSahul = 0.8 * Math.exp(-distSq(lng, lat, 134.0, -24.0) / 500.0) +
                        3.2 * Math.exp(-distSq(lng, lat, 132.9, -12.5) / 120.0) + // Madjedbebe / Arnhem Land
                        2.8 * Math.exp(-distSq(lng, lat, 125.0, -16.0) / 150.0) + // Kimberley / Carpenter's Gap
                        2.6 * Math.exp(-distSq(lng, lat, 143.0, -33.7) / 160.0) + // Lake Mungo / Willandra Lakes
                        2.2 * Math.exp(-distSq(lng, lat, 116.0, -32.0) / 140.0) + // Swan River / Devil's Lair
                        2.0 * Math.exp(-distSq(lng, lat, 143.0, -5.5) / 150.0) +  // New Guinea Highlands
                        1.5 * Math.exp(-distSq(lng, lat, 147.0, -42.0) / 100.0);  // Tasmania

                    double densEurasia = 0.45 +
                        1.5 * Math.exp(-distSq(lng, lat, 35.5, 32.5) / 180.0) +     // Levant (Ksar Akil / Boker Tachtit)
                        1.2 * Math.exp(-distSq(lng, lat, 25.0, 43.0) / 200.0) +     // Bacho Kiro / Balkans IUP
                        0.9 * Math.exp(-distSq(lng, lat, 1.5, 45.0) / 220.0) +      // Franco-Cantabrian Châtelperronian/Mousterian
                        0.8 * Math.exp(-distSq(lng, lat, -4.0, 38.0) / 200.0) +     // Iberian Mousterian
                        1.4 * Math.exp(-distSq(lng, lat, 78.0, 22.0) / 300.0) +     // Indian subcontinent
                        1.6 * Math.exp(-distSq(lng, lat, 105.0, -2.0) / 350.0) +    // Sundaland
                        1.2 * Math.exp(-distSq(lng, lat, 112.0, 32.0) / 300.0) +    // China (Tianyuan / Shuidonggou)
                        0.7 * Math.exp(-distSq(lng, lat, 84.5, 51.4) / 150.0);      // Denisova / Altai

                    dens = Math.max(densAfrica, Math.max(densSahul, densEurasia));
                } else if (year <= -22000) {
                    // -25,000 BP: Gravettian Horizon across Europe, Asian basins, Sahul, and BERINGIAN STANDSTILL
                    double densEurope = 0.4 +
                        3.8 * Math.exp(-distSq(lng, lat, 16.5, 48.8) / 120.0) +     // Pavlovian / Dolní Věstonice / Willendorf
                        3.5 * Math.exp(-distSq(lng, lat, 1.0, 45.0) / 140.0) +      // Franco-Cantabrian Gravettian (Laugerie, Abri Pataud)
                        3.2 * Math.exp(-distSq(lng, lat, 39.0, 51.4) / 150.0) +     // Kostenki-Borshchevo / Don
                        2.8 * Math.exp(-distSq(lng, lat, 40.5, 56.2) / 120.0) +     // Sungir / Upper Volga
                        2.6 * Math.exp(-distSq(lng, lat, 15.5, 41.7) / 120.0) +     // Paglicci / Italian Gravettian
                        2.4 * Math.exp(-distSq(lng, lat, -7.5, 37.5) / 120.0);      // Vale Boi / Iberian Gravettian

                    double densBeringia =
                        3.2 * Math.exp(-distSq(lng, lat, 135.4, 70.7) / 120.0) +   // Yana RHS (Arctic Siberia mammoth hunters)
                        2.8 * Math.exp(-distSq(lng, lat, 145.0, 71.0) / 120.0) +   // Berelekh mammoth graveyard
                        3.0 * Math.exp(-distSq(lng, lat, -140.7, 67.1) / 140.0) + // Bluefish Caves (Yukon / Beringia standstill)
                        2.5 * Math.exp(-distSq(lng, lat, -168.0, 65.0) / 180.0);  // Central Beringian Land Bridge

                    double densAsiaAfrica = 0.5 +
                        2.8 * Math.exp(-distSq(lng, lat, 35.5, 32.7) / 140.0) +     // Ohalo II / Early Epipaleolithic Levant
                        2.5 * Math.exp(-distSq(lng, lat, 32.0, 26.0) / 200.0) +     // Nile Valley
                        2.2 * Math.exp(-distSq(lng, lat, 22.0, -34.0) / 250.0) +    // South African LSA
                        2.2 * Math.exp(-distSq(lng, lat, 78.0, 22.0) / 250.0) +     // India
                        2.4 * Math.exp(-distSq(lng, lat, 115.0, 30.0) / 250.0) +    // Yangtze / South China
                        1.8 * Math.exp(-distSq(lng, lat, 135.0, -25.0) / 300.0);    // Sahul forager network

                    dens = Math.max(densEurope, Math.max(densBeringia, densAsiaAfrica));
                } else if (year <= -15000) {
                    // -20,000 BP: LGM Paroxysm Refugia (Solutrean Franco-Cantabria, Mediterranean, Kebaran, Beringia)
                    double densSolutrean =
                        4.5 * Math.exp(-distSq(lng, lat, 0.5, 44.8) / 90.0) +    // Dordogne & Aquitaine Solutrean Core (Laugerie-Haute, Solutré)
                        4.2 * Math.exp(-distSq(lng, lat, -4.5, 43.4) / 80.0) +   // Cantabrian / Altamira Solutrean
                        3.5 * Math.exp(-distSq(lng, lat, -3.5, 38.0) / 100.0) +   // Iberian Mediterranean (Parpalló)
                        3.0 * Math.exp(-distSq(lng, lat, -8.5, 37.1) / 90.0);    // Portuguese Estremadura (Vale Almoinha)

                    double densMedEpigravettian =
                        3.6 * Math.exp(-distSq(lng, lat, 15.5, 41.7) / 90.0) +   // Grotta Paglicci / Italian Epigravettian
                        3.2 * Math.exp(-distSq(lng, lat, 23.0, 38.5) / 100.0);    // Franchthi / Greek refuge

                    double densEasternRefugia =
                        3.2 * Math.exp(-distSq(lng, lat, 35.0, 50.5) / 110.0) +  // Mezhirich / Dnepr mammoth bone settlements
                        3.0 * Math.exp(-distSq(lng, lat, 39.0, 51.4) / 100.0);    // Kostenki

                    double densLevantAfrica = 0.4 +
                        4.2 * Math.exp(-distSq(lng, lat, 35.5, 32.7) / 90.0) +      // Kebaran Levant (Ohalo II / Sea of Galilee)
                        3.2 * Math.exp(-distSq(lng, lat, 32.5, 25.5) / 180.0) +     // Nile valley (Wadi Kubbaniya)
                        2.4 * Math.exp(-distSq(lng, lat, 22.0, -34.0) / 200.0) +    // South Africa (Nelson Bay, Boomplaas)
                        2.2 * Math.exp(-distSq(lng, lat, 80.0, 22.0) / 250.0) +     // India
                        2.5 * Math.exp(-distSq(lng, lat, 114.0, 28.0) / 250.0);     // South China

                    double densBeringiaLGM =
                        2.5 * Math.exp(-distSq(lng, lat, -140.7, 67.1) / 110.0) + // Bluefish Caves
                        2.2 * Math.exp(-distSq(lng, lat, 135.4, 70.7) / 110.0) +  // Yana RHS
                        2.0 * Math.exp(-distSq(lng, lat, -165.0, 65.0) / 140.0); // Beringia Standstill

                    double densSahulLGM =
                        2.4 * Math.exp(-distSq(lng, lat, 132.9, -12.5) / 140.0) + // Madjedbebe
                        2.2 * Math.exp(-distSq(lng, lat, 143.0, -33.7) / 160.0) + // Lake Mungo
                        2.0 * Math.exp(-distSq(lng, lat, 125.0, -16.0) / 160.0);  // Kimberley

                    dens = Math.max(densSolutrean, Math.max(densMedEpigravettian,
                           Math.max(densEasternRefugia, Math.max(densLevantAfrica,
                           Math.max(densBeringiaLGM, densSahulLGM)))));
                } else if (year <= -10500) {
                    // -10,900 BP: Younger Dryas & Clovis Horizon / Natufian Epipaleolithic
                    double densNatufian = 0.5 +
                        6.5 * Math.exp(-distSq(lng, lat, 35.58, 33.08) / 80.0) +    // Ain Mallaha / Hula Valley Natufian core
                        5.8 * Math.exp(-distSq(lng, lat, 35.22, 32.92) / 80.0) +    // Hayonim Cave
                        5.2 * Math.exp(-distSq(lng, lat, 37.00, 32.00) / 90.0) +    // Shubayqa 1 (Black Desert / early bread)
                        4.5 * Math.exp(-distSq(lng, lat, 41.50, 38.10) / 100.0);   // Hallan Çemi / Upper Tigris proto-sedentism

                    double densClovis = 0.3 +
                        3.8 * Math.exp(-distSq(lng, lat, -103.3, 34.3) / 120.0) +   // Blackwater Draw / Clovis type site
                        3.5 * Math.exp(-distSq(lng, lat, -97.7, 30.9) / 120.0) +    // Gault Site TX
                        3.2 * Math.exp(-distSq(lng, lat, -110.2, 31.6) / 100.0) +   // Murray Springs AZ
                        3.0 * Math.exp(-distSq(lng, lat, -75.1, 41.0) / 120.0) +    // Shawnee-Minisink PA
                        2.8 * Math.exp(-distSq(lng, lat, -120.5, 42.7) / 120.0);   // Paisley Caves OR

                    double densSouthAmerica = 0.3 +
                        3.5 * Math.exp(-distSq(lng, lat, -73.2, -41.5) / 120.0) +   // Monte Verde II Chile
                        2.8 * Math.exp(-distSq(lng, lat, -77.7, -9.2) / 100.0) +    // Guitarrero Cave Peru
                        2.5 * Math.exp(-distSq(lng, lat, -42.5, -9.3) / 120.0) +    // Pedra Furada Brazil
                        2.2 * Math.exp(-distSq(lng, lat, -70.0, -52.0) / 120.0);   // Fell's Cave Patagonia

                    double densEuropeYD = 0.4 +
                        4.0 * Math.exp(-distSq(lng, lat, 1.0, 45.0) / 120.0) +      // Franco-Cantabrian Late Magdalenian/Azilian
                        3.5 * Math.exp(-distSq(lng, lat, 3.0, 49.5) / 120.0) +      // Federmesser / Ahrensburgian Paris/Rhine
                        3.2 * Math.exp(-distSq(lng, lat, 15.5, 41.7) / 100.0) +     // Epigravettian Italy
                        2.8 * Math.exp(-distSq(lng, lat, 35.0, 50.5) / 120.0);     // Dnepr mammoth/reindeer camp

                    double densAsiaAfricaYD = 0.5 +
                        3.5 * Math.exp(-distSq(lng, lat, 114.0, 34.5) / 160.0) +    // Yellow River Late Paleolithic
                        3.2 * Math.exp(-distSq(lng, lat, 117.2, 28.7) / 140.0) +    // Xianrendong early pottery
                        3.0 * Math.exp(-distSq(lng, lat, 139.5, 35.7) / 120.0) +    // Incipient Jomon Japan
                        3.0 * Math.exp(-distSq(lng, lat, 77.6, 22.9) / 160.0) +     // Bhimbetka India
                        3.2 * Math.exp(-distSq(lng, lat, 32.5, 25.5) / 160.0) +     // Nile Valley
                        2.8 * Math.exp(-distSq(lng, lat, -2.4, 34.8) / 120.0) +     // Taforalt Maghreb
                        2.0 * Math.exp(-distSq(lng, lat, 143.0, -33.7) / 180.0);   // Sahul

                    dens = Math.max(densNatufian, Math.max(densClovis,
                           Math.max(densSouthAmerica, Math.max(densEuropeYD, densAsiaAfricaYD))));
                } else if (year <= -9000) {
                    // -10,000 BP: Early Holocene / Pre-Pottery Neolithic A (Göbekli Tepe, Jericho, Folsom)
                    double densPPNA = 0.6 +
                        8.5 * Math.exp(-distSq(lng, lat, 38.92, 37.22) / 70.0) +    // Göbekli Tepe / Karahan Tepe Monumental Core
                        7.5 * Math.exp(-distSq(lng, lat, 35.44, 31.87) / 70.0) +    // Jericho PPNA (Tell es-Sultan)
                        6.5 * Math.exp(-distSq(lng, lat, 38.10, 35.90) / 80.0) +    // Mureybet / Jerf el Ahmar
                        6.0 * Math.exp(-distSq(lng, lat, 39.70, 38.20) / 80.0) +    // Çayönü Tepesi
                        5.0 * Math.exp(-distSq(lng, lat, 47.40, 32.50) / 90.0);     // Ali Kosh Zagros

                    double densChinaPPN = 0.5 +
                        5.5 * Math.exp(-distSq(lng, lat, 113.6, 34.4) / 120.0) +    // Peiligang / Yellow River proto-millet
                        5.0 * Math.exp(-distSq(lng, lat, 120.0, 29.5) / 120.0) +    // Shangshan Yangtze rice foragers
                        4.5 * Math.exp(-distSq(lng, lat, 111.5, 25.5) / 120.0) +    // Yuchanyan
                        4.0 * Math.exp(-distSq(lng, lat, 139.5, 35.7) / 100.0);     // Initial Jomon Japan

                    double densEuropeMeso = 0.5 +
                        4.5 * Math.exp(-distSq(lng, lat, 22.0, 44.5) / 100.0) +     // Lepenski Vir / Danube Iron Gates
                        4.0 * Math.exp(-distSq(lng, lat, 23.1, 37.4) / 90.0) +      // Franchthi Cave Greece
                        3.8 * Math.exp(-distSq(lng, lat, 0.5, 54.2) / 110.0) +      // Star Carr / Doggerland Mesolithic
                        3.5 * Math.exp(-distSq(lng, lat, 12.0, 55.5) / 110.0) +     // Maglemosian Scandinavia
                        3.5 * Math.exp(-distSq(lng, lat, -4.0, 43.4) / 100.0);     // Cantabrian Mesolithic

                    double densAmericasEarly = 0.4 +
                        3.8 * Math.exp(-distSq(lng, lat, -103.0, 36.0) / 120.0) +   // Folsom type site NM
                        3.5 * Math.exp(-distSq(lng, lat, -99.5, 18.0) / 100.0) +    // Balsas Valley Mesoamerica
                        3.2 * Math.exp(-distSq(lng, lat, -79.3, -7.7) / 100.0) +    // Paiján complex Peru
                        3.0 * Math.exp(-distSq(lng, lat, -80.8, -2.2) / 100.0) +    // Las Vegas Ecuador
                        2.8 * Math.exp(-distSq(lng, lat, -42.5, -8.8) / 120.0);    // Serra da Capivara Brazil

                    double densAfricaSaharaEarly = 0.5 +
                        4.8 * Math.exp(-distSq(lng, lat, 32.5, 25.5) / 140.0) +     // Nile Valley Epipaleolithic
                        4.2 * Math.exp(-distSq(lng, lat, 14.0, 13.0) / 160.0) +     // Lake Mega-Chad aquatic foragers
                        3.8 * Math.exp(-distSq(lng, lat, 8.0, 35.0) / 120.0) +      // Capsian culture Maghreb
                        3.5 * Math.exp(-distSq(lng, lat, 68.0, 29.3) / 120.0) +     // Mehrgarh precursor Pakistan
                        3.2 * Math.exp(-distSq(lng, lat, 77.6, 22.9) / 140.0) +     // Bhimbetka India
                        2.5 * Math.exp(-distSq(lng, lat, 144.3, -5.8) / 120.0);    // Kuk Swamp Highlands proto-horticulture

                    dens = Math.max(densPPNA, Math.max(densChinaPPN,
                           Math.max(densEuropeMeso, Math.max(densAmericasEarly, densAfricaSaharaEarly))));
                } else if (year <= -7000) {
                    // -8,000 BP: Early Neolithic / 8.2 ka Event (Çatalhöyük, Jiahu, Mehrgarh, Early European Farmers)
                    double densAnatoliaNeolithic = 0.8 +
                        14.0 * Math.exp(-distSq(lng, lat, 32.83, 37.67) / 60.0) +   // Çatalhöyük Mega-Village Core
                        11.0 * Math.exp(-distSq(lng, lat, 30.10, 37.60) / 60.0) +   // Hacilar
                        10.5 * Math.exp(-distSq(lng, lat, 35.95, 31.98) / 70.0) +   // Ain Ghazal Jordan
                        9.5 * Math.exp(-distSq(lng, lat, 39.10, 36.50) / 80.0) +    // Tell Sabi Abyad / Halaf
                        9.0 * Math.exp(-distSq(lng, lat, 44.90, 35.60) / 80.0) +    // Jarmo Zagros
                        8.5 * Math.exp(-distSq(lng, lat, 47.20, 34.40) / 80.0);     // Ganj Dareh

                    double densChinaNeolithic = 0.8 +
                        11.5 * Math.exp(-distSq(lng, lat, 113.6, 33.6) / 80.0) +    // Jiahu (Henan - flutes, fermented rice)
                        10.0 * Math.exp(-distSq(lng, lat, 114.2, 36.7) / 90.0) +    // Cishan / Peiligang millet
                        9.5 * Math.exp(-distSq(lng, lat, 120.2, 30.1) / 80.0) +     // Kuahuqiao wet rice
                        8.0 * Math.exp(-distSq(lng, lat, 105.9, 35.0) / 90.0);     // Dadiwan Gansu

                    double densEuropeEEF = 0.7 +
                        9.5 * Math.exp(-distSq(lng, lat, 22.8, 39.3) / 70.0) +      // Sesklo / Thessaly Early Neolithic
                        8.5 * Math.exp(-distSq(lng, lat, 20.5, 44.8) / 80.0) +      // Starčevo-Körös-Criş Danube basin
                        7.5 * Math.exp(-distSq(lng, lat, 9.0, 44.0) / 80.0) +       // Cardial / Impressed Ware Liguria/Provence
                        7.0 * Math.exp(-distSq(lng, lat, 16.0, 48.5) / 90.0) +      // Early LBK pioneers Austria/Moravia
                        6.5 * Math.exp(-distSq(lng, lat, 3.0, 41.5) / 80.0);       // Cardial Catalonia

                    double densMehrgarhIndus = 0.6 +
                        9.5 * Math.exp(-distSq(lng, lat, 68.05, 29.28) / 70.0) +    // Mehrgarh Period I-II Neolithic
                        6.5 * Math.exp(-distSq(lng, lat, 81.5, 25.0) / 100.0);     // Vindhya / Ganges early farming

                    double densGreenSaharaPastoral = 0.6 +
                        8.5 * Math.exp(-distSq(lng, lat, 30.58, 22.53) / 80.0) +    // Nabta Playa Megalithic Calendar Center
                        7.5 * Math.exp(-distSq(lng, lat, 30.85, 29.35) / 80.0) +    // Faiyum A Early Agriculture
                        6.5 * Math.exp(-distSq(lng, lat, 14.5, 13.5) / 120.0) +     // Lake Mega-Chad Pastoralists
                        5.5 * Math.exp(-distSq(lng, lat, 21.5, 17.0) / 120.0);     // Ennedi / Tibesti Cattle Herders

                    double densAmericasArchaic = 0.5 +
                        5.0 * Math.exp(-distSq(lng, lat, -79.2, -6.9) / 80.0) +     // Nanchoc Valley Peru (early irrigation)
                        4.5 * Math.exp(-distSq(lng, lat, -96.4, 16.9) / 80.0) +     // Guila Naquitz Oaxaca
                        4.0 * Math.exp(-distSq(lng, lat, -97.4, 18.4) / 80.0) +     // Tehuacan Valley
                        3.5 * Math.exp(-distSq(lng, lat, -88.0, 37.0) / 120.0);    // Eastern North America Archaic

                    dens = Math.max(densAnatoliaNeolithic, Math.max(densChinaNeolithic,
                           Math.max(densEuropeEEF, Math.max(densMehrgarhIndus,
                           Math.max(densGreenSaharaPastoral, densAmericasArchaic)))));
                } else if (year <= -4500) {
                    // -6,000 BP: Middle Neolithic / Ubaid Period & Green Sahara Optimum (Eridu, Yangshao, Vinča)
                    double densUbaid = 1.0 +
                        24.0 * Math.exp(-distSq(lng, lat, 45.99, 30.82) / 60.0) +   // Eridu Temple Core (Proto-Urban Ubaid)
                        20.0 * Math.exp(-distSq(lng, lat, 45.88, 31.25) / 60.0) +   // Tell el-'Oueili
                        18.0 * Math.exp(-distSq(lng, lat, 48.26, 32.19) / 70.0) +   // Susa I Susiana
                        16.0 * Math.exp(-distSq(lng, lat, 43.27, 36.52) / 70.0);   // Tepe Gawra Northern Ubaid

                    double densEgyptPredynastic = 0.9 +
                        18.0 * Math.exp(-distSq(lng, lat, 31.37, 26.99) / 70.0) +   // Badari Upper Egypt
                        16.5 * Math.exp(-distSq(lng, lat, 30.82, 30.33) / 70.0) +   // Merimde Beni Salama Delta
                        15.0 * Math.exp(-distSq(lng, lat, 32.78, 25.10) / 70.0);   // Hierakonpolis precursor

                    double densChinaYangshao = 0.9 +
                        18.5 * Math.exp(-distSq(lng, lat, 109.06, 34.27) / 70.0) +  // Banpo / Yangshao painted pottery
                        17.0 * Math.exp(-distSq(lng, lat, 111.30, 34.70) / 70.0) +  // Miaodigou Core
                        17.5 * Math.exp(-distSq(lng, lat, 121.38, 29.96) / 70.0) +  // Hemudu mature rice agriculture
                        14.0 * Math.exp(-distSq(lng, lat, 120.60, 30.80) / 80.0) +  // Majiabang
                        12.0 * Math.exp(-distSq(lng, lat, 119.50, 41.30) / 90.0);   // Hongshan Niuheliang

                    double densEuropeVinca = 0.8 +
                        16.0 * Math.exp(-distSq(lng, lat, 20.62, 44.76) / 60.0) +   // Vinča-Belo Brdo proto-urban tell
                        14.5 * Math.exp(-distSq(lng, lat, 21.36, 43.20) / 60.0) +   // Pločnik copper metallurgy
                        14.0 * Math.exp(-distSq(lng, lat, 27.00, 47.00) / 80.0) +   // Cucuteni-Trypillia early mega-sites
                        13.0 * Math.exp(-distSq(lng, lat, 16.50, 48.20) / 80.0) +   // Lengyel / mature LBK
                        12.0 * Math.exp(-distSq(lng, lat, -3.00, 47.60) / 70.0);   // Carnac / Atlantic Megalithic builders

                    double densIndusMehrgarh = 0.8 +
                        15.0 * Math.exp(-distSq(lng, lat, 68.05, 29.28) / 70.0) +   // Mehrgarh Period III-IV
                        12.0 * Math.exp(-distSq(lng, lat, 71.50, 28.50) / 80.0);   // Hakra Ware proto-Harappan

                    double densGreenSaharaOptimum = 0.7 +
                        12.0 * Math.exp(-distSq(lng, lat, 14.00, 13.50) / 100.0) +  // Lake Mega-Chad fishing & pastoralism
                        10.0 * Math.exp(-distSq(lng, lat, 9.00, 25.50) / 100.0) +   // Tassili n'Ajjer Bovidian rock art
                        9.0 * Math.exp(-distSq(lng, lat, 8.50, 18.00) / 100.0) +    // Air Mountains
                        8.5 * Math.exp(-distSq(lng, lat, -9.50, 18.50) / 100.0);   // Dhar Tichitt precursor

                    double densAmericasMiddle = 0.6 +
                        7.5 * Math.exp(-distSq(lng, lat, -77.50, -10.90) / 70.0) +  // Norte Chico / Caral precursor Peru
                        6.5 * Math.exp(-distSq(lng, lat, -92.10, 32.30) / 80.0) +   // Watson Brake earthen mounds Louisiana
                        6.0 * Math.exp(-distSq(lng, lat, -92.80, 15.20) / 80.0) +   // Chantuto shellmounds Chiapas
                        6.0 * Math.exp(-distSq(lng, lat, -80.70, -2.00) / 80.0);   // Valdivia precursor Ecuador

                    dens = Math.max(densUbaid, Math.max(densEgyptPredynastic,
                           Math.max(densChinaYangshao, Math.max(densEuropeVinca,
                           Math.max(densIndusMehrgarh, Math.max(densGreenSaharaOptimum, densAmericasMiddle))))));
                }

                dens *= weight;
                if (dens > 0.01) {
                    double logNorm = Math.log1p(dens * 5.0) / Math.log1p(45.0);
                    int gray = (int) Math.clamp(18.0 + logNorm * 237.0, 18.0, 255.0);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(x, y, 0x000000);
                }
            }
        }
        return img;
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

    public static void ensureAllScenarioMapsGenerated() {
        ensureAllScenarioMapsGenerated(false);
    }

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

    public static BufferedImage rasterizeDensityMap(String type, Scenario scenario) {
        return rasterizeDensityMapForYear(type, scenario, (scenario != null) ? scenario.getStartDateYear() : -10000);
    }

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

    // --- 2. SOVEREIGNTY TENSOR MAP ---
    private static final int[] COLORS_SOVEREIGNTY_100K = {
        0xD35400, // Clade 0: Proto-Sapiens Pan-African Domain (#D35400)
        0xF39C12, // Clade 1: Out-of-Africa Dispersal Domain (#F39C12)
        0x1F618D, // Clade 2: Western Mousterian Clan Territories (#1F618D)
        0x1A5276, // Clade 3: Zagros & Near East Mousterian Domain (#1A5276)
        0x229954, // Clade 4: Denisovan Central Asian Domain (#229954)
        0x7D3C98  // Clade 5: Eastern Archaic Sunda Domain (#7D3C98)
    };

    private static final int[] COLORS_ISOGLOSS_100K = {
        0xE67E22, // Clade 0: Sapiens African Core (#E67E22)
        0xF39C12, // Clade 1: Sapiens Pioneers Levant & Jebel Faya (#F39C12)
        0x2980B9, // Clade 2: Neanderthal Western Classical Mousterian (#2980B9)
        0x1F4788, // Clade 3: Neanderthal Zagros & Near East / Shanidar (#1F4788)
        0x27AE60, // Clade 4: Denisovans Altai & Siberian (#27AE60)
        0x8E44AD  // Clade 5: Eastern Archaic & Sundaland (#8E44AD)
    };

    private static final int[] COLORS_KINSHIP_100K = {
        0xE74C3C, // Clade 0: Bilateral / Multi-Band Foragers (#E74C3C)
        0xF39C12, // Clade 1: Pioneer Coastal Dispersal Bands (#F39C12)
        0x3498DB, // Clade 2: Patrilocal Small Neanderthal Clades (#3498DB)
        0x2980B9, // Clade 3: Zagros Highland Cave Kin-Groups (#2980B9)
        0x2ECC71, // Clade 4: Cold-Adapted Steppe Foragers (#2ECC71)
        0x9B59B6  // Clade 5: Tropical Forest & Bamboo Bands (#9B59B6)
    };

    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        if (w == null || occWeight <= 0.001) return 0x000000;
        double r = 0.0, g = 0.0, b = 0.0;
        for (int i = 0; i < 6; i++) {
            int c = cladeColors[i];
            r += w[i] * ((c >> 16) & 0xFF);
            g += w[i] * ((c >> 8) & 0xFF);
            b += w[i] * (c & 0xFF);
        }
        int ir = Math.clamp((int) Math.round(r * occWeight), 0, 255);
        int ig = Math.clamp((int) Math.round(g * occWeight), 0, 255);
        int ib = Math.clamp((int) Math.round(b * occWeight), 0, 255);
        return (ir << 16) | (ig << 8) | ib;
    }

    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        List<double[]> empireCores = new ArrayList<>();
        if (year <= -10500L) {
            // -10,900 BP (Younger Dryas): Natufian, Clovis, Magdalenian core territories
            empireCores.add(new double[]{35.5, 33.0, 180.0, 8.0});   // Natufian Levant Core (Ain Mallaha / Hayonim)
            empireCores.add(new double[]{41.5, 38.1, 160.0, 8.0});   // Upper Tigris proto-sedentism
            empireCores.add(new double[]{1.0, 45.0, 170.0, 10.0});   // Franco-Cantabrian Magdalenian/Azilian
            empireCores.add(new double[]{-103.3, 34.3, 160.0, 14.0});// Clovis High Plains
            empireCores.add(new double[]{-97.7, 30.9, 150.0, 12.0}); // Clovis Gault / Texas Core
            empireCores.add(new double[]{-73.2, -41.5, 150.0, 10.0});// Monte Verde South American Core
            empireCores.add(new double[]{114.0, 34.5, 160.0, 12.0}); // Yellow River Late Paleolithic
            empireCores.add(new double[]{139.5, 35.7, 150.0, 8.0});  // Incipient Jomon Japan
        } else if (year <= -9000L) {
            // -10,000 BP (Early Holocene): Göbekli Tepe Sanctuary, Jericho PPNA, Peiligang
            empireCores.add(new double[]{38.92, 37.22, 210.0, 9.0}); // Göbekli Tepe / Karahan Tepe Monumental Hearth
            empireCores.add(new double[]{35.44, 31.87, 195.0, 8.0}); // Jericho PPNA (Tell es-Sultan)
            empireCores.add(new double[]{38.10, 35.90, 185.0, 8.0}); // Mureybet / Jerf el Ahmar
            empireCores.add(new double[]{113.6, 34.4, 180.0, 10.0});  // Peiligang Yellow River Proto-Farming
            empireCores.add(new double[]{120.0, 29.5, 175.0, 10.0});  // Shangshan Yangtze Rice Core
            empireCores.add(new double[]{22.0, 44.5, 170.0, 8.0});   // Lepenski Vir Danube
            empireCores.add(new double[]{-103.0, 36.0, 155.0, 12.0});// Folsom Great Plains
            empireCores.add(new double[]{-79.3, -7.7, 150.0, 10.0});  // Paiján Peru
        } else if (year <= -7000L) {
            // -8,000 BP (Early Neolithic / 8.2 ka Event): Çatalhöyük, Jiahu, Mehrgarh, Early European Farmers
            empireCores.add(new double[]{32.83, 37.67, 230.0, 10.0});// Çatalhöyük Mega-Village Core
            empireCores.add(new double[]{35.95, 31.98, 205.0, 8.0}); // Ain Ghazal / Jordan
            empireCores.add(new double[]{113.6, 33.6, 215.0, 10.0});  // Jiahu / Yellow River Neolithic
            empireCores.add(new double[]{120.2, 30.1, 200.0, 10.0});  // Kuahuqiao / Yangtze
            empireCores.add(new double[]{22.8, 39.3, 205.0, 9.0});   // Sesklo / Thessaly Early European Farmers
            empireCores.add(new double[]{20.5, 44.8, 195.0, 10.0});  // Starčevo-Körös-Criş Danube Basin
            empireCores.add(new double[]{68.05, 29.28, 210.0, 10.0});// Mehrgarh Period I-II (Indus / Balochistan)
            empireCores.add(new double[]{30.58, 22.53, 190.0, 9.0}); // Nabta Playa Green Sahara Megalithic Core
            empireCores.add(new double[]{-79.2, -6.9, 165.0, 8.0});   // Nanchoc Valley Peru Early Canal Agriculture
            empireCores.add(new double[]{-96.4, 16.9, 160.0, 8.0});  // Guila Naquitz Mesoamerica
        } else if (year <= -4500L) {
            // -6,000 BP (Middle Neolithic / Ubaid Period & Green Sahara Optimum): Eridu, Yangshao, Vinča
            empireCores.add(new double[]{45.99, 30.82, 245.0, 12.0});// Eridu Temple / Ubaid Proto-Urban Core
            empireCores.add(new double[]{48.26, 32.19, 230.0, 10.0});// Susa I Susiana
            empireCores.add(new double[]{31.37, 26.99, 225.0, 10.0});// Badari Upper Egypt
            empireCores.add(new double[]{30.82, 30.33, 220.0, 10.0});// Merimde Nile Delta
            empireCores.add(new double[]{109.06, 34.27, 235.0, 12.0});// Banpo / Yangshao Painted Pottery Civilization
            empireCores.add(new double[]{121.38, 29.96, 230.0, 12.0});// Hemudu Wet-Rice Civilization
            empireCores.add(new double[]{20.62, 44.76, 230.0, 11.0});// Vinča-Belo Brdo Copper Age Tell
            empireCores.add(new double[]{-3.00, 47.60, 210.0, 10.0}); // Carnac / Atlantic Megalithic Heartland
            empireCores.add(new double[]{68.05, 29.28, 225.0, 11.0});// Mehrgarh Period III-IV
            empireCores.add(new double[]{14.00, 13.50, 195.0, 14.0});// Lake Mega-Chad Pastoral Federation
            empireCores.add(new double[]{-77.50, -10.90, 185.0, 8.0});// Norte Chico / Caral Precursor
            empireCores.add(new double[]{-92.10, 32.30, 175.0, 8.0}); // Watson Brake Mound Builders
        } else if (year <= -2000L) {
            empireCores.add(new double[]{31.2, 29.9, 240.0, 12.0});  // Memphis / Egypt
            empireCores.add(new double[]{44.4, 32.5, 235.0, 10.0});  // Ur / Babylon
            empireCores.add(new double[]{114.3, 34.8, 220.0, 10.0}); // Erlitou / Early China
            empireCores.add(new double[]{71.5, 27.5, 210.0, 10.0});  // Mohenjo-Daro / Harappa
        } else if (year <= -500L) {
            empireCores.add(new double[]{31.2, 30.0, 240.0, 15.0});  // Egypt
            empireCores.add(new double[]{44.4, 32.5, 240.0, 15.0});  // Babylon / Assyria
            empireCores.add(new double[]{52.9, 29.9, 235.0, 18.0});  // Persepolis / Persia
            empireCores.add(new double[]{108.9, 34.3, 235.0, 15.0}); // Chang'an / Zhou China
            empireCores.add(new double[]{85.1, 25.6, 230.0, 15.0});  // Magadha / Pataliputra
            empireCores.add(new double[]{23.7, 37.9, 220.0, 8.0});   // Athens / Greece
            empireCores.add(new double[]{-94.5, 18.0, 180.0, 8.0});  // Olmec Core
        } else if (year <= 500L) {
            empireCores.add(new double[]{12.5, 41.9, 255.0, 24.0});  // Rome / Pax Romana
            empireCores.add(new double[]{28.9, 41.0, 245.0, 20.0});  // Constantinople
            empireCores.add(new double[]{108.9, 34.3, 250.0, 22.0}); // Han / Jin Chang'an
            empireCores.add(new double[]{85.1, 25.6, 240.0, 18.0});  // Pataliputra / Gupta
            empireCores.add(new double[]{44.6, 33.1, 235.0, 16.0});  // Ctesiphon / Sasanian
            empireCores.add(new double[]{-89.0, 17.2, 200.0, 8.0});  // Classic Maya
            empireCores.add(new double[]{-98.8, 19.7, 210.0, 8.0});  // Teotihuacan
        } else if (year <= 1400L) {
            empireCores.add(new double[]{114.3, 34.8, 255.0, 22.0}); // Kaifeng / Song & Yuan Dadu
            empireCores.add(new double[]{44.4, 33.3, 245.0, 20.0});  // Abbasid Baghdad
            empireCores.add(new double[]{28.9, 41.0, 230.0, 14.0});  // Byzantine Constantinople
            empireCores.add(new double[]{2.3, 48.8, 225.0, 14.0});   // Kingdom of France / Paris
            empireCores.add(new double[]{-0.1, 51.5, 220.0, 12.0});  // Kingdom of England / London
            empireCores.add(new double[]{77.2, 28.6, 235.0, 16.0});  // Delhi Sultanate
            empireCores.add(new double[]{-8.0, 12.5, 220.0, 15.0});  // Mali Empire / Niani
            empireCores.add(new double[]{135.8, 35.0, 225.0, 10.0}); // Heian-kyo / Kyoto
            empireCores.add(new double[]{-89.0, 20.5, 210.0, 8.0});  // Postclassic Maya / Chichen Itza
            empireCores.add(new double[]{-68.7, -16.5, 205.0, 8.0}); // Tiwanaku / Wari
        } else if (year <= 1700L) {
            empireCores.add(new double[]{116.4, 39.9, 255.0, 24.0}); // Ming / Qing Beijing
            empireCores.add(new double[]{28.9, 41.0, 250.0, 22.0});  // Ottoman Istanbul
            empireCores.add(new double[]{77.2, 28.6, 245.0, 20.0});  // Mughal Delhi/Agra
            empireCores.add(new double[]{51.7, 32.6, 240.0, 16.0});  // Safavid Isfahan
            empireCores.add(new double[]{2.3, 48.8, 240.0, 16.0});   // France / Paris
            empireCores.add(new double[]{-0.1, 51.5, 240.0, 15.0});  // Britain / London
            empireCores.add(new double[]{-3.7, 40.4, 240.0, 18.0});  // Spain / Madrid
            empireCores.add(new double[]{139.7, 35.7, 240.0, 12.0}); // Tokugawa Edo
            empireCores.add(new double[]{37.6, 55.7, 235.0, 20.0});  // Tsardom of Russia / Moscow
            if (year < 1492L) {
                empireCores.add(new double[]{-99.1, 19.4, 235.0, 10.0}); // Aztec Tenochtitlan
                empireCores.add(new double[]{-71.9, -13.5, 240.0, 18.0}); // Inca Cuzco
            }
        } else {
            empireCores.add(new double[]{-77.0, 38.9, 255.0, 30.0}); // USA
            empireCores.add(new double[]{116.4, 39.9, 255.0, 30.0}); // China
            empireCores.add(new double[]{37.6, 55.7, 255.0, 32.0});  // Russia
            empireCores.add(new double[]{77.2, 28.6, 250.0, 22.0});  // India
            empireCores.add(new double[]{2.3, 48.8, 250.0, 18.0});   // EU / Western Europe
            empireCores.add(new double[]{-47.9, -15.8, 240.0, 22.0}); // Brazil
            empireCores.add(new double[]{149.1, -35.3, 235.0, 25.0}); // Australia
            empireCores.add(new double[]{31.2, 30.0, 235.0, 16.0});  // North Africa / Middle East
            empireCores.add(new double[]{139.7, 35.7, 250.0, 14.0}); // Japan
            empireCores.add(new double[]{9.0, 7.5, 230.0, 18.0});    // West Africa
            empireCores.add(new double[]{28.0, -26.0, 230.0, 16.0}); // Southern Africa
        }

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double[] w = computeHomininCladeWeights(lon, lat);
                    int rgb = blendCladeRgb(w, COLORS_SOVEREIGNTY_100K, occWeight);
                    img.setRGB(x, y, rgb);
                    continue;
                }

                double maxGov = 25.0;
                for (double[] ec : empireCores) {
                    double d2 = distSq(lon, lat, ec[0], ec[1]);
                    double sigma = ec[3];
                    double gov = ec[2] * Math.exp(-d2 / (2.0 * sigma * sigma));
                    maxGov = Math.max(maxGov, gov);
                }

                int gray = Math.clamp((int) (maxGov * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 3. ISOGLOSS TENSOR MAP ---
    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        double[][] languageCenters = {
            {15.0, 50.0, 210.0, 25.0},   // Indo-European (European)
            {75.0, 25.0, 218.0, 25.0},   // Indo-European (Indo-Iranian)
            {110.0, 32.0, 240.0, 28.0},  // Sino-Tibetan
            {40.0, 22.0, 180.0, 25.0},   // Afroasiatic (Semitic/Berber)
            {78.0, 13.0, 195.0, 20.0},   // Dravidian
            {60.0, 60.0, 160.0, 30.0},   // Uralic
            {85.0, 48.0, 175.0, 35.0},   // Turkic / Altaic
            {105.0, 48.0, 170.0, 25.0},  // Mongolic
            {138.0, 36.0, 225.0, 20.0},  // Japonic
            {127.0, 37.0, 220.0, 18.0},  // Koreanic
            {102.0, 16.0, 185.0, 20.0},  // Austroasiatic / Tai-Kadai
            {15.0, 5.0, 140.0, 28.0},    // Niger-Congo (Bantu/Volta)
            {28.0, 10.0, 150.0, 22.0},   // Nilo-Saharan
            {20.0, -25.0, 105.0, 22.0},  // Khoisan
            {120.0, 0.0, 190.0, 30.0},   // Austronesian
            {142.0, -5.0, 155.0, 18.0},  // Trans-New Guinea
            {134.0, -24.0, 130.0, 28.0}, // Pama-Nyungan
            {-100.0, 42.0, 90.0, 30.0},  // North American Amerind
            {-120.0, 58.0, 75.0, 25.0},  // Na-Dene
            {-95.0, 65.0, 60.0, 30.0},   // Eskimo-Aleut
            {-92.0, 16.0, 115.0, 18.0},  // Mayan / Mesoamerican
            {-75.0, -12.0, 120.0, 22.0}, // Quechumaran
            {-55.0, -15.0, 85.0, 28.0}   // Tupi-Guarani
        };

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double[] w = computeHomininCladeWeights(lon, lat);
                    int rgb = blendCladeRgb(w, COLORS_ISOGLOSS_100K, occWeight);
                    img.setRGB(x, y, rgb);
                    continue;
                }

                double wSum = 0.0;
                double valSum = 0.0;
                for (double[] lc : languageCenters) {
                    double d2 = distSq(lon, lat, lc[0], lc[1]);
                    double sigma = lc[3];
                    double w = Math.exp(-d2 / (2.0 * sigma * sigma));
                    wSum += w;
                    valSum += w * lc[2];
                }

                double langVal = (wSum > 0.0001) ? (valSum / wSum) : 128.0;
                int gray = Math.clamp((int) (langVal * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 4. KINSHIP TENSOR MAP ---
    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        double[][] kinshipCenters = {
            {10.0, 50.0, 195.0, 35.0},   // Western Europe Bilateral
            {35.0, 55.0, 215.0, 38.0},   // Slavic Patrilineal Clan
            {45.0, 32.0, 225.0, 30.0},   // Near East Segmentary Lineage
            {78.0, 22.0, 230.0, 32.0},   // Indo-Aryan Gotra Patrilineal
            {112.0, 34.0, 220.0, 35.0},  // Chinese Agnation/Zongzu Clan
            {138.0, 36.0, 205.0, 25.0},  // Japanese Ie Stem Family
            {65.0, 48.0, 210.0, 40.0},   // Central Asian Nomadic Clan
            {20.0, 5.0, 155.0, 30.0},    // Central Bantu Patrilineal
            {25.0, -10.0, 135.0, 25.0},  // Matrilineal Belt (Zambia/Congo)
            {5.0, 8.0, 145.0, 25.0},     // West African Segmentary
            {22.0, -25.0, 115.0, 25.0},  // Khoisan Band Kinship
            {38.0, 5.0, 160.0, 25.0},    // East African Age-Set
            {-76.0, 43.0, 95.0, 22.0},   // Iroquois Matrilineal Clan
            {-100.0, 45.0, 115.0, 28.0}, // Plains Bilateral/Patrilineal
            {-110.0, 35.0, 85.0, 20.0},  // Pueblo Matrilocal Clan
            {-125.0, 52.0, 135.0, 22.0}, // Pacific Northwest Potlatch
            {-90.0, 32.0, 105.0, 22.0},  // Muskogean Matrilineal
            {-65.0, -3.0, 75.0, 30.0},   // Amazonian Moiety
            {-74.0, -13.0, 125.0, 25.0}, // Andean Ayllu Bilateral
            {-50.0, -22.0, 90.0, 25.0},  // Tupi-Guarani Lineage
            {-68.0, -45.0, 65.0, 25.0},  // Patagonian Nomadic Band
            {133.0, -25.0, 235.0, 30.0}, // Australian 8-Skin Subsection
            {145.0, -5.0, 165.0, 20.0},  // New Guinea Segmentary Clan
            {175.0, -20.0, 145.0, 25.0}  // Polynesian Ramage
        };

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double[] w = computeHomininCladeWeights(lon, lat);
                    int rgb = blendCladeRgb(w, COLORS_KINSHIP_100K, occWeight);
                    img.setRGB(x, y, rgb);
                    continue;
                }

                double wSum = 0.0;
                double valSum = 0.0;
                for (double[] kc : kinshipCenters) {
                    double d2 = distSq(lon, lat, kc[0], kc[1]);
                    double sigma = kc[3];
                    double w = Math.exp(-d2 / (2.0 * sigma * sigma));
                    wSum += w;
                    valSum += w * kc[2];
                }

                double kinVal = (wSum > 0.0001) ? (valSum / wSum) : 128.0;
                int gray = Math.clamp((int) (kinVal * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 5. RITUALS TENSOR MAP ---
    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        List<double[]> ritualCenters = new ArrayList<>();
        if (year <= -70000L) {
            // Handled separately below via Paleolithic traits
        } else if (year <= -10500L) {
            // -10,900 BP: Natufian mortars/skulls, Magdalenian cave sanctuaries, Clovis ochre caches
            ritualCenters.add(new double[]{35.58, 33.08, 210.0, 6.0}); // Ain Mallaha Natufian burials
            ritualCenters.add(new double[]{35.22, 32.92, 195.0, 6.0}); // Hayonim Cave
            ritualCenters.add(new double[]{1.0, 45.0, 190.0, 8.0});    // Lascaux / Laugerie late sanctuary
            ritualCenters.add(new double[]{-4.5, 43.4, 185.0, 8.0});   // Altamira Late Magdalenian
            ritualCenters.add(new double[]{-103.3, 34.3, 160.0, 10.0});// Clovis Anzick / Blackwater red ochre caches
            ritualCenters.add(new double[]{-73.2, -41.5, 150.0, 8.0}); // Monte Verde hearths
        } else if (year <= -9000L) {
            // -10,000 BP: Göbekli Tepe & Karahan Tepe T-shaped pillar megalithic sanctuaries
            ritualCenters.add(new double[]{38.92, 37.22, 255.0, 8.0}); // Göbekli Tepe Megalithic Sanctuary Core
            ritualCenters.add(new double[]{39.50, 37.05, 240.0, 8.0}); // Karahan Tepe
            ritualCenters.add(new double[]{35.44, 31.87, 220.0, 7.0}); // Jericho Plastered Skull Sanctuary
            ritualCenters.add(new double[]{38.20, 36.40, 210.0, 7.0}); // Jerf el Ahmar Communal Rotunda
            ritualCenters.add(new double[]{22.0, 44.5, 200.0, 8.0});   // Lepenski Vir Fish-God Shrines
            ritualCenters.add(new double[]{113.6, 34.4, 180.0, 10.0}); // Peiligang ancestral burials
        } else if (year <= -7000L) {
            // -8,000 BP: Çatalhöyük Bull Shrines, Nabta Playa Megalithic Calendar, Jiahu Flutes
            ritualCenters.add(new double[]{32.83, 37.67, 255.0, 9.0}); // Çatalhöyük Bucrania & Goddess Shrines
            ritualCenters.add(new double[]{30.58, 22.53, 245.0, 9.0}); // Nabta Playa Megalithic Astronomical Stone Circle
            ritualCenters.add(new double[]{113.6, 33.6, 230.0, 9.0});  // Jiahu Sacred Crane Bone Flutes & Burials
            ritualCenters.add(new double[]{35.95, 31.98, 220.0, 7.0}); // Ain Ghazal Plaster Statues
            ritualCenters.add(new double[]{22.8, 39.3, 210.0, 8.0});   // Sesklo Goddess Figurines
            ritualCenters.add(new double[]{68.05, 29.28, 205.0, 8.0}); // Mehrgarh Terracotta Figurines
        } else if (year <= -4500L) {
            // -6,000 BP: Eridu Temple VII, Vinča Figurines, Carnac Megalithic Alignments
            ritualCenters.add(new double[]{45.99, 30.82, 255.0, 10.0});// Eridu Enki Temple / Proto-Ziggurat
            ritualCenters.add(new double[]{20.62, 44.76, 240.0, 9.0}); // Vinča Anthropomorphic Cult Statues
            ritualCenters.add(new double[]{-3.00, 47.60, 245.0, 9.0}); // Carnac Megalithic Alignments & Bougon Tumulus
            ritualCenters.add(new double[]{109.06, 34.27, 235.0, 9.0});// Yangshao Banpo Mortuary Complexes
            ritualCenters.add(new double[]{119.50, 41.30, 240.0, 8.0});// Hongshan Niuheliang Goddess Temple & Jade Altars
            ritualCenters.add(new double[]{31.37, 26.99, 220.0, 8.0}); // Badari Cosmetic Palettes & Burials
        } else {
            ritualCenters.add(new double[]{35.2, 31.8, 255.0, 20.0});  // Jerusalem
            ritualCenters.add(new double[]{39.8, 21.4, 250.0, 20.0});  // Mecca
            ritualCenters.add(new double[]{12.5, 41.9, 245.0, 18.0});  // Rome / Vatican
            ritualCenters.add(new double[]{83.0, 25.3, 255.0, 20.0});  // Varanasi / Ganges
            ritualCenters.add(new double[]{91.1, 29.6, 240.0, 18.0});  // Lhasa / Mount Kailash
            ritualCenters.add(new double[]{117.1, 36.3, 240.0, 18.0}); // Mount Tai / Qufu
            ritualCenters.add(new double[]{138.7, 35.4, 230.0, 15.0}); // Mount Fuji / Ise
            ritualCenters.add(new double[]{32.6, 25.7, 245.0, 14.0});  // Karnak / Luxor
            ritualCenters.add(new double[]{44.4, 32.5, 235.0, 14.0});  // Babylon / Eridu
            ritualCenters.add(new double[]{103.9, 13.4, 240.0, 16.0}); // Angkor Wat
            ritualCenters.add(new double[]{110.2, -7.6, 235.0, 14.0}); // Borobudur
            ritualCenters.add(new double[]{131.0, -25.3, 245.0, 22.0});// Uluru / Kata Tjuta
            ritualCenters.add(new double[]{-98.8, 19.7, 245.0, 16.0}); // Teotihuacan / Cholula
            ritualCenters.add(new double[]{-71.9, -13.5, 245.0, 18.0});// Coricancha / Cuzco
            ritualCenters.add(new double[]{-68.7, -16.5, 240.0, 16.0});// Tiwanaku
            ritualCenters.add(new double[]{-1.8, 51.2, 230.0, 12.0});  // Stonehenge
            ritualCenters.add(new double[]{22.5, 38.5, 235.0, 12.0});  // Delphi
            ritualCenters.add(new double[]{30.9, -20.3, 225.0, 16.0}); // Great Zimbabwe
        }

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double baseRitual = blendPaleoTraits(lon, lat, 55.0, 48.0, 38.0);
                    double maxRitual = baseRitual;
                    double[][] paleoSites = {
                        {21.2, -34.4, 160.0, 10.0}, {22.1, -34.2, 155.0, 10.0}, {31.9, -27.0, 150.0, 10.0},
                        {35.3, 32.7, 145.0, 8.0},   {-8.8, 31.6, 140.0, 8.0},   {-5.3, 36.1, 135.0, 8.0},
                        {1.0, 45.0, 130.0, 8.0},    {15.8, 46.1, 125.0, 8.0},   {44.2, 36.8, 130.0, 8.0},
                        {84.0, 51.4, 130.0, 8.0},   {102.8, 35.2, 120.0, 8.0}
                    };
                    for (double[] ss : paleoSites) {
                        double d2 = distSq(lon, lat, ss[0], ss[1]);
                        double sigma = ss[3];
                        double val = ss[2] * Math.exp(-d2 / (2.0 * sigma * sigma));
                        maxRitual = Math.max(maxRitual, val);
                    }
                    int gray = Math.clamp((int) (maxRitual * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                double maxRitual = 30.0;
                for (double[] ss : ritualCenters) {
                    double d2 = distSq(lon, lat, ss[0], ss[1]);
                    double sigma = ss[3];
                    double val = ss[2] * Math.exp(-d2 / (2.0 * sigma * sigma));
                    maxRitual = Math.max(maxRitual, val);
                }

                int gray = Math.clamp((int) (maxRitual * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 6. TECHNOLOGY & SUBSISTENCE TENSOR MAP ---
    private static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        double baseTech = 25.0;
        if (year <= -10000L) baseTech = 35.0;
        else if (year <= -7000L) baseTech = 55.0;
        else if (year <= -4500L) baseTech = 80.0;
        else if (year <= 0L) baseTech = 110.0;
        else if (year <= 1400L) baseTech = 140.0;
        else if (year <= 1800L) baseTech = 180.0;
        else if (year <= 1950L) baseTech = 220.0;
        else baseTech = 245.0;

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double techVal = blendPaleoTraits(lon, lat, 130.0, 112.0, 92.0);
                    int gray = Math.clamp((int) (techVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                double tech = baseTech;
                double fertCrescent = Math.exp(-(Math.pow(lat - 36.0, 2) + Math.pow(lon - 38.0, 2)) / 70.0);
                double yellowRiver = Math.exp(-(Math.pow(lat - 34.0, 2) + Math.pow(lon - 113.0, 2)) / 80.0);
                double yangtze = Math.exp(-(Math.pow(lat - 30.0, 2) + Math.pow(lon - 120.0, 2)) / 80.0);
                double balkans = Math.exp(-(Math.pow(lat - 44.0, 2) + Math.pow(lon - 21.0, 2)) / 70.0);
                double indus = Math.exp(-(Math.pow(lat - 29.0, 2) + Math.pow(lon - 68.0, 2)) / 70.0);
                double meso = Math.exp(-(Math.pow(lat - 18.0, 2) + Math.pow(lon - (-97.0), 2)) / 60.0);
                double andes = Math.exp(-(Math.pow(lat - (-8.0), 2) + Math.pow(lon - (-78.0), 2)) / 60.0);

                if (year <= -4500L) {
                    // Early agricultural / metallurgy emergence
                    tech += (fertCrescent * 70.0 + yellowRiver * 60.0 + yangtze * 60.0 + balkans * 55.0 + indus * 50.0 + meso * 30.0 + andes * 30.0);
                } else if (year <= 0) {
                    double europe = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 100.0);
                    tech += (fertCrescent * 50.0 + yellowRiver * 45.0 + europe * 30.0 + meso * 25.0 + andes * 25.0);
                } else if (year <= 1400) {
                    double europe = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 100.0);
                    tech += (yellowRiver * 65.0 + fertCrescent * 50.0 + europe * 40.0 + meso * 30.0 + andes * 30.0);
                } else if (year <= 1850) {
                    double europe = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 100.0);
                    tech += (europe * 70.0 + yellowRiver * 45.0);
                } else {
                    double europe = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 100.0);
                    tech += (europe * 30.0 + yellowRiver * 30.0);
                }

                int gray = Math.clamp((int) (tech * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 7. TRADE NETWORK TENSOR MAP ---
    private static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (year <= -70000L) {
            // Paleolithic Raw Material Circuits
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {36.4, -1.5}, {38.5, 8.5}}, new Color(175, 175, 175), 2.0);
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}}, new Color(165, 165, 165), 2.0);
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {35.3, 32.7}, {35.6, 33.0}}, new Color(170, 170, 170), 2.0);
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(155, 155, 155), 2.0);
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 45.0}, {0.3, 45.2}}, new Color(160, 160, 160), 2.0);
            drawTradeRoute(g, new double[][]{{9.5, 48.5}, {13.0, 47.8}, {15.8, 46.1}, {17.1, 49.2}}, new Color(160, 160, 160), 2.0);
            drawTradeRoute(g, new double[][]{{34.2, 44.9}, {38.5, 44.5}, {40.2, 44.2}}, new Color(150, 150, 150), 2.0);
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {47.1, 34.4}}, new Color(150, 150, 150), 2.0);
            drawTradeRoute(g, new double[][]{{83.9, 51.4}, {85.5, 51.7}}, new Color(150, 150, 150), 2.0);
            drawTradeRoute(g, new double[][]{{114.5, 40.2}, {115.9, 39.7}}, new Color(150, 150, 150), 2.0);
        } else if (year <= -10500L) {
            // -10,900 BP: Younger Dryas / Natufian Flint & Marine Shell Transfers / Clovis Chert
            drawTradeRoute(g, new double[][]{{35.2, 32.9}, {35.6, 33.1}, {36.5, 34.2}}, new Color(175, 175, 175), 2.5); // Natufian Dentalium & Flint
            drawTradeRoute(g, new double[][]{{-103.3, 34.3}, {-101.5, 35.5}, {-97.7, 30.9}}, new Color(165, 165, 165), 2.5); // Clovis Alibates / Texas Chert
            drawTradeRoute(g, new double[][]{{-1.0, 44.5}, {1.0, 45.0}, {3.0, 46.5}}, new Color(165, 165, 165), 2.5); // Magdalenian Silex
            drawTradeRoute(g, new double[][]{{138.5, 35.0}, {139.5, 35.7}}, new Color(160, 160, 160), 2.0); // Jomon Obsidian
        } else if (year <= -9000L) {
            // -10,000 BP: Göbekli Tepe Anatolian Obsidian & PPNA Exchange
            drawTradeRoute(g, new double[][]{{34.5, 38.0}, {38.9, 37.2}, {38.1, 35.9}, {35.4, 31.9}}, new Color(195, 195, 195), 3.0); // Göllü Dağ Obsidian to Göbekli & Jericho
            drawTradeRoute(g, new double[][]{{41.5, 38.8}, {39.7, 38.2}, {44.0, 36.0}}, new Color(185, 185, 185), 2.5); // Bingöl Obsidian to Tigris/Zagros
            drawTradeRoute(g, new double[][]{{24.4, 36.7}, {23.1, 37.4}}, new Color(180, 180, 180), 2.5); // Melos Obsidian to Franchthi Cave (Aegean maritime)
            drawTradeRoute(g, new double[][]{{113.6, 34.4}, {116.0, 35.0}}, new Color(175, 175, 175), 2.5); // Peiligang Jade & Stone
        } else if (year <= -7000L) {
            // -8,000 BP: Early Neolithic Çatalhöyük Obsidian, Spondylus Shells, Mehrgarh Lapis
            drawTradeRoute(g, new double[][]{{34.5, 38.0}, {32.8, 37.7}, {35.0, 34.0}, {35.9, 32.0}}, new Color(210, 210, 210), 3.0); // Çatalhöyük Obsidian Corridor
            drawTradeRoute(g, new double[][]{{24.4, 36.7}, {22.8, 39.3}, {20.5, 44.8}, {16.0, 48.5}}, new Color(200, 200, 200), 3.0); // Spondylus Shell Route (Aegean to Danube/LBK)
            drawTradeRoute(g, new double[][]{{70.7, 36.2}, {68.0, 29.3}, {66.0, 26.0}}, new Color(195, 195, 195), 2.5); // Badakhshan Lapis Lazuli & Turquoise to Mehrgarh
            drawTradeRoute(g, new double[][]{{113.6, 33.6}, {120.2, 30.1}}, new Color(190, 190, 190), 2.5); // Jiahu - Yangtze Exchange
            drawTradeRoute(g, new double[][]{{30.6, 22.5}, {32.5, 25.5}}, new Color(185, 185, 185), 2.5); // Nabta Playa - Nile Valley
        } else if (year <= -4500L) {
            // -6,000 BP: Ubaid Maritime Gulf Routes, Vinča Copper, European Spondylus & Flint
            drawTradeRoute(g, new double[][]{{45.99, 30.82}, {48.5, 29.5}, {50.5, 26.0}, {56.0, 24.0}}, new Color(225, 225, 225), 3.5); // Ubaid Persian Gulf Maritime
            drawTradeRoute(g, new double[][]{{21.36, 43.20}, {20.62, 44.76}, {16.5, 48.2}, {8.5, 50.0}, {2.5, 49.0}}, new Color(215, 215, 215), 3.0); // Vinča Copper & Spondylus Network to Rhine
            drawTradeRoute(g, new double[][]{{-0.1, 46.4}, {-3.0, 47.6}, {-3.9, 48.7}}, new Color(205, 205, 205), 3.0); // Atlantic Megalithic Coastal Exchange
            drawTradeRoute(g, new double[][]{{109.06, 34.27}, {111.3, 34.7}, {121.4, 30.0}}, new Color(210, 210, 210), 3.0); // Yangshao - Hemudu Jade & Pottery
            drawTradeRoute(g, new double[][]{{31.37, 26.99}, {33.5, 28.0}, {34.5, 29.0}}, new Color(200, 200, 200), 2.5); // Badarian Red Sea Shell & Malachite
        } else {
            // Historical Trade Arteries
            drawTradeRoute(g, new double[][]{{115, 34}, {100, 38}, {75, 39}, {62, 37}, {44, 33}, {28, 41}}, new Color(240, 240, 240), 4.0); // Silk Road
            drawTradeRoute(g, new double[][]{{-4, 12}, {-1, 18}, {3, 27}, {10, 36}}, new Color(220, 220, 220), 3.5); // Trans-Saharan Gold & Salt
            drawTradeRoute(g, new double[][]{{45, 12}, {55, 24}, {75, 12}, {102, 2}, {115, -6}}, new Color(210, 210, 210), 3.5); // Indian Ocean Maritime
            drawTradeRoute(g, new double[][]{{6, 53}, {12, 48}, {24, 50}, {30, 60}}, new Color(200, 200, 200), 3.0); // Amber & Fur Corridors
            drawTradeRoute(g, new double[][]{{-77, -12}, {-72, -14}, {-68, -17}, {-65, -20}}, new Color(210, 210, 210), 3.5); // Inca Qhapaq Ñan
            drawTradeRoute(g, new double[][]{{-99, 19}, {-96, 17}, {-92, 15}, {-88, 14}}, new Color(200, 200, 200), 3.0); // Mesoamerican Trade Network
        }

        g.dispose();
        return applyAltimetryCoastlineMask(img);
    }

    private static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        g.setColor(col);
        g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < coords.length - 1; i++) {
            int x1 = (int) ((coords[i][0] + 180.0) / 360.0 * WIDTH);
            int y1 = (int) ((90.0 - coords[i][1]) / 180.0 * HEIGHT);
            int x2 = (int) ((coords[i + 1][0] + 180.0) / 360.0 * WIDTH);
            int y2 = (int) ((90.0 - coords[i + 1][1]) / 180.0 * HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    // --- 8. INSTITUTIONAL COMPLEXITY TENSOR MAP (SESHAT) ---
    private static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();
        List<CityPoint> cities = getCitiesForScenario(type, scenario);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -10000L) {
                    // Paleolithic Era: Egalitarian band & clan networks (Carneiro Scale Level 0 / score 18-24)
                    double instVal = 20.0;
                    if (year <= -70000L) {
                        instVal = blendPaleoTraits(lon, lat, 24.0, 18.0, 16.0);
                    } else if (year <= -40000L) {
                        instVal = 22.0; // MIS 3 Sahul & Eurasian clans
                    } else if (year <= -20000L) {
                        instVal = 24.0; // Gravettian / Solutrean complex alliances
                    }
                    int gray = Math.clamp((int) (instVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                // Holocene & Historical Eras: Seshat hierarchy scaling
                double maxInst = 25.0;
                for (CityPoint cp : cities) {
                    double d2 = distSq(lon, lat, cp.lng, cp.lat);
                    double val = cp.weight * Math.exp(-d2 / (2.0 * cp.sigma * cp.sigma)) * 55.0;
                    maxInst = Math.max(maxInst, val);
                }

                int gray = Math.clamp((int) (maxInst * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 9. ECOLOGICAL FOOTPRINT TENSOR MAP ---
    private static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -10000L) {
                    // Paleolithic hunter-gatherer metabolic footprint & fire-stick farming
                    double ecoVal = 20.0;
                    if (year <= -70000L) {
                        ecoVal = blendPaleoTraits(lon, lat, 26.0, 20.0, 18.0);
                    } else if (year <= -40000L) {
                        // Sahul anthropogenic mosaic burning (fire-stick farming)
                        double sahulFire = Math.exp(-(Math.pow(lat - (-22.0), 2) + Math.pow(lon - 135.0, 2)) / 250.0);
                        ecoVal = 22.0 + sahulFire * 45.0;
                    } else if (year <= -18000L) {
                        // Gravettian & LGM megafauna hunting pressure in periglacial plains
                        double mammothHunt = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 30.0, 2)) / 300.0);
                        ecoVal = 22.0 + mammothHunt * 35.0;
                    }
                    int gray = Math.clamp((int) (ecoVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                // Holocene Agricultural & Industrial Stress (Smooth 2D continuous Gaussians)
                double ecoStrain = 22.0;
                double mesopotamia = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lon - 44.0, 2)) / 45.0);
                double meditteranean = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 15.0, 2)) / 120.0);
                double yellowRiver = Math.exp(-(Math.pow(lat - 35.0, 2) + Math.pow(lon - 114.0, 2)) / 65.0);
                double ganges = Math.exp(-(Math.pow(lat - 26.0, 2) + Math.pow(lon - 82.0, 2)) / 60.0);
                double nwEurope = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 60.0);
                double mesoamerica = Math.exp(-(Math.pow(lat - 19.0, 2) + Math.pow(lon - (-99.0), 2)) / 40.0);
                double andes = Math.exp(-(Math.pow(lat - (-13.0), 2) + Math.pow(lon - (-72.0), 2)) / 40.0);

                ecoStrain += (mesopotamia * 180.0 + yellowRiver * 170.0 + ganges * 160.0 +
                              meditteranean * 140.0 + nwEurope * 135.0 + mesoamerica * 120.0 + andes * 120.0);

                int gray = Math.clamp((int) (ecoStrain * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
    }

    // --- 10. PATHOGEN IMMUNITY TENSOR MAP ---
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : WIDTH) / WIDTH), 0, (mask != null ? mask.getWidth() : WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HEIGHT) / HEIGHT), 0, (mask != null ? mask.getHeight() : HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -70000L) {
                    double absLat = Math.abs(lat);
                    double tropicalFactor = Math.max(0.0, Math.cos(Math.toRadians(Math.min(90.0, absLat * 2.8))));
                    double speciesBaseline = blendPaleoTraits(lon, lat, 38.0, 22.0, 26.0);
                    double pathogenVal = speciesBaseline + tropicalFactor * 135.0;
                    int gray = Math.clamp((int) (pathogenVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                // 1. Vector-borne tropical reservoir component (Malaria, Dengue, Yellow Fever, Trypanosomiasis)
                double absLat = Math.abs(lat);
                double tropicalFactor = Math.max(0.0, Math.cos(Math.toRadians(Math.min(90.0, absLat * 3.2))));
                double tropicalIntensity = tropicalFactor * 160.0;

                double amazon = Math.exp(-(Math.pow(lat - (-3.0), 2) + Math.pow(lon - (-60.0), 2)) / 180.0);
                double congo = Math.exp(-(Math.pow(lat - (0.0), 2) + Math.pow(lon - (22.0), 2)) / 140.0);
                double ganges = Math.exp(-(Math.pow(lat - (24.0), 2) + Math.pow(lon - (85.0), 2)) / 80.0);
                double niger = Math.exp(-(Math.pow(lat - (10.0), 2) + Math.pow(lon - (5.0), 2)) / 90.0);
                double mekong = Math.exp(-(Math.pow(lat - (14.0), 2) + Math.pow(lon - (105.0), 2)) / 80.0);
                tropicalIntensity += (amazon * 55.0 + congo * 65.0 + ganges * 60.0 + niger * 60.0 + mekong * 50.0);

                // 2. Old World Zoonotic Crowd Disease Reservoir (Smallpox, Measles, Plague, Cholera)
                boolean isOldWorld = lon >= -20.0 && lon <= 150.0 && lat >= -35.0 && lat <= 68.0;
                double crowdIntensity = 0.0;
                if (isOldWorld) {
                    double medit = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 15.0, 2)) / 160.0);
                    double china = Math.exp(-(Math.pow(lat - 34.0, 2) + Math.pow(lon - 114.0, 2)) / 140.0);
                    double india = Math.exp(-(Math.pow(lat - 22.0, 2) + Math.pow(lon - 78.0, 2)) / 120.0);
                    double mideast = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow(lon - 44.0, 2)) / 100.0);
                    double europe = Math.exp(-(Math.pow(lat - 48.0, 2) + Math.pow(lon - 10.0, 2)) / 120.0);
                    crowdIntensity = (medit * 75.0 + china * 90.0 + india * 85.0 + mideast * 80.0 + europe * 75.0);
                }

                // 3. Historical Pre-1492 Isolation of Americas & Oceania
                boolean isAmericas = lon <= -30.0 && lon >= -170.0;
                boolean isAustralasia = lat <= -10.0 && lon >= 110.0 && lon <= 180.0;
                double finalPathogen = 20.0;

                if (isAmericas || isAustralasia) {
                    if (year < 1492L) {
                        finalPathogen = 15.0 + (isAmericas ? amazon * 40.0 : 0.0);
                    } else {
                        double timeSinceContact = Math.min(100.0, year - 1492L);
                        double shockFactor = Math.min(1.0, timeSinceContact / 30.0);
                        finalPathogen = 15.0 + shockFactor * 210.0;
                    }
                } else {
                    finalPathogen = Math.min(255.0, 25.0 + tropicalIntensity * 0.6 + crowdIntensity * 0.7);
                }

                int gray = Math.clamp((int) (finalPathogen * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return applyAltimetryCoastlineMask(img);
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
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {-5.0, 36.0}, {0.0, 36.0}, {10.0, 38.0}, {15.0, 39.0}, {22.0, 38.0}, {30.0, 32.0}, {35.0, 32.0},
            {34.0, 35.0}, {26.0, 37.0}, {18.0, 40.0}, {12.0, 44.0}, {5.0, 43.0}, {0.0, 37.0}, {-5.0, 36.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {28.0, 41.0}, {32.0, 46.0}, {38.0, 46.0}, {41.0, 43.0}, {35.0, 41.0}, {28.0, 41.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {33.0, 27.0}, {35.0, 28.0}, {43.0, 12.0}, {40.0, 12.0}, {33.0, 27.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {48.0, 30.0}, {56.0, 27.0}, {54.0, 25.0}, {48.0, 29.0}
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

    public static void generateProceduralMapsForScenario(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    private static List<CityPoint> getCitiesForScenario(String type, Scenario scenario) {
        if (scenario == null || scenario.getInitialHumanCount() <= 0) {
            return getCitiesForScenario(type);
        }
        long pop = scenario.getInitialHumanCount();
        List<CityPoint> list = new ArrayList<>();

        if (pop >= 1_000_000_000L) { // 10^9+ Global Megacities (high tech urban hubs)
            list.add(new CityPoint("Tokyo Megacity", 35.6, 139.7, 5.5, 2.8));
            list.add(new CityPoint("New York Tri-State", 40.7, -74.0, 5.2, 2.6));
            list.add(new CityPoint("London Metro", 51.5, -0.1, 4.8, 2.4));
            list.add(new CityPoint("Shanghai Yangtze Hub", 31.2, 121.5, 5.5, 2.8));
            list.add(new CityPoint("Mumbai Metropolis", 19.0, 72.8, 5.2, 2.6));
            list.add(new CityPoint("Cairo Nile Megacity", 30.0, 31.2, 4.8, 2.4));
            list.add(new CityPoint("Lagos Gulf Corridor", 6.5, 3.3, 4.5, 2.2));
            list.add(new CityPoint("São Paulo Hub", -23.5, -46.6, 4.8, 2.4));
            list.add(new CityPoint("Beijing Capital Node", 39.9, 116.4, 5.2, 2.6));
            list.add(new CityPoint("Paris Isle Hub", 48.8, 2.35, 4.5, 2.2));
            list.add(new CityPoint("Mexico City Valley", 19.4, -99.1, 4.8, 2.4));
            list.add(new CityPoint("Sydney Pacific Hub", -33.8, 151.2, 4.0, 2.0));
        } else if (pop >= 100_000_000L) { // 10^8 Industrial / Modern Urban Networks
            list.add(new CityPoint("London", 51.5, -0.1, 4.5, 2.5));
            list.add(new CityPoint("Paris", 48.8, 2.35, 4.2, 2.2));
            list.add(new CityPoint("New York", 40.7, -74.0, 4.5, 2.5));
            list.add(new CityPoint("Tokyo", 35.6, 139.7, 4.8, 2.5));
            list.add(new CityPoint("Beijing", 39.9, 116.4, 4.5, 2.4));
            list.add(new CityPoint("Calcutta", 22.5, 88.3, 4.2, 2.2));
            list.add(new CityPoint("Cairo", 30.0, 31.2, 4.0, 2.0));
        } else if (pop >= 1_000_000L) { // 10^7 Imperial / Agrarian Networks
            list.add(new CityPoint("Rome", 41.9, 12.5, 4.2, 2.5));
            list.add(new CityPoint("Alexandria", 31.2, 29.9, 3.8, 2.2));
            list.add(new CityPoint("Chang'an", 34.2, 108.9, 4.2, 2.5));
            list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.0, 2.2));
            list.add(new CityPoint("Babylon", 32.5, 44.4, 3.8, 2.0));
        } else if (pop >= 100_000L) { // Early Agricultural Settlements
            list.add(new CityPoint("Uruk", 31.3, 45.6, 3.5, 2.0));
            list.add(new CityPoint("Memphis", 29.8, 31.2, 3.5, 2.0));
            list.add(new CityPoint("Mohenjo-Daro", 27.3, 68.1, 3.2, 2.0));
        } else { // Low / Hunter-Gatherer Band Dispersal (< 100k)
            list.add(new CityPoint("Omo", 4.5, 36.0, 2.0, 3.5));
            list.add(new CityPoint("Blombos", -34.4, 21.2, 1.8, 3.0));
            list.add(new CityPoint("Madjedbebe", -12.5, 132.9, 1.8, 3.0));
        }

        List<CityPoint> specific = getCitiesForScenario(type);
        if (specific != null && !specific.isEmpty() && pop < 1_000_000_000L) {
            for (CityPoint cp : specific) {
                boolean exists = false;
                for (CityPoint ex : list) {
                    if (distSq(cp.lng, cp.lat, ex.lng, ex.lat) < 25.0) {
                        exists = true; break;
                    }
                }
                if (!exists) list.add(cp);
            }
        }
        return list;
    }

    private static List<CityPoint> getCitiesForScenario(String type) {
        List<CityPoint> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                list.add(new CityPoint("Rome", 41.9, 12.5, 4.0, 2.5));
                list.add(new CityPoint("Alexandria", 31.2, 29.9, 3.0, 2.0));
                list.add(new CityPoint("Antioch", 36.2, 36.1, 2.5, 2.0));
                list.add(new CityPoint("Carthage", 36.8, 10.3, 2.2, 2.0));
                list.add(new CityPoint("Athens", 37.9, 23.7, 2.0, 1.8));
                list.add(new CityPoint("Constantinople", 41.0, 28.9, 2.5, 2.0));
                break;
            case "EGYPT_NILE":
                list.add(new CityPoint("Memphis", 29.8, 31.2, 3.5, 2.0));
                list.add(new CityPoint("Thebes", 25.7, 32.6, 3.0, 2.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
            case "YOUNGER_DRYAS":
                list.add(new CityPoint("Nineveh", 36.3, 43.1, 3.5, 2.0));
                list.add(new CityPoint("Babylon", 32.5, 44.4, 3.5, 2.0));
                list.add(new CityPoint("Jericho", 31.8, 35.4, 2.2, 1.5));
                break;
            case "INDIA_MAURYA":
                list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.0, 2.5));
                list.add(new CityPoint("Taxila", 33.7, 72.8, 3.0, 2.0));
                break;
            case "WEST_AFRICA_MALI":
            case "GREEN_SAHARA":
                list.add(new CityPoint("Timbuktu", 16.7, -3.0, 3.2, 2.0));
                list.add(new CityPoint("Gao", 16.2, 0.0, 2.5, 2.0));
                list.add(new CityPoint("Niani", 11.4, -8.3, 2.8, 2.0));
                break;
            case "SONG_DYNASTY":
                list.add(new CityPoint("Kaifeng", 34.7, 114.3, 4.0, 2.5));
                list.add(new CityPoint("Hangzhou", 30.2, 120.1, 3.8, 2.5));
                break;
            case "SAKOKU_JAPAN":
                list.add(new CityPoint("Edo", 35.6, 139.7, 4.0, 2.0));
                list.add(new CityPoint("Kyoto", 35.0, 135.7, 3.5, 2.0));
                list.add(new CityPoint("Osaka", 34.6, 135.5, 3.2, 2.0));
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
            case "EPIDEMIC_CONTACT":
                list.add(new CityPoint("Tenochtitlan", 19.4, -99.1, 4.0, 2.5));
                list.add(new CityPoint("Cuzco", -13.5, -71.9, 3.5, 2.5));
                list.add(new CityPoint("Tikal", 17.2, -89.6, 2.8, 2.0));
                break;
            case "SAHUL_MIGRATION":
                list.add(new CityPoint("Madjedbebe", -12.5, 132.9, 3.0, 2.5));
                list.add(new CityPoint("Lake Mungo", -33.7, 143.0, 2.5, 2.0));
                break;
            case "BERINGIA_AMERICAS":
                list.add(new CityPoint("Yana RHS", 70.7, 135.4, 3.0, 2.5));
                list.add(new CityPoint("Bluefish Caves", 67.1, -140.7, 2.5, 2.0));
                break;
            case "ONE_CONTINENT":
                // Pan-African Homo Sapiens archaeological centers (-100,000 BP)
                // Broad, interconnected African habitability zones with higher weights & sigmas
                list.add(new CityPoint("Omo Kibish (East Africa Core)", 4.5, 36.0, 4.5, 15.0));
                list.add(new CityPoint("Herto & Afar (Horn of Africa)", 8.9, 40.5, 4.2, 14.0));
                list.add(new CityPoint("Jebel Irhoud (North Africa)", 31.6, -8.8, 3.8, 15.0));
                list.add(new CityPoint("Blombos & Klasies (South Africa)", -34.4, 21.2, 3.8, 15.0));
                list.add(new CityPoint("Iho Eleru & Congo (West/Central Africa)", 7.4, 5.1, 3.5, 16.0));
                list.add(new CityPoint("Nile Corridor (African Bridge)", 22.0, 31.5, 4.0, 12.0));
                // Archaic Hominins (Sparse population densities, expanded spatial range)
                list.add(new CityPoint("Atapuerca & Spy (Néandertal Ouest)", 42.3, -3.5, 1.2, 12.0));
                list.add(new CityPoint("Shanidar (Néandertal Moyen-Orient)", 36.8, 44.2, 1.2, 12.0));
                list.add(new CityPoint("Denisova & Xiahe (Denisovien Altai)", 51.4, 84.7, 0.9, 13.0));
                break;
            default:
                list.add(new CityPoint("Tokyo", 35.6, 139.7, 4.0, 2.5));
                list.add(new CityPoint("New York", 40.7, -74.0, 3.5, 2.5));
                list.add(new CityPoint("London", 51.5, -0.1, 3.2, 2.0));
                break;
        }
        return list;
    }

    private static List<RiverRibbon> getRiversForScenario(String type) {
        List<RiverRibbon> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "EGYPT_NILE":
            case "ROMAN_EMPIRE":
                list.add(new RiverRibbon(32.0, 24.0, 31.5, 31.5, 2.5, 1.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
                list.add(new RiverRibbon(38.0, 37.0, 48.0, 30.0, 2.8, 1.2));
                break;
            case "INDIA_MAURYA":
                list.add(new RiverRibbon(78.0, 30.0, 90.0, 22.0, 3.0, 1.5));
                break;
            case "SONG_DYNASTY":
                list.add(new RiverRibbon(105.0, 35.0, 120.0, 35.0, 3.0, 1.5)); // Yellow River
                list.add(new RiverRibbon(105.0, 30.0, 121.0, 31.0, 3.0, 1.5)); // Yangtze
                break;
            case "WEST_AFRICA_MALI":
                list.add(new RiverRibbon(-10.0, 11.0, 3.0, 16.0, 2.2, 1.2));
                break;
            case "ONE_CONTINENT":
                list.add(new RiverRibbon(31.0, 3.0, 32.5, 31.0, 2.0, 1.5));
                break;
        }
        return list;
    }

    private static List<EmpireTerritory> getEmpiresForScenario(String type) {
        List<EmpireTerritory> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                EmpireTerritory rome = new EmpireTerritory("Imperium Romanum", new Color(220, 38, 38));
                rome.addBoundingBox(-9.0, 30.0, 45.0, 54.0);
                list.add(rome);
                break;
            case "INDIA_MAURYA":
                EmpireTerritory maurya = new EmpireTerritory("Empire Maurya", new Color(217, 119, 6));
                maurya.addBoundingBox(68.0, 8.0, 90.0, 34.0);
                list.add(maurya);
                break;
            case "WEST_AFRICA_MALI":
                EmpireTerritory mali = new EmpireTerritory("Empire du Mali", new Color(16, 185, 129));
                mali.addBoundingBox(-17.0, 8.0, 4.0, 20.0);
                list.add(mali);
                break;
            case "SONG_DYNASTY":
                EmpireTerritory song = new EmpireTerritory("Dynastie Song", new Color(239, 68, 68));
                song.addBoundingBox(98.0, 18.0, 124.0, 38.0);
                list.add(song);
                break;
            case "SAKOKU_JAPAN":
                EmpireTerritory japan = new EmpireTerritory("Shogunat Tokugawa", new Color(168, 85, 247));
                japan.addBoundingBox(129.0, 30.0, 145.0, 44.0);
                list.add(japan);
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
                EmpireTerritory aztec = new EmpireTerritory("Alliance Aztèque", new Color(245, 158, 11));
                aztec.addBoundingBox(-102.0, 14.0, -95.0, 21.0);
                list.add(aztec);
                EmpireTerritory inca = new EmpireTerritory("Tawantinsuyu (Inca)", new Color(220, 38, 38));
                inca.addBoundingBox(-80.0, -35.0, -68.0, 2.0);
                list.add(inca);
                break;
            case "ONE_CONTINENT":
                EmpireTerritory sapiens = new EmpireTerritory("Foyer Pan-Africain Homo Sapiens", new Color(234, 179, 8)); // Yellow/Gold
                sapiens.addBoundingBox(-18.0, -35.0, 51.0, 37.0); // Entire African continent
                list.add(sapiens);

                EmpireTerritory neanderthal = new EmpireTerritory("Niche Homo Neanderthalensis (Eurasie Ouest)", new Color(14, 165, 233)); // Cyan/Sky Blue
                neanderthal.addBoundingBox(-10.0, 32.0, 55.0, 58.0); // Europe, Near East, Caucasus
                list.add(neanderthal);

                EmpireTerritory denisova = new EmpireTerritory("Niche Homo Denisova (Eurasie Est & Asie)", new Color(217, 70, 239)); // Purple/Magenta
                denisova.addBoundingBox(55.0, 20.0, 125.0, 65.0); // Altai, Siberia, East Asia
                list.add(denisova);
                break;
            default:
                EmpireTerritory generic = new EmpireTerritory("Sphère Démographique", new Color(59, 130, 246));
                generic.addBoundingBox(-10.0, 25.0, 50.0, 55.0);
                list.add(generic);
                break;
        }
        return list;
    }

    private static List<LanguageZone> getLanguageZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Latin", 12.5, 41.9, new Color(59, 130, 246)));
                    list.add(new LanguageZone("Greek", 23.7, 37.9, new Color(6, 182, 212)));
                    list.add(new LanguageZone("Celtic", 2.3, 48.8, new Color(34, 197, 94)));
                    list.add(new LanguageZone("Punique", 10.3, 36.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Aramaic", 35.4, 31.8, new Color(239, 68, 68)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Proto-Semitic", 35.4, 31.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Sumerian", 44.4, 32.5, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Anatolian", 38.9, 37.2, new Color(168, 85, 247)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Proto-Sapiens (Afrique)", 36.0, 4.5, new Color(234, 179, 8)));
                    list.add(new LanguageZone("Proto-Néandertalien (Eurasie Ouest)", -3.5, 42.3, new Color(14, 165, 233)));
                    list.add(new LanguageZone("Proto-Denisovien (Asie & Altai)", 84.7, 51.4, new Color(217, 70, 239)));
                    return list;
            }
        }
        // Global baseline language families to reconstruct worldwide maps
        list.add(new LanguageZone("Indo-European Baseline", 15.0, 48.0, new Color(59, 130, 246)));
        list.add(new LanguageZone("Afroasiatic Baseline", 25.0, 25.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sino-Tibetan Baseline", 105.0, 32.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Niger-Congo Baseline", 15.0, 5.0, new Color(16, 185, 129)));
        list.add(new LanguageZone("Austronesian Baseline", 115.0, -2.0, new Color(6, 182, 212)));
        list.add(new LanguageZone("Trans-New Guinea / Sahul", 138.0, -18.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Amerind & Na-Dene Baseline", -85.0, 15.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Uralic & Altaic Baseline", 75.0, 55.0, new Color(101, 163, 13)));
        list.add(new LanguageZone("Dravidian Baseline", 78.0, 14.0, new Color(217, 119, 6)));
        return list;
    }

    private static List<LanguageZone> getKinshipZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        // Focal scenario kinship overrides
        if (type != null && type.equalsIgnoreCase("ROMAN_EMPIRE")) {
            list.add(new LanguageZone("Patrilineal Gens Romana", 12.5, 41.9, new Color(168, 85, 247)));
        } else if (type != null && type.equalsIgnoreCase("WEST_AFRICA_MALI")) {
            list.add(new LanguageZone("Matrilineal Lineage & Clan", -10.0, 12.0, new Color(236, 72, 153)));
        }

        // Global baseline kinship systems
        list.add(new LanguageZone("Patrilineal Gens & Lineage", 15.0, 45.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Matrilineal Clan Systems", 10.0, 8.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Bilateral Forager Bands", 135.0, -25.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Nomadic Steppe Clan Federations", 75.0, 48.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("State Lineage & Household", 110.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Amerind Matri-Clans", -80.0, 10.0, new Color(234, 179, 8)));
        return list;
    }

    private static List<LanguageZone> getSacredSitesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Rome Imperial Cult", 12.5, 41.9, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Serapis & Isis", 29.9, 31.2, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Delphi Oracle", 22.5, 38.5, new Color(6, 182, 212)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Göbekli Tepe Megalithic Shrine", 38.9, 37.2, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Marduk Sanctuary", 44.4, 32.5, new Color(245, 158, 11)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Rift Valley Ancestral Sanctuary", 36.0, 4.5, new Color(234, 179, 8)));
                    break;
            }
        }
        // Global baseline sacred sites & ritual belief systems
        list.add(new LanguageZone("Palaeolithic / Siberian Shamanism", 90.0, 55.0, new Color(99, 102, 241)));
        list.add(new LanguageZone("Megalithic Monolith Sanctuary", -3.0, 48.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Polytheist State Pantheon", 44.0, 32.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Vedic & Dharmic Sanctuaries", 78.0, 22.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("Ancestor Shrine Cults", 115.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Animist Spirit Sanctuaries", 5.0, 8.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Mesoamerican Solar Pantheons", -99.0, 19.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sahul Dreamtime Sacred Sites", 132.9, -12.5, new Color(168, 85, 247)));
        return list;
    }

    private static double distSq(double lng1, double lat1, double lng2, double lat2) {
        double dlng = lng1 - lng2;
        double dlat = lat1 - lat2;
        return dlng * dlng + dlat * dlat;
    }

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

    // --- STRUCTS ---

    public static class CityPoint {
        public String name;
        public double lat, lng, weight, sigma;

        public CityPoint(String name, double lat, double lng, double weight, double sigma) {
            this.name = name; this.lat = lat; this.lng = lng;
            this.weight = weight; this.sigma = sigma;
        }
    }

    public static class RiverRibbon {
        public double lng1, lat1, lng2, lat2, weight, widthDeg;

        public RiverRibbon(double lng1, double lat1, double lng2, double lat2, double weight, double widthDeg) {
            this.lng1 = lng1; this.lat1 = lat1; this.lng2 = lng2; this.lat2 = lat2;
            this.weight = weight; this.widthDeg = widthDeg;
        }

        public double distanceToPoint(double px, double py) {
            double l2 = distSq(lng1, lat1, lng2, lat2);
            if (l2 == 0) return Math.sqrt(distSq(px, py, lng1, lat1));
            double t = Math.max(0, Math.min(1, ((px - lng1) * (lng2 - lng1) + (py - lat1) * (lat2 - lat1)) / l2));
            double projX = lng1 + t * (lng2 - lng1);
            double projY = lat1 + t * (lat2 - lat1);
            return Math.sqrt(distSq(px, py, projX, projY));
        }
    }

    public static class EmpireTerritory {
        public String name;
        public Color color;
        public List<double[]> bounds = new ArrayList<>();

        public EmpireTerritory(String name, Color color) {
            this.name = name; this.color = color;
        }

        public void addBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
            bounds.add(new double[]{minLng, minLat, maxLng, maxLat});
        }

        public boolean contains(double lng, double lat) {
            for (double[] b : bounds) {
                if (lng >= b[0] && lng <= b[2] && lat >= b[1] && lat <= b[3]) return true;
            }
            return false;
        }
    }

    public static class LanguageZone {
        public String name;
        public double centerLng, centerLat;
        public Color color;

        public LanguageZone(String name, double centerLng, double centerLat, Color color) {
            this.name = name; this.centerLng = centerLng; this.centerLat = centerLat; this.color = color;
        }
    }

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

    private static void drawDepositHotspots(BufferedImage img, double[][] hotspots, Color colorBase) {
        int width = img.getWidth();
        int height = img.getHeight();
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (double[] spot : hotspots) {
            double lng = spot[0];
            double lat = spot[1];
            double radiusPx = spot[2];
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) ((lng + 180.0) / 360.0 * width);
            int cy = (int) ((90.0 - lat) / 180.0 * height);

            int rInt = (int) radiusPx;
            for (int dy = -rInt; dy <= rInt; dy++) {
                int py = cy + dy;
                if (py < 0 || py >= height) continue;
                double currentLat = 90.0 - (py / (double) height) * 180.0;

                for (int dx = -rInt; dx <= rInt; dx++) {
                    int px = cx + dx;
                    if (px < 0 || px >= width) continue;
                    double currentLng = -180.0 + (px / (double) width) * 360.0;

                    if (!isLand(currentLng, currentLat)) continue;

                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d <= radiusPx) {
                        double norm = 1.0 - (d / radiusPx);
                        double factor = Math.pow(norm, 1.5) * intensity;

                        int origRGB = img.getRGB(px, py);
                        int oldR = (origRGB >> 16) & 0xFF;
                        int oldG = (origRGB >> 8) & 0xFF;
                        int oldB = origRGB & 0xFF;

                        int addR = (int) (colorBase.getRed() * factor);
                        int addG = (int) (colorBase.getGreen() * factor);
                        int addB = (int) (colorBase.getBlue() * factor);

                        int newR = Math.min(255, oldR + addR);
                        int newG = Math.min(255, oldG + addG);
                        int newB = Math.min(255, oldB + addB);

                        img.setRGB(px, py, (newR << 16) | (newG << 8) | newB);
                    }
                }
            }
        }
        g.dispose();
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
        java.util.List<double[]> list = new java.util.ArrayList<>();
        java.nio.file.Path zipPath = java.nio.file.Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!java.nio.file.Files.exists(zipPath)) return list;

        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            var entry = zipFile.getEntry("mrds.csv");
            if (entry == null) return list;

            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(zipFile.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8))) {
                br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    java.util.List<String> parts = EmpiricalGeospatialDatasetIngestion.fastParseCsv(line);
                    if (parts.size() > 14) {
                        try {
                            double lat = Double.parseDouble(parts.get(5).replace("\"", "").trim());
                            double lon = Double.parseDouble(parts.get(6).replace("\"", "").trim());

                            String comms = (parts.get(11) + " " + parts.get(12) + " " + parts.get(13) + " " + parts.get(14)).toLowerCase();

                            boolean match = false;
                            for (String kw : commodityKeywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                                list.add(new double[]{lon, lat, 6.0, 1.0});
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read MRDS CSV dataset: {}", e.getMessage());
        }
        return list;
    }

    public static void rasterizeSpotListToAlpha(BufferedImage img, java.util.List<double[]> spots, Color themeColor, double defaultRadiusPx) {
        int width = img.getWidth();
        int height = img.getHeight();

        float[][] grid = new float[height][width];

        for (double[] spot : spots) {
            double lon = spot[0];
            double lat = spot[1];
            double radiusPx = spot.length > 2 ? spot[2] : defaultRadiusPx;
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) Math.round(((lon + 180.0) / 360.0) * (width - 1));
            int cy = (int) Math.round(((90.0 - lat) / 180.0) * (height - 1));

            int r = (int) Math.ceil(radiusPx);
            int minY = Math.max(0, cy - r);
            int maxY = Math.min(height - 1, cy + r);
            int minX = cx - r;
            int maxX = cx + r;

            double radiusSq = radiusPx * radiusPx;
            double invR = 1.0 / radiusPx;

            for (int py = minY; py <= maxY; py++) {
                double dy = py - cy;
                double dySq = dy * dy;
                for (int px = minX; px <= maxX; px++) {
                    double dx = px - cx;
                    double dSq = dx * dx + dySq;
                    if (dSq <= radiusSq) {
                        int wrapPx = (px % width + width) % width;
                        double dist = Math.sqrt(dSq);
                        double norm = 1.0 - (dist * invR);
                        float val = (float) (norm * Math.sqrt(norm) * intensity);
                        grid[py][wrapPx] += val;
                    }
                }
            }
        }

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float v = grid[py][px];
                if (v > 0) {
                    double norm = 1.0 - Math.exp(-v * 0.85);
                    int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                    img.setRGB(px, py, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(px, py, 0x000000);
                }
            }
        }
    }

    public static void rasterizeTieredSpotList(BufferedImage img, java.util.List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
        int width = img.getWidth();
        int height = img.getHeight();

        float[][] grid = new float[height][width];

        for (double[] spot : spots) {
            double lon = spot[0];
            double lat = spot[1];
            double radiusPx = spot.length > 2 ? spot[2] : defaultRadiusPx;
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) Math.round(((lon + 180.0) / 360.0) * (width - 1));
            int cy = (int) Math.round(((90.0 - lat) / 180.0) * (height - 1));

            int r = (int) Math.ceil(radiusPx);
            int minY = Math.max(0, cy - r);
            int maxY = Math.min(height - 1, cy + r);
            int minX = cx - r;
            int maxX = cx + r;

            double radiusSq = radiusPx * radiusPx;
            double invR = 1.0 / radiusPx;

            for (int py = minY; py <= maxY; py++) {
                double dy = py - cy;
                double dySq = dy * dy;
                for (int px = minX; px <= maxX; px++) {
                    double dx = px - cx;
                    double dSq = dx * dx + dySq;
                    if (dSq <= radiusSq) {
                        int wrapPx = (px % width + width) % width;
                        double dist = Math.sqrt(dSq);
                        double norm = 1.0 - (dist * invR);
                        float val = (float) (norm * Math.sqrt(norm) * intensity);
                        grid[py][wrapPx] += val;
                    }
                }
            }
        }

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float v = grid[py][px];
                if (v > 0.005f) {
                    double norm = Math.clamp(1.0 - Math.exp(-v * 0.75), 0.0, 1.0);
                    int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                    img.setRGB(px, py, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(px, py, 0x000000);
                }
            }
        }
    }

    private static BufferedImage loadDirectBufferedImage(String filename) {
        try {
            // 1. Check data/maps/ether/
            java.io.File etherFile = new java.io.File("data/maps/ether/" + filename);
            if (etherFile.exists()) return javax.imageio.ImageIO.read(etherFile);
            String[] subDirs = {"terre", "earth", "lune", "moon", "mars", "venus", "mercure", "mercury"};
            for (String sub : subDirs) {
                java.io.File subFile = new java.io.File("data/maps/ether/" + sub + "/" + filename);
                if (subFile.exists()) return javax.imageio.ImageIO.read(subFile);
            }
            // 2. Check data/maps/
            java.io.File file = new java.io.File("data/maps/" + filename);
            if (file.exists()) return javax.imageio.ImageIO.read(file);
            for (String sub : subDirs) {
                java.io.File subFile = new java.io.File("data/maps/" + sub + "/" + filename);
                if (subFile.exists()) return javax.imageio.ImageIO.read(subFile);
            }
            // 3. Check classpath
            java.io.InputStream isEther = HistoricalMapGenerator.class.getResourceAsStream("/maps/ether/" + filename);
            if (isEther != null) {
                try (isEther) { return javax.imageio.ImageIO.read(isEther); }
            }
            java.io.InputStream is = HistoricalMapGenerator.class.getResourceAsStream("/maps/" + filename);
            if (is != null) {
                try (is) { return javax.imageio.ImageIO.read(is); }
            }
        } catch (Exception ignored) {}
        return null;
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

    public static BufferedImage rasterizeCoalMap(String type, Scenario scenario) {
        if (cachedCoalMap != null) return cachedCoalMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        if (spots.isEmpty()) {
            spots = loadMRDSDeposits("coal", "lignite", "anthracite", "bituminous");
        }
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 8.0);
        cachedCoalMap = img;
        return img;
    }

    public static BufferedImage rasterizeOilMap(String type, Scenario scenario) {
        if (cachedOilMap != null) return cachedOilMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        if (spots.isEmpty()) {
            spots = loadMRDSDeposits("petroleum", "oil", "hydrocarbon");
        }
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 10.0);
        cachedOilMap = img;
        return img;
    }

    public static BufferedImage rasterizeGasMap(String type, Scenario scenario) {
        if (cachedGasMap != null) return cachedGasMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        if (spots.isEmpty()) {
            spots = loadMRDSDeposits("natural gas", "gas", "methane");
        }
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 10.0);
        cachedGasMap = img;
        return img;
    }

    public static BufferedImage rasterizeUraniumMap(String type, Scenario scenario) {
        if (cachedUraniumMap != null) return cachedUraniumMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("uranium", "thorium");
        double[][] iaeaMajorDeposits = {
            {-105.0, 58.0, 35, 1.4}, {136.9, -30.4, 30, 1.4}, {68.0, 44.0, 40, 1.5},
            {7.4, 18.7, 30, 1.2}, {27.5, -26.2, 30, 1.2}, {118.0, 50.0, 30, 1.2}
        };
        for (double[] b : iaeaMajorDeposits) spots.add(b);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 6.0);
        cachedUraniumMap = img;
        return img;
    }

    public static BufferedImage rasterizeHelium3Map(String type, Scenario scenario) {
        if (cachedHe3Map != null) return cachedHe3Map;
        // Grayscale map: Helium-3 is exclusively a lunar resource
        cachedHe3Map = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
        return cachedHe3Map;
    }

    public static BufferedImage rasterizeIronCopperMap(String type, Scenario scenario) {
        if (cachedIronCopperMap != null) return cachedIronCopperMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite");
        // Major global Iron & Copper deposits with 3 tiered densities in grayscale
        double[][] majorMetals = {
            {118.0, -22.5, 35, 1.5}, {120.5, -23.0, 32, 1.4}, {-50.0, -6.0, 38, 1.6}, {-43.5, -20.0, 32, 1.3},
            {37.0, 51.5, 38, 1.5}, {33.5, 48.0, 34, 1.4}, {-91.5, 47.5, 32, 1.3}, {-66.5, 54.0, 35, 1.3},
            {20.0, 67.8, 28, 1.3}, {85.5, 22.0, 34, 1.3}, {-69.0, -24.0, 38, 1.5}, {-69.5, -22.3, 35, 1.4},
            {-70.5, -34.0, 35, 1.4}, {137.0, -4.0, 32, 1.4}, {-111.0, 33.5, 30, 1.2}, {28.0, -12.5, 35, 1.4},
            {102.0, 25.0, 30, 1.2}, {88.0, 38.0, 25, 1.1}, {-108.0, 32.5, 25, 1.1}
        };
        for (double[] m : majorMetals) spots.add(m);
        rasterizeTieredSpotList(img, spots, new Color(80, 80, 80), new Color(160, 160, 160), new Color(240, 240, 240), 6.0);
        cachedIronCopperMap = img;
        return img;
    }

    public static BufferedImage rasterizePreciousMetalsMap(String type, Scenario scenario) {
        if (cachedPreciousMetalsMap != null) return cachedPreciousMetalsMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("gold", "silver", "platinum", "palladium", "electrum");
        double[][] majorPrecious = {
            {27.0, -26.5, 48, 2.8},   // Witwatersrand (South Africa - Giant Gold)
            {29.0, -24.5, 45, 2.6},   // Bushveld Complex Platinum (South Africa)
            {-116.0, 40.8, 42, 2.5},  // Carlin Trend Nevada (USA - Gold)
            {-65.7, -19.6, 45, 2.7},  // Potosí Cerro Rico (Bolivia - Silver)
            {121.5, -30.7, 40, 2.4},  // Kalgoorlie Super Pit (Australia - Gold)
            {64.6, 41.5, 42, 2.5},    // Muruntau Gold (Uzbekistan)
            {88.2, 69.3, 45, 2.6},    // Norilsk-Talnakh PGMs (Russia)
            {-81.0, 46.5, 38, 2.3},   // Sudbury Basin (Canada - PGMs/Au)
            {-78.5, -7.0, 40, 2.4},   // Yanacocha (Peru - Gold)
            {137.1, -4.0, 42, 2.5}    // Grasberg (Indonesia - Gold/Copper)
        };
        for (double[] p : majorPrecious) spots.add(p);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 5.0);
        cachedPreciousMetalsMap = img;
        return img;
    }

    public static BufferedImage rasterizeRareEarthsMap(String type, Scenario scenario) {
        if (cachedRareEarthsMap != null) return cachedRareEarthsMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("rare earth", "bastnasite", "monazite", "xenotime", "neodymium", "dysprosium", "yttrium", "lanthanum", "cerium", "lithium", "spodumene", "carbonatite", "loparite", "allanite");
        double[][] majorREE = {
            {109.9, 41.8, 55, 3.0},   // Bayan Obo (Inner Mongolia, China - Giant REE/Fe)
            {-115.5, 35.5, 42, 2.6},  // Mountain Pass (California, USA - Bastnäsite)
            {122.5, -28.7, 45, 2.7},  // Mount Weld (Western Australia - Carbonatite REE)
            {-46.0, 60.9, 45, 2.6},   // Kvanefjeld / Ilímaussaq (Greenland - REE/U)
            {116.5, 71.0, 45, 2.6},   // Tomtor (Yakutia, Russia - Carbonatite Nb/REE)
            {34.6, 67.8, 40, 2.4},    // Lovozero (Kola Peninsula, Russia - Loparite REE)
            {115.0, 25.5, 50, 2.8},   // Ganzhou / Jiangxi (South China - Heavy Ionic Clays)
            {103.5, 22.4, 38, 2.3},   // Dong Pao (Vietnam - Bastnäsite)
            {-46.9, -19.6, 42, 2.5},  // Araxá (Minas Gerais, Brazil - Carbonatite Nb/REE)
            {-67.5, -21.0, 52, 2.8},  // Salar de Atacama (Chile - Lithium Brines)
            {-68.0, -23.5, 50, 2.7},  // Salar de Uyuni (Bolivia - Lithium Brines)
            {116.0, -33.8, 42, 2.5},  // Greenbushes (Australia - Spodumene Lithium)
            {14.6, 58.1, 35, 2.2},    // Norra Kärr (Sweden - Heavy REE)
            {20.2, 67.8, 38, 2.3},    // Kiruna / Per Geijer (Sweden - Apatite REE)
            {-64.2, 56.3, 40, 2.4},   // Strange Lake (Quebec/Labrador, Canada)
            {-112.6, 62.1, 38, 2.3}   // Nechalacho (NWT, Canada - REE/Zr)
        };
        for (double[] r : majorREE) spots.add(r);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 5.0);
        cachedRareEarthsMap = img;
        return img;
    }

    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        if (cachedMantleHeatMap != null) return cachedMantleHeatMap;
        java.nio.file.Path csvPath = java.nio.file.Paths.get("data", "maps", "ihfc_davies2013", "heat_flow_2deg.csv");
        if (!java.nio.file.Files.exists(csvPath)) {
            csvPath = java.nio.file.Paths.get("data", "maps", "heat_flow_2deg.csv");
        }

        if (java.nio.file.Files.exists(csvPath)) {
            try {
                int width = 2048, height = 1024;
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                // 2-degree resolution dataset has 90 latitude rows x 180 longitude columns
                float[][] grid = new float[90][180];
                boolean[][] filled = new boolean[90][180];

                java.util.List<String> lines = java.nio.file.Files.readAllLines(csvPath);
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        try {
                            double lon = Double.parseDouble(parts[0].trim());
                            double lat = Double.parseDouble(parts[1].trim());
                            double val = Double.parseDouble(parts[2].trim());

                            int gx = (int) Math.clamp(Math.floor((lon + 180.0) / 2.0), 0, 179);
                            int gy = (int) Math.clamp(Math.floor((90.0 - lat) / 2.0), 0, 89);
                            grid[gy][gx] = (float) val;
                            filled[gy][gx] = true;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Fill any minor unmeasured cells from local neighbors
                for (int gy = 0; gy < 90; gy++) {
                    for (int gx = 0; gx < 180; gx++) {
                        if (!filled[gy][gx] || grid[gy][gx] <= 0) {
                            grid[gy][gx] = 65.0f; // Global mean continental heat flow baseline (mW/m²)
                        }
                    }
                }

                // Continuous smooth Bilinear Interpolation across 2D spherical coordinates with 360° periodic wrapping
                for (int y = 0; y < height; y++) {
                    double lat = 90.0 - (y / (double) height) * 180.0;
                    double gyDouble = Math.clamp(((90.0 - lat) / 180.0) * 90.0 - 0.5, 0.0, 88.999);
                    int gy0 = (int) Math.floor(gyDouble);
                    int gy1 = Math.min(89, gy0 + 1);
                    double fy = gyDouble - gy0;

                    for (int x = 0; x < width; x++) {
                        double lon = -180.0 + (x / (double) width) * 360.0;
                        double gxDouble = ((lon + 180.0) / 360.0) * 180.0 - 0.5;
                        if (gxDouble < 0) gxDouble += 180.0;
                        int gx0 = (int) Math.floor(gxDouble) % 180;
                        int gx1 = (gx0 + 1) % 180;
                        double fx = gxDouble - Math.floor(gxDouble);

                        float hf00 = grid[gy0][gx0];
                        float hf10 = grid[gy0][gx1];
                        float hf01 = grid[gy1][gx0];
                        float hf11 = grid[gy1][gx1];

                        float hfInterp = (float) ((1.0 - fx) * (1.0 - fy) * hf00 + fx * (1.0 - fy) * hf10 + (1.0 - fx) * fy * hf01 + fx * fy * hf11);
                        if (hfInterp <= 0) hfInterp = 65.0f;

                        double norm = Math.clamp((hfInterp - 35.0) / 110.0, 0.0, 1.0);
                        int gray = (int) Math.clamp(norm * 255.0, 0, 255);
                        img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    }
                }
                logger.info("Successfully generated 9th geology tensor (Mantle Heat Flux) with seamless 2D Bilinear Interpolation from Davies 2013 CSV.");
                cachedMantleHeatMap = img;
                return img;
            } catch (Exception e) {
                logger.warn("Could not parse Davies 2013 heat flow CSV: {}", e.getMessage());
            }
        }

        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double[][] spots = {{-155.5, 19.8, 30, 1.2}, {-178.0, -29.0, 45, 1.1}, {-72.0, -15.0, 60, 1.2}, {140.0, 36.0, 50, 1.1}, {43.0, 11.5, 40, 1.2}, {-25.0, 64.8, 45, 1.1}, {14.0, 40.8, 35, 1.0}};
        var list = new java.util.ArrayList<double[]>();
        for (double[] s : spots) list.add(s);
        rasterizeSpotListToAlpha(img, list, Color.WHITE, 15.0);
        cachedMantleHeatMap = img;
        return img;
    }

    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        if (cachedAquiferMap != null) return cachedAquiferMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalAquiferOccurrences();
        if (spots.isEmpty()) {
            // Fallback to major global sedimentary aquifer systems (UNESCO WHYMAP GWR)
            double[][] majorAquifers = {
                {25.0, 22.0, 85, 1.5},   // Nubian Sandstone Aquifer System (2.2M km2)
                {-100.0, 38.0, 65, 1.3}, // Ogallala Aquifer USA
                {-54.0, -25.0, 80, 1.5}, // Guaraní Aquifer South America (1.2M km2)
                {138.0, -26.0, 85, 1.4}, // Great Artesian Basin Australia (1.7M km2)
                {10.0, 30.0, 70, 1.3},   // Northern Sahara Aquifer System
                {80.0, 27.0, 75, 1.4},   // Indo-Gangetic Basin
                {2.0, 47.0, 55, 1.2},    // Paris / Aquitaine Basins Europe
                {-60.0, -3.0, 90, 1.5},  // Amazon Basin Aquifers
                {22.0, -1.0, 75, 1.3},   // Congo Basin Aquifer
                {75.0, 60.0, 85, 1.4},   // West Siberian Basin Aquifer
                {122.0, -18.0, 60, 1.2}, // Canning Basin Australia
                {82.0, 39.0, 55, 1.2},   // Tarim Basin Aquifer
                {-48.0, -1.5, 50, 1.2},  // Marajó Aquifer System
                {-118.0, 36.0, 45, 1.2}, // California Central Valley Aquifer
                {45.0, 25.0, 60, 1.3},   // Arabian Aquifer System
                {16.0, 14.0, 65, 1.3},   // Chad Basin Aquifer
                {23.0, -22.0, 60, 1.2},  // Kalahari / Karoo Aquifer
                {116.0, 37.0, 65, 1.3},  // North China Plain Aquifer
                {70.0, 30.0, 65, 1.3}    // Indus Basin Aquifer
            };
            for (double[] a : majorAquifers) spots.add(a);
        }
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 12.0);
        cachedAquiferMap = img;
        return img;
    }

    public static BufferedImage rasterizeExtensibleResourceTensorMap(int index, String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        long seed = scenario != null ? scenario.getCulturalSeed() : 12345L;
        java.util.Random rnd = new java.util.Random(seed ^ (index * 0x85EBCA6BL));
        double phaseLng = rnd.nextDouble() * Math.PI * 2.0;
        double phaseLat = rnd.nextDouble() * Math.PI * 2.0;
        double scale = 0.004 + (index % 4) * 0.002;
        float hueBase = (float) ((index * 0.173 + 0.5) % 1.0);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;
                if (!isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A); // Equirectangular ocean basemap (Color 15, 23, 42)
                    continue;
                }

                double val = Math.sin(lon * scale + phaseLng) * Math.cos(lat * scale + phaseLat) * 0.5 + 0.5;
                float sat = 0.7f;
                float bright = (float) (val * 0.85);
                int rgb = Color.HSBtoRGB(hueBase, sat, bright);
                img.setRGB(x, y, rgb);
            }
        }
        g.dispose();
        return img;
    }

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

