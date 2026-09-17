/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.tier2.ArthurCombinatorialTechnologyEngine;
import org.ether.society.procedural.tier2.BoserupAgriculturalIntensificationEngine;
import org.ether.society.procedural.tier2.PriceMultilevelSelectionEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying advanced Tier 2 cliodynamic plugins:
 * 1. Price Multilevel Cultural Selection Engine
 * 2. Boserupian Agricultural Intensification Engine
 * 3. Arthur Combinatorial Technology Evolution Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class Tier2AdvancedCliodynamicsTest {

    @Test
    @DisplayName("Price Equation Engine: Multilevel cultural selection on civic altruism")
    public void testPriceMultilevelSelection() {
        PriceMultilevelSelectionEngine engine = new PriceMultilevelSelectionEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Price"));

        List<H3Cell> cells = new ArrayList<>();
        H3Cell cooperativePolity = new H3Cell();
        cooperativePolity.setPopulation(1000);
        cooperativePolity.setGiniIndex(0.20); // Low inequality -> high cooperation
        cooperativePolity.setResourceCapital(500.0);
        cooperativePolity.setResourceWork(100.0);
        cells.add(cooperativePolity);

        double workBefore = cooperativePolity.getResourceWork();
        engine.process(cells, 1.0);
        double workAfter = cooperativePolity.getResourceWork();

        assertTrue(workAfter > workBefore, "High civic prosociality must increase effective collective work output");
    }

    @Test
    @DisplayName("Boserup Intensification Engine: Demographic pressure triggering agricultural shifts")
    public void testBoserupIntensification() {
        BoserupAgriculturalIntensificationEngine engine = new BoserupAgriculturalIntensificationEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Boserup"));

        List<H3Cell> cells = new ArrayList<>();
        H3Cell denseDelta = new H3Cell();
        denseDelta.setPopulation(100_000); // Very high population density
        denseDelta.setBiomassAgriculture(100.0);
        denseDelta.setResourceWork(500.0);
        cells.add(denseDelta);

        double agriBefore = denseDelta.getBiomassAgriculture();
        engine.process(cells, 1.0);
        double agriAfter = denseDelta.getBiomassAgriculture();

        assertTrue(agriAfter > agriBefore, "High demographic density must force agricultural yield intensification");
    }

    @Test
    @DisplayName("Arthur Combinatorial Technology Engine: Recombinant innovation dynamics")
    public void testArthurCombinatorialTechnology() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Arthur"));

        List<H3Cell> cells = new ArrayList<>();
        H3Cell researchHub = new H3Cell();
        researchHub.setPopulation(50_000);
        researchHub.setTechnologyLevel(5.0);
        researchHub.setResourceCapital(10_000.0);
        cells.add(researchHub);

        double techBefore = researchHub.getTechnologyLevel();
        engine.process(cells, 1.0);
        double techAfter = researchHub.getTechnologyLevel();

        assertTrue(techAfter > techBefore, "Combinatorial tech space must accelerate technological progress");
    }
}

