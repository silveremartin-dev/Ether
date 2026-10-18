/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link ResourceType}.
 * Verifies resource classification, display names, and non-null GIS color associations.
 */
class ResourceTypeTest {

    @Test
    @DisplayName("All ResourceType values define non-empty display names and non-null UI colors")
    void testResourceTypesIntegrity() {
        for (ResourceType type : ResourceType.values()) {
            assertNotNull(type.getDisplayName(), "Display name must not be null for " + type);
            assertFalse(type.getDisplayName().isBlank(), "Display name must not be blank for " + type);
            assertNotNull(type.getColor(), "Color must not be null for " + type);
        }
    }

    @Test
    @DisplayName("Core energetic and mineral resources are present in enum")
    void testCoreResourcesPresent() {
        assertNotNull(ResourceType.FOOD);
        assertNotNull(ResourceType.FRESHWATER_AQUIFER);
        assertNotNull(ResourceType.IRON);
        assertNotNull(ResourceType.COPPER);
        assertNotNull(ResourceType.COAL);
        assertNotNull(ResourceType.OIL);
        assertNotNull(ResourceType.URANIUM);
        assertNotNull(ResourceType.LITHIUM);
        assertNotNull(ResourceType.RARE_EARTHS);
    }
}
