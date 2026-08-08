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
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(8);
        setMouseTransparent(true); // Let clicks pass through
        setStyle("-fx-padding: 0 0 50px 0;");
    }

    /**
     * Show a new notification message.
     */
    public void showNotification(String message, String color) {
        Label label = new Label(message);
        label.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85);" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-width: 1px;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: 'Consolas', monospace;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 0);");

        // Animation: Fade In -> Wait -> Fade Out -> Remove
        label.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(4));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.setOnFinished(e -> getChildren().remove(label));

        getChildren().add(0, label); // Add to top
        seq.play();
    }

    public void showEvent(String eventText) {
        String color = "white"; // Default
        if (eventText.contains("ERA") || eventText.contains("AGE")) {
            color = "#ffd700"; // Gold
        } else if (eventText.contains("PLAGUE") || eventText.contains("FAMINE") || eventText.contains("DISASTER")) {
            color = "#ff5252"; // Red
        } else if (eventText.contains("MILESTONE")) {
            color = "#69f0ae"; // Green
        }

        showNotification(eventText, color);
    }
}
