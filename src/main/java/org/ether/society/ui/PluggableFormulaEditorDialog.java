/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.database.H3Cell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Interactive Dialog & Editor for Pluggable Statistics & Formulas.
 * Allows users to write custom mathematical/statistical formulas (SUM, AVG, MEDIAN, VAR, STDDEV, GINI),
 * test them live against active simulation data, and import/export formula libraries.
 *
 * @author Silvere Martin-Michiellot
 */
public class PluggableFormulaEditorDialog extends Stage {

    private final PluggableStatEngine statEngine;
    private final List<H3Cell> currentCells;

    private final ListView<String> formulaListView = new ListView<>();
    private final TextField txtId = new TextField();
    private final TextField txtName = new TextField();
    private final TextField txtCategory = new TextField("💎 Économie & Richesse");
    private final TextField txtExpression = new TextField();
    private final TextField txtUnit = new TextField("Coeff");
    private final TextArea txtDescription = new TextArea();

    private final Label lblTestResult = new Label("Résultat du Test : --");

    public PluggableFormulaEditorDialog(PluggableStatEngine statEngine, List<H3Cell> currentCells) {
        this.statEngine = statEngine;
        this.currentCells = currentCells;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("🧮 Éditeur & Gestionnaire de Formules Statistiques Personnalisées");

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");

        // Header Title
        Label headerLabel = new Label("⚙️ FORMULES & STATISTIQUES PLUGGABLES (Custom Formula Engine)");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        // Main Layout: Split into Formula List (Left) and Editor/Documentation (Right)
        HBox mainSplit = new HBox(14);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // --- LEFT COLUMN: Formula List & Actions ---
        VBox leftCol = new VBox(8);
        leftCol.setPrefWidth(260);

        Label lblListTitle = new Label("📋 Formules Enregistrées :");
        lblListTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        formulaListView.setPrefHeight(320);
        formulaListView.setStyle("-fx-control-inner-background: #1e293b; -fx-font-size: 11px;");
        refreshFormulaList();

        formulaListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String id = extractId(newV);
                PluggableStatEngine.StatDefinition def = statEngine.getStat(id);
                if (def != null) {
                    populateFields(def);
                }
            }
        });

        Button btnNew = new Button("➕ Nouvelle Formule");
        btnNew.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> clearForm());

        Button btnDelete = new Button("🗑️ Supprimer");
        btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setOnAction(e -> {
            String sel = formulaListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String id = extractId(sel);
                statEngine.unregisterStat(id);
                refreshFormulaList();
                clearForm();
            }
        });

        Button btnImport = new Button("📥 Importer (Properties)");
        btnImport.setStyle("-fx-background-color: #334155; -fx-text-fill: white;");
        btnImport.setMaxWidth(Double.MAX_VALUE);
        btnImport.setOnAction(e -> importFormulas());

        Button btnExport = new Button("📤 Exporter (Properties)");
        btnExport.setStyle("-fx-background-color: #334155; -fx-text-fill: white;");
        btnExport.setMaxWidth(Double.MAX_VALUE);
        btnExport.setOnAction(e -> exportFormulas());

        leftCol.getChildren().addAll(lblListTitle, formulaListView, btnNew, btnDelete, new Separator(), btnImport, btnExport);

        // --- RIGHT COLUMN: Editor Form & Dynamic Documentation ---
        VBox rightCol = new VBox(10);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        txtId.setPromptText("ex: custom_var_wealth");
        txtName.setPromptText("ex: Ecart-Type de Richesse");
        txtExpression.setPromptText("ex: STDDEV(wealth) / AVG(wealth)");
        txtUnit.setPromptText("ex: %, t, Coeff");
        txtDescription.setPrefRowCount(3);
        txtDescription.setPromptText("Description mathématique et portée de la formule...");

        grid.addRow(0, createLabel("Identifiant Unique (ID) :"), txtId);
        grid.addRow(1, createLabel("Nom de la Statistique :"), txtName);
        grid.addRow(2, createLabel("Catégorie :"), txtCategory);
        grid.addRow(3, createLabel("Expression / Formule :"), txtExpression);
        grid.addRow(4, createLabel("Unité de Mesure :"), txtUnit);
        grid.addRow(5, createLabel("Description / Documentation :"), txtDescription);

        // Test Bar
        Button btnTest = new Button("🔬 Évaluer & Tester la Formule");
        btnTest.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnTest.setOnAction(e -> testFormula());

        lblTestResult.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        HBox testBox = new HBox(12, btnTest, lblTestResult);
        testBox.setAlignment(Pos.CENTER_LEFT);

        // Help Documentation Card
        VBox docBox = new VBox(4);
        docBox.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1);");

        Label docTitle = new Label("📖 Manuel de Syntaxe & Variables Disponibles :");
        docTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-font-size: 11px;");

        Label docText = new Label(
                "• Variables Brutes : wealth, population, food, water, temperature, rainfall, elevation, tech, age\n" +
                "• Fonctions Stat : SUM(var), AVG(var), MEDIAN(var), VAR(var), STDDEV(var), MIN(var), MAX(var), GINI(var), COUNT(var), RANGE(var)\n" +
                "• Opérateurs & Math : +, -, *, /, %, ^, SQRT(), ABS(), LOG(), EXP(), ROUND()\n" +
                "• Exemple : GINI(wealth)  |  STDDEV(food)  |  SUM(food) / COUNT(population)"
        );
        docText.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1; -fx-font-family: 'Consolas', monospace;");
        docText.setWrapText(true);

        docBox.getChildren().addAll(docTitle, docText);

        // Save Button Bar
        Button btnSave = new Button("💾 Enregistrer la Formule");
        btnSave.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16;");
        btnSave.setOnAction(e -> saveFormula());

        Button btnClose = new Button("Fermer");
        btnClose.setOnAction(e -> close());

        HBox bottomBar = new HBox(10, btnSave, btnClose);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        rightCol.getChildren().addAll(grid, testBox, docBox, bottomBar);

        mainSplit.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(headerLabel, mainSplit);

        Scene scene = new Scene(root, 840, 560);
        setScene(scene);
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private void refreshFormulaList() {
        formulaListView.getItems().clear();
        for (PluggableStatEngine.StatDefinition def : statEngine.getRegisteredStats()) {
            String builtinBadge = def.isBuiltin() ? " [SYSTÈME]" : " [CUSTOM]";
            formulaListView.getItems().add(def.getName() + builtinBadge + " (" + def.getId() + ")");
        }
    }

    private String extractId(String item) {
        int idxStart = item.lastIndexOf('(');
        int idxEnd = item.lastIndexOf(')');
        if (idxStart >= 0 && idxEnd > idxStart) {
            return item.substring(idxStart + 1, idxEnd);
        }
        return item;
    }

    private void populateFields(PluggableStatEngine.StatDefinition def) {
        txtId.setText(def.getId());
        txtId.setEditable(!def.isBuiltin());
        txtName.setText(def.getName());
        txtCategory.setText(def.getCategory());
        txtExpression.setText(def.getExpression());
        txtUnit.setText(def.getUnit());
        txtDescription.setText(def.getDescription());
        lblTestResult.setText("Résultat du Test : --");
    }

    private void clearForm() {
        txtId.setText("custom_" + System.currentTimeMillis() % 10000);
        txtId.setEditable(true);
        txtName.setText("Nouvelle Formule Statistique");
        txtCategory.setText("💎 Économie & Richesse");
        txtExpression.setText("STDDEV(wealth)");
        txtUnit.setText("Coeff");
        txtDescription.setText("Description de la statistique personnalisée...");
        lblTestResult.setText("Résultat du Test : --");
    }

    private void testFormula() {
        String expr = txtExpression.getText();
        if (expr == null || expr.isBlank()) {
            lblTestResult.setText("Résultat : Erreur (Expression vide)");
            return;
        }
        try {
            double res = statEngine.computeValue(expr, currentCells, null);
            lblTestResult.setText(String.format("Résultat du Test : %.4f %s", res, txtUnit.getText()));
        } catch (Exception ex) {
            lblTestResult.setText("Erreur : " + ex.getMessage());
        }
    }

    private void saveFormula() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String cat = txtCategory.getText().trim();
        String expr = txtExpression.getText().trim();
        String unit = txtUnit.getText().trim();
        String desc = txtDescription.getText().trim();

        if (id.isEmpty() || name.isEmpty() || expr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir l'ID, le nom et l'expression de la formule.").show();
            return;
        }

        PluggableStatEngine.StatDefinition existing = statEngine.getStat(id);
        boolean isBuiltin = existing != null && existing.isBuiltin();

        PluggableStatEngine.StatDefinition def = new PluggableStatEngine.StatDefinition(id, name, cat, expr, unit, desc, isBuiltin);
        statEngine.registerStat(def);
        refreshFormulaList();
        new Alert(Alert.AlertType.INFORMATION, "Formule '" + name + "' enregistrée avec succès !").show();
    }

    private void importFormulas() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer une bibliothèque de formules (.properties)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Properties (*.properties)", "*.properties"));
        File file = chooser.showOpenDialog(this);
        if (file != null) {
            try {
                statEngine.importFormulasFromFile(file);
                refreshFormulaList();
                new Alert(Alert.AlertType.INFORMATION, "Formules importées avec succès !").show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur d'importation : " + ex.getMessage()).show();
            }
        }
    }

    private void exportFormulas() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les formules personnalisées (.properties)");
        chooser.setInitialFileName("ether_custom_formulas.properties");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Properties (*.properties)", "*.properties"));
        File file = chooser.showSaveDialog(this);
        if (file != null) {
            try {
                statEngine.exportFormulasToFile(file);
                new Alert(Alert.AlertType.INFORMATION, "Formules exportées dans : " + file.getAbsolutePath()).show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur d'exportation : " + ex.getMessage()).show();
            }
        }
    }
}
