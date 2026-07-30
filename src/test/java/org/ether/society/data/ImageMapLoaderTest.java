package org.ether.society.data;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageMapLoaderTest {

    private ImageMapLoader loader;
    private List<H3Cell> testCells;

    @BeforeEach
    void setUp() {
        loader = new ImageMapLoader();
        testCells = new ArrayList<>();

        H3Cell cell1 = new H3Cell();
        cell1.setH3Index(0x85283473fffffffL);
        cell1.setLatitude(0.0);
        cell1.setLongitude(0.0);
        testCells.add(cell1);

        H3Cell cell2 = new H3Cell();
        cell2.setH3Index(0x85283477fffffffL);
        cell2.setLatitude(45.0);
        cell2.setLongitude(90.0);
        testCells.add(cell2);
    }

    @Test
    void testMatchBiomeColor() {
        Color darkBlue = Color.rgb(0, 0, 100);
        assertEquals(Biome.DEEP_OCEAN, loader.matchBiomeColor(darkBlue));

        Color green = Color.rgb(20, 120, 20);
        assertEquals(Biome.FOREST, loader.matchBiomeColor(green));

        Color white = Color.rgb(255, 255, 255);
        assertEquals(Biome.SNOW, loader.matchBiomeColor(white));
    }

    @Test
    void testMapImagesToCells() {
        int width = 10;
        int height = 10;

        WritableImage elevImg = new WritableImage(width, height);
        WritableImage biomeImg = new WritableImage(width, height);
        WritableImage resImg = new WritableImage(width, height);

        // Fill images with test colors
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                elevImg.getPixelWriter().setColor(x, y, Color.GRAY); // 50% brightness
                biomeImg.getPixelWriter().setColor(x, y, Color.rgb(100, 200, 50)); // Plains color
                resImg.getPixelWriter().setColor(x, y, Color.color(0.8, 0.5, 0.2)); // R=0.8, G=0.5, B=0.2
            }
        }

        loader.mapImagesToCells(testCells, elevImg, biomeImg, resImg, -10000.0, 10000.0);

        for (H3Cell cell : testCells) {
            // Elevation: 50% of range [-10000, 10000] => ~0.0
            assertTrue(Math.abs(cell.getElevation()) < 500.0, "Elevation should be around 0 for gray heightmap");

            // Biome: should match Plains
            assertEquals(Biome.PLAINS, cell.getBiome());

            // Metal (Red = 0.8 * 1000 => 800)
            assertEquals(800.0, cell.getResourceMetal(), 10.0);

            // Wood (Green = 0.5 * 1000 => 500)
            assertEquals(500.0, cell.getWoodResource(), 10.0);

            // Water (Blue = 0.2 * 1000 => 200)
            assertEquals(200.0, cell.getWaterResource(), 10.0);
        }
    }
}
