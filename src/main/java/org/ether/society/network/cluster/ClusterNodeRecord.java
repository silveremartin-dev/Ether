/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.network.ClusterManager.ClusterRole;
import org.ether.society.network.ClusterManager.NodeStatus;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <h1>ClusterNodeRecord</h1>
 * <p>
 * Represents the network state, assigned cellular partition chunks, and health telemetry of a distributed cluster node.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ClusterNodeRecord {

    private final String id;
    private final String host;
    private final int port;
    private ClusterRole role;
    private NodeStatus status;
    private String capacity;
    private int assignedChunkStart;
    private int assignedChunkEnd;
    private final AtomicLong lastHeartbeatNanos = new AtomicLong(System.nanoTime());

    /**
     * Constructs a cluster node record.
     *
     * @param id       unique node identifier
     * @param host     network hostname or IP
     * @param port     TCP listener port
     * @param role     cluster role (MASTER, WORKER, OBSERVER)
     * @param status   current node lifecycle status
     * @param capacity computational hardware capacity profile
     */
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
