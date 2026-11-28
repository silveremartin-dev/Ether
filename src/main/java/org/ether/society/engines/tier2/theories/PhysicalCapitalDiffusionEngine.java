/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;

/**
 * Physical Capital Diffusion &amp; Induced Substitution Engine (Nelson &amp; Phelps 1966; Hicks 1932; Acemoglu 2002).
 *
 * <p>Implements two peer-reviewed mechanisms strictly over conserved physical observables ({@code AGENTS.md} §4.2 &amp; §5.1):</p>
 * <ol>
 *   <li><b>Nelson-Phelps Spatial Frontier Catch-Up (Nelson &amp; Phelps 1966, <i>AER</i> 56:69–75)</b>:
 *       $$\frac{dk_i}{dt} = \phi_d \, \max\!\left(0,\; k_{\max,\mathcal{N}_i} - k_i\right)$$
 *       diffusing productive physical tooling across contiguous H3 trade neighbors.</li>
 *   <li><b>Hicks-Acemoglu Induced Factor-Scarcity Substitution (Hicks 1932; Acemoglu 2002, <i>QJE</i> 117:781)</b>:
 *       Organic timber scarcity relative to metallic ore availability induces mineral-embodied capital deepening
 *       bounded by {@code TypeBEnginePassRunner}'s Lavoisier-Carnot envelope.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class PhysicalCapitalDiffusionEngine extends AbstractSimulationEngine {

    /** Nelson-Phelps spatial frontier catch-up rate (\(\phi_d = 0.025\,\text{yr}^{-1}\)). */
    public static final double NELSON_PHELPS_CATCHUP_RATE = 0.025;

    /**
     * Constructs the Nelson-Phelps / Hicks-Acemoglu physical capital diffusion engine.
     */
    public PhysicalCapitalDiffusionEngine() {
        super("PhysicalCapitalDiffusionEngine", "Physical Capital Diffusion Engine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && (Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()))
                || Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("TechTreeEngine")));
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Nelson-Phelps Diffusion & Hicks-Acemoglu Induced Substitution]
               • Frontier Catch-Up:     dk_i/dt = φ_d · max(0, k_frontier,N(i) - k_i)
               • Induced Substitution:  dk_ind/dt = μ_ind · S_organic · (M_metal / (M_metal + M_0))
               Parameters: φ_d = 0.025 yr^-1, M_0 = 15 t
               Ref: R. R. Nelson & E. S. Phelps (1966, AER 56:69); D. Acemoglu (2002, QJE 117:781)
               """;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0 || buffer.getHumanPopulation() == null || buffer.getResourceCapital() == null) {
            return;
        }
        float pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0f) return;

        double kSelf = buffer.getResourceCapital()[i] / pop;

        // 1. Nelson & Phelps (1966) contiguous H3 neighbor frontier catch-up
        double kNeighborMax = kSelf;
        int[][] neighbors = buffer.getNeighborIndexes();
        if (neighbors != null && i < neighbors.length && neighbors[i] != null) {
            float[] popArr = buffer.getHumanPopulation();
            float[] capArr = buffer.getResourceCapital();
            int cap = buffer.getCapacity();
            for (int nIdx : neighbors[i]) {
                if (nIdx >= 0 && nIdx < cap && popArr[nIdx] > 1.0f) {
                    double kNbr = capArr[nIdx] / popArr[nIdx];
                    if (kNbr > kNeighborMax) {
                        kNeighborMax = kNbr;
                    }
                }
            }
        }
        double nelsonPhelpsCatchUpKgPerCap = NELSON_PHELPS_CATCHUP_RATE
                * SmoothMath.softplus(kNeighborMax - kSelf, 0.0, 2.0) * dtYears;

        // 2. Hicks (1932) / Acemoglu (2002) organic-resource scarcity induced substitution
        double wood = buffer.getBiomassNatural() != null ? buffer.getBiomassNatural()[i] : 1000.0;
        double metal = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0;
        double organicDemand = pop * 0.15;
        double organicScarcityStress = SmoothMath.smoothClamp(1.0 - (wood / Math.max(1.0, organicDemand)), 0.0, 1.0, 20.0);
        double metalElasticity = metal > 0.0 ? (metal / (metal + 15.0)) : 0.0;
        double inducedDeltaKgPerCap = 5.0 * organicScarcityStress * metalElasticity * dtYears;

        acc.dResourceCapital += (float) ((nelsonPhelpsCatchUpKgPerCap + inducedDeltaKgPerCap) * pop);
    }

    /**
     * A posteriori diagnostic historical archetype record derived solely from physical observables.
     *
     * @param i18nKeyPrefix          localization bundle key prefix
     * @param defaultName            fallback English archetype name
     * @param minPowerWattsPerCapita minimum per-capita power dissipation (\(\text{W/person}\))
     * @param minMaterialYieldMPa    characteristic structural material yield strength (\(\text{MPa}\))
     * @param defaultDescription     fallback English physical description
     */
    public record PhysicalEra(
            String i18nKeyPrefix,
            String defaultName,
            double minPowerWattsPerCapita,
            double minMaterialYieldMPa,
            String defaultDescription) {

        /**
         * Returns the localized display name of this physical era.
         *
         * @return localized era name
         */
        public String name() {
            return I18n.getOrDefault(i18nKeyPrefix + ".name", defaultName);
        }

        /**
         * Returns the localized scientific description of this physical era.
         *
         * @return localized era description
         */
        public String description() {
            return I18n.getOrDefault(i18nKeyPrefix + ".desc", defaultDescription);
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
     * Determines physical historical era based strictly on per-capita mechanical/thermal power dissipation.
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
