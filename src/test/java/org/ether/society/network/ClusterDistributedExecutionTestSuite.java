/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.cluster.ClusterClockBarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterDistributedExecutionTestSuite {

    private static final int TEST_PORT = 9988;
    private static final String SECRET = "TestClusterToken2026";

    private ClusterManager master;
    private ClusterManager worker;

    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        master = new ClusterManager(ClusterManager.ClusterRole.MASTER, "127.0.0.1", TEST_PORT, SECRET);
        master.setTotalGridCellCount(200);
        master.start();

        worker = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", TEST_PORT, SECRET);
        worker.start();

        // Dynamically wait up to 3 seconds for worker registration
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && master.getNodeRegistry().size() < 2) {
            Thread.sleep(50);
        }
    }

    @AfterEach
    public void tearDown() {
        if (worker != null) worker.stop();
        if (master != null) master.stop();
    }

    @Test
    public void testWorkerRegistrationAndSpatialChunkRebalancing() {
        assertEquals(2, master.getNodeRegistry().size(), "Master should have 2 nodes (master-local + worker)");

        ClusterManager.ClusterNodeRecord masterRecord = master.getNodeRegistry().get("master-local");
        assertNotNull(masterRecord);
        assertEquals(0, masterRecord.getAssignedChunkStart());
        assertEquals(99, masterRecord.getAssignedChunkEnd());

        boolean hasWorker = master.getNodeRegistry().values().stream()
                .anyMatch(n -> n.getRole() == ClusterManager.ClusterRole.WORKER && n.getAssignedChunkStart() == 100 && n.getAssignedChunkEnd() == 199);
        assertTrue(hasWorker, "Worker should be assigned chunk 100..199");
    }

    @Test
    public void testClockBarrierSynchronization() {
        ClusterClockBarrier barrier = new ClusterClockBarrier();
        barrier.prepareTickBarrier(1L, 2);

        assertEquals(1L, barrier.getCurrentTickId());
        assertEquals(0, barrier.getAcknowledgedWorkerCount());

        barrier.acknowledgeWorkerTick("worker-1", 1L);
        assertEquals(1, barrier.getAcknowledgedWorkerCount());

        barrier.acknowledgeWorkerTick("worker-2", 1L);
        assertTrue(barrier.awaitBarrier(100), "Barrier should be reached when all workers acknowledge");
    }

    @Test
    public void testDistributedTickExecutionPipeline() throws InterruptedException {
        WorldBuffer buffer = new WorldBuffer(200);
        buffer.getElevation()[5] = 100f;
        buffer.getElevation()[150] = 500f;

        AtomicBoolean workerComputed = new AtomicBoolean(false);
        worker.setWorkerComputeDelegate(buf -> {
            workerComputed.set(true);
            buf.getElevation()[150] += 50f;
        });

        AtomicBoolean masterComputed = new AtomicBoolean(false);
        boolean success = master.executeDistributedTick(1L, buffer, 86400f, buf -> {
            masterComputed.set(true);
            buf.getElevation()[5] += 10f;
        });

        assertTrue(success, "Distributed tick should succeed");
        assertTrue(masterComputed.get(), "Master should execute its local chunk");
        assertEquals(110f, buffer.getElevation()[5], 1e-4);
    }
}
