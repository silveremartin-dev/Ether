/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Thermohaline Ocean Circulation &amp; Stommel (1961) 2-Box Overturning Engine.
 * <p>
 * Models meridional thermohaline overturning circulation ($q_{\text{AMOC}}$ in Sverdrups) and its
 * smooth ($C^\infty$) convective weakening cooling response across high-latitude maritime/western-margin
 * regions ({@code AGENTS.md} §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThermohalineStommelAMOCEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermohalineStommelAMOCEngine.class);

    public static final double SEAWATER_REF_DENSITY_KG_M3 = 1025.0;
    public static final double ALPHA_THERMAL = 2.0e-4;
    public static final double BETA_HALINE = 7.5e-4;
    public static final double BASELINE_AMOC_SV = 18.0;
    public static final double STOMMEL_COUPLING_C = 5000.0;

    /**
     * Thermohaline overturning state.
     *
     * @param amocFlowSv                 overturning volume flux in Sverdrups ($10^6\,\text{m}^3/\text{s}$)
     * @param northAtlanticCoolingShiftC smooth high-latitude thermal anomaly (°C)
     * @param isCollapsed                diagnostic flag indicating $q_{\text{AMOC}} < 8.0\,\text{Sv}$
     */
    public record StommelState(double amocFlowSv, double northAtlanticCoolingShiftC, boolean isCollapsed) {}

    /**
     * Calculates seawater density ($\text{kg/m}^3$) from temperature (°C) and practical salinity (PSU).
     */
    public static double calculateSeawaterDensity(double tempC, double salinityPsu) {
        double deltaT = tempC - 15.0;
        double deltaS = salinityPsu - 35.0;
        return SEAWATER_REF_DENSITY_KG_M3 * (1.0 - ALPHA_THERMAL * deltaT + BETA_HALINE * deltaS);
    }

    /**
     * Calculates Stommel 2-box overturning circulation flux (Sv) and smooth ($C^\infty$) convective cooling shift.
     */
    public static StommelState calculateStommelAMOC(double tempEquatorC, double tempPoleC,
                                                    double salinityEquatorPsu, double salinityPolePsu) {
        double deltaT = tempEquatorC - tempPoleC;
        double deltaS = salinityEquatorPsu - salinityPolePsu;

        double densityDrive = (ALPHA_THERMAL * deltaT) - (BETA_HALINE * deltaS);
        double amocFlowSv = SmoothMath.softplus(STOMMEL_COUPLING_C * densityDrive, 2.0);

        boolean isCollapsed = (amocFlowSv < 8.0);
        // Smooth C^infty cooling curve transitioning from linear weakening -(18 - q)*0.35 to deep convective shutdown (-8.0 C)
        double collapseWeight = 1.0 - SmoothMath.sigmoid(amocFlowSv, 8.0, 1.5);
        double linearWeakening = -SmoothMath.softplus(BASELINE_AMOC_SV - amocFlowSv, 1.5) * 0.35;
        double coolingShift = (1.0 - collapseWeight) * linearWeakening + collapseWeight * (-8.0);

        return new StommelState(amocFlowSv, coolingShift, isCollapsed);
    }

    /** Executes one monthly thermohaline circulation step. */
    public static void processThermohalineCirculation(List<H3Cell> cells, double freshwaterMeltwaterAnomalySv) {
        processThermohalineCirculation(cells, freshwaterMeltwaterAnomalySv, 1.0 / 12.0);
    }

    /**
     * Processes thermohaline circulation and applies smooth latitudinal/basin cooling relaxation.
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double freshwaterMeltwaterAnomalySv, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double polarSalinity = SmoothMath.softmax(28.0, 34.5 - (freshwaterMeltwaterAnomalySv * 1.5), 2.0);
        StommelState state = calculateStommelAMOC(28.0, 2.0, 36.5, polarSalinity);

        if (state.isCollapsed()) {
            logger.info("AMOC Thermohaline Circulation COLLAPSED: Flow = {} Sv, Cooling = {} C",
                    String.format(java.util.Locale.ROOT, "%.2f", state.amocFlowSv()),
                    String.format(java.util.Locale.ROOT, "%.2f", state.northAtlanticCoolingShiftC()));
        }

        double dtSafe = Math.max(0.0001, deltaYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 2.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // Smooth C^infty high-latitude subpolar gyre envelope (40°N-75°N, -60°E to +40°E) without hard boundary cliff
            double latWeight = SmoothMath.sigmoid(lat, 40.0, 4.0) * (1.0 - SmoothMath.sigmoid(lat, 75.0, 4.0));
            double lonWeight = SmoothMath.sigmoid(lng, -60.0, 8.0) * (1.0 - SmoothMath.sigmoid(lng, 40.0, 8.0));
            double spatialEnvelope = latWeight * lonWeight;

            if (spatialEnvelope > 1e-3) {
                double curTemp = cell.getTemperatureOr(10.0);
                double elev = cell.getElevationOr(0.0);
                double baselineTemp = 28.0 - 0.0065 * Math.max(0.0, elev) - 0.45 * Math.abs(lat);
                double regionalShift = state.northAtlanticCoolingShiftC() * spatialEnvelope * Math.tanh(Math.max(0.0, lat - 35.0) / 25.0);
                double targetTemp = baselineTemp + regionalShift;
                cell.setTemperature(curTemp + (targetTemp - curTemp) * thermalRelaxationFactor * spatialEnvelope);
            }
        }
    }
}
