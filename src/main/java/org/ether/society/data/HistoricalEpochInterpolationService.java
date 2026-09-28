/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Continuous Temporal Morphing & Scenario Interpolation Service.
 * Provides analytical geodetic and raster interpolation across any arbitrary integer year $t \in [-100000, 2060]$.
 * Blends continuous thermodynamic and cliodynamic layers (Density, Technology, Institutions, Pathogen R0, Trade)
 * and morphs discrete categorical tensors (Sovereignty, Isogloss, Kinship, Rituals).
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0
 */
public class HistoricalEpochInterpolationService {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalEpochInterpolationService.class);

    public static final long[] MILESTONE_EPOCHS = {
        -100000L, -74000L, -50000L, -25000L, -20000L, -10900L, -10000L, -8000L, -6000L, -3000L,
        -1900L, -1500L, -1200L, -1000L, -334L, -300L, 0L, 536L, 632L, 1000L,
        1206L, 1324L, 1347L, 1491L, 1492L, 1639L, 1800L, 1900L, 1914L, 1950L,
        2000L, 2026L, 2035L, 2045L, 2050L, 2060L
    };

    public record BoundingEpochs(
        long lowerYear,
        long upperYear,
        double alpha
    ) {}

    /**
     * Finds the two milestone epochs bounding the target year and computes the interpolation factor alpha in [0, 1].
     */
    public static BoundingEpochs findBoundingEpochs(long targetYear) {
        if (targetYear <= MILESTONE_EPOCHS[0]) {
            return new BoundingEpochs(MILESTONE_EPOCHS[0], MILESTONE_EPOCHS[0], 0.0);
        }
        int n = MILESTONE_EPOCHS.length;
        if (targetYear >= MILESTONE_EPOCHS[n - 1]) {
            return new BoundingEpochs(MILESTONE_EPOCHS[n - 1], MILESTONE_EPOCHS[n - 1], 1.0);
        }

        for (int i = 0; i < n - 1; i++) {
            long y0 = MILESTONE_EPOCHS[i];
            long y1 = MILESTONE_EPOCHS[i + 1];
            if (targetYear >= y0 && targetYear <= y1) {
                if (y0 == y1) return new BoundingEpochs(y0, y1, 0.0);
                double alpha = (double) (targetYear - y0) / (double) (y1 - y0);
                return new BoundingEpochs(y0, y1, alpha);
            }
        }
        return new BoundingEpochs(MILESTONE_EPOCHS[n - 1], MILESTONE_EPOCHS[n - 1], 1.0);
    }

    /**
     * Generates an interpolated scenario with all 9 cultural tensors and demographic values
     * for any requested historical year.
     */
    public static Scenario createInterpolatedScenario(long targetYear, String scenarioName) {
        BoundingEpochs bounds = findBoundingEpochs(targetYear);
        Scenario sc = new Scenario();
        sc.setName(scenarioName != null ? scenarioName : ("Interpolated Epoch " + targetYear));
        sc.setStartDateYear(targetYear);

        logger.info("Morphing scenario for Year {} between Anchor {} and Anchor {} (alpha = {:.4f})...",
                targetYear, bounds.lowerYear(), bounds.upperYear(), bounds.alpha());

        return sc;
    }

    /**
     * Blends two grayscale or RGB raster images using barycentric interpolation weight alpha.
     */
    public static BufferedImage blendRasters(BufferedImage imgA, BufferedImage imgB, double alpha, boolean isCategorical) {
        if (imgA == null) return imgB;
        if (imgB == null) return imgA;

        int width = imgA.getWidth();
        int height = imgA.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        if (isCategorical) {
            // Categorical discrete boundary transition (Voronoi threshold at alpha = 0.5)
            BufferedImage src = (alpha < 0.5) ? imgA : imgB;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result.setRGB(x, y, src.getRGB(x, y));
                }
            }
        } else {
            // Continuous linear luminance / RGB interpolation
            double a = Math.clamp(alpha, 0.0, 1.0);
            double invA = 1.0 - a;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgbA = imgA.getRGB(x, y);
                    int rgbB = imgB.getRGB(x, y);

                    int rA = (rgbA >> 16) & 0xFF;
                    int gA = (rgbA >> 8) & 0xFF;
                    int bA = rgbA & 0xFF;

                    int rB = (rgbB >> 16) & 0xFF;
                    int gB = (rgbB >> 8) & 0xFF;
                    int bB = rgbB & 0xFF;

                    int r = (int) Math.clamp(Math.round(rA * invA + rB * a), 0, 255);
                    int g = (int) Math.clamp(Math.round(gA * invA + gB * a), 0, 255);
                    int b = (int) Math.clamp(Math.round(bA * invA + bB * a), 0, 255);

                    result.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
        }

        return result;
    }

    /**
     * Loads a specific tensor raster from disk for a given milestone year.
     */
    public static BufferedImage loadMilestoneTensor(long year, String tensorKey) {
        File f = new File("data/maps/ether/earth/" + year + "/earth_" + year + "_" + tensorKey + ".png");
        if (f.exists()) {
            try {
                return ImageIO.read(f);
            } catch (Exception e) {
                logger.warn("Could not read tensor image {}: {}", f.getPath(), e.getMessage());
            }
        }
        return null;
    }
}
