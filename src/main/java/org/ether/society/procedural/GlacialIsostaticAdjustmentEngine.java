/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Glacial Isostatic Adjustment (GIA) & Viscoelastic Post-Glacial Rebound Engine.
 *
 * <p>Models lithospheric depression under glacial ice sheets and post-glacial crustal uplift:</p>
 * <ul>
 *   <li><b>Archimedean Hydrostatic Deflection</b>: At isostatic equilibrium, lithospheric depression
 *       equals $\Delta z_{eq} = -\frac{\rho_{ice}}{\rho_{mantle}} h_{ice} \approx -\frac{917}{3300} h_{ice} \approx -0.278 h_{ice}$.</li>
 *   <li><b>Viscoelastic Maxwell Mantle Relaxation</b>:
 *     $$\tau_{gia} \frac{\partial z}{\partial t} = -(z - z_{eq}) \implies z(t + \Delta t) = z_{eq} + (z(t) - z_{eq}) \cdot \exp\left(-\frac{\Delta t}{\tau_{gia}}\right)$$
 *     with characteristic relaxation timescale $\tau_{gia} \approx 4\,000\text{ years}$.
 *   </li>
 *   <li><b>Paleogeographic Consequences</b>: Post-glacial emergence of land bridges (Beringia, Doggerland)
 *       and Baltic / Laurentian crustal uplift rates of up to $10\text{ mm/year}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.4.0
 */
public class GlacialIsostaticAdjustmentEngine {
    private static final Logger logger = LoggerFactory.getLogger(GlacialIsostaticAdjustmentEngine.class);

    /** Mantle asthenosphere density in kg/m³ */
    public static final double MANTLE_DENSITY_KG_M3 = 3300.0;

    /** Glacial ice density in kg/m³ */
    public static final double ICE_DENSITY_KG_M3 = 917.0;

    /** Viscoelastic relaxation time constant τ in years (~4,000 yr for upper mantle) */
    public static final double GIA_RELAXATION_TIME_YEARS = 4000.0;

    /**
     * Calculates isostatic equilibrium elevation depression (in meters) for an ice sheet thickness h_ice.
     */
    public static double calculateEquilibriumDeflectionMeters(double iceThicknessMeters) {
        if (iceThicknessMeters <= 0.0) return 0.0;
        return -(ICE_DENSITY_KG_M3 / MANTLE_DENSITY_KG_M3) * iceThicknessMeters;
    }

    /**
     * Processes viscoelastic crustal rebound and elevation adjustments across all H3 cells.
     *
     * @param cells List of H3 cells
     * @param deltaYears Simulation time step in years
     */
    public static void processGlacialIsostasy(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.1, deltaYears);
        double decayFactor = Math.exp(-dt / GIA_RELAXATION_TIME_YEARS);

        int adjustingCells = 0;

        for (H3Cell cell : cells) {
            Double elevation = cell.getElevation();
            if (elevation == null) continue;

            double iceThickness = cell.getIceSheetThicknessMeters() != null ? cell.getIceSheetThicknessMeters() : 0.0;
            double targetDeflection = calculateEquilibriumDeflectionMeters(iceThickness);

            // Isostatic relaxation towards target equilibrium
            double currentElevation = elevation;
            // Elevation drift towards equilibrium isostatic state
            double deltaDeflection = (targetDeflection * (1.0 - decayFactor));
            if (Math.abs(deltaDeflection) > 1e-4) {
                adjustingCells++;
                cell.setElevation(currentElevation + deltaDeflection);
            }
        }

        if (adjustingCells > 0) {
            logger.debug("Glacial Isostatic Adjustment: Relaxed {} cells over {:.1f} years", adjustingCells, deltaYears);
        }
    }
}
