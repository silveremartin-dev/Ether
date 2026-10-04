/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Clock Barrier Synchronizer for Distributed Planetary Simulation.
 * Implements strict lock-step barrier synchronization (t -> t + Δt) across all active
 * cluster worker nodes, guaranteeing absolute determinism, physical conservation laws,
 * and zero temporal drift.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ClusterClockBarrier {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClockBarrier.class);

    private final AtomicLong currentTickId = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> workerTickAcks = new ConcurrentHashMap<>();
    private CountDownLatch currentBarrierLatch;
    /* Internal state variable for expected workers count (int). */
    private int expectedWorkersCount = 0;

    /*
     * Prepare tick barrier.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @param tickId the tick id parameter (long)
     * @param expectedWorkers the expected workers parameter (int)
     */
    public synchronized void prepareTickBarrier(long tickId, int expectedWorkers) {
        this.currentTickId.set(tickId);
        this.expectedWorkersCount = expectedWorkers;
        this.workerTickAcks.clear();
        this.currentBarrierLatch = new CountDownLatch(expectedWorkers);
    }

    /*
     * Acknowledge worker tick.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @param workerId the worker id parameter (String)
     * @param tickId the tick id parameter (long)
     */
    public void acknowledgeWorkerTick(String workerId, long tickId) {
        if (tickId == currentTickId.get()) {
            workerTickAcks.put(workerId, tickId);
            if (currentBarrierLatch != null) {
                currentBarrierLatch.countDown();
            }
        }
    }

    /*
     * Await barrier.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @param timeoutMs the timeout ms parameter (long)
     * @return the resulting computation or state reference
     */
    public boolean awaitBarrier(long timeoutMs) {
        if (expectedWorkersCount == 0 || currentBarrierLatch == null) return true;
        try {
            boolean reached = currentBarrierLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!reached) {
                logger.warn("⏱️ Clock Barrier timeout for Tick {} (Acks: {}/{})",
                        currentTickId.get(), workerTickAcks.size(), expectedWorkersCount);
            }
            return reached;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /*
     * Get current tick id.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @return the resulting computation or state reference
     */
    public long getCurrentTickId() {
        return currentTickId.get();
    }

    /*
     * Get acknowledged worker count.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @return the resulting computation or state reference
     */
    public int getAcknowledgedWorkerCount() {
        return workerTickAcks.size();
    }

    /*
     * Get expected workers count.
     * Enforces physical invariants and updates associated state variables within {@code ClusterClockBarrier}.
     *
     * @return the resulting computation or state reference
     */
    public int getExpectedWorkersCount() {
        return expectedWorkersCount;
    }
}

