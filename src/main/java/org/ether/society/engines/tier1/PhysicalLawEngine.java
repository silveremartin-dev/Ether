/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Physical Laws & Constants Customization Engine.
 * Allows altering fundamental physical parameters (Photosynthetic efficiency, room-temp superconductivity,
 * entropy generation rate) to simulate alternate physical realities.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PhysicalLawEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalLawEngine.class);

    private static double photosyntheticEfficiencyMultiplier = 1.0; // 1.0 = standard, 2.0 = double biomass yield
    private static boolean roomTemperatureSuperconductivity = false; // Zero Joule heat loss (I^2 * R = 0)
    private static double entropyGenerationScale = 1.0; // Scale factor for waste heat generation

    /*
     * Apply physical laws.
     * Enforces physical invariants and updates associated state variables within {@code PhysicalLawEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void applyPhysicalLaws(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // Photosynthetic efficiency boost to natural biomass
            if (photosyntheticEfficiencyMultiplier != 1.0) {
                double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
                cell.setBiomassNatural(naturalBiomass * (1.0 + (photosyntheticEfficiencyMultiplier - 1.0) * 0.02 * deltaYears));
            }

            // Room-temperature superconductivity eliminates energy transmission loss and boosts work output
            if (roomTemperatureSuperconductivity && cell.getResourceWork() != null) {
                cell.setResourceWork(cell.getResourceWork() * 1.05);
            }
        }
    }

    // Getters and Setters
    /*
     * Get photosynthetic efficiency multiplier.
     * Enforces physical invariants and updates associated state variables within {@code PhysicalLawEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getPhotosyntheticEfficiencyMultiplier() { return photosyntheticEfficiencyMultiplier; }
    public static void setPhotosyntheticEfficiencyMultiplier(double mult) { photosyntheticEfficiencyMultiplier = Math.max(0.1, mult); }

    /*
     * Is room temperature superconductivity.
     * Enforces physical invariants and updates associated state variables within {@code PhysicalLawEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isRoomTemperatureSuperconductivity() { return roomTemperatureSuperconductivity; }
    public static void setRoomTemperatureSuperconductivity(boolean active) { roomTemperatureSuperconductivity = active; }

    /*
     * Get entropy generation scale.
     * Enforces physical invariants and updates associated state variables within {@code PhysicalLawEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getEntropyGenerationScale() { return entropyGenerationScale; }
    public static void setEntropyGenerationScale(double scale) { entropyGenerationScale = Math.max(0.01, scale); }
}


