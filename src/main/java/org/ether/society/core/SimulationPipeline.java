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
 * @version 1.0.0-beta.2
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

        double dtYears = (double) dtSlow / 31_557_600.0;
        double dtMonthly = dtYears;

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
                long tickCount = engine != null ? engine.getTickCounter() : 1L;
                float dtY = (float) dtYears;
                float dtSec = (float) dtSlow;

                cachedNativeArena.syncFromJavaBuffers(worldBuffer, agentBuffer);
                cachedNativeArena.tickEnvironmental(dtY);
                cachedNativeArena.tickDemographic(demographicKernel.getTargetCohortSize(), dtY, sSeed);
                cachedNativeArena.tickUrban(dtY);
                cachedNativeArena.tickFlux(dtSec);
                cachedNativeArena.tickCulture(dtY, cultureKernel.getDiffusionRate(), cultureKernel.getForcingRate(), cultureKernel.getMutationRate(), sSeed, tickCount);
                cachedNativeArena.tickTier2(dtY);
                cachedNativeArena.syncToJavaBuffers(worldBuffer, agentBuffer);
                executedNative = true;
            } catch (Throwable t) {
                logger.warn("Native Rust execution encountered an issue, falling back to Java SIMD: {}", t.getMessage());
                executedNative = false;
            }
        }

        if (!executedNative) {
            environmentalKernel.tick(worldBuffer, dtSlow);
            if (agentBuffer != null) {
                demographicKernel.tick(worldBuffer, agentBuffer, dtSlow);
            }
            urbanKernel.tick(worldBuffer, dtSlow);
            fluxKernel.tick(worldBuffer, dtSlow);
            if (agentBuffer != null) {
                cultureKernel.tick(worldBuffer, agentBuffer, dtSlow);
            }
        }

        profiler.endPhase("3_DemographicsAndCulture");

        // --- Phase 2: Active Cliodynamic Sub-Systems (Data-Oriented DoD) ---
        profiler.beginPhase("4_ProceduralEngines");

        boolean runTier2 = perfConfig == null || perfConfig.isEnableTier2Cliodynamics();
        if (runTier2 && !executedNative && engine != null && engine.getWorldBuffer() != null) {
            typeBPassRunner.executeActiveTypeBEngines(engine.getWorldBuffer(), dtYears, dtMonthly, activeScenario, compiledKernel);
        }

        profiler.endPhase("4_ProceduralEngines");
    }
}
