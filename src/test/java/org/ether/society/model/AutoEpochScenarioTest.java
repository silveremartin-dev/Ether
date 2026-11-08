/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.ui.scenario.ScenarioEngineCatalog;
import org.ether.society.ui.scenario.ScenarioEngineCatalog.EngineMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Automatic Epoch Scenario Generator and Tier 2 Type B Engine Domain Validity Catalog.
 * Verifies epistemic validity, demographic scalings, strict 1-to-1 bijection between UI checkboxes
 * and executable Java classes, and default domain activation across the -3,000,000 BP to post-2025 CE horizon.
 */
class AutoEpochScenarioTest {

    @Test
    @DisplayName("Generate Deep-Time Plio-Pleistocene & Paleolithic Scenarios (-3,000,000 BP and -100,000 BP)")
    void testPaleolithicScenario() {
        Scenario plioPleistocene = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", -3000000L);
        assertNotNull(plioPleistocene);
        assertEquals(-3000000L, plioPleistocene.getStartDateYear());
        Map<String, Boolean> plioEngines = plioPleistocene.getTypeBEngineStates();
        assertTrue(plioEngines.get("KinSelectionHamiltonEngine"));
        assertTrue(plioEngines.get("SelfDomesticationEngine"));
        assertTrue(plioEngines.get("SexualSelectionMatingEngine"));
        assertTrue(plioEngines.get("PriceMultilevelSelectionEngine"));
        assertFalse(plioEngines.get("LanguageLinguisticEngine"));
        assertFalse(plioEngines.get("BoserupAgriculturalIntensificationEngine"));

        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", -100000L);
        assertNotNull(s);
        assertEquals(-100000L, s.getStartDateYear());
        assertTrue(s.getEndDateYear() > s.getStartDateYear());
        assertEquals(50_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() < 10.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertNotNull(engines);
        assertTrue(engines.get("KinSelectionHamiltonEngine"));
        assertTrue(engines.get("LanguageLinguisticEngine"));
        assertTrue(engines.get("CulturalSociologyEngine"));
        assertFalse(engines.get("FrontierAsabiyyahEngine"));
        assertFalse(engines.get("WienerCyberneticHomeostasisEngine"));
    }

    @Test
    @DisplayName("Generate LGM Earth Scenario (-20,000 BP)")
    void testLGMScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", -20000L);
        assertNotNull(s);
        assertEquals(-20000L, s.getStartDateYear());
        assertEquals(500_000L, s.getInitialHumanCount());

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertTrue(engines.get("LanguageLinguisticEngine"));
        assertTrue(engines.get("DynamicMaritimeRoutingGraph"));
        assertFalse(engines.get("BoserupAgriculturalIntensificationEngine"));
        assertFalse(engines.get("TurchinGoldstoneSDTEngine"));
    }

    @Test
    @DisplayName("Generate Classical Antiquity Scenario (0 AD)")
    void testAntiquityScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 0L);
        assertNotNull(s);
        assertEquals(0L, s.getStartDateYear());
        assertEquals(200_000_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() >= 100.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertTrue(engines.get("FrontierAsabiyyahEngine"));
        assertTrue(engines.get("HydrologicalEngineeringEngine"));
        assertTrue(engines.get("TurchinGoldstoneSDTEngine"));
        assertFalse(engines.get("PhysicalMediaPrintingEngine"));
        assertFalse(engines.get("SovereignTreasuryBondEngine"));
        assertFalse(engines.get("World3CouplingEngine"));
        assertFalse(engines.get("WienerCyberneticHomeostasisEngine"));
        assertFalse(engines.get("TasmanianCulturalRegressionEngine"));
        assertFalse(engines.get("EdoJapanIsolationEngine"));
    }

    @Test
    @DisplayName("Generate Industrial Revolution Scenario (1800 AD)")
    void testIndustrialScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1800L);
        assertNotNull(s);
        assertEquals(1800L, s.getStartDateYear());
        assertEquals(1_000_000_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() >= 1000.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertTrue(engines.get("PhysicalMediaPrintingEngine"));
        assertTrue(engines.get("SovereignTreasuryBondEngine"));
        assertTrue(engines.get("PomeranzGreatDivergenceEngine"));
        assertTrue(engines.get("RicardoDifferentialLandRentEngine"));
        assertTrue(engines.get("KondratievSchumpeterLongWaveEngine"));
        assertTrue(engines.get("KummelAyresExergyEngine"));
        assertFalse(engines.get("PublicSanitationWaterGridEngine"));
        assertFalse(engines.get("World3CouplingEngine"));
        assertFalse(engines.get("TasmanianCulturalRegressionEngine"));
    }

    @Test
    @DisplayName("Verify Type B Engine Domain Validity Conditions Across All 6 Epoch & Geopolitical Categories")
    void testTypeBEngineDomainValidityConditionsAcrossEpochs() {
        Map<String, Boolean> plio = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", -3000000L);
        Map<String, Boolean> paleo = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", -20000L);
        Map<String, Boolean> medieval = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", 1200L);
        Map<String, Boolean> earlyModern = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", 1680L);
        Map<String, Boolean> post1950 = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", 1960L);
        Map<String, Boolean> mars = AutoEpochScenarioHelper.calibrateTypeBEngines("mars", 2026L);

        // Category I: Deep-Time Biocultural, Evolutionary & Forager-Mobility Engines (-3,000,000 BP onward)
        assertTrue(plio.get("KinSelectionHamiltonEngine"));
        assertTrue(plio.get("SelfDomesticationEngine"));
        assertFalse(plio.get("LanguageLinguisticEngine"));
        assertTrue(paleo.get("LanguageLinguisticEngine"));
        assertTrue(paleo.get("DynamicMaritimeRoutingGraph"));
        assertFalse(mars.get("KinSelectionHamiltonEngine"));

        // Category II: Agrarian & classical state engines active from Neolithic/Bronze Age onward on Earth
        assertFalse(plio.get("TurchinGoldstoneSDTEngine"));
        assertFalse(paleo.get("TurchinGoldstoneSDTEngine"));
        assertTrue(medieval.get("TurchinGoldstoneSDTEngine"));
        assertTrue(earlyModern.get("TurchinGoldstoneSDTEngine"));
        assertTrue(post1950.get("TurchinGoldstoneSDTEngine"));
        assertFalse(mars.get("BarfieldNomadicSteppeEmpireEngine"));

        // Category III: Early Modern & Mercantile engines active from 1450/1650 CE onward on Earth
        assertFalse(paleo.get("PhysicalMediaPrintingEngine"));
        assertFalse(medieval.get("PhysicalMediaPrintingEngine"));
        assertFalse(medieval.get("SovereignTreasuryBondEngine"));
        assertTrue(earlyModern.get("PhysicalMediaPrintingEngine"));
        assertTrue(earlyModern.get("SovereignTreasuryBondEngine"));
        assertTrue(earlyModern.get("CrosbyColumbianExchangeEngine"));
        assertFalse(mars.get("CrosbyColumbianExchangeEngine"));

        // Category IV & V: Industrial & Great Acceleration engines active only in industrial/post-1900/post-1950 regimes
        assertFalse(medieval.get("PomeranzGreatDivergenceEngine"));
        assertFalse(earlyModern.get("PomeranzGreatDivergenceEngine"));
        assertFalse(medieval.get("PublicSanitationWaterGridEngine"));
        assertFalse(earlyModern.get("PublicSanitationWaterGridEngine"));
        assertFalse(medieval.get("World3CouplingEngine"));
        assertFalse(earlyModern.get("World3CouplingEngine"));
        assertTrue(post1950.get("PomeranzGreatDivergenceEngine"));
        assertTrue(post1950.get("LewisDualSectorTurningPointEngine"));
        assertTrue(post1950.get("PublicSanitationWaterGridEngine"));
        assertTrue(post1950.get("World3CouplingEngine"));
        assertTrue(post1950.get("NordhausDiceHybridEngine"));
        assertTrue(mars.get("IsruAutarkyAndSpaceColonizationEngine"));
        assertTrue(mars.get("TerraformingEngine"));

        // Category VI: Specialized regional, counterfactual & pure-theory sandbox engines ALWAYS unchecked by default
        for (Map<String, Boolean> cfg : List.of(plio, paleo, medieval, earlyModern, post1950, mars)) {
            assertFalse(cfg.get("TasmanianCulturalRegressionEngine"));
            assertFalse(cfg.get("AmerindianEcosystemEngine"));
            assertFalse(cfg.get("EdoJapanIsolationEngine"));
            assertFalse(cfg.get("RomanImperialCliodynamicEngine"));
            assertFalse(cfg.get("World3PureEngine"));
            assertFalse(cfg.get("HandyNasaPureEngine"));
            assertFalse(cfg.get("NuclearWarfareClimateEngine"));
            assertFalse(cfg.get("GeoengineeringAlbedoFeedbackEngine"));
            assertFalse(cfg.get("BifurcationChaosEngine"));
        }
    }

    @Test
    @DisplayName("Verify Strict 1-to-1 Bijection Between UI Checkboxes, AutoEpochScenarioHelper, and Java Engine Classes")
    void testStrictBijectionBetweenCatalogCheckboxesAndExecutableJavaClasses() {
        List<EngineMeta> uiCatalog = ScenarioEngineCatalog.getOptionalEngines();
        Map<String, Boolean> calibrated = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", 1800L);

        assertEquals(86, uiCatalog.size(), "UI Optional Engines Catalog must contain the exact 86 verified Type B engines");
        assertEquals(86, calibrated.size(), "AutoEpochScenarioHelper must calibrate the exact 86 verified Type B engines");

        Set<String> uiIds = uiCatalog.stream().map(EngineMeta::id).collect(Collectors.toSet());
        assertEquals(86, uiIds.size(), "UI Optional Engines Catalog must not contain duplicate engine IDs");
        assertEquals(uiIds, calibrated.keySet(), "UI Optional Engines Catalog IDs and AutoEpochScenarioHelper keys must match 1-to-1");

        // Verify deleted/phantom engines are completely absent
        List<String> deletedEngines = List.of(
                "PsychohistoryPureEngine",
                "KardashevPureEngine",
                "TechnologicalSingularityEngine",
                "AiAutonomousRegulationPureEngine",
                "SovereignAIGovernanceEngine",
                "AutoRegulationPureEngine",
                "FertileCrescentSalinizationEngine",
                "OldowanMarrowPercussionEngine",
                "MammothBoneHabitationEngine"
        );
        for (String deleted : deletedEngines) {
            assertFalse(uiIds.contains(deleted), "Deleted/phantom engine must not appear in UI catalog: " + deleted);
            assertFalse(calibrated.containsKey(deleted), "Deleted/phantom engine must not appear in AutoEpochScenarioHelper: " + deleted);
        }

        // Verify every single engine ID resolves to an actual compiled Java class in engines.tier2 or engines.tier1
        List<String> candidatePackages = List.of(
                "org.ether.society.engines.tier2.theories.",
                "org.ether.society.engines.tier2.historical.",
                "org.ether.society.engines.tier1."
        );
        for (String engineId : uiIds) {
            Class<?> resolvedClass = null;
            for (String pkg : candidatePackages) {
                try {
                    resolvedClass = Class.forName(pkg + engineId);
                    break;
                } catch (ClassNotFoundException ignored) {
                    // Try next package
                }
            }
            assertNotNull(resolvedClass, "Engine ID '" + engineId + "' in catalog does not correspond to any existing Java class!");
        }
    }

    @Test
    @DisplayName("Generate Mars Outpost Scenario")
    void testMarsScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("mars", 2026L);
        assertNotNull(s);
        assertEquals(2026L, s.getStartDateYear());
        assertNotNull(s.getPlanetPreset());
        assertTrue(s.getName().toLowerCase().contains("mars"));
    }

    @Test
    @DisplayName("Verify DataFallbackStrategy options in scenario generation")
    void testFallbackStrategies() {
        Scenario sInterp = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1450L, false,
                org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
        assertEquals(org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION,
                sInterp.getDataFallbackStrategy());

        Scenario sSnap = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1450L, false,
                org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.NEAREST_NEIGHBOR);
        assertEquals(org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.NEAREST_NEIGHBOR,
                sSnap.getDataFallbackStrategy());
    }
}
