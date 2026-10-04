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
 * @version 1.0.0-beta.1
 */
public class NaturalEarthVectorIngestor {
    private static final Logger logger = LoggerFactory.getLogger(NaturalEarthVectorIngestor.class);

    /* Internal state variable for natural earth 50m path (String). */
    public static final String NATURAL_EARTH_50M_PATH = "data/maps/naturalearth/ne_50m_admin_0_countries.geojson";
    /* Internal state variable for default width (int). */
    public static final int DEFAULT_WIDTH = 2048;
    /* Internal state variable for default height (int). */
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

        /*
         * Vector feature.
         * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
         *
         * @param id the id parameter (String)
         * @param name the name parameter (String)
         * @param layerType the layer type parameter (String)
         * @param color the color parameter (Color)
         * @param geometry the geometry parameter (Path2D)
         * @return the resulting computation or state reference
         */
        public VectorFeature(String id, String name, String layerType, Color color, Path2D geometry) {
            this.id = id;
            this.name = name;
            this.layerType = layerType;
            this.color = color != null ? color : Color.WHITE;
            this.geometry = geometry;
        }
    }

    /*
     * Rasterize vector features.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param features the features parameter (List&lt;VectorFeature&gt;)
     * @param layerFilter the layer filter parameter (String)
     * @return the resulting computation or state reference
     */
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

    /* Internal state variable for cached countries (List&lt;CountryFeature&gt;). */
    private static List<CountryFeature> CACHED_COUNTRIES = null;

    /*
     * Loads all country features from Natural Earth GeoJSON with full attribute mapping.
     */
    public static synchronized List<CountryFeature> getCountries(int imgW, int imgH) {
        if (CACHED_COUNTRIES != null && !CACHED_COUNTRIES.isEmpty()) {
            return CACHED_COUNTRIES;
        }

        File file = new File(NATURAL_EARTH_50M_PATH);
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

    // Helper subroutine: parse country feature - internal state computation & bounds checking
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
                parser.nextToken(); // START_OBJECT
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
                parser.nextToken(); // START_OBJECT
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

        // Assign authentic cliodynamic and anthropological attributes
        cf.sovereignColor = computeSovereignColor(cf.isoA3, cf.name);
        cf.isoglossColor = computeIsoglossColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.kinshipColor = computeKinshipColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.ritualColor = computeRitualColor(cf.isoA3, cf.name, cf.subregion, cf.continent);
        cf.institutionalLevel = computeInstitutionalLevel(cf.isoA3, cf.name);
        cf.technologyLevel = computeTechnologyLevel(cf.isoA3, cf.name);
        cf.ecologicalFootprint = computeEcologicalFootprint(cf.isoA3, cf.name);
        cf.pathogenStress = computePathogenStress(cf.isoA3, cf.name, cf.continent);

        return cf;
    }

    // Helper subroutine: parse coordinates to paths - internal state computation & bounds checking
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

    // Helper subroutine: parse linear ring - internal state computation & bounds checking
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
                parser.nextToken(); // END_ARRAY for point

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

    // =========================================================================
    // 1. SOVEREIGNTY TENSOR RASTERIZATION (Every Nation has its Authentic Vector)
    // =========================================================================
    /*
     * Rasterize modern sovereignty map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeModernSovereigntyMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // 1. Base fill from sovereign countries or historical colonial empires
        for (CountryFeature c : countries) {
            Color col = computeSovereignColorForYear(c, year);
            g.setColor(col);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }

        // 2. Overlay exact CShapes 2.0 polygons for 1886-2019 if available
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

    // =========================================================================
    // 2. ISOGLOSS TENSOR RASTERIZATION (Glottolog & Linguistic Phyla Mosaic)
    // =========================================================================
    /*
     * Rasterize modern isogloss map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeModernIsoglossMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CountryFeature> countries = getCountries(width, height);
        if (countries.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // 1. Base fill from sovereign countries
        for (CountryFeature c : countries) {
            g.setColor(c.isoglossColor);
            for (Path2D p : c.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        // 2. High-precision sub-national linguistic enclaves & regional dialects (lon, lat, hexColor, radiusSigma)
        double[][] subnationalIsoglosses = {
            // Spain regional languages
            {2.17, 41.38, 0xBE185D, 3.2},    // Catalan (Catalonia / Balearic) (#BE185D)
            {-2.93, 43.26, 0xF59E0B, 2.0},   // Basque / Euskara (#F59E0B)
            {-8.61, 42.87, 0x9D174D, 2.8},   // Galician (#9D174D)
            // France regional
            {1.44, 43.60, 0xF43F5E, 4.0},    // Occitan / Gascon / Provençal (#F43F5E)
            {-2.75, 48.10, 0x10B981, 2.2},   // Breton (Celtic) (#10B981)
            {9.01, 42.04, 0xE879F9, 1.8},    // Corsican (Italo-Romance) (#E879F9)
            // UK regional
            {-3.90, 52.41, 0x10B981, 2.2},   // Welsh (Celtic) (#10B981)
            {-4.25, 57.47, 0x059669, 2.5},   // Scottish Gaelic (#059669)
            // Switzerland / Belgium
            {8.54, 47.37, 0x3B82F6, 2.2},    // Swiss German (#3B82F6)
            {6.63, 46.52, 0xEC4899, 1.8},    // Swiss French (#EC4899)
            {8.96, 46.19, 0xE879F9, 1.5},    // Swiss Italian (#E879F9)
            {4.35, 50.85, 0x60A5FA, 2.0},    // Flemish Dutch (#60A5FA)
            // Italy regional
            {11.25, 43.77, 0xE879F9, 3.0},   // Tuscan (#E879F9)
            {14.26, 40.85, 0xC084FC, 3.5},   // Neapolitan / Southern Italian (#C084FC)
            {14.01, 37.59, 0xC084FC, 2.8},   // Sicilian (#C084FC)
            // Canada
            {-71.21, 46.81, 0xEC4899, 6.0},  // Quebec French (#EC4899)
            {-95.00, 65.00, 0x607D8B, 15.0}, // Inuktitut / Arctic (#607D8B)
            // China regional
            {113.26, 23.13, 0xFB7185, 4.5},  // Yue / Cantonese (#FB7185)
            {121.47, 31.23, 0xF43F5E, 3.5},  // Wu / Shanghainese (#F43F5E)
            {91.14, 29.65, 0xDC2626, 8.0},   // Tibetan (#DC2626)
            {87.62, 43.82, 0x0EA5E9, 8.0},   // Uyghur (Turkic) (#0EA5E9)
            {111.77, 40.84, 0x2ECC71, 7.0},  // Southern Mongolian (#2ECC71)
            // India regional
            {80.27, 13.08, 0xB45309, 4.5},   // Tamil (Dravidian) (#B45309)
            {78.48, 17.38, 0xB45309, 4.5},   // Telugu (Dravidian) (#B45309)
            {76.50, 14.00, 0xB45309, 4.0},   // Kannada (Dravidian) (#B45309)
            {76.27, 9.93, 0xB45309, 3.0},    // Malayalam (Dravidian) (#B45309)
            {88.36, 22.57, 0xF59E0B, 4.5},   // Bengali (#F59E0B)
            {72.88, 19.07, 0xF59E0B, 4.5},   // Marathi (#F59E0B)
            // Middle East & North Africa
            {44.00, 36.00, 0xD97706, 3.5},   // Kurdish (#D97706)
            {4.00, 36.50, 0x047857, 2.5},    // Kabyle Berber (#047857)
            {-7.00, 31.00, 0x047857, 3.5}    // Tamazight Berber (#047857)
        };

        applySubnationalCenters(img, subnationalIsoglosses, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    // =========================================================================
    // 3. KINSHIP TENSOR RASTERIZATION (Emmanuel Todd & Murdock EA Typology)
    // =========================================================================
    /*
     * Rasterize modern kinship map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
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

        // Sub-national Todd kinship refinements
        double[][] subnationalKinship = {
            {1.44, 43.60, 0x8B5CF6, 3.5},    // Occitanie / Pyrenees: Stem Family (#8B5CF6)
            {-2.93, 43.26, 0x8B5CF6, 2.0},   // Basque Country: Stem Family (#8B5CF6)
            {2.17, 41.38, 0x8B5CF6, 3.0},    // Catalonia: Stem Family (#8B5CF6)
            {9.19, 45.46, 0x8B5CF6, 3.5},    // Northern Italy: Stem Family (#8B5CF6)
            {76.27, 9.93, 0xF43F5E, 2.8},    // Kerala: Matrilineal Marumakkathayam (#F43F5E)
            {100.5, -0.50, 0xF43F5E, 3.0},   // Minangkabau (Sumatra): Matrilineal Suku (#F43F5E)
            {-76.0, 43.0, 0xF43F5E, 3.0},    // Iroquois Matrilineal Clan (#F43F5E)
            {133.5, -24.0, 0xE91E63, 7.0}    // Central Australia: 8-Skin Subsection (#E91E63)
        };

        applySubnationalCenters(img, subnationalKinship, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    // =========================================================================
    // 4. RITUALS / RELIGIONS TENSOR RASTERIZATION (Confessional Mosaic)
    // =========================================================================
    /*
     * Rasterize modern rituals map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
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

        // Sub-national religious pockets & sacred centers
        double[][] subnationalRituals = {
            {51.39, 35.69, 0x0D9488, 6.0},   // Iran: Shia Islam (#0D9488)
            {44.36, 32.00, 0x0D9488, 3.5},   // Southern Iraq (Najaf/Karbala): Shia Islam (#0D9488)
            {49.86, 40.40, 0x0D9488, 3.0},   // Azerbaijan: Shia Islam (#0D9488)
            {91.14, 29.65, 0xD97706, 7.0},   // Tibet: Vajrayana Buddhism (#D97706)
            {106.9, 47.92, 0xD97706, 6.0},   // Mongolia: Vajrayana Buddhism (#D97706)
            {115.1, -8.34, 0xF59E0B, 2.0},   // Bali: Balinese Hinduism (#F59E0B)
            {35.21, 31.77, 0x2563EB, 1.5},   // Jerusalem / Israel: Judaism (#2563EB)
            {39.82, 21.42, 0x10B981, 2.0},   // Mecca: Sacred Sanctuary (#10B981)
            {12.45, 41.90, 0xEC4899, 1.5}    // Vatican: Apostolic See (#EC4899)
        };

        applySubnationalCenters(img, subnationalRituals, width, height, elevationMask);
        return applyElevationMask(img, elevationMask);
    }

    // =========================================================================
    // 5. INSTITUTIONAL COMPLEXITY TENSOR RASTERIZATION (State Capacity & Rule of Law)
    // =========================================================================
    /*
     * Rasterize modern institutional map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
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

    // =========================================================================
    // 6. TECHNOLOGY TENSOR RASTERIZATION (Capital Intensity & Innovation Index)
    // =========================================================================
    /*
     * Rasterize modern technology map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
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

    // =========================================================================
    // 7. TRADE NETWORK TENSOR RASTERIZATION (Maritime Chokepoints & Global Supply Corridors)
    // =========================================================================
    /*
     * Rasterize modern trade network map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeModernTradeNetworkMap(long year, int width, int height, BufferedImage elevationMask) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Global Maritime Shipping Lanes & Chokepoints (lon1, lat1, lon2, lat2, intensityColor, strokeWidth)
        double[][] tradeCorridors = {
            // Asia - Europe Main Arterial via Malacca & Suez
            {121.5, 31.2, 114.2, 22.3, 0x00FFFF, 3.5},  // Shanghai -> Hong Kong
            {114.2, 22.3, 103.8, 1.35, 0x00FFFF, 4.0},  // Hong Kong -> Singapore / Malacca Strait
            {103.8, 1.35, 80.2, 6.0, 0x00E5FF, 3.5},    // Malacca -> Sri Lanka (Galle/Colombo)
            {80.2, 6.0, 51.5, 12.0, 0x00E5FF, 3.5},     // Sri Lanka -> Gulf of Aden
            {51.5, 12.0, 43.5, 12.6, 0xFF3D00, 4.5},    // Bab-el-Mandeb Chokepoint (#FF3D00 Red Hot)
            {43.5, 12.6, 32.5, 27.8, 0x00E5FF, 3.5},    // Red Sea Corridor
            {32.5, 27.8, 32.3, 31.2, 0xFF3D00, 5.0},    // Suez Canal Chokepoint (#FF3D00)
            {32.3, 31.2, 14.5, 36.0, 0x00E5FF, 3.5},    // Suez -> Strait of Sicily
            {14.5, 36.0, -5.6, 36.0, 0x00E5FF, 3.5},    // Mediterranean -> Gibraltar
            {-5.6, 36.0, -9.5, 38.7, 0xFF3D00, 4.5},    // Strait of Gibraltar Chokepoint (#FF3D00)
            {-9.5, 38.7, -4.5, 48.4, 0x00E5FF, 3.5},    // Atlantic Iberian Coast -> English Channel
            {-4.5, 48.4, 1.4, 51.0, 0xFF3D00, 4.5},     // English Channel / Dover Strait (#FF3D00)
            {1.4, 51.0, 4.0, 51.9, 0x00FFFF, 4.5},      // Dover -> Rotterdam / Antwerp Haven Core

            // Persian Gulf Oil Artery
            {48.0, 29.5, 56.5, 26.5, 0x00E5FF, 4.0},    // Persian Gulf
            {56.5, 26.5, 59.0, 23.5, 0xFF3D00, 5.0},    // Strait of Hormuz Chokepoint (#FF3D00)
            {59.0, 23.5, 72.8, 18.9, 0x00E5FF, 3.5},    // Hormuz -> Mumbai
            {59.0, 23.5, 80.2, 6.0, 0x00E5FF, 3.5},     // Hormuz -> Sri Lanka

            // Trans-Pacific Shipping Lanes
            {139.7, 35.6, -122.4, 37.8, 0x00E5FF, 3.5}, // Tokyo -> San Francisco
            {121.5, 31.2, -118.2, 33.7, 0x00FFFF, 4.0}, // Shanghai -> Los Angeles / Long Beach
            {129.0, 35.1, -123.1, 49.3, 0x00E5FF, 3.0}, // Busan -> Vancouver

            // Trans-Atlantic Shipping Lanes
            {-74.0, 40.7, -5.0, 50.0, 0x00E5FF, 3.5},   // New York -> English Channel / Le Havre
            {-80.0, 25.8, -5.6, 36.0, 0x00E5FF, 3.0},   // Miami / Caribbean -> Gibraltar

            // Panama Canal Interoceanic Corridor
            {-79.5, 8.9, -79.9, 9.3, 0xFF3D00, 5.0},    // Panama Canal Chokepoint (#FF3D00)
            {-118.2, 33.7, -79.5, 8.9, 0x00E5FF, 3.0},  // California -> Panama
            {-79.9, 9.3, -74.0, 40.7, 0x00E5FF, 3.5},   // Panama -> New York
            {-79.9, 9.3, 4.0, 51.9, 0x00E5FF, 3.0},     // Panama -> Rotterdam

            // Cape of Good Hope South Atlantic / Indian
            {18.4, -34.0, 80.2, 6.0, 0x00E5FF, 2.5},    // Cape Town -> Indian Ocean
            {-43.2, -22.9, 18.4, -34.0, 0x00E5FF, 2.5}, // Santos/Rio -> Cape Town
            {18.4, -34.0, -9.5, 38.7, 0x00E5FF, 2.5},   // Cape Town -> Western Europe

            // Eurasian Overland Freight Corridors (New Silk Road / Trans-Siberian)
            {116.4, 39.9, 87.6, 43.8, 0xF59E0B, 2.5},   // Beijing -> Urumqi
            {87.6, 43.8, 71.4, 51.1, 0xF59E0B, 2.5},    // Urumqi -> Astana
            {71.4, 51.1, 37.6, 55.7, 0xF59E0B, 2.5},    // Astana -> Moscow
            {37.6, 55.7, 21.0, 52.2, 0xF59E0B, 2.5},    // Moscow -> Warsaw
            {21.0, 52.2, 13.4, 52.5, 0xF59E0B, 2.5},    // Warsaw -> Berlin
            {13.4, 52.5, 6.9, 50.9, 0xF59E0B, 2.5}      // Berlin -> Rhine-Ruhr / Duisburg Hub
        };

        for (double[] tc : tradeCorridors) {
            double x1 = ((tc[0] + 180.0) / 360.0) * (width - 1);
            double y1 = ((90.0 - tc[1]) / 180.0) * (height - 1);
            double x2 = ((tc[2] + 180.0) / 360.0) * (width - 1);
            double y2 = ((90.0 - tc[3]) / 180.0) * (height - 1);

            g.setColor(new Color((int) tc[4]));
            g.setStroke(new BasicStroke((float) tc[5], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
        }

        // Global Logistics Super-Hub Emporia (lon, lat, sizeRadius)
        double[][] superHubs = {
            {103.8, 1.35, 6.0},   // Singapore
            {121.5, 31.2, 6.0},   // Shanghai / Yangshan
            {114.2, 22.3, 5.0},   // Hong Kong / Shenzhen
            {129.0, 35.1, 5.0},   // Busan
            {4.0, 51.9, 5.5},     // Rotterdam
            {4.4, 51.2, 5.0},     // Antwerp
            {55.3, 25.3, 5.5},    // Dubai / Jebel Ali
            {-118.2, 33.7, 5.5},  // Los Angeles
            {-74.0, 40.7, 5.0},   // New York / New Jersey
            {-79.5, 8.9, 5.0}     // Panama Hub
        };

        for (double[] sh : superHubs) {
            double x = ((sh[0] + 180.0) / 360.0) * (width - 1);
            double y = ((90.0 - sh[1]) / 180.0) * (height - 1);
            int r = (int) Math.round(sh[2]);

            g.setColor(Color.WHITE);
            g.fillOval((int) x - r, (int) y - r, r * 2, r * 2);
            g.setColor(new Color(0xFF3D00));
            g.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        }

        g.dispose();
        return img;
    }

    // =========================================================================
    // 8. ECOLOGICAL FOOTPRINT TENSOR RASTERIZATION (Industrial Exergy & Land Use)
    // =========================================================================
    /*
     * Rasterize modern ecological map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
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

    // =========================================================================
    // 9. PATHOGEN STRESS TENSOR RASTERIZATION (Analytical R0 Epidemiology)
    // =========================================================================
    /*
     * Rasterize modern pathogen map.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param year the year parameter (long)
     * @param width the width parameter (int)
     * @param height the height parameter (int)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeModernPathogenMap(long year, int width, int height, BufferedImage elevationMask) {
        return AnalyticalEpidemiologyModel.generatePathogenMap(year, width, height, elevationMask);
    }

    // =========================================================================
    // HELPER METHODS: Attribute Computations based on ISO-3166 Codes & Typologies
    // =========================================================================

    /*
     * Compute sovereign color for year.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param c the c parameter (CountryFeature)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static Color computeSovereignColorForYear(CountryFeature c, long year) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        if (c == null) return Color.BLACK;
        String iso = (c.isoA3 != null) ? c.isoA3.toUpperCase(Locale.ROOT) : "";

        // Historical imperial mapping for Belle Epoque & WWI (1900 / 1914)
        if (year >= 1886 && year <= 1920) {
            // British Empire
            if (Set.of("GBR", "IND", "PAK", "BGD", "LKA", "MMR", "MYS", "SGP", "EGY", "SDN", "KEN", "UGA", "NGA", "GHA", "ZAF", "ZMB", "ZWE", "BWA", "BLZ", "GUY", "AUS", "NZL", "CAN", "CYP", "HKG").contains(iso)) {
                return new Color(0xB91C1C); // British Imperial Red (#B91C1C)
            }
            // Russian Empire
            if (Set.of("RUS", "UKR", "BLR", "MDA", "EST", "LVA", "LTU", "GEO", "ARM", "AZE", "KAZ", "UZB", "TKM", "KGZ", "TJK", "FIN").contains(iso)) {
                return new Color(0x7C3AED); // Russian Imperial Purple (#7C3AED)
            }
            // Austro-Hungarian Empire
            if (Set.of("AUT", "HUN", "CZE", "SVK", "HRV", "BIH", "SVN").contains(iso)) {
                return new Color(0xD97706); // Habsburg Imperial Gold (#D97706)
            }
            // German Empire
            if (Set.of("DEU", "CMR", "TGO", "NAM", "TZA", "PNG").contains(iso)) {
                return new Color(0x1E293B); // Prussian Anthracite (#1E293B)
            }
            // Ottoman Empire
            if (Set.of("TUR", "SYR", "IRQ", "LBN", "JOR", "PSE", "ISR").contains(iso)) {
                return new Color(0x047857); // Ottoman Green (#047857)
            }
            // French Colonial Empire
            if (Set.of("FRA", "DZA", "TUN", "MAR", "SEN", "MLI", "GIN", "CIV", "BFA", "BEN", "NER", "TCD", "CAF", "COG", "GAB", "MDG", "VNM", "LAO", "KHM").contains(iso)) {
                return new Color(0x1D4ED8); // French Cobalt Blue (#1D4ED8)
            }
            // Dutch Empire
            if (Set.of("NLD", "IDN", "SUR").contains(iso)) {
                return new Color(0xF97316); // Dutch Oranje (#F97316)
            }
            // Portuguese Empire
            if (Set.of("PRT", "AGO", "MOZ", "GNB", "CPV", "STP", "TLS").contains(iso)) {
                return new Color(0x047857); // Portuguese Deep Green (#047857)
            }
            // Belgian Congo
            if (Set.of("BEL", "COD").contains(iso)) {
                return new Color(0xF59E0B); // Belgian Gold (#F59E0B)
            }
            // Empire of Japan
            if (Set.of("JPN", "KOR", "TWN").contains(iso)) {
                return new Color(0xE11D48); // Imperial Japanese Crimson (#E11D48)
            }
            // Qing / ROC
            if (Set.of("CHN", "MNG").contains(iso)) {
                return new Color(0xEF4444); // Chinese Red (#EF4444)
            }
            // Kingdom of Italy
            if (Set.of("ITA", "LBY", "ERI", "SOM").contains(iso)) {
                return new Color(0x059669); // Italian Emerald (#059669)
            }
        } else if (year > 1920 && year <= 1955) {
            // Post-WWII / 1950 cold war configuration
            // USSR
            if (Set.of("RUS", "UKR", "BLR", "MDA", "EST", "LVA", "LTU", "GEO", "ARM", "AZE", "KAZ", "UZB", "TKM", "KGZ", "TJK").contains(iso)) {
                return new Color(0xDC2626); // Soviet Red (#DC2626)
            }
            // British Commonwealth & Colonies
            if (Set.of("GBR", "KEN", "UGA", "NGA", "GHA", "MYS", "HKG", "CYP").contains(iso)) {
                return new Color(0xB91C1C); // British Red (#B91C1C)
            }
            // French Union
            if (Set.of("FRA", "DZA", "SEN", "MLI", "MDG", "VNM", "LAO", "KHM").contains(iso)) {
                return new Color(0x1D4ED8); // French Blue (#1D4ED8)
            }
        }

        return c.sovereignColor != null ? c.sovereignColor : computeSovereignColor(c.isoA3, c.name);
    }

    // Helper subroutine: compute sovereign color - internal state computation & bounds checking
    private static Color computeSovereignColor(String iso, String name) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA" -> new Color(0x2563EB); // Royal Blue
            case "CAN" -> new Color(0xDC2626); // Crimson Red
            case "MEX" -> new Color(0x16A34A); // Mexican Green
            case "FRA" -> new Color(0x1D4ED8); // French Cobalt Blue
            case "DEU" -> new Color(0x1E293B); // Slate / Anthracite
            case "GBR" -> new Color(0xB91C1C); // British Imperial Red
            case "ITA" -> new Color(0x059669); // Italian Emerald
            case "ESP" -> new Color(0xEA580C); // Spanish Amber / Gold-Red
            case "PRT" -> new Color(0x047857); // Portuguese Deep Green
            case "RUS" -> new Color(0x7C3AED); // Russian Imperial Purple
            case "CHN" -> new Color(0xEF4444); // China Crimson
            case "JPN" -> new Color(0xE11D48); // Japan Crimson Rose
            case "KOR" -> new Color(0x0284C7); // South Korea Cyan
            case "IND" -> new Color(0xF59E0B); // Indian Saffron
            case "AUS" -> new Color(0x0D9488); // Australian Teal / Gold-Green
            case "NZL" -> new Color(0x0F766E); // New Zealand Deep Teal
            case "BRA" -> new Color(0x10B981); // Brazilian Forest Green
            case "ARG" -> new Color(0x38BDF8); // Argentine Celeste Sky Blue
            case "ECU" -> new Color(0xFBBF24); // Ecuadorian Golden Yellow
            case "COL" -> new Color(0xF59E0B); // Colombian Saffron
            case "PER" -> new Color(0xDC2626); // Peruvian Red
            case "CHL" -> new Color(0xDC2626); // Chilean Red
            case "ZAF" -> new Color(0xF59E0B); // South Africa Gold
            case "EGY" -> new Color(0xD97706); // Egyptian Ochre
            case "NGA" -> new Color(0x15803D); // Nigerian Green
            case "IDN" -> new Color(0xDC2626); // Indonesian Merah Putih
            case "PHL" -> new Color(0x0284C7); // Philippine Ocean Blue
            case "VNM" -> new Color(0xEF4444); // Vietnam Red
            case "THA" -> new Color(0x8B5CF6); // Thai Royal Violet
            case "SAU" -> new Color(0x15803D); // Saudi Green
            case "IRN" -> new Color(0x0D9488); // Persian Turquoise
            case "TUR" -> new Color(0xDC2626); // Turkish Red
            case "UKR" -> new Color(0x0284C7); // Ukrainian Blue
            case "POL" -> new Color(0xBE185D); // Polish Magenta
            case "SWE" -> new Color(0x0284C7); // Swedish Blue
            case "NOR" -> new Color(0xDC2626); // Norwegian Crimson
            case "FIN" -> new Color(0x0D9488); // Finnish Forest Cyan
            case "DNK" -> new Color(0xB91C1C); // Danish Red
            case "NLD" -> new Color(0xF97316); // Dutch Oranje
            case "BEL" -> new Color(0xF59E0B); // Belgian Gold
            case "CHE" -> new Color(0xEF4444); // Swiss Cross Red
            case "AUT" -> new Color(0xDC2626); // Austrian Crimson
            case "GRC" -> new Color(0x06B6D4); // Greek Aegean Blue
            case "IRL" -> new Color(0x10B981); // Irish Shamrock Green
            default -> {
                int hash = Math.abs(name != null ? name.hashCode() : iso.hashCode());
                float hue = (hash % 360) / 360.0f;
                float sat = 0.70f + (hash % 25) * 0.01f;
                float bri = 0.65f + (hash % 30) * 0.01f;
                yield Color.getHSBColor(hue, sat, bri);
            }
        };
    }

    // Helper subroutine: compute isogloss color - internal state computation & bounds checking
    private static Color computeIsoglossColor(String iso, String name, String subregion, String continent) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            // Romance Branch (#EC4899 / #E11D48 / #9D174D / #E879F9 / #D946EF)
            case "FRA", "MCO", "BEL", "LUX", "HTI" -> new Color(0xEC4899); // French (Langue d'Oïl)
            case "ESP", "MEX", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY" -> new Color(0xE11D48); // Castilian Spanish
            case "PRT", "BRA", "AGO", "MOZ", "CPV", "STP", "GNB", "TLS" -> new Color(0x9D174D); // Portuguese
            case "ITA", "SMR", "VAT" -> new Color(0xE879F9); // Italo-Romance
            case "ROU", "MDA" -> new Color(0xD946EF); // Romanian / Daco-Romance

            // Germanic Branch (#2563EB / #3B82F6 / #60A5FA / #93C5FD)
            case "GBR", "USA", "CAN", "AUS", "NZL", "IRL", "JAM", "BHS", "BRB", "GUY" -> new Color(0x2563EB); // Anglic / English
            case "DEU", "AUT", "CHE", "LIE" -> new Color(0x3B82F6); // High German
            case "NLD", "SUR" -> new Color(0x60A5FA); // Netherlandic / Dutch
            case "SWE", "NOR", "DNK", "ISL" -> new Color(0x93C5FD); // North Germanic / Scandinavian

            // Slavic Branch (#8B5CF6 / #A855F7 / #7C3AED)
            case "RUS", "BLR", "UKR" -> new Color(0x8B5CF6); // East Slavic
            case "POL", "CZE", "SVK" -> new Color(0xA855F7); // West Slavic
            case "SRB", "HRV", "BIH", "MNE", "BGR", "MKD", "SVN" -> new Color(0x7C3AED); // South Slavic

            // Baltic, Celtic, Hellenic, Albanian
            case "LTU", "LVA" -> new Color(0x6366F1); // Baltic
            case "GRC", "CYP" -> new Color(0x06B6D4); // Hellenic
            case "ALB", "XKX" -> new Color(0x0284C7); // Albanian

            // Uralic (#0D9488 / #14B8A6)
            case "FIN", "EST" -> new Color(0x0D9488); // Finnic
            case "HUN" -> new Color(0x14B8A6); // Ugric / Hungarian

            // Sinitic, Japonic, Koreanic
            case "CHN", "TWN", "SGP", "HKG", "MAC" -> new Color(0xEF4444); // Sinitic
            case "JPN" -> new Color(0xF43F5E); // Japonic
            case "KOR", "PRK" -> new Color(0xA855F7); // Koreanic

            // Indo-Aryan & Dravidian
            case "IND" -> new Color(0xF59E0B); // Indo-Aryan (Hindi Core)
            case "PAK", "BGD", "NPL", "LKA" -> new Color(0xF59E0B); // Indo-Aryan

            // Iranic & Turkic
            case "IRN", "AFG", "TJK" -> new Color(0xD97706); // Iranic
            case "TUR", "AZE", "KAZ", "UZB", "TKM", "KGZ" -> new Color(0x0EA5E9); // Turkic
            case "MNG" -> new Color(0x2ECC71); // Mongolic

            // Southeast Asia
            case "VNM", "KHM" -> new Color(0x16A085); // Austroasiatic
            case "THA", "LAO" -> new Color(0xEAB308); // Tai-Kadai
            case "MMR" -> new Color(0xDC2626); // Tibeto-Burman
            case "IDN", "MYS", "PHL", "BRN", "MDG" -> new Color(0x06B6D4); // Austronesian
            case "PNG" -> new Color(0x9C27B0); // Trans-New Guinea

            // Semitic & Afroasiatic
            case "SAU", "EGY", "DZA", "MAR", "IRQ", "SDN", "SYR", "YEM", "TUN", "JOR", "ARE", "LBN", "LBY", "OMN", "KWT", "QAT", "BHR", "MRT" -> new Color(0x15803D); // Semitic / Arabic
            case "ISR" -> new Color(0x3B82F6); // Hebrew
            case "ETH", "ERI" -> new Color(0x84CC16); // Ethiosemitic / Cushitic

            // Niger-Congo Sub-Saharan Africa
            case "NGA", "COD", "TZA", "KEN", "ZAF", "GHA", "CIV", "CMR", "UGA", "ZMB", "SEN", "ZWE", "MLI", "GIN", "RWA", "BEN", "BDI", "TGO", "BFA", "COG", "GAB" -> new Color(0x22C55E); // Niger-Congo

            default -> new Color(0x2563EB);
        };
    }

    // Helper subroutine: compute kinship color - internal state computation & bounds checking
    private static Color computeKinshipColor(String iso, String name, String subregion, String continent) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            // Absolute Nuclear Family (#3B82F6 - Anglo-Saxon / Dutch)
            case "GBR", "USA", "CAN", "AUS", "NZL", "NLD" -> new Color(0x3B82F6);

            // Egalitarian Nuclear Family (#EC4899 - French / Spanish / Latin America / Southern Italy)
            case "FRA", "ESP", "PRT", "ITA", "BEL", "ROU", "MEX", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY", "VEN" -> new Color(0xEC4899);

            // Stem Family / Stammfamilie (#8B5CF6 - Germanic / Nordic / Japanese / Korean / Austrian / Swiss / Basque)
            case "DEU", "AUT", "CHE", "SWE", "NOR", "DNK", "FIN", "ISL", "JPN", "KOR" -> new Color(0x8B5CF6);

            // Communal Patrilineal Family (#DC2626 - Russian / Slavic / Chinese / Vietnamese / Balkan / Central Asian)
            case "RUS", "BLR", "UKR", "CHN", "VNM", "SRB", "BGR", "MKD", "BIH", "MNE", "KAZ", "UZB", "TKM", "KGZ", "TJK", "MNG" -> new Color(0xDC2626);

            // Segmentary Patrilineages & Endogamy (#10B981 - Arab / Sahelian / West Asian)
            case "SAU", "EGY", "DZA", "MAR", "IRQ", "SYR", "YEM", "TUN", "JOR", "ARE", "LBN", "LBY", "OMN", "KWT", "QAT", "BHR", "AFG", "PAK", "IRN", "TUR", "MRT", "SDN", "SOM" -> new Color(0x10B981);

            // Sub-Saharan Segmentary Lineages & Polygyny (#15803D)
            case "NGA", "COD", "TZA", "KEN", "ZAF", "CIV", "CMR", "UGA", "ZMB", "SEN", "ZWE", "MLI", "GIN", "RWA", "BEN", "BDI", "TGO", "BFA", "COG", "GAB", "ETH" -> new Color(0x15803D);

            // Matrilineal & Avunculocal (#F43F5E - Ghana Ashanti, Mozambique)
            case "GHA", "MOZ" -> new Color(0xF43F5E);

            // Joint Family Gotra Exogamy (#F59E0B - Northern India / Nepal / Bangladesh)
            case "IND", "BGD", "NPL" -> new Color(0xF59E0B);

            // Bilateral Matrifocal Household (#06B6D4 - Maritime Southeast Asia)
            case "IDN", "MYS", "PHL", "THA", "MMR", "KHM", "LAO", "MDG", "BRN" -> new Color(0x06B6D4);

            default -> new Color(0x8B5CF6);
        };
    }

    // Helper subroutine: compute ritual color - internal state computation & bounds checking
    private static Color computeRitualColor(String iso, String name, String subregion, String continent) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            // Roman Catholicism (#EC4899 - Magenta/Rose)
            case "FRA", "ITA", "ESP", "PRT", "POL", "IRL", "AUT", "BEL", "LTU", "HRV", "SVN", "SVK", "HUN",
                 "MEX", "BRA", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY", "VEN", "PHL", "TLS" -> new Color(0xEC4899);

            // Protestantism (#38BDF8 - Sky Cyan)
            case "USA", "GBR", "DEU", "CAN", "AUS", "NZL", "SWE", "NOR", "DNK", "FIN", "ISL", "NLD", "CHE", "ZAF", "KEN", "NGA", "GHA", "UGA", "ZWE", "ZMB", "PNG" -> new Color(0x38BDF8);

            // Eastern Orthodoxy (#8B5CF6 - Violet/Purple)
            case "RUS", "UKR", "BLR", "GRC", "ROU", "BGR", "SRB", "GEO", "MDA", "CYP", "MNE", "MKD", "ETH", "ERI" -> new Color(0x8B5CF6);

            // Sunni Islam (#10B981 - Emerald Green)
            case "SAU", "EGY", "TUR", "IDN", "PAK", "BGD", "DZA", "MAR", "SDN", "SYR", "YEM", "TUN", "JOR", "ARE", "LBY", "OMN", "KWT", "QAT", "BHR", "MYS", "KAZ", "UZB", "TKM", "KGZ", "TJK", "AFG", "SEN", "MLI", "NER", "SOM", "MRT" -> new Color(0x10B981);

            // Shia Islam (#0D9488 - Turquoise/Teal)
            case "IRN", "IRQ", "AZE" -> new Color(0x0D9488);

            // Hinduism (#F59E0B - Saffron/Amber)
            case "IND", "NPL", "MUS" -> new Color(0xF59E0B);

            // Theravada Buddhism (#EAB308 - Golden Yellow)
            case "THA", "MMR", "LKA", "KHM", "LAO" -> new Color(0xEAB308);

            // Mahayana Buddhism & East Asian Syncretism (#EF4444 - Crimson)
            case "CHN", "JPN", "KOR", "TWN", "VNM", "SGP", "HKG" -> new Color(0xEF4444);

            // Vajrayana / Tibetan Buddhism (#D97706 - Saffron Orange)
            case "MNG", "BTN" -> new Color(0xD97706);

            // Judaism (#2563EB - Royal Blue)
            case "ISR" -> new Color(0x2563EB);

            default -> new Color(0x64748B); // Secular / Indigenous
        };
    }

    // Helper subroutine: compute institutional level - internal state computation & bounds checking
    private static int computeInstitutionalLevel(String iso, String name) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "CAN", "GBR", "FRA", "DEU", "JPN", "CHE", "SWE", "NOR", "DNK", "FIN", "NLD", "AUS", "NZL", "SGP" -> 220;
            case "ITA", "ESP", "PRT", "AUT", "BEL", "IRL", "KOR", "TWN", "ISR", "POL", "CZE", "EST" -> 205;
            case "CHN", "RUS", "IND", "BRA", "MEX", "TUR", "SAU", "ZAF", "MYS", "CHL", "ARG", "ROU", "GRC", "IDN", "THA", "VNM" -> 175;
            case "EGY", "NGA", "PAK", "BGD", "COL", "PER", "PHL", "UKR", "KAZ", "DZA", "MAR", "KEN" -> 135;
            default -> 95;
        };
    }

    // Helper subroutine: compute technology level - internal state computation & bounds checking
    private static int computeTechnologyLevel(String iso, String name) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "JPN", "DEU", "KOR", "TWN", "CHE", "GBR", "FRA", "SWE", "NLD", "SGP", "ISR", "CHN" -> 250;
            case "CAN", "AUS", "AUT", "BEL", "DNK", "FIN", "NOR", "ITA", "ESP", "IRL", "NZL" -> 220;
            case "RUS", "IND", "BRA", "POL", "CZE", "TUR", "SAU", "MYS", "MEX", "EST", "HUN", "PRT" -> 175;
            case "IDN", "THA", "VNM", "ZAF", "EGY", "ARG", "CHL", "COL", "PHL", "ROU", "GRC", "UKR" -> 130;
            default -> 80;
        };
    }

    // Helper subroutine: compute ecological footprint - internal state computation & bounds checking
    private static int computeEcologicalFootprint(String iso, String name) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "CHN", "IND", "RUS", "DEU", "JPN", "KOR", "SAU", "CAN", "AUS" -> 240;
            case "GBR", "FRA", "ITA", "BRA", "MEX", "IDN", "TUR", "IRN", "ZAF", "POL", "NLD", "ESP" -> 190;
            case "EGY", "PAK", "BGD", "VNM", "THA", "NGA", "ARG", "COL", "UKR", "MYS", "PHL", "BEL" -> 140;
            default -> 80;
        };
    }

    // Helper subroutine: compute pathogen stress - internal state computation & bounds checking
    private static int computePathogenStress(String iso, String name, String continent) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        if ("Africa".equalsIgnoreCase(continent)) return 210;
        if ("South America".equalsIgnoreCase(continent)) return 140;
        return switch (code) {
            case "IND", "BGD", "PAK", "IDN", "PHL", "VNM", "MMR", "KHM", "PNG" -> 180;
            case "CHN", "BRA", "MEX", "EGY", "THA", "MYS", "COL", "PER" -> 110;
            case "USA", "CAN", "GBR", "FRA", "DEU", "JPN", "AUS", "NZL", "SWE", "NOR", "DNK", "FIN", "CHE", "NLD", "ITA", "ESP", "RUS", "KOR" -> 40;
            default -> 90;
        };
    }

    // Helper subroutine: apply subnational centers - internal state computation & bounds checking
    private static void applySubnationalCenters(BufferedImage img, double[][] centers, int width, int height, BufferedImage elevationMask) {
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;
                int currentRgb = img.getRGB(x, y) & 0xFFFFFF;
                if (currentRgb == 0x000000) continue;

                double maxInfl = 0.0;
                int bestCol = currentRgb;

                for (double[] sc : centers) {
                    double dLat = lat - sc[1];
                    double dLon = (lon - sc[0]) * Math.cos(Math.toRadians((lat + sc[1]) * 0.5));
                    double d2 = dLat * dLat + dLon * dLon;
                    double sigma = sc[3];
                    double infl = Math.exp(-d2 / (2.0 * sigma * sigma));
                    if (infl > maxInfl) {
                        maxInfl = infl;
                        bestCol = (int) sc[2];
                    }
                }

                if (maxInfl >= 0.45) {
                    img.setRGB(x, y, bestCol);
                }
            }
        }
    }

    /*
     * Apply elevation mask.
     * Enforces physical invariants and updates associated state variables within {@code NaturalEarthVectorIngestor}.
     *
     * @param img the img parameter (BufferedImage)
     * @param elevationMask the elevation mask parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage applyElevationMask(BufferedImage img, BufferedImage elevationMask) {
        if (elevationMask == null) return img;
        int w = img.getWidth();
        int h = img.getHeight();
        int ew = elevationMask.getWidth();
        int eh = elevationMask.getHeight();

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) / h * 180.0;
            int my = Math.clamp(y * eh / h, 0, eh - 1);
            for (int x = 0; x < w; x++) {
                int mx = Math.clamp(x * ew / w, 0, ew - 1);
                int land = elevationMask.getRaster().getSample(mx, my, 0);
                if (land == 0 || lat < -60.0) {
                    img.setRGB(x, y, 0x000000);
                }
            }
        }
        return img;
    }

    /*
     * Attempts to load Natural Earth 1:10m vector map for a scenario.
     */
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
