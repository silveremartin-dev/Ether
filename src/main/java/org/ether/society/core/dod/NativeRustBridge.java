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
    private static MethodHandle envTickHandle = null;
    private static MethodHandle urbanTickHandle = null;
    private static MethodHandle fluxTickHandle = null;
    private static MethodHandle demogTickHandle = null;
    private static MethodHandle demogTickFullHandle = null;
    private static MethodHandle cultureTickHandle = null;
    private static MethodHandle cultureTickFullHandle = null;

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
    public static class NativeWorldArena implements AutoCloseable {
        private final Arena arena;
        private final int capacity;
        private final int agentCapacity;
        private final MemorySegment worldStructSeg;
        private final MemorySegment agentStructSeg;

        // World primitive arrays segments
        private final MemorySegment h3IndexesSeg;
        private final MemorySegment neighborIndexesSeg;
        private final MemorySegment elevationSeg;
        private final MemorySegment temperatureSeg;
        private final MemorySegment rainfallSeg;
        private final MemorySegment biomesSeg;
        private final MemorySegment foodResourceSeg;
        private final MemorySegment waterResourceSeg;
        private final MemorySegment woodResourceSeg;
        private final MemorySegment metalResourceSeg;
        private final MemorySegment clayResourceSeg;
        private final MemorySegment biomassHumanSeg;
        private final MemorySegment biomassLivestockSeg;
        private final MemorySegment biomassFishSeg;
        private final MemorySegment biomassAgricultureSeg;
        private final MemorySegment biomassNaturalSeg;
        private final MemorySegment energyWindSeg;
        private final MemorySegment energySolarSeg;
        private final MemorySegment energyFireSeg;
        private final MemorySegment energySlavesSeg;
        private final MemorySegment energyFoodConsumedSeg;
        private final MemorySegment lifespanSeg;
        private final MemorySegment fertilitySeg;
        private final MemorySegment giniIndexSeg;
        private final MemorySegment technologyLevelSeg;
        private final MemorySegment resourceCapitalSeg;
        private final MemorySegment fluxPressureSeg;
        private final MemorySegment localPriceSeg;
        private final MemorySegment storageSeg;
        private final MemorySegment institutionalComplexitySeg;

        // Agent primitive arrays segments
        private final MemorySegment hexIdsSeg;
        private final MemorySegment agentH3Seg;
        private final MemorySegment massSeg;
        private final MemorySegment energySeg;
        private final MemorySegment sigmaCostSeg;
        private final MemorySegment techLevelSeg;
        private final MemorySegment ageSeg;
        private final MemorySegment birthsSeg;
        private final MemorySegment deathsSeg;
        private final MemorySegment genCountSeg;
        private final MemorySegment geneticsSeg;
        private final MemorySegment cultureSeg;

        public NativeWorldArena(int capacity) {
            this(capacity, capacity);
        }

        public NativeWorldArena(int worldCapacity, int agentCapacity) {
            this.capacity = worldCapacity;
            this.agentCapacity = agentCapacity;
            this.arena = Arena.ofShared();

            long cap = worldCapacity;
            this.h3IndexesSeg = arena.allocate(cap * Long.BYTES, 64);
            this.neighborIndexesSeg = arena.allocate(cap * 6 * Integer.BYTES, 64);
            this.elevationSeg = arena.allocate(cap * Float.BYTES, 64);
            this.temperatureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.rainfallSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomesSeg = arena.allocate(cap * Byte.BYTES, 64);
            this.foodResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.waterResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.woodResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.metalResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.clayResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassHumanSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassLivestockSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassFishSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassAgricultureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassNaturalSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyWindSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energySolarSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyFireSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energySlavesSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyFoodConsumedSeg = arena.allocate(cap * Float.BYTES, 64);
            this.lifespanSeg = arena.allocate(cap * Float.BYTES, 64);
            this.fertilitySeg = arena.allocate(cap * Float.BYTES, 64);
            this.giniIndexSeg = arena.allocate(cap * Float.BYTES, 64);
            this.technologyLevelSeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceCapitalSeg = arena.allocate(cap * Float.BYTES, 64);
            this.fluxPressureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.localPriceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.storageSeg = arena.allocate(cap * Float.BYTES, 64);
            this.institutionalComplexitySeg = arena.allocate(cap * Float.BYTES, 64);

            // Populate WorldBufferRaw struct in native memory (248 bytes)
            this.worldStructSeg = arena.allocate(248, 8);
            worldStructSeg.set(ValueLayout.JAVA_LONG, 0, cap);
            worldStructSeg.set(ValueLayout.ADDRESS, 8, h3IndexesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 16, neighborIndexesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 24, elevationSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 32, temperatureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 40, rainfallSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 48, biomesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 56, foodResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 64, waterResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 72, woodResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 80, metalResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 88, clayResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 96, biomassHumanSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 104, biomassLivestockSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 112, biomassFishSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 120, biomassAgricultureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 128, biomassNaturalSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 136, energyWindSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 144, energySolarSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 152, energyFireSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 160, energySlavesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 168, energyFoodConsumedSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 176, lifespanSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 184, fertilitySeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 192, giniIndexSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 200, technologyLevelSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 208, resourceCapitalSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 216, fluxPressureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 224, localPriceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 232, storageSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 240, institutionalComplexitySeg);

            // Agent Buffer allocation
            long aCap = agentCapacity;
            this.hexIdsSeg = arena.allocate(aCap * Integer.BYTES, 64);
            this.agentH3Seg = arena.allocate(aCap * Long.BYTES, 64);
            this.massSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.energySeg = arena.allocate(aCap * Float.BYTES, 64);
            this.sigmaCostSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.techLevelSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.ageSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.birthsSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.deathsSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.genCountSeg = arena.allocate(aCap * Integer.BYTES, 64);
            this.geneticsSeg = arena.allocate(aCap * 4 * Float.BYTES, 64);
            this.cultureSeg = arena.allocate(aCap * 4 * Float.BYTES, 64);

            // Populate AgentBufferRaw struct in native memory (104 bytes)
            this.agentStructSeg = arena.allocate(104, 8);
            agentStructSeg.set(ValueLayout.JAVA_LONG, 0, aCap);
            agentStructSeg.set(ValueLayout.ADDRESS, 8, hexIdsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 16, agentH3Seg);
            agentStructSeg.set(ValueLayout.ADDRESS, 24, massSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 32, energySeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 40, sigmaCostSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 48, techLevelSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 56, ageSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 64, birthsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 72, deathsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 80, genCountSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 88, geneticsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 96, cultureSeg);
        }

        /**
         * Fast bulk copy from Java {@link WorldBuffer} and {@link AgentBuffer} arrays into native memory.
         */
        public void syncFromJavaBuffers(WorldBuffer wb, AgentBuffer ab) {
            int count = Math.min(capacity, wb.getCapacity());
            if (wb.getElevation() != null) MemorySegment.copy(wb.getElevation(), 0, elevationSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getTemperature() != null) MemorySegment.copy(wb.getTemperature(), 0, temperatureSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getRainfall() != null) MemorySegment.copy(wb.getRainfall(), 0, rainfallSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomes() != null) MemorySegment.copy(wb.getBiomes(), 0, biomesSeg, ValueLayout.JAVA_BYTE, 0, count);
            if (wb.getFoodResource() != null) MemorySegment.copy(wb.getFoodResource(), 0, foodResourceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getWaterResource() != null) MemorySegment.copy(wb.getWaterResource(), 0, waterResourceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassHuman() != null) MemorySegment.copy(wb.getBiomassHuman(), 0, biomassHumanSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassNatural() != null) MemorySegment.copy(wb.getBiomassNatural(), 0, biomassNaturalSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassAgriculture() != null) MemorySegment.copy(wb.getBiomassAgriculture(), 0, biomassAgricultureSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassFish() != null) MemorySegment.copy(wb.getBiomassFish(), 0, biomassFishSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceCapital() != null) MemorySegment.copy(wb.getResourceCapital(), 0, resourceCapitalSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getTechnologyLevel() != null) MemorySegment.copy(wb.getTechnologyLevel(), 0, technologyLevelSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getLifespan() != null) MemorySegment.copy(wb.getLifespan(), 0, lifespanSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getFertility() != null) MemorySegment.copy(wb.getFertility(), 0, fertilitySeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getInstitutionalComplexity() != null) MemorySegment.copy(wb.getInstitutionalComplexity(), 0, institutionalComplexitySeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getLocalPrice() != null) MemorySegment.copy(wb.getLocalPrice(), 0, localPriceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getEnergyFoodConsumed() != null) MemorySegment.copy(wb.getEnergyFoodConsumed(), 0, energyFoodConsumedSeg, ValueLayout.JAVA_FLOAT, 0, count);

            if (wb.getNeighborIndexes() != null) {
                int[][] nbrs = wb.getNeighborIndexes();
                int rows = Math.min(capacity, nbrs.length);
                for (int r = 0; r < rows; r++) {
                    if (nbrs[r] != null) {
                        MemorySegment.copy(nbrs[r], 0, neighborIndexesSeg, ValueLayout.JAVA_INT, (long) r * 6 * Integer.BYTES, Math.min(6, nbrs[r].length));
                    }
                }
            }

            if (ab != null) {
                int aCount = Math.min(agentCapacity, ab.getCapacity());
                if (ab.getHexIds() != null) MemorySegment.copy(ab.getHexIds(), 0, hexIdsSeg, ValueLayout.JAVA_INT, 0, aCount);
                if (ab.getMass() != null) MemorySegment.copy(ab.getMass(), 0, massSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getEnergy() != null) MemorySegment.copy(ab.getEnergy(), 0, energySeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getAge() != null) MemorySegment.copy(ab.getAge(), 0, ageSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getTechLevel() != null) MemorySegment.copy(ab.getTechLevel(), 0, techLevelSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getBirths() != null) MemorySegment.copy(ab.getBirths(), 0, birthsSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getDeaths() != null) MemorySegment.copy(ab.getDeaths(), 0, deathsSeg, ValueLayout.JAVA_FLOAT, 0, aCount);

                if (ab.getGenetics() != null) {
                    float[][] gen = ab.getGenetics();
                    for (int d = 0; d < Math.min(4, gen.length); d++) {
                        if (gen[d] != null) {
                            MemorySegment.copy(gen[d], 0, geneticsSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, Math.min(aCount, gen[d].length));
                        }
                    }
                }
                if (ab.getCulture() != null) {
                    float[][] cul = ab.getCulture();
                    for (int d = 0; d < Math.min(4, cul.length); d++) {
                        if (cul[d] != null) {
                            MemorySegment.copy(cul[d], 0, cultureSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, Math.min(aCount, cul[d].length));
                        }
                    }
                }
            }
        }

        /**
         * Fast bulk copy from native memory back into Java arrays.
         */
        public void syncToJavaBuffers(WorldBuffer wb, AgentBuffer ab) {
            int count = Math.min(capacity, wb.getCapacity());
            if (wb.getFoodResource() != null) MemorySegment.copy(foodResourceSeg, ValueLayout.JAVA_FLOAT, 0, wb.getFoodResource(), 0, count);
            if (wb.getBiomassHuman() != null) MemorySegment.copy(biomassHumanSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassHuman(), 0, count);
            if (wb.getBiomassNatural() != null) MemorySegment.copy(biomassNaturalSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassNatural(), 0, count);
            if (wb.getBiomassAgriculture() != null) MemorySegment.copy(biomassAgricultureSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassAgriculture(), 0, count);
            if (wb.getResourceCapital() != null) MemorySegment.copy(resourceCapitalSeg, ValueLayout.JAVA_FLOAT, 0, wb.getResourceCapital(), 0, count);
            if (wb.getTechnologyLevel() != null) MemorySegment.copy(technologyLevelSeg, ValueLayout.JAVA_FLOAT, 0, wb.getTechnologyLevel(), 0, count);
            if (wb.getInstitutionalComplexity() != null) MemorySegment.copy(institutionalComplexitySeg, ValueLayout.JAVA_FLOAT, 0, wb.getInstitutionalComplexity(), 0, count);
            if (wb.getLocalPrice() != null) MemorySegment.copy(localPriceSeg, ValueLayout.JAVA_FLOAT, 0, wb.getLocalPrice(), 0, count);
            if (wb.getEnergyFoodConsumed() != null) MemorySegment.copy(energyFoodConsumedSeg, ValueLayout.JAVA_FLOAT, 0, wb.getEnergyFoodConsumed(), 0, count);

            if (ab != null) {
                int aCount = Math.min(agentCapacity, ab.getCapacity());
                if (ab.getHexIds() != null) MemorySegment.copy(hexIdsSeg, ValueLayout.JAVA_INT, 0, ab.getHexIds(), 0, aCount);
                if (ab.getMass() != null) MemorySegment.copy(massSeg, ValueLayout.JAVA_FLOAT, 0, ab.getMass(), 0, aCount);
                if (ab.getEnergy() != null) MemorySegment.copy(energySeg, ValueLayout.JAVA_FLOAT, 0, ab.getEnergy(), 0, aCount);
                if (ab.getAge() != null) MemorySegment.copy(ageSeg, ValueLayout.JAVA_FLOAT, 0, ab.getAge(), 0, aCount);
                if (ab.getTechLevel() != null) MemorySegment.copy(techLevelSeg, ValueLayout.JAVA_FLOAT, 0, ab.getTechLevel(), 0, aCount);
                if (ab.getBirths() != null) MemorySegment.copy(birthsSeg, ValueLayout.JAVA_FLOAT, 0, ab.getBirths(), 0, aCount);
                if (ab.getDeaths() != null) MemorySegment.copy(deathsSeg, ValueLayout.JAVA_FLOAT, 0, ab.getDeaths(), 0, aCount);

                if (ab.getGenetics() != null) {
                    float[][] gen = ab.getGenetics();
                    for (int d = 0; d < Math.min(4, gen.length); d++) {
                        if (gen[d] != null) {
                            MemorySegment.copy(geneticsSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, gen[d], 0, Math.min(aCount, gen[d].length));
                        }
                    }
                }
                if (ab.getCulture() != null) {
                    float[][] cul = ab.getCulture();
                    for (int d = 0; d < Math.min(4, cul.length); d++) {
                        if (cul[d] != null) {
                            MemorySegment.copy(cultureSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, cul[d], 0, Math.min(aCount, cul[d].length));
                        }
                    }
                }
            }
        }

        /**
         * Executes environmental photosynthesis and biomass tick via native Rust library.
         */
        public void tickEnvironmental(float dtYears) {
            if (envTickHandle == null) return;
            try {
                envTickHandle.invokeExact(worldStructSeg, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native environmental tick", t);
            }
        }

        /**
         * Executes urban agglomeration and institutional entropy tick via native Rust library.
         */
        public void tickUrban(float dtYears) {
            if (urbanTickHandle == null) return;
            try {
                urbanTickHandle.invokeExact(worldStructSeg, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native urban tick", t);
            }
        }

        /**
         * Executes finite-volume flux trade diffusion tick via native Rust library.
         */
        public void tickFlux(float dtSeconds) {
            if (fluxTickHandle == null) return;
            try {
                fluxTickHandle.invokeExact(worldStructSeg, dtSeconds);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native flux tick", t);
            }
        }

        /**
         * Executes demographic metabolic cohorts tick via native Rust library.
         */
        public void tickDemographic(float targetCohortSize, float dtYears) {
            tickDemographic(targetCohortSize, dtYears, 42L);
        }

        /**
         * Executes demographic metabolic cohorts tick with explicit scenario seed via native Rust library.
         */
        public void tickDemographic(float targetCohortSize, float dtYears, long seed) {
            if (demogTickFullHandle != null) {
                try {
                    demogTickFullHandle.invokeExact(worldStructSeg, agentStructSeg, targetCohortSize, dtYears, seed);
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("Failed native full demographic tick", t);
                }
            }
            if (demogTickHandle == null) return;
            try {
                demogTickHandle.invokeExact(worldStructSeg, agentStructSeg, targetCohortSize, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native demographic tick", t);
            }
        }

        /**
         * Executes cultural Langevin SDE tensor drift tick via native Rust library.
         */
        public void tickCulture(float dtYears, float diffusionRate, float forcingRate) {
            if (cultureTickHandle == null) return;
            try {
                cultureTickHandle.invokeExact(worldStructSeg, agentStructSeg, dtYears, diffusionRate, forcingRate);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native culture tick", t);
            }
        }

        /**
         * Executes cultural Langevin SDE tensor drift tick with explicit stochastic Wiener drift via native Rust library.
         */
        public void tickCulture(float dtYears, float diffusionRate, float forcingRate, float mutationRate, long seed, long tickCounter) {
            if (cultureTickFullHandle != null) {
                try {
                    cultureTickFullHandle.invokeExact(worldStructSeg, agentStructSeg, dtYears, diffusionRate, forcingRate, mutationRate, seed, tickCounter);
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("Failed native full culture tick", t);
                }
            }
            logger.warn("cultureTickFullHandle is null! Falling back to 5-param culture tick");
            tickCulture(dtYears, diffusionRate, forcingRate);
        }

        /**
         * Executes the complete unified 5-kernel simulation cycle in native Rust with 0 GC overhead.
         */
        public void stepUnifiedCycle(float dtDays) {
            stepUnifiedCycle(dtDays, 42L, 1L);
        }

        /**
         * Executes the complete unified 5-kernel simulation cycle in native Rust with explicit scenario seed.
         */
        public void stepUnifiedCycle(float dtDays, long seed, long tickCounter) {
            float dtYears = dtDays / 365.25f;
            float dtSeconds = dtDays * 86400.0f;

            tickEnvironmental(dtYears);
            tickDemographic(500.0f, dtYears, seed);
            tickUrban(dtYears);
            tickFlux(dtSeconds);
            tickCulture(dtYears, 0.05f, 0.02f, 0.015f, seed, tickCounter);
        }

        @Override
        public void close() {
            arena.close();
        }
    }
}
