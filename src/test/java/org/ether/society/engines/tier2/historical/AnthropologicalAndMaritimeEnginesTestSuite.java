package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying anthropogenic, maritime, and evolutionary engines.
 */
public class AnthropologicalAndMaritimeEnginesTestSuite {

    @Test
    public void testAsymmetricColonialTradeDrainsBullion() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getMetalResource()[0] = 100.0f;
        buf.getResourceCapital()[0] = 500.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new AsymmetricColonialTradeEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(buf.getMetalResource()[0] < 100.0f,
                "Asymmetric colonial trade must drain metal bullion from cell");
        assertTrue(acc.dResourceCapital > 0.0f,
                "Asymmetric colonial trade must generate positive capital accumulation derivative");
    }

    @Test
    public void testMarineSubmersionEvacuatesCoastalPopulation() {
        WorldBuffer buf = new WorldBuffer(2);
        buf.getElevation()[0] = -1.5f;
        buf.getBiomassHuman()[0] = 4000.0f;
        buf.getResourceCapital()[0] = 8000.0f;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();

        buf.getElevation()[1] = 20.0f;
        buf.getBiomassHuman()[1] = 1000.0f;
        buf.getResourceCapital()[1] = 1000.0f;
        buf.getBiomes()[1] = (byte) Biome.PLAINS.ordinal();

        buf.getNeighborIndexes()[0] = new int[]{1};
        buf.getNeighborIndexes()[1] = new int[]{0};

        new MarineSubmersionEngine().tick(buf, 30.0, 30.0);

        assertTrue(buf.getBiomassHuman()[0] < 4000.0f, "Submerged cell must evacuate population");
        assertTrue(buf.getBiomassHuman()[1] > 1000.0f, "Safe inland cell must absorb evacuees");
    }

    @Test
    public void testSelfDomesticationAndSexualSelectionDerivatives() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getGiniIndex()[0] = 0.70f;
        buf.getResourceCapital()[0] = 1000.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new SelfDomesticationEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);
        assertTrue(acc.dResourceCapital > 0.0f,
                "High population density must drive self-domestication cooperation capital derivative");

        DerivativeAccumulator accSex = new DerivativeAccumulator();
        new SexualSelectionMatingEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, accSex);
        assertTrue(accSex.dResourceCapital > 0.0f,
                "High Gini inequality must drive sexual selection mating skew capital derivative");
    }
}
