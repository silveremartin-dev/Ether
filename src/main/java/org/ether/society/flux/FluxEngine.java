package org.ether.society.flux;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.CivilizationAge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the physics-based diffusion of resources (Flux) across the map.
 * Operates on the "Pressure" principle: Resources flow from Supply (High
 * Pressure) to Demand (Low Pressure).
 */
public class FluxEngine {

    // Pressure cache to avoid recalculating during flux step
    private final Map<Long, Double> foodPressureMap = new ConcurrentHashMap<>();

    /**
     * Calculate Pressure for all cells.
     * Pressure > 0: Surplus (Supply)
     * Pressure < 0: Deficit (Demand)
     */
    public void calculatePressures(List<H3Cell> cells) {
        cells.parallelStream().forEach(cell -> {
            foodPressureMap.put(cell.getH3Index(), calculateFoodPressure(cell));
        });
    }

    /**
     * Process flux between cells and their neighbors.
     * 
     * @param neighbors Map of [Cell Index -> List of Neighbor Indices]
     * @param cellMap   Fast lookup map for H3Cell objects
     */
    public void processFlux(List<H3Cell> cells, Map<Long, List<Long>> neighbors, Map<Long, H3Cell> cellMap) {
        // Use a delta map to store changes, applied after calculation to avoid race
        // conditions
        Map<Long, Double> foodDelta = new ConcurrentHashMap<>();

        cells.parallelStream().forEach(source -> {
            long sourceId = source.getH3Index();
            double sourcePressure = foodPressureMap.getOrDefault(sourceId, 0.0);

            // Only sources with positive pressure (Surplus) push resources
            if (sourcePressure <= 0)
                return;

            List<Long> neighborIds = neighbors.get(sourceId);
            if (neighborIds == null)
                return;

            for (Long neighborId : neighborIds) {
                H3Cell target = cellMap.get(neighborId);
                if (target == null)
                    continue;

                double targetPressure = foodPressureMap.getOrDefault(neighborId, 0.0);

                // Flux flows from High to Low
                if (targetPressure >= sourcePressure)
                    continue;

                double conductivity = calculateConductivity(source, target);
                double pressureDiff = sourcePressure - targetPressure;

                // Flow Amount = Pressure Difference * Conductivity * Scaling Factor
                double flow = pressureDiff * conductivity * 50.0; // 50 units base flow per delta

                // Clamp flow to available resources
                // Use safe "available" check approximation
                if (flow > 0) {
                    // Record "Give" from Source
                    foodDelta.merge(sourceId, -flow, Double::sum);
                    // Record "Receive" at Target
                    foodDelta.merge(neighborId, flow, Double::sum);
                }
            }
        });

        // Apply Deltas
        cells.parallelStream().forEach(cell -> {
            double delta = foodDelta.getOrDefault(cell.getH3Index(), 0.0);
            if (delta != 0) {
                // Apply update synchronized or atomic?
                // Since this is final step of tick, simple update is safe if map is isolated.
                // But H3Cell methods are not synchronized.
                // However, we are inside a parallel stream of *cells*. Unique cells.
                // BUT 'foodDelta' aggregates updates from multiple neighbors.
                // The 'merge' above handles the concurrency of the map.
                // Now we just read and apply.
                double current = cell.getFoodResource();
                cell.setFoodResource(Math.max(0, current + delta));
            }
        });
    }

    private double calculateFoodPressure(H3Cell cell) {
        double production = cell.getFoodResource(); // Current stock is proxy for production capacity + stock
        // Better: Pressure = (Supply - Demand) / Capacity

        // Simple heuristic:
        // High Pop, Low Food = Low Pressure (-1.0)
        // Low Pop, High Food = High Pressure (+1.0)

        int pop = cell.getPopulation();
        double food = cell.getFoodResource();

        double consumption = pop * 1.0; // Base consumption

        if (pop == 0 && food > 0)
            return 1.0; // Maximum supply pressure (unclaimed resource)
        if (pop == 0)
            return 0.0; // Neutral

        double ratio = food / Math.max(1.0, consumption * 12.0); // 1 year of food safety?

        // Logarithmic scale centered at 1.0
        // > 1.0 -> Positive
        // < 1.0 -> Negative
        return Math.log10(Math.max(0.1, ratio));
    }

    private double calculateConductivity(H3Cell a, H3Cell b) {
        // Average conductivity of the two cells
        double condA = getTerrainConductivity(a);
        double condB = getTerrainConductivity(b);
        return (condA + condB) / 2.0;
    }

    private double getTerrainConductivity(H3Cell cell) {
        Biome biome = cell.getBiome();
        if (biome == null)
            return 1.0;

        switch (biome) {
            case MOUNTAINS:
                return 0.1; // Very hard to cross
            case JUNGLE:
                return 0.3;
            case DESERT:
                return 0.5;
            case DEEP_OCEAN:
                // If tech > Sailing, return high. For now, assume stone age default.
                // Need to inject Age/Tech.
                return 0.05;
            case OCEAN:
                return 0.1;
            case PLAINS:
                return 1.0; // Fast travel
            case HILLS:
                return 0.7;
            default:
                return 0.8;
        }
    }
}
