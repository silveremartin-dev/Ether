/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.database.H3Cell;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic SHA-256 Fingerprint Generator for Planetary Simulation State.
 * Used for instant non-regression checking, cluster sync verification,
 * and zero-drift determinism assertions.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public final class SimulationStateChecksum {

    private SimulationStateChecksum() {}

    /**
     * Computes a deterministic SHA-256 hex digest of the world state.
     */
    public static String computeStateChecksum(List<H3Cell> cells, long year, int month, double co2Ppm) {
        if (cells == null || cells.isEmpty()) return "EMPTY";

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Hash Global Clock & Atmosphere
            ByteBuffer globalBuffer = ByteBuffer.allocate(24);
            globalBuffer.putLong(year);
            globalBuffer.putInt(month);
            globalBuffer.putDouble(co2Ppm);
            md.update(globalBuffer.array());

            // Process cells sorted by canonical H3 index for guaranteed deterministic order
            List<H3Cell> sortedCells = cells.stream()
                    .sorted(Comparator.comparingLong(H3Cell::getH3Index))
                    .toList();

            ByteBuffer cellBuffer = ByteBuffer.allocate(32);
            for (H3Cell c : sortedCells) {
                cellBuffer.clear();
                cellBuffer.putLong(c.getH3Index());
                cellBuffer.putInt(c.getPopulation() != null ? c.getPopulation() : 0);
                cellBuffer.putDouble(c.getTemperature() != null ? c.getTemperature() : 0.0);
                cellBuffer.putDouble(c.getElevation() != null ? c.getElevation() : 0.0);
                md.update(cellBuffer.array());
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
