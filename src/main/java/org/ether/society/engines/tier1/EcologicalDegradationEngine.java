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
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Degradation, Climate Stress, Pollution &amp; Overfishing Engine (Tier 1 Core Physics).
 * <p>
 * Strictly governed by smooth ($C^\infty$) conservation and regeneration equations of physical
 * observables ($N, K, B_{\text{nat}}, B_{\text{agr}}, B_{\text{fish}}, W_{\text{wood}}$, soil carbon, water)
 * with zero {@code technologyLevel} parameters and zero Tier 2 static coupling ({@code AGENTS.md} §7.2, §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EcologicalDegradationEngine extends AbstractSimulationEngine {
    public EcologicalDegradationEngine() {
        super("EcologicalDegradationEngine", "EcologicalDegradationEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(EcologicalDegradationEngine.class);

    /** Standard human annual metabolic energy consumption ($3.362\,\text{GJ/capita/year}$). */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_GJ = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;

    /**
     * Executes one monthly ecological degradation tick across all cells.
     */

    /**
     * Computes local biophysical human carrying capacity strictly from soil organic carbon,
     * meteoric rainfall, freshwater availability, cultivated biomass, and physical capital per capita ($C^\infty$).
     *
     * @param cell H3 cell
     * @return biophysical carrying capacity (individuals)
     */
    public static double calculateCarryingCapacity(H3Cell cell) {
        if (cell == null) return 100.0;

        double soc = cell.getSoilOrganicCarbonOr(20.0);
        double soilFactor = SmoothMath.softmax(0.2, soc / 50.0, 5.0);
        double rain = cell.getRainfallOr(500.0);
        double rainFactor = SmoothMath.smoothClamp(rain / 500.0, 0.1, 2.0, 5.0);
        double water = cell.getWaterResourceOr(100.0);
        double waterFactor = 0.8 + 0.4 * (water / (25.0 + Math.max(0.0, water)));

        int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                : 3;
        double areaScale = Math.max(0.05, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3) / 1770.49);
        double baseCap = 50.0 * areaScale * soilFactor * rainFactor * waterFactor;

        int pop = Math.max(1, cell.getPopulationOr(1));
        double capPerCap = cell.getResourceCapitalOr(0.0) / pop;
        double agBiomass = cell.getBiomassAgricultureOr(0.0);
        double agGate = SmoothMath.sigmoid(agBiomass, 10.0, 4.0);
        double agMultiplier = 1.0 + agGate * (0.5 + 2.0 * (1.0 - Math.exp(-agBiomass / (500.0 * areaScale))));

        // Marine & coastal fishery carrying capacity boost (shellfish, fish, marine algae)
        double marineStock = cell.getBiomassFishOr(0.0) + cell.getCoastalMarineResourceOr(0.0);
        double marineMultiplier = 1.0 + 1.2 * Math.tanh(marineStock / (800.0 * areaScale));

        // Terrestrial fauna / pastoral livestock carrying capacity boost (hunting & pastoralism)
        double liveStock = cell.getBiomassLivestockOr(0.0);
        double pastoralMultiplier = 1.0 + 0.8 * Math.tanh(liveStock / (600.0 * areaScale));

        double capitalMultiplier = 1.0 + 3.0 * (1.0 - Math.exp(-capPerCap / 250.0));

        return Math.max(2.0, baseCap * agMultiplier * marineMultiplier * pastoralMultiplier * capitalMultiplier);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));
        double pop = buffer.getBiomassHuman()[i] / 0.06;

        double currPollution = buffer.getPollutionLevel()[i];
        double attenuatedPollution = Math.max(0.0, currPollution * Math.pow(0.92, dtMonthlyRatio) - 1.2 * dtMonthlyRatio);
        double newPollution = attenuatedPollution;
        if (pop > 0.0) {
            double capPerCapita = buffer.getResourceCapital()[i] / Math.max(1.0, pop);
            double popDensity = pop / 1770.49;
            double industrialGate = SmoothMath.sigmoid(capPerCapita, 1500.0, 350.0);
            double fossilCombustion = (buffer.getResourceCoal()[i] * 0.05 + buffer.getResourceOil()[i] * 0.08 + buffer.getResourceGas()[i] * 0.04) * industrialGate;
            double miningGate = SmoothMath.sigmoid(capPerCapita, 1100.0, 250.0);
            double rawMining = buffer.getMetalResource()[i] * 0.02 * SmoothMath.softmin(3.0, capPerCapita / 500.0, 4.0);
            double miningActivity = miningGate * SmoothMath.softmin(25.0, rawMining, 0.5);
            double industrialEmission = popDensity * 0.08 * industrialGate + miningActivity + SmoothMath.softmin(50.0, fossilCombustion, 0.2);
            newPollution = Math.min(1000.0, attenuatedPollution + industrialEmission * dtMonthlyRatio);
        }
        acc.dPollutionLevel += (float) (newPollution - currPollution);

        if (newPollution > 0.0) {
            double pollutionToxicity = newPollution / (150.0 + newPollution);
            acc.dLifespan -= (float) (1.2 * pollutionToxicity * dtMonthlyRatio);
            acc.dFertility -= (float) (0.5 * pollutionToxicity * dtMonthlyRatio);
            acc.dBiomassNatural -= (float) Math.min(buffer.getBiomassNatural()[i], 5.0 * pollutionToxicity * dtMonthlyRatio);
        }
    }
}
