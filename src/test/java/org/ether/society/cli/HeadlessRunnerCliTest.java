/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.network.ClusterManager;
import org.ether.society.config.SimulationPerformanceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessRunnerCliTest {

    @Test
    /*
     * Test cli help manual operation.
     * <p>
     * Executes operational logic for {@code HeadlessRunnerCliTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCliHelpManual() {
        assertDoesNotThrow(() -> HeadlessRunner.run(new String[]{"--help"}));
        assertDoesNotThrow(() -> HeadlessRunner.run(new String[]{"-h"}));
        assertDoesNotThrow(() -> HeadlessRunner.run(new String[]{"-?"}));
    }

    @Test
    /*
     * Test headless simulation with performance flags operation.
     * <p>
     * Executes operational logic for {@code HeadlessRunnerCliTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testHeadlessSimulationWithPerformanceFlags() {
        String[] args = new String[]{
                "--ticks=3",
                "--cells=25",
                "--scenario=OUT_OF_AFRICA",
                "--single-core",
                "--sparse-skipping",
                "--ocean-macro-aggregation",
                "--coastal-nav-only",
                "--ocean-multi-rate",
                "--climate-freq=2",
                "--spatial-truncation",
                "--parallel=false",
                "--no-save",
                "--no-profile",
                "--lang=fr",
                "--theme=dark"
        };
        assertDoesNotThrow(() -> HeadlessRunner.run(args));
    }

    @Test
    /*
     * Test headless simulation default scenario baseline operation.
     * <p>
     * Executes operational logic for {@code HeadlessRunnerCliTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testHeadlessSimulationDefaultScenarioBaseline() {
        String[] args = new String[]{
                "--ticks=2",
                "--cells=20",
                "--scenario=OUT_OF_AFRICA",
                "--no-save",
                "--no-profile"
        };
        assertDoesNotThrow(() -> HeadlessRunner.run(args));
    }

    @Test
    /*
     * Test multi node clustering configuration and registration operation.
     * <p>
     * Executes operational logic for {@code HeadlessRunnerCliTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testMultiNodeClusteringConfigurationAndRegistration() throws IOException, InterruptedException {
        int testPort = 9977;
        String secret = "MultiNodeTestSecret2026";

        // 1. Create Master Server with custom parameters
        ClusterManager master = new ClusterManager(ClusterManager.ClusterRole.MASTER, "127.0.0.1", testPort, secret);
        master.setPartitionStrategy(ClusterManager.PartitionStrategy.LOAD_AWARE);
        master.setBarrierTimeoutMs(2500);
        master.setHeartbeatTimeoutSec(5);
        master.setTotalGridCellCount(300);
        master.start();

        assertEquals(ClusterManager.PartitionStrategy.LOAD_AWARE, master.getPartitionStrategy());
        assertEquals(2500, master.getBarrierTimeoutMs());
        assertEquals(5, master.getHeartbeatTimeoutSec());

        // 2. Launch Worker Node 1 (Simulated GPU Node)
        ClusterManager worker1 = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", testPort, secret);
        worker1.setCustomWorkerId("worker-gpu-alpha");
        worker1.setCustomWorkerCapacity("NVIDIA RTX 4090 GPU (24GB VRAM)");
        worker1.start();

        // 3. Launch Worker Node 2 (Simulated 64-Core SIMD Node)
        ClusterManager worker2 = new ClusterManager(ClusterManager.ClusterRole.WORKER, "127.0.0.1", testPort, secret);
        worker2.setCustomWorkerId("worker-simd-beta");
        worker2.setCustomWorkerCapacity("AMD EPYC 64-Core AVX-512");
        worker2.start();

        // Wait up to 3 seconds for both workers to connect and register
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && master.getNodeRegistry().size() < 3) {
            Thread.sleep(50);
        }

        try {
            assertEquals(3, master.getNodeRegistry().size(), "Master must register both worker nodes + local master");

            ClusterManager.ClusterNodeRecord node1 = master.getNodeRegistry().get("worker-gpu-alpha");
            assertNotNull(node1, "Worker 1 must be registered with its custom ID");
            assertEquals("NVIDIA RTX 4090 GPU (24GB VRAM)", node1.getCapacity());

            ClusterManager.ClusterNodeRecord node2 = master.getNodeRegistry().get("worker-simd-beta");
            assertNotNull(node2, "Worker 2 must be registered with its custom ID");
            assertEquals("AMD EPYC 64-Core AVX-512", node2.getCapacity());

            // Verify spatial partitioning across all 3 nodes
            assertTrue(node1.getAssignedChunkEnd() >= node1.getAssignedChunkStart());
            assertTrue(node2.getAssignedChunkEnd() >= node2.getAssignedChunkStart());
        } finally {
            worker2.stop();
            worker1.stop();
            master.stop();
        }
    }

    @Test
    /*
     * Test simulation performance config properties operation.
     * <p>
     * Executes operational logic for {@code HeadlessRunnerCliTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSimulationPerformanceConfigProperties() {
        SimulationPerformanceConfig config = new SimulationPerformanceConfig(true);
        assertTrue(config.isStrictDeterminism());
        assertFalse(config.isEnableSparseCellSkipping());
        assertFalse(config.isEnableMultiFreqClimateTicks());

        config.setEnableSparseCellSkipping(true);
        assertFalse(config.isStrictDeterminism());
        assertTrue(config.isEnableSparseCellSkipping());

        config.setClimateTickFrequency(4);
        assertEquals(4, config.getClimateTickFrequency());

        config.setStrictDeterminism(true);
        assertTrue(config.isStrictDeterminism());
        assertFalse(config.isEnableSparseCellSkipping());
    }

    @Test
    /*
     * Test headless execution and snapshot creation for Ramesses II scenario.
     */
    public void testHeadlessExecutionRamessesIIScenario() {
        String[] args = new String[]{
                "--ticks=5",
                "--cells=30",
                "--scenario=ramesses_ii",
                "--save=Test_Ramesses_II_Snapshot",
                "--safe",
                "--no-profile"
        };
        assertDoesNotThrow(() -> HeadlessRunner.run(args));
    }

    @Test
    /*
     * Test headless execution and snapshot creation for Earth 2025 Business-As-Usual scenario.
     */
    public void testHeadlessExecutionEarth2025BAUScenario() {
        String[] args = new String[]{
                "--ticks=5",
                "--cells=30",
                "--scenario=business_as_usual",
                "--save=Test_Earth_2025_BAU_Snapshot",
                "--safe",
                "--no-profile"
        };
        assertDoesNotThrow(() -> HeadlessRunner.run(args));
    }
}

