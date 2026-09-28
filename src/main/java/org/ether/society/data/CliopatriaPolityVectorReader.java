/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-Precision Streaming Ingestor for Seshat / ClioPatria Historical Vector Polities (158 MB GeoJSON).
 * Extracts authentic historical border polygons for any year and rasterizes them directly onto
 * the 2048x1024 cartographic tensor grids.
 */
public class CliopatriaPolityVectorReader {
    private static final Logger logger = LoggerFactory.getLogger(CliopatriaPolityVectorReader.class);

    public static final String SESHAT_GEOJSON_PATH = "data/maps/seshat/cliopatria_polities_only.geojson";

    public static class HistoricalPolityFeature {
        public String name;
        public int fromYear;
        public int toYear;
        public String seshatId;
        public String wikipedia;
        public List<Path2D> paths = new ArrayList<>();
        public Color color;
    }

    /**
     * Loads and rasterizes Seshat ClioPatria historical polity polygons for the specified year.
     */
    public static BufferedImage rasterizeSeshatSovereigntyMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) {
            logger.warn("Seshat ClioPatria GeoJSON file not found at: {}", file.getAbsolutePath());
            return null;
        }

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) {
            logger.info("No Seshat vector polities matched year {}. Falling back.", targetYear);
            return null;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Sort polities by area descending so smaller autonomous vassals/provinces render on top of macro-empires
        for (HistoricalPolityFeature polity : polities) {
            Color c = polity.color != null ? polity.color : getPolityColor(polity.name, polity.seshatId);
            g.setColor(c);
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }
        g.dispose();

        // Apply elevation coastline mask and assign authentic regional tribal domains to stateless/unclaimed land
        if (elevationMask != null) {
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                for (int x = 0; x < width; x++) {
                    double lon = -180.0 + (x + 0.5) / width * 360.0;
                    int mx = Math.clamp(x * elevationMask.getWidth() / width, 0, elevationMask.getWidth() - 1);
                    int my = Math.clamp(y * elevationMask.getHeight() / height, 0, elevationMask.getHeight() - 1);
                    int land = elevationMask.getRaster().getSample(mx, my, 0);
                    if (land == 0 || (lat < -60.0 && targetYear < 1900L)) {
                        // Ocean or uninhabited prehistoric Antarctic ice sheet
                        img.setRGB(x, y, 0x000000);
                    } else if (img.getRGB(x, y) == 0xFF000000 || img.getRGB(x, y) == 0x000000) {
                        double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, targetYear);
                        if (occWeight <= 0.001 && targetYear < 1900L) {
                            img.setRGB(x, y, 0x000000);
                        } else {
                            // Unclaimed / stateless inhabited frontier -> assign authentic regional tribal basin color
                            int tribalColor = getTribalDomainColor(lon, lat, targetYear);
                            img.setRGB(x, y, tribalColor);
                        }
                    }
                }
            }
        }

        logger.info("Successfully rasterized {} Seshat ClioPatria authentic vector polities for year {} ({}x{}).",
                polities.size(), targetYear, width, height);
        return img;
    }

    /**
     * Determines the authentic regional tribal / clan domain color for stateless inhabited lands.
     */
    public static int getTribalDomainColor(double lon, double lat, long year) {
        if (lat < -60.0) {
            return 0x0284C7; // Antarctic Treaty / International Scientific Research Domain (#0284C7 Sky Blue)
        }

        record TribalDomain(double lon, double lat, int color) {}
        TribalDomain[] domains = {
            // Americas
            new TribalDomain(-60.0, -3.0, 0x2E7D32),    // Amazonian Indigenous Nations (#2E7D32 Forest Green)
            new TribalDomain(-62.0, -25.0, 0x5D4037),   // Gran Chaco & Pampas (#5D4037 Earth Brown)
            new TribalDomain(-68.0, 6.0, 0x33691E),     // Orinoco & Caribbean Tribes (#33691E Olive Green)
            new TribalDomain(-82.0, 38.0, 0x388E3C),    // North American Eastern Woodlands (#388E3C Woodland Green)
            new TribalDomain(-100.0, 42.0, 0xC27803),   // North American Great Plains (#C27803 Prairie Amber)
            new TribalDomain(-122.0, 48.0, 0x00796B),   // Pacific Northwest & Salish (#00796B Coastal Teal)
            new TribalDomain(-110.0, 32.0, 0xB45309),   // Southwest / Aridoamerica (#B45309 Desert Bronze)
            new TribalDomain(-105.0, 62.0, 0x455A64),   // Subarctic Dene & Athabaskan (#455A64 Taiga Slate)
            new TribalDomain(-70.0, 70.0, 0x607D8B),    // Arctic Inuit / Thule Domain (#607D8B Arctic Slate)

            // Africa
            new TribalDomain(0.0, 10.0, 0xB45309),      // West African Savanna / Voltaic (#B45309 Savanna Ochre)
            new TribalDomain(22.0, -1.0, 0x1B5E20),     // Congo Equatorial Forest Clans (#1B5E20 Jungle Dark Green)
            new TribalDomain(38.0, 4.0, 0xC2410C),      // East African Pastoralists (#C2410C Terracotta)
            new TribalDomain(24.0, -28.0, 0xA16207),    // Southern African Khoisan / San (#A16207 Kalahari Ochre)
            new TribalDomain(47.0, -19.0, 0x4E342E),    // Malagasy Indigenous Domain (#4E342E Malagasy Brown)

            // Eurasia & Siberia
            new TribalDomain(10.0, 56.0, 0x4D7C0F),     // Northern European Germanic/Celtic Forest Clans (#4D7C0F Moss Green)
            new TribalDomain(30.0, 64.0, 0x0F766E),     // Boreal Finno-Ugric & Samoyed (#0F766E Pine Teal)
            new TribalDomain(65.0, 48.0, 0x9A3412),     // Eurasian Steppe Nomads (#9A3412 Steppe Rust)
            new TribalDomain(100.0, 58.0, 0x37474F),    // Siberian Taiga & Evenki (#37474F Siberian Charcoal)
            new TribalDomain(150.0, 65.0, 0x546E7A),    // Chukchi & Koryak Paleo-Siberian (#546E7A Tundra Blue-Gray)
            new TribalDomain(100.0, 20.0, 0x4B6B40),    // Southeast Asian Zomia Highlands (#4B6B40 Highland Olive)

            // Oceania & Australasia
            new TribalDomain(134.0, -25.0, 0xA04000),   // Australian Aboriginal Nations (#A04000 Red Ochre)
            new TribalDomain(140.0, -5.0, 0x15803D),    // Papuan Highland Clans (#15803D Papuan Emerald)
            new TribalDomain(170.0, -15.0, 0x0284C7)    // Polynesian Oceanic Domain (#0284C7 Pacific Cyan)
        };

        double minDistSq = Double.MAX_VALUE;
        int bestColor = 0x4B5563;
        for (TribalDomain td : domains) {
            double dLat = lat - td.lat;
            double dLon = (lon - td.lon) * Math.cos(Math.toRadians((lat + td.lat) * 0.5));
            double d2 = dLat * dLat + dLon * dLon;
            if (d2 < minDistSq) {
                minDistSq = d2;
                bestColor = td.color;
            }
        }
        return bestColor;
    }

    /**
     * Loads and rasterizes high-precision authentic Isogloss (Linguistic Phyla) map for the specified year.
     */
    public static BufferedImage rasterizeSeshatIsoglossMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) return null;

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // 1. Vector fill from authentic Seshat polities with linguistic color mapping
        for (HistoricalPolityFeature polity : polities) {
            Color c = getPolityIsoglossColor(polity.name, polity.seshatId);
            g.setColor(c);
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }
        g.dispose();

        // 2. High-precision sub-regional and frontier linguistic centers (lon, lat, hexColor, sigma)
        double[][] linguisticCenters = {
            // --- Western & Central Europe: Romance Sub-branches ---
            {2.35, 48.86, 0xEC4899, 4.5},    // French / Langue d'Oïl (Paris / Île-de-France) (#EC4899)
            {3.06, 50.63, 0xEC4899, 3.0},    // Picard / Northern French (Lille) (#EC4899)
            {-0.58, 44.84, 0xF43F5E, 4.0},   // Occitan / Gascon (Aquitaine / Bordeaux) (#F43F5E)
            {1.44, 43.60, 0xF43F5E, 4.0},    // Occitan / Languedoc (Toulouse) (#F43F5E)
            {5.37, 43.30, 0xF43F5E, 3.5},    // Provençal (Marseille / Nice) (#F43F5E)
            {4.83, 45.76, 0xFB7185, 3.0},    // Franco-Provençal / Arpitan (Lyon) (#FB7185)
            {-2.75, 48.10, 0x10B981, 2.5},   // Breton / Celtic (Brittany) (#10B981 Celtic Emerald)
            {-3.70, 40.42, 0xE11D48, 4.5},   // Castilian Spanish (Madrid / Castile) (#E11D48)
            {-5.99, 37.38, 0xE11D48, 4.0},   // Andalusian Spanish (Seville) (#E11D48)
            {2.17, 41.38, 0xBE185D, 3.2},    // Catalan (Barcelona / Catalonia) (#BE185D)
            {-0.38, 39.47, 0xBE185D, 3.0},   // Valencian / Balearic Catalan (#BE185D)
            {-2.93, 43.26, 0xF59E0B, 2.0},   // Basque / Euskara (Isolate) (#F59E0B Amber)
            {-9.14, 38.72, 0x9D174D, 3.5},   // Portuguese (Lisbon / Southern Portugal) (#9D174D)
            {-8.61, 41.15, 0x9D174D, 3.5},   // Galician / Northern Portuguese (Porto / Santiago) (#9D174D)
            {9.19, 45.46, 0xFB7185, 3.5},    // Gallo-Italic (Lombardy / Piedmont / Milan) (#FB7185)
            {12.33, 45.44, 0xFB7185, 3.0},   // Venetian (Venice) (#FB7185)
            {11.25, 43.77, 0xE879F9, 3.0},   // Tuscan Italian (Florence) (#E879F9)
            {12.49, 41.90, 0xE879F9, 3.5},   // Roman / Central Italian (Rome) (#E879F9)
            {14.26, 40.85, 0xC084FC, 3.5},   // Neapolitan / Southern Italian (Naples) (#C084FC)
            {14.01, 37.59, 0xC084FC, 3.0},   // Sicilian (Palermo) (#C084FC)
            {26.10, 44.43, 0xD946EF, 4.0},   // Romanian (Wallachia / Moldavia / Bucharest) (#D946EF)

            // --- Germanic Phyla ---
            {-0.13, 51.51, 0x2563EB, 4.0},   // English (London / Southern England) (#2563EB Anglic Blue)
            {-1.54, 53.80, 0x2563EB, 3.5},   // English (Midlands / Northern England) (#2563EB)
            {-3.18, 55.95, 0x2563EB, 3.0},   // Lowland Scots / English (#2563EB)
            {-4.25, 57.47, 0x059669, 2.8},   // Scottish Gaelic / Celtic (#059669 Goidelic Emerald)
            {-7.50, 53.40, 0x059669, 3.0},   // Irish Gaelic / Celtic (#059669)
            {-3.90, 52.41, 0x10B981, 2.2},   // Welsh / Celtic (#10B981 Brythonic)
            {4.90, 52.37, 0x60A5FA, 3.0},    // Dutch / Netherlandic (Amsterdam) (#60A5FA Low German/Dutch)
            {3.72, 51.05, 0x60A5FA, 2.5},    // Flemish (Ghent / Antwerp) (#60A5FA)
            {9.99, 53.55, 0x60A5FA, 3.5},    // Low German / Plattdüütsch (Hamburg / Bremen) (#60A5FA)
            {13.40, 52.52, 0x3B82F6, 4.5},   // High German (Prussia / Brandenburg / Berlin) (#3B82F6 High German)
            {6.96, 50.94, 0x3B82F6, 3.0},    // Rhenish German (Cologne / Rhineland) (#3B82F6)
            {9.18, 48.78, 0x3B82F6, 3.0},    // Swabian / Alemannic (Stuttgart) (#3B82F6)
            {11.58, 48.14, 0x3B82F6, 3.5},   // Bavarian German (Munich) (#3B82F6)
            {16.37, 48.21, 0x3B82F6, 3.5},   // Austrian German (Vienna) (#3B82F6)
            {8.54, 47.37, 0x3B82F6, 2.5},    // Swiss German (Zurich / Bern) (#3B82F6)
            {12.56, 55.67, 0x93C5FD, 3.0},   // Danish (Copenhagen) (#93C5FD Scandinavian)
            {18.06, 59.33, 0x93C5FD, 4.5},   // Swedish (Stockholm) (#93C5FD)
            {10.75, 59.91, 0x93C5FD, 4.0},   // Norwegian (Oslo) (#93C5FD)
            {-21.94, 64.14, 0x93C5FD, 3.5},  // Icelandic (#93C5FD)

            // --- Uralic Phyla ---
            {19.04, 47.50, 0x14B8A6, 4.0},   // Hungarian / Magyar (Pannonian Basin) (#14B8A6 Uralic Teal)
            {24.94, 60.17, 0x0D9488, 4.5},   // Finnish (Helsinki / Tavastia) (#0D9488 Finnic)
            {24.75, 59.43, 0x0D9488, 2.5},   // Estonian (Tallinn) (#0D9488)
            {25.00, 68.00, 0x0F766E, 4.0},   // Saami (Lapland) (#0F766E Saami)
            {52.00, 56.00, 0x14B8A6, 3.5},   // Udmurt / Mari (Volga-Uralic) (#14B8A6)
            {65.00, 62.00, 0x14B8A6, 6.0},   // Khanty / Mansi (Ob-Ugric) (#14B8A6)
            {75.00, 68.00, 0x14B8A6, 5.0},   // Nenets / Samoyedic (#14B8A6)

            // --- Eastern Europe & Slavic Phyla ---
            {21.01, 52.23, 0xA855F7, 4.0},   // Polish (Warsaw / Vistula) (#A855F7 West Slavic)
            {14.42, 50.08, 0xA855F7, 2.8},   // Czech (Bohemia / Prague) (#A855F7)
            {17.10, 48.14, 0xA855F7, 2.5},   // Slovak (Bratislava) (#A855F7)
            {30.52, 50.45, 0x8B5CF6, 5.0},   // Ukrainian (Kyiv / Dnieper) (#8B5CF6 East Slavic)
            {27.56, 53.90, 0x8B5CF6, 4.0},   // Belarusian (Minsk) (#8B5CF6)
            {37.62, 55.75, 0x8B5CF6, 7.0},   // Russian (Moscow / Central Russia) (#8B5CF6)
            {30.32, 59.93, 0x8B5CF6, 5.0},   // Northern Russian (Saint Petersburg / Novgorod) (#8B5CF6)
            {25.27, 54.68, 0x6366F1, 2.8},   // Lithuanian (Baltic) (#6366F1 Indigo)
            {24.10, 56.95, 0x6366F1, 2.5},   // Latvian (Baltic) (#6366F1 Indigo)
            {20.45, 44.81, 0x7C3AED, 3.5},   // Serbian / Croatian (Belgrade / Zagreb) (#7C3AED South Slavic)
            {23.32, 42.69, 0x7C3AED, 3.0},   // Bulgarian (Sofia) (#7C3AED)
            {14.50, 46.05, 0x7C3AED, 2.2},   // Slovenian (Ljubljana) (#7C3AED)
            {19.81, 41.32, 0x0284C7, 2.2},   // Albanian (Tirana) (#0284C7 Sky Blue)
            {23.72, 37.98, 0x06B6D4, 3.5},   // Greek (Athens / Aegean / Peloponnese) (#06B6D4 Cyan)
            {33.00, 35.00, 0x06B6D4, 2.0},   // Cypriot Greek (#06B6D4)

            // --- Middle East & North Africa ---
            {32.85, 39.93, 0x0EA5E9, 5.0},   // Turkish (Anatolia / Ottoman Core) (#0EA5E9 Oghuz Turkic)
            {49.86, 40.40, 0x0EA5E9, 3.0},   // Azerbaijani (Turkic) (#0EA5E9)
            {60.00, 39.00, 0x0EA5E9, 4.0},   // Turkmen (Merv / Karakum) (#0EA5E9)
            {44.80, 41.71, 0x84CC16, 2.5},   // Georgian / Kartvelian (#84CC16 Lime)
            {45.00, 43.00, 0x84CC16, 2.5},   // North Caucasian (Circassian, Chechen, Avar) (#84CC16)
            {44.50, 40.18, 0x38BDF8, 2.2},   // Armenian (#38BDF8)
            {44.00, 36.00, 0xD97706, 3.5},   // Kurdish (Iranic) (#D97706 Orange-Brown)
            {36.29, 33.51, 0x15803D, 4.0},   // Levantine Arabic (Damascus / Aleppo / Beirut) (#15803D Semitic Forest Green)
            {31.24, 30.04, 0x15803D, 5.0},   // Egyptian Arabic (Cairo / Nile Delta) (#15803D)
            {44.36, 33.31, 0x15803D, 4.5},   // Mesopotamian Arabic (Baghdad / Basra) (#15803D)
            {46.71, 24.63, 0x15803D, 6.0},   // Peninsular Arabic (Nejd / Hejaz / Gulf) (#15803D)
            {44.20, 15.35, 0x15803D, 3.5},   // Yemeni Arabic (#15803D)
            {58.59, 23.61, 0x15803D, 3.5},   // Omani Arabic (#15803D)
            {10.18, 36.80, 0x15803D, 3.5},   // Tunisian Arabic (#15803D)
            {3.05, 36.75, 0x15803D, 4.0},    // Algerian Arabic (#15803D)
            {4.00, 36.50, 0x047857, 2.5},    // Kabyle Berber (#047857 Sea Emerald)
            {-5.00, 34.00, 0x15803D, 4.0},   // Moroccan Arabic (Fez / Casablanca) (#15803D)
            {-7.00, 31.00, 0x047857, 3.5},   // Shilha / Tamazight Berber (Atlas) (#047857)
            {-1.00, 24.00, 0x047857, 6.0},   // Tuareg Berber (Central Sahara) (#047857)
            {51.39, 35.69, 0xD97706, 6.0},   // Persian / Farsi (Tehran / Isfahan / Shiraz) (#D97706 Iranic)
            {69.17, 34.53, 0xD97706, 4.0},   // Pashto / Dari (Kabul / Kandahar) (#D97706)
            {64.00, 28.00, 0xD97706, 4.0},   // Balochi (Iranic) (#D97706)
            {68.78, 38.56, 0xD97706, 2.5},   // Tajik (Iranic) (#D97706)

            // --- Central Asia & Siberia ---
            {69.00, 41.00, 0x38BDF8, 5.0},   // Uzbek (Tashkent / Samarkand / Bukhara) (#38BDF8 Kipchak/Karluk Turkic)
            {70.00, 48.00, 0x38BDF8, 8.0},   // Kazakh (Eurasian Steppe) (#38BDF8)
            {74.50, 42.87, 0x38BDF8, 3.5},   // Kyrgyz (Tian Shan) (#38BDF8)
            {85.00, 42.00, 0x38BDF8, 5.0},   // Uyghur (Xinjiang / Tarim Basin) (#38BDF8)
            {49.12, 55.79, 0x38BDF8, 3.5},   // Tatar (Kazan / Volga) (#38BDF8)
            {56.00, 54.00, 0x38BDF8, 3.5},   // Bashkir (#38BDF8)
            {129.7, 62.03, 0x38BDF8, 6.0},   // Yakut / Sakha (Turkic Siberia) (#38BDF8)
            {106.0, 47.00, 0xF97316, 7.0},   // Mongolian / Khalkha (Mongolia) (#F97316 Mongolic Orange)
            {107.5, 52.00, 0xF97316, 4.0},   // Buryat (Lake Baikal) (#F97316)
            {46.00, 46.50, 0xF97316, 3.0},   // Kalmyk (Caspian Steppe) (#F97316)
            {95.00, 60.00, 0x7C3AED, 8.0},   // Evenki / Tungusic (Central Siberia) (#7C3AED Deep Violet)
            {126.0, 47.00, 0x7C3AED, 5.0},   // Manchu / Tungusic (Manchuria) (#7C3AED)
            {160.0, 65.00, 0x607D8B, 6.0},   // Chukchi / Koryak (Paleo-Siberian) (#607D8B Slate)

            // --- East & Southeast Asia ---
            {116.4, 39.90, 0xEF4444, 6.0},   // Mandarin Chinese (Northern China Plain / Beijing) (#EF4444 Sinitic Red)
            {114.0, 34.00, 0xEF4444, 5.5},   // Zhongyuan Mandarin (Central Plains) (#EF4444)
            {104.0, 30.60, 0xEF4444, 5.0},   // Southwestern Mandarin (Sichuan Basin) (#EF4444)
            {120.5, 31.00, 0xF87171, 4.0},   // Wu Chinese (Jiangnan / Yangtze Delta / Shanghai) (#F87171)
            {119.3, 26.08, 0xB91C1C, 3.5},   // Min Chinese (Fujian / Fuzhou) (#B91C1C)
            {113.2, 23.13, 0xDC2626, 4.0},   // Yue / Cantonese (Guangdong / Pearl River) (#DC2626)
            {115.8, 28.68, 0xEA580C, 3.5},   // Hakka / Gan Chinese (Jiangxi) (#EA580C)
            {112.9, 28.20, 0xEA580C, 3.5},   // Xiang Chinese (Hunan) (#EA580C)
            {88.00, 31.00, 0xDC2626, 6.0},   // Standard Tibetan (Lhasa / Ü-Tsang) (#DC2626 Tibeto-Burman)
            {100.0, 33.00, 0xDC2626, 4.5},   // Amdo / Kham Tibetan (#DC2626)
            {96.16, 16.86, 0xDC2626, 4.5},   // Burmese (Konbaung Burma) (#DC2626)
            {126.9, 37.57, 0xA855F7, 4.0},   // Korean (Joseon Dynasty / Seoul) (#A855F7 Violet)
            {139.6, 35.69, 0xF43F5E, 5.0},   // Japanese (Honshu / Edo / Kyoto) (#F43F5E Rose)
            {130.4, 33.60, 0xF43F5E, 3.5},   // Kyushu Japanese (#F43F5E)
            {127.7, 26.21, 0xF43F5E, 2.0},   // Ryukyuan (#F43F5E)
            {142.0, 43.00, 0x607D8B, 3.0},   // Ainu (Hokkaido / Sakhalin / Kurils) (#607D8B)
            {105.8, 21.03, 0x16A085, 4.0},   // Vietnamese (Red River Delta / Hanoi) (#16A085 Austroasiatic)
            {108.0, 16.00, 0x16A085, 3.5},   // Central Vietnamese / Hue (#16A085)
            {104.9, 11.55, 0x16A085, 3.5},   // Khmer / Cambodian (Phnom Penh) (#16A085)
            {100.5, 13.75, 0xEAB308, 4.5},   // Central Thai / Siamese (Bangkok) (#EAB308 Tai-Kadai Gold)
            {102.6, 17.97, 0xEAB308, 3.5},   // Lao / Isan (Vientiane) (#EAB308)
            {99.00, 19.00, 0xEAB308, 3.0},   // Shan / Northern Thai (#EAB308)

            // --- Indian Subcontinent ---
            {77.21, 28.61, 0xF59E0B, 5.0},   // Hindi / Hindustani / Urdu (Delhi / Doab) (#F59E0B Indo-Aryan Amber)
            {75.00, 31.00, 0xFCD34D, 4.0},   // Punjabi (Sikh Empire / Lahore) (#FCD34D)
            {88.36, 22.57, 0xFBBF24, 5.0},   // Bengali (Bengal / Calcutta) (#FBBF24)
            {73.86, 18.52, 0xD97706, 4.5},   // Marathi (Maratha Confederacy / Deccan) (#D97706)
            {72.57, 23.02, 0xD97706, 4.0},   // Gujarati (Ahmedabad) (#D97706)
            {85.82, 20.29, 0xF59E0B, 3.5},   // Odia / Oriya (#F59E0B)
            {68.50, 26.00, 0xFCD34D, 3.5},   // Sindhi (#FCD34D)
            {85.32, 27.71, 0xF59E0B, 3.0},   // Nepali (#F59E0B)
            {80.50, 7.50, 0xF59E0B, 2.5},    // Sinhala (Ceylon) (#F59E0B)
            {80.27, 13.08, 0xB45309, 4.5},   // Tamil (Carnatic / Coromandel / Madras) (#B45309 Dravidian Bronze)
            {78.48, 17.38, 0x92400E, 4.5},   // Telugu (Andhra / Hyderabad) (#92400E)
            {76.50, 14.00, 0x92400E, 4.0},   // Kannada (Mysore) (#92400E)
            {76.27, 9.93, 0xB45309, 3.0},    // Malayalam (Travancore / Malabar) (#B45309)

            // --- Maritime Southeast Asia & Oceania ---
            {106.8, -6.21, 0x06B6D4, 4.5},   // Javanese / Sundanese (Java) (#06B6D4 Austronesian Cyan)
            {101.7, 3.14, 0x06B6D4, 4.0},    // Malay (Malacca / Johor / Sumatra) (#06B6D4)
            {115.0, 0.00, 0x06B6D4, 5.0},    // Dayak / Bornean (#06B6D4)
            {120.0, -4.00, 0x06B6D4, 4.0},   // Bugis / Makassarese (Sulawesi) (#06B6D4)
            {121.0, 14.60, 0x06B6D4, 4.5},   // Tagalog / Ilocano (Luzon Philippines) (#06B6D4)
            {123.0, 10.00, 0x06B6D4, 4.0},   // Visayan (Cebu / Panay) (#06B6D4)
            {47.00, -19.0, 0x06B6D4, 4.0},   // Malagasy (Madagascar) (#06B6D4)
            {140.0, -4.50, 0x9C27B0, 5.0},   // Trans-New Guinea / Papuan (#9C27B0 Purple)
            {133.5, -24.0, 0xE91E63, 8.0},   // Pama-Nyungan Aboriginal (Central/Southern Australia) (#E91E63 Hot Pink)
            {132.0, -13.0, 0xE91E63, 4.0},   // Arnhem Land Non-Pama-Nyungan (#E91E63)
            {151.2, -33.8, 0xE91E63, 4.0},   // Sydney / Coastal Aboriginal (#E91E63)
            {175.0, -39.0, 0x06B6D4, 4.0},   // Maori (New Zealand) (#06B6D4)
            {-157.8, 21.3, 0x06B6D4, 3.0},   // Hawaiian (Polynesia) (#06B6D4)
            {-172.0, -14.0, 0x06B6D4, 3.0},  // Samoan (#06B6D4)
            {-149.5, -17.5, 0x06B6D4, 3.0},  // Tahitian (#06B6D4)

            // --- Sub-Saharan Africa ---
            {5.23, 13.06, 0x22C55E, 4.0},    // Hausa (Sokoto / Northern Nigeria) (#22C55E Niger-Congo Green)
            {3.50, 7.00, 0x22C55E, 3.5},     // Yoruba (Oyo / Gulf of Guinea) (#22C55E)
            {7.00, 5.50, 0x22C55E, 3.0},     // Igbo (#22C55E)
            {-1.50, 6.50, 0x22C55E, 3.0},    // Akan / Ashanti (Ghana) (#22C55E)
            {-8.00, 12.0, 0x22C55E, 4.5},    // Manding / Bambara (Mali / Guinea) (#22C55E)
            {-16.0, 14.5, 0x22C55E, 3.5},    // Wolof (Senegal) (#22C55E)
            {12.00, 12.0, 0x22C55E, 4.0},    // Fulani / Fula (#22C55E)
            {14.00, 13.0, 0xC0392B, 3.5},    // Kanuri (Bornu) (#C0392B Nilo-Saharan Brick Red)
            {25.00, 13.0, 0xC0392B, 4.0},    // Fur (Darfur) (#C0392B)
            {32.00, 10.0, 0xC0392B, 4.0},    // Dinka / Nuer / Nilotic (South Sudan) (#C0392B)
            {38.74, 9.03, 0x166534, 3.5},    // Amharic (Ethiopian Highlands) (#166534 Semitic Green)
            {39.00, 7.00, 0x84CC16, 4.0},    // Oromo (Cushitic) (#84CC16 Lime)
            {45.00, 5.00, 0x84CC16, 5.0},    // Somali (Horn of Africa) (#84CC16)
            {15.30, -4.3, 0x22C55E, 5.0},    // Kongo / Lingala (Congo Basin) (#22C55E Bantu)
            {36.82, -1.2, 0x22C55E, 4.0},    // Kikuyu / Swahili (East Africa) (#22C55E)
            {31.00, -29.0, 0x22C55E, 4.0},   // Zulu / Xhosa (Southern Africa) (#22C55E)
            {26.00, -26.0, 0x22C55E, 4.0},   // Sotho / Tswana (#22C55E)
            {21.00, -25.0, 0xFACC15, 4.5},   // Khoisan / Ju|'hoan / Nama (Kalahari / Namib) (#FACC15 Yellow)

            // --- Americas ---
            {-75.0, 40.0, 0x2563EB, 4.5},    // English (American Atlantic Seaboard / Philadelphia) (#2563EB Anglic)
            {-71.0, 46.8, 0xEC4899, 3.5},    // French Canadian (Quebec / Saint Lawrence) (#EC4899 Romance)
            {-90.0, 30.0, 0xEC4899, 3.0},    // Louisiana French Creole (#EC4899)
            {-76.0, 43.0, 0x3F51B5, 3.0},    // Iroquoian (Haudenosaunee) (#3F51B5 Indigo)
            {-85.0, 46.0, 0x3F51B5, 4.5},    // Algonquian (Ojibwe / Cree) (#3F51B5)
            {-100.0, 45.0, 0x3F51B5, 5.0},   // Siouan (Lakota / Dakota) (#3F51B5)
            {-102.0, 35.0, 0xFF9800, 4.5},   // Comanche / Shoshone (Uto-Aztecan) (#FF9800 Orange)
            {-110.0, 35.0, 0x009688, 3.5},   // Navajo / Apache (Southern Athabaskan) (#009688 Dark Teal)
            {-120.0, 58.0, 0x009688, 6.0},   // Northern Athabaskan / Dene (Canada Subarctic) (#009688)
            {-135.0, 57.0, 0x009688, 3.5},   // Tlingit / Haida (Pacific Northwest Coast) (#009688)
            {-90.0, 68.0, 0x607D8B, 7.0},    // Inuit (Inuktitut / Arctic Nunavut & Greenland) (#607D8B Slate)
            {-160.0, 64.0, 0x607D8B, 5.0},   // Yupik / Alaskan Inuit (#607D8B)
            {-99.13, 19.43, 0xFF9800, 4.0},  // Nahuatl / Mesoamerican (Valley of Mexico) (#FF9800)
            {-89.62, 17.22, 0xFF9800, 4.0},  // Mayan (Yucatan / Guatemala) (#FF9800)
            {-77.04, -12.0, 0x795548, 5.0},  // Quechua (Andean Peru / Ecuador / Bolivia) (#795548 Terracotta)
            {-68.15, -16.5, 0x795548, 3.5},  // Aymara (Altiplano) (#795548)
            {-55.0, -15.0, 0x4CAF50, 6.0},   // Tupi-Guarani (Paraguay / Amazonia) (#4CAF50 Leaf Green)
            {-62.0, -3.0, 0x4CAF50, 6.0},    // Arawakan / Cariban (Amazon Basin) (#4CAF50)
            {-72.0, -38.0, 0x8D6E63, 4.0}    // Mapuche (Araucanian / Chile & Patagonia) (#8D6E63 Earth Brown)
        };

        // 3. Pixel refinement with elevation mask & sub-regional overlay
        if (elevationMask != null) {
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                for (int x = 0; x < width; x++) {
                    double lon = -180.0 + (x + 0.5) / width * 360.0;
                    int mx = Math.clamp(x * elevationMask.getWidth() / width, 0, elevationMask.getWidth() - 1);
                    int my = Math.clamp(y * elevationMask.getHeight() / height, 0, elevationMask.getHeight() - 1);
                    int land = elevationMask.getRaster().getSample(mx, my, 0);
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, targetYear);
                    if (land == 0 || lat < -60.0 || occWeight <= 0.001) {
                        img.setRGB(x, y, 0x000000);
                        continue;
                    }

                    int currentRgb = img.getRGB(x, y) & 0xFFFFFF;
                    boolean isUnassigned = (currentRgb == 0x000000);

                    // Refine all land cells using distance-weighted localized linguistic centers with stochastic contact-zone dithering
                    double maxInfl = 0.0;
                    double secondInfl = 0.0;
                    int bestCol = isUnassigned ? 0x2563EB : currentRgb;
                    int secondCol = bestCol;

                    for (double[] lc : linguisticCenters) {
                        double dLat = lat - lc[1];
                        double dLon = (lon - lc[0]) * Math.cos(Math.toRadians((lat + lc[1]) * 0.5));
                        double d2 = dLat * dLat + dLon * dLon;
                        double sigma = lc[3];
                        double infl = Math.exp(-d2 / (2.0 * sigma * sigma));
                        int col = (int) lc[2];
                        if (infl > maxInfl) {
                            if (col != bestCol) {
                                secondInfl = maxInfl;
                                secondCol = bestCol;
                            }
                            maxInfl = infl;
                            bestCol = col;
                        } else if (infl > secondInfl && col != bestCol) {
                            secondInfl = infl;
                            secondCol = col;
                        }
                    }

                    int finalCol = bestCol;
                    if (secondCol != bestCol && maxInfl > 0.0) {
                        double ratio = secondInfl / maxInfl; // 0.0 to 1.0
                        if (ratio > 0.40) {
                            // Contact zone: calculate probability of secondary language pixel (0.0 to 0.45)
                            double pSecond = (ratio - 0.40) / (1.0 - 0.40) * 0.45;
                            // Deterministic high-entropy spatial hash
                            int hash = (x * 0x1F1F1F1F) ^ (y * 0x3D3D3D3D) ^ (int) (targetYear * 1013904223L);
                            double rndVal = ((hash & 0x7FFFFFFF) % 10000) / 10000.0;
                            if (rndVal < pSecond) {
                                finalCol = secondCol;
                            }
                        }
                    }

                    if (isUnassigned || maxInfl >= 0.15) {
                        img.setRGB(x, y, finalCol);
                    }
                }
            }
        }

        logger.info("Successfully rasterized authentic high-precision Isogloss tensor map for year {} ({}x{}).", targetYear, width, height);
        return img;
    }

    /**
     * Loads and rasterizes high-precision authentic Kinship & Social Organization map for the specified year.
     */
    public static BufferedImage rasterizeSeshatKinshipMap(long targetYear, int width, int height, BufferedImage elevationMask) {
        File file = new File(SESHAT_GEOJSON_PATH);
        if (!file.exists()) return null;

        List<HistoricalPolityFeature> polities = loadPolitiesForYear(file, (int) targetYear, width, height);
        if (polities.isEmpty()) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // 1. Vector fill from authentic Seshat polities with kinship color mapping
        for (HistoricalPolityFeature polity : polities) {
            Color c = getPolityKinshipColor(polity.name, polity.seshatId);
            g.setColor(c);
            for (Path2D path : polity.paths) {
                g.fill(path);
            }
        }
        g.dispose();

        // 2. High-precision sub-regional kinship centers (lon, lat, hexColor, sigma)
        double[][] kinshipCenters = {
            // --- Western Europe: Todd Typology ---
            {2.35, 48.86, 0xEC4899, 4.5},    // France - Paris / Northern France (Egalitarian Nuclear & Civil Code) (#EC4899)
            {3.06, 50.63, 0xEC4899, 3.0},    // France - Lille / Picardy (Egalitarian Nuclear) (#EC4899)
            {4.35, 50.85, 0xEC4899, 3.0},    // Belgium / Wallonia (Egalitarian Nuclear) (#EC4899)
            {3.72, 51.05, 0x3B82F6, 2.5},    // Belgium / Flanders (Absolute Nuclear) (#3B82F6)
            {4.90, 52.37, 0x3B82F6, 3.0},    // Netherlands (Commercial Absolute Nuclear) (#3B82F6)
            {-0.13, 51.51, 0x3B82F6, 4.0},   // Great Britain - England (Absolute Nuclear & Wage-Labor Proletariat) (#3B82F6)
            {-1.54, 53.80, 0x3B82F6, 3.5},   // Great Britain - Industrial Midlands / North (#3B82F6)
            {-3.18, 55.95, 0x3B82F6, 3.0},   // Scotland - Lowlands (Absolute Nuclear) (#3B82F6)
            {-4.25, 57.47, 0x8B5CF6, 2.8},   // Scottish Highlands (Stem Family / Clan) (#8B5CF6)
            {-7.50, 53.40, 0x8B5CF6, 3.0},   // Ireland (Stem Family / Impartible Inheritance) (#8B5CF6)
            {-0.58, 44.84, 0xA855F7, 4.0},   // France - Aquitaine / Gascony (Incomplete / Flexible Stem Family) (#A855F7)
            {1.44, 43.60, 0xA855F7, 4.0},    // France - Occitanie / Languedoc (Flexible Stem Family) (#A855F7)
            {5.37, 43.30, 0xA855F7, 3.5},    // France - Provence / French Midi (Flexible Stem Family) (#A855F7)
            {-2.75, 48.10, 0x8B5CF6, 2.5},   // France - Brittany (Stem Family / Famille Souche) (#8B5CF6)
            {9.19, 45.46, 0xEC4899, 4.0},    // Northern Italy - Lombardy / Tuscany (Egalitarian Nuclear) (#EC4899)
            {12.49, 41.90, 0xEC4899, 3.5},   // Central Italy - Rome (Egalitarian Nuclear) (#EC4899)
            {14.26, 40.85, 0xEC4899, 3.5},   // Southern Italy - Naples / Sicily (Egalitarian Nuclear) (#EC4899)
            {23.72, 37.98, 0xEC4899, 3.5},   // Greece - Athens / Peloponnese (Egalitarian Nuclear) (#EC4899)
            {-3.70, 40.42, 0xEC4899, 4.5},   // Spain - Castile / Madrid (Egalitarian Partible Nuclear) (#EC4899)
            {-5.99, 37.38, 0xEC4899, 4.0},   // Spain - Andalusia (Egalitarian Nuclear / Latifundia) (#EC4899)
            {2.17, 41.38, 0x8B5CF6, 3.2},    // Spain - Catalonia (Complete Stem Family / Hereu Primogeniture) (#8B5CF6)
            {-2.93, 43.26, 0x8B5CF6, 2.0},   // Basque Country (Etxea Stem Household) (#8B5CF6)
            {-8.61, 41.15, 0x8B5CF6, 3.5},   // Spain - Galicia (Stem Family) (#8B5CF6)
            {-9.14, 38.72, 0xEC4899, 4.0},   // Portugal (Egalitarian Nuclear) (#EC4899)
            {-75.0, 40.0, 0x3B82F6, 5.0},    // United States - Atlantic Seaboard Homesteads (Absolute Nuclear) (#3B82F6)

            // --- Central Europe, Scandinavia & Japan: Stem Family & Primogeniture (#8B5CF6) ---
            {13.40, 52.52, 0x8B5CF6, 4.5},   // Prussia / Northern Germany (Stammfamilie) (#8B5CF6)
            {9.99, 53.55, 0x8B5CF6, 3.5},    // Lower Saxony / Westphalia (Stammfamilie) (#8B5CF6)
            {11.58, 48.14, 0x8B5CF6, 3.5},   // Bavaria (Stammfamilie) (#8B5CF6)
            {16.37, 48.21, 0x8B5CF6, 4.0},   // Austria (Habsburg Stem Family) (#8B5CF6)
            {8.54, 47.37, 0xA855F7, 2.5},    // Switzerland (Flexible Stem Family) (#A855F7)
            {14.42, 50.08, 0x8B5CF6, 2.8},   // Czech / Bohemia (Central European Stem Family) (#8B5CF6)
            {18.06, 59.33, 0x8B5CF6, 5.0},   // Sweden & Scandinavia (#8B5CF6)
            {12.56, 55.67, 0x8B5CF6, 3.0},   // Denmark (#8B5CF6)
            {10.75, 59.91, 0x8B5CF6, 4.0},   // Norway (#8B5CF6)
            {24.94, 60.17, 0x8B5CF6, 4.5},   // Finland (#8B5CF6)
            {139.6, 35.69, 0x8B5CF6, 5.0},   // Japan Tokugawa (Ie Stem Family System) (#8B5CF6)

            // --- Eastern Europe & Slavic Lands: Patriarchal Peasant Joint Family / Mir / Zadruga (#DC2626) ---
            {37.62, 55.75, 0xDC2626, 7.0},   // Russian Peasant Obshchina (Mir / Communitary Exogamous) (#DC2626)
            {30.52, 50.45, 0xDC2626, 5.0},   // Ukraine (Dnieper Peasant Communes) (#DC2626)
            {21.01, 52.23, 0xDC2626, 4.0},   // Poland (Patriarchal Joint / Extended Family) (#DC2626)
            {17.10, 48.14, 0xDC2626, 2.5},   // Slovakia (#DC2626)
            {19.04, 47.50, 0xDC2626, 4.0},   // Hungary (Pannonian Extended Family) (#DC2626)
            {20.45, 44.81, 0xDC2626, 3.5},   // Balkan Zadruga (Serbia / Croatia / Bosnia) (#DC2626)
            {23.32, 42.69, 0xDC2626, 3.0},   // Bulgaria (#DC2626)
            {26.10, 44.43, 0xDC2626, 4.0},   // Wallachia / Moldavia (#DC2626)

            // --- East Asia: Agnatic Lineage & Confucian Zongzu Clan (#EF4444) ---
            {116.4, 39.90, 0xEF4444, 6.0},   // Han China - Northern Plain (Zongzu Patrilineal Lineages) (#EF4444)
            {114.0, 34.00, 0xEF4444, 5.5},   // Han China - Central Plains (#EF4444)
            {120.5, 31.00, 0xEF4444, 4.0},   // Han China - Jiangnan Lineages (#EF4444)
            {113.2, 23.13, 0xEF4444, 4.5},   // South China Lineage Hall Corporate Estates (Guangdong/Fujian) (#EF4444)
            {126.9, 37.57, 0xEF4444, 4.0},   // Joseon Korea (Confucian Munjung Clan) (#EF4444)
            {105.8, 21.03, 0xEF4444, 4.0},   // Vietnam (Agnatic Gia-Toc Clan) (#EF4444)
            {88.00, 31.00, 0xA855F7, 5.0},   // Tibet (Fraternal Polyandrous / Stem Household) (#A855F7)

            // --- Middle East & North Africa: Segmentary Patrilineal Lineage & Endogamous Clan (#10B981) ---
            {32.85, 39.93, 0x10B981, 5.0},   // Ottoman Anatolia (Patrilineal Sülale Clans) (#10B981)
            {36.29, 33.51, 0x10B981, 4.0},   // Levant / Syrian Clan Lineages (#10B981)
            {31.24, 30.04, 0x10B981, 5.0},   // Egypt / Nile Delta (Hamula Extended Lineages) (#10B981)
            {46.71, 24.63, 0x10B981, 6.0},   // Arabian Bedouin Tribal Lineages (Bint'Amm Endogamy) (#10B981)
            {44.20, 15.35, 0x10B981, 3.5},   // Yemen Tribal Lineages (#10B981)
            {51.39, 35.69, 0x10B981, 6.0},   // Qajar Persia Patrilineal Clans (#10B981)
            {69.17, 34.53, 0x10B981, 4.0},   // Pashtunwali Segmentary Lineages (Afghanistan) (#10B981)
            {-5.00, 34.00, 0x10B981, 4.5},   // Maghreb Berber / Arab Lineages (Qabila / Douar) (#10B981)
            {10.18, 36.80, 0x10B981, 3.5},   // Tunisia (#10B981)
            {3.05, 36.75, 0x10B981, 4.0},    // Algeria (#10B981)

            // --- India: Gotra Exogamous Patrilineal Joint Family & Jati Caste (#F59E0B / #B45309) ---
            {77.21, 28.61, 0xF59E0B, 5.0},   // Northern India Hindu Joint Family (Indo-Aryan Gotra Exogamy) (#F59E0B)
            {75.00, 31.00, 0xF59E0B, 4.0},   // Punjab Joint Family (#F59E0B)
            {88.36, 22.57, 0xF59E0B, 5.0},   // Bengal Hindu Joint Family (#F59E0B)
            {73.86, 18.52, 0xF59E0B, 4.5},   // Deccan Maratha Joint Family (#F59E0B)
            {80.27, 13.08, 0xB45309, 4.5},   // Tamil Dravidian Cross-Cousin Kinship System (#B45309)
            {78.48, 17.38, 0xB45309, 4.5},   // Telugu Dravidian Kinship (#B45309)
            {76.50, 14.00, 0xB45309, 4.0},   // Kannada Dravidian Kinship (#B45309)
            {76.27, 9.93, 0xB45309, 3.0},    // Kerala / Malabar Marumakkathayam Matrilineal Joint Household (#B45309)

            // --- Central Asia & Steppe: Nomadic Pastoral Clan Federations (#D97706) ---
            {70.00, 48.00, 0xD97706, 8.0},   // Kazakh Zhuz Steppe Clans (#D97706)
            {106.0, 47.00, 0xD97706, 7.0},   // Mongol Nomadic Lineages (Otog / Aimag) (#D97706)
            {69.00, 41.00, 0x10B981, 4.5},   // Turkestani Oasis Clans (Mahalla) (#10B981)

            // --- Matrilineal Belt: Matrilineal Clan & Avunculocal Household (#F43F5E) ---
            {100.5, -0.50, 0xF43F5E, 3.5},   // Minangkabau Matrilineal Suku & Rumah Gadang (Sumatra) (#F43F5E)
            {-1.50, 6.50, 0xF43F5E, 3.0},    // Ashanti Matrilineal Abusua Clans (Ghana) (#F43F5E)
            {28.00, -12.0, 0xF43F5E, 5.0},   // Bemba / Central African Matrilineal Belt (#F43F5E)
            {-76.0, 43.0, 0xF43F5E, 3.0},    // Iroquois Matrilineal Longhouse Clans (#F43F5E)
            {-110.5, 35.8, 0xF43F5E, 2.5},   // Hopi / Pueblo Matrilocal Clans (#F43F5E)

            // --- Sub-Saharan Africa: Segmentary Patrilineages & Age-Sets (#15803D / #FF9800) ---
            {5.23, 13.06, 0x15803D, 4.0},    // Hausa / Fulani Lineages (#15803D)
            {3.50, 7.00, 0x15803D, 3.5},     // Yoruba Lineages & Compounds (#15803D)
            {7.00, 5.50, 0x15803D, 3.0},     // Igbo Umunna Patrilineages (#15803D)
            {15.30, -4.3, 0x15803D, 5.0},    // Kongo Lineages (#15803D)
            {31.00, -29.0, 0x15803D, 4.0},   // Zulu / Xhosa Patrilineal Imizi (#15803D)
            {37.00, 1.00, 0xFF9800, 4.0},    // East African Age-Set Organization (Maasai, Oromo Gadaa) (#FF9800)

            // --- Australia & Pacific: 8-Skin Subsection & Polynesian Ramage ---
            {133.5, -24.0, 0xE91E63, 8.0},   // Australian 8-Skin Subsection Totemic System (#E91E63)
            {175.0, -39.0, 0x06B6D4, 4.0},   // Maori Iwi / Hapu Ramage Lineages (#06B6D4)
            {-157.8, 21.3, 0x06B6D4, 3.0},   // Hawaiian Ali'i Ramages (#06B6D4)
            {140.0, -4.50, 0x9C27B0, 5.0},   // New Guinea Segmentary Clans (#9C27B0)
            {106.8, -6.21, 0x06B6D4, 4.5},   // Javanese Bilateral Household (#06B6D4)
            {101.7, 3.14, 0x06B6D4, 4.0},    // Malay Nusantara Bilateral Matrifocal (#06B6D4)

            // --- Americas: Indigenous Social Formations ---
            {-100.0, 45.0, 0x3F51B5, 5.0},   // Plains Bilateral / Patrilineal Bands (#3F51B5)
            {-120.0, 55.0, 0x009688, 5.0},   // Pacific Northwest Potlatch Clan Ranking (#009688)
            {-90.0, 68.0, 0x84CC16, 7.0},    // Inuit Forager Multi-Family Bands (#84CC16)
            {-99.13, 19.43, 0x795548, 4.0},  // Calpulli / Mesoamerican Barrio Community (#795548)
            {-77.04, -12.0, 0x795548, 5.0},  // Andean Ayllu Dual Reciprocity (#795548)
            {-55.0, -15.0, 0x4CAF50, 6.0},   // Amazonian Moiety Longhouses (#4CAF50)
            {21.00, -25.0, 0x84CC16, 4.5}    // Khoisan Egalitarian Band Networks (#84CC16)
        };

        // 3. Pixel refinement with elevation mask & sub-regional overlay
        if (elevationMask != null) {
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                for (int x = 0; x < width; x++) {
                    double lon = -180.0 + (x + 0.5) / width * 360.0;
                    int mx = Math.clamp(x * elevationMask.getWidth() / width, 0, elevationMask.getWidth() - 1);
                    int my = Math.clamp(y * elevationMask.getHeight() / height, 0, elevationMask.getHeight() - 1);
                    int land = elevationMask.getRaster().getSample(mx, my, 0);
                    double occWeight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, targetYear);
                    if (land == 0 || lat < -60.0 || occWeight <= 0.001) {
                        img.setRGB(x, y, 0x000000);
                        continue;
                    }

                    int currentRgb = img.getRGB(x, y) & 0xFFFFFF;
                    boolean isUnassigned = (currentRgb == 0x000000);

                    // Refine all land cells using distance-weighted localized kinship centers
                    double maxInfl = 0.0;
                    int bestCol = isUnassigned ? 0x8B5CF6 : currentRgb;
                    for (double[] kc : kinshipCenters) {
                        double dLat = lat - kc[1];
                        double dLon = (lon - kc[0]) * Math.cos(Math.toRadians((lat + kc[1]) * 0.5));
                        double d2 = dLat * dLat + dLon * dLon;
                        double sigma = kc[3];
                        double infl = Math.exp(-d2 / (2.0 * sigma * sigma));
                        if (infl > maxInfl) {
                            maxInfl = infl;
                            bestCol = (int) kc[2];
                        }
                    }

                    if (isUnassigned || maxInfl >= 0.15) {
                        img.setRGB(x, y, bestCol);
                    }
                }
            }
        }

        logger.info("Successfully rasterized authentic high-precision Kinship tensor map for year {} ({}x{}).", targetYear, width, height);
        return img;
    }

    /**
     * Map historical polity name to authentic Isogloss (Linguistic Phylum / Branch) color.
     */
    private static String normalize(String s) {
        if (s == null) return "";
        String clean = s.replace('đ', 'd').replace('Đ', 'd');
        String n = java.text.Normalizer.normalize(clean, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    /**
     * Map historical polity name to authentic Isogloss (Linguistic Phylum / Branch) color.
     */
    public static Color getPolityIsoglossColor(String name, String seshatId) {
        if (name == null) return new Color(0x2563EB);
        String lower = normalize(name);

        // 1. Sinitic & Tibeto-Burman
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese")) return new Color(0xEF4444); // Sinitic Red
        if (lower.contains("burma") || lower.contains("konbaung") || lower.contains("tibetan") || lower.contains("tibet") ||
            lower.contains("bhutan")) return new Color(0xDC2626); // Tibeto-Burman Brick Red

        // 2. Japonic & Koreanic
        if (lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo")) return new Color(0xF43F5E); // Japonic Rose
        if (lower.contains("korea") || lower.contains("joseon")) return new Color(0xA855F7); // Koreanic Violet

        // 3. Indo-Aryan & Dravidian
        if (lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
            lower.contains("awadh") || lower.contains("bengal") || lower.contains("nepal") || lower.contains("sindh") ||
            lower.contains("india") || lower.contains("ceylon")) return new Color(0xF59E0B); // Indo-Aryan Amber
        if (lower.contains("carnatic") || lower.contains("mysore") || lower.contains("travancore") || lower.contains("hyderabad")) return new Color(0xB45309); // Dravidian Bronze

        // 4. Iranic
        if (lower.contains("persia") || lower.contains("qajar") || lower.contains("iran") ||
            lower.contains("durrani") || lower.contains("afghan") || lower.contains("baloch") || lower.contains("pashto")) return new Color(0xD97706); // Iranic Orange-Brown

        // 5. Turkic
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("bukhara") ||
            lower.contains("khiva") || lower.contains("kokand") || lower.contains("kazakh") || lower.contains("tatar")) return new Color(0x0EA5E9); // Turkic Sky Cyan

        // 6. Kartvelian & Caucasian
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16); // Kartvelian / Caucasian Lime

        // 7. Hellenic
        if (lower.contains("septinsular") || lower.contains("greek") || lower.contains("hellen")) return new Color(0x06B6D4); // Hellenic Cyan

        // 8. Uralic
        if (lower.contains("hungar") || lower.contains("magyar") || lower.contains("finland") || lower.contains("finn") || lower.contains("estonia")) return new Color(0x14B8A6); // Uralic Teal

        // 9. Prussian & German / Scandinavian / Anglo-Saxon (Check Prussia BEFORE Slavic russ!)
        if (lower.contains("prussia") || lower.contains("brandenburg") ||
            lower.contains("brit") || lower.contains("england") || lower.contains("great britain") ||
            lower.contains("united kingdom") || lower.contains("united states") || lower.contains("america") ||
            lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman") ||
            lower.contains("bavaria") || lower.contains("saxony") || lower.contains("hanover") ||
            lower.contains("baden") || lower.contains("wurttemberg") || lower.contains("hesse") ||
            lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("sweden") || lower.contains("denmark") || lower.contains("norway") ||
            lower.contains("greenland") || lower.contains("swiss") || lower.contains("helvetic")) return new Color(0x2563EB); // Germanic Royal Blue

        // 10. Slavic (Careful: exclude prussia)
        if (lower.contains("russ") || lower.contains("slav") || lower.contains("serb") ||
            lower.contains("bulgar") || lower.contains("poland") || lower.contains("bohemia") ||
            lower.contains("czech") || lower.contains("ukrain") || lower.contains("belarus")) return new Color(0x8B5CF6); // Slavic Purple

        // 11. Romance / Latin Branch (France, Spain, Portugal, Italy, Romania, etc.)
        if (lower.contains("franc") || lower.contains("french") || lower.contains("consulate") ||
            lower.contains("spain") || lower.contains("spanish") || lower.contains("castil") || lower.contains("urgell") ||
            lower.contains("portug") || lower.contains("brazil") ||
            lower.contains("ital") || lower.contains("naples") || lower.contains("sardinia") ||
            lower.contains("tuscany") || lower.contains("papal") || lower.contains("cispadane") ||
            lower.contains("cisalpine") || lower.contains("haiti") || lower.contains("moldavia") ||
            lower.contains("wallachia") || lower.contains("romania") || lower.contains("catalan") ||
            lower.contains("venice") || lower.contains("genoa") || lower.contains("parma") ||
            lower.contains("modena") || lower.contains("lucca") || lower.contains("ragusa") ||
            lower.contains("louisiana")) return new Color(0xEC4899); // Romance Vibrant Magenta/Rose

        // 12. Celtic & Basque
        if (lower.contains("celt") || lower.contains("irish") || lower.contains("gael") ||
            lower.contains("welsh") || lower.contains("breton")) return new Color(0x10B981); // Celtic Emerald
        if (lower.contains("basque") || lower.contains("eusk")) return new Color(0xF59E0B);

        // 13. Austroasiatic & Tai-Kadai
        if (lower.contains("vietnam") || lower.contains("nguyen") || lower.contains("tay son") ||
            lower.contains("dai viet") || lower.contains("khmer") || lower.contains("cambodia")) return new Color(0x16A085);
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("chiangmai") ||
            lower.contains("luang phrabang") || lower.contains("vientiane") || lower.contains("champasak") || lower.contains("thai")) return new Color(0xEAB308);

        // 14. Austronesian (Malay, Indonesian, Polynesian, Malagasy)
        if (lower.contains("banjar") || lower.contains("aceh") || lower.contains("perak") ||
            lower.contains("johor") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("yogyakarta") || lower.contains("surakarta") || lower.contains("banten") ||
            lower.contains("bone") || lower.contains("palembang") || lower.contains("jambi") ||
            lower.contains("brunei") || lower.contains("bali") || lower.contains("hawai") ||
            lower.contains("merina") || lower.contains("madagascar") || lower.contains("malay") ||
            lower.contains("malacca") || lower.contains("selangor") || lower.contains("kedah") ||
            lower.contains("negeri sembilan") || lower.contains("pontianak") || lower.contains("mempawah") ||
            lower.contains("sambas") || lower.contains("sukadana") || lower.contains("bima")) return new Color(0x06B6D4);

        // 15. Semitic & Afroasiatic
        if (lower.contains("morocco") || lower.contains("alaouite") || lower.contains("oman") ||
            lower.contains("diriyah") || lower.contains("kuwait") || lower.contains("qasimid") ||
            lower.contains("algiers") || lower.contains("tunis") || lower.contains("tripolitania") ||
            lower.contains("karamanli") || lower.contains("ethiopia") || lower.contains("egypt") || lower.contains("arab")) return new Color(0x15803D);

        // 16. Niger-Congo & Nilo-Saharan
        if (lower.contains("sokoto") || lower.contains("bornu") || lower.contains("oyo") ||
            lower.contains("ashanti") || lower.contains("bamana") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("funj") || lower.contains("lunda") ||
            lower.contains("kongo") || lower.contains("zulu") || lower.contains("xhosa") || lower.contains("dahomey")) return new Color(0x22C55E);

        return new Color(0x2563EB);
    }

    /**
     * Map historical polity name to authentic Kinship & Social Structure color.
     */
    public static Color getPolityKinshipColor(String name, String seshatId) {
        if (name == null) return new Color(0x8B5CF6);
        String lower = normalize(name);

        // 1. Egalitarian Nuclear Family & Civil Code / Partible Inheritance (#EC4899)
        if (lower.contains("franc") || lower.contains("french") || lower.contains("consulate") ||
            lower.contains("louisiana") || lower.contains("cispadane") || lower.contains("cisalpine") ||
            lower.contains("tuscany") || lower.contains("papal") || lower.contains("sardinia") ||
            lower.contains("naples") || lower.contains("two sicilies") || lower.contains("spain") ||
            lower.contains("spanish") || lower.contains("castil") || lower.contains("urgell") ||
            lower.contains("portug") || lower.contains("brazil") || lower.contains("haiti") ||
            lower.contains("belgium") || lower.contains("wallonia") || lower.contains("ragusa")) return new Color(0xEC4899);

        // 2. Absolute Nuclear Family & Proletarian Wage Labor (#3B82F6)
        if (lower.contains("brit") || lower.contains("england") || lower.contains("great britain") ||
            lower.contains("united kingdom") || lower.contains("united states") || lower.contains("america") ||
            lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("cape colony") || lower.contains("ceylon")) return new Color(0x3B82F6);

        // 3. Stem Family & Primogeniture (Stammfamilie / Famille Souche / Ie) (#8B5CF6)
        if (lower.contains("prussia") || lower.contains("brandenburg") ||
            lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman") ||
            lower.contains("bavaria") || lower.contains("saxony") || lower.contains("baden") ||
            lower.contains("wurttemberg") || lower.contains("hesse") || lower.contains("hanover") ||
            lower.contains("sweden") || lower.contains("denmark") || lower.contains("norway") ||
            lower.contains("greenland") || lower.contains("swiss") || lower.contains("helvetic") ||
            lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo") ||
            lower.contains("basque")) return new Color(0x8B5CF6);

        // 4. Patriarchal Joint Family & Peasant Commune (Mir / Obshchina / Zadruga) (#DC2626)
        if (lower.contains("russ") || lower.contains("slav") || lower.contains("serb") ||
            lower.contains("bulgar") || lower.contains("poland") || lower.contains("moldavia") ||
            lower.contains("wallachia") || lower.contains("ukrain") || lower.contains("bohemia") ||
            lower.contains("czech")) return new Color(0xDC2626);

        // 5. Agnatic Lineage & Confucian Zongzu Clan (#EF4444)
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese") ||
            lower.contains("korea") || lower.contains("joseon") || lower.contains("vietnam") ||
            lower.contains("nguyen") || lower.contains("tay son") || lower.contains("dai viet")) return new Color(0xEF4444);

        // 6. Segmentary Patrilineal Lineage & Endogamous Clan (#10B981)
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("morocco") ||
            lower.contains("alaouite") || lower.contains("oman") || lower.contains("diriyah") ||
            lower.contains("kuwait") || lower.contains("qasimid") || lower.contains("algiers") ||
            lower.contains("tunis") || lower.contains("tripolitania") || lower.contains("karamanli") ||
            lower.contains("persia") || lower.contains("qajar") || lower.contains("iran") ||
            lower.contains("durrani") || lower.contains("afghan") || lower.contains("kazakh") ||
            lower.contains("bukhara") || lower.contains("khiva") || lower.contains("kokand")) return new Color(0x10B981);

        // 7. Gotra Exogamous Patrilineal Joint Family & Jati Caste Endogamy (#F59E0B)
        if (lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
            lower.contains("awadh") || lower.contains("bengal") || lower.contains("carnatic") ||
            lower.contains("mysore") || lower.contains("travancore") || lower.contains("nepal") ||
            lower.contains("india") || lower.contains("hyderabad")) return new Color(0xF59E0B);

        // 8. Bilateral Matrifocal & Malay Nusantara / Southeast Asia (#06B6D4)
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("burma") ||
            lower.contains("konbaung") || lower.contains("khmer") || lower.contains("cambodia") ||
            lower.contains("chiangmai") || lower.contains("luang phrabang") || lower.contains("vientiane") ||
            lower.contains("champasak") || lower.contains("banjar") || lower.contains("aceh") ||
            lower.contains("johor") || lower.contains("yogyakarta") || lower.contains("surakarta") ||
            lower.contains("banten") || lower.contains("brunei") || lower.contains("bali") ||
            lower.contains("hawai") || lower.contains("merina") || lower.contains("madagascar") ||
            lower.contains("malay") || lower.contains("malacca") || lower.contains("selangor") ||
            lower.contains("kedah") || lower.contains("negeri sembilan") || lower.contains("pontianak") ||
            lower.contains("mempawah") || lower.contains("sambas") || lower.contains("sukadana") ||
            lower.contains("bima") || lower.contains("bone") || lower.contains("palembang") ||
            lower.contains("jambi") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("septinsular")) return new Color(0x06B6D4);

        // 9. African Segmentary Lineages & Age-Set Organizations (#15803D)
        if (lower.contains("sokoto") || lower.contains("bornu") || lower.contains("oyo") ||
            lower.contains("ashanti") || lower.contains("bamana") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("funj") || lower.contains("lunda") ||
            lower.contains("kongo") || lower.contains("zulu") || lower.contains("xhosa") ||
            lower.contains("dahomey") || lower.contains("ethiopia")) return new Color(0x15803D);

        // 10. Caucasian Mountain Clan Lineages (#84CC16)
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16);

        return new Color(0x8B5CF6);
    }

    /**
     * Map historical polity name to official hex color matching cultural_registry.json.
     */
    public static Color getPolityColor(String name, String seshatId) {
        if (name == null) return new Color(0x64748B);
        String lower = normalize(name);

        // 1. British Empire & Dominions (#DC2626)
        if (lower.contains("great britain") || lower.contains("kingdom of great britain") ||
            lower.contains("british") || lower.contains("england") || lower.contains("united kingdom") ||
            lower.contains("cape colony") || lower.contains("ceylon") || lower.contains("new south wales")) return new Color(0xDC2626);

        // 2. French Republic, Consulate & Dependencies (#2563EB)
        if (lower.contains("french") || lower.contains("france") || lower.contains("consulate") ||
            lower.contains("louisiana") || lower.contains("cispadane") || lower.contains("cisalpine")) return new Color(0x2563EB);

        // 3. Kingdom of Prussia (#1E293B)
        if (lower.contains("prussia") || lower.contains("brandenburg")) return new Color(0x1E293B);

        // 4. Habsburg Monarchy (#F59E0B)
        if (lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman")) return new Color(0xF59E0B);

        // 5. German Secondary States
        if (lower.contains("bavaria")) return new Color(0x38BDF8); // Bavarian Blue
        if (lower.contains("saxony")) return new Color(0x4ADE80);  // Saxon Green
        if (lower.contains("hanover")) return new Color(0xFB923C); // Hanover Amber
        if (lower.contains("baden") || lower.contains("wurttemberg") || lower.contains("hesse") ||
            lower.contains("nassau") || lower.contains("brunswick") || lower.contains("anhalt") ||
            lower.contains("liechtenstein")) return new Color(0xFBBF24);

        // 6. Scandinavian Kingdoms
        if (lower.contains("denmark")) return new Color(0xB91C1C); // Danish Crimson
        if (lower.contains("sweden")) return new Color(0x0284C7);  // Swedish Blue
        if (lower.contains("greenland")) return new Color(0x94A3B8);

        // 7. Swiss Confederation / Helvetic Republic (#EF4444)
        if (lower.contains("swiss") || lower.contains("helvetic")) return new Color(0xEF4444);

        // 8. Dutch / Batavian Republic (#F97316)
        if (lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("new netherland")) return new Color(0xF97316);

        // 9. Italian States
        if (lower.contains("sardinia")) return new Color(0xEC4899);
        if (lower.contains("naples") || lower.contains("two sicilies")) return new Color(0xB45309);
        if (lower.contains("papal")) return new Color(0xEAB308);
        if (lower.contains("tuscany")) return new Color(0xA855F7);
        if (lower.contains("ragusa")) return new Color(0x14B8A6);

        // 10. Spanish Empire & Viceroyalties (#EA580C)
        if (lower.contains("spain") || lower.contains("spanish") || lower.contains("castil") ||
            lower.contains("urgell")) return new Color(0xEA580C);

        // 11. Portuguese Empire & Colonies (#10B981)
        if (lower.contains("portug") || lower.contains("brazil")) return new Color(0x10B981);

        // 12. Russian Empire (#7C3AED)
        if (lower.contains("russ") || lower.contains("alaska")) return new Color(0x7C3AED);

        // 13. Caucasian Principalities (#84CC16)
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16);

        // 14. Ottoman Empire & North African Regencies (#059669)
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("algiers") ||
            lower.contains("tunis") || lower.contains("tripolitania") || lower.contains("karamanli") ||
            lower.contains("moldavia") || lower.contains("wallachia") || lower.contains("septinsular")) return new Color(0x059669);

        // 15. United States of America (#3B82F6)
        if (lower.contains("united states") || lower.contains("america") || lower.contains("haiti") ||
            lower.contains("cuba")) return new Color(0x3B82F6);

        // 16. Qing Empire (#EF4444)
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese")) return new Color(0xEF4444);

        // 17. Joseon Korea (#8B5CF6)
        if (lower.contains("korea") || lower.contains("joseon")) return new Color(0x8B5CF6);

        // 18. Tokugawa Shogunate (#E11D48)
        if (lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo")) return new Color(0xE11D48);

        // 19. Indian Subcontinent Polities
        if (lower.contains("maratha")) return new Color(0xF59E0B);
        if (lower.contains("mughal")) return new Color(0xD97706);
        if (lower.contains("sikh")) return new Color(0xFBBF24);
        if (lower.contains("awadh") || lower.contains("carnatic") || lower.contains("mysore") ||
            lower.contains("travancore") || lower.contains("hyderabad") || lower.contains("nepal") ||
            lower.contains("india") || lower.contains("bhutan")) return new Color(0xD97706);

        // 20. Iranian & Central Asian States
        if (lower.contains("persia") || lower.contains("qajar") || lower.contains("iran")) return new Color(0x0891B2);
        if (lower.contains("durrani") || lower.contains("afghan") || lower.contains("bukhara") ||
            lower.contains("khiva") || lower.contains("kokand") || lower.contains("kazakh")) return new Color(0x0284C7);

        // 21. Southeast Asian Kingdoms
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("chiangmai") ||
            lower.contains("luang phrabang") || lower.contains("vientiane") || lower.contains("champasak") ||
            lower.contains("khmer") || lower.contains("cambodia") || lower.contains("burma") ||
            lower.contains("konbaung") || lower.contains("thai")) return new Color(0xF97316);

        // 22. Vietnam: Nguyen & Tay Son (#16A085)
        if (lower.contains("vietnam") || lower.contains("nguyen") || lower.contains("tay son") ||
            lower.contains("dai viet")) return new Color(0x16A085);

        // 23. Maritime Nusantara Sultanates (#047857)
        if (lower.contains("banjar") || lower.contains("aceh") || lower.contains("perak") ||
            lower.contains("johor") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("yogyakarta") || lower.contains("surakarta") || lower.contains("banten") ||
            lower.contains("bone") || lower.contains("palembang") || lower.contains("jambi") ||
            lower.contains("mempawah") || lower.contains("sambas") || lower.contains("sukadana") ||
            lower.contains("bima") || lower.contains("kutai") || lower.contains("pontianak") ||
            lower.contains("selangor") || lower.contains("terengganu") || lower.contains("kelantan") ||
            lower.contains("negeri sembilan") || lower.contains("brunei") || lower.contains("bali") ||
            lower.contains("malacca")) return new Color(0x047857);

        // 24. Sahelian & West/Central African Kingdoms (#15803D)
        if (lower.contains("sokoto") || lower.contains("hausa") || lower.contains("fulani") ||
            lower.contains("bornu") || lower.contains("oyo") || lower.contains("ashanti") ||
            lower.contains("bamana") || lower.contains("funj") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("lunda") || lower.contains("kongo")) return new Color(0x15803D);

        // 25. Ethiopian Empire & Madagascar (#84CC16)
        if (lower.contains("ethiopia") || lower.contains("abyssin") || lower.contains("merina") ||
            lower.contains("madagascar")) return new Color(0x84CC16);

        // 26. Morocco & Arabian Sultanates (#0D9488)
        if (lower.contains("morocco") || lower.contains("alaouite") || lower.contains("oman") ||
            lower.contains("diriyah") || lower.contains("kuwait") || lower.contains("qasimid") ||
            lower.contains("qatar") || lower.contains("shammar") || lower.contains("nejd")) return new Color(0x0D9488);

        // 27. Polynesian & Pacific Island Domains (#E67E22)
        if (lower.contains("hawai") || lower.contains("maori") || lower.contains("polynes")) return new Color(0xE67E22);

        // 28. Indigenous Polar & Arctic Domains (#607D8B)
        if (lower.contains("inuit") || lower.contains("thule")) return new Color(0x607D8B);

        // 29. Southern African Kingdoms & Free States (#F59E0B)
        if (lower.contains("zulu") || lower.contains("xhosa") || lower.contains("khoisan") ||
            lower.contains("orange free") || lower.contains("natalia") || lower.contains("south african republic") ||
            lower.contains("swaziland")) return new Color(0xF59E0B);

        // Deterministic distinct fallback palette based on name hash
        int hash = Math.abs(name.hashCode());
        float hue = (hash % 360) / 360.0f;
        float sat = 0.75f + (hash % 20) * 0.01f;
        float bri = 0.70f + (hash % 25) * 0.01f;
        return Color.getHSBColor(hue, sat, bri);
    }

    /**
     * Fast streaming parser for 158 MB GeoJSON file using Jackson Streaming API.
     */
    public static List<HistoricalPolityFeature> loadPolitiesForYear(File geoJsonFile, int targetYear, int imgW, int imgH) {
        List<HistoricalPolityFeature> matched = new ArrayList<>();
        JsonFactory factory = new JsonFactory();

        try (InputStream is = new FileInputStream(geoJsonFile);
             JsonParser parser = factory.createParser(is)) {

            while (parser.nextToken() != null) {
                if ("features".equals(parser.currentName()) && parser.currentToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            parseFeatureIfMatches(parser, targetYear, imgW, imgH, matched);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error streaming Seshat GeoJSON: {}", e.getMessage(), e);
        }

        return matched;
    }

    private static void parseFeatureIfMatches(JsonParser parser, int targetYear, int imgW, int imgH, List<HistoricalPolityFeature> matched) throws Exception {
        String name = null;
        int fromYear = -99999;
        int toYear = 99999;
        String seshatId = null;
        String wiki = null;
        List<Path2D> paths = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            if ("properties".equals(field)) {
                parser.nextToken(); // START_OBJECT
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String prop = parser.currentName();
                    parser.nextToken();
                    if ("Name".equalsIgnoreCase(prop)) name = parser.getText();
                    else if ("FromYear".equalsIgnoreCase(prop)) fromYear = parser.getIntValue();
                    else if ("ToYear".equalsIgnoreCase(prop)) toYear = parser.getIntValue();
                    else if ("SeshatID".equalsIgnoreCase(prop)) seshatId = parser.getText();
                    else if ("Wikipedia".equalsIgnoreCase(prop)) wiki = parser.getText();
                    else parser.skipChildren();
                }
            } else if ("geometry".equals(field)) {
                parser.nextToken(); // START_OBJECT
                String geomType = null;
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String gField = parser.currentName();
                    if ("type".equalsIgnoreCase(gField)) {
                        parser.nextToken();
                        geomType = parser.getText();
                    } else if ("coordinates".equalsIgnoreCase(gField)) {
                        parser.nextToken();
                        parseCoordinatesToPaths(parser, geomType, imgW, imgH, paths);
                    } else {
                        parser.nextToken();
                        parser.skipChildren();
                    }
                }
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        // Check if polity was active at targetYear (with +/- 25 year flexibility window if near boundary)
        if (targetYear >= fromYear && targetYear <= toYear && !paths.isEmpty() && name != null) {
            HistoricalPolityFeature f = new HistoricalPolityFeature();
            f.name = name;
            f.fromYear = fromYear;
            f.toYear = toYear;
            f.seshatId = seshatId;
            f.wikipedia = wiki;
            f.paths = paths;
            f.color = getPolityColor(name, seshatId);
            matched.add(f);
        }
    }

    private static void parseCoordinatesToPaths(JsonParser parser, String geomType, int imgW, int imgH, List<Path2D> paths) throws Exception {
        if ("Polygon".equalsIgnoreCase(geomType)) {
            // Array of linear rings: [ [ [x,y], [x,y]... ], [hole...] ]
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                Path2D path = parseLinearRing(parser, imgW, imgH);
                if (path != null) paths.add(path);
            }
        } else if ("MultiPolygon".equalsIgnoreCase(geomType)) {
            // Array of polygons: [ [ [ [x,y]... ] ] ]
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    Path2D path = parseLinearRing(parser, imgW, imgH);
                    if (path != null) paths.add(path);
                }
            }
        } else {
            parser.skipChildren();
        }
    }

    private static Path2D parseLinearRing(JsonParser parser, int imgW, int imgH) throws Exception {
        if (parser.currentToken() != JsonToken.START_ARRAY) return null;

        Path2D path = new Path2D.Double();
        boolean first = true;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                double lon = parser.getDoubleValue();
                parser.nextToken();
                double lat = parser.getDoubleValue();
                parser.nextToken(); // END_ARRAY for point

                // Equirectangular projection mapping
                double px = ((lon + 180.0) / 360.0) * (imgW - 1);
                double py = ((90.0 - lat) / 180.0) * (imgH - 1);

                if (first) {
                    path.moveTo(px, py);
                    first = false;
                } else {
                    path.lineTo(px, py);
                }
            }
        }
        if (!first) {
            path.closePath();
            return path;
        }
        return null;
    }
}

