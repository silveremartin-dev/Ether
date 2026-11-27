/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.*;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.database.H3Cell;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.compiler.CompiledEngineKernel;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * High-Performance Declarative Simulation Pipeline.
 * Orchestrates deterministic phase transitions for physical, demographic, and cliodynamic sub-systems
 * using Data-Oriented Design (DOD) buffers and JIT-compiled scenario kernels.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SimulationPipeline {
    private static final Logger logger = LoggerFactory.getLogger(SimulationPipeline.class);

    private final DemographicKernel demographicKernel;
    private final UrbanKernel urbanKernel;
    private final CultureKernel cultureKernel;
    private final EnvironmentalKernel environmentalKernel;
    private final StatisticsKernel statisticsKernel;
    private final FluxKernel fluxKernel;

    private Scenario configuredScenario;
    private CompiledEngineKernel compiledKernel;
    private final TypeBEnginePassRunner typeBPassRunner = new TypeBEnginePassRunner();

    private org.ether.society.core.dod.NativeRustBridge.NativeWorldArena cachedNativeArena = null;
    private int cachedNativeArenaCapacity = -1;
    private int cachedNativeArenaAgentCapacity = -1;
    private float[] cachedPopBeforeEngines = null;
    private java.util.Map<Long, Integer> cachedH3ToSlot = null;
    private long cachedH3FirstIndex = 0L;
    private int cachedH3SlotCapacity = -1;

    public SimulationPipeline(DemographicKernel demographicKernel,
                              UrbanKernel urbanKernel,
                              CultureKernel cultureKernel,
                              EnvironmentalKernel environmentalKernel,
                              StatisticsKernel statisticsKernel) {
        this(demographicKernel, urbanKernel, cultureKernel, environmentalKernel, statisticsKernel, new FluxKernel());
    }

    public SimulationPipeline(DemographicKernel demographicKernel,
                              UrbanKernel urbanKernel,
                              CultureKernel cultureKernel,
                              EnvironmentalKernel environmentalKernel,
                              StatisticsKernel statisticsKernel,
                              FluxKernel fluxKernel) {
        this.demographicKernel = demographicKernel != null ? demographicKernel : new DemographicKernel();
        this.urbanKernel = urbanKernel != null ? urbanKernel : new UrbanKernel();
        this.cultureKernel = cultureKernel != null ? cultureKernel : new CultureKernel();
        this.environmentalKernel = environmentalKernel != null ? environmentalKernel : new EnvironmentalKernel();
        this.statisticsKernel = statisticsKernel != null ? statisticsKernel : new StatisticsKernel();
        this.fluxKernel = fluxKernel != null ? fluxKernel : new FluxKernel();
    }

    /**
     * Resets all mutable static simulation states, caches, and registers to guarantee strict inter-scenario isolation.
     */
    public static void resetGlobalSimulationState() {
        TypeBEnginePassRunner.resetGlobalSimulationState();
    }

    /**
     * Configures and JIT-compiles active Type B scenario theories into a single fused execution kernel.
     * Also binds all underlying physical and cliodynamic RNGs to the scenario seed.
     *
     * @param scenario the active simulation scenario
     */
    public void configureScenario(Scenario scenario) {
        this.configuredScenario = scenario;

        long sSeed = scenario != null && scenario.getSeed() != 0L ? scenario.getSeed() : 42L;
        if (this.cultureKernel != null) {
            this.cultureKernel.resetState();
            long cSeed = scenario != null && scenario.getCulturalSeed() != 0L ? scenario.getCulturalSeed() : sSeed;
            this.cultureKernel.setBaseSeed(cSeed);
        }
        if (this.demographicKernel != null) {
            this.demographicKernel.setBaseSeed(sSeed);
        }
        this.compiledKernel = typeBPassRunner.configureAndCompile(scenario, sSeed);
    }

    /**
     * Executes the complete deterministic cliodynamic pipeline for a single monthly simulation tick
     * directly from conserved physical and biological buffers, with zero synthetic technology input.
     */
    public void executeTick(H3SimulationEngine engine,
                            List<H3Cell> cells,
                            WorldBuffer worldBuffer,
                            AgentBuffer agentBuffer,
                            SimulationProfiler profiler,
                            SimulationPerformanceConfig perfConfig,
                            long dtSlow) {
        if (worldBuffer == null) return;

        Scenario activeScenario = engine != null ? engine.getCurrentScenario() : null;
        if (activeScenario != configuredScenario) {
            configureScenario(activeScenario);
        }

        if (engine != null && engine.getTimeManager() != null) {
            worldBuffer.setCurrentYear(engine.getCurrentYear());
        } else if (activeScenario != null) {
            worldBuffer.setCurrentYear(activeScenario.getStartDateYear());
        }
        worldBuffer.setSimulationTick(engine != null ? engine.getTickCounter() : 0L);
        worldBuffer.setDemographicKernelManaged(agentBuffer != null);
        worldBuffer.setHostileEnvironment(activeScenario != null
                && org.ether.society.engines.tier1.PressurizedHabitatEngine.isHostileEnvironment(activeScenario.getPlanetPreset()));

        SimulationTimeStep timeStep = SimulationTimeStep.ofSeconds(dtSlow);
        double dtYears = timeStep.dtYears();
        double dtMonthly = timeStep.dtMonths();

        // --- Phase 1: Demographics, Urbanization & Culture DOD Kernels ---
        profiler.beginPhase("3_DemographicsAndCulture");
        boolean executedNative = false;
        if (perfConfig != null && perfConfig.getExecutionEngineMode() == org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE
                && org.ether.society.core.dod.NativeRustBridge.isNativeAvailable() && agentBuffer != null) {
            try {
                int cap = worldBuffer.getCapacity();
                int aCap = agentBuffer.getCapacity();
                if (cachedNativeArena == null || cachedNativeArenaCapacity != cap || cachedNativeArenaAgentCapacity != aCap) {
                    if (cachedNativeArena != null) {
                        cachedNativeArena.close();
                    }
                    cachedNativeArena = new org.ether.society.core.dod.NativeRustBridge.NativeWorldArena(cap, aCap);
                    cachedNativeArenaAgentCapacity = aCap;
                    cachedNativeArenaCapacity = cap;
                }
                long sSeed = activeScenario != null && activeScenario.getSeed() != 0L ? activeScenario.getSeed() : 42L;
                long cSeed = cultureKernel != null ? cultureKernel.getBaseSeed() : sSeed;
                long tickCount = engine != null ? engine.getTickCounter() : 1L;
                float dtY = timeStep.dtYearsF();
                float dtSec = timeStep.dtSecondsF();

                cachedNativeArena.syncFromJavaBuffers(worldBuffer, agentBuffer);
                cachedNativeArena.tickEnvironmental(dtY);
                cachedNativeArena.tickDemographic(demographicKernel.getTargetCohortSize(), dtY, sSeed);
                cachedNativeArena.tickUrban(dtY);
                cachedNativeArena.tickFlux(dtSec);
                cachedNativeArena.tickCulture(dtY, cultureKernel.getDiffusionRate(), cultureKernel.getForcingRate(), cultureKernel.getMutationRate(), cSeed, tickCount);
                cachedNativeArena.syncToJavaBuffers(worldBuffer, agentBuffer);
                executedNative = true;
            } catch (Throwable t) {
                logger.warn("Native Rust execution encountered an issue, falling back to Java SIMD: {}", t.getMessage());
                executedNative = false;
            }
        }

        if (!executedNative) {
            environmentalKernel.tick(worldBuffer, 0, timeStep);
            if (agentBuffer != null) {
                demographicKernel.tick(worldBuffer, agentBuffer, timeStep);
            }
            urbanKernel.tick(worldBuffer, timeStep);
            fluxKernel.tick(worldBuffer, timeStep);
            if (agentBuffer != null) {
                cultureKernel.tick(worldBuffer, agentBuffer, timeStep);
            }
        }

        profiler.endPhase("3_DemographicsAndCulture");

        // Snapshot living human population prior to Tier 1 & Tier 2 passes so any exogenous/cliodynamic
        // demographic changes (SEIR epidemics, wet-bulb lethality, warfare, SDT crises) are closed onto AgentBuffer.mass
        int wbCap = worldBuffer.getCapacity();
        if (agentBuffer != null && worldBuffer.getHumanPopulation() != null) {
            if (cachedPopBeforeEngines == null || cachedPopBeforeEngines.length < wbCap) {
                cachedPopBeforeEngines = new float[wbCap];
            }
            System.arraycopy(worldBuffer.getHumanPopulation(), 0, cachedPopBeforeEngines, 0, wbCap);
        }

        // --- Phase 1b: Essential Tier 1 Biophysical Engines (Thermodynamics, Degradation, Pollution, Metallurgy) ---
        profiler.beginPhase("3b_Tier1Biophysical");
        typeBPassRunner.executeActiveTier1Engines(worldBuffer, dtYears, dtMonthly, activeScenario);
        profiler.endPhase("3b_Tier1Biophysical");

        // --- Phase 2: Active Cliodynamic Sub-Systems (Data-Oriented DoD) ---
        profiler.beginPhase("4_ProceduralEngines");

        boolean runTier2 = perfConfig == null || perfConfig.isEnableTier2Cliodynamics();
        if (runTier2) {
            WorldBuffer wb = (engine != null && engine.getWorldBuffer() != null) ? engine.getWorldBuffer() : worldBuffer;
            typeBPassRunner.executeActiveTypeBEngines(wb, dtYears, dtMonthly, activeScenario, compiledKernel);

            if (engine != null && engine.getNations() != null && !engine.getNations().isEmpty()) {
                long[] h3Idx = wb.getH3Indexes();
                long firstH3 = (h3Idx != null && h3Idx.length > 0) ? h3Idx[0] : 0L;
                if (cachedH3ToSlot == null || cachedH3SlotCapacity != wb.getCapacity() || cachedH3FirstIndex != firstH3) {
                    cachedH3ToSlot = new java.util.HashMap<>(wb.getCapacity());
                    if (h3Idx != null) {
                        for (int s = 0; s < wb.getCapacity() && s < h3Idx.length; s++) {
                            if (h3Idx[s] != 0L) cachedH3ToSlot.put(h3Idx[s], s);
                        }
                    }
                    cachedH3SlotCapacity = wb.getCapacity();
                    cachedH3FirstIndex = firstH3;
                }
                for (org.ether.society.database.Polity p : engine.getNations()) {
                    if (p != null) p.syncBufferIndices(cachedH3ToSlot);
                }
                java.util.Map<String, Boolean> states = activeScenario != null ? activeScenario.getTypeBEngineStates() : null;
                if (states == null || Boolean.TRUE.equals(states.get("CliodynamicsEngine"))) {
                    org.ether.society.engines.tier2.theories.CliodynamicsEngine.updateCliodynamics(engine.getNations(), wb, dtYears);
                }
                if (states == null || Boolean.TRUE.equals(states.get("WarDiplomacyEngine"))) {
                    if (cells != null && !cells.isEmpty()) {
                        org.ether.society.engines.tier2.theories.WarDiplomacyEngine.processGeopoliticalConflicts(
                                engine.getNations(), cells, wb, cachedH3ToSlot, activeScenario);
                    }
                }
                if (states == null || Boolean.TRUE.equals(states.get("SovereignTreasuryBondEngine"))) {
                    org.ether.society.engines.tier2.theories.SovereignTreasuryBondEngine.processPolityFiscalCycles(engine.getNations(), wb, dtYears);
                }
            }
        }

        // Enforce strict demographic mass conservation closure between WorldBuffer.humanPopulation and AgentBuffer.mass
        if (agentBuffer != null && cachedPopBeforeEngines != null && worldBuffer.getHumanPopulation() != null) {
            H3BufferSyncHelper.reconcileEngineDemographicDeltas(worldBuffer, agentBuffer, cachedPopBeforeEngines, wbCap);
        }
        org.ether.society.core.dod.InvariantConservationGuard.enforcePositivityAndBounds(worldBuffer);

        profiler.endPhase("4_ProceduralEngines");
    }
}
