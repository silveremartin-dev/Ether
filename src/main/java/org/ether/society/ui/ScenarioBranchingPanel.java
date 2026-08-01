/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
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

    private final ListView<String> branchListView;
    private final TextField newBranchNameField;
    private final Label infoLabel;

    public ScenarioBranchingPanel(H3SimulationEngine engine, ScenarioBranchingTree tree) {
        this.engine = engine;
        this.branchingTree = tree != null ? tree : new ScenarioBranchingTree();

        setPadding(new Insets(15));
        setSpacing(10);
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: #a78bfa; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label header = new Label("🔀 MULTIVERS & EMBRANCHEMENTS DE TRAJECTOIRES (BRANCHING)");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #a78bfa;");

        newBranchNameField = new TextField();
        newBranchNameField.setPromptText("Nom du nouveau brin (ex: Branche Fusion 2040)...");

        Button createBranchBtn = new Button("➕ Forker la Trajectoire Actuelle");
        createBranchBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white;");
        createBranchBtn.setOnAction(e -> forkCurrentTrajectory());

        HBox forkBox = new HBox(8, newBranchNameField, createBranchBtn);
        forkBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(newBranchNameField, Priority.ALWAYS);

        branchListView = new ListView<>();
        branchListView.setPrefHeight(140);
        branchListView.setStyle("-fx-control-inner-background: #090d16; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        infoLabel = new Label("Séléctionnez une branche pour comparer la téléométrie.");
        infoLabel.setStyle("-fx-text-fill: #94a3b8;");

        refreshBranchList();

        getChildren().addAll(header, forkBox, infoLabel, branchListView);
    }

    private void forkCurrentTrajectory() {
        String name = newBranchNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            name = "Branche " + (branchingTree.getBranches().size() + 1);
        }

        long currentYear = engine != null ? engine.getTimeManager().getCurrentYear() : 2026;
        var cells = engine != null ? engine.getCells() : null;

        ScenarioBranchingTree.SimulationBranch newBranch = branchingTree.createBranch(name, currentYear, cells);
        newBranchNameField.clear();
        refreshBranchList();
        logger.info("Forked trajectory into branch: {} (Year {})", newBranch.getName(), currentYear);
    }

    public void refreshBranchList() {
        branchListView.getItems().clear();
        for (var b : branchingTree.getBranches().values()) {
            String activeTag = b.getId().equals(branchingTree.getActiveBranchId()) ? " ⭐ [ACTIVE]" : "";
            branchListView.getItems().add(String.format("%s (Branchement Année %d)%s", b.getName(), b.getParentBranchYear(), activeTag));
        }
    }
}
