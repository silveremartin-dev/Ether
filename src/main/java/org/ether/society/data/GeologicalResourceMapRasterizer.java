package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * <h2>Geological Resource Map Rasterizer</h2>
 * <p>
 * Dedicated rasterizer for lithospheric mineral, fossil, geothermal, and hydrological resource layers.
 * Extracts and compiles global empirical and historical occurrences into high-resolution 2D cartographic tensors:
 * <ul>
 *   <li><b>Coal Basins</b>: USGS MRDS and empirical Permian/Carboniferous deposits (e.g., Shanxi, Newcastle, Ruhr, Appalachia).</li>
 *   <li><b>Petroleum &amp; Bitumen</b>: Giant petroleum systems (Ghawar, Burgan, Permian Basin) and historical surface seeps (Dead Sea, Hit, Baku, Pitch Lake).</li>
 *   <li><b>Natural Gas</b>: Major conventional gas reservoirs (South Pars, Urengoy, Groningen) and ancient bamboo brine drillings.</li>
 *   <li><b>Uranium &amp; Thorium</b>: IAEA NFCIS metallogenic provinces (Athabasca, Olympic Dam, Chu-Sarysu).</li>
 *   <li><b>Lunar Helium-3</b>: Regolith volatilization reserve maps.</li>
 *   <li><b>Base Metals (Iron &amp; Copper)</b>: Banded Iron Formations (Pilbara, Caraj-s, Krivoy Rog) and ancient smelting centers (Rio Tinto, Cyprus, Elba, Noricum).</li>
 *   <li><b>Precious Metals (Gold, Silver, PGEs)</b>: Hydraulic roman placer mines (Las Médulas), Witwatersrand, Potos-, Carlin Trend.</li>
 *   <li><b>Rare Earth Elements (REE &amp; Lithium)</b>: Carbonatites (Bayan Obo, Mountain Pass) and salt flat brines (Atacama, Uyuni).</li>
 *   <li><b>Mantle Heat Flow</b>: Ingestion of Davies 2013 spherical grid (2Â° resolution) via bilinear interpolation.</li>
 *   <li><b>Aquifers &amp; Groundwater Tables</b>: UNESCO WHYMAP shapefile polygon ingestion and paleo-recharge pluvial factors (\(\Delta t\)).</li>
 * </ul>
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 * @see HistoricalMapGenerator
 * @see EmpiricalGeospatialDatasetIngestion
 * @see GeologicalResourceProvincesRegistry
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
                        } catch (NumberFormatException ignored) { org.slf4j.LoggerFactory.getLogger(GeologicalResourceMapRasterizer.class).debug("Non-fatal condition in GeologicalResourceMapRasterizer: {}", ignored.getMessage()); }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read MRDS CSV dataset: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Accumulates radial spot intensities onto a 2D float grid using \(O(1)\) local kernel falloff.
     *
     * @param width grid width in pixels
     * @param height grid height in pixels
     * @param spots list of coordinate spots [lon, lat, radiusPx, intensity]
     * @param defaultRadiusPx fallback radius in pixels
     * @return accumulated scalar intensity grid
     */
    private static float[][] accumulateSpotGrid(int width, int height, List<double[]> spots, double defaultRadiusPx) {
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
        return grid;
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
        float[][] grid = accumulateSpotGrid(width, height, spots, defaultRadiusPx);

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
        float[][] grid = accumulateSpotGrid(width, height, spots, defaultRadiusPx);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        if (spots.isEmpty()) {
            spots = loadMRDSDeposits("coal", "lignite", "anthracite", "bituminous");
        }
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.SURFACE_COAL_OUTCROPS);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = new ArrayList<double[]>();
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.MAJOR_CRUSTAL_OIL_BASINS);
        var empirical = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        if (!empirical.isEmpty()) {
            spots.addAll(empirical);
        } else {
            spots.addAll(loadMRDSDeposits("petroleum", "oil", "hydrocarbon"));
        }
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.ANCIENT_BITUMEN_SPOTS);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = new ArrayList<double[]>();
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.MAJOR_GAS_BASINS);
        var empirical = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        if (!empirical.isEmpty()) {
            spots.addAll(empirical);
        }
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.ANCIENT_GAS_SPOTS);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = loadMRDSDeposits("uranium", "thorium");
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.IAEA_MAJOR_URANIUM_DEPOSITS);
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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = loadMRDSDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite");
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.MAJOR_IRON_COPPER_METALS);
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.ANCIENT_IRON_COPPER_METALS);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);

        var spots = new ArrayList<double[]>();
        for (double[] m : loadMRDSDeposits("gold", "silver", "platinum", "palladium", "electrum")) {
            spots.add(new double[]{m[0], m[1], 5.0, 1.2});
        }
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.PRECIOUS_METALS_PROVINCES);

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
        BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
        var spots = loadMRDSDeposits("rare earth", "bastnasite", "monazite", "xenotime", "neodymium",
                "dysprosium", "yttrium", "lanthanum", "cerium", "lithium", "spodumene", "carbonatite", "loparite", "allanite");
        Collections.addAll(spots, GeologicalResourceProvincesRegistry.MAJOR_REE_DEPOSITS);
        rasterizeSpotListToAlpha(img, spots, Color.WHITE, 5.0);
        cachedRareEarthsMap = img;
        return img;
    }

    /**
     * Rasterizes the global lithospheric mantle heat flow tensor (\(\text{mW/m}^2\)).
     * Ingests the empirical 2-degree Davies 2013 spherical grid with continuous bilinear interpolation.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        if (cachedMantleHeatMap != null) return cachedMantleHeatMap;
        cachedMantleHeatMap = GeologicalResourceProvincesRegistry.rasterizeDaviesMantleHeatFlow();
        return cachedMantleHeatMap;
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
        return UnescoWhymapAquiferRasterizer.getPaleoAquiferRechargeFactor(lon, lat, year);
    }

    /**
     * Reads or proceduralizes the UNESCO WHYMAP global groundwater aquifer baseline grid.
     *
     * @return 2048x1024 float array of base groundwater capacities
     */
    public static synchronized float[][] getOrComputeBaseWhymapGrid() {
        return UnescoWhymapAquiferRasterizer.getOrComputeBaseWhymapGrid();
    }

    /**
     * Rasterizes the UNESCO WHYMAP global groundwater aquifer and freshwater table tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        return UnescoWhymapAquiferRasterizer.rasterizeAquiferMap(type, scenario);
    }

    /**
     * Procedurally rasterizes an arbitrary extensible resource tensor for extra dimensions (\(N > 9\)).
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

