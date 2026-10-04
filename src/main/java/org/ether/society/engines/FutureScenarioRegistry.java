/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Deterministic Future Physical Scenario Registry.
 * Formalizes initial physical forcing terms for future projections:
 * <ol>
 *   <li><b>Business As Usual (SSP5-8.5)</b>: High CO2 emissions (+4.5°C), zero fusion, continuous NPK depletion.</li>
 *   <li><b>Technological Singularity &amp; Fusion</b>: Breakthrough D-T Fusion (EROEI &gt;= 40:1), ASI throughput (&gt;= 10^16 bits/s).</li>
 *   <li><b>Nuclear Winter Catastrophe</b>: Stratospheric soot injection (&tau; = 1.5), solar attenuation ($S_0 \cdot e^{-\tau}$).</li>
 *   <li><b>AMOC Collapse &amp; Tipping Point</b>: Freshwater ice melt drops ocean salinity (&lt; 30 PSU), triggering AMOC shutdown.</li>
 *   <li><b>Peak Phosphorus Agricultural Cliff</b>: Geological exhaustion of rock phosphate (P), triggering Liebig minimum collapse.</li>
 *   <li><b>Space Terraforming &amp; Off-World Colony</b>: Low gravity ($g_{rel} = 0.38$), artificial pressurized habitat and greenhouse atmosphere.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class FutureScenarioRegistry {
    private static final Logger logger = LoggerFactory.getLogger(FutureScenarioRegistry.class);

    public record PhysicalScenarioPreset(String id, String name, String description, double initialYear) {}

    public static final PhysicalScenarioPreset SCENARIO_BAU = new PhysicalScenarioPreset(
            "BAU_SSP585", "Business As Usual (SSP5-8.5)",
            "Continued high CO2 emissions (+4.5°C), fossil fuel dependence, and gradual agricultural NPK soil depletion.", 2026.0);

    public static final PhysicalScenarioPreset SCENARIO_SINGULARITY = new PhysicalScenarioPreset(
            "TECH_SINGULARITY", "Technological Singularity & D-T Fusion",
            "Emergence of Artificial Superintelligence (10^16 bits/s) and D-T fusion (EROEI >= 40:1), eliminating energy scarcity.", 2045.0);

    public static final PhysicalScenarioPreset SCENARIO_NUCLEAR_WINTER = new PhysicalScenarioPreset(
            "NUCLEAR_WINTER", "Nuclear Winter & Stratospheric Soot",
            "Massive stratospheric soot injection (tau = 1.5) causing severe global freeze (-15°C) and crop failures.", 2035.0);

    public static final PhysicalScenarioPreset SCENARIO_AMOC_COLLAPSE = new PhysicalScenarioPreset(
            "AMOC_COLLAPSE", "Thermohaline AMOC Circulation Collapse",
            "Ocean freshening (< 30 PSU) triggering Atlantic Meridional Overturning Circulation shutdown and European cooling.", 2060.0);

    public static final PhysicalScenarioPreset SCENARIO_PEAK_PHOSPHORUS = new PhysicalScenarioPreset(
            "PEAK_PHOSPHORUS", "Mineral Phosphorus Cliff (Peak P)",
            "Depletion of global geological phosphate rock reserves, triggering Liebig agricultural yields collapse.", 2050.0);

    public static final PhysicalScenarioPreset SCENARIO_SPACE_COLONY = new PhysicalScenarioPreset(
            "SPACE_TERRAFORM", "Extraterrestrial Colony & Terraforming",
            "Colonization of low-gravity cells (0.38g) with artificial pressurized life support and controlled atmospheres.", 2150.0);

    public static final PhysicalScenarioPreset SCENARIO_SOVEREIGN_AI_SINGLE = new PhysicalScenarioPreset(
            "SOVEREIGN_AI_LEVIATHAN", "Sovereign AI Leviathan Governance",
            "Global resource and ecological optimization governed by a centralized, benevolent Artificial Superintelligence.", 2040.0);

    public static final PhysicalScenarioPreset SCENARIO_SOVEREIGN_AI_MULTIPOLAR = new PhysicalScenarioPreset(
            "SOVEREIGN_AI_MULTIPOLAR", "Multipolar Sovereign AI Cold War",
            "Geopolitical multi-agent competition among regional autonomous sovereign AI networks for energy and resources.", 2042.0);

    /*
     * Applies physical initial forcing parameters to cells for a given future scenario preset.
     */
    public static void applyScenarioForcing(PhysicalScenarioPreset preset, List<H3Cell> cells) {
        if (preset == null || cells == null || cells.isEmpty()) return;

        logger.info("Applying physical forcing terms for future scenario: {}", preset.name());

        // Iterate over spatial cell domains and apply localized cellular state transformations
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
            } else if (preset.id().equals("SOVEREIGN_AI_LEVIATHAN")) {
                // Automatically register the Sovereign AI plugin
                ProceduralEngineRegistry.registerPlugin("SovereignAIGovernance",
                        new SovereignAIGovernanceEngine(SovereignAIGovernanceEngine.GovernanceMode.LEVIATHAN_UNIFIED, 0.9, 50.0, 0.20));
            } else if (preset.id().equals("SOVEREIGN_AI_MULTIPOLAR")) {
                // Automatically register Multi-Agent AI plugin
                ProceduralEngineRegistry.registerPlugin("SovereignAIGovernance",
                        new SovereignAIGovernanceEngine(SovereignAIGovernanceEngine.GovernanceMode.GEO_POLITICAL_COMPETITION, 0.8, 100.0, 0.25));
            }
        }
    }

    /*
     * Finds matching PhysicalScenarioPreset for a given Scenario based on name or description.
     */
    public static PhysicalScenarioPreset findPresetForScenario(Scenario scenario) {
        if (scenario == null || scenario.getName() == null) return null;
        String name = scenario.getName().toLowerCase();
        String desc = scenario.getDescription() != null ? scenario.getDescription().toLowerCase() : "";

        if (name.contains("sovereign") || name.contains("maître du monde") || name.contains("leviathan") || desc.contains("gouvernance ia")) {
            return name.contains("multipolar") || name.contains("guerre froide") ? SCENARIO_SOVEREIGN_AI_MULTIPOLAR : SCENARIO_SOVEREIGN_AI_SINGLE;
        }
        if (name.contains("nucléaire") || name.contains("nuclear") || desc.contains("nucléaire") || desc.contains("soot")) {
            return SCENARIO_NUCLEAR_WINTER;
        }
        if (name.contains("business as usual") || name.contains("ssp5") || name.contains("bau")) {
            return SCENARIO_BAU;
        }
        if (name.contains("singularité") || name.contains("singularity") || name.contains("asi")) {
            return SCENARIO_SINGULARITY;
        }
        if (name.contains("amoc") || name.contains("thermohalin")) {
            return SCENARIO_AMOC_COLLAPSE;
        }
        if (name.contains("phosphate") || name.contains("peak p")) {
            return SCENARIO_PEAK_PHOSPHORUS;
        }
        if (name.contains("terraforma") || name.contains("space")) {
            return SCENARIO_SPACE_COLONY;
        }
        return null;
    }
}
