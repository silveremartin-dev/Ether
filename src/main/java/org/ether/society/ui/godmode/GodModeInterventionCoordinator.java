/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.godmode;

import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.events.ActiveEvent;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.WindowHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Coordinates physical event validation, immediate forcing execution, chronological scheduling,
 * and audit trail list view synchronization for {@link org.ether.society.ui.GodModePanel}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class GodModeInterventionCoordinator {

    /** Diagnostic logger for God Mode forcing interventions. */
    private static final Logger logger = LoggerFactory.getLogger(GodModeInterventionCoordinator.class);

    /**
     * Private constructor preventing instantiation of utility coordinator class.
     */
    private GodModeInterventionCoordinator() {
        // Utility coordinator class
    }

    /**
     * Validates geographical compatibility, logs the event in the scenario timeline,
     * and optionally executes immediate physical forcing on the H3 simulation mesh.
     *
     * @param immediate {@code true} to execute immediately at current simulation year
     * @param item selected event catalog item
     * @param rawEventName user-entered event title
     * @param spinnerTargetYear target year selected in spinner
     * @param lat epicenter latitude in degrees \([-90, +90]\)
     * @param lng epicenter longitude in degrees \([-180, +180]\)
     * @param mag perturbation magnitude
     * @param radiusKm spatial action radius in kilometers
     * @param durationDays active perturbation duration in days
     * @param engine active H3 simulation engine
     * @param timeline scenario chronological timeline
     * @param mapCanvas cartographic H3 canvas
     * @param onTimelineUpdated callback refreshing the UI timeline list
     */
    public static void scheduleOrExecuteEvent(
            boolean immediate,
            GodModeEventRegistry.EventTypeItem item,
            String rawEventName,
            int spinnerTargetYear,
            int spinnerTargetMonth,
            int spinnerTargetDay,
            double lat,
            double lng,
            double mag,
            double radiusKm,
            int durationDays,
            H3SimulationEngine engine,
            ScenarioTimeline timeline,
            H3MapCanvas mapCanvas,
            Runnable onTimelineUpdated) {
        String type = item != null ? item.id() : "VOLCANO";
        String name = (rawEventName == null || rawEventName.isBlank())
                ? (item != null ? item.getDefaultTitle() : type)
                : rawEventName;

        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        int targetYear = immediate ? currentYear : spinnerTargetYear;
        int targetMonth = immediate ? currentMonth : Math.max(0, Math.min(11, spinnerTargetMonth - 1));
        int targetDay = immediate ? currentDay : Math.max(1, Math.min(31, spinnerTargetDay));

        if (!validateGeographyCompatibility(engine, type, name, lat, lng)) {
            return;
        }

        String daysUnit = durationDays > 1 ? "jours" : "jour";
        String details = String.format(Locale.ROOT, "Date: %d-%02d-%02d, Lat: %.2fÂ°, Lng: %.2fÂ°, Mag: %.1f, R: %.0fkm, Durée: %d %s",
                targetYear, targetMonth + 1, targetDay, lat, lng, mag, radiusKm, durationDays, daysUnit);

        timeline.addEntry(targetYear, type, name, details, true, lat, lng, mag, durationDays);
        if (onTimelineUpdated != null) onTimelineUpdated.run();

        if (engine != null && engine.getEventSystem() != null) {
            ActiveEvent ae = new ActiveEvent(
                    "GM_" + System.currentTimeMillis(),
                    "âš¡ GOD MODE: " + name,
                    type, lat, lng, targetYear, targetMonth, targetDay,
                    Math.max(15.0, durationDays * 0.5), mag, durationDays
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        if (immediate || (targetYear == currentYear && targetMonth == currentMonth && targetDay <= currentDay) || targetYear < currentYear) {
            GodModeDisasterExecutor.executePhysicalForcing(engine, type, mag, lat, lng, radiusKm);
            if (engine != null && engine.getWorldBuffer() != null && engine.getCells() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
                if (engine.getHistoryManager() != null) {
                    engine.getHistoryManager().captureSnapshot(engine);
                    engine.getHistoryManager().captureWorldSnapshot(engine);
                }
            }
            logger.info("God Mode intervention EXECUTED immediately (Date {}-{:02d}-{:02d}): {} - {}",
                    currentYear, currentMonth + 1, currentDay, name, details);
            if (mapCanvas != null) {
                mapCanvas.flyTo(lat, lng);
            }
        } else {
            if (engine != null && engine.getCurrentScenario() != null) {
                if (engine.getCurrentScenario().getClimateEvents() == null) {
                    engine.getCurrentScenario().setClimateEvents(new java.util.ArrayList<>());
                }
                engine.getCurrentScenario().getClimateEvents().add(new org.ether.society.model.ClimateEvent(
                        type, name, targetYear, targetMonth + 1, targetDay, lat, lng, 10.0, mag, durationDays));
            }
            logger.info("God Mode intervention SCHEDULED for Date {}-{:02d}-{:02d}: {} - {}",
                    targetYear, targetMonth + 1, targetDay, name, details);
        }
    }

    /**
     * Backward-compatible scheduleOrExecuteEvent defaulting to month 1 and day 1.
     */
    public static void scheduleOrExecuteEvent(
            boolean immediate,
            GodModeEventRegistry.EventTypeItem item,
            String rawEventName,
            int spinnerTargetYear,
            double lat,
            double lng,
            double mag,
            double radiusKm,
            int durationDays,
            H3SimulationEngine engine,
            ScenarioTimeline timeline,
            H3MapCanvas mapCanvas,
            Runnable onTimelineUpdated) {
        scheduleOrExecuteEvent(immediate, item, rawEventName, spinnerTargetYear, 1, 1, lat, lng, mag, radiusKm, durationDays, engine, timeline, mapCanvas, onTimelineUpdated);
    }

    /**
     * Checks whether the chosen event type is compatible with the oceanic/terrestrial biome at the epicenter.
     *
     * @param engine simulation engine
     * @param type canonical event identifier
     * @param name localized event name
     * @param lat epicenter latitude
     * @param lng epicenter longitude
     * @return {@code true} if geographically compatible, {@code false} if incompatible (after displaying alert)
     */
    private static boolean validateGeographyCompatibility(
            H3SimulationEngine engine, String type, String name, double lat, double lng) {
        H3Cell nearestCell = GodModeDisasterExecutor.findNearestCell(engine, lat, lng);
        if (nearestCell == null) return true;

        boolean isOcean = nearestCell.getBiome() == Biome.OCEAN || nearestCell.getBiome() == Biome.DEEP_OCEAN;
        if (GodModeDisasterExecutor.isTerrestrialEventType(type) && isOcean) {
            String warnMsg = String.format(Locale.ROOT,
                    "âš ï¸ Incompatibilité Géographique :\nL'événement '%s' (%s) ne peut pas se produire en plein océan (Lat: %.2fÂ°, Lng: %.2fÂ°).",
                    name, type, lat, lng);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("godmode.incompatible.title", "Emplacement Incompatible"));
            alert.setHeaderText(I18n.getOrDefault("godmode.incompatible.header", "âš ï¸ Événement incompatible avec le milieu océanique"));
            alert.setContentText(warnMsg + "\n\n" + I18n.getOrDefault("godmode.incompatible.advice", "Veuillez sélectionner des coordonnées situées sur la terre ferme."));
            WindowHelper.applyWindowIcon(alert);
            alert.showAndWait();
            return false;
        }
        if (GodModeDisasterExecutor.isOceanicEventType(type) && !isOcean
                && nearestCell.getElevation() != null && nearestCell.getElevation() > 300) {
            String warnMsg = String.format(Locale.ROOT,
                    "âš ï¸ Incompatibilité Géographique :\nL'événement '%s' (%s) nécessite une zone maritime ou côtière (Lat: %.2fÂ°, Lng: %.2fÂ°).",
                    name, type, lat, lng);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("godmode.incompatible.title", "Emplacement Incompatible"));
            alert.setHeaderText(I18n.getOrDefault("godmode.incompatible.header", "âš ï¸ Événement incompatible avec les hautes terres"));
            alert.setContentText(warnMsg + "\n\n" + I18n.getOrDefault("godmode.incompatible.advice_ocean", "Veuillez sélectionner des coordonnées maritimes ou côtières."));
            WindowHelper.applyWindowIcon(alert);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    /**
     * Records a direct God Mode intervention into the world buffer, history snapshots, and timeline audit log.
     *
     * @param engine simulation engine
     * @param timeline scenario timeline
     * @param type canonical event identifier
     * @param title human-readable title
     * @param details descriptive details
     * @param lat epicenter latitude
     * @param lng epicenter longitude
     * @param mag magnitude
     * @param durationDays duration in days
     * @param onTimelineUpdated callback refreshing the UI timeline list
     */
    public static void recordIntervention(
            H3SimulationEngine engine,
            ScenarioTimeline timeline,
            String type,
            String title,
            String details,
            double lat,
            double lng,
            double mag,
            int durationDays,
            Runnable onTimelineUpdated) {
        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;

        if (engine != null && engine.getWorldBuffer() != null && engine.getCells() != null) {
            DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
        }
        if (engine != null && engine.getHistoryManager() != null) {
            engine.getHistoryManager().captureSnapshot(engine);
            engine.getHistoryManager().captureWorldSnapshot(engine);
        }

        timeline.addEntry(currentYear, type, title, details, true, lat, lng, mag, durationDays);
        if (onTimelineUpdated != null) onTimelineUpdated.run();

        if (engine != null && engine.getEventSystem() != null) {
            ActiveEvent ae = new ActiveEvent(
                    "GM_" + System.currentTimeMillis(),
                    "âš¡ GOD MODE: " + title,
                    type, lat, lng, currentYear, currentMonth, currentDay,
                    Math.max(15.0, durationDays * 0.5), mag, durationDays
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        logger.info("God Mode intervention recorded at Year {}: {} - {}", currentYear, title, details);
    }

    /**
     * Configures color-coded temporal cell rendering and camera fly-to navigation on timeline selection.
     *
     * @param timelineListView audit trail {@link ListView}
     * @param timeline scenario timeline model
     * @param engine simulation engine
     * @param onCoordinatesSelected callback receiving {@code (lat, lng)} when an entry is clicked
     */
    public static void configureTimelineListView(
            ListView<String> timelineListView,
            ScenarioTimeline timeline,
            H3SimulationEngine engine,
            BiConsumer<Double, Double> onCoordinatesSelected) {
        timelineListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
                    int eventYear = currentYear;
                    try {
                        if (item.contains("|")) {
                            String yearPart = item.substring(0, item.indexOf('|')).replaceAll("[^0-9-]", "").trim();
                            if (!yearPart.isEmpty()) {
                                eventYear = Integer.parseInt(yearPart);
                            }
                        }
                    } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(GodModeInterventionCoordinator.class).debug("Non-fatal condition in GodModeInterventionCoordinator: {}", ignored.getMessage()); }

                    if (eventYear < currentYear) {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-cursor: hand;");
                    } else if (eventYear == currentYear) {
                        setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-style: italic; -fx-font-size: 11px; -fx-cursor: hand;");
                    }
                }
            }
        });

        timelineListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx == null || newIdx.intValue() < 0) return;
            List<ScenarioTimeline.TimelineEntry> entries = timeline.getEntries();
            if (newIdx.intValue() < entries.size()) {
                ScenarioTimeline.TimelineEntry entry = entries.get(newIdx.intValue());
                double lat = entry.latitude();
                double lng = entry.longitude();

                if (lat == 0.0 && lng == 0.0 && entry.details() != null) {
                    try {
                        String d = entry.details();
                        if (d.contains("Lat:") && d.contains("Lng:")) {
                            int latStart = d.indexOf("Lat:") + 4;
                            int latEnd = d.indexOf("Â°", latStart);
                            int lngStart = d.indexOf("Lng:") + 4;
                            int lngEnd = d.indexOf("Â°", lngStart);
                            if (latStart > 3 && latEnd > latStart && lngStart > 3 && lngEnd > lngStart) {
                                lat = Double.parseDouble(d.substring(latStart, latEnd).trim());
                                lng = Double.parseDouble(d.substring(lngStart, lngEnd).trim());
                            }
                        }
                    } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(GodModeInterventionCoordinator.class).debug("Non-fatal condition in GodModeInterventionCoordinator: {}", ignored.getMessage()); }
                }

                if (onCoordinatesSelected != null) {
                    onCoordinatesSelected.accept(lat, lng);
                }
            }
        });
    }

    /**
     * Repopulates the timeline audit log {@link ListView} with localized badges.
     *
     * @param timelineListView target list view
     * @param timeline scenario timeline model
     */
    public static void refreshTimelineView(ListView<String> timelineListView, ScenarioTimeline timeline) {
        if (timelineListView == null) return;
        int selected = timelineListView.getSelectionModel().getSelectedIndex();
        timelineListView.getItems().clear();
        String yearPrefix = I18n.getOrDefault("godmode.timeline.year_prefix", "Year");
        String godBadge = I18n.getOrDefault("godmode.timeline.godmode_badge", "âš¡ [GOD MODE]");
        String histBadge = I18n.getOrDefault("godmode.timeline.historical_badge", " [HISTORICAL]");
        for (ScenarioTimeline.TimelineEntry entry : timeline.getEntries()) {
            String badge = entry.isGodModeIntervention() ? godBadge : histBadge;
            timelineListView.getItems().add(String.format("%s %5d | %s %s : %s",
                    yearPrefix, entry.year(), badge, entry.title(), entry.details()));
        }
        if (selected >= 0 && selected < timelineListView.getItems().size()) {
            timelineListView.getSelectionModel().select(selected);
        }
    }
}


