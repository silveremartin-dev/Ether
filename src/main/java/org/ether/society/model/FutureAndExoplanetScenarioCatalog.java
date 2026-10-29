/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * <h1>FutureAndExoplanetScenarioCatalog</h1>
 * <p>
 * Canonical built-in scenario presets for Future Earth trajectories ($+2026\,\text{CE}$ to $+2150\,\text{CE}$),
 * Solar System colonization, and Exoplanetary civilizational configurations.<br>
 * Also orchestrates the unified {@link #getBuiltInScenarios()} assembly and {@link #resolvePresetKey(String, String)} lookup.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see Scenario
 * @see PaleoAndAncientScenarioCatalog
 * @see MedievalAndModernScenarioCatalog
 */
public final class FutureAndExoplanetScenarioCatalog {

    private FutureAndExoplanetScenarioCatalog() {
        // Static catalog utility class
    }

    /**
     * Resolves the canonical preset key for a scenario given its explicit key or display name.
     *
     * @param presetKey explicit preset key (may be null)
     * @param name      scenario name (may be null)
     * @return normalized preset key string, or {@code null} if unrecognized
     */
    public static String resolvePresetKey(String presetKey, String name) {
        if (presetKey != null && !presetKey.isBlank()) return presetKey;
        if (name == null) return null;
        String n = name.toLowerCase();
        if (n.contains("out of africa") || n.contains("sortie d'afrique")) return "out_of_africa";
        if (n.contains("toba")) return "toba_cataclysm_74k";
        if (n.contains("sahul")) return "sahul";
        if (n.contains("beringia") || n.contains("béringie")) return "beringia";
        if (n.contains("solutrean") || n.contains("solutréen") || n.contains("lgm")) return "lgm_solutrean";
        if (n.contains("dryas")) return "younger_dryas";
        if (n.contains("fertile crescent") || n.contains("croissant fertile")) return "fertile_crescent";
        if (n.contains("vert") || n.contains("green sahara")) return "green_sahara";
        if (n.contains("egypte") || n.contains("égypte") || n.contains("egypt")) return "ancient_egypt";
        if (n.contains("ramses") || n.contains("ramsès") || n.contains("ramesses")) return "ramesses_ii";
        if (n.contains("assyrian") || n.contains("assyrien")) return "assyrian_empire";
        if (n.contains("mesoamerica") || n.contains("mésoamérique") || n.contains("olmeques") || n.contains("olmèques")) return "mesoamerica";
        if (n.contains("bronze age collapse") || (n.contains("effondrement") && n.contains("bronze"))) return "bronze_age_collapse_1200bc";
        if (n.contains("early iron age") || n.contains("premier age du fer") || n.contains("premier âge du fer")) return "early_iron_age";
        if (n.contains("alexander") || n.contains("alexandre")) return "alexander_hellenistic_334bc";
        if (n.contains("maurya")) return "maurya_empire";
        if (n.contains("roman empire") || n.contains("empire romain")) return "roman_empire";
        if (n.contains("late antique") || n.contains("glaciaire antique") || n.contains("536")) return "late_antique_ice_age";
        if (n.contains("islamic") || n.contains("islamique")) return "islamic_expansion_632";
        if (n.contains("song")) return "song_dynasty";
        if (n.contains("mongol")) return "mongol_conquest_1206";
        if (n.contains("mali")) return "mali_empire";
        if (n.contains("black death") || n.contains("peste noire")) return "black_death_1347";
        if (n.contains("1491") || n.contains("americas") || n.contains("amériques")) return "americas_1491";
        if (n.contains("columbian") || n.contains("colombien")) return "columbian_contact";
        if (n.contains("sakoku") || n.contains("tokugawa")) return "tokugawa_japan";
        if (n.contains("industrial") || n.contains("industrielle")) return "industrial_1800";
        if (n.contains("totalitarian") || n.contains("totalitaire") || n.contains("1914")) return "world_wars_totalitarian_1914";
        if (n.contains("business as usual") || n.contains("tendancielle") || n.contains("2025") || n.contains("zero drilling") || n.contains("sans forage") || n.contains("moratoire")) return "earth_2025_business_as_usual";
        if (n.contains("ssp1") || n.contains("soutenable") || n.contains("sustainability") || n.contains("net-zero") || n.contains("net zero")) return "earth_2026_ssp1";
        if (n.contains("ssp3") || n.contains("world3") || n.contains("rivalry") || n.contains("fragmentation") || n.contains("limites")) return "earth_2026_ssp3";
        if (n.contains("ssp5") || n.contains("ssp5-8.5")) return "ssp5_85";
        if (n.contains("hiver nucleaire") || n.contains("hiver nucléaire") || n.contains("nuclear winter")) return "nuclear_winter_2035";
        if (n.contains("singularity") || n.contains("singularite") || n.contains("singularité")) return "singularity_2045";
        if (n.contains("phosphate")) return "peak_phosphate_2050";
        if (n.contains("supervolcan") || n.contains("supervolcano")) return "supervolcano_2060";
        if (n.contains("shackleton") || n.contains("marius") || n.contains("lune") || n.contains("moon")) return "moon_shackleton_2035";
        if (n.contains("mars")) return "mars_colony_2050";
        if (n.contains("venus") || n.contains("vénus") || n.contains("aerostat") || n.contains("aérostat") || n.contains("hesperos")) return "venus_cloud_cities_2080";
        if (n.contains("mercure") || n.contains("mercury") || n.contains("caloris") || n.contains("hermes")) return "mercury_caloris_forge_2120";
        if (n.contains("titan") || n.contains("kraken")) return "titan_cryo_methane_2150";
        if (n.contains("super-terre") || n.contains("super-earth") || n.contains("gaia")) return "super_earth_gaia_2200";
        if (n.contains("synchrone") || n.contains("eyeball") || n.contains("crépuscule") || n.contains("twilight")) return "eyeball_world_twilight_2220";
        if (n.contains("oceania") || n.contains("aquapolis") || n.contains("monde océan") || n.contains("water world")) return "oceania_aquapolis_2100";
        if (n.contains("boreas") || n.contains("glaciaire") || n.contains("ice world") || n.contains("subglacial")) return "boreas_subglacial_2120";
        if (n.contains("archipel") || n.contains("archipelago") || n.contains("seasteading")) return "archipelago_seasteading_2055";
        return null;
    }

    /**
     * Assembles and returns the complete list of canonical built-in scenarios sorted chronologically.
     *
     * @return chronologically sorted list of built-in {@link Scenario} instances
     */
    public static List<Scenario> getBuiltInScenarios() {
        List<Scenario> list = new ArrayList<>(PaleoAndAncientScenarioCatalog.createScenarios());
        MedievalAndModernScenarioCatalog.appendScenarios(list);
        appendScenarios(list);

        // Sort all canonical presets chronologically by start year
        list.sort(Comparator.comparingLong(Scenario::getStartDateYear));

        // Enforce precalculated map import mode for all canonical built-in scenarios
        for (Scenario sc : list) {
            if (sc.getTensorProceduralModes() == null || sc.getTensorProceduralModes().isEmpty()) {
                List<Boolean> modes = new ArrayList<>();
                int dims = sc.getCultureVectorDimensions() > 0 ? sc.getCultureVectorDimensions() : 9;
                for (int i = 0; i < dims; i++) {
                    modes.add(false); // false = Import of precalculated map
                }
                sc.setTensorProceduralModes(modes);
            }
        }

        return list;
    }

    /**
     * Appends all Future Earth, Solar System, and Exoplanet scenarios ($+2026$ onwards) to the target list.
     *
     * @param list target scenario collection
     */
    public static void appendScenarios(List<Scenario> list) {
        // --- SCÉNARIOS DU FUTUR ---

        // 1. Business-As-Usual & Continuité Tendancielle (SSP2 / UN WPP 2024 / IEA STEPS)
        Scenario sBau = new Scenario();
        sBau.setPresetKey("earth_2025_business_as_usual");
        sBau.setName("Trajectoire Tendancielle Globale (2025-2055) : Continuité Socio-Économique (SSP2)");
        sBau.setStartDateYear(2025);
        sBau.setEndDateYear(2055);
        sBau.setInitialHumanCount(8160000000L);
        sBau.setInitialCapitalPerCapita(16000.0);
        sBau.setInitialEnergyPerCapita(24000.0);
        sBau.setInitialFoodReserveMonths(10.0);
        sBau.setInitialInformationPerCapita(6000000.0);
        sBau.setPopulationDensityType("URBAN_CLUSTERS");
        sBau.setTargetCohortSize(10000);
        sBau.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sBau.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sBau.setDescription("""
            📈 SCÉNARIO FUTUR : Trajectoire Tendancielle Globale (SSP2 / Business-As-Usual)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Continuité socio-économique globale sans catastrophe nucléaire ni effondrement abrupt.
            Poursuite des tendances historiques d'urbanisation, d'efficacité énergétique incrémentale et de transition progressive.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Plateau démographique mondial atteignant ~9.8 milliards d'habitants vers 2055 (aligné UN WPP 2024).
            • Mix énergétique diversifié (essor renouvelable modéré avec persistance des hydrocarbures).
            • Pression hydrique sur les nappes phréatiques agricoles et adaptation progressive des infrastructures côtières.
            """);
        Map<String, Boolean> sBauEngines = sBau.getTypeBEngineStates();
        sBauEngines.put("KummelAyresExergyEngine", true);
        sBauEngines.put("SmilMaterialTransitionsPureEngine", true);
        sBauEngines.put("UrbanThermodynamicsEngine", true);
        sBauEngines.put("HotellingResourceDepletionEngine", true);
        sBauEngines.put("JevonsParadoxEngine", true);
        sBauEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        list.add(sBau);

        // 2. Transition Soutenable & Décarbonation Accélérée (SSP1 / Net-Zero 2050)
        Scenario sSsp1 = new Scenario();
        sSsp1.setPresetKey("earth_2026_ssp1");
        sSsp1.setName("Transition Soutenable & Décarbonation Accélérée (2026-2060) : Objectif Net-Zéro (SSP1)");
        sSsp1.setStartDateYear(2026);
        sSsp1.setEndDateYear(2060);
        sSsp1.setInitialHumanCount(8160000000L);
        sSsp1.setInitialCapitalPerCapita(18000.0);
        sSsp1.setInitialEnergyPerCapita(22000.0);
        sSsp1.setInitialFoodReserveMonths(12.0);
        sSsp1.setInitialInformationPerCapita(7000000.0);
        sSsp1.setPopulationDensityType("URBAN_CLUSTERS");
        sSsp1.setTargetCohortSize(10000);
        sSsp1.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSsp1.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sSsp1.setDescription("""
            🌱 SCÉNARIO FUTUR : Transition Écologique & Objectif Net-Zéro (SSP1)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Déploiement massif des technologies bas-carbone, électrification accélérée des transports,
            économie circulaire des métaux critiques et régénération des écosystèmes agricoles.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Pic des émissions mondiales avant 2030 et trajectoire de stabilisation thermique sous 1.8°C.
            • Baisse rapide de l'intensité énergétique par unité de PIB (efficacité exergétique Kuemmel-Ayres).
            • Préservation de la biodiversité terrestre et réduction de l'érosion des sols.
            """);
        Map<String, Boolean> sSsp1Engines = sSsp1.getTypeBEngineStates();
        sSsp1Engines.put("GeoengineeringAlbedoFeedbackEngine", true);
        sSsp1Engines.put("SmilMaterialTransitionsPureEngine", true);
        sSsp1Engines.put("ArthurCombinatorialTechnologyEngine", true);
        sSsp1Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        list.add(sSsp1);

        // 3. Tensions Énergétiques & Fragmentation Régionale (SSP3 / World3 BAU2)
        Scenario sSsp3 = new Scenario();
        sSsp3.setPresetKey("earth_2026_ssp3");
        sSsp3.setName("Tensions Énergétiques & Fragmentation Régionale (2026-2060) : Limites à la Croissance (SSP3 / World3)");
        sSsp3.setStartDateYear(2026);
        sSsp3.setEndDateYear(2060);
        sSsp3.setInitialHumanCount(8160000000L);
        sSsp3.setInitialCapitalPerCapita(14000.0);
        sSsp3.setInitialEnergyPerCapita(20000.0);
        sSsp3.setInitialFoodReserveMonths(6.0);
        sSsp3.setInitialInformationPerCapita(4000000.0);
        sSsp3.setPopulationDensityType("URBAN_CLUSTERS");
        sSsp3.setTargetCohortSize(10000);
        sSsp3.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSsp3.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sSsp3.setDescription("""
            ⚠️ SCÉNARIO FUTUR : Fragmentation Régionale & Limites à la Croissance (SSP3 / World3)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Baisse inexorable du rendement énergétique net (EROI) des gisements fossiles conventionnels,
            tensions géopolitiques sur les chaînes logistiques minérales et repli protectionniste des blocs régionaux.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Inflation énergétique mondiale et ralentissement de l'investissement dans le capital productif.
            • Vulnérabilité alimentaire accrue sous contrainte sur les engrais et le transport maritime.
            • Trajectoire de décélération industrielle alignée sur le modèle World3 (Herrington 2021).
            """);
        Map<String, Boolean> sSsp3Engines = sSsp3.getTypeBEngineStates();
        sSsp3Engines.put("World3HybridEngine", true);
        sSsp3Engines.put("HotellingResourceDepletionEngine", true);
        sSsp3Engines.put("OreGradeThermodynamicsEngine", true);
        sSsp3Engines.put("TainterComplexityCollapseEngine", true);
        list.add(sSsp3);

        // 4. Intensification Fossile SSP5-8.5
        Scenario s5 = new Scenario();
        s5.setPresetKey("ssp5_85");
        s5.setName("Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)");
        s5.setStartDateYear(2026);
        s5.setEndDateYear(2100);
        s5.setInitialHumanCount(8200000000L);
        s5.setInitialCapitalPerCapita(15000.0);
        s5.setInitialEnergyPerCapita(25000.0);
        s5.setInitialFoodReserveMonths(9.0);
        s5.setInitialInformationPerCapita(5000000.0);
        s5.setPopulationDensityType("URBAN_CLUSTERS");
        s5.setTargetCohortSize(10000);
        s5.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s5.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s5.setDescription("""
            📉 SCÉNARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans déploiement massif de la fusion ni captage du carbone.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Trajectoire de concentration CO₂ non régulée (>1000 ppm en 2100) et hausse thermique globale >4°C.
            • Submersion marine des métropoles côtières et stress thermique létal (température thermomètre mouillé Tw > 35°C).
            • Effondrement des rendements agricoles tropicaux et vagues de réfugiés climatiques vers les hautes latitudes.
            """);
        Map<String, Boolean> s5Engines = s5.getTypeBEngineStates();
        s5Engines.put("KummelAyresExergyEngine", true);
        s5Engines.put("SmilMaterialTransitionsPureEngine", true);
        s5Engines.put("World3HybridEngine", true);
        s5Engines.put("MarineSubmersionEngine", true);
        s5Engines.put("GeoengineeringAlbedoFeedbackEngine", true);
        s5Engines.put("UrbanThermodynamicsEngine", true);
        s5Engines.put("EcotoxicologyFertilityEngine", true);
        s5Engines.put("HotellingResourceDepletionEngine", true);
        s5Engines.put("OreGradeThermodynamicsEngine", true);
        s5Engines.put("JevonsParadoxEngine", true);
        list.add(s5);

        Scenario s7 = new Scenario();
        s7.setPresetKey("nuclear_winter_2035");
        s7.setName("Hiver Nucléaire & Ombre Stratosphérique (2035)");
        s7.setStartDateYear(2035);
        s7.setEndDateYear(2085);
        s7.setInitialHumanCount(8500000000L);
        s7.setInitialCapitalPerCapita(18000.0);
        s7.setInitialEnergyPerCapita(1500.0);
        s7.setInitialFoodReserveMonths(1.5);
        s7.setInitialInformationPerCapita(500000.0);
        s7.setPopulationDensityType("URBAN_CLUSTERS");
        s7.setTargetCohortSize(250);
        s7.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s7.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s7.setDescription("""
            ☢️ SCÉNARIO FUTUR : Catastrophe de la Guerre Nucléaire & Hiver Stratosphérique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Conflit nucléaire à haute intensité déclenchant d'immenses tempêtes de feu urbaines et l'injection massive de carbone suie dans la stratosphère.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale de l'insolation solaire au sol (-70%) et refroidissement planétaire de -15°C à -25°C.
            • Effondrement total de la photosynthèse et rupture généralisée des chaînes alimentaires en moins de 60 jours.
            • Survie démographique résiduelle restreinte aux refuges souterrains, biomes marins profonds et serres protégées.
            """);
        Map<String, Boolean> s7Engines = s7.getTypeBEngineStates();
        s7Engines.put("NuclearWarfareClimateEngine", true);
        s7Engines.put("NuclearSafetyRadiotoxicityEngine", true);
        s7Engines.put("BifurcationChaosEngine", true);
        s7Engines.put("VolcanicTephraRefugiaEngine", true);

        // Targeted Strategic Nuclear Strike Package (Anti-Forces & Anti-Cities)
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Washington D.C. & Pentagone", 2035, 38.88, -77.05, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Minot AFB (Dakota du Nord)", 2035, 48.41, -101.35, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Malmstrom AFB (Montana)", 2035, 47.50, -111.18, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Bunker : Complexe Cheyenne Mountain / NORAD", 2035, 38.74, -104.84, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Agglomération New York & Hub Maritime", 2035, 40.71, -74.00, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Mégalopole Los Angeles / Long Beach", 2035, 34.05, -118.24, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Moscou & Centre de Commandement", 2035, 55.75, 37.61, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Bombardiers Engels-2 (Saratov)", 2035, 51.48, 46.21, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Champs de Silos ICBM Kozelsk", 2035, 54.04, 35.80, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Chantiers Sous-Marins Severodvinsk", 2035, 64.56, 39.83, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Métropole Saint-Pétersbourg & Baltique", 2035, 59.93, 30.33, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Londres & Amirauté Britannique", 2035, 51.50, -0.12, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE Faslane / HMNB Clyde", 2035, 56.06, -4.81, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Paris & Commandement des Forces Aériennes", 2035, 48.85, 2.35, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE Île Longue (Brest)", 2035, 48.30, -4.50, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Pékin & Commission Militaire Centrale", 2035, 39.90, 116.40, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos DF-41 Yumen / Hami (Gansu)", 2035, 40.28, 97.04, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Mégalopole Industrielle Shanghai / Yangtze", 2035, 31.23, 121.47, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Tokyo & Complexe Industriel Kanto", 2035, 35.68, 139.69, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Stratégique Andersen (Guam)", 2035, 13.58, 144.92, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Aérienne Ramstein (OTAN)", 2035, 49.43, 7.60, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : New Delhi & Centres Stratégiques", 2035, 28.61, 77.20, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Islamabad & Complexe Nucléaire Kahuta", 2035, 33.68, 73.04, 0.0, 8.0));

        list.add(s7);

        Scenario s6 = new Scenario();
        s6.setPresetKey("singularity_2045");
        s6.setName("Singularité Technologique, ASI & Fusion D-T (2045)");
        s6.setStartDateYear(2045);
        s6.setEndDateYear(2100);
        s6.setInitialHumanCount(9000000000L);
        s6.setInitialCapitalPerCapita(50000.0);
        s6.setInitialEnergyPerCapita(100000.0);
        s6.setInitialFoodReserveMonths(24.0);
        s6.setInitialInformationPerCapita(100000000.0);
        s6.setPopulationDensityType("URBAN_CLUSTERS");
        s6.setTargetCohortSize(10000);
        s6.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s6.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s6.setDescription("""
            🤖 SCÉNARIO FUTUR : Singularité Technologique & Énergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Franchissement du seuil d'émergence d'une Super-Intelligence Artificielle (ASI) et maîtrise industrielle de la fusion nucléaire deutérium-tritium.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Croissance exponentielle des rendements de la recherche et automatisation intégrale du travail physique.
            • Déploiement massif de réacteurs à fusion deutérium-tritium conférant une abondance énergétique quasi-infinie.
            • Élimination des pénuries matérielles, transition vers l'échelle de Kardashev Type I et régulation systémique.
            """);
        Map<String, Boolean> s6Engines = s6.getTypeBEngineStates();
        s6Engines.put("TechnologicalSingularityEngine", true);
        s6Engines.put("AiAutonomousRegulationPureEngine", true);
        s6Engines.put("KurzweilAcceleratingReturnsEngine", true);
        s6Engines.put("KardashevPureEngine", true);
        s6Engines.put("ArthurCombinatorialTechnologyEngine", true);
        s6Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        list.add(s6);

        Scenario s8 = new Scenario();
        s8.setPresetKey("peak_phosphate_2050");
        s8.setName("Falaise du Phosphate Minéral & Crise N-P-K (2050)");
        s8.setStartDateYear(2050);
        s8.setEndDateYear(2150);
        s8.setInitialHumanCount(9500000000L);
        s8.setInitialCapitalPerCapita(22000.0);
        s8.setInitialEnergyPerCapita(12000.0);
        s8.setInitialFoodReserveMonths(4.0);
        s8.setInitialInformationPerCapita(2000000.0);
        s8.setPopulationDensityType("URBAN_CLUSTERS");
        s8.setTargetCohortSize(5000);
        s8.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s8.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s8.setDescription("""
            ⛏️ SCÉNARIO FUTUR : Épuisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Épuisement géologique complet des gisements de phosphate de roche bon marché sans transition vers un recyclage circulaire intégral.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute inexorable des rendements agricoles sous déficit de fertilisation phosphatée minérale (stœchiométrie N-P-K).
            • Flambée des prix alimentaires mondiaux, crises de famine urbaine et tensions géopolitiques autour des derniers gisements.
            • Re-localisation agricole d'urgence, baisse de la population vers la capacité de charge organique et recyclage des flux.
            """);
        Map<String, Boolean> s8Engines = s8.getTypeBEngineStates();
        s8Engines.put("World3HybridEngine", true);
        s8Engines.put("EcotoxicologyFertilityEngine", true);
        s8Engines.put("SmilMaterialTransitionsPureEngine", true);
        s8Engines.put("HotellingResourceDepletionEngine", true);
        s8Engines.put("OreGradeThermodynamicsEngine", true);
        s8Engines.put("TainterComplexityCollapseEngine", true);
        list.add(s8);

        Scenario s9 = new Scenario();
        s9.setPresetKey("supervolcano_2060");
        s9.setName("Super-Éruption Volcanique Toba/Yellowstone (2060)");
        s9.setStartDateYear(2060);
        s9.setEndDateYear(2110);
        s9.setInitialHumanCount(9800000000L);
        s9.setInitialCapitalPerCapita(25000.0);
        s9.setInitialEnergyPerCapita(20000.0);
        s9.setInitialFoodReserveMonths(3.0);
        s9.setInitialInformationPerCapita(5000000.0);
        s9.setPopulationDensityType("URBAN_CLUSTERS");
        s9.setTargetCohortSize(250);
        s9.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s9.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s9.setDescription("""
            🌋 SCÉNARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Éruption super-volcanique de degré VEI-8 éjectant plus de 1000 km³ de cendres et de dioxyde de soufre (SO₂) dans la haute atmosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T₀)]
            • Stock Capital Physique (K₀) : 25 000 kg/habitant (infrastructures avancées et serres automatisées).
            • Énergie Stockée (E₀) : 20 000 MJ/habitant (centrales nucléaires et géothermiques).
            • Réserves Alimentaires (F₀) : 3,0 mois (destructions agricoles par cendres).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (savoir automatisé & archives).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Hiver volcanique pluriannuel (baisse de 8°C à 12°C des températures globales pendant 5 à 10 ans).
            • Dépôts massifs de téphras toxiques détruisant les sols agricoles, les toitures urbaines et les réseaux électriques.
            • Déploiement d'une logistique de crise alimentaire basée sur les stocks stratégiques et les cultures protégées.
            """);
        Map<String, Boolean> s9Engines = s9.getTypeBEngineStates();
        s9Engines.put("VolcanicTephraRefugiaEngine", true);
        s9Engines.put("BifurcationChaosEngine", true);
        s9Engines.put("World3HybridEngine", true);
        list.add(s9);

        // --- EXOPLANETARY & FUTURE EXPANSION SCENARIOS (2050+) ---

        // 1. MOON (2035)
        Scenario sMoon = new Scenario();
        sMoon.setPresetKey("moon_shackleton_2035");
        sMoon.setName("Base Lunaire Shackleton & Tubes de Lave Marius Hills (2035)");
        sMoon.setStartDateYear(2035);
        sMoon.setEndDateYear(2085);
        sMoon.setInitialHumanCount(50L);
        sMoon.setInitialCapitalPerCapita(150000.0);
        sMoon.setInitialEnergyPerCapita(100000.0);
        sMoon.setInitialFoodReserveMonths(48.0);
        sMoon.setInitialInformationPerCapita(60000000.0);
        sMoon.setPopulationDensityType("URBAN_CLUSTERS");
        sMoon.setTargetCohortSize(10);
        sMoon.setPlanetPreset(PlanetPreset.MOON_LIKE);
        sMoon.setEcologyPreset(EcologyPreset.MOON_LIKE);
        sMoon.setDescription("""
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Base Lunaire Polaire & Tubes de Lave Sous-Terrain (2035 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Implantation humaine permanente au pôle Sud lunaire (cratère Shackleton) pour l'extraction de glace d'eau et sanctuarisation dans les tunnels de lave de Marius Hills sous vide absolu.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation industrielle de la glace polaire et de l'Hélium-3 pour les réacteurs à fusion.
            • Habitats pressurisés enfouis dans les cavités basaltiques assurant un blindage thermique et radiatif total.
            • Hub logistique pour l'expansion cis-lunaire et les chantiers orbitaux.
            """);
        Map<String, Boolean> sMoonEngines = sMoon.getTypeBEngineStates();
        sMoonEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMoonEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sMoonEngines.put("OreGradeThermodynamicsEngine", true);
        sMoonEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sMoon);

        // 2. MARS (2050)
        Scenario sMars = new Scenario();
        sMars.setPresetKey("mars_colony_2050");
        sMars.setName("Colonisation Martienne : Dômes Chryse & Cratère Jezero (2050)");
        sMars.setStartDateYear(2050);
        sMars.setEndDateYear(2150);
        sMars.setInitialHumanCount(500L);
        sMars.setInitialCapitalPerCapita(120000.0);
        sMars.setInitialEnergyPerCapita(80000.0);
        sMars.setInitialFoodReserveMonths(36.0);
        sMars.setInitialInformationPerCapita(50000000.0);
        sMars.setPopulationDensityType("URBAN_CLUSTERS");
        sMars.setTargetCohortSize(50);
        sMars.setPlanetPreset(PlanetPreset.MARS_LIKE);
        sMars.setEcologyPreset(EcologyPreset.MARS_LIKE);
        sMars.setDescription("""
            🔴 SCÉNARIO EXOPLANÉTAIRE : Colonisation Martienne & Dômes Sous Régolithe (2050 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Premiers habitats humains permanents au cratère Jezero et Chryse Planitia. Utilisation du cycle de Sabatier (méthanation CO₂), extraction d'eau in-situ, dômes imprimés 3D en basalte et support de vie ECLSS en boucle fermée face à une pression quasi-nulle (0,006 atm) et aux rayonnements ionisants.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement d'avant-postes pressurisés et croissance du stock de capital technique et d'énergie par habitant.
            • Autarcie progressive grâce à l'ISRU (In-Situ Resource Utilization) et aux technologies combinatoires.
            • Résilience face aux tempêtes de poussière martiennes et aux risques de brèche structurelle.
            """);
        Map<String, Boolean> sMarsEngines = sMars.getTypeBEngineStates();
        sMarsEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMarsEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMarsEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sMarsEngines.put("OreGradeThermodynamicsEngine", true);
        sMarsEngines.put("HotellingResourceDepletionEngine", true);
        list.add(sMars);

        // 3. VENUS (2080)
        Scenario sVenus = new Scenario();
        sVenus.setPresetKey("venus_cloud_cities_2080");
        sVenus.setName("Cités Flottantes Vénusiennes : Aérostats de Haute Altitude (2080)");
        sVenus.setStartDateYear(2080);
        sVenus.setEndDateYear(2180);
        sVenus.setInitialHumanCount(150L);
        sVenus.setInitialCapitalPerCapita(180000.0);
        sVenus.setInitialEnergyPerCapita(120000.0);
        sVenus.setInitialFoodReserveMonths(36.0);
        sVenus.setInitialInformationPerCapita(70000000.0);
        sVenus.setPopulationDensityType("URBAN_CLUSTERS");
        sVenus.setTargetCohortSize(25);
        sVenus.setPlanetPreset(PlanetPreset.VENUS_LIKE);
        sVenus.setEcologyPreset(EcologyPreset.VENUS_LIKE);
        sVenus.setDescription("""
            🟡 SCÉNARIO EXOPLANÉTAIRE : Aérostats Flottants & Extraction Atmosphérique Vénusienne (2080 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Cités aérostatiques flottantes à 55 km d'altitude où la pression est de 1,0 atm et la température de 25°C. Enveloppes en PTFE résistant à l'acide sulfurique et raffinage du carbone atmosphérique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation de la flottabilité naturelle de l'air respirable (N₂/O₂) comme gaz sustentateur dans une atmosphère de CO₂ dense.
            • Récolte de vapeur d'eau et de composés soufrés par condensation en haute altitude.
            • Équilibre aérostatique dynamique et résistance à la super-rotation zonale des vents.
            """);
        Map<String, Boolean> sVenusEngines = sVenus.getTypeBEngineStates();
        sVenusEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sVenusEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sVenusEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sVenus);

        // 4. MERCURY (2120)
        Scenario sMercury = new Scenario();
        sMercury.setPresetKey("mercury_caloris_forge_2120");
        sMercury.setName("Forge Solaire de Mercure & Bassin Caloris (2120)");
        sMercury.setStartDateYear(2120);
        sMercury.setEndDateYear(2220);
        sMercury.setInitialHumanCount(100L);
        sMercury.setInitialCapitalPerCapita(250000.0);
        sMercury.setInitialEnergyPerCapita(300000.0);
        sMercury.setInitialFoodReserveMonths(48.0);
        sMercury.setInitialInformationPerCapita(80000000.0);
        sMercury.setPopulationDensityType("URBAN_CLUSTERS");
        sMercury.setTargetCohortSize(20);
        sMercury.setPlanetPreset(PlanetPreset.MERCURY_LIKE);
        sMercury.setEcologyPreset(EcologyPreset.MERCURY_LIKE);
        sMercury.setDescription("""
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Forges Héliothermiques de Mercure & Métaux Lourds de Caloris (2120 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Avant-postes miniers souterrains exploitant les gisements géants de métaux denses du bassin Caloris et la concentration solaire extrême (10 kW/m²) pour faisceaux d'énergie orbitaux.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation du flux solaire paroxystique pour la métallurgie lourde et la synthèse de matériaux ultra-denses.
            • Habitats mobiles de terminateur ou cités souterraines profondes protégées des gradients thermiques jour/nuit extrêmes.
            • Exportation d'énergie et de métaux lourds vers le reste du système solaire par catapultes électromagnétiques.
            """);
        Map<String, Boolean> sMercuryEngines = sMercury.getTypeBEngineStates();
        sMercuryEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMercuryEngines.put("OreGradeThermodynamicsEngine", true);
        sMercuryEngines.put("HotellingResourceDepletionEngine", true);
        sMercuryEngines.put("KardashevPureEngine", true);
        list.add(sMercury);

        // 5. TITAN (2150)
        Scenario sTitan = new Scenario();
        sTitan.setPresetKey("titan_cryo_methane_2150");
        sTitan.setName("Colonie Cryogénique de Titan & Hydrocarbures Kraken Mare (2150)");
        sTitan.setStartDateYear(2150);
        sTitan.setEndDateYear(2250);
        sTitan.setInitialHumanCount(80L);
        sTitan.setInitialCapitalPerCapita(220000.0);
        sTitan.setInitialEnergyPerCapita(150000.0);
        sTitan.setInitialFoodReserveMonths(60.0);
        sTitan.setInitialInformationPerCapita(90000000.0);
        sTitan.setPopulationDensityType("URBAN_CLUSTERS");
        sTitan.setTargetCohortSize(250);
        sTitan.setPlanetPreset(PlanetPreset.TITAN_LIKE);
        sTitan.setEcologyPreset(EcologyPreset.TITAN_LIKE);
        sTitan.setDescription("""
            🪐 SCÉNARIO EXOPLANÉTAIRE : Cryocités de Titan & Mers d'Hydrocarbures de Kraken Mare (2150 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Habitats cryogéniques pressurisés au bord de Kraken Mare exploitant les mers de méthane/éthane liquide, l'épaisse couverture atmosphérique d'azote (1,45 atm) et les isotopes de fusion.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Richesse organique illimitée : chimie des tholins, plastiques et carburants de synthèse à coût quasi-nul.
            • Protection radiatif naturelle grâce à l'atmosphère épaisse (1,45 atm) sous très faible gravité (0,14g).
            • Utilisation de générateurs thermoélectriques nucléaires et de réacteurs à fusion deutérium pour le chauffage vital.
            """);
        Map<String, Boolean> sTitanEngines = sTitan.getTypeBEngineStates();
        sTitanEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sTitanEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sTitanEngines.put("KurzweilAcceleratingReturnsEngine", true);
        list.add(sTitan);

        // 6. SUPER-EARTH (2200)
        Scenario sSuperEarth = new Scenario();
        sSuperEarth.setPresetKey("super_earth_gaia_2200");
        sSuperEarth.setName("Arche Interstellaire : Première Colonisation de Gaia Prime (2200)");
        sSuperEarth.setStartDateYear(2200);
        sSuperEarth.setEndDateYear(2300);
        sSuperEarth.setInitialHumanCount(500000L);
        sSuperEarth.setInitialCapitalPerCapita(80000.0);
        sSuperEarth.setInitialEnergyPerCapita(90000.0);
        sSuperEarth.setInitialFoodReserveMonths(36.0);
        sSuperEarth.setInitialInformationPerCapita(100000000.0);
        sSuperEarth.setPopulationDensityType("URBAN_CLUSTERS");
        sSuperEarth.setTargetCohortSize(1000);
        sSuperEarth.setPlanetPreset(PlanetPreset.SUPER_EARTH);
        sSuperEarth.setEcologyPreset(EcologyPreset.SUPER_EARTH);
        sSuperEarth.setDescription("""
            🌍 SCÉNARIO EXOPLANÉTAIRE : Colonisation d'une Super-Terre à Forte Gravité (2200 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Arrivée d'une flotte d'arches générationnelles sur une Super-Terre massive (gravité 1,5g, pression 1,5 atm, biosphère foisonnante). Implantation modulaire haute résistance face aux contraintes tectoniques.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Adaptation biomécanique et exosquelettes face à une gravité élevée et une densité atmosphérique supérieure.
            • Exploitation d'une biomasse extraterrestre exubérante et de gisements géologiques super-abondants.
            • Essor de cités-dômes modulaires et émergence de nouvelles institutions coloniales autonomes.
            """);
        Map<String, Boolean> sSuperEarthEngines = sSuperEarth.getTypeBEngineStates();
        sSuperEarthEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sSuperEarthEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sSuperEarthEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sSuperEarthEngines.put("KurzweilAcceleratingReturnsEngine", true);
        list.add(sSuperEarth);

        // 7. EYEBALL WORLD (2220)
        Scenario sEyeball = new Scenario();
        sEyeball.setPresetKey("eyeball_world_twilight_2220");
        sEyeball.setName("Monde Synchrone : L'Anneau du Crépuscule & Terminateur (2220)");
        sEyeball.setStartDateYear(2220);
        sEyeball.setEndDateYear(2320);
        sEyeball.setInitialHumanCount(200000L);
        sEyeball.setInitialCapitalPerCapita(100000.0);
        sEyeball.setInitialEnergyPerCapita(150000.0);
        sEyeball.setInitialFoodReserveMonths(30.0);
        sEyeball.setInitialInformationPerCapita(80000000.0);
        sEyeball.setPopulationDensityType("URBAN_CLUSTERS");
        sEyeball.setTargetCohortSize(1000);
        sEyeball.setPlanetPreset(PlanetPreset.EYEBALL_WORLD);
        sEyeball.setEcologyPreset(EcologyPreset.EYEBALL_WORLD);
        sEyeball.setDescription("""
            👁️ SCÉNARIO EXOPLANÉTAIRE : Civilisation de l'Anneau Crépusculaire Synchrone (2220 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Colonisation d'une planète en verrouillage gravitationnel autour d'une naine rouge. Population concentrée exclusivement sur l'étroite bande tempérée du terminateur entre brasier permanent et nuit glacée.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Climat statique hyper-contrasté avec vents catabatiques puissants redistribuant l'énergie de la face éclairée vers la face sombre.
            • Ceinture de mégalopoles thermiquement isolées exploitant l'énergie solaire ininterrompue côté jour et les glaces permanentes côté nuit.
            • Équilibre précaire des flux hydrologiques et gestion rigoureuse de la capacité de charge locale.
            """);
        Map<String, Boolean> sEyeballEngines = sEyeball.getTypeBEngineStates();
        sEyeballEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sEyeballEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sEyeballEngines.put("World3HybridEngine", true);
        list.add(sEyeball);

        // 8. WATER WORLD / OCEANIA (2100)
        Scenario sOceania = new Scenario();
        sOceania.setPresetKey("oceania_aquapolis_2100");
        sOceania.setName("Monde Océan : Aquapolis & Cités Abyssales d'Oceania (2100)");
        sOceania.setStartDateYear(2100);
        sOceania.setEndDateYear(2200);
        sOceania.setInitialHumanCount(350000L);
        sOceania.setInitialCapitalPerCapita(90000.0);
        sOceania.setInitialEnergyPerCapita(80000.0);
        sOceania.setInitialFoodReserveMonths(36.0);
        sOceania.setInitialInformationPerCapita(75000000.0);
        sOceania.setPopulationDensityType("URBAN_CLUSTERS");
        sOceania.setTargetCohortSize(1000);
        sOceania.setPlanetPreset(PlanetPreset.WATER_WORLD);
        sOceania.setEcologyPreset(EcologyPreset.WATER_WORLD);
        sOceania.setDescription("""
            🌊 SCÉNARIO EXOPLANÉTAIRE : Planète Océan & Mégastructures Flottantes (2100 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Monde dépourvu de terres émergées couvert d'océans profonds de 10 km. Civilisation organisée en cités flottantes autonomes (Aquapolis), énergie thermique des mers (ETM) et forages hydrothermaux abyssaux.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Mariculture hyper-productive, fermes d'algues génétiquement optimisées et extraction de minéraux dissous.
            • Cités modulaires auto-lestées résistantes aux houles géantes et aux super-tempêtes océaniques.
            • Économie maritime décentralisée et réseaux de transport sous-marin automatisés.
            """);
        Map<String, Boolean> sOceaniaEngines = sOceania.getTypeBEngineStates();
        sOceaniaEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sOceaniaEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sOceaniaEngines.put("UrbanThermodynamicsEngine", true);
        list.add(sOceania);

        // 9. ICE WORLD / BOREAS (2120)
        Scenario sBoreas = new Scenario();
        sBoreas.setPresetKey("boreas_subglacial_2120");
        sBoreas.setName("Monde Glaciaire Boreas : Havres Géothermiques & Cités Sous Glace (2120)");
        sBoreas.setStartDateYear(2120);
        sBoreas.setEndDateYear(2220);
        sBoreas.setInitialHumanCount(100000L);
        sBoreas.setInitialCapitalPerCapita(110000.0);
        sBoreas.setInitialEnergyPerCapita(140000.0);
        sBoreas.setInitialFoodReserveMonths(40.0);
        sBoreas.setInitialInformationPerCapita(70000000.0);
        sBoreas.setPopulationDensityType("URBAN_CLUSTERS");
        sBoreas.setTargetCohortSize(500);
        sBoreas.setPlanetPreset(PlanetPreset.ICE_WORLD);
        sBoreas.setEcologyPreset(EcologyPreset.ICE_WORLD);
        sBoreas.setDescription("""
            ❄️ SCÉNARIO EXOPLANÉTAIRE : Cités Sous-Glaciaires & Puits Géothermiques de Boreas (2120 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Planète boule de neige cryogénique où l'humanité s'abrite dans des cavités volcaniques sous-glaciaires chauffées par le flux géothermique du manteau sous des kilomètres de banquise.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation du gradient thermique entre la croûte volcanique profonde et la calotte de surface.
            • Cités troglodytiques taillées dans la glace consolidée et la roche ignée avec biosphères hydroponiques étanches.
            • Économie d'énergie thermique maximale et recyclage thermodynamique intégral.
            """);
        Map<String, Boolean> sBoreasEngines = sBoreas.getTypeBEngineStates();
        sBoreasEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sBoreasEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sBoreasEngines.put("HotellingResourceDepletionEngine", true);
        list.add(sBoreas);

        // 10. ARCHIPELAGO / SEASTEADING (2055)
        Scenario sArchipelago = new Scenario();
        sArchipelago.setPresetKey("archipelago_seasteading_2055");
        sArchipelago.setName("Archipel : Confédération Océanique & Seasteading Autonome (2055)");
        sArchipelago.setStartDateYear(2055);
        sArchipelago.setEndDateYear(2150);
        sArchipelago.setInitialHumanCount(2500000L);
        sArchipelago.setInitialCapitalPerCapita(45000.0);
        sArchipelago.setInitialEnergyPerCapita(50000.0);
        sArchipelago.setInitialFoodReserveMonths(18.0);
        sArchipelago.setInitialInformationPerCapita(40000000.0);
        sArchipelago.setPopulationDensityType("URBAN_CLUSTERS");
        sArchipelago.setTargetCohortSize(5000);
        sArchipelago.setPlanetPreset(PlanetPreset.ARCHIPELAGO);
        sArchipelago.setEcologyPreset(EcologyPreset.ARCHIPELAGO);
        sArchipelago.setDescription("""
            🏝️ SCÉNARIO FUTUR : Micro-États Insulaires & Réseaux de Seasteading (2055 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Monde fragmenté en milliers d'atolls tropicaux et de plates-formes artificielles de seasteading reliées par hydroptères solaires et gouvernance polycentrique décentralisée.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Forte résilience institutionnelle décentralisée et innovation polycentrique (théorie d'Elinor Ostrom).
            • Mix énergétique 100% renouvelable combinant énergie houlomotrice, solaire flottant et biocarburants marins.
            • Équilibre écologique entre préservation des récifs coralliens et densité urbaine insulaire.
            """);
        Map<String, Boolean> sArchipelagoEngines = sArchipelago.getTypeBEngineStates();
        sArchipelagoEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sArchipelagoEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sArchipelagoEngines.put("UrbanThermodynamicsEngine", true);
        sArchipelagoEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        list.add(sArchipelago);
    }
}
