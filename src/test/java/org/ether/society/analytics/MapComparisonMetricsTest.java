/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous mathematical test suite for {@link MapComparisonMetrics}.
 * Validates spatial RMSE, Pearson cross-correlation, SSIM, Jaccard/Dice categorical overlap,
 * and Kullback-Leibler divergence.
 */
class MapComparisonMetricsTest {

    @Test
    @DisplayName("Comparing identical images produces zero RMSE and perfect correlation (r = 1.0, SSIM = 1.0)")
    void testIdenticalImagesComparison() {
        int width = 32;
        int height = 16;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = (x * 7 + y * 13) % 256;
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }

        MapComparisonMetrics.MapComparisonResult result = MapComparisonMetrics.compareImages(img, img);

        assertNotNull(result);
        assertEquals(0.0, result.getRmse(), 1e-6, "RMSE between identical images must be 0");
        assertEquals(1.0, result.getPearsonR(), 1e-4, "Pearson r between identical images must be 1.0");
        assertTrue(result.getSsim() >= 0.999, "SSIM between identical images must be ~1.0");
        assertEquals(1.0, result.getJaccardIndex(), 1e-6, "Jaccard index between identical images must be 1.0");
        assertEquals(1.0, result.getDiceCoefficient(), 1e-6, "Dice coefficient between identical images must be 1.0");
        assertEquals(0.0, result.getKlDivergence(), 1e-6, "KL divergence between identical distributions must be 0");
        assertEquals(0.0, result.getMaxDeltaValue(), 1e-6);

        String report = result.getFormattedReport();
        assertTrue(report.contains("RMSE (Spatial Grid Error): 0.0000"));
        assertTrue(report.contains("Pearson Correlation (r): 1.0000"));
    }

    @Test
    @DisplayName("Comparing orthogonal or inverted grids detects spatial discrepancy and non-zero RMSE")
    void testDiscrepantGridsComparison() {
        int width = 10;
        int height = 10;
        int n = width * height;

        double[] gridA = new double[n];
        double[] gridB = new double[n];
        int[] catA = new int[n];
        int[] catB = new int[n];

        for (int i = 0; i < n; i++) {
            gridA[i] = 100.0;
            gridB[i] = 200.0;
            catA[i] = 1;
            catB[i] = 2; // Different categories
        }

        MapComparisonMetrics.MapComparisonResult result = MapComparisonMetrics.compareGrids(
                gridA, gridB, catA, catB, width, height
        );

        assertNotNull(result);
        assertEquals(100.0, result.getRmse(), 1e-6, "Constant delta of 100 should give RMSE = 100");
        assertEquals(100.0, result.getMaxDeltaValue(), 1e-6);
        assertEquals(0.0, result.getJaccardIndex(), 1e-6, "Disjoint categories should have Jaccard = 0");
        assertEquals(0.0, result.getDiceCoefficient(), 1e-6, "Disjoint categories should have Dice = 0");
    }

    @Test
    @DisplayName("Null or undersized input grids throw IllegalArgumentException")
    void testValidationGuards() {
        assertThrows(IllegalArgumentException.class, () -> MapComparisonMetrics.compareImages(null, null));

        double[] smallGrid = new double[5];
        int[] cat = new int[5];
        assertThrows(IllegalArgumentException.class, () ->
                MapComparisonMetrics.compareGrids(smallGrid, smallGrid, cat, cat, 10, 10));
    }
}
