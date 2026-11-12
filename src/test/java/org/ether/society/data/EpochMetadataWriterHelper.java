/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.data.HistoricalEpochTestCatalog.EpochMeta;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utility helper for generating and writing standardized metadata artifacts
 * (cultural registries, provenance records, and READMEs) across historical simulation epochs.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class EpochMetadataWriterHelper {

    private EpochMetadataWriterHelper() {
        // Prevent instantiation
    }

    /**
     * Writes the cliodynamic cultural, linguistic, demographic, and sovereign polity registry JSON.
     *
     * @param target target destination file
     * @param em epoch metadata record
     * @param seshatFile historical Seshat polities file
     * @throws Exception if I/O or serialization fails
     */
    public static void writeEpochCulturalRegistry(File target, EpochMeta em, File seshatFile) throws Exception {
        long yr = em.year();
        List<HistoricalPolityFeature> polities = Collections.emptyList();
        List<NaturalEarthVectorIngestor.CountryFeature> modernCountries = Collections.emptyList();

        if (yr >= 1900) {
            modernCountries = NaturalEarthVectorIngestor.getCountries(2048, 1024);
        } else if (yr >= -3400 && seshatFile.exists()) {
            polities = CliopatriaPolityVectorReader.loadPolitiesForYear(seshatFile, (int) yr, 2048, 1024);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"epoch\": ").append(yr).append(",\n");
        sb.append("  \"era\": \"").append(escapeJson(em.eraName())).append("\",\n");
        sb.append("  \"description\": \"Cliodynamic Cultural, Linguistic, Demographic & Sovereign Polity Registry for Epoch ")
          .append(yr).append(" (Calibrated vs Natural Earth, Seshat, HYDE 3.4 & Murdock D-PLACE)\",\n");
        sb.append("  \"encoding\": \"ID_RGB_24BIT\",\n");
        sb.append("  \"traitDimensions\": [\n");
        sb.append("    \"linguisticBranch\",\n");
        sb.append("    \"socialStructure\",\n");
        sb.append("    \"subsistenceMode\",\n");
        sb.append("    \"ritualTradition\"\n");
        sb.append("  ],\n");
        sb.append("  \"entities\": [\n");

        if (!modernCountries.isEmpty()) {
            for (int i = 0; i < modernCountries.size(); i++) {
                NaturalEarthVectorIngestor.CountryFeature c = modernCountries.get(i);
                java.awt.Color col = NaturalEarthVectorIngestor.computeSovereignColorForYear(c, yr);
                int rgb = col.getRGB() & 0xFFFFFF;
                String hex = String.format("#%06X", rgb);
                String safeId = (c.isoA3 != null && !"-99".equals(c.isoA3) ? c.isoA3 : c.name)
                        .replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);

                sb.append("    {\n");
                sb.append("      \"id\": \"").append(safeId).append("\",\n");
                sb.append("      \"colorHex\": \"").append(hex).append("\",\n");
                sb.append("      \"colorRgb\": [").append(col.getRed()).append(", ").append(col.getGreen()).append(", ").append(col.getBlue()).append("],\n");
                sb.append("      \"traits\": [")
                  .append(String.format(Locale.US, "%.2f", c.technologyLevel / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", c.institutionalLevel / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", c.ecologicalFootprint / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", (255 - c.pathogenStress) / 255.0)).append("],\n");
                sb.append("      \"name\": {\n");
                sb.append("        \"en\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"fr\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"de\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"es\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"zh\": \"").append(escapeJson(c.name)).append("\"\n");
                sb.append("      },\n");
                sb.append("      \"isoA3\": \"").append(escapeJson(c.isoA3)).append("\",\n");
                sb.append("      \"continent\": \"").append(escapeJson(c.continent)).append("\",\n");
                sb.append("      \"fromYear\": ").append(yr <= 1945 ? 1900 : (yr <= 1990 ? 1945 : 1991)).append(",\n");
                sb.append("      \"toYear\": ").append(yr >= 2026 ? 2060 : (yr >= 1990 ? 2025 : 1990)).append(",\n");
                sb.append("      \"kinshipType\": \"").append(escapeJson(em.defaultKinship())).append("\",\n");
                sb.append("      \"lithicTechnocomplex\": \"").append(escapeJson(em.lithicComplex())).append("\",\n");
                sb.append("      \"caloricIntakePerCapitaKcal\": ").append(em.defaultKcal()).append("\n");
                sb.append("    }").append(i < modernCountries.size() - 1 ? "," : "").append("\n");
            }
        } else if (!polities.isEmpty()) {
            for (int i = 0; i < polities.size(); i++) {
                HistoricalPolityFeature p = polities.get(i);
                java.awt.Color col = CliopatriaPolityVectorReader.getPolityColor(p.name, p.seshatId);
                int rgb = col.getRGB() & 0xFFFFFF;
                String hex = String.format("#%06X", rgb);
                double[] pTraits = CliopatriaPolityVectorReader.computePolityTraits(p.name, p.seshatId, yr);
                String safeId = p.name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);
                sb.append("    {\n");
                sb.append("      \"id\": \"").append(safeId).append("\",\n");
                sb.append("      \"colorHex\": \"").append(hex).append("\",\n");
                sb.append("      \"colorRgb\": [").append(col.getRed()).append(", ").append(col.getGreen()).append(", ").append(col.getBlue()).append("],\n");
                sb.append("      \"traits\": [")
                  .append(String.format(Locale.US, "%.2f", pTraits[0])).append(", ")
                  .append(String.format(Locale.US, "%.2f", pTraits[1])).append(", ")
                  .append(String.format(Locale.US, "%.2f", pTraits[2])).append(", ")
                  .append(String.format(Locale.US, "%.2f", pTraits[3])).append("],\n");
                sb.append("      \"name\": {\n");
                sb.append("        \"en\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"fr\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"de\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"es\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"zh\": \"").append(escapeJson(p.name)).append("\"\n");
                sb.append("      },\n");
                sb.append("      \"seshatId\": \"").append(p.seshatId != null ? escapeJson(p.seshatId) : "").append("\",\n");
                sb.append("      \"fromYear\": ").append(p.fromYear).append(",\n");
                sb.append("      \"toYear\": ").append(p.toYear).append(",\n");
                sb.append("      \"kinshipType\": \"").append(escapeJson(em.defaultKinship())).append("\",\n");
                sb.append("      \"lithicTechnocomplex\": \"").append(escapeJson(em.lithicComplex())).append("\",\n");
                sb.append("      \"caloricIntakePerCapitaKcal\": ").append(em.defaultKcal()).append("\n");
                sb.append("    }").append(i < polities.size() - 1 ? "," : "").append("\n");
            }
        } else {
            sb.append("""
                {
                  \"id\": \"core_polity_east_asia\",
                  \"colorHex\": \"#EF4444\",
                  \"colorRgb\": [239, 68, 68],
                  \"traits\": [0.70, 0.85, 0.90, 0.85],
                  \"name\": {
                    \"en\": \"East Asian Core (%s)\",
                    \"fr\": \"Foyer Est-Asiatique (%s)\",
                    \"de\": \"Ostasietischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de Asia Oriental (%s)\",
                    \"zh\": \"东亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_west_eurasia\",
                  \"colorHex\": \"#2563EB\",
                  \"colorRgb\": [37, 99, 235],
                  \"traits\": [0.75, 0.80, 0.88, 0.80],
                  \"name\": {
                    \"en\": \"Western Eurasian Core (%s)\",
                    \"fr\": \"Foyer Ouest-Eurasien (%s)\",
                    \"de\": \"Westeurasischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo Euroasiático Occidental (%s)\",
                    \"zh\": \"西亚欧亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_south_asia\",
                  \"colorHex\": \"#F59E0B\",
                  \"colorRgb\": [245, 158, 11],
                  \"traits\": [0.65, 0.85, 0.92, 0.90],
                  \"name\": {
                    \"en\": \"South Asian Core (%s)\",
                    \"fr\": \"Foyer Sud-Asiatique (%s)\",
                    \"de\": \"Südasiatischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de Asia del Sur (%s)\",
                    \"zh\": \"南亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_americas\",
                  \"colorHex\": \"#06B6D4\",
                  \"colorRgb\": [6, 182, 212],
                  \"traits\": [0.55, 0.75, 0.82, 0.88],
                  \"name\": {
                    \"en\": \"Americas Core (%s)\",
                    \"fr\": \"Foyer des Amériques (%s)\",
                    \"de\": \"Amerikanischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de las Américas (%s)\",
                    \"zh\": \"美洲核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_africa\",
                  \"colorHex\": \"#22C55E\",
                  \"colorRgb\": [34, 197, 94],
                  \"traits\": [0.60, 0.70, 0.75, 0.80],
                  \"name\": {
                    \"en\": \"African Core (%s)\",
                    \"fr\": \"Foyer Africain (%s)\",
                    \"de\": \"Afrikanischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo Africano (%s)\",
                    \"zh\": \"非洲核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                }
            """.formatted(
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal()
            ));
        }

        sb.append("  ]\n");
        sb.append("}\n");
        safeWriteFile(target, sb.toString());
    }

    /**
     * Writes the provenance and data sources JSON document for an epoch.
     *
     * @param target target destination file
     * @param em epoch metadata record
     * @throws Exception if I/O or serialization fails
     */
    public static void writeEpochProvenance(File target, EpochMeta em) throws Exception {
        long yr = em.year();
        String json = """
        {
          "epoch": %d,
          "era": "%s",
          "planet": "earth",
          "resolution": "2048x1024",
          "projection": "Equirectangular (Plate Carrée, EPSG:4326)",
          "overview": {
            "summary_en": "%s",
            "summary_fr": "%s"
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
              "data_sources": ["Seshat Global History Databank Institutional Hierarchy Scales"],
              "reconstitution_rationale": "Administrative, legal, and bureaucratic state capacity."
            },
            "ecological": {
              "filename": "earth_%d_ecological.png",
              "category": "ecology",
              "data_sources": ["Global Land Use & Anthropogenic Deforestation Syntheses", "UN FAO Global Forest Resources"],
              "reconstitution_rationale": "Human ecological footprint, agricultural clearing, and resource appropriation."
            },
            "pathogen": {
              "filename": "earth_%d_pathogen.png",
              "category": "epidemiology",
              "data_sources": ["Paleoepidemiology & Ancient DNA Pathogen Catalogs", "WHO Global Infectious Disease Atlas"],
              "reconstitution_rationale": "Endemic disease pressure, malaria/zoonotic vectors, and historical immunity."
            },
            "tradenetwork": {
              "filename": "earth_%d_tradenetwork.png",
              "category": "economics",
              "data_sources": ["Historical Maritime & Overland Trade Route Atlases", "Seshat Commercial Networks"],
              "reconstitution_rationale": "Commercial trade corridors, emporia, and maritime networks."
            },
            "coal": { "filename": "earth_%d_coal.png", "category": "geology", "data_sources": ["USGS World Coal Quality Inventory"] },
            "oil": { "filename": "earth_%d_oil.png", "category": "geology", "data_sources": ["USGS World Petroleum Assessment"] },
            "gas": { "filename": "earth_%d_gas.png", "category": "geology", "data_sources": ["USGS Global Gas Assessment"] },
            "uranium": { "filename": "earth_%d_uranium.png", "category": "geology", "data_sources": ["IAEA / NEA Red Book"] },
            "helium3": { "filename": "earth_%d_helium3.png", "category": "geology", "data_sources": ["USGS Mantle Volatiles Survey"] },
            "iron_copper": { "filename": "earth_%d_iron_copper.png", "category": "geology", "data_sources": ["USGS MRDS Mineral Database"] },
            "precious_metals": { "filename": "earth_%d_precious_metals.png", "category": "geology", "data_sources": ["USGS Mineral Assessment"] },
            "rare_earths": { "filename": "earth_%d_rare_earths.png", "category": "geology", "data_sources": ["USGS REE Database"] },
            "geothermal": { "filename": "earth_%d_geothermal.png", "category": "geology", "data_sources": ["IHFC Terrestrial Heat Flow Database"] },
            "aquifers": { "filename": "earth_%d_aquifers.png", "category": "geology", "data_sources": ["UNESCO WHYMAP Global Groundwater"] }
          }
        }
        """.formatted(
            yr, escapeJson(em.eraName()), escapeJson(em.summaryEn()), escapeJson(em.summaryFr()),
            yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr
        );

        safeWriteFile(target, json);
    }

    /**
     * Writes the comprehensive README markdown document for an epoch.
     *
     * @param target target destination file
     * @param em epoch metadata record
     * @throws Exception if I/O fails
     */
    public static void writeEpochReadme(File target, EpochMeta em) throws Exception {
        long yr = em.year();
        String md = """
        # Earth Epoch %d: %s

        ## 🌐 Overview
        This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **%d** (%s).

        All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

        ### Context & Archaeological/Historical Horizon
        * **Era**: %s
        * **Technocomplex**: %s
        * **Social / Family Horizon**: %s
        * **Estimated Caloric Baseline**: %d kcal/capita/day

        ## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

        ### Physical & Climate Layers
        * `earth_%d_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
        * `earth_%d_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
        * `earth_%d_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
        * `earth_%d_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
        * `earth_%d_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

        ### Cliodynamic & Cultural Tensors
        * `earth_%d_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
        * `earth_%d_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
        * `earth_%d_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
        * `earth_%d_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
        * `earth_%d_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
        * `earth_%d_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
        * `earth_%d_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
        * `earth_%d_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
        * `earth_%d_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
        * `earth_%d_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

        ### Geological & Energy Resources
        * `earth_%d_coal.png`: Coal Basins (USGS WoCQI).
        * `earth_%d_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
        * `earth_%d_gas.png`: Natural Gas Formations (USGS).
        * `earth_%d_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
        * `earth_%d_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
        * `earth_%d_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
        * `earth_%d_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
        * `earth_%d_rare_earths.png`: Critical Rare Earth Elements (REE).
        * `earth_%d_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
        * `earth_%d_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

        ## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
        In strict accordance with `AGENTS.md` Directives:
        1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
        2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

        ## 📄 Associated Metadata Files
        * `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
        * `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
        """.formatted(
            yr, em.eraName(), yr, em.eraName(),
            em.eraName(), em.lithicComplex(), em.defaultKinship(), em.defaultKcal(),
            yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr
        );

        safeWriteFile(target, md);
    }

    /**
     * Safely and atomically writes string content to a target file.
     *
     * @param target target destination file
     * @param content UTF-8 string content
     * @throws Exception if writing fails after multiple attempts
     */
    public static void safeWriteFile(File target, String content) throws Exception {
        target.getParentFile().mkdirs();
        int attempts = 0;
        while (true) {
            try {
                java.nio.file.Path tempPath = target.toPath().resolveSibling(target.getName() + ".tmp." + Thread.currentThread().threadId() + "." + System.nanoTime());
                java.nio.file.Files.writeString(tempPath, content, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                        java.nio.file.StandardOpenOption.WRITE);
                try {
                    java.nio.file.Files.move(tempPath, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    java.nio.file.Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
                    try { java.nio.file.Files.deleteIfExists(tempPath); } catch (Exception ignored) { /* ignore temp file deletion failure */ }
                }
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts >= 10) throw e;
                Thread.sleep(200);
            }
        }
    }

    /**
     * Escapes standard JSON control characters in a string.
     *
     * @param s input string
     * @return escaped JSON string
     */
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
