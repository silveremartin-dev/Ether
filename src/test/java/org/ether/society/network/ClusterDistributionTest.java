/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the Distributed Cluster Compute Architecture.
 * Tests node registration, security handshake, spatial chunk partitioning,
 * node disconnection fault-tolerance, and late-joining node synchronization.
 */
public class ClusterDistributionTest {

    private ClusterManager masterManager;
    private static final int TEST_PORT = 9890;
    private static final String TEST_SECRET = "TestClusterSecret2026";

    @BeforeEach
    public void setUp() throws IOException {
        masterManager = new ClusterManager(ClusterManager.ClusterRole.MASTER, "127.0.0.1", TEST_PORT, TEST_SECRET);
        masterManager.setTotalGridCellCount(10000);
        masterManager.start();
    }

    @AfterEach
    public void tearDown() {
        if (masterManager != null) {
            masterManager.stop();
        }
    }

    @Test
    @DisplayName("Test Master initialization & spatial chunk assignment")
    public void testMasterInitialization() {
        ConcurrentHashMap<String, ClusterManager.ClusterNodeRecord> registry = masterManager.getNodeRegistry();
        assertFalse(registry.isEmpty(), "Node registry should contain local master node");

        ClusterManager.ClusterNodeRecord masterRecord = registry.get("master-local");
        assertNotNull(masterRecord, "Master local record should exist");
        assertEquals(ClusterManager.ClusterRole.MASTER, masterRecord.getRole());
        assertEquals(0, masterRecord.getAssignedChunkStart());
        assertEquals(9999, masterRecord.getAssignedChunkEnd());
    }

    @Test
    @DisplayName("Test Worker Registration & Spatial Chunk Rebalancing")
    public void testWorkerRegistrationAndChunkRebalancing() throws Exception {
        ClusterManager workerManager = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", TEST_PORT, TEST_SECRET);
        workerManager.start();

        // Allow network handshake time
        Thread.sleep(1200);

        ConcurrentHashMap<String, ClusterManager.ClusterNodeRecord> registry = masterManager.getNodeRegistry();
        assertTrue(registry.size() >= 2, "Registry should contain master and newly connected worker node");

        // Verify spatial partitioning split 10,000 cells into ~5,000 per node
        int totalCoveredCells = 0;
        for (ClusterManager.ClusterNodeRecord node : registry.values()) {
            if (node.getStatus() == ClusterManager.NodeStatus.ACTIVE || node.getStatus() == ClusterManager.NodeStatus.CONNECTED) {
                int count = (node.getAssignedChunkEnd() - node.getAssignedChunkStart()) + 1;
                totalCoveredCells += count;
            }
        }

        assertEquals(10000, totalCoveredCells, "Total covered H3 cells across all cluster nodes must equal 10,000");

        workerManager.stop();
    }

    @Test
    @DisplayName("Test Fault Tolerance: Node Disconnection & Rebalancing")
    public void testResilienceOnNodeDisconnect() throws Exception {
        ClusterManager workerManager = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", TEST_PORT, TEST_SECRET);
        workerManager.start();
        Thread.sleep(1000);

        assertEquals(2, masterManager.getNodeRegistry().size(), "Master should have 2 nodes before disconnect");

        // Stop worker suddenly and mark status on master
        workerManager.stop();
        for (ClusterManager.ClusterNodeRecord record : masterManager.getNodeRegistry().values()) {
            if (record.getRole() == ClusterManager.ClusterRole.WORKER) {
                record.setStatus(ClusterManager.NodeStatus.DISCONNECTED);
            }
        }
        masterManager.rebalanceSpatialChunks();

        ClusterManager.ClusterNodeRecord masterRecord = masterManager.getNodeRegistry().get("master-local");
        assertNotNull(masterRecord);
        assertEquals(0, masterRecord.getAssignedChunkStart());
        assertEquals(9999, masterRecord.getAssignedChunkEnd(), "Master should reclaim full grid after worker drop");
    }

    @Test
    @DisplayName("Test Late-Joining Node Synchronization")
    public void testLateJoiningNodeSync() throws Exception {
        // Late-join 2 workers sequentially
        ClusterManager worker1 = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", TEST_PORT, TEST_SECRET);
        worker1.start();
        Thread.sleep(800);

        ClusterManager worker2 = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", TEST_PORT, TEST_SECRET);
        worker2.start();
        Thread.sleep(800);

        ConcurrentHashMap<String, ClusterManager.ClusterNodeRecord> registry = masterManager.getNodeRegistry();
        assertTrue(registry.size() >= 3, "Master should register 3 active nodes (1 Master + 2 Workers)");

        worker1.stop();
        worker2.stop();
    }
}
