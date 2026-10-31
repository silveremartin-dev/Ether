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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <h1>KnmiClimateDataIngestor</h1>
 * <p>
 * Robust parser and academic ingestion pipeline for official KNMI Climate Explorer CMIP6
 * multi-model ensemble surface temperature (tas) time-series reconstructions.
 * </p>
 * <p>
 * Ingests multi-model mean near-surface air temperatures (1850-2100) across CMIP6 scenarios:
 * <ul>
 *   <li>{@code ssp126}: SSP1-2.6 (Sustainability / Low forcing)</li>
 *   <li>{@code ssp245}: SSP2-4.5 (Middle of the Road / Intermediate forcing)</li>
 *   <li>{@code ssp370}: SSP3-7.0 (Regional Rivalry / High forcing)</li>
 *   <li>{@code ssp585}: SSP5-8.5 (Fossil-Fueled Development / Very high forcing)</li>
 * </ul>
 * Computes pre-industrial climatological baselines (1850-1900) and exact GMST (Global Mean
 * Surface Temperature) anomalies \(\Delta T\) required for Ether climate-economy coupling.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class KnmiClimateDataIngestor {

    private static final Logger logger = LoggerFactory.getLogger(KnmiClimateDataIngestor.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** Standard IPCC pre-industrial baseline start year (inclusive). */
    public static final int PRE_INDUSTRIAL_START_YEAR = 1850;
    /** Standard IPCC pre-industrial baseline end year (inclusive). */
    public static final int PRE_INDUSTRIAL_END_YEAR = 1900;

    private KnmiClimateDataIngestor() {
        // Utility class
    }

    /**
     * Container for annual temperature data points.
     *
     * @param year calendar year
     * @param monthlyTemperatures 12 monthly temperature values in Celsius
     * @param annualMean annual mean temperature in Celsius
     * @param temperatureAnomaly anomaly relative to 1850-1900 pre-industrial baseline in Kelvin/Celsius
     */
    public record AnnualClimatePoint(
        int year,
        double[] monthlyTemperatures,
        double annualMean,
        double temperatureAnomaly
    ) {}

    /**
     * Container for an ingested CMIP6 scenario time series.
     *
     * @param scenarioId scenario identifier (e.g. "ssp245")
     * @param preIndustrialBaselineMean baseline climatological mean (1850-1900) in Celsius
     * @param annualPoints map of year to {@link AnnualClimatePoint}
     */
    public record ScenarioClimateSeries(
        String scenarioId,
        double preIndustrialBaselineMean,
        Map<Integer, AnnualClimatePoint> annualPoints
    ) {}

    /**
     * Ingests a KNMI .dat / .dat.txt file containing monthly temperature values.
     *
     * @param datFile source KNMI dat file
     * @param scenarioId identifier for the scenario (e.g. "ssp245")
     * @return structured {@link ScenarioClimateSeries}
     * @throws Exception if reading or parsing fails
     */
    public static ScenarioClimateSeries ingestKnmiDatFile(File datFile, String scenarioId) throws Exception {
        if (!datFile.exists() || !datFile.isFile()) {
            throw new IllegalArgumentException("KNMI file does not exist: " + datFile.getAbsolutePath());
        }

        Map<Integer, double[]> rawMonthly = new TreeMap<>();
        List<Double> preIndustrialValues = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 13) {
                    continue;
                }

                try {
                    int year = Integer.parseInt(parts[0]);
                    double[] monthly = new double[12];
                    double sum = 0.0;
                    for (int m = 0; m < 12; m++) {
                        monthly[m] = Double.parseDouble(parts[m + 1]);
                        sum += monthly[m];
                    }

                    rawMonthly.put(year, monthly);
                    if (year >= PRE_INDUSTRIAL_START_YEAR && year <= PRE_INDUSTRIAL_END_YEAR) {
                        for (double v : monthly) {
                            preIndustrialValues.add(v);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // skip header or corrupted line
                }
            }
        }

        double baseline = preIndustrialValues.isEmpty() ? 13.7011 :
            preIndustrialValues.stream().mapToDouble(Double::doubleValue).average().orElse(13.7011);

        Map<Integer, AnnualClimatePoint> points = new TreeMap<>();
        for (Map.Entry<Integer, double[]> entry : rawMonthly.entrySet()) {
            int year = entry.getKey();
            double[] monthly = entry.getValue();
            double annualMean = Arrays.stream(monthly).average().orElse(0.0);
            double anomaly = annualMean - baseline;
            points.put(year, new AnnualClimatePoint(year, monthly, annualMean, anomaly));
        }

        logger.info("Ingested KNMI series '{}' from {}: {} years (1850-1900 baseline = {} C)",
            scenarioId, datFile.getName(), points.size(), String.format(Locale.ROOT, "%.4f", baseline));

        return new ScenarioClimateSeries(scenarioId, baseline, points);
    }

    /**
     * Ingests all KNMI scenario files found in a directory and exports a consolidated 1D JSON.
     *
     * @param directory directory containing KNMI files (e.g. data/sources/knmi_climate/)
     * @param outputFile destination JSON file (e.g. data/variables/projections/cmip6_knmi_temperatures_1d.json)
     * @throws Exception if scanning or serialization fails
     */
    public static void ingestDirectoryAndExportJson(File directory, File outputFile) throws Exception {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + directory.getAbsolutePath());
        }

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "KNMI Climate Explorer / CMIP6 Multi-Model Ensemble Mean");
        metadata.put("variable", "tas (Near-Surface Air Temperature)");
        metadata.put("unit", "Celsius & Anomaly relative to 1850-1900 baseline");
        metadata.put("citation", "KNMI 2023 CMIP6 standard multi-model ensemble (Eyring et al., 2016)");
        root.put("metadata", metadata);

        Map<String, Object> scenariosMap = new LinkedHashMap<>();

        File[] files = directory.listFiles((dir, name) -> name.contains("modmean") && name.endsWith(".txt"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File file : files) {
                String scenarioId = "unknown";
                if (file.getName().contains("ssp126")) scenarioId = "ssp126";
                else if (file.getName().contains("ssp245")) scenarioId = "ssp245";
                else if (file.getName().contains("ssp370")) scenarioId = "ssp370";
                else if (file.getName().contains("ssp585")) scenarioId = "ssp585";

                ScenarioClimateSeries series = ingestKnmiDatFile(file, scenarioId);

                Map<String, Object> scenarioData = new LinkedHashMap<>();
                scenarioData.put("preIndustrialBaselineMean", series.preIndustrialBaselineMean());

                Map<String, Double> annualMeanMap = new LinkedHashMap<>();
                Map<String, Double> anomalyMap = new LinkedHashMap<>();
                for (AnnualClimatePoint p : series.annualPoints().values()) {
                    if (p.year() >= 2020) {
                        annualMeanMap.put(String.valueOf(p.year()), Math.round(p.annualMean() * 100.0) / 100.0);
                        anomalyMap.put(String.valueOf(p.year()), Math.round(p.temperatureAnomaly() * 100.0) / 100.0);
                    }
                }
                scenarioData.put("annualMeanCelsius", annualMeanMap);
                scenarioData.put("temperatureAnomalyCelsius", anomalyMap);

                scenariosMap.put(scenarioId, scenarioData);
            }
        }

        root.put("scenarios", scenariosMap);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        mapper.writeValue(outputFile, root);
        logger.info("Successfully exported consolidated KNMI CMIP6 temperatures to {}", outputFile.getAbsolutePath());
    }
}
