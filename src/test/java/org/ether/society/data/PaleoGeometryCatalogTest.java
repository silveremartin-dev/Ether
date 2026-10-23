/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for PaleoGeometryCatalog externalized geometry loading.
 */
public class PaleoGeometryCatalogTest {

    @Test
    public void testLoadIceSheets() {
        Map<String, double[][]> iceSheets = PaleoGeometryCatalog.loadIceSheets();
        assertNotNull(iceSheets, "Ice sheets map must not be null");
        assertFalse(iceSheets.isEmpty(), "Ice sheets map must contain entries");
        assertTrue(iceSheets.containsKey("POLY_FENNOSCANDIA_LGM"), "Must contain Fennoscandia polygon");
        assertTrue(iceSheets.containsKey("POLY_LAURENTIDE_LGM"), "Must contain Laurentide polygon");
        assertTrue(iceSheets.get("POLY_FENNOSCANDIA_LGM").length > 0, "Fennoscandia must have vertices");
    }

    @Test
    public void testLoadHomininHearths() {
        Map<String, double[][]> hearths = PaleoGeometryCatalog.loadHomininHearths();
        assertNotNull(hearths, "Hominin hearths map must not be null");
        assertTrue(hearths.containsKey("HEARTHS_SAPIENS_AFRICA"), "Must contain Sapiens hearths");
        assertTrue(hearths.containsKey("HEARTHS_NEANDERTHAL_WEST"), "Must contain Neanderthal hearths");
        assertTrue(hearths.get("HEARTHS_SAPIENS_AFRICA").length > 0, "Sapiens hearths must not be empty");
    }

    @Test
    public void testLoadMigrationRoutes() {
        Map<String, double[][]> routes = PaleoGeometryCatalog.loadMigrationRoutes();
        assertNotNull(routes, "Migration routes map must not be null");
        assertTrue(routes.containsKey("ROUTE_PACIFIC_KELP_HIGHWAY"), "Must contain Kelp Highway route");
        assertTrue(routes.containsKey("ROUTE_ICE_FREE_CORRIDOR"), "Must contain Ice Free Corridor route");
    }

    @Test
    public void testDelegationThroughPaleoGeographyCoordinatesCatalog() {
        assertNotNull(PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
        assertTrue(PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM.length > 0);
        assertNotNull(PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_AFRICA);
        assertTrue(PaleoGeographyCoordinatesCatalog.HEARTHS_SAPIENS_AFRICA.length > 0);
        assertNotNull(PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY);
        assertTrue(PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY.length > 0);
    }
}
