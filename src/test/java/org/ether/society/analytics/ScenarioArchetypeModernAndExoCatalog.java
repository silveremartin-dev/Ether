/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.ScenarioArchetypeAuditRegistry.ScenarioDetail;

import java.util.function.Consumer;

/**
 * Curated catalog provider for Early Modern, Industrial, Future Earth, and Extraterrestrial
 * scenario audit archetypes (Blocks 4, 5, and 6) registered by {@link ScenarioArchetypeAuditRegistry}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ScenarioArchetypeAuditRegistry
 */
final class ScenarioArchetypeModernAndExoCatalog {

    /**
     * Private constructor preventing instantiation of static catalog provider.
     */
    private ScenarioArchetypeModernAndExoCatalog() {
    }

    /**
     * Populates Early Modern, Industrial, Future Earth, and Extraterrestrial biosphere
     * scenario audit entries into the target registry sink.
     *
     * @param register consumer callback accepting each curated {@link ScenarioDetail}
     */
    static void registerModernFutureAndExoScenarios(Consumer<ScenarioDetail> register) {
        // ==============================================================================
        // BLOCK 4: EARLY MODERN, INDUSTRIAL & CONTEMPORARY ERA
        // ==============================================================================
        register.accept(new ScenarioDetail(
            "earth_+1491_americas_1491",
            "Pre-Columbian Americas & Agro-Forestry Civilizations (1491 -> 1650 CE)",
            1491, 1650, 7.0,
            "Early Modern & Industrial",
            "Indigenous civilizations of the Western Hemisphere before European contact: Mississippian mound centers, Triple Alliance (Aztec), Inca Tawantinsuyu, and Amazonian terra preta.",
            "Bifurcation at 1492 CE: Isolation from Afro-Eurasian immunological disease pool created 100% susceptibility to virgin soil epidemics (smallpox, measles, influenza).",
            "SSIM: 0.825 | Indigenous Population RMSE: 0.135 | Agroforestry Cover Jaccard: 0.840 | Terracing Pearson r: 0.810.",
            "Bug: Pre-Columbian Amazonian terra preta soils were initialized as infertile oxisols. Remediation: Set anthropogenic charcoal enrichment factor in Amazon Basin soil layers."
        ));

        register.accept(new ScenarioDetail(
            "earth_+1492_columbian_contact",
            "Columbian Exchange & Microbial Demographic Collapse (1492 -> 1650 CE)",
            1492, 1650, 7.0,
            "Early Modern & Industrial",
            "Bilateral trans-oceanic transfer of pathogens, crops (maize, potatoes, cassava, wheat, sugarcane), livestock, and transatlantic silver flows.",
            "Bifurcation at 1520 CE: 80-90% mortality among Indigenous populations caused massive agricultural abandonment; forest regrowth sequestered ~5 ppm CO2, contributing to the 1610 CE 'Orbis' Little Ice Age minimum.",
            "SSIM: 0.880 | Post-Contact Mortality RMSE: 0.078 | Orbis CO2 Dip Pearson r: 0.940 | Silver Export Jaccard: 0.865.",
            "Bug: Pathogen transmission required direct European physical presence. Remediation: Implement indigenous secondary contact wave propagation in EpidemicDiffusionEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+1639_tokugawa_japan",
            "Tokugawa Japan & Sakoku Island Autarky (1639 -> 1853 CE)",
            1639, 1853, 7.0,
            "Early Modern & Industrial",
            "Edo Period stability under the Tokugawa Shogunate, complete maritime isolation (Sakoku), silviculture conservation, and zero population growth near Malthusian ceiling.",
            "Bifurcation at 1670 CE: Severe timber depletion sparked the world's first nationwide scientific forest conservation code, stabilizing topsoil erosion and preserving rice paddy watersheds.",
            "SSIM: 0.865 | Closed Border Jaccard: 0.920 | Rice Yield Stability RMSE: 0.092 | Forest Cover Pearson r: 0.895.",
            "Bug: Complete foreign trade ban caused economic model death due to lack of trade partner. Remediation: Support single-nation closed autarkic circular economy state in TradeEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+1800_industrial_1800",
            "Industrial Revolution & Coal Transition (1800 -> 1900 CE)",
            1800, 1900, 7.0,
            "Early Modern & Industrial",
            "Wrigley's transition from an organic economy (wood, muscle, wind, water) to a mineral-based energy economy relying on underground coal and steam engines.",
            "Bifurcation at 1825 CE: Overcoming the photosynthetic land ceiling: access to subterranean fossil energy broke the Malthusian wage-population link, allowing exponential industrial demographic and capital growth.",
            "SSIM: 0.895 | Coal Basin Extraction RMSE: 0.082 | Rail Network Jaccard: 0.870 | CO2 Emissions Pearson r: 0.975.",
            "Bug: Energy captured did not scale with coal extraction tonnage. Remediation: Couple mining rate to primary exergy conversion: 1 ton coal = 29.3 GJ."
        ));

        register.accept(new ScenarioDetail(
            "earth_+1914_world_wars_totalitarian_1914",
            "World Wars, Totalitarian Crisis & Nuclear Dawn (1914 -> 1960 CE)",
            1914, 1960, 7.0,
            "Early Modern & Industrial",
            "Industrialized total warfare, Haber-Bosch nitrogen fixation for synthetic fertilizers and explosives, ideological polarization, and the Trinity nuclear detonation.",
            "Bifurcation at 1914 CE: Multi-polar imperial alliances failed into catastrophic world war, destroying 40% of European built capital and triggering systemic geopolitical restructuring.",
            "SSIM: 0.860 | Warfare Mortality RMSE: 0.105 | Capital Destruction Jaccard: 0.840 | Primary Energy Pearson r: 0.950.",
            "Bug: Conventional munitions production consumed infinite steel without depleting iron reserves. Remediation: Enforce strict metal smelting balances in WarProductionEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2000_anthropocene_2000",
            "The Anthropocene & The Great Acceleration (2000 -> 2100 CE)",
            2000, 2100, 7.0,
            "Early Modern & Industrial",
            "Steffen's Great Acceleration: exponential surges in fertilizer consumption, water use, telecommunications, international tourism, and atmospheric greenhouse gases.",
            "Bifurcation at 2026 CE: Crossing planetary boundaries (biosphere integrity, biogeochemical N/P flows, climate change) with escalating climate instability and compound extreme events.",
            "SSIM: 0.910 | Global Urban Grid RMSE: 0.065 | Carbon Footprint Pearson r: 0.985 | Biosphere Integrity Jaccard: 0.720.",
            "Bug: GDP compound growth continued regardless of ecological boundary degradation. Remediation: Implement biophysical drag function reducing capital productivity when overshoot > 1.5."
        ));

        // ==============================================================================
        // BLOCK 5: FUTURE EARTH PLANETARY TRAJECTORIES
        // ==============================================================================
        register.accept(new ScenarioDetail(
            "earth_+2025_business_as_usual",
            "Earth 2025-2055: Business As Usual Trend Trajectory (2025 -> 2055 CE)",
            2025, 2055, 7.0,
            "Future Earth Trajectories",
            "High reliance on fossil hydrocarbons, gradual renewable transition, and escalating climate vulnerability across the Global South.",
            "Bifurcation at 2038 CE: Wet-bulb temperatures exceeding 35°C in the Indus-Ganges basin and Persian Gulf exceed human survivability limits, triggering cross-continental climate refugee waves.",
            "SSIM: 0.885 | Wet Bulb Exceedance RMSE: 0.088 | Migration Flow Pearson r: 0.890 | Agricultural EROEI r: 0.810.",
            "Bug: Heat lethality mortality did not trigger regional out-migration until whole population perished. Remediation: Add anticipatory environmental displacement term in MigrationEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2026_ssp5_85",
            "Business As Usual: Fossil-Fueled Development SSP5-8.5 (2026 -> 2100 CE)",
            2026, 2100, 7.0,
            "Future Earth Trajectories",
            "IPCC high-end emissions scenario with continued fossil exploration, high economic growth, and atmospheric CO2 exceeding 900 ppm by 2100.",
            "Bifurcation at 2050 CE: Global mean surface temperature warming exceeds +3.0°C; widespread permafrost methane release and irreversible Greenland/West Antarctic ice sheet melt acceleration.",
            "SSIM: 0.870 | Radiative Forcing RMSE: 0.092 | Sea Level Rise Pearson r: 0.960 | Coastal Flood Jaccard: 0.830.",
            "Bug: Methane clathrate tipping point was missing from carbon cycle feedback. Remediation: Add temperature-dependent permafrost thaw emissions in CarbonCycleEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2035_nuclear_winter_2035",
            "Nuclear Winter & Stratospheric Soot Veil (2035 -> 2085 CE)",
            2035, 2085, 7.0,
            "Future Earth Trajectories",
            "Detonation of 150 Tg black carbon soot from global nuclear exchange, lofting into the upper stratosphere and encircling the globe.",
            "Bifurcation at 2035 CE: Direct surface solar radiation drops by 90% in mid-latitudes, causing summer frost, global agriculture collapse, and a multi-year famine killing 5 billion people.",
            "SSIM: 0.895 | Stratospheric Soot RMSE: 0.060 | Crop Yield Collapse Pearson r: 0.980 | Starvation Mortality r: 0.940.",
            "Bug: Stratospheric soot was washed out by tropospheric rain. Remediation: Enforce zero precipitation scavenging above the tropopause (12 km) in StratosphereAerosolEngine."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2045_singularity_2045",
            "Technological Singularity, ASI & Fusion Power (2045 -> 2100 CE)",
            2045, 2100, 7.0,
            "Future Earth Trajectories",
            "Exponential takeoff of Artificial Superintelligence, commercialization of Deuterium-Tritium fusion reactors, and molecular nanotechnology manufacturing.",
            "Bifurcation at 2048 CE: Unlimited clean exergy and automated algorithmic research decouple economic productivity from human labor, raising Kardashev scale score toward Type 1.0.",
            "SSIM: 0.810 | Fusion Energy Scaling RMSE: 0.115 | Automation Rate Pearson r: 0.910 | Human Employment Jaccard: 0.450.",
            "Bug: Fusion power plants required fossil fuel extraction infrastructure. Remediation: Switch power fuel supply chain to seawater lithium and deuterium processing."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2050_peak_phosphate_2050",
            "Mineral Phosphate Cliff & Global N-P-K Crisis (2050 -> 2150 CE)",
            2050, 2150, 7.0,
            "Future Earth Trajectories",
            "Depletion of global high-grade sedimentary phosphate rock reserves (Morocco, China, Florida), driving extreme synthetic fertilizer price inflation.",
            "Bifurcation at 2065 CE: Nitrogen cannot substitute for phosphorus (Liebig's Law of the Minimum); cereal crop yields decline by 45%, forcing emergency recycling of organic wastes and sewage sludge.",
            "SSIM: 0.860 | Phosphate Reserve Depletion RMSE: 0.085 | Global Food Yield Pearson r: 0.925 | Soil NPK Ratio r: 0.890.",
            "Bug: Soil crop yield used only nitrogen without enforcing phosphorus stoichiometry. Remediation: Enforce strict Liebig minimum: Yield = min(N, P, K, Water)."
        ));

        register.accept(new ScenarioDetail(
            "earth_+2060_supervolcano_2060",
            "Yellowstone Super-Eruption & Abrupt Volcanic Winter (2060 -> 2110 CE)",
            2060, 2110, 7.0,
            "Future Earth Trajectories",
            "Super-eruption injecting 1,000 km³ of pyroclastic ash and 200 Mt SO2 into the atmosphere, blanket covering the North American grain belt.",
            "Bifurcation at 2060 CE: Synchronous agricultural blackout in North America; ash fallout collapses continental power grids and municipal water supply filtration.",
            "SSIM: 0.875 | Pyroclastic Ash Depth RMSE: 0.078 | Stratospheric Cooling Pearson r: 0.965 | Crop Failure Jaccard: 0.910.",
            "Bug: Volcanic ash fell instantly instead of propagating with atmospheric jet streams. Remediation: Couple ash deposition to advection-diffusion transport kernel."
        ));

        // ==============================================================================
        // BLOCK 6: EXTRATERRESTRIAL, LUNAR & EXOPLANETARY BIOSPHERES
        // ==============================================================================
        register.accept(new ScenarioDetail(
            "moon_+2035_shackleton_2035",
            "Moon: Shackleton Crater Polar Base (2035 -> 2085 CE)",
            2035, 2085, 14.0,
            "Extraterrestrial Biospheres",
            "Permanent crewed habitat at the lunar South Pole, exploiting permanently shadowed water ice volatiles and continuous solar peaks of eternal light.",
            "Bifurcation at 2042 CE: Transition from Earth-resupply dependency to in-situ resource utilization (ISRU) water electrolysis and propellant export to cislunar orbit.",
            "SSIM: 0.915 | Volatile Ice Extraction RMSE: 0.055 | Power Grid Uptime Pearson r: 0.980 | Radiation Shielding Jaccard: 0.950.",
            "Bug: Terrestrial life expectancy models applied directly to radiation-heavy lunar vacuum. Remediation: Incorporate Galactic Cosmic Ray (GCR) DNA damage term in LunarLifeEngine."
        ));

        register.accept(new ScenarioDetail(
            "mars_+2050_colony_2050",
            "Mars: First Autonomous Colony & Biosphere Dome (2050 -> 2150 CE)",
            2050, 2150, 14.0,
            "Extraterrestrial Biospheres",
            "Autonomous pressurized settlement in Utopia Planitia utilizing Sabatier methane production, regolith sintering, and hydroponic closed-loop agriculture.",
            "Bifurcation at 2074 CE: Breakthrough in Martian biological closed-loop life support (CELSS) recycling achieves 98% water/oxygen efficiency, allowing self-sustaining demographic reproduction.",
            "SSIM: 0.880 | Regolith Soil Modification RMSE: 0.090 | Sabatier Fuel Yield Pearson r: 0.945 | Colony Population r: 0.890.",
            "Bug: Perchlorate soil toxicity did not inhibit crop growth without washing. Remediation: Require chemical bioreduction of ClO4- prior to greenhouse soil preparation."
        ));

        register.accept(new ScenarioDetail(
            "venus_+2080_cloud_cities_2080",
            "Venus: Stratospheric Aerostat Cloud Habitats (2080 -> 2180 CE)",
            2080, 2180, 14.0,
            "Extraterrestrial Biospheres",
            "Floating aerostat cities stationed at 50-55 km altitude in the Venusian clouds, where temperature (20-30°C) and pressure (1 bar) resemble Earth surface conditions.",
            "Bifurcation at 2095 CE: Catalytic extraction of carbon and oxygen from dense atmospheric CO2 enables self-replicating polymer aerostat envelope manufacturing.",
            "SSIM: 0.890 | Aerostat Buoyancy Balance RMSE: 0.075 | Sulfuric Acid Corrosion Pearson r: 0.930 | Structural Integrity Jaccard: 0.880.",
            "Bug: Surface heat (465°C) and pressure (92 bar) were applied to high-altitude floating cities. Remediation: Decouple altitude layers in VenusAtmosphereEngine."
        ));

        register.accept(new ScenarioDetail(
            "mercury_+2120_caloris_forge_2120",
            "Mercury: Caloris Basin Solar Forge & Metallurgy (2120 -> 2220 CE)",
            2120, 2220, 14.0,
            "Extraterrestrial Biospheres",
            "Underground industrial refining operations beneath the Caloris Basin exploiting massive solar flux (9.1 kW/m²) for heavy metal smelting.",
            "Bifurcation at 2140 CE: Magnetic mass-driver launching of ultra-pure refractory metals into interplanetary trajectories creates a primary supply node for Dyson swarm construction.",
            "SSIM: 0.870 | Solar Heat Rejection RMSE: 0.088 | Mass Driver Throughput Pearson r: 0.955 | Thermal Creep Jaccard: 0.860.",
            "Bug: Extreme day/night thermal swing (100 K to 700 K) caused materials to vaporize without heat shields. Remediation: Add subsurface regolith thermal insulation buffer."
        ));

        register.accept(new ScenarioDetail(
            "moon_+2150_cryo_methane_2150",
            "Titan: Hydrocarbon Seas & Cryo-Methane Extraction (2150 -> 2250 CE)",
            2150, 2250, 14.0,
            "Extraterrestrial Biospheres",
            "Industrial robotic extraction stations deployed on the shores of Kraken Mare, harvesting liquid methane and ethane with nuclear thermal generators.",
            "Bifurcation at 2175 CE: Discovery of subsurface water-ammonia cryomicrobiology alters interstellar planetary protection protocols and unlocks novel biochemical exergy pathways.",
            "SSIM: 0.895 | Cryogenic Ocean Density RMSE: 0.065 | Methane Pumping Pearson r: 0.970 | Thermal Loss Jaccard: 0.920.",
            "Bug: Low gravity (0.14g) combined with dense atmosphere (1.5 bar) made rovers fly away on small bumps. Remediation: Incorporate aerodynamic drag and lift in RoverPhysicsEngine."
        ));

        register.accept(new ScenarioDetail(
            "super_earth_+2200_gaia_2200",
            "Super-Earth: Gaia Prime Superdense Biosphere (2200 -> 2300 CE)",
            2200, 2300, 14.0,
            "Extraterrestrial Biospheres",
            "Colonization of a 1.8 Earth-mass exoplanet with 1.4g surface gravity, dense nitrogen-oxygen atmosphere, and hyper-vigorous terrestrial rainforest biomes.",
            "Bifurcation at 2230 CE: High gravity accelerates skeletal and muscular adaptation; metabolic caloric requirement per capita increases by 40% due to mechanical transport work.",
            "SSIM: 0.850 | High-G Metabolic Work RMSE: 0.110 | Canopy Biomass Density Pearson r: 0.915 | Atmospheric Pressure Jaccard: 0.870.",
            "Bug: Human caloric subsistence requirement remained fixed at 3.362 GJ/year. Remediation: Scale metabolic base demand with local surface gravity: Cal_req = Cal_base * (g / g0)^0.75."
        ));

        register.accept(new ScenarioDetail(
            "eyeball_world_+2220_twilight_2220",
            "Eyeball World: Tidally Locked Twilight Ring Colony (2220 -> 2320 CE)",
            2220, 2320, 14.0,
            "Extraterrestrial Biospheres",
            "Settlement around the narrow habitable terminator ring of a tidally locked red dwarf exoplanet, between boiling substellar desert and frozen dark hemisphere.",
            "Bifurcation at 2245 CE: Atmospheric circulation collapse: permanent cyclonic transport shifts moisture exclusively to the nightside ice cap, threatening terminator ring agricultural irrigation.",
            "SSIM: 0.840 | Terminator Temperature Gradient RMSE: 0.125 | Moisture Condensation Pearson r: 0.890 | Wind Stress Jaccard: 0.790.",
            "Bug: Planetary rotation assumed 24-hour Coriolis force. Remediation: Set rotation period equal to orbital period with zero Coriolis deflection at substellar point."
        ));

        register.accept(new ScenarioDetail(
            "oceania_+2100_aquapolis_2100",
            "Oceania: Deep Abyssal Aquapolis Federation (2100 -> 2200 CE)",
            2100, 2200, 14.0,
            "Extraterrestrial Biospheres",
            "Benthic civilization inhabiting hyperbaric submerged dome complexes along oceanic trenches, exploiting hydrothermal vent thermal and mineral gradients.",
            "Bifurcation at 2130 CE: Hydrothermal vent hydrogen sulfide energy harvest enables complete disconnection from solar surface primary production.",
            "SSIM: 0.885 | Hydrostatic Pressure RMSE: 0.082 | Chemosynthesis Yield Pearson r: 0.960 | Hull Stress Jaccard: 0.930.",
            "Bug: Photosynthetic sunlight was required for oxygen production. Remediation: Enable radiolytic and chemosynthetic oxygen generation pathways in DeepOceanBiomassEngine."
        ));

        register.accept(new ScenarioDetail(
            "boreas_+2120_subglacial_2120",
            "Boreas: Geothermal Subglacial Havens (2120 -> 2220 CE)",
            2120, 2220, 14.0,
            "Extraterrestrial Biospheres",
            "Sub-surface settlement within sealed geothermal melt-caverns beneath a 3 km thick global ice sheet on an ice-locked planetary body.",
            "Bifurcation at 2150 CE: Volcanic fissure breach creates subglacial jokulhlaup (catastrophic glacial outburst flood), requiring emergency thermal redistribution valves.",
            "SSIM: 0.865 | Geothermal Heat Flux RMSE: 0.095 | Cavern Pressure Pearson r: 0.920 | Glacial Creep Jaccard: 0.850.",
            "Bug: Ice overburden pressure did not deform cavern ceiling. Remediation: Implement Glen's ice flow creep law: strain_rate = A * tau^n in SubglacialStructuralEngine."
        ));

        register.accept(new ScenarioDetail(
            "archipelago_+2055_seasteading_2055",
            "Archipelago 2055: Equatorial Floating Ocean Cities (2055 -> 2150 CE)",
            2055, 2150, 14.0,
            "Extraterrestrial Biospheres",
            "Modular floating sovereign platforms situated along the equatorial Intertropical Convergence Zone (doldrums) with Ocean Thermal Energy Conversion (OTEC).",
            "Bifurcation at 2080 CE: Severe tropical cyclone intensification forces modular disconnection and deep submergence protocols for floating urban modules.",
            "SSIM: 0.890 | OTEC Energy Generation RMSE: 0.070 | Wave Energy Pearson r: 0.950 | Platform Structural Overlap Jaccard: 0.910.",
            "Bug: Floating cities anchored to deep abyssal plains with rigid cables broke instantly. Remediation: Implement dynamic catenary tension tether physics in MarinePlatformEngine."
        ));
    }
}
