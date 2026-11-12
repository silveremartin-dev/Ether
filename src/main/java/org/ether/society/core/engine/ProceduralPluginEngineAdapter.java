/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;

import java.util.List;
import java.util.Objects;

/**
 * Adapter encapsulating any legacy or custom {@link ProceduralEnginePlugin} into the
 * unified {@link SimulationEngineModule} contract.
 *
 * <p>Bridges the legacy object-oriented cell interface with the modern high-performance
 * Data-Oriented Design (DoD) pipeline, ensuring zero orphan engines.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ProceduralPluginEngineAdapter extends AbstractSimulationEngine {

    private final ProceduralEnginePlugin delegate;

    public ProceduralPluginEngineAdapter(ProceduralEnginePlugin delegate) {
        super(
                Objects.requireNonNull(delegate, "delegate plugin must not be null").getClass().getSimpleName(),
                delegate.getName(),
                delegate.getCategory() != null && delegate.getCategory().contains("Tier 1")
                        ? EngineTier.TIER_1_BIOPHYSICAL
                        : EngineTier.TIER_2_CLIODYNAMIC
        );
        this.delegate = delegate;
    }

    /**
     * Retrieves the underlying adapted procedural plugin.
     *
     * @return delegate plugin instance
     */
    public ProceduralEnginePlugin getDelegate() {
        return delegate;
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (delegate != null && buffer != null) {
            delegate.processBuffer(buffer, dtYears);
        }
    }

    /**
     * Executes legacy cell-list pass if supported by the underlying delegate.
     *
     * @param cells     list of active simulation cells
     * @param deltaYears time step duration in years
     */
    public void processCells(List<H3Cell> cells, double deltaYears) {
        if (delegate != null && cells != null && !cells.isEmpty()) {
            delegate.process(cells, deltaYears);
        }
    }
}
