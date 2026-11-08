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
 * Helper class providing default milestones, Tier 2 Type B engine domain validity
 * configurations, and text formatting routines for {@link AutoEpochScenarioGenerator}.
 *
 * <p>Per {@code AGENTS.md} §8.2, §11.3, §12.4, and §13, Tier 2 Type B engines model context-dependent
 * socio-historical, institutional, capital-dependent, or geopolitical mechanisms rather than
 * permanent universal thermodynamic laws (Tier 1). Every engine key in
 * {@link #calibrateTypeBEngines(String, long)} corresponds strictly 1-to-1 to an executable
 * Java class in {@code org.ether.society.engines.tier2.theories},
 * {@code org.ether.society.engines.tier2.historical}, or optional anthropogenic modules in
 * {@code org.ether.society.engines.tier1}. Engines whose domain validity conditions are not
 * satisfied by the target {@code planetKey} and {@code year} (spanning from the Plio-Pleistocene
 * hominin horizon at \(-3\,000\,000\text{ BP}\) through the prospective horizon at \(2200\text{ CE}\))
 * are explicitly unchecked ({@code false}) by default.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see org.ether.society.ui.scenario.ScenarioEngineCatalog
 */
public final class AutoEpochScenarioHelper {

    private AutoEpochScenarioHelper() {
    }

    public static final List<EpochMilestone> EARTH_KEY_MILESTONES = List.of(
            new EpochMilestone(-3000000L, "epoch.name.plio_pleistocene", "🦴 Plio-Pleistocene Hominins (-3M BP)", "epoch.desc.plio_pleistocene", "Early hominin emergence, Oldowan lithic percussion, savanna foraging, kin selection."),
            new EpochMilestone(-300000L, "epoch.name.archaic_sapiens", "🔥 Archaic Homo Sapiens (-300k BP)", "epoch.desc.archaic_sapiens", "Anatomically modern humans, controlled fire, symbolic ochre, proto-linguistic drift."),
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
            new EpochMilestone(2050L, "epoch.name.mid_century", "🤖 Energy & Demographics (2050 AD)", "epoch.desc.mid_century", "Demographic stabilization, post-fossil transition, resource circularity."),
            new EpochMilestone(2100L, "epoch.name.end_century", "🌡️ Late 21st Century Horizon (2100 AD)", "epoch.desc.end_century", "Long-term climate equilibrium, coastal adaptation, closed-loop industrial ecology."),
            new EpochMilestone(2200L, "epoch.name.prospective_2200", "🪐 Multi-Century Prospective Horizon (2200 AD)", "epoch.desc.prospective_2200", "Deep prospective thermodynamic equilibrium, planetary engineering, off-Earth ISRU autarky.")
    );

    /**
     * Calibrates the default activation state ({@code true} / {@code false}) for all 86 verified
     * Tier 2 Type B optional engines according to their documented spatiotemporal, technological,
     * capital, and geopolitical validity conditions ({@code AGENTS.md} §13 and
     * {@code docs/TYPE_B_ENGINES_VALIDITY_CONDITIONS_CATALOG.md}).
     *
     * <p>Engines whose domain validity conditions are not satisfied by the target {@code planetKey}
     * and {@code year} (spanning from \(-3\,000\,000\text{ BP}\) to prospective futures) are
     * explicitly unchecked ({@code false}) by default rather than masked by hidden internal sigmoids.</p>
     *
     * @param planetKey canonical planet identifier (e.g. {@code "earth"}, {@code "mars"}, {@code "moon"})
     * @param year      scenario start year in astronomical calendar convention (negative = BCE/BP, positive = CE)
     * @return map of all 86 verified Type B engine identifiers to their default boolean activation state
     */
    public static Map<String, Boolean> calibrateTypeBEngines(String planetKey, long year) {
        Map<String, Boolean> states = new HashMap<>();
        boolean isEarth = planetKey == null || "earth".equalsIgnoreCase(planetKey.trim());

        // =========================================================================
        // 1. DEEP-TIME BIOCULTURAL, EVOLUTIONARY & FORAGER-MOBILITY ENGINES (8)
        // Valid from Plio-Pleistocene hominin emergence (-3,000,000 BP) onward
        // =========================================================================
        states.put("SelfDomesticationEngine", isEarth && year >= -3000000L);
        states.put("SexualSelectionMatingEngine", isEarth && year >= -3000000L);
        states.put("KinSelectionHamiltonEngine", isEarth && year >= -3000000L);
        states.put("PriceMultilevelSelectionEngine", isEarth && year >= -3000000L);
        states.put("LanguageLinguisticEngine", isEarth && year >= -300000L);
        states.put("CulturalSociologyEngine", isEarth && year >= -300000L);
        states.put("DynamicMaritimeRoutingGraph", isEarth && year >= -60000L);
        states.put("TasmanianCulturalRegressionEngine", false); // Specialized demographic isolation / treadmill loss test

        // =========================================================================
        // 2. NEOLITHIC, BRONZE AGE & CLASSICAL AGRARIAN-STATE ENGINES (24)
        // =========================================================================
        boolean isAgrarianOrLater = isEarth && year >= -8000L;
        states.put("BoserupAgriculturalIntensificationEngine", isAgrarianOrLater);
        states.put("ScottAgainstTheGrainPureEngine", isEarth && year >= -4000L && year <= 1500L);

        boolean isStateEraOrLater = isEarth && year >= -3000L;
        states.put("DraftAnimalFodderAllocationEngine", isStateEraOrLater);
        states.put("TradeNetworkEngine", isStateEraOrLater);
        states.put("WarDiplomacyEngine", isStateEraOrLater);
        states.put("ThermodynamicWarfareEngine", isStateEraOrLater);
        states.put("MaritimeHighwayEngine", isStateEraOrLater);
        states.put("HydrologicalEngineeringEngine", isStateEraOrLater);
        states.put("SoilSalinizationHydrologyEngine", isStateEraOrLater);
        states.put("SpatialCityFractalEngine", isStateEraOrLater);
        states.put("VonThunenAgriculturalRingsEngine", isStateEraOrLater);
        states.put("WestBettencourtAllometryEngine", isStateEraOrLater);
        states.put("TurchinGoldstoneSDTEngine", isStateEraOrLater);
        states.put("StructuralDemographicBifurcationEngine", isStateEraOrLater);
        states.put("TainterComplexityCollapseEngine", isStateEraOrLater);
        states.put("CliodynamicsEngine", isStateEraOrLater);
        states.put("ArthurCombinatorialTechnologyEngine", !isEarth || year >= -3000L);
        states.put("GranovetterThresholdCascadeEngine", isStateEraOrLater);
        states.put("SchellingAxelrodSegregationEngine", isStateEraOrLater);
        states.put("SpatialMetapopulationSEIREngine", isStateEraOrLater);
        states.put("HarrisCulturalMaterialismEngine", isStateEraOrLater);
        states.put("LenskiEcologicalEvolutionaryEngine", isStateEraOrLater);
        states.put("WhiteEnergyEvolutionEngine", isStateEraOrLater);
        states.put("FrontierAsabiyyahEngine", isEarth && year >= -3000L && year <= 1800L);
        states.put("AxialAgeMoralizingGodsEngine", isEarth && year >= -1000L);

        // =========================================================================
        // 3. IRON AGE, MEDIEVAL & EARLY MODERN MERCANTILE ENGINES (13)
        // =========================================================================
        states.put("BarfieldNomadicSteppeEmpireEngine", isEarth && year >= -1000L && year <= 1750L);
        states.put("AcemogluRobinsonInstitutionsEngine", isEarth && year >= -500L);
        states.put("KrugmanCorePeripheryEngine", isEarth && year >= -500L);
        states.put("CoGovernanceTradeEngine", isEarth && year >= 1000L);
        states.put("LandReclamationEngine", isEarth && year >= 1200L);
        states.put("MilitaryTechShockEngine", isEarth && year >= 1200L);
        states.put("PhysicalMediaPrintingEngine", isEarth && year >= 1450L);
        states.put("AsymmetricColonialTradeEngine", isEarth && year >= 1492L && year <= 1960L);
        states.put("CrosbyColumbianExchangeEngine", isEarth && year >= 1492L && year <= 1750L);
        states.put("ProtestantWorkEthicEngine", isEarth && year >= 1550L && year <= 1950L);
        states.put("SovereignTreasuryBondEngine", isEarth && year >= 1650L);
        states.put("NavalHydrodynamicsFreightEngine", isEarth && year >= 1700L);
        states.put("InnisStaplesThesisEngine", isEarth && year >= 1700L);

        // =========================================================================
        // 4. INDUSTRIAL, FOSSIL-EXERGY & MODERN MACROECONOMIC ENGINES (13)
        // =========================================================================
        states.put("PomeranzGreatDivergenceEngine", isEarth && year >= 1750L);
        states.put("RicardoDifferentialLandRentEngine", isEarth && year >= 1750L);
        states.put("KondratievSchumpeterLongWaveEngine", isEarth && year >= 1780L);

        boolean isIndustrialOrLater = isEarth && year >= 1800L;
        states.put("KummelAyresExergyEngine", !isEarth || year >= 1800L);
        states.put("PolanyiFictitiousCommoditiesEngine", isIndustrialOrLater);
        states.put("HotellingResourceDepletionEngine", isIndustrialOrLater);
        states.put("UrbanThermodynamicsEngine", isEarth && year >= 1850L);
        states.put("PublicSanitationWaterGridEngine", isEarth && year >= 1850L);
        states.put("SmilMaterialTransitionsPureEngine", isEarth && year >= 1850L);
        states.put("CordenNearyDutchDiseaseEngine", isEarth && year >= 1850L);
        states.put("LewisDualSectorTurningPointEngine", isEarth && year >= 1850L);
        states.put("JevonsParadoxEngine", isEarth && year >= 1865L);
        states.put("BagehotFractionalReserveEngine", isEarth && year >= 1873L);

        // =========================================================================
        // 5. CONTEMPORARY, GREAT ACCELERATION & PROSPECTIVE ENGINES (13)
        // =========================================================================
        states.put("PhysicalLeontiefInputOutputEngine", !isEarth || year >= 1900L);
        states.put("PhysicalSupplyChainEngine", !isEarth || year >= 1900L);
        states.put("WienerCyberneticHomeostasisEngine", !isEarth || year >= 1900L);
        states.put("InformationEntropyEngine", !isEarth || year >= 1900L);
        states.put("EcotoxicologyFertilityEngine", isEarth && year >= 1950L);
        states.put("NuclearSafetyRadiotoxicityEngine", !isEarth || year >= 1950L);
        states.put("MarineSubmersionEngine", isEarth && year >= 1950L);
        states.put("World3CouplingEngine", isEarth && year >= 1950L);
        states.put("KurzweilAcceleratingReturnsEngine", isEarth && year >= 1990L);
        states.put("CliodynamicAdvisorEngine", isEarth && year >= 2020L);
        states.put("IsruAutarkyAndSpaceColonizationEngine", !isEarth || year >= 2050L);
        states.put("TerraformingEngine", !isEarth || year >= 2100L);

        // =========================================================================
        // 6. SPECIALIZED REGIONAL, COUNTERFACTUAL & PURE-THEORY SANDBOX ENGINES (15)
        // Unchecked (false) by default on general global auto-scenarios; enabled only
        // in dedicated regional/thematic presets or when explicitly checked by the user.
        // =========================================================================
        states.put("AmerindianEcosystemEngine", false);
        states.put("RomanImperialCliodynamicEngine", false);
        states.put("MonasticDemographicBufferEngine", false);
        states.put("EdoJapanIsolationEngine", false);
        states.put("OstromCommonsPureEngine", false);
        states.put("PinkerViolenceDeclinePureEngine", false);
        states.put("World3HybridEngine", false);
        states.put("World3PureEngine", false);
        states.put("HandyNasaHybridEngine", false);
        states.put("HandyNasaPureEngine", false);
        states.put("NordhausDiceHybridEngine", false);
        states.put("NordhausDicePureEngine", false);
        states.put("NuclearWarfareClimateEngine", false);
        states.put("GeoengineeringAlbedoFeedbackEngine", false);
        states.put("BifurcationChaosEngine", false);

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
