/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Physical Laws & Constants Customization Engine.
 * Allows altering fundamental physical parameters (Photosynthetic efficiency, room-temp superconductivity,
 * entropy generation rate) to simulate alternate physical realities.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicalLawEngine extends AbstractSimulationEngine {
    public PhysicalLawEngine() {
        super("PhysicalLawEngine", "PhysicalLawEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(PhysicalLawEngine.class);

    private static double photosyntheticEfficiencyMultiplier = 1.0; // 1.0 = standard, 2.0 = double biomass yield
    private static boolean roomTemperatureSuperconductivity = false; // Zero Joule heat loss (I^2 * R = 0)
    private static double entropyGenerationScale = 1.0; // Scale factor for waste heat generation

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        if (photosyntheticEfficiencyMultiplier == 1.0 && !roomTemperatureSuperconductivity && entropyGenerationScale == 1.0) {
            return;
        }
        double dtSafe = Math.max(0.0001, dtYears);
        if (photosyntheticEfficiencyMultiplier != 1.0) {
            double naturalBiomass = buffer.getBiomassNatural()[i];
            acc.dBiomassNatural += (float) (naturalBiomass * (photosyntheticEfficiencyMultiplier - 1.0) * 0.02 * dtSafe);
        }
        if (roomTemperatureSuperconductivity) {
            double fireEnergy = buffer.getEnergyFire()[i];
            acc.dEnergyFire += (float) (fireEnergy * 0.05 * dtSafe);
        }
    }

    /**
     * Apply physical laws across DoD WorldBuffer.
     *
     * @param buffer WorldBuffer DoD state
     * @param deltaYears time step in years
     */
    public static void applyPhysicalLaws(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;
        if (photosyntheticEfficiencyMultiplier == 1.0 && !roomTemperatureSuperconductivity && entropyGenerationScale == 1.0) {
            return;
        }

        double dtSafe = Math.max(0.0001, deltaYears);
        float[] bioNat = buffer.getBiomassNatural();
        float[] energyFire = buffer.getEnergyFire();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            if (photosyntheticEfficiencyMultiplier != 1.0) {
                bioNat[i] = (float) (bioNat[i] * (1.0 + (photosyntheticEfficiencyMultiplier - 1.0) * 0.02 * dtSafe));
            }
            if (roomTemperatureSuperconductivity) {
                energyFire[i] = (float) (energyFire[i] * (1.0 + 0.05 * dtSafe));
            }
        }
    }

    // Getters and Setters
    public static double getPhotosyntheticEfficiencyMultiplier() { return photosyntheticEfficiencyMultiplier; }
    public static void setPhotosyntheticEfficiencyMultiplier(double mult) { photosyntheticEfficiencyMultiplier = Math.max(0.1, mult); }

    public static boolean isRoomTemperatureSuperconductivity() { return roomTemperatureSuperconductivity; }
    public static void setRoomTemperatureSuperconductivity(boolean active) { roomTemperatureSuperconductivity = active; }

    public static double getEntropyGenerationScale() { return entropyGenerationScale; }
    public static void setEntropyGenerationScale(double scale) { entropyGenerationScale = Math.max(0.01, scale); }

    public static void resetState() {
        photosyntheticEfficiencyMultiplier = 1.0;
        roomTemperatureSuperconductivity = false;
        entropyGenerationScale = 1.0;
    }
}
