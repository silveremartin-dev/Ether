/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for {@link SimulationTickEvent} and {@link SimulationTickListener}.
 *
 * <p>Validates decoupled telemetry dispatching across both {@link DomainEventBus}
 * and direct engine observer subscriptions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationTickTelemetryTest {

    private DomainEventBus bus;

    @BeforeEach
    void setUp() {
        bus = DomainEventBus.getInstance();
        bus.clear();
    }

    @Test
    @DisplayName("SimulationTickEvent correctly published and received via DomainEventBus")
    void testDomainEventBusTelemetry() {
        List<SimulationTickEvent> receivedEvents = new ArrayList<>();

        bus.subscribe(SimulationTickEvent.class, receivedEvents::add);

        SimulationTickEvent event = new SimulationTickEvent(
                100L, 2026, 5, 8_000_000_000L, 60.5, 1.2e14, 0.42, true
        );

        bus.publishNow(event);

        assertEquals(1, receivedEvents.size());
        SimulationTickEvent received = receivedEvents.get(0);
        assertEquals(100L, received.tickIndex());
        assertEquals(2026, received.year());
        assertEquals(5, received.month());
        assertEquals(8_000_000_000L, received.totalPopulation());
        assertEquals(60.5, received.currentTPS(), 1e-4);
        assertEquals(1.2e14, received.gdp(), 1e2);
        assertEquals(0.42, received.gini(), 1e-4);
        assertTrue(received.yearChanged());
    }

    @Test
    @DisplayName("Engine tick listeners receive events on stepForward")
    void testEngineTickListener() throws Exception {
        Configuration config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);

        com.uber.h3core.H3Core h3 = com.uber.h3core.H3Core.newInstance();
        long validH3Index = h3.latLngToCell(48.8566, 2.3522, 8);

        H3Cell cell = new H3Cell();
        cell.setH3Index(validH3Index);
        cell.setLatitude(48.8566);
        cell.setLongitude(2.3522);
        cell.setBiome(Biome.FOREST);
        cell.setElevation(100.0);
        cell.setPopulation(1000);
        cell.setBiomassHuman(500.0);
        cell.setBiomassLivestock(100.0);
        cell.setBiomassFish(0.0);
        cell.setBiomassAgriculture(500.0);
        cell.setBiomassNatural(1000.0);
        cell.setTemperature(15.0);
        cell.setRainfall(800.0);
        cell.setFoodResource(2000.0);
        cell.setWaterResource(100.0);
        cell.setWoodResource(500.0);
        cell.setResourceMetal(200.0);
        cell.setResourceClay(200.0);
        cell.setResourceCapital(100.0);
        cell.setEnergyWind(50.0);
        cell.setEnergySolar(50.0);
        cell.setEnergyFire(50.0);
        cell.setEnergySlaves(0.0);
        cell.setEnergyFoodConsumed(50.0);

        engine.setCells(List.of(cell));

        AtomicInteger tickCount = new AtomicInteger(0);
        List<SimulationTickEvent> eventList = new ArrayList<>();

        SimulationTickListener listener = event -> {
            tickCount.incrementAndGet();
            eventList.add(event);
        };

        engine.addTickListener(listener);
        assertEquals(1, engine.getTickListeners().size());

        // Step forward 3 ticks
        engine.stepForward(3);

        assertEquals(3, tickCount.get());
        assertEquals(3, eventList.size());
        assertTrue(eventList.get(0).tickIndex() > 0);

        // Remove listener
        engine.removeTickListener(listener);
        assertEquals(0, engine.getTickListeners().size());

        engine.stepForward(1);
        assertEquals(3, tickCount.get(), "Listener must not receive events after removal");
        engine.shutdown();
    }
}
