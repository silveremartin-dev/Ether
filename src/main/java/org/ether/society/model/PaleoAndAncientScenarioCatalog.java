/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Catalog of Paleolithic, Neolithic, Bronze Age, Iron Age, and Classical Antiquity built-in scenarios
 * (\(-100\,000\text{ BP}\) to \(+476\text{ AD}\)).
 *
 * <p>Extracted from {@link Scenario} to adhere to the Single Responsibility Principle (SRP)
 * and separate historical preset data definitions from the domain entity state model.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PaleoAndAncientScenarioCatalog {

    private PaleoAndAncientScenarioCatalog() {}

    /**
     * Creates the official reference Benchmark Standard Scenario.
     *
     * @return standardized benchmark scenario instance
     */
    public static Scenario createBenchmarkStandardScenario() {
        Scenario s = new Scenario();
        s.setPresetKey("benchmark_standard");
        s.setName("Benchmark Standard Planetary Scenario");
        s.setDescription("Official Ether standardized benchmark scenario for empirical throughput, latency, and scaling validation across spatial resolutions (Res 2-7), temporal steps (1d-365d), and compute backends.");
        s.setStartDateYear(-10000);
        s.setEndDateYear(-5000);
        s.setH3Resolution(3);
        s.setTemporalResolutionDays(30.0);
        s.setInitialHumanCount(100_000);
        s.setInitialCapitalPerCapita(10.0);
        s.setInitialEnergyPerCapita(20.0);
        s.setInitialFoodReserveMonths(6.0);
        s.setInitialInformationPerCapita(5.0);
        s.setPopulationDensityType("GLOBAL_EXPONENTIAL");
        s.setTargetCohortSize(150);
        s.setStrictDeterminism(true);
        s.setParallelExecutionEnabled(true);
        s.setParallelThreadCount(Runtime.getRuntime().availableProcessors());
        s.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP.withResolution(3));
        s.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        return s;
    }

    /**
     * Populates Paleolithic, Neolithic, Bronze Age, Iron Age, and Classical Antiquity scenarios.
     *
     * @return list of ancient historical scenarios
     */
    public static List<Scenario> createScenarios() {
        List<Scenario> list = new ArrayList<>();

        // --- BENCHMARK REFERENCE SCENARIO ---
        list.add(createBenchmarkStandardScenario());

        // --- SCÉNARIOS DU PASSÉ ---
        Scenario s0 = new Scenario();
        s0.setPresetKey("out_of_africa");
        s0.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        s0.setStartDateYear(-100000);
        s0.setEndDateYear(-20000);
        s0.setInitialHumanCount(50000);
        s0.setInitialCapitalPerCapita(2.0);
        s0.setInitialEnergyPerCapita(5.0);
        s0.setInitialFoodReserveMonths(2.0);
        s0.setInitialInformationPerCapita(2.0);
        s0.setPopulationDensityType("ONE_CONTINENT");
        s0.setTargetCohortSize(30);
        s0.setPlanetPreset(PlanetPreset.EARTH_LIG_100000BP);
        s0.setEcologyPreset(EcologyPreset.EARTH_LIG_100000BP);
        s0.setDescription("""
            🌍 SCÉNARIO PALÉOLITHIQUE : Berceau Africain, Traversée des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.
            
            [CONDITIONS INITIALES PHYSIQUES (T₀)]
            • Population Initiale : 50 000 individus (Capacité nomade pré-agricole).
            • Stock Capital Physique (K₀) : 2 kg/habitant (bifaces en pierre, javelots).
            • Énergie Stockée (E₀) : 5 MJ/habitant (maîtrise du feu et combustible bois).
            • Réserves Alimentaires (F₀) : 2 mois de subsistance en chasse-cueillette.
            • Savoir Archivé (I₀) : 2 bits/habitant (traditions orales paléolithiques & langage).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de dispersion géographique vers le Proche-Orient, l'Asie du Sud et l'Europe.
            • Survie démographique nomade face aux glaciations et évènements de Dansgaard-Oeschger.
            • Dérive linguistique paléolithique et innovations lithiques (Levallois, emmanchement à la résine).
            """);
        Map<String, Boolean> s0Engines = s0.getTypeBEngineStates();
        s0Engines.put("PaleoLanguageDriftEngine", true);
        s0Engines.put("KarstCaveShelterEngine", true);
        s0Engines.put("LithicTradeProvenanceEngine", true);
        s0Engines.put("OchreTanningTechnologyEngine", true);
        s0Engines.put("CoastalMarineRefugiaEngine", true);
        s0Engines.put("ResinHaftingAdhesivesEngine", true);
        s0Engines.put("FireHardenedSpearEngine", true);
        s0Engines.put("GeophyteDiggingStickEngine", true);
        s0Engines.put("LevalloisPreparedCoreEngine", true);
        s0Engines.put("AcheuleanBifaceSymmetryEngine", true);
        s0Engines.put("OldowanMarrowPercussionEngine", true);
        s0Engines.put("HomininCompetitiveExclusionEngine", true);
        s0Engines.put("DemographicLifeTableEngine", true);
        s0Engines.put("ToolKitMaintenanceEngine", true);
        s0Engines.put("SelfDomesticationEngine", true);
        s0Engines.put("TasmanianCulturalRegressionEngine", true);
        s0Engines.put("KinSelectionHamiltonEngine", true);
        list.add(s0);

        // --- SCÉNARIO : CATACLYSME DU SUPERVOLCAN TOBA (-74000) ---
        Scenario sToba = new Scenario();
        sToba.setPresetKey("toba_cataclysm_74k");
        sToba.setName("Cataclysme du Supervolcan Toba & Goulot Démographique (-74000)");
        sToba.setStartDateYear(-74000);
        sToba.setEndDateYear(-50000);
        sToba.setInitialHumanCount(100000);
        sToba.setInitialCapitalPerCapita(2.0);
        sToba.setInitialEnergyPerCapita(4.0);
        sToba.setInitialFoodReserveMonths(1.0);
        sToba.setInitialInformationPerCapita(2.0);
        sToba.setPopulationDensityType("SPARSE");
        sToba.setTargetCohortSize(30);
        sToba.setPlanetPreset(PlanetPreset.EARTH_MIS3_50000BP);
        sToba.setEcologyPreset(EcologyPreset.EARTH_MIS3_50000BP);
        sToba.setDescription("""
            🌋 SCÉNARIO PALÉOCLIMATIQUE : Super-Éruption du Mont Toba & Hiver Volcanique (-74 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact biosphérique de la super-éruption du Toba (Sumatra, VEI-8) ayant éjecté 2 800 km³ de téphras. Chute thermique globale de 3 à 5°C pendant plusieurs années, créant un goulot d'étranglement génétique majeur chez Homo sapiens.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Goulot d'étranglement démographique sévère (population reproductive mondiale réduite à quelques milliers d'individus).
            • Réfuges écologiques côtiers en Afrique australe et en Inde méridionale.
            • Rebond démographique et innovations techniques lithiques post-crise (Mode 3 / Middle Stone Age).
            """);
        Map<String, Boolean> sTobaEngines = sToba.getTypeBEngineStates();
        sTobaEngines.put("VolcanicTephraRefugiaEngine", true);
        sTobaEngines.put("BifurcationChaosEngine", true);
        sTobaEngines.put("DemographicLifeTableEngine", true);
        sTobaEngines.put("HomininCompetitiveExclusionEngine", true);
        sTobaEngines.put("TasmanianCulturalRegressionEngine", true);
        sTobaEngines.put("KinSelectionHamiltonEngine", true);
        sToba.getClimateEvents().add(new ClimateEvent("VOLCANIC_ERUPTION", "Super-Éruption VEI-8 du Mont Toba", -74000, 2.58, 98.83, -5.0, 9.5));
        list.add(sToba);

        // --- SCÉNARIO : SAHUL (-50000) ---
        Scenario sSahul = new Scenario();
        sSahul.setPresetKey("sahul");
        sSahul.setName("Sahul & Premier Peuplement de l'Australie (-50000)");
        sSahul.setStartDateYear(-50000);
        sSahul.setEndDateYear(-10000);
        sSahul.setInitialHumanCount(30000);
        sSahul.setInitialCapitalPerCapita(3.0);
        sSahul.setInitialEnergyPerCapita(6.0);
        sSahul.setInitialFoodReserveMonths(2.0);
        sSahul.setInitialInformationPerCapita(3.0);
        sSahul.setPopulationDensityType("AUSTRALIA_SAHUL");
        sSahul.setTargetCohortSize(35);
        sSahul.setPlanetPreset(PlanetPreset.EARTH_MIS3_50000BP);
        sSahul.setEcologyPreset(EcologyPreset.EARTH_MIS3_50000BP);
        sSahul.setClippingEnabled(true);
        sSahul.setH3Resolution(5);
        sSahul.setMinLat(-42.0); sSahul.setMaxLat(-10.0); sSahul.setMinLng(112.0); sSahul.setMaxLng(155.0);
        sSahul.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSahul.setDescription("""
            🦘 SCÉNARIO PALÉOLITHIQUE : Traversée Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Premier franchissement maritime majeur de la ligne de Wallace par les ancêtres des Aborigènes d'Australie. Modélise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-Guinée réunies) et l'adaptation aux écosystèmes arides.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Franchissement maritime de la ligne de Wallace et navigation côtière insulaire.
            • Adaptation aux régimes arides intérieurs et gestion des paysages par brûlis (fire-stick farming).
            • Stabilité démographique à long terme et réseaux d'alliances à l'ocre à travers le désert.
            """);
        Map<String, Boolean> sSahulEngines = sSahul.getTypeBEngineStates();
        sSahulEngines.put("PaleoLanguageDriftEngine", true);
        sSahulEngines.put("KarstCaveShelterEngine", true);
        sSahulEngines.put("ResinHaftingAdhesivesEngine", true);
        sSahulEngines.put("CoastalMarineRefugiaEngine", true);
        sSahulEngines.put("RiverCanoeTransportEngine", true);
        sSahulEngines.put("AridOasisWellDiggingEngine", true);
        sSahulEngines.put("FireStickFarmingEngine", true);
        sSahulEngines.put("AridWaterStorageStashEngine", true);
        sSahulEngines.put("OchreTradeAllianceEngine", true);
        sSahulEngines.put("ArchaicIntrogressionEngine", true);
        sSahulEngines.put("TasmanianCulturalRegressionEngine", true);
        sSahulEngines.put("MegafaunaEcosystemEngine", true);
        sSahulEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sSahul);

        // --- SCÉNARIO : BÉRINGIE & PEUPLEMENT DES AMÉRIQUES (-25000) ---
        Scenario sBeringia = new Scenario();
        sBeringia.setPresetKey("beringia");
        sBeringia.setName("Béringie & Peuplement des Amériques (-25000)");
        sBeringia.setStartDateYear(-25000);
        sBeringia.setEndDateYear(-10000);
        sBeringia.setInitialHumanCount(15000);
        sBeringia.setInitialCapitalPerCapita(3.0);
        sBeringia.setInitialEnergyPerCapita(6.0);
        sBeringia.setInitialFoodReserveMonths(2.0);
        sBeringia.setInitialInformationPerCapita(4.0);
        sBeringia.setPopulationDensityType("BERINGIA_AMERICAS");
        sBeringia.setTargetCohortSize(40);
        sBeringia.setPlanetPreset(PlanetPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setEcologyPreset(EcologyPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setClippingEnabled(true);
        sBeringia.setH3Resolution(5);
        sBeringia.setMinLat(45.0); sBeringia.setMaxLat(75.0); sBeringia.setMinLng(140.0); sBeringia.setMaxLng(-120.0);
        sBeringia.setBoundaryMode("DYNAMIC_RESERVOIR");
        sBeringia.setDescription("""
            🏔️ SCÉNARIO PALÉOLITHIQUE : Le Pont Terrestre de Béringie & Incursion Américaine (-25 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'isolation des populations paléolithiques sur le pont terrestre de Béringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion à travers le corridor libre de glace et la route côtière du Pacifique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Temps de pause/stase génétique sur le pont de Béringie (Beringian Standstill).
            • Franchissement du corridor libre de glace ou de la route côtière du varech (Kelp Highway).
            • Taux d'expansion démographique rapide vers l'Amérique du Nord puis du Sud.
            """);
        Map<String, Boolean> sBeringiaEngines = sBeringia.getTypeBEngineStates();
        sBeringiaEngines.put("TailoredClothingThermalEngine", true);
        sBeringiaEngines.put("EyedNeedleSewingEngine", true);
        sBeringiaEngines.put("MeatCuringReservesEngine", true);
        sBeringiaEngines.put("OsseousIndustryCarvingEngine", true);
        sBeringiaEngines.put("MammothBoneHabitationEngine", true);
        sBeringiaEngines.put("PermafrostColdCacheEngine", true);
        sBeringiaEngines.put("LithicTradeProvenanceEngine", true);
        sBeringiaEngines.put("BeringianStandstillIsolationEngine", true);
        sBeringiaEngines.put("PressureFlakerPointEngine", true);
        sBeringiaEngines.put("IvoryHotWaterStraighteningEngine", true);
        sBeringiaEngines.put("PeriglacialLoessDustEngine", true);
        sBeringiaEngines.put("TasmanianCulturalRegressionEngine", true);
        sBeringiaEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sBeringia);

        // --- SCÉNARIO : DERNIER MAXIMUM GLACIAIRE & SOLUTRÉEN (-20000) ---
        Scenario sLGM = new Scenario();
        sLGM.setPresetKey("lgm_solutrean");
        sLGM.setName("Dernier Maximum Glaciaire & Solutréen (-20000)");
        sLGM.setStartDateYear(-20000);
        sLGM.setEndDateYear(-12000);
        sLGM.setInitialHumanCount(20000);
        sLGM.setInitialCapitalPerCapita(4.0);
        sLGM.setInitialEnergyPerCapita(7.0);
        sLGM.setInitialFoodReserveMonths(2.5);
        sLGM.setInitialInformationPerCapita(5.0);
        sLGM.setPopulationDensityType("LGM_REFUGIA");
        sLGM.setTargetCohortSize(40);
        sLGM.setPlanetPreset(PlanetPreset.EARTH_LGM_20000BP);
        sLGM.setEcologyPreset(EcologyPreset.EARTH_LGM_20000BP);
        sLGM.setDescription("""
            ❄️ SCÉNARIO PALÉOLITHIQUE : Apogée Glaciaire & Refuges Solutréens (-20 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le paroxysme du Dernier Maximum Glaciaire (LGM) avec un niveau marin abaissé de 120 mètres (exposant le Doggerland, le Sundaland, le Sahul et la Béringie), les inlandsis massifs (Laurentide, Fennoscandie) et l'industrie lithique foliacée solutréenne.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Concentration des populations dans les refuges thermiques (péninsule Ibérique, zone franco-cantabrique, Balkans).
            • Maîtrise technologique du froid extrême (vêtements ajustés à l'aiguille à chas, fosses-congélateurs périglaciaires).
            • Réexpansion démographique post-glaciaire rapide lors du réchauffement de Bølling-Allerød.
            """);
        Map<String, Boolean> sLGMEngines = sLGM.getTypeBEngineStates();
        sLGMEngines.put("PressureFlakerPointEngine", true);
        sLGMEngines.put("TailoredClothingThermalEngine", true);
        sLGMEngines.put("EyedNeedleSewingEngine", true);
        sLGMEngines.put("MeatCuringReservesEngine", true);
        sLGMEngines.put("PermafrostColdCacheEngine", true);
        sLGMEngines.put("TasmanianCulturalRegressionEngine", true);
        sLGMEngines.put("MarrowFatRenderingEngine", true);
        sLGMEngines.put("SnowTroughRefrigerationEngine", true);
        sLGMEngines.put("IvoryHotWaterStraighteningEngine", true);
        sLGMEngines.put("CaveWallClaySealingEngine", true);
        sLGMEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sLGM);

        // --- SCÉNARIO : RÉCENTS DRYAS (-10900) ---
        Scenario sYoungerDryas = new Scenario();
        sYoungerDryas.setPresetKey("younger_dryas");
        sYoungerDryas.setName("Le Récents Dryas & Choc Climatique Natufien (-10900)");
        sYoungerDryas.setStartDateYear(-10900);
        sYoungerDryas.setEndDateYear(-9500);
        sYoungerDryas.setInitialHumanCount(40000);
        sYoungerDryas.setInitialCapitalPerCapita(4.0);
        sYoungerDryas.setInitialEnergyPerCapita(8.0);
        sYoungerDryas.setInitialFoodReserveMonths(2.5);
        sYoungerDryas.setInitialInformationPerCapita(8.0);
        sYoungerDryas.setPopulationDensityType("YOUNGER_DRYAS");
        sYoungerDryas.setTargetCohortSize(50);
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        sYoungerDryas.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        sYoungerDryas.setDescription("""
            ❄️ SCÉNARIO PALÉOCLIMATIQUE : Le Récents Dryas & Pression Foragère Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale des rendements foragers de céréales sauvages au Levant suite à l'aridification.
            • Émergence des premiers hameaux sédentaires natoufiens et stockage intensif des grains en silos.
            • Pression sélective poussant à la domestication du chien et aux premiers semis intentionnels.
            """);
        Map<String, Boolean> sYoungerDryasEngines = sYoungerDryas.getTypeBEngineStates();
        sYoungerDryasEngines.put("WildCerealGrindingEngine", true);
        sYoungerDryasEngines.put("TopographicGameDriveEngine", true);
        sYoungerDryasEngines.put("AtlatlArcheryBallisticsEngine", true);
        sYoungerDryasEngines.put("PassiveSnareSmallGameEngine", true);
        sYoungerDryasEngines.put("EpipaleolithicStorageHamletEngine", true);
        sYoungerDryasEngines.put("SeasonalAggregationSanctuaryEngine", true);
        sYoungerDryasEngines.put("BisonCliffJumpDriveEngine", true);
        sYoungerDryasEngines.put("CanidDomesticationEngine", true);
        sYoungerDryasEngines.put("BasaltGrindingSlabEngine", true);
        sYoungerDryasEngines.put("OstromCommonsPureEngine", true);
        sYoungerDryasEngines.put("TasmanianCulturalRegressionEngine", true);
        list.add(sYoungerDryas);

        // --- SCÉNARIO : CROISSANT FERTILE (-8000) ---
        Scenario s1 = new Scenario();
        s1.setPresetKey("fertile_crescent");
        s1.setName("Croissant Fertile & Néolithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setEndDateYear(-5000);
        s1.setInitialHumanCount(25000);
        s1.setInitialCapitalPerCapita(5.0);
        s1.setInitialEnergyPerCapita(10.0);
        s1.setInitialFoodReserveMonths(3.0);
        s1.setInitialInformationPerCapita(5.0);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        s1.setTargetCohortSize(150);
        s1.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        s1.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        s1.setClippingEnabled(true);
        s1.setH3Resolution(5);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Explosion démographique liée aux surplus agricoles céréaliers (engrain, amidonnier, orge).
            • Émergence des premières cités-États mésopotamiennes, chefferies et différenciation sociale.
            • Dégradation environnementale précoce (déforestation des piémonts, érosion des sols, début de salinisation).
            """);
        Map<String, Boolean> s1Engines = s1.getTypeBEngineStates();
        s1Engines.put("WildCerealGrindingEngine", true);
        s1Engines.put("EpipaleolithicStorageHamletEngine", true);
        s1Engines.put("BasaltGrindingSlabEngine", true);
        s1Engines.put("CanidDomesticationEngine", true);
        s1Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s1Engines.put("ScottAgainstTheGrainPureEngine", true);
        s1Engines.put("OstromCommonsPureEngine", true);
        s1Engines.put("DeforestationErosionEngine", true);
        list.add(s1);

        // --- SCÉNARIO : SAHARA VERT (-6000) ---
        Scenario sGreenSahara = new Scenario();
        sGreenSahara.setPresetKey("green_sahara");
        sGreenSahara.setName("Le Sahara Vert & Période Humide Africaine (-6000)");
        sGreenSahara.setStartDateYear(-6000);
        sGreenSahara.setEndDateYear(-3500);
        sGreenSahara.setInitialHumanCount(60000);
        sGreenSahara.setInitialCapitalPerCapita(6.0);
        sGreenSahara.setInitialEnergyPerCapita(12.0);
        sGreenSahara.setInitialFoodReserveMonths(4.0);
        sGreenSahara.setInitialInformationPerCapita(10.0);
        sGreenSahara.setPopulationDensityType("GREEN_SAHARA");
        sGreenSahara.setTargetCohortSize(60);
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_MH_6000BP);
        sGreenSahara.setEcologyPreset(EcologyPreset.EARTH_MH_6000BP);
        sGreenSahara.setClippingEnabled(true);
        sGreenSahara.setH3Resolution(5);
        sGreenSahara.setMinLat(0.0); sGreenSahara.setMaxLat(42.0); sGreenSahara.setMinLng(-20.0); sGreenSahara.setMaxLng(45.0);
        sGreenSahara.setBoundaryMode("DYNAMIC_RESERVOIR");
        sGreenSahara.setDescription("""
            🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la Période Humide Africaine (AHP) où l'insolation printanière amplifiée par l'orbite terrestre a intensifié la mousson africaine. Le désert du Sahara était alors une savane verdoyante parsemée de lac majeurs (Lac Méga-Tchad), peuplée d'éleveurs néolithiques et de chasseurs-cueilleurs.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Densité humaine élevée et corridor trans-saharien autour du Lac Méga-Tchad et des oueds.
            • Économie mixte : pastoralisme bovin néolithique, chasse et pêche pélagique lacustre.
            • Migration et repli massif des populations vers la vallée du Nil lors de la désertification vers -3500.
            """);
        Map<String, Boolean> sGreenSaharaEngines = sGreenSahara.getTypeBEngineStates();
        sGreenSaharaEngines.put("LakeChadWadiMigrationEngine", true);
        sGreenSaharaEngines.put("AridWaterStorageStashEngine", true);
        sGreenSaharaEngines.put("PelagicFishingHookEngine", true);
        sGreenSaharaEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sGreenSaharaEngines.put("OstromCommonsPureEngine", true);
        sGreenSaharaEngines.put("CulturalSociologyEngine", true);
        list.add(sGreenSahara);

        // --- SCÉNARIO : ÉGYPTE ANTIQUE (-3000) ---
        Scenario sEgypt = new Scenario();
        sEgypt.setPresetKey("ancient_egypt");
        sEgypt.setName("Égypte Antique & Vallée du Nil (-3000)");
        sEgypt.setStartDateYear(-3000);
        sEgypt.setEndDateYear(-1000);
        sEgypt.setInitialHumanCount(1500000);
        sEgypt.setInitialCapitalPerCapita(60.0);
        sEgypt.setInitialEnergyPerCapita(40.0);
        sEgypt.setInitialFoodReserveMonths(6.0);
        sEgypt.setInitialInformationPerCapita(40.0);
        sEgypt.setPopulationDensityType("EGYPT_NILE");
        sEgypt.setTargetCohortSize(300);
        sEgypt.setPlanetPreset(PlanetPreset.EARTH_LH_3000BP);
        sEgypt.setEcologyPreset(EcologyPreset.EARTH_LH_3000BP);
        sEgypt.setClippingEnabled(true);
        sEgypt.setH3Resolution(5);
        sEgypt.setMinLat(21.0); sEgypt.setMaxLat(32.0); sEgypt.setMinLng(24.0); sEgypt.setMaxLng(36.0);
        sEgypt.setBoundaryMode("DYNAMIC_RESERVOIR");
        sEgypt.setDescription("""
            𓀀 SCÉNARIO HISTORIQUE : Unification Thinite & Crues du Nil (-3000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'émergence de la première civilisation pharaonique unifiée. Dépendance absolue vis-à-vis du rythme annuel du Nil, de la gestion du bassin d'irrigation et de l'administration hiéroglyphique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Gestion hydraulique étatique centralisée des crues annuelles et du limon fertile.
            • Accumulation monumentale de capital physique et symbolique (pyramides, canaux, greniers d'État).
            • Vulnérabilité systémique aux sécheresses prolongées (chute de l'Ancien Empire / 1ère Période Intermédiaire).
            """);
        Map<String, Boolean> sEgyptEngines = sEgypt.getTypeBEngineStates();
        sEgyptEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sEgyptEngines.put("ScottAgainstTheGrainPureEngine", true);
        sEgyptEngines.put("TurchinGoldstoneSDTEngine", true);
        sEgyptEngines.put("FertileCrescentSalinizationEngine", true);
        sEgyptEngines.put("HydrologicalEngineeringEngine", true);
        sEgyptEngines.put("MaritimeHighwayEngine", true);
        sEgyptEngines.put("SpatialCityFractalEngine", true);
        sEgyptEngines.put("TainterComplexityCollapseEngine", true);
        sEgyptEngines.put("FrontierAsabiyyahEngine", true);
        sEgyptEngines.put("DeforestationErosionEngine", true);
        list.add(sEgypt);

        // --- SCÉNARIO : ÂGE DU BRONZE MOYEN & MÉSOPOTAMIE (-1900) ---
        Scenario s3 = new Scenario();
        s3.setPresetKey("assyrian_empire");
        s3.setName("Âge du Bronze Moyen & Mésopotamie (-1900)");
        s3.setStartDateYear(-1900);
        s3.setEndDateYear(-600);
        s3.setInitialHumanCount(600000);
        s3.setInitialCapitalPerCapita(80.0);
        s3.setInitialEnergyPerCapita(50.0);
        s3.setInitialFoodReserveMonths(6.0);
        s3.setInitialInformationPerCapita(50.0);
        s3.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        s3.setTargetCohortSize(500);
        s3.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        s3.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        s3.setClippingEnabled(true);
        s3.setH3Resolution(5);
        s3.setMinLat(28.0); s3.setMaxLat(40.0); s3.setMinLng(38.0); s3.setMaxLng(52.0);
        s3.setBoundaryMode("DYNAMIC_RESERVOIR");
        s3.setDescription("""
            SCÉNARIO HISTORIQUE : Hydraulique, Salinisation & Cités-États de l'Âge du Bronze (-1900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'apogée du Bronze Moyen (Code d'Hammurabi, première dynastie de Babylone, dynastie Shang en Chine, fin de la civilisation de l'Indus) et les vulnérabilités écologiques d'irrigation intensive.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Salinisation progressive des sols irrigués de basse Mésopotamie (baisse du blé au profit de l'orge).
            • Dynamique d'Asabiyyah des peuples périphériques et militarisation de l'Empire Néo-Assyrien.
            • Réseaux urbains denses le long des canaux et dépendance au commerce du cuivre et de l'étain.
            """);
        Map<String, Boolean> s3Engines = s3.getTypeBEngineStates();
        s3Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s3Engines.put("ScottAgainstTheGrainPureEngine", true);
        s3Engines.put("TurchinGoldstoneSDTEngine", true);
        s3Engines.put("FertileCrescentSalinizationEngine", true);
        s3Engines.put("FrontierAsabiyyahEngine", true);
        s3Engines.put("HydrologicalEngineeringEngine", true);
        s3Engines.put("SpatialCityFractalEngine", true);
        s3Engines.put("TainterComplexityCollapseEngine", true);
        s3Engines.put("DeforestationErosionEngine", true);
        list.add(s3);

        // --- SCÉNARIO : MÉSOAMÉRIQUE (-1500) ---
        Scenario sMeso = new Scenario();
        sMeso.setPresetKey("mesoamerica");
        sMeso.setName("Civilisations Mésoaméricaines (Olmèques & Mayas) (-1500)");
        sMeso.setStartDateYear(-1500);
        sMeso.setEndDateYear(900);
        sMeso.setInitialHumanCount(3000000);
        sMeso.setInitialCapitalPerCapita(120.0);
        sMeso.setInitialEnergyPerCapita(80.0);
        sMeso.setInitialFoodReserveMonths(6.0);
        sMeso.setInitialInformationPerCapita(150.0);
        sMeso.setPopulationDensityType("MESOAMERICA");
        sMeso.setTargetCohortSize(250);
        sMeso.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sMeso.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sMeso.setClippingEnabled(true);
        sMeso.setH3Resolution(5);
        sMeso.setMinLat(12.0); sMeso.setMaxLat(24.0); sMeso.setMinLng(-105.0); sMeso.setMaxLng(-85.0);
        sMeso.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMeso.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Culture Mère Olmèque & Cités-États Mayas (-1500 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Émergence des centres cérémoniels de San Lorenzo et La Venta, puis essor de la civilisation maya classique. Modélise la maïsiculture intensive, les réservoirs d'eau pluviale et l'astronomie de précision.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Intensification agricole sur terrasses et chinampas sans bêtes de trait ni métallurgie du fer.
            • Cyclicités de sécheresse mésoaméricaines et résilience des réservoirs d'eau (aguadas, chultuns, cénotes).
            • Évolution fractale et dispersion des cités cérémonielles mayas suivies d'effondrements régionaux.
            """);
        Map<String, Boolean> sMesoEngines = sMeso.getTypeBEngineStates();
        sMesoEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMesoEngines.put("ScottAgainstTheGrainPureEngine", true);
        sMesoEngines.put("TurchinGoldstoneSDTEngine", true);
        sMesoEngines.put("AmerindianEcosystemEngine", true);
        sMesoEngines.put("HydrologicalEngineeringEngine", true);
        sMesoEngines.put("SpatialCityFractalEngine", true);
        sMesoEngines.put("TainterComplexityCollapseEngine", true);
        sMesoEngines.put("DeforestationErosionEngine", true);
        sMesoEngines.put("OstromCommonsPureEngine", true);
        list.add(sMeso);

        // --- SCÉNARIO HISTORIQUE : RAMSÈS II (-1279) ---
        Scenario sRamesses = new Scenario();
        sRamesses.setPresetKey("ramesses_ii");
        sRamesses.setName("Ramsès II, Pax Aegyptiaca & Bâtisseurs Monumentaux (-1279)");
        sRamesses.setStartDateYear(-1279);
        sRamesses.setEndDateYear(-1200);
        sRamesses.setInitialHumanCount(3500000L);
        sRamesses.setInitialCapitalPerCapita(140.0);
        sRamesses.setInitialEnergyPerCapita(75.0);
        sRamesses.setInitialFoodReserveMonths(12.0);
        sRamesses.setInitialInformationPerCapita(15.0);
        sRamesses.setPopulationDensityType("EGYPT_NILE");
        sRamesses.setTargetCohortSize(150);
        sRamesses.setH3Resolution(5);
        sRamesses.setTemporalResolutionDays(30.0);
        sRamesses.setClippingEnabled(true);
        sRamesses.setMinLat(18.0); sRamesses.setMaxLat(36.0); sRamesses.setMinLng(24.0); sRamesses.setMaxLng(42.0);
        sRamesses.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sRamesses.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sRamesses.setEarthHistoricalLeadersEnabled(true);
        sRamesses.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : Règne de Ramsès II, Traité de Qadesh & Architecture Monumentale (-1279 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le long règne de 66 ans de Ramsès II (XIXe dynastie), le traité de paix de Qadesh (-1259) avec l'Empire Hittite, et les chantiers monumentaux majeurs (Abou Simbel, Ramesseum, Pi-Ramsès).
            
            [THÉORIE DE L'INFLUENCE DES GRANDS HOMMES & OBSERVABLES]
            • Effet transitoire mais puissant d'un leadership exceptionnellement long : centralisation de la capacité étatique et sur-accumulation de capital physique (canaux, temples, greniers).
            • Stabilité géopolitique régionale et commerce méditerranéen sécurisé (Pax Aegyptiaca).
            • Vulnérabilité de succession post-recul du grand règne à l'approche de la crise systémique de l'Âge du Bronze (-1200).
            """);
        Map<String, Boolean> sRamessesEngines = sRamesses.getTypeBEngineStates();
        sRamessesEngines.put("HydrologicalEngineeringEngine", true);
        sRamessesEngines.put("DemographicLifeTableEngine", true);
        sRamessesEngines.put("TradeNetworkEngine", true);
        sRamessesEngines.put("WarDiplomacyEngine", true);
        sRamessesEngines.put("StructuralDemographicBifurcationEngine", true);
        sRamessesEngines.put("MaritimeHighwayEngine", true);
        sRamessesEngines.put("TurchinGoldstoneSDTEngine", true);
        sRamessesEngines.put("FertileCrescentSalinizationEngine", true);
        sRamessesEngines.put("ScottAgainstTheGrainPureEngine", true);
        sRamessesEngines.put("BoserupAgriculturalIntensificationEngine", true);
        list.add(sRamesses);

        // --- SCÉNARIO : EFFONDREMENT DE L'ÂGE DU BRONZE RÉCENT (-1200) ---
        Scenario sBronzeCollapse = new Scenario();
        sBronzeCollapse.setPresetKey("bronze_age_collapse_1200bc");
        sBronzeCollapse.setName("Effondrement de l'Âge du Bronze Récent & Peuples de la Mer (-1200)");
        sBronzeCollapse.setStartDateYear(-1200);
        sBronzeCollapse.setEndDateYear(-900);
        sBronzeCollapse.setInitialHumanCount(25000000);
        sBronzeCollapse.setInitialCapitalPerCapita(120.0);
        sBronzeCollapse.setInitialEnergyPerCapita(65.0);
        sBronzeCollapse.setInitialFoodReserveMonths(3.0);
        sBronzeCollapse.setInitialInformationPerCapita(80.0);
        sBronzeCollapse.setPopulationDensityType("URBAN_CLUSTERS");
        sBronzeCollapse.setTargetCohortSize(400);
        sBronzeCollapse.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sBronzeCollapse.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sBronzeCollapse.setDescription("""
            ⚔️ SCÉNARIO HISTORIQUE : La Grande Rupture Systémique de -1200 & Chute des Palais Mycéniens et Hittites
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'effondrement simultané en cascade des civilisations palatiales de Méditerranée orientale (Mycènes, Ugarit, Empire Hittite, affaiblissement de l'Égypte). Combinaison d'une méga-sécheresse centennale, de ruptures des routes de l'étain et des invasions des Peuples de la Mer.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Effondrement en chaîne des réseaux commerciaux interconnectés du bronze.
            • Chute brutale de la complexité institutionnelle (disparition de l'écriture Linéaire B, dépopulation urbaine).
            • Période d'Âges Sombres méditerranéens préparant la transition décentralisée vers le fer.
            """);
        Map<String, Boolean> sBronzeCollapseEngines = sBronzeCollapse.getTypeBEngineStates();
        sBronzeCollapseEngines.put("TainterComplexityCollapseEngine", true);
        sBronzeCollapseEngines.put("SpatialMetapopulationSEIREngine", true);
        sBronzeCollapseEngines.put("FertileCrescentSalinizationEngine", true);
        sBronzeCollapseEngines.put("TurchinGoldstoneSDTEngine", true);
        sBronzeCollapseEngines.put("LanchesterKineticWarfareEngine", true);
        sBronzeCollapseEngines.put("GranovetterThresholdCascadeEngine", true);
        sBronzeCollapse.getClimateEvents().add(new ClimateEvent("MEGADROUGHT", "Méga-Sécheresse Centennale de Méditerranée Orientale", -1200, 35.0, 33.0, 0.0, 8.5));
        list.add(sBronzeCollapse);

        // --- SCÉNARIO : DÉBUT ÂGE DU FER (-1000) ---
        Scenario sIron = new Scenario();
        sIron.setPresetKey("early_iron_age");
        sIron.setName("Début de l'Âge du Fer & Méditerranée Antique (-1000)");
        sIron.setStartDateYear(-1000);
        sIron.setEndDateYear(-300);
        sIron.setInitialHumanCount(20000000);
        sIron.setInitialCapitalPerCapita(150.0);
        sIron.setInitialEnergyPerCapita(70.0);
        sIron.setInitialFoodReserveMonths(6.0);
        sIron.setInitialInformationPerCapita(120.0);
        sIron.setPopulationDensityType("URBAN_CLUSTERS");
        sIron.setTargetCohortSize(500);
        sIron.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sIron.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sIron.setDescription("""
            ⚔️ SCÉNARIO HISTORIQUE : Transition Technologique vers la Sidérurgie & Réseaux Phéniciens (-1000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la transition de l'Âge du Bronze vers la métallurgie du fer après l'effondrement du Bronze Récent. Diffusion de l'alphabet phénicien, essor des cités-États grecques, expansion de l'Empire Néo-Assyrien et dynastie Zhou en Chine.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Démocratisation de l'outillage et des armes grâce à l'abondance géologique du minerai de fer.
            • Essor du commerce maritime thalassocratique en Méditerranée (Phéniciens, Grecs, Étrusques).
            • Rupture de la dépendance stratégique au cuivre/étain et multiplication des centres de pouvoir régionaux.
            """);
        Map<String, Boolean> sIronEngines = sIron.getTypeBEngineStates();
        sIronEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sIronEngines.put("TurchinGoldstoneSDTEngine", true);
        sIronEngines.put("FrontierAsabiyyahEngine", true);
        sIronEngines.put("MaritimeHighwayEngine", true);
        sIronEngines.put("DynamicMaritimeRoutingGraph", true);
        sIronEngines.put("SpatialCityFractalEngine", true);
        sIronEngines.put("TainterComplexityCollapseEngine", true);
        sIronEngines.put("KrugmanCorePeripheryEngine", true);
        sIronEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sIron);

        // --- SCÉNARIO : EXPANSION HELLÉNISTIQUE (-334) ---
        Scenario sAlexander = new Scenario();
        sAlexander.setPresetKey("alexander_hellenistic_334bc");
        sAlexander.setName("Expansion Hellénistique & Choc d'Alexandre le Grand (-334)");
        sAlexander.setStartDateYear(-334);
        sAlexander.setEndDateYear(-150);
        sAlexander.setInitialHumanCount(45000000);
        sAlexander.setInitialCapitalPerCapita(220.0);
        sAlexander.setInitialEnergyPerCapita(95.0);
        sAlexander.setInitialFoodReserveMonths(5.0);
        sAlexander.setInitialInformationPerCapita(250.0);
        sAlexander.setPopulationDensityType("URBAN_CLUSTERS");
        sAlexander.setTargetCohortSize(800);
        sAlexander.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sAlexander.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sAlexander.setDescription("""
            ⚡ SCÉNARIO HISTORIQUE : Conquête Macédonienne & Mondialisation Hellénistique (-334 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'effondrement foudroyant de l'Empire Achéménide sous la phalange macédonienne d'Alexandre. Intégration d'un corridor urbain et monétaire unifié de la Grèce à l'Indus (fondation d'Alexandries, monétarisation de l'or perse, koinè grecque).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de projection militaire et réduction spectaculaire de la friction logistique le long des routes royales perses.
            • Monétarisation massive et urbanisation grecque en Orient (Alexandrie, Séleucie, Antioche).
            • Fragmentation politique immédiate post-Alexandre (guerres des Diadoques) sans destruction du réseau urbain commercial.
            """);
        Map<String, Boolean> sAlexEngines = sAlexander.getTypeBEngineStates();
        sAlexEngines.put("MilitaryTechShockEngine", true);
        sAlexEngines.put("FrontierAsabiyyahEngine", true);
        sAlexEngines.put("DynamicMaritimeRoutingGraph", true);
        sAlexEngines.put("MaritimeHighwayEngine", true);
        sAlexEngines.put("SpatialCityFractalEngine", true);
        sAlexEngines.put("KrugmanCorePeripheryEngine", true);
        sAlexEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sAlexEngines.put("LanchesterKineticWarfareEngine", true);
        list.add(sAlexander);

        // --- SCÉNARIO : EMPIRE MAURYA (-300) ---
        Scenario sMaurya = new Scenario();
        sMaurya.setPresetKey("maurya_empire");
        sMaurya.setName("Empire Maurya & Civilisation de l'Indus-Gange (-300)");
        sMaurya.setStartDateYear(-300);
        sMaurya.setEndDateYear(100);
        sMaurya.setInitialHumanCount(50000000);
        sMaurya.setInitialCapitalPerCapita(200.0);
        sMaurya.setInitialEnergyPerCapita(90.0);
        sMaurya.setInitialFoodReserveMonths(6.0);
        sMaurya.setInitialInformationPerCapita(300.0);
        sMaurya.setPopulationDensityType("INDIA_MAURYA");
        sMaurya.setTargetCohortSize(1000);
        sMaurya.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sMaurya.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sMaurya.setClippingEnabled(true);
        sMaurya.setH3Resolution(5);
        sMaurya.setMinLat(8.0); sMaurya.setMaxLat(35.0); sMaurya.setMinLng(68.0); sMaurya.setMaxLng(90.0);
        sMaurya.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMaurya.setDescription("""
            ☸️ SCÉNARIO HISTORIQUE : L'Empire Maurya d'Ashoka & La Vallée du Gange (-300 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Unification du sous-continent indien sous Chandragupta et Ashoka. Modélise l'agriculture rizicole de la plaine gangétique, les routes commerciales de la Soie et le réseau urbain autour de Pataliputra et Taxila.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Densification démographique extrême de la plaine indo-gangétique grâce au riz irrigué.
            • Diffusion des édits impériaux et baisse institutionnelle de la violence (pacification d'Ashoka).
            • Intégration commerciale transasiatique le long de la Grand Trunk Road et des ports de l'océan Indien.
            """);
        Map<String, Boolean> sMauryaEngines = sMaurya.getTypeBEngineStates();
        sMauryaEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMauryaEngines.put("ScottAgainstTheGrainPureEngine", true);
        sMauryaEngines.put("TurchinGoldstoneSDTEngine", true);
        sMauryaEngines.put("FrontierAsabiyyahEngine", true);
        sMauryaEngines.put("SpatialCityFractalEngine", true);
        sMauryaEngines.put("SelfDomesticationEngine", true);
        sMauryaEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMauryaEngines.put("GranovetterThresholdCascadeEngine", true);
        sMauryaEngines.put("OstromCommonsPureEngine", true);
        list.add(sMaurya);

        // --- SCÉNARIO : EMPIRE ROMAIN (AN 0) ---
        Scenario sRoman = new Scenario();
        sRoman.setPresetKey("roman_empire");
        sRoman.setName("Empire Romain & Pax Romana (An 0)");
        sRoman.setStartDateYear(0);
        sRoman.setEndDateYear(476);
        sRoman.setInitialHumanCount(55000000);
        sRoman.setInitialCapitalPerCapita(350.0);
        sRoman.setInitialEnergyPerCapita(120.0);
        sRoman.setInitialFoodReserveMonths(6.0);
        sRoman.setInitialInformationPerCapita(400.0);
        sRoman.setPopulationDensityType("ROMAN_EMPIRE");
        sRoman.setTargetCohortSize(1000);
        sRoman.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sRoman.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sRoman.setClippingEnabled(true);
        sRoman.setH3Resolution(5);
        sRoman.setMinLat(25.0); sRoman.setMaxLat(55.0); sRoman.setMinLng(-10.0); sRoman.setMaxLng(45.0);
        sRoman.setBoundaryMode("DYNAMIC_RESERVOIR");
        Map<String, Boolean> sRomanEngines = sRoman.getTypeBEngineStates();
        sRomanEngines.put("RomanImperialCliodynamicEngine", true);
        sRomanEngines.put("TurchinGoldstoneSDTEngine", true);
        sRomanEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sRomanEngines.put("FrontierAsabiyyahEngine", true);
        sRomanEngines.put("MaritimeHighwayEngine", true);
        sRomanEngines.put("DynamicMaritimeRoutingGraph", true);
        sRomanEngines.put("SpatialCityFractalEngine", true);
        sRomanEngines.put("TainterComplexityCollapseEngine", true);
        sRomanEngines.put("BioMolecularEpidemiologyEngine", true);
        sRomanEngines.put("SpatialMetapopulationSEIREngine", true);
        sRomanEngines.put("DeforestationErosionEngine", true);
        sRomanEngines.put("KrugmanCorePeripheryEngine", true);
        sRomanEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sRomanEngines.put("GranovetterThresholdCascadeEngine", true);
        sRoman.setDescription("""
            SCÉNARIO HISTORIQUE : L'Empire Romain à son Apogée (Pax Romana, An 0)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE - SOURCES BESSES & BENCHMARKS CIA / SESHAT / HYDE]
            Modélise le bassin méditerranéen au moment de la Pax Romana sous Auguste. Intègre les données démographiques historiques (55 millions d'habitants), les réseaux d'infrastructures (viae, aqueducs) et les dynamiques cliodynamiques de Turchin.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement des infrastructures de transport (réseau viaire terrestre et autoroutes maritimes méditerranéennes).
            • Cycles cliodynamiques de Turchin (surproduction des élites, instabilité politique, dévaluation monétaire du denier).
            • Impact conjugué des chocs climatiques (Optimum Romain vers refroidissement) et des épidémies (Peste Antonine).
            """);
        list.add(sRoman);

        return list;
    }
}
