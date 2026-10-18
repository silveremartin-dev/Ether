package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link PhysicalLeontiefInputOutputEngine}.
 */
public class PhysicalLeontiefInputOutputEngineTest {

    @Test
    public void testLeontiefCapitalExpansionUnderAbundantResources() {
        H3Cell cell = new H3Cell(613503380827930830L, 48.0, 2.0);
        cell.setPopulation(10000);
        cell.setTechnologyLevel(2.0);
        cell.setResourceCapital(5000.0);
        cell.setResourceMetal(5000.0);
        cell.setEnergySolar(500.0);
        cell.setEnergyWind(500.0);
        cell.setEnergyFire(1000.0);

        PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(List.of(cell), 1.0);

        assertTrue(cell.getResourceCapital() > 5000.0, "Abundant inputs must yield positive net capital growth");
        assertTrue(cell.getResourceMetal() < 5000.0, "Metal input must be consumed proportionally to capital output");
    }

    @Test
    public void testLeontiefBottleneckWhenEnergyConstrained() {
        H3Cell cell = new H3Cell(613503380827930831L, 48.1, 2.1);
        cell.setPopulation(10000);
        cell.setTechnologyLevel(2.0);
        cell.setResourceCapital(5000.0);
        cell.setResourceMetal(5000.0);
        // Zero energy -> bottleneck: min(metal / a_m, energy / a_e) = 0
        cell.setEnergySolar(0.0);
        cell.setEnergyWind(0.0);
        cell.setEnergyFire(0.0);

        PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(List.of(cell), 1.0);

        assertTrue(cell.getResourceCapital() - 5000.0 < 5.0, "Zero energy input must bottleneck Leontief production to near-zero baseline capital gain");
    }

    @Test
    public void testNullSafetyWithUninitializedEnergyAndDemographics() {
        assertDoesNotThrow(() -> PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(null, 1.0));
        assertDoesNotThrow(() -> PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(new ArrayList<>(), 1.0));

        H3Cell cell = new H3Cell(613503380827930832L, 0.0, 0.0);
        cell.setPopulation(null);
        cell.setEnergySolar(null);
        cell.setEnergyWind(null);
        cell.setEnergyFire(null);
        cell.setResourceMetal(null);
        cell.setResourceCapital(null);

        assertDoesNotThrow(() -> {
            PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(List.of(cell), 1.0);
        }, "Engine must safely tolerate null energy and demographic fields without throwing NPE");
    }
}
