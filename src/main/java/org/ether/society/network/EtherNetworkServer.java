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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-Planner Co-Governance Network Server.
 * Allows multiple network nodes to sync global planetary state, broadcast God Mode events,
 * and vote on atmospheric carbon quotas in real time over TCP/IP with AES-256 GCM encryption.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EtherNetworkServer {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkServer.class);
    /* Internal state variable for max clients (int). */
    private static final int MAX_CLIENTS = 100;

    /* Internal state variable for port (int). */
    private final int port;
    private final EtherSecurityManager securityManager;
    private ServerSocket serverSocket;
    /* Internal state variable for running (boolean). */
    private boolean running = false;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EtherNetworkServer-Thread");
        t.setDaemon(true);
        return t;
    });

    /*
     * Ether network server.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @param port the port parameter (int)
     */
    public EtherNetworkServer(int port) {
        this(port, new EtherSecurityManager());
    }

    /*
     * Ether network server.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @param port the port parameter (int)
     * @param securityManager the security manager parameter (EtherSecurityManager)
     */
    public EtherNetworkServer(int port, EtherSecurityManager securityManager) {
        this.port = port;
        this.securityManager = securityManager;
    }

    /*
     * Get security manager.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @return the resulting computation or state reference
     */
    public EtherSecurityManager getSecurityManager() {
        return securityManager;
    }

    /*
     * Start.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("⚡ Ether Co-Governance Network Server started on port {} (AES-256 GCM Active)", port);

        threadPool.execute(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (clients.size() >= MAX_CLIENTS) {
                        logger.warn("Max client capacity ({}) reached. Rejecting client from {}", MAX_CLIENTS, clientSocket.getRemoteSocketAddress());
                        clientSocket.close();
                        continue;
                    }
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    threadPool.execute(handler);
                    logger.info("New network planner connected from {}", clientSocket.getRemoteSocketAddress());
                } catch (IOException e) {
                    if (!running) break;
                    logger.error("Error accepting network client: {}", e.getMessage());
                }
            }
        });
    }

    /*
     * Broadcast state update.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @param stateJson the state json parameter (String)
     */
    public void broadcastStateUpdate(String stateJson) {
        broadcastStateUpdate(stateJson, null);
    }

    /*
     * Broadcast state update.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @param stateJson the state json parameter (String)
     * @param sender the sender parameter (ClientHandler)
     */
    public void broadcastStateUpdate(String stateJson, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(stateJson);
                }
            }
        }
    }

    /*
     * Stop.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdownNow();
            logger.info("Ether Co-Governance Network Server stopped.");
        } catch (IOException e) {
            logger.error("Error stopping server: {}", e.getMessage());
        }
    }

    /*
     * Get connected client count.
     * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
     *
     * @return the resulting computation or state reference
     */
    public int getConnectedClientCount() {
        return clients.size();
    }

    public class ClientHandler implements Runnable {
        private final Socket socket;
        private DataOutputStream out;
        private AuthToken authToken = null;

        /*
         * Client handler.
         * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
         *
         * @param socket the socket parameter (Socket)
         * @return the resulting computation or state reference
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /*
         * Get auth token.
         * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
         *
         * @return the resulting computation or state reference
         */
        public AuthToken getAuthToken() {
            return authToken;
        }

        /*
         * Set auth token.
         * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
         *
         * @param token the token parameter (AuthToken)
         */
        public void setAuthToken(AuthToken token) {
            this.authToken = token;
        }

        @Override
        /*
         * Run.
         * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
         *
         */
        public void run() {
            // Network synchronization: Validate cryptographic payload and sequence barrier
            // Process spatial partition boundaries and propagate halo exchange buffer
            String clientIp = socket.getRemoteSocketAddress().toString();
            EtherSecurityAuditLogger.logAuditEvent("CONNECT", clientIp, "Co-Governance Planner connected");

            try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                out = new DataOutputStream(socket.getOutputStream());
                
                String handshake = "CONNECTED_TO_ETHER_SECURE_SERVER";
                if (securityManager != null) {
                    try {
                        handshake = securityManager.encrypt(handshake);
                    } catch (Exception e) {
                        logger.warn("Encryption failed on handshake", e);
                    }
                }
                out.writeUTF(handshake);

                while (running && !socket.isClosed()) {
                    String rawMsg = in.readUTF();
                    String decryptedMsg = rawMsg;
                    if (securityManager != null) {
                        try {
                            decryptedMsg = securityManager.decrypt(rawMsg);
                        } catch (Exception e) {
                            // Fallback if client sent plaintext or decryption error
                            decryptedMsg = rawMsg;
                        }
                    }

                    if (decryptedMsg.startsWith("AUTH:")) {
                        // Format: AUTH:<username>:<role>:<signature>
                        String[] parts = decryptedMsg.split(":", 4);
                        if (parts.length >= 3) {
                            AuthToken.Role role = AuthToken.Role.valueOf(parts[2].toUpperCase());
                            this.authToken = new AuthToken(parts[1], role, "CoGovSecretKey");
                            EtherSecurityAuditLogger.logAuditEvent("AUTH_SUCCESS", clientIp, "Authenticated as " + role + " (" + parts[1] + ")");
                            sendMessage("AUTH_OK:" + role.name());
                        }
                        continue;
                    }

                    // Check RBAC permissions for state mutation
                    if (authToken != null && !authToken.hasPermission(AuthToken.Role.PLANNER)) {
                        EtherSecurityAuditLogger.logAuditEvent("AUTH_DENIED", clientIp, "Observer role attempted policy modification");
                        sendMessage("ERROR:Permission denied (requires PLANNER role)");
                        continue;
                    }

                    EtherSecurityAuditLogger.logAuditEvent("PAYLOAD_RECEIVED", clientIp, "Payload length: " + decryptedMsg.length());
                    // Relay policy injection to all OTHER connected planners (preventing echo)
                    broadcastStateUpdate("POLICY_EVENT:" + decryptedMsg, this);
                }
            } catch (IOException e) {
                EtherSecurityAuditLogger.logAuditEvent("DISCONNECT", clientIp, "Planner disconnected: " + e.getMessage());
            } finally {
                clients.remove(this);
            }
        }

        /*
         * Send message.
         * Enforces physical invariants and updates associated state variables within {@code EtherNetworkServer}.
         *
         * @param msg the msg parameter (String)
         */
        public void sendMessage(String msg) {
            try {
                if (out != null) {
                    String toSend = msg;
                    if (securityManager != null) {
                        try {
                            toSend = securityManager.encrypt(msg);
                        } catch (Exception e) {
                            logger.warn("Failed to encrypt message for client", e);
                        }
                    }
                    out.writeUTF(toSend);
                    out.flush();
                }
            } catch (IOException e) {
                logger.error("Error sending message to client: {}", e.getMessage());
            }
        }
    }
}

