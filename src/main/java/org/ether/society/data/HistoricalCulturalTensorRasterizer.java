/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * High-precision Sociocultural, Linguistic, Religious, and Institutional Cartographic Tensor Rasterizer Facade.
 * 
 * <p>Extracted from {@code HistoricalMapGenerator} and modularized to satisfy the Single Responsibility Principle (SRP).
 * Compiles empirical archaeological, ethnographic, and historical sociopolitical tensors into 2D raster maps:
 * sovereignty, isogloss, kinship systems, sacred sites/rituals, technological progress, trade networks,
 * institutional hierarchy, ecological footprints, and epidemiological immunity landscapes.</p>
 *
 * <p>Delegates cartographic operations to specialized engines:
 * <ul>
 *   <li>{@link HistoricalPolityAndSovereigntyRasterizer} for sovereign and political boundaries.</li>
 *   <li>{@link HistoricalLinguisticAndKinshipRasterizer} for linguistic phyla and anthropological kinship.</li>
 *   <li>{@link HistoricalReligiousAndSocietalRasterizer} for sacred traditions, technology, trade, and institutions.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalCulturalTensorRasterizer {

    private HistoricalCulturalTensorRasterizer() {
        // Pure static rasterizer facade
    }

    // --- COLOR CONSTANTS (DELEGATED FOR 100% BACKWARD COMPATIBILITY) ---
    public static final int[] COLORS_SOVEREIGNTY_100K = HistoricalPolityAndSovereigntyRasterizer.COLORS_SOVEREIGNTY_100K;
    public static final int[] COLORS_ISOGLOSS_100K = HistoricalLinguisticAndKinshipRasterizer.COLORS_ISOGLOSS_100K;
    public static final int[] COLORS_KINSHIP_100K = HistoricalLinguisticAndKinshipRasterizer.COLORS_KINSHIP_100K;
    public static final int[] COLORS_RITUALS_100K = HistoricalReligiousAndSocietalRasterizer.COLORS_RITUALS_100K;

    /**
     * Blend clade rgb.
     *
     * @param w the w parameter (double[])
     * @param cladeColors the clade colors parameter (int[])
     * @param occWeight the occ weight parameter (double)
     * @return the resulting computation or state reference
     */
    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        return HistoricalPolityAndSovereigntyRasterizer.blendCladeRgb(w, cladeColors, occWeight);
    }

    /**
     * Rasterize sovereignty map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        return HistoricalPolityAndSovereigntyRasterizer.rasterizeSovereigntyMap(type, scenario);
    }

    /**
     * Rasterize isogloss map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        return HistoricalLinguisticAndKinshipRasterizer.rasterizeIsoglossMap(type, scenario);
    }

    /**
     * Rasterize kinship map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        return HistoricalLinguisticAndKinshipRasterizer.rasterizeKinshipMap(type, scenario);
    }

    /**
     * Rasterize rituals map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizeRitualsMap(type, scenario);
    }

    /**
     * Apply multi confessional dithering.
     *
     * @param img the img parameter (BufferedImage)
     * @param year the year parameter (long)
     * @param mask the mask parameter (BufferedImage)
     */
    public static void applyMultiConfessionalDithering(BufferedImage img, long year, BufferedImage mask) {
        HistoricalReligiousAndSocietalRasterizer.applyMultiConfessionalDithering(img, year, mask);
    }

    /**
     * Rasterize technology map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizeTechnologyMap(type, scenario);
    }

    /**
     * Rasterize trade network map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizeTradeNetworkMap(type, scenario);
    }

    /**
     * Draw navigable river corridors.
     *
     * @param g the g parameter (Graphics2D)
     * @param year the year parameter (long)
     */
    public static void drawNavigableRiverCorridors(Graphics2D g, long year) {
        HistoricalReligiousAndSocietalRasterizer.drawNavigableRiverCorridors(g, year);
    }

    /**
     * Draw trade route.
     *
     * @param g the g parameter (Graphics2D)
     * @param coords the coords parameter (double[][])
     * @param col the col parameter (Color)
     * @param strokeWidth the stroke width parameter (double)
     */
    public static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        HistoricalReligiousAndSocietalRasterizer.drawTradeRoute(g, coords, col, strokeWidth);
    }

    /**
     * Rasterize institutional complexity map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
    }

    /**
     * Rasterize ecological footprint map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
    }

    /**
     * Rasterize pathogen immunity map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        return HistoricalReligiousAndSocietalRasterizer.rasterizePathogenImmunityMap(type, scenario);
    }
}
