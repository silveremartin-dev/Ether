package org.ether.society.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeManagerTest {

    private TimeManager timeManager;

    @BeforeEach
    void setUp() {
        timeManager = new TimeManager(-20000); // 20000 BC
    }

    @Test
    @DisplayName("Initial state should be correct")
    void testInitialState() {
        assertEquals(-20000, timeManager.getCurrentYear());
        assertEquals(0, timeManager.getCurrentMonth()); // January
        assertEquals(1, timeManager.getCurrentDay());
        assertEquals("Day 1, Jan 20000 BC", timeManager.getFormattedDate());
    }

    @Test
    @DisplayName("Advancing days should update day, month and year properly")
    void testAdvanceDay() {
        for (int i = 0; i < 30; i++) {
            timeManager.advanceDay();
        }
        assertEquals(1, timeManager.getCurrentDay());
        assertEquals(1, timeManager.getCurrentMonth()); // February
    }

    @Test
    @DisplayName("Advancing months should update year properly")
    void testAdvanceMonth() {
        for (int i = 0; i < 12; i++) {
            timeManager.advanceMonth();
        }
        assertEquals(-19999, timeManager.getCurrentYear());
        assertEquals(0, timeManager.getCurrentMonth()); // January
    }

    @Test
    @DisplayName("Formatted date for AD years")
    void testFormattedDateAD() {
        timeManager.reset(2024);
        assertEquals(2024, timeManager.getCurrentYear());
        assertEquals("Day 1, Jan 2024 AD", timeManager.getFormattedDate());
    }

    @Test
    @DisplayName("Reset should restore year, month and day")
    void testReset() {
        timeManager.advanceMonth();
        timeManager.reset(1000);
        assertEquals(1000, timeManager.getCurrentYear());
        assertEquals(0, timeManager.getCurrentMonth());
        assertEquals(1, timeManager.getCurrentDay());
    }
}
