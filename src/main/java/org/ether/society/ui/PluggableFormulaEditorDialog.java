package org.ether.society.ui;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

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

    private final Label headerLabel;
    private final Label lblListTitle;
    private final ListView<String> formulaListView = new ListView<>();
    private final Button btnNew;
    private final Button btnDelete;
    private final Button btnImport;
    private final Button btnExport;

    private final Label lblId = createLabel();
    private final Label lblName = createLabel();
    private final Label lblCategory = createLabel();
    private final Label lblExpression = createLabel();
    private final Label lblUnit = createLabel();
    private final Label lblDescription = createLabel();

    private final TextField txtId = new TextField();
    private final TextField txtName = new TextField();
    private final TextField txtCategory = new TextField();
    private final TextField txtExpression = new TextField();
    private final TextField txtUnit = new TextField();
    private final TextArea txtDescription = new TextArea();

    private final Button btnTest;
    private final Label lblTestResult = new Label();

    private final Label docTitle;
    private final Label docText;

    private final Button btnSave;
    private final Button btnClose;

    public PluggableFormulaEditorDialog(PluggableStatEngine statEngine, List<H3Cell> currentCells) {
        this.statEngine = statEngine;
        this.currentCells = currentCells;

        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("formula_editor.window_title", "🧮 Éditeur & Gestionnaire de Formules Statistiques Personnalisées"));

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");

        // Header Title
        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        // Main Layout: Split into Formula List (Left) and Editor/Documentation (Right)
        HBox mainSplit = new HBox(14);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // --- LEFT COLUMN: Formula List & Actions ---
        VBox leftCol = new VBox(8);
        leftCol.setPrefWidth(260);

        lblListTitle = new Label();
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

        btnNew = new Button();
        btnNew.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> clearForm());

        btnDelete = new Button();
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

        btnImport = new Button();
        btnImport.setStyle("-fx-background-color: #334155; -fx-text-fill: white;");
        btnImport.setMaxWidth(Double.MAX_VALUE);
        btnImport.setOnAction(e -> importFormulas());

        btnExport = new Button();
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

        txtDescription.setPrefRowCount(3);

        grid.addRow(0, lblId, txtId);
        grid.addRow(1, lblName, txtName);
        grid.addRow(2, lblCategory, txtCategory);
        grid.addRow(3, lblExpression, txtExpression);
        grid.addRow(4, lblUnit, txtUnit);
        grid.addRow(5, lblDescription, txtDescription);

        // Test Bar
        btnTest = new Button();
        btnTest.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnTest.setOnAction(e -> testFormula());

        lblTestResult.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        HBox testBox = new HBox(12, btnTest, lblTestResult);
        testBox.setAlignment(Pos.CENTER_LEFT);

        // Help Documentation Card
        VBox docBox = new VBox(4);
        docBox.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1);");

        docTitle = new Label();
        docTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-font-size: 11px;");

        docText = new Label();
        docText.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1; -fx-font-family: 'Consolas', monospace;");
        docText.setWrapText(true);

        docBox.getChildren().addAll(docTitle, docText);

        // Save Button Bar
        btnSave = new Button();
        btnSave.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16;");
        btnSave.setOnAction(e -> saveFormula());

        btnClose = new Button();
        btnClose.setOnAction(e -> close());

        HBox bottomBar = new HBox(10, btnSave, btnClose);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        rightCol.getChildren().addAll(grid, testBox, docBox, bottomBar);

        mainSplit.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(headerLabel, mainSplit);

        Scene scene = new Scene(root, 840, 560);
        setScene(scene);

        updateTexts();
    }

    public void updateTexts() {
        setTitle(I18n.getOrDefault("formula_editor.window_title", "🧮 Éditeur & Gestionnaire de Formules Statistiques Personnalisées"));
        headerLabel.setText(I18n.getOrDefault("formula_editor.header", "⚙️ FORMULES & STATISTIQUES PLUGGABLES (Custom Formula Engine)"));
        lblListTitle.setText(I18n.getOrDefault("formula_editor.list_title", "📋 Formules Enregistrées :"));

        btnNew.setText(I18n.getOrDefault("formula_editor.btn.new", "➕ Nouvelle Formule"));
        btnNew.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.new", "Créer une nouvelle formule personnalisée.")));
        btnDelete.setText(I18n.getOrDefault("formula_editor.btn.delete", "🗑️ Supprimer"));
        btnDelete.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.delete", "Supprimer la formule sélectionnée.")));
        btnImport.setText(I18n.getOrDefault("formula_editor.btn.import", "📥 Importer (Properties)"));
        btnImport.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.import", "Importer un fichier de propriétés de formules.")));
        btnExport.setText(I18n.getOrDefault("formula_editor.btn.export", "📤 Exporter (Properties)"));
        btnExport.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.export", "Exporter toutes les formules dans un fichier de propriétés.")));

        lblId.setText(I18n.getOrDefault("formula_editor.label.id", "Identifiant Unique (ID) :"));
        lblName.setText(I18n.getOrDefault("formula_editor.label.name", "Nom de la Statistique :"));
        lblCategory.setText(I18n.getOrDefault("formula_editor.label.category", "Catégorie :"));
        lblExpression.setText(I18n.getOrDefault("formula_editor.label.expression", "Expression / Formule :"));
        lblUnit.setText(I18n.getOrDefault("formula_editor.label.unit", "Unité de Mesure :"));
        lblDescription.setText(I18n.getOrDefault("formula_editor.label.description", "Description / Documentation :"));

        txtId.setPromptText(I18n.getOrDefault("formula_editor.prompt.id", "ex: custom_var_wealth"));
        txtName.setPromptText(I18n.getOrDefault("formula_editor.prompt.name", "ex: Ecart-Type de Richesse"));
        txtExpression.setPromptText(I18n.getOrDefault("formula_editor.prompt.expression", "ex: STDDEV(wealth) / AVG(wealth)"));
        txtUnit.setPromptText(I18n.getOrDefault("formula_editor.prompt.unit", "ex: %, t, Coeff"));
        txtDescription.setPromptText(I18n.getOrDefault("formula_editor.prompt.description", "Description mathématique et portée de la formule..."));

        btnTest.setText(I18n.getOrDefault("formula_editor.btn.test", "🔬 Évaluer & Tester la Formule"));
        btnTest.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.test", "Calculer immédiatement l'expression sur les mailles courantes.")));
        if (lblTestResult.getText().isEmpty() || lblTestResult.getText().contains("--")) {
            lblTestResult.setText(I18n.getOrDefault("formula_editor.result.default", "Résultat du Test : --"));
        }

        docTitle.setText(I18n.getOrDefault("formula_editor.doc.title", "📖 Manuel de Syntaxe & Variables Disponibles :"));
        docText.setText(I18n.getOrDefault("formula_editor.doc.content",
                "• Variables Brutes : wealth, population, food, water, temperature, rainfall, elevation, tech, age\n" +
                "• Fonctions Stat : SUM(var), AVG(var), MEDIAN(var), VAR(var), STDDEV(var), MIN(var), MAX(var), GINI(var), COUNT(var), RANGE(var)\n" +
                "• Opérateurs & Math : +, -, *, /, %, ^, SQRT(), ABS(), LOG(), EXP(), ROUND()\n" +
                "• Exemple : GINI(wealth)  |  STDDEV(food)  |  SUM(food) / COUNT(population)"));

        btnSave.setText(I18n.getOrDefault("formula_editor.btn.save", "💾 Enregistrer la Formule"));
        btnSave.setTooltip(new Tooltip(I18n.getOrDefault("formula_editor.tooltip.save", "Enregistrer la formule dans le moteur statistique actif.")));
        btnClose.setText(I18n.getOrDefault("formula_editor.btn.close", "Fermer"));

        refreshFormulaList();
    }

    private Label createLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private void refreshFormulaList() {
        if (formulaListView == null) return;
        formulaListView.getItems().clear();
        String systemBadge = I18n.getOrDefault("formula_editor.badge.system", " [SYSTÈME]");
        String customBadge = I18n.getOrDefault("formula_editor.badge.custom", " [CUSTOM]");

        for (PluggableStatEngine.StatDefinition def : statEngine.getRegisteredStats()) {
            String builtinBadge = def.isBuiltin() ? systemBadge : customBadge;
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
        lblTestResult.setText(I18n.getOrDefault("formula_editor.result.default", "Résultat du Test : --"));
    }

    private void clearForm() {
        txtId.setText("custom_" + System.currentTimeMillis() % 10000);
        txtId.setEditable(true);
        txtName.setText(I18n.getOrDefault("formula_editor.default.new_name", "Nouvelle Formule Statistique"));
        txtCategory.setText(I18n.getOrDefault("formula_editor.default.category", "💎 Économie & Richesse"));
        txtExpression.setText("STDDEV(wealth)");
        txtUnit.setText("Coeff");
        txtDescription.setText(I18n.getOrDefault("formula_editor.default.description", "Description de la statistique personnalisée..."));
        lblTestResult.setText(I18n.getOrDefault("formula_editor.result.default", "Résultat du Test : --"));
    }

    private void testFormula() {
        String expr = txtExpression.getText();
        if (expr == null || expr.isBlank()) {
            lblTestResult.setText(I18n.getOrDefault("formula_editor.error.empty", "Résultat : Erreur (Expression vide)"));
            return;
        }
        try {
            double res = statEngine.computeValue(expr, currentCells, null);
            String resultPattern = I18n.getOrDefault("formula_editor.result.success", "Résultat du Test : %.4f %s");
            lblTestResult.setText(String.format(resultPattern, res, txtUnit.getText()));
        } catch (Exception ex) {
            lblTestResult.setText(I18n.getOrDefault("formula_editor.error.prefix", "Erreur : ") + ex.getMessage());
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
            new Alert(Alert.AlertType.WARNING, I18n.getOrDefault("formula_editor.alert.missing_fields", "Veuillez remplir l'ID, le nom et l'expression de la formule.")).show();
            return;
        }

        PluggableStatEngine.StatDefinition existing = statEngine.getStat(id);
        boolean isBuiltin = existing != null && existing.isBuiltin();

        PluggableStatEngine.StatDefinition def = new PluggableStatEngine.StatDefinition(id, name, cat, expr, unit, desc, isBuiltin);
        statEngine.registerStat(def);
        refreshFormulaList();
        new Alert(Alert.AlertType.INFORMATION, String.format(I18n.getOrDefault("formula_editor.alert.saved", "Formule '%s' enregistrée avec succès !"), name)).show();
    }

    private void importFormulas() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("formula_editor.dialog.import_title", "Importer une bibliothèque de formules (.properties)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.getOrDefault("formula_editor.dialog.prop_filter", "Fichiers Properties (*.properties)"), "*.properties"));
        File file = chooser.showOpenDialog(this);
        if (file != null) {
            try {
                statEngine.importFormulasFromFile(file);
                refreshFormulaList();
                new Alert(Alert.AlertType.INFORMATION, I18n.getOrDefault("formula_editor.alert.imported", "Formules importées avec succès !")).show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, I18n.getOrDefault("formula_editor.alert.import_error", "Erreur d'importation : ") + ex.getMessage()).show();
            }
        }
    }

    private void exportFormulas() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("formula_editor.dialog.export_title", "Exporter les formules personnalisées (.properties)"));
        chooser.setInitialFileName("ether_custom_formulas.properties");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.getOrDefault("formula_editor.dialog.prop_filter", "Fichiers Properties (*.properties)"), "*.properties"));
        File file = chooser.showSaveDialog(this);
        if (file != null) {
            try {
                statEngine.exportFormulasToFile(file);
                new Alert(Alert.AlertType.INFORMATION, I18n.getOrDefault("formula_editor.alert.exported", "Formules exportées dans : ") + file.getAbsolutePath()).show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, I18n.getOrDefault("formula_editor.alert.export_error", "Erreur d'exportation : ") + ex.getMessage()).show();
            }
        }
    }
}
