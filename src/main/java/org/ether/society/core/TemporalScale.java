/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

/**
 * <h1>TemporalScale</h1>
 * <p>
 * Temporal stepping scale mode for simulation ticks (daily fine-scale vs. monthly aggregated).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum TemporalScale {
    DAILY(1, "📅 Pas Quotidien (Jour par Jour - Détaillé)"),
    MONTHLY(30, "🚀 Pas Mensuel (Mois par Mois - Mode Rapide)");

    private final int factor;
    private final String label;

    TemporalScale(int factor, String label) {
        this.factor = factor;
        this.label = label;
    }

    public int getFactor() { return factor; }
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
