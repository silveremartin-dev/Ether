/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DomainEventBusTest {

    public record TestClimateEvent(String type, double deltaTemp) {}
    public record TestEcoEvent(String message) {}

    private DomainEventBus bus;

    @BeforeEach
    public void setUp() {
        bus = DomainEventBus.getInstance();
        bus.clear();
    }

    @Test
    public void testDeterministicPriorityOrder() {
        List<String> executionOrder = new ArrayList<>();

        bus.subscribe(TestClimateEvent.class, DomainEventBus.Priority.LOW, e -> executionOrder.add("LOW"));
        bus.subscribe(TestClimateEvent.class, DomainEventBus.Priority.FIRST, e -> executionOrder.add("FIRST"));
        bus.subscribe(TestClimateEvent.class, DomainEventBus.Priority.NORMAL, e -> executionOrder.add("NORMAL"));
        bus.subscribe(TestClimateEvent.class, DomainEventBus.Priority.LAST, e -> executionOrder.add("LAST"));

        bus.publishNow(new TestClimateEvent("WARMING", 1.5));

        assertEquals(List.of("FIRST", "NORMAL", "LOW", "LAST"), executionOrder, "Events must execute in strict priority order");
    }

    @Test
    public void testDeferredPhaseFlushing() {
        List<String> results = new ArrayList<>();

        bus.subscribe(TestEcoEvent.class, e -> results.add(e.message()));

        bus.enqueue(new TestEcoEvent("EVENT_1"));
        bus.enqueue(new TestEcoEvent("EVENT_2"));

        assertEquals(0, results.size(), "Pending events must not fire before flush");

        bus.flushPendingEvents();

        assertEquals(List.of("EVENT_1", "EVENT_2"), results, "Flushed events must execute in FIFO order");
    }
}