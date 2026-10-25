/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Cartographic Rasterizer for Sacred Traditions, Technology, Trade Networks,
 * Institutional Capacity, Ecological Strain, and Epidemiology.
 *
 * <p>Decomposed from {@link HistoricalCulturalTensorRasterizer} to satisfy the Single
 * Responsibility Principle (SRP). Implements multi-confessional contact hubs dithering,
 * Boserup-Kremer technology agglomeration, transport graph rasterization, Seshat
 * administrative complexity hierarchy, and continuous ecological strain fields.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalReligiousAndSocietalRasterizer {

    private HistoricalReligiousAndSocietalRasterizer() {
        // Pure static rasterizer
    }

    public static final int[] COLORS_RITUALS_100K = {
        0x10B981, // Clade 0: Sapiens Pan-African Symbolic Ochre & Shell Ornamentation (#10B981)
        0xF59E0B, // Clade 1: Pioneer Coastal Symbolic Caches (#F59E0B)
        0x3B82F6, // Clade 2: Neanderthal Intentional Burials & Raptor Claw Cults (#3B82F6)
        0x2563EB, // Clade 3: Shanidar Flower Burial Tradition (#2563EB)
        0xD97706, // Clade 4: Denisovan Altai Chloritolite Jewelry & Bone Carving (#D97706)
        0x8B5CF6  // Clade 5: Archaic Asian Megafauna & Ochre Traditions (#8B5CF6)
    };

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
            // Western European Neanderthal Intentional Burials & Raptor Claw Ornamentation
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x3B82F6, 1.2, "La Ferrassie Neanderthal Cemetery Tradition"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(15.9, 46.2, 0x2563EB, 1.2, "Krapina Neanderthal Raptor Claw Adornment"));
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(4.7, 50.5, 0x1D4ED8, 1.1, "Spy Neanderthal Intentional Mortuary Cave"));
            // Zagros Flower & Intentional Mortuary Tradition
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x60A5FA, 1.3, "Shanidar Mortuary Tradition"));
            // Denisova Bone Flutes, Beads & Green Chloritolite Bracelet Shrines
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(84.7, 51.4, 0xD97706, 1.4, "Denisova Chloritolite & Bone Ornament Tradition"));
            // Eastern Archaic Hominins Megafauna Hearth Cults
            paleoRituals.add(new OrographicGlottologPropagator.CulturalSeed(111.5, -7.4, 0x8B5CF6, 1.3, "Ngandong Solo River Hearth Shrines"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(paleoRituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 30.0f);
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
            // -74,000 BP: Toba Volcanic Winter Symbolic Refugia
            List<OrographicGlottologPropagator.CulturalSeed> tobaRituals = new ArrayList<>();
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(22.1, -34.2, 0x059669, 1.2, "Pinnacle Point Ochre Processing & Hearth Sanctuaries"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(-2.4, 34.8, 0x34D399, 1.1, "Taforalt Shell Adornment Refugium"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0x10B981, 1.2, "East African Highland Ochre Caches"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.5, 43.5, 0x3B82F6, 1.1, "Pyrenean Neanderthal Burial Refugium"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(44.2, 36.8, 0x60A5FA, 1.2, "Shanidar Cave Burial Refugium"));
            tobaRituals.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0xD97706, 1.3, "Altai Denisova Cave Symbolic Shrines"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(tobaRituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 32.0f);
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
            // -50,000 BP: MIS 3 Upper Paleolithic Symbolic Explosion & Sahul Rock Art Traditions
            List<OrographicGlottologPropagator.CulturalSeed> mis3Rituals = new ArrayList<>();
            // Sahul Aboriginal Ancestral Dreaming, Ochre Burials & Megafauna Rock Art (crossing Wallace Line)
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(132.9, -12.5, 0xA04000, 1.6, "Madjedbebe Ochre Grinding & Ancestral Dreaming"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(143.0, -33.7, 0xB45309, 1.5, "Lake Mungo Mungo Lady/Man Red Ochre Cremations"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(125.0, -16.0, 0x92400E, 1.5, "Kimberley Gwion Gwion Precursor Traditions"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(120.0, -4.5, 0xC2410C, 1.4, "Sulawesi Maros Hand Stencils & Babirusa Art"));
            // African Howiesons Poort Engraved Ostrich Eggshells
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(22.0, -34.0, 0x10B981, 1.4, "Diepkloof Engraved Ostrich Eggshell Sanctuaries"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(36.0, 0.5, 0x059669, 1.4, "Enkapune Ya Muto Shell Beads & Ritual Caches"));
            // Eurasian IUP Lion-Man & Aurignacian Figurative Art Precursors
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(10.2, 48.5, 0x4D7C0F, 1.4, "Swabian Jura Vogelherd/Hohlenstein Ivory Cults"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(25.4, 43.0, 0x8B5CF6, 1.3, "Bacho Kiro Perforated Bear Teeth Shrines"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(35.5, 32.5, 0xF59E0B, 1.3, "Levant Ksar Akil Marine Shell Ornamentation"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(71.2, 57.7, 0xD97706, 1.4, "Siberian Ust'-Ishim Mammoth Cults"));
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(84.5, 51.4, 0xD97706, 1.4, "Altai Denisova Bone/Chloritolite Shrines"));
            // Late Neanderthal Burials
            mis3Rituals.add(new OrographicGlottologPropagator.CulturalSeed(1.2, 44.9, 0x3B82F6, 1.2, "Late Neanderthal Châtelperronian Mortuary Sites"));

            BufferedImage ritImg = OrographicGlottologPropagator.propagateCulturalSeeds(mis3Rituals, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask, 32.0f);
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
            // -25,000 to -18,000 BP: Gravettian Venus Cults & Solutrean Cave Sanctuaries
            List<OrographicGlottologPropagator.CulturalSeed> lgmRituals = new ArrayList<>();
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(16.5, 48.8, 0xEA580C, 1.4, "Dolni Vestonice Ceramic Venus & Triple Burials"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(15.4, 48.3, 0xEA580C, 1.3, "Willendorf Oolitic Limestone Venus Shrine"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(39.0, 51.4, 0x8B5CF6, 1.5, "Kostenki Mammoth Ivory Venus Cult"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(40.5, 56.2, 0x8B5CF6, 1.4, "Sungir Mammoth Bead Burials"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(1.0, 45.0, 0x4D7C0F, 1.4, "Franco-Cantabrian Solutrean Cave Sanctuaries (Lascaux Precursor)"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(-4.5, 43.4, 0x4D7C0F, 1.3, "Altamira Polychrome Shrines"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(15.5, 41.7, 0xB45309, 1.3, "Paglicci Red Ochre Epigravettian Burials"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(135.4, 70.7, 0xD97706, 1.5, "Yana Arctic Siberian Ivory Cults"));
            lgmRituals.add(new OrographicGlottologPropagator.CulturalSeed(134.0, -24.0, 0xA04000, 1.6, "Sahul Pan-Continental Dreaming Songlines"));

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
        } else if (year <= -2500L) {
            // Early Bronze Age / Early Dynastic Egypt & Sumer
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(31.2, 29.8, 0x15803D, 1.3, "Egyptian Solar & Osirian Religion"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(45.6, 31.3, 0xB45309, 1.2, "Sumerian Polytheism & Ziggurat Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(120.0, 30.3, 0xEF4444, 1.4, "Liangzhu Jade Cong/Bi Ancestral Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(68.7, 27.5, 0xF59E0B, 1.3, "Indus Valley Priesthood & Great Bath"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-77.5, -10.9, 0xC27803, 1.4, "Caral-Supe Sunken Circular Plazas"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(23.7, 37.9, 0x4D7C0F, 1.2, "Aegean Minoan Goddess Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-1.8, 51.1, 0x3B82F6, 1.3, "Atlantic Megalithic Solar Alignments"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "African Traditional Animism"));
        } else if (year <= -500L) {
            // Bronze to Early Iron Age
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(31.2, 30.0, 0x15803D, 1.4, "Egyptian Amun-Ra State Religion"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(44.4, 32.5, 0xB45309, 1.3, "Babylonian Marduk Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.2, 31.8, 0x2563EB, 1.0, "Early Yahwism & Second Temple Precursors"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(76.0, 28.0, 0xF59E0B, 1.4, "Vedic Sacrificial Traditions (Yajna)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(114.3, 36.1, 0xEF4444, 1.4, "Shang Ancestral Bone Divination"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(23.7, 37.9, 0xEA580C, 1.2, "Greek Olympian Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(51.0, 32.0, 0x0891B2, 1.3, "Zoroastrianism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-94.7, 17.7, 0xD97706, 1.3, "Olmec Were-Jaguar Ceremonialism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-77.1, -9.6, 0xC27803, 1.3, "Chavín de Huántar Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "West African Traditional Animism"));
        } else if (year <= 500L) {
            // Classical Antiquity & Axial Age (Pax Romana / Han China)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(12.5, 41.9, 0xEA580C, 1.4, "Graeco-Roman Capitoline Polytheism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(35.2, 31.8, 0x2563EB, 1.1, "Judaism & Early Christianity"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(51.0, 32.0, 0x0891B2, 1.3, "Zoroastrian Mazdayasna"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(83.0, 25.3, 0xF59E0B, 1.5, "Brahmanical Hinduism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(85.1, 25.6, 0xEAB308, 1.4, "Theravada & Mahayana Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(108.9, 34.3, 0xEF4444, 1.5, "Confucianism & Daoism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(139.7, 35.6, 0xF43F5E, 1.1, "Archaic Shintoism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-89.6, 17.2, 0xD97706, 1.3, "Maya Classic Cosmology"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-98.8, 19.7, 0xEA580C, 1.2, "Teotihuacan Feathered Serpent Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-68.6, -16.5, 0xC27803, 1.3, "Tiwanaku Staff God Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(38.7, 14.1, 0x6366F1, 1.1, "Aksumite Pre-Christian & Coptic Christianity"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "African Traditional Animism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(133.0, -25.0, 0xA04000, 1.6, "Australian Dreamtime"));
        } else if (year <= 1500L) {
            // Post-Classical & High Middle Ages (Jerusalem, Rome, Constantinople, Mecca, Kaifeng)
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(12.5, 41.9, 0xEC4899, 1.5, "Roman Catholicism (Latin West)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(28.9, 41.0, 0x8B5CF6, 1.5, "Eastern Orthodoxy (Byzantine/Rus)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(39.8, 21.4, 0x10B981, 1.6, "Sunni Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(51.4, 35.7, 0x0D9488, 1.4, "Shia Islam"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(83.0, 25.3, 0xF59E0B, 1.5, "Puranic Hinduism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(100.5, 13.7, 0xEAB308, 1.3, "Theravada Buddhism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(116.4, 39.9, 0xEF4444, 1.5, "Neo-Confucianism & Mahayana"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(139.7, 35.6, 0xF43F5E, 1.1, "Shintoism"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(91.1, 29.6, 0xD97706, 1.3, "Vajrayana Buddhism (Tibet)"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-99.1, 19.4, 0xEA580C, 1.2, "Mesoamerican Mexica/Aztec Cults"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(-71.9, -13.5, 0xC27803, 1.3, "Inca Inti / Solar State Cult"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(38.7, 14.1, 0x6366F1, 1.1, "Ethiopian Orthodox Tewahedo"));
            ritualSeeds.add(new OrographicGlottologPropagator.CulturalSeed(8.0, 9.0, 0x1B5E20, 1.5, "Sub-Saharan Traditional Animism"));
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

    // --- 7. TRADE NETWORK TENSOR MAP (DELEGATED TO HistoricalTradeNetworkRasterizer) ---
    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        return HistoricalTradeNetworkRasterizer.rasterizeTradeNetworkMap(type, scenario);
    }

    public static void drawNavigableRiverCorridors(Graphics2D g, long year) {
        HistoricalTradeNetworkRasterizer.drawNavigableRiverCorridors(g, year);
    }

    public static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        HistoricalTradeNetworkRasterizer.drawTradeRoute(g, coords, col, strokeWidth);
    }

    // --- 8. INSTITUTIONAL COMPLEXITY TENSOR MAP (DELEGATED TO HistoricalInstitutionalRasterizer) ---
    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        return HistoricalInstitutionalRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
    }

    // --- 9. ECOLOGICAL FOOTPRINT TENSOR MAP (DELEGATED TO HistoricalInstitutionalRasterizer) ---
    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        return HistoricalInstitutionalRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
    }

    // --- 10. PATHOGEN IMMUNITY TENSOR MAP (DELEGATED TO HistoricalInstitutionalRasterizer) ---
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        return HistoricalInstitutionalRasterizer.rasterizePathogenImmunityMap(type, scenario);
    }
}