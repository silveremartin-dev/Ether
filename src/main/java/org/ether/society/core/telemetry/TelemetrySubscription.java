/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable telemetry subscription contract governing what variables, spatial raster slices,
 * and subsampling cadence are evaluated during simulation ticks or post-hoc snapshot extraction.
 *
 * <p>By default ({@link #NONE}), no metrics or spatial slices are subscribed and telemetry
 * execution incurs strictly zero CPU overhead per tick.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class TelemetrySubscription {

    /**
     * Supported 2D physical and cultural cellular channels extractable from live buffers or saved snapshots.
     */
    public enum SpatialChannel {
        POPULATION_DENSITY("density"),
        BUILT_CAPITAL("capital"),
        TEMPERATURE("temperature"),
        BIOME("biome"),
        ECOLOGICAL_FOOTPRINT("ecological"),
        FOOD_RESERVES("food"),
        WATER_RESERVES("water"),
        NATURAL_BIOMASS("biomass_natural"),
        AGRICULTURAL_BIOMASS("biomass_agriculture");

        private final String rendererChannelKey;

        SpatialChannel(String rendererChannelKey) {
            this.rendererChannelKey = rendererChannelKey;
        }

        /** @return canonical channel key recognized by {@link org.ether.society.ui.analytics.SpatialTensorRasterRenderer} */
        public String getRendererChannelKey() {
            return rendererChannelKey;
        }
    }

    /**
     * Specification for a 2D map slice and/or zonal statistical aggregation over a spherical bounding box.
     *
     * @param queryId       unique result lookup key
     * @param channel       physical/cultural raster channel to extract
     * @param minLat        minimum latitude bound in degrees ($\ge -90$)
     * @param maxLat        maximum latitude bound in degrees ($\le +90$)
     * @param minLng        minimum longitude bound in degrees ($\ge -180$)
     * @param maxLng        maximum longitude bound in degrees ($\le +180$)
     * @param renderImage   {@code true} to render a projected equirectangular {@link java.awt.image.BufferedImage}
     * @param width         raster width in pixels when {@code renderImage == true}
     * @param height        raster height in pixels when {@code renderImage == true}
     */
    public record SpatialRasterQuery(
            String queryId,
            SpatialChannel channel,
            double minLat,
            double maxLat,
            double minLng,
            double maxLng,
            boolean renderImage,
            int width,
            int height) {

        public SpatialRasterQuery {
            Objects.requireNonNull(queryId, "queryId");
            Objects.requireNonNull(channel, "channel");
        }

        /** @return {@code true} if the query targets a sub-global geographic window */
        public boolean isWindowed() {
            return minLat > -89.99 || maxLat < 89.99 || minLng > -179.99 || maxLng < 179.99;
        }

        /** Creates a global zonal statistics query (no raster image allocation). */
        public static SpatialRasterQuery globalZonal(String queryId, SpatialChannel channel) {
            return new SpatialRasterQuery(queryId, channel, -90.0, 90.0, -180.0, 180.0, false, 0, 0);
        }

        /** Creates a bounded regional zonal statistics query (no raster image allocation). */
        public static SpatialRasterQuery regionalZonal(String queryId, SpatialChannel channel,
                                                       double minLat, double maxLat, double minLng, double maxLng) {
            return new SpatialRasterQuery(queryId, channel, minLat, maxLat, minLng, maxLng, false, 0, 0);
        }

        /** Creates a rendered equirectangular 2D map extraction + zonal statistics query. */
        public static SpatialRasterQuery renderedMap(String queryId, SpatialChannel channel, int width, int height) {
            return new SpatialRasterQuery(queryId, channel, -90.0, 90.0, -180.0, 180.0, true, width, height);
        }
    }

    /**
     * Specification for extracting the directional ethnolinguistic cultural affinity matrix \(N \times N\),
     * baseline delta \(\Delta(t - t_0)\), mean affinity, and cultural polarization index.
     *
     * @param enabled       {@code true} if cultural matrix evaluation is requested
     * @param computeDelta  {@code true} to evaluate cultural drift against baseline epoch $t_0$
     * @param baselineEpoch baseline calendar year $t_0$ for cultural drift evaluation
     */
    public record CulturalMatrixQuery(
            boolean enabled,
            boolean computeDelta,
            long baselineEpoch) {

        public static final CulturalMatrixQuery DISABLED = new CulturalMatrixQuery(false, false, 0L);
        public static final CulturalMatrixQuery CURRENT = new CulturalMatrixQuery(true, false, 0L);

        public static CulturalMatrixQuery withDelta(long baselineEpoch) {
            return new CulturalMatrixQuery(true, true, baselineEpoch);
        }
    }

    /** Canonical 27 empirical benchmark metric identifiers for explicit calibration runs. */
    public static final Set<String> CANONICAL_BENCHMARK_METRICS = Set.of(
            "population",
            "grossWorldProduct",
            "primaryEnergy",
            "urbanizationRate",
            "co2Concentration",
            "literacyRate",
            "currencyDebasement",
            "eliteOverproductionIndex",
            "realUnskilledWage",
            "politicalStressIndex",
            "asabiyyahSocialCohesion",
            "conflict",
            "sociopoliticalInstability",
            "gini",
            "sovereignDebtBurden",
            "globalTradeVolume",
            "temperatureAnomaly",
            "soilErosionRate",
            "soilNPK",
            "agriculturalEroei",
            "deforestationRate",
            "informationSpeed",
            "lifeExpectancy",
            "metalSmeltingPerCapita",
            "fossilSharePrimaryEnergy",
            "shippingFreightCostIndex",
            "zeroContainmentScore"
    );

    /** Empty default subscription: zero metrics, zero spatial queries, zero runtime overhead. */
    public static final TelemetrySubscription NONE = new TelemetrySubscription(
            Set.of(), List.of(), CulturalMatrixQuery.DISABLED, 1);

    private final Set<String> metricIds;
    private final List<SpatialRasterQuery> spatialQueries;
    private final CulturalMatrixQuery culturalMatrixQuery;
    private final int samplingIntervalTicks;

    public TelemetrySubscription(Collection<String> metricIds,
                                 Collection<SpatialRasterQuery> spatialQueries,
                                 int samplingIntervalTicks) {
        this(metricIds, spatialQueries, CulturalMatrixQuery.DISABLED, samplingIntervalTicks);
    }

    public TelemetrySubscription(Collection<String> metricIds,
                                 Collection<SpatialRasterQuery> spatialQueries,
                                 CulturalMatrixQuery culturalMatrixQuery,
                                 int samplingIntervalTicks) {
        if (metricIds == null || metricIds.isEmpty()) {
            this.metricIds = Set.of();
        } else {
            Set<String> cleaned = new LinkedHashSet<>();
            for (String id : metricIds) {
                if (id != null && !id.isBlank()) {
                    cleaned.add(id.trim());
                }
            }
            this.metricIds = Collections.unmodifiableSet(cleaned);
        }
        if (spatialQueries == null || spatialQueries.isEmpty()) {
            this.spatialQueries = List.of();
        } else {
            this.spatialQueries = List.copyOf(new ArrayList<>(spatialQueries));
        }
        this.culturalMatrixQuery = culturalMatrixQuery != null ? culturalMatrixQuery : CulturalMatrixQuery.DISABLED;
        this.samplingIntervalTicks = Math.max(1, samplingIntervalTicks);
    }

    /** Creates a scalar-only subscription sampled every {@code samplingIntervalTicks} ticks. */
    public static TelemetrySubscription ofMetrics(Collection<String> metricIds, int samplingIntervalTicks) {
        if (metricIds == null || metricIds.isEmpty()) {
            return NONE;
        }
        return new TelemetrySubscription(metricIds, List.of(), CulturalMatrixQuery.DISABLED, samplingIntervalTicks);
    }

    /** Creates an explicit subscription covering the canonical benchmark metrics. */
    public static TelemetrySubscription allCanonicalBenchmarks(int samplingIntervalTicks) {
        return new TelemetrySubscription(CANONICAL_BENCHMARK_METRICS, List.of(), CulturalMatrixQuery.DISABLED, samplingIntervalTicks);
    }

    /** Returns a copy with an updated sampling interval in ticks ($\ge 1$). */
    public TelemetrySubscription withSamplingInterval(int ticks) {
        int clamped = Math.max(1, ticks);
        if (clamped == this.samplingIntervalTicks) {
            return this;
        }
        return new TelemetrySubscription(this.metricIds, this.spatialQueries, this.culturalMatrixQuery, clamped);
    }

    /** Returns a copy with an updated cultural matrix query. */
    public TelemetrySubscription withCulturalMatrix(CulturalMatrixQuery query) {
        return new TelemetrySubscription(this.metricIds, this.spatialQueries, query, this.samplingIntervalTicks);
    }

    /** Returns a copy with an updated spatial queries list. */
    public TelemetrySubscription withSpatialQueries(Collection<SpatialRasterQuery> queries) {
        return new TelemetrySubscription(this.metricIds, queries, this.culturalMatrixQuery, this.samplingIntervalTicks);
    }

    /** Returns {@code true} when nothing is subscribed (zero-cost idle state). */
    public boolean isEmpty() {
        return metricIds.isEmpty() && spatialQueries.isEmpty() && !isCulturalMatrixSubscribed();
    }

    /** Returns {@code true} if cultural matrix evaluation is actively subscribed. */
    public boolean isCulturalMatrixSubscribed() {
        return culturalMatrixQuery != null && culturalMatrixQuery.enabled();
    }

    /** Returns {@code true} if 2D spatial raster queries are actively subscribed. */
    public boolean isSpatialSubscribed() {
        return !spatialQueries.isEmpty();
    }

    /**
     * Evaluates whether telemetry should execute on the given simulation tick index.
     *
     * @param tick current engine tick counter
     * @return {@code true} if active subscriptions exist and the sampling cadence aligns
     */
    public boolean shouldEvaluateAtTick(long tick) {
        if (isEmpty()) {
            return false;
        }
        return samplingIntervalTicks <= 1 || tick == 0 || (tick % samplingIntervalTicks) == 0;
    }

    public Set<String> getMetricIds() {
        return metricIds;
    }

    public List<SpatialRasterQuery> getSpatialQueries() {
        return spatialQueries;
    }

    public CulturalMatrixQuery getCulturalMatrixQuery() {
        return culturalMatrixQuery;
    }

    public int getSamplingIntervalTicks() {
        return samplingIntervalTicks;
    }
}
