/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating AES-256 GCM Encryption, Decryption, and Security Audit Logging.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class EtherSecurityTestSuite {

    @Test
    public void testAesGcmEncryptionDecryption() throws Exception {
        EtherSecurityManager sec = new EtherSecurityManager();
        String originalPayload = "GOD_MODE_INJECTION:SOOT_OPTICAL_DEPTH_1.5";

        String encrypted = sec.encrypt(originalPayload);
        assertNotNull(encrypted);
        assertNotEquals(originalPayload, encrypted);

        String decrypted = sec.decrypt(encrypted);
        assertEquals(originalPayload, decrypted, "Decrypted payload should match original payload.");
    }

    @Test
    public void testSecurityAuditLogging() {
        assertDoesNotThrow(() -> {
            EtherSecurityAuditLogger.logAuditEvent("TEST_EVENT", "127.0.0.1", "Testing security audit logger write.");
        });
    }
}
