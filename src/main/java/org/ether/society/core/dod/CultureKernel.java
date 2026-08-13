package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kernel de simulation culturelle thermodynamique (SDE / Langevin Bridge).
 * Gère la dérive stochastique, la diffusion mémétique spatiale et le forçage
 * par les tenseurs culturels (Isoglosses, Kinship, Rituals, Sovereignty).
 *
 * Formule Langevin-SDE:
 * dC_i = [ \mu_d \sum_{j} (C_j - C_i) + \lambda_{forcing} (C_{tensor}(x_i) - C_i) ] dt + \sigma_{Langevin} dW_t
 */
public class CultureKernel {
    private static final Logger logger = LoggerFactory.getLogger(CultureKernel.class);

    private float diffusionRate = 0.08f;   // Taux d'acculturation inter-cohortes (mu_d)
    private float mutationRate = 0.015f;   // Intensité du bruit de Langevin (sigma_Langevin)
    private float forcingRate = 0.05f;    // Ancrage aux tenseurs géoculturels de la carte (lambda_forcing)

    public float getDiffusionRate() { return diffusionRate; }
    public void setDiffusionRate(float rate) { this.diffusionRate = Math.max(0.0f, rate); }

    public float getMutationRate() { return mutationRate; }
    public void setMutationRate(float rate) { this.mutationRate = Math.max(0.0f, rate); }

    public float getForcingRate() { return forcingRate; }
    public void setForcingRate(float rate) { this.forcingRate = Math.max(0.0f, rate); }

    /**
     * Simule l'évolution culturelle thermodynamique (SDE).
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        float dtNormalized = Math.max(0.001f, dt > 1000.0f ? (dt / (86400.0f * 365.25f)) : (dt / 365.25f));
        langevinDrift(agents, dtNormalized);
        diffuseAndForce(world, agents, dtNormalized);
    }

    /**
     * Dérive locale de Langevin : Bruit gaussien stochastique dW_t.
     */
    private void langevinDrift(AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            for (int d = 0; d < 4; d++) {
                // Bruit stochastique de Langevin (dw = Normal(0, sqrt(dt)))
                double gaussianNoise = Math.clamp(java.util.concurrent.ThreadLocalRandom.current().nextGaussian(), -3.0, 3.0);
                float dw = (float) (gaussianNoise * Math.sqrt(dt));
                
                culture[d][i] += mutationRate * dw;
                // Forme fermée sur le domaine tensoriel [0.0, 1.0]
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, culture[d][i]));
            }
        }
    }

    /**
     * Diffusion mémétique inter-hexagones et ancrage thermodynamique aux tenseurs de référence.
     */
    private void diffuseAndForce(WorldBuffer world, AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int[][] neighbors = world.getNeighborIndexes();
        
        float[][] delta = new float[4][agents.getCapacity()];
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            int myHex = hexIds[i];
            
            // 1. Diffusion spatiale avec les cohortes des hexagones voisins
            for (int j = 0; j < 6; j++) {
                int nHex = neighbors[myHex][j];
                if (nHex == -1) continue;
                
                for (int targetIdx = 0; targetIdx < agents.getCapacity(); targetIdx++) {
                    if (agents.getHexIds()[targetIdx] == nHex) {
                        for (int d = 0; d < 4; d++) {
                            float diff = culture[d][targetIdx] - culture[d][i];
                            delta[d][i] += diff * diffusionRate * dt;
                        }
                    }
                }
            }

            // 2. Terme de forçage thermodynamique lambda (rappel vers la matrice géographique régionale)
            // Dimension 0: Isoglosse, 1: Kinship, 2: Rituels, 3: Souveraineté
            for (int d = 0; d < 4; d++) {
                float targetSpatialVal = getSpatialTensorAnchor(world, myHex, d);
                float forcingDiff = targetSpatialVal - culture[d][i];
                delta[d][i] += forcingDiff * forcingRate * dt;
            }
        }
        
        // Appliquer les deltas
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            for (int d = 0; d < 4; d++) {
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, culture[d][i] + delta[d][i]));
            }
        }
    }

    /**
     * Ancrage géographique spatial pour le forçage des tenseurs.
     */
    private float getSpatialTensorAnchor(WorldBuffer world, int hexIdx, int dimension) {
        if (world == null || hexIdx < 0 || hexIdx >= world.getCapacity()) return 0.5f;
        
        float tech = world.getTechnologyLevel()[hexIdx];
        float elevation = world.getElevation()[hexIdx];
        
        switch (dimension) {
            case 0: // Isoglosse (Variabilité linguistique basée sur la rugosité de l'altitude)
                return Math.max(0.0f, Math.min(1.0f, 0.5f + (elevation / 4000.0f) * 0.3f));
            case 1: // Structure de parenté (Kinship & Clan structure)
                return Math.max(0.0f, Math.min(1.0f, 0.8f - (tech / 10.0f) * 0.4f));
            case 2: // Rituels & Asabiyyah (Cohésion sacrée)
                return Math.max(0.0f, Math.min(1.0f, 0.6f + (elevation > 1000 ? 0.2f : 0.0f)));
            case 3: // Souveraineté & Alignement politique
            default:
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.7f + 0.2f));
        }
    }

    /**
     * Calcule la distance d'intelligibilité linguistique / culturelle entre deux cohortes.
     */
    public static float calculateCulturalDistance(float[] c1, float[] c2) {
        if (c1 == null || c2 == null) return 0.0f;
        float sumSq = 0.0f;
        for (int d = 0; d < Math.min(c1.length, c2.length); d++) {
            float diff = c1[d] - c2[d];
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq);
    }
}
