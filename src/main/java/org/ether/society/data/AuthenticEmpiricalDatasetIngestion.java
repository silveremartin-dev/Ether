/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * High-performance empirical spatial ingestion engine for tens of thousands of
 * genuine geological, energy, and aquifer points/polygons from:
 * 1. USGS MRDS (300,000+ occurrences)
 * 2. GEM Global Energy Monitor (Global Coal Mine Tracker, Global Oil & Gas Extraction Tracker, GOGPT, GGIT, GOIT)
 * 3. USGS World Petroleum Assessment Shapefile (au_sumg.shp)
 * 4. UNESCO / BGR WHYMAP Global Groundwater & Aquifers Shapefiles (whymap_GW_aquifers_v1_poly.shp)
 */
public class AuthenticEmpiricalDatasetIngestion {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticEmpiricalDatasetIngestion.class);

    /**
     * Fast binary ESRI shapefile parser returning (lon, lat, weight) coordinate vertices.
     */
    public static List<double[]> readShapefileCoordinates(File shpFile, double defaultWeight) {
        List<double[]> list = new ArrayList<>();
        if (shpFile == null || !shpFile.exists()) {
            return list;
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(shpFile), 65536))) {
            int fileCode = dis.readInt();
            if (fileCode != 9994) {
                logger.warn("Not a valid shapefile: {}", shpFile.getAbsolutePath());
                return list;
            }
            dis.skipBytes(20);
            int fileLengthWords = dis.readInt();
            int version = Integer.reverseBytes(dis.readInt());
            int shapeType = Integer.reverseBytes(dis.readInt());
            dis.skipBytes(64); // Bounding box

            while (dis.available() > 0) {
                try {
                    int recNum = dis.readInt();
                    int contentWords = dis.readInt();
                    int contentBytes = contentWords * 2;
                    if (contentBytes < 4) break;

                    int recShapeType = Integer.reverseBytes(dis.readInt());
                    int remBytes = contentBytes - 4;

                    if (recShapeType == 1 || recShapeType == 11 || recShapeType == 21) { // Point
                        double lon = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        double lat = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                            list.add(new double[]{lon, lat, 8.0, defaultWeight});
                        }
                        if (remBytes > 16) dis.skipBytes(remBytes - 16);
                    } else if (recShapeType == 3 || recShapeType == 5 || recShapeType == 13 || recShapeType == 15) { // PolyLine / Polygon
                        if (remBytes < 40) {
                            dis.skipBytes(remBytes);
                            continue;
                        }
                        double xmin = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        double ymin = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        double xmax = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        double ymax = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                        int numParts = Integer.reverseBytes(dis.readInt());
                        int numPoints = Integer.reverseBytes(dis.readInt());

                        int partsBytes = numParts * 4;
                        if (partsBytes > 0 && partsBytes <= remBytes - 40) {
                            dis.skipBytes(partsBytes);
                        } else {
                            dis.skipBytes(remBytes - 40);
                            continue;
                        }

                        int maxPoints = (remBytes - 40 - partsBytes) / 16;
                        int pointsToRead = Math.min(numPoints, Math.max(0, maxPoints));

                        int step = pointsToRead > 300 ? (pointsToRead / 100) : 1;
                        for (int p = 0; p < pointsToRead; p++) {
                            double px = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                            double py = Double.longBitsToDouble(Long.reverseBytes(dis.readLong()));
                            if (p % step == 0 && px >= -180.0 && px <= 180.0 && py >= -90.0 && py <= 90.0) {
                                list.add(new double[]{px, py, 10.0, defaultWeight});
                            }
                        }

                        int alreadyRead = 40 + partsBytes + pointsToRead * 16;
                        if (remBytes > alreadyRead) {
                            dis.skipBytes(remBytes - alreadyRead);
                        }
                    } else {
                        dis.skipBytes(remBytes);
                    }
                } catch (EOFException eof) {
                    break;
                }
            }
            logger.info("Ingested {} spatial vertices from shapefile '{}'", list.size(), shpFile.getName());
        } catch (Exception e) {
            logger.warn("Error reading shapefile {}: {}", shpFile.getName(), e.getMessage());
        }
        return list;
    }

    /**
     * High-speed streaming parser for XLSX spreadsheets extracting geographic coordinates (lon, lat)
     * and optional filter keywords (e.g. "Oil", "Gas", "Coal").
     */
    public static List<double[]> readXlsxCoordinates(File xlsxFile, String... filterKeywords) {
        List<double[]> list = new ArrayList<>();
        if (xlsxFile == null || !xlsxFile.exists()) {
            return list;
        }

        try (ZipFile zip = new ZipFile(xlsxFile)) {
            // 1. Parse Shared Strings Table
            List<String> sharedStrings = new ArrayList<>();
            ZipEntry sstEntry = zip.getEntry("xl/sharedStrings.xml");
            if (sstEntry != null) {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

                try (InputStream is = zip.getInputStream(sstEntry)) {
                    XMLStreamReader reader = factory.createXMLStreamReader(is);
                    StringBuilder curText = null;
                    while (reader.hasNext()) {
                        int event = reader.next();
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            if ("t".equals(reader.getLocalName())) {
                                curText = new StringBuilder();
                            }
                        } else if (event == XMLStreamConstants.CHARACTERS) {
                            if (curText != null) {
                                curText.append(reader.getText());
                            }
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if ("t".equals(reader.getLocalName())) {
                                sharedStrings.add(curText != null ? curText.toString() : "");
                                curText = null;
                            }
                        }
                    }
                }
            }

            // 2. Parse Worksheets (sheet1.xml, sheet2.xml, etc.)
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    parseWorksheet(zip.getInputStream(entry), sharedStrings, list, filterKeywords);
                }
            }
            logger.info("Ingested {} points from XLSX '{}' with filters {}", list.size(), xlsxFile.getName(), Arrays.toString(filterKeywords));
        } catch (Exception e) {
            logger.warn("Error reading XLSX {}: {}", xlsxFile.getName(), e.getMessage());
        }
        return list;
    }

    private static void parseWorksheet(InputStream is, List<String> sst, List<double[]> out, String... filterKeywords) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = factory.createXMLStreamReader(is);
            int latCol = -1;
            int lonCol = -1;
            int typeCol = -1;

            int curRow = 0;
            int curCol = 0;
            String curType = null;
            StringBuilder cellVal = null;
            Map<Integer, String> rowValues = new HashMap<>();

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elem = reader.getLocalName();
                    if ("row".equals(elem)) {
                        curRow++;
                        rowValues.clear();
                    } else if ("c".equals(elem)) {
                        String r = reader.getAttributeValue(null, "r");
                        curCol = getColumnIndex(r);
                        curType = reader.getAttributeValue(null, "t");
                    } else if ("v".equals(elem)) {
                        cellVal = new StringBuilder();
                    }
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if (cellVal != null) {
                        cellVal.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String elem = reader.getLocalName();
                    if ("v".equals(elem)) {
                        String val = cellVal != null ? cellVal.toString().trim() : "";
                        if ("s".equals(curType)) {
                            try {
                                int idx = Integer.parseInt(val);
                                if (idx >= 0 && idx < sst.size()) {
                                    val = sst.get(idx);
                                }
                            } catch (Exception ignored) {}
                        }
                        rowValues.put(curCol, val);
                        cellVal = null;
                    } else if ("row".equals(elem)) {
                        if (curRow <= 3) { // Header detection
                            for (Map.Entry<Integer, String> e : rowValues.entrySet()) {
                                String h = e.getValue().toLowerCase().replaceAll("[^a-z0-9]", "");
                                if (latCol == -1 && (h.contains("latitude") || h.equals("lat") || h.equals("y"))) {
                                    latCol = e.getKey();
                                }
                                if (lonCol == -1 && (h.contains("longitude") || h.equals("lon") || h.equals("lng") || h.equals("x"))) {
                                    lonCol = e.getKey();
                                }
                                if (typeCol == -1 && (h.contains("fuel") || h.contains("type") || h.contains("subfuel") || h.contains("commodity"))) {
                                    typeCol = e.getKey();
                                }
                            }
                        } else if (latCol != -1 && lonCol != -1) {
                            String latStr = rowValues.get(latCol);
                            String lonStr = rowValues.get(lonCol);
                            if (latStr != null && lonStr != null) {
                                try {
                                    double lat = Double.parseDouble(latStr.trim());
                                    double lon = Double.parseDouble(lonStr.trim());
                                    if (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0) {
                                        boolean match = true;
                                        if (filterKeywords != null && filterKeywords.length > 0) {
                                            match = false;
                                            String rowFull = rowValues.values().toString().toLowerCase();
                                            for (String kw : filterKeywords) {
                                                if (rowFull.contains(kw.toLowerCase())) {
                                                    match = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (match) {
                                            out.add(new double[]{lon, lat, 7.0, 1.5});
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Worksheet parse notice: {}", e.getMessage());
        }
    }

    private static int getColumnIndex(String cellRef) {
        if (cellRef == null || cellRef.isEmpty()) return 0;
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char c = cellRef.charAt(i);
            if (Character.isLetter(c)) {
                col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
            } else {
                break;
            }
        }
        return col;
    }

    /**
     * Extracts tens of thousands of empirical Coal deposit and mine points.
     */
    public static List<double[]> getEmpiricalCoalOccurrences() {
        List<double[]> list = new ArrayList<>();
        // 1. USGS MRDS Coal occurrences
        list.addAll(extractMrdsDeposits("coal", "lignite", "anthracite", "bituminous"));

        // 2. GEM Global Coal Mine Tracker
        File coalMines = new File("data/maps/wep_world_energy/Global Coal Mine Tracker, August 2026.xlsx");
        list.addAll(readXlsxCoordinates(coalMines));

        // 3. GEM Global Coal Plant Tracker
        File coalPlants = new File("data/maps/wep_world_energy/Global-Coal-Plant-Tracker-July-2026.xlsx");
        list.addAll(readXlsxCoordinates(coalPlants, "coal", "anthracite", "lignite", "bituminous"));

        logger.info("Total empirical Coal occurrences gathered: {}", list.size());
        return list;
    }

    /**
     * Extracts tens of thousands of empirical Crude Oil extraction, basin, and pipeline points.
     */
    public static List<double[]> getEmpiricalOilOccurrences() {
        List<double[]> list = new ArrayList<>();
        // 1. GEM Global Oil and Gas Extraction Tracker (Oil)
        File oilGasExtraction = new File("data/maps/wep_world_energy/Global-Oil-and-Gas-Extraction-Tracker-March-2026.xlsx");
        list.addAll(readXlsxCoordinates(oilGasExtraction, "oil", "petroleum", "condensate", "crude", "bitumen"));

        // 2. GEM Oil Pipelines & Terminals
        File oilPipelines = new File("data/maps/wep_world_energy/GEM-GOIT-Oil-NGL-Pipelines-2026-06.xlsx");
        list.addAll(readXlsxCoordinates(oilPipelines));

        // 3. USGS World Petroleum Assessment Units
        File usgsPetroleumShp = new File("data/maps/wep_world_energy/usgs_world_petroleum/au_sumg.shp");
        list.addAll(readShapefileCoordinates(usgsPetroleumShp, 2.0));

        // 4. MRDS Oil Shale & Petroleum
        list.addAll(extractMrdsDeposits("oil shale", "bitumen", "petroleum", "asphalt"));

        logger.info("Total empirical Crude Oil occurrences gathered: {}", list.size());
        return list;
    }

    /**
     * Extracts tens of thousands of empirical Natural Gas extraction, field, pipeline, and LNG points.
     */
    public static List<double[]> getEmpiricalGasOccurrences() {
        List<double[]> list = new ArrayList<>();
        // 1. GEM Global Oil and Gas Extraction Tracker (Gas)
        File oilGasExtraction = new File("data/maps/wep_world_energy/Global-Oil-and-Gas-Extraction-Tracker-March-2026.xlsx");
        list.addAll(readXlsxCoordinates(oilGasExtraction, "gas", "methane", "lng", "cbm", "shale gas"));

        // 2. GEM Gas Pipelines
        File gasPipelines = new File("data/maps/wep_world_energy/GEM-GGIT-Gas-Pipelines-2025-11.xlsx");
        list.addAll(readXlsxCoordinates(gasPipelines));

        // 3. GEM LNG Terminals
        File lngTerminals = new File("data/maps/wep_world_energy/GEM-GGIT-LNG-Teminals-2025-09.xlsx");
        list.addAll(readXlsxCoordinates(lngTerminals));

        // 4. GOGPT Gas Plants
        File gogpt = new File("data/maps/wep_world_energy/Global Oil and Gas Plant Tracker (GOGPT) - August 2026.xlsx");
        list.addAll(readXlsxCoordinates(gogpt, "gas", "methane", "lng"));

        // 5. USGS World Petroleum Gas Assessment Units
        File usgsPetroleumShp = new File("data/maps/wep_world_energy/usgs_world_petroleum/au_sumg.shp");
        list.addAll(readShapefileCoordinates(usgsPetroleumShp, 1.8));

        logger.info("Total empirical Natural Gas occurrences gathered: {}", list.size());
        return list;
    }

    /**
     * Extracts tens of thousands of empirical WHYMAP Groundwater Aquifers & Wetlands polygons/points.
     */
    public static List<double[]> getEmpiricalAquiferOccurrences() {
        List<double[]> list = new ArrayList<>();
        // 1. WHYMAP Global Groundwater Aquifers Polygons
        File aquifersShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_GW_aquifers_v1_poly.shp");
        if (!aquifersShp.exists()) {
            aquifersShp = new File("data/maps/whymap_groundwater/whymap_GW_aquifers_v1_poly.shp");
        }
        list.addAll(readShapefileCoordinates(aquifersShp, 2.5));

        // 2. WHYMAP Groundwater-dependent Cities
        File citiesShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_cities_dependGW_v1_point.shp");
        list.addAll(readShapefileCoordinates(citiesShp, 2.0));

        // 3. WHYMAP Wetlands
        File wetlandsShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_wetlands__v1_point.shp");
        list.addAll(readShapefileCoordinates(wetlandsShp, 1.5));

        // 4. WHYMAP Saline Groundwater
        File salineShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_salineGW__v1_poly.shp");
        list.addAll(readShapefileCoordinates(salineShp, 1.0));

        logger.info("Total empirical Aquifers occurrences gathered: {}", list.size());
        return list;
    }

    /**
     * Fast parsing of MRDS CSV archive for mineral/fuel keywords.
     */
    public static List<double[]> extractMrdsDeposits(String... keywords) {
        List<double[]> list = new ArrayList<>();
        Path zipPath = Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!Files.exists(zipPath)) return list;

        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry("mrds.csv");
            if (entry == null) return list;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8), 65536)) {
                String line = br.readLine(); // Header
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    List<String> parts = fastParseCsv(line);
                    if (parts.size() > 17) {
                        try {
                            double lat = Double.parseDouble(parts.get(5).replace("\"", "").trim());
                            double lon = Double.parseDouble(parts.get(6).replace("\"", "").trim());
                            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue;

                            String comms = (parts.get(11) + " " + parts.get(12) + " " + parts.get(13) + " " + parts.get(14)).toLowerCase();
                            boolean match = false;
                            for (String kw : keywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }

                            if (match) {
                                String prodSize = parts.get(17).toUpperCase();
                                double weight = 1.0;
                                double radius = 5.0;
                                if (prodSize.contains("L") || prodSize.contains("Y")) {
                                    weight = 2.5;
                                    radius = 12.0;
                                } else if (prodSize.contains("M")) {
                                    weight = 1.8;
                                    radius = 8.0;
                                } else if (prodSize.contains("S")) {
                                    weight = 1.2;
                                    radius = 6.0;
                                }
                                list.add(new double[]{lon, lat, radius, weight});
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading MRDS CSV: {}", e.getMessage());
        }
        return list;
    }

    public static List<String> fastParseCsv(String line) {
        List<String> list = new ArrayList<>(30);
        StringBuilder sb = new StringBuilder(64);
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString().trim());
        return list;
    }
}
