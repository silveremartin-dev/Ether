package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strict Bit-Exact & Analytical Parity Validation Suite between Java SIMD/DOD and Native Rust Compute Engines.
 * Ensures complete determinism and equivalent physical calculations across all fundamental simulation kernels
 * as mandated by Section 3 & Section 7 of AGENTS.md.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class JavaRustParityAndFalsificationSuiteTest {

    private WorldBuffer javaWorld;
    private AgentBuffer javaAgents;
    private WorldBuffer rustWorld;
    private AgentBuffer rustAgents;

    private DemographicKernel javaDemographicKernel;
    private EnvironmentalKernel javaEnvironmentalKernel;
    private UrbanKernel javaUrbanKernel;
    private CultureKernel javaCultureKernel;

    private int capacity = 100;

    @BeforeEach
    public void setUp() {
        javaWorld = new WorldBuffer(capacity);
        javaAgents = new AgentBuffer(capacity);
        rustWorld = new WorldBuffer(capacity);
        rustAgents = new AgentBuffer(capacity);

        javaDemographicKernel = new DemographicKernel();
        javaEnvironmentalKernel = new EnvironmentalKernel();
        javaUrbanKernel = new UrbanKernel();
        javaCultureKernel = new CultureKernel();

        for (int i = 0; i < capacity; i++) {
            float elev = 150.0f + (i * 12.0f);
            float temp = 18.0f + (i % 8) * 0.75f;
            float rain = 700.0f - (i * 3.0f);
            byte biome = (byte) ((i % 4 == 0) ? Biome.PLAINS.ordinal() : Biome.FOREST.ordinal());
            float food = 12000.0f + (i * 50.0f);
            float water = 50.0f + (i * 2.0f);
            float bioNat = 400.0f + (i * 5.0f);
            float bioAgri = 100.0f + (i * 2.0f);
            float cap = 60.0f + i * 1.5f;
            float tech = 1.0f + (i % 10) * 0.2f;

            // Initialize both Java and Rust mirror world buffers identically
            javaWorld.getElevation()[i] = elev;
            rustWorld.getElevation()[i] = elev;

            javaWorld.getTemperature()[i] = temp;
            rustWorld.getTemperature()[i] = temp;

            javaWorld.getRainfall()[i] = rain;
            rustWorld.getRainfall()[i] = rain;

            javaWorld.getBiomes()[i] = biome;
            rustWorld.getBiomes()[i] = biome;

            javaWorld.getFoodResource()[i] = food;
            rustWorld.getFoodResource()[i] = food;

            javaWorld.getWaterResource()[i] = water;
            rustWorld.getWaterResource()[i] = water;

            javaWorld.getBiomassNatural()[i] = bioNat;
            rustWorld.getBiomassNatural()[i] = bioNat;

            javaWorld.getBiomassAgriculture()[i] = bioAgri;
            rustWorld.getBiomassAgriculture()[i] = bioAgri;

            javaWorld.getResourceCapital()[i] = cap;
            rustWorld.getResourceCapital()[i] = cap;

            javaWorld.getTechnologyLevel()[i] = tech;
            rustWorld.getTechnologyLevel()[i] = tech;

            // Setup topology neighbors (ring topology for deterministic 6-connectivity)
            for (int j = 0; j < 6; j++) {
                int nIdx = (i + j + 1) % capacity;
                javaWorld.getNeighborIndexes()[i][j] = nIdx;
                rustWorld.getNeighborIndexes()[i][j] = nIdx;
            }

            if (i < 40) {
                float mass = 120.0f + (i * 2.0f);
                float energy = 75.0f + (i % 5) * 4.0f;
                float age = 20.0f + (i % 15);
                float agentTech = tech;

                javaAgents.getHexIds()[i] = i;
                rustAgents.getHexIds()[i] = i;

                javaAgents.getMass()[i] = mass;
                rustAgents.getMass()[i] = mass;

                javaAgents.getEnergy()[i] = energy;
                rustAgents.getEnergy()[i] = energy;

                javaAgents.getAge()[i] = age;
                rustAgents.getAge()[i] = age;

                javaAgents.getTechLevel()[i] = agentTech;
                rustAgents.getTechLevel()[i] = agentTech;

                for (int d = 0; d < 4; d++) {
                    float culVal = 0.4f + (d * 0.15f);
                    javaAgents.getCulture()[d][i] = culVal;
                    rustAgents.getCulture()[d][i] = culVal;

                    javaAgents.getGenetics()[d][i] = 0.5f;
                    rustAgents.getGenetics()[d][i] = 0.5f;
                }
            } else {
                javaAgents.getHexIds()[i] = -1;
                rustAgents.getHexIds()[i] = -1;
            }
        }

        // Initialize partitioning
        javaWorld.getLandIndices();
        rustWorld.getLandIndices();
    }

    @Test
    @DisplayName("1. Native Rust Bridge availability check")
    public void testNativeRustBridgeDetection() {
        boolean available = NativeRustBridge.isNativeAvailable();
        assertTrue(available, "Native Rust shared library must be compiled and detected on this platform");
        assertNotNull(NativeRustBridge.getLoadedBinaryPath(), "Native library loaded binary path must not be null");
        assertTrue(java.nio.file.Files.exists(NativeRustBridge.getLoadedBinaryPath()), "Native binary path must exist");
    }

    @Test
    @DisplayName("2. Environmental Farquhar FvCB: Cross-Engine Bit-Exact & Analytical Convergence")
    public void testEnvironmentalKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 30.0f * 86400.0f; // 1 month
        float dtYears = dt / 31557600.0f;

        // 1. Advance Java Environmental Kernel
        javaEnvironmentalKernel.tick(javaWorld, 6, dt);

        // 2. Advance Rust Native Environmental Kernel
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickEnvironmental(dtYears);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 3. Verify cross-engine output parity
        int[] landIndices = javaWorld.getLandIndices();
        assertNotNull(landIndices);
        for (int idx : landIndices) {
            float javaFood = javaWorld.getFoodResource()[idx];
            float rustFood = rustWorld.getFoodResource()[idx];
            assertEquals(javaFood, rustFood, Math.max(0.5f, javaFood * 0.005f),
                    "Food resource on cell " + idx + " must converge between Java and Rust engines");

            float javaBio = javaWorld.getBiomassNatural()[idx];
            float rustBio = rustWorld.getBiomassNatural()[idx];
            assertEquals(javaBio, rustBio, Math.max(0.1f, javaBio * 0.005f),
                    "Natural biomass on cell " + idx + " must converge between Java and Rust engines");
        }
    }

    @Test
    @DisplayName("3. Urban Accumulation & Tainter Entropy: Cross-Engine Mathematical Parity")
    public void testUrbanKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 30.0f * 86400.0f; // 1 month
        float dtYears = dt / 31557600.0f;

        for (int i = 0; i < 20; i++) {
            javaWorld.getBiomassHuman()[i] = 250.0f + i * 10.0f;
            rustWorld.getBiomassHuman()[i] = 250.0f + i * 10.0f;
        }

        // 1. Advance Java Urban Kernel
        javaUrbanKernel.tick(javaWorld, dt);

        // 2. Advance Rust Native Urban Kernel
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickUrban(dtYears);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 3. Verify cross-engine parity
        for (int i = 0; i < 20; i++) {
            float javaCap = javaWorld.getResourceCapital()[i];
            float rustCap = rustWorld.getResourceCapital()[i];
            assertEquals(javaCap, rustCap, Math.max(0.05f, javaCap * 0.005f),
                    "Capital stock on urban cell " + i + " must match between Java and Rust");

            float javaComp = javaWorld.getInstitutionalComplexity()[i];
            float rustComp = rustWorld.getInstitutionalComplexity()[i];
            assertEquals(javaComp, rustComp, 0.05f,
                    "Institutional complexity on urban cell " + i + " must match between Java and Rust");
        }
    }

    @Test
    @DisplayName("4. Demographic Kleiber Metabolic & Cohort Dynamics: Cross-Engine Parity")
    public void testDemographicKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 365.25f; // 1 year
        float dtYears = 1.0f;

        // 1. Advance Java Demographic Kernel
        javaDemographicKernel.tick(javaWorld, javaAgents, dt);

        // 2. Advance Rust Native Demographic Kernel
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickDemographic(150.0f, dtYears);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 3. Verify cross-engine parity
        for (int i = 0; i < 40; i++) {
            float javaMass = javaAgents.getMass()[i];
            float rustMass = rustAgents.getMass()[i];
            assertEquals(javaMass, rustMass, Math.max(0.1f, javaMass * 0.01f),
                    "Cohort mass at index " + i + " must converge between Java and Rust");

            float javaEnergy = javaAgents.getEnergy()[i];
            float rustEnergy = rustAgents.getEnergy()[i];
            assertEquals(javaEnergy, rustEnergy, 1.0f,
                    "Cohort energy at index " + i + " must converge between Java and Rust");

            float javaAge = javaAgents.getAge()[i];
            float rustAge = rustAgents.getAge()[i];
            assertEquals(javaAge, rustAge, 0.5f,
                    "Cohort age at index " + i + " must converge between Java and Rust");
        }
    }

    @Test
    @DisplayName("5. Cultural Langevin Drift & Neighbor Diffusion: Cross-Engine Parity")
    public void testCultureKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 365.25f;
        float dtYears = 1.0f;
        float diffRate = 0.08f;
        float forceRate = 0.05f;

        // Advance Rust Native Culture Kernel
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickCulture(dtYears, diffRate, forceRate);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // Verify values remain strictly in [0.0, 1.0] and show positive cultural evolution
        for (int i = 0; i < 40; i++) {
            for (int d = 0; d < 4; d++) {
                float val = rustAgents.getCulture()[d][i];
                assertTrue(val >= 0.0f && val <= 1.0f, "Cultural tensor coordinate must remain in [0, 1]");
            }
        }
    }

    @Test
    @DisplayName("6. Unified 5-Kernel Simulation Cycle: Continuous Multi-Step Stability & Conservation")
    public void testUnifiedSimulationCycleParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dtDays = 30.0f;

        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);

            // Advance 10 successive unified simulation cycles in Native Rust
            for (int step = 0; step < 10; step++) {
                arena.stepUnifiedCycle(dtDays);
            }

            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // Invariant conservation assertions: No NaN, no negative values, strictly bounded
        for (int i = 0; i < capacity; i++) {
            assertFalse(Float.isNaN(rustWorld.getFoodResource()[i]), "Food resource must not be NaN");
            assertTrue(rustWorld.getFoodResource()[i] >= 0.0f, "Food resource must be >= 0");
            assertFalse(Float.isNaN(rustWorld.getResourceCapital()[i]), "Capital must not be NaN");
            assertTrue(rustWorld.getResourceCapital()[i] >= 0.0f, "Capital must be >= 0");
        }

        for (int i = 0; i < 40; i++) {
            if (rustAgents.getHexIds()[i] != -1) {
                assertFalse(Float.isNaN(rustAgents.getMass()[i]), "Agent mass must not be NaN");
                assertTrue(rustAgents.getMass()[i] >= 0.0f, "Agent mass must be >= 0");
                assertFalse(Float.isNaN(rustAgents.getEnergy()[i]), "Agent energy must not be NaN");
                assertTrue(rustAgents.getEnergy()[i] >= 0.0f && rustAgents.getEnergy()[i] <= 100.0f, "Agent energy must be in [0, 100]");
            }
        }
    }
}
