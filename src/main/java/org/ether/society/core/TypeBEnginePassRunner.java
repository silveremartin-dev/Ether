/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.*;
import org.ether.society.engines.compiler.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.generation.*;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates JIT compilation, global state reset, and per-tick execution of pluggable
 * Tier 2 cliodynamic, macroeconomic, and historical scenario engines for {@link SimulationPipeline}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see SimulationPipeline
 */
public final class TypeBEnginePassRunner {

    private static final Logger logger = LoggerFactory.getLogger(TypeBEnginePassRunner.class);

    private final KummelAyresExergyEngine kummelAyresExergyEngine = new KummelAyresExergyEngine();
    private final StructuralDemographicBifurcationEngine structuralDemographicBifurcationEngine = new StructuralDemographicBifurcationEngine();
    private final TurchinGoldstoneSDTEngine turchinGoldstoneSDTEngine = new TurchinGoldstoneSDTEngine();
    private final org.ether.society.engines.tier2.theories.LenskiEcologicalEvolutionaryEngine lenskiEcologicalEvolutionaryEngine = new org.ether.society.engines.tier2.theories.LenskiEcologicalEvolutionaryEngine();
    private final org.ether.society.engines.tier2.theories.PomeranzGreatDivergenceEngine pomeranzGreatDivergenceEngine = new org.ether.society.engines.tier2.theories.PomeranzGreatDivergenceEngine();

    /**
     * Resets all mutable static simulation states, caches, and plugin registries across engines.
     */
    public static void resetGlobalSimulationState() {
        KurzweilAcceleratingReturnsEngine.resetState();
        NuclearWarfareClimateEngine.resetState();
        BifurcationChaosEngine.resetState();
        TrophicEcosystemEngine.resetState();
        org.ether.society.engines.tier1.UnifiedNeutralBiodiversityEngine.resetState();
        org.ether.society.engines.tier1.ThermohalineStommelAMOCEngine.resetState();
        GeoengineeringAlbedoFeedbackEngine.resetState();
        TerraformingEngine.resetState();
        PhysicalLawEngine.resetState();
        World3CouplingEngine.resetState();
        CulturalSociologyEngine.resetState();
        ThermodynamicMigrationEngine.clearSpatialCache();
        DynamicMaritimeRoutingGraph.clearCache();
        PhysicalSupplyChainEngine.resetState();
        AmerindianEcosystemEngine.resetState();
        EdoJapanIsolationEngine.resetState();
        CoGovernanceTradeEngine.resetState();
        RomanImperialCliodynamicEngine.resetState();
        TradeNetworkEngine.resetCumulativeStats();
        InfrastructureInertiaEngine.resetState();
        EntropicMetalDissipationEngine.resetState();
        NuclearSafetyRadiotoxicityEngine.resetState();
        OreGradeThermodynamicsEngine.resetState();
        MonasticDemographicBufferEngine.resetState();
        ProtestantWorkEthicEngine.resetState();
        SmilMaterialTransitionsPureEngine.resetState();
        TasmanianCulturalRegressionEngine.resetState();
        JevonsParadoxEngine.resetState();
        LanguageLinguisticEngine.resetState();
        ProceduralEngineRegistry.clearPlugins();
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
        structuralDemographicBifurcationEngine.setSeed(seed);
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

        Map<String, Boolean> states = scenario.getTypeBEngineStates();
        ScenarioEngineJITCompiler compiler = new ScenarioEngineJITCompiler();
        int registeredEngines = 0;

        if (Boolean.TRUE.equals(states.get("SmilMaterialTransitionsPureEngine"))) {
            compiler.registerEngineStep("SmilMaterialTransitionsPureEngine", "resourceCapital",
                    new SymbolicExpression("resourceCapital", 0.999, 0.0), 0.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("JevonsParadoxEngine"))) {
            compiler.registerEngineStep("JevonsParadoxEngine", "energyFoodConsumed",
                    new SymbolicExpression("energyFoodConsumed", 1.005, 0.0), 0.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("TasmanianCulturalRegressionEngine"))) {
            compiler.registerEngineStep("TasmanianCulturalRegressionEngine", "resourceCapital",
                    new SymbolicExpression("resourceCapital", 0.999, 0.0), 0.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("CoGovernanceTradeEngine"))) {
            compiler.registerEngineStep("CoGovernanceTradeEngine", "pollutionLevel",
                    new SymbolicExpression("pollutionLevel", 0.98, 0.0), 200.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("KurzweilAcceleratingReturnsEngine"))) {
            compiler.registerEngineStep("KurzweilAcceleratingReturnsEngine", "resourceCapital",
                    new SymbolicExpression("resourceCapital", 1.002, 0.0), 100000.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("World3CouplingEngine"))) {
            compiler.registerEngineStep("World3CouplingEngine", "resourceCapital",
                    new SymbolicExpression("resourceCapital", 0.998, 0.0), 0.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("RomanImperialCliodynamicEngine"))) {
            compiler.registerEngineStep("RomanImperialCliodynamicEngine", "resourceCapital",
                    new SymbolicExpression("resourceCapital", 0.995, 0.0), 0.0);
            registeredEngines++;
        }

        if (registeredEngines == 0) {
            return null;
        }
        try {
            CompiledEngineKernel kernel = compiler.compile();
            logger.info("Scenario JIT Kernel compiled for '{}' with {} fused Type B theories.",
                    scenario.getName(), registeredEngines);
            return kernel;
        } catch (Exception e) {
            logger.warn("Could not compile scenario JIT kernel: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Executes active Tier 2 cliodynamic theories directly over the Data-Oriented WorldBuffer.
     * Zero-allocation DoD path maximizing CPU throughput and cache locality.
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
        if (worldBuffer == null) return;
        if (compiledKernel != null) {
            compiledKernel.executeFusedKernel(worldBuffer, (float) dtMonthly);
        }
        if (scenario == null || scenario.getTypeBEngineStates() == null) {
            return;
        }
        Map<String, Boolean> states = scenario.getTypeBEngineStates();

        if (Boolean.TRUE.equals(states.get("KummelAyresExergyEngine"))) {
            kummelAyresExergyEngine.processBuffer(worldBuffer, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("PomeranzGreatDivergenceEngine"))) {
            pomeranzGreatDivergenceEngine.processBuffer(worldBuffer, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("TurchinGoldstoneSDTEngine"))) {
            turchinGoldstoneSDTEngine.processBuffer(worldBuffer, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("PublicSanitationWaterGridEngine"))) {
            PublicSanitationWaterGridEngine.processSanitationGrids(worldBuffer, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("LenskiEcologicalEvolutionaryEngine")) || Boolean.TRUE.equals(states.get("LenskiPureEngine"))) {
            lenskiEcologicalEvolutionaryEngine.processBuffer(worldBuffer, dtYears);
        }
    }
}
