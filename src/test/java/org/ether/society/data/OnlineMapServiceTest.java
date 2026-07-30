package org.ether.society.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OnlineMapServiceTest {

    private OnlineMapService service;

    @BeforeEach
    void setUp() {
        service = new OnlineMapService();
    }

    @Test
    void testCelestialBodiesUrls() {
        assertNotNull(OnlineMapService.CelestialBody.EARTH.getElevationWmsUrl());
        assertNotNull(OnlineMapService.CelestialBody.MARS.getElevationWmsUrl());
        assertNotNull(OnlineMapService.CelestialBody.MOON.getElevationWmsUrl());
        assertNotNull(OnlineMapService.CelestialBody.VENUS.getElevationWmsUrl());

        assertTrue(OnlineMapService.CelestialBody.EARTH.getElevationWmsUrl().toLowerCase().contains("nasa"));
        assertTrue(OnlineMapService.CelestialBody.MARS.getElevationWmsUrl().toLowerCase().contains("usgs.gov"));
    }

    @Test
    void testFetchNullOrEmptyUrlAsync() {
        var future = service.fetchMapFromUrlAsync("test_invalid.png", null);
        assertNotNull(future);
        assertNull(future.join());
    }
}
