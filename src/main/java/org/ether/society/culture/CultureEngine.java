package org.ether.society.culture;

import org.ether.society.database.H3Cell;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the evolution and diffusion of cultural values across the map.
 * Simulates the "Memetic" field where influence spreads via trade and
 * proximity.
 */
public class CultureEngine {

    private final Map<Long, CultureVector> cultureMap = new ConcurrentHashMap<>();

    // Config
    private static final double DRIFT_RATE = 0.005; // 0.5% drift per year
    private static final double DIFFUSION_RATE = 0.05; // 5% blend with neighbors per year

    /**
     * Update cultural vectors for all cells.
     */
    public void updateCultures(List<H3Cell> cells, Map<Long, List<Long>> neighbors) {

        // 1. Initialization (if needed)
        // Only done lazily inside the update logic to avoid separate pass, but we need
        // read access.

        // 2. Parallel Calculation of Next State
        Map<Long, CultureVector> nextState = new ConcurrentHashMap<>();

        cells.parallelStream().forEach(cell -> {
            long id = cell.getH3Index();
            CultureVector current = cultureMap.computeIfAbsent(id, k -> CultureVector.random());

            // Step A: Drift (Innovation/Mutation)
            CultureVector next = current.drift(DRIFT_RATE);

            // Step B: Diffusion (Influence from Neighbors)
            List<Long> neighborIds = neighbors.get(id);
            if (neighborIds != null && !neighborIds.isEmpty()) {



                // For now, uniform weight. Later, weight by Flux (Trade Volume).



                for (Long nId : neighborIds) {
                    CultureVector nVec = cultureMap.get(nId);
                    if (nVec != null) {
                        // Weighted average logic simplified: sequential blend
                        // Limitation: Order dependent? No, blend is commutative-ish for small ratios.
                        // Better: Accumulate weighted sum R/G/B.
                        // But CultureVector is immutable.
                        // Let's us simple blend loop for now.
                        next = next.blend(nVec, DIFFUSION_RATE / neighborIds.size());
                    }
                }
            }

            nextState.put(id, next);
        });

        // 3. Apply updates
        cultureMap.putAll(nextState);
    }

    public CultureVector getCulture(long h3Index) {
        return cultureMap.getOrDefault(h3Index, CultureVector.NEUTRAL);
    }
}
