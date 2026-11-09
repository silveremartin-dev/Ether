/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
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
 * @version 1.0.0-beta.2
 */
public class PhysicalCapitalDiffusionEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalCapitalDiffusionEngine.class);

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

    /**
     * Executes physical era progression and spatial power diffusion with default monthly step.
     */
    public static void processCapitalDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        processCapitalDiffusion(cells, activeRoutes, 1.0 / 12.0);
    }

    /**
     * Executes spatial capital equalization along active trade routes and bottleneck-induced capital deepening.
     */
    public static void processCapitalDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dt = Math.max(0.0001, dtYears);

        // 1. Allen-Acemoglu induced capital accumulation under bottleneck stress
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double inducedCapAdvance = calculateBottleneckInducedSubstitution(cell, dt);
            if (inducedCapAdvance > 0.0) {
                double curCap = cell.getResourceCapitalOr(0.0);
                cell.setResourceCapital(curCap + inducedCapAdvance * pop);
            }
        }

        // 2. Spatial capital diffusion along trade routes
        if (activeRoutes != null && !activeRoutes.isEmpty()) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                if (route == null || route.origin() == null || route.destination() == null) continue;
                H3Cell orig = route.origin();
                H3Cell dest = route.destination();
                int popO = orig.getPopulationOr(0);
                int popD = dest.getPopulationOr(0);
                if (popO <= 0 || popD <= 0) continue;

                double kO = orig.getResourceCapitalOr(0.0) / popO;
                double kD = dest.getResourceCapitalOr(0.0) / popD;
                double diff = kO - kD;
                if (diff > 1.0) {
                    double transfer = 0.01 * diff / Math.max(1.0, route.totalFrictionCost() * 0.1) * dt;
                    dest.setResourceCapital(Math.max(0.0, dest.getResourceCapitalOr(0.0) + transfer * popD));
                } else if (diff < -1.0) {
                    double transfer = 0.01 * (-diff) / Math.max(1.0, route.totalFrictionCost() * 0.1) * dt;
                    orig.setResourceCapital(Math.max(0.0, orig.getResourceCapitalOr(0.0) + transfer * popO));
                }
            }
        }
    }

    /**
     * Evaluates universal Boserup-Allen-Acemoglu Directed Technical Change driven by physical resource
     * bottlenecks coupled with latent higher-enthalpy geological/ecological stocks.
     * Returns physical capital per-capita increment.
     *
     * @param cell    the H3 cell under evaluation
     * @param dtYears time step in years
     * @return endogenous physical capital per-capita increment \(\Delta K \ge 0\)
     */
    public static double calculateBottleneckInducedSubstitution(H3Cell cell, double dtYears) {
        if (cell == null) return 0.0;
        int pop = cell.getPopulationOr(0);
        if (pop <= 0) return 0.0;

        double capPerCapita = cell.getResourceCapitalOr(0.0) / pop;
        double wood = Math.max(
                cell.getBiomassNaturalOr(0.0),
                cell.getWoodResourceOr(0.0));
        double metal = cell.getResourceMetalOr(0.0);
        double coal = cell.getResourceCoalOr(0.0);
        double oilGas = cell.getResourceOilOr(0.0) + cell.getResourceGasOr(0.0);
        double uranium = cell.getResourceUraniumOr(0.0);

        // 1. Primary organic fuel & biomass scarcity stress relative to population
        double organicDemand = pop * 0.15;
        double organicScarcityStress = SmoothMath.smoothClamp(1.0 - (wood / Math.max(1.0, organicDemand)), 0.0, 1.0, 0.05);

        // 2. Latent higher-order resource potential available to resolve the bottleneck
        // Continuous C^1 Fermi-Dirac technological regime substitution (Allen 2009; Clark 2007; Wrigley 2010)
        double potMetal = (metal > 0.0) ? (metal / (metal + 15.0)) : 0.0;
        double potCoal = (coal > 0.0) ? (coal / (coal + 50.0)) : 0.0;
        double potHydro = (oilGas + uranium * 10.0 > 0.0) ? ((oilGas + uranium * 10.0) / (oilGas + uranium * 10.0 + 100.0)) : 0.0;

        double wCoal = SmoothMath.sigmoid(capPerCapita, 500.0, 150.0);
        double wHydro = SmoothMath.sigmoid(capPerCapita, 2000.0, 500.0);
        double wMetal = 1.0 - 0.7 * wCoal;

        double latentSubstitutePotential = Math.clamp(
                wMetal * potMetal + wCoal * (1.0 - wHydro) * potCoal + wHydro * potHydro,
                0.0, 1.0
        );

        // Physical capital deepening rate under scarcity bottlenecks (Allen 2009; Clark 2007)
        return 5.0 * organicScarcityStress * latentSubstitutePotential * Math.max(0.0001, dtYears);
    }
}
