/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.events.ActiveEvent;
import org.ether.society.events.EventSystem;
import org.ether.society.model.Biome;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.Scenario;
import org.ether.society.util.DeterministicPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h1>H3BufferAndEventCoordinator</h1>
 * <p>
 * Coordinates Data-Oriented Design (DOD) buffer synchronization ({@link WorldBuffer} $\leftrightarrow$ {@link H3Cell}
 * and {@link AgentBuffer}), scenario-scheduled climate/cataclysm event impacts, political seeding, and historical
 * world snapshot restoration for {@link H3SimulationEngine}.<br>
 * Extracted from {@link H3SimulationEngine} to enforce the Single Responsibility Principle.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3SimulationEngine
 */
public final class H3BufferAndEventCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(H3BufferAndEventCoordinator.class);

    private H3BufferAndEventCoordinator() {
        // Static coordinator utility class
    }

    /**
     * Seeds initial population and founding polities when no scenario pre-compute phase is present.
     *
     * @param cells           H3 cell list
     * @param currentScenario active scenario (may be null)
     * @param nations         mutable list of active polities
     */
    public static void initializePopulation(List<H3Cell> cells, Scenario currentScenario, List<Polity> nations) {
        if (cells == null) return;
        int existingCount = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                existingCount++;
            }
        }

        if (existingCount > 0) {
            initializePoliticalSeeding(cells, nations, existingCount);
            return;
        }

        int populatedCells = 0;
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome biome = cell.getBiome();
            if (biome == null || biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN) {
                continue;
            }

            int basePop = switch (biome) {
                case PLAINS -> 50;
                case FOREST -> 30;
                case JUNGLE -> 40;
                case HILLS -> 20;
                case BEACH -> 25;
                case MOUNTAINS -> 10;
                case TUNDRA -> 5;
                case DESERT -> 3;
                case SNOW -> 2;
                default -> 0;
            };

            long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
            long sSeed = currentScenario != null && currentScenario.getSeed() != 0L ? currentScenario.getSeed() : 42L;
            if (DeterministicPRNG.nextDouble(sSeed, 0L, cellId, 503L) < 0.1 && basePop > 0) {
                cell.setPopulation(basePop);
                populatedCells++;
            }
        }

        initializePoliticalSeeding(cells, nations, populatedCells);
    }

    /**
     * Initializes the top populated cells as founding political entities ({@link Polity}).
     *
     * @param cells          H3 cell list
     * @param nations        mutable list of polities
     * @param populatedCount number of populated cells
     */
    public static void initializePoliticalSeeding(List<H3Cell> cells, List<Polity> nations, int populatedCount) {
        if (populatedCount < 3 || cells == null || cells.isEmpty()) return;
        nations.clear();
        cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .limit(3)
                .forEach(c -> {
                    if (c.getOwner() == null) {
                        String name = "Realm of Hex " + Long.toHexString(c.getH3Index()).toUpperCase();
                        int argb = java.awt.Color.HSBtoRGB((float) DeterministicPRNG.nextDouble(c.getH3Index(), 777), 0.8f, 0.9f);
                        Polity nation = new Polity(name, argb, c);
                        nations.add(nation);
                    }
                });
    }

    /**
     * Checks and triggers scheduled scenario climate or cataclysm events for the current simulation year.
     *
     * @param currentScenario        active scenario
     * @param timeManager            simulation time manager
     * @param tickCounter            current tick index
     * @param firedScenarioEventKeys set of already-triggered event keys
     * @param eventSystem            event system recorder
     * @param cells                  H3 cell list
     * @param worldBuffer            DOD world buffer
     * @param agentBuffer            DOD agent buffer
     */
    public static void checkScenarioClimateEvents(
            Scenario currentScenario,
            TimeManager timeManager,
            long tickCounter,
            Set<String> firedScenarioEventKeys,
            EventSystem eventSystem,
            List<H3Cell> cells,
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer) {
        if (currentScenario == null || currentScenario.getClimateEvents() == null || currentScenario.getClimateEvents().isEmpty()) {
            return;
        }
        int currentYear = timeManager.getCurrentYear();
        for (ClimateEvent evt : currentScenario.getClimateEvents()) {
            if (evt.getYear() == currentYear) {
                String eventKey = evt.getName() + "_" + evt.getYear() + "_" + evt.getType();
                if (!firedScenarioEventKeys.contains(eventKey)) {
                    firedScenarioEventKeys.add(eventKey);
                    String type = evt.getType() != null ? evt.getType().toLowerCase() : "";
                    String displayTitle;
                    if (type.contains("milestone") || type.contains("hist")) {
                        displayTitle = "📜 REPÈRE : " + evt.getName();
                    } else if (type.contains("nuclear") || type.contains("strike")) {
                        displayTitle = "☢️ FRAPPE NUCLÉAIRE : " + evt.getName() + " (Mag: " + evt.getMagnitude() + ")";
                    } else {
                        displayTitle = "🌋 ÉVÉNEMENT : " + evt.getName() + " (" + evt.getType() + " - Mag: " + evt.getMagnitude() + ")";
                    }
                    ActiveEvent ae = new ActiveEvent(
                            "SCENARIO_EVT_" + currentYear + "_" + tickCounter,
                            displayTitle,
                            evt.getType().toUpperCase(),
                            evt.getLatitude(), evt.getLongitude(),
                            currentYear, timeManager.getCurrentMonth(), 1, 25.0, evt.getMagnitude()
                    );
                    eventSystem.recordSpatialEvent(ae);
                    logger.info("🌋 [Tick {}] Scenario event triggered: {} (Year {})", tickCounter, evt.getName(), currentYear);
                    applyClimateEventImpact(evt, cells, worldBuffer, agentBuffer);
                }
            }
        }
    }

    /**
     * Applies physical, demographic, and capital shocks from a triggered {@link ClimateEvent} to affected cells and DOD buffers.
     *
     * @param evt         climate or cataclysm event
     * @param cells       H3 cell list
     * @param worldBuffer DOD world buffer
     * @param agentBuffer DOD agent buffer
     */
    public static void applyClimateEventImpact(
            ClimateEvent evt,
            List<H3Cell> cells,
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer) {
        if (cells == null || cells.isEmpty()) return;
        double evtLat = evt.getLatitude();
        double evtLng = evt.getLongitude();
        double mag = evt.getMagnitude();
        int totalCells = cells.size();
        float[] cellSurvival = null;
        boolean anyAgentMortality = false;

        for (int i = 0; i < totalCells; i++) {
            H3Cell cell = cells.get(i);
            if (cell == null) continue;
            double cLat = cell.getLatitudeOr(0.0);
            double cLng = cell.getLongitudeOr(0.0);
            double distDeg = Math.hypot(cLat - evtLat, cLng - evtLng);

            String type = evt.getType() != null ? evt.getType().toLowerCase() : "";
            double impactRadius = (type.contains("pandemic") || type.contains("plague") || type.contains("epidemic"))
                    ? (evt.getName() != null && (evt.getName().toLowerCase().contains("grippe") || evt.getName().toLowerCase().contains("flu")) ? 180.0 : 85.0)
                    : Math.max(5.0, mag * 3.0);
            if (distDeg <= impactRadius) {
                double attenuation = 1.0 - (distDeg / impactRadius);
                int lost = 0;
                int prevPop = cell.getPopulationOr(0);

                if (type.contains("nuclear") || type.contains("strike")) {
                    double cooling = -0.6 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) + cooling));
                    if (prevPop > 0) {
                        double mortalityFrac = Math.min(0.95, 0.85 * attenuation);
                        lost = (int) Math.round(prevPop * mortalityFrac);
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-1.2 * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-1.6 * attenuation)));
                    }
                    cell.setPollutionLevel(Math.min(1.0, cell.getPollutionLevelOr(0.0) + 0.8 * attenuation));
                } else if (type.contains("volcano") || type.contains("ice")) {
                    double cooling = -0.5 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) + cooling));
                } else if (type.contains("cyclone") || type.contains("hurricane") || type.contains("storm") || type.contains("flood")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.15 * mag * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.25 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        double survival = Math.exp(-0.04 * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("epidemic") || type.contains("pandemic") || type.contains("plague")) {
                    if (prevPop > 0) {
                        double virulence = 0.15;
                        String tLower = evt.getName() != null ? evt.getName().toLowerCase() : "";
                        if (tLower.contains("grippe") || tLower.contains("influenza") || tLower.contains("flu")) {
                            virulence = 0.035; // Spanish Flu (1918): ~2.5-3.5% historical lethality (~50M deaths / 1.8B pop)
                        } else if (tLower.contains("peste") || tLower.contains("black death") || tLower.contains("plague")) {
                            virulence = 0.35;  // Black Death (1347): ~30-35% Eurasian lethality (Yersinia pestis)
                        }
                        double survival = Math.exp(-virulence * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.05 * mag * attenuation)));
                    }
                } else if (type.contains("earthquake") || type.contains("tsunami") || type.contains("meteor")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.08 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        double survival = Math.exp(-0.05 * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("famine") || type.contains("drought")) {
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.10 * mag * attenuation)));
                    }
                }

                if (worldBuffer != null && i < worldBuffer.getCapacity()) {
                    if (cell.getTemperature() != null) {
                        worldBuffer.getTemperature()[i] = cell.getTemperature().floatValue();
                    }
                    if (cell.getResourceCapital() != null) {
                        worldBuffer.getResourceCapital()[i] = cell.getResourceCapital().floatValue();
                    }
                    if (cell.getFoodResource() != null) {
                        worldBuffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
                    }
                    if (cell.getPopulation() != null) {
                        worldBuffer.getBiomassHuman()[i] = cell.getPopulation().floatValue();
                    }
                }

                if (agentBuffer != null && lost > 0 && prevPop > 0) {
                    if (cellSurvival == null) {
                        cellSurvival = new float[totalCells];
                        Arrays.fill(cellSurvival, 1.0f);
                    }
                    float mortalityFraction = Math.clamp((float) lost / Math.max(1.0f, (float) prevPop), 0.0f, 1.0f);
                    cellSurvival[i] = 1.0f - mortalityFraction;
                    anyAgentMortality = true;
                }
            }
        }

        if (anyAgentMortality && agentBuffer != null && cellSurvival != null) {
            int cap = agentBuffer.getCapacity();
            int[] hexIds = agentBuffer.getHexIds();
            float[] mass = agentBuffer.getMass();
            float[] deaths = agentBuffer.getDeaths();
            for (int a = 0; a < cap; a++) {
                int hId = hexIds[a];
                if (hId >= 0 && hId < totalCells && mass[a] > 0.0f) {
                    float surv = cellSurvival[hId];
                    if (surv < 1.0f) {
                        float oldM = mass[a];
                        float newM = oldM * surv;
                        mass[a] = newM;
                        deaths[a] += (oldM - newM);
                    }
                }
            }
        }
    }

    /**
     * Restores cell states and WorldBuffer from a historical world snapshot at or before {@code targetTicks}.
     *
     * @param historyManager history snapshot manager
     * @param cells          H3 cell list
     * @param worldBuffer    DOD world buffer
     * @param targetTicks    target simulation tick
     */
    public static void restoreWorldSnapshotAtTick(
            HistoryManager historyManager,
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
                        c.setTechnologyLevel(snap.getTechnologyLevel());
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
    public static void syncBufferToCells(
            WorldBuffer worldBuffer,
            List<H3Cell> cells,
            SimulationPerformanceConfig performanceConfig) {
        if (worldBuffer == null || cells == null) return;
        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] tech = worldBuffer.getTechnologyLevel();
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
                    i, cells.get(i), worldBuffer, pop, food, tech, water, wood, gini, capital, biomes, biomeValues));
        } else {
            for (int i = 0; i < limit; i++) {
                syncSingleBufferElementToCell(
                        i, cells.get(i), worldBuffer, pop, food, tech, water, wood, gini, capital, biomes, biomeValues);
            }
        }
    }

    private static void syncSingleBufferElementToCell(
            int i,
            H3Cell cell,
            WorldBuffer worldBuffer,
            float[] pop,
            float[] food,
            float[] tech,
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
        cell.setTechnologyLevel((double) tech[i]);
        cell.setWaterResource((double) water[i]);
        cell.setWoodResource((double) wood[i]);
        cell.setGiniIndex((double) gini[i]);
        if (capital != null) cell.setResourceCapital((double) capital[i]);
        if (worldBuffer.getMetalResource() != null) cell.setResourceMetal((double) worldBuffer.getMetalResource()[i]);
        if (worldBuffer.getClayResource() != null) cell.setResourceClay((double) worldBuffer.getClayResource()[i]);
        if (worldBuffer.getBiomassAgriculture() != null) cell.setBiomassAgriculture((double) worldBuffer.getBiomassAgriculture()[i]);
        if (worldBuffer.getBiomassNatural() != null) cell.setBiomassNatural((double) worldBuffer.getBiomassNatural()[i]);
        if (worldBuffer.getBiomassFish() != null) cell.setBiomassFish((double) worldBuffer.getBiomassFish()[i]);

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
    public static void syncCellsToBuffer(
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            List<H3Cell> cells,
            SimulationPerformanceConfig performanceConfig) {
        if (worldBuffer == null || cells == null) return;

        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] tech = worldBuffer.getTechnologyLevel();
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
        float[] biomassFish = worldBuffer.getBiomassFish();
        byte[] biomes = worldBuffer.getBiomes();

        int limit = Math.min(cells.size(), worldBuffer.getCapacity());
        boolean popModified = false;
        boolean[] cellPopModified = new boolean[limit];

        boolean parallel = limit >= 10000 && performanceConfig != null
                && (performanceConfig.isEnableParallelExecution() || performanceConfig.getParallelThreadCount() > 1);
        if (parallel) {
            IntStream.range(0, limit).parallel().forEach(i -> syncSingleCellToBufferElement(
                    i, cells.get(i), pop, food, tech, water, wood, metal, clay, gini, capital,
                    lifespan, fertility, biomassAgri, biomassNat, biomassFish, biomes, cellPopModified));
            for (int i = 0; i < limit; i++) {
                if (cellPopModified[i]) {
                    popModified = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < limit; i++) {
                if (syncSingleCellToBufferElement(
                        i, cells.get(i), pop, food, tech, water, wood, metal, clay, gini, capital,
                        lifespan, fertility, biomassAgri, biomassNat, biomassFish, biomes, cellPopModified)) {
                    popModified = true;
                }
            }
        }

        if (agentBuffer != null && pop != null) {
            int agentCap = agentBuffer.getCapacity();
            int[] hexIds = agentBuffer.getHexIds();
            float[] mass = agentBuffer.getMass();
            float[] agentTech = agentBuffer.getTechLevel();

            if (popModified) {
                float[] currentCohortSum = new float[limit];
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit && mass[a] > 0.0f) {
                        currentCohortSum[h] += mass[a];
                    }
                }
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit) {
                        if (tech != null) {
                            agentTech[a] = tech[h];
                        }
                        if (cellPopModified[h]) {
                            float sum = currentCohortSum[h];
                            if (sum > 0.001f && Math.abs(pop[h] - sum) > 0.01f) {
                                float ratio = Math.max(0.0f, pop[h] / sum);
                                mass[a] *= ratio;
                            }
                        }
                    }
                }
            } else if (tech != null) {
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit) {
                        agentTech[a] = tech[h];
                    }
                }
            }
        }
    }

    private static boolean syncSingleCellToBufferElement(
            int i,
            H3Cell cell,
            float[] pop,
            float[] food,
            float[] tech,
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
        if (tech != null && cell.getTechnologyLevel() != null) tech[i] = cell.getTechnologyLevel().floatValue();
        if (water != null && cell.getWaterResource() != null) water[i] = cell.getWaterResource().floatValue();
        if (wood != null && cell.getWoodResource() != null) wood[i] = cell.getWoodResource().floatValue();
        if (metal != null && cell.getResourceMetal() != null) metal[i] = cell.getResourceMetal().floatValue();
        if (clay != null && cell.getResourceClay() != null) clay[i] = cell.getResourceClay().floatValue();
        if (gini != null && cell.getGiniIndex() != null) gini[i] = cell.getGiniIndex().floatValue();
        if (capital != null && cell.getResourceCapital() != null) capital[i] = cell.getResourceCapital().floatValue();
        if (lifespan != null && cell.getLifespan() != null) lifespan[i] = cell.getLifespan().floatValue();
        if (fertility != null && cell.getFertility() != null) fertility[i] = cell.getFertility().floatValue();
        if (biomassAgri != null && cell.getBiomassAgriculture() != null) biomassAgri[i] = cell.getBiomassAgriculture().floatValue();
        if (biomassNat != null && cell.getBiomassNatural() != null) biomassNat[i] = cell.getBiomassNatural().floatValue();
        if (biomassFish != null && cell.getBiomassFish() != null) biomassFish[i] = cell.getBiomassFish().floatValue();
        if (biomes != null && cell.getBiome() != null) biomes[i] = (byte) cell.getBiome().ordinal();
        return modified;
    }
}
