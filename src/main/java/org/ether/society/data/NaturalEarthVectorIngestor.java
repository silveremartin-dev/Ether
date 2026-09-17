/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

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
import java.util.ArrayList;
import java.util.List;

/**
 * High-Precision Ingestor for Natural Earth 1:10m Vector Datasets (Countries, Coastlines, Rivers, Lakes).
 * Rasterizes high-definition vector features into 1024x512 multi-channel spatial tensors.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class NaturalEarthVectorIngestor {
    private static final Logger logger = LoggerFactory.getLogger(NaturalEarthVectorIngestor.class);

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;

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

    /**
     * Ingests external SVG / GeoJSON vector cartography for Natural Earth.
     */
    public static BufferedImage rasterizeVectorFeatures(List<VectorFeature> features, String layerFilter) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

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

    /**
     * Attempts to load Natural Earth 1:10m vector map for a scenario.
     */
    public static SvgMapIngestor.SvgIngestionResult loadNaturalEarthMap(String scenarioType) {
        String fileName = "ne_10m_" + scenarioType.toLowerCase() + ".svg";
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

