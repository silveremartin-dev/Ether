/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Foreign Function &amp; Memory (FFM / Project Panama) High-Performance Multi-Platform Bridge
 * to the Ether Native Rust High-Speed Simulation Core across Windows (.dll), Linux (.so), and macOS (.dylib).
 *
 * <p>Exposes zero-copy C-ABI downcall handles for the 5 fundamental cliodynamic and biophysical kernels:
 * <ul>
 *   <li>{@code ether_environmental_tick}: Farquhar FvCB Photosynthesis &amp; Priestley-Taylor Biomass</li>
 *   <li>{@code ether_urban_tick}: Agglomeration Scale Economies &amp; Tainter Institutional Entropy</li>
 *   <li>{@code ether_flux_tick}: Finite-Volume Hexagonal Trade &amp; Resource Diffusion</li>
 *   <li>{@code ether_demographic_tick}: Lotka-Euler Demographic Cohorts &amp; Kleiber Metabolic Scaling</li>
 *   <li>{@code ether_culture_tick}: Langevin SDE Cultural Drift &amp; Anchored Institutional Tensors</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NativeRustBridge {
    private static final Logger logger = LoggerFactory.getLogger(NativeRustBridge.class);

    private static boolean nativeAvailable = false;
    private static Path loadedBinaryPath = null;
    private static Arena nativeLibraryArena = null;

    // Downcall Method Handles
    static MethodHandle envTickHandle = null;
    static MethodHandle urbanTickHandle = null;
    static MethodHandle fluxTickHandle = null;
    static MethodHandle demogTickHandle = null;
    static MethodHandle demogTickFullHandle = null;
    static MethodHandle cultureTickHandle = null;
    static MethodHandle cultureTickFullHandle = null;
    static MethodHandle tier2TickHandle = null;

    static {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            String osArch = System.getProperty("os.arch", "").toLowerCase();
            boolean isArm = osArch.contains("aarch64") || osArch.contains("arm64");

            List<Path> candidatePaths = new ArrayList<>();

            if (osName.contains("win")) {
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/ether_core_native.dll").toAbsolutePath());
                candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-pc-windows-gnu/release/ether_core_native.dll").toAbsolutePath());
                candidatePaths.add(Paths.get("ether_core_native.dll").toAbsolutePath());
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                if (isArm) {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
                } else {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-apple-darwin/release/libether_core_native.dylib").toAbsolutePath());
                }
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/libether_core_native.dylib").toAbsolutePath());
                candidatePaths.add(Paths.get("libether_core_native.dylib").toAbsolutePath());
            } else {
                candidatePaths.add(Paths.get("/opt/ether/libether_core_native.so").toAbsolutePath());
                candidatePaths.add(Paths.get("libether_core_native.so").toAbsolutePath());
                if (isArm) {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/aarch64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
                } else {
                    candidatePaths.add(Paths.get("native/ether-core-native/target/x86_64-unknown-linux-gnu/release/libether_core_native.so").toAbsolutePath());
                }
                candidatePaths.add(Paths.get("native/ether-core-native/target/release/libether_core_native.so").toAbsolutePath());
            }

            for (Path p : candidatePaths) {
                if (Files.exists(p)) {
                    loadedBinaryPath = p;
                    break;
                }
            }

            if (loadedBinaryPath != null) {
                nativeLibraryArena = Arena.ofShared();
                SymbolLookup lookup = SymbolLookup.libraryLookup(loadedBinaryPath, nativeLibraryArena);
                Linker linker = Linker.nativeLinker();

                // 1. ether_environmental_tick(world_ptr: *mut WorldBufferRaw, dt_years: c_float)
                lookup.find("ether_environmental_tick").ifPresent(addr -> {
                    envTickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
                });

                // 2. ether_urban_tick(world_ptr: *mut WorldBufferRaw, dt_years: c_float)
                lookup.find("ether_urban_tick").ifPresent(addr -> {
                    urbanTickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
                });

                // 3. ether_flux_tick(world_ptr: *mut WorldBufferRaw, dt_seconds: c_float)
                lookup.find("ether_flux_tick").ifPresent(addr -> {
                    fluxTickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
                });

                // 4. ether_demographic_tick(world_ptr: *mut, agents_ptr: *mut, target_cohort_size: float, dt_years: float)
                lookup.find("ether_demographic_tick").ifPresent(addr -> {
                    demogTickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
                });

                lookup.find("ether_demographic_tick_full").ifPresent(addr -> {
                    demogTickFullHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_LONG));
                });

                // 5. ether_culture_tick(world_ptr: *mut, agents_ptr: *mut, dt_years: float, diff: float, force: float)
                lookup.find("ether_culture_tick").ifPresent(addr -> {
                    cultureTickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
                });

                lookup.find("ether_culture_tick_full").ifPresentOrElse(addr -> {
                    cultureTickFullHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
                    logger.info("DEBUG: ether_culture_tick_full BOUND successfully!");
                }, () -> {
                    logger.warn("DEBUG: ether_culture_tick_full NOT FOUND in loaded library!");
                });

                // 6. ether_tier2_tick(world_ptr: *mut WorldBufferRaw, dt_years: c_float)
                lookup.find("ether_tier2_tick").ifPresent(addr -> {
                    tier2TickHandle = linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
                    logger.info("DEBUG: ether_tier2_tick BOUND successfully!");
                });

                nativeAvailable = (envTickHandle != null && urbanTickHandle != null && fluxTickHandle != null
                        && demogTickHandle != null && cultureTickHandle != null);

                if (nativeAvailable) {
                    logger.info("⚡ Ether Native Rust multi-platform binary successfully bound via Panama FFM at {}", loadedBinaryPath);
                } else {
                    logger.warn("⚠️ Native binary found at {} but missing exported kernel symbols.", loadedBinaryPath);
                }
            } else {
                logger.info("ℹ️ Native Rust shared library not found. Running in pure High-Speed Vector API mode.");
            }
        } catch (Throwable t) {
            logger.info("ℹ️ Native Rust bridge disabled (pure JVM mode): {}", t.getMessage());
            nativeAvailable = false;
        }
    }

    /**
     * Checks if the Native Rust core shared library is compiled, loaded, and ready for execution.
     *
     * @return {@code true} if all 5 native kernel downcalls are bound; {@code false} otherwise
     */
    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    /**
     * Retrieves the filesystem path to the currently loaded native binary (.dll, .so, .dylib).
     *
     * @return path to loaded native binary, or {@code null} if not loaded
     */
    public static Path getLoadedBinaryPath() {
        return loadedBinaryPath;
    }

    /**
     * Zero-Copy Native Off-Heap Arena managing direct unmanaged memory segments for a
     * {@link WorldBuffer} and {@link AgentBuffer} across high-frequency simulation ticks.
     */
    /**
     * Backward-compatible delegation subclass for {@link NativeWorldArena}.
     */
    public static class NativeWorldArena extends org.ether.society.core.dod.NativeWorldArena {
        public NativeWorldArena(int capacity) {
            super(capacity);
        }

        public NativeWorldArena(int worldCapacity, int agentCapacity) {
            super(worldCapacity, agentCapacity);
        }
    }
}
