package org.ether.society.core.dod;

/**
 * Kernel de simulation démographique gérant le métabolisme et la mitose des cohortes.
 */
public class DemographicKernel {

    private float targetCohortSize = 150.0f; // Target individual population threshold per cohort (Dunbar pivot)

    public float getTargetCohortSize() {
        return targetCohortSize;
    }

    public void setTargetCohortSize(float targetCohortSize) {
        this.targetCohortSize = Math.max(0.1f, targetCohortSize);
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DemographicKernel.class);

    /**
     * Exécute un cycle de simulation démographique.
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        processMetabolism(world, agents, dt);
        processMitosis(agents);
    }

    /**
     * Chaque cohorte consomme de l'énergie/nourriture et met à jour sa masse et son âge.
     */
    private void processMetabolism(WorldBuffer world, AgentBuffer agents, float dt) {
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

        // Precalculate total cell-level metabolic demand and human population to ensure equitable food sharing and correct Malthusian feedback
        float[] cellHumanPop = new float[world.getCapacity()];
        float[] cellDemand = new float[world.getCapacity()];
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            int hIdx = hexIds[i];
            float m = mass[i];
            if (m < minMassThreshold) continue;
            cellHumanPop[hIdx] += m;
            float tech = agents.getTechLevel()[i];
            double trophicMultiplier = (tech < 1.5f)
                    ? org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER
                    : org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN;
            float baseMetabolicNeed = (float) (m * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtInYears);
            cellDemand[hIdx] += (float) (baseMetabolicNeed * (trophicMultiplier / org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER));
        }

        float[] cellSatisfaction = new float[world.getCapacity()];
        for (int h = 0; h < world.getCapacity(); h++) {
            if (cellDemand[h] > 0.0001f) {
                cellSatisfaction[h] = Math.min(1.0f, food[h] / cellDemand[h]);
                float consumed = Math.min(food[h], cellDemand[h]);
                food[h] -= consumed;
                world.getEnergyFoodConsumed()[h] += consumed;
            } else {
                cellSatisfaction[h] = 1.0f;
            }
        }

        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            int hIdx = hexIds[i];
            float m = mass[i];
            if (m < minMassThreshold) {
                hexIds[i] = -1;
                continue;
            }
            
            // Update Age (tracked in years)
            age[i] += dtInYears;
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
            float foodSatisfaction = cellSatisfaction[hIdx];
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
            
            // --- Cycle Naissances / Décès & Modèle Démographique Physique ---
            // 1. Âge au premier enfant (Primiparité) & Structure d'âge continue (Lotka)
            // L'âge au premier enfant varie de 14 ans (sociétés traditionnelles/pionnières hyper-fertiles)
            // à plus de 30 ans (mariage tardif, haute technologie / éducation).
            float[][] culture = agents.getCulture();
            float kinshipNatalism = (culture != null && culture.length > 1 && culture[1] != null) ? Math.clamp(culture[1][i], 0.0f, 1.0f) : 0.5f;
            float primiparityAge = (float) (org.ether.society.model.PhysicalConstants.HUMAN_MIN_PRIMIPARITY_AGE_YEARS 
                    + 10.0f * (1.0f - kinshipNatalism) 
                    + 6.0f * Math.tanh(tech / 80.0f));
            float peakFertilityAge = primiparityAge + 8.0f;

            float fecundityAgeFactor;
            if (ageYears < primiparityAge - 1.5f) {
                fecundityAgeFactor = 0.0f; // Infécondité avant l'âge de primiparité / puberté
            } else {
                fecundityAgeFactor = (float) Math.exp(-Math.pow(ageYears - peakFertilityAge, 2) / (2.0f * Math.pow(13.0f, 2)));
            }

            // 2. Facteur bio-énergétique / nutritionnel (ovulation et état physiologique)
            float nutritionalFactor = (float) Math.clamp(0.15f + 0.85f * (energy[i] / 75.0f) * foodSatisfaction, 0.05f, 1.25f);

            // 3. Modulation culturelle de natalité (Tenseur Culturel dimension 1: Parenté & Pro-natalisme)
            float culturalFertilityMultiplier = 0.35f + 1.30f * kinshipNatalism; // Permet un TFR de ~1.5 à 8.5+ selon la culture

            // 4. Rétroaction densité-dépendante malthusienne (N_cell / K_cell)
            float totalCellFoodGJ = Math.max(food[hIdx], world.getBiomassNatural()[hIdx] + world.getBiomassAgriculture()[hIdx] + cellDemand[hIdx]);
            float carryingCapacity = Math.max(0.05f, totalCellFoodGJ / (float) org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
            float totalCellPop = Math.max(m, cellHumanPop[hIdx]);
            float malthusianPressure = totalCellPop / carryingCapacity;
            float densityFeedback = (float) (1.0 / (1.0 + Math.pow(malthusianPressure, 2.0)));

            // 5. Taux brut de natalité annuel effectif (maximum biologique unconstrained ~10.5% pour cohorte féconde)
            float maxBiologicalFertility = 0.115f;
            float birthRate = maxBiologicalFertility * fecundityAgeFactor * nutritionalFactor * culturalFertilityMultiplier * densityFeedback;
            float newBirths = m * birthRate * dtInYears;
            births[i] = newBirths;

            // 6. Prélèvement bioénergétique maternel de reproduction (Gestation + Allaitement : 0.80 GJ / naissance)
            float reproductionMetabolicLoad = (float) (newBirths * org.ether.society.model.PhysicalConstants.HUMAN_GESTATION_LACTATION_ENERGY_GJ);
            float reproductionFoodTaken = Math.min(food[hIdx], reproductionMetabolicLoad);
            food[hIdx] -= reproductionFoodTaken;
            world.getEnergyFoodConsumed()[hIdx] += reproductionFoodTaken;

            // 7. Mortalité infantile physique (fonction du capital K/m, de l'assainissement, de l'eau et de la nutrition)
            float capitalPerCapita = m > 0.001f ? (world.getResourceCapital()[hIdx] / m) : 0.0f;
            float waterSecurity = world.getWaterResource()[hIdx] > 10.0f ? 1.0f : 0.3f;
            float hygieneFactor = (float) Math.exp(-capitalPerCapita / 50.0f); // 1.0 (paléolithique) -> 0.0 (sanitaire moderne)
            float infantMortalityRate = Math.clamp(0.015f + 0.25f * hygieneFactor * (1.5f - 0.5f * foodSatisfaction) * (1.5f - 0.5f * waterSecurity), 0.015f, 0.50f);
            float survivingNewborns = newBirths * (1.0f - infantMortalityRate);
            float nonSurvivingInfants = newBirths - survivingNewborns; // Dépense calorique investie dissipée en pure perte (entropie)

            // 8. Mortalité adulte/sénescente (Loi actuarielle de Gompertz-Makeham + aléas physiques et famines)
            float gompertzSenescence = (float) (0.0001f * Math.exp(0.08f * ageYears));
            float acuteDeficit = Math.max(0.0f, 1.0f - foodSatisfaction);
            float starvationMortality = (acuteDeficit > 0.3f ? 0.35f * (acuteDeficit - 0.3f) : 0.0f)
                    + (energy[i] < 25.0f ? 0.45f * (1.0f - energy[i] / 25.0f) : 0.0f);
            float baselineHazard = 0.012f;
            float adultMortalityRate = Math.clamp(baselineHazard + gompertzSenescence + starvationMortality, 0.01f, 0.98f);
            float adultDeaths = m * adultMortalityRate * dtInYears;
            float totalDeaths = adultDeaths + nonSurvivingInfants;
            deaths[i] = totalDeaths;

            // 9. Solde démographique net de la cohorte
            float newMass = m + survivingNewborns - adultDeaths;
            mass[i] = Math.max(0.0f, newMass);

            // 10. Dynamique de renouvellement continu de l'âge moyen et succession générationnelle :
            if (newMass > minMassThreshold) {
                float survivingMass = Math.max(0.0f, m - adultDeaths);
                float updatedMeanAge = (ageYears * survivingMass + 0.0f * survivingNewborns) / newMass;
                if (updatedMeanAge > 58.0f) {
                    // Succession générationnelle : passation démographique de la cohorte aux descendants (20-30 ans)
                    updatedMeanAge = 22.0f + (float) (Math.random() * 8.0f);
                }
                age[i] = Math.max(0.0f, updatedMeanAge);
            }

            // Accumulation dans la biomasse humaine de la cellule H3
            biomassHuman[hIdx] += mass[i];
            
            // Suppression de la cohorte si masse tombe sous le seuil dynamique minimal
            if (mass[i] < minMassThreshold) {
                hexIds[i] = -1;
            }
        }

        int activeCohorts = 0;
        float totalMassVal = 0.0f;
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] != -1) {
                activeCohorts++;
                totalMassVal += mass[i];
            }
        }
        if (activeCohorts < 100 || activeCohorts % 1000 == 0) {
            logger.info("DemographicKernel: Active cohorts: {}, Total mass: {}", activeCohorts, (long) totalMassVal);
        }
    }

    /**
     * Gère la scission des cohortes trop importantes (Mitose).
     */
    private void processMitosis(AgentBuffer agents) {
        float[] mass = agents.getMass();
        float[] energy = agents.getEnergy();
        int[] hexIds = agents.getHexIds();
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            // Seuil de reproduction (mitose de cohorte basé sur la taille cible)
            if (mass[i] >= targetCohortSize * 1.25f && energy[i] >= 25.0f) {
                // Trouver un slot libre
                int newSlot = findFreeSlot(agents);
                if (newSlot != -1) {
                    // Scinder la masse démographique en conservant l'état énergétique par habitant
                    mass[newSlot] = mass[i] / 2.0f;
                    energy[newSlot] = energy[i];
                    mass[i] /= 2.0f;
                    
                    hexIds[newSlot] = hexIds[i];
                    agents.getH3Indexes()[newSlot] = agents.getH3Indexes()[i];
                    agents.getTechLevel()[newSlot] = agents.getTechLevel()[i];
                    agents.getGenerationCount()[newSlot] = agents.getGenerationCount()[i] + 1;
                    agents.getAge()[newSlot] = Math.max(16.0f, (float) (18.0 + Math.random() * 8.0)); // Nouvelle cohorte jeune adulte (18-26 ans)
                    
                    // Héritage avec mutation (bruit)
                    for (int d = 0; d < 4; d++) {
                        agents.getGenetics()[d][newSlot] = agents.getGenetics()[d][i] + (float)(Math.random() - 0.5) * 0.05f;
                        agents.getCulture()[d][newSlot] = agents.getCulture()[d][i] + (float)(Math.random() - 0.5) * 0.05f;
                    }
                } else {
                    // Le tampon d'agents est plein, arrêt immédiat de la mitose pour éviter O(N^2)
                    break;
                }
            }
        }
    }

    private int nextFreeSlotHint = 0;

    private int findFreeSlot(AgentBuffer agents) {
        int[] hexIds = agents.getHexIds();
        int cap = agents.getCapacity();
        for (int k = 0; k < cap; k++) {
            int idx = (nextFreeSlotHint + k) % cap;
            if (hexIds[idx] == -1) {
                nextFreeSlotHint = (idx + 1) % cap;
                return idx;
            }
        }
        return -1;
    }
}
