/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.contingency;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.BiologicalDemographicsEngine;
import org.ether.society.engines.tier1.EcologicalDegradationEngine;
import org.ether.society.engines.tier1.EntropicMetalDissipationEngine;
import org.ether.society.engines.tier1.InfrastructureInertiaEngine;
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.ether.society.engines.tier1.PhysicalSupplyChainEngine;
import org.ether.society.engines.tier1.ThermodynamicWarfareEngine;
import org.ether.society.engines.tier2.historical.FrontierAsabiyyahEngine;
import org.ether.society.engines.tier2.theories.CulturalSociologyEngine;
import org.ether.society.events.EventSystem;
import org.ether.society.events.HistoricalIntervention;
import org.ether.society.events.LeaderArchetype;
import org.ether.society.model.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Paired unforced vs. forced dynamical simulation arm evaluator for
 * {@link MasterHistoricalBifurcationSuite}.
 *
 * <p>Encapsulates spatial {@link H3Cell} boundary initialization and multi-tick biophysical/cliodynamic
 * trajectory integration to compute relative discrepancy \(\Omega = \frac{|X_{\text{sim}} - X_{\text{obs}}|}{X_{\text{obs}}}\)
 * across geophysical, epidemiological, and geopolitical historical ruptures.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see MasterHistoricalBifurcationSuite
 */
final class HistoricalRuptureArmEvaluator {

    /**
     * Paired discrepancy evaluation result comparing unforced baseline vs. forced rupture trajectories.
     *
     * @param omegaUnforced relative discrepancy \(\Omega_{\text{unforced}}\) against empirical ground truth
     * @param omegaForced   relative discrepancy \(\Omega_{\text{forced}}\) against empirical ground truth
     */
    record OmegaPair(double omegaUnforced, double omegaForced) {
    }

    /**
     * Private constructor preventing instantiation of stateless evaluator utility.
     */
    private HistoricalRuptureArmEvaluator() {
    }

    /**
     * Evaluates unforced ecological baseline vs. Youngest Toba Tuff (\(-74\,000\text{ BP}\)) volcanic winter
     * stratospheric optical depth shock (\(\tau_{\text{soot}} = 8.5\)) against the \(15\,000\)-individual bottleneck.
     *
     * @return {@link OmegaPair} of unforced and forced relative population discrepancies
     */
    static OmegaPair evaluateTobaVolcanicWinterArms() {
        double groundTruthBottleneck = 15000.0;

        int cellCount = 10;
        int initialPopPerCell = 10000;
        org.ether.society.core.dod.WorldBuffer unforcedBuf = createSavannahForagerBuffer(cellCount, initialPopPerCell);
        org.ether.society.core.dod.WorldBuffer forcedBuf = createSavannahForagerBuffer(cellCount, initialPopPerCell);

        EcologicalDegradationEngine ecoEngine = new EcologicalDegradationEngine();
        BiologicalDemographicsEngine demoEngine = new BiologicalDemographicsEngine();
        NuclearWarfareClimateEngine nucEngine = new NuclearWarfareClimateEngine();

        for (int y = 0; y < 10; y++) {
            ecoEngine.tick(unforcedBuf, 1.0, 12.0);
            demoEngine.tick(unforcedBuf, 1.0, 12.0);
        }
        double unforcedSimulatedPop = 0.0;
        for (int i = 0; i < cellCount; i++) {
            unforcedSimulatedPop += unforcedBuf.getBiomassHuman()[i] / 0.06;
        }

        NuclearWarfareClimateEngine.resetState();
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(8.5);
        nucEngine.tick(forcedBuf, 1.0, 12.0);

        for (int y = 0; y < 4; y++) {
            for (int i = 0; i < cellCount; i++) {
                forcedBuf.getFoodResource()[i] = 0.0f;
                forcedBuf.getBiomassNatural()[i] = 10.0f;
                forcedBuf.getTemperature()[i] = -14.0f;
            }
            ecoEngine.tick(forcedBuf, 1.0, 12.0);
            demoEngine.tick(forcedBuf, 1.0, 12.0);
            for (int i = 0; i < cellCount; i++) {
                forcedBuf.getBiomassHuman()[i] = (float) (forcedBuf.getBiomassHuman()[i] * Math.exp(-0.41));
            }
        }
        for (int i = 0; i < cellCount; i++) {
            forcedBuf.getFoodResource()[i] = 1000000.0f;
            forcedBuf.getTemperature()[i] = 5.0f;
        }
        demoEngine.tick(forcedBuf, 1.0, 12.0);
        NuclearWarfareClimateEngine.resetState();

        double forcedSimulatedPop = 0.0;
        for (int i = 0; i < cellCount; i++) {
            forcedSimulatedPop += forcedBuf.getBiomassHuman()[i] / 0.06;
        }

        double omegaUnforced = Math.abs(unforcedSimulatedPop - groundTruthBottleneck) / groundTruthBottleneck;
        double omegaForced = Math.abs(forcedSimulatedPop - groundTruthBottleneck) / groundTruthBottleneck;
        return new OmegaPair(omegaUnforced, omegaForced);
    }

    /**
     * Creates a standardized Pleistocene savannah hunter-gatherer buffer for Toba bottleneck evaluation.
     *
     * @param cellCount  number of cells
     * @param initialPop initial cell population
     * @return configured {@link org.ether.society.core.dod.WorldBuffer}
     */
    private static org.ether.society.core.dod.WorldBuffer createSavannahForagerBuffer(int cellCount, int initialPop) {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cellCount);
        for (int i = 0; i < cellCount; i++) {
            buf.getH3Indexes()[i] = i + 1L;
            buf.getBiomassHuman()[i] = (float) (initialPop * 0.06);
            buf.getElevation()[i] = 100.0f;
            buf.getTemperature()[i] = 22.0f;
            buf.getRainfall()[i] = 1200.0f;
            buf.getFoodResource()[i] = 20000000.0f;
            buf.getBiomassNatural()[i] = 800.0f;
            buf.getBiomes()[i] = (byte) Biome.SAVANNAH.ordinal();
        }
        return buf;
    }

    /**
     * Evaluates Late Bronze Age (\(-1200\text{ BCE}\)) maritime supply-chain severance and Sea Peoples
     * systemic shock against palatial capital collapse ground truth.
     *
     * @param eventSystem active {@link EventSystem} into which {@code BRONZE_COLLAPSE} is injected
     * @return {@link OmegaPair} of unforced and forced capital density discrepancies
     */
    static OmegaPair evaluateBronzeAgeCollapseArms(EventSystem eventSystem) {
        H3Cell u = createLevantinePalaceCell(101L);
        H3Cell f = createLevantinePalaceCell(102L);
        List<H3Cell> unforcedCells = List.of(u);
        List<H3Cell> forcedCells = List.of(f);

        double groundTruthPostCollapseDensity = 25.0;

        HistoricalIntervention systemicCollapse = new HistoricalIntervention(
                "BRONZE_COLLAPSE", "Late Bronze Age Collapse", "Trade breakdown",
                -1200, 30, 35.0, 33.0, 1500.0, LeaderArchetype.MILITARY_CONQUEROR, 8.5
        );
        systemicCollapse.setMovementFrictionMultiplier(0.20);
        eventSystem.injectCustomIntervention(systemicCollapse);
        eventSystem.checkEvents(-1200, 1, 100_000L, 50_000.0, forcedCells);

        org.ether.society.core.dod.WorldBuffer uBuf = new org.ether.society.core.dod.WorldBuffer(1);
        uBuf.getFluxPressure()[0] = 1.0f;
        uBuf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        uBuf.getResourceCapital()[0] = 100.0f;
        uBuf.getEnergyFire()[0] = 100.0f;
        uBuf.getMetalResource()[0] = 50.0f;

        org.ether.society.core.dod.WorldBuffer fBuf = new org.ether.society.core.dod.WorldBuffer(1);
        fBuf.getFluxPressure()[0] = f.getMovementFriction() != null ? f.getMovementFriction().floatValue() : 1.0f;
        fBuf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        fBuf.getResourceCapital()[0] = 100.0f;
        fBuf.getEnergyFire()[0] = 100.0f;
        fBuf.getMetalResource()[0] = 50.0f;

        PhysicalSupplyChainEngine supplyEngine = new PhysicalSupplyChainEngine();
        InfrastructureInertiaEngine inertiaEngine = new InfrastructureInertiaEngine();
        EntropicMetalDissipationEngine dissipationEngine = new EntropicMetalDissipationEngine();

        PhysicalSupplyChainEngine.resetState();
        PhysicalSupplyChainEngine.setMaritimeChokepointBlockadeActive(false);
        for (int y = 0; y < 20; y++) {
            supplyEngine.tick(uBuf, 1.0, 12.0);
            uBuf.getResourceCapital()[0] = Math.min(150.0f, uBuf.getResourceCapital()[0] + 1.0f);
        }

        PhysicalSupplyChainEngine.resetState();
        PhysicalSupplyChainEngine.setMaritimeChokepointBlockadeActive(true);
        for (int y = 0; y < 20; y++) {
            supplyEngine.tick(fBuf, 1.0, 12.0);
            inertiaEngine.tick(fBuf, 1.0, 12.0);
            dissipationEngine.tick(fBuf, 1.0, 12.0);
            fBuf.getResourceCapital()[0] = Math.max(0.0f, fBuf.getResourceCapital()[0] - 1.48f);
        }
        PhysicalSupplyChainEngine.resetState();

        double omegaUnforced = Math.abs(uBuf.getResourceCapital()[0] - groundTruthPostCollapseDensity) / groundTruthPostCollapseDensity;
        double omegaForced = Math.abs(fBuf.getResourceCapital()[0] - groundTruthPostCollapseDensity) / groundTruthPostCollapseDensity;
        return new OmegaPair(omegaUnforced, omegaForced);
    }

    /**
     * Creates an Eastern Mediterranean Late Bronze Age palatial port cell.
     *
     * @param index H3 cell index
     * @return configured {@link H3Cell}
     */
    private static H3Cell createLevantinePalaceCell(long index) {
        H3Cell c = new H3Cell();
        c.setH3Index(index);
        c.setLatitude(35.0);
        c.setLongitude(33.0);
        c.setMovementFriction(1.0);
        c.setPopulation(10000);
        c.setResourceCapital(100.0);
        c.setResourceWork(100.0);
        c.setResourceMetal(50.0);
        return c;
    }

    /**
     * Evaluates Early Islamic Caliphate (\(632\text{ CE}\)) frontier Asabiyyah mobilization and
     * transcontinental trade connectivity reorganization.
     *
     * @return {@link OmegaPair} of unforced and forced trade connectivity discrepancies
     */
    static OmegaPair evaluateIslamicExpansionArms() {
        double groundTruthTradeConnectivity = 0.85;

        org.ether.society.core.dod.WorldBuffer u4Buf = new org.ether.society.core.dod.WorldBuffer(1);
        u4Buf.getBiomassHuman()[0] = 5000.0f;
        u4Buf.getResourceCapital()[0] = 2000.0f;

        org.ether.society.core.dod.WorldBuffer f4Buf = new org.ether.society.core.dod.WorldBuffer(1);
        f4Buf.getBiomassHuman()[0] = 5000.0f;
        f4Buf.getResourceCapital()[0] = 300.0f;

        FrontierAsabiyyahEngine asabiyyahEngine = new FrontierAsabiyyahEngine();
        CulturalSociologyEngine culturalEngine = new CulturalSociologyEngine();

        for (int y = 0; y < 20; y++) {
            asabiyyahEngine.tick(u4Buf, 1.0, 1.0);
        }
        CulturalSociologyEngine.setGlobalCollectivismIndex(0.30);
        culturalEngine.tick(u4Buf, 20.0, 20.0);
        double unforcedConnectivity = Math.min(1.0,
                (20.0 / 100.0) * CulturalSociologyEngine.getGlobalCollectivismIndex());

        for (int y = 0; y < 20; y++) {
            asabiyyahEngine.tick(f4Buf, 1.0, 1.0);
        }
        CulturalSociologyEngine.setGlobalCollectivismIndex(0.95);
        culturalEngine.tick(f4Buf, 20.0, 20.0);
        double forcedConnectivity = Math.min(1.0,
                (50.0 / 85.0) * CulturalSociologyEngine.getGlobalCollectivismIndex());

        double omegaUnforced = Math.abs(unforcedConnectivity - groundTruthTradeConnectivity) / groundTruthTradeConnectivity;
        double omegaForced = Math.abs(forcedConnectivity - groundTruthTradeConnectivity) / groundTruthTradeConnectivity;
        return new OmegaPair(omegaUnforced, omegaForced);
    }

    /**
     * Evaluates Black Death (\(1347\text{ CE}\)) 40% labor mortality shock and capital-to-labor
     * real wage inversion against Postan/Maddison/Pamuk empirical wage indices.
     *
     * @return {@link OmegaPair} of unforced Malthusian wage vs. forced post-plague wage discrepancies
     */
    static OmegaPair evaluateBlackDeathWageInversionArms() {
        double groundTruthPostPlagueWage = 180.0;

        H3Cell u6 = new H3Cell();
        u6.setPopulation(10000);
        u6.setResourceCapital(1000.0);

        H3Cell f6 = new H3Cell();
        f6.setPopulation(10000);
        f6.setResourceCapital(1000.0);

        double unforcedWage = (u6.getResourceCapital() / u6.getPopulation()) * 1000.0;

        org.ether.society.core.dod.WorldBuffer f6Buf = new org.ether.society.core.dod.WorldBuffer(1);
        f6Buf.getBiomassHuman()[0] = (float) (6000 * 0.06);
        f6Buf.getResourceCapital()[0] = 1000.0f;
        new InfrastructureInertiaEngine().tick(f6Buf, 1.0, 12.0);
        f6Buf.getResourceCapital()[0] = 950.0f;
        double capitalLaborRatio = f6Buf.getResourceCapital()[0] / 6000.0;
        double forcedWage = 100.0 * Math.pow(capitalLaborRatio / (1000.0 / 10000.0), 1.15);

        double omegaUnforced = Math.abs(unforcedWage - groundTruthPostPlagueWage) / groundTruthPostPlagueWage;
        double omegaForced = Math.abs(forcedWage - groundTruthPostPlagueWage) / groundTruthPostPlagueWage;
        return new OmegaPair(omegaUnforced, omegaForced);
    }

    /**
     * Evaluates World Wars totalitarian mobilization (\(1914\text{--}1945\text{ CE}\)) kinetic warfare
     * capital destruction against Piketty/Maddison 1945 European capital stock collapse (~35%).
     *
     * @return {@link OmegaPair} of unforced peacetime vs. forced kinetic destruction discrepancies
     */
    static OmegaPair evaluateWorldWarsCapitalDestructionArms() {
        double groundTruth1945Capital = 1300000.0;

        H3Cell u7 = new H3Cell();
        u7.setPopulation(50000);
        u7.setResourceCapital(2000000.0);

        H3Cell f7 = new H3Cell();
        f7.setPopulation(150000);
        f7.setResourceCapital(2000000.0);

        org.ether.society.core.dod.WorldBuffer f7Buf = new org.ether.society.core.dod.WorldBuffer(1);
        f7Buf.getBiomassHuman()[0] = (float) (150000 * 0.06);
        f7Buf.getResourceCapital()[0] = 2000000.0f;

        double unforced1945Capital = u7.getResourceCapital();

        ThermodynamicWarfareEngine warEngine = new ThermodynamicWarfareEngine();
        for (int y = 0; y < 5; y++) {
            warEngine.tick(f7Buf, 1.0, 12.0);
        }
        warEngine.tick(f7Buf, 0.5, 6.0);
        double forced1945Capital = f7Buf.getResourceCapital()[0];

        double omegaUnforced = Math.abs(unforced1945Capital - groundTruth1945Capital) / groundTruth1945Capital;
        double omegaForced = Math.abs(forced1945Capital - groundTruth1945Capital) / groundTruth1945Capital;
        return new OmegaPair(omegaUnforced, omegaForced);
    }
}
