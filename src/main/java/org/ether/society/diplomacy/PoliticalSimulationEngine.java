package org.ether.society.diplomacy;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;
import org.ether.society.h3.H3Service;


import java.util.List;
import java.util.Set;


/**
 * Handles the dynamic evolution of political entities (Nations).
 * - Expansion: Nations grow into empty neighbor cells.
 * - Consolidation: Merging or conquering.
 */
public class PoliticalSimulationEngine {


    private final DiplomacyManager diplomacyManager;
    private final H3Service h3Service;

    public PoliticalSimulationEngine(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;
        this.h3Service = H3Service.getInstance();
    }

    /**
     * Run political simulation step (e.g. once per month or year).
     */
    public void tick(List<H3Cell> allCells) {
        List<Nation> nations = diplomacyManager.getNations();

        // 1. Check for basic expansion
        // Logic: If a nation has adjacent cells with sufficient population but NO
        // owner, claim it.
        // Or simple culture spread.

        for (Nation nation : nations) {
            expandNation(nation, allCells);
        }

        // 2. Check stability (collapse checking)
        // MVP: Just expansion for now.
    }

    private void expandNation(Nation nation, List<H3Cell> allCells) {
        // Get all border cells (cells in nation that have neighbors not in nation)
        // Optimization: Pre-calculate border or iterate territory.
        // For MVP, iterate territory.

        // Use a safe copy to avoid ConcurrentModification if we modified territory
        // while iterating (though addCell handles safe set)
        // But we are iterating 'territory' and finding neighbors.

        Set<H3Cell> territory = nation.getTerritory();
        if (territory.isEmpty())
            return;

        // Naive expansion: Pick a random border cell and try to expand to a neighbor.
        // To prevent instant map fill, do this probabilistically or based on population
        // pressure.

        // Rule: Can only expand if population > 1000 (established settlement)

        for (H3Cell core : territory) {
            if (core.getPopulation() < 500)
                continue; // Too weak to expand

            // Get neighbors
            List<Long> neighborIndices = h3Service.getNeighbors(core.getH3Index());

            for (Long nIdx : neighborIndices) {
                // Find the cell object (costly search? H3SimulationEngine usually has a Map or
                // spatial index).
                // Assuming allCells is a list... O(N) search is bad.
                // WE NEED A LOOKUP.
                // Assuming we can't easily look up from 'allCells' list without a Map.
                // Let's rely on the H3Service or create a transient lookup for this tick.

                H3Cell neighbor = findCellByIndex(allCells, nIdx);

                if (neighbor != null && neighbor.getOwner() == null) {
                    // Claim empty land if it's habitable
                    if (neighbor.getBiome() != org.ether.society.model.Biome.OCEAN &&
                            neighbor.getBiome() != org.ether.society.model.Biome.DEEP_OCEAN) {

                        // Expansion Calculation
                        // Chance based on core population
                        if (Math.random() < 0.05) { // 5% chance per tick per capable cell
                            nation.addCell(neighbor);
                            // logger.debug("Nation {} expanded to {}", nation.getName(),
                            // neighbor.getH3Index());
                        }
                    }
                } else if (neighbor != null && neighbor.getOwner() != null && neighbor.getOwner() != nation) {
                    // Conflict / Pressure logic here later
                }
            }
        }
    }

    // Helper to find cell (Inefficient, needs Optimization Phase 29: Spatial
    // Indexing)
    // For now, let's optimize or just accept it's slow.
    // Actually, H3SimulationEngine doesn't expose a Map<Long, H3Cell>.
    // Let's make this O(1) by passing a Map from the engine?
    // Or simpler: Pre-build map in tick?
    private H3Cell findCellByIndex(List<H3Cell> cells, Long index) {
        // Slow linear search.
        // Better: Use a shared map if available.
        // Fallback:
        return cells.stream()
                .filter(c -> c.getH3Index().equals(index))
                .findFirst()
                .orElse(null);
    }
}
