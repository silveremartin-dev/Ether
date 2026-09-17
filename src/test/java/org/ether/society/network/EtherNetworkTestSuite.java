/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite for Ether Co-Governance Network Server & Client.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EtherNetworkTestSuite {

    private EtherNetworkServer server;
    private static final int TEST_PORT = 19876;

    @BeforeEach
    public void setUp() throws IOException {
        server = new EtherNetworkServer(TEST_PORT);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testServerClientConnection() throws IOException, InterruptedException {
        EtherNetworkClient client = new EtherNetworkClient("localhost", TEST_PORT, server.getSecurityManager());
        client.connect();
        assertTrue(client.isConnected(), "Client should successfully connect to local Ether server.");

        Thread.sleep(100);
        assertEquals(1, server.getConnectedClientCount(), "Server should register 1 connected client.");

        client.sendPolicyInjection("POLICY:CARBON_TAX_50");
        client.disconnect();
    }
}

