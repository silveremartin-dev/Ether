/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <h1>Von ThÃƒÂ¼nen Agricultural Rings &amp; Overland Transport Friction Engine</h1>
 * <p>
 * Models the geographic logistics of agricultural food transport and urban agglomeration limits.
 * Replaces synthetic urbanization rate sigmoids with authentic spatial transport decay
 * (Von ThÃƒÂ¼nen 1826; Krugman 1991).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>Johann Heinrich von ThÃƒÂ¼nen (1826)</b> <i>Der isolirte Staat in Beziehung auf Landwirthschaft und NationalÃƒÂ¶konomie</i>, Perthes, Hamburg.</li>
 *   <li><b>Paul Krugman (1991)</b> <i>Increasing Returns and Economic Geography</i>, J. Political Econ. 99(3): 483Ã¢â‚¬â€œ499.</li>
 *   <li><b>Paul Bairoch (1988)</b> <i>Cities and Economic Development: From the Dawn of History to the Present</i>, Univ. of Chicago Press.</li>
 * </ul>
 *
 * <h2>Transport Mechanics &amp; Urbanization Limits</h2>
 * <p>
 * In pre-industrial terrestrial regimes (pack animals, ox-drawn carts), hauling bulk grain consumes
 * food at a rate of $\sim 1\text{--}2\%$ of the cargo per $10\text{ km}$ (the draft animals must eat the grain they carry).
 * Consequently, at a distance of $r_{\text{critical}} \approx 100\text{--}150\text{ km}$, transport cost exceeds total grain calories,
 * strictly bounding the maximum sustainable population of inland cities to $N_{\text{max}} \le 50\,000\text{--}100\,000$ inhabitants
 * (Rome and Constantinople only broke this barrier via sea lanes and Nile grain fleets).
 * Paved macadam roads, canals, and steam railroads expand $r_{\text{critical}}$ exponentially, allowing modern mega-cities to emerge.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class VonThunenAgriculturalRingsEngine extends AbstractSimulationEngine {
    public VonThunenAgriculturalRingsEngine() {
        super("VonThunenAgriculturalRingsEngine", "VonThunenAgriculturalRingsEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(VonThunenAgriculturalRingsEngine.class);

    @Override
    public String getName() {
        return "VonThunenAgriculturalRingsEngine";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        double capitalPerCapita = capital / pop;
        boolean isCoastal = buffer.getBiomes()[i] == (byte) org.ether.society.model.Biome.OCEAN.ordinal()
                || buffer.getElevation()[i] < 0.0f;

        double maxSustainableAgglomeration;
        if (isCoastal) {
            maxSustainableAgglomeration = 500_000.0 + capitalPerCapita * 2500.0;
        } else {
            double ancientInlandBase = 25_000.0;
            double earlyModernExpansion = 55_000.0 * org.ether.society.core.math.SmoothMath.sigmoid(capitalPerCapita, 250.0, 75.0);
            double industrialRailExpansion = 5000.0 * org.ether.society.core.math.SmoothMath.softplus(capitalPerCapita - 800.0, 0.0, 100.0);
            maxSustainableAgglomeration = ancientInlandBase + earlyModernExpansion + industrialRailExpansion;
        }

        if (pop > maxSustainableAgglomeration) {
            double excessRatio = (pop - maxSustainableAgglomeration) / maxSustainableAgglomeration;
            double currentFood = buffer.getFoodResource()[i];
            double newFood = Math.max(0.1, currentFood * (1.0 - Math.min(0.5, excessRatio * 0.2)));
            acc.dFoodResource += (newFood - currentFood);
        }
    }
}
