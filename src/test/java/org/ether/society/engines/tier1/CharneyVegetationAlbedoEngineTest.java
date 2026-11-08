/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link CharneyVegetationAlbedoEngine}.
 * Verifies analytical vegetation-albedo feedback, mid-Holocene Green Sahara collapse,
 * and 500-cell spatial grid simulation across the Saharo-Sahelian domain.
 */
public class CharneyVegetationAlbedoEngineTest {

    @Test
    @DisplayName("2.1 Analytical Vegetation Saturation & Albedo Coupling")
    public void testVegetationAlbedoCouplingFormulas() {
        // Below critical threshold (P <= 100 mm) -> Zero vegetation, desert sand albedo
        double vDry = CharneyVegetationAlbedoEngine.calculateEquilibriumVegetation(80.0);
        double albedoDry = CharneyVegetationAlbedoEngine.calculateSurfaceAlbedo(vDry);
        assertEquals(0.0, vDry, 1e-6, "Vegetation fraction must be zero below critical 100 mm threshold.");
        assertEquals(CharneyVegetationAlbedoEngine.ALBEDO_DESERT_SAND, albedoDry, 1e-4,
                "Albedo under zero vegetation must equal bare desert sand (0.38).");

        // High rainfall (P = 500 mm) -> Abundant savanna vegetation, low albedo
        double vWet = CharneyVegetationAlbedoEngine.calculateEquilibriumVegetation(500.0);
        double albedoWet = CharneyVegetationAlbedoEngine.calculateSurfaceAlbedo(vWet);
        assertTrue(vWet > 0.80, "Vegetation fraction under 500 mm rainfall must exceed 80%.");
        assertTrue(albedoWet < 0.22, "Albedo under dense vegetation must drop toward 0.16-0.20.");
    }

    @Test
    @DisplayName("2.2 Mid-Holocene Green Sahara Collapse (~-4,100 BCE): Orbital Precession Monsoonal Weakening")
    public void testGreenSaharaOrbitalCollapse() {
        // Year -7000 BCE: Early/Mid-Holocene Thermal Maximum (perihelion in boreal summer)
        double rainAnomalyAHP = CharneyVegetationAlbedoEngine.calculateOrbitalMonsoonAnomalyMm(-7000.0);

        // Year -2000 BCE: Late Holocene (weaker insolation, arid regime)
        double rainAnomalyArid = CharneyVegetationAlbedoEngine.calculateOrbitalMonsoonAnomalyMm(-2000.0);

        assertTrue(rainAnomalyAHP > 50.0,
                "Early Holocene -7000 BCE must deliver strong positive monsoonal rainfall anomaly (observed: " + rainAnomalyAHP + " mm/yr)");
        assertTrue(rainAnomalyArid < rainAnomalyAHP,
                "Orbital monsoon anomaly must substantially weaken by -2000 BCE (observed: " + rainAnomalyArid + " mm/yr)");

        // Set up a representative Saharan cell (22°N, 15°E)
        H3Cell saharanCell = new H3Cell(0x8828308280fffffL, 22.0, 15.0);
        saharanCell.setBiomassNatural(250.0); // Wet green savanna state
        saharanCell.setRainfall(350.0);
        saharanCell.setDynamicAlbedo(0.20);

        // Advance across the -4,100 BCE mid-Holocene desiccation horizon down to -2000 BCE
        List<H3Cell> domain = List.of(saharanCell);
        for (double yr = -5000.0; yr <= -2000.0; yr += 50.0) {
            CharneyVegetationAlbedoEngine.processCharneyVegetationAlbedo(domain, 50.0, yr);
        }

        assertTrue(saharanCell.getBiomassNatural() < 50.0,
                "By -2000 BCE, Green Sahara biomass must have collapsed to desert levels (observed: " + saharanCell.getBiomassNatural() + " g/m²)");
        assertTrue(saharanCell.getDynamicAlbedo() > 0.32,
                "Surface albedo must rise to high desert reflectivity (observed: " + saharanCell.getDynamicAlbedo() + ")");
        assertTrue(saharanCell.getRainfall() < 120.0,
                "Precipitation must drop below aridity threshold (observed: " + saharanCell.getRainfall() + " mm/yr)");
    }

    @Test
    @DisplayName("2.3 500-Cell Spatial Domain Verification across Saharo-Sahelian Belt")
    public void test500CellMeshSaharaSimulation() {
        List<H3Cell> mesh = new ArrayList<>();
        // Generate 500 cells: 300 within Saharo-Sahelian corridor (10°N-35°N, -20°E to +45°E) and 200 elsewhere
        for (int i = 0; i < 500; i++) {
            double lat, lon;
            if (i < 300) {
                lat = 12.0 + (20.0 * i / 299.0);
                lon = -18.0 + (58.0 * (i * 7 % 300) / 299.0);
            } else {
                lat = -40.0 + (80.0 * (i - 300) / 199.0);
                lon = 60.0 + (100.0 * ((i - 300) * 11 % 200) / 199.0);
            }
            H3Cell c = new H3Cell(0x881000000000000L + i, lat, lon);
            c.setBiome(Biome.PLAINS);
            c.setBiomassNatural(150.0);
            c.setRainfall(250.0);
            c.setDynamicAlbedo(0.25);
            mesh.add(c);
        }

        // Run simulation step for Year 2024 CE (modern hyper-arid regime)
        CharneyVegetationAlbedoEngine.processCharneyVegetationAlbedo(mesh, 25.0, 2024.0);

        // Core Sahara cell (23°N, 10°E)
        H3Cell saharanCell = mesh.stream()
                .filter(c -> c.getLatitude() > 20.0 && c.getLatitude() < 26.0 && c.getLongitude() > 5.0 && c.getLongitude() < 15.0)
                .findFirst().orElse(null);

        // Outside cell (e.g. 0°N, 100°E)
        H3Cell outsideCell = mesh.stream()
                .filter(c -> Math.abs(c.getLatitude()) < 10.0 && c.getLongitude() > 80.0)
                .findFirst().orElse(null);

        assertNotNull(saharanCell);
        assertNotNull(outsideCell);

        assertTrue(saharanCell.getDynamicAlbedo() > 0.30,
                "Saharan core cell must evolve toward high desert albedo in modern epoch.");
        assertEquals(0.25, outsideCell.getDynamicAlbedo(), 1e-4,
                "Cells outside the Saharo-Sahelian domain must remain unaltered by Charney engine.");
    }
}
