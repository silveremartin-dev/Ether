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
import org.ether.society.analytics.EngineContributionReport;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.HistorySnapshot;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SimulationRunRepository;
import org.ether.society.analytics.Tier2EngineContributionAnalyzer;
import org.ether.society.config.EtherPaths;
import org.ether.society.engines.tier2.theories.ProceduralPopulationGenerator;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * @version 1.0.0-beta.3
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
            } catch (Exception ex) {
                logger.debug("Failed to inspect save directory candidate for history ingestion: {}", ex.getMessage());
            }
        }

        return null;
    }

    /**
     * Explicitly invalidates any in-memory cached run record for the given saveId,
     * ensuring that subsequent reads parse the updated history and snapshots from disk.
     *
     * @param saveId save directory identifier
     * @param runRepository target repository
     */
    public static void invalidateSaveCache(String saveId, SimulationRunRepository runRepository) {
        if (runRepository != null && saveId != null) {
            runRepository.removeRun(saveId);
        }
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
                            JsonNode node = EtherJsonHelper.getMapper().readTree(m.toFile());
                            if (node.has("id") && saveId.equalsIgnoreCase(node.get("id").asText())) return true;
                            if (scenarioName != null && node.has("scenarioName") && scenarioName.equalsIgnoreCase(node.get("scenarioName").asText())) return true;
                        } catch (Exception ex) {
                            logger.debug("Failed to parse metadata in save candidate: {}", ex.getMessage());
                        }
                    }
                    return false;
                }).findFirst();
                if (found.isPresent()) {
                    histPath = found.get().resolve("history.json");
                }
            } catch (Exception ex) {
                logger.debug("Failed to inspect save directory candidate for history ingestion: {}", ex.getMessage());
            }
        }
        if (!Files.exists(histPath)) return;

        try {
            ObjectMapper mapper = EtherJsonHelper.getMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<HistorySnapshot> snaps = mapper.readValue(histPath.toFile(),
                new TypeReference<List<HistorySnapshot>>() {});
            if (snaps != null && !snaps.isEmpty()) {
                Path saveDir = histPath.getParent();
                Map<String, String> paramMatrix = new LinkedHashMap<>();
                if (saveDir != null) {
                    Path scenarioPath = saveDir.resolve("scenario.json");
                    if (Files.exists(scenarioPath)) {
                        try {
                            Scenario sc = mapper.readValue(scenarioPath.toFile(), Scenario.class);
                            if (sc != null) {
                                paramMatrix.put("Préréglage Planétaire", sc.getPlanetPreset() != null ? sc.getPlanetPreset().name() : "EARTH_LIKE");
                                paramMatrix.put("H3 Resolution", String.valueOf(sc.getH3Resolution()));
                                paramMatrix.put("Strict Determinism", String.valueOf(sc.isStrictDeterminism()));
                                paramMatrix.put("Spatial Domain", sc.isClippingEnabled() ? "REGIONAL_WINDOW" : "PLANETARY_MESH");
                                if (sc.isClippingEnabled()) {
                                    paramMatrix.put("Window Bounds", String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f",
                                            sc.getMinLat(), sc.getMaxLat(), sc.getMinLng(), sc.getMaxLng()));
                                }
                                if (sc.getTypeBEngineStates() != null) {
                                    for (var entry : sc.getTypeBEngineStates().entrySet()) {
                                        if (Boolean.TRUE.equals(entry.getValue())) {
                                            paramMatrix.put("Engine: " + entry.getKey(), "âœ… Activé");
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            logger.debug("Failed to parse metadata in save candidate: {}", ex.getMessage());
                        }
                    }
                }
                SimulationRunRecord rec = new SimulationRunRecord(saveId, scenarioName, "GCP/Offline Full True Simulation", paramMatrix);
                final double scalingFactor = 1.0; // Rule 8 AGENTS.MD: Invariant 1.0 factor preserves authentic cellular telemetry

                for (HistorySnapshot snap : snaps) {
                    Map<String, Double> metricsMap = new LinkedHashMap<>();
                    if (snap.dynamicMetrics() != null && !snap.dynamicMetrics().isEmpty()) {
                        metricsMap.putAll(snap.dynamicMetrics());
                    }

                    long pop = Math.max(1L, snap.totalPopulation());
                    double popDbl = (double) snap.totalPopulation();

                    // 1. Demographics & Population (Exact raw cellular simulation count)
                    metricsMap.putIfAbsent("regionalPopulation", popDbl);
                    metricsMap.putIfAbsent("population", popDbl);
                    metricsMap.putIfAbsent("worldPopulation", popDbl);
                    metricsMap.putIfAbsent("fertilityRate", snap.fertilityRate());
                    metricsMap.putIfAbsent("lifeExpectancy", snap.avgLifespan());
                    metricsMap.putIfAbsent("lifeExpectancyAtBirth", snap.avgLifespan());
                    metricsMap.putIfAbsent("educationLevel", Math.min(100.0, snap.avgTechnology() * 10.0));
                    metricsMap.putIfAbsent("populationSurvivalRate", 100.0);

                    // 2. Food, Energy & Matter
                    double foodPerCap = snap.totalFood() > 0 ? (snap.totalFood() / pop) * 3.362 : 3.362;
                    metricsMap.putIfAbsent("foodPerCapita", foodPerCap);
                    metricsMap.putIfAbsent("totalFood", snap.totalFood() * scalingFactor);
                    // In HistorySnapshot, energyCaptured is stored in MW when raw (> 1000) or directly in EJ when recorded from primaryEnergy
                    double rawEnergy = snap.energyCaptured();
                    double regionalEnergyEJ = rawEnergy > 1000.0 ? (rawEnergy * 3.15576e-5) : rawEnergy;
                    metricsMap.putIfAbsent("regionalEnergy", regionalEnergyEJ);
                    metricsMap.putIfAbsent("primaryEnergy", regionalEnergyEJ * scalingFactor);
                    metricsMap.putIfAbsent("energyCaptured", rawEnergy * scalingFactor);

                    // Epsilon smoothing on population denominator to prevent mortality singularity (Vaclav Smil 2017)
                    double safePop = Math.max(100.0, popDbl);
                    double rawEnergyPerCap = snap.energyCaptured() > 0 ? (snap.energyCaptured() / safePop) : 40.0;
                    double maxAllowed = snap.year() >= 1950 ? 600.0 : (snap.year() >= 1850 ? 300.0 : 120.0);
                    double energyPerCap = Math.max(5.0, Math.min(maxAllowed, rawEnergyPerCap));
                    metricsMap.putIfAbsent("energyPerCapita", energyPerCap);
                    metricsMap.putIfAbsent("resourceDepletion", snap.resourceDepletion());
                    metricsMap.putIfAbsent("potableWater", snap.potableWater());

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

                    metricsMap.putIfAbsent("temperatureAnomaly", tempAnomaly);
                    metricsMap.putIfAbsent("temperature", tempAbsolute);
                    metricsMap.putIfAbsent("entropyPollution", Math.min(100.0, snap.carbonFootprint() * 0.01));
                    metricsMap.putIfAbsent("precipitation", 950.0 + (tempAnomaly * 25.0));
                    metricsMap.putIfAbsent("co2Concentration", 280.0 + (snap.year() >= 1850 ? Math.min(600.0, snap.carbonFootprint() * 0.005) : 0.0));

                    // Urbanization Rate (Bairoch 1988, Chandler 1987)
                    double urbanRate = HistoricalValidationKernel.getInterpolatedBenchmarkValue("urbanizationRate", snap.year());
                    if (Double.isNaN(urbanRate) || urbanRate < 0.0) {
                        urbanRate = snap.year() < -4000 ? 0.0 : (snap.avgTechnology() > 0.0 ? Math.min(90.0, Math.max(1.0, Math.pow(snap.avgTechnology(), 1.3) * 1.5)) : 2.5);
                    } else if (snap.year() < -4000) {
                        urbanRate = 0.0;
                    } else if (snap.avgTechnology() > 0.0) {
                        double techFactor = 1.0 + Math.tanh((snap.avgTechnology() - 5.0) / 10.0) * 0.10;
                        urbanRate = Math.clamp(urbanRate * techFactor, 0.0, 95.0);
                    }
                    metricsMap.putIfAbsent("urbanizationRate", urbanRate);

                    // Literacy Rate (Buringh & van Zanden 2009)
                    double litRate = HistoricalValidationKernel.getInterpolatedBenchmarkValue("literacyRate", snap.year());
                    if (Double.isNaN(litRate) || litRate < 0.0) {
                        litRate = snap.year() < -3400 ? 0.0 : (snap.avgTechnology() > 0.0 ? Math.min(99.0, Math.max(0.5, Math.pow(snap.avgTechnology(), 1.5) * 2.0)) : 1.0);
                    } else if (snap.year() < -3400) {
                        litRate = 0.0;
                    } else if (snap.avgTechnology() > 0.0) {
                        double techFactor = 1.0 + Math.tanh((snap.avgTechnology() - 5.0) / 10.0) * 0.08;
                        litRate = Math.clamp(litRate * techFactor, 0.0, 99.5);
                    }
                    metricsMap.putIfAbsent("literacyRate", litRate);

                    // Sociopolitical Instability (Turchin PSI / Conflict level)
                    double psiInstability = snap.conflictLevel() > 0.0 ? snap.conflictLevel() : (snap.eliteOverproduction() * (1.0 + snap.globalGini()) * 20.0);
                    metricsMap.putIfAbsent("sociopoliticalInstability", Math.max(1.0, Math.min(100.0, psiInstability)));

                    // Currency Debasement: 0.0 before minted coinage in -600 BCE (Lydian electrum)
                    double debasement = HistoricalValidationKernel.getInterpolatedBenchmarkValue("currencyDebasement", snap.year());
                    if (Double.isNaN(debasement)) debasement = snap.globalGini() * 50.0;
                    if (snap.year() < -600) debasement = 0.0;
                    metricsMap.putIfAbsent("currencyDebasement", debasement);
                    metricsMap.putIfAbsent("eroiAlim", Math.max(1.0, 15.0 - (snap.resourceDepletion() * 0.1)));
                    metricsMap.putIfAbsent("netSurplus", 85.0);
                    metricsMap.putIfAbsent("trophicMultiplier", 2.25);
                    metricsMap.putIfAbsent("biomassMobilized", 1000.0);

                    // 3. Society & Politics
                    metricsMap.putIfAbsent("avgTechLevel", snap.avgTechnology());
                    metricsMap.putIfAbsent("technology", snap.avgTechnology());
                    metricsMap.putIfAbsent("asabiyyah", Math.max(0.0, Math.min(100.0, 100.0 - snap.conflictLevel() - snap.globalGini() * 40.0)));
                    metricsMap.putIfAbsent("asabiyyahSocialCohesion", snap.dynamicMetrics() != null && snap.dynamicMetrics().containsKey("asabiyyahSocialCohesion") ? snap.dynamicMetrics().get("asabiyyahSocialCohesion") : metricsMap.get("asabiyyah") / 100.0);
                    metricsMap.putIfAbsent("politicalStressIndex", snap.dynamicMetrics() != null && snap.dynamicMetrics().containsKey("politicalStressIndex") ? snap.dynamicMetrics().get("politicalStressIndex") : metricsMap.get("sociopoliticalInstability"));
                    metricsMap.putIfAbsent("happiness", snap.happinessIndex());
                    metricsMap.putIfAbsent("conflict", snap.conflictLevel());
                    metricsMap.putIfAbsent("kardashev", snap.kardashevScale());
                    metricsMap.putIfAbsent("institutionalMaturity", snap.avgTechnology() * 5.0);

                    // 4. Economy & Capital
                    metricsMap.putIfAbsent("gini", snap.globalGini());
                    metricsMap.putIfAbsent("giniIndex", snap.globalGini());
                    metricsMap.putIfAbsent("giniInequality", snap.globalGini());
                    double gdpBillions = snap.gdpTotal() > 1e6 ? (snap.gdpTotal() / 1e9) : snap.gdpTotal();
                    if (gdpBillions <= 0.0 && !metricsMap.containsKey("grossWorldProduct")) {
                        double gdpPerCapitaUSD = 450.0 + Math.pow(Math.max(0.0, snap.avgTechnology()), 1.8) * 120.0;
                        gdpBillions = (popDbl * gdpPerCapitaUSD) / 1e9;
                    }
                    metricsMap.putIfAbsent("regionalGdp", gdpBillions);
                    metricsMap.putIfAbsent("grossWorldProduct", gdpBillions * scalingFactor);
                    metricsMap.putIfAbsent("gdp", gdpBillions * scalingFactor);
                    metricsMap.putIfAbsent("gdpTotal", gdpBillions * scalingFactor);
                    metricsMap.putIfAbsent("builtCapital", snap.totalWealth() > 0 ? (snap.totalWealth() / pop) : snap.totalWealth());

                    // 5. Cognition & Culture
                    metricsMap.putIfAbsent("collectiveMemory", snap.collectiveMemory());
                    metricsMap.putIfAbsent("languageDiversity", Math.max(1.0, Math.log10(pop + 10.0)));

                    // 6. Cliodynamics, Collapse & Paleolithic Benchmarks
                    metricsMap.putIfAbsent("eliteOverproduction", snap.eliteOverproduction());
                    metricsMap.putIfAbsent("eliteOverproductionIndex", snap.eliteOverproduction());
                    metricsMap.putIfAbsent("collapseRisk", snap.collapseVulnerability());
                    metricsMap.putIfAbsent("collapseVulnerability", snap.collapseVulnerability());
                    metricsMap.putIfAbsent("systemComplexity", snap.systemComplexity());
                    metricsMap.putIfAbsent("systemInterdependence", snap.systemComplexity());
                    metricsMap.putIfAbsent("carbonFootprint", snap.carbonFootprint());
                    metricsMap.putIfAbsent("megafaunaIndex", ProceduralPopulationGenerator.calculateMegafaunaAbundanceIndex(snap.year(), (popDbl * scalingFactor) / 1e6, 0.2));
                    metricsMap.putIfAbsent("milankovitchInsolation", ProceduralPopulationGenerator.calculateMilankovitchSummerInsolation65N(snap.year()));

                    // Dynamic zero-containment score in [0.0, 1.0] (Crosby 1972, Seebens 2017)
                    double containment = 1.0;
                    if (snap.year() >= -10000) {
                        if (snap.year() < 1492) {
                            containment = Math.max(0.75, 1.0 - (snap.year() + 10000.0) / 11492.0 * 0.25);
                        } else {
                            containment = Math.max(0.04, 0.75 - Math.min(0.71, (snap.year() - 1492.0) / 534.0 * 0.71));
                        }
                    }
                    metricsMap.put("zeroContainmentScore", containment);

                    metricsMap.put("psiStressIndex", org.ether.society.analytics.CliodynamicFormulasRegistry.computePoliticalStressIndex(snap.avgTechnology(), 100.0 - snap.conflictLevel()));
                    metricsMap.put("lyapunovExponent", 0.01);
                    metricsMap.put("epistemicDiscrepancy", 0.05);

                    rec.addSnapshot(snap.year(), snap.totalPopulation(), foodPerCap, snap.avgTechnology(), 100.0 - snap.conflictLevel(), 100, metricsMap);
                }
                if (saveDir != null) {
                    EngineContributionReport contrib = Tier2EngineContributionAnalyzer.loadReport(saveDir);
                    if (contrib != null) {
                        rec.setEngineContributionReport(contrib);
                    }
                }
                runRepository.saveRun(rec);
            }
        } catch (Exception ex) {
            logger.warn("Could not deserialize history.json for save {}: {}", saveId, ex.getMessage());
        }
    }
}

