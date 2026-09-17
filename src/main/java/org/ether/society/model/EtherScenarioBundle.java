package org.ether.society.model;

import org.ether.society.procedural.PlanetPreset;
import org.ether.society.model.EcologyPreset;

/**
 * Unified bundle record encapsulating physical planet preset, ecology preset, and demographical scenario setup
 * with cryptographic provenance checksum and signature.
 */
public record EtherScenarioBundle(
        String version,
        PlanetPreset planetPreset,
        EcologyPreset ecologyPreset,
        Scenario scenario,
        String checksumSha256,
        String signature,
        String author,
        Long createdTimestamp
) {
    public EtherScenarioBundle(String version, PlanetPreset planetPreset, EcologyPreset ecologyPreset, Scenario scenario) {
        this(version, planetPreset, ecologyPreset, scenario, null, null, "Ether Creator", System.currentTimeMillis());
    }

    public EtherScenarioBundle {
        if (version == null) version = "1.0.0-beta.1";
    }
}
