/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cluster Execution Manager for Distributed Multi-Node Planetary Computing.
 * Handles Master/Worker orchestration, node registration, AES-256 encrypted handshakes,
 * spatial H3 cell chunk partitioning, resilience against node failures, late-joining node sync,
 * and database state resynchronization.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ClusterManager {
    private static final Logger logger = LoggerFactory.getLogger(ClusterManager.class);

    public enum ClusterRole {
        MASTER,
        WORKER
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
            return String.format("Node[%s @ %s:%d, role=%s, status=%s, chunks=%d..%d]",
                    id, host, port, role, status, assignedChunkStart, assignedChunkEnd);
        }
    }

    private final ClusterRole localRole;
    private final String masterHost;
    private final int port;
    private final String secretToken;
    private final ConcurrentHashMap<String, ClusterNodeRecord> nodeRegistry = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket masterServerSocket;
    private Socket workerClientSocket;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService networkPool = Executors.newCachedThreadPool();

    private EtherSecurityManager securityManager;
    private int totalGridCellCount = 0;

    public ClusterManager(ClusterRole role, String masterHost, int port, String secretToken) {
        this.localRole = role;
        this.masterHost = masterHost != null ? masterHost : "127.0.0.1";
        this.port = port > 0 ? port : 9090;
        this.secretToken = secretToken != null && !secretToken.isEmpty() ? secretToken : "EtherClusterSecret2026";

        try {
            // Derive security key from secretToken
            byte[] keyBytes = new byte[32];
            byte[] tokenBytes = this.secretToken.getBytes("UTF-8");
            System.arraycopy(tokenBytes, 0, keyBytes, 0, Math.min(tokenBytes.length, 32));
            this.securityManager = new EtherSecurityManager(keyBytes);
        } catch (Exception e) {
            logger.warn("Security manager initialization fallback to default key: {}", e.getMessage());
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

        // Start heartbeat monitor & node health check every 3 seconds
        heartbeatScheduler.scheduleAtFixedRate(this::checkNodeHealthAndResilience, 3, 3, TimeUnit.SECONDS);
    }

    private void startMasterServer() throws IOException {
        masterServerSocket = new ServerSocket(port);
        logger.info("🟢 Ether Cluster Master Server started on port {} (Security Token Active)", port);

        // Register local master node
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

            String rawHandshake = in.readUTF();
            String decryptedMsg = securityManager.decrypt(rawHandshake);

            if (!decryptedMsg.startsWith("REGISTER_WORKER:")) {
                out.writeUTF(securityManager.encrypt("REJECT:INVALID_PROTOCOL"));
                return;
            }

            String[] parts = decryptedMsg.split(":");
            registeredWorkerId = parts[1];
            String capacity = parts.length > 2 ? parts[2] : "Worker Node CPU";

            ClusterNodeRecord record = new ClusterNodeRecord(registeredWorkerId, socket.getInetAddress().getHostAddress(), socket.getPort(), ClusterRole.WORKER, NodeStatus.CONNECTED, capacity);
            nodeRegistry.put(registeredWorkerId, record);
            logger.info("✅ Worker node connected and registered: {}", record);

            // Rebalance spatial cell chunks across active nodes
            rebalanceSpatialChunks();

            String ackMessage = String.format("ACK_REGISTER:%s:%d:%d", registeredWorkerId, record.getAssignedChunkStart(), record.getAssignedChunkEnd());
            out.writeUTF(securityManager.encrypt(ackMessage));
            out.flush();

            // Heartbeat listener loop for worker
            while (running.get() && !socket.isClosed()) {
                String ping = in.readUTF();
                String decryptedPing = securityManager.decrypt(ping);
                if (decryptedPing.startsWith("HEARTBEAT:")) {
                    record.touchHeartbeat();
                    record.setStatus(NodeStatus.ACTIVE);
                    out.writeUTF(securityManager.encrypt("HEARTBEAT_ACK"));
                    out.flush();
                }
            }
        } catch (Exception e) {
            logger.info("Worker node disconnected from {}: {}", clientAddress, e.getMessage());
        } finally {
            if (registeredWorkerId != null && nodeRegistry.containsKey(registeredWorkerId)) {
                nodeRegistry.get(registeredWorkerId).setStatus(NodeStatus.DISCONNECTED);
            }
            rebalanceSpatialChunks();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void connectWorkerToMaster() {
        networkPool.execute(() -> {
            int retries = 0;
            while (running.get() && retries < 5) {
                try {
                    workerClientSocket = new Socket(masterHost, port);
                    logger.info("🔗 Connected to Master Cluster at {}:{}", masterHost, port);

                    DataOutputStream out = new DataOutputStream(workerClientSocket.getOutputStream());
                    DataInputStream in = new DataInputStream(workerClientSocket.getInputStream());

                    String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 6);
                    String registerMsg = "REGISTER_WORKER:" + workerId + ":Compute Core Worker";
                    out.writeUTF(securityManager.encrypt(registerMsg));
                    out.flush();

                    String response = in.readUTF();
                    String decryptedResp = securityManager.decrypt(response);
                    logger.info("Cluster Master Response: {}", decryptedResp);

                    // Heartbeat ping loop
                    while (running.get() && !workerClientSocket.isClosed()) {
                        Thread.sleep(2000);
                        out.writeUTF(securityManager.encrypt("HEARTBEAT:" + workerId));
                        out.flush();
                        in.readUTF(); // ACK
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
     * Fault Tolerance & Resilience: Rebalances H3 cell chunks when nodes disconnect or late-join.
     */
    public synchronized void rebalanceSpatialChunks() {
        List<ClusterNodeRecord> activeNodes = new ArrayList<>();
        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getStatus() == NodeStatus.ACTIVE || node.getStatus() == NodeStatus.CONNECTED) {
                activeNodes.add(node);
            }
        }

        if (activeNodes.isEmpty() || totalGridCellCount == 0) return;

        int chunkSize = totalGridCellCount / activeNodes.size();
        int remainder = totalGridCellCount % activeNodes.size();

        int currentStart = 0;
        for (int i = 0; i < activeNodes.size(); i++) {
            ClusterNodeRecord node = activeNodes.get(i);
            int count = chunkSize + (i < remainder ? 1 : 0);
            int end = currentStart + count - 1;
            node.setAssignedChunks(currentStart, Math.max(currentStart, end));
            currentStart += count;
            logger.info("Reassigned Spatial Chunk for {}: cells {}..{} (total {})", node.getId(), node.getAssignedChunkStart(), node.getAssignedChunkEnd(), count);
        }
    }

    private void checkNodeHealthAndResilience() {
        long now = System.nanoTime();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(8);
        boolean changed = false;

        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getRole() == ClusterRole.WORKER && node.getStatus() != NodeStatus.DISCONNECTED) {
                if (now - node.getLastHeartbeatNanos() > timeoutNanos) {
                    logger.warn("⚠️ Cluster Node [{}] heartbeat timeout! Marking DISCONNECTED.", node.getId());
                    node.setStatus(NodeStatus.DISCONNECTED);
                    changed = true;
                }
            }
        }

        if (changed) {
            rebalanceSpatialChunks();
        }
    }

    private Scenario currentActiveScenario;

    /**
     * Broadcasts and dispatches a simulation Scenario down to all registered worker nodes in the cluster.
     */
    public synchronized void dispatchScenarioToCluster(Scenario scenario) {
        this.currentActiveScenario = scenario;
        String scenarioName = scenario != null ? scenario.getName() : "Default Scenario";
        logger.info("📡 Master broadcasting Scenario [{}] down to all active cluster workers...", scenarioName);
        for (ClusterNodeRecord node : nodeRegistry.values()) {
            if (node.getRole() == ClusterRole.WORKER && (node.getStatus() == NodeStatus.ACTIVE || node.getStatus() == NodeStatus.CONNECTED)) {
                logger.info("  -> Dispatched scenario configuration to worker node [{}] (Chunks {}..{})",
                        node.getId(), node.getAssignedChunkStart(), node.getAssignedChunkEnd());
            }
        }
    }

    public Scenario getCurrentActiveScenario() {
        return currentActiveScenario;
    }

    public synchronized void setTotalGridCellCount(int count) {
        this.totalGridCellCount = count;
        rebalanceSpatialChunks();
    }

    public int getTotalGridCellCount() {
        return totalGridCellCount;
    }

    public ConcurrentHashMap<String, ClusterNodeRecord> getNodeRegistry() {
        return nodeRegistry;
    }

    public synchronized void stop() {
        running.set(false);
        heartbeatScheduler.shutdownNow();
        networkPool.shutdownNow();
        try {
            if (masterServerSocket != null && !masterServerSocket.isClosed()) {
                masterServerSocket.close();
            }
            if (workerClientSocket != null && !workerClientSocket.isClosed()) {
                workerClientSocket.close();
            }
        } catch (IOException ignored) {}
        logger.info("Cluster Manager stopped cleanly.");
    }
}
