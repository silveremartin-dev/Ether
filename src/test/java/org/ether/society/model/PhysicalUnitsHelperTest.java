/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dimensional unit tests for {@link PhysicalUnitsHelper} verifying exact SI conversion identities
 * and round-trip reversibility across time, energy, power, mass, temperature, length, area, and pressure.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public class PhysicalUnitsHelperTest {

    @Test
    @DisplayName("Time conversions: days, Julian years, months, and SI seconds are exact")
    void testTimeConversions() {
        assertEquals(1.0, PhysicalUnitsHelper.daysToYears(365.25), 1e-12);
        assertEquals(365.25, PhysicalUnitsHelper.yearsToDays(1.0), 1e-12);
        assertEquals(86_400.0, PhysicalUnitsHelper.daysToSeconds(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.secondsToDays(86_400.0), 1e-12);
        assertEquals(31_557_600.0, PhysicalUnitsHelper.yearsToSeconds(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.secondsToYears(31_557_600.0), 1e-12);
        assertEquals(12.0, PhysicalUnitsHelper.yearsToMonths(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.monthsToYears(12.0), 1e-12);
    }

    @Test
    @DisplayName("Power and Energy conversions: GJ/yr <-> W and MJ/GJ/J/MW/TW are exact")
    void testEnergyAndPowerConversions() {
        double expectedWattsPerGjYr = 1.0e9 / 31_557_600.0;
        assertEquals(expectedWattsPerGjYr, PhysicalUnitsHelper.gjPerYearToWatts(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.wattsToGjPerYear(expectedWattsPerGjYr), 1e-12);

        // Human basal metabolic rate: 3.362 GJ/yr ≈ 106.535 W
        double humanWatts = PhysicalUnitsHelper.gjPerYearToWatts(PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
        assertEquals(106.53535, humanWatts, 1e-4);
        assertEquals(PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ,
                PhysicalUnitsHelper.wattsAndYearsToGigajoules(humanWatts, 1.0), 1e-12);
        assertEquals(humanWatts,
                PhysicalUnitsHelper.gigajoulesOverYearsToWatts(PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ, 1.0), 1e-12);

        assertEquals(1.0, PhysicalUnitsHelper.joulesToGigajoules(1.0e9), 1e-12);
        assertEquals(1.0e9, PhysicalUnitsHelper.gigajoulesToJoules(1.0), 1e-6);
        assertEquals(1.0, PhysicalUnitsHelper.megajoulesToGigajoules(1_000.0), 1e-12);
        assertEquals(1_000.0, PhysicalUnitsHelper.gigajoulesToMegajoules(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.wattsToMegawatts(1.0e6), 1e-12);
        assertEquals(1.0e6, PhysicalUnitsHelper.megawattsToWatts(1.0), 1e-6);
        assertEquals(1.0, PhysicalUnitsHelper.wattsToTerawatts(1.0e12), 1e-12);
        assertEquals(1.0e12, PhysicalUnitsHelper.terawattsToWatts(1.0), 1e-3);
    }

    @Test
    @DisplayName("Mass, biomass energy density, temperature, length, area, pressure, and bits conversions")
    void testMassTempGeometryConversions() {
        assertEquals(1.0, PhysicalUnitsHelper.kgToMetricTonnes(1_000.0), 1e-12);
        assertEquals(1_000.0, PhysicalUnitsHelper.metricTonnesToKg(1.0), 1e-12);

        // 1,000 kg (1 t) of dry grain at 15 MJ/kg == 15 GJ
        assertEquals(15.0, PhysicalUnitsHelper.biomassKgToEnergyGJ(1_000.0, 15.0), 1e-12);
        assertEquals(15.0, PhysicalUnitsHelper.biomassTonnesToEnergyGJ(1.0, 15.0), 1e-12);

        assertEquals(298.15, PhysicalUnitsHelper.celsiusToKelvin(25.0), 1e-12);
        assertEquals(25.0, PhysicalUnitsHelper.kelvinToCelsius(298.15), 1e-12);

        assertEquals(1_000.0, PhysicalUnitsHelper.kmToMeters(1.0), 1e-12);
        assertEquals(1.0, PhysicalUnitsHelper.metersToKm(1_000.0), 1e-12);
        assertEquals(1.0e6, PhysicalUnitsHelper.sqKmToSqMeters(1.0), 1e-6);
        assertEquals(1.0, PhysicalUnitsHelper.sqMetersToSqKm(1.0e6), 1e-12);
        assertEquals(0.001, PhysicalUnitsHelper.mmToMeters(1.0), 1e-12);
        assertEquals(101_325.0, PhysicalUnitsHelper.millibarsToPascals(1013.25), 1e-12);
        assertEquals(Math.log(2.0), PhysicalUnitsHelper.bitsToNats(1.0), 1e-12);
    }
}
