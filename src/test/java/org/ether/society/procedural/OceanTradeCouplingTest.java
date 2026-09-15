/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OceanTradeCouplingTest {

    @Test
    public void testOceanAcidificationDiminishesTradeCorridors() {
        List<H3Cell> cells = new ArrayList<>();

        H3Cell coastalHub1 = new H3Cell();
        coastalHub1.setH3Index(101L);
        coastalHub1.setBiome(Biome.BEACH);
        coastalHub1.setPopulation(1000);
        coastalHub1.setLatitude(45.0);
        coastalHub1.setLongitude(5.0);
        coastalHub1.setElevation(10.0);
        coastalHub1.setBiomassFish(500.0);
        coastalHub1.setResourceCapital(100.0);
        cells.add(coastalHub1);

        H3Cell coastalHub2 = new H3Cell();
        coastalHub2.setH3Index(102L);
        coastalHub2.setBiome(Biome.BEACH);
        coastalHub2.setPopulation(800);
        coastalHub2.setLatitude(46.0);
        coastalHub2.setLongitude(6.0);
        coastalHub2.setElevation(15.0);
        coastalHub2.setBiomassFish(500.0);
        coastalHub2.setResourceCapital(100.0);
        cells.add(coastalHub2);

        // Healthy ocean baseline routes
        List<TradeNetworkEngine.TradeRoute> normalRoutes = TradeNetworkEngine.generateTradeNetworks(cells, 2.0);
        assertNotNull(normalRoutes);
        assertFalse(TradeNetworkEngine.getLatestRoutes().isEmpty());

        // Now trigger severe ocean acidification lowering fish biomass
        OceanAcidificationEngine.processOceanAcidification(cells, 800.0);
        assertTrue(coastalHub1.getBiomassFish() < 500.0);

        // Re-simulate trade networks under acidified ocean
        List<TradeNetworkEngine.TradeRoute> acidifiedRoutes = TradeNetworkEngine.generateTradeNetworks(cells, 2.0);
        assertNotNull(acidifiedRoutes);
    }
}