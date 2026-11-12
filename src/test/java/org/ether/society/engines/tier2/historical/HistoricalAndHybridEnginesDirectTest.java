package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for the refactored historical and hybrid engines using {@link WorldBuffer}.
 */
public class HistoricalAndHybridEnginesDirectTest {

    @Test
    public void testHandyNasaHybridEngine() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 50000.0f;
        buf.getBiomassNatural()[0] = 50.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new HandyNasaHybridEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dBiomassNatural < 0.0f, "High population density should deplete natural biomass");
    }

    @Test
    public void testMaritimeHighwayEngine() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.OCEAN.ordinal();
        buf.getElevation()[0] = 0.0f;
        buf.getTemperature()[0] = 15.0f;
        buf.getBiomassHuman()[0] = 500.0f;
        buf.getResourceCapital()[0] = 200.0f;
        buf.getEnergyWind()[0] = 5000.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new MaritimeHighwayEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dResourceCapital > 0.0f, "Ice-free maritime basin should boost capital accumulation");
    }

    @Test
    public void testMilitaryTechShockEngine() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 1000.0f;
        buf.getResourceCapital()[0] = 1_500_000.0f; // kPerCap = 1500.0 -> peak shock intensity

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new MilitaryTechShockEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dResourceCapital > 0.0f,
                "Military tech shock resonance should stimulate capital investment");
    }

    @Test
    public void testNordhausDiceHybridEngine() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getTemperature()[0] = 20.0f; // +5C above 15C baseline
        buf.getResourceCapital()[0] = 10000.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new NordhausDiceHybridEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dResourceCapital < 0.0f, "Warming anomaly should inflict DICE capital damage");
    }

    @Test
    public void testSpatialCityFractalEngine() {
        WorldBuffer buf = new WorldBuffer(3);
        buf.getBiomassHuman()[0] = 50000.0f;
        buf.getBiomassHuman()[1] = 10000.0f;
        buf.getBiomassHuman()[2] = 1000.0f;

        new SpatialCityFractalEngine().tick(buf, 1.0, 1.0);

        double total = buf.getBiomassHuman()[0] + buf.getBiomassHuman()[1] + buf.getBiomassHuman()[2];
        assertEquals(61000.0, total, 1.0, "Zipf rank-size relaxation must conserve total urban population");
    }

    @Test
    public void testWorld3HybridEngine() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 1000.0f;
        buf.getMetalResource()[0] = 5.0f; // Low metal -> high FCAOR
        buf.getPollutionLevel()[0] = 50.0f; // High pollution -> lifespan penalty
        buf.getLifespan()[0] = 70.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new World3HybridEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dResourceCapital < 0.0f, "Low metal should trigger World3 FCAOR capital decline");
        assertTrue(acc.dLifespan < 0.0f, "High pollution should reduce lifespan");
    }
}
