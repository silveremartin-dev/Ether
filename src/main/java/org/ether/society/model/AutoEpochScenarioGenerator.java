/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.TemporalMapTensorManager;
import org.ether.society.generation.PlanetPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Epistemic & Automated Historical Epoch Scenario Generator.
 * Synthesizes cohesive, physically and sociologically calibrated scenarios for any planet and epoch date.
 *
 * Automatically:
 * 1. Resolves geophysics, biomes, and paleoclimatic rasters (Tab 1)
 * 2. Resolves hydrology, geology, and mineral stocks (Tab 2)
 * 3. Calibrates demographic baseline N0, capital K0, cultural tensors, and Type B optional engines (Tab 3)
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class AutoEpochScenarioGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AutoEpochScenarioGenerator.class);

    public record EpochMilestone(long year, String nameKey, String defaultName, String descriptionKey, String defaultDesc) {}

    public static final List<EpochMilestone> EARTH_KEY_MILESTONES = List.of(
            new EpochMilestone(-100000L, "epoch.name.out_of_africa", "ðŸŒ Out of Africa (-100k BP)", "epoch.desc.out_of_africa", "Early Sapiens migration across the Old World, hunter-gatherer bands, megafauna interactions."),
            new EpochMilestone(-74000L, "epoch.name.toba", "ðŸŒ‹ Toba Bottleneck (-74k BP)", "epoch.desc.toba", "Supervolcano eruption, volcanic winter, demographic bottleneck & coastal refugia."),
            new EpochMilestone(-50000L, "epoch.name.sahul", "ðŸ¦˜ Sahul Colonization (-50k BP)", "epoch.desc.sahul", "Wallace line crossing, colonization of Australia & megafaunal extinctions."),
            new EpochMilestone(-20000L, "epoch.name.lgm", "â„ï¸ Last Glacial Maximum (-20k BP)", "epoch.desc.lgm", "LGM peak, sea level at -120m, Mammoth steppe, Beringia land bridge."),
            new EpochMilestone(-10900L, "epoch.name.younger_dryas", "â„ï¸ Younger Dryas (-10.9k BP)", "epoch.desc.younger_dryas", "Abrupt cold snap in the Northern Hemisphere, Natufian broad-spectrum foraging pressure."),
            new EpochMilestone(-8000L, "epoch.name.neolithic", "ðŸŒ¾ Neolithic Revolution (-8k BP)", "epoch.desc.neolithic", "Early agriculture in Fertile Crescent, animal domestication, permanent hamlets."),
            new EpochMilestone(-6000L, "epoch.name.green_sahara", "ðŸŒ´ Green Sahara (-6k BP)", "epoch.desc.green_sahara", "African Humid Period, Mega-Chad lake, pastoral nomadism across green savannas."),
            new EpochMilestone(-3000L, "epoch.name.bronze_age", "ðŸ›ï¸ Bronze Age (-3k BP)", "epoch.desc.bronze_age", "First urban civilizations, early metallurgy, irrigation canals, writing."),
            new EpochMilestone(-1200L, "epoch.name.bronze_collapse", "âš”ï¸ Bronze Age Collapse (-1.2k BP)", "epoch.desc.bronze_collapse", "Eastern Mediterranean systemic breakdown, Sea Peoples, trade disruptions."),
            new EpochMilestone(-1000L, "epoch.name.iron_age", "ðŸ›¡ï¸ Iron Age (-1k BP)", "epoch.desc.iron_age", "Widespread iron smelting, Phoenician and Greek Mediterranean trade colonies."),
            new EpochMilestone(0L, "epoch.name.antiquity", "ðŸ›ï¸ Roman & Han Optimum (0 AD)", "epoch.desc.antiquity", "Classical empires, Mediterranean integration, Pax Romana, Silk Road trade."),
            new EpochMilestone(536L, "epoch.name.late_antique_ice", "â„ï¸ Volcanic Winter (536 AD)", "epoch.desc.late_antique_ice", "Late Antique Little Ice Age, crop failures, Justinian Plague, nomadic migrations."),
            new EpochMilestone(1000L, "epoch.name.medieval_optimum", "ðŸ‰ Song Dynasty & Medieval (1000 AD)", "epoch.desc.medieval_optimum", "Medieval Climate Anomaly, Song proto-industrialization, trans-Eurasian trade."),
            new EpochMilestone(1347L, "epoch.name.black_death", "â˜ ï¸ Black Death (1347 AD)", "epoch.desc.black_death", "Yersinia pestis pandemic, labor shortages, demographic reset, peasant wage shocks."),
            new EpochMilestone(1492L, "epoch.name.columbian_exchange", "â›µ Columbian Exchange (1492 AD)", "epoch.desc.columbian_exchange", "Global oceanic trade networks, trans-Atlantic crop and pathogen exchange."),
            new EpochMilestone(1800L, "epoch.name.industrial_rev", "âš™ï¸ Industrial Revolution (1800 AD)", "epoch.desc.industrial_rev", "Steam power, coal extraction, demographic transition, fossil energy regime."),
            new EpochMilestone(1950L, "epoch.name.great_acceleration", "ðŸš€ Great Acceleration (1950 AD)", "epoch.desc.great_acceleration", "Global hydrocarbon exploitation, deep aquifer pumping, exponential demographic boom."),
            new EpochMilestone(2026L, "epoch.name.modern_baseline", "ðŸŒ Anthropocene Baseline (2026 AD)", "epoch.desc.modern_baseline", "Present-day empirical satellite baseline, global trade networks, energy transition."),
            new EpochMilestone(2050L, "epoch.name.mid_century", "ðŸ¤– Energy & Demographics (2050 AD)", "epoch.desc.mid_century", "Demographic stabilization, post-fossil transition, resource circularity.")
    );

    /**
     * Synthesizes an automatic scenario for a specified planet and target epoch date.
     */
    public static Scenario generateScenarioForEpoch(String planetKey, long targetYear) {
        return generateScenarioForEpoch(planetKey, targetYear, true, TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
    }

    /**
     * Synthesizes an automatic scenario for a specified planet, target epoch date,
     * and optional dedicated raster generation from raw empirical datasets.
     */
    public static Scenario generateScenarioForEpoch(String planetKey, long targetYear, boolean forceRegenerateMaps) {
        return generateScenarioForEpoch(planetKey, targetYear, forceRegenerateMaps, TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
    }

    /**
     * Synthesizes an automatic scenario for a specified planet, target epoch date,
     * optional dedicated raster generation, and explicit data fallback strategy.
     */
    public static Scenario generateScenarioForEpoch(String planetKey, long targetYear, boolean forceRegenerateMaps, 
                                                    TemporalMapTensorManager.DataFallbackStrategy fallbackStrategy) {
        String pKey = TemporalMapTensorManager.normalizePlanet(planetKey);
        boolean isEarth = "earth".equalsIgnoreCase(pKey);
        Scenario s = new Scenario();

        // 1. Resolve Best Matching Built-In Template (for Earth) or Clean Extraterrestrial Base
        if (isEarth) {
            Scenario template = findClosestBuiltInScenario(pKey, targetYear);
            if (template != null) {
                copyTemplateProperties(template, s);
            }
        } else {
            // Default parameters for non-Earth colonial/planetary scenarios
            s.setTargetCohortSize(100);
            s.setTemporalResolutionDays(30.0);
            s.setH3Resolution(3);
            s.setStrictDeterminism(true);
            s.setSparseCellSkippingEnabled(false);
            s.setOceanMacroAggregationEnabled(false);
            s.setCoastalNavigationOnlyEnabled(false);
            s.setOceanMultiRateTickingEnabled(false);
            s.setParallelExecutionEnabled(true);
            s.setSpatialRangeTruncationEnabled(false);
            s.setRandomEventsEnabled(true);
            s.setEarthHistoricalLeadersEnabled(false);
            s.setProceduralLeadersEnabled(true);
        }

        // 2. Set Chronology
        s.setStartDateYear(targetYear);
        long endYear = computeRecommendedEndYear(targetYear);
        s.setEndDateYear(endYear);

        // 3. Name & Description
        String epochLabel = formatEpochName(targetYear);
        String name = String.format("Auto-Scenario: %s (%s)", capitalize(pKey), epochLabel);
        s.setName(name);
        s.setDescription(generateEpochDescription(pKey, targetYear, epochLabel));

        // 4. Planet & Ecology Presets Matching the Epoch
        PlanetPreset planetPreset = resolvePlanetPresetForEpoch(pKey, targetYear);
        EcologyPreset ecologyPreset = resolveEcologyPresetForEpoch(pKey, targetYear, planetPreset);
        s.setPlanetPreset(planetPreset);
        s.setEcologyPreset(ecologyPreset);
        s.setEcologyPresetName(ecologyPreset != null ? ecologyPreset.name() : null);

        // 5. Demographics (N0, K0, Energy, Reserves)
        long popN0 = estimatePopulation(pKey, targetYear);
        double capitalK0 = estimateCapitalPerCapita(pKey, targetYear);
        double energyE0 = estimateEnergyPerCapita(pKey, targetYear);
        double foodMonths = estimateFoodReserves(pKey, targetYear);
        double infoI0 = estimateInformationPerCapita(pKey, targetYear);

        s.setInitialHumanCount(popN0);
        s.setInitialCapitalPerCapita(capitalK0);
        s.setInitialEnergyPerCapita(energyE0);
        s.setInitialFoodReserveMonths(foodMonths);
        s.setInitialInformationPerCapita(infoI0);
        s.setCultureVectorDimensions(9);

        if (isEarth) {
            s.setPopulationDensityType("IMPORT_CUSTOM");
        } else {
            s.setPopulationDensityType("PROCEDURAL");
            // Set all tensors to procedural for extraterrestrial planets
            List<Boolean> procModes = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                procModes.add(true);
            }
            s.setTensorProceduralModes(procModes);
        }

        // 6. Type B Optional Engine State Calibration
        Map<String, Boolean> typeBStates = calibrateTypeBEngines(pKey, targetYear);
        s.setTypeBEngineStates(typeBStates);

        // 7. Cartographic Raster Bases
        if (isEarth) {
            try {
                var densityImg = TemporalMapTensorManager.loadTemporalMapImage(pKey, targetYear, "density", fallbackStrategy);
                if (densityImg != null) {
                    String b64 = ImageMapLoader.imageToBase64Png(densityImg);
                    s.setCustomDensityBase64(b64);
                }
            } catch (Exception ex) {
                logger.warn("Could not pre-cache density raster for {} at year {}: {}", pKey, targetYear, ex.getMessage());
            }

            if (forceRegenerateMaps) {
                try {
                    org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(s);
                } catch (Exception ex) {
                    logger.warn("Could not force-generate scenario maps: {}", ex.getMessage());
                }
            }
        } else {
            // For non-Earth: check if planetary elevation / density image exists
            try {
                var elevImg = TemporalMapTensorManager.loadTemporalMapImage(pKey, targetYear, "elevation", fallbackStrategy);
                if (elevImg != null && planetPreset != null) {
                    // Pre-cached if needed
                }
            } catch (Exception ex) {
                logger.debug("Planetary raster check for {}: {}", pKey, ex.getMessage());
            }
        }

        return s;
    }

    public static long computeRecommendedEndYear(long startYear) {
        if (startYear <= -50000L) return startYear + 25000L;
        if (startYear <= -20000L) return startYear + 10000L;
        if (startYear <= -10000L) return startYear + 4000L;
        if (startYear <= -3000L) return startYear + 1500L;
        if (startYear <= 0L) return startYear + 500L;
        if (startYear <= 1500L) return startYear + 300L;
        if (startYear <= 1800L) return startYear + 150L;
        if (startYear <= 1950L) return startYear + 75L;
        return startYear + 50L;
    }

    public static long estimatePopulation(String planetKey, long year) {
        if (!"earth".equalsIgnoreCase(planetKey)) {
            // Procedural default for colonial/extraterrestrial outposts
            return 100_000L;
        }
        if (year <= -100000L) return 50_000L;
        if (year <= -74000L) return 15_000L;
        if (year <= -50000L) return 100_000L;
        if (year <= -25000L) return 300_000L;
        if (year <= -20000L) return 500_000L;
        if (year <= -10900L) return 2_500_000L;
        if (year <= -10000L) return 5_000_000L;
        if (year <= -8000L) return 8_000_000L;
        if (year <= -6000L) return 15_000_000L;
        if (year <= -3000L) return 50_000_000L;
        if (year <= -1900L) return 70_000_000L;
        if (year <= -1200L) return 85_000_000L;
        if (year <= -1000L) return 100_000_000L;
        if (year <= -334L) return 150_000_000L;
        if (year <= 0L) return 200_000_000L;
        if (year <= 536L) return 220_000_000L;
        if (year <= 632L) return 230_000_000L;
        if (year <= 1000L) return 310_000_000L;
        if (year <= 1206L) return 380_000_000L;
        if (year <= 1324L) return 420_000_000L;
        if (year <= 1347L) return 350_000_000L; // Black Death Dip
        if (year <= 1492L) return 500_000_000L;
        if (year <= 1639L) return 580_000_000L;
        if (year <= 1800L) return 1_000_000_000L;
        if (year <= 1900L) return 1_650_000_000L;
        if (year <= 1914L) return 1_800_000_000L;
        if (year <= 1950L) return 2_525_000_000L;
        if (year <= 2000L) return 6_127_000_000L;
        if (year <= 2026L) return 8_100_000_000L;
        if (year <= 2035L) return 8_800_000_000L;
        if (year <= 2045L) return 9_400_000_000L;
        if (year <= 2050L) return 9_700_000_000L;
        return 10_000_000_000L;
    }

    public static double estimateCapitalPerCapita(String planetKey, long year) {
        if (!"earth".equalsIgnoreCase(planetKey)) return 5000.0;
        if (year <= -10000L) return 2.0;    // Palaeolithic lithics
        if (year <= -3000L) return 25.0;    // Early Neolithic farming
        if (year <= 0L) return 120.0;       // Bronze/Iron tools & proto-infrastructure
        if (year <= 1500L) return 350.0;    // Medieval mills, carts, draft animals
        if (year <= 1800L) return 1000.0;   // Pre-industrial manufacturing
        if (year <= 1900L) return 3200.0;   // Steam, rail, factories
        if (year <= 1950L) return 7500.0;   // Electrical grids, internal combustion
        if (year <= 2000L) return 16000.0;  // Telecommunications, computing
        return 24000.0;                     // Modern advanced physical & digital capital
    }

    public static double estimateEnergyPerCapita(String planetKey, long year) {
        if (!"earth".equalsIgnoreCase(planetKey)) return 200.0;
        if (year <= -10000L) return 5.0;    // Fire & wood
        if (year <= 0L) return 20.0;        // Animal traction & charcoal
        if (year <= 1800L) return 40.0;     // Water/wind mills & wood
        if (year <= 1900L) return 120.0;    // Coal steam power
        if (year <= 1950L) return 250.0;    // Oil, electricity
        return 450.0;                       // Modern primary energy consumption
    }

    public static double estimateFoodReserves(String planetKey, long year) {
        if (year <= -10000L) return 2.0;    // Hunter-gatherer seasonal stocks
        if (year <= 0L) return 4.0;         // Granaries & silos
        if (year <= 1800L) return 6.0;      // Agricultural reserves
        return 8.0;                         // Modern global food supply chain
    }

    public static double estimateInformationPerCapita(String planetKey, long year) {
        if (year <= -10000L) return 2.0;    // Oral traditions
        if (year <= -3000L) return 10.0;    // Early pictograms/cuneiform
        if (year <= 1450L) return 50.0;     // Parchment & manuscript libraries
        if (year <= 1800L) return 300.0;    // Printing press & encyclopedias
        if (year <= 1950L) return 2000.0;   // Industrial telegraphy, radio & books
        return 50000.0;                     // Digital internet & computing
    }

    public static Map<String, Boolean> calibrateTypeBEngines(String planetKey, long year) {
        Map<String, Boolean> states = new HashMap<>();
        boolean isEarth = "earth".equalsIgnoreCase(planetKey);

        // 1. Paleolithic / Prehistoric Modules (active when year <= -10000 BP on Earth)
        boolean isPaleo = isEarth && (year <= -10000L);
        states.put("PaleoLanguageDriftEngine", isPaleo);
        states.put("KarstCaveShelterEngine", isPaleo);
        states.put("ParietalArtAsabiyyahEngine", isPaleo && year <= -15000L);
        states.put("SnowpackMobilityEngine", isPaleo);
        states.put("CanidDomesticationEngine", isPaleo && year >= -30000L);
        states.put("LithicTradeProvenanceEngine", isPaleo);
        states.put("MeatCuringReservesEngine", isPaleo);
        states.put("ExogamousKinshipEngine", isPaleo);
        states.put("ArchaicIntrogressionEngine", isPaleo && year <= -30000L);
        states.put("TailoredClothingThermalEngine", isPaleo);
        states.put("OchreTanningTechnologyEngine", isPaleo);
        states.put("AtlatlArcheryBallisticsEngine", isPaleo && year >= -40000L);
        states.put("CoastalMarineRefugiaEngine", isPaleo);
        states.put("SeasonalAggregationSanctuaryEngine", isPaleo);
        states.put("PassiveSnareSmallGameEngine", isPaleo);
        states.put("AridWaterStorageStashEngine", isPaleo);
        states.put("ResinHaftingAdhesivesEngine", isPaleo);
        states.put("OsseousIndustryCarvingEngine", isPaleo);
        states.put("PlantFiberCordageEngine", isPaleo);
        states.put("MegafaunaPitfallTrapEngine", isPaleo);
        states.put("AcousticFluteResonanceEngine", isPaleo && year <= -20000L);
        states.put("SymbolicBeadNetworkEngine", isPaleo);
        states.put("PortableArtFigurineEngine", isPaleo && year <= -15000L);
        states.put("CaveLightingPyrotechnicsEngine", isPaleo);
        states.put("RiverCanoeTransportEngine", isPaleo);
        states.put("PermafrostColdCacheEngine", isPaleo);
        states.put("PlantDetoxificationLeachingEngine", isPaleo);
        states.put("TopographicGameDriveEngine", isPaleo && year >= -25000L);
        states.put("LunarCalendarTallyEngine", isPaleo);
        states.put("MammothBoneHabitationEngine", isPaleo && year <= -15000L);
        states.put("FireHardenedSpearEngine", isPaleo);
        states.put("ParasiteControlRepellentEngine", isPaleo);
        states.put("WildCerealGrindingEngine", isPaleo && year >= -25000L);
        states.put("VolcanicTephraRefugiaEngine", isPaleo && (year <= -70000L && year >= -80000L));
        states.put("DemographicLifeTableEngine", true);
        states.put("ToolKitMaintenanceEngine", isPaleo);
        states.put("FireStickFarmingEngine", isPaleo);
        states.put("HomininCompetitiveExclusionEngine", isPaleo && year <= -30000L);
        states.put("ShellMiddenAccumulationEngine", isPaleo || (year <= -3000L));
        states.put("PelagicFishingHookEngine", isPaleo || (year <= -3000L));
        states.put("GeophyteDiggingStickEngine", isPaleo);
        states.put("BirchTarPyrolysisKilnEngine", isPaleo);
        states.put("SkinKayakSubArcticEngine", isPaleo);
        states.put("SalmonRunHarpoonEngine", isPaleo);
        states.put("CaveBearNicheCompetitionEngine", isPaleo && year <= -30000L);
        states.put("OchreMiningQuarryEngine", isPaleo);
        states.put("WindCuringSteppeCacheEngine", isPaleo);
        states.put("LashingsHideThongEngine", isPaleo);
        states.put("MicrolithBladeletProductionEngine", isPaleo);
        states.put("HighAltitudeHypoxiaEngine", true);
        states.put("MortuaryBurialRegaliaEngine", isPaleo);
        states.put("ProtoCeramicFiringEngine", isPaleo && year >= -30000L);
        states.put("EyedNeedleSewingEngine", isPaleo);
        states.put("KelpHighwayNavigationEngine", isPaleo && year >= -25000L);
        states.put("CaveHyenaScavengingEngine", isPaleo && year <= -30000L);

        // 2. Historical Dynastic / Asabiyyah / Classical Modules (-3000 to 1800)
        boolean isHistorical = year >= -3000L && year <= 1800L;
        states.put("FrontierAsabiyyahEngine", isHistorical);
        states.put("SelfDomesticationEngine", true);
        states.put("KinSelectionHamiltonEngine", true);
        states.put("TasmanianCulturalRegressionEngine", year <= 1800L);

        // 3. Modern / Industrial / Cybernetic Modules (1800+)
        boolean isIndustrial = year >= 1800L;
        states.put("AutoRegulationPureEngine", year >= 1900L);
        states.put("AiAutonomousRegulationPureEngine", year >= 2020L);

        return states;
    }

    public static PlanetPreset resolvePlanetPresetForEpoch(String planetKey, long year) {
        if (!"earth".equalsIgnoreCase(planetKey)) {
            // Find corresponding planet preset by name
            for (PlanetPreset p : PlanetPreset.getPresets()) {
                if (planetKey.equalsIgnoreCase(p.getCanonicalPlanet())) {
                    return p;
                }
            }
            return switch (planetKey.toLowerCase(Locale.ROOT)) {
                case "mars" -> PlanetPreset.MARS_LIKE;
                case "venus" -> PlanetPreset.VENUS_LIKE;
                case "moon" -> PlanetPreset.MOON_LIKE;
                case "mercury" -> PlanetPreset.MERCURY_LIKE;
                case "titan" -> PlanetPreset.TITAN_LIKE;
                default -> PlanetPreset.EARTH_LIKE;
            };
        }

        // For Earth: select the calibrated epoch preset
        if (year <= -100000L) return PlanetPreset.EARTH_LIG_100000BP;
        if (year <= -50000L) return PlanetPreset.EARTH_MIS3_50000BP;
        if (year <= -25000L) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (year <= -20000L) return PlanetPreset.EARTH_LGM_20000BP;
        if (year <= -10000L) return PlanetPreset.EARTH_EH_10000BP;
        if (year <= -6000L) return PlanetPreset.EARTH_MH_6000BP;
        if (year <= -3000L) return PlanetPreset.EARTH_LH_3000BP;
        if (year <= -1900L) return PlanetPreset.EARTH_BRONZE_1900BP;
        if (year <= -1000L) return PlanetPreset.EARTH_IRON_1000BP;
        return PlanetPreset.EARTH_MODERN;
    }

    public static EcologyPreset resolveEcologyPresetForEpoch(String planetKey, long year, PlanetPreset planetPreset) {
        if (!"earth".equalsIgnoreCase(planetKey)) {
            return switch (planetKey.toLowerCase(Locale.ROOT)) {
                case "mars" -> EcologyPreset.MARS_LIKE;
                case "venus" -> EcologyPreset.VENUS_LIKE;
                case "moon" -> EcologyPreset.MOON_LIKE;
                case "mercury" -> EcologyPreset.MERCURY_LIKE;
                case "titan" -> EcologyPreset.TITAN_LIKE;
                default -> EcologyPreset.EARTH_STANDARD;
            };
        }

        if (year <= -100000L) return EcologyPreset.EARTH_LIG_100000BP;
        if (year <= -50000L) return EcologyPreset.EARTH_MIS3_50000BP;
        if (year <= -25000L) return EcologyPreset.EARTH_LGM_ONSET_25000BP;
        if (year <= -20000L) return EcologyPreset.EARTH_LGM_20000BP;
        if (year <= -10000L) return EcologyPreset.EARTH_EH_10000BP;
        if (year <= -6000L) return EcologyPreset.EARTH_MH_6000BP;
        if (year <= -3000L) return EcologyPreset.EARTH_LH_3000BP;
        if (year <= -1900L) return EcologyPreset.EARTH_BRONZE_1900BP;
        if (year <= -1000L) return EcologyPreset.EARTH_IRON_1000BP;
        return EcologyPreset.EARTH_STANDARD;
    }

    private static Scenario findClosestBuiltInScenario(String planetKey, long targetYear) {
        if (!"earth".equalsIgnoreCase(planetKey)) {
            return null;
        }
        List<Scenario> builtIns = Scenario.getBuiltInScenarios();
        Scenario closest = null;
        long minDiff = Long.MAX_VALUE;

        for (Scenario sc : builtIns) {
            long scYear = sc.getStartDateYear();
            long diff = Math.abs(scYear - targetYear);
            if (diff < minDiff) {
                minDiff = diff;
                closest = sc;
            }
        }
        return closest;
    }

    private static void copyTemplateProperties(Scenario source, Scenario target) {
        target.setTargetCohortSize(source.getTargetCohortSize());
        target.setTemporalResolutionDays(source.getTemporalResolutionDays());
        target.setH3Resolution(source.getH3Resolution());
        target.setStrictDeterminism(source.isStrictDeterminism());
        target.setSparseCellSkippingEnabled(source.isSparseCellSkippingEnabled());
        target.setOceanMacroAggregationEnabled(source.isOceanMacroAggregationEnabled());
        target.setCoastalNavigationOnlyEnabled(source.isCoastalNavigationOnlyEnabled());
        target.setOceanMultiRateTickingEnabled(source.isOceanMultiRateTickingEnabled());
        target.setParallelExecutionEnabled(source.isParallelExecutionEnabled());
        target.setSpatialRangeTruncationEnabled(source.isSpatialRangeTruncationEnabled());
        target.setRandomEventsEnabled(source.isRandomEventsEnabled());
        target.setEarthHistoricalLeadersEnabled(source.isEarthHistoricalLeadersEnabled());
        target.setProceduralLeadersEnabled(source.isProceduralLeadersEnabled());
    }

    public static String formatEpochName(long year) {
        if (year < 0) {
            return Math.abs(year) + " BP (" + Math.abs(year) + " av. J.-C.)";
        } else if (year == 0) {
            return "0 AD (An 0 / AntiquitÃ©)";
        } else {
            return year + " AD (" + year + " ap. J.-C.)";
        }
    }

    private static String generateEpochDescription(String planet, long year, String epochLabel) {
        return String.format(Locale.ROOT, """
            ðŸŒ SCÃ‰NARIO AUTOMATIQUE D'Ã‰POQUE : %s â€” %s
            
            [PARAMÃ‰TRAGE SCIENTIFIQUE & GÃ‰OPHYSIQUE (TIER 1)]
            â€¢ PlanÃ¨te cible : %s
            â€¢ AnnÃ©e de dÃ©part : %s (Horizon temporel recommandÃ© : %d ans)
            â€¢ Population mondiale estimÃ©e Nâ‚€ : %,d habitants
            â€¢ Capital physique Kâ‚€ : %.1f kg/habitant
            â€¢ Ã‰nergie stockÃ©e Eâ‚€ : %.1f MJ/habitant
            
            [SYNCHRONISATION INTER-ONGLETS]
            â€¢ Onglet 1 (PlanÃ¨te) : ModÃ¨le numÃ©rique d'Ã©lÃ©vation, biomes et palÃ©oclimat de l'Ã©poque %s.
            â€¢ Onglet 2 (Ressources) : Nappes phrÃ©atiques, minerais et rÃ©serves vierges prÃ©-industrielles.
            â€¢ Onglet 3 (SociÃ©tÃ© & Moteurs) : Filtrage des modules de Type B adaptÃ©s Ã  la pÃ©riode.
            """,
                capitalize(planet), epochLabel, capitalize(planet), epochLabel,
                computeRecommendedEndYear(year) - year, estimatePopulation(planet, year),
                estimateCapitalPerCapita(planet, year), estimateEnergyPerCapita(planet, year),
                epochLabel);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }
}

