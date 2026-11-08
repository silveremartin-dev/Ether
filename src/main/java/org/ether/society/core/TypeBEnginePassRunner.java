/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

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
    private final AxialAgeMoralizingGodsEngine axialAgeMoralizingGodsEngine = new AxialAgeMoralizingGodsEngine();
    private final BarfieldNomadicSteppeEmpireEngine barfieldNomadicSteppeEmpireEngine = new BarfieldNomadicSteppeEmpireEngine();
    private final org.ether.society.engines.tier2.theories.WienerCyberneticHomeostasisEngine wienerCyberneticHomeostasisEngine = new org.ether.society.engines.tier2.theories.WienerCyberneticHomeostasisEngine();
    private final org.ether.society.engines.tier2.theories.PolanyiFictitiousCommoditiesEngine polanyiFictitiousCommoditiesEngine = new org.ether.society.engines.tier2.theories.PolanyiFictitiousCommoditiesEngine();
    private final org.ether.society.engines.tier2.theories.BagehotFractionalReserveEngine bagehotFractionalReserveEngine = new org.ether.society.engines.tier2.theories.BagehotFractionalReserveEngine();
    private final org.ether.society.engines.tier2.theories.CordenNearyDutchDiseaseEngine cordenNearyDutchDiseaseEngine = new org.ether.society.engines.tier2.theories.CordenNearyDutchDiseaseEngine();
    private final org.ether.society.engines.tier2.theories.InnisStaplesThesisEngine innisStaplesThesisEngine = new org.ether.society.engines.tier2.theories.InnisStaplesThesisEngine();
    private final org.ether.society.engines.tier2.theories.HarrisCulturalMaterialismEngine harrisCulturalMaterialismEngine = new org.ether.society.engines.tier2.theories.HarrisCulturalMaterialismEngine();
    private final org.ether.society.engines.tier2.theories.LenskiEcologicalEvolutionaryEngine lenskiEcologicalEvolutionaryEngine = new org.ether.society.engines.tier2.theories.LenskiEcologicalEvolutionaryEngine();
    private final org.ether.society.engines.tier2.theories.WhiteEnergyEvolutionEngine whiteEnergyEvolutionEngine = new org.ether.society.engines.tier2.theories.WhiteEnergyEvolutionEngine();
    private final org.ether.society.engines.tier2.theories.RicardoDifferentialLandRentEngine ricardoDifferentialLandRentEngine = new org.ether.society.engines.tier2.theories.RicardoDifferentialLandRentEngine();
    private final org.ether.society.engines.tier2.theories.LewisDualSectorTurningPointEngine lewisDualSectorTurningPointEngine = new org.ether.society.engines.tier2.theories.LewisDualSectorTurningPointEngine();
    private final org.ether.society.engines.tier2.theories.KondratievSchumpeterLongWaveEngine kondratievSchumpeterLongWaveEngine = new org.ether.society.engines.tier2.theories.KondratievSchumpeterLongWaveEngine();
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
     * Executes active Tier 2 cliodynamic theories and historical scenario engines over populated cells.
     *
     * @param populatedCells non-empty list of inhabited cells
     * @param dtYears        time step in Julian years
     * @param dtMonthly      monthly-scaled year fraction
     * @param scenario       active scenario configuration
     * @param compiledKernel optional JIT-compiled symbolic kernel
     */
    public void executeActiveTypeBEngines(List<H3Cell> populatedCells,
                                          double dtYears,
                                          double dtMonthly,
                                          Scenario scenario,
                                          CompiledEngineKernel compiledKernel) {
        if (compiledKernel != null) {
            compiledKernel.executeFusedKernel(populatedCells, dtMonthly);
        }
        if (scenario == null || scenario.getTypeBEngineStates() == null) {
            return;
        }
        Map<String, Boolean> states = scenario.getTypeBEngineStates();

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
        if (Boolean.TRUE.equals(states.get("PublicSanitationWaterGridEngine"))) {
            PublicSanitationWaterGridEngine.processSanitationGrids(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("PhysicalMediaPrintingEngine"))) {
            PhysicalMediaPrintingEngine.processMediaProduction(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("NavalHydrodynamicsFreightEngine"))) {
            NavalHydrodynamicsFreightEngine.processNavalFreight(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("VonThunenAgriculturalRingsEngine"))) {
            VonThunenAgriculturalRingsEngine.processAgriculturalRings(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("AxialAgeMoralizingGodsEngine"))) {
            axialAgeMoralizingGodsEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("BarfieldNomadicSteppeEmpireEngine"))) {
            barfieldNomadicSteppeEmpireEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("WienerCyberneticHomeostasisEngine")) || Boolean.TRUE.equals(states.get("AutoRegulationPureEngine"))) {
            wienerCyberneticHomeostasisEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("PolanyiFictitiousCommoditiesEngine"))) {
            polanyiFictitiousCommoditiesEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("BagehotFractionalReserveEngine"))) {
            bagehotFractionalReserveEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("CordenNearyDutchDiseaseEngine"))) {
            cordenNearyDutchDiseaseEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("InnisStaplesThesisEngine"))) {
            innisStaplesThesisEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("HarrisCulturalMaterialismEngine")) || Boolean.TRUE.equals(states.get("CulturalMaterialismPureEngine"))) {
            harrisCulturalMaterialismEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("LenskiEcologicalEvolutionaryEngine")) || Boolean.TRUE.equals(states.get("LenskiPureEngine"))) {
            lenskiEcologicalEvolutionaryEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("WhiteEnergyEvolutionEngine")) || Boolean.TRUE.equals(states.get("LeslieWhitePureEngine"))) {
            whiteEnergyEvolutionEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("RicardoDifferentialLandRentEngine"))) {
            ricardoDifferentialLandRentEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("LewisDualSectorTurningPointEngine"))) {
            lewisDualSectorTurningPointEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("KondratievSchumpeterLongWaveEngine"))) {
            kondratievSchumpeterLongWaveEngine.process(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("PomeranzGreatDivergenceEngine"))) {
            pomeranzGreatDivergenceEngine.process(populatedCells, dtYears);
        }

        executeHistoricalAndSpecializedEngines(populatedCells, dtYears, dtMonthly, states);
    }

    private void executeHistoricalAndSpecializedEngines(List<H3Cell> populatedCells,
                                                        double dtYears,
                                                        double dtMonthly,
                                                        Map<String, Boolean> states) {
        if (Boolean.TRUE.equals(states.get("AmerindianEcosystemEngine"))
                || Boolean.TRUE.equals(states.get("CrosbyColumbianExchangeEngine"))) {
            AmerindianEcosystemEngine.processHybrid(populatedCells, dtMonthly);
        }
        if (Boolean.TRUE.equals(states.get("AsymmetricColonialTradeEngine"))) {
            AsymmetricColonialTradeEngine.processHybrid(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("CoGovernanceTradeEngine"))) {
            CoGovernanceTradeEngine.processTradeAndGovernance(populatedCells, dtYears);
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
        if (Boolean.TRUE.equals(states.get("JevonsParadoxEngine"))) {
            JevonsParadoxEngine.processJevonsRebound(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("KinSelectionHamiltonEngine"))) {
            KinSelectionHamiltonEngine.processHybrid(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("KurzweilAcceleratingReturnsEngine"))) {
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("LandReclamationEngine"))) {
            LandReclamationEngine.processHybrid(populatedCells, dtMonthly * 365.25);
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
        if (Boolean.TRUE.equals(states.get("HandyNasaHybridEngine"))) {
            HandyNasaHybridEngine.processPlugin(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("HandyNasaPureEngine"))) {
            HandyNasaPureEngine.processHybrid(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("MilitaryTechShockEngine"))) {
            MilitaryTechShockEngine.processHybrid(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("NordhausDicePureEngine"))) {
            NordhausDicePureEngine.processHybrid(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("World3CouplingEngine"))) {
            World3CouplingEngine.processWorld3Coupling(populatedCells, dtYears);
        }
        if (Boolean.TRUE.equals(states.get("World3HybridEngine"))) {
            World3HybridEngine.processPlugin(populatedCells, dtMonthly);
        }
        if (Boolean.TRUE.equals(states.get("World3PureEngine"))) {
            World3PureEngine.processPlugin(populatedCells, dtMonthly);
        }
    }
}
