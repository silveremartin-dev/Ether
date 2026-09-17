/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Vault providing AES-256 GCM encryption and decryption for simulation state at rest.
 * Protects saved worlds and scenarios against unauthorized offline tampering or extraction.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SaveEncryptionVault {
    private static final Logger logger = LoggerFactory.getLogger(SaveEncryptionVault.class);

    private static final String MAGIC_HEADER = "ETHER_ENC_V1";
    private static final byte[] MAGIC_BYTES = MAGIC_HEADER.getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12; // 96-bit IV for GCM
    private static final int GCM_TAG_LENGTH = 128;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    private static final String DEFAULT_APP_SECRET = "Ether_Planetary_Simulation_Vault_Key_2026_Secured";

    /**
     * Checks if the given raw bytes represent an encrypted Ether save snapshot.
     */
    public static boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC_BYTES.length) return false;
        for (int i = 0; i < MAGIC_BYTES.length; i++) {
            if (data[i] != MAGIC_BYTES[i]) return false;
        }
        return true;
    }

    /**
     * Encrypts plaintext bytes using AES-256 GCM.
     */
    public static byte[] encrypt(byte[] plaintext, String password) throws Exception {
        if (plaintext == null) return new byte[0];
        String pass = (password != null && !password.isBlank()) ? password : DEFAULT_APP_SECRET;

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey secretKey = deriveKey(pass, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        // Format: [MAGIC_BYTES (12)] + [SALT (16)] + [IV (12)] + [CIPHERTEXT]
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC_BYTES.length + SALT_LENGTH + IV_LENGTH + ciphertext.length);
        buffer.put(MAGIC_BYTES);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    public static byte[] encrypt(byte[] plaintext) throws Exception {
        return encrypt(plaintext, DEFAULT_APP_SECRET);
    }

    /**
     * Decrypts encrypted bytes using AES-256 GCM.
     */
    public static byte[] decrypt(byte[] encryptedData, String password) throws Exception {
        if (encryptedData == null || !isEncrypted(encryptedData)) {
            throw new IllegalArgumentException("Data is not a valid encrypted Ether snapshot");
        }
        String pass = (password != null && !password.isBlank()) ? password : DEFAULT_APP_SECRET;

        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        byte[] magic = new byte[MAGIC_BYTES.length];
        buffer.get(magic);

        byte[] salt = new byte[SALT_LENGTH];
        buffer.get(salt);

        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey secretKey = deriveKey(pass, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        return cipher.doFinal(ciphertext);
    }

    public static byte[] decrypt(byte[] encryptedData) throws Exception {
        return decrypt(encryptedData, DEFAULT_APP_SECRET);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
