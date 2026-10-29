/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
import java.util.List;
import java.util.Map;

/**
 * <h1>MedievalAndModernScenarioCatalog</h1>
 * <p>
 * Canonical built-in scenario presets spanning Late Antiquity, the Medieval Period, the Early Modern Era,
 * Industrialization, the Anthropocene, and Present-Day Business-As-Usual trajectories ($+536\,\text{CE}$ to $+2025\,\text{CE}$).<br>
 * Extracted from {@link Scenario} to enforce the Single Responsibility Principle and modular maintainability.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see Scenario
 */
public final class MedievalAndModernScenarioCatalog {

    private MedievalAndModernScenarioCatalog() {
        // Static catalog utility class
    }

    /**
     * Appends all canonical Medieval, Early Modern, Industrial, and Anthropocene scenarios ($+536$ to $+2025$) to the target list.
     *
     * @param list target scenario collection
     */
    public static void appendScenarios(List<Scenario> list) {
        Scenario s2 = new Scenario();
        s2.setPresetKey("late_antique_ice_age");
        s2.setName("Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)");
        s2.setStartDateYear(536);
        s2.setEndDateYear(650);
        s2.setInitialHumanCount(180000000);
        s2.setInitialCapitalPerCapita(250.0);
        s2.setInitialEnergyPerCapita(100.0);
        s2.setInitialFoodReserveMonths(1.5);
        s2.setInitialInformationPerCapita(300.0);
        s2.setPopulationDensityType("URBAN_CLUSTERS");
        s2.setTargetCohortSize(1500);
        s2.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s2.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s2.setDescription("""
            🌋 SCÉNARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'année 536 est considérée par les historiens du climat comme "la pire année de l'histoire humaine". Deux éruptions volcaniques super-massives consécutives ont injecté un voile d'aérosols stratosphériques occultant le Soleil pendant 18 mois.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale des températures estivales (-2°C à -3°C) et effondrement des récoltes céréalières eurasiennes.
            • Propagation fulgurante de la Peste de Justinien (Yersinia pestis) le long des routes de commerce maritime.
            • Déstabilisation militaire des frontières impériales byzantines et grandes migrations de peuples des steppes.
            """);
        Map<String, Boolean> s2Engines = s2.getTypeBEngineStates();
        s2Engines.put("BioMolecularEpidemiologyEngine", true);
        s2Engines.put("SpatialMetapopulationSEIREngine", true);
        s2Engines.put("FrontierAsabiyyahEngine", true);
        s2Engines.put("MonasticDemographicBufferEngine", true);
        s2Engines.put("TurchinGoldstoneSDTEngine", true);
        s2Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s2Engines.put("TainterComplexityCollapseEngine", true);
        s2Engines.put("MaritimeHighwayEngine", true);
        s2Engines.put("DynamicMaritimeRoutingGraph", true);
        list.add(s2);

        // --- SCÉNARIO : EXPANSION ISLAMIQUE & RÉVOLUTION COMMERCIALE CALIFALE (632) ---
        Scenario sIslam = new Scenario();
        sIslam.setPresetKey("islamic_expansion_632");
        sIslam.setName("Expansion Islamique & Révolution Commerciale Califale (632)");
        sIslam.setStartDateYear(632);
        sIslam.setEndDateYear(900);
        sIslam.setInitialHumanCount(70000000);
        sIslam.setInitialCapitalPerCapita(320.0);
        sIslam.setInitialEnergyPerCapita(140.0);
        sIslam.setInitialFoodReserveMonths(6.0);
        sIslam.setInitialInformationPerCapita(500.0);
        sIslam.setPopulationDensityType("URBAN_CLUSTERS");
        sIslam.setTargetCohortSize(1000);
        sIslam.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sIslam.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sIslam.setDescription("""
            🌙 SCÉNARIO HISTORIQUE : Unification Califale & Révolution Agricole Arabe (632 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'expansion foudroyante des Califats Omeyyade et Abbasside de l'Espagne à l'Indus. Effondrement de l'Empire Sassanide, diffusion massive des techniques d'irrigation (qanats, norias) et des cultures tropicales (canne à sucre, coton, agrumes), essor des réseaux maritimes de l'Océan Indien.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse d'unification géopolitique portée par la haute Asabiyyah bédouine et la doctrine califale.
            • Révolution agricole arabe : diversification agronomique et maîtrise hydraulique des zones arides.
            • Essor de Bagdad comme métropole mondiale (Maison de la Sagesse) et intégration commerciale transcontinentale.
            """);
        Map<String, Boolean> sIslamEngines = sIslam.getTypeBEngineStates();
        sIslamEngines.put("FrontierAsabiyyahEngine", true);
        sIslamEngines.put("HydrologicalEngineeringEngine", true);
        sIslamEngines.put("DynamicMaritimeRoutingGraph", true);
        sIslamEngines.put("MaritimeHighwayEngine", true);
        sIslamEngines.put("SpatialCityFractalEngine", true);
        sIslamEngines.put("KrugmanCorePeripheryEngine", true);
        sIslamEngines.put("OstromCommonsPureEngine", true);
        sIslamEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sIslamEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sIslam);

        Scenario s4 = new Scenario();
        s4.setPresetKey("song_dynasty");
        s4.setName("Dynastie Song & Pré-Industrialisation Hydraulique (1000)");
        s4.setStartDateYear(1000);
        s4.setEndDateYear(1279);
        s4.setInitialHumanCount(100000000);
        s4.setInitialCapitalPerCapita(600.0);
        s4.setInitialEnergyPerCapita(500.0);
        s4.setInitialFoodReserveMonths(8.0);
        s4.setInitialInformationPerCapita(1200.0);
        s4.setPopulationDensityType("RIVER_VALLEYS");
        s4.setTargetCohortSize(2000);
        s4.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s4.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s4.setDescription("""
            🏮 SCÉNARIO HISTORIQUE : Le Siècle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la première pré-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des réseaux de transport fluviaux ultra-efficaces.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Révolution énergétique pré-industrielle : transition massive vers le charbon minéral et hauts-fourneaux au coke.
            • Révolution monétaire et commerciale : première monnaie fiduciaire en papier (Jiaozi) et navigation à la boussole.
            • Hyper-urbanisation fluviale (Kaifeng, Hangzhou) et vulnérabilité militaire face aux cavaliers nomades du Nord.
            """);
        Map<String, Boolean> s4Engines = s4.getTypeBEngineStates();
        s4Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s4Engines.put("TurchinGoldstoneSDTEngine", true);
        s4Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        s4Engines.put("MilitaryTechShockEngine", true);
        s4Engines.put("DynamicMaritimeRoutingGraph", true);
        s4Engines.put("MaritimeHighwayEngine", true);
        s4Engines.put("SpatialCityFractalEngine", true);
        s4Engines.put("HydrologicalEngineeringEngine", true);
        s4Engines.put("KrugmanCorePeripheryEngine", true);
        s4Engines.put("ArthurCombinatorialTechnologyEngine", true);
        s4Engines.put("GranovetterThresholdCascadeEngine", true);
        s4Engines.put("WestBettencourtAllometryEngine", true);
        list.add(s4);

        // --- SCÉNARIO : L'EMPIRE MONGOL & LA GRANDE RUPTURE EURASIENNE (1206) ---
        Scenario sMongol = new Scenario();
        sMongol.setPresetKey("mongol_conquest_1206");
        sMongol.setName("L'Empire Mongol & la Grande Rupture Eurasienne (1206)");
        sMongol.setStartDateYear(1206);
        sMongol.setEndDateYear(1368);
        sMongol.setInitialHumanCount(110000000);
        sMongol.setInitialCapitalPerCapita(400.0);
        sMongol.setInitialEnergyPerCapita(250.0);
        sMongol.setInitialFoodReserveMonths(5.0);
        sMongol.setInitialInformationPerCapita(600.0);
        sMongol.setPopulationDensityType("EURASIA_STEPPE");
        sMongol.setTargetCohortSize(1500);
        sMongol.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMongol.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sMongol.setDescription("""
            🏹 SCÉNARIO HISTORIQUE : Conquête Mongole de Gengis Khan & Pax Mongolica (1206 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le plus grand empire territorial contigu de l'histoire humaine. Choc cinétique et démographique majeur en Asie centrale, Perse et Chine, suivi de l'unification sécurisée de la Route de la Soie (Pax Mongolica) qui servira de vecteur à la Peste Noire.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Projection militaire nomade ultra-rapide (cavalerie légère, réseau de relais de poste Yam).
            • Choc de mortalité urbaine et désertification de certaines oasis irriguées de Perse/Transoxiane.
            • Intégration économique trans-eurasienne de Pékin à Tabriz et la mer Noire.
            """);
        Map<String, Boolean> sMongolEngines = sMongol.getTypeBEngineStates();
        sMongolEngines.put("MilitaryTechShockEngine", true);
        sMongolEngines.put("FrontierAsabiyyahEngine", true);
        sMongolEngines.put("TurchinGoldstoneSDTEngine", true);
        sMongolEngines.put("KrugmanCorePeripheryEngine", true);
        sMongolEngines.put("LanchesterKineticWarfareEngine", true);
        sMongolEngines.put("SpatialMetapopulationSEIREngine", true);
        sMongolEngines.put("BioMolecularEpidemiologyEngine", true);
        list.add(sMongol);

        // --- SCÉNARIO : EMPIRE DU MALI (1324) ---
        Scenario sMali = new Scenario();
        sMali.setPresetKey("mali_empire");
        sMali.setName("Empire du Mali & Commerce Trans-Saharien (1324)");
        sMali.setStartDateYear(1324);
        sMali.setEndDateYear(1591);
        sMali.setInitialHumanCount(12000000);
        sMali.setInitialCapitalPerCapita(250.0);
        sMali.setInitialEnergyPerCapita(120.0);
        sMali.setInitialFoodReserveMonths(6.0);
        sMali.setInitialInformationPerCapita(400.0);
        sMali.setPopulationDensityType("WEST_AFRICA_MALI");
        sMali.setTargetCohortSize(500);
        sMali.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMali.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sMali.setClippingEnabled(true);
        sMali.setH3Resolution(5);
        sMali.setMinLat(5.0); sMali.setMaxLat(25.0); sMali.setMinLng(-18.0); sMali.setMaxLng(15.0);
        sMali.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMali.setDescription("""
            🕌 SCÉNARIO HISTORIQUE : L'Apogée de l'Empire du Mali sous Mansa Musa (1324 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le réseau urbain et marchand trans-saharien de la boucle du Niger (Tombouctou, Gao, Djenné). Contrôle des mines d'or de Bambouk/Boure et des salines de Teghaza.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Flux caravaniers trans-sahariens de métaux précieux (or) contre sel gemme, textiles et manuscrits.
            • Essor académique et théologique de Tombouctou (Université Sankoré) et sédentarisation sahélienne.
            • Choc monétaire mondial provoqué par les dépenses d'or de Mansa Musa lors de son pèlerinage au Caire.
            """);
        Map<String, Boolean> sMaliEngines = sMali.getTypeBEngineStates();
        sMaliEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMaliEngines.put("TurchinGoldstoneSDTEngine", true);
        sMaliEngines.put("FrontierAsabiyyahEngine", true);
        sMaliEngines.put("AsymmetricColonialTradeEngine", true);
        sMaliEngines.put("SpatialCityFractalEngine", true);
        sMaliEngines.put("OstromCommonsPureEngine", true);
        sMaliEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMaliEngines.put("GranovetterThresholdCascadeEngine", true);
        list.add(sMali);

        // --- SCÉNARIO : LA PESTE NOIRE & INVERSION POST-FÉODALE (1347) ---
        Scenario sBlackDeath = new Scenario();
        sBlackDeath.setPresetKey("black_death_1347");
        sBlackDeath.setName("La Peste Noire & Inversion Économique Post-Féodale (1347)");
        sBlackDeath.setStartDateYear(1347);
        sBlackDeath.setEndDateYear(1450);
        sBlackDeath.setInitialHumanCount(440000000L);
        sBlackDeath.setInitialCapitalPerCapita(680.0);
        sBlackDeath.setInitialEnergyPerCapita(150.0);
        sBlackDeath.setInitialFoodReserveMonths(4.0);
        sBlackDeath.setInitialInformationPerCapita(450.0);
        sBlackDeath.setPopulationDensityType("URBAN_CLUSTERS");
        sBlackDeath.setTargetCohortSize(2000);
        sBlackDeath.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sBlackDeath.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sBlackDeath.setDescription("""
            💀 SCÉNARIO HISTORIQUE : La Pandémie de Peste Noire & Inversion du Rapport Capital/Travail (1347 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Vague pandémique foudroyante de Yersinia pestis anéantissant 35% à 60% de la population européenne et moyen-orientale en moins de 5 ans. Choc démographique provoquant la rareté soudaine de la main-d'œuvre, la hausse spectaculaire des salaires réels et l'effondrement du servage.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de contagion métapopulationnelle le long des routes de commerce génoises et vénitiennes.
            • Inversion institutionnelle : émancipation paysanne en Europe occidentale vs second servage à l'Est.
            • Déprise agricole temporaire, reforestation spontanée et baisse des rentes foncières seigneuriales.
            """);
        Map<String, Boolean> sBDEngines = sBlackDeath.getTypeBEngineStates();
        sBDEngines.put("SpatialMetapopulationSEIREngine", true);
        sBDEngines.put("BioMolecularEpidemiologyEngine", true);
        sBDEngines.put("TurchinGoldstoneSDTEngine", true);
        sBDEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sBDEngines.put("GranovetterThresholdCascadeEngine", true);
        sBDEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sBDEngines.put("DynamicMaritimeRoutingGraph", true);
        list.add(sBlackDeath);

        // --- SCÉNARIO : AMÉRIQUES PRÉCOLOMBIENNES (1491) ---
        Scenario sAmericas1491 = new Scenario();
        sAmericas1491.setPresetKey("americas_1491");
        sAmericas1491.setName("Amériques Précolombiennes : Tawantinsuyu & Anahuac (1491)");
        sAmericas1491.setStartDateYear(1491);
        sAmericas1491.setEndDateYear(1650);
        sAmericas1491.setInitialHumanCount(60000000);
        sAmericas1491.setInitialCapitalPerCapita(220.0);
        sAmericas1491.setInitialEnergyPerCapita(150.0);
        sAmericas1491.setInitialFoodReserveMonths(6.0);
        sAmericas1491.setInitialInformationPerCapita(250.0);
        sAmericas1491.setPopulationDensityType("AMERICAS_1491");
        sAmericas1491.setTargetCohortSize(1000);
        sAmericas1491.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sAmericas1491.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sAmericas1491.setClippingEnabled(true);
        sAmericas1491.setH3Resolution(5);
        sAmericas1491.setMinLat(-45.0); sAmericas1491.setMaxLat(30.0); sAmericas1491.setMinLng(-110.0); sAmericas1491.setMaxLng(-35.0);
        sAmericas1491.setBoundaryMode("DYNAMIC_RESERVOIR");
        sAmericas1491.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Les Amériques à la Veille du Contact (1491 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise les grands empires précolombiens (Empire Inca du Tawantinsuyu, Empire Aztèque de la Triple Alliance) et les sociétés Mississippiennes avant la rupture épidémique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Optimisation écologique des paysages anthropiques (terra preta amazonienne, terrasses andines, canaux chinampas).
            • Densités urbaines précolombiennes culminantes dans les bassins de Mexico (Tenochtitlan) et de Cuzco.
            • Absence d'immunité croisée et vulnérabilité maximale face aux pathogènes de l'Ancien Monde.
            """);
        Map<String, Boolean> sAmericas1491Engines = sAmericas1491.getTypeBEngineStates();
        sAmericas1491Engines.put("BoserupAgriculturalIntensificationEngine", true);
        sAmericas1491Engines.put("TurchinGoldstoneSDTEngine", true);
        sAmericas1491Engines.put("AmerindianEcosystemEngine", true);
        sAmericas1491Engines.put("HydrologicalEngineeringEngine", true);
        sAmericas1491Engines.put("SpatialCityFractalEngine", true);
        sAmericas1491Engines.put("OstromCommonsPureEngine", true);
        sAmericas1491Engines.put("TainterComplexityCollapseEngine", true);
        sAmericas1491Engines.put("DeforestationErosionEngine", true);
        list.add(sAmericas1491);

        // --- SCÉNARIO : CHOC DU CONTACT PRÉCOLOMBIEN (1492) ---
        Scenario sColumbian = new Scenario();
        sColumbian.setPresetKey("columbian_contact");
        sColumbian.setName("Arrivée des Européens aux Amériques & Choc Microbiens (1492)");
        sColumbian.setStartDateYear(1492);
        sColumbian.setEndDateYear(1650);
        sColumbian.setInitialHumanCount(60000000);
        sColumbian.setInitialCapitalPerCapita(250.0);
        sColumbian.setInitialEnergyPerCapita(160.0);
        sColumbian.setInitialFoodReserveMonths(5.0);
        sColumbian.setInitialInformationPerCapita(300.0);
        sColumbian.setPopulationDensityType("COLUMBIAN_CONTACT");
        sColumbian.setTargetCohortSize(1000);
        sColumbian.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sColumbian.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sColumbian.setDescription("""
            ⛵ SCÉNARIO HISTORIQUE : Le Choc du Contact d'Échange Colombien & Effondrement Épidémique (1492)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact bio-démographique mondial de la rencontre entre l'Ancien et le Nouveau Monde. Trajectoire de choc microbiologique (chute démographique de 80-90% du continent américain) et réorganisation commerciale transatlantique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute démographique cataclysmique (80 à 90%) des populations amérindiennes sous l'effet de la variole et de la rougeole.
            • Échange colombien global : diffusion mondiale du maïs, de la pomme de terre, du manioc et de l'argent du Potosí.
            • Reforestation spontanée des terres abandonnées et baisse temporaire du CO₂ atmosphérique mondial (Orbis Spike).
            """);
        Map<String, Boolean> sColumbianEngines = sColumbian.getTypeBEngineStates();
        sColumbianEngines.put("BioMolecularEpidemiologyEngine", true);
        sColumbianEngines.put("SpatialMetapopulationSEIREngine", true);
        sColumbianEngines.put("AsymmetricColonialTradeEngine", true);
        sColumbianEngines.put("DynamicMaritimeRoutingGraph", true);
        sColumbianEngines.put("MaritimeHighwayEngine", true);
        sColumbianEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sColumbianEngines.put("AmerindianEcosystemEngine", true);
        list.add(sColumbian);

        // --- SCÉNARIO : JAPON EDO & SAKOKU (1639) ---
        Scenario sSakoku = new Scenario();
        sSakoku.setPresetKey("tokugawa_japan");
        sSakoku.setName("Japon Tokugawa & Isolement Sakoku (1639)");
        sSakoku.setStartDateYear(1639);
        sSakoku.setEndDateYear(1853);
        sSakoku.setInitialHumanCount(27000000);
        sSakoku.setInitialCapitalPerCapita(450.0);
        sSakoku.setInitialEnergyPerCapita(200.0);
        sSakoku.setInitialFoodReserveMonths(8.0);
        sSakoku.setInitialInformationPerCapita(800.0);
        sSakoku.setPopulationDensityType("JAPAN_SAKOKU");
        sSakoku.setTargetCohortSize(500);
        sSakoku.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSakoku.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sSakoku.setClippingEnabled(true);
        sSakoku.setH3Resolution(5);
        sSakoku.setMinLat(30.0); sSakoku.setMaxLat(45.0); sSakoku.setMinLng(128.0); sSakoku.setMaxLng(146.0);
        sSakoku.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSakoku.setDescription("""
            ⛩️ SCÉNARIO HISTORIQUE : L'Ère d'Isolement Autarcique Tokugawa (Sakoku, 1639 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Fermeture des frontières de l'archipel japonais décrétée par le Shogunat Tokugawa. Modélise une économie circulaire hautement autarcique, l'urbanisation géante d'Edo (Tokyo, 1 million d'habitants) et l'absence d'intrants extérieurs jusqu'à l'arrivée des bateaux noirs du Commandant Perry en 1853.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Stabilité démographique et gestion rigoureuse des ressources forestières et agricoles en boucle fermée.
            • Modèle de croissance zéro durable sans dépendance aux importations énergétiques ou minérales extérieures.
            • Urbanisation pacifiée d'Edo et floraison artisanale et culturelle sous autarcie stricte.
            """);
        Map<String, Boolean> sSakokuEngines = sSakoku.getTypeBEngineStates();
        sSakokuEngines.put("EdoJapanIsolationEngine", true);
        sSakokuEngines.put("OstromCommonsPureEngine", true);
        sSakokuEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sSakokuEngines.put("TurchinGoldstoneSDTEngine", true);
        sSakokuEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sSakokuEngines.put("SpatialCityFractalEngine", true);
        sSakokuEngines.put("DeforestationErosionEngine", true);
        list.add(sSakoku);

        // --- SCÉNARIO : RÉVOLUTION INDUSTRIELLE (1800) ---
        Scenario sIndustrial1800 = new Scenario();
        sIndustrial1800.setPresetKey("industrial_1800");
        sIndustrial1800.setName("Révolution Industrielle & Transition Charbonnière (1800)");
        sIndustrial1800.setStartDateYear(1800);
        sIndustrial1800.setEndDateYear(1900);
        sIndustrial1800.setInitialHumanCount(900000000);
        sIndustrial1800.setInitialCapitalPerCapita(1200.0);
        sIndustrial1800.setInitialEnergyPerCapita(1500.0);
        sIndustrial1800.setInitialFoodReserveMonths(6.0);
        sIndustrial1800.setInitialInformationPerCapita(15000.0);
        sIndustrial1800.setPopulationDensityType("INDUSTRIAL_1800");
        sIndustrial1800.setTargetCohortSize(5000);
        sIndustrial1800.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sIndustrial1800.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sIndustrial1800.setDescription("""
            SCÉNARIO HISTORIQUE : La Machine à Vapeur & L'Émergence du Charbon (1800 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Basculement énergétique mondial du régime organique vers le régime minéral fossile (charbon de terre, machine à vapeur de Watt).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Rupture du piège malthusien : découplage entre croissance démographique et contrainte surfacique des sols.
            • Paradoxe de Jevons : les gains d'efficacité des machines à vapeur décuplent la consommation globale de charbon.
            • Urbanisation industrielle ultra-rapide (exode rural) et amorce des émissions anthropiques massives de CO₂.
            """);
        Map<String, Boolean> sIndustrial1800Engines = sIndustrial1800.getTypeBEngineStates();
        sIndustrial1800Engines.put("KummelAyresExergyEngine", true);
        sIndustrial1800Engines.put("SmilMaterialTransitionsPureEngine", true);
        sIndustrial1800Engines.put("JevonsParadoxEngine", true);
        sIndustrial1800Engines.put("EntropicMetalDissipationEngine", true);
        sIndustrial1800Engines.put("ProtestantWorkEthicEngine", true);
        sIndustrial1800Engines.put("UrbanThermodynamicsEngine", true);
        sIndustrial1800Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        sIndustrial1800Engines.put("ArthurCombinatorialTechnologyEngine", true);
        sIndustrial1800Engines.put("WestBettencourtAllometryEngine", true);
        sIndustrial1800Engines.put("KrugmanCorePeripheryEngine", true);
        sIndustrial1800Engines.put("HotellingResourceDepletionEngine", true);
        list.add(sIndustrial1800);

        // --- SCÉNARIO : GUERRES MONDIALES, RUPTURE TOTALITAIRE & ÈRE NUCLÉAIRE (1914) ---
        Scenario sWW = new Scenario();
        sWW.setPresetKey("world_wars_totalitarian_1914");
        sWW.setName("Guerres Mondiales, Rupture Totalitaire & Ère Nucléaire (1914)");
        sWW.setStartDateYear(1914);
        sWW.setEndDateYear(1960);
        sWW.setInitialHumanCount(1750000000L);
        sWW.setInitialCapitalPerCapita(3500.0);
        sWW.setInitialEnergyPerCapita(4500.0);
        sWW.setInitialFoodReserveMonths(5.0);
        sWW.setInitialInformationPerCapita(80000.0);
        sWW.setPopulationDensityType("URBAN_CLUSTERS");
        sWW.setTargetCohortSize(8000);
        sWW.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sWW.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sWW.setDescription("""
            ⚔️ SCÉNARIO HISTORIQUE : Guerre Industrielle Totale, Ruptures Idéologiques & Bombe Atomique (1914 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la période de crise systémique paroxystique de la modernité industrielle (1914-1945). Mobilisation intégrale de l'exergie fossile et chimique (synthèse Haber-Bosch), ruptures totalitaires (1917, 1933), destruction massive de capital en Europe/Asie, et franchissement du seuil de destruction thermonucléaire (1945).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement de la guerre industrielle et de la puissance de feu cinétique (artillerie lourde, aviation, blindés).
            • Ruptures de régime idéologiques extrêmes (collectivisme soviétique, militarisme fasciste) et purges démographiques.
            • Chute brutale du capital en 1939-1945 suivie de la reconstruction fordiste accélérée des Trente Glorieuses.
            """);
        Map<String, Boolean> sWWEngines = sWW.getTypeBEngineStates();
        sWWEngines.put("LanchesterKineticWarfareEngine", true);
        sWWEngines.put("SmilMaterialTransitionsPureEngine", true);
        sWWEngines.put("KummelAyresExergyEngine", true);
        sWWEngines.put("TurchinGoldstoneSDTEngine", true);
        sWWEngines.put("BifurcationChaosEngine", true);
        sWWEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sWWEngines.put("NuclearSafetyRadiotoxicityEngine", true);
        sWWEngines.put("JevonsParadoxEngine", true);
        sWWEngines.put("WestBettencourtAllometryEngine", true);
        list.add(sWW);

        // --- SCÉNARIO : ANTHROPOCÈNE (2000) ---
        Scenario sModern2000 = new Scenario();
        sModern2000.setPresetKey("anthropocene_2000");
        sModern2000.setName("Anthropocène & Grande Accélération Mondiale (2000)");
        sModern2000.setStartDateYear(2000);
        sModern2000.setEndDateYear(2100);
        sModern2000.setInitialHumanCount(6127000000L);
        sModern2000.setInitialCapitalPerCapita(12000.0);
        sModern2000.setInitialEnergyPerCapita(20000.0);
        sModern2000.setInitialFoodReserveMonths(8.0);
        sModern2000.setInitialInformationPerCapita(2500000.0);
        sModern2000.setPopulationDensityType("URBAN_CLUSTERS");
        sModern2000.setTargetCohortSize(10000);
        sModern2000.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sModern2000.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sModern2000.setDescription("""
            🌐 SCÉNARIO HISTORIQUE : L'Ère Numérique & La Grande Accélération (2000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Consolidation du système économique mondial interconnecté, essor des microprocesseurs en silicium, de l'Internet mondial et de l'urbanisation globale.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Grande Accélération : croissance exponentielle des flux de matière, d'énergie fossile et d'information numérique.
            • Franchissement des limites planétaires (Cycle de l'azote/phosphore, forçage radiatif CO₂, érosion de biodiversité).
            • Rendements décroissants de la complexité institutionnelle et fragilité des chaînes logistiques just-in-time.
            """);
        Map<String, Boolean> sModern2000Engines = sModern2000.getTypeBEngineStates();
        sModern2000Engines.put("KummelAyresExergyEngine", true);
        sModern2000Engines.put("SmilMaterialTransitionsPureEngine", true);
        sModern2000Engines.put("World3HybridEngine", true);
        sModern2000Engines.put("EcotoxicologyFertilityEngine", true);
        sModern2000Engines.put("UrbanThermodynamicsEngine", true);
        sModern2000Engines.put("KurzweilAcceleratingReturnsEngine", true);
        sModern2000Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        sModern2000Engines.put("ArthurCombinatorialTechnologyEngine", true);
        sModern2000Engines.put("WestBettencourtAllometryEngine", true);
        sModern2000Engines.put("KrugmanCorePeripheryEngine", true);
        sModern2000Engines.put("HotellingResourceDepletionEngine", true);
        sModern2000Engines.put("OreGradeThermodynamicsEngine", true);
        sModern2000Engines.put("JevonsParadoxEngine", true);
        sModern2000Engines.put("EntropicMetalDissipationEngine", true);
        sModern2000Engines.put("BioMolecularEpidemiologyEngine", true);
        list.add(sModern2000);

        // --- SCÉNARIO DU FUTUR : TRAJECTOIRE TENDANCIELLE BIOPHYSIQUE 2025-2055 (BUSINESS AS USUAL) ---
        Scenario sEarthFutureBAU = new Scenario();
        sEarthFutureBAU.setPresetKey("business_as_usual");
        sEarthFutureBAU.setName("Terre 2025-2055 : Trajectoire Tendancielle Biophysique (Business As Usual)");
        sEarthFutureBAU.setStartDateYear(2025);
        sEarthFutureBAU.setEndDateYear(2055);
        sEarthFutureBAU.setInitialHumanCount(8100000000L);
        sEarthFutureBAU.setInitialCapitalPerCapita(24000.0);
        sEarthFutureBAU.setInitialEnergyPerCapita(18000.0);
        sEarthFutureBAU.setInitialFoodReserveMonths(6.0);
        sEarthFutureBAU.setInitialInformationPerCapita(10000000.0);
        sEarthFutureBAU.setPopulationDensityType("URBAN_CLUSTERS");
        sEarthFutureBAU.setTargetCohortSize(5000);
        sEarthFutureBAU.setH3Resolution(4);
        sEarthFutureBAU.setTemporalResolutionDays(30.0);
        sEarthFutureBAU.setPlanetPreset(PlanetPreset.EARTH_MODERN);
        sEarthFutureBAU.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sEarthFutureBAU.setDescription("""
            🌐 SCÉNARIO FUTUR : Trajectoire Tendancielle Biophysique & Ressources Réelles (2025-2055)
            
            [DESCRIPTION DES CONDITIONS INITIALES & FRICTIONS (T₀)]
            Modélisation de la poursuite de l'histoire humaine à partir de l'état macro-physique réel de 2025 sans présumer d'aucun moratoire normatif ni plafond artificiel. Intègre les frictions géopolitiques contemporaines de transit (détroit d'Ormuz, mer Rouge, sanctions d'Europe orientale), la déplétion cumulative des gisements conventionnels (~48% du pétrole déjà extrait, transition vers le schiste et l'offshore profond), et le taux d'apprentissage industriel des énergies bas-carbone.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Extraction continue des combustibles fossiles et des minerais gouvernée par la thermodynamique d'abaissement des teneurs (Bihouix) et la rente d'Hotelling.
            • Ajustement endogène de l'exergue utile (Kümmel-Ayres) entre capital fossile et déploiement spontané des renouvelables/nucléaire.
            • Évolution non-contrainte des émissions de CO₂, forçage radiatif continu et rétroactions climatiques sur les rendements agricoles et le stress hydrique.
            """);
        Map<String, Boolean> sEarthFutureEngines = sEarthFutureBAU.getTypeBEngineStates();
        sEarthFutureEngines.put("KummelAyresExergyEngine", true);
        sEarthFutureEngines.put("SmilMaterialTransitionsPureEngine", true);
        sEarthFutureEngines.put("HotellingResourceDepletionEngine", true);
        sEarthFutureEngines.put("RenewableEnergyPhysicsEngine", true);
        sEarthFutureEngines.put("NetEnergyEROEIEngine", true);
        sEarthFutureEngines.put("PhysicalEnergyGridEngine", true);
        sEarthFutureEngines.put("ResourceRecyclingEngine", true);
        sEarthFutureEngines.put("JevonsParadoxEngine", true);
        sEarthFutureEngines.put("World3HybridEngine", true);
        sEarthFutureEngines.put("OreGradeThermodynamicsEngine", true);
        list.add(sEarthFutureBAU);
    }
}
