/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance streaming GeoJSON parser for Seshat / ClioPatria historical vector datasets.
 *
 * <p>Extracted from {@link CliopatriaPolityVectorReader} to satisfy the Single Responsibility Principle (SRP).
 * Streams large GeoJSON files (e.g. 158 MB) using Jackson token streaming without loading the entire JSON tree
 * into memory, converting Polygon and MultiPolygon coordinates directly into equirectangular cartographic {@link Path2D} geometries.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliopatriaGeoJsonStreamingParser {

    private static final Logger logger = LoggerFactory.getLogger(CliopatriaGeoJsonStreamingParser.class);

    private CliopatriaGeoJsonStreamingParser() {
        // Utility class
    }

    /**
     * Fast streaming parser for Seshat GeoJSON files using Jackson Streaming API.
     *
     * @param geoJsonFile the source GeoJSON file
     * @param targetYear target historical simulation epoch year
     * @param imgW raster target width in pixels
     * @param imgH raster target height in pixels
     * @return list of historical polity features matching the target year
     */
    public static List<HistoricalPolityFeature> loadPolitiesForYear(File geoJsonFile, int targetYear, int imgW, int imgH) {
        List<HistoricalPolityFeature> matched = new ArrayList<>();
        JsonFactory factory = new JsonFactory();

        try (InputStream is = new FileInputStream(geoJsonFile);
             JsonParser parser = factory.createParser(is)) {

            while (parser.nextToken() != null) {
                if ("features".equals(parser.currentName()) && parser.currentToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            parseFeatureIfMatches(parser, targetYear, imgW, imgH, matched);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error streaming Seshat GeoJSON: {}", e.getMessage(), e);
        }

        return matched;
    }

    private static void parseFeatureIfMatches(JsonParser parser, int targetYear, int imgW, int imgH, List<HistoricalPolityFeature> matched) throws Exception {
        String name = null;
        int fromYear = -99999;
        int toYear = 99999;
        String seshatId = null;
        String wiki = null;
        List<Path2D> paths = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            if ("properties".equals(field)) {
                parser.nextToken(); // START_OBJECT
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String prop = parser.currentName();
                    parser.nextToken();
                    if ("Name".equalsIgnoreCase(prop)) name = parser.getText();
                    else if ("FromYear".equalsIgnoreCase(prop)) fromYear = parser.getIntValue();
                    else if ("ToYear".equalsIgnoreCase(prop)) toYear = parser.getIntValue();
                    else if ("SeshatID".equalsIgnoreCase(prop)) seshatId = parser.getText();
                    else if ("Wikipedia".equalsIgnoreCase(prop)) wiki = parser.getText();
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

        // Check if polity was active at targetYear
        if (targetYear >= fromYear && targetYear <= toYear && !paths.isEmpty() && name != null) {
            HistoricalPolityFeature f = new HistoricalPolityFeature();
            f.name = name;
            f.fromYear = fromYear;
            f.toYear = toYear;
            f.seshatId = seshatId;
            f.wikipedia = wiki;
            f.paths = paths;
            f.color = CliopatriaCulturalTaxonomyCatalog.getPolityColor(name, seshatId);
            matched.add(f);
        }
    }

    private static void parseCoordinatesToPaths(JsonParser parser, String geomType, int imgW, int imgH, List<Path2D> paths) throws Exception {
        if ("Polygon".equalsIgnoreCase(geomType)) {
            // Array of linear rings: [ [ [x,y], [x,y]... ], [hole...] ]
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                Path2D path = parseLinearRing(parser, imgW, imgH);
                if (path != null) paths.add(path);
            }
        } else if ("MultiPolygon".equalsIgnoreCase(geomType)) {
            // Array of polygons: [ [ [ [x,y]... ] ] ]
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

                // Equirectangular projection mapping
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
}
