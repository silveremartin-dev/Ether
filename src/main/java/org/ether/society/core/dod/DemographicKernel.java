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

            // Food & Trophic energy consumption (in Gigajoules GJ):
            // Base human metabolic need: 3.362 GJ/hab/yr. Mobilized raw biomass = need * trophicMultiplier.
            float baseMetabolicNeed = (float) (m * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtInYears);
            float rawBiomassMobilized = (float) (baseMetabolicNeed * (trophicMultiplier / org.ether.society.model.PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER));
            
            float foodTaken = Math.min(food[hIdx], rawBiomassMobilized);
            food[hIdx] -= foodTaken;
            world.getEnergyFoodConsumed()[hIdx] += foodTaken;
            
            // Total Anatomical Exploitation (Binford 1978, Speth 1983):
            // In hunter-gatherer bands (Tech < 1.5), 20% of harvested faunal biomass is converted directly into physical tools & capital (K)
            if (tech < 1.5f && foodTaken > 0.0f) {
                float carcassCapital = (float) (foodTaken * org.ether.society.model.PhysicalConstants.CARCASS_MATERIAL_BYPRODUCT_FRACTION * 0.05f);
                world.getResourceCapital()[hIdx] += carcassCapital;
            }
            
            // Calculate food satisfaction ratio (0.0 to 1.0)
            float foodSatisfaction = rawBiomassMobilized > 0.0001f ? (foodTaken / rawBiomassMobilized) : 1.0f;
            
            // Update internal energy store (0 to 100)
            if (foodSatisfaction >= 0.8f) {
                energy[i] = Math.min(100.0f, energy[i] + 5.0f * foodSatisfaction);
            } else {
                energy[i] = Math.max(0.0f, energy[i] - 15.0f * (1.0f - foodSatisfaction));
            }
            
            // --- Cycle Naissances / Décès ---
            // Fécondité biologique dépendante de l'âge de la cohorte (Wood 1994, Coale-Trussell)
            // 0% avant 15 ans et après 48 ans (ménopause), pic reproductif entre 20 et 35 ans
            float ageFecundity;
            if (ageYears < 15.0f || ageYears >= 48.0f) {
                ageFecundity = 0.0f; // Infécondité infantile & post-ménopause stricte
            } else if (ageYears >= 20.0f && ageYears <= 35.0f) {
                ageFecundity = 1.0f; // Fenêtre optimale de fertilité
            } else if (ageYears < 20.0f) {
                ageFecundity = (ageYears - 15.0f) / 5.0f; // Transition adolescente (15-20 ans)
            } else {
                ageFecundity = Math.max(0.0f, (48.0f - ageYears) / 13.0f); // Déclin préménopause (35-48 ans)
            }

            // Taux de natalité annuel effectif (3.5% à 6% en âge fertile optimal selon l'énergie)
            float baseFertility = (energy[i] > 50.0f ? 0.055f : 0.020f);
            float fertility = baseFertility * ageFecundity;
            float newBirths = m * fertility * dtInYears;
            births[i] = newBirths;

            // Mortalité infantile physique (1.5% à 25% selon le stock de capital physique K/m, l'eau et la nutrition)
            // Le niveau technologique reste un simple observable dérivé du capital et de l'entropie
            float capitalPerCapita = m > 0.001f ? (world.getResourceCapital()[hIdx] / m) : 0.0f;
            float waterSecurity = world.getWaterResource()[hIdx] > 10.0f ? 1.0f : 0.3f;
            float hygieneFactor = (float) Math.exp(-capitalPerCapita / 50.0f); // 1.0 (paléolithique) -> 0.0 (sanitaire moderne)
            float infantMortalityRate = Math.clamp(0.015f + 0.20f * hygieneFactor * (2.0f - foodSatisfaction) * (2.0f - waterSecurity), 0.015f, 0.45f);
            float survivingNewborns = newBirths * (1.0f - infantMortalityRate);

            // Taux de mortalité adulte/sénescente annuel (Loi de Gompertz-Makeham + famine)
            float baseMortality = 0.015f; // 1.5% baseline
            float ageMortality = (float) (Math.pow(ageYears / 75.0f, 3.5) * 0.04f); // Sénescence
            float starvationMortality = (energy[i] < 20.0f ? 0.15f * (1.0f - energy[i] / 20.0f) : 0.0f);
            
            float annualMortality = Math.min(0.95f, baseMortality + ageMortality + starvationMortality);
            float adultDeaths = m * annualMortality * dtInYears;
            float totalDeaths = adultDeaths + (newBirths - survivingNewborns);
            deaths[i] = totalDeaths;

            // Solde démographique net de la cohorte
            float newMass = m + survivingNewborns - adultDeaths;
            mass[i] = Math.max(0.0f, newMass);

            // Dynamique de renouvellement générationnel de l'âge moyen :
            // Les nouveau-nés survivants entrent strictement à l'âge 0.0f (0 * survivingNewborns), rajeunissant la cohorte.
            if (newMass > minMassThreshold) {
                float survivingMass = Math.max(0.0f, m - adultDeaths);
                float updatedMeanAge = (ageYears * survivingMass + 0.0f * survivingNewborns) / newMass;
                age[i] = Math.max(0.0f, Math.min(95.0f, updatedMeanAge));
            }

            // Accumulation dans la biomasse humaine de la cellule H3
            biomassHuman[hIdx] += mass[i];
            
            // Suppression de la cohorte si masse tombe sous le seuil dynamique minimal
            if (mass[i] < minMassThreshold) {
                hexIds[i] = -1;
            }
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
            if (mass[i] >= targetCohortSize * 2.0f && energy[i] >= 50.0f) {
                // Trouver un slot libre
                int newSlot = findFreeSlot(agents);
                if (newSlot != -1) {
                    // Scinder la masse et l'énergie
                    mass[newSlot] = mass[i] / 2.0f;
                    energy[newSlot] = energy[i] / 2.0f;
                    mass[i] /= 2.0f;
                    energy[i] /= 2.0f;
                    
                    hexIds[newSlot] = hexIds[i];
                    agents.getH3Indexes()[newSlot] = agents.getH3Indexes()[i];
                    agents.getTechLevel()[newSlot] = agents.getTechLevel()[i];
                    agents.getGenerationCount()[newSlot] = agents.getGenerationCount()[i] + 1;
                    agents.getAge()[newSlot] = Math.max(5.0f, (float) (Math.random() * 22.0)); // Nouvelle cohorte jeune (5-22 ans)
                    
                    // Héritage avec mutation (bruit)
                    for (int d = 0; d < 4; d++) {
                        agents.getGenetics()[d][newSlot] = agents.getGenetics()[d][i] + (float)(Math.random() - 0.5) * 0.05f;
                        agents.getCulture()[d][newSlot] = agents.getCulture()[d][i] + (float)(Math.random() - 0.5) * 0.05f;
                    }
                }
            }
        }
    }

    private int findFreeSlot(AgentBuffer agents) {
        int[] hexIds = agents.getHexIds();
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) return i;
        }
        return -1;
    }
}
