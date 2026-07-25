package org.ether.society.diplomacy;

import org.ether.society.model.Nation;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiplomacyManagerTest {

    private DiplomacyManager diplomacyManager;

    @BeforeEach
    void setUp() {
        diplomacyManager = new DiplomacyManager();
    }

    @Test
    @DisplayName("Register and retrieve nation by ID")
    void testRegisterNation() {
        Nation nation = new Nation("Rome", Color.RED, null);
        diplomacyManager.registerNation(nation);

        assertEquals(1, diplomacyManager.getNations().size());
        Nation found = diplomacyManager.getNationById(nation.getId());
        assertNotNull(found);
        assertEquals("Rome", found.getName());
    }

    @Test
    @DisplayName("Remove nation")
    void testRemoveNation() {
        Nation nation = new Nation("Carthage", Color.BLUE, null);
        diplomacyManager.registerNation(nation);
        diplomacyManager.removeNation(nation);

        assertEquals(0, diplomacyManager.getNations().size());
        assertNull(diplomacyManager.getNationById(nation.getId()));
    }
}
