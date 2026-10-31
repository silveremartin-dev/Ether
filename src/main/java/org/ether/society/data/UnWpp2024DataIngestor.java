/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * <h1>UnWpp2024DataIngestor</h1>
 * <p>
 * Ingestion engine for the United Nations World Population Prospects (UN WPP 2024 Revision).
 * Reads the official compressed demographic projection dataset ({@code WPP2024_TotalPopulationBySex.csv.gz})
 * and produces calibrated 1D world demographic series (Medium, Low, High fertility variants).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class UnWpp2024DataIngestor {

    private static final Logger logger = LoggerFactory.getLogger(UnWpp2024DataIngestor.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private UnWpp2024DataIngestor() {
        // Utility class
    }

    /**
     * Container for demographic trajectory points.
     *
     * @param year calendar year
     * @param populationInMillions population in millions of inhabitants
     * @param populationInPersons total population count in persons
     */
    public record DemographicPoint(
        int year,
        double populationInMillions,
        long populationInPersons
    ) {}

    /**
     * Ingests the world series from a UN WPP compressed CSV file.
     *
     * @param gzFile path to the .csv.gz file
     * @return map of variant name ("Medium", "High", "Low") to map of year -&gt; {@link DemographicPoint}
     * @throws Exception if reading or parsing fails
     */
    public static Map<String, Map<Integer, DemographicPoint>> ingestWorldProjections(File gzFile) throws Exception {
        if (!gzFile.exists()) {
            throw new FileNotFoundException("WPP file does not exist: " + gzFile.getAbsolutePath());
        }

        Map<String, Map<Integer, DemographicPoint>> variants = new LinkedHashMap<>();
        variants.put("Medium", new TreeMap<>());
        variants.put("High", new TreeMap<>());
        variants.put("Low", new TreeMap<>());

        try (InputStream fileStream = new FileInputStream(gzFile);
             InputStream gzipStream = new GZIPInputStream(fileStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Empty WPP file");
            }

            // Strip UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = headerLine.split(",");
            int locIndex = -1;
            int variantIndex = -1;
            int timeIndex = -1;
            int popTotalIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\"", "");
                if ("Location".equalsIgnoreCase(h)) locIndex = i;
                else if ("Variant".equalsIgnoreCase(h)) variantIndex = i;
                else if ("Time".equalsIgnoreCase(h)) timeIndex = i;
                else if ("PopTotal".equalsIgnoreCase(h)) popTotalIndex = i;
            }

            if (locIndex < 0 || variantIndex < 0 || timeIndex < 0 || popTotalIndex < 0) {
                throw new IllegalStateException("Missing required columns in WPP header: " + headerLine);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length <= popTotalIndex) continue;

                String loc = parts[locIndex].trim().replace("\"", "");
                if (!"World".equalsIgnoreCase(loc)) continue;

                String variant = parts[variantIndex].trim().replace("\"", "");
                if (!variants.containsKey(variant)) continue;

                try {
                    int year = Integer.parseInt(parts[timeIndex].trim().replace("\"", ""));
                    double popThousands = Double.parseDouble(parts[popTotalIndex].trim().replace("\"", ""));
                    double popMillions = popThousands / 1000.0;
                    long popPersons = Math.round(popThousands * 1000.0);

                    variants.get(variant).put(year, new DemographicPoint(year, popMillions, popPersons));
                } catch (NumberFormatException ignored) {
                    // Skip invalid data line
                }
            }
        }

        logger.info("Successfully ingested UN WPP 2024 World demographic projections: {} variants parsed.", variants.size());
        return variants;
    }

    /**
     * Ingests UN WPP gzip file and exports continuous JSON to destination.
     *
     * @param gzFile source compressed file
     * @param outputFile destination JSON file
     * @throws Exception if processing fails
     */
    public static void ingestAndExportJson(File gzFile, File outputFile) throws Exception {
        Map<String, Map<Integer, DemographicPoint>> variants = ingestWorldProjections(gzFile);

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "United Nations Department of Economic and Social Affairs (DESA) Population Division");
        metadata.put("dataset", "World Population Prospects 2024 (WPP2024)");
        metadata.put("citation", "United Nations, DESA, Population Division (2024). World Population Prospects 2024.");
        metadata.put("unit", "Millions of inhabitants (worldPopulation) and total persons");
        root.put("metadata", metadata);

        Map<String, Object> variantsMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Integer, DemographicPoint>> entry : variants.entrySet()) {
            Map<String, Double> yearMap = new LinkedHashMap<>();
            for (Map.Entry<Integer, DemographicPoint> yEntry : entry.getValue().entrySet()) {
                if (yEntry.getKey() >= 2020) {
                    yearMap.put(String.valueOf(yEntry.getKey()), Math.round(yEntry.getValue().populationInMillions() * 10.0) / 10.0);
                }
            }
            variantsMap.put(entry.getKey(), yearMap);
        }
        root.put("projections", variantsMap);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        mapper.writeValue(outputFile, root);
        logger.info("Exported UN WPP 2024 world projections to {}", outputFile.getAbsolutePath());
    }
}
