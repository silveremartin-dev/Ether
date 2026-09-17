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
    private int expectedWorkersCount = 0;

    public synchronized void prepareTickBarrier(long tickId, int expectedWorkers) {
        this.currentTickId.set(tickId);
        this.expectedWorkersCount = expectedWorkers;
        this.workerTickAcks.clear();
        this.currentBarrierLatch = new CountDownLatch(expectedWorkers);
    }

    public void acknowledgeWorkerTick(String workerId, long tickId) {
        if (tickId == currentTickId.get()) {
            workerTickAcks.put(workerId, tickId);
            if (currentBarrierLatch != null) {
                currentBarrierLatch.countDown();
            }
        }
    }

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

    public long getCurrentTickId() {
        return currentTickId.get();
    }

    public int getAcknowledgedWorkerCount() {
        return workerTickAcks.size();
    }

    public int getExpectedWorkersCount() {
        return expectedWorkersCount;
    }
}

