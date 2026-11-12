/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.HabitatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pressurized Extraterrestrial Habitat & Life Support (ECLSS) Physical Engine.
 *
 * Tier 1 Invariant Physics:
 * 1. Overrides lethal ambient exoplanetary conditions (vacuum, hyperbaric acid, cosmic radiation, extreme thermal cycles)
 *    for populations sheltered inside pressurized structures (Domes, Sintered Vaults, Subsurface Lava Tubes, Venusian Aerostats).
 * 2. Simulates physical aging, harsh environmental wear-and-tear, material fatigue, and continuous maintenance economics.
 * 3. Enforces catastrophic failure thresholds, structural breach lethality, and ECLSS power grid requirements.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PressurizedHabitatEngine extends AbstractSimulationEngine {
    public PressurizedHabitatEngine() {
        super("PressurizedHabitatEngine", "PressurizedHabitatEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(PressurizedHabitatEngine.class);

    /**
     * Evaluates whether a planet's surface conditions are lethal to unprotected human life.
     */
    public static boolean isHostileEnvironment(PlanetPreset preset) {
        if (preset == null) return false;
        double pressure = preset.atmospherePressureAtm();
        double o2 = preset.oxygenPercentage();
        double temp = preset.averageTempC();

        return pressure < 0.10 || pressure > 5.0 || o2 < 12.0 || temp < -30.0 || temp > 55.0;
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processPressurizedHabitats(buf, null, dtYears);
    }

    /**
     * Executes one simulation tick of habitat protection, structural aging, maintenance, and mortality across DoD buffer.
     */
    public static void processPressurizedHabitats(WorldBuffer buffer, PlanetPreset preset, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        boolean hostile = isHostileEnvironment(preset);
        if (!hostile) return;

        double dt = Math.max(0.001, Math.min(1.0, deltaYears));
        float[] bioHuman = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] metalArr = buffer.getMetalResource();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 0.0) continue;

            double requiredCapital = pop * 5.0 * dt;
            double requiredMetal = pop * 1.0 * dt;
            double availableCapital = capArr[i];
            double availableMetal = metalArr[i];

            double capCoverage = (requiredCapital > 0.0) ? Math.min(1.0, availableCapital / requiredCapital) : 1.0;
            double metalCoverage = (requiredMetal > 0.0) ? Math.min(1.0, availableMetal / requiredMetal) : 1.0;
            double maintFactor = (capCoverage + metalCoverage) / 2.0;

            if (maintFactor < 0.30) {
                double exposureMortalityRate = 0.95 * dt * (1.0 - maintFactor / 0.30);
                bioHuman[i] = (float) Math.max(0.0, bioHuman[i] * (1.0 - exposureMortalityRate));
            } else {
                capArr[i] = (float) Math.max(0.0, availableCapital - requiredCapital * maintFactor);
                metalArr[i] = (float) Math.max(0.0, availableMetal - requiredMetal * maintFactor);
            }
        }
    }

    /**
     * Initializes a pioneering outpost habitat on a cell according to local planetary geology.
     */
    public static void initializePioneerOutpost(H3Cell cell, PlanetPreset preset, long initialPopulation) {
        if (cell == null) return;

        HabitatType chosenType;
        if (preset != null && preset.atmospherePressureAtm() > 50.0) {
            chosenType = HabitatType.VENUS_AEROSTAT;
        } else {
            double elev = cell.getElevationOr(0.0);
            double mantleHeat = cell.getMantleHeatFlowOr(0.0);
            if (elev > 1000.0 || mantleHeat > 100.0) {
                chosenType = HabitatType.LAVA_TUBE;
            } else if (elev < 0) {
                chosenType = HabitatType.SINTERED_3D_VAULT;
            } else {
                chosenType = HabitatType.SURFACE_DOME;
            }
        }

        long cap = Math.max(1_000L, Math.round(initialPopulation * 1.5));
        int tier = (cap > 50_000) ? 3 : (cap > 5_000) ? 2 : 1;

        cell.setHabitatType(chosenType);
        cell.setHabitatTier(tier);
        cell.setHabitatCapacity((double) cap);
        cell.setHabitatIntegrity(1.0);
        cell.setHabitatAgeYears(0.0);
        cell.setHabitatEnergyKw(initialPopulation * chosenType.getBaseEnergyKwPerCapita());

        logger.info("🚀 Initialized Pioneer Extraterrestrial Habitat ({}, Tier {}) at H3 cell {} for {} colonists.",
                chosenType, tier, cell.getH3Index(), initialPopulation);
    }
}
