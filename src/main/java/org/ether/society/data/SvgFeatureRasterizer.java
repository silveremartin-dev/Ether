/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.data.SvgMapIngestor.SvgLayerFeature;
import org.ether.society.data.SvgMapIngestor.SvgIngestionResult;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/**
 * Multi-channel cartographic rasterizer for SVG features.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SvgFeatureRasterizer {

    private SvgFeatureRasterizer() {
    }

    public static final int WIDTH = SvgMapIngestor.WIDTH;
    public static final int HEIGHT = SvgMapIngestor.HEIGHT;

    // --- MULTI-CHANNEL RASTERIZER ---

    public static SvgMapIngestor.SvgIngestionResult rasterizeFeatures(List<SvgLayerFeature> features) {
        SvgIngestionResult res = new SvgIngestionResult();
        res.parsedFeaturesCount = features.size();

        res.densityImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        res.sovereigntyImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        res.isoglossImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        res.kinshipImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        res.ritualsImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D gDensity = res.densityImage.createGraphics();
        Graphics2D gSov = res.sovereigntyImage.createGraphics();
        Graphics2D gIso = res.isoglossImage.createGraphics();
        Graphics2D gKin = res.kinshipImage.createGraphics();
        Graphics2D gRit = res.ritualsImage.createGraphics();

        // Enable Anti-Aliasing for smooth cartographic borders
        for (Graphics2D g : List.of(gDensity, gSov, gIso, gKin, gRit)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }

        for (SvgLayerFeature f : features) {
            if (f.geometry == null) continue;

            String type = f.layerType.toUpperCase(Locale.ROOT);
            if ("ALL".equals(type) || "SOVEREIGNTY".equals(type)) {
                gSov.setColor(f.color);
                gSov.fill(f.geometry);
            }

            if ("ALL".equals(type) || "ISOGLOSS".equals(type)) {
                gIso.setColor(f.color);
                gIso.fill(f.geometry);
            }

            if ("ALL".equals(type) || "KINSHIP".equals(type)) {
                gKin.setColor(f.color);
                gKin.fill(f.geometry);
            }

            if ("ALL".equals(type) || "RITUALS".equals(type)) {
                gRit.setColor(f.color);
                gRit.fill(f.geometry);
            }

            if ("ALL".equals(type) || "DENSITY".equals(type)) {
                int intensity = (int) Math.min(255, Math.max(10, f.densityWeight * 255.0));
                gDensity.setColor(new Color(intensity, intensity, intensity));
                gDensity.fill(f.geometry);
            }
        }

        for (Graphics2D g : List.of(gDensity, gSov, gIso, gKin, gRit)) {
            g.dispose();
        }

        // Convert BufferedImages to Base64 PNGs
        res.densityBase64 = HistoricalMapGenerator.bufferedImageToBase64Png(res.densityImage);
        res.sovereigntyBase64 = HistoricalMapGenerator.bufferedImageToBase64Png(res.sovereigntyImage);
        res.isoglossBase64 = HistoricalMapGenerator.bufferedImageToBase64Png(res.isoglossImage);
        res.kinshipBase64 = HistoricalMapGenerator.bufferedImageToBase64Png(res.kinshipImage);
        res.ritualsBase64 = HistoricalMapGenerator.bufferedImageToBase64Png(res.ritualsImage);

        return res;
    }
}
