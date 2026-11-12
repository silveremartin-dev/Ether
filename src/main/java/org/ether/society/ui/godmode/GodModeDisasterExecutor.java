/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.godmode;

import org.ether.society.core.EventPhysicalImpactExecutor;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.ClimateEvent;

/**
 * UI-facing Console Facade for God Mode disaster injection, terraforming brushes, and interventions.
 *
 * <p>Strictly delegates all physical, thermodynamic, epidemiological, and demographic forcing
 * to the headless {@link EventPhysicalImpactExecutor} in the core simulation engine.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class GodModeDisasterExecutor {

    private GodModeDisasterExecutor() {
        // Utility class
    }

    /**
     * Finds the closest simulation cell to the specified latitude and longitude coordinates.
     */
    public static H3Cell findNearestCell(H3SimulationEngine engine, double lat, double lng) {
        if (engine == null || engine.getCells() == null) return null;
        return EventPhysicalImpactExecutor.findNearestCell(engine.getCells(), lat, lng);
    }

    /**
     * Checks if the event type is strictly terrestrial (cannot occur in deep ocean).
     */
    public static boolean isTerrestrialEventType(String type) {
        return EventPhysicalImpactExecutor.isTerrestrialEventType(type);
    }

    /**
     * Checks if the event type requires oceanic or coastal coordinates.
     */
    public static boolean isOceanicEventType(String type) {
        return EventPhysicalImpactExecutor.isOceanicEventType(type);
    }

    /**
     * Executes physical, climatic, or sociological forcing across spatial simulation cells.
     */
    public static void executePhysicalForcing(H3SimulationEngine engine, String type, double mag, double lat, double lng, double radiusKm) {
        if (engine == null || type == null) return;
        int defaultDuration = ClimateEvent.getDefaultDurationDaysForType(type);
        EventPhysicalImpactExecutor.executeEventForcing(engine, type, type, mag, lat, lng, 10.0, radiusKm, defaultDuration);
    }

    /**
     * Applies demographic injection at epicenter coordinates.
     */
    public static H3Cell injectPopulationAtEpicenter(H3SimulationEngine engine, double lat, double lng, int popCount) {
        H3Cell nearest = findNearestCell(engine, lat, lng);
        if (nearest != null) {
            nearest.setPopulation(nearest.getPopulationOr(0) + popCount);
            if (engine.getWorldBuffer() != null && engine.getCells() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
        return nearest;
    }

    /**
     * Injects global agricultural food stocks into all simulation cells.
     */
    public static void injectGlobalFood(H3SimulationEngine engine, double amount) {
        if (engine != null && engine.getCells() != null) {
            for (H3Cell c : engine.getCells()) {
                if (c == null) continue;
                c.setFoodResource((c.getFoodResource() != null ? c.getFoodResource() : 0.0) + amount);
            }
            if (engine.getWorldBuffer() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
    }

    /**
     * Executes cataclysmic mass extinction cutting world population by percentage.
     */
    public static void triggerMassExtinction(H3SimulationEngine engine, double fractionRemaining) {
        if (engine != null && engine.getCells() != null) {
            for (H3Cell c : engine.getCells()) {
                if (c == null) continue;
                c.setPopulation((int) (c.getPopulationOr(0) * fractionRemaining));
            }
            if (engine.getWorldBuffer() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
    }

    /**
     * Record representing a local terraform brush modification outcome.
     */
    public record BrushOutcome(H3Cell cell, String type, String title, String description, double magnitude) {}

    /**
     * Applies a localized ecological or physical terraform brush to the nearest cell.
     */
    public static BrushOutcome applyTerraformBrush(H3SimulationEngine engine, int selectedIdx, double lat, double lng) {
        if (engine == null || engine.getCells() == null) return null;
        EventPhysicalImpactExecutor.HeadlessBrushOutcome outcome = EventPhysicalImpactExecutor.applyTerraformBrush(
                engine.getCells(), engine.getWorldBuffer(), selectedIdx, lat, lng);
        if (outcome == null) return null;
        return new BrushOutcome(outcome.cell(), outcome.type(), outcome.title(), outcome.description(), outcome.magnitude());
    }
}
