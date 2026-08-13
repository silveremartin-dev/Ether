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
        this.targetCohortSize = Math.max(1.0f, targetCohortSize);
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

        // dt is passed in seconds (e.g. 2,592,000s for 30 days) or days. Convert to fractional years.
        float dtInYears = Math.max(0.0001f, dt > 1000.0f ? (dt / (86400.0f * 365.25f)) : (dt / 365.25f));

        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            int hIdx = hexIds[i];
            float m = mass[i];
            if (m <= 0.01f) {
                hexIds[i] = -1;
                continue;
            }
            
            // Update Age (tracked in years)
            age[i] += dtInYears;
            float ageYears = age[i];
            
            // Structure cost (Sigma)
            sigma[i] = (float) Math.pow(m, 1.05) * 0.01f; 
            
            // Food consumption: 1 food unit in WorldBuffer feeds 1 human per year
            float foodRequired = m * dtInYears;
            float foodTaken = Math.min(food[hIdx], foodRequired);
            food[hIdx] -= foodTaken;
            
            // Calculate food satisfaction ratio (0.0 to 1.0)
            float foodSatisfaction = foodRequired > 0.0001f ? (foodTaken / foodRequired) : 1.0f;
            
            // Update internal energy store (0 to 100)
            if (foodSatisfaction >= 0.8f) {
                energy[i] = Math.min(100.0f, energy[i] + 5.0f * foodSatisfaction);
            } else {
                energy[i] = Math.max(0.0f, energy[i] - 15.0f * (1.0f - foodSatisfaction));
            }
            
            // --- Cycle Naissances / Décès ---
            // Taux de natalité annuel (2% à 4.5% par an selon le niveau d'énergie)
            float fertility = (energy[i] > 50.0f ? 0.035f : 0.010f);
            float newBirths = m * fertility * dtInYears;
            births[i] = newBirths;

            // Taux de mortalité annuel (Loi de Gompertz-Makeham + famine)
            float baseMortality = 0.015f; // 1.5% baseline
            float ageMortality = (float) (Math.pow(ageYears / 75.0f, 3.5) * 0.04f); // Sénescence
            float starvationMortality = (energy[i] < 20.0f ? 0.15f * (1.0f - energy[i] / 20.0f) : 0.0f);
            
            float annualMortality = Math.min(0.95f, baseMortality + ageMortality + starvationMortality);
            float newDeaths = m * annualMortality * dtInYears;
            deaths[i] = newDeaths;

            // Solde démographique de la cohorte
            float newMass = m + newBirths - newDeaths;
            mass[i] = Math.max(0.0f, newMass);

            // Accumulation dans la biomasse humaine de la cellule H3
            biomassHuman[hIdx] += mass[i];
            
            // Suppression de la cohorte si masse tombe sous 1 personne
            if (mass[i] < 1.0f) {
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
                    agents.getAge()[newSlot] = 0; // Nouvelle cohorte repart à 0
                    
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
