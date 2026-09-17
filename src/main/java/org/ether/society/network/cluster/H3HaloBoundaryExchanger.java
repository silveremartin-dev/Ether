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
 * @version 1.0.0-beta.1
 */
public class H3HaloBoundaryExchanger {

    public static class HaloCellDelta {
        private final int cellIndex;
        private final float fluxPressure;
        private final float temperature;
        private final float biomassHuman;
        private final float localPrice;

        public HaloCellDelta(int cellIndex, float fluxPressure, float temperature, float biomassHuman, float localPrice) {
            this.cellIndex = cellIndex;
            this.fluxPressure = fluxPressure;
            this.temperature = temperature;
            this.biomassHuman = biomassHuman;
            this.localPrice = localPrice;
        }

        public int getCellIndex() { return cellIndex; }
        public float getFluxPressure() { return fluxPressure; }
        public float getTemperature() { return temperature; }
        public float getBiomassHuman() { return biomassHuman; }
        public float getLocalPrice() { return localPrice; }
    }

    /**
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

    /**
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

    /**
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

