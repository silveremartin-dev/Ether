/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for H3ClimateSystem.
 * Tests temperature calculations based on latitude, elevation, and season.
 */
class H3ClimateSystemTest {

    private H3ClimateSystem climateSystem;

    @BeforeEach
    void setUp() {
        climateSystem = new H3ClimateSystem();
    }

    @Test
    @DisplayName("Equator is warmer than poles")
    void testLatitudeTemperature() {
        H3Cell equatorCell = createCell(0.0, 0.0);
        H3Cell arcticCell = createCell(70.0, 0.0);

        climateSystem.updateClimate(List.of(equatorCell, arcticCell), 6); // June

        assertTrue(equatorCell.getTemperature() > arcticCell.getTemperature(),
                "Equator should be warmer than Arctic");
    }

    @Test
    @DisplayName("Summer is warmer than winter in northern hemisphere")
    void testSeasonalTemperature() {
        H3Cell summerCell = createCell(50.0, 0.0);
        H3Cell winterCell = createCell(50.0, 10.0);

        // Summer (June)
        climateSystem.updateClimate(List.of(summerCell), 6);
        double summerTemp = summerCell.getTemperature();

        // Winter (December)
        climateSystem.updateClimate(List.of(winterCell), 11);
        double winterTemp = winterCell.getTemperature();

        assertTrue(summerTemp > winterTemp,
                "Northern hemisphere should be warmer in June than December");
    }

    @Test
    @DisplayName("Higher elevation is cooler")
    void testElevationTemperature() {
        H3Cell lowCell = createCell(45.0, 0.0);
        lowCell.setElevation(100.0);

        H3Cell highCell = createCell(45.0, 0.0);
        highCell.setElevation(3000.0);

        climateSystem.updateClimate(List.of(lowCell, highCell), 6);

        assertTrue(lowCell.getTemperature() > highCell.getTemperature(),
                "Higher elevation should be cooler (lapse rate)");

        // Verify reasonable lapse rate (~6°C per 1000m)
        double tempDifference = lowCell.getTemperature() - highCell.getTemperature();
        double elevationDifference = (3000.0 - 100.0) / 1000.0; // 2.9km
        double lapseRate = tempDifference / elevationDifference;

        assertTrue(lapseRate > 4 && lapseRate < 8,
                "Lapse rate should be approximately 6°C per 1000m");
    }

    @Test
    @DisplayName("Equatorial regions have minimal seasonal variation")
    void testEquatorialStability() {
        H3Cell equatorCell = createCell(2.0, 0.0);

        climateSystem.updateClimate(List.of(equatorCell), 6); // June
        double juneTemp = equatorCell.getTemperature();

        climateSystem.updateClimate(List.of(equatorCell), 0); // January
        double janTemp = equatorCell.getTemperature();

        double variation = Math.abs(juneTemp - janTemp);

        assertTrue(variation < 5.0,
                "Equatorial regions should have minimal seasonal variation (<5°C)");
    }

    @Test
    @DisplayName("Southern hemisphere seasons are opposite to northern")
    void testSouthernHemisphere() {
        H3Cell northCell = createCell(50.0, 0.0);
        H3Cell southCell = createCell(-50.0, 0.0);

        // June - Summer in north, winter in south
        climateSystem.updateClimate(List.of(northCell, southCell), 6);

        double northJune = northCell.getTemperature();
        double southJune = southCell.getTemperature();

        assertTrue(northJune > southJune,
                "June should be warmer in north than south");
    }

    @Test
    @DisplayName("Temperature values are within realistic range")
    void testTemperatureRange() {
        H3Cell hotCell = createCell(0.0, 0.0); // Equator, low elevation
        hotCell.setElevation(0.0);

        H3Cell coldCell = createCell(80.0, 0.0); // Arctic, high elevation
        coldCell.setElevation(2000.0);

        climateSystem.updateClimate(List.of(hotCell), 7); // August
        climateSystem.updateClimate(List.of(coldCell), 0); // January

        assertTrue(hotCell.getTemperature() < 50,
                "Temperature should not exceed 50°C");
        assertTrue(coldCell.getTemperature() > -50,
                "Temperature should not go below -50°C");
    }

    // Helper method to create test cells
    private H3Cell createCell(double lat, double lng) {
        H3Cell cell = new H3Cell();
        cell.setLatitude(lat);
        cell.setLongitude(lng);
        cell.setBiome(Biome.PLAINS);
        cell.setElevation(500.0);
        cell.setTemperature(15.0);
        cell.setRainfall(500.0);
        cell.setH3Index((long) (lat * 1000000 + lng * 1000));
        return cell;
    }
}
