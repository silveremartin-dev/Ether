/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
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
 * @version 1.0.0-beta.2
 */
public class PublicSanitationWaterGridEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(PublicSanitationWaterGridEngine.class);

    @Override
    public String getName() {
        return "PublicSanitationWaterGridEngine";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processSanitationGrids(cells, deltaYears);
    }

    /**
     * Executes one tick of sanitation coverage and pathogen mortality attenuation.
     *
     * @param cells      inhabited cells
     * @param deltaYears time step fraction in years
     */
    public static void processSanitationGrids(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double capitalPerCapita = capital / pop;
            double waterAvailability = cell.getWaterResourceOr(1.0);

            // Urban crowding fecal pathogen pressure
            double cellAreaKm2 = 36.13; // default res 8 cell area or average
            double densityPerKm2 = pop / cellAreaKm2;
            double crowdingPathogenPressure = Math.min(1.0, densityPerKm2 / 200.0);

            // Municipal sanitation infrastructure requires masonry/iron piping capital (k_pc >= 200 kg/cap)
            double sanitationCoverage = 0.0;
            if (capitalPerCapita > 200.0) {
                sanitationCoverage = Math.min(0.98, (capitalPerCapita - 200.0) / 2000.0);
            }

            // Net waterborne pathogen mortality risk
            double waterborneMortalityRisk = 0.025 * crowdingPathogenPressure * (1.0 - sanitationCoverage);

            // Adjust cell lifespan accordingly (lifespan ranges from ~28 in unhygienic dense cities to ~78 in clean grids)
            double currentLifespan = cell.getLifespanOr(35.0);
            double targetLifespan = 30.0 + 48.0 * sanitationCoverage * Math.min(1.0, waterAvailability);
            double updatedLifespan = currentLifespan + (targetLifespan - currentLifespan) * Math.min(1.0, deltaYears * 0.1);
            cell.setLifespan(Math.clamp(updatedLifespan, 22.0, 85.0));
        }
    }
}
