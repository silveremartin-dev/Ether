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
import org.ether.society.data.HistoricalStateRegistry;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.database.Polity;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.ether.society.events.ActiveEvent;
import org.ether.society.events.EventSystem;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * <h1>H3BufferAndEventCoordinator</h1>
 * <p>
 * Coordinates Data-Oriented Design (DOD) buffer synchronization ({@link WorldBuffer} $\leftrightarrow$ {@link H3Cell}
 * and {@link AgentBuffer} via {@link H3BufferSyncHelper}), scenario-scheduled climate/cataclysm event impacts,
 * political seeding, and historical world snapshot restoration for {@link H3SimulationEngine}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3SimulationEngine
 * @see H3BufferSyncHelper
 */
public final class H3BufferAndEventCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(H3BufferAndEventCoordinator.class);

    private H3BufferAndEventCoordinator() {
        // Static coordinator utility class
    }

    /**
     * Seeds initial population across cells when no raster map or pre-computed population exists.
     * Derives carrying capacity analytically from Miami Net Primary Production (Lieth 1975)
     * and hunter-gatherer density scaling (Tallavaara et al. 2015, PNAS):
     * $$\rho_{\text{HG}} \approx 0.0012 \cdot \text{NPP}^{0.75}\ \text{hab/km}^2$$
     *
     * @param cells           H3 cell list
     * @param currentScenario active scenario (may be null)
     * @param nations         mutable list of active polities
     */
    public static void initializePopulation(List<H3Cell> cells, Scenario currentScenario, List<Polity> nations) {
        if (cells == null || cells.isEmpty()) return;
        int existingCount = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                existingCount++;
            }
        }

        if (existingCount > 0) {
            initializePoliticalEntities(cells, nations, currentScenario);
            return;
        }

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double elev = cell.getElevationOr(0.0);
            if (elev < 0.0) continue;

            double temp = cell.getTemperatureOr(15.0);
            double rain = cell.getRainfallOr(500.0);
            if (temp < -20.0 || rain < 25.0) continue;

            double areaKm2 = cell.getEffectiveSurfaceAreaKm2() > 0 ? cell.getEffectiveSurfaceAreaKm2() : 1770.49;
            double npp = PreComputePhase.calculateMiamiNPP(temp, rain);
            double densityHg = 0.0012 * Math.pow(Math.max(10.0, npp), 0.75);
            int pop = (int) Math.round(densityHg * areaKm2);
            if (pop > 0) {
                cell.setPopulation(pop);
            }
        }

        initializePoliticalEntities(cells, nations, currentScenario);
    }

    /**
     * Initializes sovereign geopolitical polities ({@link Polity}) from authentic historical transitions
     * ({@link HistoricalStateRegistry}) for the active scenario year.
     * Enforces Deep-Time Invariant ({@code AGENTS.md} §8): zero sovereign states prior to $-4000$ BCE.
     *
     * @param cells           H3 cell list
     * @param nations         mutable list of polities
     * @param currentScenario active scenario
     */
    public static void initializePoliticalEntities(List<H3Cell> cells, List<Polity> nations, Scenario currentScenario) {
        if (cells == null || cells.isEmpty() || nations == null) return;
        nations.clear();

        if (currentScenario == null) return;
        long year = currentScenario.getStartDateYear();
        if (year < -4000L) {
            logger.info("Deep prehistory (Year {} BCE): Sovereign administrative polities baseline == 0.", Math.abs(year));
            return;
        }

        try {
            HistoricalStateRegistry.HistoricalEpoch epoch = HistoricalStateRegistry.getActiveEpoch(year);
            if (epoch == null || epoch.polities() == null || epoch.polities().isEmpty()) {
                logger.info("No formal polities cataloged for year {} CE in HistoricalStateRegistry.", year);
                return;
            }

            for (HistoricalStateRegistry.PolityState ps : epoch.polities()) {
                H3Cell coreCell = null;
                double minDistanceSq = Double.MAX_VALUE;
                for (H3Cell c : cells) {
                    if (c == null || c.getElevationOr(0.0) < 0.0) continue;
                    double dLat = c.getLatitudeOr(0.0) - ps.coreLat();
                    double dLon = (c.getLongitudeOr(0.0) - ps.coreLon()) * Math.cos(Math.toRadians(ps.coreLat()));
                    double distSq = dLat * dLat + dLon * dLon;
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        coreCell = c;
                    }
                }

                if (coreCell != null) {
                    Polity polity = new Polity(ps.name(), ps.colorRgb(), coreCell);
                    double maxRadiusSq = ps.radiusDeg() * ps.radiusDeg();
                    for (H3Cell c : cells) {
                        if (c == null || c.getOwner() != null || c.getElevationOr(0.0) < 0.0) continue;
                        double dLat = c.getLatitudeOr(0.0) - ps.coreLat();
                        double dLon = (c.getLongitudeOr(0.0) - ps.coreLon()) * Math.cos(Math.toRadians(ps.coreLat()));
                        if (dLat * dLat + dLon * dLon <= maxRadiusSq) {
                            polity.addCell(c);
                        }
                    }
                    nations.add(polity);
                }
            }
            logger.info("Initialized {} authentic historical polities for epoch '{}' (Year {} CE).",
                    nations.size(), epoch.label(), year);
        } catch (Exception e) {
            logger.warn("HistoricalStateRegistry query skipped for year {}: {}", year, e.getMessage());
        }
    }

    /**
     * Legacy backward-compatibility method.
     *
     * @param cells          H3 cell list
     * @param nations        polity list
     * @param populatedCount populated cell count
     * @deprecated Replaced by {@link #initializePoliticalEntities(List, List, Scenario)}
     */
    @Deprecated
    public static void initializePoliticalSeeding(List<H3Cell> cells, List<Polity> nations, int populatedCount) {
        // Intentionally deprecated: do not seed dummy 1-cell 'Realm of Hex' nations
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
            double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                    cell.getLatitudeOr(0.0), cell.getLongitudeOr(0.0), evtLat, evtLng);

            String type = evt.getType() != null ? evt.getType().toLowerCase() : "";
            double impactRadiusDeg = (type.contains("pandemic") || type.contains("plague") || type.contains("epidemic"))
                    ? (evt.getName() != null && (evt.getName().toLowerCase().contains("grippe") || evt.getName().toLowerCase().contains("flu")) ? 180.0 : 85.0)
                    : Math.max(5.0, mag * 3.0);
            double impactRadiusKm = impactRadiusDeg * 111.32;
            if (distKm <= impactRadiusKm) {
                double attenuation = 1.0 - (distKm / impactRadiusKm);
                int lost = 0;
                int prevPop = cell.getPopulationOr(0);

                if (type.contains("nuclear") || type.contains("strike")) {
                    cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - 0.6 * mag * attenuation));
                    if (prevPop > 0) {
                        lost = (int) Math.round(prevPop * Math.min(0.95, 0.85 * attenuation));
                        H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-1.2 * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-1.6 * attenuation)));
                    }
                    cell.setPollutionLevel(Math.min(1.0, cell.getPollutionLevelOr(0.0) + 0.8 * attenuation));
                } else if (type.contains("volcano") || type.contains("ice")) {
                    cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - 0.5 * mag * attenuation));
                } else if (type.contains("cyclone") || type.contains("hurricane") || type.contains("storm") || type.contains("flood")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.15 * mag * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.25 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        lost = (int) Math.round(prevPop * (1.0 - Math.exp(-0.04 * mag * attenuation)));
                        H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("epidemic") || type.contains("pandemic") || type.contains("plague")) {
                    if (prevPop > 0) {
                        double virulence = 0.15;
                        String tLower = evt.getName() != null ? evt.getName().toLowerCase() : "";
                        if (tLower.contains("grippe") || tLower.contains("influenza") || tLower.contains("flu")) {
                            virulence = 0.035;
                        } else if (tLower.contains("peste") || tLower.contains("black death") || tLower.contains("plague")) {
                            virulence = 0.35;
                        }
                        lost = (int) Math.round(prevPop * (1.0 - Math.exp(-virulence * mag * attenuation)));
                        H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.05 * mag * attenuation)));
                    }
                } else if (type.contains("earthquake") || type.contains("tsunami") || type.contains("meteor")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.08 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        lost = (int) Math.round(prevPop * (1.0 - Math.exp(-0.05 * mag * attenuation)));
                        H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("famine") || type.contains("drought")) {
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.10 * mag * attenuation)));
                    }
                }

                if (worldBuffer != null && i < worldBuffer.getCapacity()) {
                    if (cell.getTemperature() != null) worldBuffer.getTemperature()[i] = cell.getTemperature().floatValue();
                    if (cell.getResourceCapital() != null) worldBuffer.getResourceCapital()[i] = cell.getResourceCapital().floatValue();
                    if (cell.getFoodResource() != null) worldBuffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
                    if (cell.getPopulation() != null) worldBuffer.getBiomassHuman()[i] = cell.getPopulation().floatValue();
                }

                if (agentBuffer != null && lost > 0 && prevPop > 0) {
                    if (cellSurvival == null) {
                        cellSurvival = new float[totalCells];
                        Arrays.fill(cellSurvival, 1.0f);
                    }
                    cellSurvival[i] = 1.0f - Math.clamp((float) lost / Math.max(1.0f, (float) prevPop), 0.0f, 1.0f);
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
     * Restores cell states and {@link WorldBuffer} from a historical world snapshot at or before {@code targetTicks}.
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
        H3BufferSyncHelper.restoreWorldSnapshotAtTick(historyManager, cells, worldBuffer, targetTicks);
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
        H3BufferSyncHelper.syncBufferToCells(worldBuffer, cells, performanceConfig);
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
        H3BufferSyncHelper.syncCellsToBuffer(worldBuffer, agentBuffer, cells, performanceConfig);
    }
}
