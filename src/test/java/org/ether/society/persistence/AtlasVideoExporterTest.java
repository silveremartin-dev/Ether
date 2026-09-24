package org.ether.society.persistence;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.ui.DisplayMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AtlasVideoExporterTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ether_video_test");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var s = Files.walk(tempDir)) {
                s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    void testRenderFrameSingle() {
        List<H3Cell> cells = createSampleCells();
        Set<DisplayMode> layers = Set.of(DisplayMode.BIOME, DisplayMode.POPULATION, DisplayMode.TEMPERATURE);

        BufferedImage img = AtlasVideoExporter.renderFrame(
                800, 440, cells, layers, 150L, 2, 5
        );

        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(800);
        assertThat(img.getHeight()).isEqualTo(440);
    }

    @Test
    void testExportVideoAsyncCreatesValidGif() throws Exception {
        HistoryManager historyManager = new HistoryManager();

        // Add 5 snapshots
        for (long tick = 10; tick <= 50; tick += 10) {
            historyManager.getWorldSnapshots().put(tick, createSampleCells());
        }

        Set<DisplayMode> layers = Set.of(DisplayMode.BIOME, DisplayMode.POPULATION);
        File targetFile = tempDir.resolve("test_atlas_animation.gif").toFile();

        AtomicInteger progressCalls = new AtomicInteger(0);
        CompletableFuture<File> future = AtlasVideoExporter.exportVideoAsync(
                historyManager,
                layers,
                0,
                4,
                100,
                targetFile,
                "TestScenario",
                p -> progressCalls.incrementAndGet()
        );

        File resultFile = future.get(10, TimeUnit.SECONDS);

        assertThat(resultFile).isNotNull();
        assertThat(resultFile.exists()).isTrue();
        assertThat(resultFile.length()).isGreaterThan(500); // Has GIF frame bytes
        assertThat(progressCalls.get()).isGreaterThan(0);

        // Verify GIF header (GIF89a or GIF87a)
        try (FileInputStream fis = new FileInputStream(resultFile)) {
            byte[] header = new byte[6];
            int read = fis.read(header);
            assertThat(read).isEqualTo(6);
            String headerStr = new String(header);
            assertThat(headerStr).startsWith("GIF");
        }
    }

    private List<H3Cell> createSampleCells() {
        List<H3Cell> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(0x881f1d4881fffff0L + i);
            c.setLatitude(45.0 + (i % 5) * 1.5);
            c.setLongitude(5.0 + (i / 5) * 1.5);
            c.setBiome(Biome.FOREST);
            c.setPopulation(1000 + i * 250);
            c.setTemperature(18.5);
            c.setRainfall(750.0);
            c.setWaterResource(80.0);
            c.setDynamicAlbedo(0.25);
            c.setMovementFriction(1.0);
            c.setTechnologyLevel(2.5);
            c.setResourceMetal(50.0);
            list.add(c);
        }
        return list;
    }
}
