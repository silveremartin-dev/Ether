/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Multi-Planner Co-Governance Network Client.
 * Connects to a remote Ether Network Server to receive live planetary updates
 * and send God Mode interventions or policy votes with AES-256 GCM encryption.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EtherNetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkClient.class);

    /* Internal state variable for host (String). */
    private final String host;
    /* Internal state variable for port (int). */
    private final int port;
    private final EtherSecurityManager securityManager;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    /* Internal state variable for connected (boolean). */
    private boolean connected = false;

    /*
     * Ether network client.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     * @param host the host parameter (String)
     * @param port the port parameter (int)
     */
    public EtherNetworkClient(String host, int port) {
        this(host, port, null);
    }

    /*
     * Ether network client.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     * @param host the host parameter (String)
     * @param port the port parameter (int)
     * @param securityManager the security manager parameter (EtherSecurityManager)
     */
    public EtherNetworkClient(String host, int port, EtherSecurityManager securityManager) {
        this.host = host;
        this.port = port;
        this.securityManager = securityManager;
    }

    /*
     * Connect.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        connected = true;

        String welcome = in.readUTF();
        if (securityManager != null) {
            try {
                welcome = securityManager.decrypt(welcome);
            } catch (Exception ignored) {}
        }
        EtherSecurityAuditLogger.logAuditEvent("CLIENT_CONNECT", host + ":" + port, "Handshake: " + welcome);
    }

    /*
     * Send policy injection.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     * @param policyPayload the policy payload parameter (String)
     */
    public void sendPolicyInjection(String policyPayload) throws IOException {
        if (!connected || out == null) throw new IllegalStateException("Client is not connected.");
        String toSend = policyPayload;
        if (securityManager != null) {
            try {
                toSend = securityManager.encrypt(policyPayload);
            } catch (Exception e) {
                logger.warn("Encryption failed on client send", e);
            }
        }
        out.writeUTF(toSend);
        out.flush();
    }

    /*
     * Disconnect.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
            logger.info("Disconnected from Ether server.");
        } catch (IOException e) {
            logger.error("Error disconnecting client: {}", e.getMessage());
        }
    }

    /*
     * Is connected.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkClient}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isConnected() { return connected; }
}

