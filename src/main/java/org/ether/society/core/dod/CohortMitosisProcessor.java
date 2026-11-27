/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.util.DeterministicPRNG;

/**
 * Stateless contiguous-memory processor for demographic cohort fission (Dunbar mitosis)
 * and O(1) amortized ring-buffer slot allocation in {@link AgentBuffer}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see DemographicKernel
 */
public final class CohortMitosisProcessor {

    private CohortMitosisProcessor() {
        // Stateless helper class
    }

    /**
     * Manages the mitosis and splitting of oversized population cohorts exceeding
     * {@code 1.25 * targetCohortSize} with positive physiological energy reserves.
     *
     * @param agents           contiguous agent/cohort memory buffer
     * @param targetCohortSize target Dunbar community size (individuals per cohort)
     * @param baseSeed         deterministic scenario PRNG seed
     * @param nextFreeSlotHint starting ring-buffer slot hint for amortized O(1) allocation
     * @return updated {@code nextFreeSlotHint} pointer after allocations
     */
    public static int processMitosis(AgentBuffer agents, float targetCohortSize, long baseSeed, int nextFreeSlotHint) {
        return processMitosis(agents, targetCohortSize, baseSeed, nextFreeSlotHint, 1.0);
    }

    /**
     * Manages Dunbar mitosis of oversized population cohorts with organism-scaled maturation age
     * and complete multi-dimensional cultural tensor inheritance ($0 \dots D_{\text{culture}}-1$).
     */
    public static int processMitosis(
            AgentBuffer agents,
            float targetCohortSize,
            long baseSeed,
            int nextFreeSlotHint,
            double lifespanScale) {
        float[] mass = agents.getMass();
        float[] energy = agents.getEnergy();
        int[] hexIds = agents.getHexIds();
        float[][] genetics = agents.getGenetics();
        float[][] culture = agents.getCulture();
        int genDims = genetics != null ? Math.min(4, genetics.length) : 0;
        int culDims = culture != null ? culture.length : 0;
        float scaleF = (float) Math.max(0.05, lifespanScale);
        int slotHint = nextFreeSlotHint;

        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;

            if (mass[i] >= targetCohortSize * 1.25f && energy[i] >= 25.0f) {
                int newSlot = findFreeSlot(agents, slotHint);
                if (newSlot != -1) {
                    slotHint = (newSlot + 1) % agents.getCapacity();
                    mass[newSlot] = mass[i] / 2.0f;
                    energy[newSlot] = energy[i];
                    mass[i] /= 2.0f;

                    hexIds[newSlot] = hexIds[i];
                    agents.getH3Indexes()[newSlot] = agents.getH3Indexes()[i];
                    agents.getGenerationCount()[newSlot] = agents.getGenerationCount()[i] + 1;
                    float ageRnd = (float) DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 403L);
                    agents.getAge()[newSlot] = Math.max(16.0f, 18.0f + ageRnd * 8.0f) * scaleF;

                    for (int d = 0; d < genDims; d++) {
                        float mutG = (float) (DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 405L + d) - 0.5) * 0.05f;
                        genetics[d][newSlot] = Math.clamp(genetics[d][i] + mutG, 0.0f, 1.0f);
                    }
                    for (int d = 0; d < culDims; d++) {
                        float mutC = (float) (DeterministicPRNG.nextDouble(baseSeed, 0L, newSlot, 415L + d) - 0.5) * 0.05f;
                        culture[d][newSlot] = Math.clamp(culture[d][i] + mutC, 0.0f, 1.0f);
                    }
                } else {
                    break;
                }
            }
        }
        return slotHint;
    }

    /**
     * Finds a free slot ({@code hexIds[idx] == -1}) in the {@link AgentBuffer} using a rotating pointer hint.
     *
     * @param agents           contiguous agent memory buffer
     * @param nextFreeSlotHint starting index hint
     * @return index of free slot, or {@code -1} if full
     */
    public static int findFreeSlot(AgentBuffer agents, int nextFreeSlotHint) {
        int[] hexIds = agents.getHexIds();
        int cap = agents.getCapacity();
        if (cap <= 0) return -1;
        int start = Math.floorMod(nextFreeSlotHint, cap);
        for (int i = 0; i < cap; i++) {
            int idx = (start + i) % cap;
            if (hexIds[idx] == -1) {
                return idx;
            }
        }
        return -1;
    }
}
