/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.database.Polity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.ether.society.core.EventCellularShockHelper.*;

/**
 * <h1>EventPhysicalImpactExecutor</h1>
 * <p>
 * Centralized, fully headless physical execution engine for planetary events,
 * astronomical forcings, cataclysms, and ecological shocks.
 * </p>
 * <p>
 * <b>Epistemic Modeling Principle — Exogenous Boundary Forcings vs. Emergence:</b><br>
 * In macroeconomic and Earth System Models (ESMs, CMIP6, IPCC), external shocks such as
 * explosive volcanic eruptions (e.g., 1815 Tambora, 1257 Samalas) or extraterrestrial impacts
 * are not artificial ad-hoc overrides of internal dynamics; they constitute authentic
 * <i>exogenous boundary conditions</i>. Ether strictly routes these external events through
 * biophysical conservation channels:
 * <ul>
 *   <li>Atmospheric aerosol optical depth \(\tau\) attenuating top-of-atmosphere solar irradiance:
 *       \(S = S_0 \cdot e^{-\tau}\) (Beer-Lambert law).</li>
 *   <li>Thermodynamic surface cooling and precipitation reduction impacting net primary productivity (NPP).</li>
 *   <li>Physical infrastructure and agricultural capital degradation through exponential survival curves:
 *       \(K(t^+) = K(t^-) \cdot e^{-\alpha \cdot \text{magnitude}}\).</li>
 * </ul>
 * Governs both scheduled scenario shocks and live exploratory sandbox interventions,
 * ensuring complete bitwise and mathematical parity between GUI and headless executions
 * ({@code AGENTS.md} §1, §7, §10).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class EventPhysicalImpactExecutor {

    private static final Logger logger = LoggerFactory.getLogger(EventPhysicalImpactExecutor.class);

    private EventPhysicalImpactExecutor() {
        // Static headless utility class
    }

    /**
     * Finds the nearest simulation cell to the specified latitude and longitude coordinates.
     *
     * @param cells cell list
     * @param lat target latitude in degrees
     * @param lng target longitude in degrees
     * @return nearest H3Cell, or {@code null} if list is empty
     */
    public static H3Cell findNearestCell(List<H3Cell> cells, double lat, double lng) {
        if (cells == null || cells.isEmpty()) return null;
        H3Cell nearest = null;
        double minD = Double.MAX_VALUE;
        for (H3Cell c : cells) {
            if (c == null) continue;
            double d = Math.hypot(c.getLatitudeOr(0.0) - lat, c.getLongitudeOr(0.0) - lng);
            if (d < minD) {
                minD = d;
                nearest = c;
            }
        }
        return nearest;
    }

    /**
     * Checks if the event type is strictly terrestrial (cannot originate in open ocean).
     */
    public static boolean isTerrestrialEventType(String type) {
        return isTerrestrialEventType(org.ether.society.model.PhysicalShockType.fromString(type));
    }

    /**
     * Checks if the event type is strictly terrestrial (cannot originate in open ocean).
     */
    public static boolean isTerrestrialEventType(org.ether.society.model.PhysicalShockType shockType) {
        return shockType != null && shockType.isTerrestrialOnly();
    }

    /**
     * Checks if the event type requires oceanic or coastal coordinates.
     */
    public static boolean isOceanicEventType(String type) {
        return isOceanicEventType(org.ether.society.model.PhysicalShockType.fromString(type));
    }

    /**
     * Checks if the event type requires oceanic or coastal coordinates.
     */
    public static boolean isOceanicEventType(org.ether.society.model.PhysicalShockType shockType) {
        return shockType != null && shockType.isOceanicOnly();
    }

    /**
     * Executes physical, climatic, epidemiological, or infrastructural forcing across cells.
     */
    public static void executeEventForcing(
            H3SimulationEngine engine,
            String type,
            String name,
            double mag,
            double lat,
            double lng,
            double depth,
            double radiusKm,
            int durationDays) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty() || type == null) {
            return;
        }

        applyPhysicalForcingToGrid(
                engine.getCells(),
                engine.getWorldBuffer(),
                engine.getAgentBuffer(),
                engine.getNations(),
                type,
                name,
                mag,
                lat,
                lng,
                depth,
                radiusKm,
                durationDays
        );
    }

    /**
     * Applies physical forcing directly to cellular collections and DOD buffers.
     * Fully headless: zero dependency on JavaFX, GUI, or audio rendering.
     */
    public static void applyPhysicalForcingToGrid(
            List<H3Cell> cells,
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            List<Polity> nations,
            String type,
            String name,
            double mag,
            double lat,
            double lng,
            double depth,
            double radiusKm,
            int durationDays) {

        if (cells == null || cells.isEmpty() || type == null) return;

        String upperType = type.toUpperCase(Locale.ROOT);
        String lowerName = name != null ? name.toLowerCase(Locale.ROOT) : "";
        int totalCells = cells.size();

        double effectiveRadiusKm = radiusKm > 0.0 ? radiusKm : calculateDefaultRadiusKm(upperType, lowerName, mag);

        applyAtmosphericGlobalForcing(upperType, mag, durationDays);

        float[] cellSurvival = null;
        boolean anyAgentMortality = false;

        for (int i = 0; i < totalCells; i++) {
            H3Cell cell = cells.get(i);
            if (cell == null) continue;

            double cellLat = cell.getLatitudeOr(0.0);
            double cellLng = cell.getLongitudeOr(0.0);
            double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(cellLat, cellLng, lat, lng);
            double distance3dKm = Math.hypot(distKm, Math.max(0.0, depth));

            if (distance3dKm <= effectiveRadiusKm) {
                double attenuation = Math.max(0.0, 1.0 - (distance3dKm / effectiveRadiusKm));
                int prevPop = cell.getPopulationOr(0);

                int lostPop = applyCellularShock(cell, upperType, lowerName, mag, attenuation, prevPop);

                if (lostPop > 0 && prevPop > 0) {
                    int remaining = Math.max(0, prevPop - lostPop);
                    H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, remaining);
                    cell.setBiomassHuman((double) remaining);

                    if (agentBuffer != null) {
                        if (cellSurvival == null) {
                            cellSurvival = new float[totalCells];
                            Arrays.fill(cellSurvival, 1.0f);
                        }
                        cellSurvival[i] = (float) remaining / (float) prevPop;
                        anyAgentMortality = true;
                    }
                }

                if (worldBuffer != null && i < worldBuffer.getCapacity()) {
                    if (cell.getTemperature() != null) worldBuffer.getTemperature()[i] = cell.getTemperature().floatValue();
                    if (cell.getResourceCapital() != null) worldBuffer.getResourceCapital()[i] = cell.getResourceCapital().floatValue();
                    if (cell.getFoodResource() != null) worldBuffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
                    if (cell.getPopulation() != null) worldBuffer.getBiomassHuman()[i] = cell.getPopulation().floatValue();
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
     * Executes localized terraforming brush outcome across cells in headless mode.
     */
    public record HeadlessBrushOutcome(H3Cell cell, String type, String title, String description, double magnitude) {}

    public static HeadlessBrushOutcome applyTerraformBrush(List<H3Cell> cells, WorldBuffer worldBuffer, int selectedIdx, double lat, double lng) {
        H3Cell nearest = findNearestCell(cells, lat, lng);
        if (nearest == null) return null;

        String brushType;
        String brushTitle;
        String brushDesc;
        double brushMag;

        switch (selectedIdx) {
            case 0 -> {
                nearest.setPopulation(nearest.getPopulationOr(0) + 50000);
                brushType = "POP_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.pop", "👥 Boost Population (+50 000 hab)");
                brushDesc = String.format(Locale.ROOT, "Ajout de +50 000 habitants à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 50000.0;
            }
            case 1 -> {
                nearest.setFoodResource((nearest.getFoodResource() != null ? nearest.getFoodResource() : 0.0) + 500.0);
                brushType = "AGRI_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.agri", "🌾 Injection Agricole & Silos (+500 t)");
                brushDesc = String.format(Locale.ROOT, "Apport de +500 t de céréales à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 500.0;
            }
            case 2 -> {
                nearest.setFreshwaterAquifer((nearest.getFreshwaterAquifer() != null ? nearest.getFreshwaterAquifer() : 0.0) + 2000.0);
                brushType = "WATER_RECHARGE";
                brushTitle = I18n.getOrDefault("godmode.brush.water", "🚰 Recharge Aquifère (+2 000 m³)");
                brushDesc = String.format(Locale.ROOT, "Recharge de nappe phréatique (+2 000 m³) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 2000.0;
            }
            case 3 -> {
                nearest.setTemperature(nearest.getTemperatureOr(15.0) + 10.0);
                brushType = "HEAT_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.heat", "🔥 Vague de Chaleur Locale (+10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Hausse thermique locale (+10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 10.0;
            }
            case 4 -> {
                nearest.setTemperature(nearest.getTemperatureOr(15.0) - 10.0);
                brushType = "COLD_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.cold", "❄️ Refroidissement Local (-10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Refroidissement local (-10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 10.0;
            }
            default -> {
                nearest.setPollutionLevel(0.0);
                brushType = "CLEAN_ECO";
                brushTitle = I18n.getOrDefault("godmode.brush.clean", "🧼 Nettoyage Écologique Intégral");
                brushDesc = String.format(Locale.ROOT, "Dépollution intégrale à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 1.0;
            }
        }

        if (worldBuffer != null && cells != null) {
            DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        }

        return new HeadlessBrushOutcome(nearest, brushType, brushTitle, brushDesc, brushMag);
    }
}
