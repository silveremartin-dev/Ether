package org.ether.society.util;

import org.ether.society.model.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainGeneratorTest {

    private TerrainGenerator terrainGenerator;
    private World world;

    @BeforeEach
    void setUp() {
        terrainGenerator = new TerrainGenerator(12345L);
        world = new World(20, 20);
    }

    @Test
    @DisplayName("Generate populates world grid with valid cells")
    void testGenerate() {
        terrainGenerator.generate(world);

        assertNotNull(world.getGrid());
        assertEquals(20, world.getWidth());
        assertEquals(20, world.getHeight());
        assertNotNull(world.getCell(10, 10));
        assertNotNull(world.getCell(10, 10).getBiome());
    }
}
