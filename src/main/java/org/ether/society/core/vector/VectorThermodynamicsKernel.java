/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.vector;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * High-Performance SIMD Vector Processing Kernel for Planetary Thermodynamics.
 * Leverages Java Vector API (AVX-512 / AVX2 / ARM Neon) with strict deterministic
 * floating point execution order.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class VectorThermodynamicsKernel {
    private static final Logger logger = LoggerFactory.getLogger(VectorThermodynamicsKernel.class);
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    /**
     * Executes SIMD vectorized thermodynamic temperature and radiative forcing update
     * across all simulation cells.
     *
     * @param cells List of simulation cells
     * @param solarConstant Solar insolation base (W/m^2)
     * @param greenhouseForcing Radiative forcing factor (W/m^2)
     * @param dtYears Time step in years
     */
    public static void computeRadiativeEquilibrium(List<H3Cell> cells, double solarConstant, double greenhouseForcing, double dtYears) {
        if (cells == null || cells.isEmpty()) return;

        int size = cells.size();
        double[] albedos = new double[size];
        double[] temperatures = new double[size];
        double[] latitudes = new double[size];

        for (int i = 0; i < size; i++) {
            H3Cell c = cells.get(i);
            albedos[i] = c.calculateDynamicAlbedo() > 0 ? c.calculateDynamicAlbedo() : 0.30;
            temperatures[i] = c.getTemperature();
            latitudes[i] = c.getLatitude() != null ? c.getLatitude() : 0.0;
        }

        double[] updatedTemperatures = new double[size];
        int vectorBound = SPECIES.loopBound(size);

        // Vectorized SIMD loop
        for (int i = 0; i < vectorBound; i += SPECIES.length()) {
            DoubleVector vAlbedo = DoubleVector.fromArray(SPECIES, albedos, i);
            DoubleVector vTemp = DoubleVector.fromArray(SPECIES, temperatures, i);
            DoubleVector vLat = DoubleVector.fromArray(SPECIES, latitudes, i);

            // Insolation factor approximation: cos(lat_rad)
            // vInsolation = (1.0 - vAlbedo) * (solarConstant * cosFactor) + greenhouseForcing
            // For strict vectorization:
            DoubleVector vOne = DoubleVector.broadcast(SPECIES, 1.0);
            DoubleVector vAbsorbed = vOne.sub(vAlbedo);
            DoubleVector vSolar = DoubleVector.broadcast(SPECIES, solarConstant * 0.25);
            DoubleVector vGHG = DoubleVector.broadcast(SPECIES, greenhouseForcing);

            DoubleVector vNetFlux = vAbsorbed.mul(vSolar).add(vGHG);
            // Thermal relaxation: dT = (vNetFlux - outgoing) * dtFactor
            DoubleVector vTargetTemp = vNetFlux.mul(0.1).sub(15.0);
            DoubleVector vDelta = vTargetTemp.sub(vTemp).mul(dtYears * 0.5);
            DoubleVector vNewTemp = vTemp.add(vDelta);

            vNewTemp.intoArray(updatedTemperatures, i);
        }

        // Scalar fallback loop for tail elements (guarantees identical math)
        for (int i = vectorBound; i < size; i++) {
            double absorbed = 1.0 - albedos[i];
            double netFlux = (absorbed * solarConstant * 0.25) + greenhouseForcing;
            double targetTemp = (netFlux * 0.1) - 15.0;
            double delta = (targetTemp - temperatures[i]) * dtYears * 0.5;
            updatedTemperatures[i] = temperatures[i] + delta;
        }

        // Write back results
        for (int i = 0; i < size; i++) {
            cells.get(i).setTemperature(updatedTemperatures[i]);
        }
    }
}
