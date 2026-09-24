package org.ether.society.core.dod;

import org.ether.society.model.PhysicalConstants;

/**
 * Kernel gérant l'émergence urbaine, l'accumulation de capital et l'entropie institutionnelle.
 * Les agglomérations émergent aux points de concentration de flux sous contrainte
 * de maintenance thermodynamique de Tainter (rendements marginaux décroissants).
 */
public class UrbanKernel {

    /**
     * Met à jour le capital et l'urbanisation du monde sous conservation thermodynamique.
     */
    public void tick(WorldBuffer world, float dt) {
        float[] prices = world.getLocalPrice();
        float[] capital = world.getResourceCapital();
        float[] tech = world.getTechnologyLevel();
        float[] pop = world.getBiomassHuman();
        float[] complexity = world.getInstitutionalComplexity();
        
        float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                : (dt > 20.0f ? dt / 365.25 : dt));

        for (int i = 0; i < world.getCapacity(); i++) {
            if (pop[i] > 10.0f) {
                // 1. Mise à jour de la complexité institutionnelle C
                // Elle croît de façon sous-linéaire avec le stock de capital accumulé
                complexity[i] = (float) Math.log1p(capital[i] * 0.05f);
                
                // 2. Coût de maintenance institutionnelle (Entropie de Tainter / West-Bettencourt)
                // Sigma = k * C^1.15 (super-linéarité des coûts de coordination)
                float sigma = (float) Math.pow(complexity[i], PhysicalConstants.TAINTER_COMPLEXITY_EXPONENT) * 50.0f;
                
                // 3. Production brute de capital (surplus mobilisé par la population productive)
                float priceFactor = prices != null ? Math.max(0.1f, prices[i]) : 1.0f;
                float grossProduction = priceFactor * pop[i] * 0.05f;
                float netAccumulation = (grossProduction - sigma) * dtInYears;
                
                capital[i] = Math.max(0.0f, capital[i] + netAccumulation);
                
                // 4. Effet d'agglomération & innovation technologique (Arthur / Romer)
                if (capital[i] > 50.0f && tech != null) {
                    tech[i] += (float) Math.sqrt(capital[i]) * 0.0005f * dtInYears;
                }
                
                // 5. Effondrement / Simplification de Tainter : si le coût de maintenance surpasse la production,
                // l'entropie institutionnelle force une simplification organisationnelle.
                if (sigma > grossProduction) {
                    complexity[i] *= (float) Math.pow(0.95, dtInYears);
                }
            } else {
                // Dépréciation physique lente du capital et désagrégation institutionnelle en l'absence de population
                capital[i] = Math.max(0.0f, capital[i] * (float) Math.pow(0.95, dtInYears));
                complexity[i] = Math.max(0.0f, complexity[i] * (float) Math.pow(0.90, dtInYears));
            }
        }
    }
}
