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
 * @see org.ether.society.persistence.PresetStorageService
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
     * @return normalized preset key string
     */
    public static String resolvePresetKey(String presetKey, String name) {
        if (presetKey != null && !presetKey.isBlank()) {
            return presetKey.trim();
        }
        if (name != null && !name.isBlank()) {
            return org.ether.society.persistence.PresetStorageService.slugify(name);
        }
        return "unnamed_scenario";
    }

    /**
     * Assembles and returns the complete list of canonical built-in scenarios sorted chronologically.
     *
     * @return chronologically sorted list of built-in {@link Scenario} instances
     */
    public static List<Scenario> getBuiltInScenarios() {
        List<Scenario> list = new ArrayList<>(org.ether.society.persistence.PresetStorageService.loadAllScenarios());

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
        // --- SCÉNARIOS DU FUTUR (TRAJECTOIRES IPCC AR6 / SSP 1 À 5) ---

        // --- SCÉNARIO DU FUTUR : PROJECTION PROSPECTIVE ETHER (2026-2100) ---
        // Dynamique biophysique standard : sans triche, sans forçage artificiel, sans accident nucléaire.
        Scenario sFuture = new Scenario();
        sFuture.setPresetKey("earth_2026_future");
        sFuture.setName("Projection Prospective Ether (2026-2100) : Dynamique Biophysique Standard");
        sFuture.setStartDateYear(2026);
        sFuture.setEndDateYear(2100);
        sFuture.setInitialHumanCount(8264000000L); // 8.264B UN WPP 2024 exact baseline
        sFuture.setInitialCapitalPerCapita(13500.0); // World Bank / Maddison 2024 GWP/capita
        sFuture.setInitialEnergyPerCapita(73.0); // Vaclav Smil 2021 / IEA: 605 EJ / 8.26B ~ 73.2 GJ/capita
        sFuture.setInitialFoodReserveMonths(3.5);
        sFuture.setInitialInformationPerCapita(150000.0);
        sFuture.setPopulationDensityType("URBAN_CLUSTERS");
        sFuture.setTargetCohortSize(50000);
        sFuture.setPlanetPreset(PlanetPreset.EARTH_MODERN);
        sFuture.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sFuture.setDescription("""
            🌐 SCÉNARIO FUTUR : Projection Biophysique Standard du Moteur Ether (2026-2100)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE & ÉCOLOGIQUE (T₀ = 2026)]
            Trajectoire prospective non-contrainte issue de la physique standard du moteur Ether.
            Intègre l'état biophysique réel de 2026 (CO₂ à 423 ppm, température +1.28°C, déplétion pétrolière Hotelling,
            abaissement des teneurs minérales de Bihouix, exergue Kümmel-Ayres et rendements décroissants).
            
            [OBJECTIF SCIENTIFIQUE & FALSIFICATION]
            Ce scénario unique sert de base de projection endogène du moteur Ether, comparé ensuite
            aux 5 modèles exogènes de référence du GIEC/IIASA (SSP1, SSP2, SSP3, SSP4, SSP5) et aux projections ONU (UN WPP 2024).
            """);
        Map<String, Boolean> sFutureEngines = sFuture.getTypeBEngineStates();
        sFutureEngines.put("KummelAyresExergyEngine", true);
        sFutureEngines.put("SmilMaterialTransitionsPureEngine", true);
        sFutureEngines.put("HotellingResourceDepletionEngine", true);
        sFutureEngines.put("RenewableEnergyPhysicsEngine", true);
        sFutureEngines.put("NetEnergyEROEIEngine", true);
        sFutureEngines.put("PhysicalEnergyGridEngine", true);
        sFutureEngines.put("ResourceRecyclingEngine", true);
        sFutureEngines.put("JevonsParadoxEngine", true);
        sFutureEngines.put("OreGradeThermodynamicsEngine", true);
        sFutureEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sFutureEngines.put("BoserupAgriculturalIntensificationEngine", true);
        list.add(sFuture);

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
        
        
        s6Engines.put("KurzweilAcceleratingReturnsEngine", true);
        
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
        s8Engines.put("World3HybridEngine", false);
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
        s9Engines.put("World3HybridEngine", false);
        list.add(s9);

        ExoplanetScenarioCatalog.appendExoplanetScenarios(list);
    }
}
