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
        if (allCells == null || allCells.isEmpty()) return;
        List<Nation> nations = diplomacyManager.getNations();
        if (nations.isEmpty()) return;

        // Build O(1) lookup map for current tick
        java.util.Map<Long, H3Cell> cellMap = new java.util.HashMap<>(allCells.size());
        for (H3Cell c : allCells) {
            cellMap.put(c.getH3Index(), c);
        }

        for (Nation nation : nations) {
            expandNation(nation, cellMap);
        }
    }

    private void expandNation(Nation nation, java.util.Map<Long, H3Cell> cellMap) {
        Set<H3Cell> territory = nation.getTerritory();
        if (territory.isEmpty()) return;

        for (H3Cell core : territory) {
            if (core.getPopulation() < 500) continue;

            List<Long> neighborIndices = h3Service.getNeighbors(core.getH3Index());
            for (Long nIdx : neighborIndices) {
                H3Cell neighbor = cellMap.get(nIdx);
                if (neighbor != null && neighbor.getOwner() == null) {
                    if (neighbor.getBiome() != org.ether.society.model.Biome.OCEAN &&
                        neighbor.getBiome() != org.ether.society.model.Biome.DEEP_OCEAN) {
                        if (Math.random() < 0.05) {
                            nation.addCell(neighbor);
                        }
                    }
                }
            }
        }
    }
}
