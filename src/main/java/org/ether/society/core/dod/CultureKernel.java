package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thermodynamic Cultural Simulation Kernel (Langevin Stochastic Differential Equation Bridge).
 * <p>
 * Models stochastic cultural drift, spatial memetic diffusion across H3 hexagonal neighbors,
 * and restoration forcing toward biophysical geocultural tensors (Isogloss, Kinship, Rituals, Sovereignty)
 * derived strictly from conserved physical observables ({@code AGENTS.md} §10.1, §10.2).
 * </p>
 * <p>
 * Mathematically synchronized with {@code native/ether-core-native/src/culture.rs}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CultureKernel {
    private static final Logger logger = LoggerFactory.getLogger(CultureKernel.class);

    private float diffusionRate = 0.08f;
    private float mutationRate = 0.015f;
    private float forcingRate = 0.05f;
    private long deterministicTickCounter = 0L;
    private long baseSeed = 42L;

    private float[][] cachedHexCultureSum;
    private int[] cachedHexAgentCount;
    private float[][] cachedHexCultureMean;
    private int[] activeHexes;
    private int activeHexCount = 0;

    /** Resets internal deterministic tick counter and default kernel rates. */
    public void resetState() {
        this.diffusionRate = 0.08f;
        this.mutationRate = 0.015f;
        this.forcingRate = 0.05f;
        this.deterministicTickCounter = 0L;
        this.baseSeed = 42L;
    }

    /** @param seed deterministic scenario seed */
    public void setBaseSeed(long seed) { this.baseSeed = seed; }
    /** @return deterministic scenario seed */
    public long getBaseSeed() { return baseSeed; }

    /** @return memetic spatial diffusion rate */
    public float getDiffusionRate() { return diffusionRate; }
    /** @param rate memetic spatial diffusion rate */
    public void setDiffusionRate(float rate) { this.diffusionRate = Math.max(0.0f, rate); }

    /** @return Langevin stochastic mutation rate */
    public float getMutationRate() { return mutationRate; }
    /** @param rate Langevin stochastic mutation rate */
    public void setMutationRate(float rate) { this.mutationRate = Math.max(0.0f, rate); }

    /** @return regional tensor forcing rate */
    public float getForcingRate() { return forcingRate; }
    /** @param rate regional tensor forcing rate */
    public void setForcingRate(float rate) { this.forcingRate = Math.max(0.0f, rate); }

    /**
     * Executes one thermodynamic cultural evolution step (Langevin SDE integration).
     *
     * @param world  contiguous world state buffer
     * @param agents contiguous agent/cohort state buffer
     * @param dt     time step duration in days or seconds (if &gt; 1000)
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        float dtNormalized = Math.max(0.0001f, dt > 1000.0f ? (dt / 31557600.0f) : (dt / 365.25f));
        deterministicTickCounter++;
        langevinDrift(agents, dtNormalized, deterministicTickCounter);
        diffuseAndForce(world, agents, dtNormalized);
    }

    private void langevinDrift(AgentBuffer agents, float dt, long tickIndex) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int dims = culture != null ? culture.length : 0;
        if (mutationRate <= 0.0f || dims == 0) return;
        float noiseCoeff = (float) (mutationRate * Math.sqrt(dt));

        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            for (int d = 0; d < dims; d++) {
                double gaussianNoise = Math.clamp(
                        org.ether.society.util.DeterministicPRNG.nextGaussian(baseSeed, tickIndex, ((long) i << 4) | d),
                        -3.0, 3.0);
                culture[d][i] = Math.clamp(culture[d][i] + (float) (gaussianNoise * noiseCoeff), 0.0f, 1.0f);
            }
        }
    }

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

        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            int count = cachedHexAgentCount[h];
            if (count > 0) {
                for (int d = 0; d < dims; d++) {
                    cachedHexCultureMean[d][h] = cachedHexCultureSum[d][h] / count;
                }
            }
        }

        float diffCoeff = (diffusionRate / 6.0f) * dt;
        float forceCoeff = forcingRate * dt;

        for (int i = 0; i < agentCapacity; i++) {
            int myHex = hexIds[i];
            if (myHex < 0 || myHex >= worldCapacity) continue;

            for (int d = 0; d < dims; d++) {
                float myVal = culture[d][i];
                float delta = 0.0f;

                for (int j = 0; j < 6; j++) {
                    int nHex = neighbors[myHex][j];
                    if (nHex >= 0 && nHex < worldCapacity) {
                        float neighborMean = (cachedHexAgentCount[nHex] > 0)
                                ? cachedHexCultureMean[d][nHex]
                                : getSpatialTensorAnchor(world, nHex, d);
                        delta += (neighborMean - myVal) * diffCoeff;
                    }
                }

                float targetSpatialVal = getSpatialTensorAnchor(world, myHex, d);
                delta += (targetSpatialVal - myVal) * forceCoeff;

                culture[d][i] = Math.clamp(myVal + delta, 0.0f, 1.0f);
            }
        }

        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            cachedHexAgentCount[h] = 0;
            for (int d = 0; d < dims; d++) {
                cachedHexCultureSum[d][h] = 0.0f;
            }
        }
    }

    /**
     * Computes the smooth ($C^\infty$) geographic spatial tensor anchor for a given cell and cultural dimension
     * strictly from conserved physical observables (elevation, physical capital per capita $K/N$,
     * institutional complexity, population, biomass, and lifespan).
     */
    private float getSpatialTensorAnchor(WorldBuffer world, int hexIdx, int dimension) {
        if (world == null || hexIdx < 0 || hexIdx >= world.getCapacity()) return 0.5f;

        float elevation = world.getElevation() != null && hexIdx < world.getElevation().length ? world.getElevation()[hexIdx] : 0.0f;
        float instComp = world.getInstitutionalComplexity() != null && hexIdx < world.getInstitutionalComplexity().length ? world.getInstitutionalComplexity()[hexIdx] : 0.0f;
        float humanPop = world.getBiomassHuman() != null && hexIdx < world.getBiomassHuman().length ? world.getBiomassHuman()[hexIdx] : 0.0f;
        float capital = world.getResourceCapital() != null && hexIdx < world.getResourceCapital().length ? world.getResourceCapital()[hexIdx] : 0.0f;
        float capPerCap = humanPop > 0.001f ? (capital / humanPop) : 0.0f;
        float lifespan = world.getLifespan() != null && hexIdx < world.getLifespan().length ? world.getLifespan()[hexIdx] : 35.0f;
        float natBiomass = (world.getBiomassNatural() != null && hexIdx < world.getBiomassNatural().length ? world.getBiomassNatural()[hexIdx] : 0.0f)
                + (world.getBiomassAgriculture() != null && hexIdx < world.getBiomassAgriculture().length ? world.getBiomassAgriculture()[hexIdx] : 0.0f);

        switch (dimension) {
            case 0: // Isogloss (topographic elevation roughness, Nichols 1992)
                return Math.clamp(0.5f + (float) Math.tanh(elevation / 3500.0) * 0.35f, 0.0f, 1.0f);
            case 1: // Kinship & Clan structure vs. Market/Institutional nuclearization (Henrich 2020; Goody 1983)
                float clanPersistence = 1.0f - SmoothMath.sigmoidF(capPerCap, 120.0f, 45.0f);
                return Math.clamp(0.20f + 0.60f * clanPersistence, 0.0f, 1.0f);
            case 2: // Rituals & Asabiyyah (frontier solidarity, Turchin 2003)
                float asabiyyahRuggedness = SmoothMath.sigmoidF(elevation, 1000.0f, 333.333f);
                return Math.clamp(0.50f + 0.30f * asabiyyahRuggedness, 0.0f, 1.0f);
            case 3: // Sovereignty & State Integration (Tilly 1990; coercive/administrative centralization)
                float stateCentralization = instComp / (15.0f + Math.max(0.0f, instComp));
                return Math.clamp(0.15f + 0.70f * stateCentralization, 0.0f, 1.0f);
            case 4: // Material Capitalization (Solow 1956; intensive physical infrastructure K/N)
                return Math.clamp(0.10f + 0.80f * (float) Math.tanh(capPerCap / 250.0), 0.0f, 1.0f);
            case 5: // Trade networks & Market integration (Krugman 1991; lowland accessibility and settlement scale)
                float lowlandAccess = 1.0f - SmoothMath.sigmoidF(elevation, 600.0f, 250.0f);
                float popDensityScale = (float) Math.tanh(humanPop / 5000.0f);
                return Math.clamp(0.25f + 0.40f * lowlandAccess + 0.25f * popDensityScale, 0.0f, 1.0f);
            case 6: // Institutional complexity (Seshat scale, Turchin et al. 2018)
                float seshatHierarchy = instComp / (25.0f + Math.max(0.0f, instComp));
                return Math.clamp(0.10f + 0.80f * seshatHierarchy, 0.0f, 1.0f);
            case 7: // Ecological footprint / Malthusian stress (Boserup 1965)
                float malthusianLoad = humanPop / (100.0f + Math.max(0.0f, natBiomass));
                float footprintScale = malthusianLoad / (0.6f + malthusianLoad);
                return Math.clamp(0.15f + 0.75f * footprintScale, 0.0f, 1.0f);
            case 8: // Pathogen memory & Sanitary barrier (Omran 1971; Preston 1975)
                float sanitaryTransition = SmoothMath.sigmoidF(lifespan, 45.0f, 8.333f);
                return Math.clamp(0.20f + 0.65f * sanitaryTransition, 0.0f, 1.0f);
            default:
                return 0.5f;
        }
    }

    /**
     * Computes Euclidean linguistic/cultural intelligibility distance between two cohorts.
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
