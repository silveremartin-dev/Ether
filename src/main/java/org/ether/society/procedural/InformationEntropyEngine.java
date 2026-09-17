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
 * Shannon Information Entropy & Command Bandwidth Engine.
 * Replaces abstract cultural drift numbers with Shannon Information Theory:
 * 1. <b>Shannon Channel Capacity (C = B * log2(1 + S/N))</b>: Maximum information transmission rate (bits/sec) across terrain.
 * 2. <b>Signal Attenuation & Cultural Decoupling</b>: Signal attenuation over distance and noise (S/N) drives dialect/political divergence.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class InformationEntropyEngine {
    private static final Logger logger = LoggerFactory.getLogger(InformationEntropyEngine.class);

    /**
     * Calculates Shannon Channel Capacity (bits/sec) across terrain friction.
     *
     * @param bandwidthHz Frequency bandwidth in Hz
     * @param signalToNoiseRatio Signal-to-noise ratio (linear)
     * @return Channel capacity C in bits/second
     */
    public static double calculateShannonCapacityBitsPerSec(double bandwidthHz, double signalToNoiseRatio) {
        if (signalToNoiseRatio <= 0.0) return 0.0;
        return bandwidthHz * (Math.log(1.0 + signalToNoiseRatio) / Math.log(2.0));
    }

    /**
     * Executes one information channel capacity update across cells.
     */
    public static void processInformationEntropy(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;

            // Bandwidth Hz scales with technology era (Runner -> Paper -> Telegraph -> Fiber)
            double bandwidthHz = 1.0 + Math.pow(tech, 3.0) * 1000.0;
            double snr = Math.max(0.1, 10.0 / friction);

            double channelCapacityBitsPerSec = calculateShannonCapacityBitsPerSec(bandwidthHz, snr);

            // High information capacity suppresses linguistic drift
            if (channelCapacityBitsPerSec > 100.0) {
                cell.setLinguisticDrift(Math.max(0.0, cell.getLinguisticDrift() - 0.01));
            }
        }
    }
}

