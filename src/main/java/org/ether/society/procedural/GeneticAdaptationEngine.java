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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Physiological & Population Genetics Diffusion Engine (Kimura / Wright-Fisher SDE).
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Kimura Genetic Drift & Darwinian Selection SDE</b>:
 *       $$p_{t+\Delta t} = p_t + s \cdot p_t(1 - p_t) \Delta t + \sqrt{\frac{p_t(1 - p_t)}{2 N_e}} \cdot dW_t$$
 *   </li>
 *   <li><b>High Altitude Hypoxia Selection (EPAS1)</b>: Selection coefficient $s = +0.02$ at $z > 3000\text{ m}$.</li>
 *   <li><b>Thermal Stress Resistance (HSP)</b>: Selection coefficient $s = +0.015$ at $T > 38^\circ\text{C}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.2.0
 */
public class GeneticAdaptationEngine {
    private static final Logger logger = LoggerFactory.getLogger(GeneticAdaptationEngine.class);

    /**
     * Calculates the next allele frequency using the continuous Kimura / Wright-Fisher stochastic diffusion equation:
     * p_{t+dt} = p + s * p * (1 - p) * dt + sqrt( p * (1 - p) / (2 * Ne) ) * N(0, dt)
     *
     * @param currentFreq Current allele frequency p in [0.0, 1.0]
     * @param selectionCoeff Darwinian selection coefficient s
     * @param effectivePop Effective breeding population size N_e
     * @param dtYears Time delta in years
     * @return Updated allele frequency in [0.0, 1.0]
     */
    public static double calculateKimuraDiffusion(double currentFreq, double selectionCoeff, int effectivePop, double dtYears) {
        double p = Math.clamp(currentFreq, 0.0, 1.0);
        if (p <= 0.0 || p >= 1.0) return p; // Fixed or lost allele

        int ne = Math.max(10, effectivePop);
        double dt = Math.max(0.001, dtYears);

        // 1. Deterministic directional selection drift
        double deterministicDrift = selectionCoeff * p * (1.0 - p) * dt;

        // 2. Stochastic genetic drift (Brownian noise scaled by 1 / 4Ne)
        double variance = (p * (1.0 - p)) / (2.0 * ne);
        double stdDev = Math.sqrt(variance * dt);
        double gaussianNoise = Math.clamp(ThreadLocalRandom.current().nextGaussian(), -3.0, 3.0);
        double stochasticDrift = stdDev * gaussianNoise;

        return Math.clamp(p + deterministicDrift + stochasticDrift, 0.0, 1.0);
    }

    /**
     * Executes one genetic adaptation selection tick across cells.
     */
    public static void processGeneticAdaptation(List<H3Cell> cells) {
        processGeneticAdaptation(cells, 30.0 / 365.25);
    }

    /**
     * Executes genetic adaptation selection with explicit time delta in years.
     */
    public static void processGeneticAdaptation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        int adaptedCells = 0;
        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 20.0;

            // 1. High-altitude hypoxia adaptation (EPAS1 selection, > 3000m elevation)
            if (elev > 3000.0) {
                adaptedCells++;
                double epas1Freq = cell.getMovementFriction() != null ? Math.clamp(1.0 - (cell.getMovementFriction() / 3.0), 0.01, 0.99) : 0.05;
                double newFreq = calculateKimuraDiffusion(epas1Freq, 0.02, (int)(pop * 0.3), dt);

                // High allele frequency reduces hypoxia movement friction
                double updatedFriction = Math.max(0.3, 3.0 * (1.0 - newFreq));
                cell.setMovementFriction(updatedFriction);
            }

            // 2. Heat shock protein (HSP) adaptation in arid extreme heat (> 38°C)
            if (tempC > 38.0) {
                adaptedCells++;
                double hspFreq = cell.getLifespan() != null ? Math.clamp((cell.getLifespan() - 30.0) / 50.0, 0.01, 0.99) : 0.05;
                double newFreq = calculateKimuraDiffusion(hspFreq, 0.015, (int)(pop * 0.3), dt);

                // Increases thermal mortality threshold tolerance
                cell.setLifespan(Math.min(80.0, 30.0 + newFreq * 50.0));
            }
        }

        if (adaptedCells > 0) {
            logger.info("Genetic Engine: Multi-generational Kimura evolutionary adaptation active across {} specialized cells.", adaptedCells);
        }
    }
}

