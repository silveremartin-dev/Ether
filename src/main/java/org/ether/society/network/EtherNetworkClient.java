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
import java.net.Socket;

/**
 * Multi-Planner Co-Governance Network Client.
 * Connects to a remote Ether Network Server to receive live planetary updates
 * and send God Mode interventions or policy votes.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class EtherNetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(EtherNetworkClient.class);

    private final String host;
    private final int port;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean connected = false;

    public EtherNetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        connected = true;

        String welcome = in.readUTF();
        logger.info("Connected to Ether server: {}", welcome);
    }

    public void sendPolicyInjection(String policyPayload) throws IOException {
        if (!connected || out == null) throw new IllegalStateException("Client is not connected.");
        out.writeUTF(policyPayload);
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
