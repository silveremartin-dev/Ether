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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <h1>UsgsMineralsSummaryIngestor</h1>
 * <p>
 * Ingests official USGS Mineral Commodity Summaries (MCS) dataset
 * ({@code data/sources/usgs_minerals/MCS2026_Commodities_Data.csv}).
 * </p>
 * <p>
 * Extracts global mine production and proved crustal reserves for biophysically critical commodities:
 * <ul>
 *   <li><b>Phosphate Rock</b> (Agricultural Liebig / NPK cycle &amp; peak phosphorus)</li>
 *   <li><b>Copper, Lithium, Cobalt, Nickel</b> (Electrification &amp; energy transition)</li>
 *   <li><b>Rare Earths</b> (Advanced metallurgy &amp; magnetics)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class UsgsMineralsSummaryIngestor {

    private static final Logger logger = LoggerFactory.getLogger(UsgsMineralsSummaryIngestor.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public record CommodityStats(
        String commodity,
        int year,
        double worldProductionMetricTons,
        double worldReservesMetricTons,
        Map<String, Double> productionByCountry,
        Map<String, Double> reservesByCountry
    ) {}

    private UsgsMineralsSummaryIngestor() {
        // Utility ingestor
    }

    /**
     * Parses the USGS MCS CSV dataset and returns a map of Commodity Name -&gt; CommodityStats.
     *
     * @param csvFile input CSV file
     * @return map of commodity statistics
     * @throws IOException on I/O error
     */
    public static Map<String, CommodityStats> parseUsgsMinerals(File csvFile) throws IOException {
        Map<String, Map<String, Double>> prodByComm = new LinkedHashMap<>();
        Map<String, Map<String, Double>> resByComm = new LinkedHashMap<>();

        if (!csvFile.exists()) {
            logger.warn("USGS Minerals CSV not found at: {}", csvFile.getAbsolutePath());
            return Collections.emptyMap();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return Collections.emptyMap();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 9) continue;

                String section = tokens.get(1).trim();
                if (!section.equalsIgnoreCase("World Mine Production and Reserves")) {
                    continue;
                }

                String commodity = tokens.get(2).trim();
                String country = tokens.get(3).trim();
                String stats = tokens.get(4).trim();
                String unit = tokens.get(6).trim();
                String yearStr = tokens.get(7).trim();
                String valStr = tokens.get(8).trim().replace(",", "");

                if (valStr.isBlank() || valStr.equalsIgnoreCase("N/A") || valStr.equalsIgnoreCase("W")) {
                    continue;
                }

                double val;
                try {
                    val = Double.parseDouble(valStr);
                } catch (NumberFormatException ignored) {
                    continue;
                }

                // Normalization to metric tons
                double multiplier = 1.0;
                if (unit.toLowerCase(Locale.ROOT).contains("thousand metric ton") || unit.toLowerCase(Locale.ROOT).contains("thousand tons")) {
                    multiplier = 1000.0;
                } else if (unit.toLowerCase(Locale.ROOT).contains("million metric ton")) {
                    multiplier = 1_000_000.0;
                }

                val *= multiplier;

                if (stats.equalsIgnoreCase("Production") || stats.toLowerCase(Locale.ROOT).contains("production")) {
                    prodByComm.computeIfAbsent(commodity, k -> new LinkedHashMap<>()).put(country, val);
                } else if (stats.equalsIgnoreCase("Reserves") || stats.toLowerCase(Locale.ROOT).contains("reserve")) {
                    resByComm.computeIfAbsent(commodity, k -> new LinkedHashMap<>()).put(country, val);
                }
            }
        }

        Map<String, CommodityStats> result = new LinkedHashMap<>();
        Set<String> allCommodities = new LinkedHashSet<>();
        allCommodities.addAll(prodByComm.keySet());
        allCommodities.addAll(resByComm.keySet());

        for (String comm : allCommodities) {
            Map<String, Double> prod = prodByComm.getOrDefault(comm, Collections.emptyMap());
            Map<String, Double> res = resByComm.getOrDefault(comm, Collections.emptyMap());

            double totalProd = prod.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalRes = res.values().stream().mapToDouble(Double::doubleValue).sum();

            result.put(comm, new CommodityStats(comm, 2024, totalProd, totalRes, prod, res));
        }

        logger.info("Successfully ingested {} mineral commodities from {}", result.size(), csvFile.getName());
        return result;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list;
    }
}
