package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates and synchronizes historical epoch cartographic provenance documents, cultural registries,
 * and academic markdown descriptions in accordance with AGENTS.md Rule 6.
 *
 * <p>Produces:
 * <ul>
 *   <li>{@code provenance_and_sources.json}: Comprehensive multi-layer scientific provenance.</li>
 *   <li>{@code cultural_registry.json}: Cultural entities and ethnolinguistic identities registry.</li>
 *   <li>{@code README.md}: Scholarly documentation detailing the two-tier ontological separation ($t = t_0$ vs $t > t_0$)
 *       and empirical citations for all 25 cartographic rasters.</li>
 * </ul>
 * </p>
 */
public final class HistoricalEpochMetadataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalEpochMetadataGenerator.class);

    private HistoricalEpochMetadataGenerator() {
        // Utility class
    }

    /**
     * Generates or refreshes provenance_and_sources.json, cultural_registry.json, and README.md
     * in accordance with AGENTS.md Rule 6.
     *
     * @param earthDir directory path corresponding to the given epoch year
     * @param year the epoch year in astronomical numbering (negative for BCE/BP)
     */
    public static void generateOrUpdateEpochMetadataFiles(Path earthDir, long year) {
        if (earthDir == null) return;
        try {
            File provFile = earthDir.resolve("provenance_and_sources.json").toFile();
            File regFile = earthDir.resolve("cultural_registry.json").toFile();
            File readmeFile = earthDir.resolve("README.md").toFile();

            // 1. Write provenance_and_sources.json
            String provJson = """
            {
              "epoch": %d,
              "era": "Epoch %d",
              "planet": "earth",
              "resolution": "2048x1024",
              "projection": "Equirectangular (Plate Carrée, EPSG:4326)",
              "overview": {
                "summary_en": "Standard 25-raster cartographic and cliodynamic tensor suite for epoch %d.",
                "summary_fr": "Suite cartographique et tensorielle cliodynamique standard à 25 rasters pour l'époque %d."
              },
              "layers": {
                "elevation": {
                  "filename": "earth_%d_elevation.png",
                  "category": "geophysics",
                  "data_sources": ["NOAA ETOPO 2022 15-arc-second Global Relief Model", "GEBCO 2023 Grid Bathymetric Model"],
                  "reconstitution_rationale": "High-fidelity topography and bathymetry calibrated against epoch eustatic sea level offset."
                },
                "biomes": {
                  "filename": "earth_%d_biomes.png",
                  "category": "ecology",
                  "data_sources": ["WorldClim v2.1 Bioclimatic Indicators", "Biome 6000 Project", "CHELSA-Trace21k"],
                  "reconstitution_rationale": "Coupled Holdridge-Whittaker bioclimatic classification driven by empirical temperature and precipitation."
                },
                "temperature": {
                  "filename": "earth_%d_temperature.png",
                  "category": "climate",
                  "data_sources": ["WorldClim v2.1 Bio1 (Annual Mean Temperature)", "PMIP4 Paleoclimate Synthesis"],
                  "reconstitution_rationale": "Empirical baseline modulated by continuous 2D orbital and continental paleoclimatic anomaly field."
                },
                "precipitation": {
                  "filename": "earth_%d_precipitation.png",
                  "category": "climate",
                  "data_sources": ["WorldClim v2.1 Bio12 (Annual Precipitation)", "Speleothem & Lake Core Records"],
                  "reconstitution_rationale": "Empirical precipitation grid with dynamic ITCZ, monsoonal, and glacial humidity corrections."
                },
                "seasonality": {
                  "filename": "earth_%d_seasonality.png",
                  "category": "climate",
                  "data_sources": ["WorldClim v2.1 Bio4 (Temperature Seasonality)", "Milankovitch Astronomical Solutions"],
                  "reconstitution_rationale": "Continuous seasonality amplitude accounting for axial tilt, obliquity, and ocean thermal inertia."
                },
                "density": {
                  "filename": "earth_%d_density.png",
                  "category": "demography",
                  "data_sources": ["HYDE 3.4 History Database of the Global Environment", "Seshat Global History Databank"],
                  "reconstitution_rationale": "Empirical demographic density field."
                },
                "sovereignty": {
                  "filename": "earth_%d_sovereignty.png",
                  "category": "sociology",
                  "data_sources": ["Seshat Databank Polities (ClioPatria 2023)", "Historical GIS Global Boundary Datasets"],
                  "reconstitution_rationale": "Multi-center polity sovereign domains and political borders."
                },
                "isogloss": {
                  "filename": "earth_%d_isogloss.png",
                  "category": "linguistics",
                  "data_sources": ["WALS World Atlas of Language Structures", "Glottolog 4.8", "D-PLACE Ethnolinguistic Database"],
                  "reconstitution_rationale": "Global ethnolinguistic phyla, sub-branches, and dialectal zones."
                },
                "kinship": {
                  "filename": "earth_%d_kinship.png",
                  "category": "anthropology",
                  "data_sources": ["D-PLACE Murdock Ethnographic Atlas Kinship Codes", "Todd Anthropological Family Systems Database"],
                  "reconstitution_rationale": "Spatial social structures, descent rules, and kinship organization modes."
                },
                "rituals": {
                  "filename": "earth_%d_rituals.png",
                  "category": "anthropology",
                  "data_sources": ["Archaeological Temple & Monumental Site Catalog", "World Religion Sacred Geography Datasets"],
                  "reconstitution_rationale": "Sacred geography, pilgrimage networks, and monumental ritual intensity."
                },
                "technology": {
                  "filename": "earth_%d_technology.png",
                  "category": "technology",
                  "data_sources": ["Archaeological Metallurgical & Innovation Datasets", "Maddison Project Historical GDP/Tech"],
                  "reconstitution_rationale": "Technological complexity index and diffusion fronts."
                },
                "institutional": {
                  "filename": "earth_%d_institutional.png",
                  "category": "sociology",
                  "data_sources": ["Seshat Complexity Characteristics", "Carneiro Organizational Scale"],
                  "reconstitution_rationale": "Administrative hierarchy levels, legal infrastructure, and state capacity."
                },
                "ecological": {
                  "filename": "earth_%d_ecological.png",
                  "category": "ecology",
                  "data_sources": ["Global Land Use Transitions (HYDE 3.4)", "Paleo-Deforestation Surveys"],
                  "reconstitution_rationale": "Anthropogenic ecological footprint, deforestation, and agro-pastoral soil transformation."
                },
                "pathogen": {
                  "filename": "earth_%d_pathogen.png",
                  "category": "epidemiology",
                  "data_sources": ["Historical Epidemiology & Paleopathology Catalogs", "Vector Ecology Baseline"],
                  "reconstitution_rationale": "Continuous epidemiological stress field, vector habitats, and zoonotic crowd disease risk."
                },
                "tradenetwork": {
                  "filename": "earth_%d_tradenetwork.png",
                  "category": "economy",
                  "data_sources": ["Ancient Trade Routes Geodatabase", "Historical Emporia & Caravan Nexus Catalog"],
                  "reconstitution_rationale": "Maritime trade arteries, overland caravan routes, and commercial centrality hubs."
                },
                "coal": {
                  "filename": "earth_%d_coal.png",
                  "category": "geology",
                  "data_sources": ["USGS World Coal Quality Inventory", "Global Coal Basin Assessment"],
                  "reconstitution_rationale": "Dual lithospheric reserve: total Carboniferous/Permian in-situ crustal endowment and surface outcrops."
                },
                "oil": {
                  "filename": "earth_%d_oil.png",
                  "category": "geology",
                  "data_sources": ["USGS World Petroleum Assessment", "Petroleum Sedimentary Basin Map"],
                  "reconstitution_rationale": "Crustal hydrocarbons reserve: total in-situ geological traps and surface bitumen seeps."
                },
                "gas": {
                  "filename": "earth_%d_gas.png",
                  "category": "geology",
                  "data_sources": ["USGS Conventional and Unconventional Natural Gas Resources"],
                  "reconstitution_rationale": "Crustal conventional and tight gas reservoirs across major sedimentary basins."
                },
                "uranium": {
                  "filename": "earth_%d_uranium.png",
                  "category": "geology",
                  "data_sources": ["IAEA NFCIS World Distribution of Uranium Deposits (UDEPO)"],
                  "reconstitution_rationale": "Total crustal radioactive mineral reserves (unconformity, sandstone, and calcrete deposits)."
                },
                "helium3": {
                  "filename": "earth_%d_helium3.png",
                  "category": "geology",
                  "data_sources": ["NASA Planetary Surface Composition", "Lunar Regolith He-3 Abundance Models"],
                  "reconstitution_rationale": "Terrestrial abundance is zero; strictly exclusive to solar-wind irradiated airless regoliths."
                },
                "iron_copper": {
                  "filename": "earth_%d_iron_copper.png",
                  "category": "geology",
                  "data_sources": ["USGS MRDS Mineral Resources Data System", "Precambrian BIF World Inventory"],
                  "reconstitution_rationale": "Total banded iron formations (BIFs) and porphyry copper belts plus ancient smelting centres."
                },
                "precious_metals": {
                  "filename": "earth_%d_precious_metals.png",
                  "category": "geology",
                  "data_sources": ["USGS MRDS Gold, Silver and PGM Deposits", "Ancient Metallurgical Inventories"],
                  "reconstitution_rationale": "Ancient historical mines (Las Médulas, Rio Tinto, Laurion, Roșia Montană, Nubia) and global giant provinces."
                },
                "rare_earths": {
                  "filename": "earth_%d_rare_earths.png",
                  "category": "geology",
                  "data_sources": ["USGS Critical Mineral Resources (REE, Lithium, Carbonatite Complexes)"],
                  "reconstitution_rationale": "Intrusive carbonatite complexes, pegmatite dykes, and lithium brine salars."
                },
                "geothermal": {
                  "filename": "earth_%d_geothermal.png",
                  "category": "geophysics",
                  "data_sources": ["Davies (2013) Global Mantle Heat Flow Database (IHFC)"],
                  "reconstitution_rationale": "Empirical terrestrial surface heat flow (mW/m²) across mid-ocean ridges, rifts, and cratons."
                },
                "aquifers": {
                  "filename": "earth_%d_aquifers.png",
                  "category": "geology",
                  "data_sources": ["UNESCO WHYMAP Global Groundwater Resources of the World 2022"],
                  "reconstitution_rationale": "Major regional sedimentary basin groundwater reservoirs modulated by elevation and lithological texture."
                }
              }
            }
            """.formatted(
                year, year, year, year,
                year, year, year, year, year,
                year, year, year, year, year,
                year, year, year, year, year,
                year, year, year, year, year,
                year, year, year, year, year
            );
            Files.writeString(provFile.toPath(), provJson);

            // 2. Write cultural_registry.json if not present
            if (!regFile.exists()) {
                String regJson = """
                {
                  "epoch": %d,
                  "planet": "earth",
                  "encoding": "ID_RGB_24BIT",
                  "traitDimensions": ["linguisticBranch", "socialStructure", "subsistenceMode", "ritualTradition"],
                  "entities": []
                }
                """.formatted(year);
                Files.writeString(regFile.toPath(), regJson);
            }

            // 3. Write README.md
            String readme = """
            # Cartographic & Cliodynamic Tensor Documentation — Epoch %d

            ## 🌍 Overview
            * **Planet**: Earth
            * **Epoch Year**: %d
            * **Resolution**: 2048 x 1024 (Equirectangular / Plate Carrée)
            * **Cartographic Standard**: 25 High-Definition Physical, Ecological, Cliodynamic, and Geological Resource Rasters.

            ---

            ## 🔬 Two-Tier Ontological Separation & Reconstitution
            In accordance with **Rule 6 of AGENTS.md**:
            1. **Static Initial Conditions ($t = t_0$)**: Initial state variables are ingested directly from peer-reviewed empirical datasets (NOAA ETOPO 2022, GEBCO 2023, WorldClim v2.1, UNESCO WHYMAP 2022, HYDE 3.4, Seshat ClioPatria, USGS MRDS, IAEA NFCIS, Davies 2013).
            2. **Dynamic Simulation Ticks ($t > t_0$)**: Simulation engines (Farquhar photosynthesis, Darcy groundwater recharge, Stull wet-bulb mortality, Turchin SDT demographic oscillations, technological diffusion) run dynamically from these base rasters.

            ---

            ## 🗺️ 25 Cartographic Layers & Academic Provenance

            ### A. Geophysical & Ecological Tensors (5 Rasters)
            1. `earth_%d_elevation.png`: Global Relief & Bathymetry (NOAA ETOPO 2022 / GEBCO 2023).
            2. `earth_%d_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Classification.
            3. `earth_%d_temperature.png`: Annual Mean Surface Temperature with Orographic Lapse Rate (WorldClim v2.1 Bio1).
            4. `earth_%d_precipitation.png`: Mean Annual Precipitation & Orographic Monsoons (WorldClim v2.1 Bio12).
            5. `earth_%d_seasonality.png`: Temperature Seasonality Amplitude (WorldClim v2.1 Bio4).

            ### B. Cliodynamic & Anthropological Tensors (10 Rasters)
            6. `earth_%d_density.png`: Empirical Demographic Density (HYDE 3.4 / Seshat).
            7. `earth_%d_sovereignty.png`: Sovereign Polities & Autonomous Domains (Seshat ClioPatria / Natural Earth).
            8. `earth_%d_isogloss.png`: Ethnolinguistic Phyla & Dialectal Trees (Glottolog 4.8 / WALS).
            9. `earth_%d_kinship.png`: Spatial Kinship & Family Structures (Murdock D-PLACE / Todd).
            10. `earth_%d_rituals.png`: Sacred Geography & Religious Traditions (World Sacred Heritage).
            11. `earth_%d_technology.png`: Technological Complexity Index (Maddison / Archaeo-metallurgy).
            12. `earth_%d_institutional.png`: Institutional Complexity & State Capacity (Carneiro / Seshat).
            13. `earth_%d_ecological.png`: Anthropogenic Ecological Footprint & Agro-pastoral Modification.
            14. `earth_%d_pathogen.png`: Epidemiological Stress Field & Vector Habitats ($R_0$).
            15. `earth_%d_tradenetwork.png`: Commercial Arteries, Silk Road & Maritime Emporia.

            ### C. Geological & Energy Resource Tensors (10 Rasters)
            16. `earth_%d_coal.png`: In-Situ Coal Basins & Surface Outcrops (USGS WCOAL).
            17. `earth_%d_oil.png`: Hydrocarbon Traps & Bitumen Seeps (USGS World Petroleum Assessment).
            18. `earth_%d_gas.png`: Natural Gas Sedimentary Reserves (USGS Natural Gas).
            19. `earth_%d_uranium.png`: Crustal Uranium Provinces (IAEA NFCIS / UDEPO).
            20. `earth_%d_helium3.png`: Terrestrial Helium-3 (Zero Abundance Baseline).
            21. `earth_%d_iron_copper.png`: Banded Iron Formations & Copper Belts (USGS MRDS).
            22. `earth_%d_precious_metals.png`: Historical Gold/Silver Mines & Giant Provinces (USGS MRDS).
            23. `earth_%d_rare_earths.png`: Critical Minerals, REE & Lithium Salars (USGS REE).
            24. `earth_%d_geothermal.png`: Terrestrial Mantle Heat Flow (Davies 2013 IHFC).
            25. `earth_%d_aquifers.png`: Deep Sedimentary Basin Groundwater Aquifers (UNESCO WHYMAP 2022).
            """.formatted(
                year, year,
                year, year, year, year, year,
                year, year, year, year, year, year, year, year, year, year,
                year, year, year, year, year, year, year, year, year, year
            );
            Files.writeString(readmeFile.toPath(), readme);

        } catch (Exception e) {
            logger.warn("Failed to write metadata files for year {}: {}", year, e.getMessage());
        }
    }
}
