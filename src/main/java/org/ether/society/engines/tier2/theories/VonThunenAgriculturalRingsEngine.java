/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.Biome;
import org.ether.society.model.SpatialSurfaceHelper;

/**
 * <h1>Von Thünen Agricultural Rings &amp; Overland Transport Friction Engine</h1>
 * <p>
 * Models the geographic logistics of agricultural food transport and urban agglomeration limits,
 * formulating spatial transport decay across terrestrial hinterlands versus coastal/fluvial waterways
 * (von Thünen 1826; Krugman 1991; Bairoch 1988).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>Johann Heinrich von Thünen (1826)</b> <i>Der isolierte Staat in Beziehung auf Landwirtschaft und Nationalökonomie</i>, Perthes, Hamburg.</li>
 *   <li><b>Paul Krugman (1991)</b> <i>Increasing Returns and Economic Geography</i>, J. Political Econ. 99(3): 483-499.</li>
 *   <li><b>Paul Bairoch (1988)</b> <i>Cities and Economic Development: From the Dawn of History to the Present</i>, Univ. of Chicago Press.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class VonThunenAgriculturalRingsEngine extends AbstractSimulationEngine {

    /**
     * Constructs the von Thünen agricultural transport rings engine.
     */
    public VonThunenAgriculturalRingsEngine() {
        super("VonThunenAgriculturalRingsEngine", "VonThunenAgriculturalRingsEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String getName() {
        return "VonThunenAgriculturalRingsEngine";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0) return;

        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        double capitalPerCapita = capital / pop;
        boolean hasMaritimeOrRiverAccess = hasWaterwayAccess(buffer, i);

        double areaScale = SpatialSurfaceHelper.resolveAreaScaleVsRes4(buffer, i);
        double maxSustainableAgglomeration;
        if (hasMaritimeOrRiverAccess) {
            maxSustainableAgglomeration = (500_000.0 + capitalPerCapita * 2500.0) * areaScale;
        } else {
            double ancientInlandBase = 25_000.0 * areaScale;
            double earlyModernExpansion = 55_000.0 * areaScale * SmoothMath.sigmoid(capitalPerCapita, 250.0, 75.0);
            double industrialRailExpansion = 5000.0 * areaScale * SmoothMath.softplus(capitalPerCapita - 800.0, 0.0, 100.0);
            maxSustainableAgglomeration = ancientInlandBase + earlyModernExpansion + industrialRailExpansion;
        }

        if (pop > maxSustainableAgglomeration && maxSustainableAgglomeration > 0.0) {
            double excessRatio = (pop - maxSustainableAgglomeration) / maxSustainableAgglomeration;
            double currentFood = Math.max(0.0, buffer.getFoodResource()[i]);
            double annualTransportLoss = Math.min(0.35, excessRatio * 0.15) * Math.min(1.0, dtYears);
            acc.dFoodResource -= (float) (currentFood * annualTransportLoss);
        }
    }

    private static boolean hasWaterwayAccess(WorldBuffer buffer, int i) {
        float[] elev = buffer.getElevation();
        byte[] biomes = buffer.getBiomes();
        float[] water = buffer.getWaterResource();
        if (water != null && i < water.length && water[i] >= 400.0f) {
            return true;
        }
        int[][] neighbors = buffer.getNeighborIndexes();
        if (neighbors != null && i < neighbors.length && neighbors[i] != null) {
            for (int nIdx : neighbors[i]) {
                if (nIdx < 0 || nIdx >= buffer.getCapacity()) continue;
                if (elev != null && nIdx < elev.length && elev[nIdx] < 0.0f) {
                    return true;
                }
                if (biomes != null && nIdx < biomes.length
                        && (biomes[nIdx] == (byte) Biome.OCEAN.ordinal()
                        || biomes[nIdx] == (byte) Biome.DEEP_OCEAN.ordinal())) {
                    return true;
                }
            }
        }
        return false;
    }
}
