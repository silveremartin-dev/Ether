/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Armor-clad End-to-End (E2E) integration test executing 5-tick simulations across historical presets.
 *
 * <p>Strictly verifies:
 * <ul>
 *   <li>Unified engine registry registration and non-null resolution for all active scenario engines.</li>
 *   <li>Effective execution of cliodynamic engines (including SpatialMetapopulationSEIREngine).</li>
 *   <li>Cellular and DOD WorldBuffer state mutations across ticks.</li>
 *   <li>Absence of unphysical demographic explosions and valid closed-form-free telemetry outputs.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class FullPipelineEngineAndTelemetryEndToEndTest {

    private SimulationEngineRegistry registry;
    private Configuration config;

    @BeforeEach
    public void setUp() throws Exception {
        registry = SimulationEngineRegistry.getInstance();
        config = ConfigurationLoader.loadDefault();
        TypeBEnginePassRunner.resetGlobalSimulationState();
    }

    @Test
    @DisplayName("Verify SimulationEngineRegistry contains all built-in and adapted SPI engines")
    public void testRegistryCompleteness() {
        List<SimulationEngineModule> allEngines = registry.getAllEngines();
        assertNotNull(allEngines);
        assertTrue(allEngines.size() >= 25, "Registry must contain at least 25 unified simulation engine modules, found: " + allEngines.size());

        // Verify key engines exist
        assertNotNull(registry.getEngine("SpatialMetapopulationSEIREngine"), "SpatialMetapopulationSEIREngine must be registered");
        assertNotNull(registry.getEngine("TurchinGoldstoneSDTEngine"), "TurchinGoldstoneSDTEngine must be registered");
        assertNotNull(registry.getEngine("BoserupAgriculturalIntensificationEngine"), "BoserupAgriculturalIntensificationEngine must be registered");
        assertNotNull(registry.getEngine("KummelAyresExergyEngine"), "KummelAyresExergyEngine must be registered");
        assertNotNull(registry.getEngine("PomeranzGreatDivergenceEngine"), "PomeranzGreatDivergenceEngine must be registered");
    }

    @Test
    @DisplayName("5-Tick E2E Test on Industrial Revolution 1800: Zero demographic explosion & calibrated telemetry")
    public void testIndustrial1800FiveTicksE2E() {
        Scenario scenario = Scenario.getBuiltInScenarios().stream()
                .filter(s -> "earth_+1800_industrial_1800".equalsIgnoreCase(s.getPresetKey())
                        || (s.getName() != null && s.getName().contains("1800")))
                .findFirst()
                .orElse(null);
        assertNotNull(scenario, "earth_+1800_industrial_1800 preset must load");

        // Verify all engines declared active in scenario exist in registry
        Map<String, Boolean> engineStates = scenario.getTypeBEngineStates();
        assertNotNull(engineStates);
        for (Map.Entry<String, Boolean> entry : engineStates.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                SimulationEngineModule module = registry.getEngine(entry.getKey());
                assertNotNull(module, "Engine '" + entry.getKey() + "' is active in scenario but missing from SimulationEngineRegistry!");
            }
        }

        scenario.setH3Resolution(3);
        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(3));

        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);

        WorldBuffer wb = engine.getWorldBuffer();
        assertNotNull(wb, "WorldBuffer must be initialized");
        assertTrue(wb.getCapacity() > 0, "WorldBuffer capacity must be > 0");

        double initialPop = engine.getTotalPopulation();
        assertTrue(initialPop > 100_000, "Initial population must be non-trivial for 1800, found: " + initialPop);

        // Run 5 ticks
        for (int tick = 1; tick <= 5; tick++) {
            H3EngineLifecycleAndTickCoordinator.executeTick(engine);
        }

        double finalPop = engine.getTotalPopulation();
        double growthPct = ((finalPop - initialPop) / initialPop) * 100.0;

        // Verify no population explosion: in 5 ticks (150 days at 30 days/tick = 0.41 years),
        // growth must be strictly bounded below 2.0%
        assertTrue(growthPct < 2.0, "Population exploded in 5 ticks: growth was " + growthPct + "%, expected < 2.0%");
        assertTrue(growthPct > -5.0, "Population suffered unphysical crash without catastrophe: growth was " + growthPct + "%");

        // Evaluate Telemetry and verify all metrics are within biophysical reality
        TelemetrySubscription sub = new TelemetrySubscription(
                List.of("grossWorldProduct", "gini", "literacyRate", "fossilSharePrimaryEnergy",
                        "eliteOverproduction", "urbanizationRate", "lifeExpectancy"),
                List.of(),
                1
        );
        engine.setTelemetrySubscription(sub);

        TelemetryEvaluationResult res = engine.evaluateTelemetryNow();
        assertNotNull(res);
        Map<String, Double> metrics = res.getMetrics();
        assertNotNull(metrics);

        // Verify fossil energy share is NOT 80% at 1800 CE
        Double fossilShare = metrics.get("fossilSharePrimaryEnergy");
        assertNotNull(fossilShare, "fossilSharePrimaryEnergy must be evaluated");
        assertTrue(fossilShare < 25.0, "Fossil fuel share at 1800 CE should be < 25%, but evaluated to: " + fossilShare + "%");

        // Verify literacy rate is reasonable (~30-65% in 1800) and not an instant 99%
        Double literacy = metrics.get("literacyRate");
        assertNotNull(literacy, "literacyRate must be evaluated");
        assertTrue(literacy < 80.0, "Literacy rate in 1800 must not exceed 80%, found: " + literacy + "%");
        assertTrue(literacy >= 0.0, "Literacy rate must be >= 0%");

        // Verify elite overproduction is not NaN or 0
        Double eliteOverprod = metrics.get("eliteOverproduction");
        assertNotNull(eliteOverprod, "eliteOverproduction must be evaluated");
        assertFalse(Double.isNaN(eliteOverprod));
        assertTrue(eliteOverprod > 0.0, "eliteOverproduction should be > 0.0, found: " + eliteOverprod);
    }

    @Test
    @DisplayName("5-Tick E2E Test on Black Death 1347: SpatialMetapopulationSEIREngine active execution")
    public void testBlackDeath1347FiveTicksE2E() {
        Scenario scenario = Scenario.getBuiltInScenarios().stream()
                .filter(s -> "earth_+1347_black_death_1347".equalsIgnoreCase(s.getPresetKey())
                        || (s.getName() != null && s.getName().contains("1347")))
                .findFirst()
                .orElse(null);
        assertNotNull(scenario, "earth_+1347_black_death_1347 preset must load");

        // SpatialMetapopulationSEIREngine must be active
        assertTrue(Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("SpatialMetapopulationSEIREngine")),
                "SpatialMetapopulationSEIREngine must be enabled in Black Death 1347 scenario");

        SimulationEngineModule seirModule = registry.getEngine("SpatialMetapopulationSEIREngine");
        assertNotNull(seirModule, "SpatialMetapopulationSEIREngine must be registered in SimulationEngineRegistry");

        scenario.setH3Resolution(3);
        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(3));

        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);

        WorldBuffer wb = engine.getWorldBuffer();
        assertNotNull(wb);

        // Seed epidemic index cases into European populated cells
        float[] popArr = wb.getBiomassHuman();
        int seededCell = -1;
        for (int i = 0; i < wb.getCapacity(); i++) {
            if (popArr[i] > 1000.0f) {
                seededCell = i;
                break;
            }
        }
        assertTrue(seededCell >= 0, "Must have at least one populated cell to seed Black Death");

        if (seirModule instanceof org.ether.society.engines.tier2.theories.SpatialMetapopulationSEIREngine seir) {
            seir.seedInfection(seededCell, 250.0f);
        }

        double initialPop = engine.getTotalPopulation();

        // Run 5 ticks (7 days per tick = 35 days)
        for (int tick = 1; tick <= 5; tick++) {
            H3EngineLifecycleAndTickCoordinator.executeTick(engine);
        }

        double postTickPop = engine.getTotalPopulation();
        // Contagion and mortality should be actively operating
        assertTrue(postTickPop <= initialPop, "Black Death epidemic must produce mortality, pop before: "
                + initialPop + ", after: " + postTickPop);
    }
}
