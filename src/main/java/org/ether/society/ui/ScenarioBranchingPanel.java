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
 * @version 1.0.0-beta.1
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
        getStyleClass().add("branching-panel");

        headerLabel = new Label();
        headerLabel.getStyleClass().add("sidebar-title");

        newBranchNameField = new TextField();

        createBranchBtn = new Button();
        createBranchBtn.getStyleClass().add("button");
        createBranchBtn.setOnAction(e -> forkCurrentTrajectory());

        HBox forkBox = new HBox(8, newBranchNameField, createBranchBtn);
        forkBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(newBranchNameField, Priority.ALWAYS);

        branchListView = new ListView<>();
        branchListView.setPrefHeight(140);
        branchListView.getStyleClass().add("branching-list-view");

        infoLabel = new Label();
        infoLabel.getStyleClass().add("hint-label");

        getChildren().addAll(headerLabel, forkBox, infoLabel, branchListView);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void updateTexts() {
        headerLabel.setText(I18n.getOrDefault("branching.title", "🔀 MULTIVERS & EMBRANCHEMENTS DE TRAJECTOIRES (BRANCHING)"));
        newBranchNameField.setPromptText(I18n.getOrDefault("branching.prompt.name", "Nom du nouveau brin (ex: Branche Fusion 2040)..."));
        newBranchNameField.setTooltip(new Tooltip(I18n.getOrDefault("branching.tooltip.name", "Enter an identifying name for this bifurcated scenario trajectory.")));
        createBranchBtn.setText(I18n.getOrDefault("branching.btn.fork", "➕ Forker la Trajectoire Actuelle"));
        createBranchBtn.setTooltip(new Tooltip(I18n.getOrDefault("branching.tooltip.fork", "Creates a new independent branch from current planet state.")));
        infoLabel.setText(I18n.getOrDefault("branching.info.select", "Select a branch to compare telemetry."));

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
        String formatPattern = I18n.getOrDefault("branching.format.branch", "%s (Branch Year %d)%s");

        for (var b : branchingTree.getBranches().values()) {
            String activeTag = b.getId().equals(branchingTree.getActiveBranchId()) ? activeTagText : "";
            branchListView.getItems().add(String.format(formatPattern, b.getName(), b.getParentBranchYear(), activeTag));
        }
    }
}

