/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Gerhard Lenski Ecological-Evolutionary Model Variant B9.1 (Pure) & B9.2 (Hybrid).
 * Lenski Law: Gini inequality peaks in Agrarian societies and is modulated by technology stage.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LenskiPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LenskiPureEngine.class);

    /**
     * Computes theoretical Lenski-Milanovic biophysical Gini coefficient directly from per-capita
     * physical capital stock \(k_{\text{pc}} = K/N\) (\(\text{kg/capita}\))
     * (Lenski 1966 <i>Power and Privilege</i>; Milanovic, Lindert &amp; Williamson 2011 <i>Pre-Industrial Inequality</i>;
     * Borgerhoff Mulder et al. 2009 <i>Science</i> 326:682):
     * $$G^*(k_{\text{pc}}) = G_{\text{forager}} + \Delta G_{\text{agr}} \exp\!\left(-\frac{[\ln((k_{\text{pc}}+10)/(k_{\text{agr}}+10))]^2}{2\sigma_k^2}\right) + \Delta G_{\text{ind}} \, \frac{k_{\text{pc}}^2}{k_{\text{pc}}^2 + k_{\text{ind}}^2}$$
     *
     * @param capitalPerCapitaKg per-capita physical capital stock \(k_{\text{pc}} = K/N\) in \(\text{kg/capita}\)
     * @return theoretical Gini index in \([0.18, 0.70]\)
     */
    public static double calculateLenskiGini(double capitalPerCapitaKg) {
        double kpc = org.ether.society.core.math.SmoothMath.softplus(capitalPerCapitaKg, 0.0, 10.0);
        double gForager = 0.25;
        double logRatio = Math.log((kpc + 10.0) / (280.0 + 10.0));
        double agrarianSurge = 0.40 * Math.exp(-(logRatio * logRatio) / (2.0 * 1.25 * 1.25));
        double industrialShift = 0.13 * (kpc * kpc) / (kpc * kpc + 2500.0 * 2500.0);
        return gForager + agrarianSurge + industrialShift;
    }

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;
            double kpc = cell.getResourceCapitalOr(0.0) / pop;
            double theoreticalGini = calculateLenskiGini(kpc);
            if (cell.getResourceCapital() != null) {
                cell.setResourceCapital(cell.getResourceCapital() * (1.0 - theoreticalGini * 0.05 * deltaYears));
            }
        }
    }
}


