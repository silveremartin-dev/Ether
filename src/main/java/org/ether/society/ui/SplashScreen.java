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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ether.society.i18n.I18n;


/**
 * Startup Splash Screen with loading progress bar and Earth heatmap background.
 */
public class SplashScreen {
    private final Stage stage;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label subStatusLabel;

    public SplashScreen() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        WindowUtils.applyWindowIcon(stage);

        double width = 620;
        double height = 350;

        StackPane card = new StackPane();
        card.setPrefSize(width, height);
        card.setMaxSize(width, height);
        card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: #38bdf8; -fx-border-radius: 16; -fx-border-width: 1.5; -fx-effect: dropshadow(three-pass-box, rgba(56, 189, 248, 0.45), 24, 0, 0, 0);");

        try {
            var bgUrl = getClass().getResource("/images/splash_background.jpg");
            if (bgUrl != null) {
                Image bgImage = new Image(bgUrl.toExternalForm(), width, height, false, true, false);
                ImageView bgView = new ImageView(bgImage);
                bgView.setFitWidth(width);
                bgView.setFitHeight(height);
                bgView.setPreserveRatio(false);

                Rectangle clip = new Rectangle(width, height);
                clip.setArcWidth(32);
                clip.setArcHeight(32);
                bgView.setClip(clip);

                card.getChildren().add(bgView);
            }
        } catch (Exception ignored) {
        }

        StackPane overlay = new StackPane();
        overlay.setPrefSize(width, height);
        overlay.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(15, 23, 42, 0.65) 0%, rgba(15, 23, 42, 0.82) 45%, rgba(15, 23, 42, 0.95) 100%); -fx-background-radius: 16;");
        card.getChildren().add(overlay);

        Label titleLabel = new Label(I18n.getOrDefault("splash.title", "ETHER"));
        titleLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-effect: dropshadow(three-pass-box, rgba(56,189,248,0.7), 16, 0, 0, 0);");

        Label versionBadge = new Label(I18n.getOrDefault("splash.version", "v1.0 b1"));
        versionBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.2); -fx-padding: 3 8; -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.6); -fx-border-radius: 6;");

        HBox titleBox = new HBox(12, titleLabel, versionBadge);
        titleBox.setAlignment(Pos.CENTER);

        Label subtitleLabel = new Label(I18n.getOrDefault("splash.subtitle", "Cliodynamic Engine & Planetary Biophysics"));
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #bae6fd; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 1);");

        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(480);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: #38bdf8; -fx-control-inner-background: rgba(15, 23, 42, 0.85); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        statusLabel = new Label(I18n.getOrDefault("splash.status.init", "Initializing system..."));
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 6, 0, 0, 1);");

        subStatusLabel = new Label(I18n.getOrDefault("splash.status.loading", "Please wait while subsystems are loading..."));
        subStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 4, 0, 0, 1);");

        VBox content = new VBox(14, titleBox, subtitleLabel, progressBar, statusLabel, subStatusLabel);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 36 44;");

        card.getChildren().add(content);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        Scene scene = new Scene(root, width + 40, height + 40);
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
