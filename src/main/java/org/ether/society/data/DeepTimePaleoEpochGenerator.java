/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * <h1>DeepTimePaleoEpochGenerator</h1>
 * <p>
 * Generates academic cartographic rasters (25-layer tensor suite), 1D initial state vectors,
 * and scientific metadata for deep Paleolithic and Pleistocene planetary epochs:
 * <ul>
 *   <li>{@code -3,000,000 BP}: Late Pliocene Australopithecus Cradle (Afar, Turkana, South Africa)</li>
 *   <li>{@code -2,000,000 BP}: Early Pleistocene Homo erectus Out of Africa I (Dmanisi, Sangiran, Nihewan)</li>
 *   <li>{@code -1,000,000 BP}: Early/Mid-Pleistocene Acheulean Expansion (Africa, Southern Europe, Asia)</li>
 *   <li>{@code -300,000 BP}: Middle Paleolithic Archaic Sapiens & Neanderthal Dispersal (Jebel Irhoud)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class DeepTimePaleoEpochGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DeepTimePaleoEpochGenerator.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static final long[] DEEP_TIME_EPOCHS = {
        -3000000L,
        -2000000L,
        -1000000L,
        -300000L
    };

    private DeepTimePaleoEpochGenerator() {
        // Utility
    }

    /**
     * Generates all 4 deep-time prehistoric planetary epochs in data/ether/earth/.
     */
    public static void generateAllDeepTimeEpochs() {
        for (long epoch : DEEP_TIME_EPOCHS) {
            generateDeepTimeEpoch(epoch);
        }
    }

    /**
     * Generates a single deep-time prehistoric planetary epoch.
     *
     * @param epoch negative astronomical year (e.g. -2000000)
     */
    public static void generateDeepTimeEpoch(long epoch) {
        try {
            Path epochDir = Paths.get("data", "ether", "earth", String.valueOf(epoch));
            Files.createDirectories(epochDir);

            // 1. Copy lithospheric static rasters from -100,000 BP baseline
            Path refDir = Paths.get("data", "ether", "earth", "-100000");
            if (!Files.exists(refDir)) {
                refDir = Paths.get("data", "ether", "earth", "2025");
            }

            String[] staticLayers = {
                "elevation", "coal", "oil", "gas", "uranium", "helium3",
                "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };

            for (String layer : staticLayers) {
                Path dest = epochDir.resolve("earth_" + epoch + "_" + layer + ".png");
                if (!Files.exists(dest)) {
                    Path src = refDir.resolve("earth_-100000_" + layer + ".png");
                    if (!Files.exists(src)) {
                        src = refDir.resolve("earth_2026_" + layer + ".png");
                    }
                    if (Files.exists(src)) {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // 2. Build Scenario object and generate cultural & paleolithic tensors
            Scenario sc = new Scenario();
            sc.setPlanetPreset(org.ether.society.generation.PlanetPreset.EARTH_LIKE);
            sc.setStartDateYear(epoch);
            sc.setName("PALEOLITHIC_DEEP_" + Math.abs(epoch));
            sc.setPopulationDensityType("OUT_OF_AFRICA_100K");

            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

            // 3. Generate provenance_and_sources.json, cultural_registry.json, and README.md
            HistoricalEpochMetadataGenerator.generateOrUpdateEpochMetadataFiles(epochDir, epoch);

            // 4. Generate state_1d.json with strict zero-baseline and authentic paleo proxies
            generateState1dJson(epochDir, epoch);

            logger.info("Successfully generated deep time epoch {} in {}", epoch, epochDir);

        } catch (Exception e) {
            logger.error("Failed to generate deep time epoch {}", epoch, e);
        }
    }

    private static void generateState1dJson(Path epochDir, long epoch) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("planet", "earth");
        metadata.put("year", epoch);
        metadata.put("temporalWindow", Math.abs(epoch) + " BP");
        metadata.put("referenceModel", "PALEO_SOURCES");
        metadata.put("modelDescription", "Paléo Sources (EPICA Dome C, PaleoClim v1.2, Jouzel 2007, Biraben 1979)");
        root.put("metadata", metadata);

        Map<String, Object> vars = new LinkedHashMap<>();

        double pop = switch ((int) epoch) {
            case -3000000 -> 0.001; // 1,000 Australopithecus individuals
            case -2000000 -> 0.002; // 2,000 early Homo erectus / habilis
            case -1000000 -> 0.003; // 3,000 Acheulean Homo erectus
            case -300000  -> 0.010; // 10,000 Archaic Sapiens & Neanderthals
            default -> 0.005;
        };

        double co2 = switch ((int) epoch) {
            case -3000000 -> 380.0; // Pliocene warm period (foraminifera B/Ca proxies)
            case -2000000 -> 280.0; // Early Pleistocene interglacial
            case -1000000 -> 260.0; // Mid-Pleistocene Transition
            case -300000  -> 240.0; // MIS 9/8 Glacial-Interglacial
            default -> 260.0;
        };

        addVar(vars, "urbanizationRate", 0.0, "%", "Urbanization Rate", "Zero baseline for Pleistocene prehistory");
        addVar(vars, "literacyRate", 0.0, "%", "Literacy Rate", "Zero baseline: Writing emerges c. -3400 BCE");
        addVar(vars, "sovereignDebtBurden", 0.0, "% of GDP", "Sovereign Debt", "Zero baseline for non-state societies");
        addVar(vars, "eliteOverproductionIndex", 0.0, "Index", "Elite Overproduction", "Zero baseline");
        addVar(vars, "currencyDebasement", 0.0, "%", "Currency Debasement", "Zero baseline: Coinage emerges c. -650 BCE");
        addVar(vars, "deforestationRate", 0.0, "%", "Anthropogenic Deforestation", "Zero baseline");
        addVar(vars, "metalSmeltingPerCapita", 0.0, "kg/capita", "Metal Smelting", "Zero baseline: Metallurgy emerges c. -5000 BCE");
        addVar(vars, "agriculturalEroei", 0.0, "Ratio", "Agricultural EROEI", "Zero baseline: Pre-agricultural foraging");

        addVar(vars, "worldPopulation", pop, "Millions of people", "World Population", "Biraben (1979), Kremer (1993)");
        addVar(vars, "co2Concentration", co2, "ppm", "Atmospheric CO2", "EPICA Dome C, Jouzel et al. (2007), Lüthi et al. (2008)");
        addVar(vars, "giniInequality", 0.17, "Gini Index", "Wealth & Land Gini", "Kohler et al. (2017) Nature nomadic foraging baseline");
        addVar(vars, "lifeExpectancyAtBirth", 22.0, "Years", "Life Expectancy at Birth", "Gurven & Kaplan (2007) Pleistocene hominin mortality");
        addVar(vars, "sociopoliticalInstability", 5.0, "Index", "Inter-group Instability", "Inter-band skirmish background");

        root.put("variables", vars);

        File outFile = epochDir.resolve("state_1d.json").toFile();
        mapper.writeValue(outFile, root);
    }

    private static void addVar(Map<String, Object> map, String key, double value, String unit, String name, String source) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("value", value);
        v.put("unit", unit);
        v.put("name", name);
        v.put("source", source);
        map.put(key, v);
    }
}
