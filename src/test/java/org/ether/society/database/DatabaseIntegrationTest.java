/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database layer.
 * Tests H3CellRepository, WorldMapRepository, and DataService.
 * 
 * NOTE: Requires PostgreSQL database running on localhost:54320
 * Run: docker-compose up -d
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseIntegrationTest.class);

    private DataService dataService;

    @BeforeAll
    void setup() {
        logger.info("Setting up database integration tests...");

        // Check if database is available
        if (!DatabaseConfig.isDatabaseAvailable()) {
            logger.warn("Database not available - skipping tests");
            Assumptions.assumeTrue(false, "Database not available");
        }

        dataService = new DataService();
    }

    @BeforeEach
    void cleanDatabase() {
        // Clear database before each test
        dataService.clearDatabase();
    }

    @AfterAll
    void tearDown() {
        if (dataService != null) {
            dataService.close();
        }
    }

    @Test
    @DisplayName("Should save and load H3 cells")
    void testSaveAndLoadCells() {
        // Given: Sample H3 cells
        List<H3Cell> cells = createSampleCells(100);

        // When: Save simulation
        dataService.saveSimulation("test_map", cells);

        // Then: Verify cell count
        long count = dataService.getCellCount();
        assertEquals(100, count, "Should have saved 100 cells");

        // And: Load simulation
        List<H3Cell> loadedCells = dataService.loadSimulation("test_map");
        assertEquals(100, loadedCells.size(), "Should load 100 cells");
    }

    @Test
    @DisplayName("Should handle large batch inserts")
    void testBatchInsert() {
        // Given: Large dataset (10k cells)
        List<H3Cell> cells = createSampleCells(10000);

        // When: Save with timing
        long startTime = System.currentTimeMillis();
        dataService.saveSimulation("large_test", cells);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Verify performance
        assertTrue(duration < 30000, "Should save 10k cells in < 30 seconds (was: " + duration + "ms)");
        assertEquals(10000, dataService.getCellCount());

        logger.info("Batch insert of 10k cells completed in {}ms", duration);
    }

    @Test
    @DisplayName("Should list available maps")
    void testListMaps() {
        // Given: Multiple saved maps
        dataService.saveSimulation("map1", createSampleCells(10));
        dataService.clearDatabase();
        dataService.saveSimulation("map2", createSampleCells(20));

        // When: Get available maps
        List<String> maps = dataService.getAvailableMaps();

        // Then: Verify maps exist
        assertFalse(maps.isEmpty(), "Should have at least one map");
        assertTrue(maps.contains("map2"), "Should contain map2");
    }

    @Test
    @DisplayName("Should verify database availability")
    void testDatabaseAvailability() {
        // When: Check availability
        boolean available = dataService.isDatabaseAvailable();

        // Then: Should be available
        assertTrue(available, "Database should be available");
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        // Given: Multiple threads
        int threadCount = 5;
        List<Thread> threads = new ArrayList<>();

        // When: Multiple threads save data
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                List<H3Cell> cells = createSampleCells(100);
                dataService.saveSimulation("concurrent_map_" + threadId, cells);
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then: Verify all maps saved
        List<String> maps = dataService.getAvailableMaps();
        assertTrue(maps.size() >= threadCount, "Should have saved all maps");
    }

    /**
     * Helper: Create sample H3 cells for testing.
     */
    private List<H3Cell> createSampleCells(int count) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index(600000000000000L + i); // Sample H3 index
            cell.setLatitude(45.0 + (i * 0.001));
            cell.setLongitude(2.0 + (i * 0.001));
            cell.setElevation(100.0 + (i % 1000));
            cell.setTemperature(15.0);
            cell.setRainfall(500.0);
            cell.setBiome(Biome.PLAINS);
            cells.add(cell);
        }
        return cells;
    }
}
