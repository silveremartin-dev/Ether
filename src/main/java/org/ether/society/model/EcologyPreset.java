/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ether.society.procedural.PlanetPreset;

/**
 * Preset model for ecological and resource distributions.
 */
public record EcologyPreset(
        String name,
        String planetPresetName,          // Associated Tab 1 planet preset name (cascading dependency)
        PlanetPreset embeddedPlanetPreset, // Embedded full planet preset snapshot for standalone restoration
        double terrestrialBiomassGtC,    // Global plant & forest biomass (Gigatons of Carbon, GtC)
        double soilOrganicCarbonGtC,     // Global soil organic carbon & agricultural stock (GtC)
        double faunaBiomassGtC,          // Global terrestrial animal & game fauna biomass (GtC)
        double aquaticBiomassGtC,        // Global marine & freshwater biomass (GtC)
        double crustalMetalOresGt,       // Industrial base metal reserves in crust (Gigatons, Gt)
        double preciousMetalOresMt,      // Precious & rare earth ores (Megatons, Mt)
        double mantleHeatFlowMwM2,       // Mantle heat flow & tectonic/geothermal index (mW/m²)
        double freshwaterReserveKm3,     // Groundwater & aquifer reserves (in 10^3 km³)
        long seed,
        String customBiomeBase64,
        String customResourceBase64,
        String customHydroBase64,
        String customClimateBase64,
        String customRainfallBase64,
        String customSeasonalityBase64,
        List<String> customGeologyTensorMapsBase64
) implements Serializable {

    /** Canonical compact constructor for normalization */
    public EcologyPreset {
        if (planetPresetName == null || planetPresetName.isBlank()) {
            planetPresetName = embeddedPlanetPreset != null ? embeddedPlanetPreset.name() : org.ether.society.procedural.PlanetPreset.EARTH_LIKE.name();
        }
    }

    /** Constructor overload without embeddedPlanetPreset & customGeologyTensorMapsBase64 */
    public EcologyPreset(
            String name,
            String planetPresetName,
            PlanetPreset embeddedPlanetPreset,
            double terrestrialBiomassGtC,
            double soilOrganicCarbonGtC,
            double faunaBiomassGtC,
            double aquaticBiomassGtC,
            double crustalMetalOresGt,
            double preciousMetalOresMt,
            double mantleHeatFlowMwM2,
            double freshwaterReserveKm3,
            long seed,
            String customBiomeBase64,
            String customResourceBase64,
            String customHydroBase64,
            String customClimateBase64,
            String customRainfallBase64,
            String customSeasonalityBase64
    ) {
        this(name, planetPresetName, embeddedPlanetPreset, terrestrialBiomassGtC, soilOrganicCarbonGtC, faunaBiomassGtC,
                aquaticBiomassGtC, crustalMetalOresGt, preciousMetalOresMt, mantleHeatFlowMwM2, freshwaterReserveKm3,
                seed, customBiomeBase64, customResourceBase64, customHydroBase64, customClimateBase64,
                customRainfallBase64, customSeasonalityBase64, null);
    }

    /** Constructor overload without embeddedPlanetPreset for backward compatibility with JSON / older presets */
    public EcologyPreset(
            String name,
            String planetPresetName,
            double terrestrialBiomassGtC,
            double soilOrganicCarbonGtC,
            double faunaBiomassGtC,
            double aquaticBiomassGtC,
            double crustalMetalOresGt,
            double preciousMetalOresMt,
            double mantleHeatFlowMwM2,
            double freshwaterReserveKm3,
            long seed,
            String customBiomeBase64,
            String customResourceBase64,
            String customHydroBase64,
            String customClimateBase64,
            String customRainfallBase64,
            String customSeasonalityBase64
    ) {
        this(name, planetPresetName, null, terrestrialBiomassGtC, soilOrganicCarbonGtC, faunaBiomassGtC,
                aquaticBiomassGtC, crustalMetalOresGt, preciousMetalOresMt, mantleHeatFlowMwM2, freshwaterReserveKm3,
                seed, customBiomeBase64, customResourceBase64, customHydroBase64, customClimateBase64,
                customRainfallBase64, customSeasonalityBase64, null);
    }

    /** Overloaded constructor without planetPresetName for backward compatibility */
    public EcologyPreset(
            String name,
            double terrestrialBiomassGtC,
            double soilOrganicCarbonGtC,
            double faunaBiomassGtC,
            double aquaticBiomassGtC,
            double crustalMetalOresGt,
            double preciousMetalOresMt,
            double mantleHeatFlowMwM2,
            double freshwaterReserveKm3,
            long seed,
            String customBiomeBase64,
            String customResourceBase64,
            String customHydroBase64,
            String customClimateBase64,
            String customRainfallBase64,
            String customSeasonalityBase64
    ) {
        this(name, org.ether.society.procedural.PlanetPreset.EARTH_LIKE.name(), null, terrestrialBiomassGtC, soilOrganicCarbonGtC, faunaBiomassGtC,
                aquaticBiomassGtC, crustalMetalOresGt, preciousMetalOresMt, mantleHeatFlowMwM2, freshwaterReserveKm3,
                seed, customBiomeBase64, customResourceBase64, customHydroBase64, customClimateBase64,
                customRainfallBase64, customSeasonalityBase64, null);
    }

    /** Modern Earth preset (2026 baseline) */
    public static final EcologyPreset EARTH_MODERN = new EcologyPreset(
            PlanetPreset.EARTH_MODERN.name(), PlanetPreset.EARTH_MODERN.name(), 450.0, 1500.0, 2.0, 6.0, 80.0, 1200.0, 87.0, 15000.0, 12345L, null, null, null, null, null, null);

    public static final EcologyPreset EARTH_LIKE = EARTH_MODERN;
    public static final EcologyPreset EARTH_STANDARD = EARTH_MODERN;

    /** -1 000 ans : Début Âge du Fer */
    public static final EcologyPreset EARTH_IRON_1000BP = new EcologyPreset(
            PlanetPreset.EARTH_IRON_1000BP.name(), PlanetPreset.EARTH_IRON_1000BP.name(), 580.0, 1780.0, 7.0, 17.0, 135.0, 2100.0, 87.0, 21000.0, 12344L, null, null, null, null, null, null);

    /** -1 900 ans : Âge du Bronze Moyen */
    public static final EcologyPreset EARTH_BRONZE_1900BP = new EcologyPreset(
            PlanetPreset.EARTH_BRONZE_1900BP.name(), PlanetPreset.EARTH_BRONZE_1900BP.name(), 590.0, 1800.0, 9.0, 19.0, 145.0, 2250.0, 87.0, 23000.0, 12345L, null, null, null, null, null, null);

    /** -3 000 ans (LH) : Holocène tardif */
    public static final EcologyPreset EARTH_LH_3000BP = new EcologyPreset(
            PlanetPreset.EARTH_LH_3000BP.name(), PlanetPreset.EARTH_LH_3000BP.name(), 560.0, 1750.0, 8.0, 18.0, 140.0, 2200.0, 87.0, 22000.0, 12346L, null, null, null, null, null, null);

    /** -6 000 ans (MH) : Holocène moyen avec le Sahara Vert et le lac Méga-Tchad */
    public static final EcologyPreset EARTH_MH_6000BP = new EcologyPreset(
            PlanetPreset.EARTH_MH_6000BP.name(), PlanetPreset.EARTH_MH_6000BP.name(), 680.0, 1900.0, 14.0, 22.0, 150.0, 2400.0, 87.0, 28000.0, 12347L, null, null, null, null, null, null);

    /** -10 000 ans (EH) : Holocène précoce */
    public static final EcologyPreset EARTH_EH_10000BP = new EcologyPreset(
            PlanetPreset.EARTH_EH_10000BP.name(), PlanetPreset.EARTH_EH_10000BP.name(), 520.0, 1600.0, 18.0, 20.0, 150.0, 2400.0, 87.0, 25000.0, 12348L, null, null, null, null, null, null);

    /** -20 000 ans (LGM) : Dernier Maximum Glaciaire */
    public static final EcologyPreset EARTH_LGM_20000BP = new EcologyPreset(
            PlanetPreset.EARTH_LGM_20000BP.name(), PlanetPreset.EARTH_LGM_20000BP.name(), 320.0, 1300.0, 25.0, 14.0, 150.0, 2400.0, 87.0, 20000.0, 12349L, null, null, null, null, null, null);

    /** -25 000 ans : Début LGM & Béringie */
    public static final EcologyPreset EARTH_LGM_ONSET_25000BP = new EcologyPreset(
            PlanetPreset.EARTH_LGM_ONSET_25000BP.name(), PlanetPreset.EARTH_LGM_ONSET_25000BP.name(), 360.0, 1400.0, 24.0, 16.0, 150.0, 2400.0, 87.0, 21000.0, 12351L, null, null, null, null, null, null);

    /** -50 000 ans : Stade Isotopique 3 & Sahul */
    public static final EcologyPreset EARTH_MIS3_50000BP = new EcologyPreset(
            PlanetPreset.EARTH_MIS3_50000BP.name(), PlanetPreset.EARTH_MIS3_50000BP.name(), 480.0, 1600.0, 23.0, 20.0, 150.0, 2400.0, 87.0, 24000.0, 12352L, null, null, null, null, null, null);

    /** -100 000 ans (LIG) : Dernier Interglaciaire / Eémien */
    public static final EcologyPreset EARTH_LIG_100000BP = new EcologyPreset(
            PlanetPreset.EARTH_LIG_100000BP.name(), PlanetPreset.EARTH_LIG_100000BP.name(), 620.0, 1800.0, 22.0, 24.0, 150.0, 2400.0, 87.0, 26000.0, 12350L, null, null, null, null, null, null);

    public static final EcologyPreset MARS_LIKE = new EcologyPreset(
            PlanetPreset.MARS_LIKE.name(), PlanetPreset.MARS_LIKE.name(), 0.0, 0.0, 0.0, 0.0, 140.0, 1800.0, 30.0, 500.0, 98765L, null, null, null, null, null, null);

    public static final EcologyPreset VENUS_LIKE = new EcologyPreset(
            PlanetPreset.VENUS_LIKE.name(), PlanetPreset.VENUS_LIKE.name(), 0.0, 0.0, 0.0, 0.0, 95.0, 1400.0, 120.0, 0.0, 55555L, null, null, null, null, null, null);

    public static final EcologyPreset MOON_LIKE = new EcologyPreset(
            PlanetPreset.MOON_LIKE.name(), PlanetPreset.MOON_LIKE.name(), 0.0, 0.0, 0.0, 0.0, 45.0, 600.0, 18.0, 10.0, 88888L, null, null, null, null, null, null);

    public static final EcologyPreset MERCURY_LIKE = new EcologyPreset(
            PlanetPreset.MERCURY_LIKE.name(), PlanetPreset.MERCURY_LIKE.name(), 0.0, 0.0, 0.0, 0.0, 280.0, 3500.0, 40.0, 2.0, 66666L, null, null, null, null, null, null);

    public static final EcologyPreset TITAN_LIKE = new EcologyPreset(
            PlanetPreset.TITAN_LIKE.name(), PlanetPreset.TITAN_LIKE.name(), 0.0, 200.0, 0.0, 0.0, 30.0, 400.0, 25.0, 8000.0, 77711L, null, null, null, null, null, null);

    public static final EcologyPreset SUPER_EARTH = new EcologyPreset(
            PlanetPreset.SUPER_EARTH.name(), PlanetPreset.SUPER_EARTH.name(), 950.0, 2800.0, 5.0, 15.0, 220.0, 3200.0, 140.0, 35000.0, 44444L, null, null, null, null, null, null);

    public static final EcologyPreset EYEBALL_WORLD = new EcologyPreset(
            PlanetPreset.EYEBALL_WORLD.name(), PlanetPreset.EYEBALL_WORLD.name(), 180.0, 600.0, 0.8, 4.0, 110.0, 1600.0, 90.0, 12000.0, 33333L, null, null, null, null, null, null);

    public static final EcologyPreset WATER_WORLD = new EcologyPreset(
            PlanetPreset.WATER_WORLD.name(), PlanetPreset.WATER_WORLD.name(), 80.0, 300.0, 0.4, 30.0, 50.0, 900.0, 75.0, 60000.0, 54321L, null, null, null, null, null, null);

    public static final EcologyPreset ICE_WORLD = new EcologyPreset(
            PlanetPreset.ICE_WORLD.name(), PlanetPreset.ICE_WORLD.name(), 30.0, 150.0, 0.2, 1.5, 130.0, 2100.0, 60.0, 4000.0, 11111L, null, null, null, null, null, null);

    public static final EcologyPreset ARCHIPELAGO = new EcologyPreset(
            PlanetPreset.ARCHIPELAGO.name(), PlanetPreset.ARCHIPELAGO.name(), 550.0, 1800.0, 3.0, 18.0, 90.0, 1400.0, 110.0, 20000.0, 77777L, null, null, null, null, null, null);

    public static List<EcologyPreset> getBuiltInPresets() {
        return List.of(
                EARTH_MODERN,
                EARTH_IRON_1000BP,
                EARTH_BRONZE_1900BP,
                EARTH_LH_3000BP,
                EARTH_MH_6000BP,
                EARTH_EH_10000BP,
                EARTH_LGM_20000BP,
                EARTH_LGM_ONSET_25000BP,
                EARTH_MIS3_50000BP,
                EARTH_LIG_100000BP,
                MARS_LIKE,
                VENUS_LIKE,
                MOON_LIKE,
                MERCURY_LIKE,
                TITAN_LIKE,
                SUPER_EARTH,
                EYEBALL_WORLD,
                WATER_WORLD,
                ICE_WORLD,
                ARCHIPELAGO
        );
    }

    public long getAssociatedEpochYear() {
        String lower = (name != null ? name : "").toLowerCase();
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return -100000L;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return -50000L;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return -25000L;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire")) return -20000L;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene")) return -10000L;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return -6000L;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return -3000L;
        if (lower.contains("-1900") || lower.contains("bronze")) return -1900L;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return -1000L;
        return 2026L;
    }

    public String getCanonicalPlanet() {
        String lower = (planetPresetName != null ? planetPresetName : name != null ? name : "").toLowerCase();
        if (lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia")) return "none";
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) return "earth";
        if (lower.contains("mars") || lower.contains("ares")) return "mars";
        if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) return "venus";
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) return "moon";
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) return "mercury";
        return "none";
    }

    @Override
    public String toString() {
        return name;
    }
}
