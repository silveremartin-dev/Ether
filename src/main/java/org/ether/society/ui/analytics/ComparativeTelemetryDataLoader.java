/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.HistorySnapshot;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SimulationRunRepository;
import org.ether.society.config.EtherPaths;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
import org.ether.society.persistence.EtherJsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Telemetry and Simulation History Snapshot Loader for Comparative Analytics.
 * 
 * <p>Ingests historical offline and GCP cluster run recordings, mapping discrete simulation
 * snapshot metrics into standardized dimensional SI continuous time-series records.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ComparativeTelemetryDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComparativeTelemetryDataLoader.class);

    private ComparativeTelemetryDataLoader() {
        // Static utility class
    }

    /**
     * Resolves the SimulationRunRecord for a given ScenarioSelectableItem.
     * Looks up by run ID, scenario name, preset key, and automatically attempts to load
     * telemetry from matching save directories on disk if not yet present in repository.
     *
     * @param item the selectable item
     * @param runRepository the repository storing loaded runs
     * @return the resolved SimulationRunRecord, or null if unexecuted
     */
    public static SimulationRunRecord resolveRunRecord(ScenarioSelectableItem item, SimulationRunRepository runRepository) {
        if (item == null) return null;
        String runId = item.getRunId();
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) return null;

        if (runId != null && !runId.isBlank() && !"N/A".equals(runId)) {
            SimulationRunRecord run = runRepository.getRun(runId);
            if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                return run;
            }
            ensureHistoryLoadedForSave(runId, item.getName(), runRepository);
            run = runRepository.getRun(runId);
            if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                return run;
            }
        }

        // Try lookup by scenario name
        SimulationRunRecord run = runRepository.getRunByScenarioName(item.getName()).orElse(null);
        if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
            return run;
        }

        if (item.getScenario() != null) {
            String scName = item.getScenario().getName();
            if (scName != null && !scName.isBlank()) {
                run = runRepository.getRunByScenarioName(scName).orElse(null);
                if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                    return run;
                }
            }

            // Fallback: search disk saves directory for matching directory or metadata
            String scKey = item.getScenario().resolvePresetKey();
            String scNorm = scName != null ? scName.toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String keyNorm = scKey != null ? scKey.toLowerCase().replaceAll("[^a-z0-9]", "") : "";

            try (Stream<Path> stream = Files.list(EtherPaths.getSavesDir())) {
                var match = stream.filter(Files::isDirectory).filter(d -> {
                    String dName = d.getFileName().toString().toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (!keyNorm.isEmpty() && dName.contains(keyNorm)) return true;
                    if (!scNorm.isEmpty() && (dName.contains(scNorm) || scNorm.contains(dName))) return true;
                    return false;
                }).findFirst();

                if (match.isPresent()) {
                    String foundId = match.get().getFileName().toString();
                    ensureHistoryLoadedForSave(foundId, item.getName(), runRepository);
                    run = runRepository.getRun(foundId);
                    if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                        item.setRunId(foundId);
                        item.setExecuted(true);
                        return run;
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Ensures that snapshot telemetry history is read from disk and cached into the run repository.
     *
     * @param saveId save directory identifier
     * @param scenarioName name of the scenario
     * @param runRepository target repository
     */
    public static void ensureHistoryLoadedForSave(String saveId, String scenarioName, SimulationRunRepository runRepository) {
        if (saveId == null || saveId.isBlank() || "N/A".equals(saveId) || "HISTORICAL_GROUND_TRUTH".equals(saveId)) return;
        if (runRepository.getRun(saveId) != null) return;

        Path histPath = EtherPaths.getSavesDir().resolve(saveId).resolve("history.json");
        if (!Files.exists(histPath)) {
            try (Stream<Path> stream = Files.list(EtherPaths.getSavesDir())) {
                var found = stream.filter(Files::isDirectory).filter(d -> {
                    if (d.getFileName().toString().equalsIgnoreCase(saveId)) return true;
                    Path m = d.resolve("metadata.json");
                    if (Files.exists(m)) {
                        try {
                            JsonNode node = EtherJsonUtil.getMapper().readTree(m.toFile());
                            if (node.has("id") && saveId.equalsIgnoreCase(node.get("id").asText())) return true;
                            if (scenarioName != null && node.has("scenarioName") && scenarioName.equalsIgnoreCase(node.get("scenarioName").asText())) return true;
                        } catch (Exception ignored) {}
                    }
                    return false;
                }).findFirst();
                if (found.isPresent()) {
                    histPath = found.get().resolve("history.json");
                }
            } catch (Exception ignored) {}
        }
        if (!Files.exists(histPath)) return;

        try {
            ObjectMapper mapper = EtherJsonUtil.getMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<HistorySnapshot> snaps = mapper.readValue(histPath.toFile(),
                new TypeReference<List<HistorySnapshot>>() {});
            if (snaps != null && !snaps.isEmpty()) {
                SimulationRunRecord rec = new SimulationRunRecord(saveId, scenarioName, "GCP/Offline Full True Simulation", null);
                for (HistorySnapshot snap : snaps) {
                    Map<String, Double> metricsMap = new LinkedHashMap<>();
                    long pop = Math.max(1L, snap.totalPopulation());
                    double popDbl = (double) snap.totalPopulation();

                    // 1. Demographics & Population
                    metricsMap.put("population", popDbl);
                    metricsMap.put("worldPopulation", popDbl);
                    metricsMap.put("fertilityRate", snap.fertilityRate());
                    metricsMap.put("lifeExpectancy", snap.avgLifespan());
                    metricsMap.put("educationLevel", Math.min(100.0, snap.avgTechnology() * 10.0));
                    metricsMap.put("populationSurvivalRate", 100.0);

                    // 2. Food, Energy & Matter
                    double foodPerCap = snap.totalFood() > 0 ? (snap.totalFood() / pop) * 3.362 : 3.362;
                    metricsMap.put("foodPerCapita", foodPerCap);
                    metricsMap.put("totalFood", snap.totalFood());
                    metricsMap.put("primaryEnergy", snap.energyCaptured() * 3.15576e-5);
                    metricsMap.put("energyCaptured", snap.energyCaptured());
                    metricsMap.put("energyPerCapita", snap.energyCaptured() > 0 ? (snap.energyCaptured() / pop) : 40.0);
                    metricsMap.put("resourceDepletion", snap.resourceDepletion());
                    metricsMap.put("potableWater", snap.potableWater());

                    double baseAnomaly = HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                        "temperatureAnomaly", snap.year(), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);

                    double shockForcing = 0.0;
                    String scLower = (scenarioName != null ? scenarioName : saveId).toLowerCase();
                    if (scLower.contains("nuclear_winter") || scLower.contains("hiver_nucleaire")) {
                        shockForcing = -12.0;
                    } else if (scLower.contains("supervolcano") || scLower.contains("toba")) {
                        shockForcing = -5.0;
                    } else if (scLower.contains("younger_dryas") || scLower.contains("recents_dryas") || scLower.contains("dryas")) {
                        shockForcing = -1.2;
                    } else if (snap.year() >= 1850) {
                        double co2Ppm = 280.0 + Math.min(600.0, snap.carbonFootprint() * 0.005);
                        shockForcing = 3.0 * (Math.log(co2Ppm / 280.0) / Math.log(2.0));
                    }

                    double tempAnomaly = Math.max(-20.0, Math.min(25.0, baseAnomaly + shockForcing));
                    double tempAbsolute = 14.0 + tempAnomaly;

                    metricsMap.put("temperatureAnomaly", tempAnomaly);
                    metricsMap.put("temperature", tempAbsolute);
                    metricsMap.put("entropyPollution", Math.min(100.0, snap.carbonFootprint() * 0.01));
                    metricsMap.put("precipitation", 950.0 + (tempAnomaly * 25.0));
                    metricsMap.put("co2Concentration", 280.0 + (snap.year() >= 1850 ? Math.min(600.0, snap.carbonFootprint() * 0.005) : 0.0));
                    metricsMap.put("urbanizationRate", Math.min(100.0, Math.max(1.0, snap.avgTechnology() * 0.8)));
                    metricsMap.put("literacyRate", Math.min(100.0, snap.avgTechnology() * 1.0));
                    metricsMap.put("sociopoliticalInstability", snap.conflictLevel());
                    metricsMap.put("currencyDebasement", snap.globalGini() * 50.0);
                    metricsMap.put("eroiAlim", Math.max(1.0, 15.0 - (snap.resourceDepletion() * 0.1)));
                    metricsMap.put("netSurplus", 85.0);
                    metricsMap.put("trophicMultiplier", 2.25);
                    metricsMap.put("biomassMobilized", 1000.0);

                    // 3. Society & Politics
                    metricsMap.put("avgTechLevel", snap.avgTechnology());
                    metricsMap.put("technology", snap.avgTechnology());
                    metricsMap.put("asabiyyah", Math.max(0.0, Math.min(100.0, 100.0 - snap.conflictLevel() - snap.globalGini() * 40.0)));
                    metricsMap.put("happiness", snap.happinessIndex());
                    metricsMap.put("conflict", snap.conflictLevel());
                    metricsMap.put("kardashev", snap.kardashevScale());
                    metricsMap.put("institutionalMaturity", snap.avgTechnology() * 5.0);

                    // 4. Economy & Capital
                    metricsMap.put("gini", snap.globalGini());
                    metricsMap.put("giniIndex", snap.globalGini());
                    double gdpBillions = snap.gdpTotal() > 1e6 ? (snap.gdpTotal() / 1e9) : snap.gdpTotal();
                    metricsMap.put("gdp", gdpBillions);
                    metricsMap.put("gdpTotal", gdpBillions);
                    metricsMap.put("grossWorldProduct", gdpBillions);
                    metricsMap.put("builtCapital", snap.totalWealth() > 0 ? (snap.totalWealth() / pop) : snap.totalWealth());

                    // 5. Cognition & Culture
                    metricsMap.put("collectiveMemory", snap.collectiveMemory());
                    metricsMap.put("languageDiversity", Math.max(1.0, Math.log10(pop + 10.0)));

                    // 6. Cliodynamics, Collapse & Paleolithic Benchmarks
                    metricsMap.put("eliteOverproduction", snap.eliteOverproduction());
                    metricsMap.put("collapseRisk", snap.collapseVulnerability());
                    metricsMap.put("collapseVulnerability", snap.collapseVulnerability());
                    metricsMap.put("systemComplexity", snap.systemComplexity());
                    metricsMap.put("systemInterdependence", snap.systemComplexity());
                    metricsMap.put("carbonFootprint", snap.carbonFootprint());
                    metricsMap.put("megafaunaIndex", ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(snap.year(), popDbl / 1e6, 0.2));
                    metricsMap.put("milankovitchInsolation", ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(snap.year()));
                    metricsMap.put("zeroContainmentScore", 100.0);
                    metricsMap.put("psiStressIndex", snap.eliteOverproduction() * (1.0 + snap.globalGini()) * Math.max(0.5, snap.conflictLevel() / 20.0));
                    metricsMap.put("lyapunovExponent", 0.01);
                    metricsMap.put("epistemicDiscrepancy", 0.05);

                    rec.addSnapshot(snap.year(), snap.totalPopulation(), foodPerCap, snap.avgTechnology(), 100.0 - snap.conflictLevel(), 100, metricsMap);
                }
                runRepository.saveRun(rec);
            }
        } catch (Exception ex) {
            logger.warn("Could not deserialize history.json for save {}: {}", saveId, ex.getMessage());
        }
    }
}
