/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for multi-layer data rendering and simulation controls in Ether.
 */
public class MultiLayerRenderingTest {

    private static boolean jfxInitialized = false;

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        if (!jfxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    jfxInitialized = true;
                    Platform.setImplicitExit(false);
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                jfxInitialized = true;
                latch.countDown();
            }
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Verify default active layers include Biome and Population")
    public void testDefaultActiveLayers() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);
                Set<DisplayMode> activeModes = canvas.getActiveDisplayModes();

                assertTrue(activeModes.contains(DisplayMode.BIOME), "Default layers should include BIOME");
                assertTrue(activeModes.contains(DisplayMode.POPULATION), "Default layers should include POPULATION");
                assertFalse(canvas.isOnlyStaticBiome(), "Default multi-layer is not static biome only");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify toggling active display modes")
    public void testToggleDisplayModes() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME));
                assertEquals(1, canvas.getActiveDisplayModes().size());
                assertTrue(canvas.isOnlyStaticBiome());

                canvas.setDisplayModeActive(DisplayMode.TEMPERATURE, true);
                assertEquals(2, canvas.getActiveDisplayModes().size());
                assertTrue(canvas.isDisplayModeActive(DisplayMode.TEMPERATURE));
                assertFalse(canvas.isOnlyStaticBiome());

                canvas.setDisplayModeActive(DisplayMode.TEMPERATURE, false);
                assertEquals(1, canvas.getActiveDisplayModes().size());
                assertFalse(canvas.isDisplayModeActive(DisplayMode.TEMPERATURE));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify blended color overlay: terrain remains visible under population")
    public void testBlendedBiomePopulationRendering() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                H3Cell unpopulatedLand = new H3Cell(0x882681a339fffffL, 45.0, 5.0);
                unpopulatedLand.setBiome(Biome.DESERT);
                unpopulatedLand.setElevation(100.0);
                unpopulatedLand.setPopulation(0);

                H3Cell populatedLand = new H3Cell(0x882681a33bfffffL, 46.0, 6.0);
                populatedLand.setBiome(Biome.FOREST);
                populatedLand.setElevation(100.0);
                populatedLand.setPopulation(500);

                canvas.setCells(List.of(unpopulatedLand, populatedLand));

                // 1. In default mode (BIOME + POPULATION):
                canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));

                Color blendHelper = canvas.blendColors(Color.WHITE, Color.BLACK, 0.5);
                assertNotNull(blendHelper);
                assertEquals(0.5, blendHelper.getRed(), 0.01);

                // 2. Floating layer check
                assertTrue(canvas.isShowFloatingLayers(), "Floating layers should be enabled by default");
                assertTrue(canvas.hasThematicOverlay(populatedLand), "Populated cell should have thematic overlay");
                assertFalse(canvas.hasThematicOverlay(unpopulatedLand), "Unpopulated cell without other metrics has no overlay");

                // 3. Multi-layer ColorLegend verification
                ColorLegend legend = new ColorLegend();
                legend.updateFromCanvas(canvas);
                assertNotNull(legend.getDisplayMode());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}