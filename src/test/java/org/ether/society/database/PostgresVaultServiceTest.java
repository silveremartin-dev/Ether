package org.ether.society.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresVaultServiceTest {

    @Test
    @DisplayName("Verify PostgresVaultService instantiation & backup script availability")
    public void testVaultBackupFiles() {
        File jsonDump = new File("data/vault_backups/ether_full_vault_dump.json");
        File sqlDump = new File("data/vault_backups/ether_postgres_import.sql");

        assertTrue(jsonDump.exists(), "JSON Vault Dump file must exist");
        assertTrue(sqlDump.exists(), "SQL Postgres import dump file must exist");
        assertTrue(jsonDump.length() > 1000000, "JSON Vault dump size must be over 1MB");
        assertTrue(sqlDump.length() > 1000000, "SQL Postgres import dump size must be over 1MB");

        PostgresVaultService service = new PostgresVaultService("jdbc:postgresql://localhost:5432/ether", "postgres", "postgres");
        assertNotNull(service, "PostgresVaultService instance must not be null");
    }
}
