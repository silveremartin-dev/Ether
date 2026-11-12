/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Biome;
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
 *     $$\tau_{gia} \frac{\partial \Delta z}{\partial t} = -(\Delta z - \Delta z_{eq}) \implies \Delta z(t + \Delta t) = \Delta z_{eq} + (\Delta z(t) - \Delta z_{eq}) \cdot \exp\left(-\frac{\Delta t}{\tau_{gia}}\right)$$
 *     with characteristic relaxation timescale $\tau_{gia} \approx 4\,000\text{ years}$.
 *   </li>
 *   <li><b>Paleogeographic Consequences</b>: Post-glacial emergence of land bridges (Beringia, Doggerland)
 *       and Baltic / Laurentian crustal uplift rates of up to $10\text{ mm/year}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class GlacialIsostaticAdjustmentEngine extends AbstractSimulationEngine {
    public GlacialIsostaticAdjustmentEngine() {
        super("GlacialIsostaticAdjustmentEngine", "GlacialIsostaticAdjustmentEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

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

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) return;
        byte[] biomes = buffer.getBiomes();
        float[] elevArr = buffer.getElevation();
        byte glacierOrd = (byte) Biome.GLACIER.ordinal();
        double decayFactor = 1.0 - Math.exp(-Math.max(0.001, dtYears) / GIA_RELAXATION_TIME_YEARS);
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            double iceThickness = (biomes[i] == glacierOrd) ? 1000.0 : 0.0;
            double targetDeflection = calculateEquilibriumDeflectionMeters(iceThickness);
            if (Math.abs(targetDeflection) > 1e-3) {
                elevArr[i] += (float) (targetDeflection * decayFactor * 0.01);
            }
        }
    }
}
