/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Catalog of sub-regional linguistic (Glottolog 4.8) and anthropological kinship (Murdock D-PLACE /
 * Emmanuel Todd) seed coordinates and Classical Antiquity autonomous city-state enclaves supporting
 * {@link CliopatriaPolityVectorReader}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CliopatriaPolityVectorReader
 */
final class CliopatriaSubregionalCentersCatalog {

    private CliopatriaSubregionalCentersCatalog() {
        // Static catalog class
    }

    /**
     * Renders autonomous Silk Road oasis kingdoms, Caucasus buffer states, and East Asian proto-states
     * during Classical Antiquity (\(-500 \le t \le 500\text{ CE}\)).
     *
     * @param g target graphics context
     * @param width raster width in pixels
     * @param height raster height in pixels
     */
    static void renderClassicalAntiquityEnclaves(Graphics2D g, int width, int height) {
        record ClassicalPolity(double lon, double lat, double radiusDeg, int color) {}
        ClassicalPolity[] classicalStates = {
            // Tarim Basin Silk Road Oasis City-States (Distinct from Han Core)
            new ClassicalPolity(79.92, 37.11, 2.2, 0xEAB308),  // Kingdom of Khotan (Yutian #EAB308 Gold)
            new ClassicalPolity(82.96, 41.72, 2.2, 0xA855F7),  // Kingdom of Kucha (Qiuci #A855F7 Purple)
            new ClassicalPolity(89.17, 39.03, 2.5, 0x06B6D4),  // Kingdom of Shanshan / Loulan (#06B6D4 Cyan)
            new ClassicalPolity(75.98, 39.46, 2.0, 0x10B981),  // Kingdom of Kashgar (Shule #10B981 Emerald)
            new ClassicalPolity(89.18, 42.95, 1.8, 0xF97316),  // Kingdom of Turpan / Jiaohe (#F97316 Amber)

            // Near East, Caucasus & Black Sea Client / Independent States
            new ClassicalPolity(35.44, 30.32, 2.5, 0xEA580C),  // Nabataean Kingdom (Petra #EA580C Terracotta)
            new ClassicalPolity(44.50, 40.18, 3.5, 0x9333EA),  // Kingdom of Armenia (Artaxiad #9333EA Royal Purple)
            new ClassicalPolity(44.72, 41.84, 2.0, 0x84CC16),  // Caucasian Iberia (#84CC16 Lime)
            new ClassicalPolity(47.85, 40.98, 2.2, 0x14B8A6),  // Caucasian Albania (#14B8A6 Teal)
            new ClassicalPolity(36.47, 45.35, 2.5, 0x0284C7),  // Bosporan Kingdom (Crimea #0284C7 Sky Blue)
            new ClassicalPolity(2.25, 36.59, 3.0, 0xD97706),   // Kingdom of Mauretania (Juba II #D97706 Bronze)
            new ClassicalPolity(13.06, 26.53, 3.2, 0xB45309),  // Garamantes Saharan Kingdom (#B45309 Ochre)

            // Asia & Proto-States
            new ClassicalPolity(105.15, 10.50, 3.0, 0x16A085), // Kingdom of Funan (Mekong Delta #16A085)
            new ClassicalPolity(125.75, 39.03, 2.5, 0x8B5CF6), // Early Goguryeo (Korea #8B5CF6)
            new ClassicalPolity(127.00, 37.50, 2.0, 0xEC4899), // Early Baekje / Mahan (#EC4899)
            new ClassicalPolity(129.20, 35.80, 2.0, 0x3B82F6), // Early Silla / Jinhan (#3B82F6)
            new ClassicalPolity(130.40, 33.60, 2.5, 0xF43F5E)  // Yayoi Chiefdoms of Japan (#F43F5E)
        };

        for (ClassicalPolity cp : classicalStates) {
            g.setColor(new Color(cp.color));
            int cx = (int) (((cp.lon + 180.0) / 360.0) * width);
            int cy = (int) (((90.0 - cp.lat) / 180.0) * height);
            int rx = (int) (cp.radiusDeg / 360.0 * width);
            int ry = (int) (cp.radiusDeg / 180.0 * height);
            g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
        }
    }

    /**
     * Returns high-precision sub-regional and frontier linguistic centers {@code [lon, lat, hexColor, sigma]}
     * parameterized by historical epoch.
     *
     * @param targetYear simulation epoch year
     * @return array of linguistic center tuples
     */
    static double[][] getSubregionalLinguisticCenters(long targetYear) {
        return new double[][] {
            // --- Western & Central Europe: Romance Sub-branches (or Celtic/Iberian in Antiquity) ---
            {2.35, 48.86, (targetYear < 500L ? 0x10B981 : 0xEC4899), 4.5},    // French / Gaulish
            {3.06, 50.63, (targetYear < 500L ? 0x10B981 : 0xEC4899), 3.0},    // Picard / Belgic Gaulish
            {-0.58, 44.84, (targetYear < 500L ? 0xF59E0B : 0xF43F5E), 4.0},   // Aquitanian / Proto-Basque / Gascon
            {1.44, 43.60, (targetYear < 500L ? 0x10B981 : 0xF43F5E), 4.0},    // Occitan / Volcae Gaulish
            {5.37, 43.30, (targetYear < 500L ? 0xE879F9 : 0xF43F5E), 3.5},    // Massalia / Provençal
            {4.83, 45.76, (targetYear < 500L ? 0xE879F9 : 0xFB7185), 3.0},    // Lugdunum / Arpitan
            {-2.75, 48.10, 0x10B981, 2.5},                                    // Breton / Armorican Celtic
            {-3.70, 40.42, (targetYear < 500L ? 0x059669 : 0xE11D48), 4.5},   // Celtiberian / Castilian Spanish
            {-5.99, 37.38, (targetYear < 500L ? 0xE879F9 : 0xE11D48), 4.0},   // Baetica Latin / Andalusian Spanish
            {2.17, 41.38, (targetYear < 500L ? 0xD97706 : 0xBE185D), 3.2},    // Iberian / Catalan
            {-0.38, 39.47, (targetYear < 500L ? 0xD97706 : 0xBE185D), 3.0},   // Edetanian Iberian / Valencian
            {-2.93, 43.26, 0xF59E0B, 2.0},                                    // Basque / Euskara / Vasconic
            {-9.14, 38.72, (targetYear < 500L ? 0x059669 : 0x9D174D), 3.5},   // Lusitanian / Portuguese
            {-8.61, 41.15, (targetYear < 500L ? 0x059669 : 0x9D174D), 3.5},   // Gallaecian Celtic / Galician
            {9.19, 45.46, (targetYear < 500L ? 0xE879F9 : 0xFB7185), 3.5},    // Cisalpine Gaul / Lombard
            {12.33, 45.44, (targetYear < 500L ? 0xE879F9 : 0xFB7185), 3.0},   // Venetic / Venetian
            {11.25, 43.77, (targetYear < 500L ? 0xE879F9 : 0xE879F9), 3.0},   // Etruscan / Roman Latin (Florence)
            {12.49, 41.90, 0xE879F9, 3.5},                                    // Roman Classical Latin (Rome)
            {14.26, 40.85, 0xE879F9, 3.5},                                    // Roman / Oscan / Magna Graecia
            {14.01, 37.59, 0xE879F9, 3.0},                                    // Sicilian (Palermo)
            {26.10, 44.43, (targetYear < 500L ? 0x0284C7 : 0xD946EF), 4.0},   // Dacian / Thracian / Romanian

            // --- Germanic Phyla ---
            {-0.13, 51.51, (targetYear < 500L ? 0x10B981 : 0x2563EB), 4.0},   // Brittonic / English
            {-1.54, 53.80, (targetYear < 500L ? 0x10B981 : 0x2563EB), 3.5},   // Brigantian Celtic / Northern English
            {-3.18, 55.95, (targetYear < 500L ? 0x10B981 : 0x2563EB), 3.0},   // Votadini Celtic / Scots
            {-4.25, 57.47, (targetYear < 500L ? 0x059669 : 0x059669), 2.8},   // Pictish / Scottish Gaelic
            {-7.50, 53.40, 0x059669, 3.0},                                    // Primitive Irish / Goidelic Celtic
            {-3.90, 52.41, 0x10B981, 2.2},                                    // Welsh / Brythonic
            {4.90, 52.37, 0x60A5FA, 3.0},                                     // Dutch / Netherlandic
            {3.72, 51.05, 0x60A5FA, 2.5},                                     // Flemish
            {9.99, 53.55, 0x60A5FA, 3.5},                                     // Low German / Plattdüütsch
            {13.40, 52.52, 0x3B82F6, 4.5},                                    // High German (Brandenburg / Berlin)
            {6.96, 50.94, 0x3B82F6, 3.0},                                     // Rhenish German
            {9.18, 48.78, 0x3B82F6, 3.0},                                     // Swabian / Alemannic
            {11.58, 48.14, 0x3B82F6, 3.5},                                    // Bavarian German
            {16.37, 48.21, 0x3B82F6, 3.5},                                    // Austrian German
            {8.54, 47.37, 0x3B82F6, 2.5},                                     // Swiss German
            {12.56, 55.67, 0x93C5FD, 3.0},                                    // Danish
            {18.06, 59.33, 0x93C5FD, 4.5},                                    // Swedish
            {10.75, 59.91, 0x93C5FD, 4.0},                                    // Norwegian
            {-21.94, 64.14, 0x93C5FD, 3.5},                                   // Icelandic

            // --- Uralic Phyla ---
            {19.04, 47.50, 0x14B8A6, 4.0},   // Hungarian / Magyar
            {24.94, 60.17, 0x0D9488, 4.5},   // Finnish
            {24.75, 59.43, 0x0D9488, 2.5},   // Estonian
            {25.00, 68.00, 0x0F766E, 4.0},   // Saami
            {52.00, 56.00, 0x14B8A6, 3.5},   // Udmurt / Mari
            {65.00, 62.00, 0x14B8A6, 6.0},   // Khanty / Mansi
            {75.00, 68.00, 0x14B8A6, 5.0},   // Nenets / Samoyedic

            // --- Eastern Europe & Slavic Phyla ---
            {21.01, 52.23, 0xA855F7, 4.0},   // Polish
            {14.42, 50.08, 0xA855F7, 2.8},   // Czech
            {17.10, 48.14, 0xA855F7, 2.5},   // Slovak
            {30.52, 50.45, 0x8B5CF6, 5.0},   // Ukrainian
            {27.56, 53.90, 0x8B5CF6, 4.0},   // Belarusian
            {37.62, 55.75, 0x8B5CF6, 7.0},   // Russian
            {30.32, 59.93, 0x8B5CF6, 5.0},   // Northern Russian
            {25.27, 54.68, 0x6366F1, 2.8},   // Lithuanian
            {24.10, 56.95, 0x6366F1, 2.5},   // Latvian
            {20.45, 44.81, 0x7C3AED, 3.5},   // Serbian / Croatian
            {23.32, 42.69, 0x7C3AED, 3.0},   // Bulgarian
            {14.50, 46.05, 0x7C3AED, 2.2},   // Slovenian
            {19.81, 41.32, 0x0284C7, 2.2},   // Albanian
            {23.72, 37.98, 0x06B6D4, 3.5},   // Greek
            {33.00, 35.00, 0x06B6D4, 2.0},   // Cypriot Greek

            // --- Middle East & North Africa ---
            {32.85, 39.93, 0x0EA5E9, 5.0},   // Turkish
            {49.86, 40.40, 0x0EA5E9, 3.0},   // Azerbaijani
            {60.00, 39.00, 0x0EA5E9, 4.0},   // Turkmen
            {44.80, 41.71, 0x84CC16, 2.5},   // Georgian / Kartvelian
            {45.00, 43.00, 0x84CC16, 2.5},   // North Caucasian
            {44.50, 40.18, 0x38BDF8, 2.2},   // Armenian
            {44.00, 36.00, 0xD97706, 3.5},   // Kurdish
            {36.29, 33.51, 0x15803D, 4.0},   // Levantine Arabic
            {31.24, 30.04, 0x15803D, 5.0},   // Egyptian Arabic
            {44.36, 33.31, 0x15803D, 4.5},   // Mesopotamian Arabic
            {46.71, 24.63, 0x15803D, 6.0},   // Peninsular Arabic
            {44.20, 15.35, 0x15803D, 3.5},   // Yemeni Arabic
            {58.59, 23.61, 0x15803D, 3.5},   // Omani Arabic
            {10.18, 36.80, 0x15803D, 3.5},   // Tunisian Arabic
            {3.05, 36.75, 0x15803D, 4.0},    // Algerian Arabic
            {4.00, 36.50, 0x047857, 2.5},    // Kabyle Berber
            {-5.00, 34.00, 0x15803D, 4.0},   // Moroccan Arabic
            {-7.00, 31.00, 0x047857, 3.5},   // Shilha / Tamazight Berber
            {-1.00, 24.00, 0x047857, 6.0},   // Tuareg Berber
            {51.39, 35.69, 0xD97706, 6.0},   // Persian / Farsi
            {69.17, 34.53, 0xD97706, 4.0},   // Pashto / Dari
            {64.00, 28.00, 0xD97706, 4.0},   // Balochi
            {68.78, 38.56, 0xD97706, 2.5},   // Tajik

            // --- Central Asia & Siberia ---
            {69.00, 41.00, 0x38BDF8, 5.0},   // Uzbek
            {70.00, 48.00, 0x38BDF8, 8.0},   // Kazakh
            {74.50, 42.87, 0x38BDF8, 3.5},   // Kyrgyz
            {85.00, 42.00, 0x38BDF8, 5.0},   // Uyghur
            {49.12, 55.79, 0x38BDF8, 3.5},   // Tatar
            {56.00, 54.00, 0x38BDF8, 3.5},   // Bashkir
            {129.7, 62.03, 0x38BDF8, 6.0},   // Yakut / Sakha
            {106.0, 47.00, 0xF97316, 7.0},   // Mongolian / Khalkha
            {107.5, 52.00, 0xF97316, 4.0},   // Buryat
            {46.00, 46.50, 0xF97316, 3.0},   // Kalmyk
            {95.00, 60.00, 0x7C3AED, 8.0},   // Evenki / Tungusic
            {126.0, 47.00, 0x7C3AED, 5.0},   // Manchu / Tungusic
            {160.0, 65.00, 0x607D8B, 6.0},   // Chukchi / Koryak

            // --- East & Southeast Asia ---
            {116.4, 39.90, 0xEF4444, 6.0},   // Mandarin Chinese
            {114.0, 34.00, 0xEF4444, 5.5},   // Zhongyuan Mandarin
            {104.0, 30.60, 0xEF4444, 5.0},   // Southwestern Mandarin
            {120.5, 31.00, 0xF87171, 4.0},   // Wu Chinese
            {119.3, 26.08, 0xB91C1C, 3.5},   // Min Chinese
            {113.2, 23.13, 0xDC2626, 4.0},   // Yue / Cantonese
            {115.8, 28.68, 0xEA580C, 3.5},   // Hakka / Gan Chinese
            {112.9, 28.20, 0xEA580C, 3.5},   // Xiang Chinese
            {88.00, 31.00, 0xDC2626, 6.0},   // Standard Tibetan
            {100.0, 33.00, 0xDC2626, 4.5},   // Amdo / Kham Tibetan
            {96.16, 16.86, 0xDC2626, 4.5},   // Burmese
            {126.9, 37.57, 0xA855F7, 4.0},   // Korean
            {139.6, 35.69, 0xF43F5E, 5.0},   // Japanese (Honshu)
            {130.4, 33.60, 0xF43F5E, 3.5},   // Kyushu Japanese
            {127.7, 26.21, 0xF43F5E, 2.0},   // Ryukyuan
            {142.0, 43.00, 0x607D8B, 3.0},   // Ainu
            {105.8, 21.03, 0x16A085, 4.0},   // Vietnamese (North)
            {108.0, 16.00, 0x16A085, 3.5},   // Central Vietnamese
            {104.9, 11.55, 0x16A085, 3.5},   // Khmer / Cambodian
            {100.5, 13.75, 0xEAB308, 4.5},   // Central Thai / Siamese
            {102.6, 17.97, 0xEAB308, 3.5},   // Lao / Isan
            {99.00, 19.00, 0xEAB308, 3.0},   // Shan / Northern Thai

            // --- Indian Subcontinent ---
            {77.21, 28.61, 0xF59E0B, 5.0},   // Hindi / Hindustani / Urdu
            {75.00, 31.00, 0xFCD34D, 4.0},   // Punjabi
            {88.36, 22.57, 0xFBBF24, 5.0},   // Bengali
            {73.86, 18.52, 0xD97706, 4.5},   // Marathi
            {72.57, 23.02, 0xD97706, 4.0},   // Gujarati
            {85.82, 20.29, 0xF59E0B, 3.5},   // Odia / Oriya
            {68.50, 26.00, 0xFCD34D, 3.5},   // Sindhi
            {85.32, 27.71, 0xF59E0B, 3.0},   // Nepali
            {80.50, 7.50, 0xF59E0B, 2.5},    // Sinhala
            {80.27, 13.08, 0xB45309, 4.5},   // Tamil
            {78.48, 17.38, 0x92400E, 4.5},   // Telugu
            {76.50, 14.00, 0x92400E, 4.0},   // Kannada
            {76.27, 9.93, 0xB45309, 3.0},    // Malayalam

            // --- Maritime Southeast Asia & Oceania ---
            {106.8, -6.21, 0x06B6D4, 4.5},   // Javanese / Sundanese
            {101.7, 3.14, 0x06B6D4, 4.0},    // Malay
            {115.0, 0.00, 0x06B6D4, 5.0},    // Dayak / Bornean
            {120.0, -4.00, 0x06B6D4, 4.0},   // Bugis / Makassarese
            {121.0, 14.60, 0x06B6D4, 4.5},   // Tagalog / Ilocano
            {123.0, 10.00, 0x06B6D4, 4.0},   // Visayan
            {47.00, -19.0, 0x06B6D4, 4.0},   // Malagasy
            {140.0, -4.50, 0x9C27B0, 5.0},   // Trans-New Guinea / Papuan
            {133.5, -24.0, 0xE91E63, 8.0},   // Pama-Nyungan Aboriginal
            {132.0, -13.0, 0xE91E63, 4.0},   // Arnhem Land Non-Pama-Nyungan
            {151.2, -33.8, 0xE91E63, 4.0},   // Sydney / Coastal Aboriginal
            {175.0, -39.0, 0x06B6D4, 4.0},   // Maori
            {-157.8, 21.3, 0x06B6D4, 3.0},   // Hawaiian
            {-172.0, -14.0, 0x06B6D4, 3.0},  // Samoan
            {-149.5, -17.5, 0x06B6D4, 3.0},  // Tahitian

            // --- Sub-Saharan Africa ---
            {-8.00, 12.0, 0x16A34A, 4.0},    // Manding / Bambara
            {-1.00, 10.0, 0x059669, 3.5},    // Gur / Voltaic
            {-1.50, 6.50, 0x15803D, 3.0},    // Akan / Kwa
            {3.50, 7.00, 0x15803D, 3.5},     // Yoruba / Kwa
            {7.00, 5.50, 0x16A34A, 3.0},     // Igbo
            {5.23, 13.06, 0x22C55E, 3.5},    // Hausa / Chadic
            {-16.0, 14.5, 0x22C55E, 3.5},    // Wolof / Senegambian
            {0.00, 15.0, 0xC0392B, 3.5},     // Songhay
            {14.00, 13.0, 0xC0392B, 4.0},    // Kanuri
            {25.00, 13.0, 0xC0392B, 4.0},    // Fur / Darfur
            {32.00, 8.00, 0x0284C7, 4.0},    // Eastern Sudanic / Nilotic
            {20.00, 5.00, 0x0D9488, 4.0},    // Ubangian / Central Sudanic
            {38.74, 9.03, 0x166534, 3.5},    // Ge'ez / Ethiopian Semitic
            {39.00, 7.00, 0x84CC16, 4.0},    // Cushitic (Oromo / Sidama)
            {45.00, 5.00, 0x84CC16, 5.0},    // Somali / Lowland Cushitic
            {35.50, -4.5, 0xEAB308, 3.0},    // Rift Valley Foragers (Hadza / Sandawe)
            {11.00, 4.50, 0x22C55E, 3.5},    // Grassfields / Proto-Bantu Cradle
            {15.30, -4.3, (targetYear < 500L ? 0x0D9488 : 0x22C55E), 4.5},
            {36.82, -1.2, (targetYear < 500L ? 0x84CC16 : 0x22C55E), 4.0},
            (targetYear < 500L ? new double[]{28.00, -28.0, 0xFACC15, 6.0} : new double[]{31.00, -29.0, 0x22C55E, 4.0}),
            (targetYear < 500L ? new double[]{24.00, -32.0, 0xFACC15, 6.0} : new double[]{26.00, -26.0, 0x22C55E, 4.0}),
            {21.00, -25.0, 0xFACC15, 5.5},   // Khoisan / Ju|'hoan / Nama
            {18.00, -33.5, 0xFACC15, 4.5},   // Cape Khoekhoe

            // --- Americas ---
            {-75.0, 40.0, (targetYear >= 1700L ? 0x2563EB : 0x3F51B5), 4.5},
            {-71.0, 46.8, (targetYear >= 1700L ? 0xEC4899 : 0x3F51B5), 3.5},
            {-76.0, 43.0, 0x3F51B5, 3.5},    // Iroquoian
            {-85.0, 46.0, 0x3F51B5, 4.5},    // Algonquian
            {-100.0, 45.0, 0x3F51B5, 5.0},   // Siouan
            {-102.0, 35.0, 0xFF9800, 4.5},   // Comanche / Shoshone
            {-110.0, 35.0, 0x009688, 3.5},   // Navajo / Apache
            {-120.0, 58.0, 0x009688, 6.0},   // Northern Athabaskan / Dene
            {-135.0, 57.0, 0x009688, 3.5},   // Tlingit / Haida
            {-90.0, 68.0, 0x607D8B, 7.0},    // Inuit
            {-160.0, 64.0, 0x607D8B, 5.0},   // Yupik
            {-99.13, 19.43, 0xFF9800, 4.0},  // Nahuatl / Mesoamerican
            {-89.62, 17.22, 0xFF9800, 4.0},  // Mayan
            {-77.04, -12.0, 0x795548, 5.0},  // Quechua
            {-68.15, -16.5, 0x795548, 3.5},  // Aymara
            {-55.0, -15.0, 0x4CAF50, 6.0},   // Tupi-Guarani
            {-62.0, -3.0, 0x4CAF50, 6.0},    // Arawakan / Cariban
            {-72.0, -38.0, 0x8D6E63, 4.0}    // Mapuche
        };
    }

    /**
     * Returns high-precision sub-regional kinship and family-structure centers {@code [lon, lat, hexColor, sigma]}
     * parameterized by historical epoch (Emmanuel Todd &amp; Murdock D-PLACE).
     *
     * @param targetYear simulation epoch year
     * @return array of kinship center tuples
     */
    static double[][] getSubregionalKinshipCenters(long targetYear) {
        return new double[][] {
            // --- Western Europe: Todd Typology ---
            {2.35, 48.86, 0xEC4899, 4.5},    // Northern France (Egalitarian Nuclear)
            {3.06, 50.63, 0xEC4899, 3.0},    // Lille / Picardy
            {4.35, 50.85, 0xEC4899, 3.0},    // Belgium / Wallonia
            {3.72, 51.05, 0x3B82F6, 2.5},    // Flanders (Absolute Nuclear)
            {4.90, 52.37, 0x3B82F6, 3.0},    // Netherlands
            {-0.13, 51.51, 0x3B82F6, 4.0},   // England (Absolute Nuclear)
            {-1.54, 53.80, 0x3B82F6, 3.5},   // English Midlands / North
            {-3.18, 55.95, 0x3B82F6, 3.0},   // Scottish Lowlands
            {-4.25, 57.47, 0x8B5CF6, 2.8},   // Scottish Highlands (Stem Family / Clan)
            {-7.50, 53.40, 0x8B5CF6, 3.0},   // Ireland (Stem Family)
            {-0.58, 44.84, 0xA855F7, 4.0},   // Aquitaine / Gascony (Flexible Stem)
            {1.44, 43.60, 0xA855F7, 4.0},    // Occitanie / Languedoc
            {5.37, 43.30, 0xA855F7, 3.5},    // Provence / Midi
            {-2.75, 48.10, 0x8B5CF6, 2.5},   // Brittany (Famille Souche)
            {9.19, 45.46, 0xEC4899, 4.0},    // Northern Italy
            {12.49, 41.90, 0xEC4899, 3.5},   // Central Italy
            {14.26, 40.85, 0xEC4899, 3.5},   // Southern Italy / Sicily
            {23.72, 37.98, 0xEC4899, 3.5},   // Greece
            {-3.70, 40.42, 0xEC4899, 4.5},   // Castile / Madrid
            {-5.99, 37.38, 0xEC4899, 4.0},   // Andalusia
            {2.17, 41.38, 0x8B5CF6, 3.2},    // Catalonia (Hereu Primogeniture)
            {-2.93, 43.26, 0x8B5CF6, 2.0},   // Basque Country (Etxea Stem)
            {-8.61, 41.15, 0x8B5CF6, 3.5},   // Galicia
            {-9.14, 38.72, 0xEC4899, 4.0},   // Portugal
            {-75.0, 40.0, (targetYear >= 1700L ? 0x3B82F6 : 0xF43F5E), 5.0},

            // --- Central Europe, Scandinavia & Japan: Stem Family & Primogeniture (#8B5CF6) ---
            {13.40, 52.52, 0x8B5CF6, 4.5},   // Prussia / Northern Germany
            {9.99, 53.55, 0x8B5CF6, 3.5},    // Lower Saxony / Westphalia
            {11.58, 48.14, 0x8B5CF6, 3.5},   // Bavaria
            {16.37, 48.21, 0x8B5CF6, 4.0},   // Austria
            {8.54, 47.37, 0xA855F7, 2.5},    // Switzerland
            {14.42, 50.08, 0x8B5CF6, 2.8},   // Bohemia
            {18.06, 59.33, 0x8B5CF6, 5.0},   // Sweden
            {12.56, 55.67, 0x8B5CF6, 3.0},   // Denmark
            {10.75, 59.91, 0x8B5CF6, 4.0},   // Norway
            {24.94, 60.17, 0x8B5CF6, 4.5},   // Finland
            {139.6, 35.69, 0x8B5CF6, 5.0},   // Japan (Ie Stem Family)

            // --- Eastern Europe & Slavic Lands: Patriarchal Joint Family / Mir / Zadruga (#DC2626) ---
            {37.62, 55.75, 0xDC2626, 7.0},   // Russian Obshchina
            {30.52, 50.45, 0xDC2626, 5.0},   // Ukraine
            {21.01, 52.23, 0xDC2626, 4.0},   // Poland
            {17.10, 48.14, 0xDC2626, 2.5},   // Slovakia
            {19.04, 47.50, 0xDC2626, 4.0},   // Hungary
            {20.45, 44.81, 0xDC2626, 3.5},   // Balkan Zadruga
            {23.32, 42.69, 0xDC2626, 3.0},   // Bulgaria
            {26.10, 44.43, 0xDC2626, 4.0},   // Wallachia / Moldavia

            // --- East Asia: Agnatic Lineage & Confucian Zongzu Clan (#EF4444) ---
            {116.4, 39.90, 0xEF4444, 6.0},   // North China
            {114.0, 34.00, 0xEF4444, 5.5},   // Central Plains
            {120.5, 31.00, 0xEF4444, 4.0},   // Jiangnan
            {113.2, 23.13, 0xEF4444, 4.5},   // South China Lineage Estates
            {126.9, 37.57, 0xEF4444, 4.0},   // Joseon Munjung Clan
            {105.8, 21.03, 0xEF4444, 4.0},   // Vietnam Gia-Toc Clan
            {88.00, 31.00, 0xA855F7, 5.0},   // Tibet Fraternal Polyandry

            // --- Middle East & North Africa: Segmentary Patrilineal Lineage (#10B981) ---
            {32.85, 39.93, 0x10B981, 5.0},   // Anatolia
            {36.29, 33.51, 0x10B981, 4.0},   // Levant
            {31.24, 30.04, 0x10B981, 5.0},   // Egypt Hamula
            {46.71, 24.63, 0x10B981, 6.0},   // Arabian Bint'Amm Endogamy
            {44.20, 15.35, 0x10B981, 3.5},   // Yemen
            {51.39, 35.69, 0x10B981, 6.0},   // Persia
            {69.17, 34.53, 0x10B981, 4.0},   // Pashtunwali
            {-5.00, 34.00, 0x10B981, 4.5},   // Morocco
            {10.18, 36.80, 0x10B981, 3.5},   // Tunisia
            {3.05, 36.75, 0x10B981, 4.0},    // Algeria

            // --- India: Gotra Exogamous Joint Family & Dravidian Cross-Cousin (#F59E0B / #B45309) ---
            {77.21, 28.61, 0xF59E0B, 5.0},   // Northern India
            {75.00, 31.00, 0xF59E0B, 4.0},   // Punjab
            {88.36, 22.57, 0xF59E0B, 5.0},   // Bengal
            {73.86, 18.52, 0xF59E0B, 4.5},   // Deccan Maratha
            {80.27, 13.08, 0xB45309, 4.5},   // Tamil Dravidian
            {78.48, 17.38, 0xB45309, 4.5},   // Telugu Dravidian
            {76.50, 14.00, 0xB45309, 4.0},   // Kannada Dravidian
            {76.27, 9.93, 0xB45309, 3.0},    // Kerala Marumakkathayam

            // --- Central Asia, Steppe & Boreal Eurasia ---
            {45.00, 48.00, 0xD97706, 4.5},   // Pontic-Caspian Steppe
            {70.00, 48.00, 0xD97706, 5.0},   // Kazakh Zhuz Clans
            {106.0, 47.00, 0xD97706, 5.0},   // Mongolian Steppe
            {69.00, 41.00, 0x10B981, 4.0},   // Transoxiana Oasis Clans
            {55.00, 58.00, 0xA855F7, 4.0},   // Volga-Ural
            {65.00, 62.00, 0xA855F7, 4.0},   // Ob-Ugric
            {85.00, 66.00, 0x06B6D4, 4.5},   // Samoyedic / Yeniseian
            {115.0, 60.00, 0x10B981, 5.0},   // Tungusic / Evenki
            {160.0, 64.00, 0x64748B, 5.0},   // Paleosiberian

            // --- Matrilineal Belt, Sub-Saharan Africa, Oceania & Americas ---
            {100.5, -0.50, 0xF43F5E, 3.5},   // Minangkabau
            {-1.50, 6.50, 0xF43F5E, 3.0},    // Ashanti Abusua
            {28.00, -12.0, 0xF43F5E, 5.0},   // Bemba Matrilineal Belt
            {-76.0, 43.0, 0xF43F5E, 3.0},    // Iroquois Longhouse
            {-110.5, 35.8, 0xF43F5E, 2.5},   // Hopi / Pueblo
            {5.23, 13.06, 0x15803D, 4.0},    // Hausa / Fulani
            {3.50, 7.00, 0x15803D, 3.5},     // Yoruba
            {7.00, 5.50, 0x15803D, 3.0},     // Igbo Umunna
            {15.30, -4.3, 0x15803D, 5.0},    // Kongo
            (targetYear < 500L ? new double[]{28.00, -28.0, 0x84CC16, 6.0} : new double[]{31.00, -29.0, 0x15803D, 4.0}),
            {37.00, 1.00, 0xFF9800, 4.0},    // East African Age-Set
            {133.5, -24.0, 0xE91E63, 8.0},   // Australian 8-Skin Subsection
            {175.0, -39.0, 0x06B6D4, 4.0},   // Maori Iwi / Hapu
            {-157.8, 21.3, 0x06B6D4, 3.0},   // Hawaiian Ali'i Ramages
            {140.0, -4.50, 0x9C27B0, 5.0},   // New Guinea Clans
            {106.8, -6.21, 0x06B6D4, 4.5},   // Javanese Bilateral
            {101.7, 3.14, 0x06B6D4, 4.0},    // Malay Bilateral
            {-100.0, 45.0, 0x3F51B5, 5.0},   // Plains Bands
            {-120.0, 55.0, 0x009688, 5.0},   // Pacific Northwest Potlatch
            {-90.0, 68.0, 0x84CC16, 7.0},    // Inuit Bands
            {-99.13, 19.43, 0x795548, 4.0},  // Mesoamerican Calpulli
            {-77.04, -12.0, 0x795548, 5.0},  // Andean Ayllu
            {-55.0, -15.0, 0x4CAF50, 6.0},   // Amazonian Moiety
            {21.00, -25.0, 0x84CC16, 4.5}    // Khoisan Egalitarian Bands
        };
    }

    /**
     * Fills unassigned land pixels from an orographic background raster and masks oceans/uninhabited zones.
     *
     * @param img target polity raster
     * @param orographicBg pre-propagated orographic cultural background
     * @param elevationMask ETOPO coastline mask
     * @param targetYear historical epoch year
     * @param fallbackHex fallback RGB color if {@code orographicBg} is null
     */
    static void applyOrographicFallbackAndCoastlineMask(BufferedImage img, BufferedImage orographicBg,
                                                        BufferedImage elevationMask, long targetYear, int fallbackHex) {
        if (elevationMask == null) return;
        int width = img.getWidth();
        int height = img.getHeight();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;
                int mx = Math.clamp(x * elevationMask.getWidth() / width, 0, elevationMask.getWidth() - 1);
                int my = Math.clamp(y * elevationMask.getHeight() / height, 0, elevationMask.getHeight() - 1);
                int land = elevationMask.getRaster().getSample(mx, my, 0);
                double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, targetYear);
                if (land == 0 || (lat < -60.0 && targetYear < 1900L) || (occWeight <= 0.001 && targetYear < 1900L)) {
                    img.setRGB(x, y, 0x000000);
                } else if (img.getRGB(x, y) == 0xFF000000 || img.getRGB(x, y) == 0x000000) {
                    int orographicColor = (orographicBg != null) ? orographicBg.getRGB(x, y) : fallbackHex;
                    img.setRGB(x, y, orographicColor);
                }
            }
        }
    }
}
