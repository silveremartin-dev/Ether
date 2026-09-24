/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.*;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.database.H3Cell;
import org.ether.society.density.H3ClimateSystem;
import org.ether.society.events.EventSystem;
import org.ether.society.procedural.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * High-Performance Declarative Simulation Pipeline.
 * Orchestrates deterministic phase transitions for physical, demographic, and cliodynamic sub-systems.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SimulationPipeline {
    private static final Logger logger = LoggerFactory.getLogger(SimulationPipeline.class);

    private final DemographicKernel demographicKernel;
    private final UrbanKernel urbanKernel;
    private final CultureKernel cultureKernel;
    private final EnvironmentalKernel environmentalKernel;
    private final StatisticsKernel statisticsKernel;

    public SimulationPipeline(DemographicKernel demographicKernel,
                              UrbanKernel urbanKernel,
                              CultureKernel cultureKernel,
                              EnvironmentalKernel environmentalKernel,
                              StatisticsKernel statisticsKernel) {
        this.demographicKernel = demographicKernel != null ? demographicKernel : new DemographicKernel();
        this.urbanKernel = urbanKernel != null ? urbanKernel : new UrbanKernel();
        this.cultureKernel = cultureKernel != null ? cultureKernel : new CultureKernel();
        this.environmentalKernel = environmentalKernel != null ? environmentalKernel : new EnvironmentalKernel();
        this.statisticsKernel = statisticsKernel != null ? statisticsKernel : new StatisticsKernel();
    }

    /**
     * Executes the complete deterministic cliodynamic pipeline for a single monthly simulation tick.
     */
    public void executeTick(H3SimulationEngine engine,
                            List<H3Cell> cells,
                            WorldBuffer worldBuffer,
                            AgentBuffer agentBuffer,
                            H3ClimateSystem climateSystem,
                            SimulationProfiler profiler,
                            SimulationPerformanceConfig perfConfig,
                            long dtSlow,
                            double avgTech) {
        if (cells == null || cells.isEmpty()) return;

        double dtYears = (double) dtSlow / 31_557_600.0;
        double dtMonthly = dtYears;

        // --- Phase 1: Demographics, Urbanization & Culture DOD Kernels ---
        profiler.beginPhase("3_DemographicsAndCulture");
        demographicKernel.tick(worldBuffer, agentBuffer, dtSlow);
        urbanKernel.tick(worldBuffer, dtSlow);
        cultureKernel.tick(worldBuffer, agentBuffer, dtSlow);
        profiler.endPhase("3_DemographicsAndCulture");

        // --- Phase 2: Core Physicalist & Cliodynamic Sub-Systems ---
        profiler.beginPhase("4_ProceduralEngines");
        executeCorePhysicsPhase(cells, dtYears, avgTech, perfConfig);
        executeAdvancedCliodynamicsPhase(cells, dtMonthly);

        // --- Phase 3: Dynamic Procedural Engine Plugins ---
        ProceduralEngineRegistry.processPlugins(cells, dtMonthly);
        profiler.endPhase("4_ProceduralEngines");
    }

    private void executeCorePhysicsPhase(List<H3Cell> cells, double dtYears, double avgTech, SimulationPerformanceConfig perfConfig) {
        // Step 1: Atmosphere, Radiation & Climate Feedbacks
        RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(cells);
        AtmosphericOxygenEngine.processAtmosphericOxygen(cells, 0.21, 1.0);
        WetBulbTemperatureEngine.processWetBulbHyperthermia(cells);
        AlbedoClimateEngine.processAlbedoFeedback(cells);

        // Step 2: Soil Nutrients, Hydrology & Ecosystem
        SoilNutrientNPKEngine.processSoilNutrients(cells);
        DeforestationErosionEngine.processDeforestationErosion(cells);
        AquiferDepletionEngine.processAquiferDepletion(cells);
        EcologicalDegradationEngine.processEcologicalDegradation(cells, avgTech);

        // Step 3: Demographics & Molecular Epidemiology
        BiologicalDemographicsEngine.processBiologicalDemographics(cells, dtYears);
        BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells);
        EcotoxicologyFertilityEngine.processEcotoxicologyFertility(cells);

        // Step 4: Energy Grid, Metallurgy & Nuclear Dynamics
        PhysicalEnergyGridEngine.processPhysicalEnergyGrid(cells);
        NetEnergyEROEIEngine.processNetEnergyEROEI(cells);
        MetallurgyEnthalpyEngine.processOreSmelting(cells);
        ResourceRecyclingEngine.processResourceRecycling(cells);
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(cells);
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(cells);

        // Step 5: Warfare, Infrastructure & Migration
        PhysicsTransportEngine.processPhysicsTransport(cells);
        ThermodynamicWarfareEngine.processKineticWarfare(cells, dtYears);
        NuclearWarfareClimateEngine.processNuclearWarfareClimate(cells);
        InfrastructureEnergyEngine.processInfrastructureEnergy(cells);
        ThermodynamicMigrationEngine.processThermodynamicMigration(cells, perfConfig);

        // Step 6: Tech Tree & Information Entropy
        InformationEntropyEngine.processInformationEntropy(cells);
        MegafaunaEcosystemEngine.processMegafaunaEcosystem(cells);
        SelectiveBreedingEngine.processSelectiveBreeding(cells);
        TechnologicalSingularityEngine.processTechnologicalSingularity(cells);
        TechTreeEngine.processTechnologyDiffusion(cells, null);
    }

    private void executeAdvancedCliodynamicsPhase(List<H3Cell> cells, double dtMonthly) {
        TerraformingEngine.processTerraforming(cells, dtMonthly);
        TrophicEcosystemEngine.processTrophicEcosystem(cells, dtMonthly);
        PhysicalSupplyChainEngine.processSupplyChains(cells, dtMonthly);
        UrbanThermodynamicsEngine.processUrbanThermodynamics(cells, dtMonthly);
        PhysicalLawEngine.applyPhysicalLaws(cells, dtMonthly);
        CulturalSociologyEngine.processCulturalSociology(cells, dtMonthly);
        CoGovernanceTradeEngine.processTradeAndGovernance(cells, dtMonthly);
        OreGradeThermodynamicsEngine.processOreDepletion(cells, dtMonthly);
        InfrastructureInertiaEngine.processInfrastructureInertia(cells, dtMonthly);
        EntropicMetalDissipationEngine.processEntropicDissipation(cells, dtMonthly);
        JevonsParadoxEngine.processJevonsRebound(cells, dtMonthly);
        World3CouplingEngine.processWorld3System(cells, dtMonthly);
        KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(cells, dtMonthly);
        BifurcationChaosEngine.processBifurcationAnalysis(cells, dtMonthly);
        DynamicHydrographicSiltationEngine.processHydrographicSiltation(cells, dtMonthly);
        PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(cells, dtMonthly);
        GeoengineeringAlbedoFeedbackEngine.processGeoengineeringAlbedo(cells, dtMonthly);
    }
}

