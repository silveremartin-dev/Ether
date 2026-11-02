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
 * @version 1.0.0-beta.2
 */
public class TelemetryVariableSelectorDialog extends Dialog<Set<String>> {

    public record TelemetryVariableMeta(String id, String label, String category, String description) {}

    private static final Map<String, TelemetryVariableMeta> AVAILABLE_VARIABLES = new LinkedHashMap<>();

    static {
        // 👥 Démographie & Société
        register("population", "Population Humaine", "👥 Démographie", "Taille absolue du stock démographique mondial");
        register("fertilityRate", "Taux de Fécondité", "👥 Démographie", "Nombre moyen d'enfants par femme");
        register("lifeExpectancy", "Espérance de Vie", "👥 Démographie", "Espérance de vie moyenne à la naissance (années)");
        register("urbanizationRate", "Taux d'Urbanisation", "👥 Démographie", "Part de la population résidant dans des pôles urbains (%)");
        register("literacyRate", "Taux d'Alphabétisation", "👥 Démographie", "Proportion de la population sachant lire et écrire (%)");

        // 💰 Économie & Macro-Production
        register("gdpTotal", "PIB Mondial (GWP)", "💰 Économie", "Produit intérieur brut mondial agrégé (Int$ 1990)");
        register("giniIndex", "Coefficient de Gini", "💰 Économie", "Indice d'inégalité de distribution de la richesse");
        register("realUnskilledWage", "Salaire Réel Non Qualifié", "💰 Économie", "Indice du pouvoir d'achat du travailleur de subsistance");
        register("globalTradeVolume", "Volume du Commerce Global", "💰 Économie", "Intensité des échanges marchands inter-régionaux");
        register("shippingFreightCostIndex", "Coût du Fret Maritime", "💰 Économie", "Indice de coût de transport maritime au mille marin");

        // ⚡ Énergie, Métabolisme & Matière
        register("energyCaptured", "Énergie Primaire Capturée", "⚡ Énergie", "Flux d'exergie brute soutiré à la biosphère (MW)");
        register("energyPerCapita", "Énergie par Habitant", "⚡ Énergie", "Consommation primaire par individu (MJ/hab/an)");
        register("fossilSharePrimaryEnergy", "Part des Énergies Fossiles", "⚡ Énergie", "Proportion d'énergie issue du charbon et pétrole (%)");
        register("eroiAlim", "EROEI Alimentaire", "⚡ Énergie", "Rendement énergétique net de la production agricole (E_out / E_in)");
        register("metalSmeltingPerCapita", "Métallurgie par Habitant", "⚡ Énergie", "Production d'acier, fer et métaux raffinés (kg/hab)");

        // 🏛️ Institutions & Théorie Structurelle-Démographique (SDT)
        register("politicalStressIndex", "Indice de Stress Politique (PSI)", "🏛️ Institutions", "Pression de crise institutionnelle selon Turchin");
        register("eliteOverproduction", "Surproduction des Élites", "🏛️ Institutions", "Compétition intra-élite pour les postes de pouvoir");
        register("currencyDebasement", "Dépréciation Monétaire", "🏛️ Institutions", "Perte de titre métallique et dévaluation de la monnaie");
        register("asabiyyah", "Cohésion Sociale (Asabiyyah)", "🏛️ Institutions", "Capacité d'action collective selon Ibn Khaldun");
        register("sovereignDebtBurden", "Dette Souveraine", "🏛️ Institutions", "Poids de l'endettement public par rapport au produit fiscal");

        // 🌿 Environnement & Cycles Planétaires
        register("carbonFootprint", "Concentration CO2 Atmosphérique", "🌿 Environnement", "Teneur en dioxyde de carbone atmosphérique (ppm)");
        register("temperatureAnomaly", "Anomalie Thermique", "🌿 Environnement", "Écart de température par rapport à la moyenne préindustrielle (°C)");
        register("soilNPK", "Érosion & Qualité des Sols", "🌿 Environnement", "Teneur en macronutriments N-P-K et érosion du sol superficiel");
        register("forestCoverRemaining", "Couverture Forestière Restante", "🌿 Environnement", "Pourcentage de rétention forestière post-glaciaire (%)");
        register("megafaunaIndex", "Biomasse Mégafaune", "🌿 Environnement", "Survie des grands herbivores et prédateurs (>44 kg)");

        // ⚡ Réseaux & Information
        register("informationSpeed", "Vitesse de l'Information", "⚡ Réseaux", "Vitesse de propagation des dépêches et savoirs (km/jour)");
        register("systemInterdependence", "Interdépendance Systémique", "⚡ Réseaux", "Connectivité et vulnérabilité aux cascades de rupture");
    }

    private static void register(String id, String label, String category, String description) {
        AVAILABLE_VARIABLES.put(id, new TelemetryVariableMeta(id, label, category, description));
    }

    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
    private final Set<String> selectedVariableIds = new LinkedHashSet<>();

    public TelemetryVariableSelectorDialog(Set<String> currentlyActiveIds) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("telemetry.dialog.title", "⚙️ Configuration de la Télémétrie en Temps Réel"));
        setHeaderText(I18n.getOrDefault("telemetry.dialog.header", "Sélectionnez les variables à calculer et afficher en direct pendant la simulation"));

        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");

        // Toolbar: Profile selection buttons
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnSelectAll = new Button("Tout Sélectionner");
        btnSelectAll.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnSelectAll.setOnAction(e -> setAllSelection(true));

        Button btnDeselectAll = new Button("Tout Désélectionner");
        btnDeselectAll.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-size: 11px;");
        btnDeselectAll.setOnAction(e -> setAllSelection(false));

        Button btnProfileFast = new Button("Profil Rapide (5 variables)");
        btnProfileFast.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 11px;");
        btnProfileFast.setOnAction(e -> applyProfileFast());

        Button btnProfileCliodynamic = new Button("Profil Cliodynamique Turchin");
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
