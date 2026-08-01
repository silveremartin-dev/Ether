package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;
import org.junit.jupiter.api.Test;
import javafx.scene.paint.Color;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CliodynamicsEngineTest {

    @Test
    public void testCliodynamicsSecularCycleUpdate() {
        H3Cell cell1 = new H3Cell(613503380827930623L, 45.0, 5.0);
        cell1.setPopulation(5000);
        cell1.setGiniIndex(0.65);

        H3Cell cell2 = new H3Cell(613503380827930624L, 45.1, 5.1);
        cell2.setPopulation(3000);
        cell2.setGiniIndex(0.50);

        Nation nation = new Nation("Test Empire", Color.BLUE, cell1);
        nation.addCell(cell2);

        double initialAsabiyyah = nation.getAsabiyyah();

        CliodynamicsEngine.updateCliodynamics(List.of(nation), List.of(cell1, cell2));

        assertTrue(nation.getAsabiyyah() <= initialAsabiyyah, "Asabiyyah should decay under inequality and large territory");
        assertTrue(nation.getEliteOverproduction() > 0.0, "Elite overproduction should increase with high Gini index");
        assertTrue(nation.getPoliticalInstability() >= 0.0, "Political instability index should be computed");
    }

    @Test
    public void testTradeNetworkGeneration() {
        H3Cell origin = new H3Cell(613503380827930625L, 40.0, 10.0);
        origin.setPopulation(1000);

        H3Cell dest = new H3Cell(613503380827930626L, 42.0, 12.0);
        dest.setPopulation(1200);

        List<H3Cell> cells = List.of(origin, dest);

        List<TradeNetworkEngine.TradeRoute> routes = TradeNetworkEngine.generateTradeNetworks(cells, 5.0);
        assertNotNull(routes, "Trade route list should not be null");
    }

    @Test
    public void testEcologicalDegradationAndMalthusianLimits() {
        H3Cell cell = new H3Cell(613503380827930627L, 35.0, 15.0);
        cell.setElevation(300.0); // Sloped terrain
        cell.setBiome(org.ether.society.model.Biome.PLAINS);
        cell.setSoilOrganicCarbon(40.0);
        cell.setRainfall(1200.0);
        cell.setBiomassNatural(50.0); // Clear-cut biomass
        cell.setPopulation(50000); // Extreme overpopulation

        double initialSoil = cell.getSoilOrganicCarbon();

        DeforestationErosionEngine.processDeforestationErosion(List.of(cell));

        assertTrue(cell.getSoilOrganicCarbon() < initialSoil, "Soil carbon should deplete due to deforestation and erosion");
    }
}
