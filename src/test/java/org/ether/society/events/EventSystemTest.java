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

    @Test
    @DisplayName("Spatial events are properly recorded in recentEventsHistory")
    void testRecentEventsHistory() {
        ActiveEvent event1 = new ActiveEvent("EVT_1", "Test Event 1", "VOLCANO", 12.5, 45.0, 100, 2, 15, 20.0, 7.5);
        ActiveEvent event2 = new ActiveEvent("EVT_2", "Test Event 2", "EARTHQUAKE", -8.2, -60.1, 105, 5, 1, 20.0, 4.2);

        eventSystem.recordSpatialEvent(event1);
        eventSystem.recordSpatialEvent(event2);

        List<ActiveEvent> history = eventSystem.getRecentEventsHistory();
        assertEquals(2, history.size());
        assertEquals("Test Event 1", history.get(0).getTitle());
        assertEquals(7.5, history.get(0).getMagnitude());
        assertEquals("Élevée", history.get(0).getIntensityLabel());
        assertTrue(history.get(0).getFormattedCoordinates().contains("12.50°N"));

        assertEquals("Faible", history.get(1).getIntensityLabel());
        assertTrue(history.get(1).getFormattedCoordinates().contains("8.20°S"));
    }
}

