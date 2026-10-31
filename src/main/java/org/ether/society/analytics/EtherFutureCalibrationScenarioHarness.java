/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <h1>EtherFutureCalibrationScenarioHarness</h1>
 * <p>
 * Dedicated scientific calibration harness and offline generator for the flagship
 * <b>Ether Biophysical Planetary Simulation (2025 CE - 2200 CE)</b>.
 * </p>
 * <p>
 * <b>Design Principles:</b>
 * <ul>
 *   <li><b>Non-UI Internal Scenario</b>: Deliberately excluded from the standard user scenario presets catalog.</li>
 *   <li><b>Spatial Resolution</b>: H3 Resolution $r = 4$ for high-precision regional biogeographical fidelity.</li>
 *   <li><b>Temporal Resolution</b>: Monthly step size $\Delta t = 30.0$ days.</li>
 *   <li><b>Authentic T0 Initialization</b>: Empirical 2024/2025 baseline seamlessly continued from ERA5, UN WPP 2024, and Maddison 2020.</li>
 *   <li><b>Tier 2 Cliodynamic Engine Filter</b>: Systematically activates all peer-reviewed contemporary/future
 *       Type B engines, while <b>strictly excluding</b> the 7 falsified/invalidated debate models
 *       (Malthusian Static Trap, Nordhaus DICE, Pinker Pacification, Scott Cereal Determinism,
 *       Hardin Unregulated Commons, Diamond Pure Geography, Psychohistory).</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class EtherFutureCalibrationScenarioHarness {

    private static final Logger logger = LoggerFactory.getLogger(EtherFutureCalibrationScenarioHarness.class);

    public static final String SCENARIO_ID = "ETHER_CALIBRATION_2025_2200";
    public static final int START_YEAR = 2025;
    public static final int END_YEAR = 2200;
    public static final int H3_RESOLUTION = 4;
    public static final double TIME_STEP_DAYS = 30.0; // Monthly time step

    private EtherFutureCalibrationScenarioHarness() {
        // Utility harness
    }

    /**
     * Builds and returns the canonical unadulterated {@link Scenario} definition
     * for the 2025-2200 Earth Biophysical Calibration trajectory.
     *
     * @return fully configured Scenario instance
     */
    public static Scenario createCalibrationScenario() {
        Scenario s = new Scenario();
        s.setPresetKey(SCENARIO_ID);
        s.setName("Ether Planetary Biophysical Simulation (Calibrated 2025-2200)");
        s.setDescription("Flagship scientific calibration baseline projecting coupled demographic, thermodynamic, "
                + "and institutional dynamics from 2025 to 2200 CE under H3 Resolution 4 and monthly step size.");

        // Planetary baseline: Real Earth geography & satellite biomes
        s.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s.setUseRealEarthData(true);
        s.setReferenceTrajectoryModel("ETHER_CALIBRATED");

        // Spatiotemporal dimensions
        s.setStartDateYear(START_YEAR);
        s.setEndDateYear(END_YEAR);
        s.setH3Resolution(H3_RESOLUTION);
        s.setTemporalResolutionDays(TIME_STEP_DAYS);
        s.setTargetCohortSize(2000);
        s.setStrictDeterminism(true);

        // Initial 2025 empirical conditions (UN WPP 2024, World Bank, IEA 2024)
        s.setInitialHumanCount(8160000000L); // 8.16 Billion people (UN WPP 2024)
        s.setInitialCapitalPerCapita(17500.0); // 2011 US$ PPP (Maddison / World Bank)
        s.setInitialEnergyPerCapita(78000.0);  // MJ/capita/year (Smil 2017 / IEA 2024)
        s.setInitialFoodReserveMonths(3.5);
        s.setInitialTechLevel(7.5);
        s.setClimateHarshness(0.5);

        // Configure Tier 2 Cliodynamic Engines (Filter: Accept authentic, Reject falsified)
        Map<String, Boolean> engines = new LinkedHashMap<>();

        // ── 1. ACTIVATED & VALIDATED THEORIES (Contemporary / Future Era) ──
        engines.put("TurchinGoldstoneSDTEngine", true);                   // Elite overproduction & social cohesion cycles
        engines.put("AcemogluRobinsonInstitutionsEngine", true);           // Inclusive vs extractive institutional feedback
        engines.put("BoserupAgriculturalIntensificationEngine", true);     // Endogenous agricultural yield response
        engines.put("BioMolecularEpidemiologyEngine", true);               // Pathogen mutation & host interaction
        engines.put("SpatialMetapopulationSEIREngine", true);              // Metapopulation transmission across flight/rail networks
        engines.put("GranovetterThresholdCascadeEngine", true);           // Social tipping points & collective action cascades
        engines.put("SmilMaterialTransitionsPureEngine", true);            // Vaclav Smil thermodynamic exergy & material limits
        engines.put("KummelAyresExergyEngine", true);                      // Biophysical exergy-driven economic production
        engines.put("OstromCommonsPureEngine", true);                      // Polycentric governance of water/commons (Elinor Ostrom)
        engines.put("KrugmanCorePeripheryEngine", true);                   // New Economic Geography agglomeration & scale economies
        engines.put("ArthurCombinatorialTechnologyEngine", true);          // Combinatorial technological evolution (Brian Arthur)
        engines.put("JevonsParadoxEngine", true);                          // Rebound effect from energy efficiency gains
        engines.put("WestBettencourtAllometryEngine", true);               // Urban metabolic scaling laws (Geoffrey West)
        engines.put("HotellingResourceDepletionEngine", true);             // Optimal non-renewable resource extraction rent
        engines.put("PhysicalLeontiefInputOutputEngine", true);            // Biophysical material balance input-output matrix
        engines.put("DynamicMaritimeRoutingGraph", true);                  // Global container trade choke-points (Suez, Malacca, Panama)
        engines.put("SovereignAIGovernanceEngine", true);                  // Digital coordination & institutional computation capacity
        engines.put("TechnologicalSingularityEngine", false);              // Neutral: disabled by default to evaluate biophysical trajectory

        // ── 2. FORMALLY REJECTED / FALSIFIED MODELS (STRICTLY DISABLED) ──
        // Model 1: Malthusian Static Subsistence Trap (Falsified by Boserup, Green Revolution, & Technological substitution)
        engines.put("MalthusianCapacityEngine", false);
        // Model 2: Nordhaus DICE Quadratic Climate Damage (Falsified by critical biophysical exergy collapse thresholds)
        engines.put("NordhausDicePureEngine", false);
        engines.put("NordhausDiceHybridEngine", false);
        // Model 3: Pinker Linear Pacification Narrative (Falsified by Turchin SDT secular instability cycles)
        engines.put("PinkerViolenceDeclinePureEngine", false);
        // Model 4: James Scott Static Anti-State Cereal Determinism (Falsified by Henrich cumulative cultural evolution)
        engines.put("ScottAgainstTheGrainPureEngine", false);
        // Model 5: Pure Geographic Determinism Diamond (Falsified by Acemoglu-Robinson institutional primacy)
        engines.put("DiamondGeographicDeterminismEngine", false);
        // Model 6: Psychohistory Rigid Determinism (Falsified science-fiction construct)
        engines.put("PsychohistoryPureEngine", false);
        // Model 7: Unregulated Hardin Tragedy of Commons (Falsified by Ostrom polycentric institutional arrangements)
        engines.put("HardinTragedyOfCommonsEngine", false);

        s.setTypeBEngineStates(engines);
        return s;
    }

    /**
     * Resolves the target telemetry output file path for the calibration run.
     *
     * @return file path for storing calibration telemetry time series
     */
    public static File getTelemetryOutputFile() {
        File dir = new File("data/variables/projections");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "ether_calibrated_projection_1d.json");
    }
}
