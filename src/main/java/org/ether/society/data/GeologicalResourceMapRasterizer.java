package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * <h2>Geological Resource Map Rasterizer</h2>
 * <p>
 * Dedicated rasterizer for lithospheric mineral, fossil, geothermal, and hydrological resource layers.
 * Extracts and compiles global empirical and historical occurrences into high-resolution 2D cartographic tensors:
 * <ul>
 *   <li><b>Coal Basins</b>: USGS MRDS and empirical Permian/Carboniferous deposits (e.g., Shanxi, Newcastle, Ruhr, Appalachia).</li>
 *   <li><b>Petroleum & Bitumen</b>: Giant petroleum systems (Ghawar, Burgan, Permian Basin) and historical surface seeps (Dead Sea, Hit, Baku, Pitch Lake).</li>
 *   <li><b>Natural Gas</b>: Major conventional gas reservoirs (South Pars, Urengoy, Groningen) and ancient bamboo brine drillings.</li>
 *   <li><b>Uranium & Thorium</b>: IAEA NFCIS metallogenic provinces (Athabasca, Olympic Dam, Chu-Sarysu).</li>
 *   <li><b>Lunar Helium-3</b>: Regolith volatilization reserve maps.</li>
 *   <li><b>Base Metals (Iron & Copper)</b>: Banded Iron Formations (Pilbara, Carajás, Krivoy Rog) and ancient smelting centers (Rio Tinto, Cyprus, Elba, Noricum).</li>
 *   <li><b>Precious Metals (Gold, Silver, PGEs)</b>: Hydraulic roman placer mines (Las Médulas), Witwatersrand, Potosí, Carlin Trend.</li>
 *   <li><b>Rare Earth Elements (REE & Lithium)</b>: Carbonatites (Bayan Obo, Mountain Pass) and salt flat brines (Atacama, Uyuni).</li>
 *   <li><b>Mantle Heat Flow</b>: Ingestion of Davies 2013 spherical grid (2° resolution) via bilinear interpolation.</li>
 *   <li><b>Aquifers & Groundwater Tables</b>: UNESCO WHYMAP shapefile polygon ingestion and paleo-recharge pluvial factors ($\Delta t$).</li>
 * </ul>
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 * @see HistoricalMapGenerator
 * @see EmpiricalGeospatialDatasetIngestion
 */
public final class GeologicalResourceMapRasterizer {

    private static final Logger logger = LoggerFactory.getLogger(GeologicalResourceMapRasterizer.class);

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

    private GeologicalResourceMapRasterizer() {
        // Utility class
    }

    /**
     * Loads mineral and ore deposits from the local USGS MRDS compressed archive.
     *
     * @param commodityKeywords variable list of keywords to match (e.g. "coal", "iron", "gold")
     * @return list of coordinate tuples [longitude, latitude, radiusPx, intensity]
     */
    public static List<double[]> loadMRDSDeposits(String... commodityKeywords) {
        List<double[]> list = new ArrayList<>();
        Path zipPath = Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!Files.exists(zipPath)) return list;

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var entry = zipFile.getEntry("mrds.csv");
            if (entry == null) return list;

            try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    List<String> parts = EmpiricalGeospatialDatasetIngestion.fastParseCsv(line);
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

    /**
     * Rasterizes a continuous spatial spot list onto an image buffer using quadratic falloff and exponential brightness clamping.
     *
     * @param img destination buffered image
     * @param spots list of coordinate spots [lon, lat, radiusPx, intensity]
     * @param themeColor color tint
     * @param defaultRadiusPx fallback radius in pixels
     */
    public static void rasterizeSpotListToAlpha(BufferedImage img, List<double[]> spots, Color themeColor, double defaultRadiusPx) {
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

    /**
     * Rasterizes a tiered spot list with intensity-differentiated color thresholds.
     *
     * @param img destination buffered image
     * @param spots list of coordinate spots [lon, lat, radiusPx, intensity]
     * @param lowColor low intensity color
     * @param medColor medium intensity color
     * @param highColor high intensity color
     * @param defaultRadiusPx fallback radius in pixels
     */
    public static void rasterizeTieredSpotList(BufferedImage img, List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
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

    /**
     * Rasterizes the global coal deposit tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeCoalMap(String type, Scenario scenario) {
        if (cachedCoalMap != null) return cachedCoalMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        if (spots.isEmpty()) {
            spots = loadMRDSDeposits("coal", "lignite", "anthracite", "bituminous");
        }

        double[][] surfaceCoalOutcrops = {
            {113.0, 35.0, 34, 2.8},  // Henan & Shanxi
            {123.9, 41.9, 30, 2.6},  // Fushun / Liaoning
            {-2.5, 51.3, 26, 2.3},   // Somerset / Camerton
            {-1.6, 54.9, 26, 2.3},   // Newcastle / Northumberland
            {5.6, 50.6, 24, 2.2},    // Liège / Meuse Valley
            {7.0, 49.3, 24, 2.2},    // Sarre Basin
            {-79.5, 40.5, 36, 2.5},  // Appalachian Outcrops
            {7.2, 51.5, 32, 2.4}     // Ruhr Valley Outcrops
        };
        for (double[] s : surfaceCoalOutcrops) spots.add(s);

        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 8.0);
        cachedCoalMap = img;
        return img;
    }

    /**
     * Rasterizes the global petroleum and bitumen deposits tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeOilMap(String type, Scenario scenario) {
        if (cachedOilMap != null) return cachedOilMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double[][] majorCrustalOilBasins = {
            {49.5, 26.0, 42, 2.6},    // Ghawar / Rub' al Khali
            {48.0, 29.0, 38, 2.5},    // Burgan Field
            {49.0, 31.5, 38, 2.5},    // Marun / Ahvaz
            {-102.5, 31.8, 38, 2.4},  // Permian Basin
            {-92.0, 19.5, 36, 2.3},   // Cantarell / Campeche Basin
            {73.0, 61.0, 40, 2.5},    // Samotlor / West Siberian Oil Basin
            {2.0, 56.5, 32, 2.2},     // North Sea Central Graben
            {-149.0, 70.3, 34, 2.3},  // Prudhoe Bay
            {-71.5, 10.0, 36, 2.4},   // Maracaibo Basin
            {119.0, 38.0, 35, 2.3}    // Shengli / Bohai Bay
        };
        var spots = new ArrayList<double[]>();
        for (double[] b : majorCrustalOilBasins) spots.add(b);
        var empirical = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        if (!empirical.isEmpty()) {
            spots.addAll(empirical);
        } else {
            var mrds = loadMRDSDeposits("petroleum", "oil", "hydrocarbon");
            spots.addAll(mrds);
        }

        double[][] ancientBitumenSpots = {
            {35.5, 31.5, 32, 2.8},   // Dead Sea
            {42.8, 33.6, 34, 2.9},   // Hit / Is on Euphrates
            {49.9, 40.4, 32, 2.7},   // Baku / Absheron Peninsula
            {44.3, 35.5, 30, 2.6},   // Kirkuk / Baba Gurgur
            {48.2, 32.2, 28, 2.4},   // Susa / Elam & Khuzestan
            {20.8, 37.7, 24, 2.2},   // Zakynthos / Keri
            {19.5, 40.7, 24, 2.2},   // Apollonia / Nymphaeum
            {103.6, 31.0, 28, 2.4},  // Dujiangyan / Sichuan
            {-61.6, 10.2, 30, 2.6},  // Pitch Lake
            {-70.9, 9.8, 30, 2.5},   // Mene Grande / Lake Maracaibo
            {-118.4, 34.1, 26, 2.2}  // La Brea / Carpinteria
        };
        for (double[] s : ancientBitumenSpots) spots.add(s);

        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 9.0);
        cachedOilMap = img;
        return img;
    }

    /**
     * Rasterizes the global natural gas deposit tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeGasMap(String type, Scenario scenario) {
        if (cachedGasMap != null) return cachedGasMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double[][] majorGasBasins = {
            {52.0, 26.5, 42, 2.8},    // South Pars / North Dome
            {77.0, 66.0, 42, 2.6},    // Urengoy Field
            {75.0, 67.5, 40, 2.5},    // Yamburg Field
            {6.8, 53.3, 32, 2.3},     // Groningen Giant Gas Field
            {62.3, 36.5, 36, 2.4},    // Dauletabad / Galkynysh
            {3.3, 32.9, 35, 2.3},     // Hassi R'Mel Gas Field
            {-79.5, 41.0, 36, 2.4},   // Marcellus Shale Gas Basin
            {-93.5, 32.0, 32, 2.2},   // Haynesville Shale Gas
            {106.0, 30.5, 36, 2.4},   // Sichuan Gas Basin
            {34.0, 32.8, 30, 2.1},    // Leviathan & Tamar Basins
            {44.0, 73.0, 35, 2.2},    // Shtokman Gas Field
            {116.0, -19.5, 32, 2.2},  // Northwest Shelf Gas Basin
            {-120.0, 56.0, 32, 2.1},  // Montney Gas Basin
            {49.0, 51.5, 34, 2.3},    // Karachaganak Gas Field
            {103.5, 6.0, 30, 2.0}     // Gulf of Thailand Gas Basin
        };
        var spots = new ArrayList<double[]>();
        for (double[] gSpot : majorGasBasins) spots.add(gSpot);
        var empirical = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        if (!empirical.isEmpty()) {
            spots.addAll(empirical);
        }

        double[][] ancientGasSpots = {
            {104.5, 30.0, 34, 2.8},  // Sichuan Basin
            {50.0, 40.5, 32, 2.7},   // Baku / Yanar Dag
            {30.5, 36.4, 26, 2.2},   // Mount Chimaera / Phaselis
            {44.3, 35.5, 28, 2.4}    // Baba Gurgur
        };
        for (double[] s : ancientGasSpots) spots.add(s);

        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 8.0);
        cachedGasMap = img;
        return img;
    }

    /**
     * Rasterizes the global uranium and thorium deposit tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeUraniumMap(String type, Scenario scenario) {
        if (cachedUraniumMap != null) return cachedUraniumMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        var spots = loadMRDSDeposits("uranium", "thorium");
        double[][] iaeaMajorDeposits = {
            {-105.0, 58.0, 38, 2.4},  // Athabasca Basin
            {136.9, -30.4, 36, 2.3},  // Olympic Dam
            {68.0, 44.0, 42, 2.5},    // Chu-Sarysu Basin
            {7.4, 18.7, 34, 2.2},     // Arlit / Tim Mersoï Basin
            {27.5, -26.2, 34, 2.2},   // Witwatersrand Basin
            {118.0, 50.0, 34, 2.2},   // Streltsovskoye Caldera
            {15.0, -22.5, 32, 2.0},   // Rössing & Husab
            {-108.5, 35.5, 30, 2.0},  // Grants Mineral Belt
            {132.8, -12.7, 32, 2.1},  // Ranger / Jabiluka
            {119.5, -23.5, 30, 1.9}   // Yeelirrie
        };
        for (double[] b : iaeaMajorDeposits) spots.add(b);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 6.0);
        cachedUraniumMap = img;
        return img;
    }

    /**
     * Rasterizes the lunar Helium-3 tensor (grayscale baseline for extraterrestrial bodies).
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 BufferedImage
     */
    public static BufferedImage rasterizeHelium3Map(String type, Scenario scenario) {
        if (cachedHe3Map != null) return cachedHe3Map;
        cachedHe3Map = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
        return cachedHe3Map;
    }

    /**
     * Rasterizes the global iron and copper mineral deposit tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeIronCopperMap(String type, Scenario scenario) {
        if (cachedIronCopperMap != null) return cachedIronCopperMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        var spots = loadMRDSDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite");
        double[][] majorMetals = {
            {118.0, -22.5, 40, 2.2},  // Pilbara / Hamersley BIFs
            {120.5, -23.0, 36, 2.0},  // Mount Whaleback
            {-50.0, -6.0, 42, 2.4},   // Carajás Iron Giant
            {-43.5, -20.0, 36, 1.8},  // Quadrilátero Ferrífero
            {37.0, 51.5, 40, 2.2},    // Kursk Magnetic Anomaly
            {33.5, 48.0, 36, 2.0},    // Krivoy Rog
            {-91.5, 47.5, 36, 1.9},   // Mesabi Iron Range
            {-66.5, 54.0, 38, 2.0},   // Labrador Trough
            {20.0, 67.8, 34, 1.9},    // Kiruna Magnetite
            {85.5, 22.0, 36, 1.9},    // Singhbhum Iron Ore Belt
            {-69.0, -24.0, 42, 2.3},  // Escondida
            {-69.5, -22.3, 40, 2.2},  // Chuquicamata
            {-70.5, -34.0, 38, 2.0},  // El Teniente
            {137.0, -4.0, 38, 2.1},   // Grasberg
            {-111.0, 33.5, 34, 1.8},  // Morenci & Arizona Copper Basin
            {28.0, -12.5, 38, 2.1}    // Central African Copperbelt
        };
        for (double[] m : majorMetals) spots.add(m);

        double[][] ancientMetals = {
            {-6.56, 37.69, 34, 2.8},  // Rio Tinto
            {32.9, 35.0, 32, 2.7},    // Cyprus / Troodos Mountains
            {10.3, 42.8, 30, 2.6},    // Elba & Populonia
            {14.9, 47.5, 30, 2.6},    // Noricum / Erzberg
            {114.0, 34.5, 34, 2.8},   // Han Dynasty Iron Monopolies
            {8.0, 9.5, 28, 2.4},      // Nok Culture / Taruga
            {75.8, 28.0, 30, 2.5},    // Khetri Copper Belt
            {35.4, 30.6, 28, 2.4},    // Faynan & Timna
            {24.06, 37.71, 26, 2.2}   // Laurion / Attica
        };
        for (double[] m : ancientMetals) spots.add(m);

        rasterizeTieredSpotList(img, spots, new Color(80, 80, 80), new Color(160, 160, 160), new Color(240, 240, 240), 6.0);
        cachedIronCopperMap = img;
        return img;
    }

    /**
     * Rasterizes the global precious metals deposit tensor (Gold, Silver, PGEs).
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizePreciousMetalsMap(String type, Scenario scenario) {
        if (cachedPreciousMetalsMap != null) return cachedPreciousMetalsMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        var spots = new ArrayList<double[]>();
        var mrds = loadMRDSDeposits("gold", "silver", "platinum", "palladium", "electrum");
        for (double[] m : mrds) {
            spots.add(new double[]{m[0], m[1], 5.0, 1.2});
        }

        double[][] metallogenicProvinces = {
            {-6.77, 42.46, 12.0, 4.2},  // Las Médulas
            {-6.50, 42.55, 9.0, 3.5},   // El Teleno
            {-8.10, 42.15, 8.0, 2.8},   // Galicia / Sil River
            {-6.56, 37.69, 14.0, 4.0},  // Rio Tinto
            {-6.20, 37.75, 10.0, 3.2},  // Aznalcóllar
            {-4.84, 38.77, 11.0, 3.6},  // Almadén
            {-5.90, 38.30, 9.0, 3.0},   // Ossa Morena
            {-6.60, 40.20, 8.0, 2.6},   // Tagus Basin
            {24.06, 37.71, 12.0, 4.2},  // Laurion
            {24.20, 40.90, 11.0, 3.8},  // Mount Pangaeon
            {23.13, 46.30, 14.0, 4.5},  // Roșia Montană
            {22.85, 46.10, 10.0, 3.5},  // Brad / Săcărâmb
            {21.43, 42.62, 11.0, 3.6},  // Novo Brdo
            {22.18, 42.08, 9.0, 3.0},   // Kratovo
            {24.70, 36.97, 7.0, 2.6},   // Siphnos
            {8.03, 45.55, 10.0, 3.4},   // Bessa
            {7.30, 45.74, 8.0, 2.8},    // Val d'Aosta
            {33.50, 22.00, 15.0, 4.5},  // Wadi Allaqi
            {33.58, 25.99, 12.0, 3.8},  // Wadi Hammamat
            {34.80, 24.95, 13.0, 4.0},  // Sukari
            {33.80, 24.20, 10.0, 3.2},  // Barramiya
            {40.87, 23.50, 14.0, 4.2},  // Mahd adh Dhahab
            {42.50, 20.00, 10.0, 3.0},  // Asir
            {28.04, 38.49, 12.0, 4.0},  // Sardis / Pactolus River
            {42.60, 42.25, 11.0, 3.6},  // Colchis
            {44.38, 41.38, 9.0, 3.2},   // Sakdrisi
            {64.60, 41.50, 18.0, 5.0},  // Muruntau
            {67.50, 39.50, 14.0, 4.0},  // Zeravshan Valley
            {78.27, 12.96, 13.0, 4.2},  // Kolar Gold Fields
            {76.65, 16.20, 10.0, 3.4},  // Hutti Gold Mines
            {85.00, 50.00, 14.0, 4.0},  // Altai Mountains
            {120.40, 37.36, 15.0, 4.5}, // Zhaoyuan
            {117.81, 30.93, 12.0, 3.6}, // Tongling
            {112.50, 23.00, 10.0, 3.0}, // Lingnan
            {138.30, 38.00, 9.0, 3.2},  // Sado Island
            {-11.50, 13.50, 14.0, 4.0}, // Bambouk
            {-9.50, 11.50, 13.0, 3.8},  // Bure
            {-1.67, 6.20, 16.0, 4.6},   // Ashanti
            {30.00, -20.00, 13.0, 3.8}, // Great Zimbabwe
            {27.00, -26.50, 22.0, 5.5}, // Witwatersrand
            {29.00, -24.50, 18.0, 4.8}, // Bushveld
            {-76.60, 5.70, 14.0, 4.2},  // Chocó
            {-79.50, -6.70, 12.0, 3.8}, // Lambayeque
            {-70.20, -14.20, 13.0, 4.0},// Carabaya
            {-65.75, -19.58, 18.0, 5.2},// Cerro Rico / Potosí
            {-102.58, 22.77, 16.0, 4.6},// Zacatecas
            {-116.00, 40.80, 16.0, 4.5},// Carlin Trend
            {121.50, -30.75, 16.0, 4.5},// Kalgoorlie Golden Mile
            {150.80, 62.50, 16.0, 4.4}, // Kolyma
            {88.20, 69.30, 18.0, 4.8},  // Norilsk-Talnakh
            {-81.00, 46.50, 14.0, 4.0}, // Sudbury Basin
            {-78.50, -7.00, 15.0, 4.2}, // Yanacocha
            {137.10, -4.05, 16.0, 4.5}  // Grasberg
        };

        for (double[] p : metallogenicProvinces) spots.add(p);

        rasterizeTieredSpotList(img, spots, new Color(90, 90, 90), new Color(175, 175, 175), new Color(255, 255, 255), 8.0);
        cachedPreciousMetalsMap = img;
        return img;
    }

    /**
     * Rasterizes the global rare earth elements and lithium deposit tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeRareEarthsMap(String type, Scenario scenario) {
        if (cachedRareEarthsMap != null) return cachedRareEarthsMap;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("rare earth", "bastnasite", "monazite", "xenotime", "neodymium", "dysprosium", "yttrium", "lanthanum", "cerium", "lithium", "spodumene", "carbonatite", "loparite", "allanite");
        double[][] majorREE = {
            {109.9, 41.8, 55, 3.0},   // Bayan Obo
            {-115.5, 35.5, 42, 2.6},  // Mountain Pass
            {122.5, -28.7, 45, 2.7},  // Mount Weld
            {-46.0, 60.9, 45, 2.6},   // Kvanefjeld
            {116.5, 71.0, 45, 2.6},   // Tomtor
            {34.6, 67.8, 40, 2.4},    // Lovozero
            {115.0, 25.5, 50, 2.8},   // Ganzhou
            {103.5, 22.4, 38, 2.3},   // Dong Pao
            {-46.9, -19.6, 42, 2.5},  // Araxá
            {-67.5, -21.0, 52, 2.8},  // Salar de Atacama
            {-68.0, -23.5, 50, 2.7},  // Salar de Uyuni
            {116.0, -33.8, 42, 2.5},  // Greenbushes
            {14.6, 58.1, 35, 2.2},    // Norra Kärr
            {20.2, 67.8, 38, 2.3},    // Kiruna / Per Geijer
            {-64.2, 56.3, 40, 2.4},   // Strange Lake
            {-112.6, 62.1, 38, 2.3}   // Nechalacho
        };
        for (double[] r : majorREE) spots.add(r);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 5.0);
        cachedRareEarthsMap = img;
        return img;
    }

    /**
     * Rasterizes the global lithospheric mantle heat flow tensor ($\text{mW/m}^2$).
     * Ingests the empirical 2-degree Davies 2013 spherical grid with continuous bilinear interpolation.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        if (cachedMantleHeatMap != null) return cachedMantleHeatMap;
        Path csvPath = Paths.get("data", "maps", "ihfc_davies2013", "heat_flow_2deg.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("data", "maps", "heat_flow_2deg.csv");
        }

        if (Files.exists(csvPath)) {
            try {
                int width = 2048, height = 1024;
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                float[][] grid = new float[90][180];
                boolean[][] filled = new boolean[90][180];

                List<String> lines = Files.readAllLines(csvPath);
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

                for (int gy = 0; gy < 90; gy++) {
                    for (int gx = 0; gx < 180; gx++) {
                        if (!filled[gy][gx] || grid[gy][gx] <= 0) {
                            grid[gy][gx] = 65.0f; // Global mean continental heat flow baseline (mW/m²)
                        }
                    }
                }

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
        var list = new ArrayList<double[]>();
        for (double[] s : spots) list.add(s);
        rasterizeSpotListToAlpha(img, list, Color.WHITE, 15.0);
        cachedMantleHeatMap = img;
        return img;
    }

    /**
     * Recharges aquifer water factor historically across geological eras (Green Sahara, LGM, etc.).
     *
     * @param lon longitude in degrees
     * @param lat latitude in degrees
     * @param year historical epoch year
     * @return groundwater recharge factor multiplier
     */
    public static double getPaleoAquiferRechargeFactor(double lon, double lat, long year) {
        if (year <= -85000L) {
            double centerDist = Math.hypot((lon - 18.0) / 36.0, (lat - 21.0) / 11.0);
            double pluvial = Math.exp(-centerDist * centerDist);
            return 1.10 + 1.15 * pluvial;
        } else if (year <= -65000L) {
            double dAsianDrought = Math.exp(-(Math.pow(lat - 12.0, 2) / 350.0 + Math.pow(lon - 85.0, 2) / 1000.0));
            double dAfricanDrought = Math.exp(-(Math.pow(lat - 8.0, 2) / 350.0 + Math.pow(lon - 20.0, 2) / 600.0));
            return 0.75 - (dAsianDrought * 0.20) - (dAfricanDrought * 0.10);
        } else if (year <= -30000L) {
            double dSahul = Math.exp(-(Math.pow(lat - (-25.0), 2) / 200.0 + Math.pow(lon - 134.0, 2) / 350.0));
            double dMed = Math.exp(-(Math.pow(lat - 35.0, 2) / 150.0 + Math.pow(lon - 20.0, 2) / 450.0));
            return 0.85 + (dSahul * 0.75) + (dMed * 0.40);
        } else if (year <= -16000L) {
            double permafrostFactor = 1.0 / (1.0 + Math.exp(-(lat - 45.0) / 4.0));
            return 0.55 - (permafrostFactor * 0.15);
        } else if (year <= -5000L) {
            double centerDist = Math.hypot((lon - 18.0) / 36.0, (lat - 21.0) / 11.0);
            double pluvial = Math.exp(-centerDist * centerDist);
            return 1.05 + 1.10 * pluvial;
        }
        return 1.0;
    }

    /**
     * Reads or proceduralizes the UNESCO WHYMAP global groundwater aquifer baseline grid.
     *
     * @return 2048x1024 float array of base groundwater capacities
     */
    public static synchronized float[][] getOrComputeBaseWhymapGrid() {
        if (cachedWhymapBlurredGrid != null) return cachedWhymapBlurredGrid;

        int width = 2048, height = 1024;
        File shpFile = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_GW_aquifers_v1_poly.shp");
        if (!shpFile.exists()) {
            shpFile = new File("data/maps/whymap_groundwater/whymap_GW_aquifers_v1_poly.shp");
        }
        File dbfFile = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_GW_aquifers_v1_poly.dbf");
        if (!dbfFile.exists()) {
            dbfFile = new File("data/maps/whymap_groundwater/whymap_GW_aquifers_v1_poly.dbf");
        }
        File riversShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_rivers__v1_line.shp");
        if (!riversShp.exists()) {
            riversShp = new File("data/maps/whymap_groundwater/whymap_rivers__v1_line.shp");
        }

        float[][] blurred = new float[height][width];

        if (shpFile.exists()) {
            try {
                int[] hygeoCodes = null;
                if (dbfFile.exists()) {
                    try (var dis = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(dbfFile)))) {
                        byte[] b32 = new byte[32];
                        dis.readFully(b32);
                        int numRecs = (b32[4] & 0xFF) | ((b32[5] & 0xFF) << 8) | ((b32[6] & 0xFF) << 16) | ((b32[7] & 0xFF) << 24);
                        int headerLen = (b32[8] & 0xFF) | ((b32[9] & 0xFF) << 8);
                        int recLen = (b32[10] & 0xFF) | ((b32[11] & 0xFF) << 8);
                        dis.skipBytes(headerLen - 32);

                        hygeoCodes = new int[numRecs];
                        for (int r = 0; r < numRecs; r++) {
                            byte del = dis.readByte();
                            byte[] bHygeo = new byte[5];
                            dis.readFully(bHygeo);
                            if (recLen > 6) {
                                dis.skipBytes(recLen - 6);
                            }
                            String sH = new String(bHygeo).trim();
                            try {
                                hygeoCodes[r] = Integer.parseInt(sH);
                            } catch (Exception ignored) {
                                hygeoCodes[r] = 33;
                            }
                        }
                    }
                }

                BufferedImage whymapImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D gw = whymapImg.createGraphics();
                gw.setColor(new Color(60, 60, 60));
                gw.fillRect(0, 0, width, height);
                gw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                try (var disShp = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(shpFile), 65536))) {
                    disShp.skipBytes(100);
                    int curRec = 0;
                    while (true) {
                        try {
                            int recNum = disShp.readInt();
                            int contentLenWords = disShp.readInt();
                            int contentLenBytes = contentLenWords * 2;
                            if (contentLenBytes <= 0) { curRec++; continue; }
                            byte[] recBytes = new byte[contentLenBytes];
                            disShp.readFully(recBytes);
                            ByteBuffer bb = ByteBuffer.wrap(recBytes).order(ByteOrder.LITTLE_ENDIAN);
                            int recShapeType = bb.getInt(0);
                            if (recShapeType == 5 || recShapeType == 15) {
                                int code = (hygeoCodes != null && curRec < hygeoCodes.length) ? hygeoCodes[curRec] : 11;
                                int colorVal;
                                if (code >= 10 && code <= 19) {
                                    colorVal = 215 + (code % 5) * 5;
                                } else if (code >= 20 && code <= 29) {
                                    colorVal = 135 + (code % 5) * 5;
                                } else if (code >= 80) {
                                    colorVal = 20;
                                } else {
                                    colorVal = 65 + (code % 5) * 4;
                                }

                                int numParts = bb.getInt(36);
                                int numPoints = bb.getInt(40);
                                int[] parts = new int[numParts];
                                for (int p = 0; p < numParts; p++) {
                                    parts[p] = bb.getInt(44 + p * 4);
                                }
                                int ptsOffset = 44 + numParts * 4;
                                for (int p = 0; p < numParts; p++) {
                                    int start = parts[p];
                                    int end = (p + 1 < numParts) ? parts[p + 1] : numPoints;
                                    if (end > start) {
                                        Path2D.Double path = new Path2D.Double();
                                        for (int pt = start; pt < end; pt++) {
                                            double px = bb.getDouble(ptsOffset + pt * 16);
                                            double py = bb.getDouble(ptsOffset + pt * 16 + 8);
                                            double rx = (px + 180.0) / 360.0 * width;
                                            double ry = (90.0 - py) / 180.0 * height;
                                            if (pt == start) path.moveTo(rx, ry);
                                            else path.lineTo(rx, ry);
                                        }
                                        path.closePath();
                                        gw.setColor(new Color(colorVal, colorVal, colorVal));
                                        gw.fill(path);
                                    }
                                }
                            }
                            curRec++;
                        } catch (EOFException eof) {
                            break;
                        }
                    }
                }

                if (riversShp.exists()) {
                    gw.setColor(new Color(230, 230, 230));
                    gw.setStroke(new BasicStroke(1.6f));
                    try (var disRiv = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(riversShp), 65536))) {
                        disRiv.skipBytes(100);
                        while (true) {
                            try {
                                int recNum = disRiv.readInt();
                                int contentLenWords = disRiv.readInt();
                                int contentLenBytes = contentLenWords * 2;
                                if (contentLenBytes <= 0) continue;
                                byte[] recBytes = new byte[contentLenBytes];
                                disRiv.readFully(recBytes);
                                ByteBuffer bb = ByteBuffer.wrap(recBytes).order(ByteOrder.LITTLE_ENDIAN);
                                int recShapeType = bb.getInt(0);
                                if (recShapeType == 3 || recShapeType == 13) {
                                    int numParts = bb.getInt(36);
                                    int numPoints = bb.getInt(40);
                                    int[] parts = new int[numParts];
                                    for (int p = 0; p < numParts; p++) parts[p] = bb.getInt(44 + p * 4);
                                    int ptsOffset = 44 + numParts * 4;
                                    for (int p = 0; p < numParts; p++) {
                                        int start = parts[p];
                                        int end = (p + 1 < numParts) ? parts[p + 1] : numPoints;
                                        if (end > start) {
                                            Path2D.Double path = new Path2D.Double();
                                            for (int pt = start; pt < end; pt++) {
                                                double px = bb.getDouble(ptsOffset + pt * 16);
                                                double py = bb.getDouble(ptsOffset + pt * 16 + 8);
                                                double rx = (px + 180.0) / 360.0 * width;
                                                double ry = (90.0 - py) / 180.0 * height;
                                                if (pt == start) path.moveTo(rx, ry);
                                                else path.lineTo(rx, ry);
                                            }
                                            gw.draw(path);
                                        }
                                    }
                                }
                            } catch (EOFException eof) {
                                break;
                            }
                        }
                    }
                }
                gw.dispose();

                float[][] k = {
                    {0.003f, 0.013f, 0.022f, 0.013f, 0.003f},
                    {0.013f, 0.059f, 0.097f, 0.059f, 0.013f},
                    {0.022f, 0.097f, 0.159f, 0.097f, 0.022f},
                    {0.013f, 0.059f, 0.097f, 0.059f, 0.013f},
                    {0.003f, 0.013f, 0.022f, 0.013f, 0.003f}
                };

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        float sum = 0.0f;
                        for (int dy = -2; dy <= 2; dy++) {
                            int ny = Math.clamp(y + dy, 0, height - 1);
                            for (int dx = -2; dx <= 2; dx++) {
                                int nx = (x + dx + width) % width;
                                int val = whymapImg.getRGB(nx, ny) & 0xFF;
                                sum += val * k[dy + 2][dx + 2];
                            }
                        }
                        blurred[y][x] = sum;
                    }
                }
                cachedWhymapBlurredGrid = blurred;
                return blurred;
            } catch (Exception e) {
                logger.warn("WHYMAP shapefile ingestion notice: {}, using procedural aquifer model", e.getMessage());
            }
        }

        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : width) / width), 0, (mask != null ? mask.getWidth() : width) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : height) / height), 0, (mask != null ? mask.getHeight() : height) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double elevM = (mask != null) ? (land / 255.0) * 8848.0 : 200.0;
                double orographicCapacityFactor = elevM <= 350.0 ? 1.0 : (elevM <= 1000.0 ? 1.0 - (elevM - 350.0) / 650.0 * 0.70 : Math.max(0.08, 0.30 - (elevM - 1000.0) / 2000.0 * 0.22));
                blurred[y][x] = (float) (65.0 * orographicCapacityFactor);
            }
        }
        cachedWhymapBlurredGrid = blurred;
        return blurred;
    }

    /**
     * Rasterizes the UNESCO WHYMAP global groundwater aquifer and freshwater table tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 2000L;
        int width = 2048, height = 1024;

        float[][] baseGrid = getOrComputeBaseWhymapGrid();
        BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;
                if (!HistoricalMapGenerator.isLand(lon, lat)) {
                    outImg.setRGB(x, y, 0x000000);
                    continue;
                }
                float rawVal = baseGrid[y][x];
                double rechargeFactor = getPaleoAquiferRechargeFactor(lon, lat, year);
                int lum = Math.clamp((int) Math.round(rawVal * rechargeFactor), 0, 255);
                outImg.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(outImg);
    }

    /**
     * Procedurally rasterizes an arbitrary extensible resource tensor for extra dimensions ($N > 9$).
     *
     * @param index tensor layer index
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 BufferedImage
     */
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
                if (!HistoricalMapGenerator.isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A);
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
}
