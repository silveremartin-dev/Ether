package org.ether.society.persistence;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Nation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.scene.paint.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationSaveManagerTest {

    @Test
    @DisplayName("Saving simulation with interconnected Cells and Nations does not trigger circular recursion")
    void testSaveSimulationWithNationOwnership(@TempDir Path tempDir) throws Exception {
        SimulationSaveManager saveManager = new SimulationSaveManager();
        Configuration config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);

        // Create cell grid
        List<H3Cell> cells = new ArrayList<>();
        H3Cell capitalCell = new H3Cell(123456789L, 48.85, 2.35);
        capitalCell.setBiome(Biome.PLAINS);
        capitalCell.setPopulation(5000);
        cells.add(capitalCell);

        H3Cell territoryCell = new H3Cell(987654321L, 48.86, 2.36);
        territoryCell.setBiome(Biome.FOREST);
        territoryCell.setPopulation(2000);
        cells.add(territoryCell);

        // Create nation linking capital and territory
        Nation nation = new Nation("Test Empire", Color.BLUE, capitalCell);
        nation.addCell(territoryCell);

        // Establish bidirectional linkage
        capitalCell.setOwner(nation);
        territoryCell.setOwner(nation);

        engine.setCells(cells);

        // This must succeed without JsonMappingException / Document nesting depth (1001) exceeds maximum
        assertDoesNotThrow(() -> {
            saveManager.saveSimulation(engine, "Test_Save_Nation_Ownership");
        });
    }
}
