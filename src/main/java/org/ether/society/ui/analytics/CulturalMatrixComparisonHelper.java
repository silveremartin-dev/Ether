/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.model.CulturalEntity;
import org.ether.society.model.Scenario;

import java.util.*;

/**
 * <h1>Cultural Matrix Comparison Helper</h1>
 *
 * <p>Provides colorimetric interpolation, CSS styling, entity alignment, and mathematical
 * affinity divergence statistics for ethnographic tensor comparison views.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see CulturalMatrixComparisonPane
 */
final class CulturalMatrixComparisonHelper {

    private CulturalMatrixComparisonHelper() {
        // Pure stateless utility
    }

    /**
     * Statistical discrepancy metrics between two cultural matrices.
     */
    record MatrixStats(double meanA, double meanB, double frobeniusNorm,
                       double mad, double maxDelta, String maxOrig, String maxTarg) {}

    static Rectangle createBadge(String colorHex, double size) {
        Rectangle r = new Rectangle(size, size);
        r.setArcWidth(3);
        r.setArcHeight(3);
        try {
            r.setFill(Color.web(colorHex != null ? colorHex : "#94a3b8"));
        } catch (Exception ex) {
            r.setFill(Color.GRAY);
        }
        return r;
    }

    static String truncateName(String name, int maxLen) {
        if (name == null) return "";
        return name.length() > maxLen ? name.substring(0, maxLen - 1) + "…" : name;
    }

    static Color computeAffinityColor(double val) {
        // [0..1] -> Hostile (crimson) -> Neutral (slate/blue) -> Assimilated (emerald)
        val = Math.clamp(val, 0.0, 1.0);
        if (val < 0.5) {
            double t = val / 0.5;
            return Color.rgb(
                    (int) (239 * (1.0 - t) + 30 * t),
                    (int) (68 * (1.0 - t) + 41 * t),
                    (int) (68 * (1.0 - t) + 59 * t)
            );
        } else {
            double t = (val - 0.5) / 0.5;
            return Color.rgb(
                    (int) (30 * (1.0 - t) + 16 * t),
                    (int) (41 * (1.0 - t) + 185 * t),
                    (int) (59 * (1.0 - t) + 129 * t)
            );
        }
    }

    static String computeDeltaCellStyle(double delta) {
        if (Math.abs(delta) < 0.005) {
            return "-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 4;";
        } else if (delta > 0) {
            double alpha = Math.min(0.9, 0.25 + delta * 0.65);
            return String.format(Locale.ROOT, "-fx-background-color: rgba(16, 185, 129, %.2f); -fx-background-radius: 4;", alpha);
        } else {
            double alpha = Math.min(0.9, 0.25 + Math.abs(delta) * 0.65);
            return String.format(Locale.ROOT, "-fx-background-color: rgba(239, 68, 68, %.2f); -fx-background-radius: 4;", alpha);
        }
    }

    static double getAffinity(Scenario s, CulturalEntity e1, CulturalEntity e2) {
        if (e1 == null || e2 == null) return 0.5;
        if (e1.id().equals(e2.id())) return 1.0;
        if (s != null && s.getCulturalAffinityMatrix() != null) {
            String k1 = e1.id() + ":" + e2.id();
            String k2 = e2.id() + ":" + e1.id();
            Double v = s.getCulturalAffinityMatrix().get(k1);
            if (v != null) return v;
            v = s.getCulturalAffinityMatrix().get(k2);
            if (v != null) return v;
        }
        return e1.affinityTowards(e2);
    }

    static List<CulturalEntity> alignEntities(List<CulturalEntity> listA, List<CulturalEntity> listB) {
        Set<String> idsB = new HashSet<>();
        for (CulturalEntity eb : listB) idsB.add(eb.id());

        List<CulturalEntity> common = new ArrayList<>();
        for (CulturalEntity ea : listA) {
            if (idsB.contains(ea.id())) {
                common.add(ea);
            }
        }
        return common.isEmpty() ? listA : common;
    }
}
