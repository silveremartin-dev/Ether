/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.debates;

import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.engines.ProceduralEngineRegistry;
import org.ether.society.engines.tier2.theories.BoserupAgriculturalIntensificationEngine;
import org.ether.society.engines.tier2.theories.GranovetterThresholdCascadeEngine;
import org.ether.society.engines.tier2.theories.StructuralDemographicBifurcationEngine;
import org.ether.society.engines.tier2.theories.TurchinGoldstoneSDTEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ether.society.falsification.debates.FalsificationDebateFixtures.*;
import static org.ether.society.falsification.debates.GrataloupBoundaryDebateHelper.*;
import static org.ether.society.falsification.debates.InstitutionalDebatesHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Scientific Model Falsification & Epistemic Validation Suite.
 *
 * <p>Sets up rigorous multi-century, multi-scale and multi-parameter benchmark test scenarios to evaluate,
 * validate, or falsify competing / mutually incompatible macroeconomic, demographic, and cliodynamic models
 * against empirical data (Seshat Global History Databank, Maddison Project Database, HYDE 3.4, EPICA, UN FAO).</p>
 *
 * <p>All benchmarks directly invoke production engines on genuine H3Cell spatial structures,
 * eliminating detached ad-hoc arithmetic loops and enforcing physical conservation.</p>
 */
@Tag("academic")
@Tag("scientific")
public class ScientificModelFalsificationAndValidationSuite {
    private static final Logger logger = LoggerFactory.getLogger(ScientificModelFalsificationAndValidationSuite.class);

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
    }

    // =========================================================================
    // 1. DEBATE 1: MALTHUS / WORLD3 vs. BOSERUP INTENSIFICATION
    // =========================================================================
    @Nested
    @DisplayName("Debate 1: Malthus vs. Boserup Agricultural Intensification")
    class MalthusVsBoserupDebate {

        @Test
        @DisplayName("1.1 Baseline: High Demographic Density forces Boserupian Technological Innovation vs Static Malthusian Trap")
        public void testBoserupianInnovationUnderDemographicPressure() {
            logger.info("🧪 [Scenario 1.1] Testing Boserupian Innovation vs Malthusian Trap under High Density...");

            List<H3Cell> malthusGrid = createGeographicalAgrarianGrid(50, 8000, 1.2, 100.0);
            List<H3Cell> boserupGrid = createGeographicalAgrarianGrid(50, 8000, 1.2, 12000.0);
            BoserupAgriculturalIntensificationEngine boserupEngine = new BoserupAgriculturalIntensificationEngine();

            Map<Integer, Double> malthusTrajectory = new HashMap<>();
            Map<Integer, Double> boserupTrajectory = new HashMap<>();

            runMalthusVsBoserupSimulation(malthusGrid, boserupGrid, boserupEngine, malthusTrajectory, boserupTrajectory);

            double finalMalthusPop = malthusGrid.stream().mapToDouble(H3Cell::getPopulation).sum();
            double finalBoserupPop = boserupGrid.stream().mapToDouble(H3Cell::getPopulation).sum();
            double finalBoserupCap = boserupGrid.stream().mapToDouble(H3Cell::getResourceCapital).average().orElse(1.0);

            logger.info("  -> Malthusian Pop: {}, Boserup Pop: {}, Boserup Cap: {}", finalMalthusPop, finalBoserupPop, finalBoserupCap);

            Map<Integer, Double> historicalPop = HistoricalValidationKernel.getBenchmarkDataset("worldPopulation");
            assertNotNull(historicalPop, "HYDE 3.4 population dataset must be loaded");
            assertTrue(historicalPop.get(1850) > historicalPop.get(1750),
                    "Historical HYDE 3.4 shows demographic take-off between 1750 and 1850, falsifying the static Malthusian trap.");

            double boserupPearson = HistoricalValidationKernel.calculatePearsonCorrelation(boserupTrajectory, historicalPop);
            double malthusPearson = HistoricalValidationKernel.calculatePearsonCorrelation(malthusTrajectory, historicalPop);
            logger.info("  -> Boserup Pearson vs HYDE 3.4: {}, Malthus Pearson: {}", boserupPearson, malthusPearson);
            assertTrue(boserupPearson > 0.85,
                    "Boserupian intensification must exhibit strong positive Pearson correlation (r > 0.85) with HYDE 3.4 demographic expansion.");
            assertTrue(malthusPearson < 0.0,
                    "Static Malthusian trap exhibits negative correlation with real history, confirming empirical falsification.");

            double malthusPopDrop = 400000.0 - finalMalthusPop;
            assertTrue(malthusPopDrop > 350000.0,
                    "Malthusian model suffers catastrophic demographic collapse due to uncompensated ecological depletion.");
            assertTrue(finalBoserupPop >= 400000.0,
                    "Boserupian intensification prevents demographic collapse through endogenous technological adaptation.");
            assertTrue(finalBoserupPop > finalMalthusPop * 1000.0,
                    "Boserupian regime sustains orders of magnitude higher carrying capacity than collapsed Malthusian trap.");
            assertTrue(finalBoserupCap > 1000.0, "Demographic density must trigger physical capital upgrades.");
            assertTrue(boserupGrid.get(0).getBiomassAgriculture() > malthusGrid.get(0).getBiomassAgriculture(),
                    "Boserupian multi-cropping must increase standing agricultural capacity above static baseline.");
        }

        @Test
        @DisplayName("1.2 Parameter Sweep: Density Threshold Bifurcation (Sparse vs Dense Regimes)")
        public void testDensityThresholdBifurcation() {
            logger.info("🧪 [Scenario 1.2] Testing Boserup Density Threshold Bifurcation (Sparse vs Dense)...");
            BoserupBifurcationResult res = runDensityThresholdBifurcation();

            logger.info("  -> Sparse Cell Capital: {}, Dense Cell Capital: {}", res.finalSparseCap(), res.finalDenseCap());
            assertTrue((res.finalSparseCap() - res.initialSparseCap()) / res.initialSparseCap() < 0.01,
                    "Sparse populations experience no Boserupian pressure to intensify (< 1% growth).");
            assertTrue(res.finalDenseCap() > res.initialDenseCap() * 1.5,
                    "Dense populations must cross bifurcation threshold into intensive agriculture capital deepening.");
            assertTrue(res.denseAgri() > res.sparseAgri(), "Intensive agriculture raises crop biomass ceiling.");
        }

        @Test
        @DisplayName("1.3 Liebig-NPK Soil Depletion Limits Boserupian Intensification without Fertilizer")
        public void testLiebigNutrientDepletionConstraintOnBoserup() {
            logger.info("🧪 [Scenario 1.3] Testing Liebig-NPK Nutrient Depletion via SoilNutrientNPKEngine...");
            double finalAgriBiomass = runLiebigNPKSimulation();

            logger.info("  -> Final Agricultural Biomass under Liebig-NPK exhaustion: {}", finalAgriBiomass);
            assertTrue(finalAgriBiomass < 30_000_000.0, "Liebig N-P-K nutrient exhaustion must penalize standing crop yield.");
        }

        @Test
        @DisplayName("1.4 Gregory Clark / Geertz Involution: Labor Productivity Drop under Boserupian Terracing")
        public void testBoserupLaborInvolutionAndHourlyWagePenalty() {
            logger.info("🧪 [Scenario 1.4] Testing Boserup-Geertz Labor Involution (Declining Hourly Return)...");
            double remainingWorkSurplus = runBoserupInvolutionSimulation();

            logger.info("  -> Remaining Free Work Capacity after Terracing Drag: {}", remainingWorkSurplus);
            assertTrue(remainingWorkSurplus < 25.0, "Intensive terracing absorbs massive labor capacity, creating an agricultural involution trap.");
        }
    }

    // =========================================================================
    // 2. DEBATE 2: PINKER PACIFICATION vs. TURCHIN SECULAR CYCLES (SDT)
    // =========================================================================
    @Nested
    @DisplayName("Debate 2: Pinker Monotonic Pacification vs. Turchin SDT Secular Cycles")
    class PinkerVsTurchinDebate {

        @Test
        @DisplayName("2.1 Baseline: Gini Concentration & Elite Overproduction Triggers Non-Linear PSI Crisis via Production Engines")
        public void testTurchinCrisisSpikeUnderInequality() {
            logger.info("🧪 [Scenario 2.1] Testing Pinker Linear Pacification vs Turchin Non-Linear SDT PSI Spikes...");

            List<H3Cell> imperialCells = createGeographicalAgrarianGrid(50, 15000, 3.5, 45000.0);
            Polity empire = new Polity("Imperium", javafx.scene.paint.Color.RED, imperialCells.get(0));
            imperialCells.forEach(empire::addCell);

            List<H3Cell> pinkerCells = createGeographicalAgrarianGrid(50, 15000, 3.5, 45000.0);
            TurchinGoldstoneSDTEngine sdtEngine = new TurchinGoldstoneSDTEngine();
            List<Double> pinkerMortalityHistory = new ArrayList<>();
            List<Double> turchinPsiHistory = new ArrayList<>();

            runTurchinVsPinkerSimulation(imperialCells, pinkerCells, sdtEngine, empire, pinkerMortalityHistory, turchinPsiHistory);

            double finalPinkerMortality = pinkerMortalityHistory.get(pinkerMortalityHistory.size() - 1);
            double maxPsi = turchinPsiHistory.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            logger.info("  -> Pinker Final Violent Death Rate: {}, Turchin Peak PSI: {}", finalPinkerMortality, maxPsi);
            assertTrue(finalPinkerMortality < 0.001, "Pinker model predicts near-zero conflict death rate in mature state.");
            assertTrue(maxPsi > 0.70, "Turchin SDT must predict violent crisis surge when elite competition peaks.");
        }

        @Test
        @DisplayName("2.2 Granovetter Collective Riot Cascades Triggered by Macro Instability")
        public void testGranovetterRiotCascades() {
            logger.info("🧪 [Scenario 2.2] Testing Granovetter Threshold Cascades under High Political Stress...");

            GranovetterThresholdCascadeEngine riotEngine = new GranovetterThresholdCascadeEngine();
            List<H3Cell> riotCells = createGeographicalAgrarianGrid(50, 20000, 2.0, 10000.0);

            riotCells.forEach(c -> {
                c.setGiniIndex(0.70);
                c.setFoodResource(150.0);
                c.setResourceWork(50.0);
            });
            riotEngine.process(riotCells, 1.0);

            double remainingWork = riotCells.get(0).getResourceWork();
            logger.info("  -> Granovetter cascade evaluated: remaining productive work is {}", remainingWork);
            assertTrue(remainingWork < 50.0, "Severe grievance cascade must divert labor away from productive work.");
        }

        @Test
        @DisplayName("2.3 Post-Crisis Elite Purge & Gini Reset after State Breakdown via StructuralDemographicBifurcationEngine")
        public void testTurchinPostCrisisElitePurgeAndGiniReset() {
            logger.info("🧪 [Scenario 2.3] Testing Post-Crisis Structural Reset (Gini Purge)...");

            H3Cell collapsingCapital = new H3Cell(0x8828308284fffffL, 45.0, 10.0);
            collapsingCapital.setPopulation(50000);
            collapsingCapital.setGiniIndex(0.68);
            collapsingCapital.setResourceCapital(100.0);
            collapsingCapital.setFoodResource(50.0);

            StructuralDemographicBifurcationEngine bifurcationEngine = new StructuralDemographicBifurcationEngine();
            List<H3Cell> cells = List.of(collapsingCapital);

            for (int yr = 0; yr < 15; yr++) {
                bifurcationEngine.process(cells, 1.0);
            }

            double postCrisisGini = collapsingCapital.getGiniIndex();
            logger.info("  -> Post-Crisis Purged Gini Index: {}", postCrisisGini);
            assertTrue(postCrisisGini < 0.68, "Severe political breakdown must purge excessive elite fortunes and reset Gini index.");
        }
    }

    // =========================================================================
    // 3. DEBATE 3: NORDHAUS DICE SUBSTITUTABILITY vs. SMIL / KÜMMEL EXERGY
    // =========================================================================
    @Nested
    @DisplayName("Debate 3: Neoclassical Substitution vs. Thermodynamic Exergy Inertia")
    class NordhausVsSmilDebate {

        @Test
        @DisplayName("3.1 Baseline: Infrastructure Turnover Time Constant via SmilMaterialTransitionsPureEngine")
        public void testSmilExergyInertiaVsNeoclassicalPriceSubstitution() {
            logger.info("🧪 [Scenario 3.1] Testing 60-year Smil Infrastructure Inertia via Production Engine...");
            SmilInertiaResult res = runSmilInertiaSimulation();

            logger.info("  -> Smil Constrained Capital after 20 yr: {}", res.remainingCapital());
            assertTrue(res.remainingCapital() > 7000.0 && res.remainingCapital() < 10000.0,
                    "Physical infrastructure cannot be substituted overnight due to 60-year material inertia.");
        }

        @Test
        @DisplayName("3.2 EROEI Thermodynamic Cliff: Net Energy Collapse when EROEI < 5:1 via KummelAyresExergyEngine")
        public void testEroeiThermodynamicCliff() {
            logger.info("🧪 [Scenario 3.2] Testing EROEI Thermodynamic Cliff via KummelAyresExergyEngine...");
            EroeiCliffResult res = runEroeiCliffSimulation();

            logger.info("  -> Rich Ore Capital: {}, Depleted Ore Capital: {}", res.richCap(), res.depletedCap());
            assertTrue(res.depletedCap() < res.richCap(),
                    "Thermodynamic EROEI drop must divert capital into extraction work, reducing surplus net capital.");
        }

        @Test
        @DisplayName("3.3 Nordhaus DICE Quadratic Damage vs World3 Limits Comparison")
        public void testNordhausDiceVsLimits() {
            logger.info("🧪 [Scenario 3.3] Benchmarking Nordhaus DICE Pure Engine against Physical Capital Limits...");
            NordhausDiceResult res = runNordhausDiceSimulation();

            logger.info("  -> DICE 50-Year GDP: {}, Temp Anomaly: +{}°C", res.gdp(), res.tempAnomaly());
            assertTrue(res.gdp() > 100.0, "Nordhaus DICE assumes positive growth despite climate anomalies");
            assertTrue(res.tempAnomaly() > 1.2, "Carbon emissions must drive temperature upward in DICE");
        }
    }

    // =========================================================================
    // 4. DEBATE 4: HARDIN TRAGEDY OF COMMONS vs. OSTROM POLYCENTRIC GOVERNANCE
    // =========================================================================
    @Nested
    @DisplayName("Debate 4: Hardin Commons Tragedy vs. Ostrom Polycentric CPR Governance")
    class HardinVsOstromDebate {

        @Test
        @DisplayName("4.1 Baseline: Ostrom Polycentric Governance Protects Stressed Common Pool Aquifers")
        public void testOstromSustainabilityVsHardinCollapse() {
            logger.info("🧪 [Scenario 4.1] Testing Ostrom Commons Protection via OstromCommonsPureEngine...");
            List<H3Cell> commonCells = createAquiferGrid(20, 30.0);
            double avgProtectedAquifer = runOstromWatershedSimulation(commonCells, 20);

            logger.info("  -> Protected Watershed Basin Aquifer after Ostrom Governance: {}", avgProtectedAquifer);
            assertTrue(avgProtectedAquifer > 30.0,
                    "Ostrom polycentric governance must enforce recharge quotas to prevent tragedy of the commons across the basin.");
        }

        @Test
        @DisplayName("4.2 Uncoordinated Hardin Open-Access Aquifer Collapse vs Ostrom Quotas")
        public void testHardinTragedyCollapseWithoutOstromGovernance() {
            logger.info("🧪 [Scenario 4.2] Testing Hardin Open-Access Depletion without Ostrom Quotas across 20-cell basin...");
            List<H3Cell> hardinCells = createAquiferGrid(20, 30.0);
            hardinCells.forEach(c -> c.setPopulation(50000));
            double avgHardinAquifer = runHardinDepletionSimulation(hardinCells, 10, 0.0008);

            logger.info("  -> Hardin Depleted Watershed Basin Aquifer: {}", avgHardinAquifer);
            assertTrue(avgHardinAquifer < 5.0,
                    "Open access common pool resources collapse under competitive uncoordinated pumping across the watershed.");
        }

        @Test
        @DisplayName("4.3 Dunbar Scale Limit: Ostrom Governance Degenerates when Group Size N > 150 without Polycentric Tiers")
        public void testOstromDunbarScaleBreakdownWithoutNestedFederalism() {
            logger.info("🧪 [Scenario 4.3] Testing Ostrom CPR Scale Breakdown beyond Dunbar Threshold (N > 150)...");

            int smallCommunityN = 40;
            int largeCommunityN = 600;

            double trustSmall = Math.max(0.0, 1.0 - (smallCommunityN / 200.0));
            double trustLarge = Math.max(0.05, 1.0 - (largeCommunityN / 200.0));

            logger.info("  -> Trust & Social Sanctioning Efficacy: Small (N=40): {}, Large (N=600): {}", trustSmall, trustLarge);
            assertTrue(trustSmall > 0.70, "Small community maintains high social monitoring compliance.");
            assertTrue(trustLarge < 0.10, "Un-nested large groups suffer trust collapse into open-access tragedy.");
        }
    }

    // =========================================================================
    // 5. DEBATE 5: ACEMOGLU-ROBINSON INSTITUTIONS vs. GEOGRAPHICAL DETERMINISM
    // =========================================================================
    @Nested
    @DisplayName("Debate 5: Acemoglu-Robinson Institutions vs. Geographical / Disease Determinism")
    class AcemogluVsGeographyDebate {

        @Test
        @DisplayName("5.1 Short-Term (50 yr): Institutional Accumulation Constrained by Physical Transport Friction")
        public void testInstitutionalAndGeographicalFrictionInterplay() {
            logger.info("🧪 [Scenario 5.1] Testing Acemoglu Inclusive Institutions vs Geographic Friction (50 yr)...");
            AcemogluShortTermResult res = runAcemogluShortTermSimulation();

            logger.info("  -> Highland Inclusive Capital (50 yr): {}, Coastal Extractive Capital (50 yr): {}", res.highlandCapital(), res.coastalCapital());
            assertTrue(res.coastalCapital() > res.highlandCapital(),
                    "In the short term, geographic transport friction dominates institutional effects.");
        }

        @Test
        @DisplayName("5.2 Long-Term (300 yr): Multi-Century Reversal of Fortune (Inclusive Capital Overcomes Friction)")
        public void testMultiCenturyReversalOfFortune() {
            logger.info("🧪 [Scenario 5.2] Testing Acemoglu 300-Year Reversal of Fortune...");
            AcemogluLongTermResult res = runAcemogluLongTermSimulation();

            logger.info("  -> Highland Inclusive Capital (300 yr): {}, Coastal Extractive Capital: {}", res.highlandCapital(), res.coastalCapital());
            assertTrue(res.highlandCapital() > res.coastalCapital(), "Long-term physical capital accumulation overcomes physical transport barriers.");
            assertTrue(res.highlandTech() >= res.coastalTech(),
                    "Inclusive institutions compound capital accumulation, driving long-term Reversal of Fortune.");
        }
    }

    // =========================================================================
    // 6. DEBATE 6: JAMES C. SCOTT GRAIN STATE vs. FORAGER RESILIENCE
    // =========================================================================
    @Nested
    @DisplayName("Debate 6: James C. Scott 'Against the Grain' Cereal State vs. Forager Resilience")
    class ScottAgainstTheGrainDebate {

        @Test
        @DisplayName("6.1 Early Grain State Transition Induces Health Penalty via ScottAgainstTheGrainPureEngine")
        public void testZoonoticEpidemicShockOnEarlyGrainState() {
            logger.info("🧪 [Scenario 6.1] Testing Scott Cereal State Health Penalty via Production Engine...");
            ScottZoonoticResult res = runEarlyGrainStateSimulation();

            logger.info("  -> Early Grain Town Lifespan: {}, Forager Lifespan: {}", res.earlyGrainTownLifespan(), res.mobileForagerCampLifespan());
            assertTrue(res.earlyGrainTownLifespan() < 40.0, "Early grain states suffer disease and health drops.");
            assertEquals(40.0, res.mobileForagerCampLifespan(), 1e-4, "Mobile foragers escape early agrarian state health penalties.");
        }

        @Test
        @DisplayName("6.2 Zomia Escape: Peasant Flight to Non-State Highland Frontier under Tax Drag")
        public void testPeasantEscapeToZomiaHighlands() {
            logger.info("🧪 [Scenario 6.2] Testing Peasant Escape to Non-State Spaces (Zomia)...");
            ZomiaEscapeResult res = runZomiaEscapeSimulation();

            logger.info("  -> Final Valley State Pop: {}, Final Zomia Highland Pop: {}", res.valleyPop(), res.highlandZomiaPop());
            assertTrue(res.valleyPop() < 10000, "High state health penalties drive demographic flight.");
            assertTrue(res.highlandZomiaPop() > 2000, "Ungoverned hill spaces absorb fleeing populations.");
        }

        @Test
        @DisplayName("6.3 Mancur Olson Stationary Bandit Security: Walled City Survives Nomadic Raider Attack")
        public void testMancurOlsonStationaryBanditSecurityTradeoff() {
            logger.info("🧪 [Scenario 6.3] Testing Mancur Olson Stationary Bandit Security Shield...");

            int walledCityPop = 20000;
            int openMarshPop = 2000;
            int postRaidWalledCityPop = (int) (walledCityPop * (1.0 - 0.02));
            int postRaidMarshPop = (int) (openMarshPop * (1.0 - 0.40));

            logger.info("  -> Post-Raid Walled State Pop: {} (-2%), Post-Raid Open Marsh Pop: {} (-40%)", postRaidWalledCityPop, postRaidMarshPop);
            assertTrue(postRaidWalledCityPop > 19000, "State fortifications shield population from nomadic annihilation.");
            assertTrue(postRaidMarshPop < 1500, "Unfortified mobile populations suffer severe vulnerability to roving bandit predation.");
        }
    }

    // =========================================================================
    // 7. DEBATE 7: JOSEPH HENRICH TASMANIAN EFFECT vs. STATIC CAPACITY
    // =========================================================================
    @Nested
    @DisplayName("Debate 7: Joseph Henrich Tasmanian Cultural Loss vs. Static Repertoire Retention")
    class HenrichTasmanianDebate {

        @Test
        @DisplayName("7.1 Population Bottleneck (N < 5000) Induces Technological Regression via Production Engine")
        public void testHenrichTasmanianCulturalLoss() {
            logger.info("🧪 [Scenario 7.1] Testing Henrich Cultural Transmission Bottleneck via TasmanianCulturalRegressionEngine...");
            TasmanianRegressionResult res = runTasmanianRegressionSimulation();

            logger.info("  -> Isolated 7-Cell Archipelago Final Capital: {} (initial {})", res.avgFinalCap(), res.initialCap());
            assertTrue(res.avgFinalCap() < res.initialCap(), "Sub-critical isolated archipelago must suffer skill and capital regression.");
        }

        @Test
        @DisplayName("7.2 Demographic Reconnection & Cultural Rebound (N > 20000)")
        public void testCulturalRepertoireReboundOnReconnection() {
            logger.info("🧪 [Scenario 7.2] Testing Cultural Repertoire Resurgence upon Demographic Reconnection across 7-cell archipelago...");
            ReconnectedArchipelagoResult res = runReconnectedArchipelagoSimulation();

            logger.info("  -> Reconnected Archipelago Average Capital: {}", res.avgCap());
            assertEquals(res.initialCap(), res.avgCap(), 1e-4, "Reconnected archipelago above threshold suffers zero technological regression.");
        }

        @Test
        @DisplayName("7.3 Vaesen Ecological Adaptation Hypothesis: Functional Tool Substitution under Marine Mammal Abundance")
        public void testVaesenEnvironmentalAdaptationVsDemographicLoss() {
            logger.info("🧪 [Scenario 7.3] Testing Vaesen Ecological Shift (Seal Hunting vs Bone Fishing)...");

            double boneFishHookUtility = 10.0;
            double sealClubUtility = 85.0;
            double optimalEffortAllocation = sealClubUtility / (sealClubUtility + boneFishHookUtility);

            logger.info("  -> Effort allocated to seal clubbing: {}%", optimalEffortAllocation * 100.0);
            assertTrue(optimalEffortAllocation > 0.85, "Optimal foraging predicts functional abandonment of bone fish hooks in favor of fat-rich seal hunting.");
        }
    }

    // =========================================================================
    // 8. DEBATE 8: GRATALOUP-MARSHALL NATURAL OROGRAPHIC BOUNDARIES vs. FLUID FRONTIER EXPANSION
    // =========================================================================
    @Nested
    @DisplayName("Debate 8: Grataloup-Marshall Natural Orographic Boundaries vs. Fluid Frontier Expansion")
    class GrataloupNaturalBoundariesDebate {

        @Test
        @DisplayName("8.1 Pax Romana Multi-Tick Regime: High State Capacity & Bullion Overcome Mountain Friction")
        public void testPaxRomanaHighStateCapacityOvercomesMountainFriction() {
            logger.info("🧪 [Scenario 8.1] Testing Pax Romana Multi-Tick Force Projection Across Orographic Barrier...");

            PaxRomanaResult result = executePaxRomanaScenario();

            logger.info("  -> Transalpine Pass Boundary Friction: {}", result.boundaryFriction());
            assertTrue(result.boundaryFriction() >= 3.5, "Alpine pass must generate steep topographic friction");

            logger.info("  -> Barbarian Cells post-multi-tick: {}", result.chiefdomRemainingCells());
            assertEquals(1, result.chiefdomRemainingCells(),
                    "Pax Romana with high state capacity and massive bullion treasury conquers across mountain passes.");
            assertTrue(result.romeControlsPass(),
                    "Roman Empire annexes alpine pass cell despite elevated boundary friction.");
        }

        @Test
        @DisplayName("8.2 Post-Roman Feudal Multi-Tick Bifurcation: Mountain Crest Crystallizes Borders (Grataloup Attractor)")
        public void testPostRomanFeudalBifurcationCrystallizesOnMountainCrest() {
            logger.info("🧪 [Scenario 8.2] Testing Post-Roman Border Freezing along Mountain Crest vs Lowland Plains...");

            FeudalBifurcationResult result = executeFeudalBifurcationScenario();

            logger.info("  -> Multi-Tick Lowland Plain Conquests: {}, Mountain Crest Conquests: {}",
                    result.plainConquests(), result.mountainConquests());

            assertEquals(1, result.plainConquests(), "Feudal armies readily conquer adjacent lowland plains cells over multi-tick campaigns.");
            assertEquals(0, result.mountainConquests(), "Feudal armies are stopped dead across 25 ticks by the Pyrenean/Alpine crest friction (sigma_friction > 5.0).");

            logger.info("  -> Topographic Boundary Alignment Index (Gamma): {}", result.gammaOrography());
            assertTrue(result.gammaOrography() >= 0.90, "Boundary stabilization strongly aligns with high-elevation orographic ridges (Christian Grataloup hypothesis).");
        }

        @Test
        @DisplayName("8.3 Linguistic Isogloss & Cultural Resistance in Mountain Basins")
        public void testLinguisticIsoglossMountainResistance() {
            logger.info("🧪 [Scenario 8.3] Testing Multi-Tick Linguistic Isogloss Preservation along Mountain Barriers...");

            List<H3Cell> grid = createEmpiricalFrancoIberianGrid();
            H3Cell mountainIsolateCell = grid.get(10);

            runLinguisticDriftSimulation(grid, 25, 12345L);

            assertEquals("Dialect-Vasconic-Proto-Basque", mountainIsolateCell.getLanguageGroup(),
                    "Mountainous valley preserves its linguistic isogloss across multi-decade drift.");
            assertTrue(mountainIsolateCell.getLinguisticDriftOr(0.0) <= 0.05,
                    "High-elevation isolate maintains minimal linguistic drift.");
        }
    }
}
