/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.image.BufferedImage;

/**
 * <h1>HistoricalInstitutionalRasterizer</h1>
 * <p>
 * Cartographic rasterizer for institutional complexity (Seshat Global History Databank),
 * ecological footprint, and epidemiological pathogen immunity tensors.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalInstitutionalRasterizer {

    private HistoricalInstitutionalRasterizer() {
        // Utility class
    }

    /**
     * Rasterizes the institutional complexity tensor map (Seshat global dataset).
     *
     * @param type     layer type descriptor
     * @param scenario scenario specifying epoch and parameters
     * @return 2D rasterized institutional complexity image
     */
    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernInstitutionalMap(
                    year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) / HistoricalMapGenerator.WIDTH), 0, (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) / HistoricalMapGenerator.HEIGHT), 0, (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -10000L) {
                    double socVal = 10.0;
                    if (year <= -70000L) {
                        socVal = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 14.0, 10.0, 8.0);
                    } else if (year <= -40000L) {
                        socVal = 12.0;
                    } else if (year <= -22000L) {
                        double gravettianArt = Math.exp(-(Math.pow(lat - 48.0, 2) + Math.pow(lon - 16.0, 2)) / 180.0);
                        socVal = 12.0 + gravettianArt * 28.0;
                    } else if (year <= -12000L) {
                        double magdalenian = Math.exp(-(Math.pow(lat - 45.0, 2) + Math.pow(lon - 1.0, 2)) / 120.0);
                        socVal = 15.0 + magdalenian * 35.0;
                    } else {
                        double gobekliTepe = Math.exp(-(Math.pow(lat - 37.2, 2) + Math.pow(lon - 38.9, 2)) / 45.0);
                        double natufian = Math.exp(-(Math.pow(lat - 32.5, 2) + Math.pow(lon - 35.5, 2)) / 35.0);
                        socVal = 18.0 + gobekliTepe * 70.0 + natufian * 45.0;
                    }
                    int gray = Math.clamp((int) (socVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                double complexity = 15.0;
                double egypt = Math.exp(-(Math.pow(lat - 27.0, 2) + Math.pow(lon - 30.5, 2)) / 60.0);
                double mesopotamia = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lon - 44.0, 2)) / 50.0);
                double indus = Math.exp(-(Math.pow(lat - 26.0, 2) + Math.pow(lon - 68.0, 2)) / 60.0);
                double china = Math.exp(-(Math.pow(lat - 34.5, 2) + Math.pow(lon - 110.0, 2)) / 80.0);
                double rome = Math.exp(-(Math.pow(lat - 41.9, 2) + Math.pow(lon - 12.5, 2)) / 75.0);
                double greece = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 23.5, 2)) / 45.0);
                double mesoamerica = Math.exp(-(Math.pow(lat - 19.5, 2) + Math.pow(lon - (-99.0), 2)) / 50.0);
                double andes = Math.exp(-(Math.pow(lat - (-12.0), 2) + Math.pow(lon - (-77.0), 2)) / 50.0);

                if (year >= -3000L && year < -500L) {
                    complexity += (egypt * 180.0 + mesopotamia * 190.0 + indus * 150.0 + china * 160.0);
                } else if (year >= -500L && year < 500L) {
                    complexity += (rome * 220.0 + greece * 180.0 + china * 210.0 + egypt * 150.0 + mesopotamia * 160.0 + indus * 150.0 + mesoamerica * 120.0);
                } else if (year >= 500L && year < 1500L) {
                    double baghdad = Math.exp(-(Math.pow(lat - 33.3, 2) + Math.pow(lon - 44.4, 2)) / 60.0);
                    double constantinople = Math.exp(-(Math.pow(lat - 41.0, 2) + Math.pow(lon - 28.9, 2)) / 45.0);
                    double songChina = Math.exp(-(Math.pow(lat - 31.0, 2) + Math.pow(lon - 118.0, 2)) / 100.0);
                    complexity += (songChina * 230.0 + baghdad * 210.0 + constantinople * 190.0 + mesoamerica * 140.0 + andes * 130.0);
                } else {
                    double nwEurope = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 5.0, 2)) / 90.0);
                    complexity += (nwEurope * 240.0 + china * 210.0 + mesoamerica * 130.0 + andes * 120.0);
                }

                int gray = Math.clamp((int) (complexity * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }

    /**
     * Rasterizes the global ecological footprint tensor map.
     *
     * @param type     layer type descriptor
     * @param scenario scenario specifying epoch and parameters
     * @return 2D rasterized ecological footprint image
     */
    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernEcologicalMap(
                    year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;

                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) / HistoricalMapGenerator.WIDTH), 0, (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) / HistoricalMapGenerator.HEIGHT), 0, (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (land == 0 || occWeight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                if (year <= -10000L) {
                    double ecoVal = 20.0;
                    if (year <= -70000L) {
                        ecoVal = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 26.0, 20.0, 18.0);
                    } else if (year <= -40000L) {
                        double sahulFire = Math.exp(-(Math.pow(lat - (-22.0), 2) + Math.pow(lon - 135.0, 2)) / 250.0);
                        ecoVal = 22.0 + sahulFire * 45.0;
                    } else if (year <= -18000L) {
                        double mammothHunt = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 30.0, 2)) / 300.0);
                        ecoVal = 22.0 + mammothHunt * 35.0;
                    }
                    int gray = Math.clamp((int) (ecoVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                double ecoStrain = 22.0;
                double mesopotamia = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lon - 44.0, 2)) / 45.0);
                double meditteranean = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 15.0, 2)) / 120.0);
                double yellowRiver = Math.exp(-(Math.pow(lat - 35.0, 2) + Math.pow(lon - 114.0, 2)) / 65.0);
                double ganges = Math.exp(-(Math.pow(lat - 26.0, 2) + Math.pow(lon - 82.0, 2)) / 60.0);
                double nwEurope = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 60.0);
                double mesoamerica = Math.exp(-(Math.pow(lat - 19.0, 2) + Math.pow(lon - (-99.0), 2)) / 40.0);
                double andes = Math.exp(-(Math.pow(lat - (-13.0), 2) + Math.pow(lon - (-72.0), 2)) / 40.0);

                ecoStrain += (mesopotamia * 180.0 + yellowRiver * 170.0 + ganges * 160.0 +
                              meditteranean * 140.0 + nwEurope * 135.0 + mesoamerica * 120.0 + andes * 120.0);

                int gray = Math.clamp((int) (ecoStrain * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }

    /**
     * Rasterizes the epidemiological pathogen immunity tensor map.
     *
     * @param type     layer type descriptor
     * @param scenario scenario specifying epoch and parameters
     * @return 2D rasterized pathogen immunity image
     */
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
        BufferedImage img = AnalyticalEpidemiologyModel.generatePathogenMap(
                year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }
}
