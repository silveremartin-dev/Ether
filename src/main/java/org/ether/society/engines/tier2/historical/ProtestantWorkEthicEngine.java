/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Max Weber Protestant Work Ethic &amp; Human Capital Engine.
 *
 * <p>Formalizes Max Weber (1905) and Becker &amp; Woessmann (2009, <i>QJE</i> 124:531): religious
 * and institutional reforms promoting literacy, thrift, and ascetic reinvestment of surpluses
 * accelerate capital formation (\(\Delta K\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ProtestantWorkEthicEngine {
    private static final Logger logger = LoggerFactory.getLogger(ProtestantWorkEthicEngine.class);

    private static double capitalSavingsMultiplier = 1.30;
    private static double techAccelerationMultiplier = 1.20;

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double capital = cell.getResourceCapitalOr(100.0);
            double pop = cell.getPopulation();

            // Accelerated capital formation via frugal savings (Weber 1905; Becker & Woessmann 2009)
            // Human capital weighting derived continuously from capital per capita
            double literacyWeight = SmoothMath.smoothClamp(capital / (pop * 2000.0), 0.10, 1.0, 0.05);
            double capitalGain = (pop * 0.10 * (1.0 + (capitalSavingsMultiplier - 1.0) * literacyWeight)) * deltaYears;
            cell.setResourceCapital(capital + capitalGain);
        }
    }

    public static double getCapitalSavingsMultiplier() { return capitalSavingsMultiplier; }
    public static void setCapitalSavingsMultiplier(double mult) { capitalSavingsMultiplier = mult; }

    public static double getTechAccelerationMultiplier() { return techAccelerationMultiplier; }
    public static void setTechAccelerationMultiplier(double mult) { techAccelerationMultiplier = mult; }

    public static void resetState() {
        capitalSavingsMultiplier = 1.30;
        techAccelerationMultiplier = 1.20;
    }
}
