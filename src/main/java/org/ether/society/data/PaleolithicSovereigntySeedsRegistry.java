/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Paleolithic cultural seed generator and rasterizer for deep-time epochs.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PaleolithicSovereigntySeedsRegistry {

    private PaleolithicSovereigntySeedsRegistry() {
    }

    public static BufferedImage rasterizePaleolithicEpoch(long year, BufferedImage mask) {
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
        return null;
    }
}
