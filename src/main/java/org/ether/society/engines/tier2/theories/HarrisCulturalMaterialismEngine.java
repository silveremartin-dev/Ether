/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.H3Cell;

import org.ether.society.engines.tier2.historical.CulturalMaterialismPureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Marvin Harris Cultural Materialism Engine.
 *
 * <p>Implements Marvin Harris's cultural materialism paradigm (1979, <i>Cultural Materialism: The Struggle for a Science of Culture</i>):
 * societal evolution follows a deterministic primacy of infrastructure:
 * <b>Infrastructure (Techno-Environmental/Caloric Base) &rarr; Structure (Economy/Polity) &rarr; Superstructure (Ideology/Taboos)</b>.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CulturalMaterialismPureEngine
 */
public class HarrisCulturalMaterialismEngine extends AbstractSimulationEngine {
    public HarrisCulturalMaterialismEngine() {
        super("HarrisCulturalMaterialismEngine", "HarrisCulturalMaterialismEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(HarrisCulturalMaterialismEngine.class);

    @Override
    public String getName() {
        return "HarrisCulturalMaterialismEngine";
    }

    @Override
    public String getDescription() {
        return "Models Marvin Harris (1979) cultural materialism: infrastructural caloric and ecological deficits force structural political collapse.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Marvin Harris Cultural Materialism (Harris 1979)]
               • Infrastructural Primacy: Infrastructure -> Structure -> Superstructure
               • Caloric Threshold:     If Food/Pop < 0.05 -> Structural Capital decay at 10%/year
               Ref: M. Harris (1979), "Cultural Materialism", Random House.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Anthropological Theories";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        CulturalMaterialismPureEngine.processHybrid(cells, deltaYears);
    }
}
