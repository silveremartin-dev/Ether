/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.model.Biome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Irrigated Basin Soil Salinization & Hydrological Mass-Balance Engine.
 *
 * <p>Models the progressive accumulation of soluble salts ($Na^+$, $Cl^-$, $Ca^{2+}$, $SO_4^{2-}$) in
 * irrigated agricultural topsoils under arid/semi-arid conditions with high potential evapotranspiration (PET):</p>
 * <pre>
 *   d[Salts]/dt = Q_irrigation Â· [Salts]_in - Q_drainage Â· [Salts]_leach - Î³_flush
 *   Yield_Penalty = max(0.20, 1.0 - k_salts Â· [Salts]_soil)
 * </pre>
 *
 * <p>Historically documented in Sumerian Mesopotamia (-2400 to -1700 BCE), the Indus Valley,
 * and the modern Aral Sea basin (Jacobsen & Adams, 1958; Gelburd, 1985).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SoilSalinizationHydrologyEngine extends AbstractSimulationEngine {
    public SoilSalinizationHydrologyEngine() {
        super("SoilSalinizationHydrologyEngine", "SoilSalinizationHydrologyEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(SoilSalinizationHydrologyEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Soil Salinization Hydrology Engine";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models progressive topsoil salt deposition and agricultural yield collapse under intensive irrigation in arid and semi-arid basins.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Soil Salinization & Irrigation Mass Balance (Jacobsen & Adams 1958, FAO 1985)]
               • Mass Balance:       d[Salts]/dt = (Q_irr Â· [Salts]_water) / Depth_rhizosphere - (Q_drain Â· [Salts]_leached)
               • Potential Evapo:    PET = 1.6 Â· (10 Â· Temp / I_heat)^a  (Thornthwaite)
               • Yield Degradation:  Y(t) = Y_0 Â· max(0.15, 1.0 - 0.08 Â· [Salts_dS/m])
               • Crop Substitution:  Wheat -> Barley (Tolerance shift) -> Final Sterilization
               Ref: T. Jacobsen & R. M. Adams (1958) "Salt and Silt in Ancient Mesopotamian Agriculture", Science 128
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Agro-Hydrological Degradation";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 50.0) return;

        double rainfall = buffer.getRainfall()[i];
        double temp = buffer.getTemperature()[i];
        double elevation = buffer.getElevation()[i];

        if (rainfall < 450.0 && temp > 18.0 && elevation < 800.0) {
            double kPerCapita = buffer.getResourceCapital()[i] / Math.max(1.0, pop);
            double food = buffer.getFoodResource()[i];

            double modernDrainageActivation = 1.0 / (1.0 + Math.exp(-(kPerCapita - 1200.0) / 350.0));
            double irrigationEfficiencyFactor = 1.2 * (1.0 - modernDrainageActivation) + 0.4 * modernDrainageActivation;
            double irrigationIntensity = Math.min(3.0, (pop / 200.0)) * irrigationEfficiencyFactor;

            double accessibleAquifer = buffer.getFreshwaterAquifer()[i];
            double capillaryFlux = (accessibleAquifer > 10.0)
                    ? Math.max(0.0, (temp - 15.0) * 0.04 * (1.0 - Math.min(1.0, rainfall / 450.0)))
                    : 0.0;

            double saltInputRate = (irrigationIntensity * 0.15) + (capillaryFlux * 0.08);
            double drainageLeaching = (0.005 + 0.075 * modernDrainageActivation)
                    + (rainfall > 400.0 ? (rainfall - 400.0) * 0.002 : 0.0);

            // Quasi-steady salinity concentration (dS/m) clamped to [0, 30]
            double eqSalinity = Math.clamp(saltInputRate / Math.max(0.005, drainageLeaching), 0.0, 30.0);
            double excessSalinity = Math.max(0.0, eqSalinity - 2.0);
            double yieldFactor = Math.clamp(1.0 - (0.06 * excessSalinity), 0.10, 1.0);
            double annualDegradation = Math.pow(yieldFactor, Math.min(5.0, dtYears));

            double newFood = Math.max(5.0, food * annualDegradation);
            acc.dFoodResource += (newFood - food);

            double agri = buffer.getBiomassAgriculture()[i];
            if (agri > 0.0) {
                double newAgri = Math.max(1.0, agri * annualDegradation);
                acc.dBiomassAgriculture += (newAgri - agri);
            }
        }
    }
}


