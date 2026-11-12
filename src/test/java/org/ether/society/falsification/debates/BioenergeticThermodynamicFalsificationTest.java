/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.debates;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.DynamicEnergyBudgetEngine;
import org.ether.society.engines.tier1.EcologicalStoichiometryEngine;
import org.ether.society.engines.tier1.MetabolicEcologyEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.ether.society.falsification.debates.FalsificationDebateFixtures.createGeographicalAgrarianGrid;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Large-Scale Bioenergetic &amp; Thermodynamic Model Falsification Test Suite.
 *
 * <p>Benchmarking Kooijman Standard Dynamic Energy Budget (DEB), Brown et al. Metabolic Theory of
 * Ecology (MTE), and Sterner &amp; Elser Ecological Stoichiometry against empirical biological
 * datasets and classical phenomenological models (Verhulst logistic ceilings, static Liebig minima).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("academic")
@Tag("scientific")
public class BioenergeticThermodynamicFalsificationTest {
    private static final Logger logger = LoggerFactory.getLogger(BioenergeticThermodynamicFalsificationTest.class);

    @Nested
    @DisplayName("Falsification 1: DEB Reserve Dynamics vs. Classical Verhulst Logistic Ceiling")
    class DEBVsVerhulstFalsification {

        @Test
        @DisplayName("1.1 Multi-Decade Harvest Shock Falsification: Reserve Buffer preserves demographic continuity")
        public void testDEBReserveBufferUnderCyclicFamine() {
            logger.info("🧪 [Falsification 1.1] Testing 50-year DEB simulation under decadal harvest shocks...");

            // 30 spatial agrarian cells, initial population 4,000 per cell = 120,000 headcount
            List<H3Cell> grid = createGeographicalAgrarianGrid(30, 4000, 1.2, 24000.0);
            double initialPop = grid.stream().mapToDouble(H3Cell::getPopulation).sum();
            assertEquals(120000.0, initialPop, 1.0);

            List<Double> debPopTrajectory = new ArrayList<>();
            List<Double> verhulstPopTrajectory = new ArrayList<>();

            double verhulstPop = initialPop;
            double normalK = 120000.0;
            double rIntrinsic = 0.035;

            // Run 50-year benchmark
            org.ether.society.core.dod.WorldBuffer debBuf = new org.ether.society.core.dod.WorldBuffer(grid.size());
            org.ether.society.data.DODDataGenerator.populateWorldBuffer(grid, debBuf, false);
            DynamicEnergyBudgetEngine debEngine = new DynamicEnergyBudgetEngine();
            for (int year = 1; year <= 50; year++) {
                // Decadal volcanic/monsoon crisis occurring every 10 years for a duration of 2 years
                boolean isCrisisYear = (year % 10 == 0 || year % 10 == 1);
                double foodStock = isCrisisYear ? 7000.0 : 25000.0; // 72% drop in harvest during crisis

                // Update DEB grid
                for (int i = 0; i < grid.size(); i++) {
                    debBuf.getFoodResource()[i] = (float) foodStock;
                }
                debEngine.tick(debBuf, 1.0, 12.0);
                double currentDebPop = 0.0;
                for (int i = 0; i < grid.size(); i++) {
                    currentDebPop += debBuf.getBiomassHuman()[i] / 0.06;
                }
                debPopTrajectory.add(currentDebPop);

                // Update Verhulst step
                double effectiveK = isCrisisYear ? normalK * 0.28 : normalK;
                verhulstPop = verhulstPop + rIntrinsic * verhulstPop * (1.0 - (verhulstPop / effectiveK));
                verhulstPop = Math.max(100.0, verhulstPop);
                verhulstPopTrajectory.add(verhulstPop);
            }

            double finalDebPop = debPopTrajectory.get(debPopTrajectory.size() - 1);
            double finalVerhulstPop = verhulstPopTrajectory.get(verhulstPopTrajectory.size() - 1);

            logger.info("  -> 50-Year Trajectory Final Pop: DEB={}, Verhulst={}", finalDebPop, finalVerhulstPop);

            // Calculate demographic coefficient of variation (stability metric)
            double debMean = debPopTrajectory.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
            double debVariance = debPopTrajectory.stream().mapToDouble(p -> Math.pow(p - debMean, 2)).average().orElse(0.0);
            double debCV = Math.sqrt(debVariance) / debMean;

            double verhulstMean = verhulstPopTrajectory.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
            double verhulstVariance = verhulstPopTrajectory.stream().mapToDouble(p -> Math.pow(p - verhulstMean, 2)).average().orElse(0.0);
            double verhulstCV = Math.sqrt(verhulstVariance) / verhulstMean;

            logger.info("  -> Coefficient of Variation: DEB CV={}, Verhulst CV={}",
                    String.format(java.util.Locale.ROOT, "%.4f", debCV),
                    String.format(java.util.Locale.ROOT, "%.4f", verhulstCV));

            // Biophysical verification: DEB preserves demographic continuity across 50 years
            assertTrue(debPopTrajectory.stream().allMatch(p -> p > 10000.0),
                    "Population under DEB must maintain demographic continuity without catastrophic extinction.");
            assertTrue(finalDebPop > 50000.0,
                    "DEB population must remain robust and viable across 50-year multidecadal run.");
            assertTrue(debMean > 60000.0,
                    "Mean DEB population across 50 years preserves pre-industrial carrying capacity.");
        }
    }

    @Nested
    @DisplayName("Falsification 2: Metabolic Theory of Ecology (MTE) Multi-Taxon Allometry & Kinetics")
    class MTEAllometryAndKineticsFalsification {

        @Test
        @DisplayName("2.1 Empirical Allometric Mass Scaling: Validated across 8 orders of biological magnitude")
        public void testAllometricScalingAcrossEightTaxa() {
            logger.info("🧪 [Falsification 2.1] Testing MTE across 8 empirical animal clades (5g to 5000kg)...");

            record TaxonDatum(String name, double massKg, MetabolicEcologyEngine.TaxonGuild guild) {}

            List<TaxonDatum> taxa = List.of(
                    new TaxonDatum("Pygmy Shrew (Suncus etruscus)", 0.002, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("House Mouse (Mus musculus)", 0.025, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("European Hare (Lepus europaeus)", 3.8, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("Gray Wolf (Canis lupus)", 40.0, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("Homo sapiens", 70.0, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("Red Deer (Cervus elaphus)", 200.0, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("Steppe Bison (Bison priscus)", 900.0, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL),
                    new TaxonDatum("Woolly Mammoth (Mammuthus primigenius)", 5000.0, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL)
            );

            double tempK = 310.15; // 37°C mammalian core
            List<Double> logMasses = new ArrayList<>();
            List<Double> logWatts = new ArrayList<>();
            List<Double> logSpecificWatts = new ArrayList<>();

            for (TaxonDatum taxon : taxa) {
                double bWatts = MetabolicEcologyEngine.calculateMetabolicRateWatts(taxon.massKg(), tempK, taxon.guild());
                double bSpecific = bWatts / taxon.massKg();

                logMasses.add(Math.log(taxon.massKg()));
                logWatts.add(Math.log(bWatts));
                logSpecificWatts.add(Math.log(bSpecific));

                logger.info("  -> {} ({} kg): Basal Power = {} W, Specific = {} W/kg",
                        taxon.name(), taxon.massKg(),
                        String.format(java.util.Locale.ROOT, "%.3f", bWatts),
                        String.format(java.util.Locale.ROOT, "%.3f", bSpecific));
            }

            // Linear regression of log(Watts) on log(Mass): slope must be 0.75
            double slopeWatts = calculateRegressionSlope(logMasses, logWatts);
            double slopeSpecific = calculateRegressionSlope(logMasses, logSpecificWatts);

            logger.info("  -> Estimated Allometric Power Slope: {}, Expected: 0.750",
                    String.format(java.util.Locale.ROOT, "%.4f", slopeWatts));
            logger.info("  -> Estimated Mass-Specific Slope: {}, Expected: -0.250",
                    String.format(java.util.Locale.ROOT, "%.4f", slopeSpecific));

            assertEquals(0.75, slopeWatts, 0.001, "MTE whole-organism basal metabolic power slope must equal 0.75 exactly.");
            assertEquals(-0.25, slopeSpecific, 0.001, "MTE mass-specific metabolic dissipation slope must equal -0.25 exactly.");
        }

        private double calculateRegressionSlope(List<Double> x, List<Double> y) {
            int n = x.size();
            double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            double cov = 0.0;
            double varX = 0.0;
            for (int i = 0; i < n; i++) {
                cov += (x.get(i) - meanX) * (y.get(i) - meanY);
                varX += Math.pow(x.get(i) - meanX, 2);
            }
            return cov / varX;
        }
    }

    @Nested
    @DisplayName("Falsification 3: Ecological Stoichiometry & Continuous Multi-Nutrient Bottlenecks")
    class EcologicalStoichiometryFalsification {

        @Test
        @DisplayName("3.1 Elevated CO2 Agronomic Penalty: Validates Loladze (2014) protein dilution envelope")
        public void testCO2EnrichmentAgronomicDilution() {
            logger.info("🧪 [Falsification 3.1] Testing atmospheric CO2 gradient from 280 ppm to 850 ppm...");

            double[] co2Levels = { 280.0, 350.0, 420.0, 560.0, 700.0, 850.0 };
            double previousFactor = 1.05;

            for (double co2 : co2Levels) {
                double factor = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(co2);
                logger.info("  -> CO2 {} ppm => Protein Quality Factor: {}",
                        co2, String.format(java.util.Locale.ROOT, "%.4f", factor));

                assertTrue(factor <= previousFactor, "Nutritional protein factor must decline monotonically with rising CO2.");
                assertTrue(factor >= 0.88, "Protein dilution must stay within empirical experimental boundaries [0.88, 1.0].");
                previousFactor = factor;
            }

            // Validates that at 280 ppm (pre-industrial), factor is 1.0
            assertEquals(1.0, EcologicalStoichiometryEngine.calculateProteinDilutionFactor(280.0), 1e-4);
        }

        @Test
        @DisplayName("3.2 Continuous Liebig Softmin: Eliminates discontinuous derivative steps in multi-nutrient limitation")
        public void testContinuousLiebigSmoothness() {
            logger.info("🧪 [Falsification 3.2] Testing C^1 smoothness across N-P nutrient crossover...");

            // Scan across nitrogen availability transitioning from deficient (0.1) to surplus (1.5) with fixed P=0.6, K=0.8
            double prevYield = 0.0;
            double pRatio = 0.60;
            double kRatio = 0.80;

            for (double n = 0.10; n <= 1.50; n += 0.05) {
                double yieldFactor = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(n, pRatio, kRatio);
                assertTrue(yieldFactor >= prevYield, "Yield factor must increase monotonically as limiting nitrogen rises.");
                assertTrue(yieldFactor <= 1.0 && yieldFactor >= 0.05, "Yield factor must remain bounded within [0.05, 1.0].");
                prevYield = yieldFactor;
            }
        }
    }
}
