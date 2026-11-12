/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.scene.paint.Color;

/**
 * Cartographic color gradients and hypsometric palettes for planetary visualization.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PlanetColorPaletteHelper {

    private PlanetColorPaletteHelper() {
    }

    public static Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.max(0.0, Math.min(1.0, r)), Math.max(0.0, Math.min(1.0, g)), Math.max(0.0, Math.min(1.0, b)));
    }

    public static Color getTemperatureColor(double tempC, double minT, double maxT) {
        double norm = Math.max(0.0, Math.min(1.0, (tempC - minT) / (maxT - minT + 0.001)));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(40 * (1 - t)), (int)(100 * t), (int)(120 + 100 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(100 * t), (int)(100 + 100 * t), (int)(220 * (1 - t)));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(100 + 140 * t), (int)(200 - 40 * t), 0);
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(240 - 20 * t), (int)(160 * (1 - t)), (int)(40 * t));
        }
    }

    public static Color getPrecipitationColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(210 - 10 * t), (int)(170 + 40 * t), (int)(110 + 10 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(200 - 140 * t), (int)(210 - 30 * t), (int)(120 - 40 * t));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(60 - 40 * t), (int)(180 - 20 * t), (int)(80 + 100 * t));
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(20 - 10 * t), (int)(160 - 100 * t), (int)(180 + 20 * t));
        }
    }

    public static Color getSeasonalityColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.33) {
            double t = norm / 0.33;
            return Color.rgb((int)(30 + 50 * t), (int)(80 + 100 * t), (int)(140 - 40 * t));
        } else if (norm < 0.66) {
            double t = (norm - 0.33) / 0.33;
            return Color.rgb((int)(80 + 160 * t), (int)(180 - 40 * t), (int)(100 - 80 * t));
        } else {
            double t = (norm - 0.66) / 0.34;
            return Color.rgb((int)(240 - 20 * t), (int)(140 - 110 * t), (int)(20 + 60 * t));
        }
    }

    public static Color getHypsometricColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.20) {
            double t = norm / 0.20;
            return Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
        } else if (norm < 0.40) {
            double t = (norm - 0.20) / 0.20;
            return Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
        } else if (norm < 0.65) {
            double t = (norm - 0.40) / 0.25;
            return Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
        } else if (norm < 0.85) {
            double t = (norm - 0.65) / 0.20;
            return Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
        } else {
            double t = (norm - 0.85) / 0.15;
            return Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
        }
    }
}
