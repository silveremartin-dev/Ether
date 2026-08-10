/*
 * Ether - Human Society Simulation
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * MIT License
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Structured Data Exporter for Ether Simulation Analytics.
 * Exports reconstructed time series and spatial stats in JSON, NDJSON (JSON-Lines), TSV, and CSV formats
 * for direct interoperability with Python (Pandas/NumPy), R, Julia, and Java data science frameworks.
 *
 * @author Silvere Martin-Michiellot
 */
public class StructuredDataExporter {
    private static final Logger logger = LoggerFactory.getLogger(StructuredDataExporter.class);

    public enum ExportFormat {
        CSV, TSV, JSON, NDJSON_LINES
    }

    /**
     * Exporter for time series to a structured file.
     */
    public static void exportSeriesToFile(
            StatisticalReplayEngine.ReconstitutedSeries series,
            File file,
            ExportFormat format) throws IOException {

        if (series == null || file == null) return;

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            switch (format) {
                case JSON -> exportAsJson(series, writer);
                case NDJSON_LINES -> exportAsNdJson(series, writer);
                case TSV -> exportAsDelimiter(series, writer, "\t");
                case CSV -> exportAsDelimiter(series, writer, ",");
            }
        }
        logger.info("Exported series '{}' to {} in format {}", series.getFormulaName(), file.getAbsolutePath(), format);
    }

    private static void exportAsJson(StatisticalReplayEngine.ReconstitutedSeries series, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(escapeJson(series.getFormulaId())).append("\",\n");
        sb.append("  \"name\": \"").append(escapeJson(series.getFormulaName())).append("\",\n");
        sb.append("  \"expression\": \"").append(escapeJson(series.getExpression())).append("\",\n");
        sb.append("  \"unit\": \"").append(escapeJson(series.getUnit())).append("\",\n");
        sb.append("  \"summary\": {\n");
        sb.append(String.format(Locale.US, "    \"min\": %.6f,\n", series.getMin()));
        sb.append(String.format(Locale.US, "    \"max\": %.6f,\n", series.getMax()));
        sb.append(String.format(Locale.US, "    \"mean\": %.6f\n", series.getMean()));
        sb.append("  },\n");
        sb.append("  \"data\": [\n");

        List<Long> ticks = series.getTicks();
        List<Double> vals = series.getValues();
        for (int i = 0; i < ticks.size(); i++) {
            sb.append(String.format(Locale.US, "    {\"tick\": %d, \"value\": %.6f}%s\n",
                    ticks.get(i), vals.get(i), (i < ticks.size() - 1 ? "," : "")));
        }
        sb.append("  ]\n");
        sb.append("}\n");

        writer.write(sb.toString());
    }

    private static void exportAsNdJson(StatisticalReplayEngine.ReconstitutedSeries series, Writer writer) throws IOException {
        List<Long> ticks = series.getTicks();
        List<Double> vals = series.getValues();
        for (int i = 0; i < ticks.size(); i++) {
            String line = String.format(Locale.US,
                    "{\"id\":\"%s\",\"tick\":%d,\"value\":%.6f,\"unit\":\"%s\"}\n",
                    escapeJson(series.getFormulaId()), ticks.get(i), vals.get(i), escapeJson(series.getUnit()));
            writer.write(line);
        }
    }

    private static void exportAsDelimiter(StatisticalReplayEngine.ReconstitutedSeries series, Writer writer, String sep) throws IOException {
        writer.write("# Formula: " + series.getFormulaName() + " (" + series.getExpression() + ")\n");
        writer.write("# Unit: " + series.getUnit() + "\n");
        writer.write("tick" + sep + "value\n");

        List<Long> ticks = series.getTicks();
        List<Double> vals = series.getValues();
        for (int i = 0; i < ticks.size(); i++) {
            writer.write(String.format(Locale.US, "%d%s%.6f\n", ticks.get(i), sep, vals.get(i)));
        }
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
