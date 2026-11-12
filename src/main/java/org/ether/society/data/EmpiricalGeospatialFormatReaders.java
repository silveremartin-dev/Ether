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
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Low-level binary and XML parsers for shapefiles, spreadsheets, and CSV tables
 * in empirical geospatial datasets.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class EmpiricalGeospatialFormatReaders {

    private static final Logger logger = LoggerFactory.getLogger(EmpiricalGeospatialFormatReaders.class);

    private EmpiricalGeospatialFormatReaders() {}

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
            if (fileCode != 0x0000270a) {
                return list;
            }
            dis.skipBytes(20);
            int fileLengthWords = dis.readInt();
            int version = Integer.reverseBytes(dis.readInt());
            int shapeType = Integer.reverseBytes(dis.readInt());
            dis.skipBytes(64);

            while (true) {
                if (list.size() >= 5000) break;
                try {
                    int recNum = dis.readInt();
                    int contentLenWords = dis.readInt();
                    int contentLenBytes = contentLenWords * 2;
                    if (contentLenBytes <= 0) continue;

                    byte[] recBytes = new byte[contentLenBytes];
                    dis.readFully(recBytes);
                    ByteBuffer bb = ByteBuffer.wrap(recBytes).order(ByteOrder.LITTLE_ENDIAN);
                    int recShapeType = bb.getInt();

                    if (recShapeType == 1) { // Point
                        double x = bb.getDouble();
                        double y = bb.getDouble();
                        if (x >= -180 && x <= 180 && y >= -90 && y <= 90) {
                            list.add(new double[]{x, y, defaultWeight});
                        }
                    } else if (recShapeType == 3 || recShapeType == 5) { // PolyLine or Polygon
                        bb.position(bb.position() + 32);
                        int numParts = bb.getInt();
                        int numPoints = bb.getInt();
                        bb.position(bb.position() + numParts * 4);

                        int step = Math.max(1, numPoints / 10);
                        for (int p = 0; p < numPoints; p++) {
                            double x = bb.getDouble();
                            double y = bb.getDouble();
                            if (p % step == 0 && x >= -180 && x <= 180 && y >= -90 && y <= 90) {
                                list.add(new double[]{x, y, defaultWeight});
                            }
                        }
                    }
                } catch (EOFException eof) {
                    break;
                }
            }
            logger.info("Ingested {} points from Shapefile '{}'", list.size(), shpFile.getName());
        } catch (Exception e) {
            logger.warn("Error reading shapefile {}: {}", shpFile.getName(), e.getMessage());
        }
        return list;
    }

    /**
     * Fast XML streaming parser for OpenXML (.xlsx) files extracting (lon, lat) coordinates.
     */
    public static List<double[]> readXlsxCoordinates(File xlsxFile, String... filterKeywords) {
        List<double[]> list = new ArrayList<>();
        if (xlsxFile == null || !xlsxFile.exists()) return list;

        try (ZipFile zip = new ZipFile(xlsxFile)) {
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
                            if ("t".equals(reader.getLocalName())) curText = new StringBuilder();
                        } else if (event == XMLStreamConstants.CHARACTERS) {
                            if (curText != null) curText.append(reader.getText());
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if ("t".equals(reader.getLocalName())) {
                                sharedStrings.add(curText != null ? curText.toString() : "");
                                curText = null;
                            }
                        }
                    }
                }
            }

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
                    if (cellVal != null) cellVal.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String elem = reader.getLocalName();
                    if ("v".equals(elem)) {
                        String val = cellVal != null ? cellVal.toString().trim() : "";
                        if ("s".equals(curType)) {
                            try {
                                int idx = Integer.parseInt(val);
                                if (idx >= 0 && idx < sst.size()) val = sst.get(idx);
                            } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(EmpiricalGeospatialFormatReaders.class).debug("Non-fatal condition in EmpiricalGeospatialFormatReaders: {}", ignored.getMessage()); }
                        }
                        rowValues.put(curCol, val);
                        cellVal = null;
                    } else if ("row".equals(elem)) {
                        if (curRow <= 3) {
                            for (Map.Entry<Integer, String> e : rowValues.entrySet()) {
                                String h = e.getValue().toLowerCase().replaceAll("[^a-z0-9]", "");
                                if (latCol == -1 && (h.contains("latitude") || h.equals("lat") || h.equals("y"))) latCol = e.getKey();
                                if (lonCol == -1 && (h.contains("longitude") || h.equals("lon") || h.equals("lng") || h.equals("x"))) lonCol = e.getKey();
                                if (typeCol == -1 && (h.contains("fuel") || h.contains("type") || h.contains("subfuel") || h.contains("commodity"))) typeCol = e.getKey();
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
                                            String rowType = typeCol != -1 ? rowValues.get(typeCol) : "";
                                            String fullRow = rowType + " " + rowValues.values();
                                            for (String kw : filterKeywords) {
                                                if (fullRow.toLowerCase().contains(kw.toLowerCase())) {
                                                    match = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (match) out.add(new double[]{lon, lat, 1.0});
                                    }
                                } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(EmpiricalGeospatialFormatReaders.class).debug("Non-fatal condition in EmpiricalGeospatialFormatReaders: {}", ignored.getMessage()); }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error streaming worksheet XML: {}", e.getMessage());
        }
    }

    private static int getColumnIndex(String cellRef) {
        if (cellRef == null) return 0;
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char c = cellRef.charAt(i);
            if (Character.isLetter(c)) {
                col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
            } else {
                break;
            }
        }
        return col - 1;
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

