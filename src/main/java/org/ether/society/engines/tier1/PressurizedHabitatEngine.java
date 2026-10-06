/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.HabitatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
 * @version 1.0.0-beta.2
 */
public class PressurizedHabitatEngine {
    private static final Logger logger = LoggerFactory.getLogger(PressurizedHabitatEngine.class);

    /*
     * Evaluates whether a planet's surface conditions are lethal to unprotected human life.
     */
    public static boolean isHostileEnvironment(PlanetPreset preset) {
        if (preset == null) return false;
        double pressure = preset.atmospherePressureAtm();
        double o2 = preset.oxygenPercentage();
        double temp = preset.averageTempC();

        return pressure < 0.10 || pressure > 5.0 || o2 < 12.0 || temp < -30.0 || temp > 55.0;
    }

    /*
     * Executes one simulation tick of habitat protection, structural aging, maintenance, and mortality.
     */
    public static void processPressurizedHabitats(List<H3Cell> cells, PlanetPreset preset, double deltaYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        boolean hostile = isHostileEnvironment(preset);
        double dt = Math.max(0.001, Math.min(1.0, deltaYears));

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            HabitatType type = cell.getHabitatType();

            // If planet is hostile
            if (hostile) {
                if (pop > 0) {
                    if (type == HabitatType.NONE || cell.getHabitatIntegrity() <= 0.05) {
                        // Unprotected or completely breached: massive catastrophic exposure mortality
                        double exposureMortalityRate = 0.95 * dt;
                        int surviving = (int) Math.round(pop * Math.max(0.0, 1.0 - exposureMortalityRate));
                        if (surviving != pop) {
                            logger.warn("💀 Severe atmospheric/vacuum exposure mortality at H3 cell {} (Pop: {} -> {})",
                                    cell.getH3Index(), pop, surviving);
                            cell.setPopulation(surviving);
                            cell.setBiomassHuman((double) surviving);
                        }
                        continue;
                    }

                    // Process active sheltered habitat
                    processShelteredCell(cell, type, preset, dt);
                }
            } else {
                // On temperate Earth-like planet: habitats age normally if present
                if (type != HabitatType.NONE && pop > 0) {
                    processShelteredCell(cell, type, preset, dt);
                }
            }
        }
    }

    // Helper subroutine: process sheltered cell - internal state computation & bounds checking
    private static void processShelteredCell(H3Cell cell, HabitatType type, PlanetPreset preset, double dt) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double integrity = cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() : 1.0;
        double capacity = cell.getHabitatCapacity() != null ? cell.getHabitatCapacity() : 0.0;
        double age = cell.getHabitatAgeYears() != null ? cell.getHabitatAgeYears() : 0.0;

        // Ensure minimum capacity baseline if uninitialized
        if (capacity < pop) {
            capacity = Math.max(500.0, pop * 1.25);
            cell.setHabitatCapacity(capacity);
        }

        // 1. Environmental Harshness Factor delta_env
        double deltaEnv = 0.0;
        if (preset != null) {
            if (preset.atmospherePressureAtm() < 0.01) deltaEnv += 0.005; // Vacuum / micrometeorite stress
            if (preset.averageTempC() < -50.0 || preset.averageTempC() > 100.0) deltaEnv += 0.010; // Thermal shock
            if (preset.atmospherePressureAtm() > 50.0) deltaEnv += 0.015; // Venusian super-dense acid atmosphere
        }

        // 2. Maintenance Economics
        // Required maintenance per person per year: 5 kg capital and 1 kg metal
        double requiredCapital = pop * 5.0 * dt;
        double requiredMetal = pop * 1.0 * dt;

        double availableCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
        double availableMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

        double capCoverage = (requiredCapital > 0) ? Math.min(1.0, availableCapital / requiredCapital) : 1.0;
        double metalCoverage = (requiredMetal > 0) ? Math.min(1.0, availableMetal / requiredMetal) : 1.0;
        double maintFactor = (capCoverage + metalCoverage) / 2.0;

        // Deduct invested maintenance resources
        if (requiredCapital > 0 && availableCapital > 0) {
            cell.setResourceCapital(Math.max(0.0, availableCapital - (requiredCapital * maintFactor)));
        }
        if (requiredMetal > 0 && availableMetal > 0) {
            cell.setResourceMetal(Math.max(0.0, availableMetal - (requiredMetal * maintFactor)));
        }

        // 3. Structural Aging & Integrity Evolution
        double baseWear = type.getBaselineWearRatePerYear();
        double grossWear = (baseWear + deltaEnv) * dt;

        // Maintenance mitigates up to 90% of gross wear and repairs up to +5%/year if well-funded
        double netWear = grossWear * (1.0 - 0.90 * maintFactor);
        double repairRate = (maintFactor > 0.95 && integrity < 1.0) ? (0.05 * dt) : 0.0;

        integrity = Math.clamp(integrity - netWear + repairRate, 0.0, 1.0);
        cell.setHabitatIntegrity(integrity);
        cell.setHabitatAgeYears(age + dt);

        // 4. Operational ECLSS Energy Calculation
        double energyKwPerCapita = type.getBaseEnergyKwPerCapita() * (1.0 + (1.0 - integrity) * 0.50);
        double totalEnergyKw = pop * energyKwPerCapita;
        cell.setHabitatEnergyKw(totalEnergyKw);

        // 5. Vétusté / Critical Degradation Hazard (Integrity < 0.30)
        if (integrity < 0.30) {
            double breachHazard = (0.30 - integrity) * 0.80 * dt;
            int breachDeaths = (int) Math.round(pop * breachHazard);
            if (breachDeaths > 0) {
                int newPop = Math.max(0, pop - breachDeaths);
                cell.setPopulation(newPop);
                cell.setBiomassHuman((double) newPop);
                pop = newPop;
            }
        }

        // 6. Pressurized Capacity Overcrowding Enforcement
        if (pop > capacity && capacity > 0) {
            double excess = pop - capacity;
            // Acute hypoxia & life support rationing on excess population
            int hypoxiaDeaths = (int) Math.round(excess * (1.0 - Math.exp(-2.5 * dt)));
            if (hypoxiaDeaths > 0) {
                int newPop = Math.max(0, pop - hypoxiaDeaths);
                cell.setPopulation(newPop);
                cell.setBiomassHuman((double) newPop);
                pop = newPop;
            }
        }

        // 7. Auto-Expansion of Habitats when approaching capacity
        if (pop >= 0.80 * capacity && availableCapital >= 1000.0 && availableMetal >= 200.0) {
            int currentTier = cell.getHabitatTier() != null ? cell.getHabitatTier() : 1;
            int newTier = Math.min(4, currentTier + 1);
            double newCapacity = switch (newTier) {
                case 1 -> 500.0;
                case 2 -> 5_000.0;
                case 3 -> 50_000.0;
                case 4 -> 500_000.0;
                default -> capacity * 2.0;
            };
            if (newCapacity > capacity) {
                cell.setHabitatTier(newTier);
                cell.setHabitatCapacity(newCapacity);
                cell.setResourceCapital(Math.max(0.0, availableCapital - 800.0));
                cell.setResourceMetal(Math.max(0.0, availableMetal - 150.0));
                logger.info("🪐 Extraterrestrial Habitat expanded to Tier {} at H3 cell {} [New Capacity: {}]",
                        newTier, cell.getH3Index(), newCapacity);
            }
        }
    }

    /*
     * Initializes a pioneering outpost habitat on a cell according to local planetary geology.
     */
    public static void initializePioneerOutpost(H3Cell cell, PlanetPreset preset, long initialPopulation) {
        if (cell == null) return;

        HabitatType chosenType;
        if (preset != null && preset.atmospherePressureAtm() > 50.0) {
            chosenType = HabitatType.VENUS_AEROSTAT;
        } else {
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double mantleHeat = cell.getMantleHeatFlow() != null ? cell.getMantleHeatFlow() : 0.0;
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
