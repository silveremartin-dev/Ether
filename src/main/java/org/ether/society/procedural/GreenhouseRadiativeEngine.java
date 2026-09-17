package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Radiative Greenhouse Effect, Stefan-Boltzmann Equilibrium & Clausius-Clapeyron Dynamics Engine.
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Stefan-Boltzmann Radiative Balance</b>:
 *       $$\epsilon \sigma T^4 = \frac{S_0}{4}(1 - \alpha) + \Delta F_{\text{GES}}$$
 *   </li>
 *   <li><b>Clausius-Clapeyron Saturation Vapor Pressure</b>:
 *       $$e_s(T) = e_0 \cdot \exp\left( \frac{L_v}{R_v} \left( \frac{1}{T_0} - \frac{1}{T_K} \right) \right)$$
 *   </li>
 *   <li><b>CO2 & CH4 Radiative Forcing</b>:
 *       $$\Delta F = 5.35 \ln\left(\frac{[\text{CO}_2]}{[\text{CO}_2]_0}\right) + 0.036\left(\sqrt{[\text{CH}_4]} - \sqrt{[\text{CH}_4]_0}\right)$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class GreenhouseRadiativeEngine {
    private static final Logger logger = LoggerFactory.getLogger(GreenhouseRadiativeEngine.class);

    private static final double BASELINE_CO2_PPM = 280.0;
    private static final double BASELINE_CH4_PPB = 720.0;
    private static final double CLIMATE_SENSITIVITY_LAMBDA = 0.8; // °C per W/m²

    // Physical constants for Stefan-Boltzmann and Clausius-Clapeyron
    public static final double STEFAN_BOLTZMANN_SIGMA = 5.670374e-8; // W/(m²·K⁴)
    public static final double EMISSIVITY_EPSILON = 0.98;
    public static final double SOLAR_CONSTANT_TOA = 1361.0; // W/m²
    public static final double LATENT_HEAT_VAPORIZATION_LV = 2.501e6; // J/kg
    public static final double GAS_CONSTANT_VAPOR_RV = 461.5; // J/(kg·K)
    public static final double REFERENCE_VAPOR_PRESSURE_E0 = 611.3; // Pa at 273.15 K

    private double currentCo2Ppm = 280.0;
    private double currentCh4Ppb = 720.0;
    private double seaLevelDeltaMeters = 0.0;

    private double technologicalMitigationEfficiency = 0.0;

    public GreenhouseRadiativeEngine() {
        this(280.0, 720.0);
    }

    public GreenhouseRadiativeEngine(double initialCo2Ppm, double initialCh4Ppb) {
        this.currentCo2Ppm = initialCo2Ppm;
        this.currentCh4Ppb = initialCh4Ppb;
    }

    /**
     * Updates technological mitigation factor based on planetary tech level
     * (Carbon Capture & Storage, Fusion, Geoengineering).
     */
    public void applyTechnologicalMitigation(double avgTechLevel) {
        if (avgTechLevel > 5.0) {
            this.technologicalMitigationEfficiency = Math.clamp((avgTechLevel - 5.0) * 0.10, 0.0, 0.60);
        } else {
            this.technologicalMitigationEfficiency = 0.0;
        }
    }

    public double getTechnologicalMitigationEfficiency() {
        return technologicalMitigationEfficiency;
    }

    /**
     * Calculates radiative forcing in W/m² based on greenhouse gas concentrations
     * modulated by technological carbon capture and solar geoengineering mitigation.
     */
    public double computeRadiativeForcingWpm2() {
        double co2Ratio = Math.max(1.0, currentCo2Ppm) / BASELINE_CO2_PPM;
        double co2Forcing = 5.35 * Math.log(co2Ratio);

        double ch4Forcing = 0.036 * (Math.sqrt(Math.max(0.0, currentCh4Ppb)) - Math.sqrt(BASELINE_CH4_PPB));
        double rawForcing = Math.max(0.0, co2Forcing + ch4Forcing);
        return rawForcing * (1.0 - technologicalMitigationEfficiency);
    }

    /**
     * Computes the global mean temperature anomaly (°C) relative to baseline.
     */
    public double computeTemperatureAnomalyC() {
        return computeRadiativeForcingWpm2() * CLIMATE_SENSITIVITY_LAMBDA;
    }

    /**
     * Calculates saturation vapor pressure e_s(T) in Pascals using Clausius-Clapeyron.
     *
     * @param tempCelsius Temperature in Celsius
     * @return Saturation vapor pressure in Pascals
     */
    public static double calculateClausiusClapeyronVaporPressurePa(double tempCelsius) {
        double tempK = Math.max(150.0, tempCelsius + PhysicalConstants.KELVIN_ZERO_CELSIUS);
        double exponent = (LATENT_HEAT_VAPORIZATION_LV / GAS_CONSTANT_VAPOR_RV) * ( (1.0 / PhysicalConstants.KELVIN_ZERO_CELSIUS) - (1.0 / tempK) );
        return REFERENCE_VAPOR_PRESSURE_E0 * Math.exp(Math.clamp(exponent, -15.0, 15.0));
    }

    /**
     * Computes radiative equilibrium surface temperature in Kelvin using Stefan-Boltzmann:
     * T_eq = [ (S_0/4 * (1 - alpha) + Delta_F) / (epsilon * sigma) ]^(1/4)
     */
    public static double calculateStefanBoltzmannEquilibriumTempK(double albedo, double greenhouseForcingWpm2) {
        double absorbedSolarFlux = (SOLAR_CONSTANT_TOA / 4.0) * (1.0 - Math.clamp(albedo, 0.05, 0.95));
        double totalDownwardFlux = absorbedSolarFlux + Math.max(0.0, greenhouseForcingWpm2);
        double radiativeDenominator = EMISSIVITY_EPSILON * STEFAN_BOLTZMANN_SIGMA;
        return Math.pow(totalDownwardFlux / radiativeDenominator, 0.25);
    }

    /**
     * Computes sea level rise in meters based on thermal expansion & ice sheet melt.
     */
    public double computeSeaLevelDeltaMeters() {
        double deltaT = computeTemperatureAnomalyC();
        return deltaT * 42.5; // ~42.5m rise per °C long term
    }

    /**
     * Advances radiative state by adding anthropogenic or volcanic greenhouse emissions.
     */
    public void addEmissions(double deltaCo2Ppm, double deltaCh4Ppb) {
        this.currentCo2Ppm = Math.max(50.0, this.currentCo2Ppm + deltaCo2Ppm);
        this.currentCh4Ppb = Math.max(50.0, this.currentCh4Ppb + deltaCh4Ppb);
        this.seaLevelDeltaMeters = computeSeaLevelDeltaMeters();
    }

    /**
     * Applies temperature anomaly, Stefan-Boltzmann equilibrium, and sea level shift to the H3 grid.
     */
    public void applyToGrid(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double deltaT = computeTemperatureAnomalyC();
        double seaShift = computeSeaLevelDeltaMeters();
        double ghForcing = computeRadiativeForcingWpm2();

        logger.debug("Greenhouse Engine Step: CO2={} ppm, CH4={} ppb | DeltaT=+{:.2f}°C, SeaLevelDelta=+{:.1f}m, Forcing={:.2f} W/m²",
                currentCo2Ppm, currentCh4Ppb, deltaT, seaShift, ghForcing);

        cells.parallelStream().forEach(cell -> {
            if (cell.getTemperature() != null) {
                cell.setTemperature(cell.getTemperature() + deltaT);
            }
            if (cell.getElevation() != null && cell.getElevation() > 0) {
                cell.setElevation(Math.max(-100.0, cell.getElevation() - seaShift));
            }
        });
    }

    public double getCurrentCo2Ppm() { return currentCo2Ppm; }
    public double getCurrentCh4Ppb() { return currentCh4Ppb; }
    public double getSeaLevelDeltaMeters() { return seaLevelDeltaMeters; }
}

