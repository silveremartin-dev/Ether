/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.model.AutoEpochScenarioGenerator.EpochMilestone;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class providing default milestones, Type B engine catalog configurations,
 * and text formatting routines for {@link AutoEpochScenarioGenerator}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class AutoEpochScenarioHelper {

    private AutoEpochScenarioHelper() {
    }

    public static final List<EpochMilestone> EARTH_KEY_MILESTONES = List.of(
            new EpochMilestone(-100000L, "epoch.name.out_of_africa", "🌍 Out of Africa (-100k BP)", "epoch.desc.out_of_africa", "Early Sapiens migration across the Old World, hunter-gatherer bands, megafauna interactions."),
            new EpochMilestone(-74000L, "epoch.name.toba", "🌋 Toba Bottleneck (-74k BP)", "epoch.desc.toba", "Supervolcano eruption, volcanic winter, demographic bottleneck & coastal refugia."),
            new EpochMilestone(-50000L, "epoch.name.sahul", "🦘 Sahul Colonization (-50k BP)", "epoch.desc.sahul", "Wallace line crossing, colonization of Australia & megafaunal extinctions."),
            new EpochMilestone(-20000L, "epoch.name.lgm", "❄️ Last Glacial Maximum (-20k BP)", "epoch.desc.lgm", "LGM peak, sea level at -120m, Mammoth steppe, Beringia land bridge."),
            new EpochMilestone(-10900L, "epoch.name.younger_dryas", "❄️ Younger Dryas (-10.9k BP)", "epoch.desc.younger_dryas", "Abrupt cold snap in the Northern Hemisphere, Natufian broad-spectrum foraging pressure."),
            new EpochMilestone(-8000L, "epoch.name.neolithic", "🌾 Neolithic Revolution (-8k BP)", "epoch.desc.neolithic", "Early agriculture in Fertile Crescent, animal domestication, permanent hamlets."),
            new EpochMilestone(-6000L, "epoch.name.green_sahara", "🌴 Green Sahara (-6k BP)", "epoch.desc.green_sahara", "African Humid Period, Mega-Chad lake, pastoral nomadism across green savannas."),
            new EpochMilestone(-3000L, "epoch.name.bronze_age", "🏛️ Bronze Age (-3k BP)", "epoch.desc.bronze_age", "First urban civilizations, early metallurgy, irrigation canals, writing."),
            new EpochMilestone(-1200L, "epoch.name.bronze_collapse", "⚔️ Bronze Age Collapse (-1.2k BP)", "epoch.desc.bronze_collapse", "Eastern Mediterranean systemic breakdown, Sea Peoples, trade disruptions."),
            new EpochMilestone(-1000L, "epoch.name.iron_age", "🛡️ Iron Age (-1k BP)", "epoch.desc.iron_age", "Widespread iron smelting, Phoenician and Greek Mediterranean trade colonies."),
            new EpochMilestone(0L, "epoch.name.antiquity", "🏛️ Roman & Han Optimum (0 AD)", "epoch.desc.antiquity", "Classical empires, Mediterranean integration, Pax Romana, Silk Road trade."),
            new EpochMilestone(536L, "epoch.name.late_antique_ice", "❄️ Volcanic Winter (536 AD)", "epoch.desc.late_antique_ice", "Late Antique Little Ice Age, crop failures, Justinian Plague, nomadic migrations."),
            new EpochMilestone(1000L, "epoch.name.medieval_optimum", "🏯 Song Dynasty & Medieval (1000 AD)", "epoch.desc.medieval_optimum", "Medieval Climate Anomaly, Song proto-industrialization, trans-Eurasian trade."),
            new EpochMilestone(1347L, "epoch.name.black_death", "💀 Black Death (1347 AD)", "epoch.desc.black_death", "Yersinia pestis pandemic, labor shortages, demographic reset, peasant wage shocks."),
            new EpochMilestone(1492L, "epoch.name.columbian_exchange", "⛵ Columbian Exchange (1492 AD)", "epoch.desc.columbian_exchange", "Global oceanic trade networks, trans-Atlantic crop and pathogen exchange."),
            new EpochMilestone(1800L, "epoch.name.industrial_rev", "⚙️ Industrial Revolution (1800 AD)", "epoch.desc.industrial_rev", "Steam power, coal extraction, demographic transition, fossil energy regime."),
            new EpochMilestone(1950L, "epoch.name.great_acceleration", "🚀 Great Acceleration (1950 AD)", "epoch.desc.great_acceleration", "Global hydrocarbon exploitation, deep aquifer pumping, exponential demographic boom."),
            new EpochMilestone(2026L, "epoch.name.modern_baseline", "🌍 Anthropocene Baseline (2026 AD)", "epoch.desc.modern_baseline", "Present-day empirical satellite baseline, global trade networks, energy transition."),
            new EpochMilestone(2050L, "epoch.name.mid_century", "🤖 Energy & Demographics (2050 AD)", "epoch.desc.mid_century", "Demographic stabilization, post-fossil transition, resource circularity.")
    );

    public static Map<String, Boolean> calibrateTypeBEngines(String planetKey, long year) {
        Map<String, Boolean> states = new HashMap<>();
        boolean isEarth = "earth".equalsIgnoreCase(planetKey);

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

        boolean isHistorical = year >= -3000L && year <= 1800L;
        states.put("FrontierAsabiyyahEngine", isHistorical);
        states.put("SelfDomesticationEngine", true);
        states.put("KinSelectionHamiltonEngine", true);
        states.put("TasmanianCulturalRegressionEngine", year <= 1800L);

        states.put("AutoRegulationPureEngine", year >= 1900L);
        return states;
    }

    public static void copyTemplateProperties(Scenario source, Scenario target) {
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
        target.setChronicleSingularityObserverEnabled(source.isChronicleSingularityObserverEnabled());
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }
}
