/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * High-Precision Streaming Ingestor for Seshat / ClioPatria Historical Vector Polities (158 MB GeoJSON).
 * Extracts authentic historical border polygons for any year and rasterizes them directly onto
 * the 2048x1024 cartographic tensor grids.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CliopatriaGeoJsonStreamingParser
 * @see CliopatriaCulturalTaxonomyRegistry
 * @see CliopatriaSubregionalCentersRegistry
 */
public class CliopatriaPolityVectorReader {
    private static final Logger logger = LoggerFactory.getLogger(CliopatriaPolityVectorReader.class);

    /** Path to the Seshat ClioPatria historical polity GeoJSON dataset. */
    public static final String SESHAT_GEOJSON_PATH = "data/maps/seshat/cliopatria_polities_only.geojson";

    /**
     * Loads and rasterizes Seshat ClioPatria historical polity polygons for the specified year.
     *
     * @param targetYear historical epoch year
     * @param width target raster width in pixels
     * @param height target raster height in pixels
     * @param elevationMask ETOPO coastline mask
     * @return rasterized sovereignty {@link BufferedImage}, or {@code null} if unavailable
     */
    public static BufferedImage rasterizeSeshatSovereigntyMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) {
            logger.warn("Seshat ClioPatria GeoJSON file not found at: {}", file.getAbsolutePath());
            return null;
        }

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) {
            logger.info("No Seshat vector polities matched year {}. Falling back.", targetYear);
            return null;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Sort polities by area descending so smaller autonomous vassals/provinces render on top of macro-empires
        for (HistoricalPolityFeature polity : polities) {
            Color c = polity.color != null ? polity.color : getPolityColor(polity.name, polity.seshatId);
            g.setColor(c);
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }

        if (targetYear >= -500L && targetYear <= 500L) {
            CliopatriaSubregionalCentersRegistry.renderClassicalAntiquityEnclaves(g, width, height);
        }

        g.dispose();

        // Pre-generate orographic tribal domain background over NOAA ETOPO digital elevation model
        List<OrographicGlottologPropagator.CulturalSeed> tribalSeeds = getTribalDomainSeeds(targetYear);
        BufferedImage tribalBg = OrographicGlottologPropagator.propagateCulturalSeeds(tribalSeeds, width, height, elevationMask);

        if (elevationMask != null) {
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                for (int x = 0; x < width; x++) {
                    double lon = -180.0 + (x + 0.5) / width * 360.0;
                    int mx = Math.clamp(x * elevationMask.getWidth() / width, 0, elevationMask.getWidth() - 1);
                    int my = Math.clamp(y * elevationMask.getHeight() / height, 0, elevationMask.getHeight() - 1);
                    int land = elevationMask.getRaster().getSample(mx, my, 0);
                    if (land == 0 || (lat < -60.0 && targetYear < 1900L)) {
                        img.setRGB(x, y, 0x000000);
                    } else if (img.getRGB(x, y) == 0xFF000000 || img.getRGB(x, y) == 0x000000) {
                        double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, targetYear);
                        if (occWeight <= 0.001 && targetYear < 1900L) {
                            img.setRGB(x, y, 0x000000);
                        } else {
                            int unassignedColor = (targetYear >= 1900L && tribalBg != null) ? tribalBg.getRGB(x, y) : 0x2D3748;
                            img.setRGB(x, y, unassignedColor);
                        }
                    }
                }
            }
        }

        logger.info("Successfully rasterized {} Seshat ClioPatria authentic vector polities for year {} ({}x{}).",
                polities.size(), targetYear, width, height);
        return img;
    }

    /**
     * Builds authentic regional tribal / clan domain seeds for orographic cost-distance propagation.
     *
     * @param year historical epoch year
     * @return list of regional cultural seeds
     */
    public static List<OrographicGlottologPropagator.CulturalSeed> getTribalDomainSeeds(long year) {
        List<OrographicGlottologPropagator.CulturalSeed> seeds = new ArrayList<>();
        // Americas
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-60.0, -3.0, 0x2E7D32, 1.4, "Amazonian Indigenous Nations"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-62.0, -25.0, 0x5D4037, 1.3, "Gran Chaco & Pampas"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-68.0, 6.0, 0x33691E, 1.2, "Orinoco & Caribbean Tribes"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-82.0, 38.0, 0x388E3C, 1.4, "North American Eastern Woodlands"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-100.0, 42.0, 0xC27803, 1.5, "North American Great Plains"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-122.0, 48.0, 0x00796B, 1.3, "Pacific Northwest & Salish"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-110.0, 32.0, 0xB45309, 1.3, "Southwest / Aridoamerica"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-105.0, 62.0, 0x455A64, 1.5, "Subarctic Dene & Athabaskan"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(-70.0, 70.0, 0x607D8B, 1.6, "Arctic Inuit / Thule Domain"));

        // Africa
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(0.0, 10.0, 0xB45309, 1.4, "West African Savanna & Voltaic"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -1.0, 0x1B5E20, 1.5, "Congo Equatorial Forest Clans"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(38.0, 4.0, 0xC2410C, 1.3, "East African Pastoralists"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(24.0, -28.0, 0xA16207, 1.4, "Southern African Khoisan / San"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(47.0, -19.0, 0x4E342E, 1.2, "Malagasy Indigenous Domain"));

        // Eurasia & Siberia
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(10.0, 56.0, 0x4D7C0F, 1.3, "Northern European Germanic & Celtic Forest Clans"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(30.0, 64.0, 0x0F766E, 1.4, "Boreal Finno-Ugric & Samoyed"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(65.0, 48.0, 0x9A3412, 1.6, "Eurasian Steppe Nomads"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(100.0, 58.0, 0x37474F, 1.5, "Siberian Taiga & Evenki"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(150.0, 65.0, 0x546E7A, 1.5, "Chukchi & Koryak Paleo-Siberian"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(100.0, 20.0, 0x4B6B40, 1.3, "Southeast Asian Zomia Highlands"));

        // Oceania & Australasia
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -25.0, 0xA04000, 1.6, "Australian Aboriginal Nations"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(140.0, -5.0, 0x15803D, 1.3, "Papuan Highland Clans"));
        seeds.add(new OrographicGlottologPropagator.CulturalSeed(170.0, -15.0, 0x0284C7, 1.5, "Polynesian Oceanic Domain"));
        return seeds;
    }

    /**
     * Determines the authentic regional tribal / clan domain color for stateless inhabited lands.
     *
     * @param lon longitude in degrees
     * @param lat latitude in degrees
     * @param year historical epoch year
     * @return RGB hex color integer
     */
    public static int getTribalDomainColor(double lon, double lat, long year) {
        if (lat < -60.0) {
            return 0x0284C7; // Antarctic Treaty / International Scientific Research Domain (#0284C7 Sky Blue)
        }
        double minDistSq = Double.MAX_VALUE;
        int bestColor = 0x4B5563;
        for (OrographicGlottologPropagator.CulturalSeed td : getTribalDomainSeeds(year)) {
            double dLat = lat - td.lat();
            double dLon = (lon - td.lon()) * Math.cos(Math.toRadians((lat + td.lat()) * 0.5));
            double d2 = dLat * dLat + dLon * dLon;
            if (d2 < minDistSq) {
                minDistSq = d2;
                bestColor = td.rgbColor();
            }
        }
        return bestColor;
    }

    /**
     * Loads and rasterizes high-precision authentic Isogloss (Linguistic Phyla) map for the specified year.
     *
     * @param targetYear historical epoch year
     * @param width target raster width in pixels
     * @param height target raster height in pixels
     * @param elevationMask ETOPO coastline mask
     * @return rasterized isogloss {@link BufferedImage}, or {@code null} if unavailable
     */
    public static BufferedImage rasterizeSeshatIsoglossMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) return null;

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (HistoricalPolityFeature polity : polities) {
            g.setColor(getPolityIsoglossColor(polity.name, polity.seshatId));
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }
        g.dispose();

        double[][] linguisticCenters = CliopatriaSubregionalCentersRegistry.getSubregionalLinguisticCenters(targetYear);
        List<OrographicGlottologPropagator.CulturalSeed> isoglossSeeds = new ArrayList<>();
        for (double[] lc : linguisticCenters) {
            isoglossSeeds.add(new OrographicGlottologPropagator.CulturalSeed(lc[0], lc[1], (int) lc[2], lc[3] / 18.0, "Isogloss"));
        }
        BufferedImage isoglossBg = OrographicGlottologPropagator.propagateCulturalSeeds(isoglossSeeds, width, height, elevationMask);

        CliopatriaSubregionalCentersRegistry.applyOrographicFallbackAndCoastlineMask(img, isoglossBg, elevationMask, targetYear, 0x2563EB);
        logger.info("Successfully rasterized authentic high-precision Isogloss tensor map for year {} ({}x{}).", targetYear, width, height);
        return img;
    }

    /**
     * Loads and rasterizes high-precision authentic Kinship &amp; Social Organization map for the specified year.
     *
     * @param targetYear historical epoch year
     * @param width target raster width in pixels
     * @param height target raster height in pixels
     * @param elevationMask ETOPO coastline mask
     * @return rasterized kinship {@link BufferedImage}, or {@code null} if unavailable
     */
    public static BufferedImage rasterizeSeshatKinshipMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) return null;

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (HistoricalPolityFeature polity : polities) {
            g.setColor(getPolityKinshipColor(polity.name, polity.seshatId));
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }
        g.dispose();

        double[][] kinshipCenters = CliopatriaSubregionalCentersRegistry.getSubregionalKinshipCenters(targetYear);
        List<OrographicGlottologPropagator.CulturalSeed> kinshipSeeds = new ArrayList<>();
        for (double[] kc : kinshipCenters) {
            kinshipSeeds.add(new OrographicGlottologPropagator.CulturalSeed(kc[0], kc[1], (int) kc[2], kc[3] / 18.0, "Kinship"));
        }
        BufferedImage kinshipBg = OrographicGlottologPropagator.propagateCulturalSeeds(kinshipSeeds, width, height, elevationMask);

        CliopatriaSubregionalCentersRegistry.applyOrographicFallbackAndCoastlineMask(img, kinshipBg, elevationMask, targetYear, 0x8B5CF6);
        logger.info("Successfully rasterized authentic high-precision Kinship tensor map for year {} ({}x{}).", targetYear, width, height);
        return img;
    }

    /**
     * Normalize strings for resilient historical keyword matching.
     *
     * @param s raw string
     * @return normalized string
     */
    public static String normalize(String s) {
        return CliopatriaCulturalTaxonomyRegistry.normalize(s);
    }

    /**
     * Map historical polity name to authentic Isogloss (Linguistic Phylum / Branch) color.
     *
     * @param name polity name
     * @param seshatId Seshat identifier
     * @return isogloss color
     */
    public static Color getPolityIsoglossColor(String name, String seshatId) {
        return CliopatriaCulturalTaxonomyRegistry.getPolityIsoglossColor(name, seshatId);
    }

    /**
     * Map historical polity name to authentic Kinship &amp; Social Structure color.
     *
     * @param name polity name
     * @param seshatId Seshat identifier
     * @return kinship color
     */
    public static Color getPolityKinshipColor(String name, String seshatId) {
        return CliopatriaCulturalTaxonomyRegistry.getPolityKinshipColor(name, seshatId);
    }

    /**
     * Map historical polity name to official hex color matching cultural_registry.json.
     *
     * @param name polity name
     * @param seshatId Seshat identifier
     * @return polity sovereign color
     */
    public static Color getPolityColor(String name, String seshatId) {
        return CliopatriaCulturalTaxonomyRegistry.getPolityColor(name, seshatId);
    }

    /**
     * Computes authentic 4D cliodynamic trait vector [linguisticBranch, socialStructure, subsistenceMode, ritualTradition].
     *
     * @param name polity name
     * @param seshatId Seshat identifier
     * @param targetYear target simulation year
     * @return 4D trait vector
     */
    public static double[] computePolityTraits(String name, String seshatId, long targetYear) {
        return CliopatriaCulturalTaxonomyRegistry.computePolityTraits(name, seshatId, targetYear);
    }

    /**
     * Fast streaming parser for 158 MB GeoJSON file using Jackson Streaming API.
     *
     * @param geoJsonFile GeoJSON file handle
     * @param targetYear target year
     * @param imgW raster width
     * @param imgH raster height
     * @return list of parsed polity features
     */
    public static List<HistoricalPolityFeature> loadPolitiesForYear(File geoJsonFile, int targetYear, int imgW, int imgH) {
        return CliopatriaGeoJsonStreamingParser.loadPolitiesForYear(geoJsonFile, targetYear, imgW, imgH);
    }
}
