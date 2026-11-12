/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.generation;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;

import java.util.List;
import java.util.Map;

/**
 * Topographic drainage basin and fluvial hydrography flow accumulator.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PlanetHydrographyFlowHelper {

    private PlanetHydrographyFlowHelper() {
    }

    public static void accumulateHydrographyFlow(List<H3Cell> cells, PlanetPreset preset, java.util.function.BooleanSupplier cancelSupplier) {
        if (cells == null || cells.isEmpty() || preset.atmospherePressureAtm() < 0.01) return;

        double waterLvl = preset.waterLevel();
        List<H3Cell> landCells = cells.stream()
                .filter(c -> c != null && c.getH3Index() != null && c.getElevation() != null && c.getElevation() > waterLvl)
                .sorted((c1, c2) -> Double.compare(c2.getElevation(), c1.getElevation()))
                .toList();

        if (landCells.isEmpty()) return;

        Map<Long, H3Cell> cellMap = new java.util.HashMap<>(cells.size());
        for (H3Cell c : cells) {
            if (c != null && c.getH3Index() != null) {
                cellMap.put(c.getH3Index(), c);
            }
        }

        Map<Long, Double> flowAccum = new java.util.concurrent.ConcurrentHashMap<>();
        for (H3Cell c : landCells) {
            if (c == null || c.getH3Index() == null) continue;
            flowAccum.put(c.getH3Index(), c.getRainfall() != null ? c.getRainfall() : 0.2);
        }

        H3Service h3Service = H3Service.getInstance();

        int step = 0;
        for (H3Cell c : landCells) {
            if (c == null || c.getH3Index() == null) continue;
            if ((cancelSupplier != null && (step++ % 20 == 0) && cancelSupplier.getAsBoolean()) || Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Hydrography flow calculation cancelled by user");
            }
            double curElev = c.getElevation() != null ? c.getElevation() : 0.0;
            double curFlow = flowAccum.getOrDefault(c.getH3Index(), 0.2);

            H3Cell lowestNeighbor = null;
            double minElev = curElev;

            List<Long> neighborIndices = h3Service.getNeighbors(c.getH3Index());
            for (Long nIdx : neighborIndices) {
                H3Cell n = cellMap.get(nIdx);
                if (n != null && n.getElevation() != null && n.getElevation() < minElev) {
                    minElev = n.getElevation();
                    lowestNeighbor = n;
                }
            }

            if (lowestNeighbor != null && lowestNeighbor.getH3Index() != null && lowestNeighbor.getElevation() > waterLvl) {
                long nIdx = lowestNeighbor.getH3Index();
                flowAccum.put(nIdx, flowAccum.getOrDefault(nIdx, 0.2) + curFlow * 0.85);
            }
        }

        for (H3Cell c : landCells) {
            if (c == null || c.getH3Index() == null) continue;
            double rawFlow = flowAccum.getOrDefault(c.getH3Index(), 0.0);
            double normalizedRiver = Math.min(1.0, Math.max(0.0, Math.log1p(rawFlow) / 3.5));
            c.setWaterResource(normalizedRiver * 1000.0);

            double baseAquifer = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 200.0;
            c.setFreshwaterAquifer(Math.min(1000.0, Math.max(0.0, baseAquifer + normalizedRiver * 400.0)));
        }
    }
}
