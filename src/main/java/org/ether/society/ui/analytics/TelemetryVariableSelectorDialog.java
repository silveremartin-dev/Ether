/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.Theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * <h1>Telemetry Variable Selector Dialog</h1>
 * <p>
 * Modal configuration dialog coupled to the real-time simulation telemetry DAG.
 * Enables users to select which biophysical, macroeconomic, and cliodynamic observables
 * are evaluated and plotted at each tick, skipping unselected branches to maximize throughput.
 * Fully localized across all 5 supported languages with technical tooltips and high-contrast theming.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class TelemetryVariableSelectorDialog extends Dialog<Set<String>> {

    /**
     * Metadata descriptor for a selectable telemetry variable using localization keys.
     *
     * @param id             canonical variable identifier
     * @param labelKey       i18n key for variable display label
     * @param categoryKey    i18n key for macro-domain category header
     * @param descriptionKey i18n key for technical tooltip and description
     */
    public record TelemetryVariableMeta(String id, String labelKey, String categoryKey, String descriptionKey) {}

    private static final Map<String, TelemetryVariableMeta> AVAILABLE_VARIABLES = new LinkedHashMap<>();

    static {
        // Demography & Society
        register("population", "telemetry.dialog.var.population", "telemetry.dialog.cat.demography", "telemetry.dialog.desc.population");
        register("fertilityRate", "telemetry.dialog.var.fertilityRate", "telemetry.dialog.cat.demography", "telemetry.dialog.desc.fertilityRate");
        register("lifeExpectancy", "telemetry.dialog.var.lifeExpectancy", "telemetry.dialog.cat.demography", "telemetry.dialog.desc.lifeExpectancy");
        register("urbanizationRate", "telemetry.dialog.var.urbanizationRate", "telemetry.dialog.cat.demography", "telemetry.dialog.desc.urbanizationRate");
        register("literacyRate", "telemetry.dialog.var.literacyRate", "telemetry.dialog.cat.demography", "telemetry.dialog.desc.literacyRate");

        // Economy & Macro-Production
        register("gdpTotal", "telemetry.dialog.var.gdpTotal", "telemetry.dialog.cat.economy", "telemetry.dialog.desc.gdpTotal");
        register("giniIndex", "telemetry.dialog.var.giniIndex", "telemetry.dialog.cat.economy", "telemetry.dialog.desc.giniIndex");
        register("realUnskilledWage", "telemetry.dialog.var.realUnskilledWage", "telemetry.dialog.cat.economy", "telemetry.dialog.desc.realUnskilledWage");
        register("globalTradeVolume", "telemetry.dialog.var.globalTradeVolume", "telemetry.dialog.cat.economy", "telemetry.dialog.desc.globalTradeVolume");
        register("shippingFreightCostIndex", "telemetry.dialog.var.shippingFreightCostIndex", "telemetry.dialog.cat.economy", "telemetry.dialog.desc.shippingFreightCostIndex");

        // Energy, Metabolism & Matter
        register("energyCaptured", "telemetry.dialog.var.energyCaptured", "telemetry.dialog.cat.energy", "telemetry.dialog.desc.energyCaptured");
        register("energyPerCapita", "telemetry.dialog.var.energyPerCapita", "telemetry.dialog.cat.energy", "telemetry.dialog.desc.energyPerCapita");
        register("fossilSharePrimaryEnergy", "telemetry.dialog.var.fossilSharePrimaryEnergy", "telemetry.dialog.cat.energy", "telemetry.dialog.desc.fossilSharePrimaryEnergy");
        register("eroiAlim", "telemetry.dialog.var.eroiAlim", "telemetry.dialog.cat.energy", "telemetry.dialog.desc.eroiAlim");
        register("metalSmeltingPerCapita", "telemetry.dialog.var.metalSmeltingPerCapita", "telemetry.dialog.cat.energy", "telemetry.dialog.desc.metalSmeltingPerCapita");

        // Institutions & Structural-Demographic Theory (SDT)
        register("politicalStressIndex", "telemetry.dialog.var.politicalStressIndex", "telemetry.dialog.cat.institutions", "telemetry.dialog.desc.politicalStressIndex");
        register("eliteOverproduction", "telemetry.dialog.var.eliteOverproduction", "telemetry.dialog.cat.institutions", "telemetry.dialog.desc.eliteOverproduction");
        register("currencyDebasement", "telemetry.dialog.var.currencyDebasement", "telemetry.dialog.cat.institutions", "telemetry.dialog.desc.currencyDebasement");
        register("asabiyyah", "telemetry.dialog.var.asabiyyah", "telemetry.dialog.cat.institutions", "telemetry.dialog.desc.asabiyyah");
        register("sovereignDebtBurden", "telemetry.dialog.var.sovereignDebtBurden", "telemetry.dialog.cat.institutions", "telemetry.dialog.desc.sovereignDebtBurden");

        // Environment & Planetary Cycles
        register("carbonFootprint", "telemetry.dialog.var.carbonFootprint", "telemetry.dialog.cat.environment", "telemetry.dialog.desc.carbonFootprint");
        register("temperatureAnomaly", "telemetry.dialog.var.temperatureAnomaly", "telemetry.dialog.cat.environment", "telemetry.dialog.desc.temperatureAnomaly");
        register("soilNPK", "telemetry.dialog.var.soilNPK", "telemetry.dialog.cat.environment", "telemetry.dialog.desc.soilNPK");
        register("forestCoverRemaining", "telemetry.dialog.var.forestCoverRemaining", "telemetry.dialog.cat.environment", "telemetry.dialog.desc.forestCoverRemaining");
        register("megafaunaIndex", "telemetry.dialog.var.megafaunaIndex", "telemetry.dialog.cat.environment", "telemetry.dialog.desc.megafaunaIndex");

        // Networks & Information
        register("informationSpeed", "telemetry.dialog.var.informationSpeed", "telemetry.dialog.cat.networks", "telemetry.dialog.desc.informationSpeed");
        register("systemInterdependence", "telemetry.dialog.var.systemInterdependence", "telemetry.dialog.cat.networks", "telemetry.dialog.desc.systemInterdependence");

        // 2D Spatial Cartography & Cultural Matrices
        register("spatialMap2D", "telemetry.dialog.var.spatialMap2D", "telemetry.dialog.cat.cartography", "telemetry.dialog.desc.spatialMap2D");
        register("culturalMatrix", "telemetry.dialog.var.culturalMatrix", "telemetry.dialog.cat.cartography", "telemetry.dialog.desc.culturalMatrix");
    }

    private static void register(String id, String labelKey, String categoryKey, String descriptionKey) {
        AVAILABLE_VARIABLES.put(id, new TelemetryVariableMeta(id, labelKey, categoryKey, descriptionKey));
    }

    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();

    /**
     * Constructs a localized telemetry variable selector dialog.
     *
     * @param currentlyActiveIds set of currently active telemetry variable identifiers
     */
    public TelemetryVariableSelectorDialog(Set<String> currentlyActiveIds) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.get("telemetry.dialog.title"));
        setHeaderText(I18n.get("telemetry.dialog.header"));

        boolean isLight = Theme.getCurrentTheme() == Theme.LIGHT;
        String bgMain = isLight ? "#f8fafc" : "#0f172a";
        String textMain = isLight ? "#0f172a" : "#e2e8f0";
        String cardBg = isLight ? "#ffffff" : "rgba(30, 41, 59, 0.85)";
        String cardBorder = isLight ? "#cbd5e1" : "rgba(71, 85, 105, 0.65)";
        String catColor = isLight ? "#0369a1" : "#38bdf8";

        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: " + bgMain + ";");

        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnSelectAll = new Button(I18n.get("telemetry.dialog.btn_select_all"));
        btnSelectAll.setTooltip(new Tooltip(I18n.get("telemetry.dialog.btn_select_all.tooltip")));
        btnSelectAll.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnSelectAll.setOnAction(e -> setAllSelection(true));

        Button btnDeselectAll = new Button(I18n.get("telemetry.dialog.btn_deselect_all"));
        btnDeselectAll.setTooltip(new Tooltip(I18n.get("telemetry.dialog.btn_deselect_all.tooltip")));
        btnDeselectAll.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 11px;");
        btnDeselectAll.setOnAction(e -> setAllSelection(false));

        Button btnProfileFast = new Button(I18n.get("telemetry.dialog.btn_profile_fast"));
        btnProfileFast.setTooltip(new Tooltip(I18n.get("telemetry.dialog.btn_profile_fast.tooltip")));
        btnProfileFast.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 11px;");
        btnProfileFast.setOnAction(e -> applyProfileFast());

        Button btnProfileCliodynamic = new Button(I18n.get("telemetry.dialog.btn_profile_cliodynamic"));
        btnProfileCliodynamic.setTooltip(new Tooltip(I18n.get("telemetry.dialog.btn_profile_cliodynamic.tooltip")));
        btnProfileCliodynamic.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 11px;");
        btnProfileCliodynamic.setOnAction(e -> applyProfileCliodynamic());

        toolbar.getChildren().addAll(btnSelectAll, btnDeselectAll, btnProfileFast, btnProfileCliodynamic);

        Map<String, VBox> categorySections = new LinkedHashMap<>();
        for (TelemetryVariableMeta meta : AVAILABLE_VARIABLES.values()) {
            categorySections.computeIfAbsent(meta.categoryKey(), catKey -> {
                VBox box = new VBox(6);
                box.setPadding(new Insets(8));
                box.setStyle("-fx-background-color: " + cardBg + "; -fx-background-radius: 6; -fx-border-color: " + cardBorder + "; -fx-border-radius: 6;");
                Label catLabel = new Label(I18n.get(catKey));
                catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + catColor + ";");
                box.getChildren().add(catLabel);
                return box;
            });

            String localizedLabel = I18n.get(meta.labelKey());
            String localizedDesc = I18n.get(meta.descriptionKey());
            CheckBox cb = new CheckBox(localizedLabel + " — " + localizedDesc);
            cb.setTooltip(new Tooltip(localizedDesc));
            cb.setStyle("-fx-text-fill: " + textMain + "; -fx-font-size: 11px;");
            boolean isActive = currentlyActiveIds == null || currentlyActiveIds.isEmpty() || currentlyActiveIds.contains(meta.id());
            cb.setSelected(isActive);
            checkBoxes.put(meta.id(), cb);
            categorySections.get(meta.categoryKey()).getChildren().add(cb);
        }

        VBox categoriesContainer = new VBox(10);
        categoriesContainer.getChildren().addAll(categorySections.values());

        ScrollPane scrollPane = new ScrollPane(categoriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(450);
        scrollPane.setStyle("-fx-background: " + bgMain + "; -fx-background-color: transparent;");

        contentBox.getChildren().addAll(toolbar, scrollPane);
        getDialogPane().setContent(contentBox);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btnType -> {
            if (btnType == ButtonType.OK) {
                Set<String> selected = new LinkedHashSet<>();
                for (var entry : checkBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selected.add(entry.getKey());
                    }
                }
                return selected;
            }
            return null;
        });
    }

    private void setAllSelection(boolean select) {
        for (CheckBox cb : checkBoxes.values()) {
            cb.setSelected(select);
        }
    }

    private void applyProfileFast() {
        setAllSelection(false);
        selectIfExists("population");
        selectIfExists("gdpTotal");
        selectIfExists("energyCaptured");
        selectIfExists("urbanizationRate");
        selectIfExists("asabiyyah");
    }

    private void applyProfileCliodynamic() {
        setAllSelection(false);
        selectIfExists("population");
        selectIfExists("politicalStressIndex");
        selectIfExists("eliteOverproduction");
        selectIfExists("currencyDebasement");
        selectIfExists("asabiyyah");
        selectIfExists("realUnskilledWage");
        selectIfExists("giniIndex");
        selectIfExists("sovereignDebtBurden");
    }

    private void selectIfExists(String id) {
        CheckBox cb = checkBoxes.get(id);
        if (cb != null) cb.setSelected(true);
    }

    /**
     * Returns the immutable set of all default active telemetry variable identifiers.
     *
     * @return unmodifiable set of telemetry variable IDs
     */
    public static Set<String> getDefaultActiveVariableIds() {
        return Collections.unmodifiableSet(AVAILABLE_VARIABLES.keySet());
    }
}
