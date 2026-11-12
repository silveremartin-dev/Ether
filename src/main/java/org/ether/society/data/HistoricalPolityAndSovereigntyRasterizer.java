/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Cartographic Rasterizer for Sovereignty and Political Boundaries across Historical Epochs.
 *
 * <p>Decomposed from {@link HistoricalCulturalTensorRasterizer} to satisfy the Single
 * Responsibility Principle (SRP). Handles deep-time hominin clade ranges, paleolithic
 * hunter-gatherer spheres, Seshat historical empires, and modern sovereign boundaries.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class HistoricalPolityAndSovereigntyRasterizer {

    private HistoricalPolityAndSovereigntyRasterizer() {
        // Pure static rasterizer
    }

    public static final int[] COLORS_SOVEREIGNTY_100K = {
        0xD35400, // Clade 0: Proto-Sapiens Pan-African Domain (#D35400)
        0xF39C12, // Clade 1: Out-of-Africa Dispersal Domain (#F39C12)
        0x1F618D, // Clade 2: Western Mousterian Clan Territories (#1F618D)
        0x1A5276, // Clade 3: Zagros & Near East Mousterian Domain (#1A5276)
        0x229954, // Clade 4: Denisovan Central Asian Domain (#229954)
        0x7D3C98  // Clade 5: Eastern Archaic Sunda Domain (#7D3C98)
    };

    /**
     * Blend clade rgb.
     *
     * @param w the w parameter (double[])
     * @param cladeColors the clade colors parameter (int[])
     * @param occWeight the occ weight parameter (double)
     * @return the resulting computation or state reference
     */
    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        if (w == null || occWeight <= 0.001) return 0x2D3748; // uninhabited land
        // Use dominant clade color (argmax) for discrete cultural zone boundaries
        int dominantIdx = 0;
        double maxW = w[0];
        for (int i = 1; i < 6; i++) {
            if (w[i] > maxW) { maxW = w[i]; dominantIdx = i; }
        }
        if (maxW <= 0.0) return 0x2D3748;
        return cladeColors[dominantIdx];
    }

    /**
     * Rasterize sovereignty map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic sovereign vector boundaries from Natural Earth for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernSovereigntyMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        // 2. Ingest authentic vector boundaries from Seshat ClioPatria for historical years
        if (year >= -3400L) {
            BufferedImage seshatImg = CliopatriaPolityVectorReader.rasterizeSeshatSovereigntyMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (seshatImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(seshatImg);
            }
        }

        // (lon, lat, colorRGB, sigma)
        List<double[]> empireCores = HistoricalEmpireCoresRegistry.getEmpireCoresForYear(year);

        if (year <= -18000L) {
            BufferedImage paleoImg = PaleolithicSovereigntySeedsRegistry.rasterizePaleolithicEpoch(year, mask);
            if (paleoImg != null) return paleoImg;
        }

        // Orographic cost-distance propagation with finite sovereign reach limit (38.0)
        List<OrographicGlottologPropagator.CulturalSeed> sovSeeds = new ArrayList<>();
        for (double[] ec : empireCores) {
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(ec[0], ec[1], (int) ec[2], ec[3] / 18.0, "Polity"));
        }
        BufferedImage sovImg = OrographicGlottologPropagator.propagateCulturalSeeds(sovSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 38.0f);

        // Pre-generate orographic tribal background for stateless frontiers
        List<OrographicGlottologPropagator.CulturalSeed> tribalSeeds = CliopatriaPolityVectorReader.getTribalDomainSeeds(year);
        BufferedImage tribalBg = OrographicGlottologPropagator.propagateCulturalSeeds(tribalSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);

        // Apply hominin occupancy and assign authentic regional tribal domains outside imperial reach
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (lat < -60.0 && year < 1900L) {
                    // Antarctica before modern era: uninhabited land a†’ neutral gray
                    sovImg.setRGB(x, y, 0x2D3748);
                } else if (occWeight <= 0.001 && year < 1900L) {
                    // Uninhabited land (remote tundra, uninhabited islands) a†’ neutral gray
                    sovImg.setRGB(x, y, 0x2D3748);
                } else {
                    int currentRgb = sovImg.getRGB(x, y) & 0xFFFFFF;
                    if (currentRgb == 0x374151 || currentRgb == 0x000000) {
                        int unassignedColor = (year >= 1900L && tribalBg != null) ? tribalBg.getRGB(x, y) : 0x2D3748;
                        sovImg.setRGB(x, y, unassignedColor);
                    }
                }
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(sovImg);
    }
}
