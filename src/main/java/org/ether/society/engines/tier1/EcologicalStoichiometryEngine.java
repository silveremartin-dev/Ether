/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Stoichiometry &amp; Biogeochemical Element Allocation Engine.
 *
 * <p>Formalizes the stoichiometric mass-balance principles established by Sterner &amp; Elser
 * (2002, <i>Ecological Stoichiometry: The Biology of Elements from Molecules to the Biosphere</i>,
 * Princeton University Press) and Alfred Redfield (1934, 1958).</p>
 *
 * <p>Key Biophysical Mechanics:</p>
 * <ol>
 *   <li><b>Redfield Molar Invariants</b>: Marine phytoplankton \(\text{C:N:P} = 106:16:1\) molar ratio
 *       (\(\approx 41.0 : 7.2 : 1.0\) by mass).</li>
 *   <li><b>Terrestrial Plant &amp; Agronomic Stoichiometry</b>: Foliar and grain tissues maintain tight
 *       elemental balances (\(\text{C:N:P} \approx 40.0 : 2.0 : 0.2\) by mass).</li>
 *   <li><b>Continuous Liebig Multi-Nutrient Limitation</b>: Replaces discrete min-thresholds with
 *       smooth (\(C^\infty\)) softmin functions over available N-P-K soil pools.</li>
 *   <li><b>Elevated \(\text{CO}_2\) Protein Dilution Effect (Loladze 2002, 2014; Myers et al. 2014)</b>:
 *       Atmospheric carbon saturation accelerates carbohydrate synthesis faster than root nitrogen
 *       transpiration, diluting crop protein and mineral density by up to \(\sim 10\text{--}15\%\).</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://press.princeton.edu/books/hardcover/9780691074917/ecological-stoichiometry">Sterner &amp; Elser (2002)</a>
 */
public class EcologicalStoichiometryEngine extends AbstractSimulationEngine {
    public EcologicalStoichiometryEngine() {
        super("EcologicalStoichiometryEngine", "EcologicalStoichiometryEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(EcologicalStoichiometryEngine.class);

    // --- Universal Stoichiometric Constants ---

    /** Redfield marine carbon to phosphorus mass ratio (\(106 \times 12.011 / 30.974 \approx 41.1\)). */
    public static final double REDFIELD_MASS_RATIO_C_P = 41.1;

    /** Redfield marine nitrogen to phosphorus mass ratio (\(16 \times 14.007 / 30.974 \approx 7.23\)). */
    public static final double REDFIELD_MASS_RATIO_N_P = 7.23;

    /** Canonical terrestrial plant mass ratios: Carbon fraction in dry biomass (\(\sim 45\%\)). */
    public static final double TERRESTRIAL_PLANT_CARBON_FRACTION = 0.45;

    /** Canonical terrestrial plant nitrogen demand (\(\approx 20.0\text{ kg N / ton dry biomass}\)). */
    public static final double TERRESTRIAL_PLANT_NITROGEN_KG_PER_TON = 20.0;

    /** Canonical terrestrial plant phosphorus demand (\(\approx 2.5\text{ kg P / ton dry biomass}\)). */
    public static final double TERRESTRIAL_PLANT_PHOSPHORUS_KG_PER_TON = 2.5;

    /** Pre-industrial atmospheric \(\text{CO}_2\) baseline in ppm. */
    public static final double PRE_INDUSTRIAL_CO2_PPM = 280.0;

    /** Maximum nutritional protein dilution penalty under extreme atmospheric carbon enrichment (\(\sim 12\%\)). */
    public static final double MAX_PROTEIN_DILUTION_FRACTION = 0.12;

    /**
     * Calculates the continuous Liebig multi-nutrient growth limitation factor:
     * \[
     * L(\text{N}, \text{P}, \text{K}) = \text{softmin}\left(\frac{\text{N}}{\text{N}_{\text{opt}}}, \;
     * \text{softmin}\left(\frac{\text{P}}{\text{P}_{\text{opt}}}, \; \frac{\text{K}}{\text{K}_{\text{opt}}}, \; 10.0\right), \; 10.0\right)
     * \]
     *
     * @param nitrogenRatio   available N relative to optimal requirement
     * @param phosphorusRatio available P relative to optimal requirement
     * @param potassiumRatio  available K relative to optimal requirement
     * @return dimensionless yield scalar in \([0, 1]\)
     */
    public static double calculateContinuousLiebigFactor(double nitrogenRatio, double phosphorusRatio, double potassiumRatio) {
        double safeN = Math.max(0.01, nitrogenRatio);
        double safeP = Math.max(0.01, phosphorusRatio);
        double safeK = Math.max(0.01, potassiumRatio);

        double minPK = SmoothMath.softmin(safeP, safeK, 8.0);
        double minNPK = SmoothMath.softmin(safeN, minPK, 8.0);

        return Math.clamp(minNPK, 0.05, 1.0);
    }

    /**
     * Calculates the nutritional protein dilution factor under elevated atmospheric \(\text{CO}_2\)
     * (Loladze 2002, 2014; Myers et al. 2014 <i>Nature</i>):
     * \[
     * D_{\text{protein}}(\text{CO}_2) = 1.0 - \delta_{\text{max}} \cdot \tanh\left(\frac{\max(0, \text{CO}_2 - 280)}{350.0}\right)
     * \]
     *
     * @param co2Ppm atmospheric carbon dioxide concentration in ppm
     * @return nutritional quality multiplier in \([0.88, 1.0]\)
     */
    public static double calculateProteinDilutionFactor(double co2Ppm) {
        double excessCo2 = Math.max(0.0, co2Ppm - PRE_INDUSTRIAL_CO2_PPM);
        double penalty = MAX_PROTEIN_DILUTION_FRACTION * Math.tanh(excessCo2 / 350.0);
        return 1.0 - penalty;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double agriBiomass = buffer.getBiomassAgriculture()[i];
        if (agriBiomass <= 0.0) return;

        double co2Ppm = 280.0 + buffer.getPollutionLevel()[i] * 0.4;
        double proteinFactor = calculateProteinDilutionFactor(co2Ppm);

        double soc = buffer.getSoilOrganicCarbon()[i] > 0.0f ? buffer.getSoilOrganicCarbon()[i] : 25.0;
        double metal = buffer.getMetalResource()[i];
        double capital = buffer.getResourceCapital()[i];
        double pop = Math.max(1.0, buffer.getBiomassHuman()[i] / 0.06);
        double capPerCap = capital / pop;

        double nRatio = Math.clamp(soc / 30.0 + 0.30 * Math.tanh(capPerCap / 50.0), 0.20, 1.50);
        double pRatio = Math.clamp(soc / 35.0 + 0.20 * Math.tanh(metal / 200.0), 0.20, 1.50);
        double kRatio = Math.clamp(soc / 35.0 + 0.20 * Math.tanh(metal / 200.0), 0.20, 1.50);

        double liebigFactor = calculateContinuousLiebigFactor(nRatio, pRatio, kRatio);
        double effectiveNutritionalYield = agriBiomass * liebigFactor * proteinFactor;
        acc.dFoodResource += (float) (effectiveNutritionalYield * 15.0 * dtYears);
    }
}
