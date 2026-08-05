package org.ether.society.core.dod;

/**
 * Kernel de simulation démographique gérant le métabolisme et la mitose des cohortes.
 */
public class DemographicKernel {

    private float targetCohortSize = 500.0f; // Target individual population threshold per cohort

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
     * Chaque cohorte consomme de l'énergie et met à jour sa masse.
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

        // Constants in SI units
        final float SECONDS_PER_DAY = 86400f;
        final float ENERGY_REQ_PER_KG_DAY = 150000f; // 150 kJ/kg/day (approx 10 MJ for 70kg)

        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            int hIdx = hexIds[i];
            float m = mass[i];
            
            // Update Age (dt is in seconds)
            age[i] += dt;
            
            // Structure cost (Sigma) in Joules
            sigma[i] = (float) Math.pow(m, 1.1) * 1000f; 
            
            // Basal consumption (Joules per tick)
            float dailyReq = m * ENERGY_REQ_PER_KG_DAY;
            float consumption = (dailyReq / SECONDS_PER_DAY + sigma[i]) * dt;
            
            // Take from local pixel (food is in Joules)
            float foodTaken = Math.min(food[hIdx], consumption);
            food[hIdx] -= foodTaken;
            
            // Update internal energy
            energy[i] += foodTaken - consumption;
            
            // --- Cycle Naissances / Décès ---
            // Taux de natalité : f(énergie, nourriture disponible, densité)
            float fertility = (energy[i] > 100 ? 0.05f : 0.01f) * (1.0f - m/2000.0f); 
            float newBirths = m * fertility * dt;
            births[i] = newBirths;
            mass[i] += newBirths;

            // Taux de mortalité : f(âge, famine, température)
            float baseMortality = 0.02f;
            float ageMortality = (age[i] / 100.0f); // Augmente avec l'âge
            float starvationMortality = (energy[i] < 0 ? 0.2f : 0);
            
            float mortality = (baseMortality + ageMortality + starvationMortality) * dt;
            float newDeaths = m * mortality;
            deaths[i] = newDeaths;
            mass[i] = Math.max(0, mass[i] - newDeaths);

            // Mise à jour de la biomasse humaine
            world.getBiomassHuman()[hIdx] = mass[i];
            
            // Suppression de la cohorte si masse critique atteinte
            if (mass[i] <= 1.0f) {
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
            if (mass[i] > targetCohortSize * 2.0f && energy[i] > 100.0f) {
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
