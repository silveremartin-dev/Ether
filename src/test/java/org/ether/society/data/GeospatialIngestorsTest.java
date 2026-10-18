/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying geospatial dataset ingestion components:
 * {@link CShapesVectorIngestor}, {@link ResourceDepositMapReader}, and {@link HgisAtlasIngestor}.
 */
class GeospatialIngestorsTest {

    @Test
    @DisplayName("CShapesFeature accurately evaluates active historical validity interval")
    void testCShapesFeatureInterval() {
        CShapesVectorIngestor.CShapesFeature feat = new CShapesVectorIngestor.CShapesFeature();
        feat.countryName = "French Third Republic";
        feat.gwCode = 220;
        feat.startYear = 1870;
        feat.endYear = 1940;

        assertTrue(feat.isActiveInYear(1870));
        assertTrue(feat.isActiveInYear(1914));
        assertTrue(feat.isActiveInYear(1940));
        assertFalse(feat.isActiveInYear(1850));
        assertFalse(feat.isActiveInYear(1950));
    }

    @Test
    @DisplayName("ResourceDepositMapReader samples normalized density correctly from continuous raster")
    void testSampleDensityFromImageTensor() {
        int width = 360;
        int height = 180;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fill with uniform white (density 1.0)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, 0xFFFFFF);
            }
        }

        double valWhite = ResourceDepositMapReader.sampleDensityFromImageTensor(img, 0.0, 0.0);
        assertEquals(1.0, valWhite, 1e-3, "White pixel must sample as density 1.0");

        // Set one pixel to black (density 0.0) at lat 0, lon 0 (x=180, y=90)
        img.setRGB(180, 90, 0x000000);
        double valBlack = ResourceDepositMapReader.sampleDensityFromImageTensor(img, 0.0, 0.0);
        assertEquals(0.0, valBlack, 1e-3, "Black pixel must sample as density 0.0");
    }

    @Test
    @DisplayName("ResourceDepositMapReader parses standard ESRI ASCII grid file")
    void testReadAsciiGridFile(@TempDir Path tempDir) throws IOException {
        Path ascPath = tempDir.resolve("test_deposit.asc");
        String ascContent =
                "ncols         2\n" +
                "nrows         2\n" +
                "xllcorner     0.0\n" +
                "yllcorner     0.0\n" +
                "cellsize      1.0\n" +
                "NODATA_value  -9999\n" +
                "10.0 20.0\n" +
                "30.0 -9999\n";
        Files.writeString(ascPath, ascContent);

        double[][] grid = ResourceDepositMapReader.readAsciiGridFile(ascPath.toFile());
        assertNotNull(grid);
        assertEquals(2, grid.length);
        assertEquals(2, grid[0].length);
        assertEquals(0.0, grid[0][0], 1e-4, "Min value (10.0) normalized should be 0.0");
        assertEquals(0.5, grid[0][1], 1e-4, "Mid value (20.0) normalized should be 0.5");
        assertEquals(1.0, grid[1][0], 1e-4, "Max value (30.0) normalized should be 1.0");
        assertEquals(0.0, grid[1][1], 1e-4, "Nodata value should be normalized to 0.0");
    }

    @Test
    @DisplayName("ResourceDepositMapReader rasterizes hotspot points with alpha blending")
    void testRasterizeDepositHotspots() {
        double[][] hotspots = new double[][]{
                {0.0, 0.0, 4.0, 1.0} // lon, lat, radius, intensity
        };

        BufferedImage img = ResourceDepositMapReader.rasterizeDepositHotspots(hotspots, 100, 50, Color.ORANGE);
        assertNotNull(img);
        assertEquals(100, img.getWidth());
        assertEquals(50, img.getHeight());
    }
}
