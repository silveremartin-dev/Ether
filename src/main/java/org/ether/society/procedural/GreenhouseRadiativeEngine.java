package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Radiative Greenhouse Effect & Sea Level Dynamics Engine.
 * Simulates atmospheric thermal feedback from CO2, CH4, and water vapor,
 * as well as thermal expansion of oceans and ice-sheet melting.
 */
public class GreenhouseRadiativeEngine {
    private static final Logger logger = LoggerFactory.getLogger(GreenhouseRadiativeEngine.class);

    private static final double BASELINE_CO2_PPM = 280.0;
    private static final double BASELINE_CH4_PPB = 720.0;
    private static final double CLIMATE_SENSITIVITY_LAMBDA = 0.8; // °C per W/m²

    private double currentCo2Ppm = 280.0;
    private double currentCh4Ppb = 720.0;
    private double seaLevelDeltaMeters = 0.0;

    public GreenhouseRadiativeEngine() {
        this(280.0, 720.0);
    }

    public GreenhouseRadiativeEngine(double initialCo2Ppm, double initialCh4Ppb) {
        this.currentCo2Ppm = initialCo2Ppm;
        this.currentCh4Ppb = initialCh4Ppb;
    }

    /**
     * Calculates radiative forcing in W/m² based on greenhouse gas concentrations.
     */
    public double computeRadiativeForcingWpm2() {
        double co2Ratio = Math.max(1.0, currentCo2Ppm) / BASELINE_CO2_PPM;
        double co2Forcing = 5.35 * Math.log(co2Ratio);

        double ch4Forcing = 0.036 * (Math.sqrt(Math.max(0.0, currentCh4Ppb)) - Math.sqrt(BASELINE_CH4_PPB));
        return Math.max(0.0, co2Forcing + ch4Forcing);
    }

    /**
     * Computes the global mean temperature anomaly (°C) relative to baseline.
     */
    public double computeTemperatureAnomalyC() {
        return computeRadiativeForcingWpm2() * CLIMATE_SENSITIVITY_LAMBDA;
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
     * Applies temperature anomaly and sea level shift to the H3 grid.
     */
    public void applyToGrid(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double deltaT = computeTemperatureAnomalyC();
        double seaShift = computeSeaLevelDeltaMeters();

        logger.info("Greenhouse Engine Step: CO2={} ppm, CH4={} ppb | DeltaT=+{:.2f}°C, SeaLevelDelta=+{:.1f}m",
                currentCo2Ppm, currentCh4Ppb, deltaT, seaShift);

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
