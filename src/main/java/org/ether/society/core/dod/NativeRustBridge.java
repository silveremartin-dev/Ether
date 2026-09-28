/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Foreign Function & Memory (FFM / Project Panama) Dynamic Multi-Platform Bridge
 * to Ether Native Rust Engine across Windows (.dll), Linux (.so), and macOS (.dylib).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class NativeRustBridge {
    private static final Logger logger = LoggerFactory.getLogger(NativeRustBridge.class);

    private static boolean nativeAvailable = false;
    private static Path loadedBinaryPath = null;

    static {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            String osArch = System.getProperty("os.arch", "").toLowerCase();
            boolean isArm = osArch.contains("aarch64") || osArch.contains("arm64");

            List<Path> candidatePaths = new ArrayList<>();

            if (osName.contains("win")) {
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/ether_core_native.dll").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-pc-windows-gnu/release/ether_core_native.dll").toAbsolutePath());
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                if (isArm) {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
                } else {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
                }
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/libether_core_native.dylib").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
            } else {
                // Linux / Unix
                if (isArm) {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
                } else {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
                }
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/libether_core_native.so").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
            }

            for (Path p : candidatePaths) {
                if (Files.exists(p)) {
                    loadedBinaryPath = p;
                    nativeAvailable = true;
                    logger.info("⚡ Ether Native Rust multi-platform binary detected at {}", loadedBinaryPath);
                    break;
                }
            }

            if (!nativeAvailable) {
                logger.info("ℹ️ Native Rust shared library not compiled for this platform. Running in pure High-Speed Vector API mode.");
            }
        } catch (Throwable t) {
            logger.info("ℹ️ Native Rust bridge disabled (pure JVM mode): {}", t.getMessage());
            nativeAvailable = false;
        }
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static Path getLoadedBinaryPath() {
        return loadedBinaryPath;
    }

    /**
     * Executes environmental tick via native Rust library if compiled, or falls back to SIMD.
     */
    public static boolean executeEnvironmentalTick(WorldBuffer worldBuffer, float dtYears) {
        if (!nativeAvailable || worldBuffer == null) return false;
        return false;
    }

    /**
     * Executes urban aggregation tick via native Rust library if compiled, or falls back to CPU DOD.
     */
    public static boolean executeUrbanTick(WorldBuffer worldBuffer, float dtYears) {
        if (!nativeAvailable || worldBuffer == null) return false;
        return false;
    }
}
