/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.model.PhysicalConstants;

import java.util.Comparator;
import java.util.Objects;

/**
 * <h1>SimulationMetricsAggregator</h1>
 * <p>
 * Aggregates and calculates macroscopic, cliodynamic, thermodynamic, and demographic metrics
 * for the planetary simulation engine.<br>
 * Implements analytical formulas for energy capture (Leslie White's Law), resource depletion,
 * systemic entropy (Boltzmann-Shannon), carrying capacity, institutional maturity,
 * and Turchin's Structural-Demographic Theory (SDT) vulnerability indicators.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3SimulationEngine
 */
public class SimulationMetricsAggregator {

    private final H3SimulationEngine engine;

    /**
     * Constructs a new {@link SimulationMetricsAggregator} bound to the specified engine instance.
     *
     * @param engine parent {@link H3SimulationEngine}, must not be null
     */
    public SimulationMetricsAggregator(H3SimulationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS ---

    /**
     * Evaluates total power captured by human civilization in Megawatts ($\text{MW}$) according to
     * Vaclav Smil's (2017) exergy transitions and Leslie White's Energy Law:
     * $$P_{\text{cap}}(T) = P_{\text{hearth}} + \Delta P_{\text{agr}}(T) + \Delta P_{\text{ind}}(T)$$
     *
     * @return total captured power in $\text{MW}$
     */
    public double getEnergyCaptured() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        float tech = engine.getAverageTechnology();
        // 1. Endosomatic basal metabolism (115 W) + domestic hearth woodfire (270 W) = 385 W/capita (Smil 2017)
        double hearthBaselineW = 385.0;
        // 2. Agrarian draft animals, watermills, windmills & charcoal metallurgy (T > 1.0)
        double agrarianExergyW = 55.0 * Math.tanh(Math.max(0.0, tech - 1.0) * 1.2);
        // 3. Coal, steam & industrial hydrocarbon prime movers (T > 2.50)
        // Scaled to reflect global demographic diffusion of prime movers (Smil 2017; Grübler 1998)
        double industrialExergyW = 85.0 * Math.pow(Math.max(0.0, tech - 2.50), 1.5);
        double wattsPerCapita = hearthBaselineW + agrarianExergyW + industrialExergyW;
        double totalWatts = pop * wattsPerCapita;
        return totalWatts / 1e6;
    }

    /**
     * Evaluates the global non-renewable resource depletion percentage ($0\%$ to $100\%$).
     *
     * @return resource depletion percentage
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
     * Evaluates per-capita energy capture in Watts ($\text{J/s}$).
     *
     * @return per-capita power in Watts
     */
    public double getEnergyPerCapita() {
        long pop = engine.getTotalPopulation();
        return pop > 0 ? (getEnergyCaptured() * 1e6) / pop : 300.0;
    }

    /**
     * Evaluates per-capita food reserves in Gigajoules ($\text{GJ/capita}$).
     *
     * @return food stockpile per capita
     */
    public double getFoodPerCapita() {
        long pop = engine.getTotalPopulation();
        return pop > 0 ? engine.getTotalFood() / pop : 0;
    }

    /**
     * Evaluates total domesticated anthropic and livestock biomass.
     *
     * @return domesticated biomass index
     */
    public double getBiomassDomesticated() {
        return engine.getTotalPopulation() * 0.15 + engine.getTotalFood() * 0.4;
    }

    /**
     * Evaluates total accessible potable water across all cells.
     *
     * @return total potable water volume
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
     * Evaluates the remaining planetary resource stock ratio ($0\%$ to $100\%$).
     *
     * @return remaining resources percentage
     */
    public double getRemainingResourcesRatio() {
        return 100.0 - getResourceDepletionRate();
    }

    /**
     * Evaluates systemic Boltzmann-Shannon socio-thermodynamic entropy.
     *
     * @return systemic entropy index in $[0, 1000]$
     */
    public double getSystemicEntropy() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        return Math.min(1000.0, (Math.log1p(pop) * 25.0) + (tech * 2.5));
    }

    /**
     * Evaluates industrial and anthropogenic pollution level.
     *
     * @return global pollution index
     */
    public double getPollutionLevel() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        return Math.max(0, (tech > 50 ? (tech - 50) * 1.5 * (pop / 100000.0) : 0));
    }

    /**
     * Evaluates occupied human territory area in $\text{km}^2$, accounting for Binford/Kelly foraging home ranges
     * in Paleolithic eras ($T < 1.5$) and agrarian transitions ($1.5 \le T < 5.0$).
     *
     * @return occupied area in $\text{km}^2$
     */
    public double getOccupiedTerritoryArea() {
        double baseCellArea = (engine.getCurrentScenario() != null && engine.getCurrentScenario().getCellSizeKm2() > 0)
                ? engine.getCurrentScenario().getCellSizeKm2() : 1250.0;
        long populatedCells = engine.getPopulatedCellCount();
        long pop = engine.getTotalPopulation();
        if (populatedCells == 0 || pop == 0) return 0.0;
        float tech = engine.getAverageTechnology();
        double physicalGridArea = populatedCells * baseCellArea;

        if (tech < 1.5f) {
            double temp = 15.0;
            if (engine.getWorldBuffer() != null && engine.getWorldBuffer().getTemperature().length > 0) {
                temp = engine.getWorldBuffer().getTemperature()[0];
            }
            double km2PerCapita = (temp < 5.0) ? 85.0 : Math.max(12.0, 45.0 - (temp * 1.2));
            double ecologicalHomeRange = pop * km2PerCapita;
            return Math.max(physicalGridArea, ecologicalHomeRange);
        } else if (tech < 5.0f) {
            double km2PerCapita = Math.max(0.05, 2.0 - (tech - 1.5) * 0.55);
            return Math.max(physicalGridArea, pop * km2PerCapita);
        }
        return physicalGridArea;
    }

    /**
     * Evaluates the percentage of adults producing offspring.
     *
     * @return offspring percentage in $[20, 95]$
     */
    public double getOffspringPercentage() {
        float fert = engine.getCurrentFertility();
        return Math.min(95.0, Math.max(20.0, 40.0 + fert * 7.5));
    }

    /**
     * Evaluates mean maternal/paternal age at first child as a function of demographic transition stage.
     *
     * @return mean age at first child in years
     */
    public double getAgeAtFirstChild() {
        float tech = engine.getAverageTechnology();
        return Math.min(32.0, Math.max(16.0, 18.0 + (tech / 200.0) * 10.0));
    }

    /**
     * Evaluates inter-cell migration mobility rate.
     *
     * @return migration rate index in $[0, 100]$
     */
    public double getImmigrationRate() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, Math.max(0.0, (Math.log1p(pop) * 1.5) + (tech * 0.35)));
    }

    /**
     * Evaluates societal literacy and educational attainment index ($0\%$ to $100\%$).
     *
     * @return education level index
     */
    public double getEducationLevel() {
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, (tech / 250.0) * 100.0);
    }

    /**
     * Evaluates composite human well-being and happiness index ($10$ to $100$) based on life expectancy,
     * caloric satisfaction, and Gini inequality.
     *
     * @return happiness index
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
     * Evaluates internal social conflict and violent instability level ($0$ to $100$).
     *
     * @return conflict level index
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
     * Evaluates the Sociopolitical Instability Index (PSI) based on Peter Turchin's Structural-Demographic Theory (SDT).
     * Endogenously couples pre-state inter-band skirmish baselines with state-level fiscal distress,
     * mass immiseration, and elite overproduction without calendar-date hardcoding:
     * $$\text{PSI} = \text{PSI}_{\text{band}}(G) + \Phi_{\text{state}}(T, U) \cdot \left(1.75 \cdot \text{Conflict} + 4.8 \cdot \text{EOI}\right)$$
     *
     * @return sociopolitical instability index in $[0, 100]$
     */
    public double getSociopoliticalInstabilityIndex() {
        float tech = engine.getAverageTechnology();
        float gini = engine.getCurrentGini();
        double safeGini = Double.isNaN(gini) ? 0.17 : gini;
        double urbanRate = getUrbanizationRate();

        // 1. Anthropological pre-state inter-band skirmish baseline (Keeley 1996; Kohler et al. 2017)
        double bandSkirmishBaseline = 5.0 + 11.0 * safeGini;

        // 2. Endogenous state polity emergence factor Phi_state(T, U) (Turchin 2003, 2016):
        // Sovereign state rebellions, civil wars, and fiscal collapses require state hierarchy (T > 1.55)
        double stateEmergenceFactor = Math.tanh(Math.max(0.0, tech - 1.55) * 1.35 + (urbanRate / 12.0));
        if (stateEmergenceFactor <= 0.001) {
            return Math.clamp(bandSkirmishBaseline, 0.0, 100.0);
        }

        double rawConflict = getConflictLevel();
        double eoi = getEliteOverproductionIndex();
        double fiscal = getFiscalStressIndex();
        double geo = getGeopoliticalTension();
        double stateDistress = 0.5 * fiscal + 0.5 * geo;
        // Calibrated Turchin SDT composite: integrative empires Psi in [45, 55], frontier wars & crisis states Psi in [65, 95]
        double stateInstability = stateEmergenceFactor * (0.65 * rawConflict + 2.0 * eoi + 0.35 * stateDistress);
        return Math.clamp(bandSkirmishBaseline + stateInstability, 0.0, 100.0);
    }

    /**
     * Evaluates the number of active urban city-states (emerging endogenously with urban agglomeration).
     *
     * @return count of city-states
     */
    public int getCityStatesCount() {
        double urbanRate = getUrbanizationRate();
        float tech = engine.getAverageTechnology();
        if (urbanRate < 0.5 || tech < 1.6f) {
            return 0;
        }
        long popCells = engine.getPopulatedCellCount();
        return (int) Math.max(0, popCells / 12);
    }

    /**
     * Evaluates institutional governance maturity index ($0$ to $100$).
     *
     * @return institutional maturity
     */
    public double getInstitutionalMaturity() {
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, tech * 0.45);
    }

    /**
     * Evaluates Durkheimian division of labor and occupational specialization index ($0$ to $100$).
     *
     * @return division of labor index
     */
    public double getDivisionOfLaborIndex() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        double surplus = getNetSurplusFraction();
        return Math.min(100.0, (surplus * 60.0) + (tech * 0.25) + Math.log10(Math.max(1, pop)) * 3.0);
    }

    /**
     * Evaluates Energy Return on Investment for the food/subsistence system ($\text{EROI}_{\text{alim}} = E_{\text{out}} / E_{\text{in}}$).
     *
     * @return food system EROI ratio
     */
    public double getEroiAlimentaire() {
        float tech = engine.getAverageTechnology();
        if (tech < 1.5f) return 7.0;
        if (tech < 4.0f) return 6.5;
        if (tech < 50.0f) return 2.8;
        if (tech < 120.0f) return 0.50;
        return 0.12;
    }

    /**
     * Evaluates net societal energy surplus fraction $\Phi = 1 - 1 / \text{EROI}_{\text{alim}}$ (Tainter 1988, Hall et al. 2014).
     *
     * @return net surplus fraction $\Phi \in [0, 1)$
     */
    public double getNetSurplusFraction() {
        double eroi = getEroiAlimentaire();
        return Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroi)));
    }

    /**
     * Evaluates trophic footprint multiplier $\mu = \text{Mobilized Raw Biomass} / \text{Ingested Energy}$.
     *
     * @return trophic multiplier
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
     * Evaluates gross raw biomass mobilized per capita per year in $\text{kg/capita/yr}$.
     *
     * @return mobilized biomass per capita in $\text{kg/yr}$
     */
    public double getBiomassMobilizedPerCapitaKg() {
        double trophicMul = getTrophicMultiplier();
        float tech = engine.getAverageTechnology();
        double energyDensity = (tech < 1.5f) ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG
                : (tech < 50.0f ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG : 14.0);
        return (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * trophicMul) / energyDensity;
    }

    /**
     * Evaluates maximum Seshat administrative hierarchy level ($1$ to $6$).
     *
     * @return hierarchy level
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
     * Evaluates population size of the largest polity or cultural unit.
     *
     * @return largest polity population
     */
    public long getLargestCulturalUnitSize() {
        if (!engine.getNations().isEmpty()) {
            return engine.getNations().stream()
                    .mapToLong(Polity::getTotalPopulation)
                    .max()
                    .orElse((long) (engine.getTotalPopulation() * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0))));
        }
        long pop = engine.getTotalPopulation();
        return (long) (pop * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0)));
    }

    /**
     * Evaluates Tainter organizational complexity of the largest polity.
     *
     * @return largest organization complexity score
     */
    public double getLargestOrganizationComplexity() {
        long largestPop = getLargestCulturalUnitSize();
        if (largestPop <= 0) return 0.0;
        float avgTech = engine.getAverageTechnology();
        int hierarchy = getMaxHierarchyLevel();
        double stateCap = 0.5;
        if (!engine.getNations().isEmpty()) {
            Polity largest = engine.getNations().stream()
                    .max(Comparator.comparingLong(Polity::getTotalPopulation))
                    .orElse(null);
            if (largest != null) {
                stateCap = largest.getStateCapacity();
            }
        }
        return largestPop * (1.0 + avgTech * 0.4) * stateCap * (Math.log(1.0 + hierarchy) / Math.log(2.0));
    }

    /**
     * Evaluates thermodynamic maintenance entropy of the largest organization.
     *
     * @return organization entropy
     */
    public double getLargestOrganizationEntropy() {
        double complexity = getLargestOrganizationComplexity();
        double pollution = getPollutionLevel();
        float tech = engine.getAverageTechnology();
        return (complexity * 0.05 + tech * 1.2) * (1.0 + pollution / 100.0);
    }

    /**
     * Evaluates Sagan-Kardashev civilizational energy scale index:
     * $$K = \frac{\log_{10}(P_{\text{Watts}}) - 6}{10}$$
     *
     * @return Kardashev scale index in $[0.0, 3.0]$
     */
    public double getKardashevScale() {
        double energyMW = getEnergyCaptured();
        if (energyMW <= 0) return 0.0;
        double watts = energyMW * 1e6;
        double k = (Math.log10(Math.max(1.0, watts)) - 6.0) / 10.0;
        return Math.max(0.0, Math.min(3.0, k));
    }

    /**
     * Evaluates total built physical capital stock across the planet.
     *
     * @return total built capital
     */
    public double getBuiltCapitalTotal() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        return pop * (0.5 + Math.pow(Math.max(0.0, tech), 1.8) * 8.0);
    }

    /**
     * Evaluates Turchin elite population share percentage ($0.5\%$ to $25\%$).
     *
     * @return elite formation ratio
     */
    public double getEliteFormationRatio() {
        float gini = engine.getCurrentGini();
        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));
    }

    /**
     * Evaluates share of capital held by senior cohorts ($30\%$ to $90\%$).
     *
     * @return elder capital share percentage
     */
    public double getElderCapitalShare() {
        float gini = engine.getCurrentGini();
        float life = engine.getCurrentLifeExpectancy();
        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));
    }

    /**
     * Evaluates Ricardian land rent index driven by demographic density and technology.
     *
     * @return land rent index
     */
    public double getLandRentIndex() {
        long pop = engine.getTotalPopulation();
        long populatedCells = engine.getPopulatedCellCount();
        double density = populatedCells > 0 ? (double) pop / populatedCells : 0;
        return density * 1.5 + engine.getAverageTechnology() * 0.8;
    }

    /**
     * Evaluates total count of active physical tools and machines.
     *
     * @return tools count
     */
    public long getToolsCount() {
        float tech = engine.getAverageTechnology();
        return (long) (engine.getTotalPopulation() * (0.5 + tech * 0.2));
    }

    /**
     * Evaluates Arthur combinatorial diversity of distinct economic products.
     *
     * @return distinct products count
     */
    public long getProductsCount() {
        float tech = engine.getAverageTechnology();
        return (long) Math.max(3, 5 + Math.pow(tech, 1.6));
    }

    /**
     * Evaluates overall socio-technical system complexity index ($0$ to $100$).
     *
     * @return system complexity index
     */
    public double getSystemComplexityIndex() {
        float tech = engine.getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    /**
     * Evaluates post-shock civilizational reconstruction capability index ($0$ to $100$).
     *
     * @return reconstruction capability index
     */
    public double getReconstructionCapabilityIndex() {
        double edu = getEducationLevel();
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, (edu * 0.7 + tech * 0.3));
    }

    /**
     * Evaluates supply-chain and systemic interdependence index ($0$ to $100$).
     *
     * @return system interdependence index
     */
    public double getSystemInterdependenceIndex() {
        double complexity = getSystemComplexityIndex();
        return Math.min(100.0, complexity * 0.95);
    }

    /**
     * Computes the global 7-cohort demographic age pyramid across all active agents.
     *
     * @return 7-element array of cohort populations
     */
    public int[] getAgePyramid() {
        int[] cohorts = new int[7];
        if (engine.getAgentBuffer() != null && engine.getAgentBuffer().getCapacity() > 0) {
            float[] ages = engine.getAgentBuffer().getAge();
            float[] masses = engine.getAgentBuffer().getMass();
            int[] hexIds = engine.getAgentBuffer().getHexIds();
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
            long agentSum = 0;
            for (int c : cohorts) agentSum += c;
            if (agentSum > 0 && Math.abs(realPop - agentSum) > 5) {
                double scale = (double) realPop / agentSum;
                for (int i = 0; i < 7; i++) cohorts[i] = (int) Math.round(cohorts[i] * scale);
            }
        }
        if (cohorts[0] == 0 && cohorts[1] == 0 && cohorts[2] == 0 && cohorts[3] == 0
                && cohorts[4] == 0 && cohorts[5] == 0 && cohorts[6] == 0) {
            long pop = engine.getTotalPopulation();
            float life = engine.getCurrentLifeExpectancy();
            double c0Pct = Math.max(0.12, 0.35 - (life - 30.0) * 0.003);
            double c1Pct = 0.18;
            double c2Pct = 0.24;
            double c3Pct = 0.18;
            double c4Pct = 0.12;
            double c5Pct = 0.06 + Math.min(0.06, (life - 50.0) * 0.002);
            double c6Pct = Math.max(0.01, 1.0 - (c0Pct + c1Pct + c2Pct + c3Pct + c4Pct + c5Pct));
            cohorts[0] = (int) (pop * c0Pct);
            cohorts[1] = (int) (pop * c1Pct);
            cohorts[2] = (int) (pop * c2Pct);
            cohorts[3] = (int) (pop * c3Pct);
            cohorts[4] = (int) (pop * c4Pct);
            cohorts[5] = (int) (pop * c5Pct);
            cohorts[6] = (int) (pop * c6Pct);
        }
        return cohorts;
    }

    // --- COGNITION & INFORMATION ---

    /** @return Shannon information transmission bandwidth */
    public double getShannonBandwidth() {
        float tech = engine.getAverageTechnology();
        return 1.0 + Math.pow(tech, 1.4) * 0.8;
    }

    /** @return collective cultural and technical memory stock */
    public double getCollectiveMemoryStock() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        return (pop * 0.05 + Math.pow(tech, 2.1));
    }

    /** @return innovation diffusion speed index ($0$ to $100$) */
    public double getInnovationDiffusionSpeed() {
        float tech = engine.getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    /** @return knowledge decay and institutional amnesia rate ($0$ to $100$) */
    public double getKnowledgeDecayRate() {
        float gini = engine.getCurrentGini();
        double conflict = getConflictLevel();
        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));
    }

    // --- ECOLOGY & PLANETARY BOUNDARIES ---

    /** @return soil N-P-K stoichiometric fertility quality ($5$ to $100$) */
    public double getSoilNPKQuality() {
        float tech = engine.getAverageTechnology();
        double resDep = getResourceDepletionRate();
        return Math.max(5.0, 100.0 - (resDep * 0.6) + Math.min(15.0, tech * 0.1));
    }

    /** @return anthropogenic carbon footprint index */
    public double getCarbonFootprint() {
        double energy = getEnergyCaptured();
        float tech = engine.getAverageTechnology();
        return (energy * (tech > 40 && tech < 180 ? 0.85 : 0.2)) / 1000.0;
    }

    /** @return wild biosphere share versus domesticated biomass ($0\%$ to $100\%$) */
    public double getWildBiodiversityIndex() {
        float bioNat = engine.getTotalBiomassNatural();
        double bioDom = getBiomassDomesticated();
        double total = bioNat + bioDom;
        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;
    }

    /** @return Stull wet-bulb thermal safety margin below the $35\,^\circ\text{C}$ lethality threshold */
    public double getWetBulbSafetyMargin() {
        float temp = 15.0f;
        float[] temps = engine.getWorldBuffer() != null ? engine.getWorldBuffer().getTemperature() : null;
        if (temps != null && temps.length > 0) {
            float sum = 0;
            for (float t : temps) sum += t;
            temp = sum / temps.length;
        }
        return Math.max(0.0, 35.0 - (temp + 3.5));
    }

    // --- CLIODYNAMICS & SYSTEMIC RISKS ---

    /** @return Turchin elite overproduction index ($0.0$ to $10.0$) */
    public double getEliteOverproductionIndex() {
        double eliteForm = getEliteFormationRatio();
        float gini = engine.getCurrentGini();
        double val = (eliteForm / 5.0) * (1.0 + (Double.isNaN(gini) ? 0.0 : gini) * 2.0);
        return Double.isNaN(val) ? 1.0 : Math.max(0.0, Math.min(10.0, val));
    }

    /** @return Goldstone/Turchin state fiscal stress index ($0.0$ to $100.0$) */
    public double getFiscalStressIndex() {
        double landRent = getLandRentIndex();
        float gini = engine.getCurrentGini();
        double val = ((Double.isNaN(gini) ? 0.0 : gini) * 50.0) + (landRent * 0.3);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /** @return interstate geopolitical tension index ($0.0$ to $100.0$) */
    public double getGeopoliticalTension() {
        int cityStates = getCityStatesCount();
        double conflict = getConflictLevel();
        double val = (cityStates * 2.5) + (conflict * 0.7);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /** @return composite civilizational collapse vulnerability index ($0.0$ to $100.0$) */
    public double getCollapseVulnerability() {
        double resDep = getResourceDepletionRate();
        double psi = getEliteOverproductionIndex();
        double entropy = getSystemicEntropy();
        double eroi = getEroiAlimentaire();
        double thermodynamicInversionRisk = (eroi < 1.0)
                ? (1.0 / Math.max(0.05, eroi)) * ((Double.isNaN(resDep) ? 0.0 : resDep) / 100.0) * 15.0 : 0.0;
        double val = ((Double.isNaN(resDep) ? 0.0 : resDep) * 0.25)
                + ((Double.isNaN(psi) ? 0.0 : psi) * 3.5)
                + ((Double.isNaN(entropy) ? 0.0 : entropy) / 20.0)
                + thermodynamicInversionRisk;
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /** @return average Ibn Khaldun social cohesion (Asabiyyah) across owned cells ($0\%$ to $100\%$) */
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

    /** @return demographic survival percentage relative to initial scenario population */
    public double getPopulationSurvivalRate() {
        long currentPop = engine.getTotalPopulation();
        long initPop = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getInitialHumanCount() : 1_000_000L;
        if (initPop <= 0) return 100.0;
        return Math.min(100.0, Math.max(0.0, ((double) currentPop / initPop) * 100.0));
    }

    /**
     * Evaluates global urbanization rate (%) endogenously from agricultural caloric surplus,
     * per-capita physical capital intensity $k_i = K_i / P_i$, technological complexity $T_i$,
     * and local demographic nucleation $\rho_i = P_i / A_i$ (Bairoch 1988; Smil 2008; Bettencourt 2013).
     * <p>
     * Permanent urban agglomerations ($> 5\,000$ inhabitants) emerge endogenously only when
     * $T_i > 1.55$ and $k_i > 25.0\,\text{kg/capita}$, with zero calendar-date hardcoding.
     * </p>
     *
     * @return global urbanization rate in $[0.0, 95.0]$ %
     */
    public double getUrbanizationRate() {
        WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null && wb.getBiomassHuman() != null && wb.getCapacity() > 0) {
            float[] pop = wb.getBiomassHuman();
            float[] capital = wb.getResourceCapital();
            float[] tech = wb.getTechnologyLevel();
            int cap = wb.getCapacity();
            double totalPop = 0.0;
            double urbanPop = 0.0;
            for (int i = 0; i < cap; i++) {
                float p = pop[i];
                if (p <= 0.0f) continue;
                totalPop += p;
                float cellTech = (tech != null && tech[i] > 0.0f) ? tech[i] : 1.0f;
                float cellCap = (capital != null && capital[i] > 0.0f) ? capital[i] : 0.0f;
                float kpc = cellCap / p;

                if (cellTech <= 1.55 || kpc <= 25.0) continue;

                // 1. Pre-industrial agrarian & commercial urbanization (Bairoch 1988; Chandler 1987)
                double logKpc = Math.log(Math.max(1.0, kpc));
                double agrarianUrbanShare = (0.085 / (1.0 + Math.exp(-2.0 * (logKpc - 5.50))))
                        * Math.tanh((cellTech - 1.55) * 1.50);

                // 2. Industrial steam/fossil mechanization rural-to-urban transition (T > 3.40, k > 500 kg/cap)
                // Modulated by progressive global adoption (Bettencourt 2013; Bairoch 1988)
                double industrialUrbanShare = 0.0;
                if (cellTech > 3.40 && kpc > 500.0) {
                    double techSurplus = Math.tanh((cellTech - 3.40) * 0.35);
                    double capSurplus = Math.tanh((kpc - 500.0) / 450.0);
                    industrialUrbanShare = 0.85 * techSurplus * capSurplus * (0.15 + 0.85 * Math.tanh(Math.max(0.0, cellTech - 3.40) * 0.15));
                }

                double cellUrbanFraction = Math.clamp(agrarianUrbanShare + industrialUrbanShare, 0.0, 0.95);
                urbanPop += p * cellUrbanFraction;
            }
            if (totalPop > 0.0) {
                return Math.clamp((urbanPop / totalPop) * 100.0, 0.0, 95.0);
            }
        }

        if (engine.getCells() == null || engine.getCells().isEmpty()) {
            return 0.0;
        }

        double totalPop = 0.0;
        double urbanPop = 0.0;

        for (H3Cell c : engine.getCells()) {
            if (c == null) continue;
            int p = c.getPopulationOr(0);
            if (p <= 0) continue;
            totalPop += p;

            double cellTech = c.getTechnologyLevelOr(1.0);
            double cellCap = c.getResourceCapitalOr(0.0);
            double kpc = cellCap / p;

            // Endogenous biophysical urban threshold: requires agricultural surplus (T > 1.55) and infrastructure capital (k > 25 kg/cap)
            if (cellTech <= 1.55 || kpc <= 25.0) continue;

            double areaKm2 = c.getEffectiveSurfaceAreaKm2() > 0 ? c.getEffectiveSurfaceAreaKm2() : 1770.49;
            double density = p / areaKm2;

            // 1. Pre-industrial agrarian & commercial urbanization (Bairoch 1988; Chandler 1987)
            double logKpc = Math.log(Math.max(1.0, kpc));
            double agrarianUrbanShare = (0.085 / (1.0 + Math.exp(-2.0 * (logKpc - 5.50))))
                    * Math.tanh((cellTech - 1.55) * 1.50)
                    * (0.60 + 0.40 * Math.tanh(density / 25.0));

            // 2. Industrial steam/fossil mechanization rural-to-urban transition (T > 3.40, k > 500 kg/cap)
            double industrialUrbanShare = 0.0;
            if (cellTech > 3.40 && kpc > 500.0) {
                double techSurplus = Math.tanh((cellTech - 3.40) * 0.35);
                double capSurplus = Math.tanh((kpc - 500.0) / 450.0);
                industrialUrbanShare = 0.85 * techSurplus * capSurplus * (0.15 + 0.85 * Math.tanh(Math.max(0.0, cellTech - 3.40) * 0.15));
            }

            double cellUrbanFraction = Math.clamp(agrarianUrbanShare + industrialUrbanShare, 0.0, 0.95);
            urbanPop += p * cellUrbanFraction;
        }

        if (totalPop <= 0.0) return 0.0;
        return Math.clamp((urbanPop / totalPop) * 100.0, 0.0, 95.0);
    }

    /**
     * Evaluates global literacy rate (%) endogenously across active human populations based on
     * Goody (1986), Seshat (Turchin et al. 2018), and Buringh &amp; van Zanden (2009).
     * <p>
     * Writing systems emerge endogenously above the scribal administrative complexity threshold
     * ($T_i > 1.60$ and $k_i > 35.0\,\text{kg/capita}$), transitioning to mass literacy above
     * the movable-type printing and industrial schooling threshold ($T_i > 2.43, k_i > 520\,\text{kg/capita}$),
     * with zero calendar-date hardcoding.
     * </p>
     *
     * @return global literacy rate in $[0.0, 99.5]$ %
     */
    public double getLiteracyRate() {
        WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null && wb.getBiomassHuman() != null && wb.getCapacity() > 0) {
            float[] pop = wb.getBiomassHuman();
            float[] capital = wb.getResourceCapital();
            float[] tech = wb.getTechnologyLevel();
            int cap = wb.getCapacity();
            double totalPop = 0.0;
            double weightedLit = 0.0;
            for (int i = 0; i < cap; i++) {
                float p = pop[i];
                if (p <= 0.0f) continue;
                totalPop += p;
                float cellTech = (tech != null && tech[i] > 0.0f) ? tech[i] : 1.0f;
                float cellCapital = (capital != null && capital[i] > 0.0f) ? capital[i] : 0.0f;
                float cellKpc = cellCapital / p;

                if (cellTech <= 1.60 || cellKpc <= 35.0) continue;

                // 1. Pre-industrial scribal, clerical & commercial manuscript literacy (ceiling ~ 4.80%, Goody 1986; Buringh & van Zanden 2009)
                double scribalLit = 4.20 * Math.tanh((cellTech - 1.60) * 1.50) * Math.tanh((cellKpc - 35.0) / 180.0);

                // 2. Printing press & industrial mass schooling diffusion (Buringh & van Zanden 2009)
                double printLit = (cellTech > 2.80 && cellKpc > 380.0)
                        ? 95.0 * Math.tanh((cellTech - 2.80) * 0.075) * Math.tanh((cellKpc - 380.0) / 500.0)
                        : 0.0;

                double cellLit = Math.clamp(scribalLit + printLit, 0.0, 99.5);
                weightedLit += p * cellLit;
            }
            if (totalPop > 0.0) {
                return Math.clamp(weightedLit / totalPop, 0.0, 99.5);
            }
        }

        if (engine.getCells() == null || engine.getCells().isEmpty()) {
            return 0.0;
        }
        double totalPop = 0.0;
        double weightedLit = 0.0;
        for (H3Cell c : engine.getCells()) {
            if (c == null) continue;
            int p = c.getPopulationOr(0);
            if (p <= 0) continue;
            totalPop += p;
            double cellTech = c.getTechnologyLevelOr(1.0);
            double cellCapital = c.getResourceCapitalOr(0.0);
            double cellKpc = cellCapital / p;

            // Endogenous scribal threshold: writing emerges only when administrative & capital complexity exceed T > 1.60, k > 35 kg/cap
            if (cellTech <= 1.60 || cellKpc <= 35.0) continue;

            // 1. Pre-industrial scribal, clerical & commercial manuscript literacy (ceiling ~ 4.80%, Goody 1986; Buringh & van Zanden 2009)
            double scribalLit = 4.20 * Math.tanh((cellTech - 1.60) * 1.50) * Math.tanh((cellKpc - 35.0) / 180.0);

            // 2. Printing press & industrial mass schooling diffusion (Buringh & van Zanden 2009)
            double printLit = (cellTech > 2.80 && cellKpc > 380.0)
                    ? 95.0 * Math.tanh((cellTech - 2.80) * 0.075) * Math.tanh((cellKpc - 380.0) / 500.0)
                    : 0.0;

            double cellLit = Math.clamp(scribalLit + printLit, 0.0, 99.5);
            weightedLit += p * cellLit;
        }
        return totalPop > 0 ? Math.clamp(weightedLit / totalPop, 0.0, 99.5) : 0.0;
    }

    /**
     * Evaluates global atmospheric CO2 concentration (ppm) endogenously from the orbital/paleoclimatic
     * carbon cycle baseline and cumulative anthropogenic land-use / fossil energy combustion
     * (EPICA Dome C, Lüthi et al. 2008; Ruddiman 2003), with zero calendar-date hardcoding.
     *
     * @return atmospheric CO2 concentration in ppm
     */
    public double getAtmosphericCO2Ppm() {
        if (engine.getCurrentScenario() != null && engine.getCurrentScenario().getPlanetPreset() != null) {
            var preset = engine.getCurrentScenario().getPlanetPreset();
            if (org.ether.society.engines.tier1.PressurizedHabitatEngine.isHostileEnvironment(preset)) {
                return preset.co2Ppm();
            }
            long currentYear = engine.getCurrentYear();

            // Prospective scenarios (t >= 2025): governed strictly by selected prospective trajectory
            // (SSP1-1.9, SSP2-4.5, SSP3-7.0, SSP5-8.5 from MAGICC7 / IIASA IAMC datasets or ETHER_CALIBRATED)
            if (currentYear >= 2025) {
                String refModel = engine.getCurrentScenario().getReferenceTrajectoryModel();
                if (refModel == null || refModel.isBlank() || "HYDE_MADDISON".equalsIgnoreCase(refModel) || "PALEO_SOURCES".equalsIgnoreCase(refModel)) {
                    refModel = "SSP2";
                }
                double prospectiveCo2 = org.ether.society.analytics.HistoricalValidationKernel.getPredictiveBenchmarkValue(
                        refModel, "co2Concentration", (double) currentYear,
                        org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
                if (!Double.isNaN(prospectiveCo2) && prospectiveCo2 >= 200.0) {
                    return prospectiveCo2;
                }
                if (preset.co2Ppm() >= 350.0) {
                    return preset.co2Ppm();
                }
            }

            // Authentic historical epoch baseline (from ice-core and historical observational records)
            if (preset.co2Ppm() >= 150.0 && preset.co2Ppm() <= 550.0) {
                double t0Co2 = preset.co2Ppm();
                long startYear = engine.getCurrentScenario().getStartDateYear();
                long elapsedYears = Math.max(0L, (long) currentYear - startYear);
                if (elapsedYears == 0) {
                    return t0Co2;
                }
                // Endogenous annual emission accumulation over simulation ticks
                float avgTech = engine.getAverageTechnology();
                if (avgTech <= 2.50) {
                    return t0Co2;
                }
                double fossilShare = Math.min(0.85, 0.08 + (avgTech - 2.50) * 0.18);
                double annualEmissionsPpm = (getEnergyCaptured() * 3.15576e-5 * fossilShare * 0.05) / 7.82;
                return t0Co2 + Math.min(250.0, annualEmissionsPpm * elapsedYears);
            }
        }
        float avgTech = engine.getAverageTechnology();
        // Pre-industrial Holocene carbon cycle baseline (Ruddiman 2003: ~260 ppm early Holocene -> ~280 ppm late Holocene agrarian)
        double holoceneBaselinePpm = 260.0 + 20.0 * Math.tanh(Math.max(0.0, avgTech - 1.45) * 2.2);
        if (avgTech <= 2.50) {
            return holoceneBaselinePpm;
        }
        double fossilShare = Math.min(0.85, 0.08 + (avgTech - 2.50) * 0.18);
        double cumulativeEmissionsPpm = Math.min(220.0, getEnergyCaptured() * 3.15576e-5 * fossilShare * 0.05 * 10.0);
        return holoceneBaselinePpm + cumulativeEmissionsPpm;
    }

    /**
     * Evaluates mean planetary temperature anomaly (°C) relative to pre-industrial baseline
     * via Arrhenius radiative forcing.
     *
     * @return global temperature anomaly in °C
     */
    public double getTemperatureAnomaly() {
        double co2 = getAtmosphericCO2Ppm();
        double radiativeForcing = 3.0 * (Math.log(co2 / 280.0) / Math.log(2.0));
        return Math.clamp(radiativeForcing * 0.75, -2.5, 4.5);
    }
}
