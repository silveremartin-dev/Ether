/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.ether.society.events.HistoricalIntervention;
import org.ether.society.events.HistoricalInterventionRegistry;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ScenarioSetupPanel.ClimateEvent;
import org.ether.society.ui.WindowHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Autonomous UI Controller and Section Builder for Historical Planetary Events and Climate Drifts.
 *
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP).
 * Constructs the events scheduling calendar, table columns, localized event type converters,
 * and loads empirical volcanic, seismic, climatic, and historical leader interventions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ScenarioEventsSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEventsSectionBuilder.class);

    private final ObservableList<ClimateEvent> eventsList;
    private final TableView<ClimateEvent> eventsTable;
    private final Runnable onEventsChangedCallback;

    private Label title3Events;
    private CheckBox randomBiophysicalEventsCheckBox;
    private CheckBox randomPoliticalEventsCheckBox;
    private CheckBox eventsCheckBox;
    private Button addEventBtn;
    private Button removeEventBtn;
    private Button clearEventsBtn;
    private Button loadEarthEventsBtn;
    private Button loadEarthLeadersBtn;

    public ScenarioEventsSectionBuilder(ObservableList<ClimateEvent> eventsList, Runnable onEventsChangedCallback) {
        this.eventsList = eventsList != null ? eventsList : FXCollections.observableArrayList();
        this.eventsTable = new TableView<>(this.eventsList);
        this.onEventsChangedCallback = onEventsChangedCallback;
    }

    /**
     * Builds and returns the historical planetary events UI section.
     *
     * @param randomBiophysicalCB biophysical random events checkbox
     * @param randomPoliticalCB political random events checkbox
     * @param startYearSupplier supplier for current starting year
     * @return JavaFX VBox container
     */
    @SuppressWarnings("unchecked")
    public VBox buildEventsSection(CheckBox randomBiophysicalCB, CheckBox randomPoliticalCB,
                                  java.util.function.Supplier<Integer> startYearSupplier) {
        this.randomBiophysicalEventsCheckBox = randomBiophysicalCB != null ? randomBiophysicalCB :
                new CheckBox(I18n.getOrDefault("scenario.events.biophysical_enable", "ðŸŒ‹ Enable random planetary biophysical events (Volcanoes, Earthquakes, Epidemics)"));
        this.randomPoliticalEventsCheckBox = randomPoliticalCB != null ? randomPoliticalCB :
                new CheckBox(I18n.getOrDefault("scenario.events.political_enable", "ðŸ‘‘ Enable random political & sociological interventions"));

        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        title3Events = new Label();
        title3Events.getStyleClass().add("label-section-header");
        title3Events.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.events", "Schedule climate and geological events.")));

        eventsTable.setEditable(true);
        eventsTable.setPrefHeight(200);
        eventsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        List<String> eventTypes = List.of(
            "VOLCANO", "EARTHQUAKE", "TSUNAMI", "FLOOD", "DROUGHT",
            "PANDEMIC", "HEATWAVE", "ICE_AGE", "METEOR",
            "SOLAR_EMP", "NUCLEAR_STRIKE", "NUCLEAR_WINTER", "CYBER_ATTACK",
            "ECONOMIC_CRASH", "BIODIVERSITY_COLLAPSE", "GEOENGINEERING",
            "RENAISSANCE_BOOM", "TECH_SINGULARITY", "ALIEN_CONTACT",
            "DOME_BREACH", "ECLSS_FAILURE", "AEROSTAT_PUNCTURE", "LAVA_TUBE_COLLAPSE",
            "LEADER_MILITARY_CONQUEROR", "LEADER_INFRASTRUCTURE_BUILDER",
            "LEADER_INSTITUTIONAL_REFORMER", "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR",
            "LEADER_MORAL_RELIGIOUS_SAGE", "LEADER_TOTALITARIAN_PURGER",
            "milestone_archaeology", "milestone_polity", "milestone_technology",
            "milestone_golden_age", "milestone_ethnography",
            "disaster_volcano", "disaster_earthquake", "disaster_tsunami", "disaster_flood",
            "disaster_drought", "disaster_pandemic", "disaster_heatwave",
            "disaster_ice_age", "disaster_meteor", "disaster_solar_emp", "disaster_nuclear_strike",
            "disaster_nuclear_winter", "disaster_cyber_attack", "disaster_economic_crash",
            "disaster_biodiversity_collapse", "disaster_geoengineering", "disaster_alien_contact",
            "milestone", "historical"
        );

        StringConverter<String> typeConverter = createEventTypeConverter();

        TableColumn<ClimateEvent, String> colType = new TableColumn<>(I18n.getOrDefault("scenario.col.type", "Type"));
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(typeConverter, FXCollections.observableArrayList(eventTypes)));
        colType.setPrefWidth(160);

        TableColumn<ClimateEvent, String> colName = new TableColumn<>(I18n.getOrDefault("scenario.col.name", "Description"));
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setPrefWidth(220);

        TableColumn<ClimateEvent, Integer> colYear = new TableColumn<>(I18n.getOrDefault("scenario.col.year", "Year"));
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty().asObject());
        colYear.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colYear.setPrefWidth(75);

        TableColumn<ClimateEvent, Integer> colMonth = new TableColumn<>(I18n.getOrDefault("scenario.col.month", "Mois"));
        colMonth.setCellValueFactory(cellData -> cellData.getValue().monthProperty().asObject());
        colMonth.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colMonth.setPrefWidth(50);

        TableColumn<ClimateEvent, Integer> colDay = new TableColumn<>(I18n.getOrDefault("scenario.col.day", "Jour"));
        colDay.setCellValueFactory(cellData -> cellData.getValue().dayProperty().asObject());
        colDay.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colDay.setPrefWidth(50);

        StringConverter<Integer> durationConverter = new StringConverter<>() {
            @Override
            public String toString(Integer val) {
                if (val == null || val <= 0) {
                    return I18n.getOrDefault("scenario.event.duration.permanent", "Permanent (âˆž)");
                }
                return val + " " + I18n.getOrDefault("scenario.event.duration.days_suffix", "j");
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) return -1;
                String trimmed = string.trim().toLowerCase(java.util.Locale.ROOT);
                if (trimmed.contains("perm") || trimmed.contains("âˆž") || trimmed.equals("0") || trimmed.equals("-1")) {
                    return -1;
                }
                String digits = trimmed.replaceAll("[^0-9-]", "");
                try {
                    int d = Integer.parseInt(digits);
                    return d <= 0 ? -1 : d;
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
        };

        TableColumn<ClimateEvent, Integer> colDuration = new TableColumn<>(I18n.getOrDefault("scenario.col.duration", "DurÃ©e"));
        colDuration.setCellValueFactory(cellData -> cellData.getValue().durationDaysProperty().asObject());
        colDuration.setCellFactory(TextFieldTableCell.forTableColumn(durationConverter));
        colDuration.setPrefWidth(110);

        TableColumn<ClimateEvent, Double> colLat = new TableColumn<>(I18n.getOrDefault("scenario.col.lat", "Latitude"));
        colLat.setCellValueFactory(cellData -> cellData.getValue().latitudeProperty().asObject());
        colLat.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colLat.setPrefWidth(80);

        TableColumn<ClimateEvent, Double> colLon = new TableColumn<>(I18n.getOrDefault("scenario.col.lon", "Longitude"));
        colLon.setCellValueFactory(cellData -> cellData.getValue().longitudeProperty().asObject());
        colLon.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colLon.setPrefWidth(80);

        TableColumn<ClimateEvent, Double> colDepth = new TableColumn<>(I18n.getOrDefault("scenario.col.depth", "Depth (km)"));
        colDepth.setCellValueFactory(cellData -> cellData.getValue().depthProperty().asObject());
        colDepth.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colDepth.setPrefWidth(80);

        TableColumn<ClimateEvent, Double> colMag = new TableColumn<>(I18n.getOrDefault("scenario.col.mag", "Intensity (0-1)"));
        colMag.setCellValueFactory(cellData -> cellData.getValue().magnitudeProperty().asObject());
        colMag.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMag.setPrefWidth(90);

        eventsTable.getColumns().setAll(colType, colName, colYear, colMonth, colDay, colDuration, colLat, colLon, colDepth, colMag);

        addEventBtn = new Button(I18n.getOrDefault("scenario.btn.add_event", "âž• Add Event"));
        addEventBtn.getStyleClass().add("button-secondary");
        addEventBtn.setOnAction(e -> {
            int currentStartYear = startYearSupplier != null ? startYearSupplier.get() : -8000;
            eventsList.add(new ClimateEvent("VOLCANO", "Volcanic Eruption", currentStartYear + 50, 0.0, 0.0, 10.0, 0.5, 365));
            notifyChange();
        });

        removeEventBtn = new Button(I18n.getOrDefault("scenario.btn.remove_event", "âž– Delete Selection"));
        removeEventBtn.getStyleClass().add("button-secondary");
        removeEventBtn.setOnAction(e -> {
            ClimateEvent selected = eventsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                eventsList.remove(selected);
                notifyChange();
            }
        });

        clearEventsBtn = new Button(I18n.getOrDefault("scenario.btn.clear_events", "ðŸ—‘ï¸ Clear All"));
        clearEventsBtn.getStyleClass().add("button-secondary");
        clearEventsBtn.setOnAction(e -> {
            if (!eventsList.isEmpty()) {
                eventsList.clear();
                notifyChange();
            }
        });

        loadEarthEventsBtn = new Button(I18n.getOrDefault("scenario.btn.load_earth_events", "ðŸŒ‹ Load Terrestrial Disasters"));
        loadEarthEventsBtn.getStyleClass().add("button-secondary");
        loadEarthEventsBtn.setOnAction(e -> {
            int yr = startYearSupplier != null ? startYearSupplier.get() : -8000;
            loadEarthBiophysicalEvents(yr, true);
        });

        loadEarthLeadersBtn = new Button(I18n.getOrDefault("scenario.btn.load_historical_leaders", "ðŸ‘‘ Load Great Historical Leaders"));
        loadEarthLeadersBtn.getStyleClass().add("button-secondary");
        loadEarthLeadersBtn.setOnAction(e -> {
            int yr = startYearSupplier != null ? startYearSupplier.get() : -8000;
            loadHistoricalLeaderInterventions(yr, true);
        });

        Label randomEventsNote = new Label(I18n.getOrDefault("scenario.events.random_note",
                "â„¹ï¸ ModÃ¨les DÃ©terministes & AlÃ©atoires : Activez les cases ci-dessus pour laisser le moteur gÃ©nÃ©rer dynamiquement des chocs climatiques, pandÃ©miques et politiques au cours des siÃ¨cles selon les lois de Poisson et la thÃ©orie de la criticalitÃ© auto-organisÃ©e."));
        randomEventsNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        randomEventsNote.setWrapText(true);

        VBox randomEventsBox = new VBox(8, randomBiophysicalEventsCheckBox, randomPoliticalEventsCheckBox, randomEventsNote);
        randomEventsBox.setStyle("-fx-padding: 10 14; -fx-background-color: rgba(56, 189, 248, 0.06); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 6;");

        eventsCheckBox = new CheckBox(I18n.getOrDefault("scenario.events.enable", "ðŸ“… Planifier des Ã©vÃ©nements datÃ©s dans le scÃ©nario (Calendrier d'Ã©vÃ©nements)"));
        eventsCheckBox.setStyle("-fx-font-weight: bold;");

        HBox btnBarRow1 = new HBox(8, addEventBtn, removeEventBtn, clearEventsBtn);
        btnBarRow1.setAlignment(Pos.CENTER_LEFT);

        HBox btnBarRow2 = new HBox(8, loadEarthEventsBtn, loadEarthLeadersBtn);
        btnBarRow2.setAlignment(Pos.CENTER_LEFT);

        VBox btnBar = new VBox(6, btnBarRow1, btnBarRow2);

        VBox eventsSubPanel = new VBox(8, btnBar, eventsTable);
        eventsSubPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(251,146,60,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        eventsSubPanel.setVisible(false);
        eventsSubPanel.setManaged(false);

        eventsCheckBox.setOnAction(e -> {
            boolean active = eventsCheckBox.isSelected();
            eventsSubPanel.setVisible(active);
            eventsSubPanel.setManaged(active);
            notifyChange();
        });

        section.getChildren().addAll(title3Events, randomEventsBox, eventsCheckBox, eventsSubPanel);
        return section;
    }

    public void loadEarthBiophysicalEvents(int startYear, boolean promptConfirmation) {
        if (eventsList == null) return;
        boolean replaceAll = promptEventLoadConfirmation(promptConfirmation, "scenario.events.load.biophysical_title");

        List<ClimateEvent> filtered = new ArrayList<>();
        // Canonical major biophysical events with exact historical dates
        if (startYear <= -1600) filtered.add(new ClimateEvent("VOLCANO", "Ã‰ruption Minoenne de ThÃ©ra (Santorin)", -1610, 1, 1, 36.4, 25.4, 10.0, 0.95, 365));
        if (startYear <= 79)    filtered.add(new ClimateEvent("VOLCANO", "Ã‰ruption du VÃ©suve (PompÃ©i & Herculanum)", 79, 8, 24, 40.8, 14.4, 8.0, 0.85, 365));
        if (startYear <= 536)   filtered.add(new ClimateEvent("VOLCANO", "AnnÃ©e sans Ã©tÃ© (Ã‰ruption mystÃ¨re de 536)", 536, 3, 1, 64.0, -18.0, 10.0, 0.90, 365));
        if (startYear <= 1257)  filtered.add(new ClimateEvent("VOLCANO", "Ã‰ruption du Samalas (Lombok, IndonÃ©sie)", 1257, 5, 1, -8.4, 116.4, 12.0, 0.98, 365));
        if (startYear <= 1347)  filtered.add(new ClimateEvent("PANDEMIC", "Peste Noire (Yersinia Pestis)", 1347, 10, 1, 45.0, 35.0, 0.0, 0.90, 730));
        if (startYear <= 1783)  filtered.add(new ClimateEvent("VOLCANO", "Ã‰ruption du Laki (Islande)", 1783, 6, 8, 64.0, -18.2, 8.0, 0.88, 365));
        if (startYear <= 1815)  filtered.add(new ClimateEvent("VOLCANO", "Ã‰ruption du Tambora (AnnÃ©e sans Ã©tÃ©)", 1815, 4, 10, -8.2, 118.0, 10.0, 0.95, 365));
        if (startYear <= 1883)  filtered.add(new ClimateEvent("VOLCANO", "Explosion du Krakatoa (DÃ©troit de la Sonde)", 1883, 8, 27, -6.1, 105.4, 10.0, 0.90, 365));
        if (startYear <= 1918)  filtered.add(new ClimateEvent("PANDEMIC", "Grippe Espagnole (H1N1)", 1918, 3, 4, 39.0, -98.0, 0.0, 0.75, 730));

        if (replaceAll) {
            eventsList.setAll(filtered);
        } else {
            for (ClimateEvent ev : filtered) {
                boolean exists = eventsList.stream().anyMatch(e -> e.getName().equalsIgnoreCase(ev.getName()) && e.getYear() == ev.getYear());
                if (!exists) {
                    eventsList.add(ev);
                }
            }
        }
        notifyChange();
    }

    public void loadHistoricalLeaderInterventions(int startYear, boolean promptConfirmation) {
        if (eventsList == null) return;
        boolean replaceAll = promptEventLoadConfirmation(promptConfirmation, "scenario.events.load.leaders_title");

        List<ClimateEvent> filtered = new ArrayList<>();
        List<HistoricalIntervention> allLeaders = HistoricalInterventionRegistry.getInstance().getInterventions();
        for (HistoricalIntervention hi : allLeaders) {
            if (hi.getYearStart() >= startYear) {
                String type = hi.getArchetype() != null ? "LEADER_" + hi.getArchetype().name() : "LEADER_INSTITUTIONAL_REFORMER";
                String title = (hi.getArchetype() != null ? hi.getArchetype().getIcon() + " " : "") + hi.getName() + " : " + hi.getDescription();
                filtered.add(new ClimateEvent(type, title, hi.getYearStart(), hi.getLatitude(), hi.getLongitude(), 0.0, hi.getMagnitude(), hi.getDurationYears() * 365));
            }
        }

        if (replaceAll) {
            eventsList.setAll(filtered);
        } else {
            for (ClimateEvent ev : filtered) {
                boolean exists = eventsList.stream().anyMatch(e -> e.getName().equalsIgnoreCase(ev.getName()) && e.getYear() == ev.getYear());
                if (!exists) {
                    eventsList.add(ev);
                }
            }
        }
        notifyChange();
    }

    private boolean promptEventLoadConfirmation(boolean promptConfirmation, String titleKey) {
        if (!promptConfirmation || eventsList == null || eventsList.isEmpty()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.getOrDefault(titleKey, "Catalogue d'Ã‰vÃ©nements Historiques"));
        alert.setHeaderText(I18n.getOrDefault("scenario.events.load.header", "Ã‰vÃ©nements Existants DÃ©tectÃ©s"));
        alert.setContentText(I18n.getOrDefault("scenario.events.load.content", 
                "Des Ã©vÃ©nements existent dÃ©jÃ  dans votre tableau. Souhaitez-vous remplacer la liste existante ou fusionner les nouveaux Ã©vÃ©nements ?"));

        ButtonType btnReplace = new ButtonType(I18n.getOrDefault("scenario.events.load.btn_replace", "Remplacer Tout"));
        ButtonType btnMerge = new ButtonType(I18n.getOrDefault("scenario.events.load.btn_merge", "Fusionner / Ajouter"));
        ButtonType btnCancel = new ButtonType(I18n.getOrDefault("scenario.events.load.btn_cancel", "Annuler"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnReplace, btnMerge, btnCancel);
        WindowHelper.applyWindowIcon(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == btnCancel) {
            return false;
        }
        return result.get() == btnReplace;
    }

    private void notifyChange() {
        if (onEventsChangedCallback != null) {
            onEventsChangedCallback.run();
        }
    }

    private StringConverter<String> createEventTypeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(String val) {
                if (val == null) return "";
                return switch (val) {
                    case "VOLCANO", "disaster_volcano", "volcano" -> "ðŸŒ‹ " + I18n.getOrDefault("godmode.type.volcano", "Volcano");
                    case "EARTHQUAKE", "disaster_earthquake", "earthquake" -> "ðŸŒ " + I18n.getOrDefault("godmode.type.earthquake", "Earthquake");
                    case "TSUNAMI", "disaster_tsunami", "tsunami" -> "ðŸŒŠ " + I18n.getOrDefault("godmode.type.tsunami", "Tsunami");
                    case "FLOOD", "disaster_flood", "flood" -> "ðŸ’§ " + I18n.getOrDefault("godmode.type.flood", "Flood");
                    case "DROUGHT", "disaster_drought", "drought" -> "â˜€ï¸ " + I18n.getOrDefault("godmode.type.drought", "Drought");
                    case "PANDEMIC", "disaster_pandemic", "pandemic" -> "ðŸ¦  " + I18n.getOrDefault("godmode.type.pandemic", "Pandemic");
                    case "HEATWAVE", "disaster_heatwave", "heatwave" -> "ðŸŒ¡ï¸ " + I18n.getOrDefault("godmode.type.heatwave", "Heatwave");
                    case "ICE_AGE", "disaster_ice_age", "ice_age" -> "â„ï¸ " + I18n.getOrDefault("godmode.type.ice_age", "Ice Age");
                    case "METEOR", "disaster_meteor", "meteor" -> "â˜„ï¸ " + I18n.getOrDefault("godmode.type.meteor", "Meteor");
                    case "SOLAR_EMP", "disaster_solar_emp", "solar_emp" -> "âš¡ " + I18n.getOrDefault("godmode.type.solar_emp", "Solar EMP");
                    case "NUCLEAR_STRIKE", "disaster_nuclear_strike", "nuclear_strike" -> "â˜¢ï¸ " + I18n.getOrDefault("godmode.type.nuclear_strike", "Nuclear Strike");
                    case "NUCLEAR_WINTER", "disaster_nuclear_winter", "nuclear_winter" -> "â„ï¸ " + I18n.getOrDefault("godmode.type.nuclear_winter", "Nuclear Winter");
                    case "CYBER_ATTACK", "disaster_cyber_attack", "cyber_attack" -> "ðŸ’» " + I18n.getOrDefault("godmode.type.cyber_attack", "Cyber Attack");
                    case "ECONOMIC_CRASH", "disaster_economic_crash", "economic_crash" -> "ðŸ“‰ " + I18n.getOrDefault("godmode.type.economic_crash", "Economic Crash");
                    case "BIODIVERSITY_COLLAPSE", "disaster_biodiversity_collapse", "biodiversity_collapse" -> "ðŸ¦‹ " + I18n.getOrDefault("godmode.type.biodiversity_collapse", "Biodiversity Collapse");
                    case "GEOENGINEERING", "disaster_geoengineering", "geoengineering" -> "ðŸ›°ï¸ " + I18n.getOrDefault("godmode.type.geoengineering", "Geoengineering");
                    case "RENAISSANCE_BOOM", "renaissance_boom" -> "ðŸ’¡ " + I18n.getOrDefault("godmode.type.renaissance_boom", "Renaissance Boom");
                    case "TECH_SINGULARITY", "tech_singularity" -> "ðŸ”® " + I18n.getOrDefault("godmode.type.tech_singularity", "Singularity");
                    case "ALIEN_CONTACT", "disaster_alien_contact", "alien_contact" -> "ðŸ›¸ " + I18n.getOrDefault("godmode.type.alien_contact", "Alien Contact");
                    case "DOME_BREACH" -> "ðŸª " + I18n.getOrDefault("godmode.type.dome_breach", "Dome Breach");
                    case "ECLSS_FAILURE" -> "ðŸ«§ " + I18n.getOrDefault("godmode.type.eclss_failure", "ECLSS Failure");
                    case "AEROSTAT_PUNCTURE" -> "ðŸŽˆ " + I18n.getOrDefault("godmode.type.aerostat_puncture", "Aerostat Puncture");
                    case "LAVA_TUBE_COLLAPSE" -> "ðŸŒ‹ " + I18n.getOrDefault("godmode.type.lava_tube_collapse", "Lava Tube Collapse");
                    case "LEADER_MILITARY_CONQUEROR" -> "âš”ï¸ " + I18n.getOrDefault("leader.archetype.military_conqueror", "Military Conqueror");
                    case "LEADER_INFRASTRUCTURE_BUILDER" -> "ðŸ›ï¸ " + I18n.getOrDefault("leader.archetype.infrastructure_builder", "Infrastructure Builder");
                    case "LEADER_INSTITUTIONAL_REFORMER" -> "ðŸ“œ " + I18n.getOrDefault("leader.archetype.institutional_reformer", "Institutional Reformer");
                    case "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR" -> "ðŸŒ¾ " + I18n.getOrDefault("leader.archetype.hydraulic_agrarian_innovator", "Hydraulic/Agrarian Innovator");
                    case "LEADER_MORAL_RELIGIOUS_SAGE" -> "ðŸ•Šï¸ " + I18n.getOrDefault("leader.archetype.moral_religious_sage", "Moral/Religious Sage");
                    case "LEADER_TOTALITARIAN_PURGER" -> "ðŸ›¡ï¸ " + I18n.getOrDefault("leader.archetype.totalitarian_purger", "Totalitarian Purger");
                    case "milestone_archaeology", "milestone" -> "ðŸº " + I18n.getOrDefault("scenario.event.milestone_archaeology", "ArchÃ©ologie");
                    case "milestone_polity", "historical" -> "ðŸ›ï¸ " + I18n.getOrDefault("scenario.event.milestone_polity", "Ã‰tat & Empire");
                    case "milestone_technology" -> "ðŸ”¬ " + I18n.getOrDefault("scenario.event.milestone_technology", "Technologie");
                    case "milestone_golden_age" -> "ðŸŒŸ " + I18n.getOrDefault("scenario.event.milestone_golden_age", "Ã‚ge d'Or");
                    case "milestone_ethnography" -> "ðŸ—£ï¸ " + I18n.getOrDefault("scenario.event.milestone_ethnography", "Ethnographie");
                    default -> val;
                };
            }

            @Override
            public String fromString(String str) {
                return str;
            }
        };
    }

    public TableView<ClimateEvent> getEventsTable() {
        return eventsTable;
    }

    public ObservableList<ClimateEvent> getEventsList() {
        return eventsList;
    }

    public CheckBox getEventsCheckBox() {
        return eventsCheckBox;
    }

    public CheckBox getRandomBiophysicalEventsCheckBox() {
        return randomBiophysicalEventsCheckBox;
    }

    public CheckBox getRandomPoliticalEventsCheckBox() {
        return randomPoliticalEventsCheckBox;
    }
}

