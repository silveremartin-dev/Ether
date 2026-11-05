/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * Chronological Epoch Dispersal Brackets and Oceanic Island Isolation Evaluator
 * for Plio-Pleistocene and Holocene hominin paleogeography (-3,000,000 BP to present).
 *
 * <p>Implements empirical archaeological colonization horizons (Out of Africa I/II,
 * Toba MIS 4 stadial, Sahul Madjedbebe arrival, Beringian standstill, Kelp Highway,
 * Younger Dryas Ice-Free Corridor, Saqqaq Greenland, and Austronesian/Polynesian voyaging).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HomininPaleoOccupancyCalculator
 * @see PaleoGeographyCoordinatesCatalog
 */
final class HomininEpochDispersalBrackets {

    private HomininEpochDispersalBrackets() {
        // Static utility class
    }

    /**
     * Determines whether a geographic point corresponds to an uninhabited isolated oceanic island
     * at a given historical year based on archaeological colonization dates.
     *
     * @param lon longitude coordinate in degrees
     * @param lat latitude coordinate in degrees
     * @param year historical epoch year
     * @return {@code true} if the point is an uninhabited oceanic island at {@code year}
     */
    static boolean isRemoteOceanicIsland(double lon, double lat, long year) {
        // 1. Remote Polynesia & Central/Eastern Pacific
        if (year < 1200) {
            if (lat >= 18.0 && lat <= 23.0 && lon >= -162.0 && lon <= -154.0) return true; // Hawaii
            if (lat >= -28.0 && lat <= -26.0 && lon >= -110.0 && lon <= -108.0) return true; // Easter Island
            if (lat >= -2.0 && lat <= 2.0 && lon >= -92.0 && lon <= -88.0) return true; // Galapagos
            if (lat >= 4.5 && lat <= 6.5 && lon >= -88.0 && lon <= -86.0) return true; // Cocos
            if (lat >= 3.0 && lat <= 4.8 && lon >= -82.5 && lon <= -80.5) return true; // Malpelo
            if (lat >= 9.0 && lat <= 11.5 && lon >= -110.5 && lon <= -108.0) return true; // Clipperton
            if (lat >= 17.5 && lat <= 20.0 && lon >= -115.5 && lon <= -109.5) return true; // Revillagigedo
            if (lat >= -34.5 && lat <= -25.5 && lon >= -82.0 && lon <= -78.0) return true; // Juan Fernandez
            if (year < 1280 && lat <= -34.0 && lat >= -48.0 && lon >= 165.0 && lon <= 180.0) return true; // New Zealand
            if (lon >= -180.0 && lon <= -120.0 && lat >= -30.0 && lat <= 30.0) return true;
            if (lon >= -150.0 && lon <= -130.0 && lat >= -30.0 && lat <= 0.0) return true;
        }
        if (year < -1200) {
            if (lon >= 155.0 && lon <= 180.0 && lat >= -25.0 && lat <= 20.0) return true;
            if (lon >= 135.0 && lon <= 165.0 && lat >= 5.0 && lat <= 22.0) return true;
        }

        // 2. Remote Atlantic Islands
        if (year < 1400) {
            if (lat >= -4.5 && lat <= -3.0 && lon >= -33.5 && lon <= -31.5) return true;
            if (lat >= -21.5 && lat <= -19.5 && lon >= -30.5 && lon <= -28.0) return true;
            if (lat >= 36.0 && lat <= 40.5 && lon >= -32.0 && lon <= -24.0) return true; // Azores
            if (lat >= 32.0 && lat <= 33.5 && lon >= -17.5 && lon <= -16.0) return true; // Madeira
            if (lat >= 14.5 && lat <= 17.5 && lon >= -25.5 && lon <= -22.5) return true; // Cape Verde
            if (lat >= -17.0 && lat <= -15.0 && lon >= -6.5 && lon <= -5.0) return true; // Saint Helena
            if (lat >= -8.5 && lat <= -7.5 && lon >= -15.0 && lon <= -13.5) return true; // Ascension
            if (lat >= -38.0 && lat <= -36.0 && lon >= -13.5 && lon <= -11.5) return true; // Tristan da Cunha
            if (lat >= -53.5 && lat <= -51.0 && lon >= -62.0 && lon <= -57.0) return true; // Falklands
            if (lat >= -55.0 && lat <= -53.5 && lon >= -39.0 && lon <= -35.0) return true; // South Georgia
            if (lat >= 31.5 && lat <= 33.0 && lon >= -65.5 && lon <= -64.0) return true; // Bermuda
            if (year < 874 && lat >= 63.0 && lat <= 67.0 && lon >= -25.0 && lon <= -12.0) return true; // Iceland
        }
        if (year < -1000) {
            if (lat >= 27.5 && lat <= 29.5 && lon >= -18.5 && lon <= -13.0) return true; // Canary Islands
        }

        // 3. Remote Indian Ocean Islands
        if (year < 500) {
            if (lat <= -11.5 && lat >= -26.0 && lon >= 43.0 && lon <= 51.0) return true; // Madagascar
            if (lat <= -19.5 && lat >= -22.0 && lon >= 55.0 && lon <= 64.0) return true; // Mascarenes
            if (lat >= -5.5 && lat <= -4.0 && lon >= 54.5 && lon <= 56.5) return true; // Seychelles
            if (lat >= -13.0 && lat <= -11.0 && lon >= 43.0 && lon <= 45.5) return true; // Comoros
            if (year < -500 && lat >= -8.0 && lat <= 8.0 && lon >= 71.0 && lon <= 74.5) return true; // Maldives & Chagos
            if (lat < -35.0 && lon >= 35.0 && lon <= 90.0) return true; // Sub-Antarctic
        }

        return false;
    }

    /**
     * Evaluates the continuous hominin occupancy coefficient in range \([0.0, 1.0]\)
     * across deep-time Pleistocene ice sheet boundaries and continental migration corridors.
     *
     * @param lon longitude coordinate in degrees
     * @param lat latitude coordinate in degrees
     * @param year historical epoch year
     * @return hominin occupancy weight \([0.0, 1.0]\)
     */
    static double getHomininOccupancyWeight(double lon, double lat, long year) {
        if (lat < -60.0) return 0.0; // Antarctica strictly uninhabited

        // Greenland strictly glaciated and uninhabited before Saqqaq colonization (~2500 BC / 4500 BP)
        if (year < -2500 && lat >= 58.0 && lon >= -75.0 && lon <= -10.0) {
            return 0.0;
        }

        // Remote oceanic islands filter
        if (isRemoteOceanicIsland(lon, lat, year)) {
            return 0.0;
        }

        if (year <= -2500000L) {
            // -3,000,000 BP (Late Pliocene / Australopithecus): Strictly confined to Sub-Saharan African cradle
            if (lon < 10.0 || lon > 45.0 || lat > 18.0 || lat < -35.0) return 0.0;
            boolean inRift = (lat >= -5.0 && lat <= 15.0 && lon >= 32.0 && lon <= 44.0);
            boolean inSouthAfrica = (lat >= -32.0 && lat <= -23.0 && lon >= 24.0 && lon <= 33.0);
            boolean inChad = (lat >= 12.0 && lat <= 18.0 && lon >= 15.0 && lon <= 21.0);
            if (inRift || inSouthAfrica || inChad) return 1.0;
            return 0.0;
        } else if (year <= -1500000L) {
            // -2,000,000 BP (Early Pleistocene / Homo erectus Out of Africa I)
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 45.0) return 0.0;
            if (lat >= -35.0 && lat <= 30.0 && lon >= -18.0 && lon <= 45.0) return 1.0;
            if (lat >= 27.0 && lat <= 42.5 && lon >= 33.0 && lon <= 48.0) return 1.0;
            if (lat >= -9.0 && lat <= 32.0 && lon >= 48.0 && lon <= 112.0) return 0.9;
            if (lat >= 22.0 && lat <= 41.0 && lon >= 100.0 && lon <= 122.0) return 0.8;
            return 0.0;
        } else if (year <= -500000L) {
            // -1,000,000 BP (Early/Mid-Pleistocene Acheulean): Africa, Southern/Western Europe, Southern & Eastern Asia
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 52.0) return 0.0;
            if (lat >= 35.0 && lat <= 52.0 && lon >= -10.0 && lon <= 30.0) return 0.85;
            if (lat <= 45.0 && lon >= -18.0 && lon <= 125.0) return 1.0;
            return 0.0;
        } else if (year <= -200000L) {
            // -300,000 BP (Jebel Irhoud, Archaic Sapiens, Pre-Neanderthals, Denisovans)
            if (lon < -25.0 || (lon > 115.0 && lat < -5.0) || lat > 55.0) return 0.0;
            return 1.0;
        } else if (year <= -85000L) {
            // -100,000 BP (MIS 5e Eemian Interglacial): Confined to Africa & Eurasia, Sahul & Americas strictly 0
            if (lon < -25.0 && lon > -170.0) return 0.0;
            if (lon > 110.0 && lat < -5.0) return 0.0;
            int species = HomininPaleoOccupancyCalculator.getHomininSpeciesType(lon, lat);
            if (species == 0) return 0.0;
            double maxLat = 56.0 + 3.0 * Math.sin((lon - 10.0) * Math.PI / 90.0) - (lon > 60.0 ? (lon - 60.0) * 0.05 : 0.0);
            maxLat = Math.clamp(maxLat, 48.0, 58.5);
            double fadeStart = maxLat - 5.0;
            if (lat > maxLat) return 0.0;
            if (lat > fadeStart) {
                double t = Math.clamp((lat - fadeStart) / (maxLat - fadeStart), 0.0, 1.0);
                return 1.0 - t * t * (3.0 - 2.0 * t);
            }
            return 1.0;
        } else if (year <= -65000L) {
            return evaluateMis4TobaAndSahulDispersal(lon, lat);
        } else if (year <= -52000L) {
            return evaluateEarlySahulDispersal(lon, lat);
        } else if (year <= -42000L) {
            // -52,000 to -42,000 BP (MIS 3 Sahul Colonization across interior)
            if (lon < -25.0 && lon > -170.0) return 0.0;
            if (lon > 110.0 && lat < -5.0) {
                return (lat < -39.0) ? 0.0 : 1.0;
            }
            return evaluateFennoscandiaMis3Margin(lon, lat, 50.0, 58.5);
        } else if (year <= -32000L) {
            // -42,000 to -32,000 BP (Late MIS 3 / Aurignacian / Tasmania firmly established)
            if (lon < -25.0 && lon > -170.0) return 0.0;
            return evaluateFennoscandiaMis3Margin(lon, lat, 50.0, 58.5);
        } else if (year <= -24000L) {
            return evaluatePreLgmBeringiaAndAmericas(lon, lat);
        } else if (year <= -16000L) {
            return evaluateLgmPeakOccupancy(lon, lat);
        } else if (year <= -13500L) {
            return evaluateDeglaciationPacificRoute(lon, lat);
        } else if (year <= -10500L) {
            // -13,500 to -10,500 BP (Younger Dryas, Ice-Free Corridor Open, Clovis/Folsom)
            double dLaur = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_YD);
            if (dLaur <= 0) return 0.0;
            double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

            double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_YD);
            if (dFenno <= 0) return 0.0;
            double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0; // Greenland inland ice
            return Math.min(wLaur, wFenno);
        } else if (year <= -8000L) {
            // -10,500 to -8,000 BP (Early Holocene Deglaciation / Maglemosian / PPNA)
            double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_EARLY_HOLOCENE);
            if (dFenno <= 0) return 0.0;
            if (lat > 60.0 && lon > -55.0 && lon < -18.0) return 0.0;
            if (lat > 75.0) return 0.0;
            return 1.0;
        } else {
            if (lat > 60.0 && lon > -55.0 && lon < -18.0 && year < -2500) return 0.0; // Greenland pre-Saqqaq
            if (lat > 80.0) return 0.0;
            return 1.0;
        }
    }

    private static double evaluateMis4TobaAndSahulDispersal(double lon, double lat) {
        if ((lon < -25.0 && lon > -170.0) || (lon > 110.0 && lat < -5.0)) {
            return 0.0;
        }
        int species = HomininPaleoOccupancyCalculator.getHomininSpeciesType(lon, lat);
        if (species == 0) return 0.0;

        double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
        if (dFenno <= 0) return 0.0;
        double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

        double dAsh = Math.exp(-(Math.pow(lat - 18.0, 2) / 120.0 + Math.pow(lon - 80.0, 2) / 220.0));
        double ashFactor = Math.max(0.08, 1.0 - dAsh * 0.88);

        if (lat > 52.0) return 0.0;
        if (lat > 46.0) {
            double t = Math.clamp((lat - 46.0) / 6.0, 0.0, 1.0);
            return Math.min(wFenno, (1.0 - t * t * (3.0 - 2.0 * t)) * ashFactor);
        }
        return wFenno * ashFactor;
    }

    private static double evaluateEarlySahulDispersal(double lon, double lat) {
        if (lon < -25.0 && lon > -170.0) return 0.0;
        if (lon > 110.0 && lat < -5.0) {
            if (lat < -20.0) return 0.0;
            double dMadj = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, 132.9, -12.5));
            double dKimb = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, 125.0, -16.0));
            if (dMadj <= 5.0 || dKimb <= 5.0) return 0.65;
            return 0.15;
        }
        return evaluateFennoscandiaMis3Margin(lon, lat, 48.0, 56.0);
    }

    private static double evaluateFennoscandiaMis3Margin(double lon, double lat, double fadeLat, double maxLat) {
        double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
        if (dFenno <= 0) return 0.0;
        double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));
        if (lat >= maxLat) return 0.0;
        if (lat > fadeLat) {
            double t = Math.clamp((lat - fadeLat) / (maxLat - fadeLat), 0.0, 1.0);
            return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
        }
        return wFenno;
    }

    private static double evaluatePreLgmBeringiaAndAmericas(double lon, double lat) {
        double dLaur = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_MIS3);
        if (dLaur <= 0) return 0.0;
        double wLaur = 1.0 / (1.0 + Math.exp(-dLaur / 1.5));

        double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
        if (dFenno <= 0) return 0.0;
        double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR) <= 5.5) {
            return Math.min(wLaur, 0.85);
        }
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY) <= 4.0) {
            return 0.50 * wLaur;
        }

        double dWhiteSands = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -106.3, 32.8));
        double dChiquihuite = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -103.5, 24.2));
        double dMeadowcroft = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -80.4, 40.3));
        double dFurada = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -42.5, -9.3));
        if (dWhiteSands <= 6.0 || dChiquihuite <= 6.0 || dMeadowcroft <= 5.0 || dFurada <= 6.0) {
            return 0.35 * wLaur;
        }

        if (lon >= -135.0 && lon <= -30.0) {
            return (lat <= 36.0 && lat >= -54.0) ? 0.10 * wLaur : 0.0;
        }

        if (lat >= 72.0) return 0.0;
        if (lat > 66.0) {
            double t = Math.clamp((lat - 66.0) / 6.0, 0.0, 1.0);
            return Math.min(wFenno, 1.0 - t * t * (3.0 - 2.0 * t));
        }
        return wFenno;
    }

    private static double evaluateLgmPeakOccupancy(double lon, double lat) {
        double dLaurentide = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM);
        if (dLaurentide <= 0) return 0.0;
        double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

        double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
        if (dFenno <= 0) return 0.0;
        double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

        if (HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_ALPS_LGM) <= 0) return 0.0;
        if (HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_PATAGONIA_LGM) <= 0) return 0.0;

        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR) <= 6.0) {
            return Math.min(wLaur, 0.85);
        }
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY) <= 4.5) {
            return 0.70 * wLaur;
        }
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR) <= 4.0) return 0.55;
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM) <= 4.0) return 0.45;
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA) <= 4.0) return 0.50;

        double dWhiteSands = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -106.3, 32.8));
        double dCactus = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -77.3, 36.8));
        double dPageLadson = Math.sqrt(HistoricalScenarioFeaturesCatalog.distSq(lon, lat, -83.9, 30.1));
        if (dWhiteSands <= 6.5 || dCactus <= 5.5 || dPageLadson <= 5.5) {
            return 0.45 * wLaur;
        }

        if (lon >= -135.0 && lon <= -30.0) {
            return (lat <= 38.0 && lat >= -54.0) ? 0.15 * wLaur : 0.0;
        }

        if (lat >= 72.0) return 0.0;
        double wArctic = 1.0;
        if (lat > 68.0) {
            double t = Math.clamp((lat - 68.0) / 4.0, 0.0, 1.0);
            wArctic = 1.0 - t * t * (3.0 - 2.0 * t);
        }
        return Math.min(wFenno, wArctic);
    }

    private static double evaluateDeglaciationPacificRoute(double lon, double lat) {
        double dLaurentide = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM);
        if (dLaurentide <= 0) return 0.0;
        double wLaur = 1.0 / (1.0 + Math.exp(-dLaurentide / 1.5));

        double dFenno = HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
        if (dFenno <= 0) return 0.0;
        double wFenno = 1.0 / (1.0 + Math.exp(-dFenno / 1.5));

        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR) <= 6.0) {
            return Math.min(wLaur, 0.90);
        }
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY) <= 5.5) {
            return 0.85 * wLaur;
        }
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR) <= 4.0) return 0.65;
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM) <= 4.0) return 0.55;
        if (HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA) <= 4.0) return 0.60;

        if (lon >= -135.0 && lon <= -30.0) {
            return (lat <= 44.0 && lat >= -54.0) ? 0.35 * wLaur : 0.0;
        }
        return Math.min(wFenno, 1.0);
    }
}
