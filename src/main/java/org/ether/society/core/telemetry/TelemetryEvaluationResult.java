/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable evaluation output produced by {@link CompiledTelemetryPlan} for a live tick
 * or an offline saved snapshot.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class TelemetryEvaluationResult {

    /**
     * Zonal summary statistics and optional equirectangular rendered image for a {@link TelemetrySubscription.SpatialRasterQuery}.
     */
    public record SpatialRasterResult(
            String queryId,
            TelemetrySubscription.SpatialChannel channel,
            double sum,
            double mean,
            double min,
            double max,
            int cellCount,
            BufferedImage renderedMap) {
    }

    public static final TelemetryEvaluationResult EMPTY = new TelemetryEvaluationResult(
            -1L, 0L, Map.of(), Map.of(),
            0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0f, 0.0f, 0.0f, 0.0f, 0,
            new int[0], new int[0]);

    private final long tick;
    private final long year;
    private final Map<String, Double> metrics;
    private final Map<String, SpatialRasterResult> spatialResults;

    private final long totalPopulation;
    private final double totalFood;
    private final double potableWater;
    private final double naturalBiomass;
    private final double builtCapitalTotal;
    private final double energyCapturedMw;
    private final double avgTech;
    private final float gini;
    private final float gdp;
    private final float lifeExpectancy;
    private final float fertility;
    private final int populatedCellCount;
    private final int[] agePyramid;
    private final int[] densityDistribution;

    public TelemetryEvaluationResult(
            long tick,
            long year,
            Map<String, Double> metrics,
            Map<String, SpatialRasterResult> spatialResults,
            long totalPopulation,
            double totalFood,
            double potableWater,
            double naturalBiomass,
            double builtCapitalTotal,
            double energyCapturedMw,
            double avgTech,
            float gini,
            float gdp,
            float lifeExpectancy,
            float fertility,
            int populatedCellCount,
            int[] agePyramid,
            int[] densityDistribution) {
        this.tick = tick;
        this.year = year;
        this.metrics = metrics != null ? Collections.unmodifiableMap(metrics) : Map.of();
        this.spatialResults = spatialResults != null ? Collections.unmodifiableMap(spatialResults) : Map.of();
        this.totalPopulation = totalPopulation;
        this.totalFood = totalFood;
        this.potableWater = potableWater;
        this.naturalBiomass = naturalBiomass;
        this.builtCapitalTotal = builtCapitalTotal;
        this.energyCapturedMw = energyCapturedMw;
        this.avgTech = avgTech;
        this.gini = gini;
        this.gdp = gdp;
        this.lifeExpectancy = lifeExpectancy;
        this.fertility = fertility;
        this.populatedCellCount = populatedCellCount;
        this.agePyramid = agePyramid != null ? agePyramid : new int[0];
        this.densityDistribution = densityDistribution != null ? densityDistribution : new int[0];
    }

    public boolean isEmpty() {
        return metrics.isEmpty() && spatialResults.isEmpty();
    }

    public long getTick() { return tick; }
    public long getYear() { return year; }
    public Map<String, Double> getMetrics() { return metrics; }
    public Map<String, SpatialRasterResult> getSpatialResults() { return spatialResults; }

    public Double getMetric(String key) {
        return metrics.get(key);
    }

    public long getTotalPopulation() { return totalPopulation; }
    public double getTotalFood() { return totalFood; }
    public double getPotableWater() { return potableWater; }
    public double getNaturalBiomass() { return naturalBiomass; }
    public double getBuiltCapitalTotal() { return builtCapitalTotal; }
    public double getEnergyCapturedMw() { return energyCapturedMw; }
    public double getAvgTech() { return avgTech; }
    public float getGini() { return gini; }
    public float getGdp() { return gdp; }
    public float getLifeExpectancy() { return lifeExpectancy; }
    public float getFertility() { return fertility; }
    public int getPopulatedCellCount() { return populatedCellCount; }
    public int[] getAgePyramid() { return agePyramid; }
    public int[] getDensityDistribution() { return densityDistribution; }
}
