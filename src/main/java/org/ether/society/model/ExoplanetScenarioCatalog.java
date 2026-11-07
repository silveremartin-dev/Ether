/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
import java.util.List;
import java.util.Map;

/**
 * Canonical built-in scenario presets for Solar System colonization (Moon, Mars, Venus, Mercury, Titan)
 * and Exoplanetary civilizational configurations (Super-Earth, Eyeball World, Water World, Ice World, Archipelago).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see FutureAndExoplanetScenarioCatalog
 */
public final class ExoplanetScenarioCatalog {

    private ExoplanetScenarioCatalog() {
        // Static catalog utility class
    }

    /**
     * Appends all Solar System and Exoplanetary scenarios to the target collection.
     *
     * @param list target scenario collection
     */
    public static void appendExoplanetScenarios(List<Scenario> list) {
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
