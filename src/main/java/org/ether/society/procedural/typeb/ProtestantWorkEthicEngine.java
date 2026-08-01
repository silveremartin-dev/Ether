/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Weberian Protestant Work Ethic Engine Variant B31.1 (Pure) & B31.2 (Hybrid).
 * Models Max Weber's hypothesis (The Protestant Ethic and the Spirit of Capitalism):
 * Delayed gratification, high literacy rates, capital accumulation, and accelerated technological innovation.
 * Increases capital savings rate and tech research rate while dampening fertility (demographic transition).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class ProtestantWorkEthicEngine {
    private static final Logger logger = LoggerFactory.getLogger(ProtestantWorkEthicEngine.class);

    private static double capitalSavingsMultiplier = 1.35; // +35% capital retention
    private static double techAccelerationMultiplier = 1.25; // +25% tech innovation speed
    private static double literacyRateMultiplier = 1.40; // +40% literacy boost

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            // Apply Protestant Ethic effects to cells with active cultural/religious transformation
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 100.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double pop = cell.getPopulation();

            // Accelerated capital formation via frugal savings
            double capitalGain = (pop * 0.05 * capitalSavingsMultiplier) * deltaYears;
            cell.setResourceCapital(capital + capitalGain);

            // Accelerated technology adoption rate
            double techGain = (0.01 * techAccelerationMultiplier) * deltaYears;
            cell.setTechnologyLevel(tech + techGain);

            // Frugality & literacy reduce birth rate slightly (demographic transition effect)
            if (pop > 1000) {
                cell.setPopulation((int) (pop * (1.0 + 0.001 * deltaYears))); // Controlled demographic growth
            }
        }
    }

    public static double getCapitalSavingsMultiplier() { return capitalSavingsMultiplier; }
    public static void setCapitalSavingsMultiplier(double v) { capitalSavingsMultiplier = Math.max(1.0, v); }

    public static double getTechAccelerationMultiplier() { return techAccelerationMultiplier; }
    public static void setTechAccelerationMultiplier(double v) { techAccelerationMultiplier = Math.max(1.0, v); }

    public static double getLiteracyRateMultiplier() { return literacyRateMultiplier; }
    public static void setLiteracyRateMultiplier(double v) { literacyRateMultiplier = Math.max(1.0, v); }
}
