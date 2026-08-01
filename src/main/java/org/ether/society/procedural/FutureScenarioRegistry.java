/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Deterministic Future Physical Scenario Registry.
 * Formalizes initial physical forcing terms (desequilibres physiques de départ) for future projections:
 * <ol>
 *   <li><b>Business As Usual (SSP5-8.5)</b>: High CO2 emissions (+4.5°C), zero fusion, continuous NPK depletion.</li>
 *   <li><b>Technological Singularity & Fusion</b>: Breakthrough D-T Fusion (EROEI >= 40:1), ASI throughput (>= 10^16 bits/s).</li>
 *   <li><b>Nuclear Winter Catastrophe</b>: Stratospheric soot injection (τ = 1.5), solar attenuation (S_0 * e^-τ).</li>
 *   <li><b>AMOC Collapse & Tipping Point</b>: Freshwater ice melt drops ocean salinity (< 30 PSU), triggering AMOC shutdown.</li>
 *   <li><b>Peak Phosphorus Agricultural Cliff</b>: Geological exhaustion of rock phosphate (P), triggering Liebig minimum collapse.</li>
 *   <li><b>Space Terraforming & Off-World Colony</b>: Low gravity (g_rel = 0.38), artificial greenhouse P_atmo.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class FutureScenarioRegistry {
    private static final Logger logger = LoggerFactory.getLogger(FutureScenarioRegistry.class);

    public record PhysicalScenarioPreset(String id, String name, String description, double initialYear) {}

    public static final PhysicalScenarioPreset SCENARIO_BAU = new PhysicalScenarioPreset(
            "BAU_SSP585", "Business As Usual (SSP5-8.5)",
            "Poursuite des émissions de CO2 (+4.5°C), dépendance fossile et épuisement lent des sols NPK.", 2026.0);

    public static final PhysicalScenarioPreset SCENARIO_SINGULARITY = new PhysicalScenarioPreset(
            "TECH_SINGULARITY", "Singularité Technologique & Fusion D-T",
            "Émergence d'une ASI (10^16 bits/s) et fusion D-T (EROEI >= 40:1) éliminant la rareté énergétique.", 2045.0);

    public static final PhysicalScenarioPreset SCENARIO_NUCLEAR_WINTER = new PhysicalScenarioPreset(
            "NUCLEAR_WINTER", "Hiver Nucléaire & Ombre Stratosphérique",
            "Injection massive de suies stratosphériques (τ = 1.5) provoquant un gel mondial (-15°C).", 2035.0);

    public static final PhysicalScenarioPreset SCENARIO_AMOC_COLLAPSE = new PhysicalScenarioPreset(
            "AMOC_COLLAPSE", "Effondrement Thermohalin AMOC",
            "Dessalage des océans (< 30 PSU) provoquant l'arrêt du Gulf Stream et un refroidissement européen.", 2060.0);

    public static final PhysicalScenarioPreset SCENARIO_PEAK_PHOSPHORUS = new PhysicalScenarioPreset(
            "PEAK_PHOSPHORUS", "Falaise du Phosphate Minéral (Peak P)",
            "Épuisement des gisements géologiques de Phosphate (P), déclenchant la limite de Liebig.", 2050.0);

    public static final PhysicalScenarioPreset SCENARIO_SPACE_COLONY = new PhysicalScenarioPreset(
            "SPACE_TERRAFORM", "Colonie Extraterrestre & Terraformation",
            "Colonisation de cellules H3 à gravité réduite (0.38g) et atmosphère artificielle sous dôme.", 2150.0);

    /**
     * Applies physical initial forcing parameters to cells for a given future scenario preset.
     */
    public static void applyScenarioForcing(PhysicalScenarioPreset preset, List<H3Cell> cells) {
        if (preset == null || cells == null || cells.isEmpty()) return;

        logger.info("Applying physical forcing terms for future scenario: {}", preset.name());

        for (H3Cell cell : cells) {
            if (preset.id().equals("BAU_SSP585")) {
                // High CO2 greenhouse forcing (+4.5°C) & high initial pollution
                cell.setTemperature((cell.getTemperature() != null ? cell.getTemperature() : 15.0) + 4.5);
                cell.setPollutionLevel(2000.0);
            } else if (preset.id().equals("TECH_SINGULARITY")) {
                // High energy grid capability (50,000 W/capita)
                cell.setResourceCapital((cell.getResourceCapital() != null ? cell.getResourceCapital() : 1000.0) + 50000.0);
            } else if (preset.id().equals("NUCLEAR_WINTER")) {
                // Stratospheric soot injection
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.5);
            } else if (preset.id().equals("AMOC_COLLAPSE")) {
                // Ocean thermohaline collapse (-8°C cooling drop)
                cell.setTemperature((cell.getTemperature() != null ? cell.getTemperature() : 15.0) - 8.0);
            } else if (preset.id().equals("PEAK_PHOSPHORUS")) {
                // Phosphate depletion
                cell.setResourceMetal(5.0); // Depleted mineral ore
            }
        }
    }
}
