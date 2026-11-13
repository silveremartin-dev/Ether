/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.analytics.HistoricalScenarioCalibrationHarness;
import org.ether.society.model.Scenario;
import org.ether.society.model.StartDatePreset;
import org.ether.society.persistence.PresetStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a CLI scenario identifier against calibration definitions, built-in scenarios,
 * JSON presets, and {@link StartDatePreset} epochs, applying explicit CLI overrides.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see HeadlessRunner
 */
public final class HeadlessScenarioResolver {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessScenarioResolver.class);

    private HeadlessScenarioResolver() {
        // Stateless resolver
    }

    /**
     * Resolves and configures the runtime {@link Scenario} from {@link HeadlessCliOptions}.
     *
     * @param opts parsed CLI options
     * @return resolved and configured {@link Scenario}
     */
    public static Scenario resolveAndConfigure(HeadlessCliOptions opts) {
        String scenarioName = opts.scenarioName;
        Scenario scenario = new Scenario();
        scenario.setName("Headless: " + scenarioName);
        scenario.setPopulationDensityType(scenarioName);

        boolean matched = false;

        // Direct file resolution for preset JSON (e.g. earth_+1950_great_acceleration, black_death_1347, or path/to/file.json)
        java.nio.file.Path presetsDir = org.ether.society.config.EtherPaths.getPresetsScenariosDir();
        java.nio.file.Path directJsonPath = java.nio.file.Paths.get(scenarioName.endsWith(".json") ? scenarioName : scenarioName + ".json");
        if (!java.nio.file.Files.exists(directJsonPath) && presetsDir != null) {
            directJsonPath = presetsDir.resolve(scenarioName.endsWith(".json") ? scenarioName : scenarioName + ".json");
        }
        if (!java.nio.file.Files.exists(directJsonPath) && presetsDir != null && java.nio.file.Files.exists(presetsDir)) {
            String target = scenarioName.toLowerCase(java.util.Locale.ROOT).replace(".json", "");
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(presetsDir)) {
                java.util.Optional<java.nio.file.Path> candidate = stream
                        .filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> {
                            String fn = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).replace(".json", "");
                            return fn.equalsIgnoreCase(target) || fn.contains(target) || target.contains(fn);
                        })
                        .findFirst();
                if (candidate.isPresent()) {
                    directJsonPath = candidate.get();
                }
            } catch (Exception e) {
                logger.warn("Could not list presets scenarios directory: {}", e.getMessage());
            }
        }
        if (java.nio.file.Files.exists(directJsonPath)) {
            try {
                Scenario loaded = org.ether.society.persistence.PresetStorageService.getMapper().readValue(directJsonPath.toFile(), Scenario.class);
                if (loaded != null) {
                    scenario = loaded.copy();
                    if (scenario.getName() == null || scenario.getName().isBlank()) {
                        scenario.setName(scenarioName);
                    }
                    logger.info("Directly loaded JSON Scenario Preset from file '{}': '{}' (Years {} -> {})",
                            directJsonPath.getFileName(), scenario.getName(), scenario.getStartDateYear(), scenario.getEndDateYear());
                    matched = true;
                }
            } catch (Exception e) {
                logger.warn("Could not directly load scenario JSON '{}': {}", directJsonPath, e.getMessage());
            }
        }

        if (!matched) {
            for (HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition calDef
                    : HistoricalScenarioCalibrationHarness.CALIBRATION_SCENARIOS) {
                if (calDef.scenarioKey().equalsIgnoreCase(scenarioName)
                        || calDef.displayName().equalsIgnoreCase(scenarioName)
                        || scenarioName.toLowerCase().contains(calDef.scenarioKey().toLowerCase())
                        || calDef.scenarioKey().toLowerCase().contains(scenarioName.toLowerCase())) {
                    scenario.setStartDateYear(calDef.startYear());
                    scenario.setEndDateYear(calDef.endYear());
                    scenario.setInitialHumanCount((long) (calDef.initialWorldPopMillions() * 1_000_000L));
                    scenario.setInitialCapitalPerCapita(calDef.initialCapitalPerCapita());
                    scenario.setInitialTechLevel((int) calDef.initialTechLevel());
                    scenario.setTemporalResolutionDays(365.0);
                    scenario.setName(calDef.displayName());
                    scenario.setDescription(calDef.historicalRegimeDescription());
                    logger.info("Matched Historical Calibration Scenario: '{}' (Years {} -> {})",
                            calDef.displayName(), calDef.startYear(), calDef.endYear());
                    matched = true;
                    break;
                }
            }
        }

        if (!matched) {
            for (Scenario builtIn : Scenario.getBuiltInScenarios()) {
                if ((builtIn.getPresetKey() != null && builtIn.getPresetKey().equalsIgnoreCase(scenarioName))
                        || (builtIn.getName() != null && builtIn.getName().equalsIgnoreCase(scenarioName))
                        || (builtIn.getPresetKey() != null && (scenarioName.toLowerCase().contains(builtIn.getPresetKey().toLowerCase())
                        || builtIn.getPresetKey().toLowerCase().contains(scenarioName.toLowerCase())))) {
                    scenario = builtIn.copy();
                    logger.info("Matched Built-In Scenario: '{}' (Years {} -> {})",
                            builtIn.getName(), builtIn.getStartDateYear(), builtIn.getEndDateYear());
                    matched = true;
                    break;
                }
            }
        }

        if (!matched) {
            for (Scenario jsonPreset : PresetStorageService.loadAllScenarios()) {
                if ((jsonPreset.getPresetKey() != null && jsonPreset.getPresetKey().equalsIgnoreCase(scenarioName))
                        || (jsonPreset.getName() != null && jsonPreset.getName().equalsIgnoreCase(scenarioName))
                        || (jsonPreset.getPresetKey() != null && (scenarioName.toLowerCase().contains(jsonPreset.getPresetKey().toLowerCase())
                        || jsonPreset.getPresetKey().toLowerCase().contains(scenarioName.toLowerCase())))) {
                    scenario = jsonPreset.copy();
                    logger.info("Matched JSON Scenario Preset: '{}' (Years {} -> {})",
                            jsonPreset.getName(), jsonPreset.getStartDateYear(), jsonPreset.getEndDateYear());
                    matched = true;
                    break;
                }
            }
        }

        if (!matched) {
            for (StartDatePreset preset : StartDatePreset.values()) {
                if (preset.name().equalsIgnoreCase(scenarioName)
                        || preset.getDisplayName().equalsIgnoreCase(scenarioName)
                        || scenarioName.toLowerCase().contains(preset.getDisplayName().toLowerCase())) {
                    scenario.setStartDateYear(preset.getYear());
                    scenario.setEndDateYear(preset.getYear()
                            + Math.max(500, (long) (opts.ticksToRun * scenario.getTemporalResolutionDays() / 365.25) + 50));
                    scenario.setInitialHumanCount(preset.getEstimatedPopulation());
                    scenario.setInitialTechLevel(preset.getEstimatedTechLevel());
                    scenario.setName(preset.getDisplayName());
                    logger.info("Matched Historical Scenario Preset: '{}' (Start Year: {})",
                            preset.getDisplayName(), preset.getYear());
                    matched = true;
                    break;
                }
            }
        }

        if (!matched) {
            scenario.setEndDateYear(scenario.getStartDateYear()
                    + Math.max(500, (long) (opts.ticksToRun * scenario.getTemporalResolutionDays() / 365.25) + 50));
            logger.info("Custom Scenario Name: '{}' (Using default start parameters)", scenarioName);
        }

        if (opts.h3Resolution >= 0) scenario.setH3Resolution(opts.h3Resolution);
        if (opts.overrideStartYear != null) scenario.setStartDateYear(opts.overrideStartYear);
        if (opts.overrideEndYear != null) scenario.setEndDateYear(opts.overrideEndYear);
        if (opts.overrideDt != null) scenario.setTemporalResolutionDays(opts.overrideDt);
        if (opts.overrideInitialPop != null) scenario.setInitialHumanCount(opts.overrideInitialPop);
        if (opts.overrideInitialTech != null) scenario.setInitialTechLevel(opts.overrideInitialTech);
        if (opts.randomSeed != null) scenario.setSeed(opts.randomSeed);

        if (opts.cliSparseCellSkipping != null) scenario.setSparseCellSkippingEnabled(opts.cliSparseCellSkipping);
        if (opts.cliOceanMacroAggregation != null) scenario.setOceanMacroAggregationEnabled(opts.cliOceanMacroAggregation);
        if (opts.cliCoastalNavigationOnly != null) scenario.setCoastalNavigationOnlyEnabled(opts.cliCoastalNavigationOnly);
        if (opts.cliOceanMultiRateTicking != null) scenario.setOceanMultiRateTickingEnabled(opts.cliOceanMultiRateTicking);
        if (opts.cliClimateTickFrequency != null) scenario.setClimateTickFrequency(opts.cliClimateTickFrequency);
        if (opts.cliParallelExecution != null) {
            scenario.setParallelExecutionEnabled(opts.cliParallelExecution);
            scenario.setParallelThreadCount(opts.threadCount);
        }
        if (opts.cliSpatialRangeTruncation != null) scenario.setSpatialRangeTruncationEnabled(opts.cliSpatialRangeTruncation);

        return scenario;
    }
}
