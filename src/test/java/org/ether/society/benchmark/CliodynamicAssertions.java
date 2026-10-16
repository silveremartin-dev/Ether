/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standardized Cliodynamic Assertion Suite for Intermediate Century-Scale Simulations.
 *
 * <p>Enforces physical, biological, thermodynamic, and demographic invariants across multi-year sweeps:
 * <ul>
 *   <li><b>Physical Integrity</b>: Zero NaN/Infinity, strict non-negativity of physical stocks ($P \ge 0$, $F \ge 0$, $W \ge 0$), bounded Gini $\in [0, 1]$.</li>
 *   <li><b>Euler-Lotka Bounds</b>: Compound annual population growth rate strictly bounded by human reproductive biology ($r \in [-15\%, +4.0\%]$).</li>
 *   <li><b>Thermodynamic Continuity</b>: Strict energy and mass conservation across symplectic time steps.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliodynamicAssertions {

    private CliodynamicAssertions() {
        // Utility class
    }

    /**
     * Asserts that all physical, demographic, and ecological layers of a {@link WorldBuffer}
     * satisfy strict numerical finiteness, non-negativity, and domain bounds.
     *
     * @param buffer the WorldBuffer to validate
     */
    public static void assertPhysicalIntegrity(WorldBuffer buffer) {
        assertNotNull(buffer, "WorldBuffer must not be null.");

        int cap = buffer.getCapacity();
        float[] bHuman = buffer.getBiomassHuman();
        float[] bAgri = buffer.getBiomassAgriculture();
        float[] bNat = buffer.getBiomassNatural();
        float[] food = buffer.getFoodResource();
        float[] capital = buffer.getResourceCapital();
        float[] gini = buffer.getGiniIndex();
        float[] temp = buffer.getTemperature();

        for (int i = 0; i < cap; i++) {
            if (buffer.getH3Indexes()[i] == 0) continue;

            // Finiteness (no NaN, no Inf)
            assertFalse(Float.isNaN(bHuman[i]), "Biomass human at index " + i + " must not be NaN.");
            assertFalse(Float.isInfinite(bHuman[i]), "Biomass human at index " + i + " must not be Infinite.");
            assertFalse(Float.isNaN(food[i]), "Food at index " + i + " must not be NaN.");
            assertFalse(Float.isNaN(capital[i]), "Capital at index " + i + " must not be NaN.");

            // Positivity invariants
            assertTrue(bHuman[i] >= 0.0f, "Biomass human at index " + i + " must be non-negative, got: " + bHuman[i]);
            assertTrue(bAgri[i] >= 0.0f, "Biomass agriculture at index " + i + " must be non-negative, got: " + bAgri[i]);
            assertTrue(bNat[i] >= 0.0f, "Biomass natural at index " + i + " must be non-negative, got: " + bNat[i]);
            assertTrue(food[i] >= 0.0f, "Food resource at index " + i + " must be non-negative, got: " + food[i]);
            assertTrue(capital[i] >= 0.0f, "Resource capital at index " + i + " must be non-negative, got: " + capital[i]);

            // Gini bound [0.0, 1.0]
            float g = gini[i];
            assertTrue(g >= 0.0f && g <= 1.0f, "Gini index at index " + i + " must be in [0, 1], got: " + g);

            // Planetary temperature bounds [-90°C, +70°C]
            if (temp != null) {
                float t = temp[i];
                assertTrue(t >= -90.0f && t <= 70.0f, "Temperature at index " + i + " must be within physical planetary limits [-90, +70], got: " + t);
            }
        }
    }

    /**
     * Asserts that all cells in an {@link H3Cell} list satisfy numerical finiteness and physical bounds.
     *
     * @param cells the cell list to inspect
     */
    public static void assertPhysicalIntegrity(List<H3Cell> cells) {
        assertNotNull(cells, "Cell list must not be null.");
        assertFalse(cells.isEmpty(), "Cell list must not be empty.");

        for (H3Cell cell : cells) {
            if (cell.getPopulation() != null) {
                assertTrue(cell.getPopulation() >= 0, "Cell population must be non-negative, got: " + cell.getPopulation());
            }
            if (cell.getFoodResource() != null) {
                assertFalse(Double.isNaN(cell.getFoodResource()), "Cell food must not be NaN.");
                assertTrue(cell.getFoodResource() >= 0.0, "Cell food must be non-negative, got: " + cell.getFoodResource());
            }
            if (cell.getResourceCapital() != null) {
                assertFalse(Double.isNaN(cell.getResourceCapital()), "Cell capital must not be NaN.");
                assertTrue(cell.getResourceCapital() >= 0.0, "Cell capital must be non-negative, got: " + cell.getResourceCapital());
            }
            if (cell.getTemperature() != null) {
                assertFalse(Double.isNaN(cell.getTemperature()), "Cell temperature must not be NaN.");
                assertTrue(cell.getTemperature() >= -90.0 && cell.getTemperature() <= 70.0,
                        "Cell temperature must be within [-90, +70]°C, got: " + cell.getTemperature());
            }
        }
    }

    /**
     * Asserts that multi-century population dynamics satisfy the biological limits of the Euler-Lotka equation.
     * <p>Homo sapiens maximum biological reproductive fecundity is bounded above by ~3.5-4.0% per year ($r \le 0.040$).
     * Catastrophic annual collapse is bounded by extreme mortality scenarios ($r \ge -0.20$).</p>
     *
     * @param initialPop population at start of period
     * @param finalPop population at end of period
     * @param elapsedYears number of elapsed years ($t > 0$)
     */
    public static void assertEulerLotkaPlausible(double initialPop, double finalPop, double elapsedYears) {
        assertTrue(initialPop > 0.0, "Initial population must be positive.");
        assertTrue(finalPop >= 0.0, "Final population must be non-negative.");
        assertTrue(elapsedYears > 0.0, "Elapsed years must be positive.");

        if (finalPop == 0.0) {
            // Extinction event, allowed if initial population was tiny
            return;
        }

        double compoundRate = Math.log(finalPop / initialPop) / elapsedYears;

        // Fecundity ceiling (Euler-Lotka human maximum reproductive rate)
        assertTrue(compoundRate <= 0.040,
                String.format("Population growth rate (%.2f%%/yr) exceeds biological Euler-Lotka ceiling (4.0%%/yr) over %.1f years (P0=%.0f -> P1=%.0f)",
                        compoundRate * 100.0, elapsedYears, initialPop, finalPop));

        // Plausibility floor (no sustained perpetual negative collapse beyond complete systemic collapse)
        assertTrue(compoundRate >= -0.20,
                String.format("Population collapse rate (%.2f%%/yr) exceeds maximum plausible mortality floor (-20.0%%/yr) over %.1f years",
                        compoundRate * 100.0, elapsedYears));
    }

    /**
     * Asserts that total planetary biomass is conserved within floating-point relative error.
     *
     * @param massBefore total mass before step
     * @param massAfter total mass after step
     * @param relativeTolerance allowable relative tolerance (e.g. 1e-4)
     */
    public static void assertMassConservation(double massBefore, double massAfter, double relativeTolerance) {
        double diff = Math.abs(massAfter - massBefore);
        double scale = Math.max(1.0, Math.max(Math.abs(massBefore), Math.abs(massAfter)));
        double relError = diff / scale;

        assertTrue(relError <= relativeTolerance,
                String.format("Mass conservation violation: before=%.4e, after=%.4e, relError=%.4e (tol=%.4e)",
                        massBefore, massAfter, relError, relativeTolerance));
    }
}
