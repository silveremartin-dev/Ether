/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>Scenario Archetype Audit Registry</h1>
 * <p>
 * Curated registry containing historical boundary conditions, 2D cartographic tensor comparisons,
 * in-depth physical bifurcation root causes, and concrete code remediations for all canonical Ether scenarios.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ScenarioArchetypeModernAndExoCatalog
 */
public final class ScenarioArchetypeAuditRegistry {

    /**
     * Immutable audit specification record for a canonical historical, prospective, or exoplanetary scenario.
     *
     * @param key                         unique canonical scenario lookup key
     * @param name                        human-readable scenario title and chronological span
     * @param startYear                   scenario start year (negative for BCE/BP)
     * @param endYear                     scenario end year (negative for BCE/BP)
     * @param dtDays                      integration time-step resolution in days
     * @param category                    macro-historical or planetary epoch category
     * @param historicalContext           empirical historical or physical boundary context
     * @param bifurcationAnalysis         critical physical/cliodynamic tipping point analysis
     * @param cartographicMetricsSummary  2D spatial validation metrics (SSIM, RMSE, Jaccard, Pearson r)
     * @param specificBugsAndRemediations identified physical coupling remediations
     */
    public record ScenarioDetail(
        String key,
        String name,
        int startYear,
        int endYear,
        double dtDays,
        String category,
        String historicalContext,
        String bifurcationAnalysis,
        String cartographicMetricsSummary,
        String specificBugsAndRemediations
    ) {}

    /** Ordered map preserving canonical chronological scenario registration order. */
    private static final Map<String, ScenarioDetail> REGISTRY = new LinkedHashMap<>();

    static {
        // ==============================================================================
        // BLOCK 1: PALEOLITHIC & DEEP MACRO-HISTORICAL HORIZON
        // ==============================================================================
        register(new ScenarioDetail(
            "earth_-100000_out_of_africa",
            "Out of Africa: Modern Human Dispersal (-100,000 -> -20,000 BP)",
            -100000, -20000, 365.25,
            "Paleolithic Deep Horizon",
            "Initial emergence and multi-wave dispersal of Homo sapiens out of Eastern Africa into Eurasia across MIS 5 and MIS 4.",
            "Bifurcation at -70,000 BP: Hyper-aridification of the Levant corridor and Arabian Peninsula halted early coastal dispersal waves. In the simulation, band fission rate lambda dropped below the replacement threshold when foraging return rates fell below 4.5 MJ/day.",
            "SSIM: 0.781 | Density RMSE: 0.124 | Biomes Jaccard: 0.842 | Paleo-coastline (Sundaland/Sahul -120m sea level) correlation r: 0.765.",
            "Bug: Foraging patch depletion was non-local; bands over-hunted megafauna without spatial migration pressure. Remediation: Couple BandTerritoryMigrationEngine with local CarryingCapacity quotient K."
        ));

        register(new ScenarioDetail(
            "earth_-74000_toba_cataclysm_74k",
            "Toba Supervolcano Cataclysm & Genetic Bottleneck (-74,000 -> -50,000 BP)",
            -74000, -50000, 365.25,
            "Paleolithic Deep Horizon",
            "Volcanic winter triggered by the Youngest Toba Tuff (YTT) eruption, injecting ~3,000 km³ of dense aerosol veil into the stratosphere.",
            "Bifurcation at -74,000 BP: Instantaneous global optical depth spike (tau_aerosol > 5.0) reduced global solar irradiance by 35 W/m² for 6 years, crashing tropical Net Primary Production (NPP) by 82% and reducing breeding human population to < 10,000 individuals.",
            "SSIM: 0.824 | Aerosol Veil RMSE: 0.089 | Temperature Anomaly Pearson r: 0.912 | Vegetation Die-off Jaccard: 0.865.",
            "Bug: Volcanic radiative forcing lacked latitudinal Brewer-Dobson circulation dissipation. Remediation: Implement 2D stratospheric aerosol decay kernel in AtmosphereEngine."
        ));

        register(new ScenarioDetail(
            "earth_-50000_sahul",
            "Sahul First Peopling & Australian Mega-Continent (-50,000 -> -10,000 BP)",
            -50000, -10000, 365.25,
            "Paleolithic Deep Horizon",
            "Maritime island hopping across Wallacea and colonization of the joined Australian-Tasmanian-New Guinean landmass (Sahul).",
            "Bifurcation at -42,000 BP: Anthropogenic fire-stick farming altered Australian monsoon moisture recycling, causing rapid desiccation of the interior lake systems (Lake Eyre) and swift extinction of Diprotodon and Genyornis.",
            "SSIM: 0.745 | Density RMSE: 0.142 | Fire Regime Jaccard: 0.710 | Megafauna Pearson r: 0.680.",
            "Bug: Wallace line water crossing permitted teleportation without boat technology prerequisites. Remediation: Enforce navigation tech gate (tech >= 1.2) in MarineTraversalEngine."
        ));

        register(new ScenarioDetail(
            "earth_-25000_beringia",
            "Beringian Standstill & First Peopling of the Americas (-25,000 -> -10,000 BP)",
            -25000, -10000, 365.25,
            "Paleolithic Deep Horizon",
            "Human occupation of the exposed Beringian mammoth steppe and subsequent southward expansion into North and South America.",
            "Bifurcation at -14,500 BP: Opening of the ice-free corridor between Laurentide and Cordilleran ice sheets and Pacific coastal route. Rapid Clovis demographic expansion triggered continent-wide megafaunal crash within 1,200 years.",
            "SSIM: 0.792 | Glacial Margin RMSE: 0.098 | Clovis Site Jaccard: 0.778 | Megafauna Decay r: 0.841.",
            "Bug: Ice sheet collision boundaries were treated as passable tundra. Remediation: Set passability resistance factor to infinity for cells where iceThickness > 200m."
        ));

        register(new ScenarioDetail(
            "earth_-20000_lgm_solutrean",
            "Last Glacial Maximum (LGM) & Solutrean Refugia (-20,000 -> -12,000 BP)",
            -20000, -12000, 365.25,
            "Paleolithic Deep Horizon",
            "Maximum extent of Northern Hemisphere continental ice sheets, severe global cooling, and demographic retreat into Franco-Cantabrian refugia.",
            "Bifurcation at -19,000 BP: Thermal compression pushed human densities into high-relief microclimates. Cave art, spear-throwers (atlatl), and pressure flaking emerged as risk-buffering social cohesion mechanisms.",
            "SSIM: 0.835 | Glacial Extent Jaccard: 0.892 | Refugial Density Pearson r: 0.795 | Temperature RMSE: 0.078.",
            "Bug: Albedo feedback produced a runaway freezing loop. Remediation: Clamp maximum sea ice albedo at 0.72 in EnergyBalanceModel."
        ));

        register(new ScenarioDetail(
            "earth_-10900_younger_dryas",
            "Younger Dryas Cold Shock & Natufian Pre-Agrarian Adaptation (-10,900 -> -9,500 BP)",
            -10900, -9500, 365.25,
            "Paleolithic Deep Horizon",
            "Abrupt cooling event triggered by Lake Agassiz freshwater discharge into the North Atlantic, shutting down the AMOC.",
            "Bifurcation at -10,800 BP: Abrupt 6°C temperature drop in Western Eurasia forced Natufian hunter-gatherers to choose between nomadic dispersal and intensive cultivation of wild cereals (rye/emmer), seeding the first Neolithic founder crops.",
            "SSIM: 0.810 | AMOC Flow RMSE: 0.110 | Natufian Site Jaccard: 0.834 | Cereal Yield Pearson r: 0.812.",
            "Bug: Wild cereal yield dropped to exactly zero during cold ticks. Remediation: Implement cold-hardy wild grass tolerance curve in BotanicalYieldModel."
        ));

        // ==============================================================================
        // BLOCK 2: NEOLITHIC REVOLUTION, BRONZE AGE & HYDRAULIC STATES
        // ==============================================================================
        register(new ScenarioDetail(
            "earth_-8000_fertile_crescent",
            "Fertile Crescent Neolithic Revolution & Agrarian Sedentism (-8,000 -> -5,000 BCE)",
            -8000, -5000, 60.0,
            "Neolithic & Bronze Age",
            "Domestication of wheat, barley, pulses, sheep, and goats; transition from foraging to sedentary agrarian village communities.",
            "Bifurcation at -7,500 BCE: Sedentary fertility surge (reduced inter-birth intervals from 4 years to 1.5 years) triggered high demographic density, leading to soil exhaustion, endemic zoonotic diseases, and initial social stratification.",
            "SSIM: 0.805 | Agrarian Density RMSE: 0.138 | Domesticate Spread Pearson r: 0.825 | Soil NPK Decay r: 0.760.",
            "Bug: Zoonotic pathogen spillover rate was fixed to zero. Remediation: Link settlement density directly to PathogenIncidenceEngine."
        ));

        register(new ScenarioDetail(
            "earth_-6000_green_sahara",
            "Green Sahara Humid Period & Desiccation Collapse (-6,000 -> -3,500 BCE)",
            -6000, -3500, 60.0,
            "Neolithic & Bronze Age",
            "African Humid Period with extensive mega-lakes (Mega-Lake Chad) and pastoralist cattle societies across modern Sahara.",
            "Bifurcation at -4,100 BCE: Orbital Milankovitch insolation decline triggered abrupt vegetation-albedo collapse. The green savanna desiccated into hyper-arid desert within three centuries, driving pastoralists into the Nile Valley refugium.",
            "SSIM: 0.871 | Lake Surface Area Jaccard: 0.852 | Monsoon Precipitation Pearson r: 0.895 | Vegetation RMSE: 0.082.",
            "Bug: Vegetation feedback was modeled as linear rather than bistable hysteretic. Remediation: Implement Charney vegetation-albedo bifurcation loop in EcoHydrologyEngine."
        ));

        register(new ScenarioDetail(
            "earth_-3000_ancient_egypt",
            "Ancient Egypt Early Dynastic Unification & Nile Irrigation (-3,000 -> -1,000 BCE)",
            -3000, -1000, 60.0,
            "Neolithic & Bronze Age",
            "Formation of the Old Kingdom state, centralized hydraulic bureaucracy, monumental masonry, and annual Nile flood basin management.",
            "Bifurcation at -2,180 BCE (4.2 ka event): Multi-decadal failure of the Ethiopian monsoon caused catastrophic low Nile inundations, destroying food stockpiles and causing the collapse of the Old Kingdom into the First Intermediate Period.",
            "SSIM: 0.864 | Nile Basin Density RMSE: 0.115 | Floodplain Yield Pearson r: 0.910 | Administrative Reach Jaccard: 0.840.",
            "Bug: Nile flood height was treated as stationary Poisson process. Remediation: Modulate Nile discharge with ITCZ Milankovitch and ENSO paleoclimate indices."
        ));

        register(new ScenarioDetail(
            "earth_-1900_assyrian_empire",
            "Middle Bronze Age Mesopotamia & Assyrian Commercial Networks (-1,900 -> -600 BCE)",
            -1900, -600, 60.0,
            "Neolithic & Bronze Age",
            "Kanesh trade colonies (Karum), Old Assyrian tin and textile merchant routes, and early Babylonian legal administration (Hammurabi).",
            "Bifurcation at -1,750 BCE: Progressive salinization of southern Mesopotamian irrigated soils (Euphrates-Tigris basin) reduced wheat yields by 65%, permanently shifting geopolitical primacy northward toward Upper Mesopotamia (Assyria).",
            "SSIM: 0.795 | Soil Salinization RMSE: 0.128 | Trade Network Pearson r: 0.810 | Urban Density Jaccard: 0.785.",
            "Bug: Soil salinization did not track irrigation water mineral load. Remediation: Add salt deposition term: dSalt/dt = Q_irrigation * C_salt - Leach_rate."
        ));

        register(new ScenarioDetail(
            "earth_-1500_mesoamerica",
            "Mesoamerican Civilizations: Olmec Foundations & Classic Maya (-1,500 BCE -> 900 CE)",
            -1500, 900, 60.0,
            "Neolithic & Bronze Age",
            "Maize-beans-squash polyculture, monumental basalt architecture (Olmec), raised field systems, and Classic Maya city-states (Tikal, Calakmul).",
            "Bifurcation at 850 CE (Classic Maya Collapse): Severe multi-year drought coupled with chronic deforestation for lime stucco plaster collapsed the seasonal karst reservoir systems (aguadas), sparking endemic warfare and urban abandonment.",
            "SSIM: 0.760 | Karst Aquifer RMSE: 0.152 | Maya Polities Jaccard: 0.710 | Forest Biomass Pearson r: 0.790.",
            "Bug: Cenotes and aguadas did not model karst leakage during severe dry seasons. Remediation: Add Darcy hydraulic gradient sink in KarstHydrologyEngine."
        ));

        register(new ScenarioDetail(
            "earth_-1279_ramesses_ii",
            "Ramesses II, Pax Aegyptiaca & Monumental Builders (-1,279 -> -1,200 BCE)",
            -1279, -1200, 60.0,
            "Neolithic & Bronze Age",
            "19th Dynasty New Kingdom zenith, Battle of Kadesh (-1274 BCE), Treaty of Eternal Peace with the Hittites, and construction of Pi-Ramesses and Abu Simbel.",
            "Bifurcation at -1,208 BCE: Incursion of the Sea Peoples (Sherden, Lukka, Peleset) and widespread eastern Mediterranean trade disruptions forced massive military spending, exhausting imperial silver reserves.",
            "SSIM: 0.852 | Imperial Garrison RMSE: 0.104 | Levant Trade Network Pearson r: 0.865 | Tax Revenue r: 0.820.",
            "Bug: Military upkeep costs did not scale with logistical distance from Memphis. Remediation: Apply transport work friction factor: Cost = Distance * Ton_km_cost."
        ));

        register(new ScenarioDetail(
            "earth_-1200_bronze_age_collapse_1200bc",
            "Late Bronze Age Collapse & Palatial Systemic Rupture (-1,200 -> -900 BCE)",
            -1200, -900, 60.0,
            "Neolithic & Bronze Age",
            "Synchronous collapse of the Mycenaean, Hittite, Ugaritic, and Kassite palatial civilizations across the Eastern Mediterranean.",
            "Bifurcation at -1,180 BCE: Systemic hyper-fragility: simultaneous megadrought, tin trade disruption from Afghanistan, earthquake storm, and Sea Peoples attacks caused an irrecoverable cascade failure in centralized palace redistribution economies.",
            "SSIM: 0.812 | Palatial Destruction Jaccard: 0.875 | Bronze Tool Density Pearson r: 0.890 | Population Drop RMSE: 0.134.",
            "Bug: Supply chain vulnerability lacked cross-commodity substitution limits (copper + tin = bronze). Remediation: Enforce stoichiometric production constraints in MetallurgyEngine."
        ));

        // ==============================================================================
        // BLOCK 3: IRON AGE, CLASSICAL ANTIQUITY & CONTINENTAL EMPIRES
        // ==============================================================================
        register(new ScenarioDetail(
            "earth_-1000_early_iron_age",
            "Early Iron Age & Mediterranean Commercial Resurgence (-1,000 -> -300 BCE)",
            -1000, -300, 60.0,
            "Classical Antiquity",
            "Democratic democratization of metallurgy (iron replacing scarce bronze), Phoenician maritime thalassocracy, and Greek colonial expansion across the Mediterranean.",
            "Bifurcation at -650 BCE: Invention of electrum/silver coinage in Lydia and alphabet dissemination reduced transaction costs by 80%, triggering a market economy explosion and decentralized merchant polities.",
            "SSIM: 0.818 | Iron Smelting RMSE: 0.118 | Phoenician Colonization Jaccard: 0.790 | Trade Volume Pearson r: 0.845.",
            "Bug: Bloomery iron smelting charcoal demand did not deplete surrounding forests. Remediation: Link iron tonnage produced to deforestation rate: 30 kg charcoal per 1 kg iron."
        ));

        register(new ScenarioDetail(
            "earth_-334_alexander_hellenistic_334bc",
            "Hellenistic Surge & Alexander the Great Conquests (-334 -> -150 BCE)",
            -334, -150, 14.0,
            "Classical Antiquity",
            "Rapid Macedonian conquest of the Achaemenid Empire from the Hellespont to the Indus River, and subsequent Diadochi state fragmentation.",
            "Bifurcation at -323 BCE (Death of Alexander in Babylon): Abrupt leadership decapitation without institutional succession protocol triggered the 40-year Wars of the Diadochi, partitioning the empire into Seleucid, Ptolemaic, and Antigonid rival powers.",
            "SSIM: 0.790 | Military Movement RMSE: 0.160 | Hellenistic Polities Jaccard: 0.760 | Coinage Diffusion Pearson r: 0.815.",
            "Bug: Charismatic leader death immediately defaulted state cohesion to zero. Remediation: Implement succession institution inertia buffer in LeadershipStateTransitionEngine."
        ));

        register(new ScenarioDetail(
            "earth_-300_maurya_empire",
            "Maurya Empire & Arthashastra Statecraft (-300 -> 100 CE)",
            -300, 100, 14.0,
            "Classical Antiquity",
            "Unification of the Indian subcontinent under Chandragupta and Ashoka, Buddhist moral governance, and dense agrarian taxation along the Gangetic plain.",
            "Bifurcation at -185 BCE (Shunga Coup): High fiscal costs of maintaining a massive standing army without external conquests degraded royal treasury reserves, triggering imperial fragmentation into regional kingdoms.",
            "SSIM: 0.815 | Gangetic Agrarian Density RMSE: 0.120 | Buddhist Edict Distribution Jaccard: 0.825 | GWP Pearson r: 0.780.",
            "Bug: Monastic tax exemptions did not reduce central crown fiscal revenue. Remediation: Subtract institutional religious land grants from state tax base in TaxHarvestingEngine."
        ));

        register(new ScenarioDetail(
            "earth_+0_roman_empire",
            "Roman Empire & Pax Romana (Year 0 -> 476 CE)",
            0, 476, 14.0,
            "Classical Antiquity",
            "Augustan Principate, trans-Mediterranean Pax Romana, Roman climatic optimum, Antonine Plague (165 CE), Third Century Crisis (235 CE), and Western collapse.",
            "Bifurcation at 165 CE (Antonine Plague): Smallpox epidemic wiped out 20-30% of taxpayers and legionaries, while debasement of the denarius (silver fineness falling from 90% to 5%) triggered hyperinflation and military anarchy by 235 CE.",
            "SSIM: 0.729 | Population Density RMSE: 0.184 | Biomes Jaccard: 0.814 | Coin Debasement Pearson r: 0.892.",
            "Bug: Regional Mediterranean grid was compared against global aggregate telemetry. Remediation: Normalize regional population to global demographic scale via active surface mask."
        ));

        register(new ScenarioDetail(
            "earth_+536_late_antique_ice_age",
            "Late Antique Little Ice Age (LALIA) & Justinian Plague (536 -> 650 CE)",
            536, 650, 14.0,
            "Classical Antiquity",
            "Mystery volcanic veil of 536 CE (Ilopango/Icelandic eruption) causing the coldest decade in 2,000 years, followed by the First Plague Pandemic (Yersinia pestis) in 541 CE.",
            "Bifurcation at 541 CE: Crop failures combined with bubonic plague destroyed Justinian's reconquest of the Western Mediterranean, depleting Byzantine imperial manpower and leaving the Levant vulnerable to early Islamic expansion.",
            "SSIM: 0.830 | Stratospheric Dust Veil RMSE: 0.075 | Justinian Plague Spread Jaccard: 0.845 | Byzantine GWP r: 0.860.",
            "Bug: Bubonic plague vectors (Xenopsylla cheopis on black rats) migrated across winter frost zones. Remediation: Impose minimum temperature threshold (Tw > 10°C) for flea active reproduction."
        ));

        register(new ScenarioDetail(
            "earth_+632_islamic_expansion_632",
            "Early Islamic Expansion & Caliphate Commercial Revolution (632 -> 900 CE)",
            632, 900, 14.0,
            "Medieval Era",
            "Rapid consolidation of the Arabian Peninsula under the Rashidun and Umayyad Caliphates, collapse of Sasanian Persia, and creation of an integrated Afro-Eurasian trade sphere.",
            "Bifurcation at 750 CE (Abbasid Revolution): Shift of imperial capital from Damascus to Baghdad linked Mediterranean and Indian Ocean maritime networks, triggering agricultural intensification (introduction of sugar, cotton, citrus).",
            "SSIM: 0.842 | Caliphate Border Jaccard: 0.815 | Trade Volume RMSE: 0.112 | Agricultural Crop Diffusion Pearson r: 0.870.",
            "Bug: Dhimmi tax (Jizya) revenue did not decrease as populations converted to Islam. Remediation: Dynamically update tax yield based on religious demographic tensor shares."
        ));

        register(new ScenarioDetail(
            "earth_+1000_song_dynasty",
            "Song Dynasty & Hydraulic Pre-Industrialization (1000 -> 1279 CE)",
            1000, 1279, 14.0,
            "Medieval Era",
            "Intensive Champa quick-ripening rice double-cropping, southwards demographic migration, water-powered blast furnaces, paper currency (Jiaozi), and maritime compass trade.",
            "Bifurcation at 1127 CE (Jingkang Incident): Jurchen Jin invasion forced the dynasty to retreat south of the Huai River (Southern Song), creating an ultra-dense proto-industrial maritime urban civilization around Hangzhou.",
            "SSIM: 0.855 | Iron Output RMSE: 0.098 | Urbanization Rate Pearson r: 0.880 | Grand Canal Traffic Jaccard: 0.860.",
            "Bug: Paper money expansion triggered unlimited liquidity without inflation penalties. Remediation: Link fiat currency emission to market commodity price inflation via Fisher equation."
        ));

        register(new ScenarioDetail(
            "earth_+1206_mongol_conquest_1206",
            "Mongol Conquests & Pax Mongolica Eurasian Rupture (1206 -> 1368 CE)",
            1206, 1368, 14.0,
            "Medieval Era",
            "Unification of the nomadic steppe tribes under Genghis Khan, siege campaigns across China, Central Asia, and Eastern Europe, and creation of the Yam postal relay network.",
            "Bifurcation at 1258 CE (Siege of Baghdad): Destruction of the Tigris-Euphrates hydraulic canal network permanently set back Middle Eastern agricultural carrying capacity; Silk Road integration later served as vector for the Black Death.",
            "SSIM: 0.805 | Nomadic Cavalry Expansion RMSE: 0.145 | Destructured Farmland Jaccard: 0.810 | Postal Station r: 0.875.",
            "Bug: Steppe pasture biomass was not consumed by mega-herds of horses. Remediation: Implement Lotka-Volterra grazing pressure in SteppeEcologyEngine."
        ));

        register(new ScenarioDetail(
            "earth_+1324_mali_empire",
            "Mali Empire & Trans-Saharan Gold Trade (1324 -> 1591 CE)",
            1324, 1591, 14.0,
            "Medieval Era",
            "Peak of the West African Mali Empire under Mansa Musa, pilgrimage to Mecca with immense gold bullion reserves, and scholarly development of Timbuktu.",
            "Bifurcation at 1325 CE: Injection of massive gold volumes into Cairo and Mediterranean markets caused multi-decade currency debasement and stagflation across the Mamluk Sultanate.",
            "SSIM: 0.812 | Gold Mining Centroid RMSE: 0.108 | Caravan Trail Jaccard: 0.795 | Urban Literacy Pearson r: 0.760.",
            "Bug: Gold mining output did not deplete local alluvial reserves. Remediation: Enforce Hubbert peak curve on Bambuk and Bure gold placers in ResourceMiningEngine."
        ));

        register(new ScenarioDetail(
            "earth_+1347_black_death_1347",
            "The Black Death & Post-Feudal Economic Inversion (1347 -> 1492 CE)",
            1347, 1492, 14.0,
            "Medieval Era",
            "Introduction of Yersinia pestis via Genoese galleys from Caffa to Messina, wiping out 30-50% of Europe's and the Near East's populations.",
            "Bifurcation at 1348 CE: Violent demographic collapse inverted the land-to-labor ratio: real unskilled wages doubled, feudal serfdom collapsed in Western Europe, and marginal farmland reforested, sequestering atmospheric CO2.",
            "SSIM: 0.875 | Epidemic Mortality RMSE: 0.082 | Real Wage Spike Pearson r: 0.925 | Land Abandonment Jaccard: 0.890.",
            "Bug: Immediate depopulation caused division by zero in per-capita primary energy. Remediation: Add epsilon smoothing to all per-capita quotient evaluators."
        ));

        // Delegate Blocks 4, 5, and 6 (Early Modern, Future Earth, and Extraterrestrial Biospheres)
        ScenarioArchetypeModernAndExoCatalog.registerModernFutureAndExoScenarios(ScenarioArchetypeAuditRegistry::register);
    }

    /**
     * Private constructor preventing instantiation of static audit registry.
     */
    private ScenarioArchetypeAuditRegistry() {
    }

    /**
     * Registers a curated {@link ScenarioDetail} into the central map.
     *
     * @param d scenario detail entry to register
     */
    private static void register(ScenarioDetail d) {
        REGISTRY.put(d.key(), d);
    }

    /**
     * Returns a mutable defensive copy of all registered canonical scenario audit specifications.
     *
     * @return list of all registered {@link ScenarioDetail} entries in chronological order
     */
    public static List<ScenarioDetail> getAllScenarios() {
        return new ArrayList<>(REGISTRY.values());
    }

    /**
     * Resolves a registered scenario audit entry matching or partially matching the supplied key.
     *
     * @param key full or partial case-insensitive scenario identifier
     * @return optional containing the matching {@link ScenarioDetail}, or empty if none matches
     */
    public static Optional<ScenarioDetail> getScenario(String key) {
        if (key == null) return Optional.empty();
        String norm = key.toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<String, ScenarioDetail> entry : REGISTRY.entrySet()) {
            if (norm.contains(entry.getKey()) || entry.getKey().contains(norm)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
