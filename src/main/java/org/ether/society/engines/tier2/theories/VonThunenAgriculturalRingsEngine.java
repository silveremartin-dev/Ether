/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.dod.WorldBufferComponentAdapter;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.model.SpatialSurfaceHelper;

/**
 * <h1>Von Thünen Agricultural Rings &amp; Overland Transport Friction Engine</h1>
 * <p>
 * Implements Johann Heinrich von Thünen's (1826) canonical spatial bid-rent and draught-fodder
 * grain transport decay model across circular supply hinterlands:
 * </p>
 * <ul>
 *   <li><b>Locational Rent Equation</b>: \(R(r) = Y (p - c - f_{\text{mode}} \cdot r)\), vanishing at the
 *       zero-rent cultivation margin \(r_{\max} = (p - c) / f_{\text{mode}}\).</li>
 *   <li><b>Circular Supply Ring Radius</b>: \(R_{\text{ring}} = \sqrt{\frac{N_i \cdot e_{\text{meta}}}{\pi \, y_{\text{agr},i}}}\),
 *       with haulage beyond local village field-walk radius \(r_0 = 5\,\text{km}\) (Chisholm 1962; von Thünen 1826).</li>
 *   <li><b>Exact Disk-Integrated Draught Transit Consumption</b>:
 *       \(L_{\text{transit}}(u) = 1 - \frac{2}{u^2}\left[1 - (1 + u)e^{-u}\right]\) where \(u = c_{\text{eff}} \, d_{\text{haul}}\)
 *       (\(c_{\text{land}} = 0.008\,\text{km}^{-1}\) overland ox-wagon vs \(c_{\text{water}} = 0.0008\,\text{km}^{-1}\) navigable barge,
 *       von Thünen 1826; Bairoch 1988).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class VonThunenAgriculturalRingsEngine extends AbstractSimulationEngine {

    /** Pre-industrial overland ox-wagon grain fodder share consumed per km (\(\text{km}^{-1}\), von Thünen 1826 §4: ~125 km zero-net limit). */
    public static final double OVERLAND_WAGON_LOSS_PER_KM = 0.008;

    /** Navigable river/coastal barge transport energy share per km (\(\text{km}^{-1}\), Bairoch 1988: ~10x lower than overland cartage). */
    public static final double WATERWAY_BARGE_LOSS_PER_KM = 0.0008;

    /** Local rural village field-commute radius requiring zero bulk inter-settlement cartage (\(\text{km}\), Chisholm 1962). */
    public static final double LOCAL_VILLAGE_WALK_RADIUS_KM = 5.0;

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
    public String getEquationsTooltip() {
        return """
               [Von Thünen Spatial Bid-Rent & Transport Consumption (von Thünen 1826)]
               • Locational Land Rent:   R(r) = Y · (p - c - f_mode · r)
               • Supply Ring Radius:     R_ring = sqrt( (N · e_meta) / (π · y_agr) )
               • Disk Transit Loss:      L(u) = 1 - (2 / u^2) · [1 - (1 + u) · exp(-u)],  u = c_eff · d_haul
               Parameters: c_land = 0.008 km^-1 (ox-wagon), c_water = 0.0008 km^-1 (barge), r_0 = 5 km
               Ref: J. H. von Thünen (1826) "Der isolierte Staat"; P. Bairoch (1988) "Cities and Economic Development"
               """;
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

        double currentFood = Math.max(0.0, buffer.getFoodResource()[i]);
        if (currentFood <= 0.0) return;

        double areaKm2 = Math.max(1.0, SpatialSurfaceHelper.resolveCellAreaKm2(buffer, i));
        double availEdibleGJ = WorldBufferComponentAdapter.computeAvailableEdibleCaloricGJ(buffer, i);
        double agrYieldDensityGJPerKm2 = Math.max(180.0, availEdibleGJ / areaKm2);

        // Required caloric demand N * e_meta [GJ/yr] -> circular supply ring radius R_ring [km]
        double annualCaloricDemandGJ = pop * PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        double supplyRadiusKm = Math.sqrt(annualCaloricDemandGJ / (Math.PI * agrYieldDensityGJPerKm2));
        double haulDistanceKm = SmoothMath.softplus(supplyRadiusKm - LOCAL_VILLAGE_WALK_RADIUS_KM, 0.0, 1.0);
        if (haulDistanceKm <= 1.0e-3) return;

        double capitalPerCapita = Math.max(0.0, buffer.getResourceCapital()[i] / pop);
        boolean waterway = hasWaterwayAccess(buffer, i);
        double baseModalCostPerKm = waterway ? WATERWAY_BARGE_LOSS_PER_KM : OVERLAND_WAGON_LOSS_PER_KM;
        double terrainMultiplier = (buffer.getFluxPressure() != null && buffer.getFluxPressure()[i] > 0.0f)
                ? Math.clamp(buffer.getFluxPressure()[i], 0.4, 3.0)
                : 1.0;

        // Rolling-stock / rail infrastructure attenuation of overland transport energy per ton-km
        double effectiveCostPerKm = (baseModalCostPerKm * terrainMultiplier) / (1.0 + capitalPerCapita / 250.0);
        double u = effectiveCostPerKm * haulDistanceKm;

        // Exact analytical integral of (1 - exp(-c * r)) over circular supply disk of radius d_haul
        double transitLossFraction;
        if (u < 1.0e-4) {
            transitLossFraction = (2.0 / 3.0) * u - 0.25 * u * u;
        } else {
            transitLossFraction = 1.0 - (2.0 / (u * u)) * (1.0 - (1.0 + u) * Math.exp(-u));
        }

        double boundedAnnualLoss = Math.clamp(transitLossFraction, 0.0, 0.40) * Math.min(1.0, dtYears);
        acc.dFoodResource -= (float) (currentFood * boundedAnnualLoss);
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
