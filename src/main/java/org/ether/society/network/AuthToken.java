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
 * @version 1.0.0-beta.1
 */
public class AuthToken implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN(100),
        PLANNER(50),
        OBSERVER(10);

        /* Internal state variable for level (int). */
        private final int level;
        Role(int level) { this.level = level; }
        /*
         * Get level.
         * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
         *
         * @return the resulting computation or state reference
         */
        public int getLevel() { return level; }
    }

    /* Internal state variable for token id (String). */
    private final String tokenId;
    /* Internal state variable for username (String). */
    private final String username;
    private final Role role;
    /* Internal state variable for issue timestamp (long). */
    private final long issueTimestamp;
    /* Internal state variable for signature (String). */
    private final String signature;

    /*
     * Auth token.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @param username the username parameter (String)
     * @param role the role parameter (Role)
     * @param secretKey the secret key parameter (String)
     */
    public AuthToken(String username, Role role, String secretKey) {
        this.tokenId = UUID.randomUUID().toString();
        this.username = username != null ? username : "Anonymous";
        this.role = role != null ? role : Role.OBSERVER;
        this.issueTimestamp = System.currentTimeMillis();
        this.signature = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
    }

    /*
     * Is valid.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @param secretKey the secret key parameter (String)
     * @return the resulting computation or state reference
     */
    public boolean isValid(String secretKey) {
        if (signature == null) return false;
        String expected = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
        return signature.equals(expected);
    }

    /*
     * Has permission.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @param requiredRole the required role parameter (Role)
     * @return the resulting computation or state reference
     */
    public boolean hasPermission(Role requiredRole) {
        if (requiredRole == null) return true;
        return this.role.getLevel() >= requiredRole.getLevel();
    }

    private static String generateSignature(String id, String user, String roleName, long timestamp, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String payload = id + ":" + user + ":" + roleName + ":" + timestamp + ":" + secret;
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /*
     * Get token id.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @return the resulting computation or state reference
     */
    public String getTokenId() { return tokenId; }
    /*
     * Get username.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @return the resulting computation or state reference
     */
    public String getUsername() { return username; }
    /*
     * Get role.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @return the resulting computation or state reference
     */
    public Role getRole() { return role; }
    /*
     * Get issue timestamp.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @return the resulting computation or state reference
     */
    public long getIssueTimestamp() { return issueTimestamp; }
    /*
     * Get signature.
     * Enforces physical invariants and updates associated state variables within {@code AuthToken}.
     *
     * @return the resulting computation or state reference
     */
    public String getSignature() { return signature; }
}
