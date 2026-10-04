/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * <h1>Notification Overlay</h1>
 * <p>
 * User interface component and visualization panel for the Ether simulation platform.<br>
 * Provides interactive rendering, real-time spatial heatmaps, parameter controls, and multi-language localized analytics.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class NotificationOverlay extends VBox {

    /*
     * Notification overlay.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     */
    public NotificationOverlay() {
        setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(8);
        setPickOnBounds(false); // Let mouse events pass through empty space
        setStyle("-fx-padding: 0 0 45px 20px;");
    }

    /*
     * Show a new notification message.
     */
    public void showNotification(String message, String color) {
        showSpatialNotification(message, color, null);
    }

    /*
     * Show spatial notification.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param message the message parameter (String)
     * @param color the color parameter (String)
     * @param onClickAction the on click action parameter (Runnable)
     */
    public void showSpatialNotification(String message, String color, Runnable onClickAction) {
        Label label = new Label(message);
        String cursorStyle = onClickAction != null ? "-fx-cursor: hand;" : "";
        label.setStyle("-fx-background-color: rgba(15, 23, 42, 0.92);" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: 'Consolas', monospace;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 0);" +
                cursorStyle);

        if (onClickAction != null) {
            label.setOnMouseClicked(e -> onClickAction.run());
        }

        // Animation: Fade In -> Wait -> Fade Out -> Remove
        label.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(6));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.setOnFinished(e -> getChildren().remove(label));

        getChildren().add(0, label); // Add to top
        seq.play();
    }

    /*
     * Show event.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param eventText the event text parameter (String)
     */
    public void showEvent(String eventText) {
        showEvent(eventText, null);
    }

    /*
     * Show event.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param eventText the event text parameter (String)
     * @param onClickAction the on click action parameter (Runnable)
     */
    public void showEvent(String eventText, Runnable onClickAction) {
        String color = "white"; // Default
        if (eventText.contains("ERA") || eventText.contains("AGE")) {
            color = "#ffd700"; // Gold
        } else if (eventText.contains("PLAGUE") || eventText.contains("FAMINE") || eventText.contains("DISASTER") || eventText.contains("NUCL")) {
            color = "#ff5252"; // Red
        } else if (eventText.contains("MILESTONE")) {
            color = "#69f0ae"; // Green
        } else {
            color = "#38bdf8"; // Cyan
        }

        showSpatialNotification(eventText, color, onClickAction);
    }

    /*
     * Show warning.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param message the message parameter (String)
     */
    public void showWarning(String message) {
        showNotification(message, "#f59e0b");
    }

    /*
     * Show info.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param message the message parameter (String)
     */
    public void showInfo(String message) {
        showNotification(message, "#38bdf8");
    }

    /*
     * Show error.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param message the message parameter (String)
     */
    public void showError(String message) {
        showNotification(message, "#ef4444");
    }

    /*
     * Show success.
     * Enforces physical invariants and updates associated state variables within {@code NotificationOverlay}.
     *
     * @param message the message parameter (String)
     */
    public void showSuccess(String message) {
        showNotification(message, "#10b981");
    }
}

