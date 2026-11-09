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
 * @version 1.0.0-beta.2
 */
public class AutoEpochScenarioGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AutoEpochScenarioGenerator.class);

    public record EpochMilestone(long year, String nameKey, String defaultName, String descriptionKey, String defaultDesc) {}

    public static final List<EpochMilestone> EARTH_KEY_MILESTONES = AutoEpochScenarioHelper.EARTH_KEY_MILESTONES;



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
        if (s.getTargetCohortSize() <= 0) {
            s.setTargetCohortSize(AutoEpochScenarioHelper.calibrateDefaultTargetCohortSize(pKey, targetYear, popN0));
        }

        if (isEarth) {
            s.setPopulationDensityType(s.getCustomDensityBase64() != null ? "IMPORT_CUSTOM" : "UNBIASED_NATURAL");
        } else {
            s.setPopulationDensityType("PROCEDURAL");
        }
        // Set all tensors to procedural by default
        List<Boolean> procModes = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            procModes.add(true);
        }
        s.setTensorProceduralModes(procModes);

        // 6. Type B Optional Engine State Calibration
        Map<String, Boolean> typeBStates = calibrateTypeBEngines(pKey, targetYear);
        s.setTypeBEngineStates(typeBStates);

        // 6b. Cultural Affinity Matrix Calibration
        Map<String, Double> affinityMatrix = AutoEpochScenarioHelper.initializeCulturalAffinityMatrix(pKey, targetYear);
        s.setCulturalAffinityMatrix(affinityMatrix);

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

    /**
     * Compute recommended end year.
     *
     * @param startYear the start year parameter (long)
     * @return the resulting computation or state reference
     */
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

    /**
     * Estimate population.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
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

    /**
     * Estimate capital per capita.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
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

    /**
     * Estimate energy per capita.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double estimateEnergyPerCapita(String planetKey, long year) {
        if (!"earth".equalsIgnoreCase(planetKey)) return 200.0;
        if (year <= -10000L) return 5.0;    // Fire & wood
        if (year <= 0L) return 20.0;        // Animal traction & charcoal
        if (year <= 1800L) return 40.0;     // Water/wind mills & wood
        if (year <= 1900L) return 120.0;    // Coal steam power
        if (year <= 1950L) return 250.0;    // Oil, electricity
        return 450.0;                       // Modern primary energy consumption
    }

    /**
     * Estimate food reserves.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double estimateFoodReserves(String planetKey, long year) {
        if (year <= -10000L) return 2.0;    // Hunter-gatherer seasonal stocks
        if (year <= 0L) return 4.0;         // Granaries & silos
        if (year <= 1800L) return 6.0;      // Agricultural reserves
        return 8.0;                         // Modern global food supply chain
    }

    /**
     * Estimate information per capita.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double estimateInformationPerCapita(String planetKey, long year) {
        if (year <= -10000L) return 2.0;    // Oral traditions
        if (year <= -3000L) return 10.0;    // Early pictograms/cuneiform
        if (year <= 1450L) return 50.0;     // Parchment & manuscript libraries
        if (year <= 1800L) return 300.0;    // Printing press & encyclopedias
        if (year <= 1950L) return 2000.0;   // Industrial telegraphy, radio & books
        return 50000.0;                     // Digital internet & computing
    }

    /**
     * Calibrate type bengines.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static Map<String, Boolean> calibrateTypeBEngines(String planetKey, long year) {
        return AutoEpochScenarioHelper.calibrateTypeBEngines(planetKey, year);
    }


    /**
     * Resolve planet preset for epoch.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
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
        // Continuous PCHIP CO2 concentration from historical ice cores / HYDE / SSP projections
        double co2 = org.ether.society.analytics.HistoricalValidationKernel.getInterpolatedBenchmarkValue("co2Concentration", (double) year);
        if (Double.isNaN(co2) || co2 < 180.0) {
            co2 = (year < 1800) ? 280.0 : ((year < 1914) ? 295.0 : 420.0);
        }
        String epochLabel = formatEpochName(year);
        return PlanetPreset.EARTH_MODERN
                .withName("Terre (" + epochLabel + ")")
                .withCo2Ppm(co2);
    }

    /**
     * Resolve ecology preset for epoch.
     *
     * @param planetKey the planet key parameter (String)
     * @param year the year parameter (long)
     * @param planetPreset the planet preset parameter (PlanetPreset)
     * @return the resulting computation or state reference
     */
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
        AutoEpochScenarioHelper.copyTemplateProperties(source, target);
    }


    /**
     * Format epoch name.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static String formatEpochName(long year) {
        if (year < 0) {
            return Math.abs(year) + " BP (" + Math.abs(year) + " av. J.-C.)";
        } else if (year == 0) {
            return "0 AD (An 0 / Antiquité)";
        } else {
            return year + " AD (" + year + " ap. J.-C.)";
        }
    }

    private static String generateEpochDescription(String planet, long year, String epochLabel) {
        return String.format(Locale.ROOT, """
            ðŸŒ SCÉNARIO AUTOMATIQUE D'ÉPOQUE : %s — %s
            
            [PARAMÉTRAGE SCIENTIFIQUE & GÉOPHYSIQUE (TIER 1)]
            • Planète cible : %s
            • Année de départ : %s (Horizon temporel recommandé : %d ans)
            • Population mondiale estimée N₀ : %,d habitants
            • Capital physique K₀ : %.1f kg/habitant
            • Énergie stockée E₀ : %.1f MJ/habitant
            
            [SYNCHRONISATION INTER-ONGLETS]
            • Onglet 1 (Planète) : Modèle numérique d'élévation, biomes et paléoclimat de l'époque %s.
            • Onglet 2 (Ressources) : Nappes phréatiques, minerais et réserves vierges pré-industrielles.
            • Onglet 3 (Société & Moteurs) : Filtrage des modules de Type B adaptés à la période.
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

