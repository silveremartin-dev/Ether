package org.ether.society.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventSystemTest {

    private EventSystem eventSystem;

    @BeforeEach
    void setUp() {
        eventSystem = new EventSystem();
    }

    @Test
    @DisplayName("Historical milestone event triggers at exact year")
    void testHistoricalEventTrigger() {
        eventSystem.checkEvents(-10000, 100, 1000.0); // Agricultural Revolution
        List<String> events = eventSystem.flushEvents();

        assertFalse(events.isEmpty(), "Should trigger historical event for -10000 BC");
        assertTrue(events.get(0).contains("Agricultural Revolution"));
    }

    @Test
    @DisplayName("Custom triggered event can be added and flushed")
    void testTriggerEvent() {
        eventSystem.triggerEvent("⚡ TEST: Solar Eclipse!");
        List<String> events = eventSystem.flushEvents();

        assertEquals(1, events.size());
        assertEquals("⚡ TEST: Solar Eclipse!", events.get(0));
        assertTrue(eventSystem.peekEvents().isEmpty(), "Queue should be empty after flush");
    }

    @Test
    @DisplayName("Milestone achievement triggers at 1 million population")
    void testPopulationMilestone() {
        eventSystem.checkEvents(100, 1_050_000, 50_000_000.0);
        List<String> events = eventSystem.flushEvents();

        assertTrue(events.stream().anyMatch(e -> e.contains("1 million")));
    }
}
