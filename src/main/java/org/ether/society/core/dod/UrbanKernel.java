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
        float[] complexity = world.getInstitutionalComplexity();
        
        for (int i = 0; i < world.getCapacity(); i++) {
            // L'urbanisation est tirée par l'activité économique
            if (pop[i] > 100.0f) {
                // 1. Mise à jour de la complexité institutionnelle C
                // Elle croît avec le capital accumulé
                complexity[i] = (float) Math.log1p(capital[i] * 0.1f);
                
                // 2. Coût de maintenance (Entropie de Tainter) Sigma = k * C^1.15
                // Exprimé en Joules (ponction sur le capital/production)
                float sigma = (float) Math.pow(complexity[i], 1.15f) * 1000.0f;
                
                // 3. Accumulation nette (Activité - Maintenance)
                float grossProduction = prices[i] * pop[i] * 0.1f;
                float netAccumulation = (grossProduction - sigma) * dt;
                
                capital[i] = Math.max(0, capital[i] + netAccumulation);
                
                // 4. Effet d'agglomération technologique
                if (capital[i] > 100.0f) {
                    tech[i] += (float) Math.sqrt(capital[i]) * 0.001f * dt;
                }
                
                // 5. Effondrement de Tainter : si sigma > production, le capital s'érode
                // et la complexité doit être réduite par fragmentation.
                if (sigma > grossProduction) {
                    complexity[i] *= 0.99f; // Simplification forcée
                }
            } else {
                // Dépréciation lente du capital et de la complexité
                capital[i] *= (1.0f - 0.05f * dt);
                complexity[i] *= (1.0f - 0.02f * dt);
            }
        }
    }
}
