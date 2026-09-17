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

    private final String host;
    private final int port;
    private final EtherSecurityManager securityManager;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean connected = false;

    public EtherNetworkClient(String host, int port) {
        this(host, port, null);
    }

    public EtherNetworkClient(String host, int port, EtherSecurityManager securityManager) {
        this.host = host;
        this.port = port;
        this.securityManager = securityManager;
    }

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

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
            logger.info("Disconnected from Ether server.");
        } catch (IOException e) {
            logger.error("Error disconnecting client: {}", e.getMessage());
        }
    }

    public boolean isConnected() { return connected; }
}

