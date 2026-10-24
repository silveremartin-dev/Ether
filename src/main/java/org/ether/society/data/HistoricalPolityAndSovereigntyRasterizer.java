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
 * Cartographic Rasterizer for Sovereignty and Political Boundaries across Historical Epochs.
 *
 * <p>Decomposed from {@link HistoricalCulturalTensorRasterizer} to satisfy the Single
 * Responsibility Principle (SRP). Handles deep-time hominin clade ranges, paleolithic
 * hunter-gatherer spheres, Seshat historical empires, and modern sovereign boundaries.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalPolityAndSovereigntyRasterizer {

    private HistoricalPolityAndSovereigntyRasterizer() {
        // Pure static rasterizer
    }

    public static final int[] COLORS_SOVEREIGNTY_100K = {
        0xD35400, // Clade 0: Proto-Sapiens Pan-African Domain (#D35400)
        0xF39C12, // Clade 1: Out-of-Africa Dispersal Domain (#F39C12)
        0x1F618D, // Clade 2: Western Mousterian Clan Territories (#1F618D)
        0x1A5276, // Clade 3: Zagros & Near East Mousterian Domain (#1A5276)
        0x229954, // Clade 4: Denisovan Central Asian Domain (#229954)
        0x7D3C98  // Clade 5: Eastern Archaic Sunda Domain (#7D3C98)
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
}
