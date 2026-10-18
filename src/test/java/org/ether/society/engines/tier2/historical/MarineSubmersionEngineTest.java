package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link MarineSubmersionEngine}.
 */
public class MarineSubmersionEngineTest {

    @Test
    public void testCoastalFloodingAndEvacuation() {
        // Flooded coastal cell
        H3Cell flooded = new H3Cell(613503380827930820L, 52.0, 4.0);
        flooded.setElevation(1.0);
        flooded.setSeaLevelOffsetMeters(2.0); // 2m sea level rise > 1m elevation -> flooded
        flooded.setPopulation(5000);
        flooded.setResourceCapital(10000.0);
        flooded.setTechnologyLevel(3.0);
        flooded.setBiome(Biome.PLAINS);

        // Safe inland neighbor cell
        H3Cell safe = new H3Cell(613503380827930821L, 52.1, 4.1);
        safe.setElevation(25.0); // 25m > 2m -> safe
        safe.setSeaLevelOffsetMeters(2.0);
        safe.setPopulation(1000);
        safe.setResourceCapital(2000.0);
        safe.setBiome(Biome.PLAINS);

        MarineSubmersionEngine.processHybrid(List.of(flooded, safe), 30.0);

        assertTrue(flooded.getPopulation() < 5000, "Flooded cell must evacuate population");
        assertTrue(safe.getPopulation() > 1000, "Safe neighbor must absorb evacuated refugees");
        assertTrue(safe.getResourceCapital() > 2000.0, "Refugees must transfer portable capital");
    }

    @Test
    public void testPolderDikeBreachWithLowCapital() {
        H3Cell polder = new H3Cell(613503380827930822L, 52.5, 4.5);
        polder.setElevation(0.5);
        polder.setSeaLevelOffsetMeters(1.0);
        polder.setIsPolder(true);
        polder.setResourceCapital(0.0); // Zero capital to maintain dikes
        polder.setPopulation(2000);
        polder.setBiome(Biome.PLAINS);

        MarineSubmersionEngine.processHybrid(List.of(polder), 30.0);

        assertTrue(polder.getPopulation() < 2000, "Polder with zero maintenance capital must suffer breach and casualty/evacuation");
    }

    @Test
    public void testNullSafetyGuards() {
        assertDoesNotThrow(() -> MarineSubmersionEngine.processHybrid(null, 30.0));
        assertDoesNotThrow(() -> MarineSubmersionEngine.processHybrid(new ArrayList<>(), 30.0));

        // Incomplete cell with null attributes
        H3Cell incomplete = new H3Cell(613503380827930823L, 0.0, 0.0);
        incomplete.setPopulation(null);
        incomplete.setResourceCapital(null);
        incomplete.setSeaLevelOffsetMeters(null);
        incomplete.setElevation(null);
        incomplete.setIsPolder(null);

        assertDoesNotThrow(() -> {
            MarineSubmersionEngine.processHybrid(List.of(incomplete), 30.0);
        }, "Marine submersion must not throw NPE when encountering uninitialized or null fields");
    }
}
