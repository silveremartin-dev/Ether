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
        capitalCell.setElevation(150.0);
        capitalCell.setBiome(Biome.PLAINS);
        capitalCell.setPopulation(5000);
        capitalCell.setFoodResource(25000.0);
        capitalCell.setResourceCapital(12000.0);
        cells.add(capitalCell);

        H3Cell territoryCell = new H3Cell(987654321L, 48.86, 2.36);
        territoryCell.setElevation(200.0);
        territoryCell.setBiome(Biome.FOREST);
        territoryCell.setPopulation(2000);
        territoryCell.setFoodResource(10000.0);
        territoryCell.setResourceCapital(5000.0);
        cells.add(territoryCell);

        // Create nation linking capital and territory
        Nation nation = new Nation("Test Empire", Color.BLUE, capitalCell);
        nation.addCell(territoryCell);

        // Establish bidirectional linkage
        capitalCell.setOwner(nation);
        territoryCell.setOwner(nation);

        engine.setCells(cells);

        // This must succeed without error
        assertDoesNotThrow(() -> {
            saveManager.saveSimulation(engine, "Test_Save_Unified_Architecture");
        });

        // Verify listSaves finds it
        List<SaveMetadata> saves = saveManager.listSaves();
        assertFalse(saves.isEmpty(), "Saved simulation should be listed in saves registry");

        // Test loading back into a new engine
        H3SimulationEngine loadedEngine = new H3SimulationEngine(config);
        assertDoesNotThrow(() -> {
            saveManager.loadSimulation(saves.get(0).getId(), loadedEngine);
        });

        assertNotNull(loadedEngine.getCells());
        assertEquals(2, loadedEngine.getCells().size());

        H3Cell loadedCapital = loadedEngine.getCells().get(0);
        assertEquals(123456789L, loadedCapital.getH3Index());
        assertEquals(48.85, loadedCapital.getLatitude(), 1e-4);
        assertEquals(2.35, loadedCapital.getLongitude(), 1e-4);
        assertEquals(5000, loadedCapital.getPopulation());
    }
}
