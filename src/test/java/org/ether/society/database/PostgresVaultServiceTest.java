package org.ether.society.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresVaultServiceTest {

    /**
     * Test vault schema DDL and service instantiation.
     * <p>
     * Verifies that the PostgreSQL schema DDL exists and that the {@link PostgresVaultService}
     * can be instantiated with valid connection credentials.
     * </p>
     */
    @Test
    @DisplayName("Verify PostgresVaultService instantiation & schema DDL availability")
    public void testVaultSchemaAndService() {
        File schemaSql = new File("data/vault_backups/schema.sql");

        assertTrue(schemaSql.exists(), "Schema DDL file (schema.sql) must exist");
        assertTrue(schemaSql.length() > 0, "Schema DDL file must not be empty");

        PostgresVaultService service = new PostgresVaultService("jdbc:postgresql://localhost:5432/ether", "postgres", "postgres");
        assertNotNull(service, "PostgresVaultService instance must not be null");
    }
}
