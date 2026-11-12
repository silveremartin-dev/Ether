/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ether.society.core.telemetry.CompiledTelemetryPlan.*;

/**
 * Ahead-of-time DAG compiler for live tick and offline snapshot telemetry evaluation.
 *
 * <p>Resolves all user/UI/benchmark metric aliases into canonical nodes, traverses only the minimal
 * transitive dependency subgraph, accumulates the required cellular/kernel {@code inputMask}, and
 * emits an immutable topologically sorted {@link CompiledTelemetryPlan}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see CompiledTelemetryPlan
 * @see TelemetrySubscription
 */
public final class TelemetryAstCompiler {

    private record NodeSpec(String canonicalId, int inputMask, List<String> dependencies) {
    }

    private static final Map<String, NodeSpec> NODES = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        // Primary cellular observables & shared intermediates
        reg("population", MASK_POP_FOOD_WATER);
        reg("populatedCellCount", MASK_POP_FOOD_WATER);
        reg("occupiedTerritory", MASK_POP_FOOD_WATER);
        reg("populationSurvivalRate", MASK_POP_FOOD_WATER);
        reg("foodPerCapita", MASK_POP_FOOD_WATER);
        reg("biomassNatural", MASK_POP_FOOD_WATER);
        reg("biomassDomesticated", MASK_POP_FOOD_WATER);
        reg("potableWater", MASK_POP_FOOD_WATER);

        reg("builtCapital", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL);
        reg("capitalPerCapita", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "population", "builtCapital");
        reg("energyCaptured", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL);
        reg("energyPerCapita", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "population", "energyCaptured");
        reg("avgTechLevel", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "capitalPerCapita", "energyPerCapita");
        reg("primaryEnergy", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "population", "avgTechLevel", "energyCaptured");
        reg("kardashevScale", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "population", "energyPerCapita");

        reg("resourceDepletion", MASK_ENERGY_CAPITAL);
        reg("remainingResources", MASK_ENERGY_CAPITAL, "resourceDepletion");
        reg("soilNPK", MASK_ENERGY_CAPITAL | MASK_ENV_BIOME_SOIL, "resourceDepletion", "avgTechLevel");

        // Statistical kernel reductions (only triggered when explicitly subscribed)
        reg("gini", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_STAT_GINI);
        reg("landGini", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_STAT_GINI, "gini");
        reg("gdp", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_STAT_GDP);
        reg("grossWorldProduct", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_STAT_GDP, "population", "avgTechLevel", "gdp");
        reg("lifeExpectancy", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_STAT_LIFE_EXP);
        reg("fertilityRate", MASK_POP_FOOD_WATER | MASK_STAT_FERTILITY);

        // Socio-economic & environmental DAG
        reg("urbanizationRate", MASK_POP_FOOD_WATER | MASK_URBAN_POP_WEIGHT | MASK_ENERGY_CAPITAL);
        reg("literacyRate", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL, "avgTechLevel");
        reg("asabiyyah", MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_POLITIES, "avgTechLevel");
        reg("asabiyyahSocialCohesion", 0, "asabiyyah");
        reg("currencyDebasement", 0, "avgTechLevel", "asabiyyah");
        reg("eliteOverproduction", MASK_POLITIES, "avgTechLevel", "asabiyyah");
        reg("realUnskilledWage", 0, "avgTechLevel");
        reg("politicalStressIndex", 0, "avgTechLevel", "asabiyyah");
        reg("sociopoliticalInstability", MASK_POLITIES, "avgTechLevel", "asabiyyah");
        reg("conflict", 0, "sociopoliticalInstability");

        reg("globalTradeVolume", 0, "avgTechLevel");
        reg("inlandFluvialFreightVolume", 0, "avgTechLevel");
        reg("shippingFreightCostIndex", 0, "avgTechLevel");
        reg("sovereignDebtBurden", 0, "avgTechLevel", "globalTradeVolume", "literacyRate", "asabiyyah");
        reg("zeroContainmentScore", 0, "globalTradeVolume");

        reg("fossilSharePrimaryEnergy", 0, "avgTechLevel");
        reg("co2Concentration", MASK_ENERGY_CAPITAL, "avgTechLevel", "energyCaptured", "fossilSharePrimaryEnergy");
        reg("temperatureAnomaly", MASK_ENV_BIOME_SOIL, "co2Concentration");
        reg("carbonFootprint", 0, "energyCaptured", "fossilSharePrimaryEnergy");

        reg("soilErosionRate", 0, "avgTechLevel");
        reg("agriculturalEroei", 0, "avgTechLevel");
        reg("netSurplus", 0, "agriculturalEroei");
        reg("deforestationRate", 0, "avgTechLevel");
        reg("wildBiodiversity", MASK_POP_FOOD_WATER);
        reg("wetBulbSafety", MASK_ENV_BIOME_SOIL);
        reg("carryingCapacitySat", MASK_POP_FOOD_WATER);
        reg("planetaryOvershoot", 0, "carryingCapacitySat");
        reg("megafaunaIndex", MASK_POP_FOOD_WATER | MASK_ENV_BIOME_SOIL);
        reg("milankovitchInsolation", MASK_ENV_BIOME_SOIL);

        reg("informationSpeed", 0, "avgTechLevel");
        reg("metalSmeltingPerCapita", 0, "avgTechLevel");
        reg("trophicMultiplier", 0, "avgTechLevel");
        reg("biomassMobilized", 0, "trophicMultiplier");
        reg("entropyPollution", 0, "population", "capitalPerCapita");
        reg("happiness", MASK_POP_FOOD_WATER, "foodPerCapita", "potableWater");

        reg("institutionalMaturity", 0, "avgTechLevel");
        reg("divisionLabor", 0, "avgTechLevel");
        reg("cityStates", MASK_POLITIES);
        reg("maxHierarchy", MASK_POLITIES);
        reg("largestCulture", MASK_POLITIES | MASK_POP_FOOD_WATER);
        reg("largestOrgComplexity", MASK_POLITIES, "avgTechLevel");
        reg("largestOrgEntropy", MASK_POLITIES, "energyPerCapita");
        reg("eliteFormation", 0, "avgTechLevel");
        reg("elderCapitalShare", 0, "lifeExpectancy");
        reg("landRent", MASK_POP_FOOD_WATER);
        reg("toolsCount", 0, "population", "capitalPerCapita");
        reg("productsCount", 0, "population", "capitalPerCapita");
        reg("shannonBandwidth", 0, "avgTechLevel");
        reg("collectiveMemory", 0, "population", "avgTechLevel");
        reg("innovationDiffusion", 0, "avgTechLevel");
        reg("knowledgeDecay", 0, "sociopoliticalInstability");
        reg("fiscalStress", 0, "sovereignDebtBurden");
        reg("geopoliticalTension", 0, "sociopoliticalInstability");
        reg("collapseRisk", 0, "politicalStressIndex", "asabiyyah");
        reg("systemComplexity", 0, "avgTechLevel");
        reg("reconstructionCapability", 0, "literacyRate");
        reg("systemInterdependence", 0, "avgTechLevel");
        reg("offspringPct", 0, "avgTechLevel");
        reg("ageFirstChild", 0, "avgTechLevel");
        reg("immigrationRate", MASK_AGENT_DEMO);
        reg("pibMaterialFlow", 0, "population", "avgTechLevel");
        reg("energyEroi", 0, "avgTechLevel");

        // Special telemetry views / diagnostics
        reg("agePyramid", MASK_POP_FOOD_WATER | MASK_AGE_PYRAMID);
        reg("densityDistribution", MASK_POP_FOOD_WATER | MASK_STAT_DENSITY_BIN);
        reg("spatialMap2D", 0);
        reg("culturalMatrix", 0);
        reg("engineTPS", 0);
        reg("ramMemory", 0);
        reg("cellCount", 0);

        // Aliases across MetricRegistry (27 canonical benchmarks) & MetricCardMeta (UI dashboard)
        alias("worldPopulation", "population");
        alias("gdpTotal", "grossWorldProduct");
        alias("culturalAffinityMatrix", "culturalMatrix");
        alias("spatialMaps", "spatialMap2D");
        alias("spatialMap", "spatialMap2D");
        alias("giniIndex", "gini");
        alias("giniInequality", "gini");
        alias("giniCoefficient", "gini");
        alias("giniCoeff", "gini");
        alias("eroiAlim", "agriculturalEroei");
        alias("turchinPsi", "politicalStressIndex");
        alias("eliteOverproductionIndex", "eliteOverproduction");
        alias("atmosphericCO2Ppm", "co2Concentration");
        alias("educationLevel", "literacyRate");
        alias("healthIndex", "happiness");
        alias("happinessIndex", "happiness");
        alias("lifeExpectancyAtBirth", "lifeExpectancy");
        alias("collapseVulnerability", "collapseRisk");
        alias("demographicPressure", "carryingCapacitySat");
        alias("technology", "avgTechLevel");
        alias("emergentTechnologyLevel", "avgTechLevel");
        alias("kardashev", "kardashevScale");
        alias("realUnskilledWageIndex", "realUnskilledWage");
        alias("sovereignDebtBurdenPct", "sovereignDebtBurden");
        alias("globalTradeVolumeIndex", "globalTradeVolume");
        alias("topsoilErosionLossPct", "soilErosionRate");
        alias("forestCoverRemainingPct", "deforestationRate");
        alias("informationTravelSpeedKmPerDay", "informationSpeed");
        alias("metalSmeltingPerCapitaKg", "metalSmeltingPerCapita");
        alias("fossilSharePrimaryEnergyPct", "fossilSharePrimaryEnergy");
        alias("biodiversityIndex", "wildBiodiversity");
    }

    private TelemetryAstCompiler() {
    }

    private static void reg(String id, int mask, String... deps) {
        NODES.put(id, new NodeSpec(id, mask, List.of(deps)));
    }

    private static void alias(String aliasId, String canonicalId) {
        ALIASES.put(aliasId, canonicalId);
    }

    /** Resolves an external or UI metric key to its canonical DAG node identifier. */
    public static String resolveCanonicalId(String metricId) {
        if (metricId == null) return "";
        String trimmed = metricId.trim();
        return ALIASES.getOrDefault(trimmed, trimmed);
    }

    /** @return unmodifiable set of all canonical DAG metric keys */
    public static Set<String> getAllCanonicalIds() {
        return Collections.unmodifiableSet(NODES.keySet());
    }

    /**
     * Compiles a {@link TelemetrySubscription} into an optimized, minimal-bitmask execution plan.
     *
     * @param subscription immutable metric and spatial subscription (nullable)
     * @return pre-compiled topological evaluation plan ({@link CompiledTelemetryPlan#NO_OP} if empty)
     */
    public static CompiledTelemetryPlan compile(TelemetrySubscription subscription) {
        if (subscription == null || subscription.isEmpty()) {
            return CompiledTelemetryPlan.NO_OP;
        }

        TelemetrySubscription effectiveSub = subscription;
        boolean hasCulturalId = subscription.getMetricIds().stream()
                .anyMatch(id -> "culturalMatrix".equals(resolveCanonicalId(id)));
        if (hasCulturalId && !subscription.isCulturalMatrixSubscribed()) {
            effectiveSub = effectiveSub.withCulturalMatrix(TelemetrySubscription.CulturalMatrixQuery.CURRENT);
        }

        boolean hasSpatialId = subscription.getMetricIds().stream()
                .anyMatch(id -> "spatialMap2D".equals(resolveCanonicalId(id)));
        if (hasSpatialId && !effectiveSub.isSpatialSubscribed()) {
            effectiveSub = effectiveSub.withSpatialQueries(List.of(
                    TelemetrySubscription.SpatialRasterQuery.renderedMap(
                            "spatial_density", TelemetrySubscription.SpatialChannel.POPULATION_DENSITY, 128, 64)));
        }

        Set<String> canonicalTargets = new LinkedHashSet<>();
        for (String req : effectiveSub.getMetricIds()) {
            canonicalTargets.add(resolveCanonicalId(req));
        }

        Set<String> visited = new LinkedHashSet<>();
        List<String> postOrder = new ArrayList<>();
        int[] maskAcc = new int[1];

        for (String target : canonicalTargets) {
            visitPostOrder(target, visited, postOrder, maskAcc);
        }

        return new CompiledTelemetryPlan(effectiveSub, canonicalTargets, postOrder, maskAcc[0]);
    }

    private static void visitPostOrder(String nodeId,
                                       Set<String> visited,
                                       List<String> postOrder,
                                       int[] maskAcc) {
        if (nodeId == null || visited.contains(nodeId)) {
            return;
        }
        visited.add(nodeId);
        NodeSpec spec = NODES.get(nodeId);
        if (spec == null) {
            return;
        }
        maskAcc[0] |= spec.inputMask();
        for (String dep : spec.dependencies()) {
            visitPostOrder(dep, visited, postOrder, maskAcc);
        }
        postOrder.add(spec.canonicalId());
    }
}
