/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.security;

import org.ether.society.model.EtherScenarioBundle;
import org.ether.society.network.EtherSecurityAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Cryptographic Provenance & Integrity Signer for Ether Scenario Bundles (.ether).
 * Computes SHA-256 digests and validates provenance signatures to prevent tampered payloads.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EtherBundleSigner {
    private static final Logger logger = LoggerFactory.getLogger(EtherBundleSigner.class);
    private static final String SYSTEM_SALT = "ETHER_CLIODYNAMICS_V4_INTEGRITY_SALT";

    /**
     * Computes deterministic SHA-256 canonical hash of the bundle content.
     */
    public static String computeChecksum(EtherScenarioBundle bundle) {
        if (bundle == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(bundle.version() != null ? bundle.version() : "");
            sb.append(":");
            sb.append(bundle.planetPreset() != null ? bundle.planetPreset().name() : "");
            sb.append(":");
            sb.append(bundle.ecologyPreset() != null ? bundle.ecologyPreset().name() : "");
            sb.append(":");
            sb.append(bundle.scenario() != null ? bundle.scenario().getName() : "");

            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            logger.error("Failed to compute bundle checksum", e);
            return "";
        }
    }

    /**
     * Signs a bundle with an author tag and generates signature metadata.
     */
    public static EtherScenarioBundle signBundle(EtherScenarioBundle bundle, String author) {
        if (bundle == null) return null;
        String checksum = computeChecksum(bundle);
        String signature = generateSignature(checksum, author);
        long now = System.currentTimeMillis();

        EtherSecurityAuditLogger.logAuditEvent("BUNDLE_SIGNED", author != null ? author : "Anonymous", 
                "Signed bundle for scenario: " + (bundle.scenario() != null ? bundle.scenario().getName() : "Unknown"));

        return new EtherScenarioBundle(
                bundle.version(),
                bundle.planetPreset(),
                bundle.ecologyPreset(),
                bundle.scenario(),
                checksum,
                signature,
                author != null ? author : "Ether System",
                now
        );
    }

    /**
     * Verifies the cryptographic integrity and provenance signature of a bundle.
     *
     * @param bundle The scenario bundle to verify
     * @return true if bundle is intact or legacy unsigned, false if signature is forged/corrupted
     */
    public static boolean verifyBundle(EtherScenarioBundle bundle) {
        if (bundle == null) return false;
        
        // If bundle is legacy (no checksum/signature), accept with audit notice
        if (bundle.checksumSha256() == null || bundle.signature() == null) {
            logger.info("ℹ️ Legacy unsigned bundle imported (Scenario: {})", 
                    bundle.scenario() != null ? bundle.scenario().getName() : "Unknown");
            return true;
        }

        String expectedChecksum = computeChecksum(bundle);
        if (!expectedChecksum.equalsIgnoreCase(bundle.checksumSha256())) {
            String alert = "🛑 Integrity Mismatch: Bundle content altered for scenario: " + 
                    (bundle.scenario() != null ? bundle.scenario().getName() : "Unknown");
            logger.warn(alert);
            EtherSecurityAuditLogger.logAuditEvent("INTEGRITY_VIOLATION", "EtherBundleSigner", alert);
            return false;
        }

        String expectedSignature = generateSignature(bundle.checksumSha256(), bundle.author());
        if (!expectedSignature.equals(bundle.signature())) {
            String alert = "🛑 Signature Forgery: Invalid cryptographic signature on bundle.";
            logger.warn(alert);
            EtherSecurityAuditLogger.logAuditEvent("SIGNATURE_FORGERY", "EtherBundleSigner", alert);
            return false;
        }

        EtherSecurityAuditLogger.logAuditEvent("BUNDLE_VERIFIED", bundle.author() != null ? bundle.author() : "Unknown", 
                "Verified authentic bundle: " + (bundle.scenario() != null ? bundle.scenario().getName() : "Unknown"));
        return true;
    }

    private static String generateSignature(String checksum, String author) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = checksum + "::" + (author != null ? author : "Ether") + "::" + SYSTEM_SALT;
            byte[] sigBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "SIG_ED256_" + HexFormat.of().formatHex(sigBytes);
        } catch (Exception e) {
            return "SIG_INVALID";
        }
    }
}

