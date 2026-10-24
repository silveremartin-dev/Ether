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
            // -6,000 BP: Middle Neolithic Vinča Copper, Ubaid Gulf Maritime, Yangtze Jade
            drawTradeRoute(g, new double[][]{{20.6, 44.8}, {21.5, 43.5}, {23.0, 42.0}, {25.0, 40.0}}, new Color(240, 190, 50), 3.0);  // Vinča Rudna Glava Copper Network – bright gold
            drawTradeRoute(g, new double[][]{{46.0, 31.0}, {48.5, 30.0}, {50.5, 26.5}, {56.0, 24.5}}, new Color(20, 150, 210), 3.0);  // Persian Gulf Ubaid Maritime Route – oceanic blue
            drawTradeRoute(g, new double[][]{{109.1, 34.3}, {112.5, 34.5}, {115.0, 34.0}}, new Color(240, 190, 50), 2.5);             // Banpo Yangshao Painted Pottery – bright gold
            drawTradeRoute(g, new double[][]{{-77.0, -10.0}, {-77.5, -11.0}}, new Color(240, 190, 50), 2.5);                           // Early Andean Coast - Highland Exchange – bright gold
        } else if (year <= -2500L) {
            // -3,000 BP: Early Bronze Age Tin & Copper Circuits, Dilmun Gulf, Caral Fishery
            drawTradeRoute(g, new double[][]{{45.6, 31.3}, {48.5, 30.0}, {50.5, 26.2}, {58.5, 23.5}}, new Color(245, 195, 55), 3.2);  // Dilmun / Magan Bronze Age Gulf Network – rich gold
            drawTradeRoute(g, new double[][]{{31.2, 29.9}, {33.5, 28.0}, {34.5, 28.0}}, new Color(20, 160, 220), 2.8);               // Old Kingdom Sinai Turquoise & Copper – ocean cyan
            drawTradeRoute(g, new double[][]{{70.7, 36.2}, {68.0, 27.5}, {68.7, 27.5}}, new Color(40, 80, 200), 2.8);                 // Badakhshan Lapis to Indus – lapis blue
            drawTradeRoute(g, new double[][]{{-77.5, -10.9}, {-77.6, -10.6}}, new Color(245, 195, 55), 2.5);                           // Caral-Supe Cotton & Anchovy Network – rich gold
            drawTradeRoute(g, new double[][]{{120.0, 30.4}, {121.5, 31.2}}, new Color(245, 195, 55), 2.5);                            // Liangzhu Jade Exchange – rich gold
            drawTradeRoute(g, new double[][]{{25.2, 35.3}, {24.0, 37.0}, {23.7, 37.9}}, new Color(20, 160, 220), 2.8);               // Cycladic / Minoan Early Aegean Maritime – ocean cyan
        } else if (year <= -1200L) {
            // -1,500 BP: Late Bronze Age Mediterranean Tin & Glass, Shang Jade, Vedic Amber
            drawTradeRoute(g, new double[][]{{35.2, 33.0}, {33.0, 34.5}, {28.0, 36.0}, {25.2, 35.3}, {22.7, 37.7}}, new Color(250, 200, 60), 3.5); // Uluburun Mediterranean Circuit – radiant gold
            drawTradeRoute(g, new double[][]{{32.6, 25.7}, {34.0, 27.0}, {35.0, 20.0}, {40.0, 15.0}}, new Color(250, 200, 60), 3.0);  // Egyptian Punt Expedition – radiant gold
            drawTradeRoute(g, new double[][]{{114.3, 36.1}, {112.0, 34.5}, {108.0, 34.0}}, new Color(250, 200, 60), 3.0);             // Shang Dynasty Jade / Bronze Tribute – radiant gold
            drawTradeRoute(g, new double[][]{{-94.8, 17.8}, {-96.0, 17.0}, {-98.0, 19.0}}, new Color(250, 200, 60), 2.5);             // Olmec Jade & Magnetite Network – radiant gold
        } else if (year <= 0L) {
            // 500 BC to 0 AD: Classical Silk Road & Indian Ocean Monsoon Maritime Network
            // 1. SILK ROAD NORTHERN & SOUTHERN BRANCHES (Chang'an -> Dunhuang -> Samarkand -> Merv -> Ctesiphon -> Antioch -> Rome)
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {104.0, 36.0}, {94.7, 40.1}, {86.0, 42.0}, {76.0, 40.5}, {69.2, 41.3}, {67.0, 39.6}, {62.0, 37.5}, {51.4, 35.7}, {44.4, 33.3}, {36.2, 36.2}, {31.2, 30.0}, {12.5, 41.9}}, new Color(255, 190, 40), 3.8); // Northern Silk Artery
            drawTradeRoute(g, new double[][]{{94.7, 40.1}, {80.0, 37.0}, {75.0, 37.5}, {68.0, 36.0}, {62.0, 37.5}}, new Color(255, 190, 40), 2.8); // Southern Silk Road (Khotan Jade)

            // 2. ERYTHRAEAN SEA / INDIAN OCEAN MONSOON MARITIME ROUTE (Alexandria -> Berenike -> Aden -> Barygaza / Muziris)
            drawTradeRoute(g, new double[][]{{32.3, 31.2}, {32.6, 26.0}, {35.5, 23.9}, {43.3, 12.6}, {54.0, 17.0}, {72.8, 19.0}, {76.2, 10.0}}, new Color(30, 170, 220), 3.5);

            // 3. ROMAN MARE NOSTRUM HIGHWAYS (Rome -> Carthage -> Alexandria -> Gades)
            drawTradeRoute(g, new double[][]{{12.5, 41.9}, {10.3, 36.8}, {3.0, 36.7}, {-5.5, 36.0}}, new Color(30, 170, 220), 3.2);
            drawTradeRoute(g, new double[][]{{12.5, 41.9}, {15.0, 38.0}, {23.7, 37.9}, {29.9, 31.2}}, new Color(30, 170, 220), 3.2);

            // 4. INCENSE ROUTE (South Arabia Frankincense -> Petra -> Gaza)
            drawTradeRoute(g, new double[][]{{54.0, 17.0}, {48.5, 15.5}, {44.0, 15.0}, {40.0, 21.0}, {38.5, 24.5}, {35.4, 30.3}, {34.4, 31.5}}, new Color(220, 160, 40), 2.8);

            // 5. GRAND CANAL PRECURSORS & CHINESE INTERNAL RIVERS
            drawTradeRoute(g, new double[][]{{108.9, 34.3}, {112.5, 34.7}, {114.3, 34.8}, {117.2, 36.6}, {119.4, 32.4}}, new Color(240, 180, 50), 3.0);
        } else if (year <= 1500L) {
            // Post-Classical & High Medieval (1000-1491 AD): Mongol Pax, Hanseatic, Trans-Saharan, Maritime Silk Road
            // 1. MARITIME SILK ROAD & SPICE ARCHIPELAGO (Quanzhou -> Malacca -> Calicut -> Hormuz / Cairo -> Venice)
            drawTradeRoute(g, new double[][]{{118.6, 24.9}, {113.3, 23.1}, {109.0, 12.0}, {103.8, 1.3}, {95.0, 5.5}, {80.2, 6.9}, {76.0, 11.2}, {72.8, 19.0}, {56.3, 27.1}, {48.0, 30.0}}, new Color(20, 180, 230), 4.2); // Quanzhou to Persian Gulf
            drawTradeRoute(g, new double[][]{{76.0, 11.2}, {54.0, 17.0}, {43.3, 12.6}, {35.5, 23.9}, {32.5, 29.9}, {31.2, 31.2}, {24.0, 35.0}, {12.3, 45.4}}, new Color(20, 180, 230), 4.0); // Calicut -> Red Sea -> Venice

            // 2. MONGOL STEPPE SILK ROAD (Karakorum -> Sarai -> Caffa -> Constantinople)
            drawTradeRoute(g, new double[][]{{102.8, 47.2}, {85.0, 45.0}, {76.9, 43.3}, {69.0, 41.0}, {60.0, 44.0}, {48.0, 47.0}, {35.4, 45.0}, {28.9, 41.0}}, new Color(255, 180, 30), 3.8);

            // 3. TRANS-SAHARAN GOLD & SALT ARTERIES (Timbuktu / Gao -> Sijilmassa -> Fez / Cairo)
            drawTradeRoute(g, new double[][]{{-3.0, 16.8}, {-1.0, 21.0}, {-2.5, 27.0}, {-4.0, 31.5}, {-5.0, 34.0}}, new Color(240, 150, 30), 3.2); // Gold Road to Fez
            drawTradeRoute(g, new double[][]{{0.0, 16.2}, {9.0, 18.0}, {13.0, 24.0}, {14.0, 27.0}, {20.0, 30.0}, {31.2, 30.0}}, new Color(240, 150, 30), 3.0); // Salt Road to Cairo

            // 4. HANSEATIC LEAGUE & BALTIC-NORTH SEA NETWORKS (London -> Bruges -> Lübeck -> Danzig -> Novgorod)
            drawTradeRoute(g, new double[][]{{-0.1, 51.5}, {3.2, 51.2}, {6.0, 53.5}, {10.7, 53.9}, {18.6, 54.4}, {24.1, 56.9}, {31.3, 58.5}}, new Color(80, 160, 240), 3.4);

            // 5. SWAHILI COAST MONSOON CORRIDOR (Kilwa -> Zanzibar -> Mogadishu -> Aden -> India)
            drawTradeRoute(g, new double[][]{{39.5, -8.9}, {39.2, -6.1}, {41.0, -2.2}, {45.3, 2.0}, {43.3, 12.6}}, new Color(20, 180, 230), 3.0);

            // 6. INCA ROYAL HIGHWAY (Qhapaq Ñan: Quito -> Cusco -> Lake Titicaca -> Tucumán)
            drawTradeRoute(g, new double[][]{{-78.5, -0.2}, {-79.0, -4.0}, {-77.5, -9.0}, {-76.0, -12.0}, {-72.0, -13.5}, {-69.0, -16.0}, {-66.0, -22.0}, {-65.0, -27.0}}, new Color(230, 110, 40), 3.2);

            // 7. MESOAMERICAN POCHTECA ROADS (Tenochtitlan -> Gulf Coast -> Xoconochco / Soconusco)
            drawTradeRoute(g, new double[][]{{-99.1, 19.4}, {-96.9, 19.5}, {-94.5, 18.0}, {-92.4, 15.0}}, new Color(230, 110, 40), 2.8);
        } else if (year <= 1850L) {
            // Early Modern (1500-1850 AD): Age of Sail, Manila Galleons, Triangular Atlantic Trade
            // 1. SPANISH TREASURE FLEET & MANILA GALLEONS
            drawTradeRoute(g, new double[][]{{121.0, 14.6}, {140.0, 20.0}, {-170.0, 25.0}, {-130.0, 30.0}, {-104.3, 19.0}}, new Color(255, 215, 0), 3.8); // Manila -> Acapulco
            drawTradeRoute(g, new double[][]{{-96.1, 19.2}, {-85.0, 22.0}, {-80.0, 25.0}, {-60.0, 35.0}, {-30.0, 40.0}, {-6.0, 36.5}}, new Color(255, 215, 0), 4.0); // Veracruz/Havana -> Seville/Cadiz
            drawTradeRoute(g, new double[][]{{-77.0, -12.0}, {-79.5, 9.0}}, new Color(255, 215, 0), 3.0); // Lima to Panama

            // 2. VOC & EIC CAPE ROUTE TO THE INDIES (London/Amsterdam -> Cape of Good Hope -> Batavia)
            drawTradeRoute(g, new double[][]{{4.4, 51.9}, {-5.0, 49.0}, {-15.0, 30.0}, {-25.0, 0.0}, {-20.0, -20.0}, {18.4, -33.9}, {60.0, -35.0}, {90.0, -30.0}, {106.8, -6.2}}, new Color(30, 200, 220), 4.2);

            // 3. TRIANGULAR TRANSATLANTIC ROUTE (Europe -> West Africa -> Caribbean/Americas -> Europe)
            drawTradeRoute(g, new double[][]{{-0.1, 51.5}, {-10.0, 40.0}, {-15.0, 20.0}, {-5.0, 5.0}}, new Color(210, 100, 80), 3.5); // Europe -> Guinea Coast
            drawTradeRoute(g, new double[][]{{-5.0, 5.0}, {-30.0, 0.0}, {-60.0, 15.0}, {-75.0, 20.0}}, new Color(210, 100, 80), 3.5); // Middle Passage
            drawTradeRoute(g, new double[][]{{-75.0, 20.0}, {-60.0, 35.0}, {-30.0, 45.0}, {-0.1, 51.5}}, new Color(210, 100, 80), 3.5); // Return leg (Sugar/Tobacco)

            // 4. PORTUGUESE CARREIRA DA INDIA (Lisbon -> Mozambique -> Goa -> Malacca -> Macau)
            drawTradeRoute(g, new double[][]{{-9.1, 38.7}, {18.4, -33.9}, {40.5, -15.0}, {73.8, 15.5}, {102.2, 2.2}, {113.5, 22.2}}, new Color(30, 200, 220), 3.8);

            // 5. INTRA-ASIAN JUNK TRADE & SPICE ARCHIPELAGO
            drawTradeRoute(g, new double[][]{{113.5, 22.2}, {121.5, 31.2}, {130.0, 33.0}, {139.7, 35.6}}, new Color(255, 190, 40), 3.2);
            drawTradeRoute(g, new double[][]{{113.5, 22.2}, {106.8, -6.2}, {115.0, -8.5}, {128.0, -3.5}}, new Color(255, 190, 40), 3.2); // South China Sea to Moluccas Spice Islands

            // 6. SAHARA CARAVAN NETWORKS (Tripoli -> Ghadames -> Ghat -> Kano)
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
