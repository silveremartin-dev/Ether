package com.ether.society.ui;

import com.ether.society.core.H3SimulationEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ControlPanel extends HBox {
    private final SimulationEngine engine;
    private final Label yearLabel;

    public ControlPanel(SimulationEngine engine) {
        this.engine = engine;
        this.yearLabel = new Label("Year: " + engine.getTimeManager().getFormattedYear());

        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #263238; -fx-text-fill: white;");

        Button startBtn = new Button("Start");
        startBtn.setOnAction(e -> engine.start());

        Button pauseBtn = new Button("Pause");
        pauseBtn.setOnAction(e -> engine.pause());

        Button speed1x = new Button("1x");
        speed1x.setOnAction(e -> engine.setSpeed(1));

        Button speed5x = new Button("5x");
        speed5x.setOnAction(e -> engine.setSpeed(5));

        Button speed20x = new Button("20x");
        speed20x.setOnAction(e -> engine.setSpeed(20));

        yearLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        getChildren().addAll(yearLabel, startBtn, pauseBtn, speed1x, speed5x, speed20x);
    }

    public void updateYear(String year) {
        yearLabel.setText("Year: " + year);
    }
}
