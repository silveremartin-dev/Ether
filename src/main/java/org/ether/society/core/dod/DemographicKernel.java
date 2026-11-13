package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.OrganismBiophysicalProfile;
import org.ether.society.model.PhysicalConstants;

/**
 * <h1>Data-Oriented Demographic Simulation Kernel</h1>
 * <p>
 * Simulates cellular population metabolism, caloric energy consumption, mortality curves,
 * reproductive mitosis, and cohort splitting across the global H3 hexagonal grid.
 * </p>
 * <p>
 * <b>Governing Principles &amp; Universal Biophysical Laws ({@code AGENTS.md} Â§10.1â€“Â§10.3):</b>
 * <ul>
 *   <li><b>Kleiber Allometric Metabolism</b>: Metabolic demand, primiparity, gestation load, and
 *       reproductive cadence scale universally via {@link OrganismBiophysicalProfile} (supporting
 *       {@code Homo sapiens}, eusocial insects, and arbitrary allometric collective organisms).</li>
 *   <li><b>Physical Observable Regime Transitions ($C^\infty$)</b>: Trophic mobilization, Bocquet-Appel
 *       weaning fertility transition, Omran sanitary infant-mortality decline, and Lotka equilibrium age
 *       derive smoothly from conserved physical observables ($K/N$, $B_{\text{agr}}$, $B_{\text{nat}}$,
 *       $W_{\text{water}}$) with zero {@code technologyLevel} state variables or step discontinuities.</li>
 * </ul>
 * Mathematically synchronized with {@code native/ether-core-native/src/demographic.rs}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class DemographicKernel {

    /** Target individual population threshold per cohort (Dunbar / colony fission pivot). */
    private float targetCohortSize = 150.0f;

    /** @return target population per cohort */
    public float getTargetCohortSize() {
        return targetCohortSize;
    }

    /** @param targetCohortSize target cohort size threshold, bounded below by 0.1 */
    public void setTargetCohortSize(float targetCohortSize) {
        this.targetCohortSize = Math.max(0.1f, targetCohortSize);
    }

    private long baseSeed = 42L;

    /** @param seed scenario seed for deterministic cohort fission */
    public void setBaseSeed(long seed) {
        this.baseSeed = seed;
    }

    /** @return base deterministic scenario seed */
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

    static final float SILER_CRUDE_JUVENILE_RATE = 0.0028f;
    static final float SILER_CRUDE_BACKGROUND_RATE = 0.0127f;
    static final float SILER_CRUDE_SENESCENT_RATE = 0.0095f;

    private float[] cachedCellHumanPop;
    private float[] cachedCellDemand;
    private float[] cachedCellSatisfaction;
    private int[] activeCellIndices;
    private int activeCellCount = 0;

    /**
     * Executes one complete demographic simulation cycle across all active cohorts
     * using the canonical {@link org.ether.society.core.SimulationTimeStep}.
     *
     * @param world    global world buffer containing conserved cell states
     * @param agents   agent/cohort contiguous memory buffer
     * @param timeStep canonical time step representation
     */
    public void tick(WorldBuffer world, AgentBuffer agents, org.ether.society.core.SimulationTimeStep timeStep) {
        float dtYears = timeStep != null ? timeStep.dtYearsF() : (1.0f / 12.0f);
        tickWithDtYears(world, agents, dtYears);
    }

    /**
     * Executes one complete demographic simulation cycle across all active cohorts.
     *
     * @param world  global world buffer containing conserved cell states
     * @param agents agent/cohort contiguous memory buffer
     * @param dt     time step duration in days or seconds (if &gt; 1000)
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        float dtYears = dt > 1000.0f
                ? (float) (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                : (float) (dt > 20.0f ? dt / PhysicalConstants.DAYS_PER_JULIAN_YEAR : dt);
        tickWithDtYears(world, agents, dtYears);
    }

    private void tickWithDtYears(WorldBuffer world, AgentBuffer agents, float dtYears) {
        processMetabolism(world, agents, dtYears);
        processMitosis(agents);
    }

    /**
     * Computes the smooth ($C^\infty$) trophic biomass mobilization ratio relative to baseline foraging
     * from physical capital per capita ($K/N$) and aquatic/marine biomass availability.
     */
    private static double computeSmoothTrophicRatio(double capPerCap, double fishBiomass) {
        // Coastal marine protein subsidy transitions smoothly with fish stock (0 -> +0.22 ratio)
        double coastalShare = Math.tanh(Math.max(0.0, fishBiomass) / 250.0);
        double foragerRatio = 1.0 + 0.22222222 * coastalShare;
        // Exosomatic agricultural/livestock/cold-chain trophic expansion driven smoothly by K/N
        double agrarianDraftExpansion = 1.22 * SmoothMath.sigmoid(capPerCap, 45.0, 18.0);
        double industrialColdChainExpansion = 5.60 * SmoothMath.sigmoid(capPerCap, 1200.0, 450.0);
        return foragerRatio + agrarianDraftExpansion + industrialColdChainExpansion;
    }

    /**
     * Evaluates caloric consumption, mass updates, age progression, births, and deaths per cohort.
     */
    private void processMetabolism(WorldBuffer world, AgentBuffer agents, float dtYears) {
        OrganismBiophysicalProfile organism = world.getOrganismProfile();
        final double annualMetabolicGJ = organism.annualMetabolicEnergyGJ();
        final double minPrimiparityYears = organism.minPrimiparityAgeYears();
        final double gestationEnergyGJ = organism.gestationEnergyGJ();
        // Quarter-power allometric time scale relative to Homo sapiens (1.0 for Homo sapiens)
        final double lifespanScale = organism.referenceLifespanYears() / OrganismBiophysicalProfile.HOMO_SAPIENS.referenceLifespanYears();

        float[] mass = agents.getMass();
        float[] energy = agents.getEnergy();
        float[] sigma = agents.getSigmaCost();
        int[] hexIds = agents.getHexIds();
        float[] food = world.getFoodResource();
        float[] age = agents.getAge();
        float[] births = agents.getBirths();
        float[] deaths = agents.getDeaths();

        float[] biomassHuman = world.getBiomassHuman();
        java.util.Arrays.fill(biomassHuman, 0.0f);

        float dtInYears = Math.max(0.0001f, dtYears);
        float dtMonthlyEquivalent = dtInYears * 12.0f;
        float minMassThreshold = Math.max(0.0001f, Math.min(0.05f, targetCohortSize * 0.001f));

        int worldCap = world.getCapacity();
        if (cachedCellDemand == null || cachedCellDemand.length < worldCap) {
            cachedCellDemand = new float[worldCap];
            cachedCellSatisfaction = new float[worldCap];
            cachedCellHumanPop = new float[worldCap];
            java.util.Arrays.fill(cachedCellSatisfaction, 1.0f);
            activeCellIndices = new int[worldCap];
        }
        activeCellCount = 0;

        float[] capArr = world.getResourceCapital();
        float[] bioAgriArr = world.getBiomassAgriculture();
        float[] bioNatArr = world.getBiomassNatural();
        float[] bioLivestockArr = world.getBiomassLivestock();
        float[] bioFishArr = world.getBiomassFish();
        float[] waterArr = world.getWaterResource();
        float[] lifespanArr = world.getLifespan();
        float[] foodConsumedArr = world.getEnergyFoodConsumed();

        // Step 1: Accumulate active cohort population per cell
        for (int i = 0; i < agents.getCapacity(); i++) {
            int hIdx = hexIds[i];
            if (hIdx < 0 || hIdx >= worldCap) continue;
            float m = mass[i];
            if (m < minMassThreshold) continue;
            if (cachedCellHumanPop[hIdx] == 0.0f) {
                activeCellIndices[activeCellCount++] = hIdx;
            }
            cachedCellHumanPop[hIdx] += m;
        }

        // Step 2: Compute cell-level metabolic demand and food satisfaction from physical observables
        for (int k = 0; k < activeCellCount; k++) {
            int h = activeCellIndices[k];
            float cellPop = cachedCellHumanPop[h];
            if (cellPop > 0.0001f) {
                float cellCap = (capArr != null && h < capArr.length) ? capArr[h] : 0.0f;
                double capPerCap = cellCap / cellPop;
                double fishBio = (bioFishArr != null && h < bioFishArr.length) ? bioFishArr[h] : 0.0;
                // Primary caloric intake demand scales smoothly with physical processing regime
                double demandTrophicFactor = 1.0 + 0.15 * Math.tanh(capPerCap / 120.0);
                float cellDemand = (float) (cellPop * annualMetabolicGJ * demandTrophicFactor * dtInYears);
                cachedCellDemand[h] = cellDemand;

                float edibleAgriFluxAnnual = bioAgriArr[h] * 0.70f * (float) (PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG * 1e-3);
                float wildHarvestFraction = 0.005f + 0.003f / (1.0f + cellPop / 1500.0f);
                float edibleNaturalFluxAnnual = bioNatArr[h] * wildHarvestFraction * (float) PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
                float livestockBio = (bioLivestockArr != null && h < bioLivestockArr.length) ? bioLivestockArr[h] : 0.0f;
                float edibleFaunaFluxAnnual = livestockBio * 0.15f * (float) (PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG * 1e-3);
                float edibleFishFluxAnnual = (float) fishBio * 0.25f * (float) (PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG * 1e-3);
                float totalAnnualFoodFluxGJ = edibleAgriFluxAnnual + edibleNaturalFluxAnnual + edibleFaunaFluxAnnual + edibleFishFluxAnnual;

                float foodHarvestTickGJ = totalAnnualFoodFluxGJ * dtInYears;
                float totalAvailableFoodGJ = food[h] + foodHarvestTickGJ;

                cachedCellSatisfaction[h] = Math.clamp(totalAvailableFoodGJ / Math.max(1e-6f, cellDemand), 0.0f, 1.0f);
                float consumed = Math.min(totalAvailableFoodGJ, cellDemand);
                float surplus = Math.max(0.0f, totalAvailableFoodGJ - consumed);

                float storageLogistic = (float) Math.tanh(capPerCap / 35.0);
                float storageYears = 0.083f + 1.417f * storageLogistic;
                float maxGranaryCapacity = cellDemand * (storageYears / dtInYears);
                food[h] = Math.min(maxGranaryCapacity, surplus);
                foodConsumedArr[h] += consumed;
            } else {
                cachedCellSatisfaction[h] = 1.0f;
            }
        }

        // Step 3: Cohort Metabolic & Demographic Evolution Pass
        float[][] culture = agents.getCulture();
        for (int i = 0; i < agents.getCapacity(); i++) {
            int hIdx = hexIds[i];
            if (hIdx < 0 || hIdx >= worldCap) continue;
            float m = mass[i];
            if (m < minMassThreshold) {
                hexIds[i] = -1;
                continue;
            }

            float ageYears = age[i];
            sigma[i] = (float) Math.pow(m, 1.05) * 0.01f;

            float totalCellPop = Math.max(m, cachedCellHumanPop[hIdx]);
            double capPerCap = (capArr != null && hIdx < capArr.length) ? (capArr[hIdx] / totalCellPop) : 0.0;
            double fishBio = (bioFishArr != null && hIdx < bioFishArr.length) ? bioFishArr[hIdx] : 0.0;
            double trophicRatio = computeSmoothTrophicRatio(capPerCap, fishBio);

            float foodSatisfaction = cachedCellSatisfaction[hIdx];
            double baseMetabolicNeed = m * annualMetabolicGJ * dtInYears;
            float foodTaken = (float) (baseMetabolicNeed * trophicRatio * foodSatisfaction);

            // Smooth Total Anatomical Exploitation (Binford 1978, Speth 1983): bone/sinew/hide tools in low-capital foraging regimes
            double foragerWeight = 1.0 - Math.tanh(capPerCap / 20.0);
            if (foodTaken > 0.0f) {
                capArr[hIdx] += (float) (foodTaken * PhysicalConstants.CARCASS_MATERIAL_BYPRODUCT_FRACTION * 0.05 * foragerWeight);
            }

            // Smooth C^infty internal somatic energy store update in [0, 100] scaled by dt
            double satGate = SmoothMath.sigmoid(foodSatisfaction, 0.80, 0.05);
            double energyGain = 5.0 * foodSatisfaction;
            double energyLoss = 15.0 * (1.0 - foodSatisfaction);
            double deltaEnergy = (satGate * energyGain - (1.0 - satGate) * energyLoss) * dtMonthlyEquivalent;
            energy[i] = Math.clamp((float) (energy[i] + deltaEnergy), 0.0f, 100.0f);

            // 1. Smooth Primiparity & Fecundity Curve (Zero step discontinuity at primiparityAge - 1.5)
            float kinshipNatalism = (culture != null && culture.length > 1 && culture[1] != null)
                    ? Math.clamp(culture[1][i], 0.0f, 1.0f)
                    : 0.5f;
            double delayedMarriageShift = 6.0 * lifespanScale * Math.tanh(capPerCap / 450.0);
            double primiparityAge = minPrimiparityYears + (10.0 * lifespanScale) * (1.0 - kinshipNatalism) + delayedMarriageShift;
            double peakFertilityAge = primiparityAge + 8.0 * lifespanScale;
            double maturityGate = SmoothMath.sigmoid(ageYears, primiparityAge - 1.0 * lifespanScale, 0.75 * lifespanScale);
            double ageDiff = (ageYears - peakFertilityAge) / lifespanScale;
            float fecundityAgeFactor = (float) (maturityGate * Math.exp(-(ageDiff * ageDiff) / 338.0));

            // 2. Bio-energetic & cultural fertility factors
            float nutritionalFactor = Math.clamp(0.15f + 0.85f * (energy[i] / 75.0f) * foodSatisfaction, 0.05f, 1.25f);
            float culturalFertilityMultiplier = 0.40f + 1.20f * kinshipNatalism;

            // 3. Malthusian density-dependent negative feedback from physical edible biomass + capital nutrient cycling
            double wildHarvestFrac = 0.005 + 0.003 * foragerWeight;
            double edibleNaturalBiomassGJ = bioNatArr[hIdx] * wildHarvestFrac * PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
            double edibleAgriBiomassGJ = bioAgriArr[hIdx] * 0.70 * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG * 1e-3);
            double livestockBioCell = (bioLivestockArr != null && hIdx < bioLivestockArr.length) ? bioLivestockArr[hIdx] : 0.0;
            double edibleFaunaBiomassGJ = livestockBioCell * 0.15 * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG * 1e-3);
            double edibleFishBiomassGJ = fishBio * 0.25 * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG * 1e-3);
            double renewableAnnualFlowGJ = edibleNaturalBiomassGJ + edibleAgriBiomassGJ + edibleFaunaBiomassGJ + edibleFishBiomassGJ;
            double annualCellProductivityGJ = renewableAnnualFlowGJ + Math.min(renewableAnnualFlowGJ * 0.20, food[hIdx] * 0.15);

            // Smooth Boserupian & synthetic nitrogen intensification driven by physical capital per capita (Smil 2001)
            double boserupIntensification = 1.0 + 0.36 * Math.tanh(capPerCap / 80.0);
            double syntheticNitrogenBoost = 1.0 + 3.5 * SmoothMath.sigmoid(capPerCap, 800.0, 250.0);
            double carryingCapacity = Math.max(1.0, (annualCellProductivityGJ * boserupIntensification * syntheticNitrogenBoost) / annualMetabolicGJ);
            double malthusianPressure = totalCellPop / carryingCapacity;
            float densityFeedback = (float) (1.0 / (1.0 + malthusianPressure * malthusianPressure));

            // 4. Bocquet-Appel (2011) Neolithic Demographic Transition driven smoothly by cultivated agrarian caloric share
            // followed by van de Kaa (1987) modern fertility transition at post-industrial capital depths (K/N > 2400$)
            double agriCaloricShare = edibleAgriBiomassGJ / (renewableAnnualFlowGJ + 1e-4);
            double neolithicWeight = SmoothMath.sigmoid(agriCaloricShare + 0.35 * Math.tanh(capPerCap / 30.0), 0.25, 0.10);
            double baseCadence = 1.0 / lifespanScale;
            double foragerFertility = 0.026 * baseCadence;
            double agrarianFertility = (0.042 - 0.016 * SmoothMath.sigmoid(capPerCap, 2400.0, 600.0)) * baseCadence;
            float maxBiologicalFertility = (float) ((1.0 - neolithicWeight) * foragerFertility + neolithicWeight * agrarianFertility);
            float fertilityModulation = fecundityAgeFactor * Math.min(1.0f, nutritionalFactor) * culturalFertilityMultiplier * densityFeedback;

            // Maximum biological fecundity bound (Wood 1994, Dynamics of Human Reproduction, max biological CBR ~ 48a€°)
            float birthRate = (float) Math.min(0.048 / lifespanScale, maxBiologicalFertility * Math.clamp(fertilityModulation, 0.0f, 1.25f));
            float newBirths = m * birthRate * dtInYears;
            births[i] = m * birthRate;

            // 5. Maternal bioenergetic reproductive burden
            float reproductionMetabolicLoad = (float) (newBirths * gestationEnergyGJ);
            float reproductionFoodTaken = Math.min(food[hIdx], reproductionMetabolicLoad);
            food[hIdx] -= reproductionFoodTaken;
            foodConsumedArr[hIdx] += reproductionFoodTaken;

            // 6. Omran (1971) / Cutler & Miller (2005) Sanitary & Potable Water Infant Mortality (Canonical Eq. 99)
            // Evaluates clean water and sewer penetration fractions via Hill/Michaelis-Menten kinetics over physical capital
            float cellWater = (waterArr != null && hIdx < waterArr.length) ? waterArr[hIdx] : 10.0f;
            double waterSecurity = 0.30 + 0.70 * (cellWater / (10.0 + Math.max(0.0, cellWater)));
            double fClean = (capPerCap / (capPerCap + 1200.0)) * Math.min(1.0, waterSecurity);
            double kCapSq = capPerCap * capPerCap;
            double fSewer = kCapSq / (kCapSq + (2500.0 * 2500.0));
            double unsanitaryExposure = Math.pow(Math.max(0.0, 1.0 - fClean), 1.5) * (1.0 - 0.5 * fSewer);
            double nutritionalStress = 1.5 - 0.5 * foodSatisfaction;
            double waterStress = 1.5 - 0.5 * waterSecurity;
            float infantMortalityRate = (float) Math.clamp(
                    (0.015 + 0.305 * unsanitaryExposure) * nutritionalStress * waterStress,
                    0.015, 0.55
            );

            float lifespanRatio = 1.0f;
            if (lifespanArr != null && hIdx < lifespanArr.length && lifespanArr[hIdx] > (float) (5.0 * lifespanScale)) {
                lifespanRatio = (float) Math.clamp((40.0 * lifespanScale) / lifespanArr[hIdx], 0.40, 2.50);
                infantMortalityRate = Math.clamp(infantMortalityRate * (0.85f + 0.15f * lifespanRatio), 0.015f, 0.55f);
            }

            float survivingNewborns = newBirths * (1.0f - infantMortalityRate);
            float nonSurvivingInfants = newBirths - survivingNewborns;

            // 7. Gompertz-Makeham Adult & Senescent Hazard Law (Canonical Eq. 25)
            double scaledAge = ageYears / lifespanScale;
            float gompertzSenescence = (float) ((0.0001 / lifespanScale) * Math.exp(0.08 * scaledAge));
            double acuteDeficit = SmoothMath.softplus(1.0 - foodSatisfaction, 20.0);
            double deficitMortality = 0.35 * SmoothMath.sigmoid(acuteDeficit, 0.40, 0.08);
            double energyMortality = 0.45 * (1.0 - SmoothMath.sigmoid(energy[i], 15.0, 4.0));
            double starvationMortality = (deficitMortality + energyMortality) / lifespanScale;
            double baselineHazard = ((0.008 + 0.016 * unsanitaryExposure) / lifespanScale) * lifespanRatio;
            float adultHazardRate = (float) Math.max(0.008 / lifespanScale, baselineHazard + gompertzSenescence + starvationMortality);

            float survivalFraction = (float) Math.exp(-adultHazardRate * dtInYears);
            float adultDeaths = m * (1.0f - survivalFraction);
            deaths[i] = adultDeaths + nonSurvivingInfants;

            float newMass = (m * survivalFraction) + survivingNewborns;
            mass[i] = Math.max(0.0f, newMass);

            // 8. Continuous Lotka Age Renewal Dynamics
            if (newMass > minMassThreshold) {
                float senescentDeaths = Math.min(adultDeaths, m * gompertzSenescence * dtInYears);
                float meanSenescentDeathAge = (float) Math.max(ageYears + 10.0 * lifespanScale, 62.0 * lifespanScale);
                float ageSumAfterEvents = (ageYears + dtInYears) * m
                        - ageYears * Math.max(0.0f, adultDeaths - senescentDeaths)
                        - meanSenescentDeathAge * senescentDeaths;
                float updatedMeanAge = (float) Math.max(16.0 * lifespanScale, ageSumAfterEvents / newMass);

                float targetEquilibriumAge = (float) ((24.5 + 12.0 * Math.tanh(capPerCap / 450.0)) * lifespanScale);
                float generationalTurnoverRate = Math.min(1.0f, (float) (0.08 / lifespanScale) * dtInYears);
                updatedMeanAge += (targetEquilibriumAge - updatedMeanAge) * generationalTurnoverRate;
                age[i] = Math.clamp(updatedMeanAge, (float) (18.0 * lifespanScale), (float) (50.0 * lifespanScale));
            }

            biomassHuman[hIdx] += mass[i];
            if (mass[i] < minMassThreshold) {
                hexIds[i] = -1;
            }
        }

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

    private int nextFreeSlotHint = 0;

    private void processMitosis(AgentBuffer agents) {
        this.nextFreeSlotHint = CohortMitosisProcessor.processMitosis(agents, targetCohortSize, baseSeed, nextFreeSlotHint);
    }
}
