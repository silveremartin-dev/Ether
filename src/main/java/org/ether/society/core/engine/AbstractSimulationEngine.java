/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

import org.ether.society.model.Scenario;

/**
 * Skeletal base implementation for {@link SimulationEngineModule} reducing boilerplate across
 * Tier 1 and Tier 2 simulation engines.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public abstract class AbstractSimulationEngine implements SimulationEngineModule {

    private final String id;
    private final String name;
    private final EngineTier tier;

    protected AbstractSimulationEngine(String id, String name, EngineTier tier) {
        this.id = id;
        this.name = name;
        this.tier = tier;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public EngineTier tier() {
        return tier;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        if (scenario == null) {
            // Default: Tier 1 always enabled, Tier 2 disabled without explicit scenario
            return tier == EngineTier.TIER_1_BIOPHYSICAL;
        }

        if (scenario.getTypeBEngineStates() != null && scenario.getTypeBEngineStates().containsKey(id)) {
            return Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id));
        }

        // Tier 1 defaults to enabled unless explicitly disabled for ablation studies
        return tier == EngineTier.TIER_1_BIOPHYSICAL;
    }

    @Override
    public void reset() {
        // Default no-op for stateless engines
    }
}
