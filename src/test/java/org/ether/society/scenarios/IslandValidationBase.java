/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot & Gemini AI
 */
package org.ether.society.scenarios;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base fixture for island cliodynamic validation suites providing H3 hexagonal grid initializers.
 */
public abstract class IslandValidationBase {
    protected static final Logger logger = LoggerFactory.getLogger(IslandValidationBase.class);
    protected H3Core h3;
    protected Configuration config;

    @BeforeEach
    public void setUp() throws Exception {
        h3 = H3Core.newInstance();
        config = ConfigurationLoader.loadDefault();
    }

    protected List<H3Cell> createContiguousIsland(double centerLat, double centerLng, int h3Resolution, int ringRadius, double initialPopPerCell) {
        return createContiguousIsland(centerLat, centerLng, h3Resolution, ringRadius, initialPopPerCell, 1.5);
    }

    protected List<H3Cell> createContiguousIsland(double centerLat, double centerLng, int h3Resolution, int ringRadius, double initialPopPerCell, double techLevel) {
        long center = h3.latLngToCell(centerLat, centerLng, h3Resolution);
        List<Long> diskH3 = h3.gridDisk(center, ringRadius);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(h3Resolution) / 1770.49;
        List<H3Cell> cells = new ArrayList<>(diskH3.size());
        for (Long idx : diskH3) {
            LatLng coord = h3.cellToLatLng(idx);
            H3Cell cell = new H3Cell(idx, coord.lat, coord.lng);
            cell.setElevation(250.0);
            cell.setTemperature(22.0);
            cell.setRainfall(1100.0);
            cell.setBiome(Biome.FOREST);
            cell.setFoodResource(3500.0 * areaScale);
            cell.setWaterResource(2000.0 * areaScale);
            cell.setWoodResource(6000.0 * areaScale);
            cell.setBiomassNatural(6000.0 * areaScale);
            cell.setResourceMetal(500.0 * areaScale);
            cell.setResourceClay(800.0 * areaScale);
            cell.setPopulation((int) initialPopPerCell);
            cell.setBiomassHuman(initialPopPerCell);
            cell.setTechnologyLevel(techLevel);
            cell.setResourceCapital(initialPopPerCell * (techLevel < 1.5 ? 2.0 : 15.0));
            cell.setLifespan(techLevel < 1.5 ? 35.0 : 48.0);
            cell.setFertility(techLevel < 1.5 ? 2.5 : 3.2);
            cell.setBiomassAgriculture((techLevel >= 1.5) ? Math.min(3000.0 * areaScale, initialPopPerCell * 3.5 * areaScale) : 0.0);
            cells.add(cell);
        }
        return cells;
    }
}