/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
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
 * and vote on atmospheric carbon quotas in real time over TCP/IP.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class EtherNetworkServer {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkServer.class);

    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public EtherNetworkServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("⚡ Ether Co-Governance Network Server started on port {}", port);

        threadPool.execute(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
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
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(stateJson);
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

    private class ClientHandler implements Runnable {
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
                out.writeUTF("CONNECTED_TO_ETHER_SECURE_SERVER");

                while (running && !socket.isClosed()) {
                    String msg = in.readUTF();
                    EtherSecurityAuditLogger.logAuditEvent("PAYLOAD_RECEIVED", clientIp, "Payload length: " + msg.length());
                    // Relay policy injection to all other connected planners
                    broadcastStateUpdate("POLICY_EVENT:" + msg);
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
                    out.writeUTF(msg);
                    out.flush();
                }
            } catch (IOException e) {
                logger.error("Error sending message to client: {}", e.getMessage());
            }
        }
    }
}
