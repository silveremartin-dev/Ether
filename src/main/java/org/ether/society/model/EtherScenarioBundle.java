package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
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
    /*
     * Ether scenario bundle.
     * Enforces physical invariants and updates associated state variables within {@code encapsulating}.
     *
     * @param version the version parameter (String)
     * @param planetPreset the planet preset parameter (PlanetPreset)
     * @param ecologyPreset the ecology preset parameter (EcologyPreset)
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public EtherScenarioBundle(String version, PlanetPreset planetPreset, EcologyPreset ecologyPreset, Scenario scenario) {
        this(version, planetPreset, ecologyPreset, scenario, null, null, "Ether Creator", System.currentTimeMillis());
    }

    public EtherScenarioBundle {
        if (version == null) version = "1.0.0-beta.1";
    }
}

