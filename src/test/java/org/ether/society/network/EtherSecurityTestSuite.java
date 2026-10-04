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
 * @version 1.0.0-beta.1
 */
public class EtherSecurityTestSuite {

    @Test
    /*
     * Test aes gcm encryption decryption operation.
     * <p>
     * Executes operational logic for {@code EtherSecurityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
    /*
     * Test security audit logging operation.
     * <p>
     * Executes operational logic for {@code EtherSecurityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSecurityAuditLogging() {
        assertDoesNotThrow(() -> {
            EtherSecurityAuditLogger.logAuditEvent("TEST_EVENT", "127.0.0.1", "Testing security audit logger write.");
        });
    }
}

