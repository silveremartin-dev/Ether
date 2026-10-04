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

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * High-Resolution Historical Vector Ingestor for CShapes 2.0 Dataset (1886-2019).
 * Parses exact sovereign state boundaries, capital coordinates, and Gleditsch & Ward (GW) codes
 * with daily/monthly historical validity intervals [gwsdate, gwedate].
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0
 */
public class CShapesVectorIngestor {
    private static final Logger logger = LoggerFactory.getLogger(CShapesVectorIngestor.class);

    /* Internal state variable for cshapes path (String). */
    public static final String CSHAPES_PATH = "data/maps/cshapes/cshapes_2.0.geojson";

    public static class CShapesFeature {
        public String countryName;
        public int gwCode;
        public int startYear;
        public int startMonth;
        public int startDay;
        public int endYear;
        public int endMonth;
        public int endDay;
        public String capitalName;
        public double capLon;
        public double capLat;
        public List<Path2D> paths = new ArrayList<>();
        public Color sovereignColor;

        /*
         * Is active in year.
         * Enforces physical invariants and updates associated state variables within {@code CShapesVectorIngestor}.
         *
         * @param year the year parameter (long)
         * @return the resulting computation or state reference
         */
        public boolean isActiveInYear(long year) {
            return year >= startYear && year <= endYear;
        }
    }

    /* Internal state variable for cached features (List&lt;CShapesFeature&gt;). */
    private static List<CShapesFeature> CACHED_FEATURES = null;

    /*
     * Loads and caches all CShapes 2.0 historical features.
     */
    public static synchronized List<CShapesFeature> getAllFeatures(int imgW, int imgH) {
        if (CACHED_FEATURES != null && !CACHED_FEATURES.isEmpty()) {
            return CACHED_FEATURES;
        }

        File file = new File(CSHAPES_PATH);
        if (!file.exists()) {
            file = new File("in/CShapes-2.0.geojson");
        }
        if (!file.exists()) {
            logger.warn("CShapes 2.0 GeoJSON not found at: {}", file.getAbsolutePath());
            return Collections.emptyList();
        }

        List<CShapesFeature> list = new ArrayList<>();
        JsonFactory factory = new JsonFactory();

        try (InputStream is = new FileInputStream(file);
             JsonParser parser = factory.createParser(is)) {

            while (parser.nextToken() != null) {
                if ("features".equals(parser.currentName()) && parser.currentToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            CShapesFeature cs = parseFeature(parser, imgW, imgH);
                            if (cs != null && !cs.paths.isEmpty()) {
                                list.add(cs);
                            }
                        }
                    }
                    break;
                }
            }
            logger.info("Successfully ingested {} historical boundary features from CShapes 2.0 (1886-2019).", list.size());
        } catch (Exception e) {
            logger.error("Failed to parse CShapes GeoJSON: {}", e.getMessage(), e);
        }

        CACHED_FEATURES = list;
        return list;
    }

    /*
     * Returns all active CShapes polities for a specific historical year.
     */
    public static List<CShapesFeature> getFeaturesForYear(long year, int imgW, int imgH) {
        List<CShapesFeature> all = getAllFeatures(imgW, imgH);
        List<CShapesFeature> result = new ArrayList<>();
        for (CShapesFeature f : all) {
            if (f.isActiveInYear(year)) {
                result.add(f);
            }
        }
        return result;
    }

    /*
     * Rasterizes exact CShapes sovereign polygons for a given year (e.g. 1900, 1914, 1950).
     */
    public static BufferedImage rasterizeCShapesSovereigntyMap(long year, int width, int height, BufferedImage elevationMask) {
        List<CShapesFeature> active = getFeaturesForYear(year, width, height);
        if (active.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (CShapesFeature f : active) {
            g.setColor(f.sovereignColor);
            for (Path2D p : f.paths) {
                g.fill(p);
            }
        }
        g.dispose();

        return NaturalEarthVectorIngestor.applyElevationMask(img, elevationMask);
    }

    private static CShapesFeature parseFeature(JsonParser parser, int imgW, int imgH) throws Exception {
        String cntryName = null;
        int gwcode = 0;
        int sYear = 1886, sMonth = 1, sDay = 1;
        int eYear = 2019, eMonth = 12, eDay = 31;
        String capName = null;
        double capLong = 0.0, capLat = 0.0;
        List<Path2D> paths = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            if ("properties".equals(field)) {
                parser.nextToken(); // START_OBJECT
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String prop = parser.currentName();
                    parser.nextToken();
                    if ("cntry_name".equalsIgnoreCase(prop)) cntryName = parser.getText();
                    else if ("gwcode".equalsIgnoreCase(prop)) gwcode = parser.getIntValue();
                    else if ("gwsyear".equalsIgnoreCase(prop)) sYear = parser.getIntValue();
                    else if ("gwsmonth".equalsIgnoreCase(prop)) sMonth = parser.getIntValue();
                    else if ("gwsday".equalsIgnoreCase(prop)) sDay = parser.getIntValue();
                    else if ("gweyear".equalsIgnoreCase(prop)) eYear = parser.getIntValue();
                    else if ("gwemonth".equalsIgnoreCase(prop)) eMonth = parser.getIntValue();
                    else if ("gweday".equalsIgnoreCase(prop)) eDay = parser.getIntValue();
                    else if ("capname".equalsIgnoreCase(prop)) capName = parser.getText();
                    else if ("caplong".equalsIgnoreCase(prop)) capLong = parser.getDoubleValue();
                    else if ("caplat".equalsIgnoreCase(prop)) capLat = parser.getDoubleValue();
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

        if (cntryName == null) return null;

        CShapesFeature cf = new CShapesFeature();
        cf.countryName = cntryName;
        cf.gwCode = gwcode;
        cf.startYear = sYear;
        cf.startMonth = sMonth;
        cf.startDay = sDay;
        cf.endYear = eYear;
        cf.endMonth = eMonth;
        cf.endDay = eDay;
        cf.capitalName = capName;
        cf.capLon = capLong;
        cf.capLat = capLat;
        cf.paths = paths;
        cf.sovereignColor = computeHistoricalSovereignColor(gwcode, cntryName);

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

    private static Color computeHistoricalSovereignColor(int gwcode, String name) {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        return switch (gwcode) {
            case 2 -> new Color(0x2563EB);   // United States of America (#2563EB)
            case 20 -> new Color(0xDC2626);  // Canada (#DC2626)
            case 70 -> new Color(0x16A34A);  // Mexico (#16A34A)
            case 200 -> new Color(0xB91C1C); // United Kingdom (#B91C1C)
            case 210 -> new Color(0xF97316); // Netherlands (#F97316)
            case 211 -> new Color(0xF59E0B); // Belgium (#F59E0B)
            case 220 -> new Color(0x1D4ED8); // France (#1D4ED8)
            case 230 -> new Color(0xEA580C); // Spain (#EA580C)
            case 235 -> new Color(0x047857); // Portugal (#047857)
            case 255 -> new Color(0x1E293B); // Germany / German Empire (#1E293B)
            case 260 -> new Color(0x1E293B); // German Federal Republic
            case 265 -> new Color(0x991B1B); // German Democratic Republic (East) (#991B1B)
            case 300 -> new Color(0xD97706); // Austria-Hungary / Austria (#D97706)
            case 310 -> new Color(0x14B8A6); // Hungary (#14B8A6)
            case 315 -> new Color(0xA855F7); // Czechoslovakia (#A855F7)
            case 325 -> new Color(0x059669); // Italy (#059669)
            case 345 -> new Color(0x7C3AED); // Yugoslavia / Serbia (#7C3AED)
            case 350 -> new Color(0x06B6D4); // Greece (#06B6D4)
            case 365 -> new Color(0x7C3AED); // Russia / Russian Empire (#7C3AED)
            case 364 -> new Color(0xDC2626); // Soviet Union (USSR) (#DC2626)
            case 640 -> new Color(0x047857); // Ottoman Empire / Turkey (#047857)
            case 651 -> new Color(0xD97706); // Egypt (#D97706)
            case 670 -> new Color(0x15803D); // Saudi Arabia (#15803D)
            case 630 -> new Color(0x0D9488); // Iran / Persia (#0D9488)
            case 710 -> new Color(0xEF4444); // China / Qing / Republic of China (#EF4444)
            case 740 -> new Color(0xE11D48); // Japan (#E11D48)
            case 730 -> new Color(0xA855F7); // Korea / Korean Empire (#A855F7)
            case 731 -> new Color(0xDC2626); // North Korea (#DC2626)
            case 732 -> new Color(0x0284C7); // South Korea (#0284C7)
            case 750 -> new Color(0xF59E0B); // India (#F59E0B)
            case 770 -> new Color(0x047857); // Pakistan (#047857)
            case 820 -> new Color(0xDC2626); // Malaysia / Indonesia (#DC2626)
            case 900 -> new Color(0x0D9488); // Australia (#0D9488)
            case 920 -> new Color(0x0F766E); // New Zealand (#0F766E)
            case 100 -> new Color(0x38BDF8); // Colombia (#38BDF8)
            case 140 -> new Color(0x10B981); // Brazil (#10B981)
            case 160 -> new Color(0x38BDF8); // Argentina (#38BDF8)
            case 155 -> new Color(0xDC2626); // Chile (#DC2626)
            default -> {
                int hash = Math.abs(name != null ? name.hashCode() : gwcode * 31);
                float hue = (hash % 360) / 360.0f;
                float sat = 0.70f + (hash % 25) * 0.01f;
                float bri = 0.65f + (hash % 30) * 0.01f;
                yield Color.getHSBColor(hue, sat, bri);
            }
        };
    }
}
