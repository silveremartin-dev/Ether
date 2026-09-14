/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.security;

import org.ether.society.model.EcologyPreset;
import org.ether.society.model.EtherScenarioBundle;
import org.ether.society.model.Scenario;
import org.ether.society.procedural.PlanetPreset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EtherBundleSignerTest {

    @Test
    public void testSignAndVerifyBundle() {
        Scenario scenario = new Scenario();
        scenario.setName("Holocene Dawn");
        scenario.setDescription("Holocene simulation scenario");

        EtherScenarioBundle rawBundle = new EtherScenarioBundle("2.0.0", PlanetPreset.EARTH_LIKE, EcologyPreset.EARTH_STANDARD, scenario);
        EtherScenarioBundle signedBundle = EtherBundleSigner.signBundle(rawBundle, "Silvere");

        assertNotNull(signedBundle);
        assertNotNull(signedBundle.checksumSha256(), "Checksum must be generated");
        assertNotNull(signedBundle.signature(), "Signature must be generated");
        assertEquals("Silvere", signedBundle.author());

        boolean valid = EtherBundleSigner.verifyBundle(signedBundle);
        assertTrue(valid, "Authentic signed bundle must pass cryptographic verification");
    }

    @Test
    public void testTamperedBundleFailsVerification() {
        Scenario scenario = new Scenario();
        scenario.setName("Holocene Dawn");

        EtherScenarioBundle rawBundle = new EtherScenarioBundle("2.0.0", PlanetPreset.EARTH_LIKE, EcologyPreset.EARTH_STANDARD, scenario);
        EtherScenarioBundle signedBundle = EtherBundleSigner.signBundle(rawBundle, "Silvere");

        // Tamper scenario name while keeping original checksum/signature
        Scenario tamperedScenario = new Scenario();
        tamperedScenario.setName("Malicious Modded Scenario");

        EtherScenarioBundle tamperedBundle = new EtherScenarioBundle(
                signedBundle.version(),
                signedBundle.planetPreset(),
                signedBundle.ecologyPreset(),
                tamperedScenario,
                signedBundle.checksumSha256(),
                signedBundle.signature(),
                signedBundle.author(),
                signedBundle.createdTimestamp()
        );

        boolean valid = EtherBundleSigner.verifyBundle(tamperedBundle);
        assertFalse(valid, "Tampered bundle must fail cryptographic verification");
    }

    @Test
    public void testLegacyBundlePassesVerification() {
        Scenario scenario = new Scenario();
        scenario.setName("Legacy Scenario");

        EtherScenarioBundle legacyBundle = new EtherScenarioBundle("1.0.0", PlanetPreset.EARTH_LIKE, EcologyPreset.EARTH_STANDARD, scenario);
        boolean valid = EtherBundleSigner.verifyBundle(legacyBundle);
        assertTrue(valid, "Legacy unsigned bundle must pass verification with backward compatibility");
    }
}
