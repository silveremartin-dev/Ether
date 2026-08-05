package org.ether.society.model;

import org.ether.society.procedural.PlanetPreset;
import org.ether.society.model.EcologyPreset;

/**
 * Unified bundle record encapsulating physical planet preset, ecology preset, and demographical scenario setup.
 */
public record EtherScenarioBundle(
        String version,
        PlanetPreset planetPreset,
        EcologyPreset ecologyPreset,
        Scenario scenario
) {
    public EtherScenarioBundle {
        if (version == null) version = "2.0.0";
    }
}
