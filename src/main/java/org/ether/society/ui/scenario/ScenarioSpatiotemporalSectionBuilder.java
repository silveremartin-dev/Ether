/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;

import java.util.Locale;

/**
 * UI Section Builder for Epoch, Spatiotemporal resolution, and H3 hexagonal discretization.
 *
 * <p>Configures:
 * <ul>
 *   <li>Chronological timeline boundaries: $t_0 \in [-100\,000, +2\,100]$ to $t_{\text{end}}$</li>
 *   <li>H3 spatial resolution: $r \in [1, 8]$ ($41\,162$ to $2\,800\,000$ cells)</li>
 *   <li>Temporal step: $\Delta t \in [1.0, 365.0]$ days</li>
 *   <li>Agent cohort Dunbar grouping: $N_{\text{cohort}} \in [1, 50\,000]$ hab/cohort</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSpatiotemporalSectionBuilder {

    /**
     * Host context contract connecting the spatiotemporal builder to the scenario setup panel.
     */
    public interface HostContext {
        void notifyParamChange();
        void loadEarthHistoricalEvents();
        <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction);
    }

    private final HostContext host;

    private Label title1;
    private Spinner<Integer> startYearSpinner;
    private Spinner<Integer> endYearSpinner;
    private Label startYearLabel;
    private Label endYearLabel;

    private ComboBox<Integer> h3ResolutionCombo;
    private Label h3ResolutionLabel;
    private Label h3ResolutionHintLabel;

    private Spinner<Integer> targetCohortSizeSpinner;
    private Label cohortSizeLabel;
    private Label cohortSizeHintLabel;

    private ComboBox<Double> temporalResolutionCombo;
    private Label temporalResolutionLabel;
    private Label temporalResolutionHintLabel;

    private ComboBox<String> referenceModelCombo;
    private Label referenceModelLabel;
    private Label referenceModelHintLabel;

    /**
     * Constructs a spatiotemporal section builder.
     *
     * @param host callback context
     */
    public ScenarioSpatiotemporalSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the spatiotemporal definition UI section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        title1 = new Label(I18n.getOrDefault("scenario.section.spatiotemporal", "🌐 EPOCH & SPATIOTEMPORAL DEFINITION"));
        title1.getStyleClass().add("label-section-header");

        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        startYearSpinner = new Spinner<>(-3000000, 2200, -8000, 100);
        startYearSpinner.setEditable(true);
        startYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start_year", "Chronological marker at T=0 to set the simulation on a standard calendar reference. This value is for reference and does not directly affect physics equations.")));

        endYearSpinner = new Spinner<>(-3000000, 2200, 100, 100);
        endYearSpinner.setEditable(true);
        endYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.end_year", "Target end year of simulation. Determines total campaign duration for headless runs and comparative analytics.")));
        endYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (host != null) host.notifyParamChange();
        });

        startYearLabel = new Label();
        endYearLabel = new Label();
        h3ResolutionLabel = new Label();

        // H3 Resolution (Row 0)
        h3ResolutionCombo = new ComboBox<>();
        h3ResolutionCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
        h3ResolutionCombo.setValue(3);
        h3ResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        h3ResolutionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer item) {
                return item == null ? "" : I18n.getOrDefault("planet.param.resolution.res" + item, "Résolution " + item);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        Tooltip.install(h3ResolutionCombo, new Tooltip(I18n.getOrDefault("planet.tooltip.resolution", "H3 Grid Resolution")));
        Tooltip.install(h3ResolutionLabel, h3ResolutionCombo.getTooltip());
        h3ResolutionHintLabel = new Label(I18n.getOrDefault("scenario.h3.hint",
                "• Res 2–3 : Planétaire global multi-millénaire (~41k cel.) | Res 4–5 : Régional (~2.8M cel.) | Res 6+ : Micro-bassin."));
        h3ResolutionHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        h3ResolutionHintLabel.setWrapText(true);
        VBox h3Box = new VBox(2, h3ResolutionCombo, h3ResolutionHintLabel);

        // Target Cohort Size (Row 1)
        cohortSizeLabel = new Label();
        targetCohortSizeSpinner = new Spinner<>(1, 100000, 150, 25);
        targetCohortSizeSpinner.setEditable(true);
        targetCohortSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        targetCohortSizeSpinner.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cohort_size",
                "👥 Taille Cible des Cohortes Démographiques (Nœuds Agents DOD) [hab/cohorte] :\n" +
                "Détermine la taille moyenne des groupes d'habitants représentés par un même nœud agent.\n" +
                "• 1 à 30 hab/cohorte : Bandes nomades & chasseurs-cueilleurs\n" +
                "• 150 hab/cohorte : Nombre de Dunbar (Villages sédentaires & communautés de base - Recommandé)\n" +
                "• 10 000 à 50 000 hab/cohorte : Macro-cohortes, métropoles & calcul accéléré")));
        Tooltip.install(cohortSizeLabel, targetCohortSizeSpinner.getTooltip());
        targetCohortSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (host != null) host.notifyParamChange();
        });

        cohortSizeHintLabel = new Label(I18n.getOrDefault("scenario.cohort.hint",
                "• 1–30 : Micro-tribus nomades | 150 : Recommandé par défaut (Nombre de Dunbar) | 10k–50k : Macro-cohortes."));
        cohortSizeHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        cohortSizeHintLabel.setWrapText(true);
        VBox cohortBox = new VBox(2, targetCohortSizeSpinner, cohortSizeHintLabel);

        // Temporal Resolution (Row 2)
        temporalResolutionLabel = new Label();
        temporalResolutionCombo = new ComboBox<>();
        temporalResolutionCombo.getItems().addAll(1.0, 7.0, 15.0, 30.0, 60.0, 90.0, 180.0, 365.0);
        temporalResolutionCombo.setValue(30.0);
        temporalResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        temporalResolutionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double item) {
                if (item == null) return "";
                if (item == 1.0) return I18n.getOrDefault("scenario.temporal.1d", "1 day (High Precision Seasons & Epidemics)");
                if (item == 7.0) return I18n.getOrDefault("scenario.temporal.7d", "1 semaine (7 jours)");
                if (item == 15.0) return I18n.getOrDefault("scenario.temporal.15d", "15 jours");
                if (item == 30.0) return I18n.getOrDefault("scenario.temporal.30d", "1 month (~30 days) [Default - Balanced]");
                if (item == 60.0) return I18n.getOrDefault("scenario.temporal.60d", "2 mois");
                if (item == 90.0) return I18n.getOrDefault("scenario.temporal.90d", "1 trimestre (~3 mois)");
                if (item == 180.0) return I18n.getOrDefault("scenario.temporal.180d", "1 semestre (~6 mois)");
                if (item == 365.0) return I18n.getOrDefault("scenario.temporal.365d", "1 year (365 days) [Ultra-Fast Multi-Millennia]");
                return String.format(Locale.US, "%.0f %s", item, I18n.getOrDefault("scenario.temporal.days", "jours"));
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        Tooltip temporalTooltip = new Tooltip("""
            ⏱️ Résolution Temporelle de la Simulation (Pas de Temps Δt) :
            Détermine la granularité temporelle de chaque pas de calcul de la simulation.
            • Pas de temps court (< 1 mois, ex: 1 jour, 1 semaine) : Haute précision dynamique pour les épidémies, le climat saisonnier et la mobilité rapide.
            • Pas de temps standard (1 mois - par défaut) : Équilibre optimal entre la précision physique et la vitesse d'exécution.
            • Pas de temps long (> 1 mois, ex: 3 mois, 1 an) : Accélération majeure pour les simulations à très grande échelle sur plusieurs millénaires.
            """);
        temporalResolutionCombo.setTooltip(temporalTooltip);
        Tooltip.install(temporalResolutionLabel, temporalTooltip);
        temporalResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (host != null) host.notifyParamChange();
        });

        temporalResolutionHintLabel = new Label(I18n.getOrDefault("scenario.temporal.hint",
                "• 1 Mois : Recommandé par défaut (équilibre saisons/vitesse) | 1 Jour/Sem. : Épidémies & météo | 1 An : Multi-millénaire ultra-rapide."));
        temporalResolutionHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        temporalResolutionHintLabel.setWrapText(true);
        VBox tempBox = new VBox(2, temporalResolutionCombo, temporalResolutionHintLabel);

        // Start Year (Row 3)
        startYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (host != null) {
                host.notifyParamChange();
                if (newV != null) {
                    host.loadEarthHistoricalEvents();
                    updateReferenceModelOptions(newV);
                }
            }
        });
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());
        Tooltip.install(endYearLabel, endYearSpinner.getTooltip());

        // Reference Baseline Model (Row 5)
        referenceModelLabel = new Label();
        referenceModelCombo = new ComboBox<>();
        referenceModelCombo.setMaxWidth(Double.MAX_VALUE);
        referenceModelCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String item) {
                if (item == null) return "";
                return org.ether.society.analytics.HistoricalValidationKernel.getReferenceModelDisplayLabel(item);
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });
        Tooltip.install(referenceModelCombo, new Tooltip(I18n.getOrDefault("scenario.tooltip.reference_model", "Reference Model")));
        Tooltip.install(referenceModelLabel, referenceModelCombo.getTooltip());
        referenceModelHintLabel = new Label(I18n.getOrDefault("scenario.reference_model.hint",
                "• < -10k BP : Paléo Sources | -10k à 2024 : HYDE 3.4 & Maddison | 2025+ : SSP1-5 du GIEC ou Calibration Ether."));
        referenceModelHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        referenceModelHintLabel.setWrapText(true);
        referenceModelCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (host != null) host.notifyParamChange();
        });
        VBox referenceBox = new VBox(2, referenceModelCombo, referenceModelHintLabel);

        updateReferenceModelOptions(startYearSpinner.getValue());

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(190);
        col0.setPrefWidth(220);
        col0.setHgrow(Priority.NEVER);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setFillWidth(true);
        grid1.getColumnConstraints().addAll(col0, col1);

        h3ResolutionLabel.setWrapText(true);
        temporalResolutionLabel.setWrapText(true);
        cohortSizeLabel.setWrapText(true);
        startYearLabel.setWrapText(true);
        endYearLabel.setWrapText(true);
        referenceModelLabel.setWrapText(true);

        grid1.addRow(0, h3ResolutionLabel, h3Box);
        grid1.addRow(1, temporalResolutionLabel, tempBox);
        grid1.addRow(2, cohortSizeLabel, cohortBox);
        grid1.addRow(3, startYearLabel, startYearSpinner);
        grid1.addRow(4, endYearLabel, endYearSpinner);
        grid1.addRow(5, referenceModelLabel, referenceBox);

        VBox content = new VBox(10, grid1);
        VBox box = new VBox(8, title1, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    /**
     * Dynamically updates the reference model options based on the chosen start year.
     *
     * @param startYear the epoch start year
     */
    public void updateReferenceModelOptions(int startYear) {
        if (referenceModelCombo == null) return;
        String current = referenceModelCombo.getValue();
        referenceModelCombo.getItems().clear();
        if (startYear < -10000) {
            referenceModelCombo.getItems().add("PALEO_SOURCES");
            referenceModelCombo.setValue("PALEO_SOURCES");
            referenceModelCombo.setDisable(true);
        } else if (startYear < 2025) {
            referenceModelCombo.getItems().add("HYDE_MADDISON");
            referenceModelCombo.setValue("HYDE_MADDISON");
            referenceModelCombo.setDisable(true);
        } else if (startYear < 2100) {
            referenceModelCombo.getItems().addAll("SSP1", "SSP2", "SSP3", "SSP4", "SSP5", "ETHER_CALIBRATED");
            referenceModelCombo.setDisable(false);
            if (current != null && referenceModelCombo.getItems().contains(current)) {
                referenceModelCombo.setValue(current);
            } else {
                referenceModelCombo.setValue("SSP2");
            }
        } else if (startYear <= 2200) {
            // Beyond 2100, official IPCC SSP1-5 projections terminate; only Ether Calibrated continuity extends to 2200
            referenceModelCombo.getItems().add("ETHER_CALIBRATED");
            referenceModelCombo.setValue("ETHER_CALIBRATED");
            referenceModelCombo.setDisable(true);
        } else {
            referenceModelCombo.setDisable(true);
        }
    }

    /**
     * Applies spatiotemporal parameters from a scenario model into UI controls.
     *
     * @param s scenario model
     */
    public void applyScenario(Scenario s) {
        if (s == null) return;
        int startYear = (int) s.getStartDateYear();
        if (startYearSpinner != null && startYearSpinner.getValueFactory() != null) {
            startYearSpinner.getValueFactory().setValue(startYear);
        }
        if (endYearSpinner != null && endYearSpinner.getValueFactory() != null) {
            endYearSpinner.getValueFactory().setValue((int) s.getEndDateYear());
        }
        if (targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValueFactory() != null) {
            targetCohortSizeSpinner.getValueFactory().setValue(s.getTargetCohortSize() > 0 ? s.getTargetCohortSize() : 150);
        }
        if (temporalResolutionCombo != null) {
            temporalResolutionCombo.setValue(s.getTemporalResolutionDays() > 0 ? s.getTemporalResolutionDays() : 30.0);
        }
        if (h3ResolutionCombo != null) {
            h3ResolutionCombo.setValue(s.getH3Resolution() > 0 ? s.getH3Resolution() : 3);
        }
        if (referenceModelCombo != null) {
            updateReferenceModelOptions(startYear);
            String refModel = s.getReferenceTrajectoryModel();
            if (refModel != null && referenceModelCombo.getItems().contains(refModel)) {
                referenceModelCombo.setValue(refModel);
            }
        }
    }

    /**
     * Exports spatiotemporal parameters from UI controls into a scenario model.
     *
     * @param s scenario model
     */
    public void exportToScenario(Scenario s) {
        if (s == null) return;
        if (startYearSpinner != null && startYearSpinner.getValue() != null) {
            s.setStartDateYear(startYearSpinner.getValue());
        }
        if (endYearSpinner != null && endYearSpinner.getValue() != null) {
            s.setEndDateYear(endYearSpinner.getValue());
        }
        if (targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValue() != null) {
            s.setTargetCohortSize(targetCohortSizeSpinner.getValue());
        }
        if (temporalResolutionCombo != null && temporalResolutionCombo.getValue() != null) {
            s.setTemporalResolutionDays(temporalResolutionCombo.getValue());
        }
        if (h3ResolutionCombo != null && h3ResolutionCombo.getValue() != null) {
            s.setH3Resolution(h3ResolutionCombo.getValue());
        }
        if (referenceModelCombo != null && referenceModelCombo.getValue() != null) {
            s.setReferenceTrajectoryModel(referenceModelCombo.getValue());
        }
    }

    /**
     * Updates localized UI strings for spatiotemporal controls.
     */
    public void updateTexts() {
        if (title1 != null) title1.setText(I18n.getOrDefault("scenario.section.spatiotemporal", "🌐 EPOCH & SPATIOTEMPORAL DEFINITION"));
        if (startYearLabel != null) startYearLabel.setText(I18n.getOrDefault("scenario.start_year", "Start Year (Chronological Reference):"));
        if (endYearLabel != null) endYearLabel.setText(I18n.getOrDefault("scenario.end_year", "End / Target Year (Chronological Reference):"));
        if (h3ResolutionLabel != null) h3ResolutionLabel.setText(I18n.getOrDefault("planet.param.resolution", "H3 Resolution:"));
        if (h3ResolutionHintLabel != null) h3ResolutionHintLabel.setText(I18n.getOrDefault("scenario.h3.hint", "• Res 2–3 : Planétaire global (~41k cel.) | Res 4–5 : Régional (~2.8M cel.) | Res 6+ : Micro-bassin."));
        if (temporalResolutionLabel != null) temporalResolutionLabel.setText(I18n.getOrDefault("scenario.label.temporal_resolution", "Time Step Δt (Temporal Resolution):"));
        if (temporalResolutionHintLabel != null) temporalResolutionHintLabel.setText(I18n.getOrDefault("scenario.temporal.hint", "• 1 Mois : Recommandé par défaut (équilibre saisons/vitesse) | 1 Jour/Sem. : Épidémies & météo | 1 An : Multi-millénaire."));
        if (cohortSizeLabel != null) cohortSizeLabel.setText(I18n.getOrDefault("scenario.label.cohort_size", "Cohort Size:"));
        if (cohortSizeHintLabel != null) cohortSizeHintLabel.setText(I18n.getOrDefault("scenario.cohort.hint", "• 1–30 : Micro-tribus nomades | 150 : Recommandé par défaut (Nombre de Dunbar) | 10k–50k : Macro-cohortes."));
        if (referenceModelLabel != null) referenceModelLabel.setText(I18n.getOrDefault("scenario.label.reference_model", "Prospective / Empirical Baseline Model:"));
        if (referenceModelHintLabel != null) referenceModelHintLabel.setText(I18n.getOrDefault("scenario.reference_model.hint", "• < -10k BP: Paléo Sources | -10k to 2024: HYDE 3.4 & Maddison | 2025+: IPCC SSP1-5 or Ether Calibrated."));

        if (temporalResolutionCombo != null && temporalResolutionCombo.getCellFactory() != null) {
            temporalResolutionCombo.setButtonCell(temporalResolutionCombo.getCellFactory().call(null));
        }
        if (h3ResolutionCombo != null && h3ResolutionCombo.getCellFactory() != null) {
            h3ResolutionCombo.setButtonCell(h3ResolutionCombo.getCellFactory().call(null));
        }
        if (referenceModelCombo != null && referenceModelCombo.getCellFactory() != null) {
            referenceModelCombo.setButtonCell(referenceModelCombo.getCellFactory().call(null));
        }
    }

    public Spinner<Integer> getStartYearSpinner() { return startYearSpinner; }
    public Spinner<Integer> getEndYearSpinner() { return endYearSpinner; }
    public ComboBox<Integer> getH3ResolutionCombo() { return h3ResolutionCombo; }
    public Spinner<Integer> getTargetCohortSizeSpinner() { return targetCohortSizeSpinner; }
    public ComboBox<Double> getTemporalResolutionCombo() { return temporalResolutionCombo; }
    public ComboBox<String> getReferenceModelCombo() { return referenceModelCombo; }
}
