/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.control;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.ether.society.events.ActiveEvent;
import org.ether.society.i18n.I18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Recent historical and planetary events telemetry card for {@link org.ether.society.ui.ControlPanel}.
 *
 * <p>Extracted from {@code ControlPanel} to satisfy the Single Responsibility Principle (SRP).
 * Displays a chronological feed of recent planetary events, natural disasters, historical figures,
 * and procedural emergence chronicles with category filtering and interactive click-to-ping navigation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ControlPanelEventsSection {

    private final VBox card;
    private final Label eventsTitleLabel;
    private final Label eventsHintLabel;
    private final CheckBox filterGeophysicalCheck;
    private final CheckBox filterHistoricalCheck;
    private final CheckBox filterChronicleCheck;
    private final Label noEventsLabel;
    private final VBox eventsListBox;

    private final List<ActiveEvent> rawEventsCache = new ArrayList<>();
    private final List<String> lastRenderedEventSignatures = new ArrayList<>();
    private Consumer<ActiveEvent> onEventSelected;

    /**
     * Initializes the recent events section card and filter controls.
     */
    public ControlPanelEventsSection() {
        eventsTitleLabel = createCardTitle(I18n.getOrDefault("sim.card.recent_events", "📜 5. ÉVÉNEMENTS RÉCENTS (CHRONOLOGIE)"));
        eventsHintLabel = new Label(I18n.getOrDefault("control.events.double_click_hint", "💡 Double-cliquez sur un événement pour centrer la caméra et afficher l'épicentre."));
        eventsHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        eventsHintLabel.setWrapText(true);

        filterGeophysicalCheck = new CheckBox(I18n.getOrDefault("sim.filter.geophysical", "🌋 Événements géophysiques & catastrophes"));
        filterGeophysicalCheck.setSelected(true);
        filterGeophysicalCheck.setWrapText(true);
        filterGeophysicalCheck.setMaxWidth(Double.MAX_VALUE);
        filterGeophysicalCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_geophysical", "Afficher ou masquer les séismes, volcans, tsunamis, inondations, sécheresses et désastres naturels.")));
        filterGeophysicalCheck.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f87171; -fx-cursor: hand;");
        filterGeophysicalCheck.setOnAction(e -> applyEventsFilter());

        filterHistoricalCheck = new CheckBox(I18n.getOrDefault("sim.filter.historical_leaders", "👑 Personnages & figures historiques"));
        filterHistoricalCheck.setSelected(true);
        filterHistoricalCheck.setWrapText(true);
        filterHistoricalCheck.setMaxWidth(Double.MAX_VALUE);
        filterHistoricalCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_historical", "Afficher ou masquer les figures historiques, dirigeants, conquérants et réformateurs.")));
        filterHistoricalCheck.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-cursor: hand;");
        filterHistoricalCheck.setOnAction(e -> applyEventsFilter());

        filterChronicleCheck = new CheckBox(I18n.getOrDefault("sim.filter.chronicle_emergences", "📜 Chroniques, systèmes & émergences (Informatif)"));
        filterChronicleCheck.setSelected(true);
        filterChronicleCheck.setWrapText(true);
        filterChronicleCheck.setMaxWidth(Double.MAX_VALUE);
        filterChronicleCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_chronicle", "Afficher ou masquer les émergences procédurales (purement informatives / résultat de la simulation).")));
        filterChronicleCheck.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #34d399; -fx-cursor: hand;");
        filterChronicleCheck.setOnAction(e -> applyEventsFilter());

        VBox filtersBox = new VBox(4, filterGeophysicalCheck, filterHistoricalCheck, filterChronicleCheck);
        filtersBox.setFillWidth(true);
        filtersBox.setAlignment(Pos.CENTER_LEFT);

        noEventsLabel = new Label(I18n.getOrDefault("sim.events.no_events", "Aucun événement enregistré pour le moment."));
        noEventsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 4 0;");

        eventsListBox = new VBox(6);
        eventsListBox.getChildren().add(noEventsLabel);

        card = new VBox(8, eventsTitleLabel, eventsHintLabel, filtersBox, eventsListBox);
        card.getStyleClass().add("card-section");
    }

    /**
     * Sets callback invoked when user clicks an event item to navigate camera.
     *
     * @param onEventSelected callback accepting active event
     */
    public void setOnEventSelected(Consumer<ActiveEvent> onEventSelected) {
        this.onEventSelected = onEventSelected;
    }

    /**
     * Gets the JavaFX card container node for Section 5.
     *
     * @return events card VBox
     */
    public VBox getCard() {
        return card;
    }

    /**
     * Re-applies category filters and refreshes the event list display.
     */
    public void applyEventsFilter() {
        lastRenderedEventSignatures.clear();
        updateRecentEvents(rawEventsCache);
    }

    /**
     * Updates the recent events list from the simulation event history.
     *
     * @param events list of active events
     */
    public void updateRecentEvents(List<ActiveEvent> events) {
        if (eventsListBox == null) return;
        if (events != null) {
            rawEventsCache.clear();
            rawEventsCache.addAll(events);
        }

        boolean showGeo = filterGeophysicalCheck == null || filterGeophysicalCheck.isSelected();
        boolean showHist = filterHistoricalCheck == null || filterHistoricalCheck.isSelected();
        boolean showChronicle = filterChronicleCheck == null || filterChronicleCheck.isSelected();

        List<ActiveEvent> filteredList = new ArrayList<>();
        for (ActiveEvent evt : rawEventsCache) {
            if (evt.isGeophysical() && !showGeo) continue;
            if (evt.isHistoricalLeader() && !showHist) continue;
            if (evt.isProceduralEmergence() && !showChronicle) continue;
            filteredList.add(evt);
        }

        List<ActiveEvent> sortedList = new ArrayList<>(filteredList);
        sortedList.sort(Comparator.comparingInt(ActiveEvent::getYear)
                .thenComparingInt(ActiveEvent::getMonth)
                .thenComparingInt(ActiveEvent::getDay));

        if (sortedList.size() > 10) {
            sortedList = sortedList.subList(sortedList.size() - 10, sortedList.size());
        }

        List<String> currentSignatures = sortedList.stream()
                .map(e -> e.getId() + "@" + e.getYear() + ":" + e.getMonth() + ":" + e.getDay() + "#" + e.getMagnitude())
                .toList();

        if (currentSignatures.equals(lastRenderedEventSignatures) && !eventsListBox.getChildren().isEmpty()) {
            return;
        }

        lastRenderedEventSignatures.clear();
        lastRenderedEventSignatures.addAll(currentSignatures);

        eventsListBox.getChildren().clear();
        if (sortedList.isEmpty()) {
            eventsListBox.getChildren().add(noEventsLabel);
            return;
        }

        for (ActiveEvent evt : sortedList) {
            VBox itemCard = createEventItemNode(evt);
            eventsListBox.getChildren().add(itemCard);
        }
    }

    private VBox createEventItemNode(ActiveEvent evt) {
        VBox item = new VBox(4);
        String normalStyle = "-fx-background-color: rgba(30, 41, 59, 0.75); -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 8; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: rgba(51, 65, 85, 0.90); -fx-border-color: #38bdf8; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 8; -fx-cursor: hand;";
        item.setStyle(normalStyle);

        item.setOnMouseEntered(e -> item.setStyle(hoverStyle));
        item.setOnMouseExited(e -> item.setStyle(normalStyle));

        Label titleLabel = new Label(evt.getTitle());
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 11px;");
        titleLabel.setMaxWidth(260);
        titleLabel.setEllipsisString("...");

        Label dateLabel = new Label(evt.getFormattedDate());
        dateLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 10px;");

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        HBox topRow = new HBox(6, titleLabel, spacerTop, dateLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String magColor = evt.getIntensityBadgeColor();
        Label magLabel = new Label(String.format(java.util.Locale.ROOT, "⚡ Mag: %.1f (%s)", evt.getMagnitude(), evt.getIntensityLabel()));
        magLabel.setStyle("-fx-text-fill: " + magColor + "; -fx-font-weight: bold; -fx-font-size: 10px;");

        Label coordLabel = new Label("📍 " + evt.getFormattedCoordinates());
        coordLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);
        HBox bottomRow = new HBox(6, magLabel, spacerBottom, coordLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        item.getChildren().addAll(topRow, bottomRow);

        Tooltip tooltip = new Tooltip(String.format(
            "%s\n\n📅 Date : %s\n⚡ Magnitude : %.1f (%s)\n📍 Coordonnées : %s\n\n💡 Cliquer pour centrer la vue 3D / 2D sur cet événement.",
            evt.getTitle(), evt.getFormattedDate(), evt.getMagnitude(), evt.getIntensityLabel(), evt.getFormattedCoordinates()
        ));
        tooltip.setShowDelay(javafx.util.Duration.millis(150));
        Tooltip.install(item, tooltip);

        item.setOnMouseClicked(e -> {
            if (onEventSelected != null) {
                onEventSelected.accept(evt);
            }
        });

        return item;
    }

    /**
     * Updates all localized UI texts across the events card.
     */
    public void updateTexts() {
        if (eventsTitleLabel != null) {
            eventsTitleLabel.setText(I18n.getOrDefault("sim.card.recent_events", "📜 5. RECENT EVENTS (CHRONOLOGY)"));
        }
        if (eventsHintLabel != null) {
            eventsHintLabel.setText(I18n.getOrDefault("sim.events.double_click_hint", "💡 Double-click an event to fly camera to its epicenter location."));
        }
        if (filterGeophysicalCheck != null) {
            filterGeophysicalCheck.setText(I18n.getOrDefault("sim.filter.geophysical", "🌋 Événements géophysiques & catastrophes"));
            filterGeophysicalCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_geophysical", "Show or hide earthquakes, volcanoes, tsunamis, floods, droughts, and natural disasters.")));
        }
        if (filterHistoricalCheck != null) {
            filterHistoricalCheck.setText(I18n.getOrDefault("sim.filter.historical_leaders", "👑 Personnages & figures historiques"));
            filterHistoricalCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_historical", "Show or hide historical figures, leaders, conquerors, and reformers.")));
        }
        if (filterChronicleCheck != null) {
            filterChronicleCheck.setText(I18n.getOrDefault("sim.filter.chronicle_emergences", "📜 Chroniques, systèmes & émergences (Informatif)"));
            filterChronicleCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.filter_chronicle", "Show or hide procedural emergences (purely informative telemetry / simulation outcomes).")));
        }
        if (noEventsLabel != null) {
            noEventsLabel.setText(I18n.getOrDefault("sim.events.no_events", "No events recorded yet."));
        }
    }

    private static Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }
}
