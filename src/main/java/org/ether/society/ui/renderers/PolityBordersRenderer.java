/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;

/**
 * Thematic Layer Renderer for political sovereignty, institutional maturity, Asabiyyah solidarity, and borders.
 * 
 * <p>Extracted from {@code H3MapCanvas} to decouple the core canvas rendering loop and camera
 * projection from thematic domain coloring, ensuring modularity, high maintainability, and clean SRP.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PolityBordersRenderer {

    /**
     * Constructs a PolityBordersRenderer.
     */
    public PolityBordersRenderer() {
    }

    /**
     * Computes polity political territory color.
     *
     * @param cell H3 cell
     * @return polity territory color or neutral gray
     */
    public Color getPoliticalColor(H3Cell cell) {
        if (cell == null || cell.getOwner() == null) {
            return Color.rgb(35, 40, 50);
        }
        return cell.getOwner().getColor();
    }

    /**
     * Computes cultural identity tensor color.
     *
     * @param cell H3 cell
     * @return cultural affinity color
     */
    public Color getCultureColor(H3Cell cell) {
        if (cell == null || cell.getOwner() == null) {
            return Color.rgb(40, 45, 55);
        }
        return cell.getOwner().getColor();
    }

    /**
     * Computes Ibn Khaldunian Asabiyyah collective cohesion color.
     *
     * @param cell H3 cell
     * @return color indicating social solidarity
     */
    public Color getAsabiyyahColor(H3Cell cell) {
        if (cell == null || cell.getOwner() == null) return Color.rgb(40, 40, 50);
        double asabiyyah = cell.getOwner().getAsabiyyah();
        return Color.color(1.0 - asabiyyah, asabiyyah, 0.2);
    }

    /**
     * Computes institutional maturity and bureaucratic depth color.
     *
     * @param cell H3 cell
     * @return administrative maturity color
     */
    public Color getInstitutionalMaturityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 20, 40);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double mat = Math.min(1.0, tech / 10.0);
        return Color.color(0.4 + mat * 0.5, 0.2, 0.6 + mat * 0.4);
    }

    /**
     * Computes administrative elite density color.
     *
     * @param cell H3 cell
     * @return elite proportion color
     */
    public Color getEliteDensityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.2;
        double elite = Math.min(1.0, gini * 1.8);
        return Color.color(0.2 + elite * 0.8, 0.2, 0.5 + elite * 0.5);
    }

    /**
     * Computes civil strife and geopolitical conflict risk color.
     *
     * @param cell H3 cell
     * @return conflict intensity color
     */
    public Color getConflictColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
        double conflict = Math.min(1.0, gini * 2.0);
        return Color.color(conflict, 0.8 * (1.0 - conflict), 0.1);
    }
}
