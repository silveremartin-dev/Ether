/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.codec;

import org.ether.society.network.EtherSecurityManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <h1>ClusterWireCodec</h1>
 * <p>
 * Network wire serialization and AES/GCM frame encryption codec for P2P distributed simulation clustering.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ClusterWireCodec {

    private ClusterWireCodec() {
        // Utility class
    }

    /**
     * Serializes and encrypts a plaintext string frame over a binary data output stream.
     *
     * @param out       target data output stream
     * @param plainText unencrypted payload
     * @param sec       security manager providing cryptographic keys
     * @throws IOException if encryption or socket write fails
     */
    public static void writeEncryptedFrame(DataOutputStream out, String plainText, EtherSecurityManager sec) throws IOException {
        try {
            String encrypted = sec.encrypt(plainText);
            byte[] bytes = encrypted.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            throw new IOException("Failed to encrypt frame", e);
        }
    }

    /**
     * Reads and decrypts a framed encrypted payload from a binary data input stream.
     *
     * @param in  source data input stream
     * @param sec security manager providing cryptographic keys
     * @return decrypted plaintext payload
     * @throws IOException if socket read or decryption fails
     */
    public static String readEncryptedFrame(DataInputStream in, EtherSecurityManager sec) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        String encrypted = new String(bytes, StandardCharsets.UTF_8);
        try {
            return sec.decrypt(encrypted);
        } catch (Exception e) {
            throw new IOException("Failed to decrypt frame", e);
        }
    }
}
