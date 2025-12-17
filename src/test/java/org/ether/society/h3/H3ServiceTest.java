package org.ether.society.h3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for H3Service wrapper.
 */
class H3ServiceTest {

    private H3Service h3Service;

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8); // Resolution 8
    }

    @Test
    void testLatLngToCell() {
        // Paris coordinates
        double lat = 48.8566;
        double lng = 2.3522;

        long h3Index = h3Service.latLngToCell(lat, lng);

        assertNotEquals(0, h3Index);

        // Verify resolution by converting to string
        String h3String = h3Service.h3ToString(h3Index);
        assertTrue(h3String.startsWith("88"), "Resolution 8 prefix");
    }

    @Test
    void testCellToLatLng() {
        // Create a cell first
        double lat = 48.8566;
        double lng = 2.3522;

        long h3Index = h3Service.latLngToCell(lat, lng);
        com.uber.h3core.util.LatLng result = h3Service.cellToLatLng(h3Index);

        assertNotNull(result);

        // Should be near Paris (within hexagon size)
        assertTrue(result.lat > 48.0 && result.lat < 49.0);
        assertTrue(result.lng > 2.0 && result.lng < 3.0);
    }

    @Test
    void testRoundTripLatLngConversion() {
        double originalLat = 50.8503;
        double originalLng = 4.3517; // Brussels

        long h3Index = h3Service.latLngToCell(originalLat, originalLng);
        com.uber.h3core.util.LatLng converted = h3Service.cellToLatLng(h3Index);

        // Should be within 1km (resolution 8 hexagon size)
        assertEquals(originalLat, converted.lat, 0.01);
        assertEquals(originalLng, converted.lng, 0.01);
    }

    @Test
    void testGridDisk() {
        long center = h3Service.latLngToCell(48.8566, 2.3522); // Paris

        List<Long> neighbors = h3Service.gridDisk(center, 1);

        assertNotNull(neighbors);
        assertEquals(7, neighbors.size(), "Center + 6 neighbors");

        // Verify center is in the result
        assertTrue(neighbors.contains(center));
    }

    @Test
    void testGridDiskLargerRadius() {
        long center = h3Service.latLngToCell(48.8566, 2.3522);

        List<Long> neighbors = h3Service.gridDisk(center, 2);

        assertNotNull(neighbors);
        assertEquals(19, neighbors.size(), "k=2 → 19 cells (1 + 6 + 12)");
    }

    @Test
    void testH3ToString() {
        long h3Index = h3Service.latLngToCell(48.8566, 2.3522);

        String h3String = h3Service.h3ToString(h3Index);

        assertNotNull(h3String);
        assertEquals(15, h3String.length());
    }

    @Test
    void testDifferentResolutions() {
        double lat = 52.5200; // Berlin
        double lng = 13.4050;

        H3Service res6 = new H3Service(6);
        H3Service res8 = new H3Service(8);
        H3Service res10 = new H3Service(10);

        long h6 = res6.latLngToCell(lat, lng);
        long h8 = res8.latLngToCell(lat, lng);
        long h10 = res10.latLngToCell(lat, lng);

        // Different resolutions = different indices
        assertNotEquals(h6, h8);
        assertNotEquals(h8, h10);

        // Resolution prefix (via string conversion)
        assertTrue(res6.h3ToString(h6).startsWith("86"));
        assertTrue(res8.h3ToString(h8).startsWith("88"));
        assertTrue(res10.h3ToString(h10).startsWith("8a"));
    }

    @Test
    void testBoundaryConditions() {
        // North pole area
        long northPole = h3Service.latLngToCell(89.0, 0.0);
        assertNotEquals(0, northPole);

        // South pole area
        long southPole = h3Service.latLngToCell(-89.0, 0.0);
        assertNotEquals(0, southPole);

        // Antimeridian crossing
        long eastMeridian = h3Service.latLngToCell(0.0, 179.9);
        long westMeridian = h3Service.latLngToCell(0.0, -179.9);
        assertNotEquals(0, eastMeridian);
        assertNotEquals(0, westMeridian);
    }
}
