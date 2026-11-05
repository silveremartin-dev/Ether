/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ultra-High Fidelity SVG Cartographic Ingestor & Vector Multi-Channel Rasterizer for Ether Engine.
 * Parses SVG maps (equirectangular or projected vector boundaries) and converts them into
 * 5 multi-channel spatial tensors (Density, Sovereignty, Isogloss, Kinship, Rituals).
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.2
 */
public class SvgMapIngestor {
    private static final Logger logger = LoggerFactory.getLogger(SvgMapIngestor.class);

    /** Internal state: width (int). */
    public static final int WIDTH = 1024;
    /** Internal state: height (int). */
    public static final int HEIGHT = 512;

    public static class SvgLayerFeature {
        public String layerType; // DENSITY, SOVEREIGNTY, ISOGLOSS, KINSHIP, RITUALS
        public String id;
        public String name;
        public Color color;
        public double densityWeight = 0.5;
        public Path2D geometry;
        public double minLng = -180, maxLng = 180, minLat = -90, maxLat = 90;

        /**
         * Svg layer feature.
         *
         * @param layerType the layer type parameter (String)
         * @param id the id parameter (String)
         * @param color the color parameter (Color)
         * @param geometry the geometry parameter (Path2D)
         * @return the resulting computation or state reference
         */
        public SvgLayerFeature(String layerType, String id, Color color, Path2D geometry) {
            this.layerType = layerType;
            this.id = id;
            this.color = color != null ? color : Color.WHITE;
            this.geometry = geometry;
        }
    }

    public static class SvgIngestionResult {
        public BufferedImage densityImage;
        public BufferedImage sovereigntyImage;
        public BufferedImage isoglossImage;
        public BufferedImage kinshipImage;
        public BufferedImage ritualsImage;

        public String densityBase64;
        public String sovereigntyBase64;
        public String isoglossBase64;
        public String kinshipBase64;
        public String ritualsBase64;
        public int parsedFeaturesCount;
    }

    /**
     * Ingests SVG maps for a given scenario.
     * Searches external filesystem directory 'data/maps/svg/' first, then falls back to classpath.
     */
    public static SvgIngestionResult ingestForScenario(String scenarioType) {
        if (scenarioType == null || scenarioType.isBlank()) return null;

        String fileName = scenarioType.toLowerCase(Locale.ROOT) + ".svg";
        List<java.io.File> searchFiles = List.of(
            new java.io.File("data/maps/svg/" + fileName),
            new java.io.File("user_data/maps/svg/" + fileName),
            new java.io.File("../data/maps/svg/" + fileName)
        );

        for (java.io.File file : searchFiles) {
            if (file.exists() && file.isFile()) {
                try (InputStream is = new java.io.FileInputStream(file)) {
                    logger.info("Ingesting external SVG cartographic asset for scenario '{}': {}", scenarioType, file.getAbsolutePath());
                    return ingestSvg(is);
                } catch (Exception e) {
                    logger.warn("Failed to read external SVG file '{}': {}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }

        // Search classpath fallback
        String resourcePath = "/maps/svg/" + fileName;
        try (InputStream is = SvgMapIngestor.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                logger.info("Ingesting classpath SVG cartographic asset for scenario '{}': {}", scenarioType, resourcePath);
                return ingestSvg(is);
            }
        } catch (Exception e) {
            logger.warn("Failed to load embedded SVG resource for scenario '{}': {}", scenarioType, e.getMessage());
        }

        // Try fallback default map names if specific name not found
        String fallbackFileName = getFallbackSvgFileName(scenarioType);
        if (fallbackFileName != null) {
            java.io.File fallbackFile = new java.io.File("data/maps/svg/" + fallbackFileName);
            if (fallbackFile.exists() && fallbackFile.isFile()) {
                try (InputStream is = new java.io.FileInputStream(fallbackFile)) {
                    logger.info("Ingesting external fallback SVG cartographic asset for scenario '{}': {}", scenarioType, fallbackFile.getAbsolutePath());
                    return ingestSvg(is);
                } catch (Exception e) {
                    logger.warn("Failed to read fallback SVG file '{}': {}", fallbackFile.getAbsolutePath(), e.getMessage());
                }
            }

            String fallbackResourcePath = "/maps/svg/" + fallbackFileName;
            try (InputStream is = SvgMapIngestor.class.getResourceAsStream(fallbackResourcePath)) {
                if (is != null) {
                    logger.info("Ingesting classpath fallback SVG cartographic asset for scenario '{}': {}", scenarioType, fallbackResourcePath);
                    return ingestSvg(is);
                }
            } catch (Exception e) {
                logger.warn("Failed to load fallback SVG resource '{}': {}", fallbackResourcePath, e.getMessage());
            }
        }

        return null;
    }

    private static String getFallbackSvgFileName(String scenarioType) {
        String typeUpper = scenarioType.toUpperCase(Locale.ROOT);
        if (typeUpper.contains("ROMAN")) return "roman_empire_0.svg";
        if (typeUpper.contains("MALI")) return "mali_empire_1324.svg";
        if (typeUpper.contains("SONG")) return "song_dynasty_1000.svg";
        if (typeUpper.contains("FERTILE") || typeUpper.contains("MESOPOTAMIA")) return "fertile_crescent_8000bc.svg";
        if (typeUpper.contains("AMERICA")) return "americas_1491.svg";
        if (typeUpper.contains("AFRICA") || typeUpper.contains("CONTINENT")) return "out_of_africa_100k.svg";
        if (typeUpper.contains("JAPAN") || typeUpper.contains("SAKOKU")) return "japan_sakoku_1639.svg";
        if (typeUpper.contains("INDUSTRIAL")) return "industrial_1800.svg";
        if (typeUpper.contains("URBAN") || typeUpper.contains("MODERN") || typeUpper.contains("SSP5")) return "anthropocene_2000.svg";
        return null;
    }

    /**
     * Main entry point to ingest SVG content from an InputStream.
     */
    public static SvgIngestionResult ingestSvg(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {}
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        Element svgRoot = doc.getDocumentElement();
        double[] viewBox = parseViewBox(svgRoot);

        List<SvgLayerFeature> features = new ArrayList<>();
        parseElementNode(svgRoot, viewBox, features, "ALL", Color.WHITE);

        return rasterizeFeatures(features);
    }

    /**
     * Ingests SVG from a raw String content.
     */
    public static SvgIngestionResult ingestSvgContent(String svgContent) throws Exception {
        if (svgContent == null || svgContent.isBlank()) return null;
        try (InputStream is = new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8))) {
            return ingestSvg(is);
        }
    }

    /**
     * Applies the SvgIngestionResult to a Scenario model.
     */
    public static void applyToScenario(Scenario scenario, SvgIngestionResult result) {
        if (scenario == null || result == null) return;

        if (result.densityBase64 != null) scenario.setCustomDensityBase64(result.densityBase64);
        if (result.isoglossBase64 != null) scenario.setCustomTensorMapBase64(0, result.isoglossBase64);
        if (result.kinshipBase64 != null) scenario.setCustomTensorMapBase64(1, result.kinshipBase64);
        if (result.ritualsBase64 != null) scenario.setCustomTensorMapBase64(2, result.ritualsBase64);
        if (result.sovereigntyBase64 != null) scenario.setCustomTensorMapBase64(3, result.sovereigntyBase64);

        logger.info("Successfully applied SVG vector cartography ({} parsed features) to scenario '{}'",
                result.parsedFeaturesCount, scenario.getName());
    }

    // --- SVG DOM PARSING & RASTERIZATION ---

    private static double[] parseViewBox(Element svgRoot) {
        String vb = svgRoot.getAttribute("viewBox");
        if (vb != null && !vb.isBlank()) {
            String[] parts = vb.trim().split("[\\s,]+");
            if (parts.length >= 4) {
                try {
                    return new double[]{
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]),
                            Double.parseDouble(parts[3])
                    };
                } catch (NumberFormatException ignored) {}
            }
        }
        double w = parseDimension(svgRoot.getAttribute("width"), 1024);
        double h = parseDimension(svgRoot.getAttribute("height"), 512);
        return new double[]{0, 0, w, h};
    }

    private static double parseDimension(String val, double defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        String clean = val.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static void parseElementNode(Node node, double[] viewBox, List<SvgLayerFeature> features,
                                         String currentLayerType, Color parentColor) {
        if (node.getNodeType() != Node.ELEMENT_NODE) return;
        Element elem = (Element) node;
        String tagName = elem.getTagName().toLowerCase(Locale.ROOT);

        // Inherit or detect layer type
        String layerType = currentLayerType;
        if (elem.hasAttribute("data-layer")) {
            layerType = elem.getAttribute("data-layer").toUpperCase(Locale.ROOT);
        } else if (elem.hasAttribute("id")) {
            String id = elem.getAttribute("id").toLowerCase(Locale.ROOT);
            if (id.contains("sovereign") || id.contains("polity") || id.contains("empire") || id.contains("border")) layerType = "SOVEREIGNTY";
            else if (id.contains("isogloss") || id.contains("lang") || id.contains("linguistic")) layerType = "ISOGLOSS";
            else if (id.contains("kinship") || id.contains("lineage") || id.contains("clan")) layerType = "KINSHIP";
            else if (id.contains("ritual") || id.contains("sacred") || id.contains("cult") || id.contains("temple")) layerType = "RITUALS";
            else if (id.contains("density") || id.contains("pop") || id.contains("urban")) layerType = "DENSITY";
        }

        // Color & fill parsing
        Color elemColor = parseColorAttribute(elem, parentColor);

        // Path / Geometry parsing
        Path2D geometry = null;
        if ("path".equals(tagName)) {
            String d = elem.getAttribute("d");
            if (d != null && !d.isBlank()) {
                geometry = parseSvgPathD(d, viewBox);
            }
        } else if ("polygon".equals(tagName)) {
            geometry = SvgGeometryParser.parsePolygonPoints(elem.getAttribute("points"), viewBox);
        } else if ("rect".equals(tagName)) {
            geometry = SvgGeometryParser.parseRect(elem, viewBox);
        } else if ("circle".equals(tagName)) {
            geometry = SvgGeometryParser.parseCircle(elem, viewBox);
        }

        if (geometry != null) {
            String id = elem.hasAttribute("id") ? elem.getAttribute("id") : "feature_" + features.size();
            SvgLayerFeature feature = new SvgLayerFeature(layerType, id, elemColor, geometry);
            if (elem.hasAttribute("data-density")) {
                try {
                    feature.densityWeight = Double.parseDouble(elem.getAttribute("data-density"));
                } catch (Exception ignored) {}
            }
            features.add(feature);
        }

        // Recursive traversal of children
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            parseElementNode(children.item(i), viewBox, features, layerType, elemColor);
        }
    }

    private static Color parseColorAttribute(Element elem, Color defaultColor) {
        String fill = elem.getAttribute("fill");
        if (fill == null || fill.isBlank() || "none".equalsIgnoreCase(fill)) {
            String style = elem.getAttribute("style");
            if (style != null && style.contains("fill:")) {
                fill = extractCssProperty(style, "fill");
            }
        }
        if (fill != null && !fill.isBlank() && !"none".equalsIgnoreCase(fill)) {
            Color c = parseHexOrRgbColor(fill);
            if (c != null) return c;
        }
        return defaultColor;
    }

    private static String extractCssProperty(String style, String prop) {
        for (String pair : style.split(";")) {
            String[] kv = pair.split(":");
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(prop)) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private static Color parseHexOrRgbColor(String colorStr) {
        if (colorStr == null) return null;
        colorStr = colorStr.trim().toLowerCase(Locale.ROOT);
        try {
            if (colorStr.startsWith("#")) {
                if (colorStr.length() == 4) { // #RGB format
                    char r = colorStr.charAt(1), g = colorStr.charAt(2), b = colorStr.charAt(3);
                    return Color.decode("#" + r + r + g + g + b + b);
                }
                return Color.decode(colorStr);
            } else if (colorStr.startsWith("rgb")) {
                Matcher m = Pattern.compile("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)").matcher(colorStr);
                if (m.find()) {
                    return new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Delegated Geometry & Rasterization Methods

    public static Path2D parseSvgPathD(String d, double[] viewBox) {
        return SvgGeometryParser.parseSvgPathD(d, viewBox);
    }

    public static class Point2D {
        public double x, y;
        public Point2D(double x, double y) { this.x = x; this.y = y; }
    }

    public static SvgIngestionResult rasterizeFeatures(List<SvgLayerFeature> features) {
        return SvgFeatureRasterizer.rasterizeFeatures(features);
    }
}
