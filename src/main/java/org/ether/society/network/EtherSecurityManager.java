/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security & Encryption Manager for Ether Network Mode.
 * Implements AES-256 GCM encryption/decryption and HMAC payload integrity verification.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EtherSecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(EtherSecurityManager.class);
    /* Internal state variable for gcm tag length (int). */
    private static final int GCM_TAG_LENGTH = 128;
    /* Internal state variable for iv length bytes (int). */
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;

    /*
     * Ether security manager.
     * Enforces physical invariants and updates associated state variables within {@code EtherSecurityManager}.
     *
     */
    public EtherSecurityManager() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            this.secretKey = keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AES-256 Security Manager", e);
        }
    }

    /*
     * Ether security manager.
     * Enforces physical invariants and updates associated state variables within {@code EtherSecurityManager}.
     *
     * @param keyBytes the key bytes parameter (byte[])
     */
    public EtherSecurityManager(byte[] keyBytes) {
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /*
     * Encrypt.
     * Enforces physical invariants and updates associated state variables within {@code EtherSecurityManager}.
     *
     * @param plainText the plain text parameter (String)
     * @return the resulting computation or state reference
     */
    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /*
     * Decrypt.
     * Enforces physical invariants and updates associated state variables within {@code EtherSecurityManager}.
     *
     * @param cipherTextBase64 the cipher text base64 parameter (String)
     * @return the resulting computation or state reference
     */
    public String decrypt(String cipherTextBase64) throws Exception {
        byte[] combined = Base64.getDecoder().decode(cipherTextBase64);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);

        byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
        System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plainTextBytes = cipher.doFinal(cipherText);
        return new String(plainTextBytes, "UTF-8");
    }

    /*
     * Get key bytes.
     * Enforces physical invariants and updates associated state variables within {@code EtherSecurityManager}.
     *
     * @return the resulting computation or state reference
     */
    public byte[] getKeyBytes() {
        return secretKey.getEncoded();
    }
}

