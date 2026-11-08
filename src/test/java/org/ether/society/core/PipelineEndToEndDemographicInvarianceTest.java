/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.ConfigurationLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Pipeline End-To-End Demographic Invariance &amp; Non-Divergence Test</h1>
 * <p>
 * Validates that executing the complete {@link H3SimulationEngine} and {@link SimulationPipeline} end-to-end
 * (across both DOD buffers and cellular representations) maintains strict demographic mass conservation
 * and does NOT suffer from engine double-counting or numerical explosion.
 * </p>
 * <p>
 * Invariant Directives:
 * <ul>
 *   <li>Maximum monthly demographic change must never exceed $|\Delta N / N| \le 2.5\%$ per month
 *       (strictly preventing runaway duplicate birth additions).</li>
 *   <li>Multi-year demographic growth rate across non-crisis baseline epochs must remain within
 *       the empirical cross-taxa biological envelope: $-6.0\% \le r_{\text{net}} \le +3.5\%$ per year.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PipelineEndToEndDemographicInvarianceTest {

    @Test
    @DisplayName("Verify Demographic Growth Rate Invariance on End-To-End SimulationPipeline (24 Ticks)")
    public void testPipelineDemographicGrowthInvariance() throws Exception {
        Scenario scenario = Scenario.getBuiltInScenarios().stream()
                .filter(s -> "earth_+1000_song_dynasty".equalsIgnoreCase(s.getPresetKey())
                        || "song_dynasty".equalsIgnoreCase(s.getPresetKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(scenario, "Song Dynasty scenario preset must be available");
        scenario.setH3Resolution(3);
        scenario.setTargetCohortSize(50000);

        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(3));
        var config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);

        long initialPopulation = engine.getTotalPopulation();
        assertTrue(initialPopulation > 1_000_000L, "Initial population must be positive and realistic");

        long previousPop = initialPopulation;

        // Run 24 consecutive monthly ticks (2 complete years)
        for (int tick = 0; tick < 24; tick++) {
            engine.stepForward(1);

            long currentPop = engine.getTotalPopulation();

            double monthlyRatio = Math.abs((double) (currentPop - previousPop) / (double) previousPop);
            assertTrue(monthlyRatio < 0.025,
                    String.format(java.util.Locale.ROOT,
                            "Tick %d: Monthly demographic variation of %.4f%% exceeds invariant ceiling (2.5%%/month). Possible double counting!",
                            tick, monthlyRatio * 100.0));

            previousPop = currentPop;
        }

        double totalTwoYearGrowthRate = (double) (previousPop - initialPopulation) / (double) initialPopulation;
        double annualizedGrowthRate = totalTwoYearGrowthRate / 2.0;

        assertTrue(annualizedGrowthRate <= 0.035,
                String.format(java.util.Locale.ROOT,
                        "Annualized demographic growth of %.3f%% exceeds historical biological ceiling (+3.5%%/yr)",
                        annualizedGrowthRate * 100.0));
        assertTrue(annualizedGrowthRate >= -0.060,
                String.format(java.util.Locale.ROOT,
                        "Annualized demographic contraction of %.3f%% exceeds historical biological floor (-6.0%%/yr)",
                        annualizedGrowthRate * 100.0));
    }
}
