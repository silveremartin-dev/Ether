/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.tier2.theories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>Simulation Engine State Coordinator</h1>
 *
 * <p>Centralized coordinator responsible for deterministic lifecycle synchronization and absolute
 * state isolation across all Tier-1 biophysical and Tier-2 cliodynamic simulation engines.</p>
 *
 * <p>Prevents state leakage between successive scenarios, batch runs, or test fixtures by resetting
 * all shared engine accumulator fields to canonical baseline initial conditions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class SimulationEngineStateCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(SimulationEngineStateCoordinator.class);

    private SimulationEngineStateCoordinator() {
        // Pure static utility coordinator
    }

    /**
     * Resets all Tier-1 and Tier-2 engine simulation states, accumulators, and caches.
     * Guarantees 100% bit-identical trajectory determinism across scenario runs.
     */
    public static void resetAllEngineStates() {
        logger.debug("Resetting all Tier-1 and Tier-2 simulation engine states for scenario isolation");

        // --- Tier 1: Biophysical & Planetary Systems ---
        EntropicMetalDissipationEngine.resetState();
        GeoengineeringAlbedoFeedbackEngine.resetState();
        InfrastructureInertiaEngine.resetState();
        NuclearSafetyRadiotoxicityEngine.resetState();
        NuclearWarfareClimateEngine.resetState();
        OreGradeThermodynamicsEngine.resetState();
        PhysicalLawEngine.resetState();
        PhysicalSupplyChainEngine.resetState();
        ThermohalineStommelAMOCEngine.resetState();
        TrophicEcosystemEngine.resetState();
        UnifiedNeutralBiodiversityEngine.resetState();

        // --- Tier 2: Historical & Regional Ruptures ---
        AmerindianEcosystemEngine.resetState();
        CrosbyColumbianExchangeEngine.resetState();
        EdoJapanIsolationEngine.resetState();
        MonasticDemographicBufferEngine.resetState();
        ProtestantWorkEthicEngine.resetState();
        RomanImperialCliodynamicEngine.resetState();
        SmilMaterialTransitionsPureEngine.resetState();
        TasmanianCulturalRegressionEngine.resetState();

        // --- Tier 2: Cliodynamic & Sociological Theories ---
        BifurcationChaosEngine.resetState();
        CoGovernanceTradeEngine.resetState();
        CulturalSociologyEngine.resetState();
        JevonsParadoxEngine.resetState();
        KurzweilAcceleratingReturnsEngine.resetState();
        LanguageLinguisticEngine.resetState();
        TerraformingEngine.resetState();
        TradeNetworkRouter.resetState();
        World3CouplingEngine.resetState();
    }
}
