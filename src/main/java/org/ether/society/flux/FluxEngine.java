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
        float[] friction = world.getElevation();
        int[][] neighbors = world.getNeighborIndexes();
        
        float[] nextFood = new float[world.getCapacity()];
        System.arraycopy(food, 0, nextFood, 0, food.length);
        
        for (int i = 0; i < world.getCapacity(); i++) {
            float pA = prices[i];
            for (int j = 0; j < 6; j++) {
                int nIdx = neighbors[i][j];
                if (nIdx == -1) continue;
                
                float pB = prices[nIdx];
                float gradient = pB - pA;
                if (gradient > 0) {
                    float conductivity = 0.1f / (1.0f + Math.abs(friction[i] - friction[nIdx]) * 0.01f);
                    float flow = gradient * conductivity * dt;
                    flow = Math.min(flow, food[i] * 0.1f);
                    nextFood[i] -= flow;
                    nextFood[nIdx] += flow;
                }
            }
        }
        System.arraycopy(nextFood, 0, food, 0, food.length);
    }
}
