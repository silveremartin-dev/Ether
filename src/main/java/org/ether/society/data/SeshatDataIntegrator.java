/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Seshat: Global History Databank Integrator.
 * Connects cliodynamic institutional complexity measures (governance structures, writing systems,
 * money, infrastructure, rituals, military technology) to Ether's N-dimensional cultural tensors.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 4.2.0
 */
public class SeshatDataIntegrator {
    private static final Logger logger = LoggerFactory.getLogger(SeshatDataIntegrator.class);

    public static class SeshatPolityRecord {
        public String polityId;          // e.g., "ItRome01", "FrFrank02", "CnSong01", "EgOldK01"
        public String polityName;        // e.g., "Imperium Romanum", "Kingdom of France", "Song Dynasty"
        public int yearStart;
        public int yearEnd;
        public double institutionalComplexity; // 0.0 - 1.0
        public double informationSystemScore;  // Writing / Record keeping
        public double ritualComplexityScore;   // Temples, sacred rites, priesthood
        public double militaryTechScore;       // Cavalry, fortifications, iron/bronze arms
        public double infrastructureScore;     // Roads, canals, aqueducts
    }

    private static final Map<String, SeshatPolityRecord> BENCHMARK_POLITIES = new HashMap<>();
    private static JsonNode cliodynamicBenchmarkJson = null;

    static {
        // Load embedded cliodynamic benchmarks JSON
        try (InputStream is = SeshatDataIntegrator.class.getResourceAsStream("/historical_cliodynamic_benchmarks.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                cliodynamicBenchmarkJson = mapper.readTree(is);
                logger.info("Loaded 20-Variable Cliodynamic Benchmark Suite from historical_cliodynamic_benchmarks.json");
            }
        } catch (Exception e) {
            logger.warn("Failed to load historical_cliodynamic_benchmarks.json: {}", e.getMessage());
        }

        // Initialize default Seshat polity benchmarks
        registerPolity("ROMAN_EMPIRE", "ItRome01", "Imperium Romanum", -27, 476, 0.88, 0.90, 0.85, 0.82, 0.92);
        registerPolity("FRANCE_1800", "FrFrance03", "République Française", 1792, 1815, 0.95, 0.96, 0.70, 0.95, 0.90);
        registerPolity("EGYPT_OLD_KINGDOM", "EgOldK01", "Old Kingdom of Egypt", -2686, -2181, 0.65, 0.60, 0.92, 0.50, 0.75);
        registerPolity("MESOPOTAMIA_SUMER", "IqSumer01", "Sumerian City-States", -3500, -2000, 0.58, 0.65, 0.88, 0.45, 0.70);
        registerPolity("SONG_DYNASTY", "CnSong01", "Song Dynasty China", 960, 1279, 0.92, 0.95, 0.80, 0.88, 0.94);
        registerPolity("MALI_EMPIRE", "MlMali01", "Mali Empire", 1235, 1670, 0.75, 0.70, 0.85, 0.75, 0.78);
        registerPolity("AZTEC_EMPIRE", "MxAztec01", "Triple Alliance (Aztec Empire)", 1428, 1521, 0.72, 0.60, 0.90, 0.68, 0.72);
        registerPolity("INCA_EMPIRE", "PeInca01", "Tawantinsuyu (Inca Empire)", 1438, 1533, 0.78, 0.55, 0.88, 0.72, 0.88);
        registerPolity("MAURYA_EMPIRE", "InMaurya01", "Maurya Empire", -322, -185, 0.80, 0.78, 0.85, 0.78, 0.82);
        registerPolity("JAPAN_EDO", "JpTokug01", "Tokugawa Shogunate", 1603, 1867, 0.86, 0.90, 0.75, 0.82, 0.85);

        // Load local CSV database from data/maps/seshat/ if present
        loadLocalPolitiesCsv();
    }

    private static void loadLocalPolitiesCsv() {
        java.io.File csvFile = new java.io.File("data/maps/seshat/seshat_polities_database.csv");
        if (!csvFile.exists()) return;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] parts = line.split(",");
                if (parts.length >= 10) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    int start = Integer.parseInt(parts[3].trim());
                    int end = Integer.parseInt(parts[4].trim());
                    double complexity = Double.parseDouble(parts[5].trim());
                    double info = Double.parseDouble(parts[6].trim());
                    double ritual = Double.parseDouble(parts[7].trim());
                    double military = Double.parseDouble(parts[8].trim());
                    double infra = Double.parseDouble(parts[9].trim());

                    String key = name.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
                    registerPolity(key, id, name, start, end, complexity, info, ritual, military, infra);
                    BENCHMARK_POLITIES.put(id.toUpperCase(), BENCHMARK_POLITIES.get(key));
                }
            }
            logger.info("Loaded {} polities from local CSV dataset {}", BENCHMARK_POLITIES.size(), csvFile.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Failed reading local Seshat CSV dataset: {}", e.getMessage());
        }
    }

    private static void registerPolity(String key, String id, String name, int start, int end,
                                       double complexity, double info, double ritual, double military, double infra) {
        SeshatPolityRecord r = new SeshatPolityRecord();
        r.polityId = id;
        r.polityName = name;
        r.yearStart = start;
        r.yearEnd = end;
        r.institutionalComplexity = complexity;
        r.informationSystemScore = info;
        r.ritualComplexityScore = ritual;
        r.militaryTechScore = military;
        r.infrastructureScore = infra;
        BENCHMARK_POLITIES.put(key.toUpperCase(), r);
    }

    public static SeshatPolityRecord getSeshatRecord(String key) {
        if (key == null) return null;
        return BENCHMARK_POLITIES.get(key.toUpperCase());
    }

    public static Map<String, SeshatPolityRecord> getAllBenchmarkPolities() {
        return new HashMap<>(BENCHMARK_POLITIES);
    }

    public static JsonNode getCliodynamicBenchmarkData() {
        return cliodynamicBenchmarkJson;
    }
}
