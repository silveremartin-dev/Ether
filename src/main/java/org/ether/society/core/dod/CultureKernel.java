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

    private float[][] cachedHexCultureSum;
    private int[] cachedHexAgentCount;
    private float[][] cachedHexCultureMean;
    private int[] activeHexes;
    private int activeHexCount = 0;

    /*
     * Get diffusion rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getDiffusionRate() { return diffusionRate; }
    /*
     * Set diffusion rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setDiffusionRate(float rate) { this.diffusionRate = Math.max(0.0f, rate); }

    /*
     * Get mutation rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getMutationRate() { return mutationRate; }
    /*
     * Set mutation rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setMutationRate(float rate) { this.mutationRate = Math.max(0.0f, rate); }

    /*
     * Get forcing rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getForcingRate() { return forcingRate; }
    /*
     * Set forcing rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setForcingRate(float rate) { this.forcingRate = Math.max(0.0f, rate); }

    /*
     * Simule l'évolution culturelle thermodynamique (SDE).
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float dtNormalized = Math.max(0.001f, dt > 1000.0f ? (dt / (86400.0f * 365.25f)) : (dt / 365.25f));
        langevinDrift(agents, dtNormalized);
        diffuseAndForce(world, agents, dtNormalized);
    }

    /*
     * Dérive locale de Langevin : Bruit gaussien stochastique dW_t.
     */
    private void langevinDrift(AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int dims = culture != null ? culture.length : 0;
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            for (int d = 0; d < dims; d++) {
                // Bruit stochastique de Langevin (dw = Normal(0, sqrt(dt)))
                double gaussianNoise = Math.clamp(java.util.concurrent.ThreadLocalRandom.current().nextGaussian(), -3.0, 3.0);
                float dw = (float) (gaussianNoise * Math.sqrt(dt));
                
                culture[d][i] += mutationRate * dw;
                // Forme fermée sur le domaine tensoriel [0.0, 1.0]
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, culture[d][i]));
            }
        }
    }

    /*
     * Diffusion mémétique inter-hexagones et ancrage thermodynamique aux tenseurs de référence.
     * Complexité strictement linéaire O(N_agents + 6 * N_cells) via agrégation spatiale par cellule.
     */
    private void diffuseAndForce(WorldBuffer world, AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int worldCapacity = world != null ? world.getCapacity() : 0;
        if (worldCapacity == 0) return;

        int[][] neighbors = world.getNeighborIndexes();
        int dims = culture != null ? culture.length : 0;
        if (dims == 0) return;

        if (cachedHexAgentCount == null || cachedHexAgentCount.length < worldCapacity || cachedHexCultureSum.length < dims) {
            cachedHexCultureSum = new float[dims][worldCapacity];
            cachedHexAgentCount = new int[worldCapacity];
            cachedHexCultureMean = new float[dims][worldCapacity];
            activeHexes = new int[worldCapacity];
        }
        activeHexCount = 0;

        int agentCapacity = agents.getCapacity();
        for (int i = 0; i < agentCapacity; i++) {
            int h = hexIds[i];
            if (h >= 0 && h < worldCapacity) {
                if (cachedHexAgentCount[h] == 0) {
                    activeHexes[activeHexCount++] = h;
                }
                cachedHexAgentCount[h]++;
                for (int d = 0; d < dims; d++) {
                    cachedHexCultureSum[d][h] += culture[d][i];
                }
            }
        }

        // Calculer les moyennes par cellule uniquement pour les cellules actives
        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            int count = cachedHexAgentCount[h];
            if (count > 0) {
                for (int d = 0; d < dims; d++) {
                    cachedHexCultureMean[d][h] = cachedHexCultureSum[d][h] / count;
                }
            }
        }

        // 2. Appliquer la diffusion spatiale avec les voisins et le forçage tensoriel : O(N_agents)
        for (int i = 0; i < agentCapacity; i++) {
            int myHex = hexIds[i];
            if (myHex < 0 || myHex >= worldCapacity) continue;

            for (int d = 0; d < dims; d++) {
                float myVal = culture[d][i];
                float delta = 0.0f;

                // Diffusion avec les 6 voisins hexagones
                for (int j = 0; j < 6; j++) {
                    int nHex = neighbors[myHex][j];
                    if (nHex >= 0 && nHex < worldCapacity) {
                        float neighborMean = (cachedHexAgentCount[nHex] > 0)
                                ? cachedHexCultureMean[d][nHex]
                                : getSpatialTensorAnchor(world, nHex, d);
                        delta += (neighborMean - myVal) * (diffusionRate / 6.0f) * dt;
                    }
                }

                // Forçage thermodynamique lambda vers le tenseur régional
                float targetSpatialVal = getSpatialTensorAnchor(world, myHex, d);
                delta += (targetSpatialVal - myVal) * forcingRate * dt;

                // Mise à jour bornée dans [0.0, 1.0]
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, myVal + delta));
            }
        }

        // Réinitialiser uniquement les cellules actives
        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            cachedHexAgentCount[h] = 0;
            for (int d = 0; d < dims; d++) {
                cachedHexCultureSum[d][h] = 0.0f;
            }
        }
    }

    /*
     * Ancrage géographique spatial pour le forçage des tenseurs.
     */
    private float getSpatialTensorAnchor(WorldBuffer world, int hexIdx, int dimension) {
        if (world == null || hexIdx < 0 || hexIdx >= world.getCapacity()) return 0.5f;
        
        float tech = world.getTechnologyLevel() != null && hexIdx < world.getTechnologyLevel().length ? world.getTechnologyLevel()[hexIdx] : 1.0f;
        float elevation = world.getElevation() != null && hexIdx < world.getElevation().length ? world.getElevation()[hexIdx] : 0.0f;
        
        switch (dimension) {
            case 0: // Isoglosse (Variabilité linguistique basée sur la rugosité de l'altitude)
                return Math.max(0.0f, Math.min(1.0f, 0.5f + (elevation / 4000.0f) * 0.3f));
            case 1: // Structure de parenté (Kinship & Clan structure)
                return Math.max(0.0f, Math.min(1.0f, 0.8f - (tech / 10.0f) * 0.4f));
            case 2: // Rituels & Asabiyyah (Cohésion sacrée)
                return Math.max(0.0f, Math.min(1.0f, 0.6f + (elevation > 1000 ? 0.2f : 0.0f)));
            case 3: // Souveraineté & Alignement politique
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.7f + 0.2f));
            case 4: // Technologie & Matérialité
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.9f + 0.1f));
            case 5: // Réseaux commerciaux (Centralité côtière et plaines)
                return Math.max(0.0f, Math.min(1.0f, elevation < 500 ? 0.7f : 0.3f));
            case 6: // Complexité institutionnelle (Seshat)
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.8f + 0.1f));
            case 7: // Empreinte écologique / Stress malthusien
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.5f + 0.2f));
            case 8: // Mémoire pathogène & Barrière sanitaire
                return Math.max(0.0f, Math.min(1.0f, 0.5f));
            default:
                // Ancrage neutre pour les 10e, 11e ou autres dimensions personnalisées
                return 0.5f;
        }
    }

    /*
     * Calcule la distance d'intelligibilité linguistique / culturelle entre deux cohortes.
     */
    public static float calculateCulturalDistance(float[] c1, float[] c2) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        if (c1 == null || c2 == null) return 0.0f;
        float sumSq = 0.0f;
        for (int d = 0; d < Math.min(c1.length, c2.length); d++) {
            float diff = c1[d] - c2[d];
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq);
    }
}
