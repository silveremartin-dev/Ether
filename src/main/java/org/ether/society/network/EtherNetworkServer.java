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
 * @version 4.0.0
 */
public class EtherNetworkServer {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkServer.class);
    private static final int MAX_CLIENTS = 100;

    private final int port;
    private final EtherSecurityManager securityManager;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EtherNetworkServer-Thread");
        t.setDaemon(true);
        return t;
    });

    public EtherNetworkServer(int port) {
        this(port, new EtherSecurityManager());
    }

    public EtherNetworkServer(int port, EtherSecurityManager securityManager) {
        this.port = port;
        this.securityManager = securityManager;
    }

    public EtherSecurityManager getSecurityManager() {
        return securityManager;
    }

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

    public void broadcastStateUpdate(String stateJson) {
        broadcastStateUpdate(stateJson, null);
    }

    public void broadcastStateUpdate(String stateJson, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(stateJson);
                }
            }
        }
    }

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

    public int getConnectedClientCount() {
        return clients.size();
    }

    public class ClientHandler implements Runnable {
        private final Socket socket;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

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
                    String decryptedMsg = rawMsg;
                    if (securityManager != null) {
                        try {
                            decryptedMsg = securityManager.decrypt(rawMsg);
                        } catch (Exception e) {
                            // Fallback if client sent plaintext or decryption error
                            decryptedMsg = rawMsg;
                        }
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
