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

    /** 11 Built-in presets corresponding to all 11 planet types */
    public static final EcologyPreset EARTH_LIKE = new EcologyPreset(
            PlanetPreset.EARTH_LIKE.name(), PlanetPreset.EARTH_LIKE.name(), 450.0, 1500.0, 2.0, 6.0, 80.0, 1200.0, 87.0, 15000.0, 12345L, null, null, null, null, null, null);

    public static final EcologyPreset EARTH_STANDARD = EARTH_LIKE;

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
                EARTH_LIKE,
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

    @Override
    public String toString() {
        return name;
    }
}
