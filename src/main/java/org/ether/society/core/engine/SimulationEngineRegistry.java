/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;


import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central thread-safe registry and execution dispatcher for all {@link SimulationEngineModule} instances.
 *
 * <p>Completely decouples scenario execution and engine lifecycle management from hardcoded
 * conditional chains. Enables seamless ablation studies and uniform resetting across runs.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SimulationEngineRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SimulationEngineRegistry.class);
    private static final SimulationEngineRegistry INSTANCE = new SimulationEngineRegistry();

    private final Map<String, SimulationEngineModule> enginesById = new LinkedHashMap<>();
    private final List<SimulationEngineModule> allEngines = new ArrayList<>();
    private final List<SimulationEngineModule> tier1Engines = new ArrayList<>();
    private final List<SimulationEngineModule> tier2Engines = new ArrayList<>();

    public static SimulationEngineRegistry getInstance() {
        return INSTANCE;
    }

    private SimulationEngineRegistry() {
        registerBuiltInEngines();
    }

    /**
     * Registers a new simulation engine module.
     *
     * @param module engine module instance
     */
    public synchronized void register(SimulationEngineModule module) {
        if (module == null) return;
        enginesById.put(module.id(), module);
        allEngines.removeIf(e -> e.id().equals(module.id()));
        allEngines.add(module);

        if (module.tier() == EngineTier.TIER_1_BIOPHYSICAL) {
            tier1Engines.removeIf(e -> e.id().equals(module.id()));
            tier1Engines.add(module);
        } else {
            tier2Engines.removeIf(e -> e.id().equals(module.id()));
            tier2Engines.add(module);
        }
    }

    /**
     * Retrieves an engine module by its unique identifier, dynamically resolving and adapting
     * any existing class if not yet formally registered.
     *
     * @param id engine ID
     * @return module instance, or {@code null}
     */
    public synchronized SimulationEngineModule getEngine(String id) {
        if (id == null) return null;
        SimulationEngineModule engine = enginesById.get(id);
        if (engine != null) return engine;

        // Dynamic automatic resolution: if an engine is declared or requested by a scenario,
        // resolve it from known packages and adapt it seamlessly!
        engine = attemptDynamicEngineResolution(id);
        if (engine != null) {
            register(engine);
            return engine;
        }
        return null;
    }

    /**
     * Returns an unmodifiable list of all registered engines.
     *
     * @return list of all engines
     */
    public synchronized List<SimulationEngineModule> getAllEngines() {
        return Collections.unmodifiableList(new ArrayList<>(allEngines));
    }

    /**
     * Returns an unmodifiable list of registered Tier 1 biophysical engines.
     *
     * @return list of Tier 1 engines
     */
    public synchronized List<SimulationEngineModule> getTier1Engines() {
        return Collections.unmodifiableList(new ArrayList<>(tier1Engines));
    }

    /**
     * Returns an unmodifiable list of registered Tier 2 cliodynamic engines.
     *
     * @return list of Tier 2 engines
     */
    public synchronized List<SimulationEngineModule> getTier2Engines() {
        return Collections.unmodifiableList(new ArrayList<>(tier2Engines));
    }

    /**
     * Resolves all active Tier 2 engines enabled for the given scenario.
     *
     * @param scenario active scenario
     * @return list of active Tier 2 engines
     */
    public synchronized List<SimulationEngineModule> getActiveTier2Engines(Scenario scenario) {
        if (scenario != null && scenario.getTypeBEngineStates() != null) {
            for (Map.Entry<String, Boolean> entry : scenario.getTypeBEngineStates().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    getEngine(entry.getKey());
                }
            }
        }
        List<SimulationEngineModule> active = new ArrayList<>();
        for (SimulationEngineModule e : tier2Engines) {
            if (e.isEnabled(scenario)) {
                active.add(e);
            }
        }
        return active;
    }

    /**
     * Attempts to dynamically discover, instantiate, or adapt any engine class by ID.
     */
    private SimulationEngineModule attemptDynamicEngineResolution(String id) {
        String[] searchPackages = {
                "org.ether.society.engines.tier2.theories.",
                "org.ether.society.engines.tier2.historical.",
                "org.ether.society.engines.tier1.",
                "org.ether.society.engines.",
                "org.ether.society.core.engine.",
                "org.ether.society.core."
        };

        for (String pkg : searchPackages) {
            try {
                Class<?> clazz = Class.forName(pkg + id);
                if (SimulationEngineModule.class.isAssignableFrom(clazz)) {
                    return (SimulationEngineModule) clazz.getDeclaredConstructor().newInstance();
                }
            } catch (ClassNotFoundException ignored) {
                // Continue searching candidate packages
            } catch (Throwable t) {
                logger.warn("Could not dynamically instantiate engine '{}': {}", id, t.getMessage());
            }
        }

        // Return a named virtual engine fallback for descriptive scenario metadata tags
        return new AbstractSimulationEngine(id, id, EngineTier.TIER_2_CLIODYNAMIC) {
            @Override
            public void tick(org.ether.society.core.dod.WorldBuffer buffer, double dtYears, double dtMonthly) {
                // Descriptive or composite scenario meta-tag pass
            }
        };
    }

    /**
     * Resolves all active engines (Tier 1 + Tier 2) enabled for the given scenario.
     *
     * @param scenario active scenario
     * @return list of active engines
     */
    public List<SimulationEngineModule> getActiveEngines(Scenario scenario) {
        List<SimulationEngineModule> active = new ArrayList<>();
        for (SimulationEngineModule e : allEngines) {
            if (e.isEnabled(scenario)) {
                active.add(e);
            }
        }
        return active;
    }

    /**
     * Resets all registered engines to ensure zero cross-scenario state leakage.
     */
    public void resetAll() {
        for (SimulationEngineModule e : allEngines) {
            try {
                e.reset();
            } catch (Throwable t) {
                logger.warn("Error resetting engine {}: {}", e.id(), t.getMessage());
            }
        }
    }

    /**
     * Registers all standard built-in Tier 1 and Tier 2 simulation engines.
     */
    private void registerBuiltInEngines() {
        // --- Core Tier 2 Cliodynamic Theories (Data-Oriented Implementations) ---
        register(new KummelAyresExergyEngine());
        register(new PomeranzGreatDivergenceEngine());
        register(new TurchinGoldstoneSDTEngine());
        register(new PublicSanitationWaterGridEngine());
        register(new LenskiEcologicalEvolutionaryEngine());
        register(new StructuralDemographicBifurcationEngine());
        register(new PhysicalCapitalDiffusionEngine());

        // --- Unified Tier 2 Cliodynamic Theories & Historical Models ---
        register(new SmilMaterialTransitionsPureEngine());
        register(new JevonsParadoxEngine());
        register(new TasmanianCulturalRegressionEngine());
        register(new CoGovernanceTradeEngine());
        register(new KurzweilAcceleratingReturnsEngine());
        register(new World3CouplingEngine());
        register(new RomanImperialCliodynamicEngine());
        register(new EdoJapanIsolationEngine());
        register(new BoserupAgriculturalIntensificationEngine());
        register(new SpatialMetapopulationSEIREngine());

        // --- Discover and register all SPI SimulationEngineModule instances automatically ---
        try {
            ServiceLoader<SimulationEngineModule> loader = ServiceLoader.load(SimulationEngineModule.class);
            var it = loader.iterator();
            while (it.hasNext()) {
                try {
                    SimulationEngineModule module = it.next();
                    register(module);
                } catch (Throwable t) {
                    logger.warn("Skipping unresolvable SPI module: {}", t.getMessage());
                }
            }
        } catch (Throwable t) {
            logger.warn("Could not discover SPI engines into SimulationEngineRegistry: {}", t.getMessage());
        }
    }
}
