/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.paint.Color;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite for GeospatialRasterExporter.
 * Validates GeoTIFF Float32 binary layout, 16-bit PNG heightmaps, color PNGs, ESRI World Files, and ASCII Grids.
 */
public class GeospatialRasterExporterTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFloat32GeoTiffExportAndStructure() throws Exception {
        int w = 64;
        int h = 32;
        float[][] grid = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                grid[y][x] = (float) (-4000.0 + (x * 100.0) + (y * 50.0));
            }
        }

        File targetFile = tempDir.resolve("test_dem.tif").toFile();
        GeospatialRasterExporter.writeFloat32GeoTiff(grid, targetFile, -180.0, 180.0, -90.0, 90.0, "Test DEM");

        assertTrue(targetFile.exists());
        assertTrue(targetFile.length() > (w * h * 4));

        // Validate TIFF binary header
        try (RandomAccessFile raf = new RandomAccessFile(targetFile, "r")) {
            byte[] header = new byte[8];
            raf.readFully(header);

            // Little Endian 'II'
            assertEquals('I', (char) header[0]);
            assertEquals('I', (char) header[1]);

            // Magic number 42
            int magic = (header[2] & 0xFF) | ((header[3] & 0xFF) << 8);
            assertEquals(42, magic);

            // First float value at offset 8
            raf.seek(8);
            byte[] floatBuf = new byte[4];
            raf.readFully(floatBuf);
            float firstVal = ByteBuffer.wrap(floatBuf).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            assertEquals(grid[0][0], firstVal, 0.001f);
        }
    }

    @Test
    public void test16BitPngExportAndWorldFile() throws Exception {
        int w = 64;
        int h = 32;
        float[][] grid = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                grid[y][x] = (float) (-11000.0 + (x * 300.0) + (y * 100.0));
            }
        }

        File pngFile = tempDir.resolve("test_heightmap.png").toFile();
        GeospatialRasterExporter.write16BitPng(grid, -11000.0f, 8848.0f, pngFile);

        assertTrue(pngFile.exists());

        // Validate image dimensions and type
        BufferedImage bImg = ImageIO.read(pngFile);
        assertNotNull(bImg);
        assertEquals(w, bImg.getWidth());
        assertEquals(h, bImg.getHeight());

        // Validate accompanying .tfw ESRI World File
        File tfwFile = tempDir.resolve("test_heightmap.tfw").toFile();
        assertTrue(tfwFile.exists());
        List<String> lines = Files.readAllLines(tfwFile.toPath());
        assertEquals(6, lines.size());
        assertTrue(Double.parseDouble(lines.get(0).trim()) > 0); // dx
        assertTrue(Double.parseDouble(lines.get(3).trim()) < 0); // dy (negative for North-up)
    }

    @Test
    public void testColorPngExportWithWorldFile() throws Exception {
        int w = 64;
        int h = 32;
        float[][] grid = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                grid[y][x] = (float) (y * 10.0 - 50.0);
            }
        }

        File pngFile = tempDir.resolve("test_color.png").toFile();
        GeospatialRasterExporter.writeColorPng(grid, val -> {
            if (val < 0) return Color.BLUE;
            return Color.GREEN;
        }, pngFile, true);

        assertTrue(pngFile.exists());
        BufferedImage bImg = ImageIO.read(pngFile);
        assertNotNull(bImg);
        assertEquals(w, bImg.getWidth());
        assertEquals(h, bImg.getHeight());

        File tfwFile = tempDir.resolve("test_color.tfw").toFile();
        assertTrue(tfwFile.exists());
    }

    @Test
    public void testEsriAsciiGridExport() throws Exception {
        int w = 16;
        int h = 8;
        float[][] grid = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                grid[y][x] = (float) (x + y * 2.5);
            }
        }

        File ascFile = tempDir.resolve("test_grid.asc").toFile();
        GeospatialRasterExporter.writeEsriAsciiGrid(grid, ascFile, -180.0, -90.0, 22.5, -9999.0f);

        assertTrue(ascFile.exists());
        List<String> lines = Files.readAllLines(ascFile.toPath());
        assertTrue(lines.size() >= 14);
        assertTrue(lines.get(0).startsWith("NCOLS         16"));
        assertTrue(lines.get(1).startsWith("NROWS         8"));
        assertTrue(lines.get(2).startsWith("XLLCORNER"));
        assertTrue(lines.get(3).startsWith("YLLCORNER"));
        assertTrue(lines.get(4).startsWith("CELLSIZE"));
        assertTrue(lines.get(5).startsWith("NODATA_VALUE"));
    }

    @Test
    public void testProceduralElevationNonUniformity() {
        ProceduralGenerator generator = ProceduralGenerator.getInstance();
        PlanetPreset preset = PlanetPreset.EARTH_LIKE;

        int w = 128;
        int h = 64;
        float[][] grid = new float[h][w];

        float minFound = Float.MAX_VALUE;
        float maxFound = -Float.MAX_VALUE;

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - ((double) y / h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = ((double) x / w) * 360.0 - 180.0;
                ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset);
                float elevM = (float) pt.elevationMeters();
                grid[y][x] = elevM;
                if (elevM < minFound) minFound = elevM;
                if (elevM > maxFound) maxFound = elevM;
            }
        }

        // Elevation in meters MUST have significant dynamic range on Earth (from ocean abyss to mountain peaks)
        assertTrue(minFound < 0.0f, "Ocean floor must be negative elevation in meters, found: " + minFound);
        assertTrue(maxFound > 1000.0f, "Mountain peaks must exceed 1000m, found: " + maxFound);
        assertTrue(maxFound - minFound > 5000.0f, "Dynamic range must exceed 5000m on Earth, found: " + (maxFound - minFound));
    }
}
