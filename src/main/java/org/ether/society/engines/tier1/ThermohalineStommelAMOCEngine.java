/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Dynamic Thermohaline Circulation &amp; Stommel (1961) 2-Box Overturning Engine.
 *
 * <p>Models the non-linear dynamics, hysteresis, and bistability of the Atlantic Meridional
 * Overturning Circulation (AMOC) across deep-time timescales (from $-3\,000\,000$ BP to $+100\,000$ CE)
 * based on the canonical two-box formulation of Henry Stommel (1961):</p>
 *
 * <p><b>Stommel 2-Box Coupled Differential Equations:</b>
 * $$\frac{dT_p}{dt} = \frac{T_p^* - T_p}{\tau_T} + |q| \frac{T_e - T_p}{V_p}$$
 * $$\frac{dS_p}{dt} = \frac{S_p^* - S_p}{\tau_S} + |q| \frac{S_e - S_p}{V_p} - \frac{F_{\text{melt}}}{V_p}$$
 * </p>
 *
 * <p>where:
 * <ul>
 *   <li>$T_p, S_p$: Subpolar North Atlantic polar box temperature (Â°C) and salinity (PSU).</li>
 *   <li>$T_e, S_e$: Equatorial / Tropical Atlantic box temperature ($28.0^\circ\text{C}$) and salinity ($36.5\text{ PSU}$).</li>
 *   <li>$\tau_T \approx 20\text{ years}$: Fast atmospheric thermal relaxation timescale.</li>
 *   <li>$\tau_S \approx 300\text{ years}$: Slow haline diffusion timescale providing multi-centennial memory.</li>
 *   <li>$q$: Volume overturning flux in Sverdrups ($10^6\,\text{m}^3/\text{s}$), $q = C (\alpha \Delta T - \beta \Delta S)$.</li>
 *   <li>$F_{\text{melt}}$: Freshwater meltwater discharge flux (Sv), driven by Milankovitch orbital insolation at 65Â°N and greenhouse anomalies.</li>
 * </ul>
 * </p>
 *
 * <p><b>Bipolar Seesaw Response (Broecker 1998, EPICA 2006):</b>
 * An AMOC collapse deprives the North Atlantic and Western Europe of northward heat transport ($\Delta T_{\text{North}} \approx -8.0^\circ\text{C}$),
 * while heat accumulation in the Southern Ocean triggers a compensatory warming ($\Delta T_{\text{South}} \approx +1.5\text{ to }+2.0^\circ\text{C}$).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThermohalineStommelAMOCEngine extends AbstractSimulationEngine {
    public ThermohalineStommelAMOCEngine() {
        super("ThermohalineStommelAMOCEngine", "ThermohalineStommelAMOCEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(ThermohalineStommelAMOCEngine.class);

    public static final double SEAWATER_REF_DENSITY_KG_M3 = 1025.0;
    public static final double ALPHA_THERMAL = 2.0e-4;
    public static final double BETA_HALINE = 7.5e-4;
    public static final double BASELINE_AMOC_SV = 18.0;
    public static final double STOMMEL_COUPLING_C = 11500.0;
    public static final double GYRE_THERMAL_EFFICIENCY = 0.55;

    /** Timescales for thermal and haline memory in years */
    public static final double TAU_THERMAL_YEARS = 20.0;
    public static final double TAU_HALINE_DIFFUSION_YEARS = 500.0;

    /** Equilibrium restoring baseline states */
    public static final double RESTORING_EQUATOR_TEMP_C = 28.0;
    public static final double RESTORING_EQUATOR_SALINITY_PSU = 36.5;
    public static final double RESTORING_POLAR_TEMP_C = 2.0;
    public static final double RESTORING_POLAR_SALINITY_PSU = 34.8;
    public static final double SEAWATER_REFERENCE_SALINITY_PSU = 35.0;

    /** Effective polar convective box volume in Sverdrup-years */
    public static final double POLAR_BOX_VOLUME_SV_YR = 250.0;

    /** Natural baseline subpolar freshwater precipitation/runoff flux balancing advection at steady state (Sv) */
    public static final double BASELINE_POLAR_FRESHWATER_SV =
            BASELINE_AMOC_SV * (RESTORING_EQUATOR_SALINITY_PSU - RESTORING_POLAR_SALINITY_PSU) / SEAWATER_REFERENCE_SALINITY_PSU;

    /** Dynamic state variables (preserved across simulation ticks for true haline memory) */
    private static double polarTempC = RESTORING_POLAR_TEMP_C;
    private static double polarSalinityPsu = RESTORING_POLAR_SALINITY_PSU;
    private static double currentAmocFlowSv = BASELINE_AMOC_SV;
    private static double currentCoolingShiftC = 0.0;

    /**
     * Thermohaline overturning state record.
     *
     * @param amocFlowSv                 overturning volume flux in Sverdrups ($10^6\,\text{m}^3/\text{s}$)
     * @param northAtlanticCoolingShiftC smooth high-latitude thermal anomaly (Â°C)
     * @param southernOceanWarmingShiftC bipolar seesaw southern anomaly (Â°C)
     * @param isCollapsed                diagnostic flag indicating $q_{\text{AMOC}} < 8.0\,\text{Sv}$
     */
    public record StommelState(double amocFlowSv, double northAtlanticCoolingShiftC, double southernOceanWarmingShiftC, boolean isCollapsed) {}

    /**
     * Calculates seawater density ($\text{kg/m}^3$) from temperature (Â°C) and practical salinity (PSU).
     */
    public static double calculateSeawaterDensity(double tempC, double salinityPsu) {
        double deltaT = tempC - 15.0;
        double deltaS = salinityPsu - 35.0;
        return SEAWATER_REF_DENSITY_KG_M3 * (1.0 - ALPHA_THERMAL * deltaT + BETA_HALINE * deltaS);
    }

    /**
     * Evaluates the instantaneous Stommel overturning flow rate $q$ (Sv).
     *
     * @param tEquatorC    Equatorial temperature (Â°C)
     * @param tPoleC       Polar temperature (Â°C)
     * @param sEquatorPsu  Equatorial salinity (PSU)
     * @param sPolePsu     Polar salinity (PSU)
     * @return overturning flux in Sv
     */
    public static double calculateOverturningFlowSv(double tEquatorC, double tPoleC, double sEquatorPsu, double sPolePsu) {
        double deltaTEff = GYRE_THERMAL_EFFICIENCY * (tEquatorC - tPoleC);
        double deltaS = sEquatorPsu - sPolePsu;
        double densityDrive = (ALPHA_THERMAL * deltaTEff) - (BETA_HALINE * deltaS);
        return SmoothMath.softplus(STOMMEL_COUPLING_C * densityDrive, 1.5);
    }

    /**
     * Evaluates the instantaneous Stommel overturning state from prescribed box states.
     *
     * @param tempEquatorC    Equatorial temperature (Â°C)
     * @param tempPoleC       Polar temperature (Â°C)
     * @param salinityEquatorPsu Equatorial salinity (PSU)
     * @param salinityPolePsu    Polar salinity (PSU)
     * @return StommelState instance
     */
    public static StommelState calculateStommelAMOC(double tempEquatorC, double tempPoleC,
                                                    double salinityEquatorPsu, double salinityPolePsu) {
        double amocFlowSv = calculateOverturningFlowSv(tempEquatorC, tempPoleC, salinityEquatorPsu, salinityPolePsu);
        boolean isCollapsed = (amocFlowSv < 8.0);

        double collapseFraction = 1.0 / (1.0 + Math.exp((amocFlowSv - 8.0) / 1.5));
        double linearWeakening = -SmoothMath.softplus(BASELINE_AMOC_SV - amocFlowSv, 1.5) * 0.35;
        double northAtlanticCooling = (1.0 - collapseFraction) * linearWeakening + collapseFraction * (-8.0);
        double southernOceanWarming = -0.22 * northAtlanticCooling;

        return new StommelState(amocFlowSv, northAtlanticCooling, southernOceanWarming, isCollapsed);
    }

    /**
     * Advances the Stommel 2-box differential equations over time $\Delta t$ with freshwater forcing $F_{\text{melt}}$.
     * Uses internal numerical sub-cycling (dt <= 1.0 year) to ensure unconditional numerical stability across deep time.
     *
     * @param freshwaterMeltwaterAnomalySv Freshwater meltwater anomaly (Sv)
     * @param dtYears                      Integration timestep in years
     * @return current thermodynamic StommelState
     */
    public static StommelState stepStommelDynamics(double freshwaterMeltwaterAnomalySv, double dtYears) {
        double dtSafe = Math.clamp(dtYears, 0.001, 50.0);

        int subSteps = Math.max(1, (int) Math.ceil(dtSafe / 1.0));
        double dt = dtSafe / subSteps;

        double q = currentAmocFlowSv;

        for (int i = 0; i < subSteps; i++) {
            // 1. Calculate current overturning flow from state
            q = calculateOverturningFlowSv(RESTORING_EQUATOR_TEMP_C, polarTempC, RESTORING_EQUATOR_SALINITY_PSU, polarSalinityPsu);

            // 2. Compute derivatives: dT_p/dt and dS_p/dt (Stommel 1961 canonical two-box advection-diffusion)
            double dTempDt = (RESTORING_POLAR_TEMP_C - polarTempC) / TAU_THERMAL_YEARS;

            double advectionS = (Math.max(0.0, q) / POLAR_BOX_VOLUME_SV_YR) * (RESTORING_EQUATOR_SALINITY_PSU - polarSalinityPsu);
            // Clausius-Clapeyron atmospheric scaling: cold stadial AMOC collapse curtails subpolar precipitation
            double baselineFreshwaterSv = BASELINE_POLAR_FRESHWATER_SV * (Math.max(0.0, q) / BASELINE_AMOC_SV);
            double totalFreshwaterSv = baselineFreshwaterSv + Math.max(0.0, freshwaterMeltwaterAnomalySv);
            double freshwaterDilutionS = (totalFreshwaterSv * SEAWATER_REFERENCE_SALINITY_PSU) / POLAR_BOX_VOLUME_SV_YR;
            double slowDiffusionS = (RESTORING_POLAR_SALINITY_PSU - polarSalinityPsu) / TAU_HALINE_DIFFUSION_YEARS;

            double dSalinityDt = advectionS - freshwaterDilutionS + slowDiffusionS;

            // 3. Sub-step integration
            polarTempC = Math.clamp(polarTempC + dTempDt * dt, -1.8, 20.0);
            polarSalinityPsu = Math.clamp(polarSalinityPsu + dSalinityDt * dt, 20.0, 38.0);
        }

        // 4. Update overturning flow and convective state after integrated step
        q = calculateOverturningFlowSv(RESTORING_EQUATOR_TEMP_C, polarTempC, RESTORING_EQUATOR_SALINITY_PSU, polarSalinityPsu);
        currentAmocFlowSv = q;
        boolean isCollapsed = (q < 8.0);

        // Smooth C^1 cooling response curve: from minor weakening to deep convective shutdown (-8.0A°C)
        double collapseFraction = 1.0 / (1.0 + Math.exp((q - 8.0) / 1.5));
        double linearWeakening = -SmoothMath.softplus(BASELINE_AMOC_SV - q, 1.5) * 0.35;
        double northAtlanticCooling = (1.0 - collapseFraction) * linearWeakening + collapseFraction * (-8.0);

        // Bipolar seesaw: South Atlantic / Southern Ocean heat accumulation (+2.0A°C max)
        double southernOceanWarming = -0.22 * northAtlanticCooling;

        currentCoolingShiftC = northAtlanticCooling;

        return new StommelState(q, northAtlanticCooling, southernOceanWarming, isCollapsed);
    }

    /**
     * Calculates the orbital and climatological freshwater meltwater flux $F_{\text{melt}}$ (Sv).
     *
     * @param currentYear Current simulation calendar year (negative for BCE/BP)
     * @param globalTempAnomalyC Global surface temperature anomaly relative to pre-industrial (Â°C)
     * @return estimated freshwater discharge in Sverdrups
     */
    public static double calculateMeltwaterAnomalySv(double currentYear, double globalTempAnomalyC) {
        // 1. Orbital forcing component from 65A°N summer solstice insolation
        MilankovitchOrbitalEngine.MilankovitchParameters orbital = MilankovitchOrbitalEngine.computeOrbitalParameters(currentYear);
        double summerInsolation65N = MilankovitchOrbitalEngine.calculateDailyInsolation(65.0, 172, orbital);

        // Baseline summer insolation at 65A°N is ~480 W/mA². High insolation (>485 W/mA²) triggers ice sheet ablation
        double insolationExcess = Math.max(0.0, summerInsolation65N - 485.0);
        double orbitalMeltSv = (insolationExcess / 45.0) * 0.25;

        // 2. Global temperature anomaly melting component (contemporary & prospective)
        double greenhouseMeltSv = Math.max(0.0, globalTempAnomalyC) * 0.06;

        return orbitalMeltSv + greenhouseMeltSv;
    }

    /**
     * Executes one thermohaline circulation step and applies smooth spatial anomalies across planetary cells.
     *
     * @param cells Simulation H3 cells
     * @param dtYears Timestep duration in years
     * @param currentYear Simulation year
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double dtYears, double currentYear) {
        if (cells == null || cells.isEmpty()) return;

        double meltwaterAnomalySv = calculateMeltwaterAnomalySv(currentYear, 0.0);
        processThermohalineCirculationWithMeltwater(cells, meltwaterAnomalySv, dtYears);
    }

    /**
     * Executes one thermohaline circulation step for current baseline era (year 0 CE).
     *
     * @param cells Simulation H3 cells
     * @param dtYears Timestep duration in years
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double dtYears) {
        processThermohalineCirculation(cells, dtYears, 0.0);
    }

    /**
     * Executes one thermohaline circulation step with explicit freshwater meltwater anomaly.
     *
     * @param cells Simulation H3 cells
     * @param freshwaterMeltwaterAnomalySv Freshwater discharge in Sverdrups
     * @param dtYears Timestep duration in years
     */
    public static void processThermohalineCirculationWithMeltwater(List<H3Cell> cells, double freshwaterMeltwaterAnomalySv, double dtYears) {
        if (cells == null || cells.isEmpty()) return;

        StommelState state = stepStommelDynamics(freshwaterMeltwaterAnomalySv, dtYears);

        if (state.isCollapsed()) {
            logger.info("AMOC Thermohaline Circulation COLLAPSED: Flow = {} Sv, North Cooling = {} C, South Warming = {} C",
                    String.format(java.util.Locale.ROOT, "%.2f", state.amocFlowSv()),
                    String.format(java.util.Locale.ROOT, "%.2f", state.northAtlanticCoolingShiftC()),
                    String.format(java.util.Locale.ROOT, "%.2f", state.southernOceanWarmingShiftC()));
        }

        double dtSafe = Math.max(0.0001, dtYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 2.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double lat = cell.getLatitudeOr(0.0);
            double lng = cell.getLongitudeOr(0.0);

            // 1. North Atlantic & Western European Spatial Envelope (40A°N-75A°N, -65A°E to +35A°E)
            double northLatWeight = SmoothMath.sigmoid(lat, 40.0, 4.0) * (1.0 - SmoothMath.sigmoid(lat, 75.0, 4.0));
            double northLonWeight = SmoothMath.sigmoid(lng, -65.0, 8.0) * (1.0 - SmoothMath.sigmoid(lng, 35.0, 8.0));
            double northEnvelope = northLatWeight * northLonWeight;

            // 2. Southern Ocean / South Atlantic Bipolar Seesaw Envelope (-70A°S to -30A°S, -60A°E to +30A°E)
            double southLatWeight = SmoothMath.sigmoid(lat, -70.0, 5.0) * (1.0 - SmoothMath.sigmoid(lat, -30.0, 5.0));
            double southLonWeight = SmoothMath.sigmoid(lng, -60.0, 10.0) * (1.0 - SmoothMath.sigmoid(lng, 30.0, 10.0));
            double southEnvelope = southLatWeight * southLonWeight;

            double curTemp = cell.getTemperatureOr(10.0);

            if (northEnvelope > 1e-3) {
                double deltaTNorth = state.northAtlanticCoolingShiftC() * northEnvelope;
                cell.setTemperature(curTemp + deltaTNorth * thermalRelaxationFactor);
            } else if (southEnvelope > 1e-3) {
                double deltaTSouth = state.southernOceanWarmingShiftC() * southEnvelope;
                cell.setTemperature(curTemp + deltaTSouth * thermalRelaxationFactor);
            }
        }
    }

    // Getters and state management
    public static double getPolarTempC() { return polarTempC; }
    public static double getPolarSalinityPsu() { return polarSalinityPsu; }
    public static double getCurrentAmocFlowSv() { return currentAmocFlowSv; }
    public static double getCurrentCoolingShiftC() { return currentCoolingShiftC; }

    public static void setPolarSalinityPsu(double salinity) { polarSalinityPsu = salinity; }
    public static void setPolarTempC(double temp) { polarTempC = temp; }

    /** Resets the Stommel dynamic ocean state to pristine pre-industrial baseline. */
    public static void resetState() {
        polarTempC = RESTORING_POLAR_TEMP_C;
        polarSalinityPsu = RESTORING_POLAR_SALINITY_PSU;
        currentAmocFlowSv = BASELINE_AMOC_SV;
        currentCoolingShiftC = 0.0;
    }
}
