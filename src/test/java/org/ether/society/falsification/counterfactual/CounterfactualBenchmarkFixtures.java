/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.counterfactual;

import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.theories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Benchmark fixtures, Monte Carlo execution runners, and statistical metrics
 * for empirical engine counterfactual validation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class CounterfactualBenchmarkFixtures {

    private static final Logger logger = LoggerFactory.getLogger(CounterfactualBenchmarkFixtures.class);

    public static final long[] MONTE_CARLO_SEEDS = { 101L, 202L, 303L, 404L, 505L };

    private CounterfactualBenchmarkFixtures() {
        // Prevent instantiation
    }

    /**
     * Composite benchmark outcome containing paired control/treatment series and effect size statistics.
     *
     * @param control raw control distribution across seeds
     * @param treatment raw treatment distribution across seeds
     * @param meanControl mean value of control series
     * @param meanTreatment mean value of treatment series
     * @param cohensD Cohen's d effect size statistic
     * @param ksDistance Kolmogorov-Smirnov D statistic (0.0 if not computed)
     */
    public record BenchmarkResult(
            double[] control,
            double[] treatment,
            double meanControl,
            double meanTreatment,
            double cohensD,
            double ksDistance
    ) {}

    public static PlanetPreset createEarthPreset(long seed) {
        return new PlanetPreset(
                "Terre Planetary Res2", 2, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, seed, 1.0, 1.0, 0.35, 40.0, 21.0, 0.30, 1.0,
                false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
                false, "none", false, "", 12445L + seed, false, "", 13345L + seed, false, "", 14345L + seed);
    }

    public static List<H3Cell> cloneCellGrid(List<H3Cell> source) {
        List<H3Cell> cloned = new ArrayList<>(source.size());
        for (H3Cell c : source) {
            H3Cell nc = new H3Cell(c.getH3Index(), c.getLatitude(), c.getLongitude());
            nc.setElevation(c.getElevation());
            nc.setTemperature(c.getTemperature());
            nc.setRainfall(c.getRainfall());
            nc.setFoodResource(c.getFoodResource());
            nc.setWaterResource(c.getWaterResource());
            nc.setWoodResource(c.getWoodResource());
            nc.setPopulation(c.getPopulation());
            nc.setResourceCapital(c.getResourceCapital());
            nc.setResourceMetal(c.getResourceMetal());
            nc.setResourceCoal(c.getResourceCoal());
            nc.setResourceWork(c.getResourceWork());
            nc.setEnergyFoodConsumed(c.getEnergyFoodConsumed());
            nc.setGiniIndex(c.getGiniIndex());
            nc.setMovementFriction(c.getMovementFriction());
            nc.setBiome(c.getBiome());
            nc.setBiomassAgriculture(c.getBiomassAgriculture());
            nc.setBiomassLivestock(c.getBiomassLivestock());
            nc.setBiomassNatural(c.getBiomassNatural());
            nc.setTradeNetwork(c.getTradeNetwork());
            nc.setSoilOrganicCarbon(c.getSoilOrganicCarbon());
            cloned.add(nc);
        }
        return cloned;
    }

    public static double calculateCohensD(double[] control, double[] treatment) {
        if (control.length == 0 || treatment.length == 0) return 0.0;
        double mean1 = Arrays.stream(control).average().orElse(0.0);
        double mean2 = Arrays.stream(treatment).average().orElse(0.0);

        double var1 = 0.0;
        for (double v : control) var1 += Math.pow(v - mean1, 2.0);
        var1 = control.length > 1 ? var1 / (control.length - 1) : 0.0;

        double var2 = 0.0;
        for (double v : treatment) var2 += Math.pow(v - mean2, 2.0);
        var2 = treatment.length > 1 ? var2 / (treatment.length - 1) : 0.0;

        double pooledStd = Math.sqrt((var1 + var2) / 2.0);
        if (pooledStd <= 1e-9) {
            return (mean2 != mean1) ? (mean2 > mean1 ? 10.0 : -10.0) : 0.0;
        }
        return (mean2 - mean1) / pooledStd;
    }

    public static double calculateKolmogorovSmirnovD(double[] sample1, double[] sample2) {
        double[] s1 = sample1.clone();
        double[] s2 = sample2.clone();
        Arrays.sort(s1);
        Arrays.sort(s2);

        int n1 = s1.length;
        int n2 = s2.length;
        if (n1 == 0 || n2 == 0) return 0.0;

        int i = 0, j = 0;
        double dMax = 0.0;

        while (i < n1 && j < n2) {
            double v1 = s1[i];
            double v2 = s2[j];
            double cdf1 = (double) (i + 1) / n1;
            double cdf2 = (double) (j + 1) / n2;

            if (v1 <= v2) {
                dMax = Math.max(dMax, Math.abs(cdf1 - (double) j / n2));
                i++;
            } else {
                dMax = Math.max(dMax, Math.abs((double) i / n1 - cdf2));
                j++;
            }
        }
        return dMax;
    }

    public static double calculateGini(List<H3Cell> cells) {
        List<Double> capitals = new ArrayList<>();
        for (H3Cell c : cells) {
            if (c.getPopulation() != null && c.getPopulation() > 50 && c.getResourceCapital() != null) {
                capitals.add(c.getResourceCapital());
            }
        }
        if (capitals.size() < 2) return 0.0;
        Collections.sort(capitals);

        double sumDiff = 0.0;
        double totalSum = 0.0;
        int n = capitals.size();

        for (int i = 0; i < n; i++) {
            totalSum += capitals.get(i);
            for (int j = 0; j < n; j++) {
                sumDiff += Math.abs(capitals.get(i) - capitals.get(j));
            }
        }
        if (totalSum == 0.0) return 0.0;
        return sumDiff / (2.0 * n * totalSum);
    }

    public static BenchmarkResult runWestBettencourtBenchmark() {
        WestBettencourtAllometryEngine engine = new WestBettencourtAllometryEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCapitals = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCapitals = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            Scenario sc = new Scenario();
            sc.setName("Empire Romain & Pax Romana (An 0)");
            sc.setInitialHumanCount(55_000_000L);
            sc.setInitialCapitalPerCapita(350.0 + (s * 10.0));
            sc.setPopulationDensityType("ROMAN_EMPIRE");
            new PreComputePhase(sc).execute(initialCells);

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 200;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                for (H3Cell c : controlGrid) {
                    int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                    if (pop > 100) {
                        double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 10.0;
                        c.setResourceCapital(cap * (1.0 + 0.015 * deltaYears));
                    }
                }
                engine.process(treatmentGrid, deltaYears);
            }

            controlGrid.sort((a, b) -> Integer.compare(b.getPopulation() != null ? b.getPopulation() : 0, a.getPopulation() != null ? a.getPopulation() : 0));
            treatmentGrid.sort((a, b) -> Integer.compare(b.getPopulation() != null ? b.getPopulation() : 0, a.getPopulation() != null ? a.getPopulation() : 0));

            int topN = Math.max(10, controlGrid.size() / 10);
            controlCapitals[s] = controlGrid.stream().limit(topN).mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCapitals[s] = treatmentGrid.stream().limit(topN).mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCapitals, treatmentCapitals);
        double ks = calculateKolmogorovSmirnovD(controlCapitals, treatmentCapitals);
        double meanControl = Arrays.stream(controlCapitals).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCapitals).average().orElse(0.0);
        return new BenchmarkResult(controlCapitals, treatmentCapitals, meanControl, meanTreatment, d, ks);
    }

    public static BenchmarkResult runTainterCollapseBenchmark() {
        TainterComplexityCollapseEngine engine = new TainterComplexityCollapseEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCapitalTerminal = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCapitalTerminal = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(2_000 + s * 100);
                c.setResourceCapital(15_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 200;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                for (H3Cell c : controlGrid) {
                    double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
                    c.setResourceCapital(Math.max(10.0, cap - 2.0 * deltaYears));
                }
                engine.process(treatmentGrid, deltaYears);
            }

            controlCapitalTerminal[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCapitalTerminal[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCapitalTerminal, treatmentCapitalTerminal);
        double meanControl = Arrays.stream(controlCapitalTerminal).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCapitalTerminal).average().orElse(0.0);
        return new BenchmarkResult(controlCapitalTerminal, treatmentCapitalTerminal, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runArthurTechBenchmark() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlTechTerminal = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentTechTerminal = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            Scenario sc = new Scenario();
            sc.setName("Industrial Revolution & Steam Engine (1800)");
            sc.setInitialHumanCount(50_000_000L);
            sc.setInitialCapitalPerCapita(150.0 + (s * 10.0));
            sc.setPopulationDensityType("INDUSTRIAL_1800");
            new PreComputePhase(sc).execute(initialCells);

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 200;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                for (H3Cell c : controlGrid) {
                    int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                    if (pop >= 100) {
                        double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 1.0;
                        c.setResourceCapital(cap + 0.08 * pop * deltaYears);
                    }
                }
                engine.process(treatmentGrid, deltaYears);
            }

            controlTechTerminal[s] = controlGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() >= 100)
                    .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 1.0).average().orElse(1.0);
            treatmentTechTerminal[s] = treatmentGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() >= 100)
                    .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 1.0).average().orElse(1.0);
        }

        double d = calculateCohensD(controlTechTerminal, treatmentTechTerminal);
        double meanControl = Arrays.stream(controlTechTerminal).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentTechTerminal).average().orElse(0.0);
        return new BenchmarkResult(controlTechTerminal, treatmentTechTerminal, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runKrugmanBenchmark() {
        KrugmanCorePeripheryEngine engine = new KrugmanCorePeripheryEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] giniControl = new double[MONTE_CARLO_SEEDS.length];
        double[] giniTreatment = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            Scenario sc = new Scenario();
            sc.setName("Empire Romain & Pax Romana (An 0)");
            sc.setInitialHumanCount(20_000_000L);
            sc.setInitialCapitalPerCapita(200.0);
            sc.setPopulationDensityType("ROMAN_EMPIRE");
            new PreComputePhase(sc).execute(initialCells);

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 150;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                engine.process(treatmentGrid, deltaYears);
            }

            giniControl[s] = calculateGini(controlGrid);
            giniTreatment[s] = calculateGini(treatmentGrid);
        }

        double d = calculateCohensD(giniControl, giniTreatment);
        double meanGiniControl = Arrays.stream(giniControl).average().orElse(0.0);
        double meanGiniTreatment = Arrays.stream(giniTreatment).average().orElse(0.0);
        return new BenchmarkResult(giniControl, giniTreatment, meanGiniControl, meanGiniTreatment, d, 0.0);
    }

    public static BenchmarkResult runSpatialSEIRBenchmark() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlMortality = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentMortality = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            Scenario sc = new Scenario();
            sc.setName("Empire Romain & Pax Romana (An 0)");
            sc.setInitialHumanCount(55_000_000L);
            sc.setInitialCapitalPerCapita(350.0);
            sc.setPopulationDensityType("ROMAN_EMPIRE");
            new PreComputePhase(sc).execute(initialCells);

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);
            // Seed index patient-zero epidemic focus in the largest urban port of the treatment grid (e.g. 165 CE Antonine / 1347 Black Death entry port)
            treatmentGrid.stream()
                    .filter(c -> c.getPopulation() != null && c.getPopulation() > 500)
                    .max(Comparator.comparingInt(H3Cell::getPopulation))
                    .ifPresent(epicenter -> epicenter.setEpidemicInfected(Math.max(100, epicenter.getPopulation() / 20)));

            int simYears = 50;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                engine.process(treatmentGrid, deltaYears);
            }

            controlMortality[s] = controlGrid.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            treatmentMortality[s] = treatmentGrid.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        }

        double d = calculateCohensD(controlMortality, treatmentMortality);
        double meanControl = Arrays.stream(controlMortality).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentMortality).average().orElse(0.0);
        return new BenchmarkResult(controlMortality, treatmentMortality, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runSoilSalinizationBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlFoodTerminal = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentFoodTerminal = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setRainfall(200.0);
                c.setTemperature(26.0);
                c.setElevation(50.0);
                c.setPopulation(1_500);
                c.setFoodResource(20_000.0);
                c.setResourceCapital(75_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 150;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                SoilSalinizationHydrologyEngine.processSoilSalinization(treatmentGrid, deltaYears);
            }

            controlFoodTerminal[s] = controlGrid.stream().mapToDouble(c -> c.getFoodResource() != null ? c.getFoodResource() : 0.0).average().orElse(0.0);
            treatmentFoodTerminal[s] = treatmentGrid.stream().mapToDouble(c -> c.getFoodResource() != null ? c.getFoodResource() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlFoodTerminal, treatmentFoodTerminal);
        double meanControl = Arrays.stream(controlFoodTerminal).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentFoodTerminal).average().orElse(0.0);
        return new BenchmarkResult(controlFoodTerminal, treatmentFoodTerminal, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runDraftAnimalBenchmark() {
        DraftAnimalFodderAllocationEngine engine = new DraftAnimalFodderAllocationEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] capitalControl = new double[MONTE_CARLO_SEEDS.length];
        double[] capitalTreatment = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(50_000.0);
                c.setFoodResource(15_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 100;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                engine.process(treatmentGrid, deltaYears);
            }

            capitalControl[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            capitalTreatment[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(capitalControl, capitalTreatment);
        double meanControl = Arrays.stream(capitalControl).average().orElse(0.0);
        double meanTreatment = Arrays.stream(capitalTreatment).average().orElse(0.0);
        return new BenchmarkResult(capitalControl, capitalTreatment, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runHotellingBenchmark() {
        HotellingResourceDepletionEngine engine = new HotellingResourceDepletionEngine();
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlMetalTerminal = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentMetalTerminal = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(2_000);
                c.setResourceMetal(500.0);
                c.setResourceCapital(100_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 100;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                engine.process(treatmentGrid, deltaYears);
            }

            controlMetalTerminal[s] = controlGrid.stream().mapToDouble(c -> c.getResourceMetal() != null ? c.getResourceMetal() : 0.0).average().orElse(0.0);
            treatmentMetalTerminal[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceMetal() != null ? c.getResourceMetal() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlMetalTerminal, treatmentMetalTerminal);
        double meanControl = Arrays.stream(controlMetalTerminal).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentMetalTerminal).average().orElse(0.0);
        return new BenchmarkResult(controlMetalTerminal, treatmentMetalTerminal, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runPolanyiBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlAgri = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentAgri = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(150_000.0); // kpc = 150 > 80
                c.setTradeNetwork(30.0);         // trade > 15
                c.setBiomassAgriculture(10_000.0);
                c.setFoodResource(20_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 30;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                PolanyiFictitiousCommoditiesEngine.processPolanyiTransformation(treatmentGrid, deltaYears);
            }

            controlAgri[s] = controlGrid.stream().mapToDouble(c -> c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0).average().orElse(0.0);
            treatmentAgri[s] = treatmentGrid.stream().mapToDouble(c -> c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlAgri, treatmentAgri);
        double meanControl = Arrays.stream(controlAgri).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentAgri).average().orElse(0.0);
        return new BenchmarkResult(controlAgri, treatmentAgri, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runBagehotBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(200_000.0);
                c.setTradeNetwork(30.0);    // commercial banking integration (> 15)
                c.setResourceMetal(0.0); // unbuffered liquidity crisis (zero bullion reserves)
                c.setFoodResource(500.0);   // acute systemic stress
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 15;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                BagehotFractionalReserveEngine.processBagehotBankingLiquidity(treatmentGrid, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runCordenNearyBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(50_000.0);
                c.setResourceMetal(30_000.0); // Massive resource rent surge
                c.setBiomassAgriculture(10_000.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 25;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                CordenNearyDutchDiseaseEngine.processDutchDiseaseDynamics(treatmentGrid, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runInnisBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlTrade = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentTrade = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(500);
                c.setBiomassNatural(25_000.0);
                c.setResourceMetal(5_000.0);
                c.setTradeNetwork(5.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 30;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                InnisStaplesThesisEngine.processStaplesDynamics(treatmentGrid, deltaYears);
            }

            controlTrade[s] = controlGrid.stream().mapToDouble(c -> c.getTradeNetwork() != null ? c.getTradeNetwork() : 0.0).average().orElse(0.0);
            treatmentTrade[s] = treatmentGrid.stream().mapToDouble(c -> c.getTradeNetwork() != null ? c.getTradeNetwork() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlTrade, treatmentTrade);
        double meanControl = Arrays.stream(controlTrade).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentTrade).average().orElse(0.0);
        return new BenchmarkResult(controlTrade, treatmentTrade, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runBarfieldSteppeBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlNomadCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentNomadCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            int half = initialCells.size() / 2;
            for (int i = 0; i < initialCells.size(); i++) {
                H3Cell c = initialCells.get(i);
                if (i < half) {
                    // Sedentary agrarian core
                    c.setPopulation(5_000);
                    c.setResourceCapital(500_000.0);
                    c.setBiomassAgriculture(50_000.0);
                } else {
                    // Nomadic steppe pastoralists
                    c.setPopulation(1_000);
                    c.setBiomassLivestock(10_000.0);
                    c.setBiomassAgriculture(50.0);
                    c.setResourceCapital(10_000.0);
                    c.setResourceWork(40.0);
                }
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 20;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                BarfieldNomadicSteppeEmpireEngine.processSteppeExtortionDynamics(treatmentGrid, deltaYears);
            }

            controlNomadCap[s] = controlGrid.subList(half, controlGrid.size()).stream()
                    .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentNomadCap[s] = treatmentGrid.subList(half, treatmentGrid.size()).stream()
                    .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlNomadCap, treatmentNomadCap);
        double meanControl = Arrays.stream(controlNomadCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentNomadCap).average().orElse(0.0);
        return new BenchmarkResult(controlNomadCap, treatmentNomadCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runWienerCyberneticBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlWork = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentWork = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setFoodResource(1_500.0); // 1.5 food/cap > 1.2 target
                c.setResourceCapital(50_000.0);
                c.setResourceWork(80.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 15;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                WienerCyberneticHomeostasisEngine.processCyberneticHomeostasis(treatmentGrid, deltaYears);
            }

            controlWork[s] = controlGrid.stream().mapToDouble(c -> c.getResourceWork() != null ? c.getResourceWork() : 0.0).average().orElse(0.0);
            treatmentWork[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceWork() != null ? c.getResourceWork() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlWork, treatmentWork);
        double meanControl = Arrays.stream(controlWork).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentWork).average().orElse(0.0);
        return new BenchmarkResult(controlWork, treatmentWork, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runRicardoBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_500);
                c.setResourceCapital(100_000.0);
                c.setBiomassAgriculture(500.0); // High population pressure on marginal land
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 25;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                RicardoDifferentialLandRentEngine.processDifferentialRentDynamics(treatmentGrid, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runLewisBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(300_000.0); // kpc = 300 > 250 (post Lewis turning point)
                c.setTradeNetwork(30.0);
                c.setResourceWork(50.0);
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 20;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                LewisDualSectorTurningPointEngine.processDualSectorDynamics(treatmentGrid, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runKondratievBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(200_000.0); // kpc = 200 > 120
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 25;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                KondratievSchumpeterLongWaveEngine.processLongWaveDynamics(treatmentGrid, 1850.0 + year, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }

    public static BenchmarkResult runPomeranzBenchmark() {
        ProceduralGenerator generator = new ProceduralGenerator();
        double[] controlCap = new double[MONTE_CARLO_SEEDS.length];
        double[] treatmentCap = new double[MONTE_CARLO_SEEDS.length];

        for (int s = 0; s < MONTE_CARLO_SEEDS.length; s++) {
            long seed = MONTE_CARLO_SEEDS[s];
            List<H3Cell> initialCells = generator.generatePlanet(createEarthPreset(seed));
            for (H3Cell c : initialCells) {
                c.setPopulation(1_000);
                c.setResourceCapital(100_000.0);
                c.setResourceCoal(2_000.0); // Proximate coal deposits
                c.setMovementFriction(0.35); // Navigable water corridor
                c.setTradeNetwork(35.0);     // Transoceanic ghost acreage
            }

            List<H3Cell> controlGrid = cloneCellGrid(initialCells);
            List<H3Cell> treatmentGrid = cloneCellGrid(initialCells);

            int simYears = 25;
            double deltaYears = 1.0;
            for (int year = 0; year < simYears; year++) {
                PomeranzGreatDivergenceEngine.processGreatDivergenceDynamics(treatmentGrid, deltaYears);
            }

            controlCap[s] = controlGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
            treatmentCap[s] = treatmentGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0);
        }

        double d = calculateCohensD(controlCap, treatmentCap);
        double meanControl = Arrays.stream(controlCap).average().orElse(0.0);
        double meanTreatment = Arrays.stream(treatmentCap).average().orElse(0.0);
        return new BenchmarkResult(controlCap, treatmentCap, meanControl, meanTreatment, d, 0.0);
    }
}
