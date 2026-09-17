/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;
import java.util.List;

/**
 * Zero-Copy Native Off-Heap Planetary State Buffer.
 * Stores dense cellular matrices in contiguous unmanaged off-heap memory
 * to eliminate JVM Garbage Collector pauses during high-frequency simulation ticks.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PanamaWorldBuffer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PanamaWorldBuffer.class);

    private final int capacity;

    // Direct Off-Heap Native Buffers
    private final DoubleBuffer temperatureBuffer;
    private final DoubleBuffer elevationBuffer;
    private final LongBuffer populationBuffer;
    private final DoubleBuffer waterBuffer;
    private final DoubleBuffer foodBuffer;

    public PanamaWorldBuffer(int capacity) {
        this.capacity = capacity;

        this.temperatureBuffer = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.elevationBuffer = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.populationBuffer = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();
        this.waterBuffer = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.foodBuffer = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();

        logger.info("⚡ Allocated 5 Direct Off-Heap Memory Buffers ({} cells, {} KB off-heap).",
                capacity, (capacity * (Double.BYTES * 4 + Long.BYTES)) / 1024);
    }

    /**
     * Ingests a list of H3 cells into contiguous off-heap direct memory buffers.
     */
    public void ingestCells(List<H3Cell> cells) {
        if (cells == null) return;
        int count = Math.min(capacity, cells.size());
        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            temperatureBuffer.put(i, c.getTemperature() != null ? c.getTemperature() : 15.0);
            elevationBuffer.put(i, c.getElevation() != null ? c.getElevation() : 0.0);
            populationBuffer.put(i, c.getPopulation() != null ? (long) c.getPopulation() : 0L);
            waterBuffer.put(i, c.getWaterResource() != null ? c.getWaterResource() : 0.0);
            foodBuffer.put(i, c.getFoodResource() != null ? c.getFoodResource() : 0.0);
        }
    }

    public double getTemperature(int index) {
        return temperatureBuffer.get(index);
    }

    public void setTemperature(int index, double value) {
        temperatureBuffer.put(index, value);
    }

    public long getPopulation(int index) {
        return populationBuffer.get(index);
    }

    public void setPopulation(int index, long value) {
        populationBuffer.put(index, value);
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public void close() {
        logger.info("🗑️ Cleared Panama Off-Heap WorldBuffer native memory buffers.");
    }
}
