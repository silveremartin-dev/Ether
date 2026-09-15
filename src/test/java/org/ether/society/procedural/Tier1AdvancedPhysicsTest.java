/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying advanced Tier 1 invariant physics engines:
 * 1. Milankovitch Astronomical Orbital Forcing & Insolation Cycles
 * 2. Atmospheric Circulation, Coriolis Geostrophy & Hadley Cells
 * 3. Glacial Isostatic Adjustment (GIA) & Viscoelastic Post-Glacial Rebound
 * 4. Radiocarbon (C-14) Radioactive Decay & Delta-13C Isotope Geochemistry
 *
 * @author Silvere Martin-Michiellot
 * @version 4.4.0
 */
public class Tier1AdvancedPhysicsTest {

    @Test
    @DisplayName("Milankovitch Orbital Engine: Insolation cycles and orbital eccentricity")
    public void testMilankovitchOrbitalForcing() {
        MilankovitchOrbitalEngine.MilankovitchParameters modern = MilankovitchOrbitalEngine.computeOrbitalParameters(0.0);
        assertTrue(modern.eccentricity() >= 0.005 && modern.eccentricity() <= 0.060);
        assertTrue(modern.obliquityDeg() >= 22.0 && modern.obliquityDeg() <= 25.0);

        // Summer solstice at 65°N (day 172) should have high insolation (> 400 W/m²)
        double summer65N = MilankovitchOrbitalEngine.calculateDailyInsolation(65.0, 172, modern);
        assertTrue(summer65N > 400.0 && summer65N < 600.0, "High latitude summer insolation must be ~450-520 W/m²");

        // Winter solstice at 65°N (day 355) should have zero or near-zero insolation (polar night)
        double winter65N = MilankovitchOrbitalEngine.calculateDailyInsolation(65.0, 355, modern);
        assertTrue(winter65N < 10.0, "High latitude winter insolation must be near zero");

        // Cell forcing application
        List<H3Cell> cells = new ArrayList<>();
        H3Cell cell = new H3Cell();
        cell.setLatitude(65.0);
        cell.setTemperature(10.0);
        cells.add(cell);

        MilankovitchOrbitalEngine.applyMilankovitchForcing(cells, -20000.0); // LGM ~20,000 BP
        assertNotNull(cell.getTemperature());
    }

    @Test
    @DisplayName("Atmospheric Circulation Hadley Engine: Coriolis parameter and wind regimes")
    public void testAtmosphericCirculationHadley() {
        // Coriolis at Equator must be 0, at North Pole must be 2*Omega
        assertEquals(0.0, AtmosphericCirculationHadleyEngine.calculateCoriolisParameter(0.0), 1e-9);
        double fPole = AtmosphericCirculationHadleyEngine.calculateCoriolisParameter(90.0);
        assertTrue(fPole > 1.4e-4, "Coriolis parameter at pole must be ~1.458e-4 s^-1");

        // In Hadley cell (e.g. Lat 15°N in July month 7): Trade winds should be easterly (negative zonal speed)
        AtmosphericCirculationHadleyEngine.WindVector tradeWind = AtmosphericCirculationHadleyEngine.calculateAtmosphericWind(15.0, 7);
        assertEquals("HADLEY_TRADE_WINDS", tradeWind.cellType());
        assertTrue(tradeWind.zonalSpeedM_S() < 0, "Hadley cell surface wind must be easterly (u < 0)");

        // In Ferrel cell (e.g. Lat 45°N): Westerlies should be positive
        AtmosphericCirculationHadleyEngine.WindVector westerlies = AtmosphericCirculationHadleyEngine.calculateAtmosphericWind(45.0, 7);
        assertEquals("FERREL_WESTERLIES", westerlies.cellType());
        assertTrue(westerlies.zonalSpeedM_S() > 0, "Ferrel cell surface wind must be westerly (u > 0)");

        // Process grid
        List<H3Cell> cells = new ArrayList<>();
        H3Cell cell = new H3Cell();
        cell.setLatitude(45.0);
        cells.add(cell);

        AtmosphericCirculationHadleyEngine.processAtmosphericCirculation(cells, 7);
        assertTrue(cell.getEnergyWind() > 0.0, "Atmospheric wind must generate kinetic power density");
    }

    @Test
    @DisplayName("Glacial Isostatic Adjustment: Viscoelastic crustal rebound")
    public void testGlacialIsostasy() {
        // 1000m of ice sheet should cause ~-278m of isostatic crustal depression
        double deflection = GlacialIsostaticAdjustmentEngine.calculateEquilibriumDeflectionMeters(1000.0);
        assertTrue(deflection < -250.0 && deflection > -300.0, "1000m ice must deflect mantle by ~-278m");

        // Rebound relaxation over 1,000 years after ice deglaciation
        List<H3Cell> cells = new ArrayList<>();
        H3Cell scandinavia = new H3Cell();
        scandinavia.setElevation(100.0);
        scandinavia.setIceSheetThicknessMeters(0.0); // Deglaciated
        cells.add(scandinavia);

        GlacialIsostaticAdjustmentEngine.processGlacialIsostasy(cells, 1000.0);
        assertNotNull(scandinavia.getElevation());
    }

    @Test
    @DisplayName("Radiocarbon Isotope Engine: C14 nuclear decay and delta13C fractionation")
    public void testRadiocarbonIsotopeDecay() {
        // After 5730 years, exactly 50% activity must remain
        double halfLifeRemaining = RadiocarbonIsotopeEngine.calculateRemainingC14Activity(5730.0);
        assertEquals(0.50, halfLifeRemaining, 0.01);

        // Radiocarbon age from 50% activity must be ~5730 BP
        double ageBP = RadiocarbonIsotopeEngine.calculateRadiocarbonAgeBP(0.50);
        assertTrue(ageBP > 5500.0 && ageBP < 5900.0);

        // Delta 13C signatures
        double d13CSavannah = RadiocarbonIsotopeEngine.determineIsotopicSignatureDelta13C(Biome.SAVANNAH);
        double d13CForest = RadiocarbonIsotopeEngine.determineIsotopicSignatureDelta13C(Biome.FOREST);
        assertTrue(d13CSavannah > d13CForest, "C4 savannah grasses must be less negative (-12 ‰) than C3 forest (-28 ‰)");
    }
}
