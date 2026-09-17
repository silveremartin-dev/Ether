/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Security Audit Logger for Ether Network Mode.
 * Tracks all network authentication attempts, encrypted payload decryptions, and security audit violations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EtherSecurityAuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(EtherSecurityAuditLogger.class);
    private static final File auditLogFile = new File("ether_network_security_audit.log");

    public static synchronized void logAuditEvent(String eventType, String clientIp, String details) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] AUDIT [%s] Client: %s | Details: %s", timestamp, eventType, clientIp, details);

        logger.info(logEntry);

        try (FileWriter writer = new FileWriter(auditLogFile, true)) {
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            logger.error("Failed to write security audit log: {}", e.getMessage());
        }
    }
}

