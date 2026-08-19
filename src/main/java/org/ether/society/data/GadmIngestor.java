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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * GADM (Global Administrative Areas) Ingestor.
 * Parses sub-national administrative divisions (Level 1 regions, Level 2 departments/districts, Level 3 municipalities)
 * for ultra-fine spatial resolution in European and regional historical scenarios.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 4.0.0
 */
public class GadmIngestor {
    private static final Logger logger = LoggerFactory.getLogger(GadmIngestor.class);

    public static class AdminRegion {
        public String countryCode; // e.g. "FRA", "DEU", "ITA"
        public String regionName;  // e.g. "Île-de-France", "Bavaria", "Tuscany"
        public int adminLevel;      // 1 = Region/State, 2 = Department/Kreis, 3 = Canton
        public Path2D boundary;
        public Color color;

        public AdminRegion(String countryCode, String regionName, int adminLevel, Path2D boundary, Color color) {
            this.countryCode = countryCode;
            this.regionName = regionName;
            this.adminLevel = adminLevel;
            this.boundary = boundary;
            this.color = color != null ? color : Color.LIGHT_GRAY;
        }
    }

    /**
     * Rasterizes GADM administrative boundaries into a high-definition 1024x512 region mask.
     */
    public static BufferedImage renderAdminRegions(List<AdminRegion> regions) {
        BufferedImage img = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1024, 512);

        for (AdminRegion r : regions) {
            if (r.boundary == null) continue;
            g.setColor(r.color);
            g.fill(r.boundary);
            g.setColor(Color.DARK_GRAY);
            g.draw(r.boundary);
        }

        g.dispose();
        return img;
    }

    /**
     * Attempts to load GADM data for a country code.
     */
    public static SvgMapIngestor.SvgIngestionResult loadGadmMap(String countryCode) {
        String fileName = "gadm_" + countryCode.toLowerCase() + ".svg";
        File file = new File("data/maps/gadm/" + fileName);

        if (file.exists() && file.isFile()) {
            try (InputStream is = new FileInputStream(file)) {
                logger.info("Ingesting GADM administrative boundaries for '{}': {}", countryCode, file.getAbsolutePath());
                return SvgMapIngestor.ingestSvg(is);
            } catch (Exception e) {
                logger.warn("Failed to ingest GADM file '{}': {}", file.getAbsolutePath(), e.getMessage());
            }
        }
        return null;
    }
}
