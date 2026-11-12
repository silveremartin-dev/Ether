package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Radiative Greenhouse Effect, Stefan-Boltzmann Equilibrium &amp; Clausius-Clapeyron Dynamics Engine.
 * <p>
 * Strictly governed by physical atmospheric concentrations ($[\text{CO}_2]$, $[\text{CH}_4]$) and
 * surface radiative physics ({@code AGENTS.md} Â§7.2, Â§10.1, Â§10.2), with zero dependency on
 * synthetic {@code avgTechLevel} mitigation cliffs.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class GreenhouseRadiativeEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(GreenhouseRadiativeEngine.class);

    private static final double BASELINE_CO2_PPM = 280.0;
    private static final double BASELINE_CH4_PPB = 720.0;
    private static final double CLIMATE_SENSITIVITY_LAMBDA = 0.8; // A°C per W/mA²

    public static final double STEFAN_BOLTZMANN_SIGMA = 5.670374e-8;
    public static final double EMISSIVITY_EPSILON = 0.98;
    public static final double SOLAR_CONSTANT_TOA = 1361.0;
    public static final double LATENT_HEAT_VAPORIZATION_LV = 2.501e6;
    public static final double GAS_CONSTANT_VAPOR_RV = 461.5;
    public static final double REFERENCE_VAPOR_PRESSURE_E0 = 611.3;

    private double currentCo2Ppm = 280.0;
    private double currentCh4Ppb = 720.0;
    private double seaLevelDeltaMeters = 0.0;
    private double previousAppliedDeltaT = 0.0;
    private double previousAppliedSeaShift = 0.0;

    /** Constructs a radiative engine initialized to pre-industrial Holocene equilibrium (280 ppm CO2, 720 ppb CH4). */
    public GreenhouseRadiativeEngine() {
        this(280.0, 720.0);
    }

    /**
     * Constructs a radiative engine with explicit atmospheric greenhouse gas concentrations.
     *
     * @param initialCo2Ppm initial $\text{CO}_2$ concentration in ppm
     * @param initialCh4Ppb initial $\text{CH}_4$ concentration in ppb
     */
    public GreenhouseRadiativeEngine(double initialCo2Ppm, double initialCh4Ppb) {
        super("GreenhouseRadiativeEngine", "GreenhouseRadiativeEngine", EngineTier.TIER_1_BIOPHYSICAL);
        this.currentCo2Ppm = initialCo2Ppm;
        this.currentCh4Ppb = initialCh4Ppb;
    }

    /**
     * Calculates net radiative forcing in $\text{W/m}^2$ directly from physical atmospheric
     * $\text{CO}_2$ and $\text{CH}_4$ concentrations (Myhre et al. 1998; IPCC AR6):
     * $$\Delta F = 5.35 \ln\!\left(\frac{[\text{CO}_2]}{[\text{CO}_2]_0}\right) + 0.036\left(\sqrt{[\text{CH}_4]} - \sqrt{[\text{CH}_4]_0}\right)$$
     * Note: Negative anomalies ($[\text{CO}_2] < 280\,\text{ppm}$, e.g. glacial LGM $185\,\text{ppm}$)
     * produce authentic negative radiative forcing without artificial zero-clipping.
     *
     * @return radiative forcing anomaly in $\text{W/m}^2$
     */
    public double computeRadiativeForcingWpm2() {
        double co2Ratio = SmoothMath.softmax(50.0, currentCo2Ppm, 0.2) / BASELINE_CO2_PPM;
        double co2Forcing = 5.35 * Math.log(co2Ratio);
        double ch4Forcing = 0.036 * (Math.sqrt(SmoothMath.softplus(currentCh4Ppb, 0.2)) - Math.sqrt(BASELINE_CH4_PPB));
        return SmoothMath.softplus(co2Forcing + ch4Forcing, 4.0);
    }

    /**
     * Computes the global mean surface temperature anomaly (Â°C) relative to Holocene baseline.
     */
    public double computeTemperatureAnomalyC() {
        return computeRadiativeForcingWpm2() * CLIMATE_SENSITIVITY_LAMBDA;
    }

    /**
     * Calculates saturation vapor pressure $e_s(T)$ in Pascals using Clausius-Clapeyron.
     */
    public static double calculateClausiusClapeyronVaporPressurePa(double tempCelsius) {
        double tempK = SmoothMath.softmax(150.0, tempCelsius + PhysicalConstants.KELVIN_ZERO_CELSIUS, 0.2);
        double exponent = (LATENT_HEAT_VAPORIZATION_LV / GAS_CONSTANT_VAPOR_RV)
                * ((1.0 / PhysicalConstants.KELVIN_ZERO_CELSIUS) - (1.0 / tempK));
        return REFERENCE_VAPOR_PRESSURE_E0 * Math.exp(Math.clamp(exponent, -15.0, 15.0));
    }

    /**
     * Computes radiative equilibrium surface temperature in Kelvin using Stefan-Boltzmann.
     */
    public static double calculateStefanBoltzmannEquilibriumTempK(double albedo, double greenhouseForcingWpm2) {
        double absorbedSolarFlux = (SOLAR_CONSTANT_TOA / 4.0) * (1.0 - Math.clamp(albedo, 0.05, 0.95));
        double totalDownwardFlux = absorbedSolarFlux + SmoothMath.softplus(greenhouseForcingWpm2, 2.0);
        double radiativeDenominator = EMISSIVITY_EPSILON * STEFAN_BOLTZMANN_SIGMA;
        return Math.pow(totalDownwardFlux / radiativeDenominator, 0.25);
    }

    /**
     * Computes equilibrium eustatic sea level rise in meters from thermal expansion and ice-sheet melt.
     */
    public double computeSeaLevelDeltaMeters() {
        return computeTemperatureAnomalyC() * 42.5;
    }

    /**
     * Advances radiative state by adding net greenhouse gas flux (emissions minus physical biosphere/CCS sinks).
     */
    public void addEmissions(double deltaCo2Ppm, double deltaCh4Ppb) {
        this.currentCo2Ppm = Math.max(50.0, this.currentCo2Ppm + deltaCo2Ppm);
        this.currentCh4Ppb = Math.max(50.0, this.currentCh4Ppb + deltaCh4Ppb);
        this.seaLevelDeltaMeters = computeSeaLevelDeltaMeters();
    }

    /**
     * Applies incremental temperature anomaly and sea-level shift to the H3 grid.
     */
    public void applyToGrid(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double deltaT = computeTemperatureAnomalyC();
        double seaShift = computeSeaLevelDeltaMeters();
        double ghForcing = computeRadiativeForcingWpm2();

        double incrementalDeltaT = deltaT - previousAppliedDeltaT;
        double incrementalSeaShift = seaShift - previousAppliedSeaShift;

        this.previousAppliedDeltaT = deltaT;
        this.previousAppliedSeaShift = seaShift;

        if (Math.abs(incrementalDeltaT) < 1.0e-5 && Math.abs(incrementalSeaShift) < 1.0e-5) return;

        logger.debug("Greenhouse Engine Step: CO2={} ppm, CH4={} ppb | DeltaT=+{}Â°C, SeaLevelDelta=+{}m, Forcing={} W/mÂ²",
                currentCo2Ppm, currentCh4Ppb,
                String.format(java.util.Locale.ROOT, "%.2f", deltaT),
                String.format(java.util.Locale.ROOT, "%.1f", seaShift),
                String.format(java.util.Locale.ROOT, "%.2f", ghForcing));

        cells.parallelStream().forEach(cell -> {
            if (cell == null) return;
            if (cell.getTemperature() != null) {
                cell.setTemperature(cell.getTemperature() + incrementalDeltaT);
            }
            if (cell.getElevation() != null && cell.getElevation() > 0) {
                cell.setElevation(Math.max(-100.0, cell.getElevation() - incrementalSeaShift));
            }
        });
    }

    /** @return current atmospheric $\text{CO}_2$ concentration in ppm */
    public double getCurrentCo2Ppm() { return currentCo2Ppm; }
    /** @return current atmospheric $\text{CH}_4$ concentration in ppb */
    public double getCurrentCh4Ppb() { return currentCh4Ppb; }
    /** @return cumulative sea level delta in meters */
    public double getSeaLevelDeltaMeters() { return seaLevelDeltaMeters; }
}
