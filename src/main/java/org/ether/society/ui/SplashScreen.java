/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ether.society.i18n.I18n;

/**
 * Startup Splash Screen with loading progress bar.
 */
public class SplashScreen {
    private final Stage stage;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label subStatusLabel;

    public SplashScreen() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        WindowUtils.applyWindowIcon(stage);

        Label titleLabel = new Label(I18n.getOrDefault("splash.title", "ETHER 2.0"));
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-effect: dropshadow(three-pass-box, rgba(56,189,248,0.6), 12, 0, 0, 0);");

        Label subtitleLabel = new Label(I18n.getOrDefault("splash.subtitle", "Cliodynamic Engine & Planetary Biophysics"));
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(440);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: #38bdf8; -fx-control-inner-background: rgba(15, 23, 42, 0.8);");

        statusLabel = new Label(I18n.getOrDefault("splash.status.init", "Initializing system..."));
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        subStatusLabel = new Label(I18n.getOrDefault("splash.status.loading", "Please wait while subsystems are loading..."));
        subStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        VBox content = new VBox(14, titleLabel, subtitleLabel, progressBar, statusLabel, subStatusLabel);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.96); -fx-padding: 32 44; -fx-background-radius: 16; -fx-border-color: #38bdf8; -fx-border-radius: 16; -fx-border-width: 1.5;");

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 540, 270);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public void show() {
        stage.show();
    }

    public void updateProgress(double progress, String status, String subStatus) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setProgress(progress);
            if (status != null) statusLabel.setText(status);
            if (subStatus != null) subStatusLabel.setText(subStatus);
        });
    }

    public void close() {
        javafx.application.Platform.runLater(stage::close);
    }
}
