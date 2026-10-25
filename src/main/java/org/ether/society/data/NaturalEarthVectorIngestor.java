/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * High-Precision Vector Ingestor & Cartographic Rasterizer for Natural Earth Datasets (50m/10m).
 * Provides authentic sovereign polygon boundaries, linguistic branches, kinship systems (Murdock/Todd),
 * confessional distributions, state capacity, and global trade corridors for modern and contemporary epochs (1900-2060).
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.2
 */
public class NaturalEarthVectorIngestor {
    private static final Logger logger = LoggerFactory.getLogger(NaturalEarthVectorIngestor.class);

    public static final String NATURAL_EARTH_50M_PATH = "data/maps/naturalearth/ne_50m_admin_0_countries.geojson";
    public static final int DEFAULT_WIDTH = 2048;
    public static final int DEFAULT_HEIGHT = 1024;

    public static class CountryFeature {
        public String name;
        public String admin;
        public String sovereignt;
        public String isoA3;
        public String continent;
        public String subregion;
        public List<Path2D> paths = new ArrayList<>();
        public Color sovereignColor;
        public Color isoglossColor;
        public Color kinshipColor;
        public Color ritualColor;
        public int institutionalLevel; // 0-255
        public int technologyLevel;    // 0-255
        public int ecologicalFootprint;// 0-255
        public int pathogenStress;     // 0-255
    }

    public static class VectorFeature {
        public String id;
        public String name;
        public String layerType; // SOVEREIGNTY, COASTLINE, RIVER, LAKE
        public Color color;
        public Path2D geometry;

        public VectorFeature(String id, String name, String layerType, Color color, Path2D geometry) {
            this.id = id;
            this.name = name;
            this.layerType = layerType;
            this.color = color != null ? color : Color.WHITE;
            this.geometry = geometry;
        }
    }

    public static BufferedImage rasterizeVectorFeatures(List<VectorFeature> features, String layerFilter) {
        int width = 1024;
        int height = 512;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (VectorFeature f : features) {
            if (f.geometry == null) continue;
            if (layerFilter != null && !layerFilter.equalsIgnoreCase(f.layerType) && !"ALL".equalsIgnoreCase(layerFilter)) {
                continue;
            }

            g.setColor(f.color);
            if ("RIVER".equalsIgnoreCase(f.layerType) || "COASTLINE".equalsIgnoreCase(f.layerType)) {
                g.setStroke(new BasicStroke(1.5f));
                g.draw(f.geometry);
            } else {
                g.fill(f.geometry);
            }
        }

        g.dispose();
        return img;
    }

    private static List<CountryFeature> CACHED_COUNTRIES = null;

    /**
     * Loads all country features from Natural Earth GeoJSON with full attribute mapping.
     */
    public static synchronized List<CountryFeature> getCountries(int imgW, int imgH) {
        if (CACHED_COUNTRIES != null && !CACHED_COUNTRIES.isEmpty()) {
            return CACHED_COUNTRIES;
        }

        File file = org.ether.society.config.EtherPaths.resolveSourceFile("naturalearth/ne_50m_admin_0_countries.geojson");
        if (!file.exists()) {
            file = new File(NATURAL_EARTH_50M_PATH);
        }
        if (!file.exists()) {
            logger.warn("Natural Earth 50m GeoJSON not found at: {}", file.getAbsolutePath());
            return Collections.emptyList();
        }

        List<CountryFeature> list = new ArrayList<>();
        JsonFactory factory = new JsonFactory();

        try (InputStream is = new FileInputStream(file);
             JsonParser parser = factory.createParser(is)) {

            while (parser.nextToken() != null) {
                if ("features".equals(parser.currentName()) && parser.currentToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            CountryFeature cf = parseCountryFeature(parser, imgW, imgH);
                            if (cf != null && !cf.paths.isEmpty()) {
                                list.add(cf);
                            }
                        }
                    }
                    break;
                }
            }
            logger.info("Successfully ingested {} authentic sovereign country features from Natural Earth ({}x{}).",
                    list.size(), imgW, imgH);
        } catch (Exception e) {
            logger.error("Failed to parse Natural Earth GeoJSON: {}", e.getMessage(), e);
        }

        CACHED_COUNTRIES = list;
        return list;
    }

    private static CountryFeature parseCountryFeature(JsonParser parser, int imgW, int imgH) throws Exception {
        String name = null;
        String admin = null;
        String sovereignt = null;
        String isoA3 = null;
        String continent = null;
        String subregion = null;
        List<Path2D> paths = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            if ("properties".equals(field)) {
                parser.nextToken();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String prop = parser.currentName();
                    parser.nextToken();
                    if ("NAME".equalsIgnoreCase(prop)) name = parser.getText();
                    else if ("ADMIN".equalsIgnoreCase(prop)) admin = parser.getText();
                    else if ("SOVEREIGNT".equalsIgnoreCase(prop)) sovereignt = parser.getText();
                    else if ("ADM0_A3".equalsIgnoreCase(prop) || "ISO_A3".equalsIgnoreCase(prop)) {
                        if (isoA3 == null || "-99".equals(isoA3)) isoA3 = parser.getText();
                    }
                    else if ("CONTINENT".equalsIgnoreCase(prop)) continent = parser.getText();
                    else if ("SUBREGION".equalsIgnoreCase(prop)) subregion = parser.getText();
                    else parser.skipChildren();
                }
            } else if ("geometry".equals(field)) {
                parser.nextToken();
                String geomType = null;
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String gField = parser.currentName();
                    if ("type".equalsIgnoreCase(gField)) {
                        parser.nextToken();
                        geomType = parser.getText();
                    } else if ("coordinates".equalsIgnoreCase(gField)) {
                        parser.nextToken();
                        parseCoordinatesToPaths(parser, geomType, imgW, imgH, paths);
                    } else {
                        parser.nextToken();
                        parser.skipChildren();
                    }
                }
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        if (name == null && admin != null) name = admin;
        if (name == null) return null;

        CountryFeature cf = new CountryFeature();
        cf.name = name;
        cf.admin = admin != null ? admin : name;
        cf.sovereignt = sovereignt != null ? sovereignt : name;
        cf.isoA3 = isoA3 != null ? isoA3 : "UNK";
        cf.continent = continent != null ? continent : "";
        cf.subregion = subregion != null ? subregion : "";
        cf.paths = paths;

        // Assign authentic cliodynamic and anthropological attributes via classifier
        cf.sovereignColor = NaturalEarthAttributeClassifier.computeSovereignColor(cf.isoA3, cf.name);
        cf.isoglossColor = NaturalEarthAttributeClassifier.computeIsoglossColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.kinshipColor = NaturalEarthAttributeClassifier.computeKinshipColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.ritualColor = NaturalEarthAttributeClassifier.computeRitualColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.institutionalLevel = NaturalEarthAttributeClassifier.computeInstitutionalLevel(cf.isoA3, cf.name);
        cf.technologyLevel = NaturalEarthAttributeClassifier.computeTechnologyLevel(cf.isoA3, cf.name);
        cf.ecologicalFootprint = NaturalEarthAttributeClassifier.computeEcologicalFootprint(cf.isoA3, cf.name);
        cf.pathogenStress = NaturalEarthAttributeClassifier.computePathogenStress(cf.isoA3, cf.name, cf.continent);

        return cf;
    }

    private static void parseCoordinatesToPaths(JsonParser parser, String geomType, int imgW, int imgH, List<Path2D> paths) throws Exception {
        if ("Polygon".equalsIgnoreCase(geomType)) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                Path2D path = parseLinearRing(parser, imgW, imgH);
                if (path != null) paths.add(path);
            }
        } else if ("MultiPolygon".equalsIgnoreCase(geomType)) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    Path2D path = parseLinearRing(parser, imgW, imgH);
                    if (path != null) paths.add(path);
                }
            }
        } else {
            parser.skipChildren();
        }
    }

    private static Path2D parseLinearRing(JsonParser parser, int imgW, int imgH) throws Exception {
        if (parser.currentToken() != JsonToken.START_ARRAY) return null;

        Path2D path = new Path2D.Double();
        boolean first = true;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                double lon = parser.getDoubleValue();
                parser.nextToken();
                double lat = parser.getDoubleValue();
                parser.nextToken();

                double px = ((lon + 180.0) / 360.0) * (imgW - 1);
                double py = ((90.0 - lat) / 180.0) * (imgH - 1);

                if (first) {
                    path.moveTo(px, py);
                    first = false;
                } else {
                    path.lineTo(px, py);
                }
            }
        }
        if (!first) {
            path.closePath();
            return path;
        }
        return null;
    }

    public static BufferedImage rasterizeModernSovereigntyMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            Color col = NaturalEarthAttributeClassifier.computeSovereignColorForYear(c, year);
            g.setColor(col);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }

        if (year >= 1886 && year <= 2019) {
            List<CShapesVectorIngestor.CShapesFeature> cshapesFeatures = CShapesVectorIngestor.getFeaturesForYear(year, width, height);
            for (CShapesVectorIngestor.CShapesFeature cs : cshapesFeatures) {
                g.setColor(cs.sovereignColor);
                for (Path2D p : cs.paths) {
                    g.fill(p);
                }
            }
        }

        g.dispose();
        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernIsoglossMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            g.setColor(c.isoglossColor);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        double[][] subnationalIsoglosses = {
            {2.17, 41.38, 0xBE185D, 3.2},
            {-2.93, 43.26, 0xF59E0B, 2.0},
            {-8.61, 42.87, 0x9D174D, 2.8},
            {1.44, 43.60, 0xF43F5E, 4.0},
            {-2.75, 48.10, 0x10B981, 2.2},
            {9.01, 42.04, 0xE879F9, 1.8},
            {-3.90, 52.41, 0x10B981, 2.2},
            {-4.25, 57.47, 0x059669, 2.5},
            {8.54, 47.37, 0x3B82F6, 2.2},
            {6.63, 46.52, 0xEC4899, 1.8},
            {8.96, 46.19, 0xE879F9, 1.5},
            {4.35, 50.85, 0x60A5FA, 2.0},
            {11.25, 43.77, 0xE879F9, 3.0},
            {14.26, 40.85, 0xC084FC, 3.5},
            {14.01, 37.59, 0xC084FC, 2.8},
            {-71.21, 46.81, 0xEC4899, 6.0},
            {-95.00, 65.00, 0x607D8B, 15.0},
            {113.26, 23.13, 0xFB7185, 4.5},
            {121.47, 31.23, 0xF43F5E, 3.5},
            {91.14, 29.65, 0xDC2626, 8.0},
            {87.62, 43.82, 0x0EA5E9, 8.0},
            {111.77, 40.84, 0x2ECC71, 7.0},
            {80.27, 13.08, 0xB45309, 4.5},
            {78.48, 17.38, 0xB45309, 4.5},
            {76.50, 14.00, 0xB45309, 4.0},
            {76.27, 9.93, 0xB45309, 3.0},
            {88.36, 22.57, 0xF59E0B, 4.5},
            {72.88, 19.07, 0xF59E0B, 4.5},
            {44.00, 36.00, 0xD97706, 3.5},
            {4.00, 36.50, 0x047857, 2.5},
            {-7.00, 31.00, 0x047857, 3.5}
        };

        NaturalEarthSubnationalLayerHelper.applySubnationalCenters(img, subnationalIsoglosses, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernKinshipMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            g.setColor(c.kinshipColor);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        double[][] subnationalKinship = {
            {1.44, 43.60, 0x8B5CF6, 3.5},
            {-2.93, 43.26, 0x8B5CF6, 2.0},
            {2.17, 41.38, 0x8B5CF6, 3.0},
            {9.19, 45.46, 0x8B5CF6, 3.5},
            {76.27, 9.93, 0xF43F5E, 2.8},
            {100.5, -0.50, 0xF43F5E, 3.0},
            {-76.0, 43.0, 0xF43F5E, 3.0},
            {133.5, -24.0, 0xE91E63, 7.0}
        };

        NaturalEarthSubnationalLayerHelper.applySubnationalCenters(img, subnationalKinship, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernRitualsMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            g.setColor(c.ritualColor);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        double[][] subnationalRituals = {
            {51.39, 35.69, 0x0D9488, 6.0},
            {44.36, 32.00, 0x0D9488, 3.5},
            {49.86, 40.40, 0x0D9488, 3.0},
            {91.14, 29.65, 0xD97706, 7.0},
            {106.9, 47.92, 0xD97706, 6.0},
            {115.1, -8.34, 0xF59E0B, 2.0},
            {35.21, 31.77, 0x2563EB, 1.5},
            {39.82, 21.42, 0x10B981, 2.0},
            {12.45, 41.90, 0xEC4899, 1.5}
        };

        NaturalEarthSubnationalLayerHelper.applySubnationalCenters(img, subnationalRituals, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernInstitutionalMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            int v = c.institutionalLevel;
            g.setColor(new Color(v, v, v));
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernTechnologyMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            int v = c.technologyLevel;
            g.setColor(new Color(v, v, v));
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernTradeNetworkMap(long year, int width, int height, BufferedImage elevationMask) {
        return NaturalEarthSubnationalLayerHelper.rasterizeModernTradeNetworkMap(year, width, height, elevationMask);
    }

    public static BufferedImage rasterizeModernEcologicalMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CountryFeature c : countries) {
            int v = c.ecologicalFootprint;
            g.setColor(new Color(v, v, v));
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        return applyElevationMask(img, elevationMask);
    }

    public static BufferedImage rasterizeModernPathogenMap(long year, int width, int height, BufferedImage elevationMask) {
        return AnalyticalEpidemiologyModel.generatePathogenMap(year, width, height, elevationMask);
    }

    public static Color computeSovereignColorForYear(CountryFeature c, long year) {
        return NaturalEarthAttributeClassifier.computeSovereignColorForYear(c, year);
    }

    public static BufferedImage applyElevationMask(BufferedImage img, BufferedImage elevationMask) {
        return NaturalEarthSubnationalLayerHelper.applyElevationMask(img, elevationMask);
    }

    public static SvgMapIngestor.SvgIngestionResult loadNaturalEarthMap(String scenarioType) {
        String fileName = "ne_10m_" + (scenarioType != null ? scenarioType.toLowerCase() : "default") + ".svg";
        File file = new File("data/maps/vector/" + fileName);

        if (file.exists() && file.isFile()) {
            try (InputStream is = new FileInputStream(file)) {
                logger.info("Ingesting Natural Earth 1:10m vector cartography: {}", file.getAbsolutePath());
                return SvgMapIngestor.ingestSvg(is);
            } catch (Exception e) {
                logger.warn("Failed to ingest Natural Earth vector file '{}': {}", file.getAbsolutePath(), e.getMessage());
            }
        }
        return null;
    }
}
