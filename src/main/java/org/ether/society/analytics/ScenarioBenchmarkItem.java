/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

/**
 * Domain benchmark item abstraction for cliodynamic scenario comparative audits.
 *
 * <p>Decouples analytical statistical engines from graphical UI presentation wrappers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public interface ScenarioBenchmarkItem {

    /**
     * @return Human-readable scenario name or descriptor.
     */
    String getName();

    /**
     * @return Unique run identifier.
     */
    String getRunId();
}
