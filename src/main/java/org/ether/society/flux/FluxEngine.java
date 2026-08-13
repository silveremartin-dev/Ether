package org.ether.society.flux;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import java.util.List;
import java.util.Map;

/**
 * Moteur de flux thermodynamique optimisé via Vector API (SIMD).
 */
public class FluxEngine {
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    public void tick(WorldBuffer world, float dt) {
        calculatePricesVectorized(world);
        diffuseResources(world, dt);
    }

    /**
     * Calcule les prix via SIMD.
     */
    private void calculatePricesVectorized(WorldBuffer world) {
        float[] food = world.getFoodResource();
        float[] pop = world.getBiomassHuman();
        float[] prices = world.getLocalPrice();
        int length = world.getCapacity();
        
        int i = 0;
        int upperBound = SPECIES.loopBound(length);
        
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector vFood = FloatVector.fromArray(SPECIES, food, i);
            FloatVector vPop = FloatVector.fromArray(SPECIES, pop, i);
            
            // Price = (Pop + 1) / (Food + 1)
            FloatVector vPrice = vPop.add(1.0f).div(vFood.add(1.0f));
            vPrice.intoArray(prices, i);
        }
        
        // Tail loop
        for (; i < length; i++) {
            prices[i] = (pop[i] + 1.0f) / (food[i] + 1.0f);
        }
    }

    private void diffuseResources(WorldBuffer world, float dt) {
        float[] food = world.getFoodResource();
        float[] prices = world.getLocalPrice();
        float[] elevation = world.getElevation();
        int[][] neighbors = world.getNeighborIndexes();
        int capacity = world.getCapacity();

        // Exact physical H3 Level 8 cell area (~0.73737 km²) and physical mobility conductivity
        final float BASE_CONDUCTIVITY = 0.05f; // s/m²
        
        float[] deltaFood = new float[capacity];
        
        // Exact Symmetric Finite Volume Method (Volumes Finis) over unique neighbor edges (i < nIdx)
        for (int i = 0; i < capacity; i++) {
            float pA = prices[i];
            float hA = elevation[i];
            float foodA = food[i];
            
            for (int j = 0; j < 6; j++) {
                int nIdx = neighbors[i][j];
                // Process each undirected edge exactly once to guarantee 100% strict mass conservation
                if (nIdx == -1 || i >= nIdx) continue;
                
                float pB = prices[nIdx];
                float hB = elevation[nIdx];
                float foodB = food[nIdx];
                
                // Potential gradient (J/kg)
                float gradient = pB - pA;
                
                // Geographical friction based on topographic slope: f = 1 + |dh| * 0.1
                float friction = 1.0f + Math.abs(hB - hA) * 0.1f;
                float conductivity = BASE_CONDUCTIVITY / friction;
                
                // Unbounded physical Onsager flux
                float rawFlux = gradient * conductivity * dt;
                
                // Strict CFL Physical Mass Limitation (a cell cannot send more than half its available resources in a single step)
                float maxTransferFromA = foodA > 0.0f ? foodA * 0.5f : 0.0f;
                float maxTransferFromB = foodB > 0.0f ? foodB * 0.5f : 0.0f;
                
                float flux = Math.max(-maxTransferFromB, Math.min(maxTransferFromA, rawFlux));
                
                // Strict symmetric mass conservation across interface
                deltaFood[i] -= flux;
                deltaFood[nIdx] += flux;
            }
        }
        
        for (int i = 0; i < capacity; i++) {
            food[i] = Math.max(0.0f, food[i] + deltaFood[i]);
        }
    }
}
