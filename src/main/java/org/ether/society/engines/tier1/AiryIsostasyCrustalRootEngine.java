/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Airy-Heiskanen Tectonic Isostasy & Mountain Crustal Root Engine (George B. Airy, 1855).
 *
 * <p>Models hydrostatic buoyant equilibrium of the continental lithosphere floating atop the denser mantle:</p>
 * <ul>
 *   <li><b>Airy Isostatic Equilibrium Condition</b>:
 *     $$h_{\text{root}} = h_{\text{topo}} \cdot \frac{\rho_{\text{crust}}}{\rho_{\text{mantle}} - \rho_{\text{crust}}}$$
 *     with $\rho_{\text{crust}} \approx 2700\text{ kg/m}^3$ (granitic upper crust) and $\rho_{\text{mantle}} \approx 3300\text{ kg/m}^3$ (peridotite).
 *     $$\frac{\rho_{\text{crust}}}{\rho_{\text{mantle}} - \rho_{\text{crust}}} = \frac{2700}{3300 - 2700} = \frac{2700}{600} = 4.5$$
 *     A mountain peak of $4\,000\text{ m}$ elevation possesses a deep crustal root of $18\,000\text{ m}$ ($18\text{ km}$).
 *   </li>
 *   <li><b>Total Crustal Thickness ($T_{\text{crust}}$)</b>: $T_{\text{crust}} = T_0 + h_{\text{topo}} + h_{\text{root}} = 35\text{ km} + 5.5 \cdot h_{\text{topo}}$.</li>
 *   <li><b>Geothermal & Metallogenic Implications</b>: Deep crustal roots insulate mantle heat flux ($Q_{\text{mantle}} \propto 1 / T_{\text{crust}}$)
 *       and generate hydrothermal porphyry metal deposits via deep magma differentiation.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AiryIsostasyCrustalRootEngine extends AbstractSimulationEngine {
    public AiryIsostasyCrustalRootEngine() {
        super("AiryIsostasyCrustalRootEngine", "AiryIsostasyCrustalRootEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(AiryIsostasyCrustalRootEngine.class);

    /** Continental granitic crust density in kg/mÂ³ */
    public static final double CRUST_DENSITY_KG_M3 = 2700.0;

    /** Upper mantle peridotite density in kg/mÂ³ */
    public static final double MANTLE_DENSITY_KG_M3 = 3300.0;

    /** Baseline sea-level continental crust thickness in meters (35 km) */
    public static final double BASELINE_CRUST_THICKNESS_METERS = 35_000.0;

    /** Airy buoyancy ratio = Ï_crust / (Ï_mantle - Ï_crust) = 4.5 */
    public static final double AIRY_ROOT_RATIO = CRUST_DENSITY_KG_M3 / (MANTLE_DENSITY_KG_M3 - CRUST_DENSITY_KG_M3);

    /**
     * Calculates mountain root thickness (in meters) penetrating into the mantle for a given topography elevation.
     *
     * @param elevationMeters Topographic elevation in meters
     * @return Crustal root depth in meters
     */
    public static double calculateMountainRootDepthMeters(double elevationMeters) {
        if (elevationMeters <= 0.0) return 0.0;
        return elevationMeters * AIRY_ROOT_RATIO;
    }

    /**
     * Calculates total lithospheric crustal thickness (in km) at a given topographic elevation.
     */
    public static double calculateTotalCrustThicknessKm(double elevationMeters) {
        double rootMeters = calculateMountainRootDepthMeters(elevationMeters);
        double topoMeters = Math.max(0.0, elevationMeters);
        return (BASELINE_CRUST_THICKNESS_METERS + topoMeters + rootMeters) / 1000.0;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double elevation = buffer.getElevation()[i];
        if (elevation <= 0.0) return;

        if (elevation > 1500.0) {
            double currentMetal = buffer.getMetalResource()[i];
            double orogenicOreCap = Math.min(1000.0, 100.0 + (elevation / 1000.0) * 150.0);
            if (orogenicOreCap > currentMetal) {
                acc.dMetalResource += (float) ((orogenicOreCap - currentMetal) * Math.min(1.0, dtYears * 0.01));
            }
        }
    }
}
