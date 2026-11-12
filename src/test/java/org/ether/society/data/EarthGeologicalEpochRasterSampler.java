/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.ether.society.data.PlanetaryRasterSampler.HEIGHT;
import static org.ether.society.data.PlanetaryRasterSampler.WIDTH;
import static org.ether.society.data.PlanetaryRasterSampler.fastParseCsv;
import static org.ether.society.data.PlanetaryRasterSampler.rasterizeAlphaDensity;
import static org.ether.society.data.PlanetaryRasterSampler.rasterizeBasinsAndSpots;
import static org.ether.society.data.PlanetaryRasterSampler.rasterizeTieredDensity;
import static org.ether.society.data.PlanetaryRasterSampler.saveMapImage;

/**
 * Geological and mineral tensor raster generation helper for Earth epochs.
 *
 * <p>Ingests USGS MRDS mineral records and models global coal, crude oil, natural gas,
 * uranium, iron/copper, precious metals, and UNESCO WHYMAP aquifers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class EarthGeologicalEpochRasterSampler {

    private static final Logger logger = LoggerFactory.getLogger(EarthGeologicalEpochRasterSampler.class);

    private EarthGeologicalEpochRasterSampler() {
        // Utility class
    }

    static List<double[]> extractMrdsDeposits(String... keywords) {
        List<double[]> list = new ArrayList<>();
        Path zipPath = Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!Files.exists(zipPath)) return list;

        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry("mrds.csv");
            if (entry == null) return list;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8), 65536)) {
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    List<String> parts = fastParseCsv(line);
                    if (parts.size() > 17) {
                        try {
                            double lat = Double.parseDouble(parts.get(5).replace("\"", "").trim());
                            double lon = Double.parseDouble(parts.get(6).replace("\"", "").trim());
                            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue;

                            String comms = (parts.get(11) + " " + parts.get(12) + " " + parts.get(13) + " " + parts.get(14)).toLowerCase();
                            boolean match = false;
                            for (String kw : keywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }

                            if (match) {
                                String prodSize = parts.get(17).toUpperCase();
                                double weight = 1.0;
                                double radius = 5.0;
                                if (prodSize.contains("L") || prodSize.contains("Y")) {
                                    weight = 2.5;
                                    radius = 12.0;
                                } else if (prodSize.contains("M")) {
                                    weight = 1.8;
                                    radius = 8.0;
                                } else if (prodSize.contains("S")) {
                                    weight = 1.2;
                                    radius = 6.0;
                                }
                                list.add(new double[]{lon, lat, radius, weight});
                            }
                        } catch (Exception ignored) { /* harmless test raster write */ }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading MRDS CSV: {}", e.getMessage());
        }
        return list;
    }

    static void generateEarthResourcesForEpoch(long epoch) {
        logger.info("Generating Earth Geological & Mineral Tensors for epoch {}...", epoch);

        double depletionMultiplier = 1.0;
        if (epoch > 1800L) {
            double progress = (epoch - 1800.0) / (2026.0 - 1800.0);
            depletionMultiplier = Math.clamp(1.0 - progress * 0.35, 0.65, 1.0);
        }

        // 1. COAL BASINS & DEPOSITS
        BufferedImage imgCoal = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<PlanetaryRasterSampler.GeologicalBasin> coalBasins = createCoalBasins(depletionMultiplier);
        List<double[]> coalSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        rasterizeBasinsAndSpots(imgCoal, coalBasins, coalSpots, 6.0);
        saveMapImage(imgCoal, "earth_coal.png", "earth", epoch);

        // 2. CRUDE OIL BASINS & WELLS
        BufferedImage imgOil = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<PlanetaryRasterSampler.GeologicalBasin> oilBasins = createOilBasins(depletionMultiplier);
        List<double[]> oilSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        rasterizeBasinsAndSpots(imgOil, oilBasins, oilSpots, 6.0);
        saveMapImage(imgOil, "earth_oil.png", "earth", epoch);

        // 3. NATURAL GAS BASINS & FIELDS
        BufferedImage imgGas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<PlanetaryRasterSampler.GeologicalBasin> gasBasins = createGasBasins(depletionMultiplier);
        List<double[]> gasSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        rasterizeBasinsAndSpots(imgGas, gasBasins, gasSpots, 6.0);
        saveMapImage(imgGas, "earth_gas.png", "earth", epoch);

        // 4. Uranium Deposits
        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> uSpots = extractMrdsDeposits("uranium", "thorium", "uraninite", "pitchblende", "carnotite");
        double[][] uBasins = {
            {-105.0, 58.0, 42, 2.8}, {136.9, -30.4, 38, 2.8}, {68.0, 44.0, 48, 3.0},
            {7.4, 18.7, 36, 2.4}, {27.5, -26.2, 36, 2.4}, {118.0, 50.0, 38, 2.4},
            {15.0, -22.5, 38, 2.4}, {132.8, -12.7, 35, 2.2}, {-109.5, 38.5, 38, 2.2}
        };
        for (double[] b : uBasins) uSpots.add(b);
        rasterizeAlphaDensity(imgUranium, uSpots, 8.0);
        saveMapImage(imgUranium, "earth_uranium.png", "earth", epoch);

        // 5. Helium-3 (0 on Earth)
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(imgHe3, "earth_helium3.png", "earth", epoch);

        // 6. Iron & Copper Formations
        BufferedImage imgIronCopper = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> feCuSpots = extractMrdsDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite", "bornite", "taconite");
        double[][] majorFeCu = {
            {118.0, -22.5, 45, 2.8}, {120.5, -23.0, 40, 2.5}, {-50.0, -6.0, 48, 3.0}, {-43.5, -20.0, 40, 2.4},
            {37.0, 51.5, 48, 2.8}, {33.5, 48.0, 42, 2.5}, {-91.5, 47.5, 40, 2.4}, {-66.5, 54.0, 42, 2.4},
            {20.0, 67.8, 35, 2.2}, {85.5, 22.0, 42, 2.4}, {-69.0, -24.0, 48, 2.8}, {-69.5, -22.3, 45, 2.6},
            {-70.5, -34.0, 45, 2.6}, {137.0, -4.0, 40, 2.5}, {-111.0, 33.5, 38, 2.2}, {28.0, -12.5, 45, 2.6},
            {102.0, 25.0, 38, 2.2}, {88.0, 38.0, 32, 2.0}, {-108.0, 32.5, 32, 2.0}
        };
        for (double[] b : majorFeCu) feCuSpots.add(b);
        rasterizeTieredDensity(imgIronCopper, feCuSpots, 8.0);
        saveMapImage(imgIronCopper, "earth_iron_copper.png", "earth", epoch);

        // 7-10. Precious metals, rare earths, geothermal, aquifers
        saveOptionalMap(HistoricalMapGenerator.rasterizePreciousMetalsMap("EARTH", null), "earth_precious_metals.png", epoch);
        saveOptionalMap(HistoricalMapGenerator.rasterizeRareEarthsMap("EARTH", null), "earth_rare_earths.png", epoch);
        saveOptionalMap(HistoricalMapGenerator.rasterizeMantleHeatMap("EARTH", null), "earth_geothermal.png", epoch);
        saveOptionalMap(HistoricalMapGenerator.rasterizeAquiferMap("EARTH", null), "earth_aquifers.png", epoch);
    }

    private static void saveOptionalMap(BufferedImage img, String filename, long epoch) {
        if (img != null) {
            saveMapImage(img, filename, "earth", epoch);
        }
    }

    private static List<PlanetaryRasterSampler.GeologicalBasin> createCoalBasins(double d) {
        List<PlanetaryRasterSampler.GeologicalBasin> b = new ArrayList<>();
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-80.0, 38.5, 6.5, 2.2, 45.0, 3.8 * d, "Appalachian Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-89.0, 38.5, 3.5, 2.8, 0.0, 3.2 * d, "Illinois Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-105.8, 44.5, 3.2, 2.0, 115.0, 4.2 * d, "Powder River Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-102.5, 47.5, 3.0, 2.2, 0.0, 2.8 * d, "Williston Fort Union Lignite"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-95.0, 36.0, 3.5, 2.0, 40.0, 2.6 * d, "Western Interior / Arkoma"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-95.5, 31.5, 5.5, 1.2, 60.0, 2.4 * d, "Gulf Coast Wilcox Lignite"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-107.5, 41.5, 2.5, 1.8, 0.0, 2.8 * d, "Green River / Hanna Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-109.5, 39.5, 2.8, 1.8, 120.0, 2.8 * d, "Uinta-Piceance Coal"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-108.0, 36.5, 2.2, 1.8, 0.0, 3.0 * d, "San Juan Fruitland Coal"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-116.5, 53.0, 6.0, 2.0, 135.0, 3.4 * d, "Alberta Foothills Coal"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-60.2, 46.2, 1.5, 1.0, 60.0, 2.4 * d, "Sydney Basin Nova Scotia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-151.0, 61.5, 2.5, 1.2, 30.0, 2.4 * d, "Cook Inlet Beluga Alaska"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-101.5, 27.8, 2.0, 1.2, 130.0, 2.4 * d, "Sabinas Basin Mexico"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-72.7, 11.1, 2.2, 1.0, 45.0, 3.8 * d, "Cerrejon Colombia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-73.5, 5.5, 2.0, 1.0, 35.0, 2.8 * d, "Boyaca-Cundinamarca Colombia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-72.3, 10.9, 1.5, 0.8, 40.0, 2.8 * d, "Guasare Basin Venezuela"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-51.5, -29.5, 4.5, 2.0, 90.0, 3.0 * d, "Parana Basin Brazil"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-72.3, -51.5, 1.8, 1.0, 0.0, 2.4 * d, "Rio Turbio Argentina"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-77.0, -10.5, 1.5, 0.8, 140.0, 2.2 * d, "Oyon Basin Peru"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(19.0, 50.2, 2.2, 1.6, 120.0, 4.0 * d, "Upper Silesian Basin Poland/Czechia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(23.0, 51.3, 1.8, 1.0, 135.0, 2.8 * d, "Lublin Coal Basin Poland"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(7.3, 51.5, 2.0, 1.2, 70.0, 3.8 * d, "Ruhr Basin Germany"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(6.8, 49.3, 1.5, 0.8, 60.0, 2.8 * d, "Saar-Lorraine Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(6.5, 50.9, 1.2, 0.8, 135.0, 3.5 * d, "Rhineland Lignite District"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(13.5, 51.6, 2.5, 1.5, 0.0, 3.2 * d, "Lusatian / Central German Lignite"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(3.0, 50.4, 2.5, 0.6, 80.0, 2.8 * d, "Nord-Pas-de-Calais France/Belgium"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-1.3, 53.5, 2.0, 1.2, 0.0, 3.0 * d, "Yorkshire / East Midlands UK"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-3.6, 51.7, 1.5, 0.8, 90.0, 2.8 * d, "South Wales Coalfield"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-3.8, 55.9, 1.2, 0.6, 70.0, 2.4 * d, "Scottish Central Coalfield"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-5.8, 43.3, 1.5, 0.8, 90.0, 2.6 * d, "Asturias Basin Spain"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(23.3, 45.4, 1.0, 0.5, 90.0, 2.6 * d, "Jiu Valley Romania"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(26.0, 42.2, 1.5, 1.0, 90.0, 2.8 * d, "Maritsa Iztok Lignite Bulgaria"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(21.7, 40.5, 1.5, 0.8, 140.0, 2.6 * d, "Ptolemaida-Florina Greece"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(20.3, 44.4, 1.8, 1.0, 120.0, 2.8 * d, "Kolubara-Kostolac Serbia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(31.8, 41.4, 1.5, 0.8, 75.0, 2.8 * d, "Zonguldak Basin Turkey"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(27.6, 39.2, 1.2, 0.8, 45.0, 2.6 * d, "Soma Lignite Basin Turkey"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(38.2, 48.2, 4.5, 1.8, 110.0, 4.2 * d, "Donbas"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(87.0, 54.5, 3.8, 2.2, 160.0, 4.8 * d, "Kuzbass"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(93.5, 56.0, 6.5, 2.0, 80.0, 4.4 * d, "Kansk-Achinsk"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(98.0, 64.0, 8.0, 6.0, 0.0, 3.8 * d, "Tunguska Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(126.0, 65.0, 7.0, 4.5, 0.0, 3.6 * d, "Lena Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(60.5, 66.5, 3.5, 2.0, 45.0, 3.8 * d, "Pechora Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(73.1, 49.8, 2.5, 1.5, 90.0, 3.8 * d, "Karaganda"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(75.3, 51.7, 1.8, 1.2, 45.0, 4.0 * d, "Ekibastuz"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(112.5, 37.8, 5.5, 2.5, 25.0, 5.0 * d, "Shanxi Datong"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(109.5, 39.0, 4.5, 3.5, 0.0, 5.0 * d, "Ordos Shenfu"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(86.2, 23.7, 3.5, 1.2, 90.0, 4.5 * d, "Damodar Valley"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(103.8, -3.7, 3.5, 1.8, 135.0, 3.8 * d, "South Sumatra"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(116.8, -1.0, 4.0, 2.0, 0.0, 4.4 * d, "East Kalimantan"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(29.2, -26.0, 2.8, 1.8, 90.0, 4.4 * d, "Witbank South Africa"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(148.5, -22.5, 6.0, 2.0, 160.0, 4.8 * d, "Bowen Basin Australia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(150.8, -32.8, 3.5, 1.8, 90.0, 4.2 * d, "Sydney Basin Australia"));
        return b;
    }

    private static List<PlanetaryRasterSampler.GeologicalBasin> createOilBasins(double d) {
        List<PlanetaryRasterSampler.GeologicalBasin> b = new ArrayList<>();
        b.add(new PlanetaryRasterSampler.GeologicalBasin(49.3, 25.5, 6.5, 3.0, 15.0, 5.0 * d, "Ghawar Saudi Arabia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(48.0, 29.1, 2.8, 2.0, 0.0, 4.8 * d, "Burgan Kuwait"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(47.2, 30.5, 6.5, 2.5, 135.0, 4.8 * d, "Rumaila Iraq"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(49.8, 31.3, 5.5, 2.0, 135.0, 4.8 * d, "Zagros Oil Belt"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(76.5, 61.2, 7.5, 5.0, 0.0, 5.0 * d, "West Siberia Samotlor"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(52.5, 54.8, 6.0, 4.0, 0.0, 4.5 * d, "Volga-Ural Romashkino"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(51.8, 46.5, 5.0, 3.5, 0.0, 5.0 * d, "Tengiz/Kashagan Kazakhstan"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(50.5, 40.0, 4.0, 2.5, 135.0, 4.6 * d, "South Caspian Baku"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-102.5, 31.8, 4.5, 3.2, 140.0, 5.0 * d, "Permian Basin TX/NM"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-90.5, 27.5, 5.0, 2.5, 90.0, 4.8 * d, "Deepwater Gulf of Mexico"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-148.5, 70.2, 5.0, 1.8, 90.0, 4.8 * d, "Prudhoe Bay Alaska"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-111.5, 56.8, 6.5, 3.5, 135.0, 5.0 * d, "Athabasca Oil Sands"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-71.5, 10.0, 3.0, 2.0, 0.0, 4.8 * d, "Maracaibo Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-64.0, 8.5, 6.5, 1.5, 90.0, 5.0 * d, "Faja del Orinoco"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-43.0, -24.5, 6.0, 3.0, 45.0, 5.0 * d, "Santos Pre-Salt Brazil"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(6.0, 4.8, 5.0, 3.5, 0.0, 5.0 * d, "Niger Delta"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(11.8, -6.5, 5.0, 2.5, 140.0, 4.8 * d, "Lower Congo Deepwater"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(19.5, 29.0, 4.5, 3.5, 0.0, 4.6 * d, "Sirte Basin Libya"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(6.0, 31.5, 4.0, 3.0, 0.0, 4.6 * d, "Hassi Messaoud Algeria"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(2.5, 57.5, 6.0, 3.0, 0.0, 4.8 * d, "North Sea Ekofisk"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(125.0, 46.5, 4.5, 2.5, 25.0, 4.8 * d, "Daqing Songliao"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(118.5, 38.0, 4.0, 3.0, 40.0, 4.6 * d, "Bohai Bay Shengli"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(101.5, 0.8, 4.5, 2.0, 135.0, 4.6 * d, "Central Sumatra Minas"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(72.0, 19.3, 3.5, 2.0, 0.0, 4.4 * d, "Mumbai High"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(148.5, -38.5, 2.5, 1.2, 90.0, 4.2 * d, "Gippsland Basin Australia"));
        return b;
    }

    private static List<PlanetaryRasterSampler.GeologicalBasin> createGasBasins(double d) {
        List<PlanetaryRasterSampler.GeologicalBasin> b = new ArrayList<>();
        b.add(new PlanetaryRasterSampler.GeologicalBasin(51.8, 26.5, 4.0, 3.0, 150.0, 5.0 * d, "North Field Qatar"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(69.5, 70.5, 5.5, 3.5, 0.0, 5.0 * d, "Yamal Bovanenkovo"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(77.5, 66.0, 6.0, 4.0, 0.0, 5.0 * d, "Urengoy/Yamburg Russia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(62.2, 37.3, 4.5, 3.0, 135.0, 5.0 * d, "Galkynysh Turkmenistan"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-78.5, 40.5, 6.5, 3.0, 45.0, 5.0 * d, "Appalachian Marcellus Shale"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-93.8, 32.2, 3.0, 2.2, 120.0, 4.6 * d, "Haynesville Shale"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-120.0, 56.0, 6.0, 3.0, 135.0, 4.8 * d, "Montney Formation Canada"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(6.8, 53.3, 4.5, 2.0, 90.0, 4.8 * d, "Groningen Netherlands"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(3.5, 60.6, 5.0, 2.5, 0.0, 5.0 * d, "Troll Norwegian North Sea"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(3.3, 32.9, 3.5, 2.5, 0.0, 5.0 * d, "Hassi R'Mel Algeria"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(40.8, -11.0, 3.5, 1.5, 0.0, 5.0 * d, "Rovuma Mozambique"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(106.0, 30.5, 4.5, 3.0, 40.0, 5.0 * d, "Sichuan Basin"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(115.5, -19.5, 5.0, 2.2, 50.0, 5.0 * d, "Gorgon NW Shelf Australia"));
        b.add(new PlanetaryRasterSampler.GeologicalBasin(-72.8, -11.8, 2.5, 1.5, 135.0, 4.8 * d, "Camisea Peru"));
        return b;
    }
}
