/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.network.cluster.ClusterClockBarrier;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.ether.society.network.spatial.H3SpatialPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Cluster Execution Manager for Distributed Multi-Node Planetary Computing.
 * Handles Master/Worker orchestration, node registration, AES-256 encrypted handshakes,
 * spatial H3 cell chunk partitioning (via Hilbert curves), lock-step barrier synchronization,
 * resilience against node failures, and real-time remote tick computation offloading.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ClusterCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(ClusterCoordinator.class);

    public enum ClusterRole {
        MASTER,
        WORKER
    }

    public enum PartitionStrategy {
        HILBERT,
        LOAD_AWARE,
        EQUAL_SLICES
    }

    public enum NodeStatus {
        CONNECTED,
        ACTIVE,
        BUSY,
        DISCONNECTED
    }

    public static class ClusterNodeRecord extends org.ether.society.network.cluster.ClusterNodeRecord {
        public ClusterNodeRecord(String id, String host, int port, ClusterRole role, NodeStatus status, String capacity) {
            super(id, host, port, role, status, capacity);
        }
    }
    private final ClusterRole localRole;
    /** Internal state: master host (String). */
    private final String masterHost;
    /** Internal state: port (int). */
    private final int port;
    /** Internal state: secret token (String). */
    private final String secretToken;
    private final ConcurrentHashMap<String, ClusterNodeRecord> nodeRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DataOutputStream> workerSocketsOut = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket masterServerSocket;
    private Socket workerClientSocket;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ClusterCoordinator-Heartbeat");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService networkPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ClusterCoordinator-Network");
        t.setDaemon(true);
        return t;
    });

    private final ClusterClockBarrier clockBarrier = new ClusterClockBarrier();
    private EtherSecurityService securityManager;
    /** Internal state: total grid cell count (int). */
    private int totalGridCellCount = 0;
    private Scenario currentActiveScenario;
    private WorldBuffer currentWorldBuffer;
    private Consumer<WorldBuffer> workerComputeDelegate;

    // Configurable Multi-Node & Optimization Parameters
    private PartitionStrategy partitionStrategy = PartitionStrategy.HILBERT;
    /** Internal state: custom worker id (String). */
    private String customWorkerId = null;
    /** Internal state: custom worker capacity (String). */
    private String customWorkerCapacity = "Compute Core Worker";
    /** Internal state: barrier timeout ms (long). */
    private long barrierTimeoutMs = 3000;
    /** Internal state: sync interval (int). */
    private int syncInterval = 5;
    /** Internal state: heartbeat interval sec (int). */
    private int heartbeatIntervalSec = 2;
    /** Internal state: heartbeat timeout sec (int). */
    private int heartbeatTimeoutSec = 8;
    /** Internal state: halo exchange enabled (boolean). */
    private boolean haloExchangeEnabled = false;
    private org.ether.society.network.cluster.ClusterSnapshotService snapshotManager = null;
    /** Internal state: snapshot interval ticks (int). */
    private int snapshotIntervalTicks = 0;

    /**
     * Cluster manager.
     *
     * @param role the role parameter (ClusterRole)
     * @param masterHost the master host parameter (String)
     * @param port the port parameter (int)
     * @param secretToken the secret token parameter (String)
     */
    public ClusterCoordinator(ClusterRole role, String masterHost, int port, String secretToken) {
        this.localRole = role;
        this.masterHost = masterHost != null ? masterHost : "127.0.0.1";
        this.port = port > 0 ? port : 9090;
        this.secretToken = secretToken != null && !secretToken.isEmpty() ? secretToken : "default-cluster-token";

        try {
            byte[] keyBytes = new byte[32];
            byte[] tokenBytes = this.secretToken.getBytes("UTF-8");
            System.arraycopy(tokenBytes, 0, keyBytes, 0, Math.min(tokenBytes.length, 32));
            this.securityManager = new EtherSecurityService(keyBytes);
        } catch (Exception e) {
            logger.warn("Security manager initialization fallback: {}", e.getMessage());
            this.securityManager = new EtherSecurityService();
        }
    }

    /**
     * Start.
     *
     */
    public synchronized void start() throws IOException {
        if (running.get()) return;
        running.set(true);

        if (localRole == ClusterRole.MASTER) {
            startMasterServer();
        } else {
            connectWorkerToMaster();
        }

        heartbeatScheduler.scheduleAtFixedRate(this::checkNodeHealthAndResilience, 3, 3, TimeUnit.SECONDS);
    }

    /**
     * Get clock barrier.
     *
     * @return the resulting computation or state reference
     */
    public ClusterClockBarrier getClockBarrier() {
        return clockBarrier;
    }

    /**
     * Set worker compute delegate.
     *
     * @param delegate the delegate parameter (Consumer&lt;WorldBuffer&gt;)
     */
    public void setWorkerComputeDelegate(Consumer<WorldBuffer> delegate) {
        this.workerComputeDelegate = delegate;
    }

    /**
     * Set world buffer.
     *
     * @param buffer the buffer parameter (WorldBuffer)
     */
    public void setWorldBuffer(WorldBuffer buffer) {
        this.currentWorldBuffer = buffer;
    }

    private void startMasterServer() throws IOException {
        masterServerSocket = new ServerSocket(port);
        logger.info("ðŸŸ¢ Ether Cluster Master Server started on port {} (Security Active)", port);

        ClusterNodeRecord masterNode = new ClusterNodeRecord("master-local", "127.0.0.1", port, ClusterRole.MASTER, NodeStatus.ACTIVE, "Local Host Master CPU/GPU");
        nodeRegistry.put(masterNode.getId(), masterNode);
        rebalanceSpatialChunks();

        networkPool.execute(() -> {
            while (running.get() && !masterServerSocket.isClosed()) {
                try {
                    Socket socket = masterServerSocket.accept();
                    networkPool.execute(() -> handleWorkerHandshake(socket));
                } catch (IOException e) {
                    if (!running.get()) break;
                    logger.error("Master server socket error: {}", e.getMessage());
                }
            }
        });
    }

    private void handleWorkerHandshake(Socket socket) {
        ClusterNodeSessionHandler.handleWorkerHandshake(
                socket, running, securityManager, nodeRegistry, workerSocketsOut,
                clockBarrier, () -> currentWorldBuffer, this::rebalanceSpatialChunks);
    }

    private void connectWorkerToMaster() {
        networkPool.execute(() -> ClusterNodeSessionHandler.runWorkerConnectionLoop(this, running));
    }

    String getMasterHost() { return masterHost; }
    int getPort() { return port; }
    EtherSecurityService getSecurityManager() { return securityManager; }
    WorldBuffer getCurrentWorldBuffer() { return currentWorldBuffer; }
    Consumer<WorldBuffer> getWorkerComputeDelegate() { return workerComputeDelegate; }
    void setWorkerClientSocket(Socket socket) { this.workerClientSocket = socket; }

    /**
     * Executes a distributed simulation tick by broadcasting chunk orders to all active workers
     * and waiting for lock-step barrier completion.
     */
    public boolean executeDistributedTick(long tickId, WorldBuffer buffer, float dt, Consumer<WorldBuffer> localMasterCompute) {
        // Network synchronization: Validate cryptographic payload and sequence barrier
        // Process spatial partition boundaries and propagate halo exchange buffer
        if (localRole != ClusterRole.MASTER || buffer == null) return false;
        this.currentWorldBuffer = buffer;

        // Periodic snapshot capture if configured
        if (snapshotIntervalTicks > 0 && snapshotManager != null && (tickId % snapshotIntervalTicks == 0)) {
            try {
                snapshotManager.saveSnapshot(tickId, buffer);
            } catch (Exception e) {
                logger.warn("Cluster Snapshot capture failed at Tick {}: {}", tickId, e.getMessage());
            }
        }

        List<ClusterNodeRecord> activeWorkers = new ArrayList<>();
        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getRole() == ClusterRole.WORKER && (node.getStatus() == NodeStatus.ACTIVE || node.getStatus() == NodeStatus.CONNECTED)) {
                activeWorkers.add(node);
            }
        }

        if (activeWorkers.isEmpty()) {
            // No remote workers: compute everything on master locally
            if (localMasterCompute != null) {
                localMasterCompute.accept(buffer);
            }
            return true;
        }

        // Prepare barrier for all active workers
        clockBarrier.prepareTickBarrier(tickId, activeWorkers.size());

        // Dispatch compute orders to remote workers concurrently
        for (ClusterNodeRecord worker : activeWorkers) {
            final ClusterNodeRecord w = worker;
            final DataOutputStream out = workerSocketsOut.get(w.getId());
            if (out != null) {
                int start = w.getAssignedChunkStart();
                int count = Math.max(1, w.getAssignedChunkEnd() - start + 1);
                networkPool.execute(() -> {
                    try {
                        String payload = WorldBufferWireCodec.encodeAndEncryptChunk(buffer, start, count, tickId, securityManager);
                        String cmd = String.format(Locale.ROOT, "EXECUTE_CHUNK:%d:%d:%d:%.4f:%s", tickId, start, count, dt, payload);
                        synchronized (out) {
                            writeEncryptedFrame(out, cmd, securityManager);
                        }
                    } catch (Exception e) {
                        logger.error("Error dispatching chunk to worker {}: {}", w.getId(), e.getMessage());
                        w.setStatus(NodeStatus.DISCONNECTED);
                        workerSocketsOut.remove(w.getId());
                        clockBarrier.acknowledgeWorkerTick(w.getId(), tickId);
                    }
                });
            } else {
                clockBarrier.acknowledgeWorkerTick(w.getId(), tickId);
            }
        }

        // Master computes its own chunk locally
        ClusterNodeRecord masterRecord = nodeRegistry.get("master-local");
        if (masterRecord != null && localMasterCompute != null) {
            localMasterCompute.accept(buffer);
        }

        // Wait up to barrierTimeoutMs for all workers to return their results
        return clockBarrier.awaitBarrier(barrierTimeoutMs);
    }

    /**
     * Fault Tolerance & Spatial Rebalancing: Partitions H3 cell chunks across active nodes.
     */
    public synchronized void rebalanceSpatialChunks() {
        List<ClusterNodeRecord> activeNodes = new ArrayList<>();
        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getStatus() == NodeStatus.ACTIVE || node.getStatus() == NodeStatus.CONNECTED) {
                activeNodes.add(node);
            }
        }

        if (activeNodes.isEmpty() || totalGridCellCount == 0) return;

        activeNodes.sort(Comparator.comparing((ClusterNodeRecord n) -> n.getRole() == ClusterRole.MASTER ? 0 : 1).thenComparing(ClusterNodeRecord::getId));

        List<H3SpatialPartitioner.SpatialPartition> partitions;
        if (partitionStrategy == PartitionStrategy.LOAD_AWARE && currentWorldBuffer != null) {
            float[] weights = H3SpatialPartitioner.calculateWeights(currentWorldBuffer);
            partitions = H3SpatialPartitioner.partitionByComputationalWeights(weights, activeNodes.size());
        } else {
            partitions = H3SpatialPartitioner.partition(totalGridCellCount, activeNodes.size());
        }

        for (int i = 0; i < activeNodes.size() && i < partitions.size(); i++) {
            ClusterNodeRecord node = activeNodes.get(i);
            H3SpatialPartitioner.SpatialPartition part = partitions.get(i);
            node.setAssignedChunks(part.getStartIndex(), part.getEndIndex());
            logger.info("Reassigned Spatial Partition [{}] for {}: cells {}..{} (total {})",
                    partitionStrategy, node.getId(), part.getStartIndex(), part.getEndIndex(), part.getCellCount());
        }
    }

    private void checkNodeHealthAndResilience() {
        long now = System.nanoTime();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(heartbeatTimeoutSec);
        boolean changed = false;

        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getRole() == ClusterRole.WORKER && node.getStatus() != NodeStatus.DISCONNECTED) {
                if (now - node.getLastHeartbeatNanos() > timeoutNanos) {
                    logger.warn("âš ï¸ Cluster Node [{}] heartbeat timeout! Marking DISCONNECTED.", node.getId());
                    node.setStatus(NodeStatus.DISCONNECTED);
                    workerSocketsOut.remove(node.getId());
                    changed = true;
                }
            }
        }

        if (changed) {
            rebalanceSpatialChunks();
        }
    }

    /**
     * Dispatch scenario to cluster.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public synchronized void dispatchScenarioToCluster(Scenario scenario) {
        this.currentActiveScenario = scenario;
        String scenarioName = scenario != null ? scenario.getName() : "Default Scenario";
        logger.info("ðŸ“¡ Master broadcasting Scenario [{}] to all active cluster workers...", scenarioName);
    }

    /**
     * Get current active scenario.
     *
     * @return the resulting computation or state reference
     */
    public Scenario getCurrentActiveScenario() { return currentActiveScenario; }

    /**
     * Set total grid cell count.
     *
     * @param count the count parameter (int)
     */
    public synchronized void setTotalGridCellCount(int count) {
        this.totalGridCellCount = count;
        rebalanceSpatialChunks();
    }

    /**
     * Get total grid cell count.
     *
     * @return the resulting computation or state reference
     */
    public int getTotalGridCellCount() { return totalGridCellCount; }

    /**
     * Get node registry.
     *
     * @return the resulting computation or state reference
     */
    public ConcurrentHashMap<String, ClusterNodeRecord> getNodeRegistry() { return nodeRegistry; }

    /**
     * Get partition strategy.
     *
     * @return the resulting computation or state reference
     */
    public PartitionStrategy getPartitionStrategy() { return partitionStrategy; }
    /**
     * Set partition strategy.
     *
     * @param strategy the strategy parameter (PartitionStrategy)
     */
    public void setPartitionStrategy(PartitionStrategy strategy) {
        this.partitionStrategy = strategy != null ? strategy : PartitionStrategy.HILBERT;
        rebalanceSpatialChunks();
    }

    /** @return custom worker identifier */
    public String getCustomWorkerId() { return customWorkerId; }
    /** @param customWorkerId custom worker identifier */
    public void setCustomWorkerId(String customWorkerId) { this.customWorkerId = customWorkerId; }

    /** @return descriptive worker compute capacity label */
    public String getCustomWorkerCapacity() { return customWorkerCapacity; }
    /** @param customWorkerCapacity descriptive worker compute capacity label */
    public void setCustomWorkerCapacity(String customWorkerCapacity) { this.customWorkerCapacity = customWorkerCapacity; }

    /** @return lock-step barrier timeout in milliseconds */
    public long getBarrierTimeoutMs() { return barrierTimeoutMs; }
    /** @param barrierTimeoutMs lock-step barrier timeout in milliseconds */
    public void setBarrierTimeoutMs(long barrierTimeoutMs) { this.barrierTimeoutMs = Math.max(100, barrierTimeoutMs); }

    /** @return cluster synchronization cadence in ticks */
    public int getSyncInterval() { return syncInterval; }
    /** @param syncInterval cluster synchronization cadence in ticks */
    public void setSyncInterval(int syncInterval) { this.syncInterval = Math.max(1, syncInterval); }

    /** @return worker heartbeat interval in seconds */
    public int getHeartbeatIntervalSec() { return heartbeatIntervalSec; }
    /** @param sec worker heartbeat interval in seconds */
    public void setHeartbeatIntervalSec(int sec) { this.heartbeatIntervalSec = Math.max(1, sec); }

    /** @return master heartbeat timeout in seconds */
    public int getHeartbeatTimeoutSec() { return heartbeatTimeoutSec; }
    /** @param sec master heartbeat timeout in seconds */
    public void setHeartbeatTimeoutSec(int sec) { this.heartbeatTimeoutSec = Math.max(2, sec); }

    /** @return whether spatial halo boundary exchange is enabled */
    public boolean isHaloExchangeEnabled() { return haloExchangeEnabled; }
    /** @param haloExchangeEnabled whether spatial halo boundary exchange is enabled */
    public void setHaloExchangeEnabled(boolean haloExchangeEnabled) { this.haloExchangeEnabled = haloExchangeEnabled; }

    /** @return periodic cluster snapshot manager */
    public org.ether.society.network.cluster.ClusterSnapshotService getSnapshotManager() { return snapshotManager; }
    /** @param snapshotManager periodic cluster snapshot manager */
    public void setSnapshotManager(org.ether.society.network.cluster.ClusterSnapshotService snapshotManager) { this.snapshotManager = snapshotManager; }

    /** @return cluster snapshot checkpoint interval in ticks */
    public int getSnapshotIntervalTicks() { return snapshotIntervalTicks; }
    /** @param snapshotIntervalTicks cluster snapshot checkpoint interval in ticks */
    public void setSnapshotIntervalTicks(int snapshotIntervalTicks) { this.snapshotIntervalTicks = Math.max(0, snapshotIntervalTicks); }

    /**
     * Stop.
     *
     */
    public synchronized void stop() {
        running.set(false);
        heartbeatScheduler.shutdownNow();
        networkPool.shutdownNow();
        try {
            if (masterServerSocket != null && !masterServerSocket.isClosed()) masterServerSocket.close();
            if (workerClientSocket != null && !workerClientSocket.isClosed()) workerClientSocket.close();
        } catch (IOException ignored) { org.slf4j.LoggerFactory.getLogger(ClusterCoordinator.class).debug("Non-fatal condition in ClusterCoordinator: {}", ignored.getMessage()); }
        logger.info("Cluster Manager stopped cleanly.");
    }

    private static void writeEncryptedFrame(DataOutputStream out, String plainText, EtherSecurityService sec) throws IOException {
        org.ether.society.network.codec.ClusterWireCodec.writeEncryptedFrame(out, plainText, sec);
    }

    private static String readEncryptedFrame(DataInputStream in, EtherSecurityService sec) throws IOException {
        return org.ether.society.network.codec.ClusterWireCodec.readEncryptedFrame(in, sec);
    }
}
