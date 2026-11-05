/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.*;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.database.H3Cell;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.generation.*;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * High-Performance Declarative Simulation Pipeline.
 * Orchestrates deterministic phase transitions for physical, demographic, and cliodynamic sub-systems
 * using a Fused 3-Pass Architecture and JIT-compiled Type B scenario kernels.
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
    private final GreenhouseRadiativeEngine greenhouseRadiativeEngine = new GreenhouseRadiativeEngine();
    private final TypeBEnginePassRunner typeBPassRunner = new TypeBEnginePassRunner();

    private List<H3Cell> cachedLandCells = null;
    private int cachedLandCellsCount = -1;
    private List<H3Cell> reusablePopulatedCells = null;
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
        this.cachedLandCells = null;
        this.cachedLandCellsCount = -1;

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
     * Executes the complete deterministic cliodynamic pipeline for a single monthly simulation tick.
     */
    public void executeTick(H3SimulationEngine engine,
                            List<H3Cell> cells,
                            WorldBuffer worldBuffer,
                            AgentBuffer agentBuffer,
                            SimulationProfiler profiler,
                            SimulationPerformanceConfig perfConfig,
                            long dtSlow,
                            double avgTech) {
        if (cells == null || cells.isEmpty()) return;

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
                && org.ether.society.core.dod.NativeRustBridge.isNativeAvailable() && worldBuffer != null && agentBuffer != null) {
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
                cachedNativeArena.syncToJavaBuffers(worldBuffer, agentBuffer);
                executedNative = true;
            } catch (Throwable t) {
                logger.warn("Native Rust execution encountered an issue, falling back to Java SIMD: {}", t.getMessage());
                executedNative = false;
            }
        }

        if (!executedNative) {
            environmentalKernel.tick(worldBuffer, dtSlow);
            demographicKernel.tick(worldBuffer, agentBuffer, dtSlow);
            urbanKernel.tick(worldBuffer, dtSlow);
            fluxKernel.tick(worldBuffer, dtSlow);
            cultureKernel.tick(worldBuffer, agentBuffer, dtSlow);
        }

        if (engine != null) {
            engine.syncBufferToCells();
        }

        profiler.endPhase("3_DemographicsAndCulture");

        // --- Phase 2: Core Physicalist & Cliodynamic Sub-Systems (Fused 3-Pass Architecture) ---
        profiler.beginPhase("4_ProceduralEngines");

        // Physical Domain Partitioning (100% Deterministic: avoids useless evaluations on ocean & uninhabited cells)
        int totalCellCount = cells.size();
        if (cachedLandCells == null || cachedLandCellsCount != totalCellCount) {
            cachedLandCells = new java.util.ArrayList<>(totalCellCount / 3);
            for (int i = 0; i < totalCellCount; i++) {
                H3Cell c = cells.get(i);
                if (c == null) continue;
                Double elev = c.getElevation();
                Biome b = c.getBiome();
                if (elev == null || elev >= 0 || (b != null && b != Biome.OCEAN)) {
                    cachedLandCells.add(c);
                }
            }
            cachedLandCellsCount = totalCellCount;
        }
        List<H3Cell> landCells = cachedLandCells;

        if (reusablePopulatedCells == null) {
            reusablePopulatedCells = new java.util.ArrayList<>(Math.min(10000, totalCellCount));
        } else {
            reusablePopulatedCells.clear();
        }
        List<H3Cell> populatedCells = reusablePopulatedCells;

        float[] popArray = worldBuffer != null ? worldBuffer.getBiomassHuman() : null;
        if (popArray != null) {
            int limit = Math.min(totalCellCount, popArray.length);
            for (int i = 0; i < limit; i++) {
                if (popArray[i] > 0.0f) {
                    H3Cell c = cells.get(i);
                    if (c != null) {
                        populatedCells.add(c);
                    }
                }
            }
        } else {
            for (int i = 0; i < totalCellCount; i++) {
                H3Cell c = cells.get(i);
                if (c == null) continue;
                Integer pop = c.getPopulation();
                if (pop != null && pop > 0) {
                    populatedCells.add(c);
                }
            }
        }

        // Pass 1: Planetary & Atmospheric Fused Pass
        double currentCo2Ppm = (engine != null && engine.getMetricsAggregator() != null)
                ? engine.getMetricsAggregator().getAtmosphericCO2Ppm()
                : 280.0;
        executeFusedAtmosphericPass(cells, worldBuffer, agentBuffer, dtYears, dtMonthly, perfConfig, currentCo2Ppm);

        // Pass 2: Terrestrial Soil, Water & Ecosystem Fused Pass
        if (!landCells.isEmpty()) {
            executeFusedTerrestrialPass(landCells, dtYears, dtMonthly);
        }

        // Pass 3: Anthropogenic Inhabited Cliodynamics & JIT-Compiled Scenario Theories Fused Pass
        boolean runTier2 = perfConfig == null || perfConfig.isEnableTier2Cliodynamics();
        if (runTier2 && !populatedCells.isEmpty()) {
            executeFusedAnthropogenicPass(engine, populatedCells, dtYears, dtMonthly, activeScenario);

            // Dynamic Procedural Engine Plugins
            ProceduralEngineRegistry.processPlugins(populatedCells, dtMonthly);
        }

        if (engine != null) {
            engine.syncCellsToBuffer();
        }

        profiler.endPhase("4_ProceduralEngines");
    }

    /**
     * Pass 1: Atmosphere, Radiation, Energy & Climate Feedbacks (Planetary Grid Domain).
     */
    private void executeFusedAtmosphericPass(List<H3Cell> allCells,
                                             WorldBuffer worldBuffer,
                                             AgentBuffer agentBuffer,
                                             double dtYears, double dtMonthly,
                                             SimulationPerformanceConfig perfConfig,
                                             double co2Ppm) {
        RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(allCells);
        AtmosphericOxygenEngine.processAtmosphericOxygen(allCells, 0.21, 1.0);
        AlbedoClimateEngine.processAlbedoFeedback(allCells, dtYears);

        greenhouseRadiativeEngine.applyToGrid(allCells);

        TerraformingEngine.processTerraforming(allCells, dtMonthly);
        PhysicalLawEngine.applyPhysicalLaws(allCells, dtMonthly);
        GeoengineeringAlbedoFeedbackEngine.processGeoengineeringAlbedo(allCells, dtMonthly);
        ThermohalineOceanEngine.processThermohalineCirculation(allCells, 0.0);
        OceanAcidificationEngine.processOceanAcidification(allCells, co2Ppm);

        ThermodynamicMigrationEngine.processThermodynamicMigration(allCells, perfConfig, worldBuffer, agentBuffer);
    }

    /**
     * Pass 2: Soil Nutrients, Hydrology, Erosion & Terrestrial Ecosystems (Land Domain).
     */
    private void executeFusedTerrestrialPass(List<H3Cell> landCells, double dtYears, double dtMonthly) {
        GlacialIsostaticAdjustmentEngine.processGlacialIsostasy(landCells, dtYears);
        SoilWaterRetentionEngine.processSoilWaterRetention(landCells);
        CrustalGeothermalEngine.processCrustalGeothermal(landCells);
        PermafrostThawEngine.processPermafrostThaw(landCells);
        SoilNutrientNPKEngine.processSoilNutrients(landCells, dtYears);
        DeforestationErosionEngine.processDeforestationErosion(landCells, dtYears);
        AquiferDepletionEngine.processAquiferDepletion(landCells, dtYears);
        org.ether.society.engines.tier1.ManningStricklerHydrodynamicsEngine.processRiverHydrodynamics(landCells, dtYears);
        EcologicalDegradationEngine.processEcologicalDegradation(landCells, dtYears);
        MegafaunaEcosystemEngine.processMegafaunaEcosystem(landCells, dtYears);

        TrophicEcosystemEngine.processTrophicEcosystem(landCells, dtMonthly);
        DynamicHydrographicSiltationEngine.processHydrographicSiltation(landCells, dtMonthly);
    }

    /**
     * Pass 3: Human Demographics, Power Grid, Metallurgy, Technology & Active Cliodynamic Theories (Populated Domain).
     */
    private void executeFusedAnthropogenicPass(H3SimulationEngine engine, List<H3Cell> populatedCells, double dtYears, double dtMonthly, Scenario scenario) {
        // Step 3.1: Physiological & Bioclimatic Survival
        WetBulbTemperatureEngine.processWetBulbHyperthermia(populatedCells, dtYears);
        PressurizedHabitatEngine.processPressurizedHabitats(populatedCells, null, dtYears);
        BiologicalDemographicsEngine.processBiologicalDemographics(populatedCells, dtYears);
        BioMolecularEpidemiologyEngine.processBioMolecularImmunity(populatedCells, dtYears);
        EcotoxicologyFertilityEngine.processEcotoxicologyFertility(populatedCells, dtYears);

        // Step 3.2: Energy Grid, Metallurgy & Physical Industry
        PhysicalEnergyGridEngine.processPhysicalEnergyGrid(populatedCells, dtYears);
        NetEnergyEROEIEngine.processNetEnergyEROEI(populatedCells, dtYears);
        MetallurgyEnthalpyEngine.processOreSmelting(populatedCells, dtYears);
        ResourceRecyclingEngine.processResourceRecycling(populatedCells, dtYears);
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(populatedCells, engine != null ? engine.getTickCounter() : 0L, dtYears);

        // Step 3.3: Transport, Kinetic Warfare & Infrastructure
        PhysicsTransportEngine.processPhysicsTransport(populatedCells, dtYears);
        if (scenario != null && scenario.getTypeBEngineStates() != null && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("ThermodynamicWarfareEngine"))) {
            ThermodynamicWarfareEngine.processKineticWarfare(populatedCells, dtYears);
        }
        if (scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("NuclearWarfareClimateEngine"))) {
            NuclearWarfareClimateEngine.processNuclearWarfareClimate(populatedCells);
        }
        InfrastructureEnergyEngine.processInfrastructureEnergy(populatedCells);

        // Step 3.4: Recombinant Technology, Information Entropy & Ozone
        SelectiveBreedingEngine.processSelectiveBreeding(populatedCells, dtYears);
        if (scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("TechnologicalSingularityEngine"))) {
            TechnologicalSingularityEngine.processTechnologicalSingularity(populatedCells, dtYears);
        }
        InformationEntropyEngine.processInformationEntropy(populatedCells);

        List<TradeNetworkEngine.TradeRoute> tradeRoutes = null;
        if (scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("TradeNetworkEngine"))) {
            tradeRoutes = TradeNetworkEngine.generateTradeNetworks(populatedCells, 1.0, dtYears);
        }
        TechTreeEngine.processTechnologyDiffusion(populatedCells, tradeRoutes, dtYears);
        LanguageLinguisticEngine.processLinguisticDrift(populatedCells, tradeRoutes);
        if (engine != null && engine.getNations() != null && !engine.getNations().isEmpty()) {
            CliodynamicsEngine.updateCliodynamics(engine.getNations(), populatedCells, dtYears);
            WarDiplomacyEngine.processGeopoliticalConflicts(engine.getNations(), populatedCells, scenario);
        }
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(populatedCells);

        // Step 3.5: Invariant Tier 1 Industrial Physics & Thermodynamics
        OreGradeThermodynamicsEngine.processOreDepletion(populatedCells, dtMonthly);
        InfrastructureInertiaEngine.processInfrastructureInertia(populatedCells, dtMonthly);
        EntropicMetalDissipationEngine.processEntropicDissipation(populatedCells, dtMonthly);

        // Step 3.6 & 3.7: JIT-Compiled Kernel Fusion & Active Type B Cliodynamic Engines
        typeBPassRunner.executeActiveTypeBEngines(populatedCells, dtYears, dtMonthly, scenario, compiledKernel);
    }
}
