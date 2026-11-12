/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.analytics.SimulationHistoryCoordinator;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Package-private helper synchronizing Data-Oriented Design (DOD) arrays ({@link WorldBuffer}
 * and {@link AgentBuffer}) with {@link H3Cell} entity instances and restoring world snapshots.
 *
 * <p>Synchronizes conserved primary biophysical observables exclusively ({@code AGENTS.md} Section 10.1).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see H3BufferAndEventCoordinator
 */
final class H3BufferSyncHelper {

    private H3BufferSyncHelper() {
        // Static package-private helper
    }

    /**
     * Restores cell states and {@link WorldBuffer} from a historical world snapshot at or before {@code targetTicks}.
     *
     * @param historyManager history snapshot manager
     * @param cells          H3 cell list
     * @param worldBuffer    DOD world buffer
     * @param targetTicks    target simulation tick
     */
    static void restoreWorldSnapshotAtTick(
            SimulationHistoryCoordinator historyManager,
            List<H3Cell> cells,
            WorldBuffer worldBuffer,
            long targetTicks) {
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();
        if (!snapshots.isEmpty()) {
            Map.Entry<Long, List<H3Cell>> entry = snapshots.floorEntry(targetTicks);
            if (entry == null) {
                entry = snapshots.firstEntry();
            }
            if (entry != null && entry.getValue() != null) {
                List<H3Cell> snapshot = entry.getValue();
                Map<Long, H3Cell> map = snapshot.stream()
                        .filter(c -> c != null && c.getH3Index() != null)
                        .collect(Collectors.toMap(H3Cell::getH3Index, c -> c, (a, b) -> a));
                for (H3Cell c : cells) {
                    if (c == null || c.getH3Index() == null) continue;
                    H3Cell snap = map.get(c.getH3Index());
                    if (snap != null) {
                        c.setPopulation(snap.getPopulation());
                        c.setTemperature(snap.getTemperature());
                        c.setFoodResource(snap.getFoodResource());
                        c.setWaterResource(snap.getWaterResource());
                        c.setFreshwaterAquifer(snap.getFreshwaterAquifer());
                        c.setBiomassHuman(snap.getBiomassHuman());
                        c.setBiomassNatural(snap.getBiomassNatural());
                        c.setBiomassLivestock(snap.getBiomassLivestock());
                        c.setBiomassAgriculture(snap.getBiomassAgriculture());
                        c.setResourceCapital(snap.getResourceCapital());
                        c.setPollutionLevel(snap.getPollutionLevel());
                    }
                }
                if (worldBuffer != null) {
                    DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
                }
            }
        }
    }

    /**
     * Synchronizes DOD {@link WorldBuffer} arrays into {@link H3Cell} entity instances.
     *
     * @param worldBuffer       DOD world buffer
     * @param cells             H3 cell list
     * @param performanceConfig performance configuration controlling parallel execution
     */
    static void syncBufferToCells(
            WorldBuffer worldBuffer,
            List<H3Cell> cells,
            SimulationPerformanceConfig performanceConfig) {
        if (worldBuffer == null || cells == null) return;
        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] water = worldBuffer.getWaterResource();
        float[] wood = worldBuffer.getWoodResource();
        float[] gini = worldBuffer.getGiniIndex();
        float[] capital = worldBuffer.getResourceCapital();
        byte[] biomes = worldBuffer.getBiomes();

        Biome[] biomeValues = Biome.VALUES;
        int limit = Math.min(cells.size(), worldBuffer.getCapacity());

        boolean parallel = limit >= 10000 && performanceConfig != null
                && (performanceConfig.isEnableParallelExecution() || performanceConfig.getParallelThreadCount() > 1);
        if (parallel) {
            IntStream.range(0, limit).parallel().forEach(i -> syncSingleBufferElementToCell(
                    i, cells.get(i), worldBuffer, pop, food, water, wood, gini, capital, biomes, biomeValues));
        } else {
            for (int i = 0; i < limit; i++) {
                syncSingleBufferElementToCell(
                        i, cells.get(i), worldBuffer, pop, food, water, wood, gini, capital, biomes, biomeValues);
            }
        }
    }

    private static void syncSingleBufferElementToCell(
            int i,
            H3Cell cell,
            WorldBuffer worldBuffer,
            float[] pop,
            float[] food,
            float[] water,
            float[] wood,
            float[] gini,
            float[] capital,
            byte[] biomes,
            Biome[] biomeValues) {
        if (cell == null) return;
        int p = Math.max(0, Math.round(pop[i]));
        cell.setPopulation(p);
        cell.setBiomassHuman((double) pop[i]);
        cell.setFoodResource((double) food[i]);
        cell.setWaterResource((double) water[i]);
        cell.setWoodResource((double) wood[i]);
        cell.setGiniIndex((double) gini[i]);
        if (capital != null) cell.setResourceCapital((double) capital[i]);
        if (worldBuffer.getMetalResource() != null) cell.setResourceMetal((double) worldBuffer.getMetalResource()[i]);
        if (worldBuffer.getClayResource() != null) cell.setResourceClay((double) worldBuffer.getClayResource()[i]);
        if (worldBuffer.getBiomassAgriculture() != null) cell.setBiomassAgriculture((double) worldBuffer.getBiomassAgriculture()[i]);
        if (worldBuffer.getBiomassNatural() != null) cell.setBiomassNatural((double) worldBuffer.getBiomassNatural()[i]);
        if (worldBuffer.getBiomassLivestock() != null) cell.setBiomassLivestock((double) worldBuffer.getBiomassLivestock()[i]);
        if (worldBuffer.getBiomassFish() != null) cell.setBiomassFish((double) worldBuffer.getBiomassFish()[i]);
        if (worldBuffer.getResourceCoal() != null) cell.setResourceCoal((double) worldBuffer.getResourceCoal()[i]);
        if (worldBuffer.getResourceOil() != null) cell.setResourceOil((double) worldBuffer.getResourceOil()[i]);
        if (worldBuffer.getResourceGas() != null) cell.setResourceGas((double) worldBuffer.getResourceGas()[i]);
        if (worldBuffer.getResourceUranium() != null) cell.setResourceUranium((double) worldBuffer.getResourceUranium()[i]);
        if (worldBuffer.getFreshwaterAquifer() != null) cell.setFreshwaterAquifer((double) worldBuffer.getFreshwaterAquifer()[i]);
        if (worldBuffer.getSoilDepth() != null) cell.setSoilDepth((double) worldBuffer.getSoilDepth()[i]);
        if (worldBuffer.getSoilOrganicCarbon() != null) cell.setSoilOrganicCarbon((double) worldBuffer.getSoilOrganicCarbon()[i]);
        if (worldBuffer.getCommercialFluxVolume() != null) cell.setTradeNetwork((double) worldBuffer.getCommercialFluxVolume()[i]);
        if (worldBuffer.getPollutionLevel() != null) cell.setPollutionLevel((double) worldBuffer.getPollutionLevel()[i]);

        if (cell.getBiome() != null) {
            biomes[i] = (byte) cell.getBiome().ordinal();
        } else {
            int bIdx = biomes[i] & 0xFF;
            if (bIdx < biomeValues.length) {
                cell.setBiome(biomeValues[bIdx]);
            }
        }
    }

    /**
     * Synchronizes domain states from {@link H3Cell} objects back into {@link WorldBuffer} and {@link AgentBuffer}.
     *
     * @param worldBuffer       DOD world buffer
     * @param agentBuffer       DOD agent buffer
     * @param cells             H3 cell list
     * @param performanceConfig performance configuration controlling parallel execution
     */
    static void syncCellsToBuffer(
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            List<H3Cell> cells,
            SimulationPerformanceConfig performanceConfig) {
        if (worldBuffer == null || cells == null) return;

        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] water = worldBuffer.getWaterResource();
        float[] wood = worldBuffer.getWoodResource();
        float[] metal = worldBuffer.getMetalResource();
        float[] clay = worldBuffer.getClayResource();
        float[] gini = worldBuffer.getGiniIndex();
        float[] capital = worldBuffer.getResourceCapital();
        float[] lifespan = worldBuffer.getLifespan();
        float[] fertility = worldBuffer.getFertility();
        float[] biomassAgri = worldBuffer.getBiomassAgriculture();
        float[] biomassNat = worldBuffer.getBiomassNatural();
        float[] biomassLivestock = worldBuffer.getBiomassLivestock();
        float[] biomassFish = worldBuffer.getBiomassFish();
        byte[] biomes = worldBuffer.getBiomes();

        int limit = Math.min(cells.size(), worldBuffer.getCapacity());
        boolean popModified = false;
        boolean[] cellPopModified = new boolean[limit];

        boolean parallel = limit >= 10000 && performanceConfig != null
                && (performanceConfig.isEnableParallelExecution() || performanceConfig.getParallelThreadCount() > 1);
        if (parallel) {
            IntStream.range(0, limit).parallel().forEach(i -> syncSingleCellToBufferElement(
                    i, cells.get(i), worldBuffer, pop, food, water, wood, metal, clay, gini, capital,
                    lifespan, fertility, biomassAgri, biomassNat, biomassLivestock, biomassFish, biomes, cellPopModified));
            for (int i = 0; i < limit; i++) {
                if (cellPopModified[i]) {
                    popModified = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < limit; i++) {
                if (syncSingleCellToBufferElement(
                        i, cells.get(i), worldBuffer, pop, food, water, wood, metal, clay, gini, capital,
                        lifespan, fertility, biomassAgri, biomassNat, biomassLivestock, biomassFish, biomes, cellPopModified)) {
                    popModified = true;
                }
            }
        }

        if (agentBuffer != null && pop != null && popModified) {
            reconcileAgentBufferCohorts(worldBuffer, agentBuffer, pop, limit, cellPopModified);
        }
    }

    private static void reconcileAgentBufferCohorts(
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            float[] pop,
            int limit,
            boolean[] cellPopModified) {
        int agentCap = agentBuffer.getCapacity();
        int[] hexIds = agentBuffer.getHexIds();
        float[] mass = agentBuffer.getMass();

        float[] currentCohortSum = new float[limit];
        for (int a = 0; a < agentCap; a++) {
            int h = hexIds[a];
            if (h >= 0 && h < limit && mass[a] > 0.0f) {
                currentCohortSum[h] += mass[a];
            }
        }
        for (int a = 0; a < agentCap; a++) {
            int h = hexIds[a];
            if (h >= 0 && h < limit && cellPopModified[h]) {
                float sum = currentCohortSum[h];
                if (pop[h] <= 0.001f) {
                    mass[a] = 0.0f;
                    hexIds[a] = -1;
                } else if (sum > 0.001f && Math.abs(pop[h] - sum) > 0.01f) {
                    float ratio = Math.max(0.0f, pop[h] / sum);
                    mass[a] *= ratio;
                }
            }
        }
        int freeSearchStart = 0;
        for (int h = 0; h < limit; h++) {
            if (cellPopModified[h] && currentCohortSum[h] <= 0.001f && pop[h] > 0.001f) {
                while (freeSearchStart < agentCap && hexIds[freeSearchStart] != -1) {
                    freeSearchStart++;
                }
                if (freeSearchStart < agentCap) {
                    hexIds[freeSearchStart] = h;
                    if (worldBuffer.getH3Indexes() != null && h < worldBuffer.getH3Indexes().length) {
                        agentBuffer.getH3Indexes()[freeSearchStart] = worldBuffer.getH3Indexes()[h];
                    }
                    mass[freeSearchStart] = pop[h];
                    agentBuffer.getEnergy()[freeSearchStart] = 85.0f;
                    agentBuffer.getAge()[freeSearchStart] = 24.5f;
                    currentCohortSum[h] = pop[h];
                    freeSearchStart++;
                }
            }
        }
    }

    private static boolean syncSingleCellToBufferElement(
            int i,
            H3Cell cell,
            WorldBuffer worldBuffer,
            float[] pop,
            float[] food,
            float[] water,
            float[] wood,
            float[] metal,
            float[] clay,
            float[] gini,
            float[] capital,
            float[] lifespan,
            float[] fertility,
            float[] biomassAgri,
            float[] biomassNat,
            float[] biomassLivestock,
            float[] biomassFish,
            byte[] biomes,
            boolean[] cellPopModified) {
        if (cell == null) return false;
        boolean modified = false;
        if (pop != null && cell.getPopulation() != null) {
            int cellIntPop = cell.getPopulation();
            int bufferRoundedPop = Math.max(0, Math.round(pop[i]));
            if (cellIntPop != bufferRoundedPop) {
                int delta = cellIntPop - bufferRoundedPop;
                pop[i] = Math.max(0.0f, pop[i] + delta);
                cellPopModified[i] = true;
                modified = true;
            }
        }
        if (food != null && cell.getFoodResource() != null) food[i] = cell.getFoodResource().floatValue();
        if (water != null && cell.getWaterResource() != null) water[i] = cell.getWaterResource().floatValue();
        if (wood != null && cell.getWoodResource() != null) wood[i] = cell.getWoodResource().floatValue();
        if (metal != null && cell.getResourceMetal() != null) metal[i] = cell.getResourceMetal().floatValue();
        if (clay != null && cell.getResourceClay() != null) clay[i] = cell.getResourceClay().floatValue();
        if (gini != null && cell.getGiniIndex() != null) gini[i] = cell.getGiniIndex().floatValue();
        if (capital != null && cell.getResourceCapital() != null) capital[i] = cell.getResourceCapital().floatValue();
        if (lifespan != null && cell.getLifespan() != null) lifespan[i] = cell.getLifespan().floatValue();
        if (cell.getResourceCoal() != null && worldBuffer.getResourceCoal() != null) worldBuffer.getResourceCoal()[i] = cell.getResourceCoal().floatValue();
        if (cell.getResourceOil() != null && worldBuffer.getResourceOil() != null) worldBuffer.getResourceOil()[i] = cell.getResourceOil().floatValue();
        if (cell.getResourceGas() != null && worldBuffer.getResourceGas() != null) worldBuffer.getResourceGas()[i] = cell.getResourceGas().floatValue();
        if (cell.getResourceUranium() != null && worldBuffer.getResourceUranium() != null) worldBuffer.getResourceUranium()[i] = cell.getResourceUranium().floatValue();
        if (cell.getFreshwaterAquifer() != null && worldBuffer.getFreshwaterAquifer() != null) worldBuffer.getFreshwaterAquifer()[i] = cell.getFreshwaterAquifer().floatValue();
        if (cell.getSoilDepth() != null && worldBuffer.getSoilDepth() != null) worldBuffer.getSoilDepth()[i] = cell.getSoilDepth().floatValue();
        if (cell.getSoilOrganicCarbon() != null && worldBuffer.getSoilOrganicCarbon() != null) worldBuffer.getSoilOrganicCarbon()[i] = cell.getSoilOrganicCarbon().floatValue();
        if (cell.getTradeNetwork() != null && worldBuffer.getCommercialFluxVolume() != null) worldBuffer.getCommercialFluxVolume()[i] = cell.getTradeNetwork().floatValue();
        if (cell.getPollutionLevel() != null && worldBuffer.getPollutionLevel() != null) worldBuffer.getPollutionLevel()[i] = cell.getPollutionLevel().floatValue();
        if (fertility != null && cell.getFertility() != null) fertility[i] = cell.getFertility().floatValue();
        if (biomassAgri != null && cell.getBiomassAgriculture() != null) biomassAgri[i] = cell.getBiomassAgriculture().floatValue();
        if (biomassNat != null && cell.getBiomassNatural() != null) biomassNat[i] = cell.getBiomassNatural().floatValue();
        if (biomassLivestock != null && cell.getBiomassLivestock() != null) biomassLivestock[i] = cell.getBiomassLivestock().floatValue();
        if (biomassFish != null && cell.getBiomassFish() != null) biomassFish[i] = cell.getBiomassFish().floatValue();
        if (biomes != null && cell.getBiome() != null) biomes[i] = (byte) cell.getBiome().ordinal();
        return modified;
    }
}
