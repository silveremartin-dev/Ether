/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Thermohaline Ocean Circulation & Stommel 2-Box AMOC Tipping Point Engine (Henry Stommel, 1961).
 *
 * <p>Models the Atlantic Meridional Overturning Circulation (AMOC) driven by thermal and haline density gradients:</p>
 * <ul>
 *   <li><b>Equation of State for Seawater Density</b>:
 *     $$\rho(T, S) = \rho_0 \left[ 1 - \alpha_T (T - T_0) + \beta_S (S - S_0) \right]$$
 *     with thermal expansion $\alpha_T \approx 2.0 \times 10^{-4}\text{ K}^{-1}$ and haline contraction $\beta_S \approx 7.5 \times 10^{-4}\text{ PSU}^{-1}$.
 *   </li>
 *   <li><b>Stommel Non-Linear 2-Box Overturning Flow ($q_{\text{AMOC}}$ in Sverdrups, $1\text{ Sv} = 10^6\text{ m}^3/\text{s}$)</b>:
 *     $$q = C \cdot \left[ \alpha_T (T_{\text{equator}} - T_{\text{pole}}) - \beta_S (S_{\text{equator}} - S_{\text{pole}}) \right]$$
 *   </li>
 *   <li><b>Abrupt Tipping Point (Bifurcation)</b>:
 *     If meltwater discharge (e.g. from Greenland / Laurentide) freshens polar surface waters ($\Delta S_{\text{pole}} < -2.5\text{ PSU}$),
 *     the AMOC collapses ($q \to 0$ or reverses), triggering a catastrophic high-latitude cooling of $-5^\circ\text{C}$ to $-10^\circ\text{C}$
 *     (Younger Dryas / Heinrich event analogue).
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ThermohalineStommelAMOCEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermohalineStommelAMOCEngine.class);

    /** Reference seawater density in kg/m³ */
    public static final double SEAWATER_REF_DENSITY_KG_M3 = 1025.0;

    /** Seawater thermal expansion coefficient (1/K) */
    public static final double ALPHA_THERMAL = 2.0e-4;

    /** Seawater haline contraction coefficient (1/PSU) */
    public static final double BETA_HALINE = 7.5e-4;

    /** Baseline North Atlantic Overturning strength in Sverdrups (10^6 m³/s) */
    public static final double BASELINE_AMOC_SV = 18.0;

    /** Stommel coupling constant */
    public static final double STOMMEL_COUPLING_C = 5000.0;

    public record StommelState(double amocFlowSv, double northAtlanticCoolingShiftC, boolean isCollapsed) {}

    /**
     * Calculates seawater density anomaly (kg/m³) as a function of temperature (°C) and practical salinity (PSU).
     */
    public static double calculateSeawaterDensity(double tempC, double salinityPsu) {
        double deltaT = tempC - 15.0;
        double deltaS = salinityPsu - 35.0;
        return SEAWATER_REF_DENSITY_KG_M3 * (1.0 - ALPHA_THERMAL * deltaT + BETA_HALINE * deltaS);
    }

    /**
     * Calculates Stommel 2-box overturning circulation flux (in Sv) and climatic shift.
     *
     * @param tempEquatorC Equator surface temperature (~28°C)
     * @param tempPoleC Polar North Atlantic surface temperature (~2°C)
     * @param salinityEquatorPsu Equator salinity (~36.5 PSU)
     * @param salinityPolePsu Polar salinity (~34.5 PSU baseline, dropping during Heinrich meltwater events)
     * @return StommelState record
     */
    public static StommelState calculateStommelAMOC(double tempEquatorC, double tempPoleC,
                                                   double salinityEquatorPsu, double salinityPolePsu) {
        double deltaT = tempEquatorC - tempPoleC;
        double deltaS = salinityEquatorPsu - salinityPolePsu;

        // Density driving force difference
        double densityDrive = (ALPHA_THERMAL * deltaT) - (BETA_HALINE * deltaS);
        double amocFlowSv = Math.max(0.0, STOMMEL_COUPLING_C * densityDrive);

        // AMOC collapsed if flow drops below critical convective threshold (8.0 Sv)
        boolean isCollapsed = (amocFlowSv < 8.0);
        // Cooling anomaly over Europe/North Atlantic when AMOC is weakened
        double coolingShift = isCollapsed ? -7.5 : -(18.0 - amocFlowSv) * 0.35;

        return new StommelState(amocFlowSv, coolingShift, isCollapsed);
    }

    /**
     * Processes thermohaline ocean circulation and applies regional temperature shifts across oceanic/coastal cells.
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double freshwaterMeltwaterAnomalySv) {
        if (cells == null || cells.isEmpty()) return;

        // Freshwater meltwater pulse reduces polar North Atlantic salinity
        double polarSalinity = Math.max(28.0, 34.5 - (freshwaterMeltwaterAnomalySv * 1.5));
        StommelState state = calculateStommelAMOC(28.0, 2.0, 36.5, polarSalinity);

        if (state.isCollapsed()) {
            logger.info("⚠️ AMOC Thermohaline Circulation COLLAPSED! Flow = {:.2f} Sv, Cooling = {:.2f}°C",
                    state.amocFlowSv(), state.northAtlanticCoolingShiftC());
        }

        // Apply high-latitude North Atlantic / European cooling anomaly
        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // North Atlantic & Europe region: Lat 40°N to 75°N, Lng -60° to +40°
            if (lat >= 40.0 && lat <= 75.0 && lng >= -60.0 && lng <= 40.0) {
                double baseTemp = cell.getTemperature() != null ? cell.getTemperature() : 10.0;
                double regionalShift = state.northAtlanticCoolingShiftC() * Math.min(1.0, (lat - 35.0) / 25.0);
                cell.setTemperature(baseTemp + regionalShift * 0.05);
            }
        }
    }
}

