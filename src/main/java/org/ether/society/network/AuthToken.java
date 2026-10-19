/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.network;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Role-Based Access Control (RBAC) Token for Co-Governance Network Clients.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AuthToken implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN(100),
        PLANNER(50),
        OBSERVER(10);

        /** Internal state: level (int). */
        private final int level;
        Role(int level) { this.level = level; }
        /**
         * Get level.
         *
         * @return the resulting computation or state reference
         */
        public int getLevel() { return level; }
    }

    /** Internal state: token id (String). */
    private final String tokenId;
    /** Internal state: username (String). */
    private final String username;
    private final Role role;
    /** Internal state: issue timestamp (long). */
    private final long issueTimestamp;
    /** Internal state: signature (String). */
    private final String signature;

    /**
     * Constructs a newly issued AuthToken and computes its SHA-256 HMAC signature.
     *
     * @param username client identity name
     * @param role assigned RBAC role
     * @param secretKey cryptographic signing secret
     */
    public AuthToken(String username, Role role, String secretKey) {
        this.tokenId = UUID.randomUUID().toString();
        this.username = username != null ? username : "Anonymous";
        this.role = role != null ? role : Role.OBSERVER;
        this.issueTimestamp = System.currentTimeMillis();
        this.signature = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
    }

    /**
     * Reconstructs an existing AuthToken from validated wire transport fields.
     *
     * @param tokenId unique token identifier
     * @param username client identity name
     * @param role assigned RBAC role
     * @param issueTimestamp token epoch issue timestamp in milliseconds
     * @param signature cryptographic signature string
     */
    public AuthToken(String tokenId, String username, Role role, long issueTimestamp, String signature) {
        this.tokenId = tokenId != null ? tokenId : UUID.randomUUID().toString();
        this.username = username != null ? username : "Anonymous";
        this.role = role != null ? role : Role.OBSERVER;
        this.issueTimestamp = issueTimestamp;
        this.signature = signature;
    }

    /**
     * Validates whether the token's signature matches the expected signature computed with the secret key.
     *
     * @param secretKey shared secret key
     * @return true if the signature is valid, false otherwise
     */
    public boolean isValid(String secretKey) {
        if (signature == null || secretKey == null) return false;
        String expected = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
        return signature.equals(expected);
    }

    /**
     * Checks if this token satisfies the minimum required authorization level.
     *
     * @param requiredRole minimum role required
     * @return true if authorized, false otherwise
     */
    public boolean hasPermission(Role requiredRole) {
        if (requiredRole == null) return true;
        return this.role != null && this.role.getLevel() >= requiredRole.getLevel();
    }

    /**
     * Computes the SHA-256 signature for the given token payload and secret key.
     *
     * @param id token UUID
     * @param user username
     * @param roleName role identifier
     * @param timestamp issue timestamp
     * @param secret shared secret key
     * @return hexadecimal SHA-256 signature string
     */
    public static String generateSignature(String id, String user, String roleName, long timestamp, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String payload = id + ":" + user + ":" + roleName + ":" + timestamp + ":" + secret;
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the token UUID.
     * @return token identifier string
     */
    public String getTokenId() { return tokenId; }

    /**
     * Returns the username associated with this token.
     * @return username string
     */
    public String getUsername() { return username; }

    /**
     * Returns the RBAC role granted by this token.
     * @return assigned Role
     */
    public Role getRole() { return role; }

    /**
     * Returns the epoch timestamp when this token was generated.
     * @return issue timestamp in milliseconds
     */
    public long getIssueTimestamp() { return issueTimestamp; }

    /**
     * Returns the cryptographic signature string.
     * @return signature string
     */
    public String getSignature() { return signature; }
}
