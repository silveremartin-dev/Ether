/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * High-precision Sociocultural, Linguistic, Religious, and Institutional Cartographic Tensor Rasterizer.
 * 
 * <p>Extracted from {@code HistoricalMapGenerator} to satisfy the Single Responsibility Principle (SRP).
 * Compiles empirical archaeological, ethnographic, and historical sociopolitical tensors into 2D raster maps:
 * sovereignty, isogloss, kinship systems, sacred sites/rituals, technological progress, trade networks,
 * institutional hierarchy, ecological footprints, and epidemiological immunity landscapes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalCulturalTensorRasterizer {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalCulturalTensorRasterizer.class);

    private HistoricalCulturalTensorRasterizer() {
        // Pure static rasterizer
    }

// --- 2. SOVEREIGNTY TENSOR MAP ---
    public static final int[] COLORS_SOVEREIGNTY_100K = {
        0xD35400, // Clade 0: Proto-Sapiens Pan-African Domain (#D35400)
        0xF39C12, // Clade 1: Out-of-Africa Dispersal Domain (#F39C12)
        0x1F618D, // Clade 2: Western Mousterian Clan Territories (#1F618D)
        0x1A5276, // Clade 3: Zagros & Near East Mousterian Domain (#1A5276)
        0x229954, // Clade 4: Denisovan Central Asian Domain (#229954)
        0x7D3C98  // Clade 5: Eastern Archaic Sunda Domain (#7D3C98)
    };

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

    public static final int[] COLORS_RITUALS_100K = {
        0x10B981, // Clade 0: Sapiens Pan-African Symbolic Ochre & Shell Ornamentation (#10B981)
        0xF59E0B, // Clade 1: Pioneer Coastal Symbolic Caches (#F59E0B)
        0x3B82F6, // Clade 2: Neanderthal Intentional Burials & Raptor Claw Cults (#3B82F6)
        0x2563EB, // Clade 3: Shanidar Flower Burial Tradition (#2563EB)
        0xD97706, // Clade 4: Denisovan Altai Chloritolite Jewelry & Bone Carving (#D97706)
        0x8B5CF6  // Clade 5: Archaic Asian Megafauna & Ochre Traditions (#8B5CF6)
    };

    /**
     * Blend clade rgb.
     *
     * @param w the w parameter (double[])
     * @param cladeColors the clade colors parameter (int[])
     * @param occWeight the occ weight parameter (double)
     * @return the resulting computation or state reference
     */
    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        if (w == null || occWeight <= 0.001) return 0x2D3748; // uninhabited land
        // Use dominant clade color (argmax) for discrete cultural zone boundaries
        int dominantIdx = 0;
        double maxW = w[0];
        for (int i = 1; i < 6; i++) {
            if (w[i] > maxW) { maxW = w[i]; dominantIdx = i; }
        }
        if (maxW <= 0.0) return 0x2D3748;
        return cladeColors[dominantIdx];
    }

    /**
     * Rasterize sovereignty map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic sovereign vector boundaries from Natural Earth for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernSovereigntyMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        // 2. Ingest authentic vector boundaries from Seshat ClioPatria for historical years
        if (year >= -3400L) {
            BufferedImage seshatImg = CliopatriaPolityVectorReader.rasterizeSeshatSovereigntyMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (seshatImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(seshatImg);
            }
        }

        // (lon, lat, colorRGB, sigma)
        List<double[]> empireCores = new ArrayList<>();


        if (year <= -70000L) {
            // Handled via computeHomininCladeWeights
        } else if (year <= -40000L) {
            // -50,000 BP: MIS 3 Clade Spheres
            empireCores.add(new double[]{36.0, 0.5, 0xD35400, 25.0});    // Sapiens East African Core (#D35400)
            empireCores.add(new double[]{22.0, -34.0, 0xE67E22, 22.0});  // Sapiens Southern African LSA (#E67E22)
            empireCores.add(new double[]{35.5, 32.5, 0xF39C12, 18.0});   // Sapiens Levant IUP (#F39C12)
            empireCores.add(new double[]{1.5, 45.0, 0x1F618D, 20.0});    // Neanderthal Western Europe (#1F618D)
            empireCores.add(new double[]{44.0, 36.0, 0x1A5276, 18.0});   // Neanderthal Zagros (#1A5276)
            empireCores.add(new double[]{84.5, 51.4, 0x229954, 22.0});   // Denisovan Altai / Siberia (#229954)
            empireCores.add(new double[]{105.0, -2.0, 0x7D3C98, 25.0});  // Archaic Sundaland (#7D3C98)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Sahul Aboriginal Domain (#C0392B)
        } else if (year <= -18000L) {
            // -25,000 & -20,000 BP: Gravettian / LGM Cultural Horizons
            empireCores.add(new double[]{1.0, 45.0, 0xE74C3C, 18.0});    // Franco-Cantabrian Solutrean/Gravettian (#E74C3C)
            empireCores.add(new double[]{16.5, 48.8, 0x3498DB, 18.0});   // Central European Pavlovian / Gravettian (#3498DB)
            empireCores.add(new double[]{15.5, 41.7, 0x9B59B6, 16.0});   // Mediterranean Epigravettian (#9B59B6)
            empireCores.add(new double[]{35.5, 32.7, 0xF39C12, 16.0});   // Levant Kebaran / Ohalo II (#F39C12)
            empireCores.add(new double[]{39.0, 51.4, 0x1ABC9C, 20.0});   // Kostenki-Don Steppe (#1ABC9C)
            empireCores.add(new double[]{135.4, 70.7, 0x16A085, 22.0});  // Yana / Arctic Siberia Mammoth Hunters (#16A085)
            empireCores.add(new double[]{-140.7, 67.1, 0x00BCD4, 20.0}); // Bluefish / Beringian Standstill (#00BCD4)
            empireCores.add(new double[]{115.0, 30.0, 0x2ECC71, 24.0});  // East Asian / South China Paleolithic (#2ECC71)
            empireCores.add(new double[]{78.0, 22.0, 0xD35400, 22.0});   // South Asian Paleolithic (#D35400)
            empireCores.add(new double[]{36.0, 0.5, 0xE67E22, 25.0});    // East African LSA (#E67E22)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South African LSA (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Sahul Australian Foragers (#C0392B)
        } else if (year <= -10500L) {
            // -10,900 BP (Younger Dryas)
            empireCores.add(new double[]{35.5, 33.0, 0xF39C12, 12.0});   // Natufian Levant (#F39C12)
            empireCores.add(new double[]{41.5, 38.1, 0xE67E22, 12.0});   // Upper Tigris (#E67E22)
            empireCores.add(new double[]{1.0, 45.0, 0xE74C3C, 14.0});    // Franco-Cantabrian Magdalenian (#E74C3C)
            empireCores.add(new double[]{-103.3, 34.3, 0x3498DB, 18.0}); // Clovis High Plains (#3498DB)
            empireCores.add(new double[]{-97.7, 30.9, 0x2980B9, 16.0});  // Clovis Texas (#2980B9)
            empireCores.add(new double[]{-73.2, -41.5, 0x1ABC9C, 15.0}); // Monte Verde South America (#1ABC9C)
            empireCores.add(new double[]{114.0, 34.5, 0x2ECC71, 16.0});  // Yellow River Late Paleo (#2ECC71)
            empireCores.add(new double[]{139.5, 35.7, 0x9B59B6, 12.0});  // Incipient Jomon (#9B59B6)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South African Robberg/Oakhurst (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Sahul / Australian Foragers (#C0392B)
            empireCores.add(new double[]{32.5, 25.5, 0xD35400, 18.0});   // Nile Valley Epipaleolithic (#D35400)
            empireCores.add(new double[]{-2.4, 34.8, 0xE67E22, 18.0});   // Maghreb Iberomaurusian (#E67E22)
            empireCores.add(new double[]{77.6, 22.9, 0xD35400, 20.0});   // Indian Mesolithic / Bhimbetka (#D35400)
            empireCores.add(new double[]{130.0, 62.0, 0x16A085, 22.0});  // Siberian Dyuktai Tradition (#16A085)
            empireCores.add(new double[]{10.0, 5.0, 0xE67E22, 22.0});    // West / Central African LSA (#E67E22)
        } else if (year <= -9000L) {
            // -10,000 BP (Early Holocene)
            empireCores.add(new double[]{38.92, 37.22, 0xE67E22, 12.0}); // Göbekli Tepe (#E67E22)
            empireCores.add(new double[]{35.44, 31.87, 0xF39C12, 12.0}); // Jericho PPNA (#F39C12)
            empireCores.add(new double[]{113.6, 34.4, 0x2ECC71, 15.0});  // Peiligang Yellow River (#2ECC71)
            empireCores.add(new double[]{120.0, 29.5, 0x27AE60, 15.0});  // Shangshan Yangtze (#27AE60)
            empireCores.add(new double[]{22.0, 44.5, 0x3498DB, 12.0});   // Lepenski Vir Danube (#3498DB)
            empireCores.add(new double[]{-103.0, 36.0, 0x3F51B5, 16.0}); // Folsom Great Plains (#3F51B5)
            empireCores.add(new double[]{-79.3, -7.7, 0x009688, 14.0});  // Paiján Peru (#009688)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South African Wilton/Oakhurst (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australian Aboriginal Nations (#C0392B)
            empireCores.add(new double[]{77.6, 22.9, 0xD35400, 20.0});   // South Asian Foragers (#D35400)
            empireCores.add(new double[]{36.0, 0.5, 0xE67E22, 22.0});    // East / Central Africa (#E67E22)
            empireCores.add(new double[]{130.0, 62.0, 0x16A085, 22.0});  // Siberian Early Holocene (#16A085)
        } else if (year <= -7000L) {
            // -8,000 BP (Early Neolithic)
            empireCores.add(new double[]{32.83, 37.67, 0xD35400, 14.0}); // Çatalhöyük (#D35400)
            empireCores.add(new double[]{35.95, 31.98, 0xF39C12, 12.0}); // Ain Ghazal (#F39C12)
            empireCores.add(new double[]{113.6, 33.6, 0xE74C3C, 15.0});  // Jiahu (#E74C3C)
            empireCores.add(new double[]{22.8, 39.3, 0x3498DB, 14.0});   // Sesklo Greece (#3498DB)
            empireCores.add(new double[]{68.05, 29.28, 0x9C27B0, 14.0}); // Mehrgarh Indus (#9C27B0)
            empireCores.add(new double[]{30.58, 22.53, 0x27AE60, 14.0}); // Nabta Playa (#27AE60)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-100.0, 38.0, 0x3F51B5, 22.0}); // North American Archaic (#3F51B5)
            empireCores.add(new double[]{-75.0, -10.0, 0x009688, 20.0}); // South American Archaic (#009688)
            empireCores.add(new double[]{36.0, 0.5, 0xE67E22, 22.0});    // East / Central Africa (#E67E22)
            empireCores.add(new double[]{125.0, 50.0, 0x16A085, 22.0});  // Northeast Asia (#16A085)
        } else if (year <= -4500L) {
            // -6,000 BP (Middle Neolithic)
            empireCores.add(new double[]{45.99, 30.82, 0xE74C3C, 15.0}); // Eridu Ubaid (#E74C3C)
            empireCores.add(new double[]{48.26, 32.19, 0xF39C12, 14.0}); // Susa I (#F39C12)
            empireCores.add(new double[]{31.37, 26.99, 0xD35400, 14.0}); // Badari Egypt (#D35400)
            empireCores.add(new double[]{109.06, 34.27, 0x2ECC71, 16.0});// Banpo Yangshao (#2ECC71)
            empireCores.add(new double[]{20.62, 44.76, 0x3498DB, 15.0}); // Vinča Copper (#3498DB)
            empireCores.add(new double[]{-3.00, 47.60, 0x9B59B6, 14.0}); // Carnac Megalithic (#9B59B6)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // South Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North American Archaic Mounds (#3F51B5)
            empireCores.add(new double[]{-77.0, -10.0, 0x009688, 20.0}); // South American Caral Precursor (#009688)
            empireCores.add(new double[]{68.0, 29.0, 0x9C27B0, 18.0});   // Mehrgarh III (#9C27B0)
            empireCores.add(new double[]{125.0, 50.0, 0x16A085, 22.0});  // Northeast Asia (#16A085)
        } else if (year <= -2500L) {
            // -3,000 BP (Early Bronze Age: Narmer Egypt, Uruk/Sumer, Caral, Liangzhu)
            empireCores.add(new double[]{31.20, 29.85, 0xD35400, 15.0}); // Early Dynastic Egypt / Memphis (#D35400)
            empireCores.add(new double[]{45.64, 31.32, 0xE74C3C, 14.0}); // Sumerian City-States / Uruk IV (#E74C3C)
            empireCores.add(new double[]{48.26, 32.19, 0xF39C12, 14.0}); // Proto-Elamite Susa (#F39C12)
            empireCores.add(new double[]{68.70, 27.50, 0x9B59B6, 16.0}); // Kot Diji / Early Harappan (#9B59B6)
            empireCores.add(new double[]{120.00, 30.38, 0x2ECC71, 16.0});// Liangzhu Culture / Yangtze (#2ECC71)
            empireCores.add(new double[]{114.50, 34.80, 0x27AE60, 16.0});// Longshan Culture / Yellow River (#27AE60)
            empireCores.add(new double[]{23.70, 37.90, 0x3498DB, 14.0}); // Early Cycladic / Helladic Greece (#3498DB)
            empireCores.add(new double[]{36.00, 48.00, 0x2980B9, 20.0}); // Yamnaya Steppe Pastoralists (#2980B9)
            empireCores.add(new double[]{-77.52, -10.89, 0x1ABC9C, 15.0});// Norte Chico / Caral-Supe Peru (#1ABC9C)
            empireCores.add(new double[]{32.00, 19.50, 0xE67E22, 16.0}); // Early Kerma / Nubia (#E67E22)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa LSA (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North American Archaic (#3F51B5)
        } else if (year <= -1700L) {
            // -1,900 BP (Middle Bronze Age: Hammurabi Babylon, Middle Kingdom Egypt, Erlitou Xia)
            empireCores.add(new double[]{44.42, 32.54, 0xE74C3C, 15.0}); // Hammurabi Old Babylonian Empire (#E74C3C)
            empireCores.add(new double[]{32.65, 25.72, 0xD35400, 16.0}); // Middle Kingdom Egypt / Thebes (#D35400)
            empireCores.add(new double[]{68.14, 27.33, 0x9B59B6, 16.0}); // Mature Harappan / Mohenjo-Daro (#9B59B6)
            empireCores.add(new double[]{112.70, 34.70, 0x2ECC71, 16.0});// Xia Dynasty / Erlitou (#2ECC71)
            empireCores.add(new double[]{25.16, 35.30, 0x3498DB, 12.0}); // Minoan Knossos / Crete (#3498DB)
            empireCores.add(new double[]{34.60, 40.00, 0xF39C12, 14.0}); // Old Hittite Kingdom / Hattusa (#F39C12)
            empireCores.add(new double[]{32.40, 19.60, 0xE67E22, 15.0}); // Kingdom of Kerma / Kush (#E67E22)
            empireCores.add(new double[]{-77.50, -10.90, 0x1ABC9C, 15.0});// Caral / Kotosh Peru (#1ABC9C)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North America Poverty Point (#3F51B5)
        } else if (year <= -1200L) {
            // -1,500 BP (Late Bronze Age: New Kingdom Egypt, Shang China, Hittites, Mycenae)
            empireCores.add(new double[]{32.65, 25.72, 0xD35400, 18.0}); // New Kingdom Egypt (Thutmose III) (#D35400)
            empireCores.add(new double[]{114.30, 36.10, 0x2ECC71, 18.0});// Shang Dynasty Anyang/Yin (#2ECC71)
            empireCores.add(new double[]{34.60, 40.00, 0xF39C12, 16.0}); // Hittite Empire (#F39C12)
            empireCores.add(new double[]{22.75, 37.73, 0x3498DB, 14.0}); // Mycenaean Greece (#3498DB)
            empireCores.add(new double[]{44.42, 32.54, 0xE74C3C, 15.0}); // Kassite Babylon (#E74C3C)
            empireCores.add(new double[]{40.50, 36.80, 0xE67E22, 15.0}); // Mitanni Kingdom (#E67E22)
            empireCores.add(new double[]{75.80, 30.90, 0x9B59B6, 16.0}); // Early Vedic Aryan Punjab (#9B59B6)
            empireCores.add(new double[]{-94.76, 17.75, 0x1ABC9C, 14.0});// Olmec Early San Lorenzo (#1ABC9C)
            empireCores.add(new double[]{-77.18, -9.60, 0x16A085, 14.0}); // Chavin Precursor Andes (#16A085)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North America (#3F51B5)
        } else if (year <= -600L) {
            // -1,000 BP (Early Iron Age: Neo-Assyrian, Phoenician, Western Zhou)
            empireCores.add(new double[]{43.15, 36.36, 0xE74C3C, 18.0}); // Neo-Assyrian Empire Nimrud/Nineveh (#E74C3C)
            empireCores.add(new double[]{35.20, 33.27, 0x9B59B6, 14.0}); // Phoenician Thalassocracy Tyre/Sidon (#9B59B6)
            empireCores.add(new double[]{108.70, 34.20, 0x2ECC71, 20.0});// Western Zhou Dynasty Haojing (#2ECC71)
            empireCores.add(new double[]{23.70, 37.90, 0x3498DB, 15.0}); // Greek Archaic City-States (#3498DB)
            empireCores.add(new double[]{31.88, 31.00, 0xD35400, 15.0}); // 21st Dynasty Egypt / Tanis (#D35400)
            empireCores.add(new double[]{77.20, 28.60, 0xF39C12, 18.0}); // Vedic Kuru-Panchala Janapadas (#F39C12)
            empireCores.add(new double[]{31.80, 18.50, 0xE67E22, 16.0}); // Kingdom of Kush / Napata (#E67E22)
            empireCores.add(new double[]{-94.76, 17.75, 0x1ABC9C, 14.0});// Olmec San Lorenzo / La Venta (#1ABC9C)
            empireCores.add(new double[]{-77.18, -9.60, 0x16A085, 14.0}); // Chavin de Huantar Andes (#16A085)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North America (#3F51B5)
        } else if (year <= -100L) {
            // -300 BP (Hellenistic & Maurya: Ashoka Empire, Seleucid, Ptolemaic, Rome)
            empireCores.add(new double[]{85.14, 25.61, 0xF59E0B, 22.0}); // Maurya Empire Pataliputra (#F59E0B)
            empireCores.add(new double[]{36.20, 36.20, 0x3498DB, 18.0}); // Seleucid Empire Antioch (#3498DB)
            empireCores.add(new double[]{29.92, 31.20, 0xD35400, 16.0}); // Ptolemaic Egypt Alexandria (#D35400)
            empireCores.add(new double[]{12.50, 41.90, 0xDC2626, 16.0}); // Roman Republic Rome (#DC2626)
            empireCores.add(new double[]{10.32, 36.85, 0x9B59B6, 15.0}); // Carthaginian Republic (#9B59B6)
            empireCores.add(new double[]{108.70, 34.34, 0x2ECC71, 20.0});// Qin & Warring States Xianyang (#2ECC71)
            empireCores.add(new double[]{22.50, 40.75, 0x2980B9, 14.0}); // Antigonid Macedonia (#2980B9)
            empireCores.add(new double[]{-89.80, 17.75, 0x06B6D4, 14.0});// Preclassic Maya El Mirador (#06B6D4)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North America (#3F51B5)
        } else if (year <= 250L) {
            // An 0 (Pax Romana & Han Dynasty)
            empireCores.add(new double[]{12.50, 41.90, 0xDC2626, 24.0}); // Roman Empire Rome / Augustus (#DC2626)
            empireCores.add(new double[]{108.94, 34.26, 0xEF4444, 24.0});// Western Han Dynasty Chang'an (#EF4444)
            empireCores.add(new double[]{44.58, 33.09, 0x10B981, 18.0}); // Parthian Empire Ctesiphon (#10B981)
            empireCores.add(new double[]{72.82, 33.75, 0xF59E0B, 18.0}); // Kushan Empire Taxila (#F59E0B)
            empireCores.add(new double[]{80.50, 16.50, 0xD97706, 18.0}); // Satavahana Dynasty Deccan (#D97706)
            empireCores.add(new double[]{38.72, 14.13, 0xE67E22, 15.0}); // Kingdom of Aksum (#E67E22)
            empireCores.add(new double[]{-89.62, 17.22, 0x06B6D4, 14.0});// Maya Lowlands Tikal Precursor (#06B6D4)
            empireCores.add(new double[]{-98.88, 19.69, 0x0891B2, 14.0});// Teotihuacan Basin of Mexico (#0891B2)
            empireCores.add(new double[]{130.40, 33.60, 0x9B59B6, 12.0}); // Yayoi Japan (#9B59B6)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // Hopewell Culture North America (#3F51B5)
        } else if (year <= 750L) {
            // 536 (Late Antique: Justinian Byzantium, Sasanian Khosrow, Northern Wei)
            empireCores.add(new double[]{28.98, 41.01, 0x9333EA, 22.0}); // Byzantine Empire Constantinople (#9333EA)
            empireCores.add(new double[]{44.58, 33.09, 0x10B981, 18.0}); // Sasanian Empire Ctesiphon (#10B981)
            empireCores.add(new double[]{112.45, 34.62, 0xEF4444, 22.0});// Northern Wei / Liang Luoyang (#EF4444)
            empireCores.add(new double[]{79.92, 27.05, 0xF59E0B, 20.0}); // Harsha Empire / Post-Gupta India (#F59E0B)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 16.0});  // Merovingian Frankish Kingdom (#2563EB)
            empireCores.add(new double[]{-4.02, 39.86, 0x3B82F6, 15.0}); // Visigothic Kingdom Toledo (#3B82F6)
            empireCores.add(new double[]{-89.62, 17.22, 0x06B6D4, 14.0});// Classic Maya Tikal & Calakmul (#06B6D4)
            empireCores.add(new double[]{-68.67, -16.55, 0x1ABC9C, 15.0});// Tiwanaku Altiplano Andes (#1ABC9C)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
            empireCores.add(new double[]{-90.0, 35.0, 0x3F51B5, 20.0});  // North America (#3F51B5)
        } else if (year <= 1150L) {
            // 1000 (High Medieval: Song Dynasty, Fatimid Caliphate, Holy Roman Empire)
            empireCores.add(new double[]{114.35, 34.79, 0xEF4444, 24.0});// Song Dynasty Kaifeng (#EF4444)
            empireCores.add(new double[]{31.24, 30.04, 0x10B981, 20.0}); // Fatimid Caliphate Cairo (#10B981)
            empireCores.add(new double[]{11.58, 48.14, 0x2563EB, 18.0}); // Holy Roman Empire (#2563EB)
            empireCores.add(new double[]{28.98, 41.01, 0x9333EA, 18.0}); // Byzantine Empire Basil II (#9333EA)
            empireCores.add(new double[]{79.13, 10.79, 0xF59E0B, 18.0}); // Chola Empire Thanjavur (#F59E0B)
            empireCores.add(new double[]{30.52, 50.45, 0x3B82F6, 18.0}); // Kievan Rus Kiev (#3B82F6)
            empireCores.add(new double[]{-4.78, 37.89, 0x059669, 16.0}); // Cordoba Caliphate Al-Andalus (#059669)
            empireCores.add(new double[]{-88.57, 20.68, 0x06B6D4, 14.0});// Maya Toltec Chichen Itza (#06B6D4)
            empireCores.add(new double[]{-90.06, 38.66, 0x3F51B5, 15.0});// Cahokia Mississippian Metropolis (#3F51B5)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
        } else if (year <= 1400L) {
            // 1324 (Mansa Musa Mali & Mongol Khanates)
            empireCores.add(new double[]{-8.30, 11.38, 0xF59E0B, 22.0}); // Mali Empire Mansa Musa / Niani (#F59E0B)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 25.0});// Yuan Dynasty Khanbaliq/Beijing (#EF4444)
            empireCores.add(new double[]{46.29, 38.08, 0x10B981, 20.0}); // Ilkhanate Tabriz (#10B981)
            empireCores.add(new double[]{47.25, 47.15, 0x3B82F6, 22.0}); // Golden Horde Sarai (#3B82F6)
            empireCores.add(new double[]{77.21, 28.61, 0xD97706, 20.0}); // Delhi Sultanate (#D97706)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 16.0});  // Kingdom of France (#2563EB)
            empireCores.add(new double[]{31.24, 30.04, 0x059669, 18.0}); // Mamluk Sultanate Cairo (#059669)
            empireCores.add(new double[]{-0.13, 51.51, 0xDC2626, 14.0}); // Kingdom of England (#DC2626)
            empireCores.add(new double[]{-99.13, 19.43, 0x06B6D4, 14.0});// Aztec Mexica Tenochtitlan Foundation (#06B6D4)
            empireCores.add(new double[]{-71.97, -13.53, 0x1ABC9C, 15.0});// Inca Cusco Foundation (#1ABC9C)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
        } else if (year <= 1550L) {
            // 1491 & 1492 (Columbian Horizon: Aztec, Inca, Ming, Renaissance Europe)
            empireCores.add(new double[]{-99.13, 19.43, 0x06B6D4, 18.0});// Aztec Triple Alliance Tenochtitlan (#06B6D4)
            empireCores.add(new double[]{-71.97, -13.53, 0x10B981, 22.0});// Inca Empire Tawantinsuyu (#10B981)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 25.0});// Ming Dynasty Beijing (#EF4444)
            empireCores.add(new double[]{28.98, 41.01, 0x059669, 20.0}); // Ottoman Empire Bayezid II (#059669)
            empireCores.add(new double[]{-3.70, 40.42, 0xDC2626, 16.0}); // Spanish Crown Castile & Aragon (#DC2626)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 16.0});  // Kingdom of France (#2563EB)
            empireCores.add(new double[]{13.40, 52.52, 0x3B82F6, 16.0}); // Holy Roman Empire (#3B82F6)
            empireCores.add(new double[]{-0.05, 16.27, 0xF59E0B, 18.0}); // Songhai Empire Gao/Timbuktu (#F59E0B)
            empireCores.add(new double[]{76.46, 15.33, 0xD97706, 18.0}); // Vijayanagara Empire Hampi (#D97706)
            empireCores.add(new double[]{22.0, -34.0, 0xF1C40F, 22.0});  // Southern Africa (#F1C40F)
            empireCores.add(new double[]{134.0, -24.0, 0xC0392B, 28.0}); // Australia (#C0392B)
        } else if (year <= 1700L) {
            // 1639 (Sakoku Japan & Westphalia: Tokugawa, Qing, Mughal, Ottoman)
            empireCores.add(new double[]{139.69, 35.69, 0xE11D48, 15.0});// Tokugawa Shogunate Edo (#E11D48)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 25.0});// Ming / Qing Dynasty China (#EF4444)
            empireCores.add(new double[]{77.21, 28.61, 0xF59E0B, 22.0}); // Mughal Empire Shah Jahan (#F59E0B)
            empireCores.add(new double[]{28.98, 41.01, 0x059669, 20.0}); // Ottoman Empire Murad IV (#059669)
            empireCores.add(new double[]{51.68, 32.65, 0x10B981, 18.0}); // Safavid Empire Isfahan (#10B981)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 16.0});  // Kingdom of France Louis XIII (#2563EB)
            empireCores.add(new double[]{-3.70, 40.42, 0xDC2626, 18.0}); // Spanish Global Empire (#DC2626)
            empireCores.add(new double[]{37.62, 55.75, 0x7C3AED, 24.0}); // Tsardom of Russia Moscow (#7C3AED)
            empireCores.add(new double[]{-0.13, 51.51, 0x3B82F6, 14.0}); // Kingdom of England (#3B82F6)
            empireCores.add(new double[]{-99.13, 19.43, 0xD97706, 18.0});// Viceroyalty of New Spain (#D97706)
            empireCores.add(new double[]{-77.04, -12.05, 0xEA580C, 18.0});// Viceroyalty of Peru (#EA580C)
        } else if (year <= 1850L) {
            // 1800 (Industrial Revolution & Global Napoleonic / Sovereign Era)
            empireCores.add(new double[]{-0.13, 51.51, 0xDC2626, 22.0}); // British Empire (Great Britain & Ireland) (#DC2626)
            empireCores.add(new double[]{88.36, 22.57, 0xDC2626, 20.0}); // British East India Company (Bengal & India) (#DC2626)
            empireCores.add(new double[]{-71.21, 46.81, 0xDC2626, 22.0}); // British North America: Canada & Quebec (#DC2626)
            empireCores.add(new double[]{-97.0, 54.0, 0xDC2626, 28.0});  // British Rupert's Land & Hudson's Bay Company (#DC2626)
            empireCores.add(new double[]{18.42, -33.92, 0xDC2626, 18.0}); // British / Dutch Cape Colony (South Africa) (#DC2626)
            empireCores.add(new double[]{151.21, -33.87, 0xDC2626, 18.0});// British Colony of New South Wales (Sydney) (#DC2626)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 18.0});  // French Republic & Sister Republics (#2563EB)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 28.0});// Qing Empire China Jiaqing (#EF4444)
            empireCores.add(new double[]{30.32, 59.93, 0x7C3AED, 26.0}); // Russian Empire Saint Petersburg & European Russia (#7C3AED)
            empireCores.add(new double[]{73.0, 55.0, 0x7C3AED, 30.0});   // Russian Empire: Siberia & Urals (#7C3AED)
            empireCores.add(new double[]{129.7, 62.0, 0x7C3AED, 35.0});  // Russian Empire: Far East & Yakutsk (#7C3AED)
            empireCores.add(new double[]{-135.33, 57.05, 0x7C3AED, 24.0});// Russian America: Alaska, Sitka & Kodiak (#7C3AED)
            empireCores.add(new double[]{16.37, 48.21, 0xF59E0B, 16.0}); // Austrian Habsburg Monarchy (#F59E0B)
            empireCores.add(new double[]{13.40, 52.52, 0x1E293B, 15.0}); // Kingdom of Prussia (#1E293B)
            empireCores.add(new double[]{-77.04, 38.91, 0x3B82F6, 22.0}); // United States of America (#3B82F6)
            empireCores.add(new double[]{28.98, 41.01, 0x059669, 22.0}); // Sublime Ottoman Empire (#059669)
            empireCores.add(new double[]{-6.8, 34.0, 0x059669, 18.0});   // Alaouite Sultanate of Morocco (#059669)
            empireCores.add(new double[]{-3.70, 40.42, 0xEA580C, 16.0});  // Spanish Crown Spain (#EA580C)
            empireCores.add(new double[]{-99.13, 19.43, 0xEA580C, 24.0}); // Spanish Empire: Viceroyalty of New Spain (#EA580C)
            empireCores.add(new double[]{-77.04, -12.05, 0xEA580C, 22.0});// Spanish Empire: Viceroyalty of Peru (#EA580C)
            empireCores.add(new double[]{-58.38, -34.60, 0xEA580C, 20.0});// Spanish Empire: Viceroyalty of Rio de la Plata (#EA580C)
            empireCores.add(new double[]{121.0, 14.6, 0xEA580C, 18.0});  // Spanish Captaincy General of the Philippines (#EA580C)
            empireCores.add(new double[]{-9.14, 38.72, 0x10B981, 14.0});  // Kingdom of Portugal (#10B981)
            empireCores.add(new double[]{-43.17, -22.90, 0x10B981, 26.0});// Portuguese State of Brazil (#10B981)
            empireCores.add(new double[]{106.85, -6.21, 0x10B981, 24.0}); // Dutch East Indies / VOC Java & Nusantara (#10B981)
            empireCores.add(new double[]{73.86, 18.52, 0xD97706, 18.0}); // Maratha Confederacy Pune (#D97706)
            empireCores.add(new double[]{139.69, 35.69, 0xE11D48, 14.0});// Tokugawa Shogunate Japan (#E11D48)
            empireCores.add(new double[]{126.98, 37.57, 0x8B5CF6, 12.0});// Joseon Dynasty Korea (#8B5CF6)
            empireCores.add(new double[]{51.39, 35.69, 0x0D9488, 16.0}); // Qajar Dynasty Persia (#0D9488)
            empireCores.add(new double[]{69.17, 34.53, 0x0284C7, 16.0}); // Durrani Afghan Empire (#0284C7)
            empireCores.add(new double[]{69.0, 41.0, 0x0284C7, 22.0});   // Central Asian Khanates: Bukhara, Kokand, Khiva (#0284C7)
            empireCores.add(new double[]{100.50, 13.75, 0xF97316, 15.0});// Kingdom of Siam Rattanakosin (#F97316)
            empireCores.add(new double[]{105.8, 21.0, 0x16A085, 16.0});  // Nguyen Dynasty Vietnam (#16A085)
            empireCores.add(new double[]{96.1, 16.8, 0xD97706, 16.0});   // Konbaung Dynasty Burma (#D97706)
            empireCores.add(new double[]{5.23, 13.06, 0x15803D, 18.0});  // Sokoto Caliphate Sahel (#15803D)
            empireCores.add(new double[]{15.0, -5.0, 0x15803D, 22.0});   // Kingdom of Kongo & Central Africa (#15803D)
            empireCores.add(new double[]{37.47, 12.60, 0x84CC16, 15.0}); // Ethiopian Solomonic Empire Gondar (#84CC16)
            empireCores.add(new double[]{47.0, -19.0, 0x84CC16, 18.0});  // Merina Kingdom Madagascar (#84CC16)
            empireCores.add(new double[]{28.5, -31.5, 0xF59E0B, 18.0});  // Southern African Kingdoms: Xhosa, Zulu, Khoisan (#F59E0B)
            empireCores.add(new double[]{39.0, -6.0, 0x0D9488, 18.0});   // Omani Swahili Sultanate Zanzibar (#0D9488)
            empireCores.add(new double[]{133.5, -24.0, 0xC0392B, 32.0}); // Indigenous Australian Domains (#C0392B)
            empireCores.add(new double[]{175.0, -39.0, 0xE67E22, 16.0}); // Maori Iwi Domains New Zealand (#E67E22)
            empireCores.add(new double[]{-105.0, 45.0, 0x06B6D4, 25.0}); // Great Plains Indigenous Nations: Lakota / Comanche (#06B6D4)
            empireCores.add(new double[]{-90.0, 68.0, 0x607D8B, 30.0});  // Arctic Inuit / Thule Domain (#607D8B)
        } else if (year <= 1925L) {
            // 1900 (Belle Époque & Global Empires)
            empireCores.add(new double[]{-0.13, 51.51, 0xDC2626, 26.0}); // British Empire Global (#DC2626)
            empireCores.add(new double[]{2.35, 48.86, 0x2563EB, 20.0});  // French Colonial Empire (#2563EB)
            empireCores.add(new double[]{13.40, 52.52, 0x1E293B, 16.0}); // German Empire Berlin (#1E293B)
            empireCores.add(new double[]{30.32, 59.93, 0x7C3AED, 28.0}); // Russian Empire Nicholas II (#7C3AED)
            empireCores.add(new double[]{-77.04, 38.91, 0x3B82F6, 25.0}); // United States (#3B82F6)
            empireCores.add(new double[]{139.69, 35.69, 0xE11D48, 16.0});// Empire of Japan Meiji (#E11D48)
            empireCores.add(new double[]{16.37, 48.21, 0xF59E0B, 16.0}); // Austro-Hungarian Empire (#F59E0B)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 25.0});// Qing Empire China (#EF4444)
        } else if (year <= 1975L) {
            // 1950 (Cold War & Decolonization)
            empireCores.add(new double[]{-77.04, 38.91, 0x2563EB, 30.0}); // Western Bloc / NATO (USA) (#2563EB)
            empireCores.add(new double[]{37.62, 55.75, 0xDC2626, 30.0}); // Eastern Bloc / Warsaw Pact (USSR) (#DC2626)
            empireCores.add(new double[]{116.41, 39.90, 0xEF4444, 26.0});// People's Republic of China (#EF4444)
            empireCores.add(new double[]{77.21, 28.61, 0xF59E0B, 22.0}); // Republic of India (Nehru) (#F59E0B)
            empireCores.add(new double[]{106.85, -6.21, 0x10B981, 20.0});// Non-Aligned Movement (Bandung) (#10B981)
            empireCores.add(new double[]{2.35, 48.86, 0x3B82F6, 16.0});  // Western Europe (#3B82F6)
            empireCores.add(new double[]{-47.93, -15.78, 0x059669, 22.0});// Latin America (#059669)
        } else {
            // 2000 to 2060 (Contemporary & Future Multipolar World)
            empireCores.add(new double[]{-77.0, 38.9, 0x2563EB, 30.0});  // North America (USA/Canada) (#2563EB)
            empireCores.add(new double[]{4.35, 50.85, 0x3B82F6, 18.0});  // European Union Brussels (#3B82F6)
            empireCores.add(new double[]{116.4, 39.9, 0xDC2626, 30.0});  // China / East Asia (#DC2626)
            empireCores.add(new double[]{77.2, 28.6, 0xF59E0B, 22.0});   // India / South Asia (#F59E0B)
            empireCores.add(new double[]{37.6, 55.7, 0x7C3AED, 32.0});   // Russia / Northern Eurasia (#7C3AED)
            empireCores.add(new double[]{-47.9, -15.8, 0x10B981, 25.0}); // Latin America (#10B981)
            empireCores.add(new double[]{31.2, 30.0, 0xEA580C, 18.0});   // Middle East & North Africa (#EA580C)
            empireCores.add(new double[]{3.38, 6.52, 0xD97706, 24.0});   // Sub-Saharan Africa (#D97706)
            empireCores.add(new double[]{106.8, -6.2, 0x06B6D4, 22.0});  // ASEAN Southeast Asia (#06B6D4)
            empireCores.add(new double[]{149.1, -35.3, 0x14B8A6, 24.0}); // Oceania / Australia (#14B8A6)
        }

        // ---------------------------------------------------------------------------
        // PALEOLITHIC & DEEP-TIME SOVEREIGNTY / TAXONOMIC EXPANSION BRACKETS
        // ---------------------------------------------------------------------------
        if (year <= -85000L) {
            // MIS 5e Paleolithic Hominin Bio-Geographical & Taxonomic Ranges (-100,000 BP)
            List<OrographicGlottologPropagator.CulturalSeed> sovSeeds = new ArrayList<>();
            // Homo sapiens: Pan-African Multiregional Domain (#D35400)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xD35400, 1.5, "Homo sapiens African Rift Core (Omo/Herto)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-4.0, 31.5, 0xD35400, 1.4, "Homo sapiens North African Domain (Jebel Irhoud)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(26.0, -28.0, 0xD35400, 1.4, "Homo sapiens Southern African Domain (Florisbad/Klasies)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(5.0, 7.0, 0xD35400, 1.4, "Homo sapiens West African Domain (Iwo Eleru)"));
            // Homo sapiens: Pioneer Out-of-Africa Dispersal (#F39C12)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.3, 32.7, 0xF39C12, 1.2, "Homo sapiens Levantine Corridor (Skhul/Qafzeh)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(48.0, 23.0, 0xF39C12, 1.3, "Homo sapiens Arabian Green Corridor (Jebel Faya)"));
            // Homo neanderthalensis: Classical Western Mousterian Domain (#1F618D)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(1.5, 45.0, 0x1F618D, 1.4, "Homo neanderthalensis Western European Core (La Ferrassie/Spy)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-5.5, 40.0, 0x1F618D, 1.3, "Homo neanderthalensis Iberian Domain (El Sidron/Gibraltar)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(16.0, 46.0, 0x1F618D, 1.3, "Homo neanderthalensis Central European Range (Krapina/Vindija)"));
            // Homo neanderthalensis: Near East & Zagros Domain (#1A5276)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x1A5276, 1.3, "Homo neanderthalensis Zagros-Taurus Highland Range (Shanidar)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(67.0, 39.0, 0x1A5276, 1.3, "Homo neanderthalensis Central Asian Range (Teshik-Tash)"));
            // Denisovans: Altai & Siberian High-Latitude Domain (#229954)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x229954, 1.5, "Denisovan Altai-Siberian Range (Denisova Cave)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(102.5, 35.5, 0x229954, 1.4, "Denisovan Tibetan Plateau Range (Baishiya Karst)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(115.0, 40.0, 0x229954, 1.4, "Denisovan / Archaic North China Range (Xujiayao/Harbin)"));
            // Eastern Archaic Hominins / Late Erectus & Floresiensis (#7D3C98)
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(110.0, -7.5, 0x7D3C98, 1.5, "Homo erectus soloensis / Sundaland Archaic (Ngandong/Java)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(120.4, -8.5, 0x7D3C98, 1.2, "Homo floresiensis Island Endemic Domain (Liang Bua)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(105.0, 18.0, 0x7D3C98, 1.4, "Eastern Archaic Indochina Range (Tam Pa Ling)"));
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 22.0, 0x7D3C98, 1.4, "South Asian Archaic Narmada Range"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(sovSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
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
            // -74,000 BP: Youngest Toba Tuff Super-Eruption Bottleneck & MIS 4 Glacial Refugia
            List<OrographicGlottologPropagator.CulturalSeed> tobaSeeds = new ArrayList<>();
            // Sapiens Coastal & Highland African Refugia (#D35400 & #F39C12)
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0xD35400, 1.2, "Pinnacle Point Coastal Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(24.0, -34.0, 0xD35400, 1.2, "Klasies River Mouth Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xD35400, 1.3, "East African Highland Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0xD35400, 1.1, "Taforalt Maghreb Coastal Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.3, 32.7, 0xF39C12, 1.0, "Levantine Residual Contact (Qafzeh)"));
            // Neanderthal Southern European & Zagros Refugia (#1F618D & #1A5276)
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-5.5, 36.1, 0x1F618D, 1.2, "Gibraltar Gorham's Cave Neanderthal Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(1.5, 43.5, 0x1F618D, 1.2, "Pyrenean / Aquitaine Mousterian Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(15.0, 40.5, 0x1F618D, 1.1, "Southern Italian / Mediterranean Mousterian"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x1A5276, 1.2, "Zagros Highland Shanidar Refugium"));
            // Denisovan Altai & Tibetan Cold Refugia (#229954)
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x229954, 1.3, "Altai Denisova Cave Refugium"));
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(102.5, 35.5, 0x229954, 1.2, "Baishiya High-Altitude Plateau Refugium"));
            // Sundaland Archaic (#7D3C98)
            tobaSeeds.add(new OrographicGlottologPropagator.CulturalSeed(110.0, -7.5, 0x7D3C98, 1.3, "Solo River Late Erectus (Ngandong)"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(tobaSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 32.0f);
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
            // -50,000 BP: MIS 3 Upper Paleolithic Revolution & Sahul Landfall across Wallace Line
            List<OrographicGlottologPropagator.CulturalSeed> mis3Seeds = new ArrayList<>();
            // Homo sapiens: Pan-African Early LSA (#D35400 & #E67E22)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0xD35400, 1.5, "Homo sapiens African Rift Core (Omo/Herto)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0xE67E22, 1.4, "Homo sapiens Southern African Howiesons Poort"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(-5.0, 34.0, 0xD35400, 1.3, "Homo sapiens Maghreb Bladelet Complex"));
            // Homo sapiens: Initial Upper Paleolithic (IUP) Eurasian Expansion (#F39C12 & #E74C3C)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.5, 0xF39C12, 1.3, "Levant IUP Ahmarian / Ksar Akil Hub"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(25.4, 43.0, 0xF39C12, 1.3, "Balkan IUP Bacho Kiro Pioneer Front"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(71.2, 57.7, 0xF39C12, 1.4, "Siberian IUP Ust'-Ishim Mammoth Hunters"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 15.3, 0xF39C12, 1.4, "South Asian Post-Toba Microblade Expansion (Jwalapuram)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(115.9, 39.7, 0xF39C12, 1.4, "East Asian Modern Humans (Tianyuan)"));
            // Homo sapiens: Sahul Aboriginal Colonization across Wallace Line (#C0392B & #EA580C)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(132.9, -12.5, 0xC0392B, 1.6, "Sahul Pioneer Domain (Madjedbebe / Arnhem Land)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(125.0, -16.0, 0xC0392B, 1.5, "Kimberley Early Edge-Ground Axe Domain"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -33.7, 0xC0392B, 1.5, "Willandra Lakes / Lake Mungo Ancestral Domain"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(115.0, -34.0, 0xC0392B, 1.4, "Southwestern Sahul Domain (Devil's Lair)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -5.5, 0xEA580C, 1.5, "Papuan Highland Sahul Domain (Huon)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(147.0, -42.0, 0xC0392B, 1.3, "Tasmanian Southern Sahul Foragers"));
            // Neanderthal Late Châtelperronian & Regressing Mousterian (#1F618D & #1A5276)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x1F618D, 1.3, "Western European Neanderthal (La Ferrassie/Spy)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(16.0, 46.3, 0x1F618D, 1.2, "Central European Late Mousterian (Vindija)"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(40.0, 44.2, 0x1A5276, 1.2, "Caucasus Late Neanderthal (Mezmaiskaya)"));
            // Denisovans (#229954)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0x229954, 1.4, "Denisovan Altai Domain"));
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(102.5, 35.5, 0x229954, 1.3, "Denisovan Tibetan Plateau Domain"));
            // Sundaland Late Archaic (#7D3C98)
            mis3Seeds.add(new OrographicGlottologPropagator.CulturalSeed(110.0, -7.5, 0x7D3C98, 1.4, "Sundaland Ngandong / Solo River"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(mis3Seeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
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
            // -25,000 to -18,000 BP: Gravettian Mammoth Steppe & Last Glacial Maximum (LGM) Refugia
            List<OrographicGlottologPropagator.CulturalSeed> lgmSeeds = new ArrayList<>();
            // Franco-Cantabrian Solutrean & Mediterranean Epigravettian (#E74C3C & #9B59B6)
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0xE74C3C, 1.4, "Franco-Cantabrian Solutrean Refuge (Laugerie-Haute)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-4.5, 43.4, 0xE74C3C, 1.3, "Cantabrian Cave Sanctuary Domain (Altamira)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(15.5, 41.7, 0x9B59B6, 1.3, "Italian Epigravettian Refuge (Grotta Paglicci)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(23.0, 38.5, 0x9B59B6, 1.2, "Balkan / Aegean Coastal Epigravettian"));
            // Central & Eastern European Gravettian Mammoth Hunters (#3498DB & #1ABC9C)
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(16.5, 48.8, 0x3498DB, 1.4, "Pavlovian / Moravian Mammoth Camp (Dolni Vestonice)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x1ABC9C, 1.4, "Don Steppe Mammoth Hunters (Kostenki)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.0, 50.5, 0x1ABC9C, 1.3, "Dnepr River Mammoth Bone Dwellings (Mezhirich)"));
            // Arctic Siberia & Beringian Standstill (#16A085 & #00BCD4)
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(135.4, 70.7, 0x16A085, 1.5, "Yana RHS Arctic Mammoth Hunters"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-140.7, 67.1, 0x00BCD4, 1.5, "Beringian Standstill Refugium (Bluefish Caves)"));
            // Levant Kebaran & Nile Valley (#F39C12 & #D35400)
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.7, 0xF39C12, 1.2, "Kebaran Epipaleolithic Encampments (Ohalo II)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(32.5, 25.5, 0xD35400, 1.3, "Nile Valley Wadi Kubbaniya Foragers"));
            // Asian & Sahul Domains
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(115.0, 30.0, 0x2ECC71, 1.5, "Yangtze / South China Paleolithic (Yuchanyan)"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(78.0, 22.0, 0xD35400, 1.4, "Indian Subcontinent Mesolithic Precursor"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xC0392B, 1.6, "Sahul Aboriginal Nations"));
            lgmSeeds.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0xF1C40F, 1.4, "South African Coastal Robberg LSA"));

            BufferedImage orographicImg = OrographicGlottologPropagator.propagateCulturalSeeds(lgmSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
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

        // Orographic cost-distance propagation with finite sovereign reach limit (38.0)
        List<OrographicGlottologPropagator.CulturalSeed> sovSeeds = new ArrayList<>();
        for (double[] ec : empireCores) {
            sovSeeds.add(new OrographicGlottologPropagator.CulturalSeed(ec[0], ec[1], (int) ec[2], ec[3] / 18.0, "Polity"));
        }
        BufferedImage sovImg = OrographicGlottologPropagator.propagateCulturalSeeds(sovSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 38.0f);

        // Pre-generate orographic tribal background for stateless frontiers
        List<OrographicGlottologPropagator.CulturalSeed> tribalSeeds = CliopatriaPolityVectorReader.getTribalDomainSeeds(year);
        BufferedImage tribalBg = OrographicGlottologPropagator.propagateCulturalSeeds(tribalSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);

        // Apply hominin occupancy and assign authentic regional tribal domains outside imperial reach
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (lat < -60.0 && year < 1900L) {
                    // Antarctica before modern era: uninhabited land → neutral gray
                    sovImg.setRGB(x, y, 0x2D3748);
                } else if (occWeight <= 0.001 && year < 1900L) {
                    // Uninhabited land (remote tundra, uninhabited islands) → neutral gray
                    sovImg.setRGB(x, y, 0x2D3748);
                } else {
                    int currentRgb = sovImg.getRGB(x, y) & 0xFFFFFF;
                    if (currentRgb == 0x374151 || currentRgb == 0x000000) {
                        int unassignedColor = (year >= 1900L && tribalBg != null) ? tribalBg.getRGB(x, y) : 0x2D3748;
                        sovImg.setRGB(x, y, unassignedColor);
                    }
                }
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(sovImg);
    }

    // --- 3. ISOGLOSS TENSOR MAP ---
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

    // --- 4. KINSHIP TENSOR MAP ---
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

    // --- 5. RITUALS TENSOR MAP ---
    /**
     * Rasterize rituals map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic religious & confessional systems for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernRitualsMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        if (year <= -85000L) {
            // MIS 5e Documented Symbolic Sanctuaries, Ochre Shrines & Intentional Burials (-100,000 BP)
            List<OrographicGlottologPropagator.CulturalSeed> paleoRituals = new ArrayList<>();
            // Levant Sapiens & Neanderthal Intentional Mortuary Sites (Qafzeh, Skhul, Tabun)
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(35.3, 32.7, 0x10B981, 1.2, "Levant Skhul/Qafzeh Ochre & Shell Burials"));
            // South African Coastal Ochre Processing & Shellfish Shrines
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0x059669, 1.2, "Pinnacle Point Heat-Treated Silcrete & Ochre Shrines"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(21.2, -34.4, 0x047857, 1.1, "Blombos Precursor Ochre Engravings"));
            // North African Perforated Shell Adornment Tradition
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0x34D399, 1.2, "Taforalt / Bizmoune Perforated Nassarius Beads"));
            // East African Rift Ochre Processing
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(41.8, 9.6, 0x6EE7B7, 1.1, "Porc-Epic Cave Ochre Processing Center"));
            // European Neanderthal Symbolic Sanctuaries
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.7, 44.1, 0x2563EB, 1.2, "Bruniquel Deep Cave Stalagmite Ring Sanctuary"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x3B82F6, 1.1, "La Ferrassie Neanderthal Mortuary Shrines"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(15.9, 46.2, 0x1D4ED8, 1.1, "Krapina Raptor Talon & Eagle Feather Cult"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x60A5FA, 1.1, "Shanidar Zagros Mortuary Traditions"));
            // Denisovan & Asian Archaic Art & Depositions
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0xD97706, 1.3, "Denisova Cave Chloritolite Jewelry & Polished Bone Art"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(113.7, 34.1, 0xB45309, 1.2, "Lingjing Incised Bone Engravings & Red Pigments"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(111.5, -7.4, 0x8B5CF6, 1.2, "Ngandong Solo River Megafauna Calvaria Depositions"));

            // Finite reach limit (25.0f): symbolic behaviour radiates around proven sanctuaries, remaining land is slate gray #2D3748
            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(paleoRituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 25.0f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        ritImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(ritImg);
        } else if (year <= -65000L) {
            // -74,000 BP: Toba Volcanic Winter Symbolic Refugia (Coastal & Highland Shrines)
            List<OrographicGlottologPropagator.CulturalSeed> tobaRituals = new ArrayList<>();
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0x059669, 1.2, "Pinnacle Point Ochre Processing & Shellfish Shrines"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(24.4, -34.1, 0x10B981, 1.1, "Klasies River Mortuary Deposition Focus"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0x34D399, 1.2, "Taforalt Perforated Shell Adornment Tradition"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(41.8, 9.6, 0x6EE7B7, 1.1, "Porc-Epic High-Altitude Ochre Cave"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(-5.3, 36.1, 0x2563EB, 1.0, "Gorham's Cave Neanderthal Bedrock Engravings"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x3B82F6, 1.1, "Perigord Neanderthal Burials (La Ferrassie)"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x60A5FA, 1.1, "Shanidar Neanderthal Flower/Pollen Mortuary Cult"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0xD97706, 1.2, "Denisova Cave Hearth Pigments & Bone Personal Adornments"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(tobaRituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 22.0f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        ritImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(ritImg);
        } else if (year <= -35000L) {
            // -50,000 BP: MIS 3 Upper Paleolithic Cave Art & Sahul Dreamtime Landfall
            List<OrographicGlottologPropagator.CulturalSeed> mis3Rituals = new ArrayList<>();
            // Sahul Rock Art & Mortuary Shrines (Madjedbebe & Sulawesi)
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(119.8, -4.9, 0xA04000, 1.3, "Sulawesi Leang Tedongnge Warty Pig Cave Art"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(132.9, -12.5, 0xC0392B, 1.5, "Madjedbebe Ochre Palettes & Grinding Stones"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(125.0, -16.0, 0xD35400, 1.4, "Kimberley Gwion Gwion Rock Art Traditions"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -33.7, 0xE67E22, 1.4, "Lake Mungo Red Ochre Intentional Burial"));
            // African LSA ochre
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(21.2, -34.4, 0x10B981, 1.3, "Blombos/Diepkloof Engraved Ostrich Eggshell"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0x059669, 1.3, "Enkapune Ya Muto Shell Bead Traditions"));
            // European Aurignacian / IUP
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(4.4, 44.4, 0x4D7C0F, 1.4, "Chauvet Cave Megafauna Art"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(10.2, 48.5, 0x2E7D32, 1.3, "Swabian Jura Löwenmensch & Mammoth Ivory Flutes"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(25.4, 43.0, 0x8B5CF6, 1.3, "Bacho Kiro IUP Bone Ornaments"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(11.2, 45.6, 0x3B82F6, 1.2, "Fumane Cave Ochre Painted Stones"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(115.9, 39.7, 0xD97706, 1.3, "Tianyuan Cave Red Ochre Adornment"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(mis3Rituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 30.0f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        ritImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(ritImg);
        } else if (year <= -18000L) {
            // -25,000 to -18,000 BP: Gravettian & Solutrean Venus Cults & Mammoth Shrines
            List<OrographicGlottologPropagator.CulturalSeed> lgmRituals = new ArrayList<>();
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(15.4, 48.3, 0x4D7C0F, 1.5, "Willendorf & Dolni Vestonice Ceramic Venus Shrines"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0x2E7D32, 1.4, "Franco-Cantabrian Solutrean Bas-Relief & Polychrome Art"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x8B5CF6, 1.5, "Kostenki Mammoth Bone Architectural Sanctuaries"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(102.8, 52.8, 0xD97706, 1.4, "Mal'ta & Buret' Mammoth Ivory Figurine Traditions"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(135.4, 70.7, 0x00BCD4, 1.4, "Yana RHS Rhinoceros Horn & Ivory Carvings"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.7, 0xB45309, 1.2, "Ohalo II Floral Caches & Kebaran Mortuary Tracks"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xA04000, 1.6, "Sahul Panaramittee Petroglyphs & Sacred Trackways"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(21.2, -34.4, 0x10B981, 1.4, "Boomplaas Cave Symbolic Caches"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(lgmRituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 32.0f);
            for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
                double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                    double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                    if (occWeight <= 0.001) {
                        ritImg.setRGB(x, y, 0x2D3748);
                    }
                }
            }
            return HistoricalMapGenerator.applyAltimetryCoastlineMask(ritImg);
        }

        // 3. Authentic Discrete Categorization & SESHAT Sacred Traditions with Orographic Propagation
        List<OrographicGlottologPropagator.CulturalSeed> ritualSeeds = new ArrayList<>();

        if (year <= -10500L) {
            // Upper Paleolithic & Younger Dryas Cave Sanctuaries & Burials
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-4.5, 43.4, 0x4D7C0F, 1.2, "Franco-Cantabrian Cave Art Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.58, 33.08, 0xB45309, 1.0, "Natufian Skull Burials"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x8B5CF6, 1.3, "Kostenki Mammoth Shrines"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(102.8, 52.8, 0xD97706, 1.4, "Siberian Ochre & Chloritolite Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-103.3, 34.3, 0xC27803, 1.5, "Clovis Red Ochre Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-73.2, -41.5, 0x2E7D32, 1.4, "Andean Paleo Hearth Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Sahul Dreamtime Songlines"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(21.2, -34.4, 0x10B981, 1.5, "African LSA Symbolic Caches"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(32.6, 24.1, 0x15803D, 1.1, "Nile Qadan Cemetery Tradition"));
        } else if (year <= -7000L) {
            // Early Neolithic / Göbekli Tepe / Çatalhöyük / Mehrgarh
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(38.92, 37.22, 0xEA580C, 1.2, "Göbekli Pillar Megalithic Shrines"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(32.83, 37.67, 0xEA580C, 1.1, "Çatalhöyük Bucrania Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.44, 31.87, 0xB45309, 1.0, "Jericho Plastered Skull Sanctuary"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(22.0, 44.5, 0x4D7C0F, 1.1, "Lepenski Vir Fish-God Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(113.6, 33.6, 0xEF4444, 1.3, "Jiahu Ancestral Bone Flute Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(68.05, 29.28, 0xF59E0B, 1.2, "Mehrgarh Terracotta Goddess Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(30.58, 22.53, 0x15803D, 1.2, "Nabta Playa Megalithic Solar Alignments"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-98.0, 19.0, 0xD97706, 1.4, "Mesoamerican Archaic Rituals"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-77.0, -10.0, 0xC27803, 1.4, "Early Andean Hearth Shrines"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "West African Traditional Animism"));
        } else if (year <= -4500L) {
            // Late Neolithic / Chalcolithic / Eridu / Vinča / Carnac
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(45.99, 30.82, 0xB45309, 1.2, "Eridu Enki Temple & Proto-Ziggurats"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(31.37, 26.99, 0x15803D, 1.1, "Badari / Pre-Dynastic Egyptian Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(20.62, 44.76, 0x4D7C0F, 1.2, "Vinča Anthropomorphic Shrines"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-3.0, 47.6, 0x4D7C0F, 1.2, "Carnac Megalithic Alignments"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(109.06, 34.27, 0xEF4444, 1.3, "Yangshao Banpo Mortuary Complexes"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(68.05, 29.28, 0xF59E0B, 1.2, "Mehrgarh Proto-Indus Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-98.0, 19.0, 0xD97706, 1.4, "Mesoamerican Ceremonial Centers"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-77.0, -10.0, 0xC27803, 1.4, "Caral-Supe Sacred Plazas"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "African Ancestral Systems"));
        } else if (year <= -500L) {
            // Bronze & Early Iron Age Empires (Sumer, Egypt, Shang, Vedic)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.4, 32.5, 0xB45309, 1.3, "Mesopotamian Ziggurat Pantheon"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(32.6, 25.7, 0x15803D, 1.3, "Egyptian Amun-Ra & Osiris Religion"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(22.5, 38.5, 0xEA580C, 1.2, "Aegean & Mycenaean Olympian Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(83.0, 25.3, 0xF59E0B, 1.4, "Vedic Sacrificial Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(114.3, 36.1, 0xEF4444, 1.4, "Shang & Zhou Ancestral Oracle Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-1.8, 51.2, 0x4D7C0F, 1.2, "Stonehenge Atlantic Sacred Complex"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-94.8, 17.8, 0xD97706, 1.3, "Olmec Jaguar & Sacred Centers"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-77.2, -9.6, 0xC27803, 1.3, "Chavin Staff God Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.5, 9.5, 0x1B5E20, 1.5, "Sub-Saharan Ancestral Traditions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Aboriginal Songlines"));
        } else if (year <= 500L) {
            // Classical Axial Age & Antiquity (Rome, Han, Maurya, Parthia & Indigenous Cosmologies)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(12.5, 41.9, 0xEA580C, 1.4, "Greco-Roman Civic Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(23.7, 37.9, 0xEA580C, 1.2, "Hellenistic Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.2, 31.8, 0x2563EB, 0.9, "Judaism & Second Temple Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.4, 33.1, 0x0891B2, 1.3, "Zoroastrianism & Mazdeism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(85.1, 25.6, 0xF59E0B, 1.4, "Brahmanism & Classical Hinduism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(80.4, 8.3, 0xEAB308, 1.1, "Theravada Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(108.9, 34.3, 0xEF4444, 1.5, "Confucianism & Daoism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(29.9, 31.2, 0x15803D, 1.1, "Kemetic & Greco-Egyptian Serapis"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-2.0, 48.0, 0x4D7C0F, 1.2, "Celtic Druidic Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(10.0, 53.0, 0x3B82F6, 1.3, "Germanic & Norse Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(30.0, 52.0, 0x64748B, 1.3, "Slavic Nature Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(70.0, 48.0, 0x0284C7, 1.5, "Steppe Tengrism & Horse Sacrifices"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(106.0, 47.0, 0x0284C7, 1.5, "Xiongnu Celestial Tengrism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(90.0, 60.0, 0x7C3AED, 1.6, "Siberian Reindeer Shamanism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(130.0, 62.0, 0x7C3AED, 1.6, "Tungusic & Paleosiberian Spirit Flights"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-98.8, 19.7, 0xD97706, 1.2, "Teotihuacan Cosmic Pantheon"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-89.6, 17.2, 0xEA580C, 1.2, "Maya Solar & Underworld Cosmovision"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-83.0, 39.0, 0x8B5CF6, 1.3, "Hopewell Earthwork & Mortuary Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-102.0, 44.0, 0x3B82F6, 1.4, "Great Plains Medicine Wheel Shamanism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-125.0, 52.0, 0x0D9488, 1.3, "Pacific Northwest Potlatch & Totemism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-100.0, 64.0, 0x64748B, 1.5, "Arctic Inuit & Dene Shamanism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-75.0, -14.0, 0xC27803, 1.3, "Andean Paracas, Moche & Nazca Sacred Ceque"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-60.0, -3.0, 0x15803D, 1.5, "Amazonian Master of Animals Shamanism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.5, 9.5, 0x1B5E20, 1.5, "Sub-Saharan Ancestral Systems"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(24.0, -28.0, 0xA16207, 1.4, "Khoisan Rock Art & Trance Dance"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime Songlines"));
        } else if (year <= 1491L) {
            // Medieval Era (Catholicism, Orthodoxy, Islam, Hinduism, Buddhism)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(12.5, 41.9, 0xEC4899, 1.5, "Roman Catholicism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(28.9, 41.0, 0x8B5CF6, 1.4, "Eastern Orthodoxy"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(30.5, 50.4, 0x8B5CF6, 1.4, "Rus Orthodoxy"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(39.8, 21.4, 0x10B981, 1.6, "Sunni Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.3, 32.0, 0x0D9488, 1.3, "Shia Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-4.8, 37.9, 0x10B981, 1.1, "Al-Andalus Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(83.0, 25.3, 0xF59E0B, 1.5, "Hinduism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(103.9, 13.4, 0xEAB308, 1.3, "Theravada Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(114.3, 34.7, 0xEF4444, 1.5, "Mahayana & Neo-Confucianism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(135.7, 35.0, 0xF43F5E, 1.1, "Shinto & Buddhist Syncretism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(91.1, 29.6, 0xD97706, 1.3, "Vajrayana Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-99.1, 19.4, 0xD97706, 1.3, "Aztec & Maya Cosmic Religions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-71.9, -13.5, 0xC27803, 1.3, "Inca Inti Sun Religion"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-3.0, 16.8, 0x10B981, 1.3, "West African Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.5, 9.5, 0x1B5E20, 1.5, "African Traditional Religions"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
        } else {
            // Early Modern & Global Age of Faiths (1492-1900)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(12.5, 41.9, 0xEC4899, 1.5, "Roman Catholicism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-0.1, 51.5, 0x3B82F6, 1.3, "Protestantism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(37.6, 55.7, 0x8B5CF6, 1.5, "Russian & Eastern Orthodoxy"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(39.8, 21.4, 0x10B981, 1.6, "Sunni Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(51.4, 35.7, 0x0D9488, 1.4, "Shia Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(83.0, 25.3, 0xF59E0B, 1.5, "Hinduism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(100.5, 13.7, 0xEAB308, 1.3, "Theravada Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(116.4, 39.9, 0xEF4444, 1.5, "Confucianism & Mahayana"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(139.7, 35.6, 0xF43F5E, 1.1, "Shintoism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(91.1, 29.6, 0xD97706, 1.3, "Vajrayana Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-99.1, 19.4, 0xEC4899, 1.4, "Catholicism (Latin America)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-74.0, 40.7, 0x3B82F6, 1.3, "Protestantism (North America)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.5, 9.5, 0x1B5E20, 1.5, "Sub-Saharan Traditional"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
        }

        BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(ritualSeeds, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);

        // Apply Multi-Confessional Coexistence Dithering in Historical Cosmopolises & Sacred Contact Hubs
        applyMultiConfessionalDithering(ritImg, year, mask);

        // Apply hominin occupancy filter and black out Antarctica
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (lat < -60.0 || occWeight <= 0.001) {
                    // Antarctica and uninhabited land → neutral gray (not ocean black)
                    ritImg.setRGB(x, y, 0x2D3748);
                }
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(ritImg);
    }

    public static void applyMultiConfessionalDithering(BufferedImage img, long year, BufferedImage mask) {
        List<ConfessionalPocket> pockets = new ArrayList<>();

        if (year <= 500L && year >= -1000L) {
            // Classical Antiquity & Axial Age (Year 0 / Pax Romana)
            // Jerusalem: 65% Judaism (#2563EB), 25% Greco-Roman Paganism (#EA580C), 10% Semitic (#B45309)
            pockets.add(new ConfessionalPocket(35.21, 31.77, 2.5, new int[]{0x2563EB, 0xEA580C, 0xB45309}, new double[]{0.65, 0.25, 0.10}));
            // Alexandria: 50% Greco-Egyptian Isis/Serapis (#15803D), 35% Judaism (#2563EB), 15% Roman (#EA580C)
            pockets.add(new ConfessionalPocket(29.92, 31.20, 2.8, new int[]{0x15803D, 0x2563EB, 0xEA580C}, new double[]{0.50, 0.35, 0.15}));
            // Antioch: 60% Hellenistic (#EA580C), 25% Judaism/Early Christianity (#2563EB), 15% Syrian (#B45309)
            pockets.add(new ConfessionalPocket(36.16, 36.20, 2.5, new int[]{0xEA580C, 0x2563EB, 0xB45309}, new double[]{0.60, 0.25, 0.15}));
            // Rome: 80% Capitoline Polytheism (#EA580C), 12% Isis/Mithras (#15803D), 8% Jewish Quarter (#2563EB)
            pockets.add(new ConfessionalPocket(12.49, 41.90, 2.8, new int[]{0xEA580C, 0x15803D, 0x2563EB}, new double[]{0.80, 0.12, 0.08}));
            // Ctesiphon: 70% Zoroastrian (#0891B2), 20% Mesopotamian Polytheism (#B45309), 10% Judaism (#2563EB)
            pockets.add(new ConfessionalPocket(44.58, 33.09, 2.5, new int[]{0x0891B2, 0xB45309, 0x2563EB}, new double[]{0.70, 0.20, 0.10}));
            // Taxila: 55% Buddhism (#EAB308), 35% Vedic Hinduism (#F59E0B), 10% Indo-Greek Cults (#EA580C)
            pockets.add(new ConfessionalPocket(72.82, 33.74, 2.8, new int[]{0xEAB308, 0xF59E0B, 0xEA580C}, new double[]{0.55, 0.35, 0.10}));
            // Varanasi: 70% Hinduism (#F59E0B), 30% Buddhism (#EAB308)
            pockets.add(new ConfessionalPocket(83.00, 25.31, 2.8, new int[]{0xF59E0B, 0xEAB308}, new double[]{0.70, 0.30}));
            // Dunhuang: 65% Mahayana Buddhism (#EF4444), 25% Daoism (#EF4444), 10% Sogdian Zoroastrianism (#0891B2)
            pockets.add(new ConfessionalPocket(94.66, 40.14, 2.5, new int[]{0xEF4444, 0x0891B2}, new double[]{0.85, 0.15}));
        } else if (year > 500L && year <= 1491L) {
            // Medieval Era
            // Jerusalem: 50% Sunni Islam (#10B981), 30% Eastern Orthodoxy (#8B5CF6), 20% Judaism (#2563EB)
            pockets.add(new ConfessionalPocket(35.21, 31.77, 2.5, new int[]{0x10B981, 0x8B5CF6, 0x2563EB}, new double[]{0.50, 0.30, 0.20}));
            // Constantinople: 80% Orthodoxy (#8B5CF6), 12% Latin Catholicism (#EC4899), 8% Islam/Judaism (#10B981)
            pockets.add(new ConfessionalPocket(28.97, 41.00, 2.8, new int[]{0x8B5CF6, 0xEC4899, 0x10B981}, new double[]{0.80, 0.12, 0.08}));
            // Cordoba: 65% Islam (#10B981), 25% Mozarabic Catholicism (#EC4899), 10% Judaism (#2563EB)
            pockets.add(new ConfessionalPocket(-4.77, 37.88, 2.8, new int[]{0x10B981, 0xEC4899, 0x2563EB}, new double[]{0.65, 0.25, 0.10}));
            // Kerala / Malabar: 60% Hinduism (#F59E0B), 25% Saint Thomas Christianity (#6366F1), 15% Islam (#10B981)
            pockets.add(new ConfessionalPocket(76.27, 9.93, 3.0, new int[]{0xF59E0B, 0x6366F1, 0x10B981}, new double[]{0.60, 0.25, 0.15}));
            // Canton / Guangzhou: 75% Confucian/Mahayana (#EF4444), 15% Muslim Arab (#10B981), 10% Theravada (#EAB308)
            pockets.add(new ConfessionalPocket(113.26, 23.12, 2.8, new int[]{0xEF4444, 0x10B981, 0xEAB308}, new double[]{0.75, 0.15, 0.10}));
        }

        for (ConfessionalPocket cp : pockets) {
            double r2 = cp.radiusDeg() * cp.radiusDeg();
            int minX = (int) Math.clamp(((cp.lon() - cp.radiusDeg() + 180.0) / 360.0) * HistoricalMapGenerator.WIDTH, 0, HistoricalMapGenerator.WIDTH - 1);
            int maxX = (int) Math.clamp(((cp.lon() + cp.radiusDeg() + 180.0) / 360.0) * HistoricalMapGenerator.WIDTH, 0, HistoricalMapGenerator.WIDTH - 1);
            int minY = (int) Math.clamp(((90.0 - (cp.lat() + cp.radiusDeg())) / 180.0) * HistoricalMapGenerator.HEIGHT, 0, HistoricalMapGenerator.HEIGHT - 1);
            int maxY = (int) Math.clamp(((90.0 - (cp.lat() - cp.radiusDeg())) / 180.0) * HistoricalMapGenerator.HEIGHT, 0, HistoricalMapGenerator.HEIGHT - 1);

            for (int py = minY; py <= maxY; py++) {
                double lat = 90.0 - (py + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
                for (int px = minX; px <= maxX; px++) {
                    double lon = -180.0 + (px + 0.5) / HistoricalMapGenerator.WIDTH * 360.0;
                    double d2 = HistoricalScenarioFeaturesCatalog.distSq(lon, lat, cp.lon(), cp.lat());
                    if (d2 <= r2) {
                        int mx = Math.clamp((int) ((px + 0.5) * (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) / HistoricalMapGenerator.WIDTH), 0, (mask != null ? mask.getWidth() : HistoricalMapGenerator.WIDTH) - 1);
                        int my = Math.clamp((int) ((py + 0.5) * (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) / HistoricalMapGenerator.HEIGHT), 0, (mask != null ? mask.getHeight() : HistoricalMapGenerator.HEIGHT) - 1);
                        int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                        if (land == 0) continue;

                        // Deterministic high-entropy spatial dithering
                        int h = ((px * 73856093) ^ (py * 19349663) ^ ((int) year * 83492791)) & 0x7FFFFFFF;
                        double rnd = (h % 10000) / 10000.0;
                        double cum = 0.0;
                        int chosenCol = cp.colors()[0];
                        for (int k = 0; k < cp.weights().length; k++) {
                            cum += cp.weights()[k];
                            if (rnd <= cum) {
                                chosenCol = cp.colors()[k];
                                break;
                            }
                        }
                        img.setRGB(px, py, chosenCol);
                    }
                }
            }
        }
    }

    // --- 6. TECHNOLOGY & SUBSISTENCE TENSOR MAP ---
    public static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic capital intensity & innovation index for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernTechnologyMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        // 2. Ingest empirical HYDE 3.4 population density grid to derive urban agglomeration scaling (Boserup-Kremer effect)
        BufferedImage densityGrid = (year >= -10000L && year <= 2024L) ? Hyde34GridReader.loadForYear(year) : null;

        double baseTech = 25.0;
        if (year <= -10000L) baseTech = 35.0;
        else if (year <= -7000L) baseTech = 55.0;
        else if (year <= -4500L) baseTech = 75.0;
        else if (year <= 0L) baseTech = 95.0;
        else if (year <= 1000L) baseTech = 120.0;
        else if (year <= 1500L) baseTech = 150.0;
        else if (year <= 1800L) baseTech = 180.0;
        else if (year <= 1950L) baseTech = 215.0;
        else baseTech = 240.0;

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

                if (year <= -70000L) {
                    double techVal = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 130.0, 112.0, 92.0);
                    int gray = Math.clamp((int) (techVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                double tech = baseTech;

                // Boserup-Kremer urban agglomeration scaling derived from empirical HYDE 3.4 density
                if (densityGrid != null) {
                    int densSample = densityGrid.getRGB(x, y) & 0xFF;
                    if (densSample > 0) {
                        double urbanBonus = 42.0 * Math.log(1.0 + densSample * 2.0) / Math.log(512.0);
                        tech += urbanBonus;
                    }
                }

                // Authentic historical innovation hearths & metallurgical/agricultural cradle intensity
                double medCore = Math.exp(-(Math.pow(lat - 38.0, 2) + Math.pow(lon - 15.0, 2)) / 140.0); // Greco-Roman Mediterranean & Alexandria
                double fertCrescent = Math.exp(-(Math.pow(lat - 34.0, 2) + Math.pow(lon - 42.0, 2)) / 90.0);  // Fertile Crescent & Mesopotamia
                double chinaCore = Math.exp(-(Math.pow(lat - 34.0, 2) + Math.pow(lon - 114.0, 2)) / 110.0); // Yellow River & Yangtze Sinic Core
                double indiaCore = Math.exp(-(Math.pow(lat - 24.0, 2) + Math.pow(lon - 80.0, 2)) / 100.0);  // Indo-Gangetic & Deccan (Wootz Steel & Maurya)
                double mesoCore = Math.exp(-(Math.pow(lat - 18.0, 2) + Math.pow(lon - (-96.0), 2)) / 70.0); // Mesoamerican Civilizations
                double andesCore = Math.exp(-(Math.pow(lat - (-11.0), 2) + Math.pow(lon - (-76.0), 2)) / 70.0); // Central Andean Civilizations

                if (year <= -4500L) {
                    // Early agricultural / metallurgy emergence (Fertile Crescent, Vinča, Mehrgarh, Yangshao, Caral)
                    tech += (fertCrescent * 55.0 + chinaCore * 45.0 + indiaCore * 40.0 + mesoCore * 25.0 + andesCore * 25.0);
                } else if (year <= 500L) {
                    // Classical Antiquity & Axial Age (Rome, Alexandria, Chang'an, Pataliputra, Ctesiphon)
                    tech += (medCore * 50.0 + fertCrescent * 40.0 + chinaCore * 48.0 + indiaCore * 42.0 + mesoCore * 25.0 + andesCore * 25.0);
                } else if (year <= 1500L) {
                    // Post-Classical & Medieval (Song Dynasty, Islamic Golden Age, Medieval Europe, Chola)
                    double europeCore = Math.exp(-(Math.pow(lat - 48.0, 2) + Math.pow(lon - 8.0, 2)) / 100.0);
                    tech += (chinaCore * 55.0 + fertCrescent * 45.0 + indiaCore * 40.0 + europeCore * 40.0 + medCore * 35.0 + mesoCore * 25.0 + andesCore * 25.0);
                } else if (year <= 1850L) {
                    // Early Modern & First Industrial Revolution (Western Europe Atlantic Arc & East Asia)
                    double europeCore = Math.exp(-(Math.pow(lat - 51.0, 2) + Math.pow(lon - 4.0, 2)) / 80.0);
                    tech += (europeCore * 65.0 + chinaCore * 35.0 + medCore * 30.0 + indiaCore * 25.0);
                } else {
                    // Modern Global Diffusion
                    double europeCore = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 6.0, 2)) / 90.0);
                    tech += (europeCore * 25.0 + chinaCore * 25.0);
                }

                int gray = Math.clamp((int) (tech * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }

    // --- 7. TRADE NETWORK TENSOR MAP ---
    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic global maritime shipping corridors & intermodal supply chains for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernTradeNetworkMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return modernImg;
            }
        }

        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (year <= -85000L) {
            // -100,000 BP Paleolithic Raw Material Circuits
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {36.4, -1.5}, {38.5, 8.5}}, new Color(180, 120, 40), 2.0);   // East African Rift Obsidian – ochre
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}}, new Color(180, 120, 40), 2.0);             // South African Silcrete – ochre
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {35.3, 32.7}, {35.6, 33.0}}, new Color(160, 70, 50), 2.0);                 // Levant Flint – red sienna
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(160, 70, 50), 2.0);                 // Levant / Nile Flint – red sienna
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 45.0}, {0.3, 45.2}}, new Color(200, 140, 30), 2.0);   // Franco-Cantabrian – amber
            drawTradeRoute(g, new double[][]{{9.5, 48.5}, {13.0, 47.8}, {15.8, 46.1}, {17.1, 49.2}}, new Color(200, 140, 30), 2.0);  // Danube / Balkans – amber
            drawTradeRoute(g, new double[][]{{34.2, 44.9}, {38.5, 44.5}, {40.2, 44.2}}, new Color(200, 140, 30), 2.0);                // Caucasus – amber
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {47.1, 34.4}}, new Color(200, 140, 30), 2.0);                              // Zagros – amber
            drawTradeRoute(g, new double[][]{{83.9, 51.4}, {85.5, 51.7}}, new Color(200, 140, 30), 2.0);                              // Altai – amber
            drawTradeRoute(g, new double[][]{{114.5, 40.2}, {115.9, 39.7}}, new Color(200, 140, 30), 2.0);                            // North China – amber
        } else if (year <= -65000L) {
            // -74,000 BP: Toba Volcanic Winter Refugial Raw Material Corridors (Zero Asian Routes due to Ash Blanket)
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}}, new Color(180, 100, 40), 2.0);             // Cape Coastal Refugium Silcrete
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {38.5, 8.5}}, new Color(180, 100, 40), 2.0);                    // East African Highland Obsidian
            drawTradeRoute(g, new double[][]{{-2.4, 34.8}, {-4.0, 35.5}}, new Color(160, 70, 50), 2.0);                                // Maghreb Shell & Silex
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 44.9}}, new Color(190, 130, 30), 2.0);                  // Franco-Cantabrian Neanderthal Refugia
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {46.0, 35.5}}, new Color(190, 130, 30), 2.0);                               // Zagros Cave Corridor
        } else if (year <= -40000L) {
            // -50,000 BP: MIS 3 Sahul Crossing, Levantine IUP, African Ochre & European Keilmesser
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}, {31.9, -27.0}}, new Color(204, 120, 40), 2.5);  // South African Silcrete & Ochre – ochre
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {36.4, -1.5}, {38.5, 8.5}}, new Color(204, 120, 40), 2.5);          // East African Rift Obsidian – ochre
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {35.3, 32.7}, {35.6, 33.0}, {36.2, 34.0}}, new Color(200, 100, 80), 2.5);       // Levant IUP Flint Corridors – coral
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(204, 120, 40), 2.0);                     // Nile Valley – ochre
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 45.0}, {3.5, 47.5}}, new Color(200, 100, 80), 2.5);         // Franco-Cantabrian Châtelperronian Silex – coral
            drawTradeRoute(g, new double[][]{{9.5, 48.5}, {13.0, 47.8}, {15.8, 46.1}, {17.1, 49.2}, {25.0, 43.0}}, new Color(200, 100, 80), 2.5); // Danube/Balkans IUP Radiolarite – coral
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {47.1, 34.4}}, new Color(200, 100, 80), 2.0);                                   // Zagros – coral
            drawTradeRoute(g, new double[][]{{83.9, 51.4}, {85.5, 51.7}}, new Color(200, 100, 80), 2.0);                                   // Denisova / Altai – coral
            drawTradeRoute(g, new double[][]{{132.9, -12.5}, {130.0, -14.0}, {125.0, -16.0}}, new Color(30, 150, 140), 2.5);               // Sahul Northern Ochre & Baler Shells – teal
            drawTradeRoute(g, new double[][]{{143.0, -33.7}, {138.5, -34.5}, {134.0, -24.0}}, new Color(30, 150, 140), 2.5);               // Willandra Lakes / Lake Mungo Ochre – teal
            drawTradeRoute(g, new double[][]{{111.5, 25.5}, {108.0, 23.0}, {102.0, 20.0}}, new Color(200, 100, 80), 2.0);                  // South China / Indochina Quartz – coral
        } else if (year <= -22000L) {
            // -25,000 BP: Gravettian Mammoth Ivory Highway, Western European Marine Shells, Beringia Standstill
            drawTradeRoute(g, new double[][]{{16.5, 48.8}, {17.5, 49.5}, {19.9, 50.0}, {30.5, 50.5}, {39.0, 51.4}}, new Color(210, 160, 30), 3.0);  // Gravettian Ivory Highway – amber gold
            drawTradeRoute(g, new double[][]{{-4.5, 43.4}, {0.5, 44.8}, {1.0, 45.0}, {7.5, 43.7}}, new Color(220, 100, 80), 2.5);                   // Atlantic Shell Route – coral
            drawTradeRoute(g, new double[][]{{39.0, 51.4}, {40.5, 56.2}}, new Color(210, 160, 30), 2.5);                                            // Kostenki -> Sungir Ivory Route – amber gold
            drawTradeRoute(g, new double[][]{{135.4, 70.7}, {145.0, 71.0}}, new Color(20, 140, 130), 2.5);                                          // Yana RHS / Berelekh Arctic Ivory – deep teal
            drawTradeRoute(g, new double[][]{{-168.0, 65.0}, {-140.7, 67.1}}, new Color(20, 140, 130), 2.5);                                        // Beringian Standstill Tool Tracks – deep teal
            drawTradeRoute(g, new double[][]{{35.2, 32.7}, {35.6, 32.8}, {36.0, 31.5}}, new Color(220, 100, 80), 2.5);                              // Levant Ohalo II Marine Shells – coral
            drawTradeRoute(g, new double[][]{{134.0, -24.0}, {143.0, -33.7}, {140.0, -37.0}}, new Color(20, 140, 130), 2.5);                        // Sahul Red Ochre Circuits – deep teal
            drawTradeRoute(g, new double[][]{{21.5, -34.2}, {25.0, -33.8}, {29.0, -31.0}}, new Color(220, 100, 80), 2.5);                           // South African LSA Beads – coral
        } else if (year <= -15000L) {
            // -20,000 BP: Solutrean Leaf-Point & Pressure-Flaked Flint Network, LGM Refugia Exchanges
            drawTradeRoute(g, new double[][]{{0.5, 44.8}, {0.7, 46.9}, {-1.0, 44.5}, {-4.5, 43.4}, {-8.5, 37.1}}, new Color(220, 150, 40), 3.0);  // Solutrean Silex – amber
            drawTradeRoute(g, new double[][]{{15.5, 41.7}, {12.5, 41.9}, {23.0, 38.5}}, new Color(60, 100, 180), 2.5);                             // Epigravettian Mediterranean maritime – slate blue
            drawTradeRoute(g, new double[][]{{31.0, 50.5}, {35.0, 50.5}, {39.0, 51.4}}, new Color(184, 115, 51), 2.5);                             // Mezhirich -> Mezin -> Kostenki Mammoth Architecture – copper
            drawTradeRoute(g, new double[][]{{35.5, 32.7}, {36.0, 31.8}, {36.5, 34.0}}, new Color(184, 115, 51), 2.5);                             // Kebaran Levant – copper
            drawTradeRoute(g, new double[][]{{-140.7, 67.1}, {-150.0, 65.0}, {-165.0, 65.0}}, new Color(184, 115, 51), 2.5);                       // Beringia Standstill – copper
            drawTradeRoute(g, new double[][]{{132.9, -12.5}, {143.0, -33.7}}, new Color(184, 115, 51), 2.5);                                       // Sahul Ochre – copper
            drawTradeRoute(g, new double[][]{{22.0, -34.0}, {24.0, -33.5}, {26.0, -32.5}}, new Color(184, 115, 51), 2.5);                          // South Africa Boomplaas / Nelson Bay – copper
        } else if (year <= -10500L) {
            // -10,900 BP: Younger Dryas / Natufian Flint & Marine Shell Transfers / Clovis Chert & Obsidian
            drawTradeRoute(g, new double[][]{{34.5, 38.0}, {36.5, 34.2}, {35.6, 33.1}, {35.2, 32.9}, {37.0, 32.0}}, new Color(220, 170, 50), 3.0); // Anatolian Obsidian / Natufian Dentalium – warm gold
            drawTradeRoute(g, new double[][]{{-103.3, 34.3}, {-101.5, 35.5}, {-97.7, 30.9}}, new Color(200, 90, 60), 2.5);                          // Clovis Americas – terracotta
            drawTradeRoute(g, new double[][]{{-110.7, 44.6}, {-103.0, 36.0}}, new Color(200, 90, 60), 2.5);                                         // Obsidian Cliff Americas – terracotta
            drawTradeRoute(g, new double[][]{{-75.1, 41.0}, {-77.0, 38.0}}, new Color(200, 90, 60), 2.5);                                           // Shawnee-Minisink Americas – terracotta
            drawTradeRoute(g, new double[][]{{-73.2, -41.5}, {-71.0, -45.0}, {-70.0, -52.0}}, new Color(200, 90, 60), 2.5);                         // Monte Verde to Fell's Cave Americas – terracotta
            drawTradeRoute(g, new double[][]{{-77.7, -9.2}, {-79.0, -7.0}}, new Color(200, 90, 60), 2.5);                                           // Guitarrero Cave Americas – terracotta
            drawTradeRoute(g, new double[][]{{-1.0, 44.5}, {1.0, 45.0}, {3.0, 46.5}, {15.5, 41.7}}, new Color(210, 150, 50), 2.5);                 // Magdalenian / Azilian Pyrenean Silex – warm amber
            drawTradeRoute(g, new double[][]{{138.5, 35.0}, {139.5, 35.7}, {140.5, 36.5}}, new Color(210, 150, 50), 2.5);                          // Incipient Jomon Kozushima Obsidian – warm amber
            drawTradeRoute(g, new double[][]{{32.5, 25.5}, {32.9, 24.1}, {31.5, 30.0}}, new Color(210, 150, 50), 2.5);                             // Nile Valley Qadan Exchange – warm amber
            drawTradeRoute(g, new double[][]{{134.0, -24.0}, {138.0, -28.0}, {143.0, -33.7}}, new Color(210, 150, 50), 2.5);                       // Australian Desert Ochre Tracks – warm amber
        } else if (year <= -9000L) {
            // -10,000 BP: Göbekli Tepe Anatolian Obsidian & PPNA Exchange
            drawTradeRoute(g, new double[][]{{34.5, 38.0}, {38.9, 37.2}, {38.1, 35.9}, {35.4, 31.9}}, new Color(230, 175, 40), 3.0);  // Göllü Dağ Obsidian to Göbekli & Jericho – bright amber
            drawTradeRoute(g, new double[][]{{41.5, 38.8}, {39.7, 38.2}, {44.0, 36.0}}, new Color(230, 175, 40), 2.5);                 // Bingöl Obsidian to Tigris/Zagros – bright amber
            drawTradeRoute(g, new double[][]{{24.4, 36.7}, {23.1, 37.4}}, new Color(30, 130, 200), 2.5);                              // Melos Obsidian – Aegean maritime – sea blue
            drawTradeRoute(g, new double[][]{{113.6, 34.4}, {116.0, 35.0}}, new Color(230, 175, 40), 2.5);                            // Peiligang Jade & Stone – bright amber
        } else if (year <= -7000L) {
            // -8,000 BP: Early Neolithic Çatalhöyük Obsidian, Spondylus Shells, Mehrgarh Lapis
            drawTradeRoute(g, new double[][]{{34.5, 38.0}, {32.8, 37.7}, {35.0, 34.0}, {35.9, 32.0}}, new Color(235, 185, 45), 3.0);  // Çatalhöyük Obsidian Corridor – warm gold
            drawTradeRoute(g, new double[][]{{24.4, 36.7}, {22.8, 39.3}, {20.5, 44.8}, {16.0, 48.5}}, new Color(30, 180, 170), 3.0);  // Spondylus Shell Route – turquoise
            drawTradeRoute(g, new double[][]{{70.7, 36.2}, {68.0, 29.3}, {66.0, 26.0}}, new Color(40, 80, 200), 2.5);                 // Badakhshan Lapis Lazuli – lapis blue
            drawTradeRoute(g, new double[][]{{113.6, 33.6}, {120.2, 30.1}}, new Color(235, 185, 45), 2.5);                            // Jiahu - Yangtze Exchange – warm gold
            drawTradeRoute(g, new double[][]{{30.6, 22.5}, {32.5, 25.5}}, new Color(235, 185, 45), 2.5);                              // Nabta Playa - Nile Valley – warm gold
        } else if (year <= -4500L) {
            // -6,000 BP: Ubaid Maritime Gulf Routes, Vinča Copper, European Spondylus & Flint
            drawTradeRoute(g, new double[][]{{45.99, 30.82}, {48.5, 29.5}, {50.5, 26.0}, {56.0, 24.0}}, new Color(240, 190, 50), 3.5); // Ubaid Persian Gulf Maritime – bright gold
            drawTradeRoute(g, new double[][]{{21.36, 43.20}, {20.62, 44.76}, {16.5, 48.2}, {8.5, 50.0}, {2.5, 49.0}}, new Color(184, 115, 51), 3.0); // VinÄa Copper – copper
            drawTradeRoute(g, new double[][]{{-0.1, 46.4}, {-3.0, 47.6}, {-3.9, 48.7}}, new Color(50, 190, 170), 3.0);                 // Atlantic Megalithic Coastal Exchange – seafoam
            drawTradeRoute(g, new double[][]{{109.06, 34.27}, {111.3, 34.7}, {121.4, 30.0}}, new Color(240, 190, 50), 3.0);            // Yangshao - Hemudu Jade & Pottery – bright gold
            drawTradeRoute(g, new double[][]{{31.37, 26.99}, {33.5, 28.0}, {34.5, 29.0}}, new Color(240, 190, 50), 2.5);              // Badarian Red Sea Shell & Malachite – bright gold
        } else if (year <= 500L) {
            // Classical Antiquity & Axial Age (-3000 BC to 500 AD)
            // 1. Overland Silk Road (Chang'an -> Dunhuang -> Kashgar -> Samarkand -> Merv -> Ctesiphon -> Palmyra -> Antioch -> Rome)
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {94.7, 40.1}, {75.9, 39.5}, {66.9, 39.6}, {62.2, 37.6}, {44.4, 33.1}, {38.3, 34.6}, {36.2, 36.2}, {28.9, 41.0}, {12.5, 41.9}}, new Color(255, 200, 50), 4.2);
            // 2. Erythraean Sea / Periplus Monsoon Maritime Route (Alexandria -> Berenike -> Bab-el-Mandeb -> Muziris/Malabar -> Sri Lanka)
            drawTradeRoute(g, new double[][]{{29.9, 31.2}, {32.5, 27.5}, {35.5, 23.9}, {43.3, 12.6}, {54.0, 14.5}, {76.2, 10.2}, {80.2, 6.9}}, new Color(30, 180, 210), 3.8);
            // 3. Incense Route (Southern Arabia -> Petra -> Gaza)
            drawTradeRoute(g, new double[][]{{49.2, 14.9}, {45.3, 15.4}, {40.0, 21.4}, {35.4, 30.3}, {34.4, 31.5}}, new Color(230, 160, 40), 3.0);
            // 4. Roman Mare Nostrum Mediterranean Arteries (Ostia -> Alexandria, Gades, Carthage, Narbo)
            drawTradeRoute(g, new double[][]{{12.2, 41.8}, {9.5, 38.0}, {10.2, 36.8}, {15.0, 35.0}, {25.0, 33.0}, {29.9, 31.2}}, new Color(50, 190, 170), 3.5);
            drawTradeRoute(g, new double[][]{{12.2, 41.8}, {5.0, 42.0}, {3.0, 43.1}, {-0.5, 38.5}, {-6.2, 36.5}}, new Color(50, 190, 170), 3.0);
            // 5. Phoenician / Punic Atlantic & Mediterranean Circuit through Gibraltar
            drawTradeRoute(g, new double[][]{{35.2, 33.2}, {24.0, 35.0}, {12.4, 37.8}, {10.2, 36.8}, {1.4, 38.9}, {-5.5, 36.0}, {-6.2, 36.5}, {-9.1, 38.7}}, new Color(200, 100, 80), 3.0);
            // 6. Amber Road (Baltic -> Carnuntum -> Aquileia / Rome)
            drawTradeRoute(g, new double[][]{{20.5, 54.7}, {18.5, 53.1}, {16.8, 51.1}, {16.9, 48.1}, {13.4, 45.7}, {12.5, 41.9}}, new Color(180, 210, 100), 2.8);
            // 7. Grand Canal & Yangtze internal waterways (China)
            drawTradeRoute(g, new double[][]{{117.2, 39.1}, {117.0, 36.6}, {119.4, 32.4}, {121.5, 31.2}, {114.3, 30.6}, {106.5, 29.5}}, new Color(255, 180, 40), 3.5);
            // 8. Mesoamerican Preclassic Obsidian & Jade Routes (El Mirador / San Lorenzo)
            drawTradeRoute(g, new double[][]{{-94.8, 17.8}, {-92.5, 15.0}, {-89.8, 17.8}, {-88.5, 15.5}}, new Color(80, 200, 120), 2.5);
            // 9. Early Andean Exchange Circuit (Chavin -> Coast -> Altiplano)
            drawTradeRoute(g, new double[][]{{-77.2, -9.6}, {-77.0, -12.0}, {-75.0, -14.0}, {-68.7, -16.5}}, new Color(200, 80, 60), 2.5);

            // 10. Roman Imperial Highway Network (Viae Publicae: Appia, Flaminia, Domitia, Augusta, Egnatia, Militaris)
            Color romanRoadCol = new Color(255, 175, 45); // Warm Terracotta Gold
            drawTradeRoute(g, new double[][]{{12.5, 41.9}, {14.3, 41.1}, {16.9, 41.1}, {17.9, 40.6}}, romanRoadCol, 3.2); // Via Appia (Rome -> Capua -> Brundisium)
            drawTradeRoute(g, new double[][]{{12.5, 41.9}, {12.6, 44.1}, {10.9, 44.7}, {9.2, 45.5}, {4.8, 45.7}, {6.6, 49.8}, {1.6, 50.7}}, romanRoadCol, 3.2); // Via Flaminia / Agrippa (Rome -> Milan -> Lyon -> Trier -> Boulogne)
            drawTradeRoute(g, new double[][]{{4.8, 45.7}, {3.0, 43.2}, {1.2, 41.1}, {-0.4, 39.5}, {-4.8, 37.9}, {-6.2, 36.5}}, romanRoadCol, 3.0); // Via Domitia / Augusta (Lyon -> Narbo -> Tarraco -> Corduba -> Gades)
            drawTradeRoute(g, new double[][]{{19.4, 41.3}, {21.3, 40.9}, {22.9, 40.6}, {26.6, 40.9}, {28.9, 41.0}}, romanRoadCol, 3.0); // Via Egnatia (Dyrrhachium -> Thessalonica -> Byzantium)
            drawTradeRoute(g, new double[][]{{20.5, 44.8}, {21.9, 43.3}, {23.3, 42.7}, {24.7, 42.1}, {28.9, 41.0}}, romanRoadCol, 2.8); // Via Militaris (Belgrade -> Serdica -> Philippopolis -> Byzantium)
            drawTradeRoute(g, new double[][]{{28.9, 41.0}, {32.8, 39.9}, {34.9, 36.9}, {36.2, 36.2}, {36.3, 33.5}, {35.2, 31.8}}, romanRoadCol, 2.8); // Anatolian-Levantine Trunk (Byzantium -> Tarsus -> Antioch -> Damascus -> Jerusalem)

            // 11. Persian Royal Road & Iranian Highway (Susa -> Ctesiphon -> Nineveh -> Harran -> Sardis)
            drawTradeRoute(g, new double[][]{{48.2, 32.2}, {44.4, 33.3}, {44.0, 36.2}, {43.1, 36.3}, {39.0, 36.9}, {38.3, 38.4}, {32.8, 39.9}, {28.1, 38.5}}, romanRoadCol, 2.8);

            // 12. Qin & Han Imperial Postal & Military Highways (Chi Dao & Straight Roads)
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {112.4, 34.6}, {114.3, 34.7}, {118.3, 36.8}}, romanRoadCol, 3.2); // Chang'an -> Luoyang -> Kaifeng -> Qi/Linzi
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {112.5, 37.9}, {116.4, 39.9}}, romanRoadCol, 3.0); // Qin Northern Straight Road (Chang'an -> Ji/Beijing)
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {107.0, 33.1}, {104.1, 30.7}}, romanRoadCol, 2.8); // Shudao Gallery Roads (Chang'an -> Hanzhong -> Chengdu)
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {102.6, 37.9}, {100.5, 38.9}, {98.5, 39.7}, {94.7, 40.1}}, romanRoadCol, 3.0); // Hexi Corridor Highway (Chang'an -> Wuwei -> Dunhuang)

            // 13. Mauryan Grand Trunk Road (Uttarapatha)
            drawTradeRoute(g, new double[][]{{85.1, 25.6}, {83.0, 25.3}, {81.8, 25.4}, {77.7, 27.5}, {77.2, 28.6}, {73.7, 33.7}, {71.5, 34.0}}, romanRoadCol, 3.0); // Pataliputra -> Varanasi -> Mathura -> Delhi -> Taxila -> Peshawar
        } else if (year <= 1491L) {
            // Post-Classical & Medieval (500 to 1491 AD)
            // 1. Pax Mongolica Northern & Southern Silk Roads
            drawTradeRoute(g, new double[][]{{116.4, 39.9}, {102.0, 38.0}, {88.0, 43.8}, {76.0, 43.0}, {60.0, 45.0}, {47.3, 47.2}, {35.3, 45.0}, {28.9, 41.0}, {12.3, 45.4}}, new Color(255, 200, 50), 4.5); // Northern Steppe Route to Venice
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {94.7, 40.1}, {75.9, 39.5}, {66.9, 39.6}, {51.6, 35.7}, {44.4, 33.3}, {36.2, 36.2}, {28.9, 41.0}}, new Color(255, 200, 50), 4.0); // Transoxiana / Silk Road
            // 2. Trans-Saharan Gold & Salt Caravan Routes (Mali Empire / Mansa Musa Pilgrimage)
            drawTradeRoute(g, new double[][]{{-8.3, 11.4}, {-3.0, 16.8}, {-0.1, 16.3}, {4.5, 24.0}, {13.0, 27.0}, {21.0, 29.0}, {31.2, 30.0}}, new Color(230, 160, 40), 3.8); // Timbuktu -> Cairo
            drawTradeRoute(g, new double[][]{{-8.3, 11.4}, {-4.0, 17.0}, {-5.0, 22.5}, {-4.5, 31.5}, {-7.5, 33.5}}, new Color(230, 160, 40), 3.2); // Timbuktu -> Taghaza -> Marrakech
            // 3. Indian Ocean Monsoon Maritime Network (Swahili Coast -> Arabia -> India -> Malacca -> China)
            drawTradeRoute(g, new double[][]{{39.5, -4.0}, {45.0, 2.0}, {45.0, 12.8}, {55.0, 17.0}, {56.3, 26.5}, {72.8, 19.0}, {76.2, 10.0}, {80.2, 6.9}, {98.0, 4.0}, {103.8, 1.3}, {108.0, 12.0}, {113.5, 22.2}, {118.6, 24.9}}, new Color(30, 180, 210), 4.2);
            // 4. Hanseatic League & Baltic Maritime Network
            drawTradeRoute(g, new double[][]{{-0.1, 51.5}, {3.2, 51.2}, {10.0, 53.5}, {10.7, 53.9}, {18.6, 54.4}, {24.1, 56.9}, {31.3, 58.5}}, new Color(180, 210, 100), 3.5);
            // 5. Route from the Varangians to the Greeks (Baltic -> Dnieper -> Black Sea -> Constantinople)
            drawTradeRoute(g, new double[][]{{30.3, 59.9}, {31.3, 58.5}, {32.0, 54.8}, {30.5, 50.4}, {31.5, 46.5}, {28.9, 41.0}}, new Color(180, 210, 100), 3.0);
            // 6. Venetian & Genoese Levant Maritime Conduits
            drawTradeRoute(g, new double[][]{{12.3, 45.4}, {16.0, 41.0}, {22.0, 37.0}, {25.0, 35.0}, {35.0, 33.0}, {35.5, 34.0}}, new Color(255, 140, 50), 3.2);
            drawTradeRoute(g, new double[][]{{8.9, 44.4}, {9.5, 38.0}, {15.0, 36.0}, {24.0, 37.5}, {29.0, 41.0}, {35.3, 45.0}}, new Color(255, 140, 50), 3.2);
            // 7. Inca Imperial Highway (Qhapaq Ñan: Quito -> Cajamarca -> Cusco -> Lake Titicaca -> Tucuman)
            drawTradeRoute(g, new double[][]{{-78.5, -0.2}, {-78.5, -7.1}, {-77.0, -12.0}, {-71.9, -13.5}, {-69.0, -16.0}, {-65.3, -24.8}}, new Color(200, 80, 60), 3.5);
            // 8. Mesoamerican Pochteca Merchant Arteries (Tenochtitlan -> Soconusco -> Maya Highlands)
            drawTradeRoute(g, new double[][]{{-99.1, 19.4}, {-96.1, 19.2}, {-93.0, 16.5}, {-92.5, 15.0}, {-90.5, 14.6}, {-89.0, 20.0}}, new Color(80, 200, 120), 3.0);
        } else if (year <= 1850L) {
            // Early Modern & Age of Discovery (1492 to 1850 AD)
            // 1. ATLANTIC TRIANGULAR TRADE (Commerce Triangulaire)
            // Leg 1: Europe -> West Africa (Manufactures, arms, textiles)
            drawTradeRoute(g, new double[][]{{-3.0, 53.4}, {-4.0, 48.0}, {-12.0, 35.0}, {-17.5, 14.5}, {-1.5, 5.0}, {2.5, 6.0}, {13.0, -8.8}}, new Color(230, 60, 40), 4.2);
            // Leg 2: West Africa -> Caribbean & Brazil (Middle Passage)
            drawTradeRoute(g, new double[][]{{2.5, 6.0}, {-15.0, 0.0}, {-38.5, -12.9}, {-34.8, -8.0}}, new Color(230, 60, 40), 3.8); // To Brazil (Bahia / Recife)
            drawTradeRoute(g, new double[][]{{2.5, 6.0}, {-25.0, 8.0}, {-55.0, 12.0}, {-61.0, 14.0}, {-72.5, 19.5}, {-77.0, 18.0}, {-82.3, 23.1}}, new Color(230, 60, 40), 4.2); // To Caribbean (Saint-Domingue, Jamaica, Cuba)
            // Leg 3: Caribbean / North America -> Western Europe (Sugar, tobacco, cotton, coffee, rum)
            drawTradeRoute(g, new double[][]{{-82.3, 23.1}, {-75.0, 28.0}, {-65.0, 35.0}, {-40.0, 43.0}, {-15.0, 47.0}, {-3.0, 47.5}, {-0.5, 45.0}, {-3.0, 53.4}}, new Color(255, 200, 50), 4.2);

            // 2. PORTUGUESE CAPE ROUTE (Carreira da Índia: Lisbon -> Cape -> Goa -> Malacca -> Macau -> Nagasaki)
            drawTradeRoute(g, new double[][]{{-9.1, 38.7}, {-16.0, 28.0}, {-25.0, 12.0}, {-30.0, -10.0}, {-20.0, -28.0}, {18.5, -34.8}, {40.5, -15.0}, {55.0, -2.0}, {73.8, 15.5}, {80.2, 6.9}, {98.0, 4.0}, {103.8, 1.3}, {113.5, 22.2}, {129.8, 32.7}}, new Color(30, 180, 210), 4.2);

            // 3. SPANISH MANILA GALLEONS (Acapulco <-> Manila Transpacific Circuit)
            // Westbound: Acapulco -> Guam -> Manila
            drawTradeRoute(g, new double[][]{{-99.9, 16.8}, {-130.0, 14.0}, {-160.0, 13.5}, {144.7, 13.4}, {125.0, 13.0}, {120.9, 14.6}}, new Color(255, 180, 40), 3.8);
            // Eastbound (Urdaneta Route): Manila -> North Pacific Kuroshio -> Cape Mendocino -> Acapulco
            drawTradeRoute(g, new double[][]{{120.9, 14.6}, {125.0, 20.0}, {140.0, 33.0}, {170.0, 38.0}, {-160.0, 40.0}, {-130.0, 38.0}, {-124.0, 38.0}, {-118.0, 33.0}, {-99.9, 16.8}}, new Color(255, 180, 40), 3.8);

            // 4. SPANISH FLOTA DE INDIAS (Veracruz / Cartagena -> Havana -> Seville / Cadiz)
            drawTradeRoute(g, new double[][]{{-96.1, 19.2}, {-88.0, 22.0}, {-82.3, 23.1}, {-79.0, 27.0}, {-60.0, 34.0}, {-35.0, 38.0}, {-15.0, 36.5}, {-6.2, 36.5}}, new Color(240, 190, 50), 4.0);
            drawTradeRoute(g, new double[][]{{-75.5, 10.4}, {-79.5, 9.5}, {-82.3, 23.1}}, new Color(240, 190, 50), 3.5); // Cartagena/Portobelo to Havana

            // 5. DUTCH VOC SPICE ROUTE (Amsterdam -> Cape Town -> Sunda Strait -> Batavia)
            drawTradeRoute(g, new double[][]{{4.9, 52.4}, {-5.0, 49.0}, {-20.0, 20.0}, {-28.0, -10.0}, {18.5, -34.5}, {60.0, -38.0}, {90.0, -38.0}, {105.8, -6.0}, {106.8, -6.2}}, new Color(255, 140, 30), 4.0);

            // 6. Trans-Saharan Caravan Network (Tripoli / Marrakech -> Timbuktu / Kano)
            drawTradeRoute(g, new double[][]{{-7.5, 33.5}, {-5.0, 25.0}, {-3.0, 16.8}, {8.5, 12.0}}, new Color(230, 160, 40), 3.2);
            drawTradeRoute(g, new double[][]{{13.2, 32.9}, {14.0, 26.0}, {13.0, 18.0}, {8.5, 12.0}, {31.2, 30.0}}, new Color(230, 160, 40), 3.0);

            // 7. Siberian Fur & Tea Road (Moscow -> Kazan -> Tobolsk -> Irkutsk -> Kyakhta -> Beijing)
            drawTradeRoute(g, new double[][]{{37.6, 55.7}, {49.1, 55.8}, {68.2, 58.2}, {82.9, 55.0}, {104.3, 52.3}, {106.5, 50.3}, {116.4, 39.9}}, new Color(180, 210, 100), 3.5);

            // 8. North American Fur Trade (Montreal / Hudson Bay -> Great Lakes)
            drawTradeRoute(g, new double[][]{{-73.5, 45.5}, {-79.4, 43.6}, {-84.5, 45.8}, {-89.2, 48.4}, {-97.1, 49.9}}, new Color(180, 210, 100), 2.8);
        } else {
            // Industrial, Imperial & Contemporary (1850 to 2026+ AD)
            // 1. SUEZ MARITIME TRUNK (Europe <-> Asia via Suez Canal & Strait of Malacca)
            drawTradeRoute(g, new double[][]{{4.4, 51.9}, {-5.0, 49.0}, {-9.5, 38.0}, {-5.5, 36.0}, {15.0, 35.0}, {32.3, 31.2}, {32.5, 29.9}, {35.5, 23.9}, {43.3, 12.6}, {55.0, 14.5}, {72.8, 19.0}, {80.2, 6.9}, {98.0, 4.0}, {103.8, 1.3}, {113.5, 22.2}, {121.5, 31.2}, {139.7, 35.6}}, new Color(255, 200, 50), 4.8);

            // 2. TRANSPACIFIC CONTAINER HIGHWAY (East Asia <-> US West Coast)
            drawTradeRoute(g, new double[][]{{121.5, 31.2}, {129.0, 35.0}, {140.0, 35.0}, {170.0, 40.0}, {-160.0, 42.0}, {-130.0, 38.0}, {-118.2, 33.7}}, new Color(30, 180, 210), 4.8); // Shanghai -> Los Angeles
            drawTradeRoute(g, new double[][]{{114.1, 22.3}, {140.0, 30.0}, {-150.0, 40.0}, {-123.1, 49.3}}, new Color(30, 180, 210), 4.2); // Hong Kong -> Vancouver

            // 3. TRANSATLANTIC CONTAINER & FREIGHT TRUNK
            drawTradeRoute(g, new double[][]{{4.4, 51.9}, {-10.0, 50.0}, {-35.0, 45.0}, {-60.0, 42.0}, {-74.0, 40.7}}, new Color(30, 180, 210), 4.5); // Rotterdam -> New York
            drawTradeRoute(g, new double[][]{{-5.5, 36.0}, {-35.0, 30.0}, {-75.0, 32.0}, {-80.2, 25.8}}, new Color(30, 180, 210), 4.0); // Mediterranean -> Florida/Savannah

            // 4. PANAMA CANAL TRANSOCEANIC ARTERY
            drawTradeRoute(g, new double[][]{{-74.0, 40.7}, {-75.0, 25.0}, {-79.5, 9.3}, {-79.5, 8.9}, {-85.0, 5.0}, {-118.2, 33.7}}, new Color(50, 190, 170), 4.2);
            drawTradeRoute(g, new double[][]{{4.4, 51.9}, {-40.0, 30.0}, {-70.0, 15.0}, {-79.5, 9.3}, {-79.5, 8.9}, {-85.0, -5.0}, {-77.0, -12.0}, {-71.6, -33.0}}, new Color(50, 190, 170), 4.0); // Europe -> Panama -> Peru/Chile

            // 5. TRANSCONTINENTAL RAILWAYS
            drawTradeRoute(g, new double[][]{{37.6, 55.7}, {61.4, 55.1}, {73.4, 54.9}, {82.9, 55.0}, {92.9, 56.0}, {104.3, 52.3}, {120.0, 52.0}, {131.9, 43.1}}, new Color(255, 140, 30), 4.2); // Trans-Siberian (Moscow -> Vladivostok)
            drawTradeRoute(g, new double[][]{{-74.0, 40.7}, {-87.6, 41.8}, {-95.9, 41.2}, {-104.9, 41.1}, {-111.9, 40.7}, {-121.5, 38.5}, {-122.4, 37.8}}, new Color(255, 140, 30), 4.0); // US Transcontinental (NYC -> Chicago -> SF)

            // 6. CAPE BULK FREIGHT ROUTE (Large oil/ore tankers bypassing Suez)
            drawTradeRoute(g, new double[][]{{55.0, 25.0}, {55.0, 10.0}, {45.0, -15.0}, {20.0, -35.0}, {-15.0, -10.0}, {-15.0, 20.0}, {4.4, 51.9}}, new Color(200, 100, 80), 3.8);

            // 7. LATIN AMERICAN & OCEANIC TRADE CONDUITS
            drawTradeRoute(g, new double[][]{{-43.2, -22.9}, {-38.5, -12.9}, {-15.0, 15.0}, {4.4, 51.9}}, new Color(200, 100, 80), 3.5); // Santos/Rio -> Europe
            drawTradeRoute(g, new double[][]{{151.2, -33.8}, {145.0, -20.0}, {130.0, -5.0}, {115.0, 10.0}, {121.5, 31.2}}, new Color(30, 180, 210), 3.8); // Australia -> China (Iron Ore)
        }

        // Draw Major Navigable River Trade Corridors (High Conductance Channels)
        drawNavigableRiverCorridors(g, year);

        g.dispose();
        return img;
    }

    public static void drawNavigableRiverCorridors(Graphics2D g, long year) {
        if (year <= -6000L) return; // Prior to Neolithic / Early Bronze, large-scale riverine trade arteries were nascent
        Color riverCol = new Color(0, 210, 230); // Turquoise / Cyan navigable river artery

        // 1. Nile Valley & Delta Artery (Aswan -> Luxor -> Memphis -> Alexandria)
        drawTradeRoute(g, new double[][]{{32.9, 24.1}, {32.6, 25.7}, {31.3, 29.8}, {31.2, 31.2}}, riverCol, 3.2);

        // 2. Tigris & Euphrates Mesopotamian Arteries (Cradle of Irrigation & Riverine Trade)
        drawTradeRoute(g, new double[][]{{40.0, 37.9}, {43.1, 36.3}, {44.4, 33.3}, {47.8, 30.5}}, riverCol, 2.8);
        drawTradeRoute(g, new double[][]{{38.5, 37.5}, {40.1, 35.3}, {42.4, 33.9}, {44.4, 32.5}, {47.8, 30.5}}, riverCol, 2.8);

        // 3. Indus & Ganges-Brahmaputra South Asian Arteries
        drawTradeRoute(g, new double[][]{{73.7, 33.7}, {71.5, 30.2}, {68.8, 27.5}, {68.1, 27.3}, {67.0, 24.8}}, riverCol, 2.8);
        drawTradeRoute(g, new double[][]{{78.2, 29.9}, {81.8, 25.4}, {83.0, 25.3}, {85.1, 25.6}, {88.3, 22.5}, {90.4, 23.7}}, riverCol, 3.0);

        // 4. Yellow River (Huang He) & Yangtze (Chang Jiang) Waterways
        drawTradeRoute(g, new double[][]{{103.8, 36.0}, {108.9, 34.3}, {111.0, 34.8}, {114.3, 34.7}, {117.5, 37.5}}, riverCol, 2.8);
        drawTradeRoute(g, new double[][]{{104.0, 28.7}, {106.5, 29.5}, {112.5, 30.3}, {114.3, 30.6}, {118.8, 32.0}, {121.5, 31.2}}, riverCol, 3.4);

        // 5. European Riverine Arteries (Rhine, Danube & Rhône-Saône)
        drawTradeRoute(g, new double[][]{{8.6, 47.6}, {7.6, 48.6}, {8.3, 50.0}, {6.9, 50.9}, {4.5, 51.9}}, riverCol, 2.6);
        drawTradeRoute(g, new double[][]{{10.0, 48.5}, {12.1, 49.0}, {16.4, 48.2}, {19.0, 47.5}, {20.5, 44.8}, {24.0, 43.7}, {28.0, 45.2}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{4.8, 47.3}, {4.8, 45.7}, {4.8, 43.9}, {5.4, 43.3}}, riverCol, 2.4);

        // 6. Eastern European & Russian Waterways (Dnieper & Volga)
        drawTradeRoute(g, new double[][]{{32.0, 54.8}, {30.5, 50.4}, {35.0, 47.8}, {32.6, 46.6}}, riverCol, 2.6);
        drawTradeRoute(g, new double[][]{{33.0, 57.0}, {39.8, 57.6}, {49.1, 55.8}, {48.7, 51.5}, {44.5, 48.7}, {48.0, 46.3}}, riverCol, 3.0);

        // 7. African & American Major Navigable Basins (Niger, Mississippi-Ohio, Amazon)
        drawTradeRoute(g, new double[][]{{-10.7, 10.0}, {-8.3, 11.4}, {-3.0, 16.8}, {0.0, 16.2}, {6.5, 6.0}}, riverCol, 2.6);
        drawTradeRoute(g, new double[][]{{-80.0, 40.4}, {-84.5, 39.1}, {-89.0, 37.0}, {-90.0, 35.0}, {-91.0, 32.5}, {-90.0, 29.9}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{-73.2, -3.7}, {-60.0, -3.1}, {-54.7, -2.4}, {-50.0, -1.0}}, riverCol, 3.2);
    }

    public static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        if (coords == null || coords.length < 2) return;
        int n = coords.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = (int) ((coords[i][0] + 180.0) / 360.0 * HistoricalMapGenerator.WIDTH);
            ys[i] = (int) ((90.0 - coords[i][1]) / 180.0 * HistoricalMapGenerator.HEIGHT);
        }

        // 1. Soft glowing supply basin / trade corridor halo
        Color haloCol = new Color(col.getRed(), col.getGreen(), col.getBlue(), 60);
        g.setColor(haloCol);
        g.setStroke(new BasicStroke((float) (strokeWidth * 3.5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, n);

        // 2. High-luminance crisp arterial line
        g.setColor(col);
        g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, n);

        // 3. Trade hub / emporia nodes
        int r = (int) Math.max(3, strokeWidth * 1.5);
        for (int i = 0; i < n; i++) {
            g.setColor(Color.WHITE);
            g.fillOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
            g.setColor(col);
            g.drawOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
        }
    }

    // --- 8. INSTITUTIONAL COMPLEXITY TENSOR MAP (SESHAT) ---
    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic state capacity & administrative centralization for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernInstitutionalMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return HistoricalMapGenerator.applyAltimetryCoastlineMask(modernImg);
            }
        }

        List<CityPoint> cities = HistoricalScenarioFeaturesCatalog.getCitiesForScenario(type, scenario);

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
                    // Paleolithic Era: Egalitarian band & clan networks (Carneiro Scale Level 0 / score 18-24)
                    double instVal = 20.0;
                    if (year <= -70000L) {
                        instVal = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 24.0, 18.0, 16.0);
                    } else if (year <= -40000L) {
                        instVal = 22.0; // MIS 3 Sahul & Eurasian clans
                    } else if (year <= -20000L) {
                        instVal = 24.0; // Gravettian / Solutrean complex alliances
                    }
                    int gray = Math.clamp((int) (instVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                // Holocene & Historical Eras: Seshat hierarchy scaling with distance decay from imperial cores
                // Calibrated scale:
                // - Neolithic (-10k to -3500 BP): chiefdoms ~ 50-65
                // - Bronze/Archaic Empires (-3500 to -500 BP): kingdoms ~ 90-120
                // - Classical Axial Empires (-500 to 500 AD): Rome/Han ~ 150-165
                // - Medieval / Early Modern (500 to 1800 AD): Song/Abbasid/Ottoman/W.Europe ~ 165-185
                // - Industrial Era (1800 to 1900 AD): ~ 185-195
                double eraMaxCap;
                if (year <= -3500L) {
                    eraMaxCap = 55.0;
                } else if (year <= -500L) {
                    eraMaxCap = 110.0;
                } else if (year <= 600L) {
                    eraMaxCap = 160.0;
                } else if (year <= 1750L) {
                    eraMaxCap = 175.0;
                } else {
                    eraMaxCap = 195.0;
                }

                double baselineInst = 20.0;
                double maxInst = baselineInst;
                for (CityPoint cp : cities) {
                    double d2 = HistoricalScenarioFeaturesCatalog.distSq(lon, lat, cp.lng, cp.lat);
                    double cityPeak = Math.min(eraMaxCap, (cp.weight / 4.2) * eraMaxCap);
                    double val = baselineInst + (cityPeak - baselineInst) * Math.exp(-d2 / (2.0 * cp.sigma * cp.sigma * 4.0));
                    maxInst = Math.max(maxInst, val);
                }

                int gray = Math.clamp((int) (maxInst * occWeight), 0, 255);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }

    // --- 9. ECOLOGICAL FOOTPRINT TENSOR MAP ---
    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        // 1. Ingest authentic agricultural & industrial exergy footprint for modern / contemporary epochs (1900-2060)
        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernEcologicalMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
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
                    // Paleolithic hunter-gatherer metabolic footprint & fire-stick farming
                    double ecoVal = 20.0;
                    if (year <= -70000L) {
                        ecoVal = HistoricalMapGenerator.blendPaleoTraits(lon, lat, 26.0, 20.0, 18.0);
                    } else if (year <= -40000L) {
                        // Sahul anthropogenic mosaic burning (fire-stick farming)
                        double sahulFire = Math.exp(-(Math.pow(lat - (-22.0), 2) + Math.pow(lon - 135.0, 2)) / 250.0);
                        ecoVal = 22.0 + sahulFire * 45.0;
                    } else if (year <= -18000L) {
                        // Gravettian & LGM megafauna hunting pressure in periglacial plains
                        double mammothHunt = Math.exp(-(Math.pow(lat - 50.0, 2) + Math.pow(lon - 30.0, 2)) / 300.0);
                        ecoVal = 22.0 + mammothHunt * 35.0;
                    }
                    int gray = Math.clamp((int) (ecoVal * occWeight), 0, 255);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    continue;
                }

                // Holocene Agricultural & Industrial Stress (Smooth 2D continuous Gaussians)
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

    // --- 10. PATHOGEN IMMUNITY TENSOR MAP ---
    /**
     * Rasterize pathogen immunity map.
     *
     * @param type the type parameter (String)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
        BufferedImage img = AnalyticalEpidemiologyModel.generatePathogenMap(year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }
}
