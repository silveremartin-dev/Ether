/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.counterfactual;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.historical.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.ether.society.falsification.counterfactual.CounterfactualBenchmarkFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous Multi-Tick, Multi-Seed Counterfactual Empirical Validation Suite.
 *
 * <p>Executes deep spatial simulation runs on H3 planetary meshes (5,882 cells) across
 * hundreds of simulation ticks (years) to quantify the empirical macro-divergence between
 * Control (Engine OFF) and Treatment (Engine ON) runs against historical datasets (HYDE 3.4,
 * Maddison Project 2020, Seshat Databank, Vaclav Smil 2017, Allen 2001, EPICA CO2).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@DisplayName("🔬 Empirical Engine Counterfactual & Multi-Metric Benchmarking Suite")
@Tag("academic")
@Tag("scientific")
public class EmpiricalEngineCounterfactualValidationSuite {
    private static final Logger logger = LoggerFactory.getLogger(EmpiricalEngineCounterfactualValidationSuite.class);

    // =========================================================================
    // 1. WEST-BETTENCOURT URBAN ALLOMETRY BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("1. West-Bettencourt Urban Allometry Empirical Validation")
    class WestBettencourtValidation {

        @Test
        @DisplayName("Quantify super-linear capital output and sub-linear infrastructure scaling across 200 simulation years")
        void testWestBettencourtSpatialUrbanScaling() {
            logger.info("=== Executing West-Bettencourt Multi-Seed Spatial Benchmark ===");
            BenchmarkResult res = runWestBettencourtBenchmark();

            logger.info("West-Bettencourt Scaling Results: Mean Control = {}, Mean Treatment = {}, Cohen's d = {}, KS D = {}",
                    String.format(Locale.ROOT, "%.2f", res.meanControl()),
                    String.format(Locale.ROOT, "%.2f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()),
                    String.format(Locale.ROOT, "%.3f", res.ksDistance()));

            assertTrue(res.meanTreatment() > res.meanControl(), "Superlinear allometry must produce higher capital concentration in metropolitan hubs");
            assertTrue(Math.abs(res.cohensD()) > 0.8, "Effect size must be large (|Cohen's d| > 0.8) to validate macroeconomic urban scaling");
            assertTrue(res.ksDistance() > 0.4, "Kolmogorov-Smirnov distance must confirm distinct distribution divergence");
        }
    }

    // =========================================================================
    // 2. TAINTER COMPLEXITY COLLAPSE BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("2. Tainter Declining Marginal Returns & Institutional Collapse Validation")
    class TainterCollapseValidation {

        @Test
        @DisplayName("Demonstrate non-linear fiscal-energetic collapse when institutional maintenance exceeds economic surplus")
        void testTainterComplexityCollapseDynamics() {
            logger.info("=== Executing Tainter Complexity Collapse Counterfactual Benchmark ===");
            BenchmarkResult res = runTainterCollapseBenchmark();

            logger.info("Tainter Collapse Results: Mean Control = {}, Mean Treatment = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.2f", res.meanControl()),
                    String.format(Locale.ROOT, "%.2f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() < res.meanControl(), "Tainter complexity maintenance must erode bloated capital reserves faster than linear decay");
            assertTrue(Math.abs(res.cohensD()) > 1.2, "Effect size must be very large (Cohen's d > 1.2) representing imperial simplification collapse");
        }
    }

    // =========================================================================
    // 3. ARTHUR COMBINATORIAL TECHNOLOGY BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("3. Arthur Combinatorial Technological Evolution Validation")
    class ArthurTechValidation {

        @Test
        @DisplayName("Validate autocatalytic recombinant technology accumulation vs linear discovery model")
        void testArthurCombinatorialInnovation() {
            logger.info("=== Executing Arthur Combinatorial Technology Evolution Benchmark ===");
            BenchmarkResult res = runArthurTechBenchmark();

            logger.info("Arthur Technology Results: Mean Control = {}, Mean Treatment = {}, Cohen's d = {}",
                    res.meanControl(), res.meanTreatment(), res.cohensD());

            assertTrue(res.meanTreatment() > res.meanControl(), "Combinatorial recombinant synthesis must accelerate technological progress beyond linear discovery");
            assertTrue(Math.abs(res.cohensD()) > 0.8, "Effect size must exceed 0.8 for autocatalytic recombinant innovation");
        }
    }

    // =========================================================================
    // 4. KRUGMAN CORE-PERIPHERY GEOGRAPHIC AGGLOMERATION BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("4. Krugman Core-Periphery Agglomeration Validation")
    class KrugmanCorePeripheryValidation {

        @Test
        @DisplayName("Validate spontaneous symmetry breaking and core-periphery industrial divergence")
        void testKrugmanAgglomerationBifurcation() {
            logger.info("=== Executing Krugman Core-Periphery Agglomeration Benchmark ===");
            BenchmarkResult res = runKrugmanBenchmark();

            logger.info("Krugman Agglomeration Results: Gini Control = {}, Gini Treatment = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.4f", res.meanControl()),
                    String.format(Locale.ROOT, "%.4f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() >= res.meanControl(), "Krugman core-periphery forces must increase spatial wealth concentration (Gini)");
        }
    }

    // =========================================================================
    // 5. SPATIAL METAPOPULATION SEIR EPIDEMIOLOGY BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("5. Spatial Metapopulation SEIR Network Epidemic Validation")
    class SpatialEpidemiologyValidation {

        @Test
        @DisplayName("Validate pandemic wave attenuation and velocity along maritime trade routes")
        void testSpatialSEIREpidemicSpread() {
            logger.info("=== Executing Spatial Metapopulation SEIR Epidemic Benchmark ===");
            BenchmarkResult res = runSpatialSEIRBenchmark();

            logger.info("Spatial SEIR Results: Baseline Pop = {}, Dynamic SEIR Pop = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.0f", res.meanControl()),
                    String.format(Locale.ROOT, "%.0f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() <= res.meanControl(),
                    "Metapopulation disease transmission must capture excess epidemiological mortality");
        }
    }

    // =========================================================================
    // 6. SOIL SALINIZATION & IRRIGATION MASS BALANCE BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("6. Soil Salinization & Hydrological Mass-Balance Validation")
    class SoilSalinizationValidation {

        @Test
        @DisplayName("Demonstrate progressive food yield collapse in arid irrigated river basins (-2400 Sumerian Mesopotamia)")
        void testSoilSalinizationYieldCollapse() {
            logger.info("=== Executing Soil Salinization Mass-Balance Benchmark ===");
            BenchmarkResult res = runSoilSalinizationBenchmark();

            logger.info("Soil Salinization Results: Control Food = {}, Salinized Food = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.1f", res.meanControl()),
                    String.format(Locale.ROOT, "%.1f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() < res.meanControl(), "Irrigation without modern drainage in arid zones must degrade agricultural food yields");
            assertTrue(Math.abs(res.cohensD()) > 1.0, "Salinization effect size must be large (Cohen's d > 1.0)");
        }
    }

    // =========================================================================
    // 7. DRAFT ANIMAL TRACTION & FODDER ALLOCATION BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("7. Draft Animal Traction & Fodder Allocation Validation")
    class DraftAnimalFodderValidation {

        @Test
        @DisplayName("Validate capital productivity boost alongside fodder land competition in pre-industrial agriculture")
        void testDraftAnimalWorkAndFodderCompetition() {
            logger.info("=== Executing Draft Animal Fodder Allocation Benchmark ===");
            BenchmarkResult res = runDraftAnimalBenchmark();

            logger.info("Draft Animal Traction Results: Control Capital = {}, Animal Traction Capital = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.1f", res.meanControl()),
                    String.format(Locale.ROOT, "%.1f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() > res.meanControl(), "Draft animal mechanical traction must amplify agrarian capital output");
            assertTrue(Math.abs(res.cohensD()) > 0.8, "Traction effect size must exceed 0.8");
        }
    }

    // =========================================================================
    // 8. HOTELLING RESOURCE DEPLETION & SCARCITY RENT BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("8. Hotelling Non-Renewable Resource Depletion Validation")
    class HotellingOreDepletionValidation {

        @Test
        @DisplayName("Validate exhaustible metal deposit depletion and escalating capital scarcity rents")
        void testHotellingOreDepletion() {
            logger.info("=== Executing Hotelling Resource Depletion Benchmark ===");
            BenchmarkResult res = runHotellingBenchmark();

            logger.info("Hotelling Depletion Results: Control Metal = {}, Depleted Metal = {}, Cohen's d = {}",
                    String.format(Locale.ROOT, "%.1f", res.meanControl()),
                    String.format(Locale.ROOT, "%.1f", res.meanTreatment()),
                    String.format(Locale.ROOT, "%.3f", res.cohensD()));

            assertTrue(res.meanTreatment() < res.meanControl(), "Hotelling extraction must deplete accessible ore reserves over time");
            assertTrue(Math.abs(res.cohensD()) > 1.0, "Depletion effect size must be large (Cohen's d > 1.0)");
        }
    }

    // =========================================================================
    // 9. PRICE MULTILEVEL CULTURAL SELECTION BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("9. Price Multilevel Cultural Selection Validation")
    class PriceMultilevelSelectionValidation {

        @Test
        @DisplayName("Validate prosocial cohesion advantage in inter-polity competitive dynamics")
        void testPriceEquationCulturalSelection() {
            logger.info("=== Executing Price Multilevel Cultural Selection Benchmark ===");
            PriceMultilevelSelectionEngine engine = new PriceMultilevelSelectionEngine();
            ProceduralGenerator generator = new ProceduralGenerator();

            double[] capitalCooperative = new double[MONTE_CARLO_SEEDS.length];

            for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
                long seed = MONTE_CARLO_SEEDS[s];
                List<H3Cell> cells = generator.generatePlanet(createEarthPreset(seed));
                for (H3Cell c : cells) {
                    c.setPopulation(1_000);
                    c.setGiniIndex(0.20); // High prosociality / low inequality
                    c.setResourceCapital(200.0);
                }

                engine.process(cells, 1.0);
                capitalCooperative[s] = cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(200.0);
            }

            double meanCap = Arrays.stream(capitalCooperative).average().orElse(0.0);
            assertTrue(meanCap >= 200.0, "Prosocial cohesion must boost collective capital output via between-group selection advantage");
        }
    }

    // =========================================================================
    // 10. SCHELLING CULTURAL SPATIAL SEGREGATION BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("10. Schelling Cultural Spatial Segregation Validation")
    class SchellingSegregationValidation {

        @Test
        @DisplayName("Validate territorial movement friction increase under acute internal inequality/segregation")
        void testSchellingSpatialSegregationFriction() {
            logger.info("=== Executing Schelling Cultural Spatial Segregation Benchmark ===");
            SchellingAxelrodSegregationEngine engine = new SchellingAxelrodSegregationEngine();
            ProceduralGenerator generator = new ProceduralGenerator();

            List<H3Cell> cells = generator.generatePlanet(createEarthPreset(101L));
            for (H3Cell c : cells) {
                c.setPopulation(500);
                c.setGiniIndex(0.70); // Severe social polarization
                c.setMovementFriction(1.0);
            }

            engine.process(cells, 10.0);

            double avgFriction = cells.stream().mapToDouble(c -> c.getMovementFriction() != null ? c.getMovementFriction() : 1.0).average().orElse(1.0);
            assertTrue(avgFriction > 1.0, "High internal social polarization must elevate movement friction and territorial border tension");
        }
    }

    // =========================================================================
    // 11. STOMMEL 2-BOX AMOC THERMOHALINE CIRCULATION BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("11. Stommel AMOC Thermohaline Circulation Validation")
    class StommelAMOCValidation {

        @Test
        @DisplayName("Validate polar meltwater freshening tipping point triggering AMOC collapse")
        void testStommelAMOCCriticalBifurcation() {
            logger.info("=== Executing Stommel AMOC Bifurcation Benchmark ===");

            ThermohalineStommelAMOCEngine.StommelState baselineState =
                    ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 4.0, 36.5, 34.8);

            assertFalse(baselineState.isCollapsed(), "Baseline AMOC must be vigorous and active (> 15 Sv)");
            assertTrue(baselineState.amocFlowSv() >= 12.0, "Active Holocene AMOC must exceed 12 Sv");

            ThermohalineStommelAMOCEngine.StommelState pulseState =
                    ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 31.0);

            assertTrue(pulseState.isCollapsed(), "Massive polar freshening must trigger non-linear AMOC collapse");
            assertTrue(pulseState.northAtlanticCoolingShiftC() <= -5.0, "AMOC collapse must induce severe high-latitude cooling (<= -5°C)");
        }
    }

    // =========================================================================
    // 12. NET ENERGY EROEI CIVILIZATIONAL METABOLISM BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("12. Net Energy EROEI Civilizational Metabolism Validation")
    class NetEnergyEROEIValidation {

        @Test
        @DisplayName("Validate energy cliff and demographic contraction when net EROEI drops below unity")
        void testNetEnergyEROEIDemographicCliff() {
            logger.info("=== Executing Net Energy EROEI Benchmark ===");
            ProceduralGenerator generator = new ProceduralGenerator();

            List<H3Cell> cells = generator.generatePlanet(createEarthPreset(101L));
            for (H3Cell c : cells) {
                c.setPopulation(1_000);
                c.setResourceCapital(0.0); // No capital
                c.setBiomassNatural(0.0);  // Depleted biomass
                c.setResourceMetal(0.0);   // Depleted ore
            }

            int initialPop = cells.stream().mapToInt(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            NetEnergyEROEIEngine.processNetEnergyEROEI(cells);
            int postCollapsePop = cells.stream().mapToInt(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();

            assertTrue(postCollapsePop < initialPop, "Sub-unity EROEI must trigger rapid energetic starvation and population mortality");
        }
    }

    // =========================================================================
    // 13. THERMODYNAMIC WARFARE & KINETIC BREACHING BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("13. Thermodynamic Warfare & Kinetic Armor Breaching Validation")
    class ThermodynamicWarfareValidation {

        @Test
        @DisplayName("Validate kinetic exergy delivery exceeding structural material yield strength (MPa)")
        void testKineticWarfareBreaching() {
            logger.info("=== Executing Thermodynamic Warfare Benchmark ===");
            ProceduralGenerator generator = new ProceduralGenerator();

            List<H3Cell> cells = generator.generatePlanet(createEarthPreset(101L));
            for (H3Cell c : cells) {
                c.setPopulation(50_000); // Massive army
                c.setResourceCapital(10_000.0); // Fortified infrastructure
            }

            ThermodynamicWarfareEngine.processKineticWarfare(cells, 1.0);

            double remainingCapital = cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            assertTrue(remainingCapital < 10_000.0, "Kinetic energy delivery exceeding yield strength must erode capital infrastructure");
        }
    }

    // =========================================================================
    // 14. MEGAFAUNA OVERKILL & TROPHIC FEEDBACK BENCHMARK
    // =========================================================================
    @Nested
    @DisplayName("14. Megafauna Overkill & Trophic Ecosystem Feedback Validation")
    class MegafaunaOverkillValidation {

        @Test
        @DisplayName("Validate human hunting pressure ratio driving wild biomass extinction and biome shift")
        void testMegafaunaOverkillCollapse() {
            logger.info("=== Executing Megafauna Overkill Benchmark ===");
            ProceduralGenerator generator = new ProceduralGenerator();

            List<H3Cell> cells = generator.generatePlanet(createEarthPreset(101L));
            for (H3Cell c : cells) {
                c.setBiome(org.ether.society.model.Biome.PLAINS);
                c.setPopulation(500); // High human hunter density
                c.setBiomassNatural(50.0); // Vulnerable megafauna stock
                c.setSoilOrganicCarbon(6.0);
            }

            MegafaunaEcosystemEngine.processMegafaunaEcosystem(cells);

            double avgBiomass = cells.stream().mapToDouble(c -> c.getBiomassNatural() != null ? c.getBiomassNatural() : 0.0).average().orElse(50.0);
            assertTrue(avgBiomass < 50.0, "High human hunting pressure must deplete natural megafauna biomass");
        }
    }

    // =========================================================================
    // 15. MULTI-METRIC MACRO-HISTORICAL TRAJECTORY BENCHMARK (HYDE 3.4 & MADDISON)
    // =========================================================================
    @Nested
    @DisplayName("15. Multi-Metric Historical Telemetry Calibration vs Empirical Benchmarks")
    class MultiMetricHistoricalBenchmark {

        @Test
        @DisplayName("Validate composite R^2 >= 0.85 and RMSE minimization across canonical historical epochs")
        void testMultiMetricBenchmarkFit() {
            logger.info("=== Executing Multi-Metric Macro-Historical Validation using HistoricalScenarioCalibrationHarness ===");

            var scenario = org.ether.society.analytics.HistoricalScenarioCalibrationHarness.CALIBRATION_SCENARIOS.get(3); // SECOND_INDUSTRIAL_ACCELERATION (1850 -> 1910)
            var result = org.ether.society.analytics.HistoricalScenarioCalibrationHarness.runScenarioCalibration(scenario);

            assertNotNull(result, "Scenario calibration result must not be null");
            logger.info("Scenario '{}': Composite R^2 = {}, RMSE = {}, Mean MAPE = {}%",
                    result.scenario.displayName(), result.compositeRSquared, result.compositeRmse, result.meanMape);

            assertTrue(result.compositeRSquared >= 0.85, "Composite R^2 across empirical series must exceed 0.85 (Got: " + result.compositeRSquared + ")");
            assertFalse(result.simulatedPopulationTrajectory.isEmpty(), "Trajectory must contain simulated points");
            assertTrue(result.meanMape < 15.0, "Mean MAPE must remain < 15% across historical metrics (Got: " + result.meanMape + "%)");
            assertTrue(result.compositeRmse < 2000.0, "Composite multi-metric RMSE must be bounded (Got: " + result.compositeRmse + ")");
        }
    }
}
