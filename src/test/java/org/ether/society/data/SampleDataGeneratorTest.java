package org.ether.society.data;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unit tests for SampleDataGenerator.
 */
class SampleDataGeneratorTest {

    @Test
    void testGenerateEuropeSample() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        assertNotNull(cells);
        assertTrue(cells.size() > 150000, "Europe should have ~175k cells");

        for (H3Cell cell : cells) {
            double elevation = cell.getElevation();
            assertTrue(elevation >= -100, "Elevation should be >= -100m (dead sea equiv)");
            assertTrue(elevation <= 5000, "Elevation should be <= 5000m (Mont Blanc ~4800m)");
        }
    }

    @Test
    void testBiomeDistribution() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        Map<Biome, Long> distribution = cells.stream()
                .collect(Collectors.groupingBy(H3Cell::getBiome, Collectors.counting()));

        // Verify expected biomes are present
        assertNotNull(distribution.get(Biome.OCEAN), "Should have ocean cells");
        assertNotNull(distribution.get(Biome.PLAINS), "Should have plains");
        assertNotNull(distribution.get(Biome.FOREST), "Should have forests");

        // Ocean should be a significant portion (Atlantic, Mediterranean)
        long oceanCount = distribution.getOrDefault(Biome.OCEAN, 0L);
        assertTrue(oceanCount > cells.size() * 0.2, "Ocean should be >20% of cells");

        // Should not be all one biome
        assertTrue(distribution.size() >= 5, "Should have at least 5 different biomes");
    }

    @Test
    void testTemperatureRange() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        for (H3Cell cell : cells) {
            double temp = cell.getTemperature();
            assertTrue(temp >= -20, "Temperature should be >= -20°C");
            assertTrue(temp <= 40, "Temperature should be <= 40°C");
        }
    }

    @Test
    void testRainfallRange() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        for (H3Cell cell : cells) {
            double rainfall = cell.getRainfall();
            assertTrue(rainfall >= 0, "Rainfall should be >= 0mm");
            assertTrue(rainfall <= 3000, "Rainfall should be <= 3000mm/year");
        }
    }

    @Test
    void testResourcesAreGenerated() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        for (H3Cell cell : cells) {
            // Resources should be present
            assertNotNull(cell.getFoodResource());
            assertNotNull(cell.getWaterResource());
            assertNotNull(cell.getWoodResource());

            // Resources should be non-negative
            assertTrue(cell.getFoodResource() >= 0);
            assertTrue(cell.getWaterResource() >= 0);
            assertTrue(cell.getWoodResource() >= 0);
        }
    }

    @Test
    void testBiomeElevationCorrelation() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        // Mountains should have high elevation
        List<H3Cell> mountains = cells.stream()
                .filter(c -> c.getBiome() == Biome.MOUNTAINS)
                .toList();

        if (!mountains.isEmpty()) {
            double avgMountainElevation = mountains.stream()
                    .mapToDouble(H3Cell::getElevation)
                    .average()
                    .orElse(0);

            assertTrue(avgMountainElevation > 1000, "Mountains should average >1000m elevation");
        }

        // Oceans should have low/negative elevation
        List<H3Cell> oceans = cells.stream()
                .filter(c -> c.getBiome() == Biome.OCEAN)
                .toList();

        if (!oceans.isEmpty()) {
            double avgOceanElevation = oceans.stream()
                    .mapToDouble(H3Cell::getElevation)
                    .average()
                    .orElse(0);

            assertTrue(avgOceanElevation < 0, "Ocean should have negative elevation");
        }
    }

    @Test
    void testPerformanceGenerationTime() {
        long start = System.currentTimeMillis();

        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        long duration = System.currentTimeMillis() - start;

        System.out.println("Generated " + cells.size() + " cells in " + duration + "ms");

        // Should complete in reasonable time (<10 seconds)
        assertTrue(duration < 10000, "Generation should complete in <10 seconds");
    }

    @Test
    void testNoDuplicateCells() {
        List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

        long uniqueCount = cells.stream()
                .map(H3Cell::getH3Index)
                .distinct()
                .count();

        assertEquals(cells.size(), uniqueCount, "All cells should have unique H3 indices");
    }
}
