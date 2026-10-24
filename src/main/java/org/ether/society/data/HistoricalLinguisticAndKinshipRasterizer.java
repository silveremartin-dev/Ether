/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Cartographic Rasterizer for Linguistic Phyla (Isoglosses) and Anthropological Kinship Structures.
 *
 * <p>Decomposed from {@link HistoricalCulturalTensorRasterizer} to satisfy the Single
 * Responsibility Principle (SRP). Uses Orographic Glottolog DEM cost-distance propagation,
 * Murdoch/Todd kinship taxonomies, and deep-time Paleolithic technocomplexes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalLinguisticAndKinshipRasterizer {

    private HistoricalLinguisticAndKinshipRasterizer() {
        // Pure static rasterizer
    }

    public static final int[] COLORS_ISOGLOSS_100K = {
        0xE67E22, // Clade 0: Sapiens African Core (#E67E22)
        0xF39C12, // Clade 1: Sapiens Pioneers Levant & Jebel Faya (#F39C12)
        0x2980B9, // Clade 2: Neanderthal Western Classical Mousterian (#2980B9)
        0x1F4788, // Clade 3: Neanderthal Zagros & Near East / Shanidar (#1F4788)
        0x27AE60, // Clade 4: Denisovans Altai & Siberian (#27AE60)
        0x8E44AD  // Clade 5: Eastern Archaic & Sundaland (#8E44AD)
    };

    public static final int[] COLORS_KINSHIP_100K = {
        0xE74C3C, // Clade 0: Bilateral / Multi-Band Foragers (#E74C3C)
        0xF39C12, // Clade 1: Pioneer Coastal Dispersal Bands (#F39C12)
        0x3498DB, // Clade 2: Patrilocal Small Neanderthal Clades (#3498DB)
        0x2980B9, // Clade 3: Zagros Highland Cave Kin-Groups (#2980B9)
        0x2ECC71, // Clade 4: Cold-Adapted Steppe Foragers (#2ECC71)
        0x9B59B6  // Clade 5: Tropical Forest & Bamboo Bands (#9B59B6)
    };

    /**
     * Rasterize isogloss map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic linguistic vector boundaries for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernIsoglossMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        // 2. Ingest authentic vector boundaries from Seshat ClioPatria for historical years
        if (year >= -3400L) {
            BufferedImage seshatImg = CliopatriaPolityVectorReader.rasterizeSeshatIsoglossMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (seshatImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(seshatImg);
            }
        }

        // (lon, lat, colorRGB, sigma)
        List<double[]> languageCenters = new ArrayList<>();
        if (year <= -70000L) {
            // Handled via computeHomininCladeWeights
        } else if (year <= -40000L) {
            // -50,000 BP (MIS 3 Language Clades)
            languageCenters.add(new double[]{36.0, 0.5, 0xE67E22, 25.0});    // Sapiens Proto-African Core (#E67E22)
            languageCenters.add(new double[]{22.0, -34.0, 0xF39C12, 22.0});  // Sapiens Southern African LSA (#F39C12)
            languageCenters.add(new double[]{35.5, 32.5, 0xE74C3C, 20.0});   // Sapiens Levant IUP (#E74C3C)
            languageCenters.add(new double[]{1.5, 45.0, 0x2980B9, 22.0});    // Neanderthal Western Mousterian (#2980B9)
            languageCenters.add(new double[]{44.0, 36.0, 0x1F4788, 20.0});   // Neanderthal Zagros (#1F4788)
            languageCenters.add(new double[]{84.5, 51.4, 0x27AE60, 25.0});   // Denisovan Altai (#27AE60)
            languageCenters.add(new double[]{105.0, -2.0, 0x8E44AD, 25.0});  // Eastern Archaic (#8E44AD)
            languageCenters.add(new double[]{134.0, -24.0, 0xD35400, 28.0}); // Sahul Aboriginal Traditions (#D35400)
        } else if (year <= -18000L) {
            // -25,000 & -20,000 BP (LGM Linguistic Phyla)
            languageCenters.add(new double[]{1.0, 45.0, 0xE74C3C, 18.0});    // Franco-Cantabrian Solutrean/Gravettian (#E74C3C)
            languageCenters.add(new double[]{16.5, 48.8, 0x3498DB, 18.0});   // Central European Gravettian (#3498DB)
            languageCenters.add(new double[]{15.5, 41.7, 0x9B59B6, 16.0});   // Mediterranean Epigravettian (#9B59B6)
            languageCenters.add(new double[]{35.5, 32.7, 0xF39C12, 16.0});   // Levant Kebaran (#F39C12)
            languageCenters.add(new double[]{39.0, 51.4, 0x1ABC9C, 20.0});   // Kostenki-Don Steppe (#1ABC9C)
            languageCenters.add(new double[]{135.4, 70.7, 0x16A085, 22.0});  // Yana Mammoth Hunters (#16A085)
            languageCenters.add(new double[]{-140.7, 67.1, 0x00BCD4, 20.0}); // Bluefish Beringia Standstill (#00BCD4)
            languageCenters.add(new double[]{115.0, 30.0, 0x2ECC71, 24.0});  // East Asian Paleolithic (#2ECC71)
            languageCenters.add(new double[]{78.0, 22.0, 0xD35400, 22.0});   // South Asian Paleolithic (#D35400)
            languageCenters.add(new double[]{36.0, 0.5, 0xE67E22, 25.0});    // East African LSA (#E67E22)
            languageCenters.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South African LSA (#F1C40F)
            languageCenters.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Sahul Australian Foragers (#C0392B)
        } else {
            // Holocene & Historical Global Linguistic Phyla
            languageCenters.add(new double[]{15.0, 50.0, 0x3498DB, 25.0});   // Indo-European (European) (#3498DB)
            languageCenters.add(new double[]{75.0, 25.0, 0x2980B9, 25.0});   // Indo-European (Indo-Iranian) (#2980B9)
            languageCenters.add(new double[]{110.0, 32.0, 0xE74C3C, 28.0});  // Sino-Tibetan (#E74C3C)
            languageCenters.add(new double[]{40.0, 22.0, 0xF39C12, 25.0});   // Afroasiatic (#F39C12)
            languageCenters.add(new double[]{78.0, 13.0, 0xD35400, 20.0});   // Dravidian (#D35400)
            languageCenters.add(new double[]{60.0, 60.0, 0x1ABC9C, 30.0});   // Uralic (#1ABC9C)
            languageCenters.add(new double[]{85.0, 48.0, 0x27AE60, 35.0});   // Turkic / Altaic (#27AE60)
            languageCenters.add(new double[]{105.0, 48.0, 0x2ECC71, 25.0});  // Mongolic (#2ECC71)
            languageCenters.add(new double[]{138.0, 36.0, 0x9B59B6, 20.0});  // Japonic (#9B59B6)
            languageCenters.add(new double[]{127.0, 37.0, 0x8E44AD, 18.0});  // Koreanic (#8E44AD)
            languageCenters.add(new double[]{102.0, 16.0, 0x16A085, 20.0});  // Austroasiatic / Tai-Kadai (#16A085)
            languageCenters.add(new double[]{15.0, 5.0, 0xE67E22, 28.0});    // Niger-Congo (#E67E22)
            languageCenters.add(new double[]{28.0, 10.0, 0xC0392B, 22.0});   // Nilo-Saharan (#C0392B)
            languageCenters.add(new double[]{20.0, -25.0, 0xF1C40F, 22.0});  // Khoisan (#F1C40F)
            languageCenters.add(new double[]{120.0, 0.0, 0x00BCD4, 30.0});   // Austronesian (#00BCD4)
            languageCenters.add(new double[]{142.0, -5.0, 0x9C27B0, 18.0});  // Trans-New Guinea (#9C27B0)
            languageCenters.add(new double[]{134.0, -24.0, 0xE91E63, 28.0}); // Pama-Nyungan (#E91E63)
            languageCenters.add(new double[]{-100.0, 42.0, 0x3F51B5, 30.0}); // North American Amerind (#3F51B5)
            languageCenters.add(new double[]{-120.0, 58.0, 0x009688, 25.0}); // Na-Dene (#009688)
            languageCenters.add(new double[]{-95.0, 65.0, 0x607D8B, 30.0});  // Eskimo-Aleut (#607D8B)
            languageCenters.add(new double[]{-92.0, 16.0, 0xFF9800, 18.0});  // Mayan / Mesoamerican (#FF9800)
            languageCenters.add(new double[]{-75.0, -12.0, 0x795548, 22.0}); // Quechumaran (#795548)
            languageCenters.add(new double[]{-55.0, -15.0, 0x4CAF50, 28.0}); // Tupi-Guarani (#4CAF50)
        }

        if (year <= -85000L) {
            // MIS 5e Paleolithic Technocomplex & Communication Macro-Provinces (-100,000 BP)
            List<OrographicGlottologPropagator.CulturalSeed> paleoLangs = new ArrayList<>();
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(-1.0, 30.0, 0xE67E22, 2.2, "Pan-Saharan Aterian Technocomplex"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(35.0, 10.0, 0xF39C12, 2.0, "East African & Nilotic Bladelet Corridor"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(16.0, -1.0, 0xD35400, 2.0, "Equatorial Sangoan-Lupemban Core-Axe Domain"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(25.0, -26.0, 0xC0392B, 2.0, "Southern African Stillbay/Pietersburg Lithic Sphere"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 34.0, 0xF59E0B, 1.8, "Levant Tabun/Qafzeh Levallois Communication Hub"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(2.0, 47.0, 0x2980B9, 2.2, "Atlantic MTA / Acheulean Tradition Mousterian"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(35.0, 50.0, 0x1F4788, 2.2, "Central & Eastern European Steppe Quina-Ferrassie Complex"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(46.0, 35.0, 0x3B82F6, 1.8, "Zagros-Caucasus Highland Mousterian"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 50.0, 0x27AE60, 2.5, "Central Asian & Siberian Denisovan Bladelet Sphere"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(110.0, 32.0, 0x059669, 2.5, "East Asian Core-Flake / Lingjing Sphere"));
            paleoLangs.add(new OrographicGlottologPropagator.CulturalSeed(105.0, 0.0, 0x8E44AD, 2.2, "Sundaland Pebble-Tool & Bamboo Communication Sphere"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(paleoLangs, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        orographicImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
        } else if (year <= -65000L) {
            // -74,000 BP: Toba Volcanic Winter Refugial Technocomplexes
            List<OrographicGlottologPropagator.CulturalSeed> tobaLangs = new ArrayList<>();
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0xC0392B, 1.6, "Southern African Coastal Pre-Howiesons Poort"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xF39C12, 1.8, "East African Rift Bladelet Refugium"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0xE67E22, 1.5, "Maghreb Aterian Refugial Technocomplex"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(35.3, 32.7, 0xF59E0B, 1.4, "Levantine Mousterian/Sapiens Boundary Hub"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(-5.5, 36.1, 0x2980B9, 1.6, "Iberian / Gorham's Mousterian Refugium"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(1.5, 43.5, 0x1F4788, 1.6, "Franco-Cantabrian Denticulate Mousterian"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x3B82F6, 1.5, "Zagros Highland Mousterian Refugium"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x27AE60, 2.0, "Altai Denisovan Cold-Stadial Bladelet Complex"));
            tobaLangs.add(new OrographicGlottologPropagator.CulturalSeed(110.0, -7.5, 0x8E44AD, 1.8, "Sundaland Late Soloensis Pebble-Tool Domain"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(tobaLangs, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        orographicImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
        } else if (year <= -35000L) {
            // -50,000 BP: MIS 3 Initial Upper Paleolithic & Sahul Edge-Ground Axe Traditions
            List<OrographicGlottologPropagator.CulturalSeed> mis3Langs = new ArrayList<>();
            // Sapiens Initial Upper Paleolithic (IUP) & African LSA
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0xE67E22, 2.0, "Howiesons Poort Geometric Microlithic Complex"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xF39C12, 2.0, "East African Enkapune Ya Muto LSA Bladelets"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(-5.0, 34.0, 0xD35400, 1.8, "Maghreb Late Aterian / Dabban Precursor"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.5, 0xE74C3C, 1.8, "Levant Early Ahmarian / Emiran Prismatic Blades"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(25.4, 43.0, 0xEF4444, 2.0, "Danube-Balkan IUP Bachokirian Blade Technocomplex"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(71.2, 57.7, 0xF97316, 2.2, "Siberian Ust'-Ishim Mammoth Hunter Blades"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 15.3, 0xD97706, 2.2, "Indian Subcontinent Jwalapuram Microblade Tradition"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(115.9, 39.7, 0x10B981, 2.2, "East Asian Tianyuan Blade & Bone-Tool Complex"));
            // Sahul Aboriginal Technocomplexes (crossing Wallace Line)
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(132.9, -12.5, 0xC0392B, 2.2, "Arnhem Land Madjedbebe Edge-Ground Axe Technocomplex"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -33.7, 0x991B1B, 2.2, "Willandra Lakes Core & Scraper Tradition"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -5.5, 0xDC2626, 2.0, "Papuan Highland Huon Waisted-Axe Technocomplex"));
            // Neanderthal & Denisovan Technocomplexes
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x2980B9, 2.0, "Franco-Cantabrian Late Mousterian & Chatelperronian"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(16.0, 46.3, 0x1F4788, 1.8, "Central European Vindija Late Mousterian"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x27AE60, 2.2, "Denisovan Altai Bladelet & Polished Jewelry Complex"));
            mis3Langs.add(new OrographicGlottologPropagator.CulturalSeed(105.0, -2.0, 0x8E44AD, 2.2, "Sundaland Late Archaic Flake Complex"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(mis3Langs, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        orographicImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
        } else if (year <= -18000L) {
            // -25,000 to -18,000 BP: Gravettian, Solutrean & LGM Technocomplexes
            List<OrographicGlottologPropagator.CulturalSeed> lgmLangs = new ArrayList<>();
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0xE74C3C, 1.8, "Franco-Cantabrian Solutrean Pressure-Flaking Complex"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(16.5, 48.8, 0x3498DB, 2.0, "Pavlovian / Central European Mammoth Hunter Blades"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x1ABC9C, 2.0, "Eastern European Kostenki-Avdeevo Technocomplex"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(15.5, 41.7, 0x9B59B6, 1.8, "Mediterranean Epigravettian Backed Bladelets"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.7, 0xF39C12, 1.6, "Levantine Kebaran Microlithic Bladelet Complex"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(135.4, 70.7, 0x16A085, 2.2, "Arctic Siberian Yana Mammoth Hunter Complex"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(-140.7, 67.1, 0x00BCD4, 2.2, "Beringian Standstill Microblade Tradition"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(115.0, 30.0, 0x2ECC71, 2.2, "South China Paleolithic / Early Pottery Precursor"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 22.0, 0xD35400, 2.0, "Indian Mesolithic Precursor Microliths"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xC0392B, 2.5, "Sahul Pan-Continental Core & Tool Technocomplex"));
            lgmLangs.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0xF1C40F, 2.0, "Southern African Robberg Bladelet Industry"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(lgmLangs, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        orographicImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
        } else if (year <= -10500L) {
            // -10,900 BP: Younger Dryas, Clovis & Natufian Epipaleolithic
            List<OrographicGlottologPropagator.CulturalSeed> ydLangs = new ArrayList<>();
            // Americas (Clovis & South America)
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(-103.3, 34.3, 0x3498DB, 2.5, "North American Clovis Fluted Point Technocomplex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(-73.2, -41.5, 0x1ABC9C, 2.2, "South American Monte Verde / Fishtail Point Complex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(-42.5, -9.3, 0x00BCD4, 2.0, "Brazilian Itaparica Quartzite Industry"));
            // Old World
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(35.58, 33.08, 0xF39C12, 1.6, "Natufian Sickle Blade & Mortar Technocomplex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0xE74C3C, 1.8, "Franco-Cantabrian Late Magdalenian / Azilian"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(10.0, 53.5, 0x3B82F6, 1.8, "North European Ahrensburgian Reindeer Hunters"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(139.5, 35.7, 0x9B59B6, 1.6, "Japanese Incipient Jomon Pottery Tradition"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(114.0, 34.5, 0x2ECC71, 2.0, "Yellow River Microblade & Grindstone Complex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(77.6, 22.9, 0xD35400, 2.0, "Indian Mesolithic Bhimbetka Geometric Microliths"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0xE67E22, 1.8, "Maghreb Iberomaurusian / Capsian Complex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(32.5, 25.5, 0xD35400, 1.8, "Nile Valley Qadan / Isnan Microblade Complex"));
            ydLangs.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xC0392B, 2.5, "Australian Core Tool & Scraper Tradition"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(ydLangs, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        orographicImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
        }

        // Orographic Glottolog DEM cost-distance propagation for ancient & prehistoric clades
        List<OrographicGlottologPropagator.CulturalSeed> seeds = new ArrayList<>();
        for (double[] lc : languageCenters) {
            seeds.add(new OrographicGlottologPropagator.CulturalSeed(lc[0], lc[1], (int) lc[2], lc[3] / 20.0, "Clade"));
        }
        BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(seeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(orographicImg);
    }

    /**
     * Rasterize kinship map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic Murdock/Todd anthropological kinship structures for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernKinshipMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        // 2. Ingest authentic vector boundaries from Seshat ClioPatria for historical years
        if (year >= -3400L) {
            BufferedImage seshatImg = CliopatriaPolityVectorReader.rasterizeSeshatKinshipMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (seshatImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(seshatImg);
            }
        }

        // (lon, lat, colorRGB, sigma)
        List<double[]> kinshipCenters = new ArrayList<>();
        if (year <= -70000L) {
            // Handled via computeHomininCladeWeights
        } else if (year <= -40000L) {
            // -50,000 BP (MIS 3 Kinship Systems)
            kinshipCenters.add(new double[]{36.0, 0.5, 0xE74C3C, 25.0});    // Bilateral / Multi-Band Sapiens (#E74C3C)
            kinshipCenters.add(new double[]{22.0, -34.0, 0xF39C12, 22.0});  // Coastal Pioneer Bands (#F39C12)
            kinshipCenters.add(new double[]{1.5, 45.0, 0x3498DB, 22.0});    // Small Patrilocal Neanderthal Clades (#3498DB)
            kinshipCenters.add(new double[]{44.0, 36.0, 0x2980B9, 20.0});   // Zagros Cave Kin-Groups (#2980B9)
            kinshipCenters.add(new double[]{84.5, 51.4, 0x2ECC71, 25.0});   // Cold-Adapted Steppe Foragers (#2ECC71)
            kinshipCenters.add(new double[]{105.0, -2.0, 0x9B59B6, 25.0});  // Tropical Forest Bands (#9B59B6)
            kinshipCenters.add(new double[]{134.0, -24.0, 0xD35400, 28.0}); // Sahul Subsection Totemic Clans (#D35400)
        } else if (year <= -18000L) {
            // -25,000 & -20,000 BP (LGM Kinship Horizons)
            kinshipCenters.add(new double[]{1.0, 45.0, 0xE74C3C, 18.0});    // Franco-Cantabrian Alliance Bands (#E74C3C)
            kinshipCenters.add(new double[]{16.5, 48.8, 0x3498DB, 18.0});   // Gravettian Aggregation Camps (#3498DB)
            kinshipCenters.add(new double[]{15.5, 41.7, 0x9B59B6, 16.0});   // Mediterranean Coastal Kin-Bands (#9B59B6)
            kinshipCenters.add(new double[]{35.5, 32.7, 0xF39C12, 16.0});   // Levant Multi-Family Encampments (#F39C12)
            kinshipCenters.add(new double[]{135.4, 70.7, 0x1ABC9C, 22.0});  // Arctic Mammoth Hunter Exogamy (#1ABC9C)
            kinshipCenters.add(new double[]{115.0, 30.0, 0x2ECC71, 24.0});  // East Asian Seasonal Aggregations (#2ECC71)
            kinshipCenters.add(new double[]{134.0, -24.0, 0xD35400, 28.0}); // Australian Skin Systems (#D35400)
            kinshipCenters.add(new double[]{36.0, 0.5, 0xE67E22, 25.0});    // African Multi-Band Networks (#E67E22)
        } else {
            // Holocene & Historical Global Kinship Typologies
            kinshipCenters.add(new double[]{10.0, 50.0, 0x3498DB, 35.0});   // Western Europe Bilateral (#3498DB)
            kinshipCenters.add(new double[]{35.0, 55.0, 0x2980B9, 38.0});   // Slavic Patrilineal Clan (#2980B9)
            kinshipCenters.add(new double[]{45.0, 32.0, 0xF39C12, 30.0});   // Near East Segmentary Lineage (#F39C12)
            kinshipCenters.add(new double[]{78.0, 22.0, 0xD35400, 32.0});   // Indo-Aryan Gotra Patrilineal (#D35400)
            kinshipCenters.add(new double[]{112.0, 34.0, 0xE74C3C, 35.0});  // Chinese Agnation/Zongzu Clan (#E74C3C)
            kinshipCenters.add(new double[]{138.0, 36.0, 0x2ECC71, 25.0});  // Japanese Ie Stem Family (#2ECC71)
            kinshipCenters.add(new double[]{65.0, 48.0, 0x16A085, 40.0});   // Central Asian Nomadic Clan (#16A085)
            kinshipCenters.add(new double[]{20.0, 5.0, 0xE67E22, 30.0});    // Central Bantu Patrilineal (#E67E22)
            kinshipCenters.add(new double[]{25.0, -10.0, 0x9B59B6, 25.0});  // Matrilineal Belt (Zambia/Congo) (#9B59B6)
            kinshipCenters.add(new double[]{5.0, 8.0, 0xC0392B, 25.0});     // West African Segmentary (#C0392B)
            kinshipCenters.add(new double[]{22.0, -25.0, 0xF1C40F, 25.0});  // Khoisan Band Kinship (#F1C40F)
            kinshipCenters.add(new double[]{38.0, 5.0, 0xFF9800, 25.0});    // East African Age-Set (#FF9800)
            kinshipCenters.add(new double[]{-76.0, 43.0, 0x9C27B0, 22.0});  // Iroquois Matrilineal Clan (#9C27B0)
            kinshipCenters.add(new double[]{-100.0, 45.0, 0x3F51B5, 28.0}); // Plains Bilateral/Patrilineal (#3F51B5)
            kinshipCenters.add(new double[]{-110.0, 35.0, 0x673AB7, 20.0}); // Pueblo Matrilocal Clan (#673AB7)
            kinshipCenters.add(new double[]{-125.0, 52.0, 0x009688, 22.0}); // Pacific Northwest Potlatch (#009688)
            kinshipCenters.add(new double[]{-65.0, -3.0, 0x4CAF50, 30.0});  // Amazonian Moiety (#4CAF50)
            kinshipCenters.add(new double[]{-74.0, -13.0, 0x795548, 25.0}); // Andean Ayllu Bilateral (#795548)
            kinshipCenters.add(new double[]{133.0, -25.0, 0xD35400, 30.0}); // Australian 8-Skin Subsection (#D35400)
            kinshipCenters.add(new double[]{145.0, -5.0, 0x00BCD4, 20.0});  // New Guinea Segmentary Clan (#00BCD4)
            kinshipCenters.add(new double[]{175.0, -20.0, 0x1ABC9C, 25.0}); // Polynesian Ramage (#1ABC9C)
        }

        if (year <= -85000L) {
            // MIS 5e Paleolithic Kinship: Local Patrilocal Neanderthal Clans & African Fission-Fusion Bands (Finite Reach = 34.0f)
            List<OrographicGlottologPropagator.CulturalSeed> paleoKin = new ArrayList<>();
            // Neanderthal Patrilocal Small Clans (very localized, reach ~30.0f)
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(-5.3, 43.3, 0x3498DB, 1.0, "Cantabrian Patrilocal Clan (El Sidron)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(-5.3, 36.1, 0x2980B9, 0.9, "Gibraltar Neanderthal Coastal Band (Gorham)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x1E40AF, 1.0, "Perigord Cave Kin-Group (La Ferrassie)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(4.7, 50.5, 0x2563EB, 1.0, "Meuse Valley Mammoth Hunters (Spy)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(15.9, 46.2, 0x1D4ED8, 1.0, "Danubian Neanderthal Clan (Krapina)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(34.3, 45.0, 0x3B82F6, 1.0, "Crimean Steppe-Fringe Clan (Kiik-Koba)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(44.3, 36.8, 0x60A5FA, 1.0, "Zagros Highland Lineage (Shanidar)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(67.0, 38.5, 0x93C5FD, 1.0, "Central Asian Cave Band (Teshik-Tash)"));

            // African MSA Fission-Fusion Bilateral Bands & Coastal Strandlopers
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(24.4, -34.1, 0xDC2626, 1.1, "Cape Coastal Strandloper Band (Klasies River)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(21.2, -34.4, 0xB91C1C, 1.0, "Southern Cape Estuary Cluster (Blombos/Pinnacle)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(26.1, -28.8, 0xEA580C, 1.2, "Highveld Savannah Big-Game Federation (Florisbad)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 4.5, 0xE74C3C, 1.2, "Rift Valley Lacustrine Bands (Omo Kibish)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(41.8, 9.6, 0xF97316, 1.1, "Horn of Africa Highlands Band (Porc-Epic)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0xFB923C, 1.2, "Maghreb Mountain-Coast Band (Taforalt)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(5.3, 7.2, 0xEF4444, 1.1, "Guinean Forest Foraging Band (Iwo Eleru)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(20.0, 0.0, 0x991B1B, 1.2, "Congo Riverine Forest Band"));

            // Levant Contact Hub
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(35.3, 32.7, 0xF59E0B, 1.0, "Levantine Caves Transitional Coalition (Qafzeh/Tabun)"));

            // Denisovans & Eastern Archaics
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(84.7, 51.4, 0x2ECC71, 1.2, "Altai Denisovan Mountain Clan (Denisova)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(102.8, 35.5, 0x10B981, 1.1, "Tibetan High-Altitude Band (Baishiya)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(113.7, 34.1, 0x15803D, 1.2, "Central Plains Riverine Extended Family (Lingjing)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(111.6, 25.5, 0x047857, 1.1, "South China Karst Forest Band (Fuyan)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(111.5, -7.4, 0x9B59B6, 1.2, "Java Solo River Tropical Band (Ngandong)"));
            paleoKin.add(new OrographicGlottologPropagator.CulturalSeed(120.4, -8.5, 0x8B5CF6, 0.8, "Flores Island Endemic Band (Liang Bua)"));

            BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(paleoKin, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        kinImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
        } else if (year <= -65000L) {
            // -74,000 BP: Toba Volcanic Winter Refugial Kinship Networks
            List<OrographicGlottologPropagator.CulturalSeed> tobaKin = new ArrayList<>();
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0xDC2626, 1.0, "Cape Coastal Refugium Multi-Family Band"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xEA580C, 1.2, "East African Highland Refugial Foragers"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0xFB923C, 1.0, "Maghreb Coastal Cave Lineage"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(-5.5, 36.1, 0x2980B9, 0.9, "Gorham's Cave Neanderthal Family"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(1.5, 43.5, 0x1E40AF, 1.0, "Pyrenean Neanderthal Patrilocal Band"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x60A5FA, 1.0, "Zagros Shanidar Mountain Clan"));
            tobaKin.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x2ECC71, 1.1, "Altai Denisovan Winter Hearth Clan"));

            BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(tobaKin, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        kinImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
        } else if (year <= -35000L) {
            // -50,000 BP: MIS 3 Sahul Subsection Systems & Eurasian IUP Bands
            List<OrographicGlottologPropagator.CulturalSeed> mis3Kin = new ArrayList<>();
            // Sahul Subsection & Totemic Clan Systems (crossing Wallace Line)
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(132.9, -12.5, 0xD35400, 1.6, "Arnhem Land 4-Section Clan System"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -33.7, 0xC0392B, 1.5, "Willandra Lakes Matrilineal Moiety Band"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(125.0, -16.0, 0xE67E22, 1.5, "Kimberley Exogamous Foraging Network"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -5.5, 0x9B59B6, 1.4, "Papuan Highland Multi-Clan Coalition"));
            // African Early LSA
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0xDC2626, 1.4, "Southern African Fission-Fusion Network"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xEA580C, 1.5, "East African Rift Multi-Band Aggregations"));
            // Eurasian IUP & Tianyuan
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.5, 0xF59E0B, 1.2, "Levantine Ahmarian Foraging Coalition"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(25.4, 43.0, 0xEF4444, 1.3, "Danubian IUP Pioneer Macro-Bands"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(71.2, 57.7, 0xF97316, 1.4, "Siberian Ust'-Ishim Extended Family"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(115.9, 39.7, 0x10B981, 1.4, "East Asian Tianyuan Riverine Clan"));
            // Late Neanderthal Clades
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x3498DB, 1.2, "Perigord Late Neanderthal Cave Clan"));
            mis3Kin.add(new OrographicGlottologPropagator.CulturalSeed(16.0, 46.3, 0x2563EB, 1.1, "Vindija Cave Foraging Group"));

            BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(mis3Kin, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        kinImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
        } else if (year <= -18000L) {
            // -25,000 to -18,000 BP: Gravettian Mammoth Hunter Macro-Bands & LGM Refugia
            List<OrographicGlottologPropagator.CulturalSeed> lgmKin = new ArrayList<>();
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(16.5, 48.8, 0x3498DB, 1.5, "Pavlovian Mammoth Hunter Aggregation Camps"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0xE74C3C, 1.4, "Solutrean Franco-Cantabrian Alliance Bands"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(15.5, 41.7, 0x9B59B6, 1.3, "Italian Coastal Epigravettian Kin-Groups"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x1ABC9C, 1.5, "Don River Kostenki Extended Family Lineages"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(135.4, 70.7, 0x16A085, 1.5, "Yana Arctic Siberian Exogamous Bands"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(-140.7, 67.1, 0x00BCD4, 1.5, "Beringian Standstill Refugial Households"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.7, 0xF39C12, 1.2, "Kebaran Encampment Multi-Family Units"));
            lgmKin.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xD35400, 1.6, "Sahul Pan-Continental Skin Section System"));

            BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(lgmKin, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        kinImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
        } else if (year <= -10500L) {
            // -10,900 BP: Younger Dryas / Natufian Co-Residential Lineages & Clovis Bands
            List<OrographicGlottologPropagator.CulturalSeed> ydKin = new ArrayList<>();
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(35.58, 33.08, 0xF39C12, 1.2, "Natufian Sedentary Hamlet Lineages (Ain Mallaha)"));
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(-103.3, 34.3, 0x3F51B5, 1.6, "Clovis High Plains Mobile Foraging Bands"));
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(-73.2, -41.5, 0x1ABC9C, 1.5, "Monte Verde Extended Family Base-Camp"));
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0xE74C3C, 1.4, "Late Magdalenian Riverine Aggregation Bands"));
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(139.5, 35.7, 0x2ECC71, 1.3, "Incipient Jomon Coastal Extended Households"));
            ydKin.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xD35400, 1.6, "Australian 8-Skin Subsection Networks"));

            BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(ydKin, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 1e5f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        kinImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
        }

        // Orographic cost-distance propagation for kinship structures
        // Uncovered inhabited land pixels receive neutral slate-gray #374151
        List<OrographicGlottologPropagator.CulturalSeed> kinSeeds = new ArrayList<>();
        for (double[] kc : kinshipCenters) {
            kinSeeds.add(new OrographicGlottologPropagator.CulturalSeed(kc[0], kc[1], (int) kc[2], kc[3] / 20.0, "Kinship"));
        }
        BufferedImage kinImg = OrographicGlottologPropagator.propagateCulturalSeeds(kinSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
        // Apply hominin occupancy filter and black out Antarctica
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (lat < -60.0 || occWeight <= 0.001) {
                    // Antarctica and uninhabited land → neutral gray (not ocean black)
                    kinImg.setRGB(x, y, 0x2D3748);
                }
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(kinImg);
    }
}
