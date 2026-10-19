*
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
import org.ether.society.engines.tier2.theories.SoilSalinizationHydrologyEngine;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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

    private List<H3Cell> cachedLandCells = null;
    private int cachedLandCellsCount = -1;
    private List<H3Cell> reusablePopulatedCells = null;

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
        KurzweilAcceleratingReturnsEngine.resetState();
        NuclearWarfareClimateEngine.resetState();
        BifurcationChaosEngine.resetState();
        TrophicEcosystemEngine.resetState();
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
        if (this.structuralDemographicBifurcationEngine != null) {
            this.structuralDemographicBifurcationEngine.setSeed(sSeed);
        }
        NuclearSafetyRadiotoxicityEngine.setBaseSeed(sSeed);
        NuclearWarfareClimateEngine.setBaseSeed(sSeed);
        LanguageLinguisticEngine.setBaseSeed(sSeed);

        if (scenario == null || scenario.getTypeBEngineStates() == null || scenario.getTypeBEngineStates().isEmpty()) {
            this.compiledKernel = null;
            return;
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
            compiler.registerEngineStep("TasmanianCulturalRegressionEngine", "technologyLevel",
                    new SymbolicExpression("technologyLevel", 0.999, 0.0), 1.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("CoGovernanceTradeEngine"))) {
            compiler.registerEngineStep("CoGovernanceTradeEngine", "pollutionLevel",
                    new SymbolicExpression("pollutionLevel", 0.98, 0.0), 200.0);
            registeredEngines++;
        }
        if (Boolean.TRUE.equals(states.get("KurzweilAcceleratingReturnsEngine"))) {
            compiler.registerEngineStep("KurzweilAcceleratingReturnsEngine", "technologyLevel",
                    new SymbolicExpression("technologyLevel", 1.002, 0.0), 10.0);
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

        if (registeredEngines > 0) {
            try {
                this.compiledKernel = compiler.compile();
                logger.info("⚡ Scenario JIT Kernel Compiled successfully for '{}' with {} fused Type B theories into a single unified pass.",
                        scenario.getName(), registeredEngines);
            } catch (Exception e) {
                logger.warn("Could not compile scenario JIT kernel: {}", e.getMessage());
                this.compiledKernel = null;
            }
        } else {
            this.compiledKernel = null;
        }
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
        environmentalKernel.tick(worldBuffer, dtSlow);
        demographicKernel.tick(worldBuffer, agentBuffer, dtSlow);
        urbanKernel.tick(worldBuffer, dtSlow);
        fluxKernel.tick(worldBuffer, dtSlow);
        cultureKernel.tick(worldBuffer, agentBuffer, dtSlow);
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
        executeFusedAtmosphericPass(cells, worldBuffer, agentBuffer, dtYears, dtMonthly, perfConfig, avgTech);

        // Pass 2: Terrestrial Soil, Water & Ecosystem Fused Pass
        if (!landCells.isEmpty()) {
            executeFusedTerrestrialPass(landCells, dtYears, dtMonthly, avgTech);
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
                                             double avgTech) {
        RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(allCells);
        AtmosphericOxygenEngine.processAtmosphericOxygen(allCells, 0.21, 1.0);
        AlbedoClimateEngine.processAlbedoFeedback(allCells, dtYears);

        greenhouseRadiativeEngine.applyTechnologicalMitigation(avgTech);
        greenhouseRadiativeEngine.applyToGrid(allCells);

        TerraformingEngine.processTerraforming(allCells, dtMonthly);
        PhysicalLawEngine.applyPhysicalLaws(allCells, dtMonthly);
        GeoengineeringAlbedoFeedbackEngine.processGeoengineeringAlbedo(allCells, dtMonthly);
        ThermohalineOceanEngine.processThermohalineCirculation(allCells, 0.0);
        OceanAcidificationEngine.processOceanAcidification(allCells, 415.0);

        ThermodynamicMigrationEngine.processThermodynamicMigration(allCells, perfConfig);
    }

    /**
     * Pass 2: Soil Nutrients, Hydrology, Erosion & Terrestrial Ecosystems (Land Domain).
     */
    private void executeFusedTerrestrialPass(List<H3Cell> landCells, double dtYears, double dtMonthly, double avgTech) {
        GlacialIsostaticAdjustmentEngine.processGlacialIsostasy(landCells, dtYears);
        SoilWaterRetentionEngine.processSoilWaterRetention(landCells);
        CrustalGeothermalEngine.processCrustalGeothermal(landCells);
        PermafrostThawEngine.processPermafrostThaw(landCells);
        SoilNutrientNPKEngine.processSoilNutrients(landCells, dtYears);
        DeforestationErosionEngine.processDeforestationErosion(landCells, dtYears);
        AquiferDepletionEngine.processAquiferDepletion(landCells, dtYears);
        EcologicalDegradationEngine.processEcologicalDegradation(landCells, avgTech, dtYears);
        MegafaunaEcosystemEngine.processMegafaunaEcosystem(landCells, dtYears);

        TrophicEcosystemEngine.processTrophicEcosystem(landCells, dtMonthly);
        DynamicHydrographicSiltationEngine.processHydrographicSiltation(landCells, dtMonthly);
    }

    private final AcemogluRobinsonInstitutionsEngine acemogluRobinsonInstitutionsEngine = new AcemogluRobinsonInstitutionsEngine();
    private final ArthurCombinatorialTechnologyEngine arthurCombinatorialTechnologyEngine = new ArthurCombinatorialTechnologyEngine();
    private final BoserupAgriculturalIntensificationEngine boserupAgriculturalIntensificationEngine = new BoserupAgriculturalIntensificationEngine();
    private final DraftAnimalFodderAllocationEngine draftAnimalFodderAllocationEngine = new DraftAnimalFodderAllocationEngine();
    private final GranovetterThresholdCascadeEngine granovetterThresholdCascadeEngine = new GranovetterThresholdCascadeEngine();
    private final HotellingResourceDepletionEngine hotellingResourceDepletionEngine = new HotellingResourceDepletionEngine();
    private final KrugmanCorePeripheryEngine krugmanCorePeripheryEngine = new KrugmanCorePeripheryEngine();
    private final KummelAyresExergyEngine kummelAyresExergyEngine = new KummelAyresExergyEngine();
    private final PriceMultilevelSelectionEngine priceMultilevelSelectionEngine = new PriceMultilevelSelectionEngine();
    private final SchellingAxelrodSegregationEngine schellingAxelrodSegregationEngine = new SchellingAxelrodSegregationEngine();
    private final SoilSalinizationHydrologyEngine soilSalinizationHydrologyEngine = new SoilSalinizationHydrologyEngine();
    private final SpatialMetapopulationSEIREngine spatialMetapopulationSEIREngine = new SpatialMetapopulationSEIREngine();
    private final StructuralDemographicBifurcationEngine structuralDemographicBifurcationEngine = new StructuralDemographicBifurcationEngine();
    private final TainterComplexityCollapseEngine tainterComplexityCollapseEngine = new TainterComplexityCollapseEngine();
    private final TurchinGoldstoneSDTEngine turchinGoldstoneSDTEngine = new TurchinGoldstoneSDTEngine();
    private final WestBettencourtAllometryEngine westBettencourtAllometryEngine = new WestBettencourtAllometryEngine();
    private final SovereignAIGovernanceEngine sovereignAIGovernanceEngine = new SovereignAIGovernanceEngine();

    /**
     * Pass 3: Human Demographics, Power Grid, Metallurgy, Technology & Active Cliodynamic Theories (Populated Domain).
     */
    private void executeFusedAnthropogenicPass(H3SimulationEngine engine, List<H3Cell> populatedCells, double dtYears, double dtMonthly, Scenario scenario) {
        // Step 3.1: Physiological & Bioclimatic Survival
        WetBulbTemperatureEngine.processWetBulbHyperthermia(populatedCells);
        PressurizedHabitatEngine.processPressurizedHabitats(populatedCells, null, dtYears);
        BiologicalDemographicsEngine.processBiologicalDemographics(populatedCells, dtYears);
        BioMolecularEpidemiologyEngine.processBioMolecularImmunity(populatedCells, dtYears);
        EcotoxicologyFertilityEngine.processEcotoxicologyFertility(populatedCells);

        // Step 3.2: Energy Grid, Metallurgy & Physical Industry
        PhysicalEnergyGridEngine.processPhysicalEnergyGrid(populatedCells, dtYears);
        NetEnergyEROEIEngine.processNetEnergyEROEI(populatedCells, dtYears);
        MetallurgyEnthalpyEngine.processOreSmelting(populatedCells, dtYears);
        ResourceRecyclingEngine.processResourceRecycling(populatedCells, dtYears);
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(populatedCells);

        // Step 3.3: Transport, Kinetic Warfare & Infrastructure
        PhysicsTransportEngine.processPhysicsTransport(populatedCells, dtYears);
        ThermodynamicWarfareEngine.processKineticWarfare(populatedCells, dtYears);
        NuclearWarfareClimateEngine.processNuclearWarfareClimate(populatedCells);
        InfrastructureEnergyEngine.processInfrastructureEnergy(populatedCells);

        // Step 3.4: Recombinant Technology, Information Entropy & Ozone
        SelectiveBreedingEngine.processSelectiveBreeding(populatedCells, dtYears);
        TechnologicalSingularityEngine.processTechnologicalSingularity(populatedCells, dtYears);
        InformationEntropyEngine.processInformationEntropy(populatedCells);

        List<TradeNetworkEngine.TradeRoute> tradeRoutes = null;
        if (scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("TradeNetworkEngine"))) {
            double meanTech = populatedCells.stream()
                    .mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0)
                    .average()
                    .orElse(2.0);
            tradeRoutes = TradeNetworkEngine.generateTradeNetworks(populatedCells, meanTech, dtYears);
        }
        TechTreeEngine.processTechnologyDiffusion(populatedCells, tradeRoutes, dtYears);
        LanguageLinguisticEngine.processLinguisticDrift(populatedCells, tradeRoutes);
        if (engine != null && engine.getNations() != null && !engine.getNations().isEmpty()) {
            CliodynamicsEngine.updateCliodynamics(engine.getNations(), populatedCells);
            WarDiplomacyEngine.processGeopoliticalConflicts(engine.getNations(), populatedCells, scenario);
        }
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(populatedCells);

        // Step 3.5: Invariant Tier 1 Industrial Physics & Thermodynamics
        OreGradeThermodynamicsEngine.processOreDepletion(populatedCells, dtMonthly);
        InfrastructureInertiaEngine.processInfrastructureInertia(populatedCells, dtMonthly);
        EntropicMetalDissipationEngine.processEntropicDissipation(populatedCells, dtMonthly);

        // Step 3.6: JIT-Compiled Kernel Fusion for Active Type B Theories
        if (compiledKernel != null) {
            compiledKernel.executeFusedKernel(populatedCells, dtMonthly);
        }

        // Step 3.7: Active Type B Cliodynamic Theories & Historical Scenario Engines
        if (scenario != null && scenario.getTypeBEngineStates() != null) {
            Map<String, Boolean> states = scenario.getTypeBEngineStates();
            
            // Macroeconomic & Sociological Type B Theories
            if (Boolean.TRUE.equals(states.get("UrbanThermodynamicsEngine"))) {
                UrbanThermodynamicsEngine.processUrbanThermodynamics(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("CulturalSociologyEngine"))) {
                CulturalSociologyEngine.processCulturalSociology(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("PhysicalLeontiefInputOutputEngine"))) {
                PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("BifurcationChaosEngine"))) {
                BifurcationChaosEngine.processBifurcationAnalysis(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("AcemogluRobinsonInstitutionsEngine"))) {
                acemogluRobinsonInstitutionsEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("ArthurCombinatorialTechnologyEngine"))) {
                arthurCombinatorialTechnologyEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("BoserupAgriculturalIntensificationEngine"))) {
                boserupAgriculturalIntensificationEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("DraftAnimalFodderAllocationEngine"))) {
                draftAnimalFodderAllocationEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("GranovetterThresholdCascadeEngine"))) {
                granovetterThresholdCascadeEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("HotellingResourceDepletionEngine"))) {
                hotellingResourceDepletionEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("KrugmanCorePeripheryEngine"))) {
                krugmanCorePeripheryEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("KummelAyresExergyEngine"))) {
                kummelAyresExergyEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("PriceMultilevelSelectionEngine"))) {
                priceMultilevelSelectionEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SchellingAxelrodSegregationEngine"))) {
                schellingAxelrodSegregationEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SoilSalinizationHydrologyEngine"))) {
                soilSalinizationHydrologyEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SpatialMetapopulationSEIREngine"))) {
                spatialMetapopulationSEIREngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("StructuralDemographicBifurcationEngine"))) {
                structuralDemographicBifurcationEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("TainterComplexityCollapseEngine"))) {
                tainterComplexityCollapseEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("TurchinGoldstoneSDTEngine"))) {
                turchinGoldstoneSDTEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("WestBettencourtAllometryEngine"))) {
                westBettencourtAllometryEngine.process(populatedCells, dtYears);
            }

            // Historical & Specialized Type B Engines
            if (Boolean.TRUE.equals(states.get("AmerindianEcosystemEngine"))) {
                AmerindianEcosystemEngine.processHybrid(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("AsymmetricColonialTradeEngine"))) {
                AsymmetricColonialTradeEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("DynamicMaritimeRoutingGraph"))) {
                DynamicMaritimeRoutingGraph.processHybrid(populatedCells, dtMonthly * 365.25);
            }
            if (Boolean.TRUE.equals(states.get("EdoJapanIsolationEngine"))) {
                EdoJapanIsolationEngine.processHybrid(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("FrontierAsabiyyahEngine"))) {
                FrontierAsabiyyahEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("HydrologicalEngineeringEngine"))) {
                HydrologicalEngineeringEngine.processHybrid(populatedCells, dtMonthly * 365.25);
            }
            if (Boolean.TRUE.equals(states.get("IsruAutarkyAndSpaceColonizationEngine"))) {
                IsruAutarkyAndSpaceColonizationEngine.processSpaceColonizationCliodynamics(populatedCells, null, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("KinSelectionHamiltonEngine"))) {
                KinSelectionHamiltonEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("LandReclamationEngine"))) {
                LandReclamationEngine.processHybrid(populatedCells, dtMonthly * 365.25);
            }
            if (Boolean.TRUE.equals(states.get("LenskiPureEngine"))) {
                LenskiPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("MarineSubmersionEngine"))) {
                MarineSubmersionEngine.processHybrid(populatedCells, dtMonthly * 365.25);
            }
            if (Boolean.TRUE.equals(states.get("MaritimeHighwayEngine"))) {
                MaritimeHighwayEngine.processHybrid(populatedCells, dtMonthly * 365.25);
            }
            if (Boolean.TRUE.equals(states.get("MonasticDemographicBufferEngine"))) {
                MonasticDemographicBufferEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("NordhausDiceHybridEngine")) || Boolean.TRUE.equals(states.get("NordhausDICE"))) {
                NordhausDiceHybridEngine.processPlugin(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("OstromCommonsPureEngine"))) {
                OstromCommonsPureEngine.processHybrid(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("PhysicalSupplyChainEngine"))) {
                PhysicalSupplyChainEngine.processSupplyChains(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("PinkerViolenceDeclinePureEngine")) || Boolean.TRUE.equals(states.get("Pinker"))) {
                PinkerViolenceDeclinePureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("ProtestantWorkEthicEngine"))) {
                ProtestantWorkEthicEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("RomanImperialCliodynamicEngine"))) {
                RomanImperialCliodynamicEngine.processHybrid(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("ScottAgainstTheGrainPureEngine"))) {
                ScottAgainstTheGrainPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SelfDomesticationEngine"))) {
                SelfDomesticationEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SexualSelectionMatingEngine"))) {
                SexualSelectionMatingEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SmilMaterialTransitionsPureEngine"))) {
                SmilMaterialTransitionsPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SpatialCityFractalEngine"))) {
                SpatialCityFractalEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("TasmanianCulturalRegressionEngine"))) {
                TasmanianCulturalRegressionEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("AiAutonomousRegulationPureEngine")) || Boolean.TRUE.equals(states.get("AutoRegulationPureEngine"))) {
                AiAutonomousRegulationPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("CulturalMaterialismPureEngine"))) {
                CulturalMaterialismPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("HandyNasaHybridEngine"))) {
                HandyNasaHybridEngine.processPlugin(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("HandyNasaPureEngine"))) {
                HandyNasaPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("KardashevPureEngine"))) {
                KardashevPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("LeslieWhitePureEngine"))) {
                LeslieWhitePureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("MilitaryTechShockEngine"))) {
                MilitaryTechShockEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("NordhausDicePureEngine"))) {
                NordhausDicePureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("PsychohistoryPureEngine"))) {
                PsychohistoryPureEngine.processHybrid(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("SovereignAIGovernanceEngine"))) {
                sovereignAIGovernanceEngine.process(populatedCells, dtYears);
            }
            if (Boolean.TRUE.equals(states.get("World3HybridEngine"))) {
                World3HybridEngine.processPlugin(populatedCells, dtMonthly);
            }
            if (Boolean.TRUE.equals(states.get("World3PureEngine"))) {
                World3PureEngine.processPlugin(populatedCells, dtMonthly);
            }
        }
    }
}
