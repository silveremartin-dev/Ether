/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

/**
 * Minimalist financial sparkline chart canvas (70x20 px) showing recent trend.
 *
 * <p>Extracted from {@link org.ether.society.ui.StatsPanel} to satisfy the Single Responsibility Principle (SRP).
 * Renders a compact 24-point historical micro-trend line with color coded momentum:
 * green for upward slope, red for downward slope, and cyan for homeostatic stability.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SparklineCanvas extends Canvas {

    private final double[] history = new double[24];
    private int count = 0;
    private int head = 0;

    /**
     * Initializes sparkline canvas with standard 70x20 dimensions.
     */
    public SparklineCanvas() {
        super(70, 20);
        drawEmpty();
    }

    /**
     * Appends a new sample to the circular history buffer and triggers redraw.
     *
     * @param val metric value sample
     */
    public void addValue(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return;
        history[head] = val;
        head = (head + 1) % history.length;
        if (count < history.length) count++;
        redraw();
    }

    private void drawEmpty() {
        var gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setStroke(Color.rgb(100, 116, 139, 0.4));
        gc.setLineWidth(1.0);
        gc.strokeLine(2, getHeight() / 2, getWidth() - 2, getHeight() / 2);
    }

    private void redraw() {
        var gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        gc.clearRect(0, 0, w, h);

        if (count < 2) {
            drawEmpty();
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        double[] ordered = new double[count];
        for (int i = 0; i < count; i++) {
            int idx = (head - count + i + history.length) % history.length;
            ordered[i] = history[idx];
            if (ordered[i] < min) min = ordered[i];
            if (ordered[i] > max) max = ordered[i];
        }

        double range = max - min;
        if (range <= 0.00001) {
            gc.setStroke(Color.rgb(148, 163, 184, 0.8));
            gc.setLineWidth(1.5);
            gc.strokeLine(2, h / 2, w - 2, h / 2);
            return;
        }

        double firstVal = ordered[0];
        double lastVal = ordered[count - 1];
        Color lineColor;
        Color fillColor;
        if (lastVal > firstVal * 1.002) {
            lineColor = Color.rgb(34, 197, 94); // Up-trend Green
            fillColor = Color.rgb(34, 197, 94, 0.15);
        } else if (lastVal < firstVal * 0.998) {
            lineColor = Color.rgb(239, 68, 68); // Down-trend Red
            fillColor = Color.rgb(239, 68, 68, 0.15);
        } else {
            lineColor = Color.rgb(56, 189, 248); // Stable Cyan
            fillColor = Color.rgb(56, 189, 248, 0.12);
        }

        double padY = 3.0;
        double usableH = h - (padY * 2);
        double stepX = (w - 6.0) / (count - 1);

        double[] xPoints = new double[count + 2];
        double[] yPoints = new double[count + 2];

        for (int i = 0; i < count; i++) {
            xPoints[i] = 3.0 + i * stepX;
            double norm = (ordered[i] - min) / range;
            yPoints[i] = h - padY - (norm * usableH);
        }

        xPoints[count] = xPoints[count - 1];
        yPoints[count] = h;
        xPoints[count + 1] = xPoints[0];
        yPoints[count + 1] = h;

        gc.setFill(fillColor);
        gc.fillPolygon(xPoints, yPoints, count + 2);

        gc.setStroke(lineColor);
        gc.setLineWidth(1.5);
        for (int i = 0; i < count - 1; i++) {
            gc.strokeLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }

        gc.setFill(lineColor);
        double lastX = xPoints[count - 1];
        double lastY = yPoints[count - 1];
        gc.fillOval(lastX - 2.0, lastY - 2.0, 4.0, 4.0);
    }
}
