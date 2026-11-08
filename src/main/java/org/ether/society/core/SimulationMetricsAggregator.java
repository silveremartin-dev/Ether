/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.metrics.CliodynamicMetricsCalculator;
import org.ether.society.core.metrics.CulturalKnowledgeMetricsCalculator;
import org.ether.society.core.metrics.DemographicMetricsCalculator;
import org.ether.society.core.metrics.EnvironmentalMetricsCalculator;
import org.ether.society.core.metrics.MacroeconomicMetricsCalculator;
import org.ether.society.core.metrics.ThermodynamicMetricsCalculator;

import java.util.Objects;

/**
 * <h1>SimulationMetricsAggregator</h1>
 * <p>
 * Aggregates and coordinates macroscopic, cliodynamic, thermodynamic, ecological,
 * and demographic metrics for the planetary simulation engine.<br>
 * Delegates domain-specific closed-form equations to {@link ThermodynamicMetricsCalculator},
 * {@link DemographicMetricsCalculator}, {@link EnvironmentalMetricsCalculator},
 * {@link CliodynamicMetricsCalculator}, {@link MacroeconomicMetricsCalculator}, and
 * {@link CulturalKnowledgeMetricsCalculator}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3SimulationEngine
 */
public final class SimulationMetricsAggregator {

    /** Owning H3 simulation engine reference. */
    private final H3SimulationEngine engine;
    /** Bioenergetic and thermodynamic calculator (Leslie White, Boltzmann-Shannon, EROI). */
    private final ThermodynamicMetricsCalculator thermodynamicCalculator;
    /** Demographic, migration, urbanization, and age-pyramid calculator. */
    private final DemographicMetricsCalculator demographicCalculator;
    /** Earth-system, soil stoichiometry, wet-bulb, and radiative calculator. */
    private final EnvironmentalMetricsCalculator environmentalCalculator;
    /** Structural-Demographic Theory (Turchin SDT), Asabiyyah, and hierarchy calculator. */
    private final CliodynamicMetricsCalculator cliodynamicCalculator;
    /** Macroeconomic capital, wage, coinage purity, debt, and trade calculator. */
    private final MacroeconomicMetricsCalculator macroeconomicCalculator;
    /** Cultural complexity, literacy, Shannon bandwidth, and ORBIS speed calculator. */
    private final CulturalKnowledgeMetricsCalculator culturalKnowledgeCalculator;

    /**
     * Constructs a new {@link SimulationMetricsAggregator} bound to the specified engine instance.
     *
     * @param engine parent {@link H3SimulationEngine}, must not be null
     */
    public SimulationMetricsAggregator(H3SimulationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.thermodynamicCalculator = new ThermodynamicMetricsCalculator(this, engine);
        this.demographicCalculator = new DemographicMetricsCalculator(this, engine);
        this.environmentalCalculator = new EnvironmentalMetricsCalculator(this, engine);
        this.cliodynamicCalculator = new CliodynamicMetricsCalculator(this, engine);
        this.macroeconomicCalculator = new MacroeconomicMetricsCalculator(this, engine);
        this.culturalKnowledgeCalculator = new CulturalKnowledgeMetricsCalculator(this, engine);
    }

    /** @return underlying {@link H3SimulationEngine} instance */
    public H3SimulationEngine getEngine() { return engine; }

    // =========================================================================
    // 1. THERMODYNAMIC & BIOENERGETIC METRICS
    // =========================================================================

    /** @return total power captured by human civilization in Megawatts ($\text{MW}$) */
    public double getEnergyCaptured() { return thermodynamicCalculator.getEnergyCaptured(); }

    /** @return global non-renewable resource depletion percentage ($0\%$ to $100\%$) */
    public double getResourceDepletionRate() { return thermodynamicCalculator.getResourceDepletionRate(); }

    /** @return per-capita energy capture in Watts ($\text{J/s}$) */
    public double getEnergyPerCapita() { return thermodynamicCalculator.getEnergyPerCapita(); }

    /** @return per-capita food reserves in Gigajoules ($\text{GJ/capita}$) */
    public double getFoodPerCapita() { return thermodynamicCalculator.getFoodPerCapita(); }

    /** @return total domesticated anthropic and livestock biomass index */
    public double getBiomassDomesticated() { return thermodynamicCalculator.getBiomassDomesticated(); }

    /** @return total accessible potable water volume across all cells */
    public double getPotableWaterTotal() { return thermodynamicCalculator.getPotableWaterTotal(); }

    /** @return remaining planetary resource stock ratio ($0\%$ to $100\%$) */
    public double getRemainingResourcesRatio() { return thermodynamicCalculator.getRemainingResourcesRatio(); }

    /** @return systemic Boltzmann-Shannon socio-thermodynamic entropy in $[0, 1000]$ */
    public double getSystemicEntropy() { return thermodynamicCalculator.getSystemicEntropy(); }

    /** @return global industrial and anthropogenic pollution index */
    public double getPollutionLevel() { return thermodynamicCalculator.getPollutionLevel(); }

    /** @return Energy Return on Investment for the food/subsistence system ($\text{EROI}_{\text{alim}}$) */
    public double getEroiAlimentaire() { return thermodynamicCalculator.getEroiAlimentaire(); }

    /** @return net societal energy surplus fraction $\Phi = 1 - 1 / \text{EROI}_{\text{alim}} \in [0, 1)$ */
    public double getNetSurplusFraction() { return thermodynamicCalculator.getNetSurplusFraction(); }

    /** @return continuous trophic footprint multiplier $\mu \in [1.05, 3.20]$ */
    public double getTrophicMultiplier() { return thermodynamicCalculator.getTrophicMultiplier(); }

    /** @return gross raw biomass mobilized per capita per year in $\text{kg/capita/yr}$ */
    public double getBiomassMobilizedPerCapitaKg() { return thermodynamicCalculator.getBiomassMobilizedPerCapitaKg(); }

    /** @return Sagan-Kardashev civilizational energy scale index in $[0.0, 3.0]$ */
    public double getKardashevScale() { return thermodynamicCalculator.getKardashevScale(); }

    /** @return total built physical capital stock across the planet */
    public double getBuiltCapitalTotal() { return thermodynamicCalculator.getBuiltCapitalTotal(); }

    /** @return total count of active physical tools and machines */
    public long getToolsCount() { return thermodynamicCalculator.getToolsCount(); }

    /** @return Arthur combinatorial diversity of distinct economic products */
    public long getProductsCount() { return thermodynamicCalculator.getProductsCount(); }

    // =========================================================================
    // 2. DEMOGRAPHIC & URBANIZATION METRICS
    // =========================================================================

    /** @return occupied human territory area in $\text{km}^2$ */
    public double getOccupiedTerritoryArea() { return demographicCalculator.getOccupiedTerritoryArea(); }

    /** @return percentage of adults producing offspring in $[20, 95]$ */
    public double getOffspringPercentage() { return demographicCalculator.getOffspringPercentage(); }

    /** @return mean maternal/paternal age at first child in years */
    public double getAgeAtFirstChild() { return demographicCalculator.getAgeAtFirstChild(); }

    /** @return inter-cell migration mobility rate index in $[0, 100]$ */
    public double getImmigrationRate() { return demographicCalculator.getImmigrationRate(); }

    /** @return composite human well-being and happiness index ($10$ to $100$) */
    public double getHappinessIndex() { return demographicCalculator.getHappinessIndex(); }

    /** @return demographic survival percentage relative to initial scenario population */
    public double getPopulationSurvivalRate() { return demographicCalculator.getPopulationSurvivalRate(); }

    /** @return endogenous global urbanization rate in $[0.0, 95.0]\%$ */
    public double getUrbanizationRate() { return demographicCalculator.getUrbanizationRate(); }

    /** @return global 7-cohort demographic age pyramid across all active agents */
    public int[] getAgePyramid() { return demographicCalculator.getAgePyramid(); }

    // =========================================================================
    // 3. ENVIRONMENTAL & EARTH SYSTEM METRICS
    // =========================================================================

    /** @return soil N-P-K stoichiometric fertility quality index ($5$ to $100$) */
    public double getSoilNPKQuality() { return environmentalCalculator.getSoilNPKQuality(); }

    /** @return anthropogenic carbon footprint index */
    public double getCarbonFootprint() { return environmentalCalculator.getCarbonFootprint(); }

    /** @return wild biosphere share versus domesticated biomass ($0\%$ to $100\%$) */
    public double getWildBiodiversityIndex() { return environmentalCalculator.getWildBiodiversityIndex(); }

    /** @return Stull wet-bulb thermal safety margin below $35\,^\circ\text{C}$ lethality in $^\circ\text{C}$ */
    public double getWetBulbSafetyMargin() { return environmentalCalculator.getWetBulbSafetyMargin(); }

    /** @return endogenous global atmospheric $\text{CO}_2$ concentration in ppm */
    public double getAtmosphericCO2Ppm() { return environmentalCalculator.getAtmosphericCO2Ppm(); }

    /** @return mean planetary temperature anomaly ($^\circ\text{C}$) relative to 1850-1900 baseline */
    public double getTemperatureAnomaly() { return environmentalCalculator.getTemperatureAnomaly(); }

    /** @return Montgomery (2007) cumulative topsoil erosion loss percentage */
    public double getTopsoilErosionLossPct() { return environmentalCalculator.getTopsoilErosionLossPct(); }

    /** @return agricultural EROEI ratio */
    public double getAgriculturalEroeiRatio() { return getEroiAlimentaire(); }

    /** @return HYDE 3.2 / FAO global forest cover remaining percentage */
    public double getForestCoverRemainingPct() { return environmentalCalculator.getForestCoverRemainingPct(); }

    /** @return fossil fuel share in primary energy percentage */
    public double getFossilSharePrimaryEnergyPct() { return environmentalCalculator.getFossilSharePrimaryEnergyPct(); }

    // =========================================================================
    // 4. CLIODYNAMICS & SYSTEMIC RISK METRICS
    // =========================================================================

    /** @return internal social conflict and violent instability level ($0$ to $100$) */
    public double getConflictLevel() { return cliodynamicCalculator.getConflictLevel(); }

    /** @return Sociopolitical Instability Index (PSI) based on Peter Turchin's SDT in $[0, 100]$ */
    public double getSociopoliticalInstabilityIndex() { return cliodynamicCalculator.getSociopoliticalInstabilityIndex(); }

    /** @return count of active urban city-states */
    public int getCityStatesCount() { return cliodynamicCalculator.getCityStatesCount(); }

    /** @return institutional governance maturity index ($0$ to $100$) */
    public double getInstitutionalMaturity() { return cliodynamicCalculator.getInstitutionalMaturity(); }

    /** @return Durkheimian division of labor and occupational specialization index ($0$ to $100$) */
    public double getDivisionOfLaborIndex() { return cliodynamicCalculator.getDivisionOfLaborIndex(); }

    /** @return Seshat administrative hierarchy complexity level in $[1, 6]$ */
    public int getMaxHierarchyLevel() { return cliodynamicCalculator.getMaxHierarchyLevel(); }

    /** @return population size of the largest polity or cultural unit */
    public long getLargestCulturalUnitSize() { return cliodynamicCalculator.getLargestCulturalUnitSize(); }

    /** @return Tainter organizational complexity score of the largest polity */
    public double getLargestOrganizationComplexity() { return cliodynamicCalculator.getLargestOrganizationComplexity(); }

    /** @return thermodynamic maintenance entropy of the largest organization */
    public double getLargestOrganizationEntropy() { return cliodynamicCalculator.getLargestOrganizationEntropy(); }

    /** @return Turchin elite population share percentage ($0.5\%$ to $25\%$) */
    public double getEliteFormationRatio() { return cliodynamicCalculator.getEliteFormationRatio(); }

    /** @return share of capital held by senior cohorts ($30\%$ to $90\%$) */
    public double getElderCapitalShare() { return cliodynamicCalculator.getElderCapitalShare(); }

    /** @return Turchin elite overproduction index ($0.0$ to $4.5$) */
    public double getEliteOverproductionIndex() { return cliodynamicCalculator.getEliteOverproductionIndex(); }

    /** @return Goldstone/Turchin state fiscal stress index ($0.0$ to $100.0$) */
    public double getFiscalStressIndex() { return cliodynamicCalculator.getFiscalStressIndex(); }

    /** @return interstate geopolitical tension index ($0.0$ to $100.0$) */
    public double getGeopoliticalTension() { return cliodynamicCalculator.getGeopoliticalTension(); }

    /** @return composite civilizational collapse vulnerability index ($0.0$ to $100.0$) */
    public double getCollapseVulnerability() { return cliodynamicCalculator.getCollapseVulnerability(); }

    /** @return average Ibn Khaldun social cohesion (Asabiyyah) across owned cells ($0\%$ to $100\%$) */
    public double getAverageAsabiyyah() { return cliodynamicCalculator.getAverageAsabiyyah(); }

    /** @return Turchin &amp; Nefedov (2009) Political Stress Index in $[0.0, 100.0]$ */
    public double getPoliticalStressIndex() { return cliodynamicCalculator.getPoliticalStressIndex(); }

    /** @return Ibn Khaldun (1377) normalized Asabiyyah collective solidarity score in $[0.0, 1.0]$ */
    public double getAsabiyyahSocialCohesionScore() { return cliodynamicCalculator.getAsabiyyahSocialCohesionScore(); }

    // =========================================================================
    // 5. MACROECONOMIC & CAPITAL METRICS
    // =========================================================================

    /** @return Ricardian land rent index */
    public double getLandRentIndex() { return macroeconomicCalculator.getLandRentIndex(); }

    /** @return silver coinage purity in $[0.0, 99.0]\%$ */
    public double getCurrencyDebasement() { return macroeconomicCalculator.getCurrencyDebasement(); }

    /** @return Robert C. Allen (2001) real unskilled worker purchasing power index */
    public double getRealUnskilledWageIndex() { return macroeconomicCalculator.getRealUnskilledWageIndex(); }

    /** @return Reinhart &amp; Rogoff (2009) sovereign debt burden in $[0.0, 250.0]\%$ of GWP */
    public double getSovereignDebtBurdenPct() { return macroeconomicCalculator.getSovereignDebtBurdenPct(); }

    /** @return Federico &amp; Tena-Junguito (2017) global trade volume index ($100 = 1913$ level) */
    public double getGlobalTradeVolumeIndex() { return macroeconomicCalculator.getGlobalTradeVolumeIndex(); }
    /** @return domestic inland fluvial and canal freight transport volume index */
    public double getInlandFluvialFreightVolume() { return macroeconomicCalculator.getInlandFluvialFreightVolume(); }

    /** @return Malandra (1990) / USGS per-capita metal smelting output in $\text{kg/capita/yr}$ */
    public double getMetalSmeltingPerCapitaKg() { return macroeconomicCalculator.getMetalSmeltingPerCapitaKg(); }

    /** @return Harley (1988) / Mohammed &amp; Williamson (2004) real maritime shipping freight cost index */
    public double getShippingFreightCostIndex() { return macroeconomicCalculator.getShippingFreightCostIndex(); }

    // =========================================================================
    // 6. CULTURAL COMPLEXITY & KNOWLEDGE METRICS
    // =========================================================================

    /** @return societal literacy and educational attainment index ($0\%$ to $100\%$) */
    public double getEducationLevel() { return culturalKnowledgeCalculator.getEducationLevel(); }

    /** @return overall socio-technical system complexity index ($0$ to $100$) */
    public double getSystemComplexityIndex() { return culturalKnowledgeCalculator.getSystemComplexityIndex(); }

    /** @return post-shock civilizational reconstruction capability index ($0$ to $100$) */
    public double getReconstructionCapabilityIndex() { return culturalKnowledgeCalculator.getReconstructionCapabilityIndex(); }

    /** @return supply-chain and systemic interdependence index ($0$ to $100$) */
    public double getSystemInterdependenceIndex() { return culturalKnowledgeCalculator.getSystemInterdependenceIndex(); }

    /** @return Shannon information transmission bandwidth index */
    public double getShannonBandwidth() { return culturalKnowledgeCalculator.getShannonBandwidth(); }

    /** @return collective cultural and technical memory stock index */
    public double getCollectiveMemoryStock() { return culturalKnowledgeCalculator.getCollectiveMemoryStock(); }

    /** @return innovation diffusion speed index ($0$ to $100$) */
    public double getInnovationDiffusionSpeed() { return culturalKnowledgeCalculator.getInnovationDiffusionSpeed(); }

    /** @return knowledge decay and institutional amnesia rate ($0$ to $100$) */
    public double getKnowledgeDecayRate() { return culturalKnowledgeCalculator.getKnowledgeDecayRate(); }

    /** @return endogenous global literacy rate in $[0.0, 99.5]\%$ */
    public double getLiteracyRate() { return culturalKnowledgeCalculator.getLiteracyRate(); }

    /** @return Scheidel (2014) ORBIS information travel speed in $\text{km/day}$ */
    public double getInformationTravelSpeedKmPerDay() { return culturalKnowledgeCalculator.getInformationTravelSpeedKmPerDay(); }

    /** @return synthetic emergent technology index from physical exergy and capital density */
    public double getEmergentTechnologyLevel() { return culturalKnowledgeCalculator.getEmergentTechnologyLevel(); }
}
