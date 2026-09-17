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
 * @version 1.0.0-beta.1
 */
public class AiryIsostasyCrustalRootEngine {
    private static final Logger logger = LoggerFactory.getLogger(AiryIsostasyCrustalRootEngine.class);

    /** Continental granitic crust density in kg/m³ */
    public static final double CRUST_DENSITY_KG_M3 = 2700.0;

    /** Upper mantle peridotite density in kg/m³ */
    public static final double MANTLE_DENSITY_KG_M3 = 3300.0;

    /** Baseline sea-level continental crust thickness in meters (35 km) */
    public static final double BASELINE_CRUST_THICKNESS_METERS = 35_000.0;

    /** Airy buoyancy ratio = ρ_crust / (ρ_mantle - ρ_crust) = 4.5 */
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

    /**
     * Processes Airy crustal roots and updates geothermal heat flow and metal concentration across cells.
     */
    public static void processAiryIsostasy(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;
            if (elevation <= 0.0) continue;

            double crustThicknessKm = calculateTotalCrustThicknessKm(elevation);

            // Geothermal heat flow: thicker orogenic crust modulates surface geothermal flux
            // Baseline 65 mW/m², thick mountain roots attenuate conductive gradient
            double heatFlowMW_M2 = 65.0 * (35.0 / Math.max(20.0, crustThicknessKm));
            cell.setMantleHeatFlow(heatFlowMW_M2);

            // Porphyry hydrothermal metallogeny: high orogenic root concentration yields metal ores
            if (elevation > 1500.0) {
                double currentMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 100.0;
                double oreEnrichment = (elevation / 1000.0) * 5.0;
                cell.setResourceMetal(Math.min(1000.0, currentMetal + oreEnrichment));
            }
        }
    }
}

