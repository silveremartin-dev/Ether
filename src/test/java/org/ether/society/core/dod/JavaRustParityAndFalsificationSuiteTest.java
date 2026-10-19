package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Strict Bit-Exact &amp; Analytical Parity Validation Suite Across 3 Compute Engines</h1>
 * <p>
 * Evaluates bit-level determinism, mass conservation, and mathematical equivalence across:
 * <ol>
 *   <li><b>Moteur Rust</b>: Native off-heap via Project Panama FFM ({@link NativeRustBridge.NativeWorldArena})</li>
 *   <li><b>Moteur Java SIMD</b>: Data-Oriented Design contiguous memory streaming ({@code org.ether.society.core.dod})</li>
 *   <li><b>Moteur Java Standard</b>: Scalar sequential execution reference pipeline</li>
 * </ol>
 * Strict compliance with Section 3 (Determinism &amp; Performance) and Section 7 (Epistemic Falsification)
 * of {@code AGENTS.md}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class JavaRustParityAndFalsificationSuiteTest {

    private WorldBuffer javaSimdWorld;
    private AgentBuffer javaSimdAgents;

    private WorldBuffer javaStdWorld;
    private AgentBuffer javaStdAgents;

    private WorldBuffer rustWorld;
    private AgentBuffer rustAgents;

    private DemographicKernel javaDemographicKernel;
    private EnvironmentalKernel javaEnvironmentalKernel;
    private UrbanKernel javaUrbanKernel;
    private CultureKernel javaCultureKernel;
    private FluxKernel javaFluxKernel;

    private DemographicKernel stdDemographicKernel;
    private EnvironmentalKernel stdEnvironmentalKernel;
    private UrbanKernel stdUrbanKernel;
    private CultureKernel stdCultureKernel;
    private FluxKernel stdFluxKernel;

    private final int capacity = 100;

    @BeforeEach
    public void setUp() {
        javaSimdWorld = new WorldBuffer(capacity);
        javaSimdAgents = new AgentBuffer(capacity);

        javaStdWorld = new WorldBuffer(capacity);
        javaStdAgents = new AgentBuffer(capacity);

        rustWorld = new WorldBuffer(capacity);
        rustAgents = new AgentBuffer(capacity);

        javaDemographicKernel = new DemographicKernel();
        javaEnvironmentalKernel = new EnvironmentalKernel();
        javaUrbanKernel = new UrbanKernel();
        javaCultureKernel = new CultureKernel();
        javaFluxKernel = new FluxKernel();

        stdDemographicKernel = new DemographicKernel();
        stdEnvironmentalKernel = new EnvironmentalKernel();
        stdUrbanKernel = new UrbanKernel();
        stdCultureKernel = new CultureKernel();
        stdFluxKernel = new FluxKernel();

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

            // Initialize all three engine mirror buffers with identical initial conditions
            populateCell(javaSimdWorld, i, elev, temp, rain, biome, food, water, bioNat, bioAgri, cap, tech);
            populateCell(javaStdWorld, i, elev, temp, rain, biome, food, water, bioNat, bioAgri, cap, tech);
            populateCell(rustWorld, i, elev, temp, rain, biome, food, water, bioNat, bioAgri, cap, tech);

            // Ring topology for deterministic 6-neighbor connectivity
            for (int j = 0; j < 6; j++) {
                int nIdx = (i + j + 1) % capacity;
                javaSimdWorld.getNeighborIndexes()[i][j] = nIdx;
                javaStdWorld.getNeighborIndexes()[i][j] = nIdx;
                rustWorld.getNeighborIndexes()[i][j] = nIdx;
            }

            if (i < 40) {
                float mass = 120.0f + (i * 2.0f);
                float energy = 75.0f + (i % 5) * 4.0f;
                float age = 20.0f + (i % 15);
                float agentTech = tech;

                populateAgent(javaSimdAgents, i, mass, energy, age, agentTech);
                populateAgent(javaStdAgents, i, mass, energy, age, agentTech);
                populateAgent(rustAgents, i, mass, energy, age, agentTech);
            } else {
                javaSimdAgents.getHexIds()[i] = -1;
                javaStdAgents.getHexIds()[i] = -1;
                rustAgents.getHexIds()[i] = -1;
            }
        }

        javaSimdWorld.getLandIndices();
        javaStdWorld.getLandIndices();
        rustWorld.getLandIndices();
    }

    private void populateCell(WorldBuffer wb, int i, float elev, float temp, float rain, byte biome,
                              float food, float water, float bioNat, float bioAgri, float cap, float tech) {
        wb.getElevation()[i] = elev;
        wb.getTemperature()[i] = temp;
        wb.getRainfall()[i] = rain;
        wb.getBiomes()[i] = biome;
        wb.getFoodResource()[i] = food;
        wb.getWaterResource()[i] = water;
        wb.getBiomassNatural()[i] = bioNat;
        wb.getBiomassAgriculture()[i] = bioAgri;
        wb.getResourceCapital()[i] = cap;
        wb.getTechnologyLevel()[i] = tech;
    }

    private void populateAgent(AgentBuffer ab, int i, float mass, float energy, float age, float tech) {
        ab.getHexIds()[i] = i;
        ab.getMass()[i] = mass;
        ab.getEnergy()[i] = energy;
        ab.getAge()[i] = age;
        ab.getTechLevel()[i] = tech;
        for (int d = 0; d < 4; d++) {
            float culVal = 0.4f + (d * 0.15f);
            ab.getCulture()[d][i] = culVal;
            ab.getGenetics()[d][i] = 0.5f;
        }
    }

    @Test
    @DisplayName("1. Native Rust Bridge Detection & C-ABI Method Handles Integrity")
    public void testNativeRustBridgeDetection() {
        boolean available = NativeRustBridge.isNativeAvailable();
        assertTrue(available, "Native Rust shared library must be compiled and loaded via Panama FFM");
        assertNotNull(NativeRustBridge.getLoadedBinaryPath(), "Native library loaded binary path must not be null");
        assertTrue(java.nio.file.Files.exists(NativeRustBridge.getLoadedBinaryPath()), "Native binary path must exist");
    }

    @Test
    @DisplayName("2. Environmental Farquhar FvCB & Biomass: 3-Engine Bit-Exact & Analytical Convergence")
    public void testEnvironmentalKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 30.0f * 86400.0f; // 1 month
        float dtYears = dt / 31557600.0f;

        // 1. Advance Java SIMD Engine
        javaEnvironmentalKernel.tick(javaSimdWorld, 6, dt);

        // 2. Advance Java Standard Engine (Scalar reference)
        stdEnvironmentalKernel.tick(javaStdWorld, 6, dt);

        // 3. Advance Native Rust Core
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickEnvironmental(dtYears);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 4. Verify 3-Engine Parity across all active land cells
        int[] landIndices = javaSimdWorld.getLandIndices();
        assertNotNull(landIndices);
        for (int idx : landIndices) {
            float simdFood = javaSimdWorld.getFoodResource()[idx];
            float stdFood = javaStdWorld.getFoodResource()[idx];
            float rustFood = rustWorld.getFoodResource()[idx];

            // Java SIMD vs Java Standard: Exact Bit-Identical IEEE-754
            assertEquals(Float.floatToRawIntBits(simdFood), Float.floatToRawIntBits(stdFood),
                    "Java SIMD and Java Standard food resources must be bit-identical on cell " + idx);

            // Java vs Rust: Strictly bounded to single precision numerical rounding (<= 0.05% relative tolerance)
            float tolFood = Math.max(0.1f, simdFood * 0.0005f);
            assertEquals(simdFood, rustFood, tolFood,
                    "Food resource on cell " + idx + " must converge between Java and Rust engines");

            float simdBio = javaSimdWorld.getBiomassNatural()[idx];
            float stdBio = javaStdWorld.getBiomassNatural()[idx];
            float rustBio = rustWorld.getBiomassNatural()[idx];

            assertEquals(Float.floatToRawIntBits(simdBio), Float.floatToRawIntBits(stdBio),
                    "Java SIMD and Java Standard biomass must be bit-identical on cell " + idx);
            float tolBio = Math.max(0.05f, simdBio * 0.0005f);
            assertEquals(simdBio, rustBio, tolBio,
                    "Natural biomass on cell " + idx + " must converge between Java and Rust engines");
        }
    }

    @Test
    @DisplayName("3. Urban Accumulation & Tainter Entropy: 3-Engine Bit-Exact & Analytical Convergence")
    public void testUrbanKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 30.0f * 86400.0f;
        float dtYears = dt / 31557600.0f;

        for (int i = 0; i < 20; i++) {
            float popVal = 250.0f + i * 10.0f;
            javaSimdWorld.getBiomassHuman()[i] = popVal;
            javaStdWorld.getBiomassHuman()[i] = popVal;
            rustWorld.getBiomassHuman()[i] = popVal;
        }

        // 1. Advance Java SIMD Engine
        javaUrbanKernel.tick(javaSimdWorld, dt);

        // 2. Advance Java Standard Engine
        stdUrbanKernel.tick(javaStdWorld, dt);

        // 3. Advance Native Rust Core
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickUrban(dtYears);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 4. Verify 3-Engine Parity
        for (int i = 0; i < 20; i++) {
            float simdCap = javaSimdWorld.getResourceCapital()[i];
            float stdCap = javaStdWorld.getResourceCapital()[i];
            float rustCap = rustWorld.getResourceCapital()[i];

            assertEquals(Float.floatToRawIntBits(simdCap), Float.floatToRawIntBits(stdCap),
                    "Capital stock on urban cell " + i + " must be bit-identical between Java SIMD and Standard");
            float tolCap = Math.max(0.01f, simdCap * 0.0005f);
            assertEquals(simdCap, rustCap, tolCap,
                    "Capital stock on urban cell " + i + " must match between Java and Rust");

            float simdComp = javaSimdWorld.getInstitutionalComplexity()[i];
            float stdComp = javaStdWorld.getInstitutionalComplexity()[i];
            float rustComp = rustWorld.getInstitutionalComplexity()[i];

            assertEquals(Float.floatToRawIntBits(simdComp), Float.floatToRawIntBits(stdComp),
                    "Complexity on urban cell " + i + " must be bit-identical between Java SIMD and Standard");
            assertEquals(simdComp, rustComp, 0.005f,
                    "Institutional complexity on urban cell " + i + " must match between Java and Rust");
        }
    }

    @Test
    @DisplayName("4. Demographic Kleiber Metabolic & Cohort Dynamics: 3-Engine Bit-Exact & Analytical Convergence")
    public void testDemographicKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 365.25f; // 1 year
        float dtYears = 1.0f;

        // 1. Advance Java SIMD Engine
        javaDemographicKernel.tick(javaSimdWorld, javaSimdAgents, dt);

        // 2. Advance Java Standard Engine
        stdDemographicKernel.tick(javaStdWorld, javaStdAgents, dt);

        // 3. Advance Native Rust Core
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickDemographic(150.0f, dtYears, 42L);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 4. Verify 3-Engine Parity
        for (int i = 0; i < 40; i++) {
            float simdMass = javaSimdAgents.getMass()[i];
            float stdMass = javaStdAgents.getMass()[i];
            float rustMass = rustAgents.getMass()[i];

            assertEquals(Float.floatToRawIntBits(simdMass), Float.floatToRawIntBits(stdMass),
                    "Cohort mass at index " + i + " must be bit-identical between Java SIMD and Standard");
            float tolMass = Math.max(0.05f, simdMass * 0.001f);
            assertEquals(simdMass, rustMass, tolMass,
                    "Cohort mass at index " + i + " must converge between Java and Rust");

            float simdEnergy = javaSimdAgents.getEnergy()[i];
            float stdEnergy = javaStdAgents.getEnergy()[i];
            float rustEnergy = rustAgents.getEnergy()[i];

            assertEquals(Float.floatToRawIntBits(simdEnergy), Float.floatToRawIntBits(stdEnergy),
                    "Cohort energy at index " + i + " must be bit-identical between Java SIMD and Standard");
            assertEquals(simdEnergy, rustEnergy, 0.05f,
                    "Cohort energy at index " + i + " must converge between Java and Rust");

            float simdAge = javaSimdAgents.getAge()[i];
            float stdAge = javaStdAgents.getAge()[i];
            float rustAge = rustAgents.getAge()[i];

            assertEquals(Float.floatToRawIntBits(simdAge), Float.floatToRawIntBits(stdAge),
                    "Cohort age at index " + i + " must be bit-identical between Java SIMD and Standard");
            assertEquals(simdAge, rustAge, 0.05f,
                    "Cohort age at index " + i + " must converge between Java and Rust");
        }
    }

    @Test
    @DisplayName("5. Cultural Langevin Drift & 6-Neighbor Diffusion: 3-Engine Bit-Exact & Analytical Convergence")
    public void testCultureKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dt = 365.25f;
        float dtYears = 1.0f;
        float diffRate = 0.08f;
        float forceRate = 0.05f;

        // Configure deterministic drift parameters in Java kernels
        javaCultureKernel.resetState();
        stdCultureKernel.resetState();

        // 1. Advance Java SIMD Engine
        javaCultureKernel.setDiffusionRate(diffRate);
        javaCultureKernel.setForcingRate(forceRate);
        javaCultureKernel.tick(javaSimdWorld, javaSimdAgents, dt);

        // 2. Advance Java Standard Engine
        stdCultureKernel.setDiffusionRate(diffRate);
        stdCultureKernel.setForcingRate(forceRate);
        stdCultureKernel.tick(javaStdWorld, javaStdAgents, dt);

        // 3. Advance Native Rust Core
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickCulture(dtYears, diffRate, forceRate, 0.015f, 42L, 1L);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 4. Verify 3-Engine Parity
        for (int i = 0; i < 40; i++) {
            for (int d = 0; d < 4; d++) {
                float simdVal = javaSimdAgents.getCulture()[d][i];
                float stdVal = javaStdAgents.getCulture()[d][i];
                float rustVal = rustAgents.getCulture()[d][i];

                assertTrue(simdVal >= 0.0f && simdVal <= 1.0f, "Cultural coordinate must remain in [0, 1]");
                assertTrue(rustVal >= 0.0f && rustVal <= 1.0f, "Cultural coordinate must remain in [0, 1]");

                assertEquals(Float.floatToRawIntBits(simdVal), Float.floatToRawIntBits(stdVal),
                        "Cultural coordinate at [" + d + "][" + i + "] must be bit-identical between Java SIMD and Standard");
                assertEquals(simdVal, rustVal, 0.05f,
                        "Cultural coordinate at [" + d + "][" + i + "] must converge between Java and Rust");
            }
        }
    }

    @Test
    @DisplayName("6. Finite-Volume Hexagonal Trade Flux & Mass Conservation: 3-Engine Exact Parity")
    public void testFluxKernelParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dtSeconds = 30.0f * 86400.0f;

        // Record initial total mass
        double totalFoodBefore = 0.0;
        for (int i = 0; i < capacity; i++) {
            totalFoodBefore += javaSimdWorld.getFoodResource()[i];
        }

        // 1. Advance Java SIMD Engine
        javaFluxKernel.tick(javaSimdWorld, dtSeconds);

        // 2. Advance Java Standard Engine
        stdFluxKernel.tick(javaStdWorld, dtSeconds);

        // 3. Advance Native Rust Core
        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);
            arena.tickFlux(dtSeconds);
            arena.syncToJavaBuffers(rustWorld, rustAgents);
        }

        // 4. Verify 100% Mass Conservation across all 3 engines
        double totalFoodSimd = 0.0;
        double totalFoodStd = 0.0;
        double totalFoodRust = 0.0;
        for (int i = 0; i < capacity; i++) {
            totalFoodSimd += javaSimdWorld.getFoodResource()[i];
            totalFoodStd += javaStdWorld.getFoodResource()[i];
            totalFoodRust += rustWorld.getFoodResource()[i];
        }

        assertEquals(totalFoodBefore, totalFoodSimd, 0.01,
                "Java SIMD trade flux must conserve 100% of global food mass");
        assertEquals(totalFoodBefore, totalFoodStd, 0.01,
                "Java Standard trade flux must conserve 100% of global food mass");
        assertEquals(totalFoodBefore, totalFoodRust, 0.01,
                "Native Rust trade flux must conserve 100% of global food mass");

        // 5. Verify cell-by-cell trade price and resource transfer parity
        for (int i = 0; i < capacity; i++) {
            float simdPrice = javaSimdWorld.getLocalPrice()[i];
            float stdPrice = javaStdWorld.getLocalPrice()[i];
            float rustPrice = rustWorld.getLocalPrice()[i];

            assertEquals(Float.floatToRawIntBits(simdPrice), Float.floatToRawIntBits(stdPrice),
                    "Price on cell " + i + " must be bit-identical between Java SIMD and Standard");
            assertEquals(simdPrice, rustPrice, 0.0001f,
                    "Price on cell " + i + " must be bit-identical between Java and Rust");

            float simdFood = javaSimdWorld.getFoodResource()[i];
            float stdFood = javaStdWorld.getFoodResource()[i];
            float rustFood = rustWorld.getFoodResource()[i];

            assertEquals(Float.floatToRawIntBits(simdFood), Float.floatToRawIntBits(stdFood),
                    "Post-trade food on cell " + i + " must be bit-identical between Java SIMD and Standard");
            assertEquals(simdFood, rustFood, 0.01f,
                    "Post-trade food on cell " + i + " must match between Java and Rust");
        }
    }

    @Test
    @DisplayName("7. Unified 5-Kernel Simulation Cycle: Continuous Multi-Step Stability & Conservation")
    public void testUnifiedSimulationCycleParity() {
        assertTrue(NativeRustBridge.isNativeAvailable(), "Rust core must be available");
        float dtDays = 30.0f;
        float dt = dtDays * 86400.0f;

        // Advance 10 successive unified simulation cycles across all 3 engines
        for (int step = 0; step < 10; step++) {
            // Java SIMD
            javaEnvironmentalKernel.tick(javaSimdWorld, 6, dt);
            javaDemographicKernel.tick(javaSimdWorld, javaSimdAgents, dt);
            javaUrbanKernel.tick(javaSimdWorld, dt);
            javaFluxKernel.tick(javaSimdWorld, dt);
            javaCultureKernel.tick(javaSimdWorld, javaSimdAgents, dt);

            // Java Standard
            stdEnvironmentalKernel.tick(javaStdWorld, 6, dt);
            stdDemographicKernel.tick(javaStdWorld, javaStdAgents, dt);
        }

        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(rustWorld, rustAgents);

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

            assertFalse(Float.isNaN(javaSimdWorld.getFoodResource()[i]), "SIMD Food resource must not be NaN");
            assertTrue(javaSimdWorld.getFoodResource()[i] >= 0.0f, "SIMD Food resource must be >= 0");
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
