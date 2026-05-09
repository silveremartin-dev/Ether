package org.ether.society.core.dod;

/**
 * Kernel gérant l'émergence urbaine et l'accumulation de capital.
 * Les villes émergent naturellement aux points de haute concentration de flux.
 */
public class UrbanKernel {

    /**
     * Met à jour le capital et l'urbanisation du monde.
     */
    public void tick(WorldBuffer world, float dt) {
        float[] prices = world.getLocalPrice();
        float[] capital = world.getResourceCapital();
        float[] tech = world.getTechnologyLevel();
        float[] pop = world.getBiomassHuman();
        
        for (int i = 0; i < world.getCapacity(); i++) {
            // L'urbanisation est tirée par la demande (prix élevé) et la présence de population
            if (pop[i] > 1000.0f) {
                // Accumulation de capital basée sur l'activité économique (flux/prix)
                float economicActivity = prices[i] * pop[i] * 0.0001f;
                capital[i] += economicActivity * dt;
                
                // Le capital génère du progrès technologique (effet d'agglomération)
                if (capital[i] > 100.0f) {
                    tech[i] += capital[i] * 0.00001f * dt;
                }
                
                // Limitation du tech level à 10 pour cette phase
                tech[i] = Math.min(10.0f, tech[i]);
            } else {
                // Dépréciation lente du capital en l'absence de population
                capital[i] *= (1.0f - 0.01f * dt);
            }
        }
    }
}
