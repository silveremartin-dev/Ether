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

/**
 * <h1>IamcSspCsvDataIngestor</h1>
 * <p>
 * High-performance, robust parser and offline ingestion pipeline for official IIASA / IPCC
 * Integrated Assessment Modeling Consortium (IAMC) tabular CSV scenario databases.
 * </p>
 * <p>
 * Parses raw downloaded IIASA SSP snapshot CSV files and converts them into standardized
 * continuous 1D benchmark projection JSON datasets in {@code data/variables/projections/}:
 * <ul>
 *   <li>{@code ssp1_sustainability_1d.json} (SSP1-1.9 / SSP1-2.6)</li>
 *   <li>{@code ssp2_middle_of_road_1d.json} (SSP2-4.5)</li>
 *   <li>{@code ssp3_regional_rivalry_1d.json} (SSP3-7.0)</li>
 *   <li>{@code ssp4_inequality_1d.json} (SSP4-3.4 / SSP4-6.0)</li>
 *   <li>{@code ssp5_fossil_development_1d.json} (SSP5-8.5)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class IamcSspCsvDataIngestor {

    private static final Logger logger = LoggerFactory.getLogger(IamcSspCsvDataIngestor.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private IamcSspCsvDataIngestor() {
        // Ingestion pipeline utility
    }

    /**
     * Ingestion result container.
     */
    public record IngestedSeries(
        String model,
        String scenario,
        String region,
        String variable,
        String unit,
        Map<Integer, Double> dataPoints
    ) {}

    /**
     * Parses an IAMC-formatted CSV file from the IIASA SSP database.
     *
     * @param csvFile input CSV file
     * @param targetRegion region filter (e.g. "World")
     * @return list of parsed time series matching target region
     * @throws IOException on reading error
     */
    public static List<IngestedSeries> parseIamcCsv(File csvFile, String targetRegion) throws IOException {
        List<IngestedSeries> results = new ArrayList<>();
        if (!csvFile.exists()) {
            logger.warn("IAMC CSV file not found at: {}", csvFile.getAbsolutePath());
            return results;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return results;

            // Normalize delimiters (comma or semicolon)
            String delimiter = headerLine.contains(";") ? ";" : ",";
            String[] headers = headerLine.split(delimiter);

            // Locate standard IAMC column indices
            int modelIdx = -1, scIdx = -1, regIdx = -1, varIdx = -1, unitIdx = -1;
            List<Integer> yearIndices = new ArrayList<>();
            List<Integer> years = new ArrayList<>();

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].replace("\"", "").trim().toLowerCase(Locale.ROOT);
                switch (h) {
                    case "model" -> modelIdx = i;
                    case "scenario" -> scIdx = i;
                    case "region" -> regIdx = i;
                    case "variable" -> varIdx = i;
                    case "unit" -> unitIdx = i;
                    default -> {
                        try {
                            int yr = Integer.parseInt(h);
                            if (yr >= 1950 && yr <= 2200) {
                                yearIndices.add(i);
                                years.add(yr);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (regIdx == -1 || varIdx == -1) {
                logger.error("Invalid IAMC CSV format: missing Region or Variable header.");
                return results;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] tokens = line.split(delimiter, -1);
                if (tokens.length <= regIdx) continue;

                String region = tokens[regIdx].replace("\"", "").trim();
                if (targetRegion != null && !targetRegion.equalsIgnoreCase(region)) {
                    continue; // Skip non-matching regions
                }

                String model = modelIdx >= 0 && tokens.length > modelIdx ? tokens[modelIdx].replace("\"", "").trim() : "";
                String sc = scIdx >= 0 && tokens.length > scIdx ? tokens[scIdx].replace("\"", "").trim() : "";
                String variable = tokens[varIdx].replace("\"", "").trim();
                String unit = unitIdx >= 0 && tokens.length > unitIdx ? tokens[unitIdx].replace("\"", "").trim() : "";

                Map<Integer, Double> pts = new TreeMap<>();
                for (int k = 0; k < yearIndices.size(); k++) {
                    int col = yearIndices.get(k);
                    if (tokens.length > col) {
                        String rawVal = tokens[col].replace("\"", "").trim();
                        if (!rawVal.isBlank() && !rawVal.equalsIgnoreCase("N/A")) {
                            try {
                                double val = Double.parseDouble(rawVal);
                                pts.put(years.get(k), val);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                if (!pts.isEmpty()) {
                    results.add(new IngestedSeries(model, sc, region, variable, unit, pts));
                }
            }
        }

        logger.info("Parsed {} series for region '{}' from {}", results.size(), targetRegion, csvFile.getName());
        return results;
    }

    /**
     * Converts raw parsed IAMC series into standardized Ether 1D projection files.
     *
     * @param seriesList parsed series from IIASA
     * @param outputDir directory to save the JSON files
     * @throws IOException on serialization error
     */
    public static void exportStandardizedProjectionFiles(List<IngestedSeries> seriesList, File outputDir) throws IOException {
        if (!outputDir.exists()) outputDir.mkdirs();

        Map<String, Map<String, Map<String, Object>>> sspDataMap = new LinkedHashMap<>();
        sspDataMap.put("SSP1", new LinkedHashMap<>());
        sspDataMap.put("SSP2", new LinkedHashMap<>());
        sspDataMap.put("SSP3", new LinkedHashMap<>());
        sspDataMap.put("SSP4", new LinkedHashMap<>());
        sspDataMap.put("SSP5", new LinkedHashMap<>());

        for (IngestedSeries s : seriesList) {
            String scLower = s.scenario().toLowerCase(Locale.ROOT);
            String targetSsp = null;
            if (scLower.contains("ssp1")) targetSsp = "SSP1";
            else if (scLower.contains("ssp2")) targetSsp = "SSP2";
            else if (scLower.contains("ssp3")) targetSsp = "SSP3";
            else if (scLower.contains("ssp4")) targetSsp = "SSP4";
            else if (scLower.contains("ssp5")) targetSsp = "SSP5";

            if (targetSsp == null) continue;

            String etherVarKey = mapIamcVariableToEtherKey(s.variable());
            if (etherVarKey != null) {
                Map<String, Object> varObj = new LinkedHashMap<>();
                varObj.put("name", etherVarKey);
                varObj.put("unit", s.unit());
                varObj.put("sourceModel", s.model());
                varObj.put("data", s.dataPoints());
                sspDataMap.get(targetSsp).put(etherVarKey, varObj);
            }
        }

        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : sspDataMap.entrySet()) {
            String sspKey = entry.getKey();
            if (!entry.getValue().isEmpty()) {
                File target = new File(outputDir, sspKey.toLowerCase(Locale.ROOT) + "_iamc_ingested_1d.json");
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("model", sspKey);
                root.put("provenance", "Official IIASA IAMC Scenario Database");
                root.put("variables", entry.getValue());
                mapper.writeValue(target, root);
                logger.info("Generated: {}", target.getAbsolutePath());
            }
        }
    }

    private static String mapIamcVariableToEtherKey(String iamcVar) {
        if (iamcVar == null) return null;
        String v = iamcVar.toLowerCase(Locale.ROOT);
        if (v.startsWith("population")) return "worldPopulation";
        if (v.startsWith("gdp|ppp")) return "gdpGrossPPP";
        if (v.startsWith("primary energy")) return "primaryEnergy";
        if (v.startsWith("temperature|global mean")) return "temperatureAnomaly";
        if (v.startsWith("atmospheric concentration|co2") || v.startsWith("concentration|co2")) return "co2Concentration";
        if (v.startsWith("land cover|cropland")) return "arableLandMha";
        if (v.startsWith("land cover|forest")) return "forestCoverMha";
        return null;
    }
}
