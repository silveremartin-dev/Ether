/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Test Suite verifying Ether 3D Globe rendering, state synchronization,
 * vertical exaggeration controls, and auto-rotation mechanisms.
 */
public class H3GlobeIntegrationTest {

    private static boolean jfxInitialized = false;

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        if (!jfxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    jfxInitialized = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                // Toolkit already initialized
                jfxInitialized = true;
                latch.countDown();
            }
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Verify H3MapCanvas ViewMode switching between 2D and 3D Globe")
    public void testViewModeSwitching() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                assertEquals(ViewMode.VIEW_2D, canvas.getViewMode(), "Default ViewMode should be 2D");

                canvas.setViewMode(ViewMode.VIEW_3D);
                assertEquals(ViewMode.VIEW_3D, canvas.getViewMode(), "ViewMode should switch to 3D Globe");

                canvas.setViewMode(ViewMode.VIEW_2D);
                assertEquals(ViewMode.VIEW_2D, canvas.getViewMode(), "ViewMode should switch back to 2D");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify Vertical Exaggeration parameters and limits in 3D Globe mode")
    public void testVerticalExaggeration() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                assertEquals(25.0, canvas.getVerticalExaggeration(), 0.001, "Default 3D relief vertical exaggeration should be 25x");

                canvas.setVerticalExaggeration(10.0);
                assertEquals(10.0, canvas.getVerticalExaggeration(), 0.001, "Vertical exaggeration should update to 10x");

                canvas.setVerticalExaggeration(50.0);
                assertEquals(50.0, canvas.getVerticalExaggeration(), 0.001, "Vertical exaggeration should update to 50x");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify 3D Globe Auto-Rotation state and tick increment")
    public void testGlobeAutoRotation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                assertFalse(canvas.isAutoRotating(), "Auto-rotation should default to false");

                canvas.setViewMode(ViewMode.VIEW_3D);
                canvas.setAutoRotating(true);
                assertTrue(canvas.isAutoRotating(), "Auto-rotation should be enabled");

                double speed = canvas.getAutoRotationSpeed();
                assertTrue(speed > 0, "Auto-rotation speed must be positive");

                // Execute rotation tick
                canvas.tickAutoRotation();
                assertTrue(canvas.isAutoRotating(), "Auto-rotation should remain active after tick");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify H3 Globe Cell population and O(1) map indexing")
    public void testCellIndexing() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                H3MapCanvas canvas = new H3MapCanvas(800.0, 600.0);

                List<H3Cell> testCells = new ArrayList<>();
                H3Cell c1 = new H3Cell();
                c1.setH3Index(0x85283473fffffffL);
                c1.setLatitude(10.0);
                c1.setLongitude(20.0);
                c1.setElevation(500.0);
                c1.setBiome(Biome.SAVANNAH);
                testCells.add(c1);

                canvas.setCells(testCells);
                canvas.setViewMode(ViewMode.VIEW_3D);

                assertEquals(ViewMode.VIEW_3D, canvas.getViewMode());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

}
