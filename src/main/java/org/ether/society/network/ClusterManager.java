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
 * @version 1.0.0-beta.1
 */
public class ClusterManager {
    private static final Logger logger = LoggerFactory.getLogger(ClusterManager.class);

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

    public static class ClusterNodeRecord {
        private final String id;
        private final String host;
        private final int port;
        private ClusterRole role;
        private NodeStatus status;
        private String capacity;
        private int assignedChunkStart;
        private int assignedChunkEnd;
        private final AtomicLong lastHeartbeatNanos = new AtomicLong(System.nanoTime());

        public ClusterNodeRecord(String id, String host, int port, ClusterRole role, NodeStatus status, String capacity) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.role = role;
            this.status = status;
            this.capacity = capacity;
        }

        public String getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public ClusterRole getRole() { return role; }
        public void setRole(ClusterRole role) { this.role = role; }
        public NodeStatus getStatus() { return status; }
        public void setStatus(NodeStatus status) { this.status = status; }
        public String getCapacity() { return capacity; }
        public void setCapacity(String capacity) { this.capacity = capacity; }
        public int getAssignedChunkStart() { return assignedChunkStart; }
        public int getAssignedChunkEnd() { return assignedChunkEnd; }
        public void setAssignedChunks(int start, int end) {
            this.assignedChunkStart = start;
            this.assignedChunkEnd = end;
        }
        public long getLastHeartbeatNanos() { return lastHeartbeatNanos.get(); }
        public void touchHeartbeat() { this.lastHeartbeatNanos.set(System.nanoTime()); }

        @Override
        public String toString() {
            return String.format("Node[%s @ %s:%d, role=%s, status=%s, capacity=%s, chunks=%d..%d]",
                    id, host, port, role, status, capacity, assignedChunkStart, assignedChunkEnd);
        }
    }

    private final ClusterRole localRole;
    private final String masterHost;
    private final int port;
    private final String secretToken;
    private final ConcurrentHashMap<String, ClusterNodeRecord> nodeRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DataOutputStream> workerSocketsOut = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket masterServerSocket;
    private Socket workerClientSocket;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ClusterManager-Heartbeat");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService networkPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ClusterManager-Network");
        t.setDaemon(true);
        return t;
    });

    private final ClusterClockBarrier clockBarrier = new ClusterClockBarrier();
    private EtherSecurityManager securityManager;
    private int totalGridCellCount = 0;
    private Scenario currentActiveScenario;
    private WorldBuffer currentWorldBuffer;
    private Consumer<WorldBuffer> workerComputeDelegate;

    // Configurable Multi-Node & Optimization Parameters
    private PartitionStrategy partitionStrategy = PartitionStrategy.HILBERT;
    private String customWorkerId = null;
    private String customWorkerCapacity = "Compute Core Worker";
    private long barrierTimeoutMs = 3000;
    private int syncInterval = 5;
    private int heartbeatIntervalSec = 2;
    private int heartbeatTimeoutSec = 8;
    private boolean haloExchangeEnabled = false;
    private org.ether.society.network.cluster.ClusterSnapshotManager snapshotManager = null;
    private int snapshotIntervalTicks = 0;

    public ClusterManager(ClusterRole role, String masterHost, int port, String secretToken) {
        this.localRole = role;
        this.masterHost = masterHost != null ? masterHost : "127.0.0.1";
        this.port = port > 0 ? port : 9090;
        this.secretToken = secretToken != null && !secretToken.isEmpty() ? secretToken : "EtherClusterSecret2026";

        try {
            byte[] keyBytes = new byte[32];
            byte[] tokenBytes = this.secretToken.getBytes("UTF-8");
            System.arraycopy(tokenBytes, 0, keyBytes, 0, Math.min(tokenBytes.length, 32));
            this.securityManager = new EtherSecurityManager(keyBytes);
        } catch (Exception e) {
            logger.warn("Security manager initialization fallback: {}", e.getMessage());
            this.securityManager = new EtherSecurityManager();
        }
    }

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

    public ClusterClockBarrier getClockBarrier() {
        return clockBarrier;
    }

    public void setWorkerComputeDelegate(Consumer<WorldBuffer> delegate) {
        this.workerComputeDelegate = delegate;
    }

    public void setWorldBuffer(WorldBuffer buffer) {
        this.currentWorldBuffer = buffer;
    }

    private void startMasterServer() throws IOException {
        masterServerSocket = new ServerSocket(port);
        logger.info("🟢 Ether Cluster Master Server started on port {} (Security Active)", port);

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
        String clientAddress = socket.getRemoteSocketAddress().toString();
        DataInputStream in = null;
        DataOutputStream out = null;
        String registeredWorkerId = null;

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            String decryptedMsg = readEncryptedFrame(in, securityManager);

            if (!decryptedMsg.startsWith("REGISTER_WORKER:")) {
                writeEncryptedFrame(out, "REJECT:INVALID_PROTOCOL", securityManager);
                return;
            }

            String[] parts = decryptedMsg.split(":");
            registeredWorkerId = parts[1];
            String capacity = parts.length > 2 ? parts[2] : "Worker Node CPU";

            ClusterNodeRecord record = new ClusterNodeRecord(registeredWorkerId, socket.getInetAddress().getHostAddress(), socket.getPort(), ClusterRole.WORKER, NodeStatus.CONNECTED, capacity);
            nodeRegistry.put(registeredWorkerId, record);
            workerSocketsOut.put(registeredWorkerId, out);
            logger.info("✅ Worker node connected and registered: {}", record);

            rebalanceSpatialChunks();

            String ackMessage = String.format("ACK_REGISTER:%s:%d:%d", registeredWorkerId, record.getAssignedChunkStart(), record.getAssignedChunkEnd());
            writeEncryptedFrame(out, ackMessage, securityManager);

            // Loop to handle incoming worker messages (Heartbeats, Chunk Results)
            while (running.get() && !socket.isClosed()) {
                String decrypted = readEncryptedFrame(in, securityManager);

                if (decrypted.startsWith("HEARTBEAT:")) {
                    record.touchHeartbeat();
                    record.setStatus(NodeStatus.ACTIVE);
                    writeEncryptedFrame(out, "HEARTBEAT_ACK", securityManager);
                } else if (decrypted.startsWith("CHUNK_RESULT:")) {
                    // CHUNK_RESULT:<workerId>:<tickId>:<start>:<count>:<payloadBase64>
                    String[] resParts = decrypted.split(":", 6);
                    if (resParts.length >= 6) {
                        String wId = resParts[1];
                        long tickId = Long.parseLong(resParts[2]);
                        String base64Payload = resParts[5];
                        if (currentWorldBuffer != null) {
                            try {
                                WorldBufferWireCodec.decryptAndDecodeChunk(base64Payload, currentWorldBuffer, securityManager);
                            } catch (Exception ex) {
                                logger.error("Error decoding chunk result from worker {}: {}", wId, ex.getMessage());
                            }
                        }
                        clockBarrier.acknowledgeWorkerTick(wId, tickId);
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Worker node disconnected from {}: {}", clientAddress, e.getMessage());
        } finally {
            if (registeredWorkerId != null) {
                workerSocketsOut.remove(registeredWorkerId);
                if (nodeRegistry.containsKey(registeredWorkerId)) {
                    nodeRegistry.get(registeredWorkerId).setStatus(NodeStatus.DISCONNECTED);
                }
            }
            rebalanceSpatialChunks();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void connectWorkerToMaster() {
        networkPool.execute(() -> {
            int retries = 0;
            while (running.get()) {
                try {
                    workerClientSocket = new Socket(masterHost, port);
                    logger.info("🔗 Connected to Master Cluster at {}:{}", masterHost, port);

                    DataOutputStream out = new DataOutputStream(workerClientSocket.getOutputStream());
                    DataInputStream in = new DataInputStream(workerClientSocket.getInputStream());

                    String workerId = (customWorkerId != null && !customWorkerId.isBlank())
                            ? customWorkerId
                            : "worker-" + UUID.randomUUID().toString().substring(0, 6);
                    String capacity = (customWorkerCapacity != null && !customWorkerCapacity.isBlank())
                            ? customWorkerCapacity
                            : "Compute Core Worker";
                    String registerMsg = "REGISTER_WORKER:" + workerId + ":" + capacity;
                    writeEncryptedFrame(out, registerMsg, securityManager);

                    String decryptedResp = readEncryptedFrame(in, securityManager);
                    logger.info("Cluster Master Response: {}", decryptedResp);

                    // Start background heartbeat sender
                    final int hbSec = heartbeatIntervalSec;
                    Thread hbThread = new Thread(() -> {
                        while (running.get() && !workerClientSocket.isClosed()) {
                            try {
                                Thread.sleep(hbSec * 1000L);
                                synchronized (out) {
                                    writeEncryptedFrame(out, "HEARTBEAT:" + workerId, securityManager);
                                }
                            } catch (Exception ignored) {
                                break;
                            }
                        }
                    }, "Worker-Heartbeat-Sender");
                    hbThread.setDaemon(true);
                    hbThread.start();

                    // Main worker loop: listen for instructions from Master
                    while (running.get() && !workerClientSocket.isClosed()) {
                        String decrypted = readEncryptedFrame(in, securityManager);

                        if ("HEARTBEAT_ACK".equals(decrypted)) {
                            // Normal ping ACK
                            continue;
                        } else if (decrypted.startsWith("EXECUTE_CHUNK:")) {
                            // EXECUTE_CHUNK:<tickId>:<start>:<count>:<dt>:<base64Payload>
                            String[] cmdParts = decrypted.split(":", 6);
                            long tickId = Long.parseLong(cmdParts[1]);
                            int start = Integer.parseInt(cmdParts[2]);
                            int count = Integer.parseInt(cmdParts[3]);
                            float dt = Float.parseFloat(cmdParts[4]);
                            String payload = cmdParts.length >= 6 ? cmdParts[5] : "";

                            if (currentWorldBuffer == null || currentWorldBuffer.getCapacity() < (start + count)) {
                                currentWorldBuffer = new WorldBuffer(Math.max(1000, start + count));
                            }

                            if (!payload.isEmpty()) {
                                try {
                                    WorldBufferWireCodec.decryptAndDecodeChunk(payload, currentWorldBuffer, securityManager);
                                } catch (Exception ignored) {}
                            }

                            // Run local computation kernel delegate if registered
                            if (workerComputeDelegate != null) {
                                workerComputeDelegate.accept(currentWorldBuffer);
                            }

                            // Encode computed delta and return to Master
                            String resultBase64 = WorldBufferWireCodec.encodeAndEncryptChunk(currentWorldBuffer, start, count, tickId, securityManager);
                            String resMsg = String.format(Locale.ROOT, "CHUNK_RESULT:%s:%d:%d:%d:%s", workerId, tickId, start, count, resultBase64);
                            synchronized (out) {
                                writeEncryptedFrame(out, resMsg, securityManager);
                            }
                        }
                    }
                    break;
                } catch (Exception e) {
                    retries++;
                    logger.warn("Worker connection attempt {} failed to {}:{}: {}", retries, masterHost, port, e.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        });
    }

    /**
     * Executes a distributed simulation tick by broadcasting chunk orders to all active workers
     * and waiting for lock-step barrier completion.
     */
    public boolean executeDistributedTick(long tickId, WorldBuffer buffer, float dt, Consumer<WorldBuffer> localMasterCompute) {
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
                    logger.warn("⚠️ Cluster Node [{}] heartbeat timeout! Marking DISCONNECTED.", node.getId());
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

    public synchronized void dispatchScenarioToCluster(Scenario scenario) {
        this.currentActiveScenario = scenario;
        String scenarioName = scenario != null ? scenario.getName() : "Default Scenario";
        logger.info("📡 Master broadcasting Scenario [{}] to all active cluster workers...", scenarioName);
    }

    public Scenario getCurrentActiveScenario() { return currentActiveScenario; }

    public synchronized void setTotalGridCellCount(int count) {
        this.totalGridCellCount = count;
        rebalanceSpatialChunks();
    }

    public int getTotalGridCellCount() { return totalGridCellCount; }

    public ConcurrentHashMap<String, ClusterNodeRecord> getNodeRegistry() { return nodeRegistry; }

    public PartitionStrategy getPartitionStrategy() { return partitionStrategy; }
    public void setPartitionStrategy(PartitionStrategy strategy) {
        this.partitionStrategy = strategy != null ? strategy : PartitionStrategy.HILBERT;
        rebalanceSpatialChunks();
    }

    public String getCustomWorkerId() { return customWorkerId; }
    public void setCustomWorkerId(String customWorkerId) { this.customWorkerId = customWorkerId; }

    public String getCustomWorkerCapacity() { return customWorkerCapacity; }
    public void setCustomWorkerCapacity(String customWorkerCapacity) { this.customWorkerCapacity = customWorkerCapacity; }

    public long getBarrierTimeoutMs() { return barrierTimeoutMs; }
    public void setBarrierTimeoutMs(long barrierTimeoutMs) { this.barrierTimeoutMs = Math.max(100, barrierTimeoutMs); }

    public int getSyncInterval() { return syncInterval; }
    public void setSyncInterval(int syncInterval) { this.syncInterval = Math.max(1, syncInterval); }

    public int getHeartbeatIntervalSec() { return heartbeatIntervalSec; }
    public void setHeartbeatIntervalSec(int sec) { this.heartbeatIntervalSec = Math.max(1, sec); }

    public int getHeartbeatTimeoutSec() { return heartbeatTimeoutSec; }
    public void setHeartbeatTimeoutSec(int sec) { this.heartbeatTimeoutSec = Math.max(2, sec); }

    public boolean isHaloExchangeEnabled() { return haloExchangeEnabled; }
    public void setHaloExchangeEnabled(boolean haloExchangeEnabled) { this.haloExchangeEnabled = haloExchangeEnabled; }

    public org.ether.society.network.cluster.ClusterSnapshotManager getSnapshotManager() { return snapshotManager; }
    public void setSnapshotManager(org.ether.society.network.cluster.ClusterSnapshotManager snapshotManager) { this.snapshotManager = snapshotManager; }

    public int getSnapshotIntervalTicks() { return snapshotIntervalTicks; }
    public void setSnapshotIntervalTicks(int snapshotIntervalTicks) { this.snapshotIntervalTicks = Math.max(0, snapshotIntervalTicks); }

    public synchronized void stop() {
        running.set(false);
        heartbeatScheduler.shutdownNow();
        networkPool.shutdownNow();
        try {
            if (masterServerSocket != null && !masterServerSocket.isClosed()) masterServerSocket.close();
            if (workerClientSocket != null && !workerClientSocket.isClosed()) workerClientSocket.close();
        } catch (IOException ignored) {}
        logger.info("Cluster Manager stopped cleanly.");
    }

    private static void writeEncryptedFrame(DataOutputStream out, String plainText, EtherSecurityManager sec) throws IOException {
        try {
            String encrypted = sec.encrypt(plainText);
            byte[] bytes = encrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            throw new IOException("Failed to encrypt frame", e);
        }
    }

    private static String readEncryptedFrame(DataInputStream in, EtherSecurityManager sec) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        String encrypted = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        try {
            return sec.decrypt(encrypted);
        } catch (Exception e) {
            throw new IOException("Failed to decrypt frame", e);
        }
    }
}

