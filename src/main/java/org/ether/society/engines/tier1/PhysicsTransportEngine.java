/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mechanical Work &amp; Hydrodynamic Transport Engine.
 * <p>
 * Models mechanical freight work $W = \mu_{\text{eff}} \, m \, g \, d$ ($\text{J}/\text{kg}\cdot\text{km}$)
 * as a smooth ($C^\infty$) continuous function of bathymetry/elevation, aridity, and standing canopy density
 * rather than discrete {@code switch(biome)} jumps ({@code AGENTS.md} §3, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicsTransportEngine extends AbstractSimulationEngine {
    public PhysicsTransportEngine() {
        super("PhysicsTransportEngine", "PhysicsTransportEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(PhysicsTransportEngine.class);

    /**
     * Calculates the mechanical work ($\text{J}/\text{kg}\cdot\text{km}$) required to transport cargo across a cell
     * from continuous physical terrain observables (elevation, precipitation, canopy biomass).
     *
     * @param cell H3 terrain cell
     * @return mechanical energy expenditure in Joules per kg per kilometer
     */
    public static double calculateTransportWorkJoules(H3Cell cell) {
        if (cell == null) return 1000.0;

        Biome biome = cell.getBiome();
        double elev = cell.getElevationOr(0.0);
        boolean isWaterBody = (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.LAKE);

        double landFraction = isWaterBody ? 0.0 : SmoothMath.sigmoid(elev, 2.0, 5.0);
        // Hydrodynamic fluid drag mu_water ~ 0.001
        double waterFriction = 0.001;

        // Continuous terrestrial Coulomb + topographic slope + sand/canopy impedance
        double posElev = SmoothMath.softplus(elev, 0.05);
        double slopeImpedance = 0.16 * Math.tanh(posElev / 1600.0);
        double rain = cell.getRainfallOr(500.0);
        // Loose dune sand impedance in hyper-arid zones (< 180 mm/yr)
        double duneImpedance = 0.10 * (1.0 - SmoothMath.sigmoid(rain, 180.0, 50.0));
        // Dense forest/jungle understory impedance
        double woodBio = cell.getWoodResourceOr(0.0) + cell.getBiomassNaturalOr(0.0) * 0.2;
        double canopyImpedance = 0.06 * Math.tanh(woodBio / 2500.0);

        double landFriction = 0.05 + slopeImpedance + duneImpedance + canopyImpedance;
        double effectiveFriction = waterFriction + landFraction * (landFriction - waterFriction);

        return effectiveFriction * 9.81 * 1000.0;
    }

    /** Executes one monthly transport work evaluation step across cells. */
    public static void processPhysicsTransport(List<H3Cell> cells) {
        processPhysicsTransport(cells, 1.0 / 12.0);
    }

    /**
     * Executes transport work evaluation step across cells, scaling cargo mass by {@code dtYears}
     * and deducting mechanical work from cellular energetic labor capacity.
     */
    public static void processPhysicsTransport(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        double totalWorkExpendedJoules = 0.0;
        int activeTransportCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop > 0) {
                double workPerKgKm = calculateTransportWorkJoules(cell);
                double cargoMassKg = pop * 2.0 * dtMonthlyRatio;
                double cellWorkJoules = workPerKgKm * cargoMassKg;
                totalWorkExpendedJoules += cellWorkJoules;
                activeTransportCells++;

                double cellWorkGJ = cellWorkJoules / 1.0e9;
                cell.setResourceWork(Math.max(0.0, cell.getResourceWorkOr(0.0) - cellWorkGJ));
            }
        }

        if (activeTransportCells > 0) {
            logger.debug("Physics Transport: Expended {} GJ of mechanical freight work across {} cells.",
                    String.format(java.util.Locale.ROOT, "%.3f", totalWorkExpendedJoules / 1.0e9), activeTransportCells);
        }
    }
}
