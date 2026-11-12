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
 * @version 1.0.0-beta.2
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
        executeActiveTypeBEngines(worldBuffer, null, dtYears, dtMonthly, scenario, compiledKernel);
    }

    /**
     * Executes active Tier 2 cliodynamic theories directly over the Data-Oriented WorldBuffer
     * and active cells using Single-Pass Cellular Kernel Fusion for maximal CPU cache locality.
     *
     * @param worldBuffer    contiguous world buffer
     * @param cells          optional H3 cell list for legacy or hybrid plugin passes
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
        if (worldBuffer == null) return;

        List<SimulationEngineModule> activeTier2 = SimulationEngineRegistry.getInstance().getActiveTier2Engines(scenario);
        if (activeTier2.isEmpty()) return;

        // Partition active engines: fused cellular derivative engines vs whole-buffer / stencil engines
        List<SimulationEngineModule> cellularEngines = new java.util.ArrayList<>();
        List<SimulationEngineModule> bufferEngines = new java.util.ArrayList<>();

        for (SimulationEngineModule engine : activeTier2) {
            if (engine.supportsCellularDerivatives()) {
                cellularEngines.add(engine);
            } else {
                bufferEngines.add(engine);
            }
        }

        // --- SINGLE-PASS KERNEL FUSION FOR CELLULAR DERIVATIVES ---
        if (!cellularEngines.isEmpty()) {
            int capacity = worldBuffer.getCapacity();
            // Pre-tick preparation for global parameters / thresholds
            for (SimulationEngineModule e : cellularEngines) {
                if (e instanceof TasmanianCulturalRegressionEngine t) {
                    t.prepareTick(worldBuffer, dtYears);
                } else if (e instanceof KurzweilAcceleratingReturnsEngine k) {
                    k.prepareTick(dtYears);
                }
            }

            DerivativeAccumulator acc = new org.ether.society.core.engine.DerivativeAccumulator();
            int engineCount = cellularEngines.size();

            for (int i = 0; i < capacity; i++) {
                acc.clear();
                for (int e = 0; e < engineCount; e++) {
                    cellularEngines.get(e).accumulateCellularDerivatives(worldBuffer, i, dtYears, dtMonthly, acc);
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
