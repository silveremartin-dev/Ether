package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.ScenarioBranchingTree;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI Panel for Scenario Multiverse Branching & Comparative Trajectory Analysis.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class ScenarioBranchingPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioBranchingPanel.class);

    private final H3SimulationEngine engine;
    private final ScenarioBranchingTree branchingTree;

    private final Label headerLabel;
    private final ListView<String> branchListView;
    private final TextField newBranchNameField;
    private final Button createBranchBtn;
    private final Label infoLabel;

    public ScenarioBranchingPanel(H3SimulationEngine engine, ScenarioBranchingTree tree) {
        this.engine = engine;
        this.branchingTree = tree != null ? tree : new ScenarioBranchingTree();

        setPadding(new Insets(15));
        setSpacing(10);
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: #a78bfa; -fx-border-radius: 8; -fx-background-radius: 8;");

        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #a78bfa;");

        newBranchNameField = new TextField();

        createBranchBtn = new Button();
        createBranchBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white;");
        createBranchBtn.setOnAction(e -> forkCurrentTrajectory());

        HBox forkBox = new HBox(8, newBranchNameField, createBranchBtn);
        forkBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(newBranchNameField, Priority.ALWAYS);

        branchListView = new ListView<>();
        branchListView.setPrefHeight(140);
        branchListView.setStyle("-fx-control-inner-background: #090d16; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        infoLabel = new Label();
        infoLabel.setStyle("-fx-text-fill: #94a3b8;");

        getChildren().addAll(headerLabel, forkBox, infoLabel, branchListView);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void updateTexts() {
        headerLabel.setText(I18n.getOrDefault("branching.title", "🔀 MULTIVERS & EMBRANCHEMENTS DE TRAJECTOIRES (BRANCHING)"));
        newBranchNameField.setPromptText(I18n.getOrDefault("branching.prompt.name", "Nom du nouveau brin (ex: Branche Fusion 2040)..."));
        newBranchNameField.setTooltip(new Tooltip(I18n.getOrDefault("branching.tooltip.name", "Entrez un nom identifiant pour cette trajectoire de scénario bifurquée.")));
        createBranchBtn.setText(I18n.getOrDefault("branching.btn.fork", "➕ Forker la Trajectoire Actuelle"));
        createBranchBtn.setTooltip(new Tooltip(I18n.getOrDefault("branching.tooltip.fork", "Crée un nouvel embranchement indépendant à partir de l'état actuel de la planète.")));
        infoLabel.setText(I18n.getOrDefault("branching.info.select", "Sélectionnez une branche pour comparer la télémétrie."));

        refreshBranchList();
    }

    private void forkCurrentTrajectory() {
        String name = newBranchNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            name = I18n.getOrDefault("branching.default_name", "Branche ") + (branchingTree.getBranches().size() + 1);
        }

        long currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        var cells = engine != null ? engine.getCells() : null;

        ScenarioBranchingTree.SimulationBranch newBranch = branchingTree.createBranch(name, currentYear, cells);
        newBranchNameField.clear();
        refreshBranchList();
        logger.info("Forked trajectory into branch: {} (Year {})", newBranch.getName(), currentYear);
    }

    public void refreshBranchList() {
        if (branchListView == null) return;
        branchListView.getItems().clear();
        String activeTagText = I18n.getOrDefault("branching.tag.active", " ⭐ [ACTIVE]");
        String formatPattern = I18n.getOrDefault("branching.format.branch", "%s (Branchement Année %d)%s");

        for (var b : branchingTree.getBranches().values()) {
            String activeTag = b.getId().equals(branchingTree.getActiveBranchId()) ? activeTagText : "";
            branchListView.getItems().add(String.format(formatPattern, b.getName(), b.getParentBranchYear(), activeTag));
        }
    }
}
