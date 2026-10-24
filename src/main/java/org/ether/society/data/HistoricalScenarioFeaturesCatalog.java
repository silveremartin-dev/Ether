/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical Scenario Cartographic Features and Archaeological Anchor Catalog.
 * 
 * <p>Extracted from {@code HistoricalMapGenerator} to satisfy the Single Responsibility Principle (SRP).
 * Provides empirical geolocations of historical cities, major river arteries, empire borders,
 * isogloss centers, kinship structures, and ritual sacred sites across civilizational eras.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalScenarioFeaturesCatalog {

    private HistoricalScenarioFeaturesCatalog() {
        // Pure static catalog
    }

    /**
     * Gets historical city points calibrated for a specific simulation scenario and start year.
     *
     * @param type scenario population type
     * @param scenario simulation scenario instance
     * @return list of city anchor points
     */
    public static List<CityPoint> getCitiesForScenario(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        List<CityPoint> list = new ArrayList<>();

        if (year <= -3000L) {
            // Early Bronze Age / Megalithic Civilizations
            list.add(new CityPoint("Uruk", 31.3, 45.6, 3.8, 2.2));
            list.add(new CityPoint("Memphis", 29.8, 31.2, 3.8, 2.2));
            list.add(new CityPoint("Susa", 32.2, 48.3, 3.5, 2.0));
            list.add(new CityPoint("Mohenjo-Daro", 27.3, 68.1, 3.5, 2.0));
            list.add(new CityPoint("Caral-Supe", -10.9, -77.5, 3.2, 2.0));
            list.add(new CityPoint("Liangzhu", 30.4, 120.0, 3.5, 2.0));
            list.add(new CityPoint("Knossos", 35.3, 25.2, 3.0, 1.8));
        } else if (year <= -500L) {
            // Bronze & Early Iron Age Empires (Thebes, Babylon, Nineveh, Anyang, Tyre)
            list.add(new CityPoint("Thebes", 25.7, 32.6, 4.0, 2.4));
            list.add(new CityPoint("Babylon", 32.5, 44.4, 4.2, 2.5));
            list.add(new CityPoint("Nineveh", 36.3, 43.1, 4.0, 2.4));
            list.add(new CityPoint("Anyang", 36.1, 114.3, 4.0, 2.4));
            list.add(new CityPoint("Tyre", 33.3, 35.2, 3.5, 2.0));
            list.add(new CityPoint("Varanasi", 25.3, 83.0, 3.8, 2.2));
            list.add(new CityPoint("San Lorenzo", 17.8, -94.8, 3.0, 2.0));
        } else if (year <= 500L) {
            // Classical Antiquity & Axial Age (Rome, Alexandria, Chang'an, Luoyang, Pataliputra, Ctesiphon, Teotihuacán)
            list.add(new CityPoint("Rome", 41.9, 12.5, 4.5, 2.8));
            list.add(new CityPoint("Alexandria", 31.2, 29.9, 4.2, 2.5));
            list.add(new CityPoint("Chang'an", 34.2, 108.9, 4.5, 2.8));
            list.add(new CityPoint("Luoyang", 34.6, 112.4, 4.2, 2.5));
            list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.2, 2.5));
            list.add(new CityPoint("Ctesiphon", 33.1, 44.6, 4.0, 2.4));
            list.add(new CityPoint("Antioch", 36.2, 36.1, 3.8, 2.2));
            list.add(new CityPoint("Carthage", 36.8, 10.3, 3.5, 2.0));
            list.add(new CityPoint("Athens", 37.9, 23.7, 3.4, 2.0));
            list.add(new CityPoint("Taxila", 33.7, 72.8, 3.5, 2.0));
            list.add(new CityPoint("Teotihuacán", 19.7, -98.9, 3.8, 2.4));
            list.add(new CityPoint("Tikal", 17.2, -89.6, 3.2, 2.0));
        } else if (year <= 1500L) {
            // Medieval Era (Constantinople, Baghdad, Kaifeng, Hangzhou, Cairo, Cordoba, Kyoto, Tenochtitlan, Cuzco)
            list.add(new CityPoint("Constantinople", 41.0, 28.9, 4.6, 2.8));
            list.add(new CityPoint("Baghdad", 33.3, 44.4, 4.8, 2.8));
            list.add(new CityPoint("Kaifeng", 34.7, 114.3, 4.8, 2.8));
            list.add(new CityPoint("Hangzhou", 30.2, 120.1, 4.8, 2.8));
            list.add(new CityPoint("Cairo", 30.0, 31.2, 4.5, 2.5));
            list.add(new CityPoint("Cordoba", 37.9, -4.8, 4.2, 2.2));
            list.add(new CityPoint("Delhi", 28.6, 77.2, 4.2, 2.4));
            list.add(new CityPoint("Kyoto", 35.0, 135.7, 4.0, 2.2));
            list.add(new CityPoint("Tenochtitlan", 19.4, -99.1, 4.2, 2.4));
            list.add(new CityPoint("Cuzco", -13.5, -71.9, 3.8, 2.2));
            list.add(new CityPoint("Timbuktu", 16.7, -3.0, 3.5, 2.0));
            list.add(new CityPoint("Angkor", 13.4, 103.9, 4.4, 2.5));
            list.add(new CityPoint("Paris", 48.8, 2.35, 4.0, 2.2));
        } else if (year <= 1800L) {
            // Early Modern Era (Beijing, London, Paris, Edo, Istanbul, Delhi, Amsterdam, Mexico City)
            list.add(new CityPoint("Beijing", 39.9, 116.4, 5.0, 2.8));
            list.add(new CityPoint("London", 51.5, -0.1, 4.8, 2.6));
            list.add(new CityPoint("Paris", 48.8, 2.35, 4.8, 2.6));
            list.add(new CityPoint("Edo", 35.6, 139.7, 5.0, 2.8));
            list.add(new CityPoint("Istanbul", 41.0, 28.9, 4.8, 2.6));
            list.add(new CityPoint("Delhi", 28.6, 77.2, 4.6, 2.5));
            list.add(new CityPoint("Amsterdam", 52.4, 4.9, 4.2, 2.2));
            list.add(new CityPoint("Mexico City", 19.4, -99.1, 4.2, 2.2));
            list.add(new CityPoint("Potosí", -19.6, -65.7, 3.8, 2.0));
        } else if (year <= 1920L) {
            // 19th - Early 20th C Industrial
            list.add(new CityPoint("London", 51.5, -0.1, 5.4, 2.8));
            list.add(new CityPoint("New York", 40.7, -74.0, 5.4, 2.8));
            list.add(new CityPoint("Paris", 48.8, 2.35, 5.0, 2.6));
            list.add(new CityPoint("Berlin", 52.5, 13.4, 4.8, 2.5));
            list.add(new CityPoint("Vienna", 48.2, 16.4, 4.6, 2.4));
            list.add(new CityPoint("Saint Petersburg", 59.9, 30.3, 4.8, 2.5));
            list.add(new CityPoint("Chicago", 41.8, -87.6, 4.8, 2.5));
            list.add(new CityPoint("Tokyo", 35.6, 139.7, 5.0, 2.6));
            list.add(new CityPoint("Shanghai", 31.2, 121.5, 4.8, 2.5));
            list.add(new CityPoint("Calcutta", 22.5, 88.3, 4.6, 2.4));
        } else {
            // Contemporary / Future
            list.add(new CityPoint("Tokyo Megacity", 35.6, 139.7, 5.5, 2.8));
            list.add(new CityPoint("New York Tri-State", 40.7, -74.0, 5.2, 2.6));
            list.add(new CityPoint("London Metro", 51.5, -0.1, 4.8, 2.4));
            list.add(new CityPoint("Shanghai Yangtze Hub", 31.2, 121.5, 5.5, 2.8));
            list.add(new CityPoint("Mumbai Metropolis", 19.0, 72.8, 5.2, 2.6));
            list.add(new CityPoint("Cairo Nile Megacity", 30.0, 31.2, 4.8, 2.4));
            list.add(new CityPoint("Lagos Gulf Corridor", 6.5, 3.3, 4.5, 2.2));
            list.add(new CityPoint("São Paulo Hub", -23.5, -46.6, 4.8, 2.4));
        }

        // Merge scenario specific historical anchors
        list.addAll(getCitiesForScenario(type));
        return list;
    }

    /**
     * Gets historical city points by preset type identifier.
     *
     * @param type preset type identifier
     * @return list of city anchor points
     */
    public static List<CityPoint> getCitiesForScenario(String type) {
        List<CityPoint> list = new ArrayList<>();
        if (type == null) return list;
        switch (type.toUpperCase()) {
            case "EGYPT_NILE":
                list.add(new CityPoint("Thebes", 32.6, 25.7, 4.0, 1.5));
                list.add(new CityPoint("Memphis", 31.2, 29.8, 4.5, 1.8));
                list.add(new CityPoint("Alexandria", 29.9, 31.2, 5.0, 2.0));
                break;
            case "ROMAN_EMPIRE":
                list.add(new CityPoint("Rome", 12.5, 41.9, 5.5, 2.5));
                list.add(new CityPoint("Constantinople", 28.9, 41.0, 5.0, 2.2));
                list.add(new CityPoint("Alexandria", 29.9, 31.2, 4.8, 2.0));
                list.add(new CityPoint("Antioch", 36.1, 36.2, 4.2, 1.8));
                list.add(new CityPoint("Carthage", 10.3, 36.8, 4.0, 1.8));
                list.add(new CityPoint("Lugdunum", 4.8, 45.7, 3.5, 1.5));
                list.add(new CityPoint("Londinium", -0.1, 51.5, 3.0, 1.5));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
                list.add(new CityPoint("Uruk", 45.6, 31.3, 4.0, 1.8));
                list.add(new CityPoint("Babylon", 44.4, 32.5, 5.0, 2.0));
                list.add(new CityPoint("Nineveh", 43.1, 36.3, 4.5, 1.8));
                list.add(new CityPoint("Ur", 46.1, 30.9, 4.2, 1.8));
                break;
            case "INDIA_MAURYA":
                list.add(new CityPoint("Pataliputra", 85.1, 25.6, 5.0, 2.5));
                list.add(new CityPoint("Taxila", 72.8, 33.7, 4.0, 1.8));
                list.add(new CityPoint("Ujjain", 75.8, 23.2, 4.2, 1.8));
                break;
            case "WEST_AFRICA_MALI":
                list.add(new CityPoint("Niani", -8.2, 11.4, 4.0, 2.0));
                list.add(new CityPoint("Timbuktu", -3.0, 16.7, 4.5, 2.2));
                list.add(new CityPoint("Gao", -0.0, 16.3, 4.0, 1.8));
                list.add(new CityPoint("Djenné", -4.5, 13.9, 3.8, 1.8));
                break;
            case "SONG_DYNASTY":
                list.add(new CityPoint("Kaifeng", 114.3, 34.7, 5.5, 2.5));
                list.add(new CityPoint("Hangzhou", 120.1, 30.2, 5.2, 2.5));
                list.add(new CityPoint("Luoyang", 112.4, 34.6, 4.5, 2.0));
                list.add(new CityPoint("Guangzhou", 113.2, 23.1, 4.2, 2.0));
                break;
            case "SAKOKU_JAPAN":
                list.add(new CityPoint("Edo", 139.7, 35.6, 5.5, 2.2));
                list.add(new CityPoint("Kyoto", 135.7, 35.0, 4.8, 2.0));
                list.add(new CityPoint("Osaka", 135.5, 34.7, 4.5, 1.8));
                list.add(new CityPoint("Nagasaki", 129.8, 32.7, 3.5, 1.5));
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
                list.add(new CityPoint("Tenochtitlan", -99.1, 19.4, 5.2, 2.2));
                list.add(new CityPoint("Teotihuacán", -98.9, 19.7, 4.5, 2.0));
                list.add(new CityPoint("Tikal", -89.6, 17.2, 4.0, 1.8));
                list.add(new CityPoint("Cuzco", -71.9, -13.5, 4.8, 2.0));
                list.add(new CityPoint("Chan Chan", -79.1, -8.1, 4.0, 1.8));
                break;
            case "BERINGIA_AMERICAS":
            case "SAHUL_MIGRATION":
                list.add(new CityPoint("Lake Mungo", 143.0, -33.7, 2.5, 2.0));
                list.add(new CityPoint("Yana RHS", 135.4, 70.7, 3.0, 2.5));
                list.add(new CityPoint("Bluefish Caves", -140.7, 67.1, 2.5, 2.0));
                break;
            case "OUT_OF_AFRICA_100K":
            case "ONE_CONTINENT":
                list.add(new CityPoint("Omo Kibish (East Africa Core)", 36.0, 4.5, 4.5, 15.0));
                list.add(new CityPoint("Herto & Afar (Horn of Africa)", 40.5, 8.9, 4.2, 14.0));
                list.add(new CityPoint("Jebel Irhoud (North Africa)", -8.8, 31.6, 3.8, 15.0));
                list.add(new CityPoint("Blombos & Klasies (South Africa)", 21.2, -34.4, 3.8, 15.0));
                list.add(new CityPoint("Iho Eleru & Congo (West/Central Africa)", 5.1, 7.4, 3.5, 16.0));
                list.add(new CityPoint("Nile Corridor (African Bridge)", 31.5, 22.0, 4.0, 12.0));
                list.add(new CityPoint("Atapuerca & Spy (Néandertal Ouest)", -3.5, 42.3, 1.2, 12.0));
                list.add(new CityPoint("Shanidar (Néandertal Moyen-Orient)", 44.2, 36.8, 1.2, 12.0));
                list.add(new CityPoint("Denisova & Xiahe (Denisovien Altai)", 84.7, 51.4, 0.9, 13.0));
                break;
            default:
                list.add(new CityPoint("Tokyo", 139.7, 35.6, 4.0, 2.5));
                list.add(new CityPoint("New York", -74.0, 40.7, 3.5, 2.5));
                list.add(new CityPoint("London", -0.1, 51.5, 3.2, 2.0));
                break;
        }
        return list;
    }

    /**
     * Gets major navigable river ribbons by preset scenario identifier.
     *
     * @param type preset scenario identifier
     * @return list of river ribbon segments
     */
    public static List<RiverRibbon> getRiversForScenario(String type) {
        List<RiverRibbon> list = new ArrayList<>();
        if (type == null) return list;
        switch (type.toUpperCase()) {
            case "EGYPT_NILE":
            case "ROMAN_EMPIRE":
                list.add(new RiverRibbon(32.0, 24.0, 31.5, 31.5, 2.5, 1.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
                list.add(new RiverRibbon(38.0, 37.0, 48.0, 30.0, 2.8, 1.2));
                break;
            case "INDIA_MAURYA":
                list.add(new RiverRibbon(78.0, 30.0, 90.0, 22.0, 3.0, 1.5));
                break;
            case "SONG_DYNASTY":
                list.add(new RiverRibbon(105.0, 35.0, 120.0, 35.0, 3.0, 1.5));
                list.add(new RiverRibbon(105.0, 30.0, 121.0, 31.0, 3.0, 1.5));
                break;
            case "WEST_AFRICA_MALI":
                list.add(new RiverRibbon(-10.0, 11.0, 3.0, 16.0, 2.2, 1.2));
                break;
            case "ONE_CONTINENT":
                list.add(new RiverRibbon(31.0, 3.0, 32.5, 31.0, 2.0, 1.5));
                break;
        }
        return list;
    }

    /**
     * Gets major historical empire boundaries by preset scenario identifier.
     *
     * @param type preset scenario identifier
     * @return list of empire territory bounding structures
     */
    public static List<EmpireTerritory> getEmpiresForScenario(String type) {
        List<EmpireTerritory> list = new ArrayList<>();
        if (type == null) return list;
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                EmpireTerritory rome = new EmpireTerritory("Imperium Romanum", new Color(220, 38, 38));
                rome.addBoundingBox(-9.0, 30.0, 45.0, 54.0);
                list.add(rome);
                break;
            case "INDIA_MAURYA":
                EmpireTerritory maurya = new EmpireTerritory("Empire Maurya", new Color(217, 119, 6));
                maurya.addBoundingBox(68.0, 8.0, 90.0, 34.0);
                list.add(maurya);
                break;
            case "WEST_AFRICA_MALI":
                EmpireTerritory mali = new EmpireTerritory("Empire du Mali", new Color(16, 185, 129));
                mali.addBoundingBox(-17.0, 8.0, 4.0, 20.0);
                list.add(mali);
                break;
            case "SONG_DYNASTY":
                EmpireTerritory song = new EmpireTerritory("Dynastie Song", new Color(239, 68, 68));
                song.addBoundingBox(98.0, 18.0, 124.0, 38.0);
                list.add(song);
                break;
            case "SAKOKU_JAPAN":
                EmpireTerritory japan = new EmpireTerritory("Shogunat Tokugawa", new Color(168, 85, 247));
                japan.addBoundingBox(129.0, 30.0, 145.0, 44.0);
                list.add(japan);
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
                EmpireTerritory aztec = new EmpireTerritory("Alliance Aztèque", new Color(245, 158, 11));
                aztec.addBoundingBox(-102.0, 14.0, -95.0, 21.0);
                list.add(aztec);
                EmpireTerritory inca = new EmpireTerritory("Tawantinsuyu (Inca)", new Color(220, 38, 38));
                inca.addBoundingBox(-80.0, -35.0, -68.0, 2.0);
                list.add(inca);
                break;
            case "ONE_CONTINENT":
                EmpireTerritory sapiens = new EmpireTerritory("Foyer Pan-Africain Homo Sapiens", new Color(234, 179, 8));
                sapiens.addBoundingBox(-18.0, -35.0, 51.0, 37.0);
                list.add(sapiens);

                EmpireTerritory neanderthal = new EmpireTerritory("Niche Homo Neanderthalensis (Eurasie Ouest)", new Color(14, 165, 233));
                neanderthal.addBoundingBox(-10.0, 32.0, 55.0, 58.0);
                list.add(neanderthal);

                EmpireTerritory denisova = new EmpireTerritory("Niche Homo Denisova (Eurasie Est & Asie)", new Color(217, 70, 239));
                denisova.addBoundingBox(55.0, 20.0, 125.0, 65.0);
                list.add(denisova);
                break;
            default:
                EmpireTerritory generic = new EmpireTerritory("Sphère Démographique", new Color(59, 130, 246));
                generic.addBoundingBox(-10.0, 25.0, 50.0, 55.0);
                list.add(generic);
                break;
        }
        return list;
    }

    /**
     * Gets historical linguistic family centers by scenario identifier.
     *
     * @param type preset scenario identifier
     * @return list of language zone anchors
     */
    public static List<LanguageZone> getLanguageZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Latin", 12.5, 41.9, new Color(59, 130, 246)));
                    list.add(new LanguageZone("Greek", 23.7, 37.9, new Color(6, 182, 212)));
                    list.add(new LanguageZone("Celtic", 2.3, 48.8, new Color(34, 197, 94)));
                    list.add(new LanguageZone("Punique", 10.3, 36.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Aramaic", 35.4, 31.8, new Color(239, 68, 68)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Proto-Semitic", 35.4, 31.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Sumerian", 44.4, 32.5, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Anatolian", 38.9, 37.2, new Color(168, 85, 247)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Proto-Sapiens (Afrique)", 36.0, 4.5, new Color(234, 179, 8)));
                    list.add(new LanguageZone("Proto-Néandertalien (Eurasie Ouest)", -3.5, 42.3, new Color(14, 165, 233)));
                    list.add(new LanguageZone("Proto-Denisovien (Asie & Altai)", 84.7, 51.4, new Color(217, 70, 239)));
                    return list;
            }
        }
        list.add(new LanguageZone("Indo-European Baseline", 15.0, 48.0, new Color(59, 130, 246)));
        list.add(new LanguageZone("Afroasiatic Baseline", 25.0, 25.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sino-Tibetan Baseline", 105.0, 32.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Niger-Congo Baseline", 15.0, 5.0, new Color(16, 185, 129)));
        list.add(new LanguageZone("Austronesian Baseline", 115.0, -2.0, new Color(6, 182, 212)));
        list.add(new LanguageZone("Trans-New Guinea / Sahul", 138.0, -18.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Amerind & Na-Dene Baseline", -85.0, 15.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Uralic & Altaic Baseline", 75.0, 55.0, new Color(101, 163, 13)));
        list.add(new LanguageZone("Dravidian Baseline", 78.0, 14.0, new Color(217, 119, 6)));
        return list;
    }

    /**
     * Gets kinship organization zones by scenario identifier.
     *
     * @param type preset scenario identifier
     * @return list of kinship zone anchors
     */
    public static List<LanguageZone> getKinshipZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null && type.equalsIgnoreCase("ROMAN_EMPIRE")) {
            list.add(new LanguageZone("Patrilineal Gens Romana", 12.5, 41.9, new Color(168, 85, 247)));
        } else if (type != null && type.equalsIgnoreCase("WEST_AFRICA_MALI")) {
            list.add(new LanguageZone("Matrilineal Lineage & Clan", -10.0, 12.0, new Color(236, 72, 153)));
        }

        list.add(new LanguageZone("Patrilineal Gens & Lineage", 15.0, 45.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Matrilineal Clan Systems", 10.0, 8.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Bilateral Forager Bands", 135.0, -25.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Nomadic Steppe Clan Federations", 75.0, 48.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("State Lineage & Household", 110.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Amerind Matri-Clans", -80.0, 10.0, new Color(234, 179, 8)));
        return list;
    }

    /**
     * Gets religious sanctuaries and sacred ritual site centers by scenario identifier.
     *
     * @param type preset scenario identifier
     * @return list of sacred ritual site anchors
     */
    public static List<LanguageZone> getSacredSitesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Rome Imperial Cult", 12.5, 41.9, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Serapis & Isis", 29.9, 31.2, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Delphi Oracle", 22.5, 38.5, new Color(6, 182, 212)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Göbekli Tepe Megalithic Shrine", 38.9, 37.2, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Marduk Sanctuary", 44.4, 32.5, new Color(245, 158, 11)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Rift Valley Ancestral Sanctuary", 36.0, 4.5, new Color(234, 179, 8)));
                    break;
            }
        }
        list.add(new LanguageZone("Palaeolithic / Siberian Shamanism", 90.0, 55.0, new Color(99, 102, 241)));
        list.add(new LanguageZone("Megalithic Monolith Sanctuary", -3.0, 48.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Polytheist State Pantheon", 44.0, 32.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Vedic & Dharmic Sanctuaries", 78.0, 22.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("Ancestor Shrine Cults", 115.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Animist Spirit Sanctuaries", 5.0, 8.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Mesoamerican Solar Pantheons", -99.0, 19.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sahul Dreamtime Sacred Sites", 132.9, -12.5, new Color(168, 85, 247)));
        return list;
    }

    /**
     * Calculates equirectangular squared angular distance between two geographic coordinates.
     *
     * @param lng1 longitude 1 in degrees
     * @param lat1 latitude 1 in degrees
     * @param lng2 longitude 2 in degrees
     * @param lat2 latitude 2 in degrees
     * @return squared angular distance
     */
    public static double distSq(double lng1, double lat1, double lng2, double lat2) {
        double dLng = lng1 - lng2;
        if (dLng > 180.0) dLng -= 360.0;
        else if (dLng < -180.0) dLng += 360.0;
        double cosLat = Math.cos(Math.toRadians((lat1 + lat2) * 0.5));
        double dx = dLng * cosLat;
        double dy = lat1 - lat2;
        return dx * dx + dy * dy;
    }
}
