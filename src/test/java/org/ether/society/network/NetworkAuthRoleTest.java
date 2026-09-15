/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkAuthRoleTest {

    @Test
    public void testRoleHierarchyPermissions() {
        AuthToken adminToken = new AuthToken("Alice", AuthToken.Role.ADMIN, "secret123");
        AuthToken plannerToken = new AuthToken("Bob", AuthToken.Role.PLANNER, "secret123");
        AuthToken observerToken = new AuthToken("Charlie", AuthToken.Role.OBSERVER, "secret123");

        assertTrue(adminToken.hasPermission(AuthToken.Role.ADMIN));
        assertTrue(adminToken.hasPermission(AuthToken.Role.PLANNER));
        assertTrue(adminToken.hasPermission(AuthToken.Role.OBSERVER));

        assertFalse(plannerToken.hasPermission(AuthToken.Role.ADMIN));
        assertTrue(plannerToken.hasPermission(AuthToken.Role.PLANNER));
        assertTrue(plannerToken.hasPermission(AuthToken.Role.OBSERVER));

        assertFalse(observerToken.hasPermission(AuthToken.Role.ADMIN));
        assertFalse(observerToken.hasPermission(AuthToken.Role.PLANNER));
        assertTrue(observerToken.hasPermission(AuthToken.Role.OBSERVER));
    }

    @Test
    public void testTokenSignatureValidation() {
        AuthToken token = new AuthToken("Silvere", AuthToken.Role.ADMIN, "CorrectKey");
        assertTrue(token.isValid("CorrectKey"), "Signature must be valid with correct key");
        assertFalse(token.isValid("TamperedKey"), "Signature must fail with invalid key");
    }
}