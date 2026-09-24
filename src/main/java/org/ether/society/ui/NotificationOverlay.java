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
 * Overlay for displaying temporary "toast" notifications (e.g., Age
 * transitions, disasters).
 */
public class NotificationOverlay extends VBox {

    public NotificationOverlay() {
        setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(8);
        setPickOnBounds(false); // Let mouse events pass through empty space
        setStyle("-fx-padding: 0 0 45px 20px;");
    }

    /**
     * Show a new notification message.
     */
    public void showNotification(String message, String color) {
        showSpatialNotification(message, color, null);
    }

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

    public void showEvent(String eventText) {
        showEvent(eventText, null);
    }

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
}
