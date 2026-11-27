/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.core.engine.SimulationEngineRegistry;

import org.ether.society.engines.compiler.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates JIT compilation, global state reset, and per-tick execution of pluggable
 * Tier 2 cliodynamic, macroeconomic, and historical scenario engines for {@link SimulationPipeline}.
 *
 * <p>Delegates engine execution dynamically to {@link SimulationEngineRegistry}, completely
 * eliminating hardcoded engine execution chains and manual reset lists.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see SimulationPipeline
 * @see SimulationEngineRegistry
 */
public final class TypeBEnginePassRunner {

    private static final Logger logger = LoggerFactory.getLogger(TypeBEnginePassRunner.class);

    /**
     * Resets all mutable simulation states, caches, and registries across all registered engines.
     */
    public static void resetGlobalSimulationState() {
        SimulationEngineRegistry.getInstance().resetAll();
    }

    /**
     * Anchors deterministic seeds on stateful Tier 2 engines and compiles active symbolic Type B
     * theories into a fused {@link CompiledEngineKernel}.
     *
     * @param scenario active scenario configuration
     * @param seed     deterministic base seed
     * @return compiled JIT kernel, or {@code null} if no symbolic theories are active
     */
    public CompiledEngineKernel configureAndCompile(Scenario scenario, long seed) {
        SimulationEngineModule sde = SimulationEngineRegistry.getInstance().getEngine("StructuralDemographicBifurcationEngine");
        if (sde instanceof StructuralDemographicBifurcationEngine sdb) {
            sdb.setSeed(seed);
        }
        NuclearSafetyRadiotoxicityEngine.setBaseSeed(seed);
        NuclearWarfareClimateEngine.setBaseSeed(seed);
        LanguageLinguisticEngine.setBaseSeed(seed);

        if (scenario != null && scenario.getTypeBEngineStates() != null) {
            EdoJapanIsolationEngine.setSakokuIsolationActive(
                    Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("EdoJapanIsolationEngine")));
        } else {
            EdoJapanIsolationEngine.setSakokuIsolationActive(false);
        }

        if (scenario == null || scenario.getTypeBEngineStates() == null || scenario.getTypeBEngineStates().isEmpty()) {
            return null;
        }

        List<SimulationEngineModule> activeTier2 = SimulationEngineRegistry.getInstance().getActiveTier2Engines(scenario);
        if (activeTier2.isEmpty()) {
            return null;
        }

        logger.info("Fused Tier 2 Cliodynamic Kernel active for '{}' with {} enabled theories.",
                scenario.getName(), activeTier2.size());
        return new CompiledEngineKernel();
    }

    /**
     * Executes active Tier 2 cliodynamic theories directly over the Data-Oriented WorldBuffer
     * using Single-Pass Cellular Kernel Fusion for maximal CPU cache locality.
     *
     * @param worldBuffer    contiguous world buffer
     * @param dtYears        time step in Julian years
     * @param dtMonthly      monthly-scaled year fraction
     * @param scenario       active scenario configuration
     * @param compiledKernel optional JIT-compiled symbolic kernel
     */
    public void executeActiveTypeBEngines(WorldBuffer worldBuffer,
                                          double dtYears,
                                          double dtMonthly,
                                          Scenario scenario,
                                          CompiledEngineKernel compiledKernel) {
        executeActiveEngines(org.ether.society.core.engine.EngineTier.TIER_2_CLIODYNAMIC,
                worldBuffer, dtYears, dtMonthly, scenario);
    }

    /**
     * Executes active Tier 2 cliodynamic theories directly over the Data-Oriented WorldBuffer
     * using Single-Pass Cellular Kernel Fusion for maximal CPU cache locality.
     *
     * @param worldBuffer    contiguous world buffer
     * @param cells          unused legacy parameter retained for signature compatibility
     * @param dtYears        time step in Julian years
     * @param dtMonthly      monthly-scaled year fraction
     * @param scenario       active scenario configuration
     * @param compiledKernel optional JIT-compiled symbolic kernel
     */
    public void executeActiveTypeBEngines(WorldBuffer worldBuffer,
                                          List<org.ether.society.database.H3Cell> cells,
                                          double dtYears,
                                          double dtMonthly,
                                          Scenario scenario,
                                          CompiledEngineKernel compiledKernel) {
        executeActiveEngines(org.ether.society.core.engine.EngineTier.TIER_2_CLIODYNAMIC,
                worldBuffer, dtYears, dtMonthly, scenario);
    }

    /**
     * Executes active Tier 1 biophysical engines over the Data-Oriented {@link WorldBuffer}
     * using the unified Single-Pass Cellular Kernel Fusion + whole-buffer pipeline.
     *
     * @param worldBuffer contiguous world memory buffer
     * @param dtYears     time step in Julian years
     * @param dtMonthly   monthly-scaled year fraction
     * @param scenario    active scenario configuration (may be {@code null})
     */
    public void executeActiveTier1Engines(WorldBuffer worldBuffer,
                                          double dtYears,
                                          double dtMonthly,
                                          Scenario scenario) {
        executeActiveEngines(org.ether.society.core.engine.EngineTier.TIER_1_BIOPHYSICAL,
                worldBuffer, dtYears, dtMonthly, scenario);
    }

    /**
     * Unified execution pipeline for all simulation engines of a given {@link org.ether.society.core.engine.EngineTier}.
     * Both Tier 1 biophysical engines and Tier 2 cliodynamic engines share this exact same
     * Single-Pass Cellular Kernel Fusion + whole-buffer stencil execution path, differing only in
     * their {@code EngineTier} metadata and default enablement policy.
     *
     * @param tier        engine tier to execute
     * @param worldBuffer contiguous world memory buffer
     * @param dtYears     time step in Julian years
     * @param dtMonthly   monthly-scaled year fraction
     * @param scenario    active scenario configuration (may be {@code null})
     */
    public void executeActiveEngines(org.ether.society.core.engine.EngineTier tier,
                                     WorldBuffer worldBuffer,
                                     double dtYears,
                                     double dtMonthly,
                                     Scenario scenario) {
        if (worldBuffer == null || tier == null) return;

        List<SimulationEngineModule> activeEngines = SimulationEngineRegistry.getInstance().getActiveEngines(tier, scenario);
        if (activeEngines.isEmpty()) return;

        // Partition active engines: fused cellular derivative engines vs whole-buffer / stencil engines
        List<SimulationEngineModule> cellularEngines = new java.util.ArrayList<>();
        List<SimulationEngineModule> bufferEngines = new java.util.ArrayList<>();

        for (SimulationEngineModule engine : activeEngines) {
            if (engine.supportsCellularDerivatives()) {
                cellularEngines.add(engine);
            } else {
                bufferEngines.add(engine);
            }
        }

        // --- SINGLE-PASS KERNEL FUSION FOR CELLULAR DERIVATIVES ---
        if (!cellularEngines.isEmpty()) {
            int capacity = worldBuffer.getCapacity();
            for (SimulationEngineModule e : cellularEngines) {
                e.prepareTick(worldBuffer, dtYears);
                if (e instanceof KurzweilAcceleratingReturnsEngine k) {
                    k.prepareTick(dtYears);
                }
            }

            DerivativeAccumulator acc = new DerivativeAccumulator();
            int engineCount = cellularEngines.size();

            /** Specific embodied energy of built physical capital (15 MJ/kg = 0.015 GJ/kg; Smil 2017, Krausmann et al. 2017). */
            final double embodiedEnergyGjPerKg = org.ether.society.model.PhysicalUnitsHelper.megajoulesToGigajoules(15.0);
            /** Marginal exergy investment ceiling for pluggable Tier 2 theories above Phase 1 UrbanKernel base turnover (~13% base + <=12% marginal <= 25% total). */
            final double maxMarginalExergyInvestmentShare = 0.12;
            final boolean isTier2 = (tier == org.ether.society.core.engine.EngineTier.TIER_2_CLIODYNAMIC);

            for (int i = 0; i < capacity; i++) {
                acc.clear();
                double maxPosCap = 0.0;
                double minNegCap = 0.0;
                for (int e = 0; e < engineCount; e++) {
                    float beforeCap = acc.dResourceCapital;
                    cellularEngines.get(e).accumulateCellularDerivatives(worldBuffer, i, dtYears, dtMonthly, acc);
                    if (isTier2) {
                        double contrib = acc.dResourceCapital - beforeCap;
                        if (contrib > maxPosCap) {
                            maxPosCap = contrib;
                        } else if (contrib < minNegCap) {
                            minNegCap = contrib;
                        }
                    }
                }
                // Lavoisier mass & Carnot exergy conservation closure (Smil 2017; Garrett 2011; Krausmann et al. 2017):
                // 1) Tier 2 theories act strictly as marginal cliodynamic modulations above Phase 1 UrbanKernel's Solow-Swan base
                //    turnover, combined by upper/lower envelope (maxPosCap + minNegCap) even when a single Tier 2 engine runs.
                // 2) Marginal positive capital formation dK+ [kg] is strictly bounded by remaining net exergy surplus (after
                //    deducting somatic basal demand, Garrett maintenance, and Phase 1 base Solow replacement) and debits raw stocks.
                if (isTier2 && (maxPosCap > 0.0 || minNegCap < 0.0)) {
                    double consensusPosK = maxPosCap;
                    double consensusNegK = minNegCap;
                    double pop = worldBuffer.getHumanPopulation()[i];
                    double curCap = worldBuffer.getResourceCapital()[i];

                    // Available annual energy flux in GJ/yr: max of explicit energy buffers and Smil (2017) prime-mover cell power
                    double cellEnergyGjYr = worldBuffer.getEnergyFire()[i] + worldBuffer.getEnergyWind()[i]
                            + worldBuffer.getEnergySolar()[i] + worldBuffer.getEnergyFoodConsumed()[i];
                    if (pop > 0.0) {
                        double cellWatts = org.ether.society.core.dod.WorldBufferComponentAdapter.computeCellPowerWattsPerCapita(worldBuffer, i) * pop;
                        cellEnergyGjYr = Math.max(cellEnergyGjYr, org.ether.society.model.PhysicalUnitsHelper.wattsToGjPerYear(cellWatts));
                    }
                    double basalGjYr = pop * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
                    double maintGjYr = org.ether.society.model.PhysicalUnitsHelper.wattsToGjPerYear(
                            curCap * org.ether.society.database.H3CellPhysicsAndStateHelper.GARRETT_CAPITAL_MAINTENANCE_WATTS_PER_UNIT);
                    double phase1ReplacementGjYr = (curCap * 0.04) * embodiedEnergyGjPerKg;
                    double netExergySurplusGj = Math.max(0.0, cellEnergyGjYr - basalGjYr - maintGjYr - phase1ReplacementGjYr)
                            * Math.max(0.0001, dtYears);
                    double carnotMaxDeltaKg = (maxMarginalExergyInvestmentShare * netExergySurplusGj) / embodiedEnergyGjPerKg;

                    // Krausmann et al. (2017, PNAS 114:1880) physical capital material stoichiometry:
                    // Built capital mass comprises ~5% metals, ~7% timber, and ~88% mineral aggregates (stone, concrete,
                    // brick, sand, gravel, clay). Continental land cells (elevation >= 0) supply local crustal aggregates
                    // (stone/sand/limestone) not stored in a separate buffer, while wood/biomass, metal, and clay stocks
                    // are explicitly debited proportionally to their mobilized shares under Lavoisier mass conservation.
                    double woodT = Math.max(0.0, worldBuffer.getWoodResource()[i]);
                    double bioNatT = worldBuffer.getBiomassNatural() != null ? Math.max(0.0, worldBuffer.getBiomassNatural()[i]) : 0.0;
                    double metalT = Math.max(0.0, worldBuffer.getMetalResource()[i]);
                    double clayT = worldBuffer.getClayResource() != null ? Math.max(0.0, worldBuffer.getClayResource()[i]) : 0.0;
                    boolean isLand = worldBuffer.getElevation() != null && worldBuffer.getElevation()[i] >= 0.0f;

                    double actualPosK = Math.min(consensusPosK, carnotMaxDeltaKg);
                    if (!isLand) {
                        // Open ocean cells have no crustal stone/aggregates: strictly bounded by explicit stocks
                        double explicitRawTonnes = woodT + metalT + clayT;
                        double lavoisierOceanMaxKg = org.ether.society.model.PhysicalUnitsHelper.metricTonnesToKg(
                                explicitRawTonnes * 0.05 * Math.max(0.0001, dtYears));
                        actualPosK = Math.min(actualPosK, lavoisierOceanMaxKg);
                    }
                    double consumedTonnes = org.ether.society.model.PhysicalUnitsHelper.kgToMetricTonnes(actualPosK);
                    if (consumedTonnes > 0.0) {
                        double dtClamped = Math.min(1.0, Math.max(0.0001, dtYears));
                        // 1) Metal share (~5% of built capital mass) debited from metalResource
                        double metalDebit = Math.min(metalT * 0.05 * dtClamped, consumedTonnes * 0.05);
                        if (metalDebit > 0.0) {
                            acc.dMetalResource -= (float) metalDebit;
                        }
                        // 2) Timber share (~7% of built capital mass) debited from woodResource, then standing forest biomass
                        double timberNeed = consumedTonnes * 0.07;
                        double woodDebit = Math.min(woodT * 0.05 * dtClamped, timberNeed);
                        if (woodDebit > 0.0) {
                            acc.dWoodResource -= (float) woodDebit;
                        }
                        double forestDebit = Math.min(bioNatT * 0.02 * dtClamped, Math.max(0.0, timberNeed - woodDebit));
                        if (forestDebit > 0.0) {
                            acc.dBiomassNatural -= (float) forestDebit;
                        }
                        // 3) Clay/brick share (~10% of built capital mass) debited from clayResource (remaining ~78% is crustal stone/sand/concrete on land)
                        double clayDebit = Math.min(clayT * 0.05 * dtClamped, consumedTonnes * 0.10);
                        if (clayDebit > 0.0 && worldBuffer.getClayResource() != null) {
                            worldBuffer.getClayResource()[i] = (float) Math.max(0.0, worldBuffer.getClayResource()[i] - clayDebit);
                        }
                    }
                    acc.dResourceCapital = (float) (actualPosK + consensusNegK);
                }
                if (acc.hasNonZeroDerivatives()) {
                    org.ether.society.core.engine.AbstractSimulationEngine.applyAccumulatorToCell(worldBuffer, i, acc);
                }
            }
        }

        // --- WHOLE BUFFER / STENCIL PASSES (DIFFUSION, WATER GRID, ETC.) ---
        for (SimulationEngineModule engine : bufferEngines) {
            engine.tick(worldBuffer, dtYears, dtMonthly);
        }
    }
}
