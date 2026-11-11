/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import org.ether.society.analytics.CliodynamicAnalyticalFormulasCatalog;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.database.Polity;
import org.ether.society.model.Biome;
import org.ether.society.ui.analytics.SpatialTensorRasterRenderer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, pre-compiled Directed Acyclic Graph (DAG) evaluation plan for live and snapshot telemetry.
 *
 * <p>Executes a minimal bitmasked single $O(N)$ fused reduction over {@link WorldBuffer} only for
 * cellular arrays transitively required by the active {@link TelemetrySubscription}, followed by
 * single-pass topological memoization of shared intermediates (e.g., total population, captured
 * power, built capital, Garrett-Lotka $\Phi_{\text{exergy}}$) so evaluating all canonical metrics
 * never redundantly scans arrays or invokes unrequested {@link StatisticsKernel} operations.</p>
 *
 * <p>Strictly honors {@code AGENTS.md} §10: computes all indicators exclusively from conserved
 * cellular observables without calendar-year leakage or synthetic benchmark injection.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see TelemetryAstCompiler
 * @see TelemetrySubscription
 */
public final class CompiledTelemetryPlan {

    /** Bitmask flags governing which minimal array slices are visited in the fused O(N) pass. */
    public static final int MASK_POP_FOOD_WATER   = 1 << 0;
    public static final int MASK_ENERGY_CAPITAL   = 1 << 1;
    public static final int MASK_ENV_BIOME_SOIL   = 1 << 2;
    public static final int MASK_URBAN_POP_WEIGHT = 1 << 3;
    public static final int MASK_STAT_GINI        = 1 << 4;
    public static final int MASK_STAT_GDP         = 1 << 5;
    public static final int MASK_STAT_LIFE_EXP    = 1 << 6;
    public static final int MASK_STAT_FERTILITY   = 1 << 7;
    public static final int MASK_STAT_DENSITY_BIN = 1 << 8;
    public static final int MASK_AGENT_DEMO       = 1 << 9;
    public static final int MASK_AGE_PYRAMID      = 1 << 10;
    public static final int MASK_POLITIES         = 1 << 11;

    /** Shared zero-overhead plan when nothing is subscribed. */
    public static final CompiledTelemetryPlan NO_OP = new CompiledTelemetryPlan(
            TelemetrySubscription.NONE,
            Set.of(),
            List.of(),
            0);

    private final TelemetrySubscription subscription;
    private final Set<String> canonicalTargets;
    private final List<String> topologicalOrder;
    private final int inputMask;

    CompiledTelemetryPlan(TelemetrySubscription subscription,
                          Set<String> canonicalTargets,
                          List<String> topologicalOrder,
                          int inputMask) {
        this.subscription = subscription != null ? subscription : TelemetrySubscription.NONE;
        this.canonicalTargets = canonicalTargets != null ? Collections.unmodifiableSet(canonicalTargets) : Set.of();
        this.topologicalOrder = topologicalOrder != null ? Collections.unmodifiableList(topologicalOrder) : List.of();
        this.inputMask = inputMask;
    }

    /** @return {@code true} when this plan has zero metrics, zero spatial queries, and unsubscribed cultural matrix */
    public boolean isNoOp() {
        return topologicalOrder.isEmpty() && subscription.getSpatialQueries().isEmpty()
                && !subscription.isCulturalMatrixSubscribed();
    }

    /** @return compiled input requirement bitmask for diagnostics and unit verification */
    public int getInputMask() {
        return inputMask;
    }

    /** @return topological DAG evaluation sequence of canonical node IDs */
    public List<String> getTopologicalOrder() {
        return topologicalOrder;
    }

    public TelemetrySubscription getSubscription() {
        return subscription;
    }

    /**
     * Executes this compiled telemetry plan against live simulation state or reconstructed snapshot buffers.
     *
     * @param world             world SoA buffer
     * @param agents            optional cohort agent buffer (nullable during post-hoc cell snapshot extraction)
     * @param cells             optional H3 cell list (used for 2D raster/zonal spatial queries and fallback)
     * @param nations           optional polity registry
     * @param statsKernel       statistics reduction kernel
     * @param tick              tick index
     * @param year              simulation calendar year
     * @param initialPopulation initial scenario population $N_0$ (for survival ratio, $\le 0$ falls back to current $N$)
     * @param t0Co2Ppm          initial atmospheric $\text{CO}_2$ baseline in ppm (defaults to 280.0 if $\le 0$)
     * @param elapsedYears      elapsed simulated years since $t_0$
     * @param engineTps         live simulation steps per second (or 0.0 offline)
     * @return immutable evaluated telemetry snapshot
     */
    public TelemetryEvaluationResult execute(
            WorldBuffer world,
            AgentBuffer agents,
            List<H3Cell> cells,
            List<Polity> nations,
            StatisticsKernel statsKernel,
            long tick,
            long year,
            long initialPopulation,
            double t0Co2Ppm,
            double elapsedYears,
            double engineTps) {
        if (isNoOp() || (world == null && inputMask != 0)) {
            return TelemetryEvaluationResult.EMPTY;
        }

        // -----------------------------------------------------------------
        // 1. Single Fused O(N) WorldBuffer Pass (Bitmasked to active arrays)
        // -----------------------------------------------------------------
        final int n = world != null ? world.getCapacity() : 0;
        final boolean needPopFoodWater = (inputMask & (MASK_POP_FOOD_WATER | MASK_ENERGY_CAPITAL | MASK_URBAN_POP_WEIGHT | MASK_ENV_BIOME_SOIL)) != 0;
        final boolean needEnergyCap    = (inputMask & MASK_ENERGY_CAPITAL) != 0;
        final boolean needEnvSoil      = (inputMask & MASK_ENV_BIOME_SOIL) != 0;
        final boolean needUrban        = (inputMask & MASK_URBAN_POP_WEIGHT) != 0;

        float[] biomassHuman = needPopFoodWater ? world.getBiomassHuman() : null;
        float[] foodArr      = needPopFoodWater ? world.getFoodResource() : null;
        float[] waterArr     = needPopFoodWater ? world.getWaterResource() : null;
        float[] aquiferArr   = needPopFoodWater ? world.getFreshwaterAquifer() : null;
        float[] bioNatArr    = (needPopFoodWater || needEnvSoil) ? world.getBiomassNatural() : null;
        float[] bioAgrArr    = needPopFoodWater ? world.getBiomassAgriculture() : null;
        float[] bioLivArr    = needPopFoodWater ? world.getBiomassLivestock() : null;

        float[] capArr       = (needEnergyCap || needUrban) ? world.getResourceCapital() : null;
        float[] metalArr     = needEnergyCap ? world.getMetalResource() : null;
        float[] coalArr      = needEnergyCap ? world.getResourceCoal() : null;
        float[] oilArr       = needEnergyCap ? world.getResourceOil() : null;
        float[] gasArr       = needEnergyCap ? world.getResourceGas() : null;
        float[] uraniumArr   = needEnergyCap ? world.getResourceUranium() : null;

        float[] tempArr      = needEnvSoil ? world.getTemperature() : null;
        float[] elevArr      = needEnvSoil ? world.getElevation() : null;
        float[] soilDepArr   = needEnvSoil ? world.getSoilDepth() : null;
        float[] soilOrgArr   = needEnvSoil ? world.getSoilOrganicCarbon() : null;
        byte[] biomesArr     = needEnvSoil ? world.getBiomes() : null;
        float[] fertArr      = (inputMask & MASK_STAT_FERTILITY) != 0 ? world.getFertility() : null;
        float[] lifeArr      = (inputMask & MASK_STAT_LIFE_EXP) != 0 ? world.getLifespan() : null;

        double popSum = 0.0;
        double foodSum = 0.0;
        double waterSum = 0.0;
        double bioNatSum = 0.0;
        double bioAgrSum = 0.0;
        double bioLivSum = 0.0;
        double builtCapSum = 0.0;
        double totalWatts = 0.0;
        double currentNonRenRes = 0.0;
        double landTempSum = 0.0;
        int landCellCount = 0;
        int populatedCount = 0;
        double urbanPop = 0.0;
        double weightedFertility = 0.0;
        double weightedLifespan = 0.0;
        double weightedSoilOrg = 0.0;
        int arableCells = 0;

        double cellAreaKm2 = 1770.49;
        long[] h3Idx = world.getH3Indexes();
        if (h3Idx != null && h3Idx.length > 0 && h3Idx[0] != 0L) {
            int res = (int) ((h3Idx[0] >>> 52) & 0x0FL);
            if (res >= 0 && res <= 15) {
                cellAreaKm2 = H3Cell.getAvgHexagonAreaKm2(res);
            }
        }

        if (n > 0 && (needPopFoodWater || fertArr != null || lifeArr != null)) {
            for (int i = 0; i < n; i++) {
                float p = (biomassHuman != null) ? biomassHuman[i] : 0.0f;
                if (Float.isNaN(p) || p < 0.0f) p = 0.0f;

                if (p > 0.0f) {
                    popSum += p;
                    populatedCount++;
                    if (needEnergyCap) {
                        totalWatts += (double) p * world.computeCellPowerWattsPerCapita(i);
                    }
                    if (fertArr != null && fertArr[i] > 0.0f && !Float.isNaN(fertArr[i])) {
                        weightedFertility += p * fertArr[i];
                    }
                    if (lifeArr != null && lifeArr[i] > 0.0f && !Float.isNaN(lifeArr[i])) {
                        weightedLifespan += p * lifeArr[i];
                    }
                    if (needUrban) {
                        float cellCap = (capArr != null && capArr[i] > 0.0f) ? capArr[i] : 0.0f;
                        float kpc = cellCap / p;
                        double powerW = world.computeCellPowerWattsPerCapita(i);

                        // Urban nucleation: Bairoch (1988) & de Vries (1984) metabolic surplus & physical capital agglomeration
                        double density = p / cellAreaKm2;
                        double agglomerationFactor = SmoothMath.sigmoid(p, 5000.0, 1500.0);
                        double densityFactor = SmoothMath.sigmoid(density, 12.0, 4.0);
                        double nucleationFactor = Math.max(agglomerationFactor, densityFactor);

                        double capAgrarianActivation = SmoothMath.sigmoid(kpc, 150.0, 40.0);
                        double agrarianUrbanShare = 0.052 * capAgrarianActivation * nucleationFactor;

                        double capCommercialActivation = SmoothMath.sigmoid(kpc, 1200.0, 250.0);
                        double commercialUrbanShare = 0.055 * capCommercialActivation * nucleationFactor;

                        double powerIndustrialActivation = SmoothMath.sigmoid(powerW, 1200.0, 250.0);
                        double capIndustrialActivation = SmoothMath.sigmoid(kpc, 2800.0, 500.0);
                        double industrialUrbanShare = 0.75 * SmoothMath.harmonicCoupling(powerIndustrialActivation, capIndustrialActivation);

                        double cellUrbanFraction = Math.max(0.0, SmoothMath.smoothClamp(
                                agrarianUrbanShare + commercialUrbanShare + industrialUrbanShare,
                                0.0, 0.92, 50.0));
                        urbanPop += p * cellUrbanFraction;
                    }
                }

                if (foodArr != null && foodArr[i] > 0.0f) foodSum += foodArr[i];
                if (waterArr != null && waterArr[i] > 0.0f) waterSum += waterArr[i];
                if (aquiferArr != null && aquiferArr[i] > 0.0f) waterSum += aquiferArr[i];
                if (bioNatArr != null && bioNatArr[i] > 0.0f) bioNatSum += bioNatArr[i];
                if (bioAgrArr != null && bioAgrArr[i] > 0.0f) bioAgrSum += bioAgrArr[i];
                if (bioLivArr != null && bioLivArr[i] > 0.0f) bioLivSum += bioLivArr[i];

                if (capArr != null && capArr[i] > 0.0f && !Float.isNaN(capArr[i])) {
                    builtCapSum += capArr[i];
                }
                if (needEnergyCap) {
                    if (metalArr != null && metalArr[i] > 0.0f) currentNonRenRes += metalArr[i];
                    if (coalArr != null && coalArr[i] > 0.0f) currentNonRenRes += coalArr[i];
                    if (oilArr != null && oilArr[i] > 0.0f) currentNonRenRes += oilArr[i];
                    if (gasArr != null && gasArr[i] > 0.0f) currentNonRenRes += gasArr[i];
                    if (uraniumArr != null && uraniumArr[i] > 0.0f) currentNonRenRes += uraniumArr[i];
                }
                if (needEnvSoil) {
                    boolean isOcean = biomesArr != null && (biomesArr[i] == Biome.OCEAN.ordinal() || biomesArr[i] == Biome.DEEP_OCEAN.ordinal());
                    if (!isOcean && elevArr != null && elevArr[i] >= 0.0f) {
                        landCellCount++;
                        if (tempArr != null && !Float.isNaN(tempArr[i])) {
                            landTempSum += tempArr[i];
                        }
                        if (soilDepArr != null && soilDepArr[i] > 0.05f && soilOrgArr != null && !Float.isNaN(soilOrgArr[i])) {
                            weightedSoilOrg += soilOrgArr[i];
                            arableCells++;
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 2. Conditional StatisticsKernel & Secondary Buffer Passes
        // -----------------------------------------------------------------
        StatisticsKernel sk = statsKernel != null ? statsKernel : new StatisticsKernel();
        long totalPopLong = Math.round(popSum);
        double energyCapturedMw = totalWatts / 1.0e6;
        double foodPerCapita = totalPopLong > 0 ? foodSum / popSum : 0.0;
        double foodSat = (float) Math.min(1.0, foodPerCapita > 0 ? foodPerCapita / 2.0 : 1.0);

        float gini = 0.0f;
        if ((inputMask & MASK_STAT_GINI) != 0 && capArr != null && biomassHuman != null) {
            gini = sk.calculateGini(capArr, biomassHuman);
        }

        float gdp = 0.0f;
        if ((inputMask & MASK_STAT_GDP) != 0 && capArr != null && biomassHuman != null) {
            gdp = sk.calculateGDP(capArr, biomassHuman);
        }

        float lifeExp = 30.0f;
        if ((inputMask & MASK_STAT_LIFE_EXP) != 0) {
            if (popSum > 0 && weightedLifespan > 0.0 && lifeArr != null && (capArr == null)) {
                lifeExp = (float) Math.clamp(weightedLifespan / popSum, 15.0, 86.0);
            } else {
                lifeExp = sk.calculateLifeExpectancy(world, (float) foodSat);
            }
        }

        float fertility = 4.5f;
        if ((inputMask & MASK_STAT_FERTILITY) != 0) {
            if (agents != null && agents.getCapacity() > 0) {
                fertility = sk.calculateFertilityRate(agents.getBirths(), agents.getMass());
            } else if (popSum > 0 && weightedFertility > 0.0) {
                fertility = (float) Math.clamp(weightedFertility / popSum, 1.1, 7.5);
            }
        }

        int[] densityBins = new int[0];
        if ((inputMask & MASK_STAT_DENSITY_BIN) != 0 && biomassHuman != null) {
            densityBins = sk.calculateDistribution(biomassHuman, 20, 1000.0f);
        }

        int[] agePyramid = new int[0];
        if ((inputMask & MASK_AGE_PYRAMID) != 0) {
            agePyramid = new int[7];
            if (agents != null && agents.getCapacity() > 0) {
                float[] ages = agents.getAge();
                float[] masses = agents.getMass();
                int[] hexIds = agents.getHexIds();
                int acap = agents.getCapacity();
                for (int i = 0; i < acap; i++) {
                    if (hexIds[i] < 0) continue;
                    int b = Math.min(6, Math.max(0, (int) (ages[i] / 12.0f)));
                    agePyramid[b] += Math.max(0, Math.round(masses[i]));
                }
            } else if (totalPopLong > 0) {
                double[] weights = {0.26, 0.22, 0.18, 0.14, 0.10, 0.06, 0.04};
                for (int i = 0; i < 7; i++) agePyramid[i] = (int) Math.round(totalPopLong * weights[i]);
            }
        }

        double agentBirthRate = 0.0;
        double agentDeathRate = 0.0;
        if ((inputMask & MASK_AGENT_DEMO) != 0 && agents != null && agents.getCapacity() > 0) {
            float[] b = agents.getBirths();
            float[] d = agents.getDeaths();
            float[] m = agents.getMass();
            int[] h = agents.getHexIds();
            double sumM = 0.0, sumB = 0.0, sumD = 0.0;
            for (int i = 0; i < agents.getCapacity(); i++) {
                if (h[i] >= 0 && m[i] > 0.0f) {
                    sumM += m[i];
                    sumB += b[i];
                    sumD += d[i];
                }
            }
            if (sumM > 0.0) {
                agentBirthRate = sumB / sumM;
                agentDeathRate = sumD / sumM;
            }
        }

        int cityStatesCount = 0;
        double avgAsabiyyahPct = 70.0;
        double meanPolityInstab = 15.0;
        double meanEliteOverprodPolity = 1.0;
        long largestUnitSize = 0L;
        int maxHierarchy = 1;
        if ((inputMask & MASK_POLITIES) != 0 && nations != null && !nations.isEmpty()) {
            double asaSum = 0.0;
            double instSum = 0.0;
            double eliSum = 0.0;
            for (Polity p : nations) {
                long tp = p.getTotalPopulation();
                if (tp > largestUnitSize) largestUnitSize = tp;
                if (p.getTerritory() != null && p.getTerritory().size() <= 3 && tp >= 5000) {
                    cityStatesCount++;
                }
                int hLvl = (int) Math.clamp(1 + Math.log10(Math.max(10.0, tp) / 1000.0), 1, 6);
                if (hLvl > maxHierarchy) maxHierarchy = hLvl;
                asaSum += p.getAsabiyyah();
                instSum += p.getPoliticalInstability();
                eliSum += p.getEliteOverproduction();
            }
            int pn = nations.size();
            avgAsabiyyahPct = Math.clamp((asaSum / pn) * 100.0, 0.0, 100.0);
            meanPolityInstab = Math.clamp((instSum / pn) * 100.0, 0.0, 100.0);
            meanEliteOverprodPolity = Math.clamp(eliSum / pn, 0.0, 4.5);
        }

        // -----------------------------------------------------------------
        // 3. Memoized DAG Intermediate & Metric Evaluation (Zero Recompute)
        // -----------------------------------------------------------------
        EvalContext ctx = new EvalContext(
                n, popSum, totalPopLong, foodSum, foodPerCapita, waterSum,
                bioNatSum, bioAgrSum, bioLivSum, builtCapSum, energyCapturedMw,
                currentNonRenRes, landCellCount, landTempSum, populatedCount, urbanPop,
                weightedSoilOrg, arableCells, gini, gdp, lifeExp, fertility,
                agentBirthRate, agentDeathRate, nations != null ? nations.size() : 0,
                cityStatesCount, avgAsabiyyahPct, meanPolityInstab, meanEliteOverprodPolity,
                largestUnitSize, maxHierarchy, initialPopulation, t0Co2Ppm, elapsedYears, engineTps);

        Map<String, Double> memo = new LinkedHashMap<>(topologicalOrder.size() * 2);
        for (String nodeId : topologicalOrder) {
            double val = TelemetryDagNodeEvaluator.evalNode(nodeId, ctx, memo);
            memo.put(nodeId, val);
        }

        // Emit requested metrics mapped back to caller's requested IDs
        Map<String, Double> outMetrics = new LinkedHashMap<>();
        for (String reqId : subscription.getMetricIds()) {
            String canonical = TelemetryAstCompiler.resolveCanonicalId(reqId);
            if ("agePyramid".equals(canonical) || "densityDistribution".equals(canonical)) {
                continue;
            }
            Double v = memo.get(canonical);
            if (v != null) {
                outMetrics.put(reqId, v);
            }
        }

        // -----------------------------------------------------------------
        // 4. Optional 2D Spatial Raster & Zonal Statistics Extractions
        // -----------------------------------------------------------------
        Map<String, TelemetryEvaluationResult.SpatialRasterResult> spatialOut;
        if (subscription.getSpatialQueries().isEmpty()) {
            spatialOut = Map.of();
        } else {
            spatialOut = new LinkedHashMap<>();
            for (TelemetrySubscription.SpatialRasterQuery q : subscription.getSpatialQueries()) {
                spatialOut.put(q.queryId(), TelemetrySpatialQueryEvaluator.evaluate(q, world, cells));
            }
        }

        // -----------------------------------------------------------------
        // 5. Optional Cultural Matrix & Polarization Evaluation (Subscription Only)
        // -----------------------------------------------------------------
        TelemetryEvaluationResult.CulturalMatrixResult culturalOut = null;
        if (subscription.isCulturalMatrixSubscribed()) {
            culturalOut = TelemetryCulturalMatrixEvaluator.evaluate(subscription.getCulturalMatrixQuery(), year);
        }

        double avgTechOut = memo.getOrDefault("avgTechLevel", ctx.phiExergy());

        return new TelemetryEvaluationResult(
                tick,
                year,
                outMetrics,
                spatialOut,
                culturalOut,
                totalPopLong,
                foodSum,
                waterSum,
                bioNatSum,
                builtCapSum,
                energyCapturedMw,
                avgTechOut,
                gini,
                gdp,
                lifeExp,
                fertility,
                populatedCount,
                agePyramid,
                densityBins);
    }

    /** Immutable per-tick evaluation context passed cleanly to DAG nodes. */
    public static final class EvalContext {
        final int capacity;
        final double popSum;
        final long popLong;
        final double foodSum;
        final double foodPerCapita;
        final double waterSum;
        final double bioNatSum;
        final double bioAgrSum;
        final double bioLivSum;
        final double builtCapSum;
        final double energyCapturedMw;
        final double currentNonRenRes;
        final int landCellCount;
        final double landTempSum;
        final int populatedCount;
        final double urbanPop;
        final double weightedSoilOrg;
        final int arableCells;
        final float gini;
        final float gdp;
        final float lifeExp;
        final float fertility;
        final double agentBirthRate;
        final double agentDeathRate;
        final int polityCount;
        final int cityStatesCount;
        final double avgAsabiyyahPct;
        final double meanPolityInstab;
        final double meanEliteOverprodPolity;
        final long largestUnitSize;
        final int maxHierarchy;
        final long initialPop;
        final double t0Co2Ppm;
        final double elapsedYears;
        final double engineTps;

        private Double cachedPhiExergy;

        public EvalContext(int capacity, double popSum, long popLong, double foodSum, double foodPerCapita,
                    double waterSum, double bioNatSum, double bioAgrSum, double bioLivSum,
                    double builtCapSum, double energyCapturedMw, double currentNonRenRes,
                    int landCellCount, double landTempSum, int populatedCount, double urbanPop,
                    double weightedSoilOrg, int arableCells, float gini, float gdp,
                    float lifeExp, float fertility, double agentBirthRate, double agentDeathRate,
                    int polityCount, int cityStatesCount, double avgAsabiyyahPct, double meanPolityInstab,
                    double meanEliteOverprodPolity, long largestUnitSize, int maxHierarchy,
                    long initialPop, double t0Co2Ppm, double elapsedYears, double engineTps) {
            this.capacity = capacity;
            this.popSum = popSum;
            this.popLong = popLong;
            this.foodSum = foodSum;
            this.foodPerCapita = foodPerCapita;
            this.waterSum = waterSum;
            this.bioNatSum = bioNatSum;
            this.bioAgrSum = bioAgrSum;
            this.bioLivSum = bioLivSum;
            this.builtCapSum = builtCapSum;
            this.energyCapturedMw = energyCapturedMw;
            this.currentNonRenRes = currentNonRenRes;
            this.landCellCount = landCellCount;
            this.landTempSum = landTempSum;
            this.populatedCount = populatedCount;
            this.urbanPop = urbanPop;
            this.weightedSoilOrg = weightedSoilOrg;
            this.arableCells = arableCells;
            this.gini = gini;
            this.gdp = gdp;
            this.lifeExp = lifeExp;
            this.fertility = fertility;
            this.agentBirthRate = agentBirthRate;
            this.agentDeathRate = agentDeathRate;
            this.polityCount = polityCount;
            this.cityStatesCount = cityStatesCount;
            this.avgAsabiyyahPct = avgAsabiyyahPct;
            this.meanPolityInstab = meanPolityInstab;
            this.meanEliteOverprodPolity = meanEliteOverprodPolity;
            this.largestUnitSize = largestUnitSize;
            this.maxHierarchy = maxHierarchy;
            this.initialPop = initialPop;
            this.t0Co2Ppm = t0Co2Ppm;
            this.elapsedYears = elapsedYears;
            this.engineTps = engineTps;
        }

        double kpc() {
            return popSum > 0.0 ? builtCapSum / popSum : 0.0;
        }

        double powerWattsPerCapita() {
            return popSum > 0.0 ? Math.max(80.0, (energyCapturedMw * 1.0e6) / popSum) : 80.0;
        }

        double phiExergy() {
            if (cachedPhiExergy == null) {
                cachedPhiExergy = H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(
                        powerWattsPerCapita(), kpc(), 80.0);
            }
            return cachedPhiExergy;
        }
    }
}
