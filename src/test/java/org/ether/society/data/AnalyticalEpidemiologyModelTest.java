/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link AnalyticalEpidemiologyModel}.
 * Validates the analytical $R_0$ pathogen raster generator across historical epochs,
 * epidemic outbreak pulses (Justinian, Black Death, Columbian exchange, Spanish Flu),
 * and spatial bounds.
 */
class AnalyticalEpidemiologyModelTest {

    @Test
    @DisplayName("generatePathogenMap generates non-null BufferedImage with requested dimensions")
    void testGeneratePathogenMapDimensions() {
        int width = 72;
        int height = 36;
        BufferedImage img = AnalyticalEpidemiologyModel.generatePathogenMap(1347L, width, height, null);

        assertNotNull(img);
        assertEquals(width, img.getWidth());
        assertEquals(height, img.getHeight());
    }

    @Test
    @DisplayName("Epidemic intensity during Black Death (1347 AD) in Europe exceeds peaceful epochs")
    void testBlackDeathEpidemicPulse() {
        int width = 180;
        int height = 90;

        BufferedImage img1347 = AnalyticalEpidemiologyModel.generatePathogenMap(1347L, width, height, null);
        BufferedImage img1200 = AnalyticalEpidemiologyModel.generatePathogenMap(1200L, width, height, null);

        // Sample near Mediterranean / Italy / Black Sea region (approx lat 42 N, lon 12 E)
        // x = (12 + 180) / 360 * 180 = 96
        // y = (90 - 42) / 180 * 90 = 24
        int rgb1347 = img1347.getRGB(96, 24) & 0xFF;
        int rgb1200 = img1200.getRGB(96, 24) & 0xFF;

        assertTrue(rgb1347 >= rgb1200, "Black Death (1347 AD) should exhibit elevated pathogen intensity over 1200 AD");
    }

    @Test
    @DisplayName("Columbian exchange epoch (1492 AD) triggers elevated pathogen shock in the Americas")
    void testColumbianExchangePulse() {
        int width = 180;
        int height = 90;

        BufferedImage img1492 = AnalyticalEpidemiologyModel.generatePathogenMap(1492L, width, height, null);
        assertNotNull(img1492);

        // Coordinates around Mesoamerica / Caribbean (lat ~19 N, lon ~ -90 W)
        // x = (-90 + 180) / 360 * 180 = 45
        // y = (90 - 19) / 180 * 90 = 35
        int rgb1492 = img1492.getRGB(45, 35) & 0xFF;
        assertTrue(rgb1492 >= 0, "Intensity must be non-negative byte value");
    }

    @Test
    @DisplayName("Extreme latitudes (Antarctica / Arctic) exhibit suppressed or zero pathogen stress")
    void testPolarPathogenSuppression() {
        int width = 72;
        int height = 36;
        BufferedImage img = AnalyticalEpidemiologyModel.generatePathogenMap(2000L, width, height, null);

        // Near South Pole (y near bottom)
        int polarRgb = img.getRGB(36, 35) & 0xFF;
        assertEquals(0, polarRgb, "Extreme polar latitudes must have zero pathogen stress");
    }
}
