/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

/**
 * Categorical classification of planetary simulation engines.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum EngineTier {
    /**
     * Tier 1: Invariant biophysical, chemical, and thermodynamic conservation laws.
     * Always enabled by default, but toggleable for scientific falsification and ablation studies.
     */
    TIER_1_BIOPHYSICAL,

    /**
     * Tier 2: Pluggable historical, cliodynamic, macroeconomic, and institutional theories.
     * Selectable per scenario via UI toggles and preset matrices.
     */
    TIER_2_CLIODYNAMIC
}
