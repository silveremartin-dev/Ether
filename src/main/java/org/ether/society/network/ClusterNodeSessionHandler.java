/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.ClusterCoordinator.ClusterNodeRecord;
import org.ether.society.network.ClusterCoordinator.ClusterRole;
import org.ether.society.network.ClusterCoordinator.NodeStatus;
import org.ether.society.network.cluster.ClusterClockBarrier;
import org.ether.society.network.codec.ClusterWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles encrypted TCP socket handshakes, heartbeat exchanges, and remote {@link WorldBuffer}
 * chunk execution frames for both Master and Worker roles in {@link ClusterCoordinator}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ClusterCoordinator
 */
public final class ClusterNodeSessionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeSessionHandler.class);

    private ClusterNodeSessionHandler() {
        // Stateless protocol handler
    }

    /**
     * Handles an incoming worker socket connection on the Master node.
     *
     * @param socket            accepted worker TCP socket
     * @param running           cluster active flag
     * @param securityManager   AES-256 frame cipher manager
     * @param nodeRegistry      concurrent node registry
     * @param workerSocketsOut  active worker output stream map
     * @param clockBarrier      lock-step tick synchronization barrier
     * @param worldBufferGetter supplier of the Master's current {@link WorldBuffer}
     * @param onTopologyChanged callback triggered upon worker registration or disconnection
     */
    public static void handleWorkerHandshake(Socket socket,
                                             AtomicBoolean running,
                                             EtherSecurityService securityManager,
                                             ConcurrentHashMap<String, ClusterNodeRecord> nodeRegistry,
                                             ConcurrentHashMap<String, DataOutputStream> workerSocketsOut,
                                             ClusterClockBarrier clockBarrier,
                                             Supplier<WorldBuffer> worldBufferGetter,
                                             Runnable onTopologyChanged) {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        String registeredWorkerId = null;

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String decryptedMsg = ClusterWireCodec.readEncryptedFrame(in, securityManager);
            if (!decryptedMsg.startsWith("REGISTER_WORKER:")) {
                ClusterWireCodec.writeEncryptedFrame(out, "REJECT:INVALID_PROTOCOL", securityManager);
                return;
            }

            String[] parts = decryptedMsg.split(":");
            registeredWorkerId = parts[1];
            String capacity = parts.length > 2 ? parts[2] : "Worker Node CPU";

            ClusterNodeRecord record = new ClusterNodeRecord(
                    registeredWorkerId, socket.getInetAddress().getHostAddress(), socket.getPort(),
                    ClusterRole.WORKER, NodeStatus.CONNECTED, capacity);
            nodeRegistry.put(registeredWorkerId, record);
            workerSocketsOut.put(registeredWorkerId, out);
            logger.info("Worker node connected and registered: {}", record);

            onTopologyChanged.run();

            String ackMessage = String.format("ACK_REGISTER:%s:%d:%d",
                    registeredWorkerId, record.getAssignedChunkStart(), record.getAssignedChunkEnd());
            ClusterWireCodec.writeEncryptedFrame(out, ackMessage, securityManager);

            while (running.get() && !socket.isClosed()) {
                String decrypted = ClusterWireCodec.readEncryptedFrame(in, securityManager);
                if (decrypted.startsWith("HEARTBEAT:")) {
                    record.touchHeartbeat();
                    record.setStatus(NodeStatus.ACTIVE);
                    ClusterWireCodec.writeEncryptedFrame(out, "HEARTBEAT_ACK", securityManager);
                } else if (decrypted.startsWith("CHUNK_RESULT:")) {
                    String[] resParts = decrypted.split(":", 6);
                    if (resParts.length >= 6) {
                        String wId = resParts[1];
                        long tickId = Long.parseLong(resParts[2]);
                        String base64Payload = resParts[5];
                        WorldBuffer worldBuffer = worldBufferGetter.get();
                        if (worldBuffer != null) {
                            try {
                                WorldBufferWireCodec.decryptAndDecodeChunk(base64Payload, worldBuffer, securityManager);
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
                ClusterNodeRecord rec = nodeRegistry.get(registeredWorkerId);
                if (rec != null) {
                    rec.setStatus(NodeStatus.DISCONNECTED);
                }
            }
            onTopologyChanged.run();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Runs the Worker-side connection, registration, heartbeat, and chunk execution loop.
     *
     * @param manager owning {@link ClusterCoordinator} instance
     * @param running cluster active flag
     */
    public static void runWorkerConnectionLoop(ClusterCoordinator manager, AtomicBoolean running) {
        int retries = 0;
        while (running.get()) {
            try {
                Socket socket = new Socket(manager.getMasterHost(), manager.getPort());
                manager.setWorkerClientSocket(socket);
                logger.info("Connected to Master Cluster at {}:{}", manager.getMasterHost(), manager.getPort());

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                EtherSecurityService sec = manager.getSecurityManager();

                String workerId = (manager.getCustomWorkerId() != null && !manager.getCustomWorkerId().isBlank())
                        ? manager.getCustomWorkerId()
                        : "worker-" + UUID.randomUUID().toString().substring(0, 6);
                String capacity = (manager.getCustomWorkerCapacity() != null && !manager.getCustomWorkerCapacity().isBlank())
                        ? manager.getCustomWorkerCapacity()
                        : "Compute Core Worker";

                ClusterWireCodec.writeEncryptedFrame(out, "REGISTER_WORKER:" + workerId + ":" + capacity, sec);
                String decryptedResp = ClusterWireCodec.readEncryptedFrame(in, sec);
                logger.info("Cluster Master Response: {}", decryptedResp);

                final int hbSec = manager.getHeartbeatIntervalSec();
                Thread hbThread = new Thread(() -> {
                    while (running.get() && !socket.isClosed()) {
                        try {
                            Thread.sleep(hbSec * 1000L);
                            synchronized (out) {
                                ClusterWireCodec.writeEncryptedFrame(out, "HEARTBEAT:" + workerId, sec);
                            }
                        } catch (Exception ignored) {
                            break;
                        }
                    }
                }, "Worker-Heartbeat-Sender");
                hbThread.setDaemon(true);
                hbThread.start();

                while (running.get() && !socket.isClosed()) {
                    String decrypted = ClusterWireCodec.readEncryptedFrame(in, sec);
                    if ("HEARTBEAT_ACK".equals(decrypted)) {
                        continue;
                    } else if (decrypted.startsWith("EXECUTE_CHUNK:")) {
                        String[] cmdParts = decrypted.split(":", 6);
                        long tickId = Long.parseLong(cmdParts[1]);
                        int start = Integer.parseInt(cmdParts[2]);
                        int count = Integer.parseInt(cmdParts[3]);
                        String payload = cmdParts.length >= 6 ? cmdParts[5] : "";

                        WorldBuffer wb = manager.getCurrentWorldBuffer();
                        if (wb == null || wb.getCapacity() < (start + count)) {
                            wb = new WorldBuffer(Math.max(1000, start + count));
                            manager.setWorldBuffer(wb);
                        }
                        if (!payload.isEmpty()) {
                            try {
                                WorldBufferWireCodec.decryptAndDecodeChunk(payload, wb, sec);
                            } catch (Exception ignored) {
                            }
                        }
                        Consumer<WorldBuffer> delegate = manager.getWorkerComputeDelegate();
                        if (delegate != null) {
                            delegate.accept(wb);
                        }
                        String resultBase64 = WorldBufferWireCodec.encodeAndEncryptChunk(wb, start, count, tickId, sec);
                        String resMsg = String.format(Locale.ROOT, "CHUNK_RESULT:%s:%d:%d:%d:%s",
                                workerId, tickId, start, count, resultBase64);
                        synchronized (out) {
                            ClusterWireCodec.writeEncryptedFrame(out, resMsg, sec);
                        }
                    }
                }
                break;
            } catch (Exception e) {
                retries++;
                logger.warn("Worker connection attempt {} failed to {}:{}: {}",
                        retries, manager.getMasterHost(), manager.getPort(), e.getMessage());
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
