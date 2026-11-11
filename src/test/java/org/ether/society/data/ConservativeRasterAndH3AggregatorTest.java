/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous verification suite for {@link ConservativeRasterResampler}, {@link H3RasterAggregator},
 * and {@link Hyde34LandUseReader}:
 * <ul>
 *   <li>Exact global mass conservation across latitudinal belts in extensive regridding</li>
 *   <li>Masked coastal preservation in intensive continuous regridding</li>
 *   <li>H3 spherical density integral conservation across H3 resolutions and strict bitwise determinism</li>
 *   <li>HYDE 3.4 land-use ingestion (cropland, irrigation, pasture, rangeland, urban) and biome/anthrome coupling</li>
 * </ul>
 */
class ConservativeRasterAndH3AggregatorTest {

    @Test
    @DisplayName("1. ConservativeRasterResampler preserves 100% global sum across latitudes")
    void testExtensiveConservationInvariant() {
        int srcW = 432, srcH = 216;
        int dstW = 204, dstH = 102;
        float[][] src = new float[srcH][srcW];

        double srcSum = 0.0;
        // Place multi-latitude clusters (equatorial, mid-latitude, high-latitude) and edge cases
        for (int r = 0; r < srcH; r++) {
            for (int c = 0; c < srcW; c++) {
                if ((r == 10 && c == 50) || (r == 54 && c == 210) || (r == 108 && c == 300) || (r == 180 && c == 400)) {
                    src[r][c] = 1234.5f;
                    srcSum += 1234.5f;
                } else if (r >= 90 && r <= 120 && c >= 180 && c <= 230) {
                    float v = (r + c) * 0.25f;
                    src[r][c] = v;
                    srcSum += v;
                } else {
                    src[r][c] = -9999.0f; // NoData sentinels must be ignored
                }
            }
        }

        float[][] dst = ConservativeRasterResampler.resampleExtensive(src, srcW, srcH, -9999.0f, dstW, dstH);
        assertEquals(dstH, dst.length);
        assertEquals(dstW, dst[0].length);

        double dstSum = 0.0;
        for (float[] row : dst) {
            for (float v : row) {
                assertTrue(v >= 0.0f && Float.isFinite(v));
                dstSum += v;
            }
        }

        assertEquals(srcSum, dstSum, srcSum * 1e-5,
                "Conservative extensive regridding must preserve total global mass/count to floating precision");
    }

    @Test
    @DisplayName("2. ConservativeRasterResampler prevents coastal NoData bleeding in intensive averaging")
    void testIntensiveMaskedCoastalPreservation() {
        int srcW = 100, srcH = 50;
        int dstW = 40, dstH = 20;
        float[][] src = new float[srcH][srcW];

        for (int r = 0; r < srcH; r++) {
            for (int c = 0; c < srcW; c++) {
                // Checkerboard coastline at c=50: land is strictly 20.0°C, ocean is -9999°C NoData
                src[r][c] = (c < 50) ? 20.0f : -9999.0f;
            }
        }

        float[][] dst = ConservativeRasterResampler.resampleIntensiveMasked(
                src, srcW, srcH, dstW, dstH, v -> v > -100.0f, Float.NaN);

        for (int y = 0; y < dstH; y++) {
            for (int x = 0; x < dstW; x++) {
                float val = dst[y][x];
                if (x < 20) {
                    assertEquals(20.0f, val, 1e-4f, "Valid land pixels must average cleanly to 20.0 without ocean NoData bias");
                } else {
                    assertTrue(Float.isNaN(val), "Pure ocean pixels must return NaN sentinel");
                }
            }
        }
    }

    @Test
    @DisplayName("3. H3RasterAggregator conserves global integral across H3 resolutions with strict determinism")
    void testH3DensityConservationAndDeterminism() {
        int w = 512, h = 256;
        BufferedImage density = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // Synthetic demographic hotspots across latitudes (Europe ~48N, Nile ~28N, Java ~-7S, Patagonia ~-48S)
        double[][] hotspots = {
            {10.0, 48.0, 220},
            {31.0, 28.0, 250},
            {110.0, -7.0, 240},
            {-68.0, -48.0, 140}
        };
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) / h * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x + 0.5) / w * 360.0;
                double val = 0.0;
                for (double[] hs : hotspots) {
                    double d2 = Math.pow(lat - hs[1], 2) + Math.pow(lon - hs[0], 2);
                    val += hs[2] * Math.exp(-d2 / 40.0);
                }
                int g = Math.clamp((int) Math.round(val), 0, 255);
                density.setRGB(x, y, (g << 16) | (g << 8) | g);
            }
        }

        double exactGlobalIntegral = H3RasterAggregator.computeGlobalDensityIntegral(density);
        assertTrue(exactGlobalIntegral > 0.0);

        // Test at H3 Res 2 (5,882 cells) and H3 Res 3 (41,162 cells)
        List<H3Cell> res2 = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(2));
        double[] wRes2Run1 = H3RasterAggregator.aggregateDensityWeights(density, res2);
        double[] wRes2Run2 = H3RasterAggregator.aggregateDensityWeights(density, res2);
        assertArrayEquals(wRes2Run1, wRes2Run2, 0.0, "Parallel H3 aggregation must be 100% bitwise deterministic");

        double sumRes2 = 0.0;
        for (double v : wRes2Run1) sumRes2 += v;
        assertEquals(exactGlobalIntegral, sumRes2, exactGlobalIntegral * 1e-4,
                "H3 Res 2 integrated weights must match continuous global spherical integral");

        List<H3Cell> res3 = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(3));
        double[] wRes3 = H3RasterAggregator.aggregateDensityWeights(density, res3);
        double sumRes3 = 0.0;
        for (double v : wRes3) sumRes3 += v;
        assertEquals(exactGlobalIntegral, sumRes3, exactGlobalIntegral * 1e-4,
                "H3 Res 3 integrated weights must match continuous global spherical integral");
    }

    @Test
    @DisplayName("4. H3RasterAggregator computes area-weighted elevation average and modal biome")
    void testH3ElevationAndModalBiomeAggregation() {
        int w = 512, h = 256;
        BufferedImage elevImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage biomeImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // Northern hemisphere (> 0°): +4000m SNOW/MOUNTAINS; Southern hemisphere (< 0°): Sea level PLAINS
        int highGray = 220;
        int highRgb = (highGray << 16) | (highGray << 8) | highGray;
        int lowGray = 141;
        int lowRgb = (lowGray << 16) | (lowGray << 8) | lowGray;

        for (int y = 0; y < h; y++) {
            boolean north = y < h / 2;
            int er = north ? highRgb : lowRgb;
            int br = north ? 0x147814 /* FOREST */ : 0x64C832 /* PLAINS */;
            for (int x = 0; x < w; x++) {
                elevImg.setRGB(x, y, er);
                biomeImg.setRGB(x, y, br);
            }
        }

        List<H3Cell> res2 = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(2));
        H3RasterAggregator.mapRastersToCells(
                res2,
                H3RasterAggregator.RasterArgbBuffer.fromBufferedImage(elevImg),
                H3RasterAggregator.RasterArgbBuffer.fromBufferedImage(biomeImg),
                null, null, null, null, -11000.0, 8848.0);

        int northForest = 0, southPlains = 0;
        for (H3Cell c : res2) {
            if (c.getLatitude() > 5.0) {
                assertEquals(Biome.FOREST, c.getBiome());
                assertTrue(c.getElevation() > 1000.0);
                northForest++;
            } else if (c.getLatitude() < -5.0) {
                assertEquals(Biome.PLAINS, c.getBiome());
                southPlains++;
            }
        }
        assertTrue(northForest > 1000 && southPlains > 1000);
    }

    @Test
    @DisplayName("5. HYDE 3.4 Land-Use Reader integrates anthropogenic footprint and biome modulation")
    void testHyde34LandUseIntegration() {
        // Deep Paleolithic invariant: land use must be null before 10,000 BCE
        assertNull(Hyde34LandUseReader.loadForYear(-50000L));

        Hyde34LandUseReader.LandUseFields lu0 = Hyde34LandUseReader.loadForYear(0L);
        assertNotNull(lu0, "0 AD HYDE 3.4 _lu.zip must load empirical land-use fractions");
        assertEquals(Hyde34LandUseReader.ETHER_HEIGHT, lu0.croplandFrac().length);
        assertEquals(Hyde34LandUseReader.ETHER_WIDTH, lu0.croplandFrac()[0].length);

        // Verify biome clearance conversion rule (pongratz / anthromes)
        int cleared = Hyde34LandUseReader.modulateBiomeWithLandUse(
                WorldClimEmpiricalRasterLoader.BIOME_FOREST, 1024, 250, lu0);
        // Should remain a valid forest or transition to plains depending on exact coordinate
        assertTrue(cleared == WorldClimEmpiricalRasterLoader.BIOME_FOREST
                || cleared == WorldClimEmpiricalRasterLoader.BIOME_PLAINS);

        // Direct rule check: high cropland converts forest -> plains, leaves ocean/glacier untouched
        float[][] dummy = new float[Hyde34LandUseReader.ETHER_HEIGHT][Hyde34LandUseReader.ETHER_WIDTH];
        dummy[512][1024] = 0.70f;
        float[][] zeros = new float[Hyde34LandUseReader.ETHER_HEIGHT][Hyde34LandUseReader.ETHER_WIDTH];
        Hyde34LandUseReader.LandUseFields syn = new Hyde34LandUseReader.LandUseFields(0L, dummy, zeros, zeros, zeros, zeros);

        assertEquals(WorldClimEmpiricalRasterLoader.BIOME_PLAINS,
                Hyde34LandUseReader.modulateBiomeWithLandUse(WorldClimEmpiricalRasterLoader.BIOME_FOREST, 1024, 512, syn));
        assertEquals(WorldClimEmpiricalRasterLoader.BIOME_GLACIER,
                Hyde34LandUseReader.modulateBiomeWithLandUse(WorldClimEmpiricalRasterLoader.BIOME_GLACIER, 1024, 512, syn));

        Scenario sc = new Scenario();
        sc.setStartDateYear(0L);
        BufferedImage ecoImg = HistoricalInstitutionalRasterizer.rasterizeEcologicalFootprintMap("EARTH", sc);
        assertNotNull(ecoImg);
        assertEquals(HistoricalMapGenerator.WIDTH, ecoImg.getWidth());
        assertEquals(HistoricalMapGenerator.HEIGHT, ecoImg.getHeight());
    }
}
