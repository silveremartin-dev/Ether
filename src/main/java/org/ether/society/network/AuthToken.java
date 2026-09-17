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

        private final int level;
        Role(int level) { this.level = level; }
        public int getLevel() { return level; }
    }

    private final String tokenId;
    private final String username;
    private final Role role;
    private final long issueTimestamp;
    private final String signature;

    public AuthToken(String username, Role role, String secretKey) {
        this.tokenId = UUID.randomUUID().toString();
        this.username = username != null ? username : "Anonymous";
        this.role = role != null ? role : Role.OBSERVER;
        this.issueTimestamp = System.currentTimeMillis();
        this.signature = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
    }

    public boolean isValid(String secretKey) {
        if (signature == null) return false;
        String expected = generateSignature(this.tokenId, this.username, this.role.name(), this.issueTimestamp, secretKey);
        return signature.equals(expected);
    }

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

    public String getTokenId() { return tokenId; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public long getIssueTimestamp() { return issueTimestamp; }
    public String getSignature() { return signature; }
}
