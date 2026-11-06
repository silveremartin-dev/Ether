/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.database.H3Cell;

import java.util.*;

/**
 * Central Metric Catalog & Registry for the Ether Simulation Platform.
 * Uniformly registers all 2D spatial metrics, 1D time-series aggregations,
 * and 0D scenario indicators across Map Canvas, Stats Panel, and Comparative Analytics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetricRegistry {

    private static final MetricRegistry INSTANCE = new MetricRegistry();
    private final Map<String, MetricDescriptor> metricsById = new LinkedHashMap<>();
    private final Map<String, MetricDescriptor> metricsByName = new LinkedHashMap<>();

    private MetricRegistry() {
        registerDefaultMetrics();
    }

    /**
     * Get instance.
     *
     * @return the resulting computation or state reference
     */
    public static MetricRegistry getInstance() {
        return INSTANCE;
    }

    private void register(MetricDescriptor descriptor) {
        metricsById.put(descriptor.getId(), descriptor);
        metricsByName.put(descriptor.getDisplayName(), descriptor);
    }

    private void registerDefaultMetrics() {
        MetricCatalogHelper.registerAll(this::register);
    }

    /**
     * Get descriptor.
     *
     * @param id the id parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptor(String id) {
        if (id == null) return null;
        if (metricsById.containsKey(id)) return metricsById.get(id);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(id)) return d;
        }
        return metricsByName.get(id);
    }

    /**
     * Get descriptor by name.
     *
     * @param displayName the display name parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptorByName(String displayName) {
        if (displayName == null) return null;
        if (metricsByName.containsKey(displayName)) return metricsByName.get(displayName);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(displayName)) return d;
        }
        return null;
    }

    /**
     * Get all metrics.
     *
     * @return the resulting computation or state reference
     */
    public Collection<MetricDescriptor> getAllMetrics() {
        return Collections.unmodifiableCollection(metricsById.values());
    }

    /**
     * Get all metric names.
     *
     * @return the resulting computation or state reference
     */
    public List<String> getAllMetricNames() {
        List<String> list = new ArrayList<>();
        for (MetricDescriptor d : metricsById.values()) {
            list.add(d.getDisplayName());
        }
        return list;
    }

    /**
     * Computes a full map of metric snapshot values for the given simulation engine state.
     * Records 100% of physical, environmental, macroeconomic, and cliodynamic variables
     * calculated directly by the engine and its metrics aggregator.
     */
    public Map<String, Double> computeMetricsMap(H3SimulationEngine engine) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (engine == null) return map;

        SimulationMetricsAggregator agg = engine.getMetricsAggregator();
        List<H3Cell> cells = engine.getCells();
        long pop = engine.getTotalPopulation();
        long currentYear = engine.getCurrentYear();

        // 1. Primary Biophysical & Macro Observables (Canonical 27 Historical Benchmark Keys)
        map.put("population", (double) pop);
        map.put("worldPopulation", (double) pop);
        double gdp = engine.getCurrentGDP();
        map.put("gdp", gdp);
        map.put("grossWorldProduct", gdp > 0 ? gdp / 1e9 : (pop * 850.0) / 1e9);

        double capturedMw = engine.getEnergyCaptured();
        map.put("energyCaptured", capturedMw);
        map.put("primaryEnergy", capturedMw * 3.15576e-5); // MW to EJ/year
        map.put("energyPerCapita", engine.getEnergyPerCapita());

        double urbanRate = (agg != null) ? agg.getUrbanizationRate() : engine.getUrbanizationRate();
        map.put("urbanizationRate", urbanRate);

        double co2 = (agg != null) ? agg.getAtmosphericCO2Ppm() : engine.getAtmosphericCO2Ppm();
        map.put("co2Concentration", co2);

        double literacy = (agg != null) ? agg.getLiteracyRate() : engine.getLiteracyRate();
        map.put("literacyRate", literacy);

        double debasement = (agg != null) ? agg.getCurrencyDebasement() : 50.0;
        map.put("currencyDebasement", debasement);

        double eliteOverprod = (agg != null) ? agg.getEliteOverproductionIndex() : engine.getEliteOverproductionIndex();
        map.put("eliteOverproduction", eliteOverprod);
        map.put("eliteOverproductionIndex", eliteOverprod);

        double realWage = (agg != null) ? agg.getRealUnskilledWageIndex() : 100.0;
        map.put("realUnskilledWage", realWage);

        double psi = (agg != null) ? agg.getPoliticalStressIndex() : 35.0;
        map.put("politicalStressIndex", psi);

        double asabiyyah = (agg != null) ? agg.getAsabiyyahSocialCohesionScore() : engine.getAverageAsabiyyah();
        if (asabiyyah > 1.0) asabiyyah /= 100.0;
        map.put("asabiyyah", asabiyyah);
        map.put("asabiyyahSocialCohesion", asabiyyah);

        double conflict = engine.getConflictLevel();
        double instability = (agg != null) ? agg.getGeopoliticalTension() : conflict;
        map.put("conflict", conflict);
        map.put("sociopoliticalInstability", instability);

        double gini = engine.getCurrentGini();
        map.put("gini", gini);
        map.put("giniInequality", gini);

        double debt = (agg != null) ? agg.getSovereignDebtBurdenPct() : 0.0;
        map.put("sovereignDebtBurden", debt);

        double trade = (agg != null) ? agg.getGlobalTradeVolumeIndex() : 0.0;
        map.put("globalTradeVolume", trade);

        double tempAnom = (agg != null) ? agg.getTemperatureAnomaly() : engine.getTemperatureAnomaly();
        map.put("temperatureAnomaly", tempAnom);

        double erosion = (agg != null) ? agg.getTopsoilErosionLossPct() : 0.03;
        map.put("soilErosionRate", erosion);
        map.put("soilNPK", (agg != null) ? agg.getSoilNPKQuality() : 80.0);

        double eroei = engine.getEroiAlimentaire();
        map.put("eroiAlim", eroei);
        map.put("agriculturalEroei", eroei);

        double forest = (agg != null) ? agg.getForestCoverRemainingPct() : 85.0;
        map.put("deforestationRate", forest);

        double infoSpeed = (agg != null) ? agg.getInformationTravelSpeedKmPerDay() : 80.0;
        map.put("informationSpeed", infoSpeed);

        double insolation = org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(currentYear);
        double megafauna = org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(currentYear, pop / 1e6, 0.2);
        map.put("milankovitchInsolation", insolation);
        map.put("megafaunaIndex", megafauna);
        map.put("zeroContainmentScore", 1.0);

        double lifeExp = engine.getCurrentLifeExpectancy();
        map.put("lifeExpectancy", lifeExp);
        map.put("lifeExpectancyAtBirth", lifeExp);

        double metal = (agg != null) ? agg.getMetalSmeltingPerCapitaKg() : 1.5;
        map.put("metalSmeltingPerCapita", metal);

        double fossilShare = (agg != null) ? agg.getFossilSharePrimaryEnergyPct() : 0.0;
        map.put("fossilSharePrimaryEnergy", fossilShare);

        double freight = (agg != null) ? agg.getShippingFreightCostIndex() : 1400.0;
        map.put("shippingFreightCostIndex", freight);

        // 2. Additional Thermodynamic & Ecological Observables
        map.put("foodPerCapita", engine.getFoodPerCapita());
        map.put("populationSurvivalRate", engine.getPopulationSurvivalRate());
        map.put("fertilityRate", (double) engine.getCurrentFertility());
        map.put("resourceDepletion", engine.getResourceDepletionRate());
        map.put("netSurplus", engine.getNetSurplusFraction() * 100.0);
        map.put("trophicMultiplier", engine.getTrophicMultiplier());
        map.put("biomassMobilized", engine.getBiomassMobilizedPerCapitaKg());
        map.put("potableWater", engine.getPotableWaterTotal());
        map.put("entropyPollution", engine.getSystemicEntropy());
        map.put("happiness", engine.getHappinessIndex());
        map.put("institutionalMaturity", engine.getInstitutionalMaturity());
        map.put("kardashev", engine.getKardashevScale());
        map.put("builtCapital", engine.getBuiltCapitalTotal());
        map.put("collectiveMemory", engine.getCollectiveMemoryStock());
        map.put("collapseRisk", engine.getCollapseVulnerability());
        map.put("systemInterdependence", engine.getSystemInterdependenceIndex());
        map.put("avgTechLevel", (double) engine.getAverageTechnology());

        if (cells != null && !cells.isEmpty()) {
            for (MetricDescriptor desc : metricsById.values()) {
                if (!map.containsKey(desc.getId())) {
                    map.put(desc.getId(), desc.aggregate(cells));
                }
            }
        }

        return map;
    }
}


