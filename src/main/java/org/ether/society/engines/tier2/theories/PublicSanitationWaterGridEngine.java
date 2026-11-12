/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <h1>Public Sanitation &amp; Municipal Clean Water Grid Engine</h1>
 * <p>
 * Models the biophysical and epidemiological mechanisms underlying the Preston Curve (1975)
 * and Omran's Epidemiological Transition (1971).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>David Cutler &amp; Grant Miller (2005)</b> <i>The Role of Public Health Improvements in Health Advances: The Twentieth-Century United States</i>, Demography 42(1): 1–22.</li>
 *   <li><b>Abdel R. Omran (1971)</b> <i>The Epidemiological Transition: A Theory of the Epidemiology of Population Change</i>, Milbank Mem. Fund Q. 49(4): 509–538.</li>
 *   <li><b>Samuel H. Preston (1975)</b> <i>The Changing Relation between Mortality and Level of Economic Development</i>, Population Studies 29(2): 231–248.</li>
 * </ul>
 *
 * <h2>Mechanistic Dynamics</h2>
 * <p>
 * In dense human agglomerations ($N \ge 2\,000$ hab), unchanneled wastewater and shallow contaminated wells
 * create an endogenous waterborne pathogen load (cholera, dysentery, typhoid) scaling with urban fecal density:
 * $$\lambda_{\text{fecal}} = \frac{N_{\text{cell}}}{A_{\text{cell}} \cdot (1 + \text{AquiferFlush})}$$
 * Clean water aqueducts, subterranean masonry sewage grids, sand filtration, and municipal chlorination
 * reduce pathogen transmission directly as a function of dedicated physical sanitary infrastructure ($K_{\text{sanitation}}$),
 * without any arbitrary calendar-year date check.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PublicSanitationWaterGridEngine extends AbstractSimulationEngine {
    public PublicSanitationWaterGridEngine() {
        super("PublicSanitationWaterGridEngine", "PublicSanitationWaterGridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PublicSanitationWaterGridEngine.class);

    @Override
    public String id() {
        return "PublicSanitationWaterGridEngine";
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public EngineTier tier() {
        return EngineTier.TIER_2_CLIODYNAMIC;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()));
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processSanitationGrids(buffer, dtYears);
    }

    @Override
    public void reset() {
        // Stateless physical operator
    }

    @Override
    public String getName() {
        return "PublicSanitationWaterGridEngine";
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processSanitationGrids(buffer, deltaYears);
    }

    /**
     * Executes one tick of sanitation coverage and pathogen mortality attenuation directly over WorldBuffer.
     *
     * @param buffer     contiguous world buffer
     * @param deltaYears time step fraction in years
     */
    public static void processSanitationGrids(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        float[] pop = buffer.getBiomassHuman();
        float[] cap = buffer.getResourceCapital();
        float[] water = buffer.getWaterResource();
        float[] lifespan = buffer.getLifespan();

        for (int i = 0; i < n; i++) {
            float p = pop[i];
            if (p <= 0.001f) continue;
            float capital = (cap != null) ? cap[i] : 0.0f;
            float kPerCap = capital / p;
            float wAvail = (water != null) ? water[i] : 1.0f;

            double densityPerKm2 = (p / 0.06) / 36.13;
            double crowdingPathogenPressure = Math.min(1.0, densityPerKm2 / 200.0);
            double sanitationCoverage = 0.0;
            if (kPerCap > 200.0) {
                sanitationCoverage = Math.min(0.98, (kPerCap - 200.0) / 2000.0);
            }
            double targetLifespan = 30.0 + 48.0 * sanitationCoverage * Math.min(1.0, (double) wAvail)
                    - 8.0 * crowdingPathogenPressure * (1.0 - sanitationCoverage);
            float currentLifespan = (lifespan != null && lifespan[i] > 10.0f) ? lifespan[i] : 35.0f;
            float updatedLifespan = (float) (currentLifespan + (targetLifespan - currentLifespan) * Math.min(1.0, deltaYears * 0.1));
            if (lifespan != null) {
                lifespan[i] = Math.clamp(updatedLifespan, 22.0f, 85.0f);
            }
        }
    }
}
