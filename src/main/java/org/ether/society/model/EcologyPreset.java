/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Preset model for ecological and resource distributions.
 */
public record EcologyPreset(
        String name,
        double woodDensityMultiplier,
        double cropYieldMultiplier,
        double gameFaunaMultiplier,
        double livestockCapacityMultiplier,
        double metalOresMultiplier,
        double preciousOresMultiplier,
        double stoneQualityMultiplier,
        double fishAbundanceMultiplier
) implements Serializable {

    public static List<EcologyPreset> getBuiltInPresets() {
        List<EcologyPreset> list = new ArrayList<>();
        list.add(new EcologyPreset("Balanced Earth Standard", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0));
        list.add(new EcologyPreset("Rich Minerals & Mining World", 0.8, 0.7, 0.6, 0.5, 3.0, 2.5, 2.0, 0.8));
        list.add(new EcologyPreset("Lush Primeval Forest", 3.0, 1.5, 2.5, 1.2, 0.5, 0.5, 0.8, 1.2));
        list.add(new EcologyPreset("Oceanic & Marine Paradise", 0.5, 0.8, 0.5, 0.5, 0.7, 0.8, 0.8, 3.5));
        list.add(new EcologyPreset("Arid Resource-Scarce Wasteland", 0.2, 0.3, 0.2, 0.2, 0.4, 0.3, 0.5, 0.3));
        return list;
    }

    @Override
    public String toString() {
        return name;
    }
}
