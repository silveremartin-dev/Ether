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
 * @version 1.0.0-beta.2
 */
public class EtherNetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkClient.class);

    /** Internal state: host (String). */
    private final String host;
    /** Internal state: port (int). */
    private final int port;
    private final EtherSecurityManager securityManager;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    /** Internal state: connected (boolean). */
    private boolean connected = false;

    /**
     * Ether network client.
     *
     * @param host the host parameter (String)
     * @param port the port parameter (int)
     */
    public EtherNetworkClient(String host, int port) {
        this(host, port, null);
    }

    /**
     * Ether network client.
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

    /**
     * Connect.
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

    /**
     * Authenticates the client with the remote Ether server using a signed RBAC token.
     *
     * @param username client identity name
     * @param role requested RBAC role
     * @param secretKey shared cluster secret key
     * @throws IOException on network communication failure
     */
    public void authenticate(String username, AuthToken.Role role, String secretKey) throws IOException {
        AuthToken token = new AuthToken(username, role, secretKey);
        String authMsg = "AUTH:" + token.getUsername() + ":" + token.getRole().name() + ":"
                + token.getTokenId() + ":" + token.getIssueTimestamp() + ":" + token.getSignature();
        sendPolicyInjection(authMsg);
    }

    /**
     * Sends a policy payload or God Mode intervention to the server over the encrypted socket.
     *
     * @param policyPayload command or state payload string
     * @throws IOException on transport error
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

    /**
     * Disconnect.
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

    /**
     * Is connected.
     *
     * @return the resulting computation or state reference
     */
    public boolean isConnected() { return connected; }
}

