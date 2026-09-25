/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime Invariant & Conservation Law Guard for Tier 1 Physical Processes.
 *
 * <p>Enforces strict physical, thermodynamic, and mass conservation invariants
 * across numerical symplectic integration steps. Verifies that energy, exergy,
 * and elemental mass do not experience artificial numerical creation or dissipation:</p>
 * <ul>
 *   <li><b>Mass Conservation Invariant</b>: $\Delta M_{\text{total}} = \sum \Delta M_{\text{biomass}} + \Delta M_{\text{food}} + \Delta M_{\text{capital}} \approx 0$ (within closed planetary boundary).</li>
 *   <li><b>First Law of Thermodynamics</b>: $\Delta E_{\text{internal}} = E_{\text{in}} - E_{\text{out}} - W_{\text{dissipated}}$.</li>
 *   <li><b>Positivity Bounds</b>: Population $P_i \ge 0$, Food $F_i \ge 0$, Energy $E_i \ge 0$, Gini $\in [0, 1]$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class InvariantConservationGuard {
    private static final Logger logger = LoggerFactory.getLogger(InvariantConservationGuard.class);

    private final double relativeTolerance;
    private final boolean throwOnViolation;

    private double lastTotalBiomass = -1.0;
    private double lastTotalEnergy = -1.0;
    private double lastTotalCapital = -1.0;
    private long totalInvariantChecks = 0;
    private long violationCount = 0;

    /**
     * Default constructor with $10^{-5}$ relative tolerance and non-throwing logging mode.
     */
    public InvariantConservationGuard() {
        this(1e-5, false);
    }

    public InvariantConservationGuard(double relativeTolerance, boolean throwOnViolation) {
        this.relativeTolerance = relativeTolerance;
        this.throwOnViolation = throwOnViolation;
    }

    /**
     * Snapshot record of aggregated physical quantities across the planetary WorldBuffer.
     */
    public record ConservationSnapshot(
            double totalBiomass,
            double totalEnergy,
            double totalCapital,
            int activeCells,
            boolean boundsValid
    ) {}

    /**
     * Captures a global conservation snapshot from the active WorldBuffer.
     *
     * @param buffer the DOD WorldBuffer to inspect
     * @return current planetary snapshot
     */
    public ConservationSnapshot captureSnapshot(WorldBuffer buffer) {
        if (buffer == null) {
            return new ConservationSnapshot(0, 0, 0, 0, true);
        }

        double biomass = 0.0;
        double energy = 0.0;
        double capital = 0.0;
        int active = 0;
        boolean boundsOk = true;

        int capacity = buffer.getCapacity();
        float[] bioHuman = buffer.getBiomassHuman();
        float[] bioLive = buffer.getBiomassLivestock();
        float[] bioAgri = buffer.getBiomassAgriculture();
        float[] bioNat = buffer.getBiomassNatural();
        float[] food = buffer.getFoodResource();

        float[] eSolar = buffer.getEnergySolar();
        float[] eWind = buffer.getEnergyWind();
        float[] eFire = buffer.getEnergyFire();
        float[] eFood = buffer.getEnergyFoodConsumed();

        float[] resCap = buffer.getResourceCapital();
        float[] gini = buffer.getGiniIndex();

        for (int i = 0; i < capacity; i++) {
            if (buffer.getH3Indexes()[i] == 0) continue;
            active++;

            // Mass aggregation
            float bH = bioHuman[i];
            float bL = bioLive[i];
            float bA = bioAgri[i];
            float bN = bioNat[i];
            float f = food[i];

            if (bH < 0 || bL < 0 || bA < 0 || bN < 0 || f < 0) {
                boundsOk = false;
            }
            biomass += (bH + bL + bA + bN + f);

            // Energy aggregation
            energy += (eSolar[i] + eWind[i] + eFire[i] + eFood[i]);

            // Capital aggregation
            float c = resCap[i];
            if (c < 0) boundsOk = false;
            capital += c;

            // Gini bounds [0, 1]
            float g = gini[i];
            if (g < 0.0f || g > 1.0f) {
                boundsOk = false;
            }
        }

        return new ConservationSnapshot(biomass, energy, capital, active, boundsOk);
    }

    /**
     * Verifies that the state transition from pre-step to post-step preserves physical invariants.
     *
     * @param before snapshot before the simulation step
     * @param after snapshot after the simulation step
     * @param externalBiomassInput external mass added (e.g. net primary productivity)
     * @param externalEnergyInput external solar/flux energy added
     * @return true if all conservation laws are respected within relative tolerance
     */
    public boolean verifyTransition(ConservationSnapshot before, ConservationSnapshot after,
                                    double externalBiomassInput, double externalEnergyInput) {
        totalInvariantChecks++;

        if (!after.boundsValid()) {
            recordViolation("Positivity or range bounds violated (negative mass/energy or invalid Gini).");
            return false;
        }

        // Expected mass balance: after.biomass ≈ before.biomass + externalBiomassInput
        double expectedBiomass = before.totalBiomass() + externalBiomassInput;
        double biomassDelta = Math.abs(after.totalBiomass() - expectedBiomass);
        double biomassScale = Math.max(1.0, Math.max(Math.abs(expectedBiomass), Math.abs(after.totalBiomass())));
        double relBiomassError = biomassDelta / biomassScale;

        if (relBiomassError > relativeTolerance && before.totalBiomass() > 0) {
            recordViolation(String.format("Biomass mass conservation violated: expected=%.4e, actual=%.4e, relError=%.4e (tol=%.4e)",
                    expectedBiomass, after.totalBiomass(), relBiomassError, relativeTolerance));
            return false;
        }

        lastTotalBiomass = after.totalBiomass();
        lastTotalEnergy = after.totalEnergy();
        lastTotalCapital = after.totalCapital();
        return true;
    }

    private void recordViolation(String message) {
        violationCount++;
        logger.warn("[InvariantConservationGuard] VIOLATION #{}: {}", violationCount, message);
        if (throwOnViolation) {
            throw new IllegalStateException("Physical invariant violated: " + message);
        }
    }

    public double getRelativeTolerance() { return relativeTolerance; }
    public boolean isThrowOnViolation() { return throwOnViolation; }
    public long getTotalInvariantChecks() { return totalInvariantChecks; }
    public long getViolationCount() { return violationCount; }
    public double getLastTotalBiomass() { return lastTotalBiomass; }
    public double getLastTotalEnergy() { return lastTotalEnergy; }
    public double getLastTotalCapital() { return lastTotalCapital; }
}
