*
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
    /** Internal state: max clients (int). */
    private static final int MAX_CLIENTS = 100;

    /** Internal state: port (int). */
    private final int port;
    private final EtherSecurityManager securityManager;
    private final String serverSecretKey;
    public static final String DEFAULT_SERVER_SECRET = "EtherSecureClusterKey_2026";
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EtherNetworkServer-Thread");
        t.setDaemon(true);
        return t;
    });

    /**
     * Constructs a network server on the designated TCP port with baseline security.
     *
     * @param port listening TCP port
     */
    public EtherNetworkServer(int port) {
        this(port, new EtherSecurityManager(), DEFAULT_SERVER_SECRET);
    }

    /**
     * Constructs a network server with a custom security manager and default cluster secret.
     *
     * @param port listening TCP port
     * @param securityManager AES-256 GCM security manager
     */
    public EtherNetworkServer(int port, EtherSecurityManager securityManager) {
        this(port, securityManager, DEFAULT_SERVER_SECRET);
    }

    /**
     * Constructs a network server with explicit security manager and cluster secret key.
     *
     * @param port listening TCP port
     * @param securityManager AES-256 GCM security manager
     * @param serverSecretKey cluster authentication secret key
     */
    public EtherNetworkServer(int port, EtherSecurityManager securityManager, String serverSecretKey) {
        this.port = port;
        this.securityManager = securityManager;
        this.serverSecretKey = serverSecretKey != null && !serverSecretKey.isBlank() ? serverSecretKey : DEFAULT_SERVER_SECRET;
    }

    /**
     * Returns the active security manager instance.
     * @return EtherSecurityManager instance
     */
    public EtherSecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * Returns the configured cluster authentication secret key.
     * @return cluster secret key
     */
    public String getServerSecretKey() {
        return serverSecretKey;
    }

    /**
     * Start.
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

    /**
     * Broadcast state update.
     *
     * @param stateJson the state json parameter (String)
     */
    public void broadcastStateUpdate(String stateJson) {
        broadcastStateUpdate(stateJson, null);
    }

    /**
     * Broadcast state update.
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

    /**
     * Stop.
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

    /**
     * Get connected client count.
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

        /**
         * Client handler.
         *
         * @param socket the socket parameter (Socket)
         * @return the resulting computation or state reference
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Get auth token.
         *
         * @return the resulting computation or state reference
         */
        public AuthToken getAuthToken() {
            return authToken;
        }

        /**
         * Set auth token.
         *
         * @param token the token parameter (AuthToken)
         */
        public void setAuthToken(AuthToken token) {
            this.authToken = token;
        }

        /**
         * Listens for inbound client communication, enforces cryptographic constraints,
         * validates RBAC authorization, and broadcasts verified policy mutations.
         */
        @Override
        public void run() {
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
                    String decryptedMsg;
                    if (securityManager != null) {
                        try {
                            decryptedMsg = securityManager.decrypt(rawMsg);
                        } catch (Exception e) {
                            EtherSecurityAuditLogger.logAuditEvent("SECURITY_VIOLATION", clientIp,
                                    "Decryption failure on payload: rejected plaintext/tampered message");
                            sendMessage("ERROR:Security violation: Decryption failed, encrypted frame required");
                            continue;
                        }
                    } else {
                        decryptedMsg = rawMsg;
                    }

                    if (decryptedMsg.startsWith("AUTH:")) {
                        String[] parts = decryptedMsg.split(":");
                        boolean authenticated = false;
                        if (parts.length >= 6) {
                            // Full signed token: AUTH:<username>:<role>:<tokenId>:<timestamp>:<signature>
                            String user = parts[1];
                            String roleStr = parts[2];
                            String tokenId = parts[3];
                            try {
                                long timestamp = Long.parseLong(parts[4]);
                                String signature = parts[5];
                                long now = System.currentTimeMillis();
                                if (Math.abs(now - timestamp) > 600_000) {
                                    EtherSecurityAuditLogger.logAuditEvent("AUTH_FAILED", clientIp, "Rejected expired authentication token");
                                    sendMessage("ERROR:Authentication token expired");
                                    continue;
                                }
                                AuthToken.Role role = AuthToken.Role.valueOf(roleStr.toUpperCase());
                                String expectedSig = AuthToken.generateSignature(tokenId, user, role.name(), timestamp, serverSecretKey);
                                if (expectedSig.equals(signature)) {
                                    this.authToken = new AuthToken(tokenId, user, role, timestamp, signature);
                                    authenticated = true;
                                }
                            } catch (Exception e) {
                                logger.warn("Malformed auth token payload from {}: {}", clientIp, e.getMessage());
                            }
                        } else if (parts.length >= 4) {
                            // Secret verification: AUTH:<username>:<role>:<providedSecret>
                            String user = parts[1];
                            String roleStr = parts[2];
                            String providedSecret = parts[3];
                            try {
                                AuthToken.Role role = AuthToken.Role.valueOf(roleStr.toUpperCase());
                                if (serverSecretKey.equals(providedSecret)) {
                                    this.authToken = new AuthToken(user, role, serverSecretKey);
                                    authenticated = true;
                                }
                            } catch (Exception e) {
                                logger.warn("Malformed auth attempt from {}: {}", clientIp, e.getMessage());
                            }
                        }

                        if (authenticated && this.authToken != null) {
                            EtherSecurityAuditLogger.logAuditEvent("AUTH_SUCCESS", clientIp, "Authenticated as " + this.authToken.getRole() + " (" + this.authToken.getUsername() + ")");
                            sendMessage("AUTH_OK:" + this.authToken.getRole().name());
                        } else {
                            EtherSecurityAuditLogger.logAuditEvent("AUTH_FAILED", clientIp, "Authentication failed: invalid credentials or signature");
                            sendMessage("ERROR:Authentication failed (invalid credentials or signature)");
                        }
                        continue;
                    }

                    // Strict Default-Deny RBAC: Every mutation requires an authenticated session with PLANNER or higher role
                    if (this.authToken == null || !this.authToken.hasPermission(AuthToken.Role.PLANNER)) {
                        EtherSecurityAuditLogger.logAuditEvent("AUTH_DENIED", clientIp,
                                "Unauthorized state mutation attempt without PLANNER role");
                        sendMessage("ERROR:Permission denied (requires authenticated session with PLANNER role)");
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

        /**
         * Send message.
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

