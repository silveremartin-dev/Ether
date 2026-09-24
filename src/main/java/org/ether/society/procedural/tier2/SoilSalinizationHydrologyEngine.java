/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Irrigated Basin Soil Salinization & Hydrological Mass-Balance Engine.
 *
 * <p>Models the progressive accumulation of soluble salts ($Na^+$, $Cl^-$, $Ca^{2+}$, $SO_4^{2-}$) in
 * irrigated agricultural topsoils under arid/semi-arid conditions with high potential evapotranspiration (PET):</p>
 * <pre>
 *   d[Salts]/dt = Q_irrigation · [Salts]_in - Q_drainage · [Salts]_leach - γ_flush
 *   Yield_Penalty = max(0.20, 1.0 - k_salts · [Salts]_soil)
 * </pre>
 *
 * <p>Historically documented in Sumerian Mesopotamia (-2400 to -1700 BCE), the Indus Valley,
 * and the modern Aral Sea basin (Jacobsen & Adams, 1958; Gelburd, 1985).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SoilSalinizationHydrologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SoilSalinizationHydrologyEngine.class);

    @Override
    public String getName() {
        return "Soil Salinization Hydrology Engine";
    }

    @Override
    public String getDescription() {
        return "Models progressive topsoil salt deposition and agricultural yield collapse under intensive irrigation in arid and semi-arid basins.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Soil Salinization & Irrigation Mass Balance (Jacobsen & Adams 1958, FAO 1985)]
               • Mass Balance:       d[Salts]/dt = (Q_irr · [Salts]_water) / Depth_rhizosphere - (Q_drain · [Salts]_leached)
               • Potential Evapo:    PET = 1.6 · (10 · Temp / I_heat)^a  (Thornthwaite)
               • Yield Degradation:  Y(t) = Y_0 · max(0.15, 1.0 - 0.08 · [Salts_dS/m])
               • Crop Substitution:  Wheat -> Barley (Tolerance shift) -> Final Sterilization
               Ref: T. Jacobsen & R. M. Adams (1958) "Salt and Silt in Ancient Mesopotamian Agriculture", Science 128
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Agro-Hydrological Degradation";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 100) continue;

            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Salinization is acute in arid/semi-arid plains and lowlands (Rain < 400mm, Temp > 20°C)
            if (rainfall < 400.0 && temp > 18.0 && elevation < 600.0) {
                double food = cell.getFoodResource() != null ? cell.getFoodResource() : 1000.0;
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

                // Irrigation intensity scales with population density and agrarian technology (pre-industrial drainage limits)
                double irrigationIntensity = Math.min(3.0, (pop / 200.0)) * (tech <= 4.0 ? 1.2 : 0.4); // Modern drainage mitigates
                double annualSaltDepositionRate = 0.008 * irrigationIntensity * (temp / 20.0) * (1.0 - Math.min(1.0, rainfall / 400.0));

                double degradation = Math.max(0.15, 1.0 - (annualSaltDepositionRate * deltaYears));
                cell.setFoodResource(Math.max(10.0, food * degradation));
            }
        }
    }
}
