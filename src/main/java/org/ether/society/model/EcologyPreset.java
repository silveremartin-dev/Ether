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
        String customSeasonalityBase64
) implements Serializable {

    /** Canonical compact constructor for normalization */
    public EcologyPreset {
        if (planetPresetName == null || planetPresetName.isBlank()) {
            planetPresetName = embeddedPlanetPreset != null ? embeddedPlanetPreset.name() : org.ether.society.procedural.PlanetPreset.EARTH_LIKE.name();
        }
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
                customRainfallBase64, customSeasonalityBase64);
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
                customRainfallBase64, customSeasonalityBase64);
    }

    /** Default Earth Standard Baseline preset (used as initial selection in the UI). */
    public static final EcologyPreset EARTH_STANDARD = new EcologyPreset(
            "Earth Standard Baseline", org.ether.society.procedural.PlanetPreset.EARTH_LIKE.name(), 450.0, 1500.0, 2.0, 6.0, 80.0, 1200.0, 87.0, 15000.0, 12345L, null, null, null, null, null, null);

    public static List<EcologyPreset> getBuiltInPresets() {
        List<EcologyPreset> list = new ArrayList<>();
        list.add(new EcologyPreset("Earth Standard Baseline", org.ether.society.procedural.PlanetPreset.EARTH_LIKE.name(), 450.0, 1500.0, 2.0, 6.0, 80.0, 1200.0, 87.0, 15000.0, 12345L, null, null, null, null, null, null));
        list.add(new EcologyPreset("Rich Mineral & Tectonic World", org.ether.society.procedural.PlanetPreset.SUPER_EARTH.name(), 200.0, 800.0, 1.0, 3.0, 350.0, 6500.0, 180.0, 8000.0, 12345L, null, null, null, null, null, null));
        list.add(new EcologyPreset("Primeval Carboniferous Jungle World", org.ether.society.procedural.PlanetPreset.SUPER_EARTH.name(), 1200.0, 3500.0, 8.0, 12.0, 60.0, 800.0, 95.0, 25000.0, 12345L, null, null, null, null, null, null));
        list.add(new EcologyPreset("Oceanic Aquifer Paradise", org.ether.society.procedural.PlanetPreset.WATER_WORLD.name(), 150.0, 500.0, 0.8, 25.0, 40.0, 900.0, 70.0, 45000.0, 12345L, null, null, null, null, null, null));
        list.add(new EcologyPreset("Arid Glacial Wasteland", org.ether.society.procedural.PlanetPreset.MARS_LIKE.name(), 25.0, 100.0, 0.1, 0.5, 120.0, 2200.0, 45.0, 1200.0, 12345L, null, null, null, null, null, null));
        return list;
    }

    @Override
    public String toString() {
        return name;
    }
}
