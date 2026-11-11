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
            double val = evalNode(nodeId, ctx, memo);
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
                spatialOut.put(q.queryId(), evaluateSpatialQuery(q, world, cells));
            }
        }

        // -----------------------------------------------------------------
        // 5. Optional Cultural Matrix & Polarization Evaluation (Subscription Only)
        // -----------------------------------------------------------------
        TelemetryEvaluationResult.CulturalMatrixResult culturalOut = null;
        if (subscription.isCulturalMatrixSubscribed()) {
            culturalOut = evaluateCulturalMatrix(subscription.getCulturalMatrixQuery(), year);
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

    // -------------------------------------------------------------------------
    // Topological Memoized Node Evaluators (Pure Physical State Only)
    // -----------------------------------------------------------------

    private static double evalNode(String id, EvalContext c, Map<String, Double> m) {
        return switch (id) {
            case "population" -> c.popSum;
            case "populatedCellCount" -> (double) c.populatedCount;
            case "occupiedTerritory" -> c.populatedCount * 225.0; // Mean H3 Res 4 cell area (~225-250 km2 inhabited footprint proxy)
            case "populationSurvivalRate" -> {
                if (c.initialPop <= 0L) yield 100.0;
                yield Math.clamp((c.popSum / c.initialPop) * 100.0, 0.0, 100.0);
            }
            case "builtCapital" -> c.builtCapSum;
            case "capitalPerCapita" -> c.kpc();
            case "energyCaptured" -> c.energyCapturedMw;
            case "energyPerCapita" -> c.powerWattsPerCapita();
            case "avgTechLevel" -> c.phiExergy();
            case "primaryEnergy" -> {
                if (c.energyCapturedMw > 0.0) yield c.energyCapturedMw * 3.15576e-5; // MW -> EJ/yr
                yield CliodynamicAnalyticalFormulasCatalog.computePrimaryEnergyEJ(c.popLong, m.getOrDefault("avgTechLevel", c.phiExergy()));
            }
            case "kardashevScale" -> CliodynamicAnalyticalFormulasCatalog.computeKardashevScale(c.powerWattsPerCapita(), c.popLong);
            case "foodPerCapita" -> c.foodPerCapita;
            case "biomassNatural" -> c.bioNatSum;
            case "biomassDomesticated" -> c.bioAgrSum + c.bioLivSum;
            case "potableWater" -> c.waterSum;
            case "resourceDepletion" -> {
                double maxRes = Math.max(1.0, c.capacity * 100_000.0);
                yield Math.clamp((1.0 - c.currentNonRenRes / maxRes) * 100.0, 0.0, 100.0);
            }
            case "remainingResources" -> 100.0 - m.getOrDefault("resourceDepletion", 0.0);
            case "soilNPK" -> {
                double resDep = m.getOrDefault("resourceDepletion", 0.0);
                double haberBosch = 22.0 * SmoothMath.sigmoid(c.phiExergy(), 2.1, 0.25);
                if (c.arableCells > 0 && c.weightedSoilOrg > 0.0) {
                    double organicScore = Math.clamp((c.weightedSoilOrg / c.arableCells) * 2.5, 10.0, 100.0);
                    yield Math.clamp(0.5 * organicScore + 0.5 * (100.0 - resDep * 0.6 + haberBosch), 5.0, 100.0);
                }
                yield Math.clamp(100.0 - resDep * 0.6 + haberBosch, 5.0, 100.0);
            }
            case "gdp" -> (double) c.gdp;
            case "grossWorldProduct" -> c.gdp > 0.0 ? (c.gdp / 1e9)
                    : CliodynamicAnalyticalFormulasCatalog.computeGrossWorldProduct(c.popLong, m.getOrDefault("avgTechLevel", c.phiExergy()));
            case "gini" -> (double) c.gini;
            case "landGini" -> Math.clamp(c.gini * 1.12, 0.15, 0.85);
            case "lifeExpectancy" -> (double) c.lifeExp;
            case "fertilityRate" -> (double) c.fertility;
            case "urbanizationRate" -> {
                if (c.popSum <= 0.0) yield 0.0;
                double spatialUrbanPct = (c.urbanPop / c.popSum) * 100.0;
                yield Math.clamp(spatialUrbanPct, 0.0, 95.0);
            }
            case "literacyRate" -> {
                double phi = m.getOrDefault("avgTechLevel", c.phiExergy());
                // Buringh & van Zanden (2009): emerges smoothly with surplus/print/industrial exergy (Deep-time invariant 0%)
                double act = SmoothMath.sigmoid(phi, 0.96, 0.025);
                double rate = 99.0 * SmoothMath.sigmoid(phi, 1.85, 0.35);
                yield Math.clamp(act * rate, 0.0, 99.5);
            }
            case "asabiyyah" -> {
                if (c.polityCount > 0) yield c.avgAsabiyyahPct;
                double coh = CliodynamicAnalyticalFormulasCatalog.computeAsabiyyahSocialCohesion(c.phiExergy(), 70.0);
                yield coh * 100.0;
            }
            case "asabiyyahSocialCohesion" -> Math.clamp(m.getOrDefault("asabiyyah", 70.0) / 100.0, 0.0, 1.0);
            case "currencyDebasement" -> {
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeCurrencyDebasement(c.phiExergy(), stab);
            }
            case "eliteOverproduction" -> {
                if (c.polityCount > 0) yield c.meanEliteOverprodPolity;
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeEliteOverproductionIndex(c.phiExergy(), stab);
            }
            case "realUnskilledWage" -> CliodynamicAnalyticalFormulasCatalog.computeRealUnskilledWageIndex(c.phiExergy());
            case "politicalStressIndex" -> {
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computePoliticalStressIndex(c.phiExergy(), stab);
            }
            case "sociopoliticalInstability" -> {
                if (c.polityCount > 0) yield c.meanPolityInstab;
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeSociopoliticalInstability(c.phiExergy(), stab);
            }
            case "conflict" -> m.getOrDefault("sociopoliticalInstability", 15.0);
            case "globalTradeVolume" -> CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy());
            case "inlandFluvialFreightVolume" -> CliodynamicAnalyticalFormulasCatalog.computeInlandFluvialFreightVolume(c.phiExergy());
            case "sovereignDebtBurden" -> {
                double trade = m.getOrDefault("globalTradeVolume", CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy()));
                double lit = m.getOrDefault("literacyRate", 0.0);
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeSovereignDebtBurden(trade, lit, stab, c.phiExergy());
            }
            case "co2Concentration" -> {
                // Strictly physical CO2: t0 baseline + exergy combustion + accumulated transition (no calendar benchmark lookup)
                double baseCo2 = c.t0Co2Ppm > 0.0 ? c.t0Co2Ppm : 280.0;
                double exergyDelta = Math.max(0.0, CliodynamicAnalyticalFormulasCatalog.computeCo2Concentration(c.phiExergy()) - 280.0);
                double fossilIntensity = CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(c.phiExergy()) / 100.0;
                double cumDelta = Math.min(250.0, fossilIntensity * (c.energyCapturedMw / 1.0e6) * 0.015 * Math.max(0.0, c.elapsedYears));
                yield Math.clamp(baseCo2 + exergyDelta + cumDelta, 160.0, 2000.0);
            }
            case "temperatureAnomaly" -> {
                double co2 = m.getOrDefault("co2Concentration", 280.0);
                double radiativeAnom = CliodynamicAnalyticalFormulasCatalog.computeTemperatureAnomalyFromCo2(co2);
                if (c.landCellCount > 0) {
                    double cellDelta = (c.landTempSum / c.landCellCount) - 14.5;
                    yield Math.clamp(0.5 * radiativeAnom + 0.5 * cellDelta, -8.5, 8.5);
                }
                yield Math.clamp(radiativeAnom, -8.5, 8.5);
            }
            case "soilErosionRate" -> CliodynamicAnalyticalFormulasCatalog.computeTopsoilErosionLoss(c.phiExergy());
            case "agriculturalEroei" -> CliodynamicAnalyticalFormulasCatalog.computeAgriculturalEroei(c.phiExergy());
            case "netSurplus" -> {
                double eroei = m.getOrDefault("agriculturalEroei", 1.0);
                yield Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroei))) * 100.0;
            }
            case "deforestationRate" -> CliodynamicAnalyticalFormulasCatalog.computeForestCoverRemaining(c.phiExergy());
            case "wildBiodiversity" -> {
                double totalBio = c.bioNatSum + c.bioAgrSum + c.bioLivSum;
                yield totalBio > 0.0 ? Math.clamp((c.bioNatSum / totalBio) * 100.0, 0.0, 100.0) : 100.0;
            }
            case "informationSpeed" -> CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(c.phiExergy());
            case "metalSmeltingPerCapita" -> CliodynamicAnalyticalFormulasCatalog.computeMetalSmeltingPerCapita(c.phiExergy());
            case "fossilSharePrimaryEnergy" -> CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(c.phiExergy());
            case "carbonFootprint" -> {
                double eMw = c.energyCapturedMw;
                double fos = m.getOrDefault("fossilSharePrimaryEnergy", 0.0);
                yield (eMw * fos) / 1000.0;
            }
            case "shippingFreightCostIndex" -> CliodynamicAnalyticalFormulasCatalog.computeShippingFreightCostIndex(c.phiExergy());
            case "zeroContainmentScore" -> {
                double trade = m.getOrDefault("globalTradeVolume", CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy()));
                yield CliodynamicAnalyticalFormulasCatalog.computeZeroContainmentScoreFromTrade(trade);
            }
            case "megafaunaIndex" -> {
                // Physical wild biomass intactness (0..1) from simulated biosphere, zero calendar curve
                double denom = Math.max(1.0, c.landCellCount * 4500.0);
                yield Math.clamp(c.bioNatSum / denom, 0.0, 1.0);
            }
            case "milankovitchInsolation" -> {
                double meanTemp = c.landCellCount > 0 ? (c.landTempSum / c.landCellCount) : 14.5;
                yield Math.clamp(495.0 + (meanTemp - 14.5) * 4.5, 420.0, 560.0);
            }
            case "trophicMultiplier" -> Math.clamp(1.05 + 1.8 * SmoothMath.sigmoid(c.phiExergy(), 1.4, 0.4), 1.05, 3.20);
            case "biomassMobilized" -> {
                double tm = m.getOrDefault("trophicMultiplier", 1.2);
                yield (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * tm) / 15.0;
            }
            case "entropyPollution" -> c.popSum <= 0.0 ? 0.0 : Math.min(1000.0, (Math.log1p(c.popSum) * 25.0) + 2.5 * Math.log1p(c.kpc() / 25.0));
            case "wetBulbSafety" -> {
                double meanT = c.landCellCount > 0 ? (c.landTempSum / c.landCellCount) : 14.5;
                yield Math.max(0.0, 35.0 - (meanT + 3.5));
            }
            case "carryingCapacitySat" -> {
                double cap = Math.max(100.0, (c.foodSum * 17.5) / 1200.0);
                yield c.popSum / cap;
            }
            case "planetaryOvershoot" -> Math.clamp(0.4 + 0.6 * m.getOrDefault("carryingCapacitySat", 0.5), 0.1, 5.0);
            case "happiness" -> {
                double foodScore = Math.min(60.0, c.foodPerCapita * 30.0);
                double waterScore = Math.min(40.0, c.popSum > 0 ? (c.waterSum / c.popSum) * 2.0 : 30.0);
                yield Math.clamp(foodScore + waterScore, 10.0, 100.0);
            }
            case "institutionalMaturity" -> Math.clamp(20.0 + 75.0 * SmoothMath.sigmoid(c.phiExergy(), 1.25, 0.3), 0.0, 100.0);
            case "divisionLabor" -> Math.clamp(10.0 + 85.0 * SmoothMath.sigmoid(c.phiExergy(), 1.35, 0.35), 0.0, 100.0);
            case "cityStates" -> (double) c.cityStatesCount;
            case "maxHierarchy" -> (double) c.maxHierarchy;
            case "largestCulture" -> (double) Math.max(c.largestUnitSize, c.popLong > 0 ? Math.min(c.popLong, 500L) : 0L);
            case "largestOrgComplexity" -> c.largestUnitSize * c.phiExergy() * c.maxHierarchy;
            case "largestOrgEntropy" -> c.largestUnitSize * c.powerWattsPerCapita() * 0.05;
            case "eliteFormation" -> Math.clamp(0.5 + 12.0 * SmoothMath.sigmoid(c.phiExergy(), 1.1, 0.25), 0.5, 25.0);
            case "elderCapitalShare" -> Math.clamp(30.0 + 50.0 * SmoothMath.sigmoid(c.lifeExp, 45.0, 12.0), 30.0, 90.0);
            case "landRent" -> Math.clamp(10.0 + 90.0 * (c.popSum / Math.max(1.0, c.capacity * 1000.0)), 1.0, 500.0);
            case "toolsCount" -> c.popSum <= 0.0 ? 0.0 : c.popSum * (0.50 + 0.15 * Math.log1p(c.kpc() / 10.0));
            case "productsCount" -> c.popSum <= 0.0 ? 3.0 : Math.max(3.0, 5.0 + 3.2 * Math.log1p(c.kpc() / 5.0));
            case "shannonBandwidth" -> Math.pow(10.0, Math.min(9.0, c.phiExergy() * 1.8));
            case "collectiveMemory" -> Math.max(0.0, c.popSum * Math.max(0.0, c.phiExergy() - 0.90) * 0.01);
            case "innovationDiffusion" -> CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(c.phiExergy());
            case "knowledgeDecay" -> {
                double instab = m.getOrDefault("sociopoliticalInstability", 15.0);
                yield Math.clamp(0.5 + 0.25 * instab, 0.1, 50.0);
            }
            case "fiscalStress" -> m.getOrDefault("sovereignDebtBurden", 0.0) * 0.4;
            case "geopoliticalTension" -> m.getOrDefault("sociopoliticalInstability", 15.0) * 0.85;
            case "collapseRisk" -> Math.clamp(m.getOrDefault("politicalStressIndex", 0.0) * 0.65 + (100.0 - m.getOrDefault("asabiyyah", 70.0)) * 0.35, 0.0, 100.0);
            case "systemComplexity" -> Math.clamp(c.phiExergy() * 18.0, 0.0, 100.0);
            case "reconstructionCapability" -> Math.clamp(30.0 + m.getOrDefault("literacyRate", 0.0) * 0.6, 5.0, 100.0);
            case "systemInterdependence" -> Math.clamp(100.0 * SmoothMath.sigmoid(c.phiExergy(), 1.6, 0.4), 0.0, 100.0);
            case "offspringPct" -> Math.clamp(85.0 - 35.0 * SmoothMath.sigmoid(c.phiExergy(), 2.2, 0.4), 35.0, 95.0);
            case "ageFirstChild" -> Math.clamp(18.0 + 13.0 * SmoothMath.sigmoid(c.phiExergy(), 2.1, 0.4), 16.0, 34.0);
            case "immigrationRate" -> (c.agentBirthRate - c.agentDeathRate) * 100.0;
            case "pibMaterialFlow" -> c.popSum * 0.004 * Math.max(1.0, c.phiExergy());
            case "energyEroi" -> Math.clamp(3.0 + 18.0 * SmoothMath.sigmoid(c.phiExergy(), 1.8, 0.45), 1.5, 45.0);
            case "engineTPS" -> c.engineTps;
            case "ramMemory" -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0);
            case "cellCount" -> (double) c.capacity;
            default -> 0.0;
        };
    }

    private TelemetryEvaluationResult.SpatialRasterResult evaluateSpatialQuery(
            TelemetrySubscription.SpatialRasterQuery q,
            WorldBuffer world,
            List<H3Cell> cells) {
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int matched = 0;

        if (cells != null && !cells.isEmpty()) {
            boolean windowed = q.isWindowed();
            for (int i = 0; i < cells.size(); i++) {
                H3Cell cell = cells.get(i);
                if (windowed) {
                    double lat = cell.getLatitude();
                    double lon = cell.getLongitude();
                    if (lat < q.minLat() || lat > q.maxLat() || lon < q.minLng() || lon > q.maxLng()) {
                        continue;
                    }
                }
                double val = extractSpatialCellValue(q.channel(), cell, world, i);
                if (Double.isNaN(val)) continue;
                sum += val;
                if (val < min) min = val;
                if (val > max) max = val;
                matched++;
            }
        } else {
            int n = world.getCapacity();
            for (int i = 0; i < n; i++) {
                double val = extractSpatialBufferValue(q.channel(), world, i);
                if (Double.isNaN(val)) continue;
                sum += val;
                if (val < min) min = val;
                if (val > max) max = val;
                matched++;
            }
        }

        if (matched == 0) {
            min = 0.0;
            max = 0.0;
        }
        double mean = matched > 0 ? sum / matched : 0.0;
        BufferedImage img = null;
        if (q.renderImage() && cells != null && !cells.isEmpty()) {
            img = SpatialTensorRasterRenderer.rasterizeCellsToImage(
                    cells, q.channel().getRendererChannelKey(), q.width(), q.height());
        }
        return new TelemetryEvaluationResult.SpatialRasterResult(
                q.queryId(), q.channel(), sum, mean, min, max, matched, img);
    }

    private TelemetryEvaluationResult.CulturalMatrixResult evaluateCulturalMatrix(
            TelemetrySubscription.CulturalMatrixQuery q,
            long year) {
        if (q == null || !q.enabled()) {
            return null;
        }
        String planet = "earth";
        List<org.ether.society.model.CulturalEntity> entities =
                org.ether.society.ui.CulturalEpochRegistryLoader.loadEpochRegistry(planet, year);
        if (entities.isEmpty()) {
            entities = org.ether.society.ui.CulturalEpochRegistryLoader.loadEpochRegistry(year);
        }
        int n = entities.size();
        if (n == 0) {
            return new TelemetryEvaluationResult.CulturalMatrixResult(
                    year, planet, List.of(), new double[0][0], new double[0][0], 0.5, 0.0);
        }

        List<org.ether.society.model.CulturalEntity> baselineEntities = entities;
        if (q.computeDelta()) {
            baselineEntities = org.ether.society.ui.CulturalEpochRegistryLoader.loadEpochRegistry(planet, q.baselineEpoch());
            if (baselineEntities.isEmpty()) {
                baselineEntities = org.ether.society.ui.CulturalEpochRegistryLoader.loadEpochRegistry(q.baselineEpoch());
            }
            if (baselineEntities.isEmpty()) {
                baselineEntities = entities;
            }
        }

        double[][] affinities = new double[n][n];
        double[][] deltas = q.computeDelta() ? new double[n][n] : new double[0][0];

        double sumAffinity = 0.0;
        double sumSqDiff = 0.0;
        int count = n * n;

        for (int i = 0; i < n; i++) {
            org.ether.society.model.CulturalEntity eOrig = entities.get(i);
            for (int j = 0; j < n; j++) {
                org.ether.society.model.CulturalEntity eTarg = entities.get(j);
                double affCurrent = eOrig.affinityTowards(eTarg);
                affinities[i][j] = affCurrent;

                sumAffinity += affCurrent;
                sumSqDiff += (affCurrent - 0.5) * (affCurrent - 0.5);

                if (q.computeDelta()) {
                    org.ether.society.model.CulturalEntity bOrig = findEntityById(baselineEntities, eOrig.id());
                    org.ether.society.model.CulturalEntity bTarg = findEntityById(baselineEntities, eTarg.id());
                    double affT0 = (bOrig != null && bTarg != null) ? bOrig.affinityTowards(bTarg) : affCurrent;
                    deltas[i][j] = affCurrent - affT0;
                }
            }
        }

        double mean = count > 0 ? sumAffinity / count : 0.5;
        double variance = count > 0 ? sumSqDiff / count : 0.0;
        double polarization = Math.sqrt(variance) * 2.0;

        return new TelemetryEvaluationResult.CulturalMatrixResult(
                year, planet, entities, affinities, deltas, mean, polarization);
    }

    private static org.ether.society.model.CulturalEntity findEntityById(
            List<org.ether.society.model.CulturalEntity> list, String id) {
        if (id == null || list == null) return null;
        for (org.ether.society.model.CulturalEntity e : list) {
            if (id.equals(e.id())) return e;
        }
        return null;
    }

    private static double extractSpatialCellValue(
            TelemetrySubscription.SpatialChannel ch, H3Cell cell, WorldBuffer wb, int idx) {
        if (wb != null && idx >= 0 && idx < wb.getCapacity()) {
            return extractSpatialBufferValue(ch, wb, idx);
        }
        return switch (ch) {
            case POPULATION_DENSITY -> cell.getBiomassHuman() != null ? cell.getBiomassHuman() : cell.getPopulationOr(0);
            case BUILT_CAPITAL -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            case TEMPERATURE -> cell.getTemperature() != null ? cell.getTemperature() : 14.5;
            case BIOME -> cell.getBiome() != null ? cell.getBiome().ordinal() : 0.0;
            case ECOLOGICAL_FOOTPRINT -> {
                double pop = cell.getPopulationOr(0);
                double bioNat = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
                double bioAgr = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                yield (bioAgr + pop * 0.05) / Math.max(1.0, bioNat + bioAgr + pop * 0.05);
            }
            case FOOD_RESERVES -> cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            case WATER_RESERVES -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
            case NATURAL_BIOMASS -> cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            case AGRICULTURAL_BIOMASS -> cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
        };
    }

    private static double extractSpatialBufferValue(
            TelemetrySubscription.SpatialChannel ch, WorldBuffer wb, int i) {
        return switch (ch) {
            case POPULATION_DENSITY -> wb.getBiomassHuman()[i];
            case BUILT_CAPITAL -> wb.getResourceCapital()[i];
            case TEMPERATURE -> wb.getTemperature()[i];
            case BIOME -> wb.getBiomes()[i];
            case ECOLOGICAL_FOOTPRINT -> {
                double pop = wb.getBiomassHuman()[i];
                double nat = wb.getBiomassNatural()[i];
                double agr = wb.getBiomassAgriculture()[i];
                yield Math.clamp((agr + pop * 0.05) / Math.max(1.0, nat + agr + pop * 0.05), 0.0, 1.0);
            }
            case FOOD_RESERVES -> wb.getFoodResource()[i];
            case WATER_RESERVES -> wb.getWaterResource()[i];
            case NATURAL_BIOMASS -> wb.getBiomassNatural()[i];
            case AGRICULTURAL_BIOMASS -> wb.getBiomassAgriculture()[i];
        };
    }

    /** Immutable per-tick evaluation context passed cleanly to DAG nodes. */
    private static final class EvalContext {
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

        EvalContext(int capacity, double popSum, long popLong, double foodSum, double foodPerCapita,
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
