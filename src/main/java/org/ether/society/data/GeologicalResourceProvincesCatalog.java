/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Empirical metallogenic provinces, hydrocarbon basins, ancient surface outcrops, and Davies (2013)
 * spherical mantle heat-flow interpolation helper for {@link GeologicalResourceMapRasterizer}.
 *
 * <p>Compiles peer-reviewed geological coordinates from USGS MRDS, IAEA NFCIS, GEM Global Energy
 * Monitor, and Davies (2013) global surface heat-flow grids into reusable coordinate catalogs.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see GeologicalResourceMapRasterizer
 */
final class GeologicalResourceProvincesCatalog {

    private static final Logger logger = LoggerFactory.getLogger(GeologicalResourceProvincesCatalog.class);

    private GeologicalResourceProvincesCatalog() {
        // Static catalog and helper class
    }

    /** Historical and near-surface coal outcrops [lon, lat, radiusPx, intensity]. */
    static final double[][] SURFACE_COAL_OUTCROPS = {
        {113.0, 35.0, 34, 2.8},  // Henan & Shanxi
        {123.9, 41.9, 30, 2.6},  // Fushun / Liaoning
        {-2.5, 51.3, 26, 2.3},   // Somerset / Camerton
        {-1.6, 54.9, 26, 2.3},   // Newcastle / Northumberland
        {5.6, 50.6, 24, 2.2},    // Liège / Meuse Valley
        {7.0, 49.3, 24, 2.2},    // Sarre Basin
        {-79.5, 40.5, 36, 2.5},  // Appalachian Outcrops
        {7.2, 51.5, 32, 2.4}     // Ruhr Valley Outcrops
    };

    /** Giant crustal petroleum provinces [lon, lat, radiusPx, intensity]. */
    static final double[][] MAJOR_CRUSTAL_OIL_BASINS = {
        {49.5, 26.0, 42, 2.6},    // Ghawar / Rub' al Khali
        {48.0, 29.0, 38, 2.5},    // Burgan Field
        {49.0, 31.5, 38, 2.5},    // Marun / Ahvaz
        {-102.5, 31.8, 38, 2.4},  // Permian Basin
        {-92.0, 19.5, 36, 2.3},   // Cantarell / Campeche Basin
        {73.0, 61.0, 40, 2.5},    // Samotlor / West Siberian Oil Basin
        {2.0, 56.5, 32, 2.2},     // North Sea Central Graben
        {-149.0, 70.3, 34, 2.3},  // Prudhoe Bay
        {-71.5, 10.0, 36, 2.4},   // Maracaibo Basin
        {119.0, 38.0, 35, 2.3}    // Shengli / Bohai Bay
    };

    /** Historical natural bitumen and petroleum surface seeps [lon, lat, radiusPx, intensity]. */
    static final double[][] ANCIENT_BITUMEN_SPOTS = {
        {35.5, 31.5, 32, 2.8},   // Dead Sea
        {42.8, 33.6, 34, 2.9},   // Hit / Is on Euphrates
        {49.9, 40.4, 32, 2.7},   // Baku / Absheron Peninsula
        {44.3, 35.5, 30, 2.6},   // Kirkuk / Baba Gurgur
        {48.2, 32.2, 28, 2.4},   // Susa / Elam & Khuzestan
        {20.8, 37.7, 24, 2.2},   // Zakynthos / Keri
        {19.5, 40.7, 24, 2.2},   // Apollonia / Nymphaeum
        {103.6, 31.0, 28, 2.4},  // Dujiangyan / Sichuan
        {-61.6, 10.2, 30, 2.6},  // Pitch Lake
        {-70.9, 9.8, 30, 2.5},   // Mene Grande / Lake Maracaibo
        {-118.4, 34.1, 26, 2.2}  // La Brea / Carpinteria
    };

    /** Giant conventional and shale natural gas reservoirs [lon, lat, radiusPx, intensity]. */
    static final double[][] MAJOR_GAS_BASINS = {
        {52.0, 26.5, 42, 2.8},    // South Pars / North Dome
        {77.0, 66.0, 42, 2.6},    // Urengoy Field
        {75.0, 67.5, 40, 2.5},    // Yamburg Field
        {6.8, 53.3, 32, 2.3},     // Groningen Giant Gas Field
        {62.3, 36.5, 36, 2.4},    // Dauletabad / Galkynysh
        {3.3, 32.9, 35, 2.3},     // Hassi R'Mel Gas Field
        {-79.5, 41.0, 36, 2.4},   // Marcellus Shale Gas Basin
        {-93.5, 32.0, 32, 2.2},   // Haynesville Shale Gas
        {106.0, 30.5, 36, 2.4},   // Sichuan Gas Basin
        {34.0, 32.8, 30, 2.1},    // Leviathan & Tamar Basins
        {44.0, 73.0, 35, 2.2},    // Shtokman Gas Field
        {116.0, -19.5, 32, 2.2},  // Northwest Shelf Gas Basin
        {-120.0, 56.0, 32, 2.1},  // Montney Gas Basin
        {49.0, 51.5, 34, 2.3},    // Karachaganak Gas Field
        {103.5, 6.0, 30, 2.0}     // Gulf of Thailand Gas Basin
    };

    /** Historical eternal flames and ancient bamboo brine-gas drillings [lon, lat, radiusPx, intensity]. */
    static final double[][] ANCIENT_GAS_SPOTS = {
        {104.5, 30.0, 34, 2.8},  // Sichuan Basin
        {50.0, 40.5, 32, 2.7},   // Baku / Yanar Dag
        {30.5, 36.4, 26, 2.2},   // Mount Chimaera / Phaselis
        {44.3, 35.5, 28, 2.4}    // Baba Gurgur
    };

    /** IAEA NFCIS major uranium and thorium metallogenic provinces [lon, lat, radiusPx, intensity]. */
    static final double[][] IAEA_MAJOR_URANIUM_DEPOSITS = {
        {-105.0, 58.0, 38, 2.4},  // Athabasca Basin
        {136.9, -30.4, 36, 2.3},  // Olympic Dam
        {68.0, 44.0, 42, 2.5},    // Chu-Sarysu Basin
        {7.4, 18.7, 34, 2.2},     // Arlit / Tim Mersoï Basin
        {27.5, -26.2, 34, 2.2},   // Witwatersrand Basin
        {118.0, 50.0, 34, 2.2},   // Streltsovskoye Caldera
        {15.0, -22.5, 32, 2.0},   // Rössing & Husab
        {-108.5, 35.5, 30, 2.0},  // Grants Mineral Belt
        {132.8, -12.7, 32, 2.1},  // Ranger / Jabiluka
        {119.5, -23.5, 30, 1.9}   // Yeelirrie
    };

    /** Banded Iron Formations (BIFs) and porphyry copper giants [lon, lat, radiusPx, intensity]. */
    static final double[][] MAJOR_IRON_COPPER_METALS = {
        {118.0, -22.5, 40, 2.2},  // Pilbara / Hamersley BIFs
        {120.5, -23.0, 36, 2.0},  // Mount Whaleback
        {-50.0, -6.0, 42, 2.4},   // Carajás Iron Giant
        {-43.5, -20.0, 36, 1.8},  // Quadrilátero Ferrífero
        {37.0, 51.5, 40, 2.2},    // Kursk Magnetic Anomaly
        {33.5, 48.0, 36, 2.0},    // Krivoy Rog
        {-91.5, 47.5, 36, 1.9},   // Mesabi Iron Range
        {-66.5, 54.0, 38, 2.0},   // Labrador Trough
        {20.0, 67.8, 34, 1.9},    // Kiruna Magnetite
        {85.5, 22.0, 36, 1.9},    // Singhbhum Iron Ore Belt
        {-69.0, -24.0, 42, 2.3},  // Escondida
        {-69.5, -22.3, 40, 2.2},  // Chuquicamata
        {-70.5, -34.0, 38, 2.0},  // El Teniente
        {137.0, -4.0, 38, 2.1},   // Grasberg
        {-111.0, 33.5, 34, 1.8},  // Morenci & Arizona Copper Basin
        {28.0, -12.5, 38, 2.1}    // Central African Copperbelt
    };

    /** Archaeological Chalcolithic, Bronze Age, and Iron Age smelting districts [lon, lat, radiusPx, intensity]. */
    static final double[][] ANCIENT_IRON_COPPER_METALS = {
        {-6.56, 37.69, 34, 2.8},  // Rio Tinto
        {32.9, 35.0, 32, 2.7},    // Cyprus / Troodos Mountains
        {10.3, 42.8, 30, 2.6},    // Elba & Populonia
        {14.9, 47.5, 30, 2.6},    // Noricum / Erzberg
        {114.0, 34.5, 34, 2.8},   // Han Dynasty Iron Monopolies
        {8.0, 9.5, 28, 2.4},      // Nok Culture / Taruga
        {75.8, 28.0, 30, 2.5},    // Khetri Copper Belt
        {35.4, 30.6, 28, 2.4},    // Faynan & Timna
        {24.06, 37.71, 26, 2.2}   // Laurion / Attica
    };

    /** Global precious metal (Au, Ag, PGE) metallogenic provinces [lon, lat, radiusPx, intensity]. */
    static final double[][] PRECIOUS_METALS_PROVINCES = {
        {-6.77, 42.46, 12.0, 4.2},  // Las Médulas
        {-6.50, 42.55, 9.0, 3.5},   // El Teleno
        {-8.10, 42.15, 8.0, 2.8},   // Galicia / Sil River
        {-6.56, 37.69, 14.0, 4.0},  // Rio Tinto
        {-6.20, 37.75, 10.0, 3.2},  // Aznalcóllar
        {-4.84, 38.77, 11.0, 3.6},  // Almadén
        {-5.90, 38.30, 9.0, 3.0},   // Ossa Morena
        {-6.60, 40.20, 8.0, 2.6},   // Tagus Basin
        {24.06, 37.71, 12.0, 4.2},  // Laurion
        {24.20, 40.90, 11.0, 3.8},  // Mount Pangaeon
        {23.13, 46.30, 14.0, 4.5},  // Roșia Montană
        {22.85, 46.10, 10.0, 3.5},  // Brad / Săcărâmb
        {21.43, 42.62, 11.0, 3.6},  // Novo Brdo
        {22.18, 42.08, 9.0, 3.0},   // Kratovo
        {24.70, 36.97, 7.0, 2.6},   // Siphnos
        {8.03, 45.55, 10.0, 3.4},   // Bessa
        {7.30, 45.74, 8.0, 2.8},    // Val d'Aosta
        {33.50, 22.00, 15.0, 4.5},  // Wadi Allaqi
        {33.58, 25.99, 12.0, 3.8},  // Wadi Hammamat
        {34.80, 24.95, 13.0, 4.0},  // Sukari
        {33.80, 24.20, 10.0, 3.2},  // Barramiya
        {40.87, 23.50, 14.0, 4.2},  // Mahd adh Dhahab
        {42.50, 20.00, 10.0, 3.0},  // Asir
        {28.04, 38.49, 12.0, 4.0},  // Sardis / Pactolus River
        {42.60, 42.25, 11.0, 3.6},  // Colchis
        {44.38, 41.38, 9.0, 3.2},   // Sakdrisi
        {64.60, 41.50, 18.0, 5.0},  // Muruntau
        {67.50, 39.50, 14.0, 4.0},  // Zeravshan Valley
        {78.27, 12.96, 13.0, 4.2},  // Kolar Gold Fields
        {76.65, 16.20, 10.0, 3.4},  // Hutti Gold Mines
        {85.00, 50.00, 14.0, 4.0},  // Altai Mountains
        {120.40, 37.36, 15.0, 4.5}, // Zhaoyuan
        {117.81, 30.93, 12.0, 3.6}, // Tongling
        {112.50, 23.00, 10.0, 3.0}, // Lingnan
        {138.30, 38.00, 9.0, 3.2},  // Sado Island
        {-11.50, 13.50, 14.0, 4.0}, // Bambouk
        {-9.50, 11.50, 13.0, 3.8},  // Bure
        {-1.67, 6.20, 16.0, 4.6},   // Ashanti
        {30.00, -20.00, 13.0, 3.8}, // Great Zimbabwe
        {27.00, -26.50, 22.0, 5.5}, // Witwatersrand
        {29.00, -24.50, 18.0, 4.8}, // Bushveld
        {-76.60, 5.70, 14.0, 4.2},  // Chocó
        {-79.50, -6.70, 12.0, 3.8}, // Lambayeque
        {-70.20, -14.20, 13.0, 4.0},// Carabaya
        {-65.75, -19.58, 18.0, 5.2},// Cerro Rico / Potosí
        {-102.58, 22.77, 16.0, 4.6},// Zacatecas
        {-116.00, 40.80, 16.0, 4.5},// Carlin Trend
        {121.50, -30.75, 16.0, 4.5},// Kalgoorlie Golden Mile
        {150.80, 62.50, 16.0, 4.4}, // Kolyma
        {88.20, 69.30, 18.0, 4.8},  // Norilsk-Talnakh
        {-81.00, 46.50, 14.0, 4.0}, // Sudbury Basin
        {-78.50, -7.00, 15.0, 4.2}, // Yanacocha
        {137.10, -4.05, 16.0, 4.5}  // Grasberg
    };

    /** Rare earth elements (REE) carbonatite complexes and lithium salar brines [lon, lat, radiusPx, intensity]. */
    static final double[][] MAJOR_REE_DEPOSITS = {
        {109.9, 41.8, 55, 3.0},   // Bayan Obo
        {-115.5, 35.5, 42, 2.6},  // Mountain Pass
        {122.5, -28.7, 45, 2.7},  // Mount Weld
        {-46.0, 60.9, 45, 2.6},   // Kvanefjeld
        {116.5, 71.0, 45, 2.6},   // Tomtor
        {34.6, 67.8, 40, 2.4},    // Lovozero
        {115.0, 25.5, 50, 2.8},   // Ganzhou
        {103.5, 22.4, 38, 2.3},   // Dong Pao
        {-46.9, -19.6, 42, 2.5},  // Araxá
        {-67.5, -21.0, 52, 2.8},  // Salar de Atacama
        {-68.0, -23.5, 50, 2.7},  // Salar de Uyuni
        {116.0, -33.8, 42, 2.5},  // Greenbushes
        {14.6, 58.1, 35, 2.2},    // Norra Kärr
        {20.2, 67.8, 38, 2.3},    // Kiruna / Per Geijer
        {-64.2, 56.3, 40, 2.4},   // Strange Lake
        {-112.6, 62.1, 38, 2.3}   // Nechalacho
    };

    /**
     * Ingests the empirical 2-degree Davies (2013) global heat-flow CSV grid via continuous 2D bilinear
     * interpolation, or falls back to major volcanic/rift geothermal hotspots if the CSV is absent.
     *
     * @return rasterized 2048x1024 grayscale mantle heat-flow {@link BufferedImage}
     */
    static BufferedImage rasterizeDaviesMantleHeatFlow() {
        Path csvPath = Paths.get("data", "maps", "ihfc_davies2013", "heat_flow_2deg.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("data", "maps", "heat_flow_2deg.csv");
        }

        if (Files.exists(csvPath)) {
            try {
                int width = 2048, height = 1024;
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                float[][] grid = new float[90][180];
                boolean[][] filled = new boolean[90][180];

                List<String> lines = Files.readAllLines(csvPath);
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        try {
                            double lon = Double.parseDouble(parts[0].trim());
                            double lat = Double.parseDouble(parts[1].trim());
                            double val = Double.parseDouble(parts[2].trim());

                            int gx = (int) Math.clamp(Math.floor((lon + 180.0) / 2.0), 0, 179);
                            int gy = (int) Math.clamp(Math.floor((90.0 - lat) / 2.0), 0, 89);
                            grid[gy][gx] = (float) val;
                            filled[gy][gx] = true;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                for (int gy = 0; gy < 90; gy++) {
                    for (int gx = 0; gx < 180; gx++) {
                        if (!filled[gy][gx] || grid[gy][gx] <= 0) {
                            grid[gy][gx] = 65.0f; // Global mean continental heat flow baseline (mW/m²)
                        }
                    }
                }

                for (int y = 0; y < height; y++) {
                    double lat = 90.0 - (y / (double) height) * 180.0;
                    double gyDouble = Math.clamp(((90.0 - lat) / 180.0) * 90.0 - 0.5, 0.0, 88.999);
                    int gy0 = (int) Math.floor(gyDouble);
                    int gy1 = Math.min(89, gy0 + 1);
                    double fy = gyDouble - gy0;

                    for (int x = 0; x < width; x++) {
                        double lon = -180.0 + (x / (double) width) * 360.0;
                        double gxDouble = ((lon + 180.0) / 360.0) * 180.0 - 0.5;
                        if (gxDouble < 0) gxDouble += 180.0;
                        int gx0 = (int) Math.floor(gxDouble) % 180;
                        int gx1 = (gx0 + 1) % 180;
                        double fx = gxDouble - Math.floor(gxDouble);

                        float hf00 = grid[gy0][gx0];
                        float hf10 = grid[gy0][gx1];
                        float hf01 = grid[gy1][gx0];
                        float hf11 = grid[gy1][gx1];

                        float hfInterp = (float) ((1.0 - fx) * (1.0 - fy) * hf00 + fx * (1.0 - fy) * hf10 + (1.0 - fx) * fy * hf01 + fx * fy * hf11);
                        if (hfInterp <= 0) hfInterp = 65.0f;

                        double norm = Math.clamp((hfInterp - 35.0) / 110.0, 0.0, 1.0);
                        int gray = (int) Math.clamp(norm * 255.0, 0, 255);
                        img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    }
                }
                logger.info("Successfully generated 9th geology tensor (Mantle Heat Flux) with seamless 2D Bilinear Interpolation from Davies 2013 CSV.");
                return img;
            } catch (Exception e) {
                logger.warn("Could not parse Davies 2013 heat flow CSV: {}", e.getMessage());
            }
        }

        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double[][] spots = {
            {-155.5, 19.8, 30, 1.2}, {-178.0, -29.0, 45, 1.1}, {-72.0, -15.0, 60, 1.2},
            {140.0, 36.0, 50, 1.1}, {43.0, 11.5, 40, 1.2}, {-25.0, 64.8, 45, 1.1}, {14.0, 40.8, 35, 1.0}
        };
        var list = new ArrayList<double[]>();
        for (double[] s : spots) list.add(s);
        GeologicalResourceMapRasterizer.rasterizeSpotListToAlpha(img, list, Color.WHITE, 15.0);
        return img;
    }
}
