/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class SaveEncryptionVaultTest {

    @Test
    public void testEncryptionAndDecryptionRoundtrip() throws Exception {
        String originalJson = "{\"scenario\":\"Holocene Dawn\",\"population\":8000000000,\"co2\":420.5}";
        byte[] plaintext = originalJson.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = SaveEncryptionVault.encrypt(plaintext, "SuperSecretPassword123!");

        assertNotNull(encrypted);
        assertTrue(SaveEncryptionVault.isEncrypted(encrypted), "Encrypted data must match MAGIC_HEADER");
        assertNotEquals(new String(encrypted, StandardCharsets.UTF_8), originalJson);

        byte[] decrypted = SaveEncryptionVault.decrypt(encrypted, "SuperSecretPassword123!");
        String decryptedJson = new String(decrypted, StandardCharsets.UTF_8);

        assertEquals(originalJson, decryptedJson, "Decrypted text must match original plaintext exactly");
    }

    @Test
    public void testWrongPasswordRejection() throws Exception {
        String originalJson = "{\"secret\":\"world_state\"}";
        byte[] encrypted = SaveEncryptionVault.encrypt(originalJson.getBytes(StandardCharsets.UTF_8), "CorrectPassword");

        assertThrows(Exception.class, () -> {
            SaveEncryptionVault.decrypt(encrypted, "WrongPassword");
        }, "Decryption with incorrect password must fail authentication tag verification");
    }
}