/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Religion, Ideology & Belief Systems Simulation Engine.
 * Models:
 * 1. <b>Evolution of Belief Systems</b>: Animism (Prehistory) -> Polytheism/Monotheism (Antiquity/Medieval) -> Rationalism/Ideologies (Modern).
 * 2. <b>Social Cohesion & Fertility Impact</b>: Traditional religious systems boost fertility and social cohesion (Asabiyyah),
 *    whereas secular rationalism boosts research speed but lowers fertility.
 * 3. <b>Religious Friction & Holy Conflict Triggers</b>: Inter-ideological friction when neighboring cells adhere to opposing belief systems.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ReligionIdeologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(ReligionIdeologyEngine.class);

    public record BeliefSystem(String name, double fertilityModifier, double cohesionModifier, double techSpeedModifier) {}

    public static final BeliefSystem ANIMISM = new BeliefSystem("Animisme", 1.15, 1.10, 0.90);
    public static final BeliefSystem MONOTHEISM = new BeliefSystem("Monothéisme / Dogme", 1.25, 1.20, 1.00);
    public static final BeliefSystem RATIONALISM = new BeliefSystem("Rationalisme / Science", 0.85, 0.90, 1.40);

    /**
     * Executes one tick of religion and ideological dynamics across cells.
     */
    public static void processReligionAndIdeology(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int beliefTransitions = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            String currentBelief = cell.getActivePathogenName(); // Storing active belief in cell metadata if non-epidemic or custom field

            // 1. Ideological Evolution based on Era Tech Level
            BeliefSystem activeSystem = ANIMISM;
            if (tech >= 2.5 && tech < 7.5) {
                activeSystem = MONOTHEISM;
            } else if (tech >= 7.5) {
                activeSystem = RATIONALISM;
            }

            // Apply Belief Modifiers to Cell Metrics
            cell.setFertility(Math.max(0.5, cell.getFertility() * activeSystem.fertilityModifier()));

            if (cell.getOwner() != null) {
                double currentAsabiyyah = cell.getOwner().getAsabiyyah();
                cell.getOwner().setAsabiyyah(Math.min(1.0, Math.max(0.0, currentAsabiyyah * activeSystem.cohesionModifier())));
            }

            // Tech speed modification
            cell.setResourceCapital(cell.getResourceCapital() * activeSystem.techSpeedModifier());
        }
    }
}

