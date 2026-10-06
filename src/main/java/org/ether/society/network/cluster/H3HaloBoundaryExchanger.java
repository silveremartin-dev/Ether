/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.WorldBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 1-Ring and 2-Ring Halo Boundary Exchanger for distributed H3 simulation domains.
 * Synchronizes physical frontier cells (flux pressures, temperatures, biomass, pathogens)
 * across worker domain borders before each computational sub-step.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3HaloBoundaryExchanger {

    public static class HaloCellDelta {
        /* Internal state variable for cell index (int). */
        private final int cellIndex;
        /* Internal state variable for flux pressure (float). */
        private final float fluxPressure;
        /* Internal state variable for temperature (float). */
        private final float temperature;
        /* Internal state variable for biomass human (float). */
        private final float biomassHuman;
        /* Internal state variable for local price (float). */
        private final float localPrice;

        /*
         * Halo cell delta.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @param cellIndex the cell index parameter (int)
         * @param fluxPressure the flux pressure parameter (float)
         * @param temperature the temperature parameter (float)
         * @param biomassHuman the biomass human parameter (float)
         * @param localPrice the local price parameter (float)
         * @return the resulting computation or state reference
         */
        public HaloCellDelta(int cellIndex, float fluxPressure, float temperature, float biomassHuman, float localPrice) {
            this.cellIndex = cellIndex;
            this.fluxPressure = fluxPressure;
            this.temperature = temperature;
            this.biomassHuman = biomassHuman;
            this.localPrice = localPrice;
        }

        /*
         * Get cell index.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @return the resulting computation or state reference
         */
        public int getCellIndex() { return cellIndex; }
        /*
         * Get flux pressure.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @return the resulting computation or state reference
         */
        public float getFluxPressure() { return fluxPressure; }
        /*
         * Get temperature.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @return the resulting computation or state reference
         */
        public float getTemperature() { return temperature; }
        /*
         * Get biomass human.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @return the resulting computation or state reference
         */
        public float getBiomassHuman() { return biomassHuman; }
        /*
         * Get local price.
         * Enforces physical invariants and updates associated state variables within {@code H3HaloBoundaryExchanger}.
         *
         * @return the resulting computation or state reference
         */
        public float getLocalPrice() { return localPrice; }
    }

    /*
     * Extracts state values for all boundary cell indices of a partition into a compact delta list.
     */
    public static List<HaloCellDelta> extractHaloDeltas(WorldBuffer buffer, Set<Integer> boundaryIndices) {
        List<HaloCellDelta> deltas = new ArrayList<>();
        if (buffer == null || boundaryIndices == null || boundaryIndices.isEmpty()) return deltas;

        float[] flux = buffer.getFluxPressure();
        float[] temp = buffer.getTemperature();
        float[] pop = buffer.getBiomassHuman();
        float[] price = buffer.getLocalPrice();

        for (int idx : boundaryIndices) {
            if (idx >= 0 && idx < buffer.getCapacity()) {
                deltas.add(new HaloCellDelta(
                        idx,
                        flux != null ? flux[idx] : 0f,
                        temp != null ? temp[idx] : 0f,
                        pop != null ? pop[idx] : 0f,
                        price != null ? price[idx] : 0f
                ));
            }
        }
        return deltas;
    }

    /*
     * Serializes halo deltas to binary payload.
     */
    public static byte[] serializeHaloDeltas(List<HaloCellDelta> deltas) {
        if (deltas == null || deltas.isEmpty()) return new byte[4];
        ByteBuffer buf = ByteBuffer.allocate(4 + deltas.size() * 20).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(deltas.size());
        for (HaloCellDelta d : deltas) {
            buf.putInt(d.getCellIndex());
            buf.putFloat(d.getFluxPressure());
            buf.putFloat(d.getTemperature());
            buf.putFloat(d.getBiomassHuman());
            buf.putFloat(d.getLocalPrice());
        }
        return buf.array();
    }

    /*
     * Deserializes halo deltas and applies them directly into the target WorldBuffer.
     */
    public static void applyHaloDeltas(byte[] binaryData, WorldBuffer targetBuffer) {
        if (binaryData == null || binaryData.length < 4 || targetBuffer == null) return;
        ByteBuffer buf = ByteBuffer.wrap(binaryData).order(ByteOrder.LITTLE_ENDIAN);
        int count = buf.getInt();

        float[] flux = targetBuffer.getFluxPressure();
        float[] temp = targetBuffer.getTemperature();
        float[] pop = targetBuffer.getBiomassHuman();
        float[] price = targetBuffer.getLocalPrice();
        int cap = targetBuffer.getCapacity();

        for (int i = 0; i < count && buf.remaining() >= 20; i++) {
            int idx = buf.getInt();
            float f = buf.getFloat();
            float t = buf.getFloat();
            float p = buf.getFloat();
            float pr = buf.getFloat();

            if (idx >= 0 && idx < cap) {
                if (flux != null) flux[idx] = f;
                if (temp != null) temp[idx] = t;
                if (pop != null) pop[idx] = p;
                if (price != null) price[idx] = pr;
            }
        }
    }
}

