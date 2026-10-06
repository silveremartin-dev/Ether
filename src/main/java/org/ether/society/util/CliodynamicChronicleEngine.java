/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.model.ScenarioTimeline;
import java.util.ArrayList;
import java.util.List;

/**
 * Generative Historical Chronicles Engine.
 * Converts physical simulation events, timeline entries, and cliodynamic alerts
 * into historiographical prose ("Chroniques Historiques de la Civilisation").
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CliodynamicChronicleEngine {

    /*
     * Generate chronicles.
     * Enforces physical invariants and updates associated state variables within {@code CliodynamicChronicleEngine}.
     *
     * @param timeline the timeline parameter (ScenarioTimeline)
     * @return the resulting computation or state reference
     */
    public static List<String> generateChronicles(ScenarioTimeline timeline) {
        List<String> prose = new ArrayList<>();
        if (timeline == null || timeline.getEntries().isEmpty()) {
            prose.add("📜 En l'an initial, la biomasse et l'énergie du monde s'éveillèrent en silence.");
            return prose;
        }

        for (ScenarioTimeline.TimelineEntry entry : timeline.getEntries()) {
            String eraTag = entry.year() < 0 ? Math.abs(entry.year()) + " av. J.-C." : "l'an " + entry.year();
            String paragraph = String.format("📜 En %s, les chroniques conservent le souvenir de : %s. %s",
                eraTag, entry.title(), entry.details());
            prose.add(paragraph);
        }

        return prose;
    }
}

