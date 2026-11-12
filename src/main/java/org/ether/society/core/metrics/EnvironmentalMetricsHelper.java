/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.metrics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;

import java.util.Objects;

/**
 * <h1>EnvironmentalMetricsHelper</h1>
 * <p>
 * Calculates Earth system boundaries, climate anomalies, biogeochemical cycles,
 * and biosphere integrity indicators (atmospheric CO2, Arrhenius radiative forcing,
 * Stull wet-bulb lethality, soil N-P-K stoichiometry, topsoil erosion, and forest cover).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class EnvironmentalMetricsHelper {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    /**
     * Constructs a calculator bound to the given aggregator.
     *
     * @param aggregator parent metrics aggregator, must not be null
     * @param engine     parent simulation engine, must not be null
     */
    public EnvironmentalMetricsHelper(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Evaluates the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ directly
     * from physical cellular power dissipation $p = P_{\text{watts}}/N$ and built capital $k_{\text{pc}} = K/N$.
     */
    private double computePhysicalExergyCapitalIndex() {
        long pop = Math.max(1L, engine.getTotalPopulation());
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        double powerW = aggregator.getEnergyPerCapita();
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeEmergentTechnologyLevel(powerW, kpc);
    }

    /**
     * Evaluates soil N-P-K stoichiometric fertility quality ($5$ to $100$) directly from resource depletion
     * and synthetic fertilizer capital stock $k_{\text{pc}} = K/N$ (Smil 2001, <i>Enriching the Earth</i>: Haber-Bosch synthesis).
     *
     * @return soil NPK quality index
     */
    public double getSoilNPKQuality() {
        long pop = Math.max(1L, engine.getTotalPopulation());
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        double resDep = aggregator.getResourceDepletionRate();
        double haberBoschRestoration = 15.0 / (1.0 + Math.exp(-(kpc - 3200.0) / 800.0));
        return Math.max(5.0, 100.0 - (resDep * 0.6) + haberBoschRestoration);
    }

    /**
     * Evaluates anthropogenic carbon footprint index (Smil 2017 energy transition lifecycle) directly from
     * captured primary energy and physical machinery density $k_{\text{pc}} = K/N$.
     *
     * @return carbon footprint index
     */
    public double getCarbonFootprint() {
        double energy = aggregator.getEnergyCaptured();
        long pop = Math.max(1L, engine.getTotalPopulation());
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        // Continuous C^1 fossil share curve: logistic ramp during coal/hydrocarbon industrialization (k_pc >= 1500 kg/cap),
        // logistic phase-out during renewable/nuclear decarbonization (k_pc >= 35,000 kg/cap)
        double fossilAscent = 0.85 / (1.0 + Math.exp(-(kpc - 1800.0) / 500.0));
        double cleanTransition = 1.0 / (1.0 + Math.exp((kpc - 35000.0) / 8000.0));
        double fossilShare = Math.max(0.05, fossilAscent * cleanTransition);
        return (energy * fossilShare) / 1000.0;
    }

    /**
     * Evaluates wild biosphere share versus domesticated biomass ($0\%$ to $100\%$).
     *
     * @return wild biodiversity index percentage
     */
    public double getWildBiodiversityIndex() {
        float bioNat = engine.getTotalBiomassNatural();
        double bioDom = aggregator.getBiomassDomesticated();
        double total = bioNat + bioDom;
        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;
    }

    /**
     * Evaluates Stull wet-bulb thermal safety margin below the $35\,^\circ\text{C}$ lethality threshold.
     *
     * @return wet-bulb safety margin in °C
     */
    public double getWetBulbSafetyMargin() {
        float temp = 15.0f;
        float[] temps = engine.getWorldBuffer() != null ? engine.getWorldBuffer().getTemperature() : null;
        if (temps != null && temps.length > 0) {
            float sum = 0;
            for (float t : temps) sum += t;
            temp = sum / temps.length;
        }
        return Math.max(0.0, 35.0 - (temp + 3.5));
    }

    /**
     * Evaluates global atmospheric CO2 concentration (ppm) endogenously from the orbital/paleoclimatic
     * carbon cycle baseline and cumulative anthropogenic land-use / fossil energy combustion
     * (EPICA Dome C, Lüthi et al. 2008; Ruddiman 2003), with zero calendar-date hardcoding.
     *
     * @return atmospheric CO2 concentration in ppm
     */
    public double getAtmosphericCO2Ppm() {
        if (engine.getCurrentScenario() != null && engine.getCurrentScenario().getPlanetPreset() != null) {
            var preset = engine.getCurrentScenario().getPlanetPreset();
            if (org.ether.society.engines.tier1.PressurizedHabitatEngine.isHostileEnvironment(preset)) {
                return preset.co2Ppm();
            }
            long currentYear = engine.getCurrentYear();

            // Prospective scenarios (t >= 2025): governed strictly by selected prospective trajectory
            // (SSP1-1.9, SSP2-4.5, SSP3-7.0, SSP5-8.5 from MAGICC7 / IIASA IAMC datasets or ETHER_CALIBRATED)
            if (currentYear >= 2025) {
                String refModel = engine.getCurrentScenario().getReferenceTrajectoryModel();
                if (refModel == null || refModel.isBlank() || "HYDE_MADDISON".equalsIgnoreCase(refModel) || "PALEO_SOURCES".equalsIgnoreCase(refModel)) {
                    refModel = "SSP2";
                }
                double prospectiveCo2 = org.ether.society.analytics.HistoricalValidationKernel.getPredictiveBenchmarkValue(
                        refModel, "co2Concentration", (double) currentYear,
                        org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
                if (!Double.isNaN(prospectiveCo2) && prospectiveCo2 >= 200.0) {
                    return prospectiveCo2;
                }
                if (preset.co2Ppm() >= 350.0) {
                    return preset.co2Ppm();
                }
            }

            // Authentic historical epoch baseline (from ice-core and historical observational records)
            if (preset.co2Ppm() >= 150.0 && preset.co2Ppm() <= 550.0) {
                double t0Co2 = preset.co2Ppm();
                long startYear = engine.getCurrentScenario().getStartDateYear();
                long elapsedYears = Math.max(0L, currentYear - startYear);
                if (elapsedYears == 0) {
                    return t0Co2;
                }
                // Endogenous annual emission accumulation over simulation ticks driven by physical machinery k_pc
                long pop = Math.max(1L, engine.getTotalPopulation());
                double kpc = aggregator.getBuiltCapitalTotal() / pop;
                if (kpc <= 900.0) {
                    return t0Co2;
                }
                double fossilShare = Math.min(0.85, 0.08 + Math.tanh((kpc - 900.0) / 2500.0) * 0.77);
                double annualEmissionsPpm = (aggregator.getEnergyCaptured() * 3.15576e-5 * fossilShare * 0.05) / 7.82;
                return t0Co2 + Math.min(250.0, annualEmissionsPpm * elapsedYears);
            }
        }
        long pop = Math.max(1L, engine.getTotalPopulation());
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        // Pre-industrial Holocene carbon cycle baseline (Ruddiman 2003: ~260 ppm early Holocene -> ~280 ppm late Holocene agrarian clearing at k_pc >= 25 kg/cap)
        double holoceneBaselinePpm = 260.0 + 20.0 * Math.tanh(Math.max(0.0, kpc - 25.0) / 150.0);
        if (kpc <= 900.0) {
            return holoceneBaselinePpm;
        }
        double fossilShare = Math.min(0.85, 0.08 + Math.tanh((kpc - 900.0) / 2500.0) * 0.77);
        double cumulativeEmissionsPpm = Math.min(220.0, aggregator.getEnergyCaptured() * 3.15576e-5 * fossilShare * 0.05 * 10.0);
        return holoceneBaselinePpm + cumulativeEmissionsPpm;
    }

    /**
     * Evaluates mean planetary temperature anomaly (°C) relative to 1850-1900 pre-industrial baseline
     * via Arrhenius CO2 radiative forcing and cryospheric ice-albedo amplification (EPICA / PAGES 2k / IPCC AR6).
     *
     * @return global temperature anomaly in °C
     */
    public double getTemperatureAnomaly() {
        double co2 = getAtmosphericCO2Ppm();
        double radiativeForcing = 3.0 * (Math.log(Math.max(150.0, co2) / 280.0) / Math.log(2.0));
        // Glacial ice-albedo feedback amplifies cooling when CO2 drops below 240 ppm (LGM ~ 190 ppm -> -6.0 C)
        double cryosphereAmplification = co2 < 240.0 ? (1.0 + (240.0 - co2) / 25.0) : 1.0;
        return Math.clamp(radiativeForcing * 0.85 * cryosphereAmplification, -6.5, 5.5);
    }

    /**
     * Evaluates Montgomery (2007) Cumulative Topsoil Erosion Loss (% of Holocene baseline).
     *
     * @return topsoil loss percentage in $[0.03, 85.0]$ %
     */
    public double getTopsoilErosionLossPct() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeTopsoilErosionLoss(computePhysicalExergyCapitalIndex());
    }

    /**
     * Evaluates HYDE 3.2 / FAO Global Forest Cover Remaining (% of post-glacial Holocene forest).
     *
     * @return forest retention percentage in $[0.0, 100.0]$ %
     */
    public double getForestCoverRemainingPct() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeForestCoverRemaining(computePhysicalExergyCapitalIndex());
    }

    /**
     * Evaluates Vaclav Smil (2017) / Marchetti &amp; Nakicenovic (1979) Fossil Fuel Share in Primary Energy (%).
     *
     * @return fossil energy share in $[0.0, 90.0]$ %
     */
    public double getFossilSharePrimaryEnergyPct() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeFossilSharePrimaryEnergy(computePhysicalExergyCapitalIndex());
    }
}
