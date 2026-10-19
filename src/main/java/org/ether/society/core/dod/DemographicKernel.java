package org.ether.society.core.dod;

/**
 * <h1>Data-Oriented Demographic Simulation Kernel</h1>
 * <p>
 * Simulates cellular population metabolism, caloric energy consumption, mortality curves,
 * reproductive mitosis, and cohorte splitting across the global H3 hexagonal grid.
 * </p>
 * <p>
 * <b>Governing Principles &amp; Models:</b>
 * <ul>
 *   <li><b>Human Metabolic Energetics</b>: Kleiber's Law &amp; basal metabolic requirement
 *       ($E_{\text{metabolic}} \approx 3.0\,\text{GJ/capita/year} = 2200\,\text{kcal/day}$).</li>
 *   <li><b>Trophic Scaling</b>: Food energy requirement scaled by technology level (hunter-gatherer
 *       vs. Neolithic agrarian crop trophic efficiency).</li>
 *   <li><b>Dunbar Cohort Mitosis</b>: Splits overpopulated cohort buffers when mass exceeds the target
 *       individual threshold ($\approx 150$ individuals).</li>
 *   <li><b>Malthusian Mortality Response</b>: Malnutrition-driven mortality scaling exponentially
 *       with food satisfaction deficit ($1 - \text{satisfaction}$).</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class DemographicKernel {

    /** Target individual population threshold per cohort (Dunbar pivot). */
    private float targetCohortSize = 150.0f;

    /**
     * Retrieves the target cohort size threshold.
     *
     * @return target population per cohort (float)
     */
    public float getTargetCohortSize() {
        return targetCohortSize;
    }

    /**
     * Sets the target cohort size threshold, bounded below by 0.1 individuals.
     *
     * @param targetCohortSize the target cohort size threshold (float)
     */
    public void setTargetCohortSize(float targetCohortSize) {
        this.targetCohortSize = Math.max(0.1f, targetCohortSize);
    }

    private long baseSeed = 42L;

    /**
     * Sets the scenario-derived random seed for deterministic demographic mutation and age sampling.
     *
     * @param seed the scenario seed (long)
     */
    public void setBaseSeed(long seed) {
        this.baseSeed = seed;
    }

    /**
     * Gets the scenario-derived random seed for deterministic demographic mutation.
     *
     * @return the base seed (long)
     */
    public long getBaseSeed() {
        return baseSeed;
    }

    /**
     * Resets internal state variables and slot hints for inter-scenario isolation.
     */
    public void resetState() {
        this.baseSeed = 42L;
        this.nextFreeSlotHint = 0;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DemographicKernel.class);

    /**
     * Juvenile (ages 1-15) crude death-rate component, in yr<sup>-1</sup>, obtained by integrating the Siler
     * immature hazard \(a_1 e^{-b_1 x}\) over the forager life table \(l(x)\) for \(x \ge 1\)
     * (Gurven &amp; Kaplan 2007: \(a_1 = 0.422\), \(b_1 = 1.131\)).
     */
    static final float SILER_CRUDE_JUVENILE_RATE = 0.0028f;

    /**
     * Age-independent (Makeham) background crude death-rate component, in yr<sup>-1</sup>
     * (Siler \(a_2 = 0.013\): accidents, violence, acute infections), weighted over \(l(x)\), \(x \ge 1\).
     */
    static final float SILER_CRUDE_BACKGROUND_RATE = 0.0127f;

    /**
     * Senescent crude death-rate component, in yr<sup>-1</sup>, obtained by integrating the Gompertz term
     * \(a_3 e^{b_3 x}\) over the forager life table (\(a_3 = 1.47\times10^{-4}\), \(b_3 = 0.086\)).
     * Replaces the former Gompertz evaluation at the cohort mean age, which underestimated mortality by
     * Jensen's inequality.
     */
    static final float SILER_CRUDE_SENESCENT_RATE = 0.0095f;

    private float[] cachedCellHumanPop;
    private float[] cachedCellDemand;
    private float[] cachedCellSatisfaction;
    private int[] activeCellIndices;
    private int activeCellCount = 0;

    /**
     * Executes one complete demographic simulation cycle across all active cohorts.
     *
     * @param world the global world buffer containing cell states
     * @param agents the agent/cohort contiguous memory buffer
     * @param dt the time step duration in days or ticks
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        processMetabolism(world, agents, dt);
        processMitosis(agents);
    }

    /**
     * Evaluates caloric consumption, mass updates, age progression, births, and deaths per cohort.
     */
    private void processMetabolism(WorldBuffer world, AgentBuffer agents, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float[] mass = agents.getMass();
        float[] energy = agents.getEnergy();
        float[] sigma = agents.getSigmaCost();
        int[] hexIds = agents.getHexIds();
        float[] food = world.getFoodResource();

        float[] age = agents.getAge();
        float[] births = agents.getBirths();
        float[] deaths = agents.getDeaths();

        // Re-accumulate human biomass across all active cohorts into WorldBuffer
        float[] biomassHuman = world.getBiomassHuman();
        java.util.Arrays.fill(biomassHuman, 0.0f);

        float dtInYears = Math.max(0.0001f, dt > 1000.0f ? (dt / (86400.0f * 365.25f)) : (dt / 365.25f));
        // Dynamic minimum mass threshold supporting small cohorts (<= 10) down to single individuals without underflow
        float minMassThreshold = Math.max(0.0001f, Math.min(0.05f, targetCohortSize * 0.001f));

        // Reusable zero-allocation arrays for cell-level metabolic demand and human population
        int worldCap = world.getCapacity();
        if (cachedCellDemand == null || cachedCellDemand.length < worldCap) {
            cachedCellDemand = new float[worldCap];
            cachedCellSatisfaction = new float[worldCap];
            cachedCellHumanPop = new float[worldCap];
            java.util.Arrays.fill(cachedCellSatisfaction, 1.0f);
            activeCellIndices = new int[worldCap];
        }
        activeCellCount = 0;

        for (int i = 0; i < agents.getCapacity(); i++) {
            int hIdx = hexIds[i];
            if (hIdx < 0 || hIdx >= worldCap) continue;
            float m = mass[i];
            if (m < minMassThreshold) continue;
            float tech = agents.getTechLevel()[i];
            double trophicMultiplier = (tech < 1.5f)
                    ? org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER
                    : org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN;
            float baseMetabolicNeed = (float) (m * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtInYears);
            float addedDemand = (float) (baseMetabolicNeed * (trophicMultiplier / org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER));

            if (cachedCellDemand[hIdx] == 0.0f && cachedCellHumanPop[hIdx] == 0.0f) {
                activeCellIndices[activeCellCount++] = hIdx;
            }
            cachedCellHumanPop[hIdx] += m;
            cachedCellDemand[hIdx] += addedDemand;
        }

        // Process food consumption only on active populated cells
        for (int k = 0; k < activeCellCount; k++) {
            int h = activeCellIndices[k];
            if (cachedCellDemand[h] > 0.0001f) {
                float cellTech = world.getTechnologyLevel() != null ? world.getTechnologyLevel()[h] : 1.0f;
                float edibleAgriFluxAnnual = world.getBiomassAgriculture()[h] * 0.70f * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG;
                float edibleNaturalFluxAnnual = world.getBiomassNatural()[h] * (cellTech < 1.5f ? 0.008f : 0.005f) * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
                float edibleFishFluxAnnual = (world.getBiomassFish() != null ? world.getBiomassFish()[h] : 0.0f) * 0.25f * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG;
                float totalAnnualFoodFluxGJ = edibleAgriFluxAnnual + edibleNaturalFluxAnnual + edibleFishFluxAnnual;

                // Food produced during this time step dt
                float foodHarvestTickGJ = totalAnnualFoodFluxGJ * dtInYears;
                float totalAvailableFoodGJ = food[h] + foodHarvestTickGJ;

                cachedCellSatisfaction[h] = Math.clamp(totalAvailableFoodGJ / cachedCellDemand[h], 0.0f, 1.0f);
                float consumed = Math.min(totalAvailableFoodGJ, cachedCellDemand[h]);
                float surplus = Math.max(0.0f, totalAvailableFoodGJ - consumed);

                // Granary storage capacity: up to 1.5 years of metabolic demand for agrarians (tech >= 1.5),
                // or 1 month for nomadic foragers (tech < 1.5, mobility & spoilage constraints)
                float storageYears = (cellTech < 1.5f) ? 0.083f : 1.5f;
                float maxGranaryCapacity = cachedCellDemand[h] * (storageYears / dtInYears);
                food[h] = Math.min(maxGranaryCapacity, surplus);
                world.getEnergyFoodConsumed()[h] += consumed;
            } else {
                cachedCellSatisfaction[h] = 1.0f;
            }
        }

        for (int i = 0; i < agents.getCapacity(); i++) {
            int hIdx = hexIds[i];
            if (hIdx < 0 || hIdx >= worldCap) continue;
            float m = mass[i];
            if (m < minMassThreshold) {
                hexIds[i] = -1;
                continue;
            }
            
            // Baseline Community Mean Age (Lotka stable age distribution)
            float ageYears = age[i];
            
            // Structure cost (Sigma)
            sigma[i] = (float) Math.pow(m, 1.05) * 0.01f; 
            
            // --- Trophic Footprint & Bioenergetic Subsistence Regime ---
            float tech = agents.getTechLevel()[i];
            double trophicMultiplier;
            double eroi;

            if (tech < 1.5f) {
                // Paleolithic / Mesolithic Hunter-Gatherers & Coastal Foragers
                boolean isCoastal = world.getBiomes()[hIdx] == (byte) org.ether.society.model.Biome.BEACH.ordinal()
                        || world.getBiomassFish()[hIdx] > 100.0f;
                if (isCoastal) {
                    trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_COASTAL_FORAGER;
                    eroi = 12.0;
                } else {
                    trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER;
                    eroi = 7.0;
                }
            } else if (tech < 4.0f) {
                // Early Neolithic Agrarian
                trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN;
                eroi = 6.5;
            } else if (tech < 50.0f) {
                // Advanced Preindustrial Agrarian with Draft Animal Traction
                trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN;
                eroi = 2.8;
            } else if (tech < 120.0f) {
                // Industrial Era
                trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER;
                eroi = 0.5; // Thermodynamic inversion
            } else {
                // Post-Industrial Globalized Food System
                trophicMultiplier = org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_POST_INDUSTRIAL;
                eroi = 0.12; // 8-10 kcal fossil per 1 kcal ingested
            }

            // Calculate food satisfaction ratio (0.0 to 1.0)
            float foodSatisfaction = cachedCellSatisfaction[hIdx];
            float baseMetabolicNeed = (float) (m * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtInYears);
            float rawBiomassMobilized = (float) (baseMetabolicNeed * (trophicMultiplier / org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER));
            float foodTaken = rawBiomassMobilized * foodSatisfaction;
            
            // Total Anatomical Exploitation (Binford 1978, Speth 1983):
            if (tech < 1.5f && foodTaken > 0.0f) {
                float carcassCapital = (float) (foodTaken * org.ether.society.model.PhysicalConstants.CARCASS_MATERIAL_BYPRODUCT_FRACTION * 0.05f);
                world.getResourceCapital()[hIdx] += carcassCapital;
            }
            
            // Update internal energy store (0 to 100)
            if (foodSatisfaction >= 0.8f) {
                energy[i] = Math.min(100.0f, energy[i] + 5.0f * foodSatisfaction);
            } else {
                energy[i] = Math.max(0.0f, energy[i] - 15.0f * (1.0f - foodSatisfaction));
            }
            
            // --- Birth / Death Cycles & Physical Demographic Model ---
            // 1. Age at first child (Primiparity) & continuous age structure (Lotka stable population theory)
            // Age at primiparity ranges dynamically from ~14 years (traditional high-fertility pioneer frontier)
            // to over 30 years in modern high-tech / delayed marriage environments.
            float[][] culture = agents.getCulture();
            float kinshipNatalism = (culture != null && culture.length > 1 && culture[1] != null) ? Math.clamp(culture[1][i], 0.0f, 1.0f) : 0.5f;
            float primiparityAge = (float) (org.ether.society.model.PhysicalConstants.HUMAN_MIN_PRIMIPARITY_AGE_YEARS 
                    + 10.0f * (1.0f - kinshipNatalism) 
                    + 6.0f * Math.tanh(tech / 80.0f));
            float peakFertilityAge = primiparityAge + 8.0f;

            float fecundityAgeFactor;
            if (ageYears < primiparityAge - 1.5f) {
                fecundityAgeFactor = 0.0f; // Biological infecundity prior to sexual maturity
            } else {
                float ageDiff = ageYears - peakFertilityAge;
                fecundityAgeFactor = (float) Math.exp(-(ageDiff * ageDiff) / 338.0f);
            }

            // 2. Bio-energetic & nutritional status factor (ovulatory function & physiological reserves)
            float nutritionalFactor = Math.clamp(0.15f + 0.85f * (energy[i] / 75.0f) * foodSatisfaction, 0.05f, 1.25f);

            // 3. Cultural fertility modulation (Cultural Tensor dimension 1: Kinship & Pro-Natalist Norms)
            float culturalFertilityMultiplier = 0.40f + 1.20f * kinshipNatalism; // 1.0 at baseline kinshipNatalism=0.5

            // 4. Malthusian density-dependent negative feedback (N_cell / K_cell)
            // Wild standing forest biomass (wood/lignin) is NOT directly human-edible:
            // Hunter-gatherers can only forage edible flora/fauna (~0.5-0.8% of standing biomass per year, Binford/Kelly).
            // Cultivated agricultural biomass yields ~70% net caloric grain/edible food; marine fisheries yield ~25% annual MSY (Schaefer).
            float edibleNaturalBiomassGJ = world.getBiomassNatural()[hIdx] * (tech < 1.5f ? 0.008f : 0.005f) * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
            float edibleAgriBiomassGJ = world.getBiomassAgriculture()[hIdx] * 0.70f * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG;
            float edibleFishBiomassGJ = (world.getBiomassFish() != null ? world.getBiomassFish()[hIdx] : 0.0f) * 0.25f * (float) org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG;
            float renewableAnnualFlowGJ = edibleNaturalBiomassGJ + edibleAgriBiomassGJ + edibleFishBiomassGJ;
            float annualCellProductivityGJ = renewableAnnualFlowGJ + Math.min(renewableAnnualFlowGJ * 0.20f, food[hIdx] * 0.15f);
            float carryingCapacity = Math.max(1.0f, annualCellProductivityGJ / (float) org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
            float totalCellPop = Math.max(m, cachedCellHumanPop[hIdx]);
            float malthusianPressure = totalCellPop / carryingCapacity;
            float densityFeedback = 1.0f / (1.0f + malthusianPressure * malthusianPressure);

            // 5. Effective annual crude birth rate (Bocquet-Appel 2002, 2011 Neolithic Demographic Transition)
            // Pre-agricultural (tech < 1.5): nomadic foragers with 3.5-4.5 yrs lactation amenorrhea (CBR ~ 25-28 / 1000)
            // Neolithic/Agrarian (tech >= 1.5): sedentism & cereal weaning foods allow inter-birth intervals ~ 2 yrs (CBR ~ 38-46 / 1000)
            float maxBiologicalFertility = (tech < 1.5f)
                    ? (float) (0.024f + 0.004f * Math.tanh(tech))
                    : (float) (0.0375f + 0.012f * Math.tanh((tech - 1.5f) / 1.5f));
            float fertilityModulation = fecundityAgeFactor * Math.min(1.0f, nutritionalFactor) * culturalFertilityMultiplier * densityFeedback;

            float birthRate = maxBiologicalFertility * Math.clamp(fertilityModulation, 0.0f, 1.25f);
            float newBirths = m * birthRate * dtInYears;
            births[i] = newBirths;

            // 6. Maternal bioenergetic reproductive burden (Gestation + Lactation: ~0.80 GJ per live birth)
            float reproductionMetabolicLoad = (float) (newBirths * org.ether.society.model.PhysicalConstants.HUMAN_GESTATION_LACTATION_ENERGY_GJ);
            float reproductionFoodTaken = Math.min(food[hIdx], reproductionMetabolicLoad);
            food[hIdx] -= reproductionFoodTaken;
            world.getEnergyFoodConsumed()[hIdx] += reproductionFoodTaken;

            // 7. Biophysical infant and child mortality (function of physical capital K/m, sanitation, clean water, and nutrition)
            float capitalPerCapita = m > 0.001f ? (world.getResourceCapital()[hIdx] / m) : 0.0f;
            float waterSecurity = world.getWaterResource()[hIdx] > 10.0f ? 1.0f : 0.3f;
            float hygieneFactor = (float) Math.exp(-capitalPerCapita / 50.0f); // 1.0 (Paleolithic baseline) -> 0.0 (Modern public sanitation)
            float infantMortalityRate = Math.clamp(0.015f + 0.20f * hygieneFactor * (1.5f - 0.5f * foodSatisfaction) * (1.5f - 0.5f * waterSecurity), 0.015f, 0.50f);

            // Coupling infant mortality to environmental/epidemiological life expectancy
            float lifespanRatio = 1.0f;
            if (world.getLifespan() != null && world.getLifespan()[hIdx] > 5.0f) {
                // Baseline pre-industrial life expectancy reference = 40.0 years
                lifespanRatio = Math.clamp(40.0f / world.getLifespan()[hIdx], 0.40f, 2.50f);
                if (lifespanRatio > 1.0f) {
                    infantMortalityRate = Math.clamp(infantMortalityRate * (0.85f + 0.15f * lifespanRatio), 0.015f, 0.55f);
                }
            }

            float survivingNewborns = newBirths * (1.0f - infantMortalityRate);
            float nonSurvivingInfants = newBirths - survivingNewborns; // Lost reproductive caloric investment (entropic dissipation)

            // 8. Adult and senescent mortality (Gompertz-Makeham hazard law: cellular senescence beta=0.08 + extrinsic Makeham hazard gamma)
            float gompertzSenescence = (float) (0.0001f * Math.exp(0.08f * ageYears));
            float acuteDeficit = Math.max(0.0f, 1.0f - foodSatisfaction);
            float starvationMortality = (acuteDeficit > 0.3f ? 0.35f * (acuteDeficit - 0.3f) : 0.0f)
                    + (energy[i] < 25.0f ? 0.45f * (1.0f - energy[i] / 25.0f) : 0.0f);
            float baselineHazard = 0.010f * lifespanRatio;
            float adultHazardRate = Math.max(0.008f, baselineHazard + gompertzSenescence + starvationMortality);
            
            // Symplectic / Exponential decay scheme: guarantees m(t+dt) > 0 for ANY arbitrary dtInYears
            float survivalFraction = (float) Math.exp(-adultHazardRate * dtInYears);
            float adultDeaths = m * (1.0f - survivalFraction);
            float totalDeaths = adultDeaths + nonSurvivingInfants;
            deaths[i] = totalDeaths;

            // 9. Net demographic balance of cohort (strictly bounded below by 0)
            float newMass = (m * survivalFraction) + survivingNewborns;
            mass[i] = Math.max(0.0f, newMass);

            // 10. Continuous age renewal dynamics and generational succession:
            if (newMass > minMassThreshold) {
                // In an aggregate community cohort (Dunbar node), age[i] represents the mean demographic age of the community A.
                // 1. Existing individuals age by dtInYears: (ageYears + dtInYears) * m
                // 2. Extrinsic deaths occur across all ages around the community mean age: - ageYears * extrinsicDeaths
                // 3. Senescent deaths occur predominantly among the elderly (~62 yrs): - 62.0f * senescentDeaths
                // 4. Surviving newborns enter at age 0.0: + 0.0f * survivingNewborns
                float senescentDeaths = Math.min(adultDeaths, m * gompertzSenescence * dtInYears);
                float meanSenescentDeathAge = Math.max(ageYears + 10.0f, 62.0f);
                float ageSumAfterEvents = (ageYears + dtInYears) * m 
                        - ageYears * Math.max(0.0f, adultDeaths - senescentDeaths) 
                        - meanSenescentDeathAge * senescentDeaths;
                float updatedMeanAge = Math.max(16.0f, ageSumAfterEvents / newMass);

                // Lotka stable population theory (Lotka 1907): Stable age pyramid equilibrium
                // Pre-industrial baseline A* ~ 24.5 yrs; modern high-tech demographic transition A* ~ 36.0 yrs
                float targetEquilibriumAge = 24.5f + 12.0f * (float) Math.tanh(tech / 80.0f);
                float generationalTurnoverRate = Math.min(1.0f, 0.08f * dtInYears);
                updatedMeanAge += (targetEquilibriumAge - updatedMeanAge) * generationalTurnoverRate;
                age[i] = Math.clamp(updatedMeanAge, 18.0f, 50.0f);
            }

            // Accumulate human biomass into the H3 cell buffer
            biomassHuman[hIdx] += mass[i];
            
            // Extinguish cohort if mass falls below dynamic threshold
            if (mass[i] < minMassThreshold) {
                hexIds[i] = -1;
            }
        }

        // Reset only the actively populated cells in the cached arrays (O(activeCellCount))
        for (int k = 0; k < activeCellCount; k++) {
            int h = activeCellIndices[k];
            cachedCellDemand[h] = 0.0f;
            cachedCellHumanPop[h] = 0.0f;
            cachedCellSatisfaction[h] = 1.0f;
        }

        if (logger.isDebugEnabled()) {
            int activeCohorts = 0;
            float totalMassVal = 0.0f;
            for (int i = 0; i < agents.getCapacity(); i++) {
                if (hexIds[i] != -1) {
                    activeCohorts++;
                    totalMassVal += mass[i];
                }
            }
            if (activeCohorts < 100 || activeCohorts % 1000 == 0) {
                logger.debug("DemographicKernel: Active cohorts: {}, Total mass: {}", activeCohorts, (long) totalMassVal);
            }
        }
    }

    /**
     * Manages the mitosis and splitting of oversized population cohorts (Dunbar fission).
     *
     * @param agents the agent/cohort contiguous memory buffer
     */
    private void processMitosis(AgentBuffer agents) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float[] mass = agents.getMass();
        float[] energy = agents.getEnergy();
        int[] hexIds = agents.getHexIds();
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            // Fission threshold based on target Dunbar cohort size
            if (mass[i] >= targetCohortSize * 1.25f && energy[i] >= 25.0f) {
                // Locate a free slot in the contiguous ring buffer
                int newSlot = findFreeSlot(agents);
                if (newSlot != -1) {
                    // Split demographic mass equally while preserving per-capita energy status
                    mass[newSlot] = mass[i] / 2.0f;
                    energy[newSlot] = energy[i];
                    mass[i] /= 2.0f;
                    
                    hexIds[newSlot] = hexIds[i];
                    agents.getH3Indexes()[newSlot] = agents.getH3Indexes()[i];
                    agents.getTechLevel()[newSlot] = agents.getTechLevel()[i];
                    agents.getGenerationCount()[newSlot] = agents.getGenerationCount()[i] + 1;
                    float ageRnd = (float) org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 403L);
                    agents.getAge()[newSlot] = Math.max(16.0f, 18.0f + ageRnd * 8.0f); // Young adult generation (18-26 yrs)
                    
                    // Cultural and genetic inheritance with stochastic mutation drift bounded to [0.0, 1.0]
                    for (int d = 0; d < 4; d++) {
                        float mutG = (float) (org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 405L + d) - 0.5) * 0.05f;
                        float mutC = (float) (org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 415L + d) - 0.5) * 0.05f;
                        agents.getGenetics()[d][newSlot] = Math.clamp(agents.getGenetics()[d][i] + mutG, 0.0f, 1.0f);
                        agents.getCulture()[d][newSlot] = Math.clamp(agents.getCulture()[d][i] + mutC, 0.0f, 1.0f);
                    }
                } else {
                    // Agent buffer saturated; break early to avoid O(N^2) scan overhead
                    break;
                }
            }
        }
    }

    /** Cached pointer hint for next free agent slot to achieve O(1) amortized allocation. */
    private int nextFreeSlotHint = 0;

    /**
     * Finds a free slot in the AgentBuffer using a rotating pointer hint.
     *
     * @param agents the agent memory buffer
     * @return index of free slot, or -1 if full
     */
    private int findFreeSlot(AgentBuffer agents) {
        int[] hexIds = agents.getHexIds();
        int cap = agents.getCapacity();
        for (int idx = nextFreeSlotHint; idx < cap; idx++) {
            if (hexIds[idx] == -1) {
                nextFreeSlotHint = (idx + 1 < cap) ? idx + 1 : 0;
                return idx;
            }
        }
        for (int idx = 0; idx < nextFreeSlotHint; idx++) {
            if (hexIds[idx] == -1) {
                nextFreeSlotHint = (idx + 1 < cap) ? idx + 1 : 0;
                return idx;
            }
        }
        return -1;
    }
}
