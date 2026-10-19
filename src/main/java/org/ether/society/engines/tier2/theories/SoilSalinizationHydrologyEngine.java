/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.engines.ProceduralEnginePlugin;
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
 * @version 1.0.0-beta.2
 */
public class SoilSalinizationHydrologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SoilSalinizationHydrologyEngine.class);

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code SoilSalinizationHydrologyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Soil Salinization Hydrology Engine";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code SoilSalinizationHydrologyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models progressive topsoil salt deposition and agricultural yield collapse under intensive irrigation in arid and semi-arid basins.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code SoilSalinizationHydrologyEngine}.
     *
     * @return the resulting computation or state reference
     */
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
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code SoilSalinizationHydrologyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Agro-Hydrological Degradation";
    }

    /*
     * Static utility entry point for fused pipeline execution.
     *
     * @param cells the spatial grid cells to process
     * @param deltaYears elapsed time step in fractional solar years
     */
    public static void processSoilSalinization(List<H3Cell> cells, double deltaYears) {
        new SoilSalinizationHydrologyEngine().process(cells, deltaYears);
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        int salinizedCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 50) continue;

            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Salinization is acute in arid/semi-arid plains and lowlands (Rain < 450mm, Temp > 18°C)
            if (rainfall < 450.0 && temp > 18.0 && elevation < 800.0) {
                double currentSalinity = cell.getSoilSalinity() != null ? cell.getSoilSalinity() : 0.0;
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
                double food = cell.getFoodResource() != null ? cell.getFoodResource() : 1000.0;

                // 1. Irrigation intensity scales with population density and agrarian technology (pre-industrial drainage limits)
                double irrigationIntensity = Math.min(3.0, (pop / 200.0)) * (tech <= 4.0 ? 1.2 : 0.4);

                // 2. Capillary groundwater upward flux driven by potential evapotranspiration (PET >> Precipitation)
                double accessibleAquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 0.0;
                double capillaryFlux = (accessibleAquifer > 10.0)
                        ? Math.max(0.0, (temp - 15.0) * 0.04 * (1.0 - Math.min(1.0, rainfall / 450.0)))
                        : 0.0;

                // 3. Salt deposition rate (irrigation canal solute load + upward capillary rise)
                double saltInputRate = (irrigationIntensity * 0.15) + (capillaryFlux * 0.08);

                // 4. Subsurface drainage leaching (modern tile drainage mitigates; pre-industrial lacks drainage)
                double drainageLeaching = (tech > 5.0 ? 0.08 : 0.005) + (rainfall > 400.0 ? (rainfall - 400.0) * 0.002 : 0.0);
                double saltLeachedRate = drainageLeaching * currentSalinity;

                // 5. Dynamic mass balance integration: d[Salts]/dt
                double deltaSalt = (saltInputRate - saltLeachedRate) * deltaYears;
                double newSalinity = Math.clamp(currentSalinity + deltaSalt, 0.0, 30.0);
                cell.setSoilSalinity(newSalinity);

                if (newSalinity > 2.0) {
                    salinizedCells++;
                }

                // 6. Maas-Hoffman / Jacobsen-Adams Crop Yield Attenuation
                // Below 2.0 dS/m: no degradation. Above 2.0: linear yield penalty up to 90% collapse
                double excessSalinity = Math.max(0.0, newSalinity - 2.0);
                double yieldFactor = Math.clamp(1.0 - (0.06 * excessSalinity), 0.10, 1.0);

                double annualDegradation = Math.pow(yieldFactor, Math.min(5.0, deltaYears));
                cell.setFoodResource(Math.max(5.0, food * annualDegradation));

                if (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0) {
                    cell.setBiomassAgriculture(Math.max(1.0, cell.getBiomassAgriculture() * annualDegradation));
                }
            }
        }

        if (salinizedCells > 0) {
            logger.debug("Soil Salinization Engine: Evaluated {} salinized agrarian cells.", salinizedCells);
        }
    }
}


