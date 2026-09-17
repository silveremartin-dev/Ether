/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Historical Geographic Information System (HGIS) Atlas Ingestor.
 * Parses historical political boundaries from EurAtlas, Centennia, and Harvard/Fudan CHGIS (China Historical GIS).
 * Supports dynamic political boundaries for ancient empires, Holy Roman Empire duchies, Ottoman vilayets, and Chinese dynasties.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class HgisAtlasIngestor {
    private static final Logger logger = LoggerFactory.getLogger(HgisAtlasIngestor.class);

    /**
     * Loads historical political borders for a specified year and region from HGIS repository.
     */
    public static SvgMapIngestor.SvgIngestionResult loadHistoricalPolityMap(long year, String region) {
        String regionCode = region != null ? region.toLowerCase() : "global";
        String fileName = "hgis_" + regionCode + "_" + (year < 0 ? Math.abs(year) + "bc" : year + "ad") + ".svg";

        File[] searchPaths = new File[]{
            new File("data/maps/hgis/" + fileName),
            new File("user_data/maps/hgis/" + fileName),
            new File("data/maps/svg/" + fileName)
        };

        for (File f : searchPaths) {
            if (f.exists() && f.isFile()) {
                try (InputStream is = new FileInputStream(f)) {
                    logger.info("Ingesting HGIS Historical Atlas cartography for '{}' in year {}: {}", region, year, f.getAbsolutePath());
                    return SvgMapIngestor.ingestSvg(is);
                } catch (Exception e) {
                    logger.warn("Failed to read HGIS file '{}': {}", f.getAbsolutePath(), e.getMessage());
                }
            }
        }
        return null;
    }
}

