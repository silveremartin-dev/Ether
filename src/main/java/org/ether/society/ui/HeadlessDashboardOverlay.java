/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.ether.society.i18n.I18n;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Headless Dashboard Overlay displayed in place of 2D/3D map rendering when headless acceleration is enabled.
 * Provides high-contrast, real-time metrics for year, ticks, population, target horizon, and computational speed.
 */
public class HeadlessDashboardOverlay extends VBox {

    private final Label lblHeadlessTitle;
    private final Label lblHeadlessDesc;
    private final Label lblHeadlessYearVal;
    private final Label lblHeadlessTicksVal;
    private final Label lblHeadlessPopVal;
    private final Label lblHeadlessTargetVal;
    private final Label lblHeadlessSpeedVal;
    private final Button btnHeadlessSwitchGui;

    private LongSupplier populationSupplier;
    private IntSupplier targetYearSupplier;
    private Consumer<ExecutionContextPanel.RenderingMode> onSwitchRenderingMode;

    public HeadlessDashboardOverlay(Consumer<ExecutionContextPanel.RenderingMode> onSwitchRenderingMode) {
        super(24);
        this.onSwitchRenderingMode = Objects.requireNonNull(onSwitchRenderingMode);

        lblHeadlessTitle = new Label();
        lblHeadlessTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 20px; -fx-font-weight: bold;");

        lblHeadlessDesc = new Label();
        lblHeadlessDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-text-alignment: center;");
        lblHeadlessDesc.setWrapText(true);
        lblHeadlessDesc.setMaxWidth(680);

        lblHeadlessYearVal = createMetricValLabel("An 0");
        lblHeadlessTicksVal = createMetricValLabel("0");
        lblHeadlessPopVal = createMetricValLabel("0");
        lblHeadlessTargetVal = createMetricValLabel("-");
        lblHeadlessSpeedVal = createMetricValLabel("0 pas/s");

        VBox cardYear = createMetricCard(I18n.getOrDefault("headless.stat.year", "Année en cours :"), lblHeadlessYearVal);
        VBox cardTicks = createMetricCard(I18n.getOrDefault("headless.stat.ticks", "Pas de simulation :"), lblHeadlessTicksVal);
        VBox cardPop = createMetricCard(I18n.getOrDefault("headless.stat.pop", "Population mondiale :"), lblHeadlessPopVal);
        VBox cardTarget = createMetricCard(I18n.getOrDefault("headless.stat.target", "Horizon cible :"), lblHeadlessTargetVal);
        VBox cardSpeed = createMetricCard(I18n.getOrDefault("headless.stat.speed", "Vitesse de calcul :"), lblHeadlessSpeedVal);

        HBox grid1 = new HBox(16, cardYear, cardTicks, cardPop);
        grid1.setAlignment(Pos.CENTER);

        HBox grid2 = new HBox(16, cardTarget, cardSpeed);
        grid2.setAlignment(Pos.CENTER);

        btnHeadlessSwitchGui = new Button();
        btnHeadlessSwitchGui.getStyleClass().add("button-secondary");
        btnHeadlessSwitchGui.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 22; -fx-cursor: hand;");
        btnHeadlessSwitchGui.setOnAction(e -> {
            if (this.onSwitchRenderingMode != null) {
                this.onSwitchRenderingMode.accept(ExecutionContextPanel.RenderingMode.GUI);
            }
        });

        getChildren().addAll(lblHeadlessTitle, lblHeadlessDesc, grid1, grid2, btnHeadlessSwitchGui);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #0f172a, #020617); -fx-padding: 40;");
        setVisible(false);
        setManaged(false);

        updateHeadlessTexts();
    }

    public void setSuppliers(LongSupplier populationSupplier, IntSupplier targetYearSupplier) {
        this.populationSupplier = populationSupplier;
        this.targetYearSupplier = targetYearSupplier;
    }

    private VBox createMetricCard(String title, Label valLabel) {
        Label lblT = new Label(title);
        lblT.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox card = new VBox(6, lblT, valLabel);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.85); -fx-padding: 14 20; -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 8; -fx-min-width: 170;");
        return card;
    }

    private Label createMetricValLabel(String initial) {
        Label lbl = new Label(initial);
        lbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");
        return lbl;
    }

    /**
     * Updates localized textual descriptions when system locale changes.
     */
    public void updateHeadlessTexts() {
        if (lblHeadlessTitle != null) lblHeadlessTitle.setText(I18n.getOrDefault("headless.banner.title", "🚀 MODE HEADLESS ACTIF (ACCÉLÉRATION MAXIMALE)"));
        if (lblHeadlessDesc != null) lblHeadlessDesc.setText(I18n.getOrDefault("headless.banner.desc", "L'affichage cartographique 2D/3D temps réel est désactivé pour allouer 100 % de la puissance de calcul CPU/GPU au moteur physique et démographique."));
        if (btnHeadlessSwitchGui != null) btnHeadlessSwitchGui.setText(I18n.getOrDefault("headless.btn.switch_gui", "🖼️ Réactiver l'affichage visuel (Mode GUI)"));
    }

    /**
     * Updates real-time telemetry metrics on the headless dashboard.
     *
     * @param dateStr current simulation date string
     * @param currentTick current iteration tick counter
     * @param fps current computation frame or iteration rate
     */
    public void updateHeadlessTelemetry(String dateStr, long currentTick, double fps) {
        if (lblHeadlessYearVal != null) lblHeadlessYearVal.setText(dateStr);
        if (lblHeadlessTicksVal != null) lblHeadlessTicksVal.setText(String.format("%,d", currentTick));
        if (lblHeadlessPopVal != null && populationSupplier != null) {
            lblHeadlessPopVal.setText(String.format("%,d hab", populationSupplier.getAsLong()));
        }
        if (lblHeadlessTargetVal != null && targetYearSupplier != null) {
            int targetYear = targetYearSupplier.getAsInt();
            lblHeadlessTargetVal.setText(targetYear != 0 ? String.format("An %,d", targetYear) : "—");
        }
        if (lblHeadlessSpeedVal != null) {
            lblHeadlessSpeedVal.setText(String.format("%.1f it/s", fps));
        }
    }
}
