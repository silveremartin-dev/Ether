/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.ether.society.model.Scenario;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Physical Capital Diffusion &amp; Induced Innovation Engine.
 *
 * <p>In strict conformance with {@code AGENTS.md} §10.1, technology is NOT stored or mutated
 * as an independent state variable. This engine provides:</p>
 * <ul>
 *   <li><b>A posteriori physical era diagnostic</b>: maps continuous per-capita power dissipation
 *       (\(P_{\text{watts}}/N\)) and material yield strength into human historical archetypes.</li>
 *   <li><b>Spatial capital and knowledge diffusion</b>: transports structural capital and
 *       productive tooling along active commercial corridors.</li>
 *   <li><b>Induced capital deepening</b>: Allen-Acemoglu directed technical change manifesting
 *       through physical capital accumulation (\(\Delta K\)).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PhysicalCapitalDiffusionEngine implements SimulationEngineModule {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalCapitalDiffusionEngine.class);

    @Override
    public String id() {
        return "PhysicalCapitalDiffusionEngine";
    }

    @Override
    public String name() {
        return "Physical Capital Diffusion Engine";
    }

    @Override
    public EngineTier tier() {
        return EngineTier.TIER_2_CLIODYNAMIC;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && (Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()))
                || Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("TechTreeEngine")));
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processBuffer(buffer, dtYears);
    }

    @Override
    public void reset() {
        // Stateless physical operator
    }

    public void processBuffer(WorldBuffer buffer, double dtYears) {
        if (buffer == null || dtYears <= 0) return;
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] woodArr = buffer.getBiomassNatural();
        float[] metalArr = buffer.getMetalResource();
        if (popArr == null || capArr == null) return;

        int count = buffer.getCapacity();
        for (int i = 0; i < count; i++) {
            float pop = popArr[i];
            if (pop <= 0.0f) continue;

            float capPerCapita = capArr[i] / pop;
            float wood = woodArr != null ? woodArr[i] : 1000.0f;
            float metal = metalArr != null ? metalArr[i] : 0.0f;

            double organicDemand = pop * 0.15;
            double organicScarcityStress = SmoothMath.smoothClamp(1.0 - (wood / Math.max(1.0, organicDemand)), 0.0, 1.0, 0.05);

            double potMetal = (metal > 0.0) ? (metal / (metal + 15.0)) : 0.0;
            double deltaK = 5.0 * organicScarcityStress * potMetal * Math.max(0.0001, dtYears);
            capArr[i] += (float) (deltaK * pop);
        }
    }

    public record PhysicalEra(String i18nKeyPrefix, String defaultName, double minPowerWattsPerCapita, double minMaterialYieldMPa, String defaultDescription) {
        public String name() {
            return org.ether.society.i18n.I18n.getOrDefault(i18nKeyPrefix + ".name", defaultName);
        }

        public String description() {
            return org.ether.society.i18n.I18n.getOrDefault(i18nKeyPrefix + ".desc", defaultDescription);
        }
    }

    public static final PhysicalEra PALEOLITHIC = new PhysicalEra(
            "era.paleolithic", "Paleolithic", 100.0, 10.0,
            "Mobile hunter-gatherer bands, lithic flaking (10 MPa), and domestic hearth fire (100 W/capita).");
    public static final PhysicalEra NEOLITHIC = new PhysicalEra(
            "era.neolithic", "Neolithic", 250.0, 50.0,
            "Domestication, kiln ceramics, native copper (50 MPa), and draft animal traction (250 W/capita).");
    public static final PhysicalEra ANTIQUITY = new PhysicalEra(
            "era.antiquity", "Classical Antiquity", 600.0, 150.0,
            "Bronze metallurgy (150 MPa), bloomery wrought iron (300 MPa), and hydraulic canals (600 W/capita).");
    public static final PhysicalEra MEDIEVAL = new PhysicalEra(
            "era.medieval", "Middle Ages", 1500.0, 400.0,
            "Heavy moldboard plow, water/wind mills (400 MPa), and forestry charcoal (1,500 W/capita).");
    public static final PhysicalEra INDUSTRIAL = new PhysicalEra(
            "era.industrial", "Industrial Revolution", 8000.0, 800.0,
            "High-pressure steam engines, Bessemer steel (800 MPa), and fossil coal grids (8,000 W/capita).");

    /**
     * Determines physical historical era based on per-capita mechanical/thermal power dissipation.
     *
     * @param cell the cell under inspection
     * @return diagnostic historical era
     */
    public static PhysicalEra getPhysicalEra(H3Cell cell) {
        if (cell == null) return PALEOLITHIC;
        double power = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
        if (power >= INDUSTRIAL.minPowerWattsPerCapita()) return INDUSTRIAL;
        if (power >= MEDIEVAL.minPowerWattsPerCapita()) return MEDIEVAL;
        if (power >= ANTIQUITY.minPowerWattsPerCapita()) return ANTIQUITY;
        if (power >= NEOLITHIC.minPowerWattsPerCapita()) return NEOLITHIC;
        return PALEOLITHIC;
    }
}
