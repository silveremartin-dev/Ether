/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.ether.society.i18n.I18n;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * <h1>Telemetry Variable Selector Dialog</h1>
 * <p>
 * Autonomous GUI dialog that couples to the main simulation telemetry pipeline.<br>
 * Allows users to choose exactly which macroscopic and cliodynamic variables are
 * actively computed and rendered in real-time, skipping calculation of unselected
 * variables to maximize simulation frame rate and CPU efficiency.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class TelemetryVariableSelectorDialog extends Dialog<Set<String>> {

    public record TelemetryVariableMeta(String id, String label, String category, String description) {}

    private static final Map<String, TelemetryVariableMeta> AVAILABLE_VARIABLES = new LinkedHashMap<>();

    static {
        // ðŸ‘¥ DÃ©mographie & SociÃ©tÃ©
        register("population", "Population Humaine", "ðŸ‘¥ DÃ©mographie", "Taille absolue du stock dÃ©mographique mondial");
        register("fertilityRate", "Taux de FÃ©conditÃ©", "ðŸ‘¥ DÃ©mographie", "Nombre moyen d'enfants par femme");
        register("lifeExpectancy", "EspÃ©rance de Vie", "ðŸ‘¥ DÃ©mographie", "EspÃ©rance de vie moyenne Ã  la naissance (annÃ©es)");
        register("urbanizationRate", "Taux d'Urbanisation", "ðŸ‘¥ DÃ©mographie", "Part de la population rÃ©sidant dans des pÃ´les urbains (%)");
        register("literacyRate", "Taux d'AlphabÃ©tisation", "ðŸ‘¥ DÃ©mographie", "Proportion de la population sachant lire et Ã©crire (%)");

        // ðŸ’° Ã‰conomie & Macro-Production
        register("gdpTotal", "PIB Mondial (GWP)", "ðŸ’° Ã‰conomie", "Produit intÃ©rieur brut mondial agrÃ©gÃ© (Int$ 1990)");
        register("giniIndex", "Coefficient de Gini", "ðŸ’° Ã‰conomie", "Indice d'inÃ©galitÃ© de distribution de la richesse");
        register("realUnskilledWage", "Salaire RÃ©el Non QualifiÃ©", "ðŸ’° Ã‰conomie", "Indice du pouvoir d'achat du travailleur de subsistance");
        register("globalTradeVolume", "Volume du Commerce Global", "ðŸ’° Ã‰conomie", "IntensitÃ© des Ã©changes marchands inter-rÃ©gionaux");
        register("shippingFreightCostIndex", "CoÃ»t du Fret Maritime", "ðŸ’° Ã‰conomie", "Indice de coÃ»t de transport maritime au mille marin");

        // aš¡ Ã‰nergie, MÃ©tabolisme & MatiÃ¨re
        register("energyCaptured", "Ã‰nergie Primaire CapturÃ©e", "âš¡ Ã‰nergie", "Flux d'exergie brute soutirÃ© Ã  la biosphÃ¨re (MW)");
        register("energyPerCapita", "Ã‰nergie par Habitant", "âš¡ Ã‰nergie", "Consommation primaire par individu (MJ/hab/an)");
        register("fossilSharePrimaryEnergy", "Part des Ã‰nergies Fossiles", "âš¡ Ã‰nergie", "Proportion d'Ã©nergie issue du charbon et pÃ©trole (%)");
        register("eroiAlim", "EROEI Alimentaire", "âš¡ Ã‰nergie", "Rendement Ã©nergÃ©tique net de la production agricole (E_out / E_in)");
        register("metalSmeltingPerCapita", "MÃ©tallurgie par Habitant", "âš¡ Ã‰nergie", "Production d'acier, fer et mÃ©taux raffinÃ©s (kg/hab)");

        // ðŸ›i¸ Institutions & ThÃ©orie Structurelle-DÃ©mographique (SDT)
        register("politicalStressIndex", "Indice de Stress Politique (PSI)", "ðŸ›ï¸ Institutions", "Pression de crise institutionnelle selon Turchin");
        register("eliteOverproduction", "Surproduction des Ã‰lites", "ðŸ›ï¸ Institutions", "CompÃ©tition intra-Ã©lite pour les postes de pouvoir");
        register("currencyDebasement", "DÃ©prÃ©ciation MonÃ©taire", "ðŸ›ï¸ Institutions", "Perte de titre mÃ©tallique et dÃ©valuation de la monnaie");
        register("asabiyyah", "CohÃ©sion Sociale (Asabiyyah)", "ðŸ›ï¸ Institutions", "CapacitÃ© d'action collective selon Ibn Khaldun");
        register("sovereignDebtBurden", "Dette Souveraine", "ðŸ›ï¸ Institutions", "Poids de l'endettement public par rapport au produit fiscal");

        // ðŸŒ¿ Environnement & Cycles PlanÃ©taires
        register("carbonFootprint", "Concentration CO2 AtmosphÃ©rique", "ðŸŒ¿ Environnement", "Teneur en dioxyde de carbone atmosphÃ©rique (ppm)");
        register("temperatureAnomaly", "Anomalie Thermique", "ðŸŒ¿ Environnement", "Ã‰cart de tempÃ©rature par rapport Ã  la moyenne prÃ©industrielle (Â°C)");
        register("soilNPK", "Ã‰rosion & QualitÃ© des Sols", "ðŸŒ¿ Environnement", "Teneur en macronutriments N-P-K et Ã©rosion du sol superficiel");
        register("forestCoverRemaining", "Couverture ForestiÃ¨re Restante", "ðŸŒ¿ Environnement", "Pourcentage de rÃ©tention forestiÃ¨re post-glaciaire (%)");
        register("megafaunaIndex", "Biomasse MÃ©gafaune", "ðŸŒ¿ Environnement", "Survie des grands herbivores et prÃ©dateurs (>44 kg)");

        // aš¡ RÃ©seaux & Information
        register("informationSpeed", "Vitesse de l'Information", "âš¡ RÃ©seaux", "Vitesse de propagation des dÃ©pÃªches et savoirs (km/jour)");
        register("systemInterdependence", "InterdÃ©pendance SystÃ©mique", "âš¡ RÃ©seaux", "ConnectivitÃ© et vulnÃ©rabilitÃ© aux cascades de rupture");

        // ðŸ—ºi¸ Cartographie Spatiale 2D & Matrices Culturelles
        register("spatialMap2D", "Rasters Spatiaux 2D (Champs Physiques)", "ðŸ—ºï¸ Cartographie & Matrices", "Extraction et rendu 2D des champs de densitÃ©, capital et biomasse");
        register("culturalMatrix", "Matrice Culturelle (N Ã— N AffinitÃ©s)", "ðŸ—ºï¸ Cartographie & Matrices", "Calcul des affinitÃ©s ethnolinguistiques, dÃ©rive culturelle et polarisation");
    }

    private static void register(String id, String label, String category, String description) {
        AVAILABLE_VARIABLES.put(id, new TelemetryVariableMeta(id, label, category, description));
    }

    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
    private final Set<String> selectedVariableIds = new LinkedHashSet<>();

    public TelemetryVariableSelectorDialog(Set<String> currentlyActiveIds) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("telemetry.dialog.title", "âš™ï¸ Configuration de la TÃ©lÃ©mÃ©trie en Temps RÃ©el"));
        setHeaderText(I18n.getOrDefault("telemetry.dialog.header", "SÃ©lectionnez les variables Ã  calculer et afficher en direct pendant la simulation"));

        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");

        // Toolbar: Profile selection buttons
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnSelectAll = new Button(I18n.getOrDefault("telemetry.dialog.btn_select_all", "Tout Sélectionner"));
        btnSelectAll.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnSelectAll.setOnAction(e -> setAllSelection(true));

        Button btnDeselectAll = new Button(I18n.getOrDefault("telemetry.dialog.btn_deselect_all", "Tout Désélectionner"));
        btnDeselectAll.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-size: 11px;");
        btnDeselectAll.setOnAction(e -> setAllSelection(false));

        Button btnProfileFast = new Button(I18n.getOrDefault("telemetry.dialog.btn_profile_fast", "Profil Rapide (5 variables)"));
        btnProfileFast.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 11px;");
        btnProfileFast.setOnAction(e -> applyProfileFast());

        Button btnProfileCliodynamic = new Button(I18n.getOrDefault("telemetry.dialog.btn_profile_cliodynamic", "Profil Cliodynamique Turchin"));
        btnProfileCliodynamic.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 11px;");
        btnProfileCliodynamic.setOnAction(e -> applyProfileCliodynamic());

        toolbar.getChildren().addAll(btnSelectAll, btnDeselectAll, btnProfileFast, btnProfileCliodynamic);

        // Group variables by category
        Map<String, VBox> categorySections = new LinkedHashMap<>();
        for (TelemetryVariableMeta meta : AVAILABLE_VARIABLES.values()) {
            categorySections.computeIfAbsent(meta.category(), cat -> {
                VBox box = new VBox(6);
                box.setPadding(new Insets(8));
                box.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(71, 85, 105, 0.5); -fx-border-radius: 6;");
                Label catLabel = new Label(cat);
                catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8;");
                box.getChildren().add(catLabel);
                return box;
            });

            CheckBox cb = new CheckBox(meta.label() + " (" + meta.description() + ")");
            cb.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
            boolean isActive = currentlyActiveIds == null || currentlyActiveIds.isEmpty() || currentlyActiveIds.contains(meta.id());
            cb.setSelected(isActive);
            checkBoxes.put(meta.id(), cb);
            categorySections.get(meta.category()).getChildren().add(cb);
        }

        VBox categoriesContainer = new VBox(10);
        categoriesContainer.getChildren().addAll(categorySections.values());

        ScrollPane scrollPane = new ScrollPane(categoriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(450);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        contentBox.getChildren().addAll(toolbar, scrollPane);
        getDialogPane().setContent(contentBox);
        getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        setResultConverter(btnType -> {
            if (btnType == javafx.scene.control.ButtonType.OK) {
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

    public static Set<String> getDefaultActiveVariableIds() {
        return Collections.unmodifiableSet(AVAILABLE_VARIABLES.keySet());
    }
}
