package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.model.Scenario;
import org.ether.society.core.dod.WorldBuffer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates and calculates macroscopic, cliodynamic, thermodynamic, and demographic metrics
 * for the planetary simulation engine.
 *
 * <p>Implements analytical formulas for energy capture (Leslie White's Law), resource depletion,
 * systemic entropy (Boltzmann-Shannon), carrying capacity, institutional maturity,
 * and Turchin's Structural-Demographic Theory (SDT) vulnerability indicators.</p>
 */
public class SimulationMetricsAggregator {

    private final H3SimulationEngine engine;

    /**
     * Constructs a new SimulationMetricsAggregator bound to the specified engine instance.
     *
     * @param engine the parent H3SimulationEngine, must not be null
     */
    public SimulationMetricsAggregator(H3SimulationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS ---

    /**
 * Get energy captured.

     *

     * @return the resulting computation or state reference

     */

    public double getEnergyCaptured() {

        long pop = engine.getTotalPopulation();

        if (pop <= 0) return 0.0;

        float tech = engine.getAverageTechnology();

        // Leslie White Energy Law: P_tot = N_pop * P_capita

        // Baseline 300 W per capita (human metabolic work ~120W + fire energy ~180W)

        // Technology scales energy extraction: animal, water, steam, electrical

        double wattsPerCapita = 300.0 + Math.pow(Math.max(0.0, tech), 2.2) * 35.0;

        double totalWatts = pop * wattsPerCapita;

        return totalWatts / 1e6; // Energy returned in Megawatts (MW)

    }

    /**
 * Get resource depletion rate.

     *

     * @return the resulting computation or state reference

     */

    public double getResourceDepletionRate() {
        if (engine.getWorldBuffer() == null || engine.getWorldBuffer().getCapacity() == 0) return 0;
        double maxRes = engine.getWorldBuffer().getCapacity() * 1000.0;
        double currentRes;
        if (engine.getLastCacheTick() == engine.getTickCounter() && engine.getCachedResourceCapital() >= 0) {
            currentRes = engine.getCachedResourceCapital();
        } else {
            currentRes = 0;
            float[] res = engine.getWorldBuffer().getResourceCapital();
            for (int i = 0; i < engine.getWorldBuffer().getCapacity(); i++) currentRes += res[i];
        }
        return Math.max(0, Math.min(100.0, (1.0 - currentRes / Math.max(1, maxRes)) * 100.0));
    }

    /**
 * Get energy per capita.

     *

     * @return the resulting computation or state reference

     */

    public double getEnergyPerCapita() {

        long pop = engine.getTotalPopulation();

        return pop > 0 ? (getEnergyCaptured() * 1e6) / pop : 300.0; // in Joules/sec (Watts)

    }

    /**
 * Get food per capita.

     *

     * @return the resulting computation or state reference

     */

    public double getFoodPerCapita() {

        long pop = engine.getTotalPopulation();

        return pop > 0 ? engine.getTotalFood() / pop : 0;

    }

    /**
 * Get biomass domesticated.

     *

     * @return the resulting computation or state reference

     */

    public double getBiomassDomesticated() {

        return engine.getTotalPopulation() * 0.15 + engine.getTotalFood() * 0.4;

    }

    /**
 * Get potable water total.

     *

     * @return the resulting computation or state reference

     */

    public double getPotableWaterTotal() {
        if (engine.getLastCacheTick() == engine.getTickCounter() && engine.getCachedPotableWater() >= 0) {
            return engine.getCachedPotableWater();
        }
        if (engine.getWorldBuffer() == null) return 0;
        double water = 0;
        float[] w = engine.getWorldBuffer().getWaterResource();
        for (int i = 0; i < engine.getWorldBuffer().getCapacity(); i++) water += w[i];
        return water;
    }

    /**
 * Get remaining resources ratio.

     *

     * @return the resulting computation or state reference

     */

    public double getRemainingResourcesRatio() {

        return 100.0 - getResourceDepletionRate();

    }

    /**
 * Get systemic entropy.

     *

     * @return the resulting computation or state reference

     */

    public double getSystemicEntropy() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        return Math.min(1000.0, (Math.log1p(pop) * 25.0) + (tech * 2.5));
    }

    /**
 * Get pollution level.

     *

     * @return the resulting computation or state reference

     */

    public double getPollutionLevel() {

        float tech = engine.getAverageTechnology();

        long pop = engine.getTotalPopulation();

        return Math.max(0, (tech > 50 ? (tech - 50) * 1.5 * (pop / 100000.0) : 0));

    }

    /**
 * Get occupied territory area.

     *

     * @return the resulting computation or state reference

     */

    public double getOccupiedTerritoryArea() {

        double baseCellArea = (engine.getCurrentScenario() != null && engine.getCurrentScenario().getCellSizeKm2() > 0)

                ? engine.getCurrentScenario().getCellSizeKm2() : 1250.0;

        long populatedCells = engine.getPopulatedCellCount();

        long pop = engine.getTotalPopulation();

        if (populatedCells == 0 || pop == 0) return 0.0;

        float tech = engine.getAverageTechnology();

        double physicalGridArea = populatedCells * baseCellArea;

        // Binford (2001), Kelly (1995), Hassan (1981) - Behavioral Ecology & Home Range:

        // Pre-agricultural Hunter-Gatherers (Tech < 1.5): 10 to 100 km² per capita diffuse subsistence home range

        if (tech < 1.5f) {

            double temp = 15.0;

            if (engine.getWorldBuffer() != null && engine.getWorldBuffer().getTemperature().length > 0) {

                temp = engine.getWorldBuffer().getTemperature()[0];

            }

            // Harsh cold/arid biomes require 80-100 km²/hab, rich temperate/river valleys 15-40 km²/hab

            double km2PerCapita = (temp < 5.0) ? 85.0 : Math.max(12.0, 45.0 - (temp * 1.2));

            double ecologicalHomeRange = pop * km2PerCapita;

            return Math.max(physicalGridArea, ecologicalHomeRange);

        } else if (tech < 5.0f) {

            // Neolithic & Agrarian transition (Tech 1.5 to 5.0): 0.05 to 2.0 km²/hab

            double km2PerCapita = Math.max(0.05, 2.0 - (tech - 1.5) * 0.55);

            return Math.max(physicalGridArea, pop * km2PerCapita);

        }

        return physicalGridArea;

    }

    /**
 * Get offspring percentage.

     *

     * @return the resulting computation or state reference

     */

    public double getOffspringPercentage() {

        float fert = engine.getCurrentFertility();

        return Math.min(95.0, Math.max(20.0, 40.0 + fert * 7.5));

    }

    /**
 * Get age at first child.

     *

     * @return the resulting computation or state reference

     */

    public double getAgeAtFirstChild() {

        float tech = engine.getAverageTechnology();

        return Math.min(32.0, Math.max(16.0, 18.0 + (tech / 200.0) * 10.0));

    }

    /**
 * Get immigration rate.

     *

     * @return the resulting computation or state reference

     */

    public double getImmigrationRate() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, Math.max(0.0, (Math.log1p(pop) * 1.5) + (tech * 0.35)));
    }

    /**
 * Get education level.

     *

     * @return the resulting computation or state reference

     */

    public double getEducationLevel() {

        float tech = engine.getAverageTechnology();

        return Math.min(100.0, (tech / 250.0) * 100.0);

    }

    /**
 * Get happiness index.

     *

     * @return the resulting computation or state reference

     */

    public double getHappinessIndex() {

        float gini = engine.getCurrentGini();

        float life = engine.getCurrentLifeExpectancy();

        double foodPerCap = getFoodPerCapita();

        double annualReq = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;

        double foodSat = Math.min(1.0, foodPerCap > 0 ? foodPerCap / annualReq : 0.5);

        double base = 40.0 + (life / 80.0) * 30.0 + (foodSat * 30.0) - (gini * 20.0);

        return Math.max(10.0, Math.min(100.0, base));

    }

    /**
 * Get conflict level.

     *

     * @return the resulting computation or state reference

     */

    public double getConflictLevel() {

        float gini = engine.getCurrentGini();

        double happiness = getHappinessIndex();

        double foodPerCap = getFoodPerCapita();

        double annualReq = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;

        double scarcity = (foodPerCap > 0 && foodPerCap < annualReq) ? (1.0 - foodPerCap / annualReq) * 25.0 : 0.0;

        return Math.max(0.0, Math.min(100.0, (gini * 35.0) + (100.0 - happiness) * 0.25 + scarcity));

    }

    /**
 * Get city states count.

     *

     * @return the resulting computation or state reference

     */

    public int getCityStatesCount() {

        float tech = engine.getAverageTechnology();

        long year = engine.getCurrentYear();

        // City-states and urban hubs strictly appear in the Bronze/Agricultural Revolution era (post -4000 BC, Tech >= 15)

        if (tech < 15.0f || year < -4000) {

            return 0;

        }

        long popCells = engine.getPopulatedCellCount();

        return (int) Math.max(0, popCells / 12);

    }

    /**
 * Get institutional maturity.

     *

     * @return the resulting computation or state reference

     */

    public double getInstitutionalMaturity() {

        float tech = engine.getAverageTechnology();

        return Math.min(100.0, tech * 0.45);

    }

    /**
 * Get division of labor index.

     *

     * @return the resulting computation or state reference

     */

    public double getDivisionOfLaborIndex() {

        float tech = engine.getAverageTechnology();

        long pop = engine.getTotalPopulation();

        double surplus = getNetSurplusFraction();

        return Math.min(100.0, (surplus * 60.0) + (tech * 0.25) + Math.log10(Math.max(1, pop)) * 3.0);

    }

    /**
 * Energy Return on Investment for the food/subsistence system (EROI_alim = E_out / E_in).

     * Preindustrial: 3:1 to 15:1. Industrial thermodynamic inversion: < 1.0 (7-10 kcal fossil per 1 kcal ingested).

     */

    public double getEroiAlimentaire() {

        float tech = engine.getAverageTechnology();

        if (tech < 1.5f) {

            return 7.0; // Paleolithic Hunter-Gatherer baseline

        } else if (tech < 4.0f) {

            return 6.5; // Early Neolithic

        } else if (tech < 50.0f) {

            return 2.8; // Preindustrial Agrarian with Draft Animals

        } else if (tech < 120.0f) {

            return 0.50; // Industrial Era (Thermodynamic inversion)

        } else {

            return 0.12; // Post-Industrial Globalized Food System (10:1 fossil input)

        }

    }

    /**
 * Net societal energy surplus fraction Phi = 1 - 1 / EROI_alim (Tainter 1988, Hall et al. 2014).

     */

    public double getNetSurplusFraction() {

        double eroi = getEroiAlimentaire();

        return Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroi)));

    }

    /**
 * Trophic footprint multiplier mu = Mobilized Raw Biomass / Ingested Energy (2.0x to 25.0x).

     */

    public double getTrophicMultiplier() {

        float tech = engine.getAverageTechnology();

        if (tech < 1.5f) return PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER;

        if (tech < 4.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN;

        if (tech < 50.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN;

        if (tech < 120.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER;

        return PhysicalConstants.TROPHIC_MULTIPLIER_POST_INDUSTRIAL;

    }

    /**
 * Gross raw biomass mobilized per capita per year in kg/hab/an.

     */

    public double getBiomassMobilizedPerCapitaKg() {

        double trophicMul = getTrophicMultiplier();

        float tech = engine.getAverageTechnology();

        double energyDensity = (tech < 1.5f) ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG

                : (tech < 50.0f ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG : 14.0);

        return (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * trophicMul) / energyDensity;

    }

    /**
 * Get max hierarchy level.

     *

     * @return the resulting computation or state reference

     */

    public int getMaxHierarchyLevel() {

        float tech = engine.getAverageTechnology();

        if (tech < 10) return 1;

        if (tech < 30) return 2;

        if (tech < 60) return 3;

        if (tech < 100) return 4;

        if (tech < 200) return 5;

        return 6;

    }

    /**
 * Get largest cultural unit size.

     *

     * @return the resulting computation or state reference

     */

    public long getLargestCulturalUnitSize() {

        if (!engine.getNations().isEmpty()) {

            return engine.getNations().stream()

                    .mapToLong(org.ether.society.database.Polity::getTotalPopulation)

                    .max()

                    .orElse((long) (engine.getTotalPopulation() * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0))));

        }

        long pop = engine.getTotalPopulation();

        return (long) (pop * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0)));

    }

    /**
 * Get largest organization complexity.

     *

     * @return the resulting computation or state reference

     */

    public double getLargestOrganizationComplexity() {

        long largestPop = getLargestCulturalUnitSize();

        if (largestPop <= 0) return 0.0;

        float avgTech = engine.getAverageTechnology();

        int hierarchy = getMaxHierarchyLevel();

        double stateCap = 0.5;

        if (!engine.getNations().isEmpty()) {

            org.ether.society.database.Polity largest = engine.getNations().stream()

                    .max(java.util.Comparator.comparingLong(org.ether.society.database.Polity::getTotalPopulation))

                    .orElse(null);

            if (largest != null) {

                stateCap = largest.getStateCapacity();

            }

        }

        return largestPop * (1.0 + avgTech * 0.4) * stateCap * (Math.log(1.0 + hierarchy) / Math.log(2.0));

    }

    /**
 * Get largest organization entropy.

     *

     * @return the resulting computation or state reference

     */

    public double getLargestOrganizationEntropy() {

        double complexity = getLargestOrganizationComplexity();

        double pollution = getPollutionLevel();

        float tech = engine.getAverageTechnology();

        return (complexity * 0.05 + tech * 1.2) * (1.0 + pollution / 100.0);

    }

    /**
 * Get kardashev scale.

     *

     * @return the resulting computation or state reference

     */

    public double getKardashevScale() {

        double energyMW = getEnergyCaptured();

        if (energyMW <= 0) return 0.0;

        double watts = energyMW * 1e6;

        double k = (Math.log10(Math.max(1.0, watts)) - 6.0) / 10.0;

        return Math.max(0.0, Math.min(3.0, k));

    }

    /**
 * Get built capital total.

     *

     * @return the resulting computation or state reference

     */

    public double getBuiltCapitalTotal() {

        float tech = engine.getAverageTechnology();

        long pop = engine.getTotalPopulation();

        // Proportional to physical tools, structures and technology level

        return pop * (0.5 + Math.pow(Math.max(0.0, tech), 1.8) * 8.0);

    }

    /**
 * Get elite formation ratio.

     *

     * @return the resulting computation or state reference

     */

    public double getEliteFormationRatio() {

        float gini = engine.getCurrentGini();

        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));

    }

    /**
 * Get elder capital share.

     *

     * @return the resulting computation or state reference

     */

    public double getElderCapitalShare() {

        float gini = engine.getCurrentGini();

        float life = engine.getCurrentLifeExpectancy();

        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));

    }

    /**
 * Get land rent index.

     *

     * @return the resulting computation or state reference

     */

    public double getLandRentIndex() {

        long pop = engine.getTotalPopulation();

        long populatedCells = engine.getPopulatedCellCount();

        double density = populatedCells > 0 ? (double) pop / populatedCells : 0;

        return density * 1.5 + engine.getAverageTechnology() * 0.8;

    }

    /**
 * Get tools count.

     *

     * @return the resulting computation or state reference

     */

    public long getToolsCount() {

        float tech = engine.getAverageTechnology();

        return (long) (engine.getTotalPopulation() * (0.5 + tech * 0.2));

    }

    /**
 * Get products count.

     *

     * @return the resulting computation or state reference

     */

    public long getProductsCount() {

        float tech = engine.getAverageTechnology();

        return (long) Math.max(3, 5 + Math.pow(tech, 1.6));

    }

    /**
 * Get system complexity index.

     *

     * @return the resulting computation or state reference

     */

    public double getSystemComplexityIndex() {

        float tech = engine.getAverageTechnology();

        double divLabor = getDivisionOfLaborIndex();

        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));

    }

    /**
 * Get reconstruction capability index.

     *

     * @return the resulting computation or state reference

     */

    public double getReconstructionCapabilityIndex() {

        double edu = getEducationLevel();

        float tech = engine.getAverageTechnology();

        return Math.min(100.0, (edu * 0.7 + tech * 0.3));

    }

    /**
 * Get system interdependence index.

     *

     * @return the resulting computation or state reference

     */

    public double getSystemInterdependenceIndex() {

        double complexity = getSystemComplexityIndex();

        return Math.min(100.0, complexity * 0.95);

    }

    /**
 * Get age pyramid.

     *

     * @return the resulting computation or state reference

     */

    public int[] getAgePyramid() {

        int[] cohorts = new int[7];

        if (engine.getAgentBuffer() != null && engine.getAgentBuffer().getCapacity() > 0) {

            float[] ages = engine.getAgentBuffer().getAge();

            float[] masses = engine.getAgentBuffer().getMass();

            int[] hexIds = engine.getAgentBuffer().getHexIds();

            // Iterate through active agent / demographic cohort buffers

            for (int i = 0; i < engine.getAgentBuffer().getCapacity(); i++) {

                if (hexIds != null && hexIds[i] == -1) continue;

                float age = ages != null ? ages[i] : 25.0f;

                float m = masses != null ? masses[i] : 1.0f;

                if (m <= 0) continue;

                int count = Math.max(1, Math.round(m));

                if (age < 15) cohorts[0] += count;

                else if (age < 25) cohorts[1] += count;

                else if (age < 40) cohorts[2] += count;

                else if (age < 55) cohorts[3] += count;

                else if (age < 70) cohorts[4] += count;

                else if (age < 85) cohorts[5] += count;

                else cohorts[6] += count;

            }

            long realPop = engine.getTotalPopulation();

            long agentSum = 0; for (int c : cohorts) agentSum += c;

            if (agentSum > 0 && Math.abs(realPop - agentSum) > 5) {

                double scale = (double) realPop / agentSum;

                // Iterate through active agent / demographic cohort buffers

                for (int i = 0; i < 7; i++) cohorts[i] = (int) Math.round(cohorts[i] * scale);

            }

        }

        if (cohorts[0] == 0 && cohorts[1] == 0 && cohorts[2] == 0 && cohorts[3] == 0 && cohorts[4] == 0 && cohorts[5] == 0 && cohorts[6] == 0) {

            long pop = engine.getTotalPopulation();

            float life = engine.getCurrentLifeExpectancy();

            double c0_pct = Math.max(0.12, 0.35 - (life - 30.0) * 0.003);

            double c1_pct = 0.18;

            double c2_pct = 0.24;

            double c3_pct = 0.18;

            double c4_pct = 0.12;

            double c5_pct = 0.06 + Math.min(0.06, (life - 50.0) * 0.002);

            double c6_pct = Math.max(0.01, 1.0 - (c0_pct + c1_pct + c2_pct + c3_pct + c4_pct + c5_pct));

            cohorts[0] = (int) (pop * c0_pct);

            cohorts[1] = (int) (pop * c1_pct);

            cohorts[2] = (int) (pop * c2_pct);

            cohorts[3] = (int) (pop * c3_pct);

            cohorts[4] = (int) (pop * c4_pct);

            cohorts[5] = (int) (pop * c5_pct);

            cohorts[6] = (int) (pop * c6_pct);

        }

        return cohorts;

    }

    // --- 🧠 COGNITION & INFORMATION ---

    /**
 * Get shannon bandwidth.

     *

     * @return the resulting computation or state reference

     */

    public double getShannonBandwidth() {

        float tech = engine.getAverageTechnology();

        return 1.0 + Math.pow(tech, 1.4) * 0.8;

    }

    /**
 * Get collective memory stock.

     *

     * @return the resulting computation or state reference

     */

    public double getCollectiveMemoryStock() {

        float tech = engine.getAverageTechnology();

        long pop = engine.getTotalPopulation();

        return (pop * 0.05 + Math.pow(tech, 2.1));

    }

    /**
 * Get innovation diffusion speed.

     *

     * @return the resulting computation or state reference

     */

    public double getInnovationDiffusionSpeed() {

        float tech = engine.getAverageTechnology();

        double divLabor = getDivisionOfLaborIndex();

        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));

    }

    /**
 * Get knowledge decay rate.

     *

     * @return the resulting computation or state reference

     */

    public double getKnowledgeDecayRate() {

        float gini = engine.getCurrentGini();

        double conflict = getConflictLevel();

        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));

    }

    // --- ECOLOGY & PLANETARY BOUNDARIES ---

    /**
 * Get soil npkquality.

     *

     * @return the resulting computation or state reference

     */

    public double getSoilNPKQuality() {

        float tech = engine.getAverageTechnology();

        double resDep = getResourceDepletionRate();

        return Math.max(5.0, 100.0 - (resDep * 0.6) + Math.min(15.0, tech * 0.1));

    }

    /**
 * Get carbon footprint.

     *

     * @return the resulting computation or state reference

     */

    public double getCarbonFootprint() {

        double energy = getEnergyCaptured();

        float tech = engine.getAverageTechnology();

        return (energy * (tech > 40 && tech < 180 ? 0.85 : 0.2)) / 1000.0;

    }

    /**
 * Get wild biodiversity index.

     *

     * @return the resulting computation or state reference

     */

    public double getWildBiodiversityIndex() {

        float bioNat = engine.getTotalBiomassNatural();

        double bioDom = getBiomassDomesticated();

        double total = bioNat + bioDom;

        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;

    }

    /**
 * Get wet bulb safety margin.

     *

     * @return the resulting computation or state reference

     */

    public double getWetBulbSafetyMargin() {

        float temp = 15.0f;

        float[] temps = engine.getWorldBuffer() != null ? engine.getWorldBuffer().getTemperature() : null;

        if (temps != null && temps.length > 0) {

            float sum = 0; for(float t : temps) sum += t;

            temp = sum / temps.length;

        }

        return Math.max(0.0, 35.0 - (temp + 3.5));

    }

    // --- CLIODYNAMICS & SYSTEMIC RISKS ---

    /**
 * Get elite overproduction index.

     *

     * @return the resulting computation or state reference

     */

    public double getEliteOverproductionIndex() {

        double eliteForm = getEliteFormationRatio();

        float gini = engine.getCurrentGini();

        double val = (eliteForm / 5.0) * (1.0 + (Double.isNaN(gini) ? 0.0 : gini) * 2.0);

        return Double.isNaN(val) ? 1.0 : Math.max(0.0, Math.min(10.0, val));

    }

    /**
 * Get fiscal stress index.

     *

     * @return the resulting computation or state reference

     */

    public double getFiscalStressIndex() {

        double landRent = getLandRentIndex();

        float gini = engine.getCurrentGini();

        double val = ((Double.isNaN(gini) ? 0.0 : gini) * 50.0) + (landRent * 0.3);

        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));

    }

    /**
 * Get geopolitical tension.

     *

     * @return the resulting computation or state reference

     */

    public double getGeopoliticalTension() {

        int cityStates = getCityStatesCount();

        double conflict = getConflictLevel();

        double val = (cityStates * 2.5) + (conflict * 0.7);

        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));

    }

    /**
 * Get collapse vulnerability.

     *

     * @return the resulting computation or state reference

     */

    public double getCollapseVulnerability() {

        double resDep = getResourceDepletionRate();

        double psi = getEliteOverproductionIndex();

        double entropy = getSystemicEntropy();

        double eroi = getEroiAlimentaire();

        // Inversion thermodynamique : if EROI < 1.0 (industrial dependency), resource depletion accelerates collapse risk

        double thermodynamicInversionRisk = (eroi < 1.0) ? (1.0 / Math.max(0.05, eroi)) * ((Double.isNaN(resDep) ? 0.0 : resDep) / 100.0) * 15.0 : 0.0;

        double val = ((Double.isNaN(resDep) ? 0.0 : resDep) * 0.25) +

                     ((Double.isNaN(psi) ? 0.0 : psi) * 3.5) +

                     ((Double.isNaN(entropy) ? 0.0 : entropy) / 20.0) +

                     thermodynamicInversionRisk;

        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));

    }

    /**
 * Get average asabiyyah.

     *

     * @return the resulting computation or state reference

     */

    public double getAverageAsabiyyah() {

        if (engine.getCells() == null || engine.getCells().isEmpty()) return 80.0;

        double sum = 0;

        int count = 0;

        for (H3Cell c : engine.getCells()) {
            if (c != null && c.getOwner() != null) {
                sum += c.getOwner().getAsabiyyah();
                count++;
            }
        }

        return count > 0 ? (sum / count) * 100.0 : 80.0;

    }

    /**
 * Get population survival rate.

     *

     * @return the resulting computation or state reference

     */

    public double getPopulationSurvivalRate() {

        long currentPop = engine.getTotalPopulation();

        long initPop = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getInitialHumanCount() : 1_000_000L;

        if (initPop <= 0) return 100.0;

        return Math.min(100.0, Math.max(0.0, ((double) currentPop / initPop) * 100.0));

    }


}
